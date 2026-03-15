#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF' >&2
usage: verify-aws-prereqs.sh --bucket <bucket> --lock-table <table> --root-domain <domain> [--region <region>]

Verifies:
  - AWS CLI credentials are configured
  - Terraform state bucket exists
  - Terraform lock table exists
  - A Route53 hosted zone exists for the root domain
EOF
  exit 1
}

bucket=""
lock_table=""
root_domain=""
region=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --bucket)
      bucket="${2:-}"
      shift 2
      ;;
    --lock-table)
      lock_table="${2:-}"
      shift 2
      ;;
    --root-domain)
      root_domain="${2:-}"
      shift 2
      ;;
    --region)
      region="${2:-}"
      shift 2
      ;;
    *)
      usage
      ;;
  esac
done

if [[ -z "${bucket}" || -z "${lock_table}" || -z "${root_domain}" ]]; then
  usage
fi

aws_args=()
if [[ -n "${region}" ]]; then
  aws_args+=(--region "${region}")
fi

echo "Checking AWS identity..."
aws "${aws_args[@]}" sts get-caller-identity >/dev/null

echo "Checking Terraform state bucket: ${bucket}"
aws "${aws_args[@]}" s3api head-bucket --bucket "${bucket}"

echo "Checking Terraform lock table: ${lock_table}"
aws "${aws_args[@]}" dynamodb describe-table --table-name "${lock_table}" >/dev/null

echo "Checking Route53 hosted zone for: ${root_domain}"
hosted_zone_json="$(aws "${aws_args[@]}" route53 list-hosted-zones-by-name --dns-name "${root_domain}")"
hosted_zone_id="$(printf '%s\n' "${hosted_zone_json}" | sed -n 's|.*"Id": "/hostedzone/\([^"]*\)".*|\1|p' | head -n 1)"
hosted_zone_name="$(printf '%s\n' "${hosted_zone_json}" | sed -n 's|.*"Name": "\([^"]*\)".*|\1|p' | head -n 1)"

if [[ -z "${hosted_zone_id}" ]]; then
  echo "No hosted zone found for ${root_domain}" >&2
  exit 1
fi

echo "Found hosted zone:"
echo "  name: ${hosted_zone_name}"
echo "  id:   ${hosted_zone_id}"
echo
echo "Use these values in GitHub variables:"
echo "  ROOT_DOMAIN=${root_domain}"
echo "  HOSTED_ZONE_ID=${hosted_zone_id}"
