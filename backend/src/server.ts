import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import healthRoutes from './routes/healthRoutes';
import { closeDatabasePool } from './config/database';
import purchaseRequestRoutes from './routes/purchaseRequestRoutes';
import listingRoutes from './routes/listingRoutes';
import userRoutes from './routes/userRoutes';

const app = express();
const port = Number(process.env.PORT || 3000);

app.use(cors());
app.use(express.json({ limit: '2mb' }));
app.use('/health', healthRoutes);
app.use('/listings', listingRoutes);
app.use('/purchase-requests', purchaseRequestRoutes);
app.use('/users', userRoutes);

app.use((_request, response) => {
  response.status(404).json({ message: 'Endpoint not found.' });
});

app.use((error: unknown, _request: express.Request, response: express.Response, _next: express.NextFunction) => {
  console.error('Request failed:', error);
  response.status(500).json({ message: 'Internal server error.' });
});

const server = app.listen(port, () => {
  console.log(`Student Marketplace API listening on port ${port}`);
});

async function shutdown(): Promise<void> {
  server.close(async () => {
    await closeDatabasePool();
    process.exit(0);
  });
}

process.on('SIGINT', () => void shutdown());
process.on('SIGTERM', () => void shutdown());
