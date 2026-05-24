#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

STORE_FILE="${SBK_BACKEND_STORE_FILE:-billing-backend-data.json}"
BACKUP_DIR="${SBK_BACKUP_DIR:-backups/billing-backend}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"
STORE_NAME="$(basename "$STORE_FILE")"
BACKUP_FILE="$BACKUP_DIR/${STORE_NAME%.json}-$TIMESTAMP.json"

if [[ ! -f "$STORE_FILE" ]]; then
    echo "Store file not found: $STORE_FILE" >&2
    exit 1
fi

mkdir -p "$BACKUP_DIR"
cp -p "$STORE_FILE" "$BACKUP_FILE"
echo "$BACKUP_FILE"
