# Dynamic RBAC Authorization Engine

A Spring Boot backend that implements **database-driven Role-Based Access Control (RBAC)**.
Every authorization decision is evaluated live against H2 database rows through a custom
`PermissionEvaluator` — there is **no** `hasRole(...)`, `hasAuthority(...)`, or `if (role.equals(...))`
anywhere in the codebase.

No UI is included. Everything is designed to be tested with **Postman**.

---

## Project Overview

The engine models the classic chain:

```
USER → ROLE → PERMISSION
```

A user is assigned one or more roles. A role is assigned one or more permissions.
`@PreAuthorize("hasPermission(null, 'SOME_PERMISSION')")` on a controller method triggers
`CustomPermissionEvaluator`, which walks that chain in the database, at that exact moment,
to decide `ALLOW` or `DENY`. Change the database and the very next request is affected —
no restart, no redeploy, no code change.

---

## Features

- HTTP Basic authentication backed by users stored in H2 (BCrypt-hashed passwords)
- Dynamic, database-driven permission checks via a custom `PermissionEvaluator`
- Method-level security (`@PreAuthorize`) on every protected endpoint
- Management APIs to create roles, create permissions, assign permissions to roles, and assign roles to users
- A protected demo endpoint (`/secure-data`) that proves the dynamic behavior
- Centralized exception handling with proper HTTP status codes
- Seed data + a bootstrap admin so the whole flow is testable immediately after startup
- Unit + integration tests (JUnit 5, Mockito, MockMvc) with Jacoco coverage reporting

---

## Technology Stack

| Layer | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security (HTTP Basic, Method Security) |
| Persistence | Spring Data JPA |
| Database | H2 (file-based, persists across restarts) |
| Build | Maven |
| Testing | JUnit 5, Mockito, MockMvc, Spring Boot Test |
| Coverage | Jacoco |

---

## Database Design

| Table | Columns |
|---|---|
| `app_user` | id, username, password (BCrypt hash) |
| `role` | id, name |
| `permission` | id, name |
| `user_role` | id, user_id → app_user, role_id → role |
| `role_permission` | id, role_id → role, permission_id → permission |

`user_role` and `role_permission` are classic many-to-many join tables, modeled as their own
JPA entities (rather than `@ManyToMany`) so they can be created, queried, and deleted
independently — which is exactly what "assign" and "remove" operations need.

---

## Entity Relationships

```
User 1───* UserRole *───1 Role 1───* RolePermission *───1 Permission
```

- One `User` can have many `UserRole` rows → many `Role`s.
- One `Role` can have many `RolePermission` rows → many `Permission`s.
- A user's effective permissions = the union of permissions across all of their roles.

---

## Authorization Flow

```
Postman
   │
   ▼
GET /secure-data
   │
   ▼
Spring Security Filter Chain (HTTP Basic authenticates the user)
   │
   ▼
@PreAuthorize("hasPermission(null, 'READ_SECURE_DATA')")
   │
   ▼
hasPermission(...) expression
   │
   ▼
CustomPermissionEvaluator.hasPermission(Authentication, null, "READ_SECURE_DATA")
   │
   ▼
1. Read username from Authentication
2. Load User from DB
3. Load all UserRole rows for that User → Roles
4. For each Role, load all RolePermission rows → Permissions
5. Is "READ_SECURE_DATA" among them?
   │
   ├── YES → 200 OK (method executes)
   └── NO  → 403 Forbidden (AccessDeniedException, method never runs)
```

Authentication (proving *who* you are) and authorization (deciding *what you can do*) are
deliberately kept separate: `CustomUserDetailsService` loads a user with **zero** granted
authorities. All the interesting decisions happen later, per-request, in
`CustomPermissionEvaluator`.

---

## Custom PermissionEvaluator

`com.example.rbac.security.CustomPermissionEvaluator` implements Spring Security's
`PermissionEvaluator` interface and is registered with `DefaultMethodSecurityExpressionHandler`
in `SecurityConfig`. It backs every `hasPermission(...)` expression used in `@PreAuthorize`
across the project. See the class Javadoc for the exact step-by-step lookup logic.

---

## Dynamic Permission Evaluation — Why It Matters

Because `CustomPermissionEvaluator` queries the database on every single request instead of
relying on authorities baked into the login session, permission changes take effect
**immediately**, for an **already logged-in** user, with **zero Java code changes**. This is
proven end-to-end in `SecureDataDynamicAuthorizationIntegrationTest` and can be reproduced
manually in Postman (see "Dynamic Authorization Test" below).

---

## Why Hardcoded Roles Are Avoided

`hasRole("ADMIN")` or `if (role.equals("ADMIN"))` bake authorization rules into compiled Java
code. Adding a new role, renaming a permission, or changing who can call an endpoint would
require a code change, a rebuild, and a redeploy. By storing roles/permissions/mappings in H2
and evaluating them through `CustomPermissionEvaluator`, the same outcome is achieved purely
through data changes — which is the entire point of a *dynamic* RBAC engine.

---

## API Documentation

All endpoints require HTTP Basic authentication (except none — even `/secure-data` requires
login). Management endpoints additionally require a specific permission, checked dynamically.

| Method | Path | Required permission | Description |
|---|---|---|---|
| POST | `/users` | `CREATE_USER` | Create a user |
| POST | `/roles` | `CREATE_ROLE` | Create a role |
| POST | `/permissions` | `CREATE_PERMISSION` | Create a permission |
| POST | `/roles/{roleId}/permissions/{permissionId}` | `ASSIGN_PERMISSION` | Attach a permission to a role |
| POST | `/users/{userId}/roles/{roleId}` | `ASSIGN_ROLE` | Attach a role to a user |
| POST | `/users/{userId}/permissions/{permissionId}` | `ASSIGN_PERMISSION` | Attach a permission directly to a user |
| GET | `/secure-data` | `READ_SECURE_DATA` | Demo protected resource |

### POST /users
Request:
```json
{ "username": "johndoe", "password": "password123" }
```
Response `201 Created`:
```json
{ "id": 3, "username": "johndoe" }
```

### POST /roles
Request:
```json
{ "name": "USER" }
```
Response `201 Created`:
```json
{ "id": 1, "name": "USER" }
```

### POST /permissions
Request:
```json
{ "name": "READ_SECURE_DATA" }
```
Response `201 Created`:
```json
{ "id": 1, "name": "READ_SECURE_DATA" }
```

### POST /roles/{roleId}/permissions/{permissionId}
No body. Response `200 OK`:
```json
{ "message": "Permission 1 assigned to role 2 successfully" }
```

### POST /users/{userId}/roles/{roleId}
No body. Response `200 OK`:
```json
{ "message": "Role 2 assigned to user 1 successfully" }
```

### POST /users/{userId}/permissions/{permissionId}
No body. Response `200 OK`:
```json
{ "message": "Permission 1 assigned to user 2 successfully" }
```

### GET /secure-data
Response `200 OK`:
```json
{ "message": "Hello ram, you have access to secure data!" }
```
Response `403 Forbidden`:
```json
{
  "timestamp": "2026-09-02T10:15:30",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to perform this action",
  "path": "/secure-data"
}
```

### Error status codes used throughout
`200 OK` · `201 CREATED` · `400 BAD REQUEST` · `401 UNAUTHORIZED` · `403 FORBIDDEN` ·
`404 NOT FOUND` · `409 CONFLICT` · `500 INTERNAL SERVER ERROR`

---

## Authentication Instructions

Seeded users (created automatically on first startup by `DataInitializer`):

| Username | Password | Seeded role | Notes |
|---|---|---|---|
| `amit` | `amit123` | `ADMIN` | Has every permission — use this to bootstrap/manage RBAC data |
| `ram` | `ram123` | *(none)* | Assign the `USER` role to Ram yourself, as part of the demo |

In Postman: **Authorization tab → Type: Basic Auth → Username / Password** as above.
Passwords are never stored in plain text — they are BCrypt-hashed before being saved.

---

## Postman Testing Steps

Follow this exact order. The project already seeds `ADMIN`/`USER` roles,
`CREATE_ROLE`/`CREATE_PERMISSION`/`CREATE_USER`/`ASSIGN_ROLE`/`ASSIGN_PERMISSION`/`READ_SECURE_DATA`
permissions, and the two users described above — so you can start testing immediately.

1. **Start the application** (see "Running the Application" below).
2. **Log in as `amit` / `amit123`** (Basic Auth) for every request below unless stated otherwise — Amit is the bootstrap admin.
3. **Confirm `/secure-data` is blocked for Ram right now:**
   `GET /secure-data` with Basic Auth `ram` / `ram123` → expect `403 Forbidden` (Ram has no role yet).
4. **Assign the existing `USER` role to Ram.** First find Ram's `userId` and the `USER` role's `roleId`
   (e.g. via the H2 console, see below — Ram is typically `id=2`, `USER` role is typically `id=2`):
   `POST /users/{ramUserId}/roles/{userRoleId}` (as `amit`) → expect `200 OK`.
5. **Call `/secure-data` as Ram again:**
   `GET /secure-data` with Basic Auth `ram` / `ram123` → expect `200 OK` — proves `Ram → USER → READ_SECURE_DATA`.
6. **(Optional) Create your own role/permission from scratch** to exercise the full flow:
   - `POST /roles` as `amit`, body `{"name":"MANAGER"}` → `201 Created`
   - `POST /permissions` as `amit`, body `{"name":"APPROVE_REQUEST"}` → `201 Created`
   - `POST /roles/{managerRoleId}/permissions/{approveRequestPermId}` as `amit` → `200 OK`
   - `POST /users/{ramUserId}/roles/{managerRoleId}` as `amit` → `200 OK`

### Dynamic Authorization Test (the core demonstration)

7. **Remove `READ_SECURE_DATA` from the `USER` role directly in the database**, using the
   H2 console at `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/rbacdb`,
   user `sa`, empty password):
   ```sql
   DELETE FROM role_permission
   WHERE role_id = (SELECT id FROM role WHERE name = 'USER')
     AND permission_id = (SELECT id FROM permission WHERE name = 'READ_SECURE_DATA');
   ```
8. **Call `/secure-data` as Ram again (no restart, no code change):**
   `GET /secure-data` with Basic Auth `ram` / `ram123` → expect `403 Forbidden`.
9. **Add the permission back:**
   ```sql
   INSERT INTO role_permission (role_id, permission_id)
   VALUES ((SELECT id FROM role WHERE name = 'USER'), (SELECT id FROM permission WHERE name = 'READ_SECURE_DATA'));
   ```
10. **Call `/secure-data` as Ram one more time** → expect `200 OK` again.

This proves: **Authorization = database configuration, not hardcoded Java logic.**

---

## Running the Application

Requirements: JDK 17+, Maven 3.9+ (or use the included `mvnw` wrapper if you add one).

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. The H2 file database is created at `./data/rbacdb.mv.db`
on first run and reused on every subsequent run, so your role/permission/user changes persist
across restarts.

H2 console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/rbacdb`
- User: `sa`
- Password: *(empty)*

---

## Running Tests

```bash
mvn test
```

This runs all unit tests (Mockito, on `RoleService`, `UserRoleService`, `CustomPermissionEvaluator`)
and integration tests (MockMvc + a real in-memory H2 database via the `test` profile), including
the dynamic-authorization proof described above.

## Test Coverage

Jacoco is wired into the build. After running tests, open:

```
target/site/jacoco/index.html
```

to see the HTML coverage report. Run tests with coverage explicitly:

```bash
mvn clean test jacoco:report
```

---

## GitHub Instructions

```bash
git init
git add .
git commit -m "Dynamic RBAC Authorization Engine - initial implementation"
git branch -M main
git remote add origin <your-repo-url>
git push -u origin main
```

`.gitignore` already excludes `target/`, the local H2 data files, and IDE folders.

---

## Project Structure

```
src/main/java/com/example/rbac
├── RbacApplication.java
├── config
│   ├── SecurityConfig.java        # HTTP Basic, method security, evaluator wiring
│   └── DataInitializer.java       # Seeds roles/permissions/users on startup
├── controller
│   ├── UserController.java        # POST /users
│   ├── UserPermissionController.java # POST /users/{id}/permissions/{id}
│   ├── RoleController.java        # POST /roles, POST /roles/{id}/permissions/{id}
│   ├── PermissionController.java  # POST /permissions
│   ├── UserRoleController.java    # POST /users/{id}/roles/{id}
│   └── SecureDataController.java  # GET /secure-data
├── dto
│   ├── CreateUserRequest.java / UserResponse.java
│   ├── NameRequest.java
│   ├── IdNameResponse.java
│   └── MessageResponse.java
├── entity
│   ├── User.java / Role.java / Permission.java
│   └── RolePermission.java / UserRole.java / UserPermission.java
├── repository
│   └── (one Spring Data repository per entity)
├── security
│   ├── CustomUserDetailsService.java
│   └── CustomPermissionEvaluator.java   ← the core of the assignment
├── service
│   ├── UserService.java / UserPermissionService.java / RoleService.java / PermissionService.java / UserRoleService.java
└── exception
    ├── ResourceNotFoundException.java / DuplicateResourceException.java
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

---

## Class-by-Class Explanation (for interview prep)

### `SecurityConfig`
**What:** Central Spring Security configuration.
**Why:** Wires up HTTP Basic auth, enables `@PreAuthorize` (`@EnableMethodSecurity`), and —
critically — registers `CustomPermissionEvaluator` on a `DefaultMethodSecurityExpressionHandler`
bean so that `hasPermission(...)` expressions actually call our custom logic instead of Spring's
default (ACL-based) implementation.
**Key methods:** `securityFilterChain()` defines which requests need authentication;
`methodSecurityExpressionHandler()` is what makes our evaluator "pluggable" into `@PreAuthorize`.

### `CustomUserDetailsService`
**What:** Implements Spring Security's `UserDetailsService`.
**Why:** Spring Security needs to know how to find a user and their password hash during login.
This class looks the user up in H2 via `UserRepository`.
**Important detail:** it deliberately grants **no authorities**. Roles/permissions are not
attached to the session at login — they are re-checked on every request. That is what makes the
whole system "dynamic" rather than "cached at login time".

### `CustomPermissionEvaluator`
**What:** Implements `PermissionEvaluator`. This is the engine's brain.
**Why:** `@PreAuthorize("hasPermission(...)")` needs *something* to decide true/false. This class
supplies that decision by walking `User → UserRole → Role → RolePermission → Permission` in the
database, right at request time.
**Key method:** `hasPermission(Authentication, Object, Object)` — takes the logged-in username
and the required permission string, and returns `true`/`false`.
**Interacts with:** `UserRepository`, `UserRoleRepository`, `RolePermissionRepository`.

### `Role` / `Permission`
**What:** Simple JPA entities — just an `id` and a `name`.
**Why:** They represent the two "levels" of the RBAC model. Kept intentionally minimal.

### `UserRole` / `RolePermission`
**What:** JPA entities representing the join tables `user_role` and `role_permission`.
**Why:** Modeled as first-class entities (not `@ManyToMany` collections) so individual mappings
can be created and deleted independently — exactly what "assign role" and "remove permission"
operations need to do.

### Controllers (`RoleController`, `PermissionController`, `UserRoleController`, `SecureDataController`)
**What:** Thin REST layer — validate input, delegate to a service, map the result to an HTTP
response.
**Why:** Each protected method is annotated with `@PreAuthorize("hasPermission(null, 'X')")`,
which is the *only* authorization check in the whole codebase — there is no manual
`if (hasRole(...))` anywhere.

### Services (`RoleService`, `PermissionService`, `UserRoleService`)
**What:** Business logic — duplicate checks, not-found checks, and persistence via repositories.
**Why:** Keeps controllers thin and keeps validation/business rules unit-testable in isolation
from the web layer (see the Mockito-based `*ServiceTest` classes).

### Repositories
**What:** Spring Data JPA interfaces — no implementation code needed.
**Why:** Give the rest of the app simple, typed methods like `findByUsername`,
`existsByRoleAndPermission`, etc., backed by generated SQL.

---

## Example Permission Checks

```java
// Protects a management endpoint - requires CREATE_ROLE
@PreAuthorize("hasPermission(null, 'CREATE_ROLE')")

// Protects the demo resource - requires READ_SECURE_DATA
@PreAuthorize("hasPermission(null, 'READ_SECURE_DATA')")
```

Neither line ever names a role. Whether a given user passes the check depends entirely on
what's currently stored in `role_permission` and `user_role` for that user.

---

## Notes

- No UI is included by design — the assignment is backend/API-focused and meant to be tested
  through Postman.
- `spring.jpa.hibernate.ddl-auto=update` is used for convenience in this assignment context;
  in a real production system you'd use a migration tool (Flyway/Liquibase) instead.
