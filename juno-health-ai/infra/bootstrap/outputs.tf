output "state_bucket_name" {
  value       = aws_s3_bucket.terraform_state.bucket
  description = "Terraform remote state bucket name."
}

output "lock_table_name" {
  value       = aws_dynamodb_table.terraform_lock.name
  description = "Terraform remote state lock table name."
}
