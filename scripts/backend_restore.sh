#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <backup-file>" >&2
    exit 1
fi

BACKUP_FILE="$1"
BACKUP_DIR="${SBK_BACKUP_DIR:-backups/billing-backend}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

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

if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "Backup file not found: $BACKUP_FILE" >&2
    exit 1
fi

if [[ "${SBK_BACKUP_WITH_DOCKER_COMPOSE:-false}" == "true" ]]; then
    mkdir -p "$BACKUP_DIR"
    PRE_RESTORE_FILE="$BACKUP_DIR/pre-restore-$TIMESTAMP.dump"
    wait_for_compose_database
    docker compose exec -T billing-db sh -c \
        'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom' >"$PRE_RESTORE_FILE"
    docker compose exec -T billing-db sh -c \
        'pg_restore --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' <"$BACKUP_FILE"
    echo "$PRE_RESTORE_FILE"
    exit 0
fi

if [[ -n "${SBK_DATABASE_URL:-}" ]]; then
    command -v pg_dump >/dev/null || {
        echo "pg_dump is required before PostgreSQL restore" >&2
        exit 1
    }
    command -v pg_restore >/dev/null || {
        echo "pg_restore is required for PostgreSQL restore" >&2
        exit 1
    }
    mkdir -p "$BACKUP_DIR"
    export PGUSER="${SBK_DATABASE_USER:-}"
    export PGPASSWORD="${SBK_DATABASE_PASSWORD:-}"
    DUMP_URL="${SBK_DATABASE_URL#jdbc:}"
    PRE_RESTORE_FILE="$BACKUP_DIR/pre-restore-$TIMESTAMP.dump"
    pg_dump --format=custom --file="$PRE_RESTORE_FILE" "$DUMP_URL"
    pg_restore --clean --if-exists --no-owner --dbname="$DUMP_URL" "$BACKUP_FILE"
    echo "$PRE_RESTORE_FILE"
    exit 0
fi

STORE_FILE="${SBK_BACKEND_STORE_FILE:-billing-backend-data.json}"

mkdir -p "$(dirname "$STORE_FILE")"
if [[ -f "$STORE_FILE" ]]; then
    cp -p "$STORE_FILE" "$STORE_FILE.pre-restore-$TIMESTAMP"
fi

cp -p "$BACKUP_FILE" "$STORE_FILE"
echo "$STORE_FILE"
