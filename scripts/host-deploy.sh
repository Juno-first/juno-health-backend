#!/usr/bin/env bash
set -euo pipefail

deploy_root="/opt/healthapp/current"

cd "${deploy_root}"

set -a
. ./.env.aws
set +a

ecr_registry="${APP_IMAGE%%/*}"

aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ecr_registry}"

docker compose --env-file .env.aws -f docker-compose.yml pull app
docker compose --env-file .env.aws -f docker-compose.yml up -d --remove-orphans
docker image prune -f
