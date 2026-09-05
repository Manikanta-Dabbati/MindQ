# MindQ API Documentation

Base URL:

```text
/api/v1
```

All protected endpoints use:

```http
Authorization: Bearer <access-token>
```

## Standard Response

```json
{
  "success": true,
  "message": "Operation completed",
  "data": {}
}
```

## Authentication

```http
POST /auth/register
POST /auth/login
POST /auth/refresh
POST /auth/logout
POST /auth/logout-all
GET  /auth/me
PUT  /auth/me
DELETE /auth/me
POST /auth/forgot-password
POST /auth/verify-reset-otp
POST /auth/reset-password
POST /auth/change-password
POST /auth/verify-email-otp
POST /auth/request-login-otp
POST /auth/verify-login-otp
POST /auth/resend-otp
```

## Materials

```http
POST   /materials
POST   /materials/upload
GET    /materials
GET    /materials/{id}
PUT    /materials/{id}
DELETE /materials/{id}
GET    /materials/storage
```

## AI

```http
GET  /ai/models
POST /ai/tools/summarize
POST /ai/tools/flashcards
POST /ai/tools/revision-notes
```

## MCQ / Quiz

```http
POST /mcq/generate
GET  /mcq/{id}
POST /mcq/{id}/submit
GET  /mcq/history
GET  /mcq/attempt/{attemptId}/answers
GET  /mcq/{id}/download
POST /mcq/{id}/save-to-vault
```

## Analytics

```http
GET /analytics/overview
GET /analytics/topics
```

## Subscription

```http
GET /subscription/plans
GET /subscription/current
```

## Payments

```http
POST /payment/checkout
GET  /payment/history
POST /payment/verify
POST /payment/test-confirm
```

## Webhooks

```http
POST /webhooks/razorpay
```

## Admin

```http
GET    /admin/dashboard
GET    /admin/users
PUT    /admin/users/{id}/status
DELETE /admin/users/{id}
PUT    /admin/users/{id}/role
```

## Health

```http
GET /health
GET /health/live
GET /health/ready
GET /metrics
```

## Search

```http
GET /search?q=
```

The exact current DTO fields and validation rules should always be taken from the current controller and DTO implementations.
