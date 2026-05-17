variable "kubeconfig_path" {
  type        = string
  default     = "~/.kube/config"
  description = "Path to kubeconfig file"
}

variable "kube_context" {
  type        = string
  default     = "minikube"
  description = "Kubernetes context to use"
}

variable "namespace" {
  type        = string
  default     = "workhub"
  description = "Kubernetes namespace"
}

variable "db_password" {
  type        = string
  sensitive   = true
  description = "Database password"
}

variable "app_image" {
  type        = string
  default     = "workhub:latest"
  description = "Docker image for the WorkHub app"
}

variable "replicas" {
  type        = number
  default     = 2
  description = "Number of app replicas"
}
