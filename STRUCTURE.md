# Project Structure

```
project/
│
├── docker-compose.yml          # Runs db + backend + frontend together
├── README.md
│
├── backend/                    # Java (Spring Boot) — port 8080
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/socialmedia/
│       ├── SocialMediaApp.java
│       │
│       ├── model/              # Data objects
│       │   └── User.java
│       │
│       ├── api/                # Receives frontend requests
│       │   ├── AuthApi.java        # /api/auth/signup|login|logout
│       │   └── ProfileApi.java     # /api/profile, /picture
│       │
│       ├── logic/              # Business rules
│       │   ├── AuthLogic.java
│       │   └── ProfileLogic.java
│       │
│       ├── data/               # Talks to PostgreSQL
│       │   └── UserData.java
│       │
│       ├── upload/             # Saves uploaded images
│       │   └── ImageUpload.java
│       │
│       └── config/             # Connection / security settings
│           ├── DatabaseConfig.java
│           ├── SecurityConfig.java
│           ├── JwtService.java
│           └── JwtAuthFilter.java
│
└── frontend/                   # React (Vite) — port 3000 (Docker) / 5173 (local dev)
    ├── Dockerfile
    ├── nginx.conf              # Proxies /api and /uploads → backend
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── App.jsx
        ├── main.jsx
        │
        ├── pages/              # Screens
        │   ├── SignupPage.jsx
        │   ├── LoginPage.jsx
        │   ├── ProfilePage.jsx
        │   └── EditProfilePage.jsx
        │
        ├── components/         # Reusable UI
        │   ├── Navbar.jsx
        │   ├── ProfileCard.jsx
        │   └── UploadButton.jsx
        │
        ├── api/                # Backend communication
        │   ├── client.js
        │   ├── AuthApi.js
        │   └── ProfileApi.js
        │
        └── assets/             # Styles / media
            └── styles.css
```

PostgreSQL is **not** inside these folders. It runs as its own Docker service; the backend connects to it.

---

## Ports

| Service    | Port  | URL / note                          |
|------------|-------|-------------------------------------|
| Frontend   | 3000  | http://localhost:3000               |
| Backend    | 8080  | http://localhost:8080               |
| PostgreSQL | 5432  | `db:5432` inside Docker network     |

In the browser, the frontend calls `/api/...` and `/uploads/...`. Nginx on port 3000 forwards those to the backend on 8080.

---

## System level (short)

1. **Frontend** — React screens call `AuthApi` / `ProfileApi`.
2. **Backend API** — `AuthApi` / `ProfileApi` receive HTTP requests.
3. **Logic** — `AuthLogic` / `ProfileLogic` apply rules (signup, login, profile, picture).
4. **Data** — `UserData` reads/writes the `users` table in PostgreSQL.
5. **Upload** — `ImageUpload` stores profile pictures on disk; served at `/uploads/...`.
6. **Auth** — JWT issued on signup/login; required for profile routes.
7. **DB** — Postgres container holds user data; backend connects via JDBC (`SPRING_DATASOURCE_*`).

```
Browser :3000  →  Frontend (Nginx/React)
                      │
                      ▼ /api , /uploads
                 Backend :8080
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
       Logic       Upload      UserData
          │                       │
          └───────────────────────┼──▶ PostgreSQL :5432
```

Start everything:

```bash
docker compose up --build
```
