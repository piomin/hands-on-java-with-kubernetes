# CLAUDE.md: sample-spring-app-with-db

Chapter 4 sample of the book "Java with Kubernetes in Action": a Spring Boot + PostgreSQL REST app used to show the inner development loop on Kubernetes with Eclipse JKube (`k8s:debug`, `k8s:watch`) and Skaffold (`skaffold dev`, `skaffold debug`, Helm charts for the app and the database).

## What this app demonstrates

- Spring Boot 4.1.x + Java 25 + Maven, Spring Data JPA, PostgreSQL. `/persons` REST API: `POST`, `PUT`, `DELETE /{id}`, `GET`, `GET /{id}`, `GET /last-name/{lastName}`, `GET /age-greater-than/{age}`. The `Person` entity has `id`, `firstName`, `lastName`, `age`, `gender`, `externalId`. SpringDoc OpenAPI UI is included (`/swagger-ui.html`).
- The schema is created by Hibernate (`spring.jpa.hibernate.ddl-auto: create`), there are no sample data. `GET /persons` returns `[]` on a fresh database.
- Database configuration from environment variables: `DATABASE_NAME`, `DATABASE_USER`, `DATABASE_PASS`; the URL is `jdbc:postgresql://person-db-postgresql:5432/${DATABASE_NAME}`. The password comes from the `Secret` `person-db-postgresql` (key `password`) created by the Bitnami PostgreSQL chart.
- Actuator exposes only `health` and `prometheus` (`management.endpoints.web.exposure.include`), health details are always shown, the readiness group contains only `db`, probes are enabled. Everything stays on the main port `8080`: the `spring-boot-api-app` chart has its probes on the `http` port (`/actuator/health/liveness`, `/actuator/health/readiness`), so a separate management port (as in chapter 3) would break it.
- Graceful shutdown: `server.shutdown=graceful` is the default, `spring.lifecycle.timeout-per-shutdown-phase: 20s`. The JKube fragment adds `terminationGracePeriodSeconds: 30`, a `preStop` `sleep` of 5 s and a startup probe; the Helm chart does not support them.
- Resources (requests `384Mi`/`200m`, limits `740Mi`/`2`, the same as in chapter 3) are set in the JKube fragment and in `skaffold.yaml` (`resources.*` values of the chart). The `containerResources` option of the JKube plugin is not applied by version 1.20.0, so do not use it.
- Eclipse JKube (`kubernetes-maven-plugin`): `src/main/jkube/deployment.yml` adds `DATABASE_PASS` from the `Secret`, resources, the shutdown settings and the startup probe, the plugin configuration in `pom.xml` sets `DATABASE_USER=spring`, `DATABASE_NAME=spring`. `mvn k8s:debug` opens a JDWP session on port `5005`, `mvn k8s:watch` redeploys on changes (needs `spring-boot-devtools`, `excludeDevtools=false` and `spring.devtools.remote.secret`).
- Skaffold (`skaffold.yaml`, `skaffold/v4beta13`): the image `piomin/sample-spring-app-with-db` is built with Paketo Buildpacks (`paketobuildpacks/builder-noble-java-tiny`, `BP_JVM_VERSION=25`) and deployed with the `spring-boot-api-app` Helm chart from `https://piomin.github.io/helm-charts`. The profile `postgres` installs the Bitnami `postgresql` chart `16.0.1` as the release `person-db` (user and database `spring`). `local.push: false` is commented out, so the image is pushed to Docker Hub.
- Tests: `PersonControllerIT` (Testcontainers `postgres:17`, `RestTestClient`, Instancio), run by Failsafe on `mvn verify`.

## Prerequisites

- JDK 25, Maven 3.9+, Docker (Testcontainers and Buildpacks), `kubectl`, `minikube` (docker driver), `helm`, `skaffold`.
- A Docker Hub account and `docker login` done. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`. `skaffold.yaml` uses the image `piomin/sample-spring-app-with-db`; replace `piomin` with `<DOCKER_USERNAME>` if it is different (also in `image.repository` of the Helm release).
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.
- The database release must be named `person-db` (the app connects to `person-db-postgresql:5432`). If the Quarkus sample of this chapter installed `person-db` with the user `quarkus`, uninstall it first (ask the user) or the credentials will not match.

## Step by step

1. Build and test locally (no side effects, Docker must be running):
   ```shell
   mvn clean verify
   ```
   `PersonControllerIT` starts PostgreSQL with Testcontainers and runs 4 tests.
2. Install the database and deploy the app with Skaffold (preferred):
   ```shell
   skaffold run --status-check=false -p postgres
   kubectl get pod,svc
   skaffold dev          # optional: rebuild and redeploy on every change, Ctrl+C removes the deployed app
   ```
   `skaffold dev` stops and cleans up the objects it deployed when interrupted; do not interrupt it unless the user agrees.
3. Alternative: Eclipse JKube. Install the database first (`helm install person-db bitnami/postgresql --set auth.username=spring --set auth.database=spring`), then:
   ```shell
   mvn k8s:resource k8s:build k8s:apply
   mvn k8s:debug         # JDWP on localhost:5005
   mvn k8s:watch         # needs spring.devtools.remote.secret in application.properties
   ```
   The generated manifests are in `target/classes/META-INF/jkube/kubernetes/`.
4. Verify the app and the probes (find the exact `Deployment` and `Service` names with `kubectl get deploy,svc`):
   ```shell
   kubectl port-forward svc/<SERVICE_NAME> 8080
   curl -X POST http://localhost:8080/persons -H 'Content-Type: application/json' \
     -d '{"firstName":"John","lastName":"Smith","age":30,"gender":"MALE","externalId":1}'
   curl http://localhost:8080/persons
   curl http://localhost:8080/actuator/health/readiness
   curl http://localhost:8080/actuator/prometheus | head
   ```
   `/actuator/health/readiness` must be `UP` only when PostgreSQL is reachable.
5. Debug with Skaffold (optional): `skaffold debug --port-forward` forwards the JDWP port and the HTTP port.
6. Clean up only on explicit user request, or after the user confirms at the end of the run:
   ```shell
   skaffold delete -p postgres   # removes the app and the person-db release
   ```
   Objects created with JKube are removed with `mvn k8s:undeploy`.

## Final report

At the end of a run, always print a short summary for the user with these three parts, then ask whether to clean up:

1. **What was built and deployed:** the image (`<DOCKER_USERNAME>/sample-spring-app-with-db`), the app `Deployment` and `Service` (port `8080`) and the PostgreSQL release `person-db`, and whether they came from Skaffold or JKube.
2. **Where it is:** cluster context (`minikube`), namespace (`default`), registry (`docker.io/<DOCKER_USERNAME>`), the database service `person-db-postgresql:5432`, `skaffold.yaml` or `target/classes/META-INF/jkube/kubernetes/`.
3. **How to test it:** `kubectl get pod,svc`, `kubectl port-forward svc/<SERVICE_NAME> 8080`, `curl http://localhost:8080/persons`, `/actuator/health/readiness`, `/swagger-ui.html`, and `mvn verify` for the Testcontainers tests. Say which of these you ran and what they returned.

## Differences from the book

The book shows only the key parts. The code also has `spring.jpa.hibernate.ddl-auto: create`, `show_sql`, the shutdown and resources settings, the JKube plugin configuration and the tests. These are intentional, do not remove them to match the text. Illustrative output in the book (image owner, tags, ports) comes from a real run and is left as is.

## Rules for agents

- Build and test without side effects first: `mvn clean verify` only needs Docker for Testcontainers.
- Ask the user before anything outward-facing: pushing to Docker Hub (Skaffold pushes by default), deploying to a cluster, installing Helm charts, deleting or recreating a Minikube profile.
- Do not clean up (`kubectl delete`, `skaffold delete`, `helm uninstall`, `mvn k8s:undeploy`) unless the user explicitly asks for it. At the end of a run, ask the user whether to clean up and wait for the answer.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Keep the image name consistent across `skaffold.yaml` (`build.artifacts[].image` and `image.repository`) and the JKube configuration.
- Do not commit `target/`.
- When a step fails or an output differs from what is described here, fix the code or this file so they agree, and mention it in the report.
