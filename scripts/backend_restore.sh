#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <backup-json-file>" >&2
    exit 1
fi

BACKUP_FILE="$1"
STORE_FILE="${SBK_BACKEND_STORE_FILE:-billing-backend-data.json}"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "Backup file not found: $BACKUP_FILE" >&2
    exit 1
fi

mkdir -p "$(dirname "$STORE_FILE")"
if [[ -f "$STORE_FILE" ]]; then
    cp -p "$STORE_FILE" "$STORE_FILE.pre-restore-$TIMESTAMP"
fi

cp -p "$BACKUP_FILE" "$STORE_FILE"
echo "$STORE_FILE"
