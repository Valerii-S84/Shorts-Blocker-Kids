#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/home/serputko/Android/Sdk}}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
GRADLE_CMD="${GRADLE_CMD:-gradle}"
export ANDROID_HOME ANDROID_SDK_ROOT

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
