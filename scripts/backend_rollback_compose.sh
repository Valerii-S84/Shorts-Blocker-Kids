#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <previous-backend-image>" >&2
    exit 1
fi

export SBK_BACKEND_IMAGE="$1"

docker compose up -d --no-deps billing-backend
docker compose ps billing-backend
