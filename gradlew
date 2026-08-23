#!/bin/sh
set -eu

GRADLE_VERSION="9.5.1"
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/custom-dists/gradle-${GRADLE_VERSION}"
GRADLE_ZIP="${TMPDIR:-/tmp}/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$GRADLE_HOME"
  echo "Downloading Gradle $GRADLE_VERSION..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$GRADLE_URL" -o "$GRADLE_ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$GRADLE_ZIP" "$GRADLE_URL"
  else
    echo "curl or wget is required to download Gradle." >&2
    exit 1
  fi

  tmp_dir="${GRADLE_HOME}.tmp"
  rm -rf "$tmp_dir"
  mkdir -p "$tmp_dir"
  unzip -q "$GRADLE_ZIP" -d "$tmp_dir"
  mv "$tmp_dir/gradle-${GRADLE_VERSION}"/* "$GRADLE_HOME/"
  rm -rf "$tmp_dir" "$GRADLE_ZIP"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
