#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

. "$ROOT_DIR/scripts/android_env.sh"
AVD_NAME="${AVD_NAME:-ShortsBlocker_Pixel_API35}"
DEVICE_PROFILE="${DEVICE_PROFILE:-pixel_6}"
SYSTEM_IMAGE="${SYSTEM_IMAGE:-system-images;android-35;google_apis_playstore;x86_64}"

AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"

if [[ ! -f "$AVDMANAGER" ]]; then
    echo "Missing avdmanager at $AVDMANAGER" >&2
    exit 1
fi

if sh "$AVDMANAGER" list avd | grep -q "Name: $AVD_NAME"; then
    echo "AVD already exists: $AVD_NAME"
    exit 0
fi

echo "Creating AVD $AVD_NAME with $SYSTEM_IMAGE and device $DEVICE_PROFILE"
printf 'no\n' | sh "$AVDMANAGER" create avd \
    --name "$AVD_NAME" \
    --package "$SYSTEM_IMAGE" \
    --device "$DEVICE_PROFILE"

sh "$AVDMANAGER" list avd
