# CLAUDE.md: chapter 5 (Security)

Walkthrough of the exercises from chapter 5 of the book "Java with Kubernetes in Action" (`../java-with-kuberneters-in-action-book/chapter-05`). The book is the source of truth for the steps; the code is in three modules of this directory: `sample-spring-k8s-ch5` (Spring Boot, `sample-spring-k8s`), `sample-quarkus-k8s-ch5` (Quarkus, `sample-quarkus-k8s`) and `sample-db-vault` (Spring Boot with Vault dynamic database credentials). The exercises build on each other, so go through them in the order below.

## Prerequisites

- JDK 25, Maven 3.9+, `kubectl`, `helm`, `skaffold`, `minikube` (docker driver), the `vault` CLI (`brew install vault`), `openssl`/`curl`.
- A Docker Hub account and `docker login` done. Get the login with `docker info | grep Username` and use it as `<DOCKER_USERNAME>`. The manifests and `skaffold.yaml` use `piomin/...` images; use `<DOCKER_USERNAME>` instead if it is different.
- Unless told otherwise, the target cluster is a local Minikube. Check `kubectl config current-context` is `minikube` before any deploy.
- Names: the book (and the Services, `Certificate` DNS names, Vault paths) use `sample-spring-k8s` and `sample-quarkus-k8s`, the modules are named `...-ch5`. Make the deployed names match the book. For Quarkus set `quarkus.application.name = sample-quarkus-k8s` in `application.properties` (otherwise the `Service` is `sample-quarkus-k8s-ch5` while the certificate is issued for `sample-quarkus-k8s`) and update the names in `GreetingTestOnKubernetesIT`. For Spring make the `image` in `k8s/deployment.yaml` and `k8s/deployment-ss.yaml` equal to the artifact image in `skaffold.yaml`, otherwise Skaffold does not substitute it.

## 1. Cert-Manager and HTTPS (book: "Manage Certificates with Cert Manager")

1. Install Cert-Manager and check the pods:
   ```shell
   helm repo add jetstack https://charts.jetstack.io --force-update
   helm install cert-manager --namespace cert-manager --set crds.enabled=true --create-namespace jetstack/cert-manager
   kubectl get pod -n cert-manager
   ```
2. Apply the self-signed `ClusterIssuer` `ss-cluster-issuer`, the `Secret` `jks-password-secret` (`k8s/secret.yaml` in the Spring module) and the `Certificate` `sample-spring-k8s` (all three YAMLs are in the book; the repo has only the `Secret`, save the other two under `k8s/`). `kubectl get certificate` must show `READY=True` and the `Secret` `sample-spring-k8s-cert` must contain `tls.crt`, `tls.key`, `keystore.jks`, `truststore.jks`.
3. Spring (`sample-spring-k8s-ch5`): the `ConfigMap` `k8s/configmap.yaml` and `k8s/deployment.yaml` are the HTTPS version (port `8443`, SSL bundle `server`). The book's `ConfigMap` also has `server.ssl.client-auth = NEED`; add it for this step. The `DemoController` in the repo is fully commented out: restore the `GET /hello` endpoint (`Hello World from Spring Boot!`). Deploy:
   ```shell
   skaffold run --port-forward
   ```
4. Call it with the client certificate:
   ```shell
   kubectl get secret sample-spring-k8s-cert -o jsonpath="{.data.tls\.key}" | base64 -d > tls.key
   kubectl get secret sample-spring-k8s-cert -o jsonpath="{.data.tls\.crt}" | base64 -d > tls.crt
   kubectl get secret sample-spring-k8s-cert -o jsonpath="{.data.ca\.crt}" | base64 -d > ca.crt
   curl https://localhost:8443/hello --cacert ca.crt --cert tls.crt --key tls.key   # Hello World from Spring Boot!
   ```
   The call without `--cert`/`--key` must fail. Do not commit the `.key`, `.crt` files.
5. Quarkus (`sample-quarkus-k8s-ch5`): the `quarkus-certmanager` extension and the `quarkus.certificate.*` properties are already in `application.properties`; `src/main/kubernetes/kubernetes.yml` adds the `ClusterIssuer` and the `Secret`. Build, push and deploy:
   ```shell
   mvn clean package -Dquarkus.kubernetes.deploy=true -Dquarkus.container-image.group=<DOCKER_USERNAME> -DskipTests
   kubectl port-forward svc/sample-quarkus-k8s 8443:443
   kubectl get secret sample-quarkus-k8s-cert -o jsonpath="{.data.ca\.crt}" | base64 -d > ca.crt
   curl https://localhost:8443/hello --cacert ca.crt        # Hello from Quarkus REST
   ```
   Stop the port-forwards afterwards.

## 2. Trust-Manager and calls between the apps (book: "Distributing Trusted Certificates")

1. Install Trust-Manager (same repo as Cert-Manager):
   ```shell
   helm install trust-manager cert-manager/trust-manager \
     --set secretTargets.enabled=true --set secretTargets.authorizedSecretsAll=true -n cert-manager
   ```
2. Apply the CA `Certificate` `my-self-signed-ca` (namespace `cert-manager`), the `ClusterIssuer` `my-ca-issuer` and the `Bundle` `main-bundle` from the book. Create the namespaces `spring` and `quarkus` and check the `ConfigMap` `main-bundle` exists in both.
3. Spring in the `spring` namespace: the `Secret` `jks-password-secret` must exist there too (`kubectl apply -f k8s/secret.yaml -n spring`). Apply the `Certificate` `sample-spring-k8s` that uses `my-ca-issuer` in `spring`. For the call from Quarkus remove `server.ssl.client-auth` from the `ConfigMap` (the Quarkus client has no client certificate). Then `skaffold dev -n spring` (it also applies the `ConfigMap`); keep it running or use `skaffold run -n spring`.
4. Quarkus in the `quarkus` namespace: add the `quarkus-rest-client-jackson` client (`SpringBootAppClient`, `GET /hello/to-spring` in `GreetingResource`, both already in the repo), set `quarkus.certificate.issuer-ref.name = my-ca-issuer`, mount the `main-bundle` `Secret` as `ca-volume` at `/opt/ca` (`quarkus.kubernetes.secret-volumes.ca-volume.*`, `quarkus.kubernetes.mounts.ca-volume.path`). Create the `ConfigMap` `sample-quarkus-k8s` in `quarkus` with `quarkus.rest-client.spring.url = https://sample-spring-k8s.spring:8443` and the truststore `/opt/ca/ca-certs.jks` (password `changeit`), then:
   ```shell
   mvn clean package -Dquarkus.kubernetes.namespace=quarkus -Dquarkus.kubernetes.deploy=true -Dquarkus.container-image.group=<DOCKER_USERNAME> -DskipTests
   kubectl port-forward svc/sample-quarkus-k8s 8443:443 -n quarkus
   curl https://localhost:8443/hello/to-spring --insecure     # Hello World from Spring Boot!
   ```
5. Spring to Quarkus: add the client SSL bundle properties and `client.url = https://sample-quarkus-k8s.quarkus:443` to the `ConfigMap`, the secure `RestClient` bean and `GET /hello/to-quarkus` to the Spring app (code in the book), redeploy with Skaffold, mount `main-bundle` at `/opt/ca` in the Spring `Deployment`. Port-forward the Spring `Service` on a different local port and call it:
   ```shell
   kubectl port-forward svc/sample-spring-k8s 9443:8443 -n spring
   curl https://localhost:9443/hello/to-quarkus --insecure     # Hello from Quarkus REST
   ```

## 3. Vault and the Secrets Store CSI Driver (book: "Manage Secrets")

1. Install Vault in dev mode with the CSI provider and check the pods:
   ```shell
   helm repo add hashicorp https://helm.releases.hashicorp.com
   helm install vault hashicorp/vault --set server.dev.enabled=true --set csi.enabled=true -n vault --create-namespace
   kubectl get pod -n vault
   ```
2. Configure Vault (keep the port-forward open in the background; in dev mode the root token is `root`):
   ```shell
   kubectl port-forward vault-0 8200 -n vault
   export VAULT_ADDR=http://127.0.0.1:8200
   vault login
   vault auth enable kubernetes
   export K8S_API=$(echo "https://$(kubectl exec vault-0 -n vault -- env | grep KUBERNETES_PORT_443_TCP_ADDR | cut -f2 -d'='):443")
   vault write auth/kubernetes/config kubernetes_host=$K8S_API
   vault write auth/kubernetes/role/spring bound_service_account_names=default bound_service_account_namespaces=spring policies=spring ttl=24h
   ```
   Create the `spring` policy (`vault policy write spring - <<EOF ... EOF`, text in the book). It must allow reading the path the app secret is stored under, `secret/data/sample-spring-k8s` (the book's `kv-v2/data/spring` does not match the path used later).
3. Install the Secrets Store CSI Driver with rotation enabled:
   ```shell
   helm repo add secrets-store-csi-driver https://kubernetes-sigs.github.io/secrets-store-csi-driver/charts
   helm install csi-secrets-store secrets-store-csi-driver/secrets-store-csi-driver -n k8s-secrets-store-csi --create-namespace \
     --set linux.providersDir=/var/run/secrets-store-csi-providers --set enableSecretRotation=true
   kubectl describe ds csi-secrets-store-secrets-store-csi-driver -n k8s-secrets-store-csi
   ```
4. Apply the `SecretProviderClass` `vault-database` in `spring` (book). Store the secret: `vault kv put secret/sample-spring-k8s custom.secret-property="123"`. Check the content of the mounted `/mnt/secrets-store/application.properties` in the pod: the file holds the raw secret value, and Spring reads it as a properties file, so if it contains only `123` store the whole property line (`custom.secure-property=123`) as the secret value.
5. Spring: implement `GET /hello/custom` returning `customProperty` and `customSecureProperty` (book code, `custom.property` comes from the `ConfigMap`). This step is plain HTTP on `8080`: use a `ConfigMap` without the SSL bundle. `k8s/deployment-ss.yaml` is the `Deployment` for this step; add a `secrets-store` Skaffold profile that uses it instead of `k8s/deployment.yaml` (it does not exist in `skaffold.yaml` yet), then:
   ```shell
   skaffold dev --port-forward -p secrets-store -n spring
   curl http://localhost:8080/hello/custom       # {"customProperty":"test","customSecureProperty":"123"}
   ```

## 4. Rotation (book: "Rotate Secrets and Certificates")

1. Quarkus secret rotation in dev mode in the `quarkus` namespace: enable `quarkus.package.jar.type = mutable-jar`, `quarkus.live-reload.password = abc123` and `quarkus.kubernetes.env.vars.QUARKUS_LAUNCH_DEVMODE = true` (commented out in the repo). Create the Vault role and the `quarkus` policy (path `secret/data/sample-quarkus-k8s`), the `SecretProviderClass` `vault-database` in `quarkus`, and `vault kv put secret/sample-quarkus-k8s custom.secret-property="123"`. The `Deployment` fragment with the CSI volume is `src/main/kubernetes/kubernetes.yml`; add `quarkus.kubernetes.mounts.secrets-store.path = /mnt/secrets-store`. Deploy with the command from 2.4, port-forward the app and call `curl http://localhost:8080/hello/custom`. Then change the value (`vault kv put ... custom.secret-property="456"`), wait up to 2 minutes (the CSI rotation poll interval) and call the endpoint again: it must return the new value without a pod restart.2. Certificate rotation for Spring: add `spring.ssl.bundle.jks.server.reload-on-update = true` to the Spring `ConfigMap` (HTTPS version, `Certificate` with `duration: 1h`, `renewBefore: 5m`), run `curl https://localhost:8443/hello --insecure -v` and watch `kubectl get event -n spring` for the reissue events. The pod must not restart.

## 5. Dynamic database credentials (book: "Automatic Authentication to Database", module `sample-db-vault`)

1. Needs Vault configured as in section 3 (steps 1 and 2). Create the `spring-vault` policy (`database/creds/default`, read), the service account in the namespace where the app runs (`kubectl create sa spring-app -n spring`), and the Vault role `spring-app` bound to it (the name used in `application.yml`). Then `vault secrets enable database`.
2. Install PostgreSQL so that the `Service` and the `Secret` are named `postgres` (the host in `spring.datasource.url`): `helm repo add bitnami https://charts.bitnami.com/bitnami` and `helm install postgres bitnami/postgresql --set fullnameOverride=postgres`. The password is in the `Secret` `postgres`, key `postgres-password`.
3. Configure the Vault database engine (`vault write database/config/postgres ...` with the password read from the `Secret`, connection host `postgres.default`, then `vault write database/roles/default ...` with `default_ttl=1m`, `max_ttl=10m`; both commands are in the book). Do not print the password in the report.
4. Deploy and test (`k8s/deployment.yaml` already uses the `spring-app` service account; deploy in the namespace of the service account):
   ```shell
   cd sample-db-vault
   skaffold dev --port-forward -n spring
   kubectl exec svc/postgres -i -t -- psql -U postgres       # then \du, repeat after a minute
   curl http://localhost:8080/persons
   ```
   New `v-kubernet-...` database users should appear about every 30 seconds while the app keeps working.
5. Clean up only on explicit user request, or after the user confirms at the end of the run. Deleted in this order: the apps (`skaffold delete -n spring`, `kubectl delete -f target/kubernetes/kubernetes.yml` for Quarkus), the `Certificate`/`Issuer`/`Bundle` objects, then `helm uninstall` of `postgres`, `csi-secrets-store`, `vault`, `trust-manager`, `cert-manager`, then the namespaces `spring`, `quarkus`, `vault`, `cert-manager`, `k8s-secrets-store-csi`.

## Final report

At the end of a run, always print a short summary for the user with these three parts, then ask whether to clean up:

1. **What was built and deployed:** the images (`<DOCKER_USERNAME>/sample-spring-k8s`, `<DOCKER_USERNAME>/sample-quarkus-k8s`, `<DOCKER_USERNAME>/sample-db-vault`), the apps and the namespaces they run in, the Helm releases (`cert-manager`, `trust-manager`, `vault`, `csi-secrets-store`, `postgres`) and the Cert-Manager / Vault objects created.
2. **Where it is:** cluster context (`minikube`), namespaces (`default`, `spring`, `quarkus`, `cert-manager`, `vault`, `k8s-secrets-store-csi`), registry (`docker.io/<DOCKER_USERNAME>`), the manifests you added under `k8s/`.
3. **How to test it:** the `curl` calls from the steps above and what they returned, `kubectl get certificate -A`, `kubectl get secretproviderclass -A`, `vault kv get secret/sample-spring-k8s`. Say which sections you completed, which you skipped, and every place where you had to deviate from the book (names, ports, policy paths).

## Rules for agents

- Build and test without side effects first: `mvn clean package -DskipTests` only generates the manifests and changes nothing on the cluster. Do not set `quarkus.kubernetes.deploy` unless the deploy is intended.
- Ask the user before anything outward-facing: pushing to Docker Hub, deploying to a cluster, installing Helm charts, deleting or recreating a Minikube profile.
- Do not clean up (`kubectl delete`, `skaffold delete`, `helm uninstall`) unless the user explicitly asks for it. At the end of a run, ask the user whether to clean up and wait for the answer.
- Never deploy to a cluster other than the local Minikube unless the user says so.
- Vault runs in dev mode (in-memory, root token `root`): local exercise only. Never put tokens, passwords or the extracted `tls.key` into files that can be committed or into the report.
- Do not commit `target/`, `*.key`, `*.crt`.
- When a step fails or an output differs from the book, fix the code or this file so they agree, and mention it in the report.
