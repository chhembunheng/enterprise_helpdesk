#!/bin/sh
set -eu

# The PostgreSQL image creates POSTGRES_DB. Create the other service databases.
# Quoted psql variables safely handle database names; rerunning is idempotent.
for database in "$AUTH_DB" "$NOTIFICATION_DB"; do
    psql --set=ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=database="$database" <<'SQL'
SELECT format('CREATE DATABASE %I', :'database')
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = :'database')
\gexec
SQL
done
