locals {
  environment   = terraform.workspace == "default" ? "staging" : terraform.workspace
  name_prefix   = "${var.project_name}-${local.environment}"
  app_subdomain = local.environment == "prod" ? "api" : "staging.api"
  app_fqdn      = "${local.app_subdomain}.${var.root_domain}"
  ai_subdomain  = local.environment == "prod" ? "ai" : "staging.ai"
  ai_fqdn       = "${local.ai_subdomain}.${var.root_domain}"

  frontend_origin = local.environment == "prod" ? var.prod_frontend_origin : var.staging_frontend_origin

  app_port = 8080
  ai_port  = 8000

  common_tags = {
    Project   = var.project_name
    ManagedBy = "terraform"
    Workspace = local.environment
  }
}
