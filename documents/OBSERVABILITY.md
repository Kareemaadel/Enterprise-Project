# Observability Documentation - WorkHub

This document outlines the observability features implemented in the WorkHub project, including health checks, metrics, and distributed tracing via Correlation IDs.

## 1. Health Endpoints (Spring Actuator)

The application uses Spring Boot Actuator to expose health and monitoring information.

| Feature | Endpoint | Description |
| :--- | :--- | :--- |
| **Main Health** | `/actuator/health` | Overall system health status with full details. |
| **Liveness** | `/actuator/health/liveness` | Kubernetes liveness probe: indicates if the app is alive. |
| **Readiness** | `/actuator/health/readiness` | Kubernetes readiness probe: indicates if the app is ready to serve traffic. |

### How to Verify:
1. Start the application.
2. Run `curl http://localhost:8080/actuator/health`.
3. **Expected Result**: A JSON response showing `UP` status and detailed component health (Disk, DB, etc.).
4. Run `curl http://localhost:8080/actuator/health/liveness`.
5. **Expected Result**: `{"status":"UP"}`.

---

## 2. Metrics & Prometheus

Metrics are collected via Micrometer and exposed in a format compatible with Prometheus.

| Feature | Endpoint | Description |
| :--- | :--- | :--- |
| **Prometheus** | `/actuator/prometheus` | Metrics formatted for Prometheus scraping. |
| **Generic Metrics** | `/actuator/metrics` | List of all available metric names. |

### Custom Metric: Report Generation
We have implemented a custom counter to track report generation per tenant.
- **Metric Name**: `workhub.reports.generated`
- **Tags**: `tenantId`

### How to Verify:
1. Trigger a report generation (via `ReportService`).
2. Run `curl http://localhost:8080/actuator/prometheus`.
3. **Expected Result**: Search for `workhub_reports_generated_total` in the output. You should see the count incremented with the corresponding `tenantId`.

---

## 3. Distributed Tracing (Correlation ID)

Every request is assigned a unique Correlation ID to track its flow through the logs.

### Features:
- **Header**: `X-Correlation-ID`
- **MDC Key**: `correlationId`
- **Auto-injection**: If a request doesn't provide a header, a new UUID is generated.
- **Response**: The ID is returned in the response headers for client-side tracking.

### How to Verify:
1. Send any request to the API:
   ```bash
   curl -i http://localhost:8080/projects
   ```
2. **Check Response Headers**: Look for `X-Correlation-ID: <uuid>`.
3. **Check Application Logs**:
   - The log pattern is: `%d{yyyy-MM-dd HH:mm:ss} [%thread] [correlationId=%X{correlationId}] %-5level %logger{36} - %msg%n`
   - **Expected Result**: Every log line generated during that request will include `[correlationId=...]`.

---

## 4. Logging Configuration

The application uses `logback-spring.xml` to standardize log output.

- **File**: `src/main/resources/logback-spring.xml`
- **Appender**: Console
- **Level**: `INFO` (Root)

### Log Pattern Example:
`2026-05-01 01:10:45 [http-nio-8080-exec-1] [correlationId=a1b2c3d4-e5f6-7890] INFO  com.example.WorkHub.service.ProjectService - Creating new project: Project Alpha`
