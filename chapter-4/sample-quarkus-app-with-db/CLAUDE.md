# CLAUDE.md: sample-quarkus-app-with-db

Walkthrough of the Quarkus exercises from chapter 4 of the book "Hands-on Java with Kubernetes": "Quarkus in Development Mode with Kubernetes" (Dev Services, `quarkus:remote-dev`) and "Local Development with Mirrord". Follow the book; the code in this directory is the finished result.

## Prerequisites

- JDK 25, Maven 3.9+, Docker (Quarkus Dev Services start PostgreSQL), `kubectl`, `minikube` (docker driver), `helm`, optional: `mirrord` (`brew install metalbear-co/mirrord/mirrord`, macOS and Linux only).
- A Docker Hub account and `docker login` done. Images go to `docker.io/<DOCKER_USERNAME>/sample-quarkus-app-with-db`. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`.
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.

## Step by step

1. Run locally in development mode, no cluster needed, Dev Services start PostgreSQL (book: "Quarkus in Development Mode with Kubernetes"):
   ```shell
   mvn quarkus:dev
   curl http://localhost:8080/persons
   ```
   `GET /persons` returns the six persons inserted by Liquibase.
2. Install PostgreSQL on the cluster with the release name `person-db` and the user and database `quarkus` (the app connects to `person-db-postgresql:5432`; the generated password lands in the `Secret` `person-db-postgresql`, key `password`):
   ```shell
   helm repo add bitnami https://charts.bitnami.com/bitnami
   helm install person-db bitnami/postgresql --set auth.username=quarkus --set auth.database=quarkus
   ```
   If the Spring sample of this chapter installed `person-db` with the user `spring`, uninstall it first (ask the user), the credentials would not match.
3. Build, push the image and deploy (the `quarkus.kubernetes.*` properties, `mutable-jar` and the live-reload password are already in `application.properties`):
   ```shell
   mvn clean package \
     -Dquarkus.kubernetes.deploy=true \
     -Dquarkus.container-image.group=<DOCKER_USERNAME>
   ```
   Without `quarkus.container-image.group` the image prefix is the OS account name and the `Deployment` points to an image that does not exist. The manifest is `target/kubernetes/kubernetes.yml`.
4. Verify. The `Service` is `NodePort`; with Minikube on the docker driver on macOS open a tunnel with `minikube service sample-quarkus-app-with-db` and use the printed address, or use port-forward:
   ```shell
   kubectl get pod,svc
   kubectl port-forward svc/sample-quarkus-app-with-db 8080:80
   curl http://localhost:8080/persons
   curl http://localhost:8080/q/health/ready
   ```
5. Remote development (book: `quarkus:remote-dev`): start the session with the address of the `NodePort` service (or of the Minikube tunnel), change the code and check that the cluster picks it up:
   ```shell
   mvn quarkus:remote-dev -Dquarkus.live-reload.url=http://127.0.0.1:<PORT>
   ```
6. Mirrord (book: "Local Development with Mirrord", optional). Add `quarkus.datasource.jdbc.url`, `quarkus.datasource.username` and `quarkus.datasource.password` without the `%prod.` prefix to `application.properties` (the three lines in the book), so that Dev Services do not start a local database, then:
   ```shell
   mirrord exec --target deployment/sample-quarkus-app-with-db mvn quarkus:dev
   mirrord exec -f config.json --target deployment/sample-quarkus-app-with-db mvn quarkus:dev
   curl http://<NODEPORT_ADDRESS>/persons -H "X-Forward-To-Local:true"
   ```
   `config.json` forwards only requests with the header `X-Forward-To-Local: true` to the local process. Revert the three properties afterwards, otherwise Dev Services stop starting the local database.
7. Integration test against the deployment (only after step 3):
   ```shell
   mvn verify -Dit.test=PersonTestOnKubernetesIT
   ```
8. Clean up only on explicit user request, or after the user confirms at the end of the run:
   ```shell
   kubectl delete -f target/kubernetes/kubernetes.yml
   ```

## Final report

At the end of a run, always print a short summary for the user with these three parts, then ask whether to clean up:

1. **What was built and deployed:** the image (`<DOCKER_USERNAME>/sample-quarkus-app-with-db:1.0-SNAPSHOT`), the `Deployment`, `Service` (`NodePort`), liquibase init `Job` and RBAC objects generated from the extensions, the PostgreSQL release `person-db`, and whether a remote-dev or Mirrord session was started.
2. **Where it is:** cluster context (`minikube`), namespace (`default`), registry (`docker.io/<DOCKER_USERNAME>`), the manifest file `target/kubernetes/kubernetes.yml`, the database service `person-db-postgresql:5432`.
3. **How to test it:** `kubectl get pod,job,svc`, `kubectl port-forward svc/sample-quarkus-app-with-db 8080:80`, `curl http://localhost:8080/persons`, `/q/health/ready`, `/q/metrics`, `/q/swagger-ui`, `mvn verify -Dit.test=PersonTestOnKubernetesIT`. Say which of these you ran and what they returned.

## Rules for agents

- Build and test without side effects first: `mvn clean package -DskipTests` changes nothing on the cluster. Do not set `quarkus.kubernetes.deploy` (or the `kubernetes` Maven property, which activates the `kubernetes` profile with deploy) unless the deploy is intended. `mvn verify` runs `PersonTestOnKubernetesIT`, which needs the deployed app, so do not run it before the deploy.
- Ask the user before anything outward-facing: pushing to Docker Hub, deploying to a cluster, installing Helm charts, starting Mirrord (it creates an agent pod in the cluster), deleting or recreating a Minikube profile.
- Do not clean up (`kubectl delete`, `helm uninstall`) unless the user explicitly asks for it. At the end of a run, ask the user whether to clean up and wait for the answer.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Do not commit `target/`.
- When a step fails or an output differs from the book, fix the code or this file so they agree, and mention it in the report.
