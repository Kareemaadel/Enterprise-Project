# WorkHub Deployment Guide

This document outlines the deployment strategy and operational procedures for the WorkHub enterprise application.

## Infrastructure as Code (IaC)
The infrastructure is managed using Terraform and targets a Kubernetes environment.

### Prerequisites
- Terraform CLI (v1.5+)
- kubectl configured with access to your cluster (local or cloud)

### Provisioning
1. Navigate to the `terraform/` directory.
2. Initialize Terraform:
   ```bash
   terraform init
   ```
3. Plan the changes:
   ```bash
   terraform plan
   ```
4. Apply the infrastructure:
   ```bash
   terraform apply
   ```

## Local Development
For local testing and development without a full Kubernetes cluster, use Docker Compose.

```bash
docker-compose up -d
```

## CI/CD Pipeline
The project uses GitHub Actions for continuous integration.
- **Workflow**: `.github/workflows/ci.yml`
- **Actions**: Triggered on push to `main` and all pull requests.
- **Steps**:
  1. Build project with Java 21.
  2. Run unit and integration tests.
  3. Verify security configurations.

## Observability
Monitoring and logging are integrated via Spring Boot Actuator and Prometheus.
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/prometheus`
- **Dashboard**: Grafana (accessible on port 3000 in local setup).

## Scaling
The application is designed for horizontal scaling.
- **App Instances**: Can be scaled via `kubectl scale deployment workhub-app --replicas=3`.
- **Database**: Uses PostgreSQL with connection pooling.
- **Messaging**: Kafka partitions allow for parallel processing of background jobs.
