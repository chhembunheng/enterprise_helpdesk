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
