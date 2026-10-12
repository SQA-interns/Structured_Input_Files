#!/usr/bin/env sh
# Runs docker compose for the local stack with the keys it needs from the repository's .env,
# without printing them and without parsing any other line of .env.
# Usage (from anywhere): 02_output/scripts/compose.sh up -d --build
set -eu
here="$(cd "$(dirname "$0")/.." && pwd)"
env_file="${ENV_FILE:-$here/../.env}"
for key in POSTGRES_PASSWORD ORGANIZER_USERNAME ORGANIZER_PASSWORD ORGANIZER_EMAILS CONFERENCE_NAME; do
  value="$(sed -n "s/^$key=//p" "$env_file" | tr -d '\r')"
  [ -n "$value" ] && export "$key=$value"
done
exec docker compose -f "$here/docker-compose.yml" "$@"
