# CLAUDE.md: sample-quarkus-k8s-ch1

## What this app demonstrates

- Quarkus 3.x + Java 25 + Maven, one `GET /hello` endpoint returning `Hello from Quarkus REST`.
- Extensions: `quarkus-rest`, `quarkus-container-image-jib`, `quarkus-kubernetes` (Dekorate under the hood).
- Kubernetes manifests generated at build time (`target/kubernetes/kubernetes.yml`), image built and pushed by Jib (no Docker daemon needed), manifests applied to the current cluster, all in one `mvn` command.

## Prerequisites

- JDK 25, Maven 3.9+, `kubectl`, `minikube` (docker driver), Quarkus CLI (`quarkus`) only for generating the project from scratch.
- A Docker Hub account and `docker login` done. Images go to `docker.io/<DOCKER_USERNAME>/sample-quarkus-k8s-ch1`. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`.
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.

## Step by step

1. Generate the project (skip if the code already exists):
   ```shell
   quarkus create app pl.piomin:sample-quarkus-k8s-ch1 \
     --extension='rest,container-image-jib,kubernetes'
   ```
   The generated `GreetingResource` serves `GET /hello`.
2. Build, push the image and deploy:
   ```shell
   mvn clean package \
     -Dquarkus.kubernetes.deploy=true \
     -Dquarkus.container-image.group=<DOCKER_USERNAME>
   ```
   Without `quarkus.container-image.group` the image prefix is the OS account name and the `Deployment` points to an image that does not exist.
   Quarkus creates a `Deployment` and a `Service` (`ClusterIP`, port `80`).
3. Verify:
   ```shell
   kubectl get pod
   kubectl get svc sample-quarkus-k8s-ch1          # ClusterIP, 80/TCP
   kubectl port-forward svc/sample-quarkus-k8s-ch1 8080:80
   curl http://localhost:8080/hello                # Hello from Quarkus REST
   ```
4. Scale (optional): `kubectl scale deploy sample-quarkus-k8s-ch1 --replicas=3`, then `kubectl get pod`; scale back to 1 afterwards.
5. Expose the service on port 8080 instead of 80: add to `src/main/resources/application.properties`
   ```properties
   quarkus.kubernetes.ports.http.host-port = 8080
   ```
   rebuild and redeploy with the command from step 2, then:
   ```shell
   kubectl get svc sample-quarkus-k8s-ch1         # 8080/TCP
   kubectl port-forward svc/sample-quarkus-k8s-ch1 8080
   curl http://localhost:8080/hello
   ```
6. Clean up after a test:
   ```shell
   kubectl delete -f target/kubernetes/kubernetes.yml
   ```

## Rules for agents

- Build and test without side effects first: `mvn clean package` (no `quarkus.kubernetes.deploy`) only compiles, runs tests and generates the manifest. Do not set `quarkus.container-image.push` or `quarkus.kubernetes.deploy` unless the deploy is intended.
- Ask the user before anything outward-facing: pushing to Docker Hub, deploying to a cluster, deleting or recreating a Minikube profile.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Do not commit `target/`.
- When a step fails or an output differs from what is described here, fix the code or this file so they agree, and mention it in the report.
