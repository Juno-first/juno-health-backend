variable "name_prefix" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  type = list(string)
}

variable "alb_sg_id" {
  type = string
}

variable "hosted_zone_id" {
  type = string
}

variable "fqdn" {
  type = string
}

variable "app_port" {
  type = number
}
