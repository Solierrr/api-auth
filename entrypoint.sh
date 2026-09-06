#!/bin/sh
set -e

if [ -n "$JWT_KEYSTORE_BASE64" ] && [ -n "$JWT_KEYSTORE_PATH" ]; then
  mkdir -p "$(dirname "$JWT_KEYSTORE_PATH")"
  echo "$JWT_KEYSTORE_BASE64" | base64 -d > "$JWT_KEYSTORE_PATH"
fi

exec java -jar app.jar
