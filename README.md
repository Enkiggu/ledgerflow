# LedgerFlow

LedgerFlow is a Java 21 payment and order infrastructure project focused on monetary precision, explicit lifecycle rules, double-entry accounting, idempotency and reliable event processing.

## Highlights

- integer minor-unit money arithmetic with currency checks;
- explicit order and payment state machines;
- balanced double-entry ledger validation;
- idempotent payment and refund operations;
- transactional outbox publishing and consumer deduplication;
- signed webhook processing and rate limiting;
- Flyway migrations for PostgreSQL;
- Prometheus metrics and a provisioned Grafana dashboard;
- unit, integration, concurrency and end-to-end tests.

## Architecture

The application keeps order, payment, ledger and messaging responsibilities in separate packages. A payment is recorded together with its ledger transaction and outbox events in one database transaction. Background publishers deliver pending events to RabbitMQ, while consumers keep processed-event records to tolerate repeated delivery.

## Local development

Requirements: Java 21 and Maven 3.9+.

Run the complete test suite:

```bash
mvn test
```

Run the service with its in-memory development profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API documentation is available at `http://localhost:8888/swagger-ui.html` in the development profile.

## Container stack

Docker Compose starts the application, PostgreSQL, Redis, RabbitMQ, Prometheus and Grafana. Define these values in your shell before starting the stack:

- `POSTGRES_PASSWORD`
- `LEDGERFLOW_ADMIN_API_KEY`
- `LEDGERFLOW_WEBHOOK_SECRET`
- `GRAFANA_ADMIN_PASSWORD`

Then run:

```bash
docker compose up --build
```

Service endpoints:

- application: `http://localhost:8080`
- API documentation: `http://localhost:8080/swagger-ui.html`
- RabbitMQ management: `http://localhost:15672`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Operational checks

The `load-tests` directory contains k6 scenarios for order creation, payment idempotency, concurrency and rate limiting. The `monitoring` directory contains ready-to-use Prometheus and Grafana configuration.

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE).
