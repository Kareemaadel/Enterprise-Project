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

variable "stable_image" {
  type        = string
  default     = "workhub:1.0.0"
  description = "Docker image for the stable WorkHub app"
}

variable "canary_image" {
  type        = string
  default     = "workhub:2.0.0"
  description = "Docker image for the canary WorkHub app"
}

variable "total_replicas" {
  type        = number
  default     = 5
  description = "Total number of app replicas to run"
}

variable "canary_ratio" {
  type        = number
  default     = 0.20
  description = "Percentage of pods running the canary release"
}
