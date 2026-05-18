output "namespace" {
  value       = kubernetes_namespace.workhub.metadata[0].name
  description = "Kubernetes namespace created"
}

output "app_service_name" {
  value       = kubernetes_service.workhub_service.metadata[0].name
  description = "Name of the LoadBalancer service"
}

output "app_replicas" {
  value       = var.total_replicas
  description = "Number of replicas deployed"
}
