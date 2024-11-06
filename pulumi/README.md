# Infrastructure as Code (IaC) Project with Pulumi and Ansible

This project uses Pulumi to create cloud infrastructure on DigitalOcean and Ansible to configure and deploy
applications. We’ll guide you through setting up a Pulumi project, deploying infrastructure, and using Ansible to
install Docker and launch your application on a cloud server.

## Prerequisites

- Pulumi Account and CLI
- Ansible
- DigitalOcean API Token
- SSH key added to DigitalOcean for access to the server

## Step 1: Infrastructure Setup with Pulumi

1. Create a Pulumi Project

```shell
mkdir pulumi-iac
```

```shell
cd pulumi-iac
```

```shell
pulumi new java
```

Note: You’ll need a Pulumi account and API token.
Follow [these instructions](https://www.pulumi.com/docs/pulumi-cloud/access-management/access-tokens/) to generate your
Pulumi access token.

2. Verify Pulumi Stack

Check that the stack is set up correctly:

```shell
pulumi stack ls
```

3. Deploy the Pulumi Stack

Deploy the infrastructure stack:

```shell
pulumi up
```

4. dd Pulumi DigitalOcean Dependency

Include the following dependency in your pom.xml to interact with DigitalOcean:

```xml

<dependency>
    <groupId>com.pulumi</groupId>
    <artifactId>digitalocean</artifactId>
    <version>4.18.0</version>
</dependency>
```

5. Create Resources with Pulumi

> Here’s an example code snippet to create a DigitalOcean Droplet:

```java
import com.pulumi.Pulumi;
import com.pulumi.digitalocean.Droplet;
import com.pulumi.digitalocean.DropletArgs;
import io.github.cdimascio.dotenv.Dotenv;

public class App {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String fingerprint = dotenv.get("FINGERPRINT");

        Pulumi.run(ctx -> {
            // Create a new Droplet in nyc1
            Droplet droplet = new Droplet("web", DropletArgs.builder()
                    .image("debian-12-x64")
                    .name("iac-madrid-jug")
                    .region("lon1")
                    .size("s-1vcpu-2gb")
                    .sshKeys(fingerprint)
                    .build());

            System.out.println("The public IP address of your Droplet application");
            System.out.println("IP  => " + droplet.ipv4Address());
        });
    }
}
```

6. Add the Droplet IP to Ansible Hosts

Once created, add the droplet's IP to the hosts file in the main iac project folder. Locate `app` in the hosts file and
replace it with your new droplet IP.

7. Generate a DigitalOcean Access Token

You’ll need a DigitalOcean access token to connect
Ansible. [Generate a token here](https://docs.digitalocean.com/reference/api/create-personal-access-token) and save it
as an environment variable:

```shell
export DIGITALOCEAN_ACCESS_TOKEN=<your_token>
```

8. nsure SSH Configuration

Make sure your SSH key registered with DigitalOcean matches the one set in `all:vars` in your Ansible hosts file:

```shell
ansible_ssh_private_key_file=~/.ssh/id_rsa
```

## Step 2: Configure Docker Deployment with Ansible

1. Update `docker-compose.yml.j2` for Production

Ensure your `docker-compose.yml.j2` template is ready for a production cloud environment. Point your domain to the IP
generated by DigitalOcean, as Traefik may temporarily block access if domain settings aren’t correct.

Example configuration:

```yaml
services:
  traefik:
    image: "traefik:v3.2"
    container_name: "traefik"
    command:
      - "--api.insecure=true"
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.tlschallenge=true"
      - "--certificatesresolvers.letsencrypt.acme.email=me@fredpena.dev"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.web.http.redirections.entryPoint.to=websecure"
      - "--entrypoints.web.http.redirections.entryPoint.scheme=https"
      - "--entrypoints.web.http.redirections.entrypoint.permanent=true"
    ports:
      - "80:80"
      - "443:443"
      - "8080:8080"
    volumes:
      - "./letsencrypt:/letsencrypt"
      - "/var/run/docker.sock:/var/run/docker.sock:ro"

  whoami:
    image: traefik/whoami
    command:
      - --port=2001
      - --name=iamfoo
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.fredpena-b.rule=Host(`balancer.fredpena.dev`)"
      - "traefik.http.routers.fredpena-b.service=fredpena-b-service"
      - "traefik.http.routers.fredpena-b.entrypoints=websecure"
      - "traefik.http.services.fredpena-b-service.loadbalancer.server.port=2001"
      - "traefik.http.routers.fredpena-b.tls.certresolver=letsencrypt"
      - "traefik.http.middlewares.onlyhttps.redirectscheme.scheme=https"
      - "traefik.http.middlewares.onlyhttps.redirectscheme.permanent=true"

  ssdwa:
    image: fredpena/ssdwa:1.0.0
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.fredpena.rule=Host(`app.fredpena.dev`)"
      - "traefik.http.routers.fredpena.service=fredpena-service"
      - "traefik.http.routers.fredpena.entrypoints=websecure"
      - "traefik.http.services.fredpena-service.loadbalancer.server.port=37650"
      - "traefik.http.routers.fredpena.tls.certresolver=letsencrypt"
      - "traefik.http.middlewares.onlyhttps.redirectscheme.scheme=https"
      - "traefik.http.middlewares.onlyhttps.redirectscheme.permanent=true"
```

2. Run `install-docker.playbook.yml` to Set Up Docker

```shell
ansible-playbook -i host install-docker.playbook.ym
```

Execute the Ansible playbook to install Docker on the server.

```yaml
---
- hosts: app
  tasks:
    - name: Removing default Docker packages
      apt:
        pkg:
          - docker
          - docker-engine
          - docker.io
          - containerd
          - runc
        state: absent

    - name: Installing required packages
      apt:
        pkg:
          - ca-certificates
          - curl
          - gnupg
          - lsb-release

    - name: Create required directory
      file:
        path: /etc/apt/keyrings
        state: directory

    - name: Adding Docker's official GPG key
      shell: curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --batch --yes --dearmor -o /etc/apt/keyrings/docker.gpg

    - name: Setting Docker repository
      shell: |
        echo \
        "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian \
        $(lsb_release -cs) stable" > /etc/apt/sources.list.d/docker.list

    - name: Installing Docker
      apt:
        pkg:
          - docker-ce
          - docker-ce-cli
          - containerd.io
          - docker-compose-plugin
          - python3-docker
        update-cache: yes
```

3. Run `install-app.playbook.yml` to Deploy the Application

```shell
ansible-playbook -i host install-app.playbook.yml
```

After Docker is installed, use this playbook to deploy the application.

## Useful Commands

```shell
pulumi destroy -s ...
```

```shell
pulumi stack rm ...
```