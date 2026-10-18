#!/usr/bin/env bash
# scripts/package.sh — macOS (.pkg) and Linux (.deb) jpackage wrapper
# Usage: bash scripts/package.sh  (from the repo root)
# Requires: JDK 21+, mvn package already run (target/jpackage-input/ must exist)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${SCRIPT_DIR}/../ban_roulette"
INPUT_DIR="${PROJECT_DIR}/target/jpackage-input"
OUTPUT_DIR="${PROJECT_DIR}/target/installer"
MAIN_CLASS="com.example.app.AppLauncher"
APP_NAME="BanRoulette"

# Auto-detect built jar and version from jpackage-input
JAR_PATH=$(find "${INPUT_DIR}" -maxdepth 1 -name "ban_roulette-*.jar" 2>/dev/null | head -n 1 || true)
if [[ -n "${JAR_PATH}" ]]; then
    MAIN_JAR="$(basename "${JAR_PATH}")"
    APP_VERSION="${MAIN_JAR#ban_roulette-}"
    APP_VERSION="${APP_VERSION%.jar}"
else
    MAIN_JAR="ban_roulette-1.0.0.jar"
    APP_VERSION="1.0.0"
fi

# Detect OS
OS="$(uname -s)"
case "${OS}" in
    Darwin)  PKG_TYPE="pkg" ;;
    Linux)   PKG_TYPE="deb" ;;
    *)       echo "Unsupported OS: ${OS}"; exit 1 ;;
esac

echo "==> Packaging ${APP_NAME} as .${PKG_TYPE} ..."
mkdir -p "${OUTPUT_DIR}"

# Icon path (optional — add ban_roulette/src/main/resources/icon.icns or icon.png later)
ICON_ARG=""
ICON_ICNS="${PROJECT_DIR}/src/main/resources/icon.icns"
ICON_PNG="${PROJECT_DIR}/src/main/resources/icon.png"
if [[ "${OS}" == "Darwin" && -f "${ICON_ICNS}" ]]; then
    ICON_ARG="--icon ${ICON_ICNS}"
elif [[ "${OS}" == "Linux" && -f "${ICON_PNG}" ]]; then
    ICON_ARG="--icon ${ICON_PNG}"
fi

jpackage \
    --type "${PKG_TYPE}" \
    --name "${APP_NAME}" \
    --app-version "${APP_VERSION}" \
    --input "${INPUT_DIR}" \
    --main-jar "${MAIN_JAR}" \
    --main-class "${MAIN_CLASS}" \
    --dest "${OUTPUT_DIR}" \
    --java-options "--enable-preview" \
    ${ICON_ARG}

echo "==> Installer written to: ${OUTPUT_DIR}"
ls -lh "${OUTPUT_DIR}"
