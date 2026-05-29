#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

. "$ROOT_DIR/scripts/android_env.sh"
GRADLE_CMD="${GRADLE_CMD:-gradle}"

ADB="$ANDROID_HOME/platform-tools/adb"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [[ -x "./gradlew" ]]; then
    GRADLE_CMD="./gradlew"
fi

if [[ ! -x "$ADB" ]]; then
    echo "Missing adb at $ADB" >&2
    exit 1
fi

"$GRADLE_CMD" :app:assembleDebug
"$ADB" install -r "$APK_PATH"
"$ADB" shell pm list packages | grep -i shorts
