variable "name_prefix" {
  type = string
}

variable "subnet_id" {
  type = string
}

variable "ec2_sg_id" {
  type = string
}

variable "ami_id" {
  type = string
}

variable "instance_type" {
  type = string
}

variable "root_volume_size" {
  type = number
}

variable "app_image_tag" {
  type = string
}

variable "deploy_bucket_name" {
  type = string
}

variable "ecr_repository_arn" {
  type = string
}

variable "aws_region" {
  type = string
}

variable "frontend_origin" {
  type = string
}
