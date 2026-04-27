# Zipkin Setup Plan for Kubernetes Cluster (Review First)

## 1) Objective

Set up distributed tracing with Zipkin for the fashion namespace services in Kubernetes, so cross-service requests can be traced end-to-end (Kong -> backend services -> async hops where applicable).

This document is only a plan. No setup actions are executed yet.

## 2) Current State Summary

- Kubernetes namespace is already standardized as `fashion`.
- Service deployments are defined in one main manifest file: `k8s/microservices.yaml`.
- Gateway is Kong in DB-less mode via `k8s/kong.yaml`.
- Spring Cloud Config is present in backend services, but tracing dependencies are not currently present.
- `spring-boot-starter-actuator` is only found in `config-service` at the moment.

## 3) Scope

In scope:
- Deploy Zipkin inside Kubernetes namespace `fashion`.
- Add tracing dependencies and config to Java services:
  - user-service
  - product-service
  - cart-service
  - order-service
  - promotion-service
  - review-service
  - chatbot-service
- Add Kubernetes environment configuration so services export traces to Zipkin.
- Add validation and rollback procedure.

Out of scope (phase after approval):
- Full observability stack (Prometheus, Grafana, Tempo, Loki).
- Long-term trace storage with Elasticsearch/MySQL (unless explicitly requested).

## 4) Proposed Target Architecture

- Zipkin runs as a Deployment + ClusterIP Service in namespace `fashion`.
- Internal endpoint for services: `http://zipkin:9411/api/v2/spans`.
- Zipkin UI access options (decision needed):
  - Option A (recommended first): internal only via `kubectl port-forward`.
  - Option B: external route via Kong/Ingress with access restriction.
- Java services send tracing spans through Micrometer Tracing (Brave bridge) to Zipkin.
- Sampling:
  - Non-prod: `1.0` for easier troubleshooting.
  - Prod: `0.1` to reduce overhead.

## 5) Implementation Plan (Phased)

### Phase 0 - Review and decisions

1. Confirm UI exposure mode (internal only vs public route).
2. Confirm sampling for production.
3. Confirm whether `config-service` also needs trace export.

Deliverable:
- Approved plan and selected options.

### Phase 1 - Kubernetes resources for Zipkin

Planned file changes:
- Add new file: `k8s/zipkin.yaml`

Planned resources:
- Deployment `zipkin` (1 replica, image `openzipkin/zipkin:3` or latest stable).
- Service `zipkin` (ClusterIP, port 9411).
- Optional resource requests/limits for low-resource VPS.
- Optional liveness/readiness probes.

Acceptance criteria:
- `kubectl get pods -n fashion` shows Zipkin running.
- `kubectl get svc -n fashion` shows `zipkin` service on port 9411.

### Phase 2 - Backend dependencies for tracing

Planned file changes (per service pom):
- `backend/services/*-service/pom.xml`

Dependencies to add in each traced service:
- `org.springframework.boot:spring-boot-starter-actuator`
- `io.micrometer:micrometer-tracing-bridge-brave`
- `io.zipkin.reporter2:zipkin-reporter-brave`

Acceptance criteria:
- Maven build succeeds for all updated services.
- No dependency conflict with Spring Boot 3.2.x / Spring Cloud 2023.0.x.

### Phase 3 - Application tracing configuration

Configuration strategy (recommended):
- Add tracing config to config-repo service files where applicable.
- Keep endpoint override possible via environment variable.

Planned config keys:
- `management.tracing.sampling.probability`
- `management.zipkin.tracing.endpoint`
- `management.endpoints.web.exposure.include` (minimal safe set)
- Logging pattern includes trace and span IDs.

Likely files:
- `backend/config-repo/user-service.yml`
- `backend/config-repo/product-service.yml`
- `backend/config-repo/cart-service.yml`
- `backend/config-repo/order-service.yml`
- `backend/config-repo/promotion-service.yml`
- `backend/config-repo/review-service.yml`
- `backend/config-repo/chatbot-service.yml`

Acceptance criteria:
- Services start successfully with tracing settings.
- Trace/span identifiers appear in service logs.

### Phase 4 - Kubernetes env wiring for runtime

Planned file changes:
- `k8s/microservices.yaml`
- (Optional) `k8s/configmaps.yaml`

Planned env wiring (if needed for K8s override):
- `MANAGEMENT_ZIPKIN_TRACING_ENDPOINT=http://zipkin:9411/api/v2/spans`
- `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=1.0` (non-prod)

Acceptance criteria:
- Pods restart cleanly.
- Services emit spans to Zipkin.

### Phase 5 - Verification and smoke tests

Validation flow:
1. Generate traffic through Kong routes:
   - auth/user/product/cart/order/promotion/review/chatbot APIs
2. Open Zipkin UI and confirm traces include multiple services in one trace tree.
3. Verify error traces are visible for failed requests.

Acceptance criteria:
- At least one end-to-end trace contains 3+ services.
- Trace IDs in logs match Zipkin trace entries.

### Phase 6 - Rollback and safety

Rollback options:
- Disable tracing export by env override:
  - `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0`
- Roll back manifests:
  - remove/apply previous `k8s/zipkin.yaml` and `k8s/microservices.yaml`
- Roll back dependency/config commits if startup issues occur.

## 6) Risk Assessment

Key risks:
- Increased CPU/memory usage when sampling too high in production.
- Startup issues if dependency versions drift.
- Over-exposed Zipkin UI if published publicly without access control.

Mitigations:
- Start with internal-only UI.
- Keep conservative resource limits.
- Set production sampling to 0.1 (or lower based on load).

## 7) Definition of Done

Done when all conditions are met:
- Zipkin is deployed and stable in namespace `fashion`.
- All selected services successfully export spans.
- End-to-end traces are visible for real request paths.
- Rollback procedure is tested and documented.

## 8) Approval Checklist (for your review)

Please confirm before implementation:

1. UI exposure mode:
   - [ ] Internal only (port-forward)
   - [ ] Public via Kong/Ingress (restricted)
2. Production sampling:
   - [ ] 0.1
   - [ ] Custom value: ______
3. Include `config-service` in tracing scope:
   - [ ] Yes
   - [ ] No
4. Keep initial Zipkin storage as in-memory:
   - [ ] Yes
   - [ ] No (specify backend)
