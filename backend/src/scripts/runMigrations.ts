
import 'dotenv/config';
import mysql, { type Connection, type RowDataPacket } from 'mysql2/promise';
import fs from 'fs';
import path from 'path';

async function ensureFirebaseUidSchema(connection: Connection): Promise<void> {
    const [columns] = await connection.execute<
        Array<RowDataPacket & { COLUMN_NAME: string }>
    >(
        `SELECT COLUMN_NAME
           FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = 'users'
            AND COLUMN_NAME = 'firebase_uid'`
    );

    if (columns.length === 0) {
        await connection.query(
            'ALTER TABLE users ADD COLUMN firebase_uid VARCHAR(128) NULL'
        );
    }

    const [uniqueIndexes] = await connection.execute<
        Array<RowDataPacket & { INDEX_NAME: string }>
    >(
        `SELECT DISTINCT INDEX_NAME
           FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = 'users'
            AND COLUMN_NAME = 'firebase_uid'
            AND NON_UNIQUE = 0`
    );

    if (uniqueIndexes.length === 0) {
        await connection.query(
            'ALTER TABLE users ADD UNIQUE INDEX uq_users_firebase_uid (firebase_uid)'
        );
    }
}

async function runMigrations(): Promise<void> {
    const connection = await mysql.createConnection({
        host: process.env.DB_HOST,
        port: Number(process.env.DB_PORT),
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        ssl: {
            ca: fs.readFileSync(process.env.DB_CA_CERT_PATH!),
            rejectUnauthorized: true
        },
        multipleStatements: false
    });

    try {
        await connection.query(`
            CREATE TABLE IF NOT EXISTS schema_migrations (
                migration_id INT NOT NULL AUTO_INCREMENT,
                migration_name VARCHAR(255) NOT NULL,
                applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (migration_id),
                UNIQUE KEY migration_name (migration_name)
            )
        `);

        const migrations = [
            '003_create_purchase_requests.sql',
            '004_add_firebase_uid_to_users.sql',
            '005_add_full_name_to_users.sql',
            '006_make_password_hash_nullable.sql',
            '007_create_device_tokens.sql'
        ];

        for (const migrationName of migrations) {
            const [existing] = await connection.execute<
                RowDataPacket[]
            >(
                'SELECT migration_id FROM schema_migrations WHERE migration_name = ?',
                [migrationName]
            );

            if (existing.length > 0) {
                console.log(`${migrationName} has already been applied.`);
                continue;
            }

            const migrationPath = path.resolve(
                process.cwd(),
                'migrations',
                migrationName
            );

            const sql = fs.readFileSync(migrationPath, 'utf8');

            await connection.beginTransaction();

            try {
                if (migrationName === '004_add_firebase_uid_to_users.sql') {
                    await ensureFirebaseUidSchema(connection);

                } else if (migrationName === '005_add_full_name_to_users.sql') {
                    const [columns] = await connection.execute<
                        Array<RowDataPacket & { COLUMN_NAME: string }>
                    >(
                        `SELECT COLUMN_NAME
                           FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE()
                            AND TABLE_NAME = 'users'
                            AND COLUMN_NAME = 'full_name'`
                    );

                    if (columns.length === 0) {
                        await connection.query(sql);
                        console.log('Added full_name column to users.');
                    } else {
                        console.log(
                            'full_name column already exists. Skipping ALTER TABLE.'
                        );
                    }

                } else if (migrationName === '006_make_password_hash_nullable.sql') {
                    const [columns] = await connection.execute<
                        Array<RowDataPacket & { IS_NULLABLE: string }>
                    >(
                        `SELECT IS_NULLABLE
                           FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = DATABASE()
                            AND TABLE_NAME = 'users'
                            AND COLUMN_NAME = 'password_hash'`
                    );

                    if (columns.length === 0) {
                        throw new Error('password_hash column does not exist.');
                    }

                    if (columns[0].IS_NULLABLE === 'NO') {
                        await connection.query(sql);
                        console.log('Made password_hash nullable.');
                    } else {
                        console.log(
                            'password_hash is already nullable. Skipping ALTER TABLE.'
                        );
                    }

                } else {
                    await connection.query(sql);
                }

                await connection.execute(
                    'INSERT INTO schema_migrations (migration_name) VALUES (?)',
                    [migrationName]
                );

                await connection.commit();

                console.log(`Successfully applied ${migrationName}.`);

            } catch (error) {
                await connection.rollback();
                throw error;
            }
        }

    } finally {
        await connection.end();
    }
}

runMigrations().catch(error => {
    console.error(
        'Migration failed:',
        error instanceof Error ? error.message : error
    );
    process.exitCode = 1;
});
