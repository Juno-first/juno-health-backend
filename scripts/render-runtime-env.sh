#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 <output-file>" >&2
  exit 1
fi

output_file="$1"

required_vars=(
  APP_IMAGE
  AI_APP_IMAGE
  AWS_REGION
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "missing required environment variable: ${var_name}" >&2
    exit 1
  fi
done

ai_jwt_secret="${AI_JWT_SECRET:-${APP_JWT_SECRET:-}}"
if [[ -z "${ai_jwt_secret}" ]]; then
  echo "missing required environment variable: AI_JWT_SECRET (or APP_JWT_SECRET fallback)" >&2
  exit 1
fi

cat >"${output_file}" <<EOF
AWS_REGION=${AWS_REGION}
APP_IMAGE=${APP_IMAGE}
AI_APP_IMAGE=${AI_APP_IMAGE}
APP_JWT_SECRET=${APP_JWT_SECRET:-your-super-secret-key-that-is-at-least-32-characters-long}
APP_JWT_EXPIRATION_MS=${APP_JWT_EXPIRATION_MS:-86400000}
APP_CORS_ALLOWED_ORIGINS=${APP_CORS_ALLOWED_ORIGINS}
AI_CORS_ALLOWED_ORIGINS=${AI_CORS_ALLOWED_ORIGINS:-${APP_CORS_ALLOWED_ORIGINS}}
AI_CREDENTIALS_HOST_PATH=/opt/healthapp/current/credentials.json
AI_GOOGLE_APPLICATION_CREDENTIALS=/opt/healthapp/current/credentials.json
AI_GEMINI_MODEL=${AI_GEMINI_MODEL:-gemini-2.5-flash-lite}
AI_TTS_VOICE_NAME=${AI_TTS_VOICE_NAME:-en-US-Chirp3-HD-Kore}
AI_TTS_LANGUAGE_CODE=${AI_TTS_LANGUAGE_CODE:-en-US}
AI_TTS_SPEAKING_RATE=${AI_TTS_SPEAKING_RATE:-1.0}
AI_KAFKA_BOOTSTRAP_SERVERS=${AI_KAFKA_BOOTSTRAP_SERVERS:-kafka:29092}
AI_KAFKA_QUEUE_TOPIC=${AI_KAFKA_QUEUE_TOPIC:-department.queue.snapshot}
AI_JWT_SECRET=${ai_jwt_secret}
AI_GEMINI_API_KEY=${AI_GEMINI_API_KEY:-}
AI_OPENAI_API_KEY=${AI_OPENAI_API_KEY:-}
AI_JWT_ALGORITHM=${AI_JWT_ALGORITHM:-HS256}
POSTGRES_DB=healthapp
POSTGRES_USER=healthapp
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-healthapp_dev}
REDIS_PASSWORD=${REDIS_PASSWORD:-redis_dev}
HOST_DATA_DIR="/opt/healthapp/data"
EOF
