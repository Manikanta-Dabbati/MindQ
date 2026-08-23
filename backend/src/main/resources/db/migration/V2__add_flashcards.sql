-- MindQ V2 — Flashcard Tables
-- Added: 2026-08-19

-- =====================================================
-- FLASHCARD SETS
-- =====================================================
CREATE TABLE flashcard_sets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    study_material_id BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    total_cards INT NOT NULL DEFAULT 0,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT fk_flashcard_sets_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_flashcard_sets_material FOREIGN KEY (study_material_id) REFERENCES study_materials(id)
);
CREATE INDEX idx_flashcard_sets_user ON flashcard_sets(user_id);
CREATE INDEX idx_flashcard_sets_created ON flashcard_sets(created_at);

-- =====================================================
-- FLASHCARDS
-- =====================================================
CREATE TABLE flashcards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flashcard_set_id BIGINT NOT NULL,
    front TEXT NOT NULL,
    back TEXT NOT NULL,
    order_index INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_flashcards_set FOREIGN KEY (flashcard_set_id) REFERENCES flashcard_sets(id)
);
CREATE INDEX idx_flashcards_set ON flashcards(flashcard_set_id);
