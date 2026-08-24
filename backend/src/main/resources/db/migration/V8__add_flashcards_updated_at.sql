-- V8: Add missing updated_at column to flashcards
ALTER TABLE flashcards ADD COLUMN updated_at DATETIME NULL;
