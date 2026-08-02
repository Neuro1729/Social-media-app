# Canopy Social Media Platform

Full-stack social app for accounts, profiles, follows, and posts.

**Live demo:** [https://social-media-app-90z.pages.dev/](https://social-media-app-90z.pages.dev/)

## Run locally with Docker

**Requirements:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or Docker Engine + Compose v2).

1. Clone the repo and open a terminal in the project root (where `docker-compose.yml` is).
2. Start everything (Postgres, Redis, backend, frontend):

```bash
docker compose up --build -d
```

3. Wait until containers are healthy, then open the site:

| Service  | URL |
|----------|-----|
| Website  | http://localhost:3000 |
| API      | http://localhost:8080 |
| Health   | http://localhost:8080/api/health |

4. Useful commands:

```bash
# Follow logs
docker compose logs -f

# Stop (keeps data)
docker compose down

# Stop and remove the database volume
docker compose down -v
```

First build can take a few minutes. Later starts reuse images and the Postgres volume.

## Configuration groups

Runtime settings are environment-driven. See `backend/.env.example` and `frontend/.env.example`.

### PostgreSQL

| Variable | Purpose | Local default |
|----------|---------|---------------|
| `DB_HOST` | Database host | `localhost` / Compose: `db` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `socialmedia` |
| `DB_USER` | Username | `postgres` |
| `DB_PASSWORD` | Password | `postgres` |
| `DB_SSL_MODE` | JDBC `sslmode` | `disable` (use `require` for hosted Postgres) |

### Redis

| Variable | Purpose | Local default |
|----------|---------|---------------|
| `REDIS_HOST` | Redis host | `localhost` / Compose: `redis` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_USERNAME` | ACL username (optional) | empty |
| `REDIS_PASSWORD` | Password (optional) | empty |
| `REDIS_SSL` | Enable TLS | `false` |

### CORS / frontend origins

| Variable | Purpose |
|----------|---------|
| `FRONTEND_ORIGINS` | Comma-separated exact origins allowed by CORS (credentials enabled) |

Local Compose sets:

```text
FRONTEND_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
```

### Refresh cookie

| Variable | Purpose | Local default |
|----------|---------|---------------|
| `REFRESH_COOKIE_SECURE` | `Secure` flag | `false` |
| `REFRESH_COOKIE_SAME_SITE` | `SameSite` | `Lax` |

For cross-site HTTPS deployments, typically `REFRESH_COOKIE_SECURE=true` and `REFRESH_COOKIE_SAME_SITE=None`.

### Frontend API origin

| Variable | Purpose |
|----------|---------|
| `VITE_API_ORIGIN` | Optional API origin. Empty → same-origin `/api`. Set → `${VITE_API_ORIGIN}/api` |

### Like synchronization

| Variable | Purpose | Local default |
|----------|---------|---------------|
| `LIKE_SYNC_ENABLED` | Background Redis→Postgres like sync | `true` |
| `LIKE_SYNC_INTERVAL_MS` | Scheduler interval | `5000` |
| `LIKE_SYNC_BATCH_SIZE` | Posts per sync batch | `50` |

Also set `PORT` (default `8080`) and `JWT_SECRET` for the backend.

## Capabilities

### Authentication & account
- Register and login with email, phone, or username
- JWT access tokens (short-lived) + refresh tokens in HTTP-only cookies
- Redis-backed sessions with device listing and per-session or bulk revoke
- Password reset via time-limited tokens
- Change or remove username (with reservation to prevent reuse races)

### Social graph
- Public profiles: bio, picture URL, follower/following counts
- Private accounts: follow requests, approve/reject
- Follow / unfollow and remove followers
- Block / unblock with mutual visibility cut-off
- Profile search by username
- Cursor-based pagination for connections, requests, and blocked lists

### Posts
- Create and soft-delete text posts
- Profile post feeds (respects privacy and blocks)
- Like / unlike with Redis hot path and background Postgres sync
- Comments create / delete
- Cursor pagination for posts, likes, and comments
- Interaction events for likes, comments, views, and follows

### Privacy model
- Private profiles hide full content from non-followers
- Blocks apply in both directions (profile, posts, follow)
- Post visibility follows the same rules as full profile access

## Frontend

React + Vite SPA with protected routes for:

| Area | Pages |
|------|--------|
| Auth | Login, register, forgot/reset password |
| Account | Profile summary, active devices/sessions |
| Social | Profile, edit profile, followers/following, follow requests, blocked users |
| Posts | Post detail with like and comments |

## Stack

| Layer | Tech |
|-------|------|
| Backend | Java 21, Spring Boot 3, Security, JPA, Flyway, Validation |
| Auth | JWT (access) + Redis sessions (refresh) |
| Data | PostgreSQL 16, Redis 7 |
| Frontend | React 19, Vite 6, React Router, Axios |
| Deploy | Docker Compose (Postgres, Redis, API, nginx frontend) |

## Project layout

```
backend/     Spring Boot API (auth, social, posts)
frontend/    React SPA
docker-compose.yml
```
