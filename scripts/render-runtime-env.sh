#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <output-file>" >&2
  exit 1
fi

output_file="$1"

required_vars=(
  APP_IMAGE
  AWS_REGION
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "missing required environment variable: ${var_name}" >&2
    exit 1
  fi
done

cat >"${output_file}" <<EOF
AWS_REGION=${AWS_REGION}
APP_IMAGE=${APP_IMAGE}
APP_JWT_SECRET=your-super-secret-key-that-is-at-least-32-characters-long
APP_JWT_EXPIRATION_MS=86400000
APP_CORS_ALLOWED_ORIGINS="http://localhost:8080"
POSTGRES_DB=healthapp
POSTGRES_USER=healthapp
POSTGRES_PASSWORD=healthapp_dev
REDIS_PASSWORD=redis_dev
HOST_DATA_DIR="/opt/healthapp/data"
EOF
