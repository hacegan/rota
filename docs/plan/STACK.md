# Rota Stack

Binding stack decision for Rota. This file records what is used and why; resolved versions
live in `VERSIONS.md`. Changing the stack requires an ADR and Samet's explicit approval.

## Backend (Java)

- Java 27 (Temurin) — language and runtime baseline for the entire backend.
- Maven wrapper — reproducible builds without depending on a global Maven install.
- Spring Boot — application framework: dependency injection, auto-configuration, actuator.
- Spring Web — REST endpoints for the API phase.
- Spring Data JPA/Hibernate — relational persistence and ORM for employees, shifts, rosters.
- Bean Validation — declarative validation of incoming requests and inputs.
- Flyway — versioned, repeatable database migrations.
- PostgreSQL — primary relational store for scheduled data.
- Timefold Solver — constraint-solving engine for shift optimization.
- Kafka — event streaming for schedule changes and downstream integrations.
- RabbitMQ — asynchronous task queue for decoupled background work.
- Redis — caching, sessions, and distributed coordination.
- MongoDB — document store for flexible, semi-structured data.
- Elasticsearch — full-text search and roster indexing.
- Spring AI — integration point for LLM-assisted features.
- gRPC — internal service-to-service RPC.
- Testcontainers — integration tests against real PostgreSQL, Kafka, and friends.
- JUnit — unit and integration test framework.
- Mockito — mocking collaborators in tests.
- ArchUnit — enforce architecture rules as tests.

## Later phases

- Next.js/React/TypeScript — frontend only (schedule board); never the backend runtime.
- Docker Compose — run the full local stack for development.
- LocalStack — emulate AWS services in tests.
- Kubernetes/Helm/ArgoCD/Istio — deployment, packaging, GitOps, and service mesh.
