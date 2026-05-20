#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/home/serputko/Android/Sdk}}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
GRADLE_CMD="${GRADLE_CMD:-gradle}"
export ANDROID_HOME ANDROID_SDK_ROOT

if [[ -x "./gradlew" ]]; then
    GRADLE_CMD="./gradlew"
fi

"$GRADLE_CMD" :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
