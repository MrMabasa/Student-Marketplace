import 'dotenv/config';
import mysql from 'mysql2/promise';
import fs from 'fs';

async function inspectDatabase(): Promise<void> {
    const connection = await mysql.createConnection({
        host: process.env.DB_HOST,
        port: Number(process.env.DB_PORT),
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        ssl: {
            ca: fs.readFileSync(process.env.DB_CA_CERT_PATH!),
            rejectUnauthorized: true
        }
    });

    try {
        const tables = [
            'users',
            'listings',
            'favourites',
            'purchase_requests',
            'schema_migrations'
        ];
        for (const table of tables) {
            const [rows] = await connection.query(
                `SHOW CREATE TABLE \`${table}\``
            );

            console.log(`\n--- ${table.toUpperCase()} ---`);
            console.log((rows as any[])[0]['Create Table']);
        }
    } finally {
        await connection.end();
    }
}

inspectDatabase().catch(error => {
    console.error('Database inspection failed:', error.message);
    process.exitCode = 1;
});