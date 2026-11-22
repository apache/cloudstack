<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

# Local CloudStack with Docker Compose

A Docker Compose environment that builds CloudStack from this working tree and
runs it on your machine with the **simulator** hypervisor, behind HTTPS.

## Goal

Give a developer a complete CloudStack (UI, API, database and a ready-to-use
zone) to test changes against, without installing Java, Maven, MySQL or Node on
the host and without a real hypervisor.

It differs from `tools/docker/Dockerfile` (the published
`apache/cloudstack-simulator` image) in that each component runs in its own
container, the database lives in a volume instead of inside the image, and the
whole thing is served over HTTPS on a hostname of your choice.

## What you get, and what you do not

The simulator (`plugins/hypervisors/simulator`) is a fake hypervisor that runs
inside the management server. Hosts, instances, networks, volumes and system VMs
are records in a database: every operation succeeds without touching real
infrastructure. It is the same setup the project CI uses for its smoke tests.

| Works | Does not work |
| --- | --- |
| UI and API, all orchestration logic | Real VMs (nothing actually boots) |
| Creating instances, networks, volumes, snapshots | VM console |
| System VMs and virtual routers (simulated) | Real template downloads |
| Failure injection through the `configureSimulator` API | KVM, VMware, XenServer |

The usage server is not part of this environment.

## Requirements

- Docker with the Compose plugin
- [mkcert](https://github.com/FiloSottile/mkcert)
- Ports 80 and 443 free on the host
- About 3 GB of free memory to run it, and noticeably more during the image build
- About 10 GB of disk for the images and the build cache

## Quick start

Run everything from the repository root. `cloud.test` is an example; pick any
domain that contains a dot.

```bash
# 1. Generate a locally trusted wildcard certificate for *.cloud.test
tools/docker/local/generate-certs.sh cloud.test

# 2. Point the hostname at your machine
echo '127.0.0.1 cloudstack.cloud.test' | sudo tee -a /etc/hosts

# 3. Build and start
docker compose -f tools/docker/local/docker-compose.yml up -d --build
```

Then open <https://cloudstack.cloud.test> and log in with `admin` / `password`
(leave the domain empty).

The first build compiles the whole project with Maven inside the image and takes
a long time. On the first start the database is created and the zone is
deployed, which adds a few minutes. Later starts take about a minute.

## Architecture

```
browser ──https://cloudstack.<domain>──> nginx :443 (TLS)      :80 redirects to HTTPS
                                           ├─ /         ──> ui:80            static UI build
                                           └─ /client/  ──> management:8080  API
                                                              └──> mysql:3306
db-init    (runs once) ──> mysql                    creates the schemas
datacenter (runs once) ──> management:8096 + mysql  deploys the simulated zone
```

| Service | Role | Image |
| --- | --- | --- |
| `nginx` | TLS termination and reverse proxy; the only published ports | `nginx:alpine` |
| `ui` | The Vue UI, built for production and served as static files | built from `Dockerfile.ui` |
| `management` | Management server with the simulator, run with `mvn jetty:run` | built from `Dockerfile` |
| `mysql` | Database (`cloud`, `cloud_usage`, `simulator`) | `mysql:8.4` |
| `db-init` | Creates the schemas, then exits | same image as `management` |
| `datacenter` | Deploys the zone with Marvin, then exits | same image as `management` |

Start order is enforced with `depends_on`: `mysql` healthy → `db-init` finished
→ `management` healthy → `datacenter`.

## How it works

### Images

Each image has its own Dockerfile and build context.

- **`management`** (`Dockerfile`, context: repository root) installs JDK 17,
  Maven and Python on Ubuntu 24.04, copies the source tree and runs `mvn -Pdeveloper -Dsimulator -DskipTests clean install`.
  Before building, it rewrites the database host from `localhost` to `mysql` in
  the image's copy of `build/replace.properties` (read by the management server)
  and generates `utils/conf/db.properties.override` (read by `deploydb`). Your
  working tree is not modified. Marvin is installed from the build output. The
  Maven repository is kept in a BuildKit cache mount, so a rebuild recompiles
  everything but does not download the dependencies again.
- **`ui`** (`Dockerfile.ui`, context: `ui/`) runs `npm run build` on Node 24
  and copies `dist` into an `nginx:alpine` image. The npm and webpack caches
  are kept in BuildKit cache mounts between builds.

`Dockerfile.dockerignore` keeps `.git`, `target` directories, this directory and
everything under `ui/` except `index.html` (the only UI file the Maven build
reads) out of the `management` build context. Editing the UI or the files here
therefore does not trigger a Maven rebuild.

### Database

`db-init.sh` runs `mvn -Pdeveloper -pl developer -Ddeploydb` and
`-Ddeploydb-simulator`. `deploydb` drops and recreates the databases, so the
script first checks whether the simulator schema exists and exits if it does.
The data is kept in the `cloudstack-mysql` volume.

### Zone

`deploy-datacenter.sh` deploys `setup/dev/advanced.cfg` with Marvin, after
pointing it at the `management` and `mysql` services. The result is an advanced
zone named `Sandbox-simulator` with four simulated hosts. The script is skipped
when a zone already exists.

### TLS

TLS ends at `nginx`; traffic behind it is plain HTTP on an isolated Docker
network. `generate-certs.sh` installs the mkcert local CA in your trust stores
and writes `certs/tls.crt` and `certs/tls.key` for `*.<domain>` and `<domain>`.
The `certs` directory is ignored by git and excluded from the images.

The nginx configuration accepts any hostname, so the only places that know your
domain are the certificate and `/etc/hosts`. To change the domain, run
`generate-certs.sh` again, update `/etc/hosts` and restart `nginx`.

### Networks and ports

| Network | Subnet | Purpose |
| --- | --- | --- |
| `cloudstack-public` | 192.168.242.0/24 | Published ports and outbound access |
| `cloudstack-internal` | 192.168.243.0/24 | Container-to-container only, no route out |

The subnets are fixed to stay clear of the ranges Docker picks by default, which
tend to collide with VPN routes.

Only `nginx` publishes ports, and only on `127.0.0.1` (80 and 443). The database
(3306) and the management server ports (8080, 8096) are reachable only from
other containers.

> **Port 8096 is the integration API and accepts requests without
> authentication.** Do not publish it. It is enabled here because the developer
> database prefill turns it on and Marvin uses it to deploy the zone.

`management` is on the public network because Maven needs to reach its
repositories when the server starts.

## Everyday use

All commands run from the repository root.

```bash
# Start / stop (data is kept)
docker compose -f tools/docker/local/docker-compose.yml up -d
docker compose -f tools/docker/local/docker-compose.yml stop

# Follow the management server log
docker compose -f tools/docker/local/docker-compose.yml logs -f management

# Rebuild after changing code
docker compose -f tools/docker/local/docker-compose.yml up -d --build

# Throw away the database and the zone and start clean
docker compose -f tools/docker/local/docker-compose.yml down -v
```

### Rebuild cost

The source is copied into the images, not mounted.

| You changed | What is rebuilt |
| --- | --- |
| Java, Maven or anything outside `ui/` | The `management` image (the whole Maven build) |
| Only files under `ui/` | The `ui` image (a few minutes) |
| Files in `tools/docker/local/` | Nothing; restart the affected service |

To rebuild a single image, name it:
`docker compose -f tools/docker/local/docker-compose.yml build ui`.

### Database changes

`db-init` does not run again once the schemas exist. After changing schema or
prefill SQL, recreate the database with `down -v` and `up -d --build`.

### Using the API from the host

Log in through HTTPS and reuse the session:

```bash
curl -c cookies.txt --data 'command=login&username=admin&password=password&domain=/&response=json' \
  https://cloudstack.cloud.test/client/api
```

The response carries a `sessionkey`; send it as a parameter, together with the
cookies, on the following calls.

## Troubleshooting

| Symptom | Cause and fix |
| --- | --- |
| `nginx` fails with "address already in use" | Something else is listening on 80 or 443. Stop it and run `up -d` again |
| `nginx` exits right after starting | `certs/tls.crt` or `certs/tls.key` is missing. Run `generate-certs.sh` |
| Browser warns about the certificate | The mkcert CA is not trusted by that browser. Run `mkcert -install` and restart the browser |
| Hostname does not resolve | The `/etc/hosts` entry is missing or uses a different domain than the certificate |
| `management` stays in `starting` | Normal for about a minute. Check `logs management`; the container is healthy once port 8096 opens |
| Build or containers get killed | The machine ran out of memory. Close other applications or enable swap |

## Files

| File | Purpose |
| --- | --- |
| `docker-compose.yml` | Service, volume and network definitions |
| `Dockerfile` | `management` image |
| `Dockerfile.dockerignore` | Build context exclusions for the `management` image |
| `Dockerfile.ui` | `ui` image |
| `Dockerfile.ui.dockerignore` | Build context exclusions for the `ui` image |
| `db-init.sh` | Creates the schemas on first run |
| `deploy-datacenter.sh` | Deploys the simulated zone on first run |
| `generate-certs.sh` | Generates the mkcert certificate (runs on the host) |
| `nginx/default.conf` | TLS and reverse proxy configuration |
| `certs/` | Generated certificate and key (not versioned) |
