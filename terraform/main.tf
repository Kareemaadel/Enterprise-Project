terraform {
  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.27"
    }
  }
}

provider "kubernetes" {
  config_path    = var.kubeconfig_path
  config_context = var.kube_context
}

resource "kubernetes_namespace" "workhub" {
  metadata {
    name = var.namespace
  }
}

resource "kubernetes_config_map" "workhub_config" {
  metadata {
    name      = "workhub-config"
    namespace = kubernetes_namespace.workhub.metadata[0].name
  }

  data = {
    SPRING_DATASOURCE_URL               = "jdbc:postgresql://postgres-service:5432/workhub"
    SPRING_DATASOURCE_USERNAME          = "workhub_user"
    SPRING_DATASOURCE_DRIVER_CLASS_NAME = "org.postgresql.Driver"
    SPRING_JPA_DATABASE_PLATFORM        = "org.hibernate.dialect.PostgreSQLDialect"
    SPRING_KAFKA_BOOTSTRAP_SERVERS      = "kafka-service:9092"
    SPRING_JPA_HIBERNATE_DDL_AUTO       = "update"
  }
}

resource "kubernetes_secret" "workhub_secret" {
  metadata {
    name      = "workhub-secret"
    namespace = kubernetes_namespace.workhub.metadata[0].name
  }

  data = {
    SPRING_DATASOURCE_PASSWORD = var.db_password
  }

  type = "Opaque"
}

resource "kubernetes_deployment" "workhub_app_stable" {
  metadata {
    name      = "workhub-app-stable"
    namespace = kubernetes_namespace.workhub.metadata[0].name
    labels = {
      app   = "workhub"
      track = "stable"
    }
  }

  spec {
    replicas = floor(var.total_replicas * (1 - var.canary_ratio))

    selector {
      match_labels = {
        app   = "workhub"
        track = "stable"
      }
    }

    template {
      metadata {
        labels = {
          app   = "workhub"
          track = "stable"
        }
      }

      spec {
        container {
          image             = var.stable_image
          image_pull_policy = "Never"
          name              = "workhub"

          port {
            container_port = 8080
          }

          env_from {
            config_map_ref {
              name = kubernetes_config_map.workhub_config.metadata[0].name
            }
          }

          env {
            name = "SPRING_DATASOURCE_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.workhub_secret.metadata[0].name
                key  = "SPRING_DATASOURCE_PASSWORD"
              }
            }
          }

          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = 8080
            }
            initial_delay_seconds = 45
            period_seconds        = 15
            failure_threshold     = 3
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = 8080
            }
            initial_delay_seconds = 30
            period_seconds        = 10
            failure_threshold     = 3
          }

          resources {
            requests = {
              cpu    = "250m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_deployment" "workhub_app_canary" {
  metadata {
    name      = "workhub-app-canary"
    namespace = kubernetes_namespace.workhub.metadata[0].name
    labels = {
      app   = "workhub"
      track = "canary"
    }
  }

  spec {
    replicas = ceil(var.total_replicas * var.canary_ratio)

    selector {
      match_labels = {
        app   = "workhub"
        track = "canary"
      }
    }

    template {
      metadata {
        labels = {
          app   = "workhub"
          track = "canary"
        }
      }

      spec {
        container {
          image             = var.canary_image
          image_pull_policy = "Never"
          name              = "workhub"

          port {
            container_port = 8080
          }

          env_from {
            config_map_ref {
              name = kubernetes_config_map.workhub_config.metadata[0].name
            }
          }

          env {
            name = "SPRING_DATASOURCE_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret.workhub_secret.metadata[0].name
                key  = "SPRING_DATASOURCE_PASSWORD"
              }
            }
          }

          liveness_probe {
            http_get {
              path = "/actuator/health/liveness"
              port = 8080
            }
            initial_delay_seconds = 45
            period_seconds        = 15
            failure_threshold     = 3
          }

          readiness_probe {
            http_get {
              path = "/actuator/health/readiness"
              port = 8080
            }
            initial_delay_seconds = 30
            period_seconds        = 10
            failure_threshold     = 3
          }

          resources {
            requests = {
              cpu    = "250m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "workhub_service" {
  metadata {
    name      = "workhub-service"
    namespace = kubernetes_namespace.workhub.metadata[0].name
  }

  spec {
    selector = {
      app = "workhub"
    }

    port {
      port        = 80
      target_port = 8080
    }

    type = "LoadBalancer"
  }
}
