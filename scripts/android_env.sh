#!/usr/bin/env bash

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
    echo "Set ANDROID_HOME or ANDROID_SDK_ROOT to your Android SDK directory." >&2
    exit 1
fi

ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export ANDROID_HOME ANDROID_SDK_ROOT
