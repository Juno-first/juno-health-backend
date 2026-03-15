output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "ecr_repository_arn" {
  value = aws_ecr_repository.app.arn
}

output "ai_ecr_repository_url" {
  value = aws_ecr_repository.ai.repository_url
}

output "ai_ecr_repository_arn" {
  value = aws_ecr_repository.ai.arn
}

output "deploy_bucket_name" {
  value = aws_s3_bucket.deploy.bucket
}
