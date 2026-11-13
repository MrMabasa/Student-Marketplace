
import {
    applicationDefault,
    getApps,
    initializeApp,
    type App
} from 'firebase-admin/app';

import { getAuth } from 'firebase-admin/auth';
import { getMessaging } from 'firebase-admin/messaging';

function getFirebaseApp(): App {
    return getApps()[0] ?? initializeApp({
        credential: applicationDefault()
    });
}

function getFirebaseAuth() {
    return getAuth(getFirebaseApp());
}

export async function verifyFirebaseIdToken(token: string) {
    return getFirebaseAuth().verifyIdToken(token);
}

export async function sendPushNotification(
    token: string,
    title: string,
    body: string,
    data?: Record<string, string>
): Promise<string> {

    const messageId = await getMessaging(getFirebaseApp()).send({
        token,
        notification: {
            title,
            body
        },
        ...(data ? { data } : {})
    });

    return messageId;
}
