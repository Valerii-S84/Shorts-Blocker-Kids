#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

IMAGE_TAG="${SBK_BACKEND_IMAGE:-shorts-blocker-kids-billing-backend:$(date -u +%Y%m%dT%H%M%SZ)}"
export SBK_BACKEND_IMAGE="$IMAGE_TAG"

./gradlew :billing-backend:test :billing-backend:installDist

docker compose build billing-backend
docker compose up -d billing-db
SBK_BACKUP_WITH_DOCKER_COMPOSE=true ./scripts/backend_backup.sh
docker compose up -d --no-deps billing-backend
docker compose ps billing-backend

echo "$SBK_BACKEND_IMAGE"
