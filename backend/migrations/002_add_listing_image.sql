-- Add optional image storage to marketplace listings.
-- Existing listings remain valid because these columns allow NULL.

ALTER TABLE listings
ADD COLUMN image_data MEDIUMBLOB NULL,
ADD COLUMN image_mime_type VARCHAR(100) NULL;