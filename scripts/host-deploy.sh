#!/usr/bin/env bash
set -euo pipefail

deploy_root="/opt/healthapp/current"
credentials_path="${deploy_root}/credentials.json"

ensure_docker_prereqs() {
  local should_install=0

  if ! command -v docker >/dev/null 2>&1; then
    should_install=1
  elif ! docker compose version >/dev/null 2>&1; then
    should_install=1
  fi

  if [[ "${should_install}" -eq 1 ]]; then
    dnf update -y
    dnf install -y docker docker-compose-plugin awscli
  fi

  systemctl enable docker

  if ! systemctl is-active --quiet docker; then
    systemctl start docker
  fi

  if ! systemctl is-active --quiet docker; then
    echo "docker service failed to start" >&2
    exit 1
  fi

  docker compose version >/dev/null

  mkdir -p \
    /opt/healthapp/current \
    /opt/healthapp/data/postgres \
    /opt/healthapp/data/redis \
    /opt/healthapp/data/kafka
  chown -R ec2-user:ec2-user /opt/healthapp
}

ensure_docker_prereqs

cd "${deploy_root}"

if [[ ! -f "${credentials_path}" ]]; then
  echo "missing AI credentials file at ${credentials_path}" >&2
  exit 1
fi

set -a
. ./.env.aws
set +a

login_ecr_registry() {
  local registry="$1"

  aws ecr get-login-password --region "${AWS_REGION}" \
    | docker login --username AWS --password-stdin "${registry}"
}

declare -A registries=()
registries["${APP_IMAGE%%/*}"]=1
registries["${AI_APP_IMAGE%%/*}"]=1

for registry in "${!registries[@]}"; do
  login_ecr_registry "${registry}"
done

docker compose --env-file .env.aws -f docker-compose.yml pull app ai
docker compose --env-file .env.aws -f docker-compose.yml up -d --remove-orphans
docker image prune -f
