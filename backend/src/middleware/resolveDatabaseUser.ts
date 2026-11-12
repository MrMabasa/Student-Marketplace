import type { NextFunction, Request, Response } from 'express';
import type { RowDataPacket } from 'mysql2';
import { getDatabasePool } from '../config/database';

declare global {
  namespace Express {
    interface Request {
      databaseUserId?: number;
    }
  }
}

interface UserRow extends RowDataPacket {
  user_id: number;
  firebase_uid: string | null;
}

export async function resolveDatabaseUser(
  request: Request,
  response: Response,
  next: NextFunction
): Promise<void> {
  const firebaseUser = request.firebaseUser;
  if (!firebaseUser?.uid) {
    response.status(401).json({ message: 'Authentication is required.' });
    return;
  }

  const connection = await getDatabasePool().getConnection();
  try {
    await connection.beginTransaction();
    const [matches] = await connection.execute<UserRow[]>(
      'SELECT user_id, firebase_uid FROM users WHERE firebase_uid = ? LIMIT 1 FOR UPDATE',
      [firebaseUser.uid]
    );

    if (matches.length > 0) {
      request.databaseUserId = matches[0].user_id;
      await connection.commit();
      next();
      return;
    }

    if (!firebaseUser.email || firebaseUser.email_verified !== true) {
      await connection.rollback();
      response.status(403).json({ message: 'A verified email is required to link this account.' });
      return;
    }

    const [emailMatches] = await connection.execute<UserRow[]>(
      'SELECT user_id, firebase_uid FROM users WHERE email = ? LIMIT 2 FOR UPDATE',
      [firebaseUser.email]
    );
    if (emailMatches.length !== 1 || emailMatches[0].firebase_uid !== null) {
      await connection.rollback();
      response.status(403).json({ message: 'This Firebase account is not linked to a marketplace user.' });
      return;
    }

    await connection.execute(
      'UPDATE users SET firebase_uid = ? WHERE user_id = ? AND firebase_uid IS NULL',
      [firebaseUser.uid, emailMatches[0].user_id]
    );
    const [linkedUsers] = await connection.execute<UserRow[]>(
      'SELECT user_id, firebase_uid FROM users WHERE firebase_uid = ? LIMIT 1',
      [firebaseUser.uid]
    );
    if (linkedUsers.length !== 1) {
      await connection.rollback();
      response.status(403).json({ message: 'This Firebase account could not be linked.' });
      return;
    }

    request.databaseUserId = linkedUsers[0].user_id;
    await connection.commit();
    next();
  } catch (error) {
    await connection.rollback();
    next(error);
  } finally {
    connection.release();
  }
}
