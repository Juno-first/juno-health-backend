bucket         = "juno-backend-state"
key            = "healthapp/live/terraform.tfstate"
region         = "us-east-1"
dynamodb_table = "juno-backend-tf-lock"
encrypt        = true
