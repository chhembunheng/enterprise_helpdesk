# Enterprise Helpdesk

## Local environment

The repository root contains `.env.example`. Copy it to `.env` if you do not
already have one, then set your local `POSTGRES_PASSWORD`. Keep values unquoted.
The real `.env` is ignored by Git.

```bash
test -f .env || cp .env.example .env
```

Windows PowerShell:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

Each service explicitly imports the root `.env` as Spring properties. It works
when the working directory is either the repository root or the service folder.
OS environment variables and `SPRING_DATASOURCE_*` overrides take precedence.
No dotenv dependency is required.

## Start the database

From the repository root:

```bash
docker compose up -d --wait postgres
```

This starts a dedicated Helpdesk PostgreSQL instance at `127.0.0.1:15432`.
It creates `ticket_db`, `auth_db`, and `notification_db`. Database contents remain
in the `pgdata` volume when the container restarts. Other PostgreSQL instances
on ports 5432 or 5433 are not changed.

The image runs initialization scripts only for a new data directory. To create
any missing additional service databases in an existing volume, run:

```bash
docker compose exec postgres sh /docker-entrypoint-initdb.d/01-init-databases.sh
```

Changing `POSTGRES_PASSWORD` in `.env` does not reset a role password in an
existing PostgreSQL data volume. Keep the configured credentials aligned with
that database instance.

## Run a service

For example, from the repository root:

```bash
cd ticket-service
sh ./gradlew bootRun
```

Windows PowerShell:

```powershell
cd ticket-service
.\gradlew.bat bootRun
```

Port 8081 is already occupied in this workspace, so Ticket Service defaults to
18081 through `.env`. Run the other services from their folders in separate
terminals:

| Service | HTTP port | Database |
| --- | --- | --- |
| ticket-service | 18081 | ticket_db |
| notification-service | 8082 | notification_db |
| auth-service | 8083 | auth_db |

The project currently targets Java 25, as configured in each service's Gradle
build file. JPA settings in `.env.example` match the existing development setup.

Check the ticket service:

```bash
curl http://localhost:18081/tickets/health
```

A `password authentication failed` error means the credentials were rejected by
the PostgreSQL instance at the configured host and port. Resolve that connection
before addressing any subsequent Hibernate dialect/metadata error.

## Search tickets

`GET /tickets/search?title=Learn` searches for the supplied text anywhere in a
ticket title, ignoring case. Do not add an extra slash before `search` or put
quotes around the query value. Leading and trailing whitespace is trimmed.

The endpoint returns HTTP 200 with a JSON array of matching `TicketResponse`
objects. Multiple tickets may match, including tickets with duplicate titles.
No matches returns `[]`. Missing, empty or whitespace-only `title` returns
HTTP 400. This replaces the original single exact-title response.

Restart Ticket Service after changing its Java source. For example:

```bash
sh ./gradlew bootRun
```

## Register, log in and submit a ticket

Start PostgreSQL, then run `sh ./gradlew bootRun` from both `auth-service` and
`ticket-service` in separate terminals. Open Ticket Service's root URL in your
browser (for example `http://localhost:8085/` when `TICKET_SERVICE_PORT=8085`).
The page has registration, login, ticket submission and your ticket list.
The auth URL follows `AUTH_SERVICE_PORT`; use `AUTH_SERVICE_URL` to override it.
Tokens are kept in browser memory and cleared on logout/reload. Login expires
in one hour by default; there is no refresh-token endpoint.

| Service | Endpoint | Request / result |
| --- | --- | --- |
| Auth | `POST /auth/register` | `{ "username": "student1", "email": "student1@example.com", "password": "ChooseAStrongPassword" }` → 201 user `{id,username,email}` |
| Auth | `POST /auth/login` | `{ "username": "student1", "password": "ChooseAStrongPassword" }` → 200 `{accessToken,tokenType,expiresIn,user}` |
| Auth | `GET /auth/me` | Bearer token → current active user |
| Ticket | `POST /tickets` | Bearer token + `{ "title": "Cannot access course", "description": "Please help me sign in" }` → 201 ticket, initially OPEN |
| Ticket | `GET /tickets` | Bearer token → your tickets only |
| Ticket | `GET /tickets/search?title=Learn` | Bearer token → matching tickets belonging to you; no matches `[]` |
| Ticket | `GET /tickets/{id}` | Bearer token → your ticket; another user's ticket is 404 |
| Ticket | `PATCH /tickets/{id}` / `PATCH /tickets/{id}/delete` | Bearer token → update/delete your own ticket only |

Use `Authorization: Bearer <accessToken>` on authenticated requests. Public
registration always creates role USER, active and not deleted; client-supplied
roles/owner IDs are never trusted. Passwords are BCrypt hashes and never appear
in user responses or JWT claims. Usernames and emails are trimmed/lowercased;
login uses username. Passwords are 8–64 characters and at most 72 UTF-8 bytes.
Duplicate username/email returns 409; invalid input 400; invalid credentials,
inactive/deleted users, missing/invalid/expired tokens return 401.

Both services validate HS256 signatures, issuer, audience and expiry using
`JWT_SECRET` (base64, at least 32 random bytes), `JWT_ISSUER` and `JWT_AUDIENCE`.
Generate a local secret with `openssl rand -base64 32` and put it in the ignored
root `.env`; keep the same value for both services. Do not commit it. Use HTTPS
outside local development. `AUTH_ALLOWED_ORIGINS` is a comma-separated allowlist
for the browser portal; defaults include the configured localhost ticket port.
Access tokens remain usable until expiry after logout/account deactivation;
`/auth/me` additionally checks the current account state. Existing tickets with
no `created_by` are left untouched and are not exposed as a user's own tickets.
This step implements login and submission; Kafka notifications and the gateway
remain separate phases of the blueprint.
