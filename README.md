# ESN Hub

Official Android hub for ES Network.

## ESN Hub 1.0

- Native ESN email/password accounts
- Security-question password recovery
- Encrypted Android session storage using Android Keystore
- Server-verified admin accounts
- Live ESN Credits balance
- Member directory for admins
- Store and official website access
- Community, invite rewards, support, announcements and events surfaces
- Session invalidation after password reset or permission changes
- Rate-limited authentication endpoints
- GitHub Actions verification for Android and backend
- Docker-ready Node.js backend

## Configuration

Android builds read `ESN_API_BASE_URL` from the GitHub Actions secret with the same name. Production deployments must also set `ESN_SESSION_SECRET`, `ESN_ADMIN_EMAIL`, and persistent `ESN_MEMBER_DATA_FILE` storage for the backend.

The production API URL should use HTTPS.

## Build

Android: `gradle :app:assembleDebug`

Backend: `cd backend && npm test`

## Release

Current app version: **1.0.0**.
