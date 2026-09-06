#!/bin/sh
set -e

INFISICAL_TOKEN=$(infisical login --method=universal-auth \
  --client-id="$INFISICAL_CLIENT_ID" \
  --client-secret="$INFISICAL_CLIENT_SECRET" \
  --silent --plain)

# JWT_KEYSTORE_BASE64/JWT_KEYSTORE_PATH só existem dentro do processo injetado
# pelo infisical run, então a decodificação do keystore roda como parte desse
# processo, antes de subir o jar.
exec infisical run \
  --token="$INFISICAL_TOKEN" \
  --projectId=2296d19c-5f3b-41e1-afa3-fcde39966a71 \
  --env="${INFISICAL_ENV:-qa}" \
  --path=/ \
  -- sh -c 'mkdir -p "$(dirname "$JWT_KEYSTORE_PATH")" && echo "$JWT_KEYSTORE_BASE64" | base64 -d > "$JWT_KEYSTORE_PATH" && exec java -jar app.jar'
