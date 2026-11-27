# CLAUDE.md: sample-spring-app-with-db

Walkthrough of the Spring Boot exercises from chapter 4 of the book "Hands-on Java with Kubernetes": "Integrate Eclipse JKube with Spring Boot" (`k8s:debug`, `k8s:watch`) and "Understanding Skaffold" (`skaffold run`, `skaffold dev`, `skaffold debug`, Helm charts for the database and for the app). Follow the book; the code and the configuration in this directory (`pom.xml`, `src/main/jkube/deployment.yml`, `skaffold.yaml`) are the finished result.

## Prerequisites

- JDK 25, Maven 3.9+, Docker (Testcontainers and Buildpacks), `kubectl`, `minikube` (docker driver), `helm`, `skaffold`.
- A Docker Hub account and `docker login` done. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`. `skaffold.yaml` uses the image `piomin/sample-spring-app-with-db`; replace `piomin` with `<DOCKER_USERNAME>` if it is different (also in `image.repository` of the Helm release).
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.
- The database release must be named `person-db` with the user and database `spring` (the app connects to `person-db-postgresql:5432`). If the Quarkus sample of this chapter installed `person-db` with the user `quarkus`, uninstall it first (ask the user), the credentials would not match.

## Step by step

1. Build and test locally (no side effects, Docker must be running):
   ```shell
   mvn clean verify
   ```
2. Install the database (the JKube exercise needs it first; the book installs it in the Skaffold section with the `postgres` profile, the result is the same):
   ```shell
   helm repo add bitnami https://charts.bitnami.com/bitnami
   helm install person-db bitnami/postgresql --set auth.username=spring --set auth.database=spring
   ```
3. Eclipse JKube (book: "Integrate Eclipse JKube with Spring Boot"). Generate the manifests, build the image and deploy, then debug and watch:
   ```shell
   mvn k8s:resource k8s:build k8s:apply
   mvn k8s:debug         # JDWP on localhost:5005, attach a remote debugger from the IDE
   mvn k8s:watch         # redeploys on changes, needs spring-boot-devtools, excludeDevtools=false and spring.devtools.remote.secret
   ```
   The generated manifests are in `target/classes/META-INF/jkube/kubernetes/`. Remove the JKube deployment before step 4 (`mvn k8s:undeploy`, ask the user first).
4. Skaffold (book: "Understanding Skaffold"). `skaffold.yaml` already exists, skip `skaffold init`. The app is deployed with the `spring-boot-api-app` Helm chart; the database with the `postgres` profile:
   ```shell
   skaffold run --status-check=false -p postgres
   kubectl get pod,svc
   skaffold dev          # rebuilds and redeploys on every change, Ctrl+C removes what it deployed
   ```
   Do not interrupt `skaffold dev` unless the user agrees. `skaffold debug --port-forward` does the same with the JDWP port and the HTTP port forwarded.
5. Verify the app (find the exact `Service` name with `kubectl get svc`):
   ```shell
   kubectl port-forward svc/<SERVICE_NAME> 8080
   curl -X POST http://localhost:8080/persons -H 'Content-Type: application/json' \
     -d '{"firstName":"John","lastName":"Smith","age":30,"gender":"MALE","externalId":1}'
   curl http://localhost:8080/persons
   curl http://localhost:8080/actuator/health/readiness
   ```
   There are no sample data, the first `GET /persons` returns `[]`. `/actuator/health/readiness` must be `UP` only when PostgreSQL is reachable.
6. Clean up only on explicit user request, or after the user confirms at the end of the run:
   ```shell
   skaffold delete -p postgres   # removes the app and the person-db release
   mvn k8s:undeploy              # objects created with JKube
   ```

## Final report

At the end of a run, always print a short summary for the user with these three parts, then ask whether to clean up:

1. **What was built and deployed:** the image (`<DOCKER_USERNAME>/sample-spring-app-with-db`), the app `Deployment` and `Service` (port `8080`) and the PostgreSQL release `person-db`, and whether they came from Skaffold or JKube.
2. **Where it is:** cluster context (`minikube`), namespace (`default`), registry (`docker.io/<DOCKER_USERNAME>`), the database service `person-db-postgresql:5432`, `skaffold.yaml` or `target/classes/META-INF/jkube/kubernetes/`.
3. **How to test it:** `kubectl get pod,svc`, `kubectl port-forward svc/<SERVICE_NAME> 8080`, `curl http://localhost:8080/persons`, `/actuator/health/readiness`, `/swagger-ui.html`, `mvn verify` for the Testcontainers tests. Say which of these you ran and what they returned.

## Rules for agents

- Build and test without side effects first: `mvn clean verify` only needs Docker for Testcontainers.
- Ask the user before anything outward-facing: pushing to Docker Hub (Skaffold pushes by default), deploying to a cluster, installing Helm charts, deleting or recreating a Minikube profile.
- Do not clean up (`kubectl delete`, `skaffold delete`, `helm uninstall`, `mvn k8s:undeploy`) unless the user explicitly asks for it. At the end of a run, ask the user whether to clean up and wait for the answer.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Keep the image name consistent across `skaffold.yaml` (`build.artifacts[].image` and `image.repository`) and the JKube configuration.
- Do not commit `target/`.
- When a step fails or an output differs from the book, fix the code or this file so they agree, and mention it in the report.
