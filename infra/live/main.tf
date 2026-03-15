data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023*-x86_64"]
  }
}

module "network" {
  source = "./modules/network"

  name_prefix = local.name_prefix
  app_port    = local.app_port
  ai_port     = local.ai_port
}

module "artifacts" {
  source = "./modules/artifacts"

  name_prefix = local.name_prefix
}

module "edge" {
  source = "./modules/edge"

  name_prefix    = local.name_prefix
  vpc_id         = module.network.vpc_id
  subnet_ids     = module.network.public_subnet_ids
  alb_sg_id      = module.network.alb_security_group_id
  hosted_zone_id = var.hosted_zone_id
  app_fqdn       = local.app_fqdn
  ai_fqdn        = local.ai_fqdn
  app_port       = local.app_port
  ai_port        = local.ai_port
}

module "host" {
  source = "./modules/host"

  name_prefix        = local.name_prefix
  subnet_id          = module.network.public_subnet_ids[0]
  ec2_sg_id          = module.network.ec2_security_group_id
  ami_id             = data.aws_ami.amazon_linux.id
  instance_type      = var.instance_type
  root_volume_size   = var.root_volume_size
  app_image_tag      = var.app_image_tag
  deploy_bucket_name = module.artifacts.deploy_bucket_name
  ecr_repository_arns = [
    module.artifacts.ecr_repository_arn,
    module.artifacts.ai_ecr_repository_arn,
  ]
  aws_region      = var.aws_region
  frontend_origin = local.frontend_origin
}

resource "aws_lb_target_group_attachment" "app" {
  target_group_arn = module.edge.app_target_group_arn
  target_id        = module.host.instance_id
  port             = local.app_port
}

resource "aws_lb_target_group_attachment" "ai" {
  target_group_arn = module.edge.ai_target_group_arn
  target_id        = module.host.instance_id
  port             = local.ai_port
}
