-- V7: Add missing topic column to flashcard_sets
ALTER TABLE flashcard_sets ADD COLUMN topic VARCHAR(150) AFTER description;
