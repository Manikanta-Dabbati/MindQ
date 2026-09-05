# MindQ Architecture

## Architecture Style

MindQ uses a **Modular Monolith**.

All backend business modules run inside a single Spring Boot application while maintaining clear domain boundaries.

## Backend Layers

```text
Controller
   ↓
DTO / Validation
   ↓
Service
   ↓
Repository / Integration
   ↓
MySQL / External Provider
```

Controllers remain thin. Business logic belongs in services. JPA entities are not used as public API contracts.

## Main Modules

- `auth` — authentication and account lifecycle
- `security` — JWT, refresh tokens, filters and rate limiting
- `material` — Knowledge Vault and document processing
- `ai` — provider abstraction and AI services
- `mcq` — generation, quiz retrieval, scoring and export
- `analytics` — learning statistics
- `payment` — Razorpay integration
- `admin` — administration
- `search` — global search
- `common` — shared API/error/health/metrics infrastructure
- `config` — application configuration and initialization

## Frontend Architecture

```text
App
 ├── ThemeProvider
 ├── ToastProvider
 ├── AuthProvider
 ├── OnboardingProvider
 └── Routes
```

The frontend uses React Context and local component state rather than Redux/Zustand.

Route-level lazy loading is used for application pages.

## Key Architectural Principles

- Modular monolith
- Backend-authoritative authorization
- DTO-based API boundaries
- Flyway as schema authority
- AI provider abstraction
- Centralized theme state
- Server-side scoring
- Server-side payment verification
- Per-user ownership enforcement
