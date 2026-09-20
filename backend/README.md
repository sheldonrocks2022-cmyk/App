# ESN Hub Backend
Native ESN accounts use email/password plus a hashed security-question answer for recovery. Passwords and recovery answers are never stored in plaintext.

## Required production environment
- ESN_SESSION_SECRET: long random secret; required to keep sessions valid across restarts.
- ESN_ADMIN_EMAIL: the owner's ESN account email. Only this exact registered account is promoted to admin.
- ESN_MEMBER_DATA_FILE: durable mounted storage path.
- PORT: optional (8080 default).

Admin access is role-checked on the server. There is no hard-coded admin password or client-side admin bypass. Register the owner's account normally using the configured ESN_ADMIN_EMAIL; the server assigns its admin role.

Endpoints: POST /v1/auth/register, POST /v1/auth/login, POST /v1/auth/recovery-question, POST /v1/auth/reset-password, GET /v1/auth/session, GET /v1/admin/members.
