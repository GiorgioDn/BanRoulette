#!/usr/bin/env bash
# scripts/android-build.sh — Build Android APK locally
# Usage: bash scripts/android-build.sh  (from the repo root)
# Requires: JDK 17+, Android SDK

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "==> Running tests and building Android Release APK..."
cd "${ROOT_DIR}"
./gradlew :core:test :android:testDebugUnitTest :android:assembleRelease -PversionCode=1 -PversionName=1.0.0 "$@"

OUTPUT_DIR="${ROOT_DIR}/android/build/outputs/apk/release"
echo "==> APK generated in: ${OUTPUT_DIR}"
ls -lh "${OUTPUT_DIR}"
