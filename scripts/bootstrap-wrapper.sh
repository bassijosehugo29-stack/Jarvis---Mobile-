#!/bin/sh
# No binaries are bundled. Fetch only the official Gradle wrapper, verify SHA-256 first.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
DEST="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
fetch() {
    if command -v curl >/dev/null 2>&1; then
        curl --fail --location --proto '=https' --tlsv1.2 --connect-timeout 10 --max-time 120 "$1" -o "$2"
    elif command -v wget >/dev/null 2>&1; then
        wget --https-only --timeout=30 "$1" -O "$2"
    else
        echo 'Instale curl ou wget, ou execute gradle wrapper com Gradle 8.11.1.' >&2
        exit 1
    fi
}
fetch 'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar' "$TMP/wrapper.jar"
fetch 'https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar.sha256' "$TMP/wrapper.sha256"
EXPECTED=$(tr -d '[:space:]' < "$TMP/wrapper.sha256")
if command -v sha256sum >/dev/null 2>&1; then
    ACTUAL=$(sha256sum "$TMP/wrapper.jar" | cut -d ' ' -f 1)
else
    ACTUAL=$(shasum -a 256 "$TMP/wrapper.jar" | cut -d ' ' -f 1)
fi
if [ "${#EXPECTED}" -ne 64 ] || [ "$ACTUAL" != "$EXPECTED" ]; then
    echo 'Checksum do wrapper inválido. Download rejeitado.' >&2
    exit 1
fi
mkdir -p "$APP_HOME/gradle/wrapper"
mv "$TMP/wrapper.jar" "$DEST"
