#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

. "$ROOT_DIR/scripts/android_env.sh"

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

if [[ ! -x "$SDKMANAGER" ]]; then
    echo "Missing sdkmanager at $SDKMANAGER" >&2
    echo "Install Android command-line tools or set ANDROID_HOME/ANDROID_SDK_ROOT." >&2
    exit 1
fi

if [[ "${HTTP_PROXY-}" == "" ]]; then unset HTTP_PROXY; fi
if [[ "${HTTPS_PROXY-}" == "" ]]; then unset HTTPS_PROXY; fi
if [[ "${http_proxy-}" == "" ]]; then unset http_proxy; fi
if [[ "${https_proxy-}" == "" ]]; then unset https_proxy; fi

echo "Using ANDROID_HOME=$ANDROID_HOME"
yes | "$SDKMANAGER" --install \
    "platform-tools" \
    "build-tools;36.0.0" \
    "platforms;android-35" \
    "platforms;android-36" \
    "emulator" \
    "system-images;android-35;google_apis_playstore;x86_64"

"$SDKMANAGER" --list_installed
