# CLAUDE.md: sample-quarkus-on-kubernetes

Walkthrough of the Quarkus exercises from chapter 3 of the book "Java with Kubernetes in Action" (`../../../java-with-kuberneters-in-action-book/chapter-03`): JSON logs, health probes, graceful shutdown, resource requests and limits, and Prometheus metrics with a generated `ServiceMonitor`. Follow the book; the code in this directory is the finished result. The Quarkus Kubernetes extension generates all manifests into `target/kubernetes/kubernetes.yml`, do not write them by hand, change them with `quarkus.kubernetes.*` properties and regenerate with `mvn clean package -DskipTests`.

## Prerequisites

- JDK 25, Maven 3.9+, Docker (Quarkus Dev Services start PostgreSQL for the tests), `kubectl`, `minikube` (docker driver), `helm`.
- A Docker Hub account and `docker login` done. Images go to `docker.io/<DOCKER_USERNAME>/sample-quarkus-on-kubernetes`. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`.
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.

## Step by step

1. Install the monitoring stack first, the `ServiceMonitor` CRD comes with it (book: "Collecting Metrics with Prometheus"), and PostgreSQL with the release name `my-db` (the app connects to `my-db-postgresql:5432`):
   ```shell
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
   kubectl get pod -n monitoring
   helm repo add bitnami https://charts.bitnami.com/bitnami
   helm install my-db bitnami/postgresql
   ```
2. Build and run the tests without deploying (Docker must be running):
   ```shell
   mvn clean package -DskipTests        # compiles and generates target/kubernetes/kubernetes.yml
   mvn verify -Dit.test=PersonResourceIT
   ```
   `PersonTestOnKubernetesIT` checks the `Deployment` and the pod on the cluster, so it only passes after step 3.
3. Build, push the image and deploy:
   ```shell
   mvn clean package \
     -Dquarkus.kubernetes.deploy=true \
     -Dquarkus.container-image.group=<DOCKER_USERNAME>
   ```
   Without `quarkus.container-image.group` the image prefix is the OS account name and the `Deployment` points to an image that does not exist. The extensions in the `pom.xml` determine what is generated: `quarkus-liquibase` adds a `Job` that runs the migration (the `Deployment` waits for it in an init container) with its `ServiceAccount`, `Role` and `RoleBinding`; `quarkus-micrometer-registry-prometheus` adds a `ServiceMonitor` and the `prometheus.io/*` annotations; `quarkus-smallrye-health` adds the liveness, readiness and startup probes.
4. Verify the app, the probes and the metrics (book: "Application Logs", "Liveness and Readiness Health Checks", "Collecting Metrics"):
   ```shell
   kubectl get pod,job
   kubectl port-forward svc/sample-quarkus-on-kubernetes 8080:80
   curl http://localhost:8080/persons
   curl http://localhost:8080/q/health/ready
   curl http://localhost:8080/q/metrics | head
   ```
   `GET /persons` returns the persons inserted by Liquibase. The pod logs are JSON in the deployed (`prod`) profile (`kubectl logs deploy/sample-quarkus-on-kubernetes`). Swagger UI is at `/q/swagger-ui`.
5. Graceful shutdown and resources (book: "Graceful Shutdown", "Properly Define Resource Requests and Limits"): check in `target/kubernetes/kubernetes.yml` and with `kubectl describe pod` the requests/limits from the book (`64Mi`/`200m`, `512Mi`/`1000m`) and `quarkus.shutdown.timeout = 20s`.
6. Integration test against the deployment:
   ```shell
   mvn verify -Dit.test=PersonTestOnKubernetesIT
   ```
7. Prometheus: `kube-prometheus-stack` only picks up the generated `ServiceMonitor` with the label `release: kube-prometheus-stack`; add it with `quarkus.kubernetes.labels.release = kube-prometheus-stack` and redeploy (step 3). Then:
   ```shell
   kubectl port-forward svc/kube-prometheus-stack-prometheus 9090 -n monitoring
   ```
   and check Status -> Target health.
8. Clean up only on explicit user request, or after the user confirms at the end of the run:
   ```shell
   kubectl delete -f target/kubernetes/kubernetes.yml
   ```

## Final report

At the end of a run, always print a short summary for the user with these three parts, then ask whether to clean up:

1. **What was built and deployed:** the image (`<DOCKER_USERNAME>/sample-quarkus-on-kubernetes:1.0`), the `Deployment`, `Service`, liquibase init `Job` and `ServiceMonitor` generated from the extensions, and the PostgreSQL release `my-db` and the `kube-prometheus-stack` release the app depends on.
2. **Where it is:** cluster context (`minikube`), namespace (`default`), registry (`docker.io/<DOCKER_USERNAME>`), the manifest file `target/kubernetes/kubernetes.yml`, the database service `my-db-postgresql:5432`.
3. **How to test it:** `kubectl get pod,job,svc`, `kubectl port-forward svc/sample-quarkus-on-kubernetes 8080:80`, `curl http://localhost:8080/persons`, `/q/health/ready`, `/q/metrics`, `/q/swagger-ui`, `mvn verify -Dit.test=PersonTestOnKubernetesIT`, the Prometheus target. Say which of these you ran and what they returned.

## Rules for agents

- Build and test without side effects first: `mvn clean package -DskipTests` and `mvn verify -Dit.test=PersonResourceIT` change nothing on the cluster. Do not set `quarkus.kubernetes.deploy` unless the deploy is intended.
- Ask the user before anything outward-facing: pushing to Docker Hub, deploying to a cluster, installing Helm charts, deleting or recreating a Minikube profile.
- Do not clean up (`kubectl delete`, `skaffold delete`, `helm uninstall`) unless the user explicitly asks for it. At the end of a run, ask the user whether to clean up and wait for the answer.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Do not commit `target/`.
- When a step fails or an output differs from the book, fix the code or this file so they agree, and mention it in the report.
