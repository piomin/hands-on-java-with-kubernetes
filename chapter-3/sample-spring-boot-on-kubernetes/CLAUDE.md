# CLAUDE.md: sample-spring-boot-on-kubernetes

Chapter 3 sample of the book "Java with Kubernetes in Action": a Spring Boot + MongoDB REST app that follows the best practices for running Java apps on Kubernetes: health probes on a separate management port, graceful shutdown, externalized configuration (`ConfigMap`/`Secret`), Prometheus metrics, resource requests and limits, and container image builds without a Dockerfile.

## What this app demonstrates

- Spring Boot 4.1.x + Java 25 + Maven, `/persons` REST API (`POST`, `PUT`, `DELETE`, `GET`, `GET /{id}`, `GET /age/{age}`, `GET /age-greater-than/{age}`, `GET /first-name/{firstName}/last-name/{lastName}`) backed by MongoDB (`spring-boot-starter-data-mongodb`). Sample data is inserted on startup when the collection is empty.
- Actuator on a separate management port `8081` (`management.server.port`), with only `health` and `prometheus` exposed (`management.endpoints.web.exposure.include`).
- Liveness probe on `8081` (`/actuator/health/liveness`), readiness probe on the main port `8080` (`/readiness`, `additional-path: server:/readiness`) that includes only the `mongo` health indicator, startup probe on `8081`.
- Graceful shutdown: `server.shutdown=graceful` is the default, `spring.lifecycle.timeout-per-shutdown-phase: 20s`, a `preStop` `sleep` of 5 s and `terminationGracePeriodSeconds: 30` in the `Deployment`.
- Configuration of MongoDB from environment variables (`MONGO_URL`, `MONGO_USERNAME`, `MONGO_PASSWORD`, `MONGO_DATABASE`) taken from a `ConfigMap` and a `Secret` defined in `k8s/mongodb-deployment.yaml`.
- Prometheus metrics at `/actuator/prometheus` on port `8081`, scraped through the `ServiceMonitor` in `k8s/servicemonitor.yaml` (named port `mgmt` of the `Service`).
- Resources: requests `384Mi`/`200m`, limits `740Mi`/`2`. `BPL_JVM_THREAD_COUNT=50` is set for Paketo Buildpacks in `pom.xml`; JVM flags in `deployment.yaml` are commented out on purpose.
- Image builds: Cloud Native Buildpacks (`spring-boot:build-image`), or Jib (profile `jib`) used by Skaffold.
- Load tests with k6 in `src/test/resources/k6`.

## Prerequisites

- JDK 25, Maven 3.9+, Docker (needed by Testcontainers and by Buildpacks), `kubectl`, `minikube` (docker driver).
- Optional: `skaffold` (build and deploy), `helm` (monitoring stack), `k6` (load tests).
- A Docker Hub account and `docker login` done. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`. The manifests and `pom.xml` use the image `piomin/sample-spring-boot-on-kubernetes:1.0-SNAPSHOT`; replace `piomin` with `<DOCKER_USERNAME>` if it is different.
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.

## Step by step

1. Build and test locally (no side effects, Docker must be running):
   ```shell
   mvn clean verify
   ```
   `PersonControllerTestIT` starts MongoDB with Testcontainers (`mongo:8.2`). Run the app locally with a containerized MongoDB: `mvn spring-boot:test-run`.
2. Build the image and push it to Docker Hub (needs the login), with Buildpacks:
   ```shell
   mvn clean spring-boot:build-image -DskipTests \
     -Dspring-boot.build-image.imageName=<DOCKER_USERNAME>/sample-spring-boot-on-kubernetes:1.0-SNAPSHOT \
     -Dspring-boot.build-image.publish=true
   ```
   Alternatively use Skaffold, which builds the image with the Jib profile and deploys both manifests: `skaffold run`.
3. Deploy MongoDB and the app (skip if Skaffold did it):
   ```shell
   kubectl apply -f k8s/mongodb-deployment.yaml
   kubectl apply -f k8s/deployment.yaml
   kubectl rollout status deploy/sample-spring-boot-on-kubernetes-deployment
   ```
4. Verify the app, the probes and the metrics:
   ```shell
   kubectl get pod
   kubectl port-forward svc/sample-spring-boot-on-kubernetes 8080 8081
   curl http://localhost:8080/persons
   curl http://localhost:8080/readiness
   curl http://localhost:8081/actuator/health/liveness
   curl http://localhost:8081/actuator/prometheus | head
   ```
   `GET /persons` returns the five sample persons. `/readiness` on 8080 must be `UP` only when MongoDB is reachable.
5. Prometheus (optional, needs the `kube-prometheus-stack` Helm release in the `monitoring` namespace):
   ```shell
   kubectl apply -f k8s/servicemonitor.yaml
   kubectl port-forward svc/kube-prometheus-stack-prometheus 9090 -n monitoring
   ```
   The target `sample-spring-boot-on-kubernetes` should appear in Status -> Target health. The `ServiceMonitor` needs the label `release: kube-prometheus-stack`.
6. Load test and graceful shutdown check (optional):
   ```shell
   k6 run -e PERSONS_URI=http://localhost:8080 src/test/resources/k6/load-tests-add.js
   ```
   While k6 is running, trigger `kubectl rollout restart deploy/sample-spring-boot-on-kubernetes-deployment` in another terminal. The error rate should stay at 0, because the pod finishes active requests before it stops.
7. Clean up after a test:
   ```shell
   kubectl delete -f k8s/servicemonitor.yaml -f k8s/deployment.yaml -f k8s/mongodb-deployment.yaml
   ```

## Rules for agents

- Build and test without side effects first: `mvn clean verify` only needs Docker for Testcontainers.
- Ask the user before anything outward-facing: pushing to Docker Hub, deploying to a cluster, installing Helm charts, deleting or recreating a Minikube profile.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Keep the image tag consistent across `pom.xml` (`${project.version}`), `k8s/deployment.yaml` and `skaffold.yaml`.
- Do not commit `target/`.
- When a step fails or an output differs from what is described here, fix the code or this file so they agree, and mention it in the report.
