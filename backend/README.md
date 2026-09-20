# ESN Hub Backend

Production-oriented authentication foundation for ESN Hub.

## Endpoints
- GET /health
- POST /v1/auth/google

The Google endpoint verifies the ID token with Google's verification library using GOOGLE_WEB_CLIENT_ID as the required audience, then creates or retrieves a stable ESN member ID.

## Environment
- GOOGLE_WEB_CLIENT_ID (required for authentication)
- PORT (optional, defaults to 8080)
- ESN_MEMBER_DATA_FILE (optional; defaults to /tmp/esn-members.json)

The included file member store is suitable for initial deployment/testing only. Before credits, purchases, staff roles, or rewards go live, replace it with a durable managed database and transactions. Never treat Android-side balances or roles as authoritative.
