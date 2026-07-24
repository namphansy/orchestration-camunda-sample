#!/bin/sh
set -eu

BASEDIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
MVN_DIR="$BASEDIR/.mvn/wrapper"
MAVEN_VERSION="3.9.10"
MAVEN_HOME="$MVN_DIR/apache-maven-$MAVEN_VERSION"
MAVEN_ZIP="$MVN_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  mkdir -p "$MVN_DIR"
  if [ ! -f "$MAVEN_ZIP" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$MAVEN_URL" -o "$MAVEN_ZIP"
    elif command -v wget >/dev/null 2>&1; then
      wget -q "$MAVEN_URL" -O "$MAVEN_ZIP"
    else
      echo "curl or wget is required to bootstrap Maven $MAVEN_VERSION." >&2
      exit 1
    fi
  fi
  unzip -q "$MAVEN_ZIP" -d "$MVN_DIR"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"

