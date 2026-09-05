# MindQ Database Documentation

## Database

MySQL 8.x

Database name:

```text
mindq_db
```

Schema management uses Flyway in the production configuration.

## Core Tables

- `users`
- `study_materials`
- `ai_models`
- `mcq_sets`
- `questions`
- `question_options`
- `quiz_attempts`
- `quiz_answers`
- `saved_questions`
- `generation_history`
- `refresh_tokens`
- `password_reset_tokens`
- `failed_login_attempts`
- `email_otps`
- `email_logs`
- `flashcard_sets`
- `flashcards`
- `plans`
- `user_subscriptions`
- `payment_transactions`

## Relationships

```text
User
 ├── StudyMaterial
 ├── McqSet
 │     └── Question
 │           └── QuestionOption
 ├── QuizAttempt
 │     └── QuizAnswer
 ├── GenerationHistory
 ├── RefreshToken
 ├── PasswordResetToken
 ├── FlashcardSet
 ├── UserSubscription
 └── PaymentTransaction
```

## Migration History

Flyway migrations V1–V9 establish the current schema and later additions such as flashcards, subscriptions, payments, OTPs, token versioning and email logs.

`ddl-auto=validate` is used where Flyway owns the schema.

Never use Hibernate schema updates as the production schema migration mechanism.
