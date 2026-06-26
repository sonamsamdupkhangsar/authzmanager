# Authzmanager
This Authzmanager is used for managing OAuth2 client, role, organization and user profile.

```
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"
```

## Run locally using profile
Use the Eureka profile `application-eureka.yaml` to run locally.


```
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=eureka"
```

## Run locally with HTTPS

Passkey/WebAuthn login testing should use HTTPS for both authorization and authzmanager. The `local-https`
profile uses the mkcert certificate shared with authorization.

Create the local certificate if it does not already exist:

```
mkdir -p ~/openissuer-local-certs
mkcert \
  -cert-file ~/openissuer-local-certs/openissuer.test.pem \
  -key-file ~/openissuer-local-certs/openissuer.test-key.pem \
  platform.openissuer.test \
  free.openissuer.test \
  business1.openissuer.test \
  business2.openissuer.test \
  platform.admin.openissuer.test \
  free.admin.openissuer.test \
  business1.admin.openissuer.test \
  business2.admin.openissuer.test \
  authorization-server \
  localhost \
  127.0.0.1
```

Start authzmanager with:

```
ISSUER_ADDRESS=https://platform.openissuer.test:9001 \
ISSUER_URI=https://platform.openissuer.test:9001 \
SPRING_PROFILES_ACTIVE=eureka,local-https ./gradlew bootRun
```

Use the tenant admin URL, for example:

```
https://free.admin.openissuer.test:9093
```

The authzmanager OAuth client in authorization must have an HTTPS redirect URI for the same tenant:

```
https://free.admin.openissuer.test:9093/login/oauth2/code/b4dfe3fb-1692-44b8-92ab-366ccc84b539-authzmanager
```

When authorization is started with `eureka,local-https`, its authzmanager client seed uses HTTPS redirect URIs.

## Build Docker image

Build docker image using included Dockerfile.

`docker build -t ghcr.io/<username>/friendships-rest-service:latest .`

## Push Docker image to repository

`docker push ghcr.io/<username>/friendships-rest-service:latest`

## Deploy Docker image locally

`docker run -e POSTGRES_USERNAME=dummy \
-e POSTGRES_PASSWORD=dummy -e POSTGRES_DBNAME=account \
-e POSTGRES_SERVICE=localhost:5432 \
-e apiKey=123 -e DB_SSLMODE=DISABLE
--publish 8080:8080 ghcr.io/<username>/friendshps-rest-service:latest`


## Installation on Kubernetes
Use my Helm chart here @ [sonam-helm-chart](https://github.com/sonamsamdupkhangsar/sonam-helm-chart):

```
helm install user-rest-service sonam/mychart -f values-backend.yaml --version 0.1.15 --namespace=yournamespace
```

## Instruction for port-forwarding database pod
```
export PGMASTER=$(kubectl get pods -o jsonpath={.items..metadata.name} -l application=spilo,cluster-name=friendships-minimal-cluster,spilo-role=master -n yournamesapce); 
echo $PGMASTER;
kubectl port-forward $PGMASTER 6432:5432 -n backend;
```

### Login to database instruction
```
export PGPASSWORD=$(kubectl get secret <SECRET_NAME> -o 'jsonpath={.data.password}' -n backend | base64 -d);
echo $PGPASSWORD;
export PGSSLMODE=require;
psql -U <USER> -d projectdb -h localhost -p 6432

```
This application is deployed at [authzmanager](https://authzmanager.sonam.cloud).  I have attached documentation
about how to create OAuth2 client and use it in general on my [about](https://sonamsamdupkhangsar.github.io/my-authorization-server/about/) page.

Features added by date:

December 2024 : Profile photo
