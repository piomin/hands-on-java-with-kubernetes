# CLAUDE.md: sample-quarkus-app-with-db

Chapter 4 sample of the book "Java with Kubernetes in Action": a Quarkus + PostgreSQL REST app used to show the inner development loop on Kubernetes: Quarkus Dev Services locally, remote development (`quarkus:remote-dev`) against a deployment on the cluster, and Mirrord (`mirrord exec`) to run the app locally with the cluster's network and environment.

## What this app demonstrates

- Quarkus 3.x (platform version in the root `pom.xml`) + Java 25 + Maven, Hibernate ORM with Panache, PostgreSQL. `/persons` REST API: `POST`, `GET`, `GET /{id}`, `GET /name/{name}`, `GET /age-greater-than/{age}`. OpenAPI and Swagger UI are always included (`quarkus.swagger-ui.always-include`).
- The schema and six sample persons are created by Liquibase (`quarkus-liquibase`, `quarkus.liquibase.migrate-at-start = true`, `db/changeLog.sql`, table `person` only); Hibernate does not touch the schema (`quarkus.hibernate-orm.schema-management.strategy = none`). `externalId` is `null` in the sample data.
- Database configuration in the `prod` profile from environment variables `POSTGRES_USERNAME`, `POSTGRES_PASSWORD`, `POSTGRES_DB`; the URL is `jdbc:postgresql://person-db-postgresql:5432/${POSTGRES_DB}`. In dev and test mode Dev Services start PostgreSQL in a container, so nothing is configured.
- JSON console logs in the `prod` profile only (`quarkus.log.console.json.enabled = false`, `%prod.quarkus.log.console.json.enabled = true`).
- SmallRye Health: `/q/health/live`, `/q/health/ready`, `/q/health/started`; the Kubernetes extension generates the three probes. Micrometer Prometheus metrics at `/q/metrics`; the extension adds the `prometheus.io/*` annotations. The `ServiceMonitor` is switched off (`quarkus.kubernetes.prometheus.generate-service-monitor = false`) so that the deploy does not need the `ServiceMonitor` CRD; remove that property when `kube-prometheus-stack` is installed.
- Graceful shutdown: `quarkus.shutdown.timeout = 20s`. Resources (requests `64Mi`/`200m`, limits `512Mi`/`1000m`, the same as in chapter 3) through `quarkus.kubernetes.resources.*`.
- Configuration through `quarkus.kubernetes.*`: `Service` type `NodePort` (port `80` -> `8080`), label `env=dev`, annotation `app-type=demo`, env variables (`POSTGRES_USERNAME` and `POSTGRES_DB` equal to `quarkus`, `POSTGRES_PASSWORD` from the `Secret` `person-db-postgresql`, key `password`).
- Remote development: `quarkus.package.jar.type = mutable-jar`, `quarkus.live-reload.password = abc123`, env `QUARKUS_LAUNCH_DEVMODE=true`. Manifests are generated into `target/kubernetes/kubernetes.yml`: `Service`, `Deployment`, and, because of `quarkus-liquibase`, the `ServiceAccount`, `Role`, `RoleBinding` and a `Job` `sample-quarkus-app-with-db-liquibase-init` that the `Deployment` waits for with an init container. Do not write them by hand. The image is built and pushed by Jib (`quarkus-container-image-jib`).
- `config.json` is the Mirrord configuration (`steal` mode, only requests with the header `X-Forward-To-Local: true` go to the local process).
- Tests (`quarkus-junit`): `PersonTestOnKubernetesIT` (`@QuarkusTest`, fabric8 client, checks the `Deployment` and exactly one pod on the cluster). `PersonResourceIT` and `PersonRepositoryIT` are fully commented out.

## Prerequisites

- JDK 25, Maven 3.9+, Docker (Quarkus Dev Services start PostgreSQL), `kubectl`, `minikube` (docker driver), `helm`, optional: `mirrord` (`brew install metalbear-co/mirrord/mirrord`, macOS and Linux only).
- A Docker Hub account and `docker login` done. Images go to `docker.io/<DOCKER_USERNAME>/sample-quarkus-app-with-db`. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`.
- PostgreSQL on the cluster, installed with the release name `person-db` and the user and database `quarkus` (the app connects to `person-db-postgresql:5432`):
  ```shell
  helm repo add bitnami https://charts.bitnami.com/bitnami
  helm install person-db bitnami/postgresql --set auth.username=quarkus --set auth.database=quarkus
  ```
  If the Spring sample of this chapter installed `person-db` with the user `spring`, uninstall it first (ask the user) or the credentials will not match.
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.

## Step by step

1. Run locally in development mode (no cluster needed, Dev Services start PostgreSQL):
   ```shell
   mvn quarkus:dev
   curl http://localhost:8080/persons
   ```
   `GET /persons` returns the six persons inserted by Liquibase. Build without deploying: `mvn clean package -DskipTests` (generates `target/kubernetes/kubernetes.yml`).
2. Build, push the image and deploy:
   ```shell
   mvn clean package \
     -Dquarkus.kubernetes.deploy=true \
     -Dquarkus.container-image.group=<DOCKER_USERNAME>
   ```
   Without `quarkus.container-image.group` the image prefix is the OS account name and the `Deployment` points to an image that does not exist. The image tag is the project version (`1.0-SNAPSHOT`).
3. Verify the app and the probes. The `Service` is `NodePort`; on Minikube with the docker driver on macOS open a tunnel with `minikube service sample-quarkus-app-with-db` and use the printed address, or use port-forward:
   ```shell
   kubectl get pod,svc
   kubectl port-forward svc/sample-quarkus-app-with-db 8080:80
   curl http://localhost:8080/persons
   curl http://localhost:8080/q/health/ready
   ```
   Swagger UI is at `/q/swagger-ui`.
4. Remote development (optional): with the app deployed in step 2, start the session with the address of the `NodePort` service (or of the Minikube tunnel) and change the code:
   ```shell
   mvn quarkus:remote-dev -Dquarkus.live-reload.url=http://127.0.0.1:<PORT>
   ```
5. Mirrord (optional): the `prod`-only datasource properties are not used in dev mode, so for this exercise add `quarkus.datasource.jdbc.url`, `quarkus.datasource.username` and `quarkus.datasource.password` without the `%prod.` prefix (see the book), then:
   ```shell
   mirrord exec --target deployment/sample-quarkus-app-with-db mvn quarkus:dev
   mirrord exec -f config.json --target deployment/sample-quarkus-app-with-db mvn quarkus:dev
   curl http://<NODEPORT_ADDRESS>/persons -H "X-Forward-To-Local:true"
   ```
   Revert the three properties afterwards, otherwise Dev Services stop starting the local database.
6. Integration test against the deployment (only after step 2):
   ```shell
   mvn verify -Dit.test=PersonTestOnKubernetesIT
   ```
7. Clean up only on explicit user request, or after the user confirms at the end of the run:
   ```shell
   kubectl delete -f target/kubernetes/kubernetes.yml
   ```

## Final report

At the end of a run, always print a short summary for the user with these three parts, then ask whether to clean up:

1. **What was built and deployed:** the image (`<DOCKER_USERNAME>/sample-quarkus-app-with-db:1.0-SNAPSHOT`), the `Deployment`, `Service` (`NodePort`), liquibase init `Job` and RBAC objects generated from the extensions, the PostgreSQL release `person-db`, and whether a remote-dev or Mirrord session was started.
2. **Where it is:** cluster context (`minikube`), namespace (`default`), registry (`docker.io/<DOCKER_USERNAME>`), the manifest file `target/kubernetes/kubernetes.yml`, the database service `person-db-postgresql:5432`.
3. **How to test it:** `kubectl get pod,job,svc`, `kubectl port-forward svc/sample-quarkus-app-with-db 8080:80`, `curl http://localhost:8080/persons`, `/q/health/ready`, `/q/metrics`, `/q/swagger-ui`, `mvn verify -Dit.test=PersonTestOnKubernetesIT`. Say which of these you ran and what they returned.

## Differences from the book

The book shows only the key parts. The code also has Liquibase, the shutdown and resources settings, the Jib extension and the tests. These are intentional, do not remove them to match the text. The book adds the `Logger` to `PersonResource` only for the local Mirrord exercise; the code already has it permanently. Illustrative output in the book (image owner, tags such as `1.0`, ports) comes from a real run and is left as is. `PersonResourceIT` and `PersonRepositoryIT` are commented out on purpose.

## Rules for agents

- Build and test without side effects first: `mvn clean package -DskipTests` changes nothing on the cluster. Do not set `quarkus.kubernetes.deploy` (or the `kubernetes` Maven property, which activates the `kubernetes` profile with deploy) unless the deploy is intended. `mvn verify` runs `PersonTestOnKubernetesIT`, which needs the deployed app, so do not run it before the deploy.
- Ask the user before anything outward-facing: pushing to Docker Hub, deploying to a cluster, installing Helm charts, starting Mirrord (it creates an agent pod in the cluster), deleting or recreating a Minikube profile.
- Do not clean up (`kubectl delete`, `helm uninstall`) unless the user explicitly asks for it. At the end of a run, ask the user whether to clean up and wait for the answer.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Do not commit `target/`.
- When a step fails or an output differs from what is described here, fix the code or this file so they agree, and mention it in the report.
