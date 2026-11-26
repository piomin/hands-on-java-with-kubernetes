# CLAUDE.md: sample-quarkus-on-kubernetes

Chapter 3 sample of the book "Java with Kubernetes in Action": a Quarkus + PostgreSQL REST app that follows the best practices for running Java apps on Kubernetes: JSON logs, health probes, externalized configuration, Prometheus metrics, resource requests and limits, and a build that generates the manifests and the image without a Dockerfile.

## What this app demonstrates

- Quarkus 3.x (platform version in the parent `pom.xml`) + Java 25 + Maven, Hibernate ORM with Panache, PostgreSQL, Liquibase (`db/changeLog.sql`, `quarkus.liquibase.migrate-at-start = true`).
- `/persons` REST API: `POST`, `GET`, `GET /{id}`, `GET /name/{name}`, `GET /age-greater-than/{age}`. OpenAPI and Swagger UI are included.
- JSON console logs in the `prod` profile only (`quarkus-logging-json`, `%prod.quarkus.log.console.json.enabled = true`).
- SmallRye Health: `/q/health/live`, `/q/health/ready`, `/q/health/started`. The Kubernetes extension generates the liveness, readiness and startup probes from them.
- Micrometer Prometheus metrics at `/q/metrics` on port `8080`. The Kubernetes extension adds the `prometheus.io/*` annotations and generates the `ServiceMonitor`.
- Graceful shutdown: `quarkus.shutdown.timeout = 20s` (without it Quarkus does not wait for active requests).
- Configuration through `quarkus.kubernetes.*`: labels, annotations, env variables (database user and name as plain values, password from the `my-db-postgresql` `Secret`, key `postgres-password`), resources (requests `64Mi`/`200m`, limits `512Mi`/`1000m`).
- Manifests are generated into `target/kubernetes/kubernetes.yml` (`Service` on port 80, `Deployment`, `ServiceAccount`, `Role`, `RoleBinding`, a Liquibase init `Job` and a `ServiceMonitor`). The image is built and pushed by Jib.

## Prerequisites

- JDK 25, Maven 3.9+, Docker (Quarkus Dev Services starts PostgreSQL for the tests), `kubectl`, `minikube` (docker driver), `helm`.
- A Docker Hub account and `docker login` done. Images go to `docker.io/<DOCKER_USERNAME>/sample-quarkus-on-kubernetes`. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`.
- PostgreSQL on the cluster, installed with the release name `my-db` (the app connects to `my-db-postgresql:5432`):
  ```shell
  helm install my-db bitnami/postgresql
  ```
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.

## Step by step

1. Build and run the tests without deploying (Docker must be running):
   ```shell
   mvn clean package -DskipTests        # compiles and generates target/kubernetes/kubernetes.yml
   mvn verify -Dit.test=PersonResourceIT
   ```
   `PersonTestOnKubernetesIT` checks the `Deployment` and the pod on the cluster, so it only passes after step 2.
2. Build, push the image and deploy:
   ```shell
   mvn clean package \
     -Dquarkus.kubernetes.deploy=true \
     -Dquarkus.container-image.group=<DOCKER_USERNAME>
   ```
   Without `quarkus.container-image.group` the image prefix is the OS account name and the `Deployment` points to an image that does not exist.
3. Verify the app, the probes and the metrics:
   ```shell
   kubectl get pod
   kubectl port-forward svc/sample-quarkus-on-kubernetes 8080:80
   curl http://localhost:8080/persons
   curl http://localhost:8080/q/health/ready
   curl http://localhost:8080/q/metrics | head
   ```
   `GET /persons` returns the persons inserted by Liquibase. Swagger UI is at `/q/swagger-ui`.
4. Integration test against the deployment:
   ```shell
   mvn verify -Dit.test=PersonTestOnKubernetesIT
   ```
5. Prometheus (optional, needs the `kube-prometheus-stack` Helm release in the `monitoring` namespace): with `quarkus-micrometer-registry-prometheus` the Kubernetes extension generates the `ServiceMonitor` (in `target/kubernetes/kubernetes.yml`) together with the `prometheus.io/*` annotations. `kube-prometheus-stack` only picks it up with the label `release: kube-prometheus-stack`, add it with `quarkus.kubernetes.labels.release = kube-prometheus-stack`. Then:
   ```shell
   kubectl port-forward svc/kube-prometheus-stack-prometheus 9090 -n monitoring
   ```
   and check Status -> Target health.
6. Clean up after a test:
   ```shell
   kubectl delete -f target/kubernetes/kubernetes.yml
   ```

## Rules for agents

- Build and test without side effects first: `mvn clean package -DskipTests` and `mvn verify -Dit.test=PersonResourceIT` change nothing on the cluster. Do not set `quarkus.kubernetes.deploy` unless the deploy is intended.
- Ask the user before anything outward-facing: pushing to Docker Hub, deploying to a cluster, installing Helm charts, deleting or recreating a Minikube profile.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Do not commit `target/`.
- When a step fails or an output differs from what is described here, fix the code or this file so they agree, and mention it in the report.
