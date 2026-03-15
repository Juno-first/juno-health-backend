output "alb_dns_name" {
  description = "DNS name of the public load balancer."
  value       = module.edge.alb_dns_name
}

output "public_api_url" {
  description = "Public HTTPS URL for the deployed backend."
  value       = "https://${local.app_fqdn}"
}

output "public_ai_url" {
  description = "Public HTTPS URL for the deployed AI service."
  value       = "https://${local.ai_fqdn}"
}

output "instance_id" {
  description = "EC2 instance ID running the compose stack."
  value       = module.host.instance_id
}

output "ecr_repository_url" {
  description = "ECR repository URL for the backend image."
  value       = module.artifacts.ecr_repository_url
}

output "ai_ecr_repository_url" {
  description = "ECR repository URL for the AI image."
  value       = module.artifacts.ai_ecr_repository_url
}

output "ai_ecr_repository_arn" {
  description = "ECR repository ARN for the AI image."
  value       = module.artifacts.ai_ecr_repository_arn
}

output "deploy_bucket_name" {
  description = "S3 bucket used to publish deployment bundles."
  value       = module.artifacts.deploy_bucket_name
}
