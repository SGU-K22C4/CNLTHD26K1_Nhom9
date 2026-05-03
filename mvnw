#!/bin/sh

set -e

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
DIST_NAME="apache-maven-3.9.9"
DIST_ARCHIVE="$WRAPPER_DIR/${DIST_NAME}-bin.zip"
DIST_HOME="$WRAPPER_DIR/dists/$DIST_NAME"
MAVEN_HOME="$DIST_HOME/$DIST_NAME"
DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/${DIST_NAME}-bin.zip"

mkdir -p "$WRAPPER_DIR/dists"

if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
  echo "Downloading Maven $DIST_NAME..."
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DOWNLOAD_URL" -o "$DIST_ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DOWNLOAD_URL" -O "$DIST_ARCHIVE"
  elif command -v powershell >/dev/null 2>&1; then
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '$DOWNLOAD_URL' -OutFile '$DIST_ARCHIVE'"
  else
    echo "Error: curl, wget, or powershell is required to download Maven." >&2
    exit 1
  fi

  rm -rf "$DIST_HOME"
  mkdir -p "$DIST_HOME"

  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$DIST_ARCHIVE" -d "$DIST_HOME"
  elif command -v powershell >/dev/null 2>&1; then
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '$DIST_ARCHIVE' -DestinationPath '$DIST_HOME' -Force"
  else
    echo "Error: unzip or powershell is required to extract Maven." >&2
    exit 1
  fi
fi

exec "$MAVEN_HOME/bin/mvn" "$@"