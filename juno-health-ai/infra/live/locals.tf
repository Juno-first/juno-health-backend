locals {
  environment = terraform.workspace == "default" ? "staging" : terraform.workspace
  name_prefix = "${var.project_name}-${local.environment}"
  subdomain   = local.environment == "prod" ? "api" : "staging.api"
  fqdn        = "${local.subdomain}.${var.root_domain}"

  frontend_origin = local.environment == "prod" ? var.prod_frontend_origin : var.staging_frontend_origin

  app_port = 8080

  common_tags = {
    Project   = var.project_name
    ManagedBy = "terraform"
    Workspace = local.environment
  }
}
