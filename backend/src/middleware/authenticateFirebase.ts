import type { NextFunction, Request, Response } from 'express';
import type { DecodedIdToken } from 'firebase-admin/auth';
import { verifyFirebaseIdToken } from '../config/firebaseAdmin';

declare global {
  namespace Express {
    interface Request {
      firebaseUser?: DecodedIdToken;
    }
  }
}

export async function authenticateFirebase(
  request: Request,
  response: Response,
  next: NextFunction
): Promise<void> {
  const authorization = request.header('authorization');
  const match = authorization?.match(/^Bearer\s+(.+)$/i);
  if (!match) {
    response.status(401).json({ message: 'A Firebase ID token is required.' });
    return;
  }

  try {
    request.firebaseUser = await verifyFirebaseIdToken(match[1]);
    next();
  } catch {
    response.status(401).json({ message: 'The Firebase ID token is invalid or expired.' });
  }
}
