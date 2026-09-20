# ESN Hub Backend

ESN Hub now uses first-party ESN accounts. Google sign-in is not required.

## Account system
- Email + password registration
- Scrypt password hashing with unique salts
- Hashed security-answer recovery
- Signed, versioned 30-day sessions
- Password reset invalidates previous sessions
- Rate limits on registration, login, admin login and recovery
- Server-enforced member/admin roles

## Admin login
Set ESN_ADMIN_EMAIL to the exact email address of the owner's ESN account. That registered account is promoted server-side to the admin role. The Android app has a dedicated Admin Login button and an admin dashboard, but the server is the authority; there is no hard-coded admin password or client-side bypass.

## Required production environment
- ESN_SESSION_SECRET: a long random secret so sessions remain valid across server restarts
- ESN_ADMIN_EMAIL: the owner's ESN account email
- ESN_MEMBER_DATA_FILE: durable mounted storage path
- PORT: optional; defaults to 8080

The current JSON file store is suitable for development/single-instance testing. Replace it with a durable database before production credits, purchases, roles, or other high-value data are launched.

## Endpoints
POST /v1/auth/register
POST /v1/auth/login
POST /v1/auth/admin-login
POST /v1/auth/recovery-question
POST /v1/auth/reset-password
GET /v1/auth/session
GET /v1/admin/members
