#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <output-tar-gz> <runtime-env-file>" >&2
  exit 1
fi

output_archive="$1"
runtime_env_file="$2"

if [[ ! -f "${runtime_env_file}" ]]; then
  echo "runtime env file not found: ${runtime_env_file}" >&2
  exit 1
fi

temp_dir="$(mktemp -d)"
trap 'rm -rf "${temp_dir}"' EXIT

cp docker-compose.yml "${temp_dir}/docker-compose.yml"
cp docker-compose.aws.yml "${temp_dir}/docker-compose.aws.yml"
cp "${runtime_env_file}" "${temp_dir}/.env.aws"
cp scripts/host-deploy.sh "${temp_dir}/host-deploy.sh"

tar -C "${temp_dir}" -czf "${output_archive}" .
