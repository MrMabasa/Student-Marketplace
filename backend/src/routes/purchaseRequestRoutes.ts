
import { Router, type Request, type Response } from 'express';
import type { ResultSetHeader, RowDataPacket } from 'mysql2';
import { getDatabasePool } from '../config/database';
import { authenticateFirebase } from '../middleware/authenticateFirebase';
import { resolveDatabaseUser } from '../middleware/resolveDatabaseUser';
import { sendPushNotification } from '../config/firebaseAdmin';

const router = Router();

router.use(authenticateFirebase, resolveDatabaseUser);

class HttpError extends Error {
    constructor(
        public readonly status: number,
        message: string
    ) {
        super(message);
    }
}

interface ListingRow extends RowDataPacket {
    listing_id: number;
    seller_id: number;
    status: string;
    title: string;
}

interface RequestRow extends RowDataPacket {
    request_id: number;
    listing_id: number;
    buyer_id: number;
    seller_id: number;
    status: 'Pending' | 'Accepted' | 'Declined' | 'Cancelled';
}

interface DeviceTokenRow extends RowDataPacket {
    fcm_token: string;
}

function currentUserId(request: Request): number {
    if (!request.databaseUserId) {
        throw new HttpError(401, 'Authentication is required.');
    }

    return request.databaseUserId;
}

async function notifyUser(
    userId: number,
    title: string,
    body: string,
    requestId: number
): Promise<void> {
    try {
        const [tokens] = await getDatabasePool().execute<DeviceTokenRow[]>(
            'SELECT fcm_token FROM device_tokens WHERE user_id = ?',
            [userId]
        );

        const results = await Promise.allSettled(
            tokens.map(({ fcm_token }) =>
                sendPushNotification(
                    fcm_token,
                    title,
                    body,
                    {
                        type: 'purchase_request',
                        request_id: String(requestId)
                    }
                )
            )
        );

        results.forEach((result) => {
            if (result.status === 'rejected') {
                console.error(
                    'Failed to deliver a marketplace notification:',
                    result.reason
                );
            }
        });

    } catch (error) {
        console.error(
            'Unable to send marketplace notification:',
            error
        );
    }
}

function positiveId(value: unknown): number | null {
    const parsed =
        typeof value === 'number'
            ? value
            : typeof value === 'string' && /^\d+$/.test(value)
                ? Number(value)
                : NaN;

    return Number.isSafeInteger(parsed) && parsed > 0
        ? parsed
        : null;
}

function errorStatus(error: unknown): number {
    return error instanceof HttpError ? error.status : 500;
}

function errorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'An unexpected error occurred.';
}

// Create a purchase request.
router.post('/', async (request, response) => {
    const buyerId = currentUserId(request);
    const listingId = positiveId(request.body?.listing_id);

    if (!listingId) {
        response.status(400).json({
            message: 'listing_id must be a positive integer.'
        });
        return;
    }

    const connection = await getDatabasePool().getConnection();

    try {
        await connection.beginTransaction();

        const [listings] = await connection.execute<ListingRow[]>(
            `SELECT listing_id, seller_id, status, title
               FROM listings
              WHERE listing_id = ?
              FOR UPDATE`,
            [listingId]
        );

        if (listings.length === 0) {
            throw new HttpError(404, 'Listing not found.');
        }

        const listing = listings[0];

        if (listing.status.toLowerCase() !== 'active') {
            throw new HttpError(409, 'Listing is not active.');
        }

        if (listing.seller_id === buyerId) {
            throw new HttpError(403, 'You cannot request your own listing.');
        }

        const [pending] = await connection.execute<RequestRow[]>(
            `SELECT request_id
               FROM purchase_requests
              WHERE listing_id = ?
                AND buyer_id = ?
                AND status = 'Pending'
              LIMIT 1`,
            [listingId, buyerId]
        );

        if (pending.length > 0) {
            throw new HttpError(
                409,
                'A pending request already exists for this listing.'
            );
        }

        const [insert] = await connection.execute<ResultSetHeader>(
            `INSERT INTO purchase_requests
                (listing_id, buyer_id, seller_id, status)
             VALUES (?, ?, ?, 'Pending')`,
            [listingId, buyerId, listing.seller_id]
        );

        await connection.commit();

        // Notify the seller after the request has been committed.
        await notifyUser(
            listing.seller_id,
            'New purchase request',
            `A student has requested to purchase "${listing.title}".`,
            insert.insertId
        );

        response.status(201).json({
            request_id: insert.insertId,
            listing_id: listingId,
            buyer_id: buyerId,
            seller_id: listing.seller_id,
            status: 'Pending'
        });

    } catch (error) {
        await connection.rollback();

        response.status(errorStatus(error)).json({
            message: errorMessage(error)
        });

    } finally {
        connection.release();
    }
});

// Get purchase requests submitted by the buyer.
router.get('/buyer', async (request, response) => {
    const [rows] = await getDatabasePool().execute(
        `SELECT pr.request_id, pr.listing_id, pr.buyer_id,
                pr.seller_id, pr.status, pr.date_requested,
                pr.date_updated, l.title, l.price
           FROM purchase_requests pr
           JOIN listings l ON l.listing_id = pr.listing_id
          WHERE pr.buyer_id = ?
          ORDER BY pr.date_requested DESC, pr.request_id DESC`,
        [currentUserId(request)]
    );

    response.status(200).json(rows);
});

// Get purchase requests received by the seller.
router.get('/seller', async (request, response) => {
    const [rows] = await getDatabasePool().execute(
        `SELECT pr.request_id, pr.listing_id, pr.buyer_id,
                pr.seller_id, pr.status, pr.date_requested,
                pr.date_updated, l.title, l.price
           FROM purchase_requests pr
           JOIN listings l ON l.listing_id = pr.listing_id
          WHERE pr.seller_id = ?
          ORDER BY pr.date_requested DESC, pr.request_id DESC`,
        [currentUserId(request)]
    );

    response.status(200).json(rows);
});

// Handle purchase-request status changes.
async function transitionRequest(
    request: Request,
    response: Response,
    actor: 'buyer' | 'seller',
    nextStatus: 'Accepted' | 'Declined' | 'Cancelled'
): Promise<void> {

    const requestId = positiveId(request.params.requestId);

    if (!requestId) {
        response.status(400).json({
            message: 'requestId must be a positive integer.'
        });
        return;
    }

    const userId = currentUserId(request);
    const connection = await getDatabasePool().getConnection();

    try {
        await connection.beginTransaction();

        const [initialRows] = await connection.execute<RequestRow[]>(
            `SELECT request_id, listing_id, buyer_id, seller_id, status
               FROM purchase_requests
              WHERE request_id = ?
              LIMIT 1`,
            [requestId]
        );

        if (initialRows.length === 0) {
            throw new HttpError(404, 'Purchase request not found.');
        }

        // Lock the listing first to serialize mutations per listing.
        const [listings] = await connection.execute<ListingRow[]>(
            `SELECT listing_id, seller_id, status, title
               FROM listings
              WHERE listing_id = ?
              FOR UPDATE`,
            [initialRows[0].listing_id]
        );

        const [requests] = await connection.execute<RequestRow[]>(
            `SELECT request_id, listing_id, buyer_id, seller_id, status
               FROM purchase_requests
              WHERE request_id = ?
              FOR UPDATE`,
            [requestId]
        );

        if (listings.length === 0 || requests.length === 0) {
            throw new HttpError(404, 'Purchase request not found.');
        }

        const listing = listings[0];
        const purchaseRequest = requests[0];

        const expectedOwner =
            actor === 'buyer'
                ? purchaseRequest.buyer_id
                : purchaseRequest.seller_id;

        if (expectedOwner !== userId) {
            throw new HttpError(
                403,
                'You do not own this purchase request.'
            );
        }

        if (purchaseRequest.status !== 'Pending') {
            throw new HttpError(
                409,
                'Only pending requests can change state.'
            );
        }

        if (nextStatus === 'Accepted') {

            if (listing.status.toLowerCase() !== 'active') {
                throw new HttpError(
                    409,
                    'The listing is no longer active.'
                );
            }

            const [accepted] = await connection.execute<RequestRow[]>(
                `SELECT request_id
                   FROM purchase_requests
                  WHERE listing_id = ?
                    AND status = 'Accepted'
                    AND request_id <> ?
                  LIMIT 1`,
                [purchaseRequest.listing_id, requestId]
            );

            if (accepted.length > 0) {
                throw new HttpError(
                    409,
                    'Another request has already been accepted for this listing.'
                );
            }
        }

        await connection.execute(
            `UPDATE purchase_requests
                SET status = ?
              WHERE request_id = ?
                AND status = 'Pending'`,
            [nextStatus, requestId]
        );

        await connection.commit();

        // The buyer receives seller decisions.
        // The seller receives buyer cancellations.
        const recipientId =
            actor === 'seller'
                ? purchaseRequest.buyer_id
                : purchaseRequest.seller_id;

        let notificationTitle: string;
        let notificationBody: string;

        switch (nextStatus) {
            case 'Accepted':
                notificationTitle = 'Purchase request accepted';
                notificationBody =
                    `Your request for "${listing.title}" has been accepted.`;
                break;

            case 'Declined':
                notificationTitle = 'Purchase request declined';
                notificationBody =
                    `Your request for "${listing.title}" has been declined.`;
                break;

            case 'Cancelled':
                notificationTitle = 'Purchase request cancelled';
                notificationBody =
                    `The request for "${listing.title}" has been cancelled.`;
                break;
        }

        await notifyUser(
            recipientId,
            notificationTitle,
            notificationBody,
            requestId
        );

        response.status(200).json({
            request_id: requestId,
            status: nextStatus
        });

    } catch (error) {
        await connection.rollback();

        response.status(errorStatus(error)).json({
            message: errorMessage(error)
        });

    } finally {
        connection.release();
    }
}

// Seller accepts a request.
router.patch('/:requestId/accept', (request, response) =>
    transitionRequest(request, response, 'seller', 'Accepted')
);

// Seller declines a request.
router.patch('/:requestId/decline', (request, response) =>
    transitionRequest(request, response, 'seller', 'Declined')
);

// Buyer cancels a request.
router.patch('/:requestId/cancel', (request, response) =>
    transitionRequest(request, response, 'buyer', 'Cancelled')
);

export default router;
