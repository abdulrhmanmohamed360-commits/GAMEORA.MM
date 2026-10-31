# Gameora — Server-Driven Production Architecture

A fully server-driven Android (Kotlin) marketplace app for gaming accounts/items.
**The app contains zero mock/demo/fake/hard-coded data.** Every piece of content the
user sees — games, categories, products, images, sellers, reviews, orders, wallet
balance, chat, notifications — is fetched from the backend API and rendered as-is.

## Architecture

```
Android App  →  API (Retrofit/OkHttp)  →  Backend  →  External Database
                                      →  External Storage (image URLs)
```

Flow on every screen: `ApiService → Repository → ViewModel → UI`, with the UI never
holding real/hard-coded content.

### Layers
- **config/ApiConfig** — the single place the backend `BASE_URL` lives (via `BuildConfig`).
- **data/remote/api** — Retrofit `ApiService` (all endpoints) + `ApiClient` (OkHttp, interceptors, timeouts).
- **data/remote/dto** — server-facing DTOs (`@SerializedName`).
- **data/mapper** — DTO → domain model mappers (incl. paginated envelope → `Paged<T>`).
- **data/repository** — one repository per feature, wrapping `safeApi` and returning `Result<T>`.
- **data/local** — `TokenStore` (EncryptedSharedPreferences) + `SessionManager` (in-memory).
- **domain/model** — clean UI types (no serialization annotations).
- **ui/** — one MVVM screen per feature; each renders Loading / Success / Empty / Error.
- **di/AppContainer** — manual DI created once in `GameoraApp`.

## No mock data (audit)
- No `listOf(...)` catalogues of games/categories/products/users.
- No emoji or bundled images as game/product art — every image is a server URL loaded with **Coil**.
- No fake wallet balance, fake orders, fake ratings, fake chat, fake notifications.
- On failure the UI shows an Error + Retry state — never fallback mock content.

## Authentication
Real backend auth: `POST /auth/login`, `POST /auth/register`, `POST /auth/logout`,
`GET /users/me`. The bearer token is stored encrypted (Android Keystore via
`security-crypto`); passwords are never stored in plain text.

## Pagination, filters, search
Products/orders/wallet/chat/notifications use server pagination (`page`+`limit`).
Product filters (game, category, price min/max, rank, level, server, status) and
search are all sent to the server (`GET /products?...`).

## Buying & orders
`POST /orders` sends only `productId` — the **server** validates availability, re-prices,
checks the buyer/balance, and returns the created order. The app never trusts a
client-side price. Order status comes from the server.

## Configuration (IMPORTANT — before building)
Set the real backend URL in **one place only**: `app/build.gradle`
```
buildConfigField "String", "API_BASE_URL", "\"https://api.gameora.example.com/v1/\""
buildConfigField "String", "IMAGE_BASE_URL", "\"https://api.gameora.example.com/storage/\""
```
No secrets (DB passwords, private keys, admin tokens) belong in source — use
environment variables / CI secrets.

## Build
Requires a machine with JDK 17 + Android SDK (compileSdk 34):
```
./gradlew clean assembleDebug
```
```
This sandbox does not ship a JDK/Android SDK, so the Gradle build must be run in a
proper Android development environment (Android Studio or a CI image with the SDK).
The wrapper (gradlew + gradle-wrapper.jar) is included and pinned to Gradle 8.7 stable.
```

## Backend contract
See `ApiService.kt` for the full endpoint list. If the backend isn't ready yet, the
interfaces/DTOs/repositories/configuration are already in place to wire to it —
no fake API was invented.
