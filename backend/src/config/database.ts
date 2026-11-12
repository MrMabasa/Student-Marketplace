import fs from 'node:fs';
import path from 'node:path';
import mysql, { Pool } from 'mysql2/promise';

let pool: Pool | undefined;

export function getDatabasePool(): Pool {
  if (pool) return pool;

  const required = ['DB_HOST', 'DB_NAME', 'DB_USER', 'DB_PASSWORD'] as const;
  for (const key of required) {
    if (!process.env[key]) throw new Error(`Missing required environment variable: ${key}`);
  }

  const caPath = process.env.DB_CA_CERT_PATH;
  if (!caPath) throw new Error('Missing required environment variable: DB_CA_CERT_PATH');
  const resolvedCaPath = path.resolve(process.cwd(), caPath);
  if (!fs.existsSync(resolvedCaPath)) {
    throw new Error(`Aiven CA certificate not found at ${resolvedCaPath}. Add the official Aiven CA certificate.`);
  }

  pool = mysql.createPool({
    host: process.env.DB_HOST,
    port: Number(process.env.DB_PORT || 3306),
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    waitForConnections: true,
    connectionLimit: 5,
    queueLimit: 0,
    ssl: {
      ca: fs.readFileSync(resolvedCaPath, 'utf8'),
      rejectUnauthorized: true
    }
  });

  return pool;
}

export async function checkDatabaseConnection(): Promise<void> {
  const connection = await getDatabasePool().getConnection();
  try {
    await connection.query('SELECT 1 AS database_ok');
  } finally {
    connection.release();
  }
}

export async function closeDatabasePool(): Promise<void> {
  if (pool) {
    await pool.end();
    pool = undefined;
  }
}
