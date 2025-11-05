.PHONY: help init plan apply destroy output kubeconfig

ENV ?= dev
TFVARS := environments/$(ENV)/terraform.tfvars

help:
	@echo "Terraform Makefile for Kubernetes Cluster"
	@echo ""
	@echo "Usage:"
	@echo "  make init          Initialize Terraform"
	@echo "  make plan          Plan changes (ENV=dev|staging|prod)"
	@echo "  make apply         Apply changes (ENV=dev|staging|prod)"
	@echo "  make destroy       Destroy infrastructure (ENV=dev|staging|prod)"
	@echo "  make output        Show outputs"
	@echo "  make kubeconfig    Get kubeconfig"
	@echo ""
	@echo "Examples:"
	@echo "  make plan ENV=dev"
	@echo "  make apply ENV=staging"

init:
	@echo "Initializing Terraform..."
	terraform init -backend-config=backend.hcl

plan:
	@echo "Planning for environment: $(ENV)"
	terraform plan -var-file=$(TFVARS)

apply:
	@echo "Applying for environment: $(ENV)"
	terraform apply -var-file=$(TFVARS)

destroy:
	@echo "⚠️  WARNING: This will destroy all infrastructure for $(ENV)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		terraform destroy -var-file=$(TFVARS); \
	fi

output:
	terraform output

kubeconfig:
	@echo "Getting kubeconfig for $(ENV)..."
	@terraform output -raw kube_config > ~/.kube/config-aks-$(ENV)
	@echo "Kubeconfig saved to ~/.kube/config-aks-$(ENV)"
	@echo "To use: export KUBECONFIG=~/.kube/config-aks-$(ENV)"

fmt:
	terraform fmt -recursive

validate:
	terraform validate

clean:
	rm -rf .terraform
	rm -f .terraform.lock.hcl

