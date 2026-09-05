# Troubleshooting

## Backend does not start

Check:

- MySQL is running
- database credentials are correct
- required environment variables are set
- JWT secret meets production requirements
- Flyway migrations succeed

## Frontend shows blank screen

Check:

- `npm install`
- `npm run build`
- browser console
- API URL
- authentication initialization

## Database connection failure

Local MySQL may use port 3306.

Docker Compose exposes MySQL on the configured host port, which should be checked in `docker-compose.yml`.

## Upload failure

Check:

- PDF/DOCX type
- file size
- storage quota
- text extraction success

Scanned/image-only documents may require OCR in a future enhancement.

## AI generation failure

Check:

- Groq API configuration
- selected model
- rate limits
- backend logs
- prompt size
- AI response validation

## Authentication issues

Check:

- access token
- refresh token
- JWT secret
- token version
- OTP expiry
- account status

## Payment issues

Check:

- Razorpay configuration
- order creation
- signature verification
- webhook configuration
- payment transaction state
