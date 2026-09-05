# Testing Guide

## Backend

Run:

```bash
cd backend
mvn test
```

Tests cover authentication, security, materials, document upload, MCQ generation, parsing, AI provider routing, payments, subscriptions, repositories and application context.

AI generation tests use deterministic mocks rather than depending on live Groq availability.

## Frontend

Run:

```bash
cd frontend
npm test
```

Production build:

```bash
npm run build
```

## E2E

Install Playwright browsers:

```bash
npx playwright install
```

Run E2E tests according to the current `package.json` script.

## Critical End-to-End Journey

```text
Register
 ↓
Email Verification
 ↓
Login
 ↓
Upload Material
 ↓
Generate MCQ
 ↓
Take Quiz
 ↓
Submit
 ↓
Results
 ↓
Download / Save
 ↓
History
```

## Testing Principles

- Keep normal tests deterministic.
- Mock external AI providers in unit/integration tests.
- Use sandbox payment environments for payment testing.
- Test user ownership/authorization.
- Test storage limits.
- Test authentication/session invalidation.
