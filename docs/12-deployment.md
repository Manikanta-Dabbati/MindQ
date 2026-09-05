# MindQ Deployment Guide

## Current Container Topology

```text
Frontend nginx
      │
      ▼
Spring Boot Backend
      │
      ├── MySQL
      ├── Mail service
      ├── Groq
      └── Razorpay
```

## Docker

Build:

```bash
docker compose up -d --build
```

The compose stack includes the application services and MySQL.

## Production Configuration

Use:

- production environment variables
- strong JWT secret
- real AI credentials kept outside source control
- Razorpay production/sandbox credentials as appropriate
- production CORS
- HTTPS
- database backups
- secure storage
- monitoring

## Flyway

Flyway manages production database schema changes.

Do not rely on `ddl-auto=update` in production.

## CI

GitHub Actions validates:

- backend tests
- frontend tests/build
- Docker builds
- application health

A production deployment job can be added separately according to the chosen hosting platform.

## Secret Management

Never commit:

- `.env`
- database passwords
- JWT secrets
- Groq keys
- Razorpay secrets
- email credentials

Use the deployment platform's secret/environment configuration.
