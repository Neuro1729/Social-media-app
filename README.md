# Auth Module

Full-stack social app built around a reusable authentication core: accounts, profiles, follows, and posts.

## Quick start

```bash
docker compose up --build -d
```

Open http://localhost:3000 (API on http://localhost:8080).

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
