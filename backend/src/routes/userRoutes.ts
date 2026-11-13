
import { Router, type Request, type Response } from 'express';
import type { ResultSetHeader, RowDataPacket } from 'mysql2';
import { getDatabasePool } from '../config/database';
import { authenticateFirebase } from '../middleware/authenticateFirebase';

const router = Router();

interface ExistingUser extends RowDataPacket {
    user_id: number;
    firebase_uid: string | null;
    email: string;
    student_number: string;
}

router.post(
    '/register',
    authenticateFirebase,
    async (request: Request, response: Response): Promise<void> => {
        try {
            const firebaseUser = request.firebaseUser;

            if (!firebaseUser?.uid || !firebaseUser.email) {
                response.status(401).json({
                    message: 'A valid Firebase account is required.'
                });
                return;
            }

            const fullName =
                typeof request.body.full_name === 'string'
                    ? request.body.full_name.trim()
                    : '';

            const studentNumber =
                typeof request.body.student_number === 'string'
                    ? request.body.student_number.trim()
                    : '';

            const institution =
                typeof request.body.institution === 'string'
                    ? request.body.institution.trim()
                    : '';

            const email = firebaseUser.email.trim().toLowerCase();

            if (!fullName || !studentNumber || !institution) {
                response.status(400).json({
                    message: 'Full name, student number and institution are required.'
                });
                return;
            }

            if (
                fullName.length > 255 ||
                studentNumber.length > 100 ||
                institution.length > 255
            ) {
                response.status(400).json({
                    message: 'One or more registration fields exceed the allowed length.'
                });
                return;
            }

            const pool = getDatabasePool();

            // Make registration safe to retry for the same Firebase account.
            const [existingByFirebase] = await pool.execute<ExistingUser[]>(
                `SELECT user_id, firebase_uid, email, student_number
                   FROM users
                  WHERE firebase_uid = ?
                  LIMIT 1`,
                [firebaseUser.uid]
            );

            if (existingByFirebase.length > 0) {
                const existing = existingByFirebase[0];

                if (
                    existing.email.toLowerCase() !== email ||
                    existing.student_number !== studentNumber
                ) {
                    response.status(409).json({
                        message: 'This Firebase account is already registered with different student details.'
                    });
                    return;
                }

                response.status(200).json({
                    message: 'Marketplace account already registered.',
                    user_id: existing.user_id
                });
                return;
            }

            // Never automatically attach a Firebase account to an
            // unrelated existing MySQL account.
            const [existingByEmail] = await pool.execute<ExistingUser[]>(
                `SELECT user_id
                   FROM users
                  WHERE email = ?
                  LIMIT 1`,
                [email]
            );

            if (existingByEmail.length > 0) {
                response.status(409).json({
                    message: 'An account with this email already exists in the marketplace.'
                });
                return;
            }

            const [existingByStudentNumber] = await pool.execute<ExistingUser[]>(
                `SELECT user_id
                   FROM users
                  WHERE student_number = ?
                  LIMIT 1`,
                [studentNumber]
            );

            if (existingByStudentNumber.length > 0) {
                response.status(409).json({
                    message: 'This student number is already registered.'
                });
                return;
            }

            const [result] = await pool.execute<ResultSetHeader>(
                `INSERT INTO users
                    (full_name, email, password_hash, student_number,
                     verification_status, firebase_uid, institution)
                 VALUES (?, ?, NULL, ?, 0, ?, ?)`,
                [
                    fullName,
                    email,
                    studentNumber,
                    firebaseUser.uid,
                    institution
                ]
            );

            response.status(201).json({
                message: 'Marketplace account created successfully.',
                user_id: result.insertId
            });

        } catch (error) {
            const databaseError = error as { code?: string };

            if (databaseError.code === 'ER_DUP_ENTRY') {
                response.status(409).json({
                    message: 'An account with this email, student number or Firebase UID already exists.'
                });
                return;
            }

            console.error('Student registration failed:', error);

            response.status(500).json({
                message: 'Unable to create your marketplace account. Please try again.'
            });
        }
    }
);

router.post(
    '/register-device',
    authenticateFirebase,
    async (request: Request, response: Response): Promise<void> => {
        try {
            const firebaseUser = request.firebaseUser;

            if (!firebaseUser?.uid) {
                response.status(401).json({
                    message: 'A valid Firebase account is required.'
                });
                return;
            }

            const token =
                typeof request.body.fcm_token === 'string'
                    ? request.body.fcm_token.trim()
                    : '';

            if (!token || token.length > 512) {
                response.status(400).json({
                    message: 'A valid FCM device token is required.'
                });
                return;
            }

            const pool = getDatabasePool();

            // Resolve the authenticated Firebase account to its marketplace user.
            const [users] = await pool.execute<RowDataPacket[]>(
                `SELECT user_id
                   FROM users
                  WHERE firebase_uid = ?
                  LIMIT 1`,
                [firebaseUser.uid]
            );

            if (users.length === 0) {
                response.status(404).json({
                    message: 'Marketplace account not found. Please complete registration first.'
                });
                return;
            }

            const userId = users[0].user_id as number;

            // A device token can belong to only one marketplace user.
            await pool.execute(
                `INSERT INTO device_tokens (user_id, fcm_token)
                 VALUES (?, ?)
                 ON DUPLICATE KEY UPDATE
                    user_id = VALUES(user_id),
                    updated_at = CURRENT_TIMESTAMP`,
                [userId, token]
            );

            response.status(200).json({
                message: 'Device registered for notifications successfully.'
            });

        } catch (error) {
            console.error('Device token registration failed:', error);

            response.status(500).json({
                message: 'Unable to register this device for notifications.'
            });
        }
    }
);

export default router;
