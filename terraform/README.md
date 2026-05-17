# Terraform IaC for WorkHub — Kubernetes Track (Track 2)

## Prerequisites
- Terraform >= 1.5
- kubectl
- minikube (or any k8s cluster)
- kubeconfig configured

## Usage
```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars and set your db_password
terraform init
terraform plan
terraform apply
```

## Destroy
`terraform destroy`

## Resources Created
- `kubernetes_namespace`
- `kubernetes_config_map`
- `kubernetes_secret`
- `kubernetes_deployment`
- `kubernetes_service`
