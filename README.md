# WorkHub Enterprise Project

A robust, multi-tenant Spring Boot REST API featuring JWT authentication, project management, asynchronous task processing, and comprehensive observability. Designed with enterprise patterns including multi-tenancy, optimistic locking, and event-driven architecture.

---

## 🛠️ Tech Stack & Features

- **Core**: Java 21, Spring Boot 3.4.0, Spring Security, Spring Data JPA
- **Database**: H2 (Development/Local) & PostgreSQL (Docker/Production)
- **Multi-Tenancy**: Support for isolated tenant schemas and discriminator column strategies
- **Messaging**: Apache Kafka for asynchronous reporting and background jobs
- **Observability**: Micrometer, Prometheus, and Grafana integration
- **Infrastructure**: Docker Compose, Kubernetes manifests, and Terraform scripts
- **Testing**: JUnit 5, Testcontainers (Kafka, DBs)

---

## 🚀 Getting Started

### Prerequisites

| Tool | Minimum version |
|------|----------------|
| Java JDK | 21 |
| Maven | 3.9+ *(or use the included wrapper)* |
| Docker & Docker Compose | Latest |
| IntelliJ IDEA | 2023.1+ (Community or Ultimate) |
| Postman | Any recent version |

### 1. Local Development (H2 In-Memory)

You can run the application directly from your IDE or terminal. By default, it uses an in-memory H2 database (`jdbc:h2:~/testdb`) and requires a local Kafka broker.

```bash
# Start the application
./mvnw spring-boot:run
```

**Note:** For local development without Docker, ensure you have a Kafka broker running on `localhost:9092` or use the Docker Compose setup below.

### 2. Docker Compose (PostgreSQL, Kafka, Prometheus, Grafana)

To run the complete infrastructure locally, use Docker Compose. This spins up PostgreSQL, Kafka, Zookeeper, Prometheus, Grafana, and the WorkHub API.

```bash
# Start all services
docker-compose up -d

# View logs for the application
docker-compose logs -f app
```

- **API Base URL**: `http://localhost:8080`
- **Grafana**: `http://localhost:3000` (User: `admin`, Password: `admin`)
- **Prometheus**: `http://localhost:9090`
- **Postgres**: `localhost:5432`
- **Kafka Broker**: `localhost:9092`

---

## 🔐 Multi-Tenancy & Seeding

The application demonstrates robust multi-tenancy (`spring.tenancy.mode: schema` by default). On the first startup, it automatically seeds three tenants:

| Name | Plan |
|------|------|
| Oscorp Industries | BASIC |
| FawryPay | PRO |
| Initech | ENTERPRISE |

**Getting a Tenant ID for Registration:**
1. Open the H2 Console: **<http://localhost:8080/h2-console>** (if running locally via H2).
   - Driver Class: `org.h2.Driver`
   - JDBC URL: `jdbc:h2:~/testdb`
   - User Name: `sa` / Password: *(leave blank)*
2. Run `SELECT * FROM TENANT;` and copy an `ID` to use during user registration.

---

## 📡 API Usage & Postman

A complete Postman collection is provided in `documents/Enterprise App.postman.json`.

1. Import the collection into Postman.
2. **Register a User:** Use the `Auth / register` endpoint, providing a `tenantId`.
3. **Login:** Use `Auth / login` to receive your JWT token.
4. **Authorize:** Set the token as a Bearer Token in Postman for subsequent requests.
5. Create Projects, add Tasks, and update Task Statuses.

---

## 🧪 Testing Concurrency (Optimistic Locking)

The application handles concurrent data modifications using Optimistic Locking. A Python script is provided to test this mechanism.

```bash
# Ensure the API is running, then execute:
python test_concurrency.py
```

One thread will succeed with a `202 Accepted`, while the concurrent thread will encounter an Optimistic Lock exception and roll back safely.

---

## ☁️ Infrastructure & Deployment

The repository includes configuration for modern infrastructure and deployment platforms:

- **Docker:** `Dockerfile` and `docker-compose.yml` for containerization.
- **Kubernetes:** Manifests (`k8s/`) including Deployments, Services, ConfigMaps, and Secrets.
- **Terraform:** Infrastructure as Code definitions (`terraform/`) for provisioning cloud resources.

For deeper dives into deployment and observability, check the documentation:
- 📖 [Deployment Guide](documents/DEPLOYMENT.md)
- 📊 [Observability Setup](documents/OBSERVABILITY.md)
- 🛡️ [Tenant Isolation Proof](documents/tenant_isolation_proof.md)

---
