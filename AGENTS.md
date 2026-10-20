# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## Overview

`mod-kb-ebsco-java` is a FOLIO backend module (Java 21, Maven) that brokers between FOLIO and the EBSCO HoldingsIQ (RM API) knowledge base: providers, packages, titles, resources, holdings, custom labels, access types, tags, assigned users, KB credentials, and Usage Consolidation (cost-per-use). It is built on **RestMicroserviceBuilder (RMB)** + Vert.x, with Spring for dependency injection. The external HoldingsIQ client lives in the separate `folio-holdingsiq-client` library (version pinned in `pom.xml`; currently a SNAPSHOT, so it may need to be built/installed locally from `../folio-holdingsiq-client`).

## Commands

```shell
mvn clean install                      # full build: RAML codegen, AspectJ weaving, checkstyle, tests, fat jar
mvn clean install -DskipTests          # build without tests
mvn test -Dtest=SomeTest               # single test class
mvn test -Dtest=SomeTest#method        # single test method
mvn checkstyle:check                   # style check (also runs automatically in process-classes; violations fail the build)
docker build -t mod-kb-ebsco-java .    # container; fat jar is target/mod-kb-ebsco-java-fat.jar
```

Surefire only picks up `Test*.java`, `*Test.java`, `*Tests.java`, `*TestCase.java`.

## Build pipeline gotchas

- **API interfaces are generated from RAML.** `ramls/` (a git submodule, see `.gitmodules`; `exec-maven-plugin` runs `git submodule update --init --recursive` at `initialize`) plus JSON schemas under `ramls/types/**` are fed to `domain-models-maven-plugin`, which generates JAX-RS interfaces and POJOs into `target/generated-sources/raml-jaxrs`. Changing an endpoint or model means editing the RAML/schema, then rebuilding — don't hand-edit generated code.
- **AspectJ weaving** (`aspectj-maven-plugin`) applies to `**/impl/*.java` and `*.aj` (e.g. `rest/aspect/ValidationErrorHandlerAspect.aj`, `@HandleValidationErrors`). Builds must go through Maven for aspects to be woven; IDE-only compilation may miss them.
- Lombok and Vert.x `CodeGenProcessor` run as annotation processors; Lombok is `provided`.
- `descriptors/*-template.json` are filtered into `target/ModuleDescriptor.json` / `DeploymentDescriptor.json` and validated by `folio-module-descriptor-validator`. New endpoints/permissions must be added to the ModuleDescriptor template as well as the RAML.
- Checkstyle uses the FOLIO config (`folio-java-checkstyle`) over both main and test sources, with `failOnViolation` on warnings.

## Architecture

Request flow: **generated RAML interface → `rest/impl/Eholdings*Impl` → services → repositories / HoldingsIQ client**, with converters mapping between layers.

- `rest/impl/` — RMB resource implementations, one per RAML resource (`EholdingsPackagesImpl`, `EholdingsTitlesImpl`, `LoadHoldingsImpl`, `TenantApiImpl`, `InitApiImpl`, …). Thin: validate, resolve KB credentials, delegate, convert result, map errors via `rest/util/ErrorHandler` / `ExceptionMappers`.
- `rest/validator/` — request-body/param validators; `rest/model/filter` and `rest/model/query` hold search filter/query models (e.g. package subject filters/facets).
- `rest/converter/` — organized per domain; convert HoldingsIQ models and DB models to the generated API models and back. Facet converters are under `converter/facets`.
- `rmapi/` — services that wrap the HoldingsIQ client for providers/packages/titles/resources (`*ServiceImpl`), plus `result/` aggregate types that combine RM API data with local data (tags, access types, cost-per-use) and `cache/` keys for the Vert.x/Spring-backed caches.
- `service/` — business logic per domain (access types, assigned users, custom labels, KB credentials, holdings load/status, Usage Consolidation under `service/uc`, `loader/` for filtering and related-entity loading). Holdings loading is split into `LoadServiceFacade` variants (default vs. transaction-based) with scheduled status/audit handling.
- `repository/` — PostgreSQL access via RMB `PostgresClient`; each domain has a `*Repository(Impl)`, `Db*` model, and `*TableConstants`. Schema is managed by Liquibase (`folio-liquibase-util`) and applied from `TenantApiImpl` / holdings-load flows.
- `client/uc/` — HTTP clients for the external Usage Consolidation (Apigee/Auth) APIs; configured in `spring/config/UcConfig.java`.
- `spring/config/ApplicationConfig.java` — Spring wiring for the whole module; `folio-di-support` is used so RMB resource impls can `@Autowired` beans.

Multi-tenancy and credentials: calls are tenant-scoped via Okapi headers. Most RM API calls first look up the tenant's KB credentials (`KbCredentials*`, `UserKbCredentialsService`) to build a HoldingsIQ client configuration.

## Tests

Tests live in `src/test/java/org/folio/{common,repository,rest,rmapi,service,spring,util}`. Fixtures are under `src/test/resources/{requests,responses,mappings}` (WireMock stubs in `mappings/` for the RM API / UC / user lookup). Stack: JUnit 5, Mockito, AssertJ, RestAssured, WireMock, vertx-junit5, easy-random, and `postgres-testing` (embedded/containerized Postgres for repository and API tests).
