output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "target_group_arn" {
  value = aws_lb_target_group.app.arn
}

output "app_target_group_arn" {
  value = aws_lb_target_group.app.arn
}

output "ai_target_group_arn" {
  value = aws_lb_target_group.ai.arn
}
