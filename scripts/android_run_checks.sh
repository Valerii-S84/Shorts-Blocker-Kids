#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

. "$ROOT_DIR/scripts/android_env.sh"
GRADLE_CMD="${GRADLE_CMD:-gradle}"

if [[ -x "./gradlew" ]]; then
    GRADLE_CMD="./gradlew"
fi

"$GRADLE_CMD" :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
