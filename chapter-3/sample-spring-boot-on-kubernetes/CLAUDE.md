# CLAUDE.md: sample-spring-boot-on-kubernetes

Walkthrough of the Spring Boot exercises from chapter 3 of the book "Java with Kubernetes in Action" (`../../../java-with-kuberneters-in-action-book/chapter-03`): health probes on a separate management port, startup probe, graceful shutdown, resource requests and limits, and Prometheus metrics with a `ServiceMonitor`. Follow the book; the code and the manifests in `k8s/` in this directory are the finished result (they also deploy MongoDB, which the app uses).

## Prerequisites

- JDK 25, Maven 3.9+, Docker (Testcontainers and Buildpacks), `kubectl`, `minikube` (docker driver), `helm`, `skaffold`.
- A Docker Hub account and `docker login` done. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`. The manifests and `pom.xml` use the image `piomin/sample-spring-boot-on-kubernetes:1.0-SNAPSHOT`; replace `piomin` with `<DOCKER_USERNAME>` if it is different.
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.

## Step by step

1. Install the monitoring stack first, the `ServiceMonitor` CRD comes with it (book: "Collecting Metrics with Prometheus"):
   ```shell
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack -n monitoring --create-namespace
   kubectl get pod -n monitoring
   ```
2. Build and test locally (no side effects, Docker must be running):
   ```shell
   mvn clean verify
   ```
3. Deploy MongoDB, the app and the `ServiceMonitor` (`k8s/mongodb-deployment.yaml`, `k8s/deployment.yaml`, `k8s/servicemonitor.yaml`) with Skaffold; it builds the image with Jib and applies the manifests, nothing else has to be deployed by hand:
   ```shell
   skaffold run
   kubectl rollout status deploy/sample-spring-boot-on-kubernetes-deployment
   ```
   The `buildpacks` profile builds the image with Cloud Native Buildpacks instead (the book's `build-image` approach): `skaffold run -p buildpacks`.
4. Verify the probes and the metrics (book: "Liveness and Readiness Health Checks", "Startup Probes", "Collecting Metrics"):
   ```shell
   kubectl get pod
   kubectl port-forward svc/sample-spring-boot-on-kubernetes 8080 8081
   curl http://localhost:8080/persons
   curl http://localhost:8080/readiness
   curl http://localhost:8081/actuator/health/liveness
   curl http://localhost:8081/actuator/prometheus | head
   ```
   `GET /persons` returns the five sample persons. The readiness probe (port `8080`, `/readiness`) includes only the `mongo` health indicator, the liveness and startup probes use port `8081`. Only `health` and `prometheus` are exposed.
5. Graceful shutdown and resources (book: "Graceful Shutdown", "Properly Define Resource Requests and Limits"): check that the `Deployment` has `terminationGracePeriodSeconds: 30`, the `preStop` `sleep` of 5 s, `spring.lifecycle.timeout-per-shutdown-phase: 20s` and the requests/limits from the book (`384Mi`/`200m`, `740Mi`/`2`), and that the pod is `Running` without restarts:
   ```shell
   kubectl describe pod -l app=sample-spring-boot-on-kubernetes
   ```
6. Prometheus (the `ServiceMonitor` was applied in step 3; it needs the label `release: kube-prometheus-stack`):
   ```shell
   kubectl port-forward svc/kube-prometheus-stack-prometheus 9090 -n monitoring
   ```
   The target `sample-spring-boot-on-kubernetes` should appear in Status -> Target health.
7. Clean up only on explicit user request, or after the user confirms at the end of the run:
   ```shell
   skaffold delete        # removes the app, MongoDB and the ServiceMonitor
   ```

## Final report

At the end of a run, always print a short summary for the user with these three parts, then ask whether to clean up:

1. **What was built and deployed:** the image (`<DOCKER_USERNAME>/sample-spring-boot-on-kubernetes:1.0-SNAPSHOT`), the app `Deployment` and `Service` (ports `8080` and `8081`), MongoDB, the `ServiceMonitor` and the `kube-prometheus-stack` release.
2. **Where it is:** cluster context (`minikube`), namespace (`default`), registry (`docker.io/<DOCKER_USERNAME>`), the manifests in `k8s/`, the Prometheus stack in the `monitoring` namespace.
3. **How to test it:** `kubectl get pod,svc`, `kubectl port-forward svc/sample-spring-boot-on-kubernetes 8080 8081`, `curl http://localhost:8080/persons`, `/readiness`, `http://localhost:8081/actuator/health/liveness`, `/actuator/prometheus`, the Prometheus target. Say which of these you ran and what they returned.

## Rules for agents

- Build and test without side effects first: `mvn clean verify` only needs Docker for Testcontainers.
- Ask the user before anything outward-facing: pushing to Docker Hub, deploying to a cluster, installing Helm charts, deleting or recreating a Minikube profile.
- Do not clean up (`kubectl delete`, `skaffold delete`, `helm uninstall`) unless the user explicitly asks for it. At the end of a run, ask the user whether to clean up and wait for the answer.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Keep the image tag consistent across `pom.xml` (`${project.version}`), `k8s/deployment.yaml` and `skaffold.yaml`.
- Do not commit `target/`.
- When a step fails or an output differs from the book, fix the code or this file so they agree, and mention it in the report.
