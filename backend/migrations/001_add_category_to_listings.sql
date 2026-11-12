-- Add category to existing marketplace listings.
ALTER TABLE listings
ADD COLUMN category VARCHAR(100) NOT NULL DEFAULT 'Other Student Items';