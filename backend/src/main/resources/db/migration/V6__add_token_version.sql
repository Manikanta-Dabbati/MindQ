-- Add token_version column to users table for JWT invalidation
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
