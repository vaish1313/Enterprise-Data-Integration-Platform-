# Enterprise Data Integration Platform

A production-ready full-stack enterprise application for managing data integration workflows.

## Tech Stack

| Layer     | Technology                          |
|-----------|-------------------------------------|
| Backend   | Java 17, Spring Boot 3, Spring Security, JPA |
| Frontend  | React 18, Vite, Axios, React Router |
| Database  | PostgreSQL                          |
| Auth      | JWT (Access + Refresh Tokens)       |
| Docs      | Swagger / OpenAPI 3                 |

## Project Structure

```
Enterprise-Data-Integration-Platform/
├── backend/        # Spring Boot REST API
├── frontend/       # React SPA
├── database/       # SQL schema, seed data, migrations
└── docs/           # Architecture diagrams, API docs, screenshots
```

## Modules

- **Auth** – JWT-based login, registration, token refresh
- **User Management** – CRUD, roles, permissions
- **Data Source** – Register and manage external data sources
- **Ingestion** – Pull data from sources into the platform
- **Transformation** – Apply rules to normalize/transform data
- **Synchronization** – Schedule and run sync jobs
- **Dashboard** – Metrics and KPIs
- **Audit** – Activity logs and compliance trail

## Getting Started

### Backend
```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Environment Profiles

- `application-dev.yml` – Local development
- `application-prod.yml` – Production deployment
