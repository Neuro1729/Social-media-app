# Social Media App

Java (Spring Boot) + React + PostgreSQL. Runs fully in Docker.

## Features

- User signup / login / logout
- Create & edit profile
- Upload profile picture

## Run

```bash
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Postgres: localhost:5432 (`socialmedia` / `postgres` / `postgres`)

PostgreSQL runs as its own container; the backend connects over the Docker network.
