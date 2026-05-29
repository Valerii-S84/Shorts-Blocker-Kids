#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BACKUP_DIR="${SBK_BACKUP_DIR:-backups/billing-backend}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

mkdir -p "$BACKUP_DIR"

wait_for_compose_database() {
    local attempts="${SBK_DATABASE_WAIT_ATTEMPTS:-30}"
    docker compose up -d billing-db >/dev/null
    until docker compose exec -T billing-db sh -c \
        'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null'; do
        attempts=$((attempts - 1))
        if [[ "$attempts" -le 0 ]]; then
            echo "Timed out waiting for Docker Compose PostgreSQL readiness" >&2
            exit 1
        fi
        sleep 2
    done
}

if [[ "${SBK_BACKUP_WITH_DOCKER_COMPOSE:-false}" == "true" ]]; then
    BACKUP_FILE="$BACKUP_DIR/postgres-$TIMESTAMP.dump"
    wait_for_compose_database
    docker compose exec -T billing-db sh -c \
        'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' >"$BACKUP_FILE"
    echo "$BACKUP_FILE"
    exit 0
fi

if [[ -n "${SBK_DATABASE_URL:-}" ]]; then
    command -v pg_dump >/dev/null || {
        echo "pg_dump is required for PostgreSQL backups" >&2
        exit 1
    }
    export PGUSER="${SBK_DATABASE_USER:-}"
    export PGPASSWORD="${SBK_DATABASE_PASSWORD:-}"
    DUMP_URL="${SBK_DATABASE_URL#jdbc:}"
    BACKUP_FILE="$BACKUP_DIR/postgres-$TIMESTAMP.dump"
    pg_dump --format=custom --file="$BACKUP_FILE" "$DUMP_URL"
    echo "$BACKUP_FILE"
    exit 0
fi

STORE_FILE="${SBK_BACKEND_STORE_FILE:-billing-backend-data.json}"
STORE_NAME="$(basename "$STORE_FILE")"
BACKUP_FILE="$BACKUP_DIR/${STORE_NAME%.json}-$TIMESTAMP.json"

if [[ ! -f "$STORE_FILE" ]]; then
    echo "Store file not found: $STORE_FILE" >&2
    exit 1
fi

cp -p "$STORE_FILE" "$BACKUP_FILE"
echo "$BACKUP_FILE"
