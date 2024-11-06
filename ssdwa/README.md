# Secure and Scalable Deployment of Web Applications

This project deploys a Spring Boot and Vaadin Flow application locally using Docker and Traefik. The setup simulates an
intranet environment, allowing access through custom hostnames.

## Prerequisites

- Docker and Docker Compose
- Java 21

## Step 1: Configure the hosts File

To access the application using `fredpena.local` and `balancer.fredpena.local`, modify your `hosts` file.

### Example for Linux

1. Open a terminal.
2. Edit the /etc/hosts file with administrative privileges:

```shell
vi /etc/hosts
```

3. Add these lines:

```shell
127.0.0.1 fredpena.local
127.0.0.1 balancer.fredpena.local
```

4. Save the changes

### Example for Windows

1. Open Notepad as Administrator.
2. Go to C:\Windows\System32\drivers\etc\hosts.
3. Add the following lines at the end of the file:

```shell
127.0.0.1 fredpena.local
127.0.0.1 balancer.fredpena.local
```

4. Save the changes.

## Step 2: Package the Application for Production

Before creating the container, package the application as a production-ready .jar file:

```shell
 mvn clean package -DskipTests -Pproduction
```

This generates the ssdwa.jar file in the target folder.

## Step 3: Deploying using Docker

To build the Dockerized version of the project, use the following commands:

```shell
docker build . -t ssdwa:latest
```

Once the Docker image is correctly built, you can test it locally by running:

```shell
docker run -p 37650:37650 ssdwa:latest
```

The application will be accessible at http://localhost:37650.

## Step 4: Docker and Traefik Deployment

### Dockerfile Configuration

Ensure you have the following `Dockerfile` in the root directory of the project:

```dockerfile
FROM eclipse-temurin:21-jre
COPY target/ssdwa.jar ssdwa.jar
EXPOSE 37650
ENTRYPOINT ["java", "-jar", "/ssdwa.jar"]
```

#### `docker-compose.yml` Configuration

The docker-compose.yml file defines the necessary services for Traefik and your Spring Boot application.

The `docker-compose.yml` file defines the necessary services for Traefik and your Spring Boot application.

```yaml
services:
  traefik:
    image: "traefik:v3.2"
    container_name: "traefik"
    command:
      #- "--log.level=DEBUG"
      - "--api.insecure=true"
      - "--api.dashboard=true"
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--entrypoints.web.address=:80"
    ports:
      - "80:80"
      - "443:443"
      - "8080:8080"
    volumes:
      - "./letsencrypt:/letsencrypt"
      - "/var/run/docker.sock:/var/run/docker.sock:ro"

  whoami:
    image: traefik/whoami
    container_name: "simple-service"
    command:
      # It tells whoami to start listening on 2001 instead of 80
      - --port=2001
      - --name=iamfoo
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.fredpena-b.rule=Host(`balancer.fredpena.local`)"
      - "traefik.http.routers.fredpena-b.service=fredpena-b-service"
      - "traefik.http.routers.fredpena-b.entrypoints=web"
      - "traefik.http.services.fredpena-b-service.loadbalancer.server.port=2001"

  ssdwa:
    build:
      context: .
      dockerfile: Dockerfile

    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.fredpena.rule=Host(`fredpena.local`)"
      - "traefik.http.routers.fredpena.service=fredpena-service"
      - "traefik.http.routers.fredpena.entrypoints=web"
      - "traefik.http.services.fredpena-service.loadbalancer.server.port=37650"
```

### Start the Services

1. Build the Docker image for your application, then launch all services with Docker Compose:

```shell
docker compose up -d --build
```

2. Once started;

- Access the application: [http://fredpena.local](http://fredpena.local)
- Balancing service: [http://balancer.fredpena.local](http://balancer.fredpena.local)
- Traefik console: [http://fredpena.local:8080/](http://fredpena.local:8080/)

### Alternative: Running the Application Directly from the `.jar`

If you prefer running the application without Docker, you can use the `.jar` file directly:

```shell
java -jar target/ssdwa.jar
```

The application will be available at [http://localhost:37650](http://localhost:37650)

## Useful Commands

- Stop the Services:

```shell
docker compose down
```

- Check Logs:

```shell
docker compose logs -f
```

- List Running Containers:

```shell
docker ps
```
