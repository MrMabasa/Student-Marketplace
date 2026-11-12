# StudentMarketplace2 API

Node.js + TypeScript REST API for the StudentMarketplace2 Android app.

## Local setup

1. Install Node.js 20 or newer and run `npm install` in this directory.
2. Keep `.env`, `private/firebase-service-account.json`, and `certs/aiven-ca.pem` local. Do not commit them.
3. Configure `GOOGLE_APPLICATION_CREDENTIALS=./private/firebase-service-account.json` and `DB_CA_CERT_PATH=certs/aiven-ca.pem` in `.env`, alongside the Aiven connection settings.
4. Run `npm run build`, `npm run migrate`, and `npm run dev`.
5. Check `GET http://localhost:3000/health` and `GET http://localhost:3000/health/database`.

Aiven connections require TLS certificate validation (`rejectUnauthorized: true`). Do not disable it to work around connection errors.

## API routes

Listing routes are public and return the listing JSON/image shape used by the Android Retrofit client:

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/listings` | List marketplace listings. |
| `GET` | `/listings/:id` | Get listing details. |
| `GET` | `/listings/:id/image` | Return listing image bytes and MIME type. |

Purchase-request routes require `Authorization: Bearer <Firebase ID token>`:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/purchase-requests` | Submit `{ "listing_id": 123 }`. |
| `GET` | `/purchase-requests/buyer` | List the authenticated buyer's requests. |
| `GET` | `/purchase-requests/seller` | List requests for the authenticated seller. |
| `PATCH` | `/purchase-requests/:requestId/accept` | Seller accepts a pending request. |
| `PATCH` | `/purchase-requests/:requestId/decline` | Seller declines a pending request. |
| `PATCH` | `/purchase-requests/:requestId/cancel` | Buyer cancels a pending request. |

The API derives buyer identity from the verified Firebase UID and seller identity from the listing row. An unlinked Firebase account is linked only by a verified email matching exactly one existing `users.email` row. Client-supplied buyer/seller IDs are ignored. Transactions and ownership checks protect request creation and state changes.

## Android integration status

The Android data layer now has purchase-request DTOs, Retrofit methods, repository calls, and an OkHttp interceptor that attaches the current Firebase ID token. The development build permits the configured local HTTP API URL; this setting is debug-only. Screens still need to call the repository methods. Firebase accounts also need a matching internal `users` row; the current Android registration flow does not provision that row in Aiven.

## Verification checklist

The backend and Android debug builds, Aiven connectivity, token verification, UID mapping, request ownership/state transitions, and listing response shapes were checked. For a repeat run, use two buyers and two sellers with temporary test accounts and listings, then verify:

1. Missing/invalid tokens return `401`; valid tokens map to the correct `users.user_id`.
2. A numeric `listing_id` creates a request; malformed IDs return `400`; self-requests return `403`.
3. Duplicate pending requests return `409`; client-supplied buyer/seller IDs do not change the server-derived IDs.
4. Buyer and seller list routes return only their own requests.
5. Buyers cannot accept/decline; non-owner sellers cannot accept/decline; non-owner buyers cannot cancel.
6. Only pending requests transition. Confirm accepted, declined, and cancelled statuses persist in Aiven.
7. Remove all temporary users, listings, and requests after testing.

This repository does not currently include a permanent automated integration-test command. Do not share Firebase ID tokens, service-account contents, or database credentials when reporting results.