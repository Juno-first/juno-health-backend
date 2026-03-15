variable "aws_region" {
  description = "AWS region for all resources."
  type        = string
}

variable "root_domain" {
  description = "Root domain name already hosted in Route53."
  type        = string
}

variable "hosted_zone_id" {
  description = "Route53 hosted zone ID for the root domain."
  type        = string
}

variable "instance_type" {
  description = "EC2 instance type for the compose host."
  type        = string
  default     = "t3.large"
}

variable "app_image_tag" {
  description = "Current application image tag being deployed."
  type        = string
}

variable "staging_frontend_origin" {
  description = "Allowed CORS origin for staging."
  type        = string
}

variable "prod_frontend_origin" {
  description = "Allowed CORS origin for production."
  type        = string
}

variable "project_name" {
  description = "Name prefix for created resources."
  type        = string
  default     = "healthapp"
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB for the compose host."
  type        = number
  default     = 80
}
