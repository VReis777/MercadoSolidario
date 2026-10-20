-- Add fields to track previous beneficiaries (years 2024, 2025, etc.)
-- Note: H2/PostgreSQL don't support IF NOT EXISTS with ALTER TABLE
-- This migration is designed to run once
ALTER TABLE families ADD COLUMN previous_beneficiary BOOLEAN DEFAULT FALSE;
ALTER TABLE families ADD COLUMN previous_beneficiary_years VARCHAR(255);
ALTER TABLE families ADD COLUMN previous_beneficiary_notes TEXT;
