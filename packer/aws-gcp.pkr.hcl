packer {
  required_plugins {
    amazon = {
      version = ">= 1.0.0"
      source  = "github.com/hashicorp/amazon"
    }
    googlecompute = {
      version = ">= 1.0.0"
      source  = "github.com/hashicorp/googlecompute"
    }
  }
}

# Variables
variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "aws_source_ami" {
  type    = string
  default = "ami-0e2c8caa4b6378d8c" # Ubuntu 24.04 LTS in us-east-1
}

variable "aws_instance_type" {
  type    = string
  default = "t2.micro"
}

variable "aws_dev_account_id" {
  type    = string
  default = "163285046203"
}

variable "aws_demo_account_id" {
  type    = string
  default = "506160144092"
}

variable "gcp_project_id" {
  type    = string
  default = "weihong-dev"
}

variable "gcp_demo_project_id" {
  type    = string
  default = "weihong-demo"
}

variable "gcp_zone" {
  type    = string
  default = "us-east1-b"
}

variable "gcp_source_image_family" {
  type    = string
  default = "ubuntu-2404-lts"
}

variable "gcp_machine_type" {
  type    = string
  default = "e2-medium"
}

variable "ssh_username" {
  type    = string
  default = "ubuntu"
}

# AWS Builder
source "amazon-ebs" "webapp" {
  ami_name      = "csye6225-webapp-{{timestamp}}"
  instance_type = var.aws_instance_type
  region        = var.aws_region
  source_ami    = var.aws_source_ami
  ssh_username  = var.ssh_username

  ami_users = [var.aws_demo_account_id] # Share with demo account

  ami_description = "Custom AMI for CSYE 6225 web application"

  tags = {
    Name        = "csye6225-webapp"
    Environment = "dev"
    Created_by  = "packer"
  }

  launch_block_device_mappings {
    device_name           = "/dev/sda1"
    volume_size           = 25
    volume_type           = "gp2"
    delete_on_termination = true
  }
}

# GCP Builder
source "googlecompute" "webapp" {
  project_id              = var.gcp_project_id
  source_image_family     = "ubuntu-2404-lts-amd64"
  source_image_project_id = ["ubuntu-os-cloud"]
  zone                    = var.gcp_zone
  machine_type            = var.gcp_machine_type
  ssh_username            = var.ssh_username

  # Network configuration
  network    = "default"
  subnetwork = ""

  # Ensure external IP is assigned for SSH access
  use_internal_ip  = false
  omit_external_ip = false
  ssh_timeout      = "10m"

  # Add tags to match firewall rules
  tags = ["allow-ssh", "packer-build"]

  # Image configuration
  image_name        = "csye6225-webapp-{{timestamp}}"
  image_description = "Custom image for CSYE 6225 web application"
  image_family      = "csye6225-webapp"

  disk_size = 25
  disk_type = "pd-balanced"

  image_labels = {
    environment = "dev"
    created_by  = "packer"
  }
}

# Build configuration
build {
  sources = [
    "source.amazon-ebs.webapp",
    "source.googlecompute.webapp"
  ]

  # Copy application JAR
  provisioner "file" {
    source      = "../target/webapp-0.0.1-SNAPSHOT.jar"
    destination = "/tmp/webapp.jar"
  }

  # Copy setup script
  provisioner "file" {
    source      = "../scripts/setup.sh"
    destination = "/tmp/setup.sh"
  }

  # Copy systemd service file
  provisioner "file" {
    source      = "../scripts/webapp.service"
    destination = "/tmp/webapp.service"
  }

  # Run setup script
  provisioner "shell" {
    inline = [
      "chmod +x /tmp/setup.sh",
      "sudo /tmp/setup.sh"
    ]
  }

  # Install systemd service
  provisioner "shell" {
    inline = [
      "sudo cp /tmp/webapp.service /etc/systemd/system/webapp.service",
      "sudo systemctl daemon-reload",
      "sudo systemctl enable webapp.service",
      "echo '[INFO] Systemd service installed and enabled'"
    ]
  }

  # Cleanup
  provisioner "shell" {
    inline = [
      "sudo rm -f /tmp/webapp.jar",
      "sudo rm -f /tmp/setup.sh",
      "sudo rm -f /tmp/webapp.service",
      "echo '[INFO] Cleanup completed'"
    ]
  }
}
