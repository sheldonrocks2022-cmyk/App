# ESN Hub authentication contract

ESN Hub uses Google Credential Manager for user authentication and can link the verified Google identity to the ESN backend.

## Required build configuration

Configure these as GitHub Actions repository secrets:

- `GOOGLE_WEB_CLIENT_ID` — Google OAuth 2.0 Web client ID used as the server client ID.
- `ESN_API_BASE_URL` — HTTPS base URL for the trusted ESN backend.

The app remains buildable when either value is absent. Google login displays a setup message if the client ID is missing. If the backend URL is absent, Google identity can be established locally but no ESN member ID is claimed.

## Backend endpoint

`POST /v1/auth/google`

Request:

```json
{"idToken":"<Google ID token>"}
```

The server MUST validate the Google ID token, including signature, issuer, audience, and expiry, before creating or linking an ESN member. The server, not the Android client, is the authority for credits, purchases, roles, invite rewards, and other protected ESN data.

Successful response:

```json
{"memberId":"<stable ESN member ID>"}
```

Never accept a Google user ID or email sent by the client as proof of identity. Derive identity from the verified token.
