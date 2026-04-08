#!/bin/sh
# ============================================================================
# Kong Entrypoint Wrapper
# Replaces ${JWT_SECRET} placeholder in kong.yml with the actual env var value
# before Kong starts, since DB-less mode doesn't support env var interpolation.
# ============================================================================

set -e

TEMPLATE_FILE="/kong/template/kong.yml.template"
CONFIG_FILE="/tmp/kong.yml"

if [ -z "$JWT_SECRET" ]; then
  echo "[kong-entrypoint] WARNING: JWT_SECRET is not set. jwt-auth plugin may fail."
fi

# Replace the placeholder with the actual secret value
echo "[kong-entrypoint] Injecting JWT_SECRET into Kong declarative config..."
sed "s|\${JWT_SECRET}|${JWT_SECRET}|g" "$TEMPLATE_FILE" > "$CONFIG_FILE"

# Point Kong to the processed config file
export KONG_DECLARATIVE_CONFIG="$CONFIG_FILE"

echo "[kong-entrypoint] Config ready at $CONFIG_FILE. Starting Kong..."

# Delegate to the official Kong entrypoint
exec /docker-entrypoint.sh kong docker-start