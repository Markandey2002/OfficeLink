# Office Link

Resource reservation platform for hybrid workplaces — desks, meeting rooms, and equipment — with conflict-safe booking, UTC time handling, and backend-enforced RBAC.

## Stack

| Layer | Choice |
| --- | --- |
| Frontend | React 18 (Vite), functional components + hooks |
| Backend | Spring Boot 3.3, Spring Security, JPA |
| Database | PostgreSQL |
| Auth | JWT with OIDC-style claims (`sub`, `email`, `roles`) validated on every API call |

## Quick start

### Prerequisites

- JDK 17+
- Node.js 18+
- PostgreSQL running locally

### 1. Database

```sql
CREATE DATABASE office_link;
```

Credentials used by default (override via env):

- `DB_USERNAME=postgres`
- `DB_PASSWORD=5189`
- `DB_URL=jdbc:postgresql://localhost:5432/office_link`

### 2. Backend

```bash
cd backend
# Windows PowerShell example
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.4.101-hotspot"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="5189"
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080`

**Employee signup:** use **Create account** on the UI (`POST /api/auth/register`). New accounts are always `USER` (employee). An admin can promote them under **Employees**.

Bootstrap admin (seeded on first boot only):

| Email | Password | Role |
| --- | --- | --- |
| `admin@officlink.com` | `Admin123!` | ADMIN |

Optional seed employee: `user@officlink.com` / `User123!`

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173`

Copy `.env.example` → `.env` if you need non-default values. **Do not commit real secrets.**

## API overview

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Public (creates USER) |
| `POST` | `/api/auth/login` | Public |
| `GET` | `/api/admin/users` | Admin |
| `PUT` | `/api/admin/users/{id}/role` | Admin |
| `GET` | `/api/resources?type=` | Authenticated |
| `POST` | `/api/bookings` | Authenticated |
| `GET` | `/api/bookings/mine` | Authenticated (own) |
| `DELETE` | `/api/bookings/{id}` | Owner or Admin |
| `GET` | `/api/admin/bookings` | Admin (JWT `ROLE_ADMIN`) |
| `PUT` | `/api/admin/bookings/{id}` | Admin |

All times are ISO-8601 instants in **UTC**.

## Architectural decisions

- **Layered backend**: `web` → `service` → `repository` / `domain`, with DTOs at the API boundary.
- **Stateless security**: JWT Bearer tokens; Spring Security method/URL rules enforce Admin vs User. Role comes from JWT claims, never from the frontend alone.
- **UTC everywhere**: JPA/`hibernate.jdbc.time_zone=UTC`, Jackson UTC, and `Instant` fields. The UI labels inputs as UTC to avoid local-offset surprises.
- **Local JWT issuer for the assessment**: Tokens carry OIDC-like claims so the validation path matches a resource-server model. For production Auth0/Keycloak, replace `JwtService` / filter with Spring OAuth2 Resource Server JWKS validation against the IdP issuer.

## Concurrency strategy (double-booking prevention)

Booking creation and admin reschedule run inside a short `@Transactional` boundary that:

1. **Pessimistically locks** the target `resources` row (`SELECT … FOR UPDATE` via `findByIdForUpdate`).
2. **Checks overlap** for confirmed bookings using half-open intervals `[startUtc, endUtc)`:
   `existing.start < requestedEnd AND existing.end > requestedStart`.
3. **Inserts/updates** only if no overlap; otherwise returns **HTTP 409 Conflict**.

Concurrent requests for the same resource serialize on the resource lock, so two overlapping bookings cannot both commit. Cancelled bookings are ignored in the overlap check. Entities also carry `@Version` for optimistic concurrency on updates.

## AI disclosure

Large portions of this project were generated or assisted by **Cursor / Composer** (AI coding assistant), including project scaffolding, Spring Security/JWT wiring, concurrency booking service, React UI, and this README. Human review should still verify security posture before any production use.

## Project layout

```
OfficeLinkProject/
  backend/          Spring Boot API
  frontend/         React (Vite) UI
  .env.example      Sample env vars (no secrets)
  README.md
```
