#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <output-tar-gz> <runtime-env-file> <credentials-json>" >&2
  exit 1
fi

output_archive="$1"
runtime_env_file="$2"
credentials_file="$3"

if [[ ! -f "${runtime_env_file}" ]]; then
  echo "runtime env file not found: ${runtime_env_file}" >&2
  exit 1
fi

if [[ ! -f "${credentials_file}" ]]; then
  echo "credentials file not found: ${credentials_file}" >&2
  exit 1
fi

temp_dir="$(mktemp -d)"
trap 'rm -rf "${temp_dir}"' EXIT

cp docker-compose.yml "${temp_dir}/docker-compose.yml"
cp "${runtime_env_file}" "${temp_dir}/.env.aws"
cp scripts/host-deploy.sh "${temp_dir}/host-deploy.sh"
cp "${credentials_file}" "${temp_dir}/credentials.json"

tar -C "${temp_dir}" -czf "${output_archive}" .
