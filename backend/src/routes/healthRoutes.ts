import { Router } from 'express';
import { checkDatabaseConnection } from '../config/database';

const router = Router();

router.get('/', (_request, response) => {
  response.status(200).json({ status: 'ok', service: 'student-marketplace-api' });
});

router.get('/database', async (_request, response) => {
  try {
    await checkDatabaseConnection();
    response.status(200).json({ status: 'ok', database: 'connected' });
  } catch (error) {
    console.error('Database health check failed:', error);
    response.status(503).json({ status: 'error', database: 'unavailable' });
  }
});

export default router;
