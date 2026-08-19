#!/bin/sh
# Minimal Maven Wrapper launcher for the project-pinned Maven distribution.
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
DIST_URL=$(sed -n 's/^distributionUrl=//p' "$PROPS")
DIST_FILE=${DIST_URL##*/}
DIST_NAME=${DIST_FILE%-bin.zip}
MAVEN_USER_HOME=${MAVEN_USER_HOME:-"$HOME/.m2"}
MAVEN_HOME="$MAVEN_USER_HOME/wrapper/dists/$DIST_NAME"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  TMP_DIR=$(mktemp -d)
  trap 'rm -rf "$TMP_DIR"' EXIT INT TERM
  ARCHIVE="$TMP_DIR/$DIST_FILE"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$ARCHIVE"
  else
    echo "Maven Wrapper requires curl or wget to download Maven." >&2
    exit 1
  fi
  mkdir -p "$MAVEN_HOME"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ARCHIVE" -d "$TMP_DIR"
  else
    (cd "$TMP_DIR" && jar xf "$ARCHIVE")
  fi
  cp -R "$TMP_DIR/$DIST_NAME/." "$MAVEN_HOME/"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
