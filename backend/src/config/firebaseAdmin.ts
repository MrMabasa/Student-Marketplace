import { applicationDefault, getApps, initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';

function getFirebaseAuth() {
  const app = getApps()[0] ?? initializeApp({ credential: applicationDefault() });
  return getAuth(app);
}

export async function verifyFirebaseIdToken(token: string) {
  return getFirebaseAuth().verifyIdToken(token);
}
