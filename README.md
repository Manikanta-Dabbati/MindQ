# MindQ — AI-Powered Learning Platform

MindQ turns study material into practice. Upload PDFs/DOCX or paste text into a Knowledge Vault, generate multiple-choice quizzes with AI (Groq), take practice, timed, or exam-style quizzes, review every answer with explanations, and track your progress with analytics.

> **Sync Your Mind with AI**

## Feature Status

**Implemented**
- **Accounts** — registration with email OTP verification, password and OTP login, JWT access tokens with rotating refresh tokens, account lockout, password reset, logout on all devices
- **Knowledge Vault** — PDF/DOCX upload with text extraction (Apache PDFBox / POI), text paste, search, 500 MB free storage quota
- **MCQ generation** — 1–20 questions per set, Easy/Medium/Hard difficulty, selectable Groq models, retry with backoff, generation history with token/latency telemetry
- **Quiz engine** — Practice / Timed / Exam modes, server-side scoring, per-question correct answers and explanations on review, attempt history
- **Insights** — analytics overview (accuracy, study streak, topic performance, 7-day activity) and dashboard weak areas
- **Retention** — branded PDF export (OpenPDF) and one-click save of any quiz back to the Vault
- **Platform** — subscriptions with Razorpay checkout (disabled by default), admin panel, global search, light/dark/system theme, health endpoints, Flyway migrations, Docker deployment, CI

**Backend implemented, UI coming soon** — Summarizer, Flashcards, Revision Notes

**Planned** — adaptive quizzes targeting weak areas, plan-based entitlement enforcement, DOCX/JSON export, refunds and cancellation

## Architecture

MindQ is a **modular monolith** with a single Spring Boot API and a React SPA.

| Layer | Stack |
|---|---|
| Backend | Java 21, Spring Boot **4.1**, Spring Security (JWT), Spring Data JPA, Flyway, MySQL 8, SpringDoc OpenAPI |
| AI | **Groq API** (OpenAI-compatible chat completions) behind a pluggable `AIProvider` abstraction — models: `openai/gpt-oss-20b` (default), `openai/gpt-oss-120b`, `qwen/qwen3.6-27b` |
| Payments | Razorpay (Orders API, Standard Checkout, signature-verified webhooks) |
| Frontend | React 18, TypeScript, Vite 6, Tailwind CSS 4, React Router 6, Axios, Lucide icons |
| Infra | Docker Compose (MySQL, backend, nginx frontend, MailDev) and GitHub Actions CI |

```
MindQ/
├── backend/          # Java 21 + Spring Boot 4.1 REST API
└── frontend/         # React + TypeScript + Vite + Tailwind CSS SPA
```

## Getting Started

### Prerequisites
- JDK 21 and Maven 3.9+
- Node.js 18+ and npm
- MySQL 8.x
- A Groq API key — https://console.groq.com/keys

### 1. Configure environment
```bash
cp .env.example .env
```
Set your database credentials, `JWT_SECRET` (at least 32 characters — the backend refuses to start with weak or default values), `GROQ_API_KEY`, and mail settings. See `.env.example` (root) and `backend/.env.example` (Razorpay) for the full list of supported variables.

### 2. Run the backend
```bash
cd backend
mvn spring-boot:run
```
- API at `http://localhost:8080` — health check: `http://localhost:8080/api/v1/health`
- Swagger UI (non-production profiles): `http://localhost:8080/swagger-ui.html`
- Flyway migrations run automatically on startup; plans and AI models are seeded automatically.

### 3. Run the frontend
```bash
cd frontend
npm install
npm run dev
```
App at `http://localhost:5173` (proxies `/api` to `:8080`).

### Docker
```bash
docker compose up -d --build
```
- Frontend: `http://localhost:80` · API: `http://localhost:8080`
- MySQL is exposed on host port **3307** (container 3306)
- MailDev UI for local email testing: `http://localhost:1080`

### Authentication in development
- If `MAIL_USERNAME` is empty, OTPs are printed to the backend console; otherwise configure SMTP (`MAIL_*`) or use MailDev.
- With `OTP_ENABLED=false` (the default) any 6-digit code is accepted for verification. The separate `OTP_BYPASS_ENABLED=true` flag enables development-only bypass endpoints. Keep both off in production.

## Testing

```bash
cd backend  && mvn test        # requires a reachable MySQL instance
cd frontend && npm test        # Vitest unit tests
cd frontend && npx playwright install && npm run test:e2e   # Playwright E2E (chromium)
```

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs backend tests against a MySQL service, frontend unit tests and build, then builds both Docker images and verifies the full compose stack comes up healthy.

## AI Model Catalog

| Model | Role |
|---|---|
| `openai/gpt-oss-20b` | Default — fast everyday generation |
| `openai/gpt-oss-120b` | Deeper reasoning |
| `qwen/qwen3.6-27b` | Balanced |

Generation requests are validated strictly: exact question count, exactly four options, a single correct answer (`A`–`D`), an explanation, and a per-question difficulty — malformed responses are retried, then rejected.
