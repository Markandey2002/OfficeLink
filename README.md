# Office Link

Resource reservation platform for hybrid workplaces — desks, meeting rooms, and equipment — with conflict-safe booking, UTC time handling, and backend-enforced RBAC.

## Stack

| Layer | Choice |
| --- | --- |
| Frontend | React 18 (Vite), functional components + hooks |
| Backend | Spring Boot 3.3, Spring Security OAuth2 Resource Server, JPA |
| Database | PostgreSQL 16 |
| Auth | Local OIDC-shaped HMAC JWT (`iss/sub/aud/roles/realm_access`), validated on every API call; optional Keycloak OIDC SSO |

---

## Quick start — full Docker stack (recommended)

### Prerequisites

- Docker Desktop with Docker Compose v2

### 1. Create `.env` from the example

```bash
cp .env.example .env
# Edit DB_PASSWORD and JWT_SECRET with secure values before going to production.
```

### 2. Start all services

```bash
docker compose up --build
```

This starts three services:

| Service | Container | Port | Purpose |
| --- | --- | --- | --- |
| `postgres` | `office-link-postgres` | 5432 | PostgreSQL database |
| `backend` | `office-link-backend` | 8080 | Spring Boot REST API |
| `keycloak` | `office-link-keycloak` | 8081 | Optional OIDC provider |

The backend waits for postgres to be healthy before starting.

### 3. Run the frontend (dev mode)

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173`

### 4. Default credentials (seeded on first boot)

| Email | Password | Role |
| --- | --- | --- |
| `admin@officlink.com` | `Admin123!` | ADMIN |
| `user@officlink.com` | `User123!` | USER |

**Employee signup:** use **Create account** on the UI (`POST /api/auth/register`). New accounts always start as `USER`.

---

## Manual local start (without Docker)

### Prerequisites

- JDK 17+
- Node.js 18+
- PostgreSQL 14+ running locally

### 1. Database

```sql
CREATE DATABASE office_link;
```

### 2. Backend

```powershell
cd backend
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your-password"
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173`

---

## Environment variables

Copy `.env.example` → `.env`. Never commit real secrets.

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/office_link` | JDBC connection URL |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `change-me` | Database password |
| `JWT_SECRET` | (dev placeholder) | HMAC secret for local tokens (≥ 32 chars) |
| `JWT_ISSUER` | `office-link-local` | `iss` claim in issued tokens |
| `JWT_EXPIRATION_MINUTES` | `480` | Token lifetime |
| `OIDC_ENABLED` | `false` | Set `true` to activate Keycloak alongside local JWT |
| `OIDC_ISSUER_URI` | `http://localhost:8081/realms/office-link` | Keycloak issuer URL |
| `OIDC_AUDIENCE` | `office-link-api` | Expected `aud` claim for Keycloak tokens |
| `OIDC_CLIENT_ID` | `office-link-frontend` | Keycloak public client ID |
| `OIDC_REDIRECT_URI` | `http://localhost:5173` | Keycloak redirect after login |
| `CORS_ORIGINS` | `http://localhost:5173,...` | Allowed CORS origins |
| `VITE_API_BASE` | `http://localhost:8080` | Frontend API base URL |

---

## Keycloak / OIDC configuration

The `keycloak/realm-office-link.json` realm is imported automatically on first start:

- Realm: `office-link`
- Public client: `office-link-frontend` (PKCE S256, redirect to `http://localhost:5173/*`)
- Bearer-only client: `office-link-api`
- Realm roles: `USER`, `ADMIN` (default role: `USER`)

To enable Keycloak login, set `OIDC_ENABLED=true` in `.env` and restart the backend. Both the local JWT issuer and the Keycloak issuer are active simultaneously via `JwtIssuerAuthenticationManagerResolver`.

---

## API overview

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Public (creates USER) |
| `POST` | `/api/auth/login` | Public |
| `GET` | `/api/auth/oidc-config` | Public (OIDC discovery for frontend) |
| `GET` | `/api/resources?type=` | Authenticated (active resources only) |
| `GET` | `/api/bookings/mine` | Authenticated (own confirmed bookings) |
| `POST` | `/api/bookings` | Authenticated (create booking) |
| `GET` | `/api/bookings/conflicts` | Authenticated (pre-flight availability probe) |
| `DELETE` | `/api/bookings/{id}` | Owner or Admin |
| `GET` | `/api/admin/users` | Admin |
| `PUT` | `/api/admin/users/{id}/role` | Admin |
| `GET` | `/api/admin/bookings` | Admin (all confirmed) |
| `PUT` | `/api/admin/bookings/{id}` | Admin (reschedule) |
| `GET` | `/api/admin/resources` | Admin (all, including inactive) |
| `POST` | `/api/admin/resources` | Admin (create resource) |
| `PUT` | `/api/admin/resources/{id}` | Admin (update / deactivate) |

All times are ISO-8601 instants in **UTC**.

---

## Architectural decisions

- **Layered backend**: `web` → `service` → `repository` / `domain`, with DTOs at the API boundary.
- **Stateless security**: JWT Bearer tokens; Spring Security URL rules + `@PreAuthorize` method security enforce Admin vs User. Role comes from JWT claims, never from the frontend alone.
- **UTC everywhere**: JPA `hibernate.jdbc.time_zone=UTC`, Jackson UTC serialization, and `Instant` fields throughout. The UI inputs are labeled UTC.
- **Dual-issuer JWT**: `JwtIssuerAuthenticationManagerResolver` allows local HMAC tokens and Keycloak OIDC tokens to coexist. The local decoder validates issuer, expiry, **and audience** (`office-link-api`).
- **Role extraction**: `RoleClaimExtractor` supports `roles[]`, `realm_access.roles[]`, and Auth0-style `https://office-link/roles[]` claim shapes.

---

## Database setup and persistence

- Schema managed by Hibernate `ddl-auto: update` (creates tables on first run).
- PostgreSQL volume `postgres_data` persists data across `docker compose` restarts.
- To reset the database: `docker compose down -v` (destroys the volume).
- Seed data (admin + employee users, 6 resources) is inserted on first boot only via `DataSeeder`.

---

## Concurrency strategy (double-booking prevention)

Booking creation and admin reschedule run inside a short `@Transactional` boundary that:

1. **Pessimistically locks** the target `resources` row (`SELECT … FOR UPDATE` via `findByIdForUpdate`).
2. **Checks overlap** for confirmed bookings using half-open intervals `[startUtc, endUtc)`:
   `existing.start < requestedEnd AND existing.end > requestedStart`.
3. **Inserts/updates** only if no overlap; otherwise returns **HTTP 409 Conflict**.

Concurrent requests for the same resource serialize on the resource lock, so two overlapping bookings cannot both commit. Cancelled bookings are excluded from the overlap check. Entities also carry `@Version` for optimistic concurrency on updates.

The frontend performs a **pre-flight conflict probe** (`GET /api/bookings/conflicts`) before the final `POST /api/bookings` submit, giving early feedback. The backend check on final submit is the authoritative guard.

---

## JWT validation and role-based authorization

1. Every protected request must carry `Authorization: Bearer <token>`.
2. The `JwtIssuerAuthenticationManagerResolver` selects the correct decoder based on the `iss` claim.
3. **Local tokens**: decoded with HMAC-SHA key; validators check signature, expiry, issuer, and **audience** (`office-link-api`).
4. **Keycloak tokens** (when `OIDC_ENABLED=true`): decoded via JWKS endpoint; standard Spring OAuth2 validators apply.
5. `JwtAuthConverter` → `UserProvisioningService` maps the validated JWT to a `UserPrincipal` with role drawn exclusively from JWT claims (never from the HTTP request).
6. URL-level rules: `/api/admin/**` requires `ROLE_ADMIN`; all other `/api/**` require `ROLE_USER` or `ROLE_ADMIN`.
7. Method-level `@PreAuthorize` annotations provide a second enforcement layer.

---

## Test commands and results

### Unit tests (no database required)

```bash
cd backend
./mvnw.cmd test -Dtest="JwtServiceTest,RoleClaimExtractorTest,BookingWindowValidationTest" -DfailIfNoTests=false
```

**Result (last run):** Tests run: 16, Failures: 0, Errors: 0, Skipped: 0

Covers:
- JWT token issuance and claim content (4 tests)
- Role extraction from all supported claim shapes (7 tests)
- Booking window validation (valid future, end-before-start, past, adjacent slots) (5 tests)

### Integration tests (require local PostgreSQL)

```bash
cd backend
./mvnw.cmd test -Dtest="OfficeLinkApplicationTests,BookingConcurrencyTest" -DfailIfNoTests=false
```

- `OfficeLinkApplicationTests`: Spring context loads successfully.
- `BookingConcurrencyTest`: Two concurrent overlapping booking attempts for the same resource — exactly 1 succeeds, 1 returns HTTP 409. Created booking is cancelled in `@AfterEach` cleanup.

### Frontend build

```bash
cd frontend
npm run build
```

**Result:** ✓ built successfully (155.78 kB JS, 4.72 kB CSS, gzip sizes shown).

### Docker Compose validation

```bash
docker compose config --quiet   # validates YAML syntax — exits 0
docker compose up --build       # starts postgres + backend + keycloak
```

---

## Project layout

```
OfficeLinkProject/
  backend/                Spring Boot API
    Dockerfile            Multi-stage build (JDK 17 → JRE runtime)
    src/main/java/…       Controllers, services, repositories, security, DTOs, domain
    src/test/java/…       Unit tests (JWT, roles, booking rules) + concurrency integration test
  frontend/               React (Vite) UI
    src/components/       BookingForm, BookingsDashboard, ResourceCatalog, EmployeeAdmin, LoginPage
    src/auth/             AuthContext (localStorage), oidc.js (Keycloak-js)
    src/utils/time.js     UTC conversion helpers
    src/api.js            Fetch wrappers for all backend endpoints
  keycloak/
    realm-office-link.json  Auto-imported realm definition
  docker-compose.yml      postgres + backend + keycloak services
  .env.example            Safe template — no real secrets
  README.md
```

---

## AI disclosure

Large portions of this project were generated or assisted by **Cursor / Composer** and **Google Antigravity** (AI coding assistants), including project scaffolding, Spring Security/JWT wiring, concurrency booking service, React UI, Docker Compose extension, and this README. Human review should still verify security posture before any production use.
