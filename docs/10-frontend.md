# MindQ Frontend Documentation

## Architecture

The frontend is a React SPA.

```text
main.tsx
  ↓
App
  ↓
Providers
  ↓
Routes
  ↓
Pages
  ↓
Reusable Components
  ↓
Services / API
```

## Main Pages

- Landing
- Login
- Register
- Verify Email
- Forgot Password
- Dashboard
- Knowledge Vault
- Material Details
- AI Studio
- Quiz
- Quiz History
- Analytics
- Profile
- Settings
- Subscription
- Admin Dashboard
- Admin Users

## Services

API-facing services are separated from visual components.

Examples:

- auth
- material
- mcq
- quiz
- subscription
- payment
- analytics
- admin
- search
- storage

## State Management

React Context is used for:

- authentication
- theme
- toast notifications
- onboarding

Local component state is used for page-specific UI/form state.

## Theme

Three modes:

- Light
- Dark
- System

The preference is persisted and System follows `prefers-color-scheme`.

## Responsive Design

Desktop uses sidebar + topbar.

Mobile uses a compact topbar and drawer-style navigation.

## UI Design Principles

- premium SaaS
- clean
- spacious
- accessible
- restrained gradients
- subtle shadows
- responsive layouts
- strong visual hierarchy
