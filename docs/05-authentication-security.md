# MindQ Authentication & Security

## Authentication

MindQ supports:

- email/password login
- email OTP login
- registration email verification
- password reset
- refresh tokens
- logout
- logout from all devices

## Registration

```text
Register
 ↓
UNVERIFIED account
 ↓
Email OTP
 ↓
Verify OTP
 ↓
ACTIVE account
```

## Password Security

Passwords are stored using BCrypt.

Password policy is enforced on the backend and surfaced through frontend validation feedback.

## JWT

Access tokens are signed using HS256.

Access tokens include identity/authorization information and token-version data.

## Refresh Tokens

Refresh tokens are opaque values stored server-side.

The implementation supports:

- rotation
- revocation
- reuse detection
- device/session limits
- token-version invalidation

## Authorization

Roles include:

- `ROLE_USER`
- `ROLE_ADMIN`

Resource ownership is enforced server-side.

Foreign user resources should be masked as not-found rather than exposed.

## Security Controls

- password hashing
- OTP hashing
- OTP expiry
- OTP attempt limits
- resend cooldown
- account lockout
- JWT validation
- refresh-token rotation
- token-version invalidation
- rate limiting
- security headers
- CORS controls
- server-side payment verification
- input validation
- protected admin endpoints

## Production Requirements

Never enable development OTP bypass in production.

Never commit API keys, passwords, JWT secrets or payment secrets.

Use production environment variables/secrets.
