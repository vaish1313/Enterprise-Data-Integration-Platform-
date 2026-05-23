# Role-Based Access Control (RBAC) Matrix

**Project:** Enterprise Data Integration Platform  
**Last Updated:** 2026-05-23  
**Enforcement Layer:** Spring Security `@PreAuthorize` (method-level) + `SecurityFilterChain` (URL-level)

---

## Roles

| Role | Description |
|------|-------------|
| `ADMIN` | Full platform access. Can manage users, delete data, and perform all operations. |
| `ANALYST` | Read and export access. Can view all data and export reports but cannot delete or manage users. |
| `OPERATOR` | Operational access. Can ingest data, trigger syncs, and view logs but cannot export or delete. |

---

## Audit Module — `/api/v1/audit/**`

| Endpoint | Method | ADMIN | ANALYST | OPERATOR | Enforcement |
|----------|--------|:-----:|:-------:|:--------:|-------------|
| View all audit logs | `GET /api/v1/audit` | ✅ | ✅ | ✅ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST','OPERATOR')")` |
| Filter by username | `GET /api/v1/audit/user/{username}` | ✅ | ✅ | ✅ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST','OPERATOR')")` |
| Filter by action | `GET /api/v1/audit/action/{action}` | ✅ | ✅ | ✅ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST','OPERATOR')")` |
| Filter by date range | `GET /api/v1/audit/range` | ✅ | ✅ | ✅ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST','OPERATOR')")` |
| Export all logs (CSV) | `GET /api/v1/audit/export/csv` | ✅ | ✅ | ❌ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")` |
| Export range (CSV) | `GET /api/v1/audit/export/csv/range` | ✅ | ✅ | ❌ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")` |
| Delete single log entry | `DELETE /api/v1/audit/{id}` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |
| Purge logs older than N days | `DELETE /api/v1/audit/purge` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |

---

## User Module — `/api/v1/users/**`

| Endpoint | Method | ADMIN | ANALYST | OPERATOR | Enforcement |
|----------|--------|:-----:|:-------:|:--------:|-------------|
| Create user | `POST /api/v1/users` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |
| Get user by ID | `GET /api/v1/users/{id}` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |
| Get all users | `GET /api/v1/users` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |
| Update user | `PUT /api/v1/users/{id}` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |
| Delete user | `DELETE /api/v1/users/{id}` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |
| Get current user | `GET /api/v1/users/me` | ✅ | ✅ | ✅ | Authenticated only |

---

## Data Source Module — `/api/v1/data-sources/**`

| Endpoint | Method | ADMIN | ANALYST | OPERATOR | Enforcement |
|----------|--------|:-----:|:-------:|:--------:|-------------|
| Create data source | `POST /api/v1/data-sources` | ✅ | ✅ | ❌ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")` |
| Get data source | `GET /api/v1/data-sources/{id}` | ✅ | ✅ | ✅ | Authenticated only |
| Get all data sources | `GET /api/v1/data-sources` | ✅ | ✅ | ✅ | Authenticated only |
| Get active sources | `GET /api/v1/data-sources/active` | ✅ | ✅ | ✅ | Authenticated only |
| Update data source | `PUT /api/v1/data-sources/{id}` | ✅ | ✅ | ❌ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")` |
| Delete data source | `DELETE /api/v1/data-sources/{id}` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |

---

## Ingestion Module — `/api/v1/ingestion/**`

| Endpoint | Method | ADMIN | ANALYST | OPERATOR | Enforcement |
|----------|--------|:-----:|:-------:|:--------:|-------------|
| Ingest CSV | `POST /api/v1/ingestion/csv/{id}` | ✅ | ✅ | ✅ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST','OPERATOR')")` |
| Ingest from API | `POST /api/v1/ingestion/api/{id}` | ✅ | ✅ | ✅ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST','OPERATOR')")` |
| Get job status | `GET /api/v1/ingestion/jobs/{id}` | ✅ | ✅ | ✅ | Authenticated only |
| Get jobs by source | `GET /api/v1/ingestion/jobs/source/{id}` | ✅ | ✅ | ✅ | Authenticated only |

---

## Transformation Module — `/api/v1/transformations/**`

| Endpoint | Method | ADMIN | ANALYST | OPERATOR | Enforcement |
|----------|--------|:-----:|:-------:|:--------:|-------------|
| Create rule | `POST /api/v1/transformations` | ✅ | ✅ | ❌ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")` |
| Get all rules | `GET /api/v1/transformations` | ✅ | ✅ | ✅ | Authenticated only |
| Get rule by ID | `GET /api/v1/transformations/{id}` | ✅ | ✅ | ✅ | Authenticated only |
| Update rule | `PUT /api/v1/transformations/{id}` | ✅ | ✅ | ❌ | `@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")` |
| Delete rule | `DELETE /api/v1/transformations/{id}` | ✅ | ❌ | ❌ | `@PreAuthorize("hasRole('ADMIN')")` |

---

## Synchronization Module — `/api/v1/sync/**`

| Endpoint | Method | ADMIN | ANALYST | OPERATOR | Enforcement |
|----------|--------|:-----:|:-------:|:--------:|-------------|
| Trigger manual sync | `POST /api/v1/sync/trigger/{id}` | ✅ | ❌ | ✅ | `@PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")` |
| Get job by ID | `GET /api/v1/sync/jobs/{id}` | ✅ | ✅ | ✅ | Authenticated only |
| Get recent jobs | `GET /api/v1/sync/jobs/recent` | ✅ | ✅ | ✅ | Authenticated only |
| Get jobs by source | `GET /api/v1/sync/jobs/source/{id}` | ✅ | ✅ | ✅ | Authenticated only |

---

## Dashboard Module — `/api/v1/dashboard/**`

| Endpoint | Method | ADMIN | ANALYST | OPERATOR | Enforcement |
|----------|--------|:-----:|:-------:|:--------:|-------------|
| Get summary | `GET /api/v1/dashboard/summary` | ✅ | ✅ | ❌ | `SecurityConfig` URL matcher + `@PreAuthorize` |

---

## Auth Module — `/api/v1/auth/**`

| Endpoint | Method | ADMIN | ANALYST | OPERATOR | Enforcement |
|----------|--------|:-----:|:-------:|:--------:|-------------|
| Register | `POST /api/v1/auth/register` | 🌐 | 🌐 | 🌐 | Public |
| Login | `POST /api/v1/auth/login` | 🌐 | 🌐 | 🌐 | Public |
| Refresh token | `POST /api/v1/auth/refresh` | 🌐 | 🌐 | 🌐 | Public |
| Logout | `POST /api/v1/auth/logout` | ✅ | ✅ | ✅ | Authenticated only |

---

## Authorization Architecture

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────┐
│  JwtAuthenticationFilter                    │
│  Validates JWT, sets SecurityContext        │
└─────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────┐
│  SecurityFilterChain (SecurityConfig)       │
│  URL-level rules:                           │
│  • /api/v1/auth/**     → permitAll          │
│  • /api/v1/users/**    → ADMIN only         │
│  • /api/v1/dashboard/** → ADMIN, ANALYST    │
│  • /api/v1/audit/**    → authenticated      │
│  • anyRequest          → authenticated      │
└─────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────┐
│  @PreAuthorize (Method Security)            │
│  Fine-grained per-endpoint role checks      │
│  Enforced by Spring AOP proxy               │
│  Requires @EnableMethodSecurity             │
└─────────────────────────────────────────────┘
     │
     ▼
  Controller Method Executes
```

### Design Principles

1. **No class-level `@PreAuthorize`** — every endpoint's access policy is explicit and independently adjustable.
2. **Two-layer defence** — `SecurityFilterChain` blocks unauthenticated requests before they reach controllers; `@PreAuthorize` enforces role checks.
3. **Least privilege** — each role has only the permissions it needs. OPERATOR cannot export or delete. ANALYST cannot delete or manage users.
4. **Audit trail protection** — DELETE operations on audit logs are ADMIN-only to preserve forensic integrity.
5. **No redundant rules** — `SecurityConfig` URL matchers and `@PreAuthorize` annotations do not duplicate each other's role checks.
