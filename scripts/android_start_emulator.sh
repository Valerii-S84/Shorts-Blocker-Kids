#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/home/serputko/Android/Sdk}}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
AVD_NAME="${AVD_NAME:-ShortsBlocker_Pixel_API35}"
EMULATOR_NO_WINDOW="${EMULATOR_NO_WINDOW:-1}"
export ANDROID_HOME ANDROID_SDK_ROOT

EMULATOR="$ANDROID_HOME/emulator/emulator"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR_LOG="${EMULATOR_LOG:-/tmp/shortsblocker-emulator.log}"

if [[ ! -x "$EMULATOR" ]]; then
    echo "Missing emulator at $EMULATOR" >&2
    exit 1
fi

if [[ ! -x "$ADB" ]]; then
    echo "Missing adb at $ADB" >&2
    exit 1
fi

ARGS=(-avd "$AVD_NAME" -netdelay none -netspeed full -no-snapshot -no-boot-anim)
if [[ "$EMULATOR_NO_WINDOW" == "1" ]]; then
    ARGS+=(-no-window)
fi

echo "Starting emulator $AVD_NAME"
echo "Emulator log: $EMULATOR_LOG"
nohup "$EMULATOR" "${ARGS[@]}" >"$EMULATOR_LOG" 2>&1 &
EMULATOR_PID=$!

echo "Emulator process PID: $EMULATOR_PID"
sleep 3
if ! kill -0 "$EMULATOR_PID" 2>/dev/null; then
    echo "Emulator process exited before adb connection." >&2
    wait "$EMULATOR_PID"
fi

timeout 180 "$ADB" wait-for-device

echo "Waiting for Android boot completion"
for _ in {1..120}; do
    BOOT_COMPLETED="$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    if [[ "$BOOT_COMPLETED" == "1" ]]; then
        "$ADB" devices
        exit 0
    fi
    sleep 2
done

echo "Timed out waiting for emulator boot." >&2
exit 1
