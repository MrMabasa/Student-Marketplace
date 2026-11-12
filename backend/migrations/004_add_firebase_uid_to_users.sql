ALTER TABLE users
    ADD COLUMN firebase_uid VARCHAR(128) NULL,
    ADD UNIQUE INDEX uq_users_firebase_uid (firebase_uid);
