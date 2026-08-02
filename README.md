# Auth Module

## Run
```bash
docker compose up --build -d
```

Open http://localhost:3000

## Stack
- Backend: Java 21, Spring Boot 3, Security, JPA, Flyway, Redis sessions, JWT
- Frontend: React + Vite
- Infra: Postgres (`authdb`) + Redis (compose)
