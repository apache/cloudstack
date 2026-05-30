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

# Apache CloudStack Security Threat Model (draft)

> **Document scope and PMC structural decision.** The CloudStack PMC owns
> five repositories: `apache/cloudstack` (the management server, agent, and
> systemvm), plus four satellite clients — `apache/cloudstack-cloudmonkey`
> (CLI), `apache/cloudstack-go` (Go SDK), `apache/cloudstack-terraform-provider`,
> `apache/cloudstack-kubernetes-provider`. This document models
> `apache/cloudstack` as the canonical threat model; the four satellite
> models are short *deltas* that inherit §3 / §4 / §7 from this
> document and add only what each satellite uniquely introduces (`§4 B1`
> reachability, the credential file shape, the wrapper-of-SDK contract,
> etc.). The deltas live at `/tmp/claude/cloudstack-<repo>-threat-model-draft.md`.
> An umbrella model was rejected because the satellites are uniformly thin
> "HMAC-SHA1-signing HTTP client" wrappers — a single document either
> drowns them in CloudStack-server content or, worse, drowns the
> CloudStack-server content in satellite caveats. Each satellite is small
> enough that a 1–2 page delta works.

## §1 Header

- **Project:** Apache CloudStack (`apache/cloudstack`) — IaaS orchestration
  platform. This document does **not** cover the four satellite repos, which
  carry their own delta models.
- **Commit:** `7308dad1` (HEAD of `main` at draft time).
- **Date:** 2026-05-29.
- **Authors:** ASF Security team draft, awaiting CloudStack PMC review.
- **Status:** Draft — under maintainer review.
- **Version binding:** This document describes the model as of the commit
  above. A vulnerability report against CloudStack release *N* (currently
  the 4.20.x line) should be triaged against the model as it stood at *N*'s
  release tag, not against HEAD.
- **Reporting:** vulnerabilities that fall under §8 (claimed properties)
  should be reported per the project's published policy
  (`security@apache.org` per `README.md` and
  `https://cloudstack.apache.org/security.html`); reports that fall under
  §3 (out of scope), §9 (properties not provided), or §11a (known
  non-findings) will be closed by CloudStack triagers citing this document.
- **Provenance legend** —
  *(documented)* = paraphrased from an in-repo source or the project website
  with citation; *(maintainer)* = stated by a CloudStack PMC member in
  response to this draft; *(inferred)* = synthesized by the producer from
  code structure or domain knowledge, awaiting PMC ratification (every
  *(inferred)* tag has a matching §14 question).
- **Draft confidence:** 36 documented / 0 maintainer / 41 inferred.

**About the project.** Apache CloudStack is an open-source Infrastructure-as-a-
Service (IaaS) orchestration platform *(documented: `README.md`,
`https://cloudstack.apache.org/`)*. It deploys and manages large fleets of
virtual machines across multiple hypervisors (KVM, VMware, XenServer/XCP-ng,
Hyper-V, baremetal-bridge, OVM) and over object/block/file storage
(NFS, Ceph/RBD, iSCSI, SMP, primary-storage plugins, S3-compatible secondary
storage). A central **management server** (Java/Tomcat-style servlets,
backed by MariaDB/MySQL) exposes a signed REST/JSON API to admins, end
users, and integrations; runs system VMs (Secondary Storage VM, Console
Proxy VM, virtual router); and orchestrates a fleet of **agents** running
on each hypervisor host. Authorization is RBAC + multi-tenant
domain/account/project hierarchy. The deployment shape is "operator-run
private/public cloud control plane", not a hosted-as-a-service appliance.

## §2 Scope and intended use

### Intended use

- A multi-tenant IaaS control plane deployed by an operator inside a
  controlled datacenter or cloud, exposing compute / storage / network
  orchestration to authenticated end users via a JSON API and a Vue.js Web
  UI, with separately authenticated administrators *(documented: `README.md`,
  `INSTALL.md`)*.
- Used both for service-provider public clouds and for on-premises private
  clouds; the trust model is the same in both *(documented: `README.md`)*.

### Deployment shape

CloudStack is **not** an in-process library, **not** a single-binary
appliance, and **not** a hosted SaaS. It is a distributed control plane:
one or more management-server instances (clustered behind a load balancer
in production), a MariaDB/MySQL database, one usage server, an optional
SecondaryStorageVM/ConsoleProxyVM/VirtualRouter set of system VMs, and a
per-hypervisor-host `cloudstack-agent` (for KVM/Hyper-V/baremetal) or
out-of-process resource bridges (for VMware / XenServer / XCP-ng). The
operator owns the surrounding L2/L3 network (the **management network**,
the **public network**, the **guest network**, the **storage network**)
and the physical hosts. The threat model is therefore that of a clustered
distributed service, not a library *(inferred — §14 Q1)*.

### Caller roles

| Role | Trust level | Notes |
| --- | --- | --- |
| **End-user API client / Web UI user** | untrusted but authenticated | Identity verified via Apache CloudStack-native (password + HMAC-SHA1 signed request), LDAP, SAML2, OAuth2, or pluggable `APIAuthenticator` *(documented: `plugins/user-authenticators/{ldap,saml2,oauth2,...}`, `server/src/main/java/com/cloud/api/ApiServer.java` `verifyRequest`)*. |
| **Domain / Project admin** | partial trust within their domain | Bounded by RBAC (`plugins/acl/{static,dynamic,project}-role-based`) and the domain hierarchy; can manage users / VMs / networks within a domain. |
| **Root admin** | trusted control plane | Global RBAC role; can change global configuration, upload templates/ISOs, run privileged orchestration. |
| **Operator / cluster admin** | trusted | OS-level access to management-server hosts, the MariaDB database, the keystore, and the agent hosts. Sets `agent.properties`, manages `cloudstack-agent` packages, manages the JCEKS keystore used by the agent for TLS *(documented: `agent/conf/agent.properties`, `framework/security/.../KeystoreManager.java`)*. |
| **Hypervisor agent (cloudstack-agent on KVM/Hyper-V/baremetal)** | trusted-once-enrolled peer | Mutually authenticated via X.509 client cert signed by the management server's Root CA *(documented: `framework/ca/`, `plugins/ca/root-ca/`, `agent/src/main/java/com/cloud/agent/Agent.java` `setupAgentKeystore`)*. |
| **System VM (SSVM / CPVM / VR)** | trusted-once-enrolled peer | Same X.509 enrolment shape as the agent; carries the agent binary inside *(inferred — §14 Q2)*. |
| **Hypervisor host (the underlying KVM/VMware/etc.)** | trusted by virtue of operator-controlled provisioning | CloudStack expects to drive the hypervisor via libvirt / VMware vSphere SDK / XenAPI as a privileged user *(documented: `plugins/hypervisors/kvm/`, `plugins/hypervisors/vmware/`, `plugins/hypervisors/xenserver/`)*. |
| **Hypervisor-managed guest VM (end-user workload)** | **untrusted** | A guest VM is an attacker's workload; the model defends against it. |
| **Reverse proxy / load balancer in front of management server** | trusted *(if `useForwardHeader=true`)* | When the operator enables forward-header processing, only requests from IPs in `proxy.forward.list` have their forward header honoured *(documented: `server/src/main/java/com/cloud/api/ApiServlet.java` `getClientAddress`)*. |
| **Underlying storage (primary / secondary)** | trusted by virtue of operator-granted credentials | CloudStack reads/writes via NFS / RBD / iSCSI / S3 with operator-supplied credentials *(documented: primary/secondary storage plugins under `plugins/storage/`)*. |
| **External integrations (Tungsten, NSX, Netscaler, Palo Alto, …)** | trusted control-plane peers | Operator-configured; CloudStack assumes truthful responses *(inferred — §14 Q3)*. |

### Component-family table

| Family | Representative entry point | Touches outside the process? | In-model? |
| --- | --- | --- | --- |
| Management server JSON API | `client/.../ApiServlet`, HTTPS on `:8080` (admin), `:8080/client/api` (user), HTTPS on `:8443` integration port *(documented: `server/src/main/java/com/cloud/api/ApiServlet.java`, `client/`)* | network (TCP, optionally TLS) | **yes** |
| Management server Web UI | Vue.js SPA under `ui/`, served by the same servlet container *(documented: `ui/`)* | network | **yes** (auth is the API auth) |
| Management server cluster RPC (peer-to-peer) | NIO + TLS between management-server replicas, `:9090` *(documented: `framework/cluster/`, `utils/.../nio/`)* | network | **yes** (peer auth via Root CA) |
| Management server → agent RPC | NIO + TLS on `:8250` (default `agent.properties`) *(documented: `agent/conf/agent.properties` line 47, `utils/.../nio/NioServer.java`)* | network | **yes** (mutually authenticated via Root CA) |
| `cloudstack-agent` (KVM/Hyper-V/baremetal) | reverse-connects to management server, runs commands via libvirt / hypervisor SDK *(documented: `agent/`, `plugins/hypervisors/kvm/`)* | network + hypervisor + OS | **yes** |
| System VMs — SecondaryStorageVM, ConsoleProxyVM, Virtual Router | shipped images under `systemvm/`; agent binaries inside them *(documented: `systemvm/`)* | network (storage / public / guest) | **yes** |
| Console proxy data path | browser ↔ ConsoleProxyVM ↔ hypervisor VNC/SPICE socket; signed token issued by management server *(documented: `server/src/main/java/com/cloud/servlet/ConsoleProxyServlet.java`, `server/src/main/java/com/cloud/servlet/ConsoleProxyPasswordBasedEncryptor.java`)* | network | **yes** |
| Secondary-storage HTTP (templates, ISO downloads, snapshot copies) | SSVM serves HTTPS *(inferred — §14 Q4)* | network | **yes** |
| Hypervisor plugins (`plugins/hypervisors/{kvm,vmware,xenserver,hyperv,ovm,ovm3,baremetal,ucs,simulator}`) | invoked by agent or by management server *(documented: `plugins/hypervisors/`)* | hypervisor APIs | **yes** for the call shape; **out-of-model** for the upstream hypervisor's own bugs |
| Network plugins (`plugins/network-elements/{netscaler,nsx,palo-alto,tungsten,nicira-nvp,...}`) | management server outbound | external SDN/firewall APIs | **yes** for credential handling and request construction; **out-of-model** for the external endpoint |
| Storage plugins (`plugins/storage/{volume,image,object}`) | management server / agent | NFS, RBD, iSCSI, S3 endpoints | **yes** for credential handling; **out-of-model** for the storage endpoint |
| User authenticator plugins (`plugins/user-authenticators/{md5,sha256salted,pbkdf2,plain-text,ldap,saml2,oauth2}`) | management server | LDAP / SAML2 IdP / OAuth2 IdP | **yes** for the local code; **out-of-model** for the IdP |
| RootCA provider (`plugins/ca/root-ca/`) | self-signed CA generated by management server at first boot, issues certs to agents *(documented: `plugins/ca/root-ca/.../RootCAProvider.java`)* | none directly | **yes** |
| Two-factor authenticators (`plugins/user-two-factor-authenticators/{static-pin,totp}`) | management server | none | **yes** |
| Backup providers (`plugins/backup/`) | management server outbound | external backup endpoints | **yes** for credential handling |
| Quota / metrics / DRS / HA planners | internal | none | **yes** as orchestration only; not a security boundary |
| Database layer (MariaDB/MySQL, Jasypt-encrypted secrets) | management server | network to DB | **yes** for credential handling; DB itself is trusted *(documented: `README.md` "Notice of Cryptographic Software" — JaSypt, native DB encryption)* |
| `cloud-cli`, `tools/marvin`, `test/`, `developer/`, `quickcloud/` | integration / test tooling | varies | **out of model** *(§3)* |
| `systemvm/agent/noVNC/vendor/pako`, other vendored JS / shell scripts | vendored upstream | n/a | in-model only at the wrapper boundary; upstream bugs go upstream *(inferred — §14 Q5)* |

## §3 Out of scope (explicit non-goals)

CloudStack is not, and does not aim to be, the following — reports
requiring any of these will be closed with the cited disposition:

1. **A defender against the operator.** Anyone with `root` on a
   management-server host, `root` on a hypervisor host, raw MariaDB
   credentials, the JCEKS keystore + `security.encryption.key` /
   `security.encryption.iv` *(documented:
   `framework/security/.../KeysManager.java`)*, or the Root CA private key
   already has unbounded power. "The operator misconfigured X" is not a
   vulnerability *(inferred — §14 Q6)*. → `OUT-OF-MODEL:
   adversary-not-in-scope`.
2. **A defender against a malicious external service the operator
   configured.** A hostile LDAP server, SAML IdP, OAuth IdP, Tungsten /
   NSX / Netscaler controller, S3 endpoint, Ceph cluster, or backup
   provider is treated as a trusted control-plane peer. If the report
   requires that peer to be hostile, it is out of model *(inferred —
   §14 Q3)*. → `OUT-OF-MODEL: trusted-input`.
3. **A defender against the hypervisor.** CloudStack drives KVM / VMware /
   XenServer / XCP-ng / Hyper-V via their own admin APIs. A hypervisor
   bug that allows guest escape, a vSphere SDK vulnerability, a libvirt
   privilege escalation — all are upstream to the hypervisor project, not
   to CloudStack *(inferred — §14 Q7)*. → `OUT-OF-MODEL:
   unsupported-component` (upstream pointer).
4. **An isolation boundary between an authorized administrator's API
   call and the management server process.** Root admin can change global
   configuration, upload templates and scripts to system VMs, register
   arbitrary network/storage plugins, and run `runCustomAction`-style
   commands. A new way for a root admin to do something they are already
   authorized to do is not a vulnerability *(inferred — §14 Q8)*. →
   `OUT-OF-MODEL: equivalent-harm`.
5. **A defender against a guest VM doing things the hypervisor allows it
   to do.** A guest VM consuming CPU, memory, or disk up to its allocated
   limit, sending arbitrary IP traffic within its assigned VLAN / VXLAN /
   security group, or exploiting another VM via the hypervisor's own
   shared surfaces (sidechannel, RowHammer, GPU passthrough leak) is out
   of model. CloudStack is responsible only for the orchestration that
   *places* the guest, not for hypervisor-level isolation *(inferred —
   §14 Q9)*. → `OUT-OF-MODEL: adversary-not-in-scope` for the
   side-channel case, `BY-DESIGN: property-disclaimed` for the
   resource-limit case.
6. **A sandbox for templates, ISO images, or user-data scripts.** A
   user-uploaded template (via `registerTemplate`) is run by the
   hypervisor with the privileges the offering grants. cloud-init /
   user-data / metadata is passed through to the guest; CloudStack does
   not parse or sanitize its semantics *(documented: kubernetes-service
   plugin `userdata` references; inferred — §14 Q10)*. →
   `BY-DESIGN: property-disclaimed`.
7. **Code that ships but is not part of the supported product:**
   `tools/marvin/`, `test/`, `developer/`, `quickcloud/`, `cloud-cli/`,
   `tools/devcloud4/`, `tools/devcloud-kvm/`, `tools/appliance/`,
   `tools/checkstyle/`, `tools/transifex/`, `services/`-side simulators,
   `simulator` hypervisor plugin, and IDE / build helpers under `tools/`.
   *(inferred — §14 Q11)*. → `OUT-OF-MODEL: unsupported-component`.
8. **Bundled / vendored upstream libraries** — JaSypt, Bouncy Castle,
   JSch, OpenSwan, noVNC + `pako`, MariaDB Connector/J, Spring,
   Apache Commons, log4j, etc. *(documented: `README.md` Cryptographic
   Software notice)*. Where CloudStack vendors source, the vendored code
   is modeled at the wrapper boundary; vulnerabilities intrinsic to the
   upstream project should be reported upstream *(inferred — §14 Q5)*.
   → `OUT-OF-MODEL: unsupported-component` (with an upstream pointer).
9. **The four satellite repos** (`apache/cloudstack-cloudmonkey`,
   `apache/cloudstack-go`, `apache/cloudstack-terraform-provider`,
   `apache/cloudstack-kubernetes-provider`) — covered by their own delta
   threat models which inherit §3 / §4 / §7 from this document.
10. **The CloudStack documentation site, Confluence wiki, downloads
    mirrors, Docker Hub images outside `apache/cloudstack-*`, gem /
    npm / PyPI packages with similar names, and other non-product
    surfaces.** Out of scope.

## §4 Trust boundaries and data flow

CloudStack has at least nine distinct trust transitions; a finding is
in-model only when it cleanly maps to one of them.

| # | Transition | Authentication | Authorization |
| --- | --- | --- | --- |
| B1 | API client → management server JSON API (`:8080`/`:8443`) | per-user API key + HMAC-SHA1 signature over query string, or session login + 2FA *(documented: `server/src/main/java/com/cloud/api/ApiServer.java` `verifyRequest`)*; signature version 3 has expiration enforcement *(documented: same file line ~1053)* | RBAC (dynamic-role-based / static-role-based / project-role-based) on the called API command name + domain/account ownership of named resources |
| B2 | Web UI → management server (`:8080`) | same as B1 plus session cookie | same as B1 |
| B3 | Browser → ConsoleProxyVM → hypervisor VNC socket | signed token issued by management server, embedded in URL; encrypted with `ConsoleProxyPasswordBasedEncryptor` *(documented: `server/src/main/java/com/cloud/servlet/ConsoleProxyServlet.java`, `ConsoleProxyPasswordBasedEncryptor.java`)* | implicit (signed-token possession) |
| B4 | Management server ↔ management server (cluster peers) | NIO + TLS, Root CA-issued certs *(documented: `framework/cluster/`, `framework/ca/`)* | peer-trust by valid cert |
| B5 | Management server → `cloudstack-agent` (KVM/Hyper-V/baremetal) | NIO + TLS on `:8250`; agent uses X.509 client cert issued by Root CA on first connect; cert provisioning is the `SetupKeyStoreCommand` shape *(documented: `agent/src/main/java/com/cloud/agent/Agent.java` `setupAgentKeystore`, `framework/ca/.../CAService.java`, `plugins/ca/root-ca/.../RootCAProvider.java`)*; trust strictness governed by `ca.plugin.root.auth.strictness` (**default `false`** — see §5a) and `ca.plugin.root.allow.expired.cert` (**default `true`** — see §5a) | peer-trust by valid cert |
| B6 | Management server → external services (LDAP / SAML2 / OAuth2 IdP, NSX, Netscaler, Tungsten, S3, backup providers) | per-provider (service account, OAuth token, etc.) | external-service-side |
| B7 | Agent → hypervisor (libvirt / vSphere SDK / XenAPI) | local Unix socket (libvirt) or operator-supplied SDK credentials | hypervisor-side |
| B8 | Management server / agent → primary/secondary storage (NFS, RBD, iSCSI, S3) | OS-level (NFS), Ceph cephx, iSCSI CHAP, IAM key / static credential (S3) | storage-side |
| B9 | Operator → management server config (`db.properties`, `server.properties`, JCEKS keystore, global config table) | filesystem permissions on the host + DB access | OS-level + DB-level |

### Reachability preconditions per family

For each family in §2, a finding is in-model only if it is reachable as
follows:

- **Management server JSON API**: reachable from an *unauthenticated* network
  peer who can reach `:8080` / `:8443`. Findings that require an
  authenticated peer collapse to "authenticated user with RBAC privilege
  X", and must additionally either clear RBAC for the harmful command or
  bypass it.
- **Web UI**: same shape as the JSON API; the Vue.js SPA is a presentation
  layer over the API.
- **Cluster RPC (B4)**: reachable from a peer that has cleared the Root CA
  trust check. A flat "cluster RPC has no auth" finding is `OUT-OF-MODEL:
  adversary-not-in-scope` because the model *requires* the Root CA to be
  enrolled across peers; a *cleartext* cluster RPC finding is gated by the
  `ca.plugin.root.auth.strictness` default (see §5a, §14 Q12).
- **Management ↔ agent (B5)**: reachable from a peer that presents a
  Root-CA-signed certificate the management server accepts. Crucially, the
  default of `ca.plugin.root.auth.strictness = false` means the management
  server *does not require* a client certificate from the connecting agent
  by default *(documented: `plugins/ca/root-ca/.../RootCAProvider.java`
  line 132–135; `RootCACustomTrustManager.java`)*; this is the highest-
  leverage configuration default in the model.
- **Console proxy (B3)**: reachable by anyone who holds a valid signed
  token. The token is the entire authorization gate.
- **Agent → hypervisor (B7)**: reachable only on the agent host, by code
  the agent runs.
- **External integrations (B6)**: reachable from the management server's
  outbound posture; a hostile external service is `OUT-OF-MODEL:
  trusted-input` (§3 item 2).

## §5 Assumptions about the environment

- **Operating system (management server / usage server)**: RHEL 8/9/10,
  CentOS 8/9, Rocky 9/10, Ubuntu 22.04/24.04, SUSE 15, openSUSE Leap 15;
  Java 17 (`README.md`, `INSTALL.md`, `packaging/{el8,el9,el10,debian,suse15}`).
- **Operating system (agent)**: same family on KVM/Hyper-V/baremetal hosts;
  agent ships as `cloudstack-agent` package *(documented: `debian/`,
  `packaging/`)*.
- **Database**: MariaDB or MySQL-compatible, accessible from each
  management-server instance; CloudStack uses native DB encryption +
  JaSypt for application-level secrets *(documented: `README.md`
  "Notice of Cryptographic Software")*.
- **Cryptography**: JaSypt (application-secret encryption), Bouncy Castle
  (general-purpose crypto, X.509 issuance in the Root CA provider), JSch
  (SSH client to system VMs), OpenSwan (optional VPN endpoint termination)
  *(documented: `README.md` Cryptographic Software notice)*.
- **Network**: operator-controlled L2/L3 with at least the management
  network, public network, guest network, and storage network as logical
  fabrics *(documented: CloudStack admin documentation; inferred —
  §14 Q13)*. The management network is the trusted control-plane
  network; the guest network carries untrusted guest VM traffic.
- **Time**: signature version 3 enforces an `expires` parameter on signed
  API requests *(documented: `ApiServer.java` line ~1054)*; this assumes
  loosely-synchronized clocks between client and management server
  *(inferred — §14 Q14)*.
- **Filesystem**: the JCEKS keystore, `db.properties`, `server.properties`,
  and Root CA private key are stored under `/etc/cloudstack/management/`
  with OS-level permissions restricted to the `cloudstack` user
  *(inferred — §14 Q15)*.
- **Hypervisor**: each supported hypervisor is assumed to provide its own
  guest isolation (memory, vCPU, disk, network) and to expose a stable
  admin API (libvirt for KVM, vSphere SDK for VMware, XenAPI for
  XenServer/XCP-ng, WinRM/Hyper-V API for Hyper-V).
- **What CloudStack does to its host** (negative claims, awaiting
  maintainer ratification):
  - **does** open listening sockets on documented ports
    (`:8080`/`:8443`/`:8250`/`:8096`/`:9090`/console-proxy ports) *(documented)*;
  - **does** maintain MariaDB connections from the management server;
  - **does** issue X.509 certificates from its self-signed Root CA *(documented:
    `plugins/ca/root-ca/.../RootCAProvider.java`)*;
  - **does** spawn child processes from the agent (`Script` invocations
    against `/usr/share/cloudstack-common/scripts/`) *(documented:
    `agent/src/main/java/com/cloud/agent/Agent.java` `keystoreSetupSetupPath`,
    `keystoreCertImportScriptPath`)*;
  - **does** write logs under operator-configured locations;
  - **does** read a documented set of environment variables and the
    `db.properties` file at startup *(inferred — §14 Q16)*;
  - **does** install signal handlers / shutdown hooks only as
    Tomcat/Jetty servlet container default *(inferred — §14 Q16)*.

## §5a Build-time and configuration variants

CloudStack ships as a family of `cloudstack-management`, `cloudstack-agent`,
`cloudstack-usage`, `cloudstack-cli`, `cloudstack-ui` packages
*(documented: `debian/`, `packaging/`)*. A sizable number of runtime
configuration knobs materially change the security envelope. The
security-relevant subset:

| Knob | Default | Maintainer stance | Effect |
| --- | --- | --- | --- |
| `ca.plugin.root.auth.strictness` | **`false`** *(documented: `RootCAProvider.java` line 132)* | **maintainer ruling required**: is the default a supported production posture or a dev-mode setting operators must flip per §10? *(inferred — §14 Q12)* | When `false`, the management server's `RootCACustomTrustManager` does **not** require a client certificate from a peer attempting to connect on `:8250` (agent port) or cluster ports. A peer without a cert is allowed in. |
| `ca.plugin.root.allow.expired.cert` | **`true`** *(documented: `RootCAProvider.java` line 138)* | **maintainer ruling required** *(inferred — §14 Q12)* | When `true`, an expired client cert is accepted during SSL handshake. |
| `ca.plugin.root.issuer.dn` | `CN=ca.cloudstack.apache.org` *(documented: same file line 128)* | configured at first management-server boot | Subject DN of the auto-generated self-signed Root CA. |
| `useforwardheader` (`use.forward.header`) | `false` *(inferred — §14 Q17)* | When `true`, the operator must restrict `proxy.forward.list` to the trusted reverse-proxy CIDR | When set, `ApiServlet.getClientAddress` honours `X-Forwarded-For` / configured headers *only* for source IPs in `proxy.forward.list` *(documented: `server/src/main/java/com/cloud/api/ApiServlet.java` lines 700–725)*. |
| `proxy.forward.list` | unset *(inferred — §14 Q17)* | required when `useforwardheader=true` | CIDR list of trusted reverse proxies. |
| `enable.2fa.for.users` / `enable.2fa.for.api` | per-domain toggle *(documented: `plugins/user-two-factor-authenticators/`)* | dev-test default off; production posture depends on PMC ruling *(inferred — §14 Q18)* | When on, users must complete static-pin or TOTP 2FA after login. |
| `security.encryption.key`, `security.encryption.iv` | auto-generated at first boot *(documented: `framework/security/.../KeysManager.java`)* | trusted secret | Base64-encoded JaSypt master key + IV used to encrypt application-level secrets in the DB. |
| `auth.password.algorithm` (`hash.user.password`) | bcrypt / pbkdf2 / sha256salted *(documented: `plugins/user-authenticators/{pbkdf2,sha256salted}`)* | **maintainer ruling required**: which is the supported default for new deployments? `md5` and `plain-text` plugins still ship *(documented: `plugins/user-authenticators/{md5,plain-text}`)* — are these legacy-compat-only or in supported production? *(inferred — §14 Q19)* | governs how user passwords are stored |
| `api.signature.version` | accepts both v1 and v3 *(documented: `ApiServer.java` line ~1053)* | v1 lacks an `expires` parameter; v3 requires expiration | A request with v3 + an expired `expires` is rejected; a v1 request without `expires` is accepted |
| `post.requests.and.timestamps.enforced` | per `isPostRequestsAndTimestampsEnforced` *(documented: `ApiServer.java` line ~1074)* | bounds `expires` to a maximum future offset | Prevents an attacker who steals a signed URL with a 10-year expiration from using it forever |
| `integration.api.port` (`:8096`) | typically disabled *(inferred — §14 Q20)* | When non-zero, exposes an *unauthenticated* admin API for integration testing | An open integration port is a complete RBAC bypass on the management server |
| Hypervisor enablement (which `plugins/hypervisors/*` are installed and configured) | per zone | operator-driven | An unused hypervisor plugin still ships but is not connected to any host |
| Hostname / SAN of management-server cert (`ca.plugin.root.management.cert.custom.san`) | unset *(inferred — §14 Q15)* | when set, included in the auto-generated cert SAN | governs which hostnames clients can use to reach the management server |
| SAML2 / OAuth2 enablement (`plugins/user-authenticators/{saml2,oauth2}`) | off *(inferred — §14 Q19)* | turning on adds an external IdP trust dependency | adds B6 transitions |
| LDAP enablement (`plugins/user-authenticators/ldap`) | off *(inferred — §14 Q19)* | turning on adds an external LDAP trust dependency | adds B6 transitions |

**The insecure-default case (highest leverage).** `ca.plugin.root.auth.strictness`
defaulting to `false` and `ca.plugin.root.allow.expired.cert` defaulting to
`true` are the two highest-leverage defaults in the entire model. Whether a
report against an open `:8250` accepting an un-certed peer is `VALID` or
`OUT-OF-MODEL: non-default-build` depends on the maintainer ruling in
§14 Q12. The text of §3 item 1, §10, and §11a assume the answer
is **"dev/test default, operator must flip both knobs to `true` /
`false` respectively per §10 for production"**.

## §6 Assumptions about inputs

### Per-endpoint trust table (network surfaces)

| Surface / route | Parameter | Attacker-controllable? | Caller must enforce |
| --- | --- | --- | --- |
| Management server `:8080`/`:8443` JSON API | command name + params | **yes** | nothing — CloudStack parses, authenticates (B1), applies RBAC, dispatches |
| Management server `:8080`/`:8443` JSON API | `signature` parameter | **yes** | HMAC-SHA1 verified *constant-time* against expected signature *(documented: `ApiServer.java` line 1137 `ConstantTimeComparator.compareStrings`)* |
| Management server `:8080`/`:8443` JSON API | `expires` parameter (sig v3) | **yes** | rejected if past, or beyond `post.requests.and.timestamps.enforced` ceiling *(documented: same file)* |
| Management server `:8080`/`:8443` JSON API | `X-Forwarded-For` and other configured forward headers | **yes** if `useforwardheader=true` | honoured **only** if request source IP ∈ `proxy.forward.list` *(documented: `ApiServlet.java` line 712)* |
| Management server `:8080`/`:8443` Web UI | session cookie | **yes** | session-fixation / invalidation handled via `invalidateHttpSession` on auth failure *(documented: `ApiServlet.java` line 418)* |
| Integration API `:8096` (if enabled) | command name + params | **yes** | **no signature check** — integration port is unauthenticated by design |
| Management ↔ agent `:8250` | NIO Thrift-like payload | **only by a peer that has cleared B5 trust** | client cert via `RootCACustomTrustManager` |
| Management ↔ cluster peer | NIO payload | **only by a peer that has cleared B4 trust** | client cert via `RootCACustomTrustManager` |
| Console proxy URL | encrypted token (containing VM identity + endpoint + duration) | **yes** | token MUST decrypt + verify with `ConsoleProxyPasswordBasedEncryptor` keys *(documented: `ConsoleProxyPasswordBasedEncryptor.java`)* |
| Secondary-storage HTTP download URL | path + signed parameters | **yes** | token / per-template ACL check *(inferred — §14 Q4)* |
| Template / ISO upload | URL of remote source | **yes** within RBAC | upload-gated by `registerTemplate` RBAC; bytes are then served to hypervisors as image data |
| User-data / metadata service (`169.254.169.254` from inside guests) | guest-controlled bytes (the request) | **yes from the guest**, but the service is reached *from the guest* and serves only that guest's data | guest-VM-side isolation by virtual router |
| Hypervisor agent log / event stream | bytes from hypervisor | trusted operator surface | none — assumed truthful |
| LDAP / SAML / OAuth response (B6) | bytes from IdP | trusted | LDAP queries treat returned attributes as authoritative |
| Storage response (B8) | bytes / metadata from storage | trusted | bytes are object content; envelope is control-plane |

### Size / shape / rate

- CloudStack does not document a maximum signed-API request size; assumed
  to be servlet-container default (Jetty / Tomcat) *(inferred — §14 Q21)*.
- API rate limiting is per-account via the global config knobs `api.throttling.*`
  *(inferred — §14 Q22)*; an attacker with a valid API key can be rate-
  limited at the application layer.
- Template / ISO upload size is bounded by storage capacity and per-account
  resource limits *(inferred — §14 Q22)*; pathological compressed-image
  inputs (e.g. extremely compressible QCOW2 with sparse holes that expand
  to TB on extraction) are robustness concerns *(inferred — §14 Q23)*.
- Cluster-peer and agent RPC payload sizes: no documented application-layer
  cap; NIO framing applies *(inferred — §14 Q21)*.

## §7 Adversary model

### Actors

| Actor | In scope? | Capabilities granted |
| --- | --- | --- |
| Unauthenticated network peer reaching `:8080`/`:8443` | **yes** | TCP to the listening ports; may attempt authentication; may attempt to violate the protocol pre-auth |
| Unauthenticated peer reaching `:8250` (agent port) | **yes** *if* `ca.plugin.root.auth.strictness` is at default `false` (§5a) | TCP to the listening port; may attempt to connect as a peer without presenting a cert |
| Unauthenticated peer reaching `:8096` (integration port) | **yes** *if* the port is open (typically not in production) | full unauthenticated admin API |
| Authenticated end user with limited RBAC role | **yes** | call APIs their role permits; manage VMs/networks/storage in their domain/account/project |
| Authenticated end user with broad RBAC role | partial | only RBAC-envelope escapes are in scope |
| Authenticated domain admin | **yes** | full management within their domain; cross-domain leakage is in scope |
| Authenticated root admin | **out of scope** — see §3 item 4 | unbounded by design |
| Co-tenant (different account in same domain or different domain on same CloudStack) | **yes** | cross-tenant leakage (VM ID guessing, network bleed, storage bleed, template visibility) is in scope |
| Guest VM workload | **partial** | hypervisor-mediated; out-of-scope for hypervisor isolation bugs (§3 item 5), in-scope for the orchestration that placed the VM (security-group rule application, VLAN tagging, public IP routing) |
| Browser holding a valid console-proxy URL | **yes** | the URL is a bearer credential; scope of harm is one VM's console for the URL's lifetime |
| Operator | **out of scope** — see §3 item 1 |
| Hostile hypervisor | **out of scope** — see §3 item 3 |
| Hostile LDAP / SAML / OAuth IdP, hostile NSX/Netscaler/Tungsten, hostile S3 endpoint | **out of scope** — see §3 item 2 |
| Reverse proxy that should be trusted but is not in `proxy.forward.list` | **out of scope** — its forward headers are not honoured |
| Local process on the management-server host running as a different UID | **partial** *(inferred — §14 Q24)*: same-host attackers with non-cloudstack UID can reach `:8080` unless host firewalling forbids; CloudStack does not defend against same-host `root` |
| Side-channel observer (cache timing, network timing, hypervisor side channels) | **out of scope** *(inferred — §14 Q25)* |
| Quantum adversary | **out of scope** |

### Authenticated-but-Byzantine peer (distributed-systems threshold)

CloudStack is **not** a Byzantine-fault-tolerant system. A compromised
management-server cluster peer with a valid Root-CA-issued cert can
schedule arbitrary work onto the agent fleet, read any guest's data, and
hand out console-proxy tokens. The cluster trusts its own membership
*(inferred — §14 Q26)*. Likewise, a compromised agent host can serve
malicious data on the management network and produce wrong status. →
reports requiring a Byzantine internal peer are `OUT-OF-MODEL:
adversary-not-in-scope`.

## §8 Security properties the project provides

For each property: condition, violation symptom, severity tier, provenance.

### P1 — Authentication of API clients via signed request

- **Condition**: a request carries `apiKey` + `signature` (and, for
  signature version 3, an `expires` parameter not in the past)
  *(documented: `ApiServer.java` `verifyRequest`)*; the signature is
  HMAC-SHA1 of the canonical parameter string under the per-user
  secret key, base64-encoded, lowercased, URL-decoded, and compared
  to the computed value using `ConstantTimeComparator.compareStrings`
  *(documented: same file line 1137)*.
- **Violation symptom**: a request executes API commands without a
  valid `apiKey`+`signature` pair (and without a valid session
  cookie / SAML / OAuth / LDAP login).
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P2 — Session authentication via password + optional 2FA

- **Condition**: user logs in via the `login` API; 2FA is verified after
  password if enabled for the user / domain *(documented: `ApiServlet.java`
  lines 360–582)*.
- **Violation symptom**: a session is created without a valid password, or
  2FA enforcement is bypassed for a user where it is mandated.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P3 — Constant-time signature comparison

- **Condition**: applies to the API signature check.
- **Violation symptom**: timing-side-channel measurement of signature
  comparison reveals the expected signature byte-by-byte.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented: `ApiServer.java` line 1137)*

### P4 — Authorization via RBAC + domain/account/project hierarchy

- **Condition**: the authenticated principal calls an API command, and the
  command name is permitted for their role *(documented: `plugins/acl/{static,dynamic,project}-role-based`)*;
  resources named in the request belong to the principal's domain/account/project
  or to a child within the principal's scope.
- **Violation symptom**: a non-root principal successfully executes an
  API command not licensed for their role, or operates on a resource
  outside their domain/account/project scope.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P5 — Mutual TLS on management ↔ agent, management ↔ cluster peer, *when configured*

- **Condition**: `ca.plugin.root.auth.strictness = true` and
  `ca.plugin.root.allow.expired.cert = false` *(documented:
  `RootCAProvider.java` lines 132–142)*. Without these (i.e., at default)
  the property is voided per §5a / §14 Q12; the management server still
  accepts cleartext-/un-certed peers.
- **Violation symptom**: a peer without a Root-CA-issued cert (or with an
  expired one) successfully completes a session on `:8250` or the
  cluster port despite both flags being flipped to strict.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented for the strict configuration; the default-state coverage
  depends on the §14 Q12 ruling.)*

### P6 — Reverse-proxy IP-trust gating for forward headers

- **Condition**: `useforwardheader = true` *(inferred — §14 Q17)*; only
  source IPs in `proxy.forward.list` have their `X-Forwarded-For` (or
  configured equivalent) header consulted *(documented: `ApiServlet.java`
  line 712 `NetUtils.isIpInCidrList`)*.
- **Violation symptom**: a request from a source IP **outside**
  `proxy.forward.list` succeeds with an attacker-supplied forward header
  taking effect.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P7 — Console-proxy token confidentiality and integrity

- **Condition**: tokens are encrypted under the
  `ConsoleProxyPasswordBasedEncryptor` keys *(documented:
  `ConsoleProxyPasswordBasedEncryptor.java`)*; a token includes the VM
  identity, the hypervisor endpoint, and a duration / expiry.
- **Violation symptom**: a third party with no console-access RBAC
  privilege forges or decrypts a token to gain console access; or a token
  remains valid past its declared expiry.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P8 — Application-secret encryption at rest in the DB via JaSypt

- **Condition**: `security.encryption.key` + `security.encryption.iv` are
  initialised at first boot and kept under filesystem ACLs
  *(documented: `framework/security/.../KeysManager.java`,
  `README.md` Cryptographic Software notice)*.
- **Violation symptom**: an attacker with read access to the DB but not
  to the encryption key file recovers plaintext for secrets the model
  claims are encrypted (typically: external service passwords, account
  API secret keys when stored encrypted).
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented)*

### P9 — Memory safety on well-formed inputs across documented surfaces (JVM-bounded)

- **Condition**: input matches the documented protocol on B1–B5; the JVM
  is conformant; native code is invoked only via documented hypervisor
  SDKs (libvirt / vSphere / XenAPI) — CloudStack's own server-side code
  is Java *(inferred — §14 Q27)*.
- **Violation symptom**: heap corruption, OOM-via-input-size attack on a
  surface where the input source is `:8080` / `:8443` / B5; JVM-side
  crashes from a request a normally-RBAC'd user could send.
- **Severity**: **security-critical** when reachable from network input;
  **`VALID-HARDENING`** when reachable only by a writer who already
  controls the bytes (§3 item 5).
- *(inferred — §14 Q27)*

### P10 — Bounded RBAC scope of cross-domain visibility (`SHOW`-equivalent listing)

- **Condition**: `list*` API commands filter responses to the principal's
  domain/account/project scope per `plugins/acl/` policy.
- **Violation symptom**: a `list*` response leaks resource IDs / names /
  metadata for resources outside the principal's RBAC scope.
- **Severity**: **security-critical** for resources whose existence is
  itself confidential (typically: customer VM names, custom template
  names); `VALID` per §13.
- *(inferred — §14 Q28)*

## §9 Security properties the project does *not* provide

State each plainly so a triager can route an inbound report to the matching
disclaimer.

- **No defence against the operator.** Anyone with root on a
  management-server host, the JCEKS keystore + `security.encryption.key`,
  the Root CA private key, or the MariaDB credentials wins. See §3 item 1
  *(inferred — §14 Q6)*.
- **No defence against a malicious external service the operator
  configured.** A hostile LDAP/SAML/OAuth IdP, NSX controller, Tungsten,
  Netscaler, S3 endpoint, or backup provider is trusted. See §3 item 2.
- **No defence against the hypervisor.** Guest VM escape via libvirt,
  vSphere, XenAPI, Hyper-V is upstream. See §3 item 3.
- **No isolation between a root admin's API call and the management-server
  process.** Root admin can register arbitrary plugins, upload arbitrary
  templates, run `runCustomAction`. See §3 item 4 *(inferred — §14 Q8)*.
- **No sandbox for guest VM workloads beyond what the hypervisor provides.**
  Side-channel leaks between co-tenant VMs (cache, branch, memory bus,
  shared GPU) are out of scope. See §3 item 5 *(inferred — §14 Q9)*.
- **No sandbox for user-data / templates / ISOs.** Templates run as their
  own OS image with their own cloud-init; CloudStack does not parse or
  reject user-data semantics. See §3 item 6 *(inferred — §14 Q10)*.
- **No defence against decompression / decoding bombs in uploaded
  templates / ISOs.** A pathological QCOW2 / RAW image can consume
  arbitrary CPU / disk on extraction; per-account resource limits are the
  bound *(inferred — §14 Q23)*.
- **No defence against intra-cluster Byzantine failure.** A compromised
  cluster peer with a valid Root-CA-issued cert can read any data the
  cluster can read; see §7 *(inferred — §14 Q26)*. Likewise a compromised
  agent host.
- **No data-at-rest encryption beyond JaSypt for selected DB columns +
  whatever storage layers provide.** Guest volumes are encrypted only if
  the primary-storage plugin supports it (Ceph RBD encryption, LUKS at
  hypervisor layer) and the operator has configured it *(inferred —
  §14 Q29)*.
- **No defence against side-channel observation** of API request timing,
  agent RPC timing, or memory access patterns *(inferred — §14 Q25)*.
- **No application-layer constant-time comparison of anything other than
  the API signature.** Login password comparison, session cookie
  comparison, console-token comparison — not documented constant-time
  *(inferred — §14 Q30)*.
- **No defender stance against an attacker on the same Linux host running
  as a non-`cloudstack` UID** — CloudStack defends only across the
  network surface; same-host attackers with shell access on the
  management-server host already have many paths to win *(inferred —
  §14 Q24)*.
- **No supported posture for the integration API port (`:8096`).** When
  open, it is an unauthenticated admin surface; closing it is the
  operator's job *(inferred — §14 Q20)*.

### False-friend properties (call out separately)

- **The Root CA is self-signed and auto-generated** — it is *not* a
  publicly-trusted CA. Browsers and external clients require manual trust
  bootstrap. The Root CA private key resides on the management server; a
  compromised management server compromises the entire agent fleet's
  trust.
- **`ca.plugin.root.auth.strictness = false` is not "TLS off" — it is
  "client cert not required"** *(documented: `RootCAProvider.java`)*. TLS
  on the wire is still there; what is missing is the peer-cert check.
  A scanner that flags "client cert not requested" is *correctly*
  identifying a §5a knob default, not a transport-encryption bug.
- **`ca.plugin.root.allow.expired.cert = true` is the operational default
  to survive cert-rotation lag** but is not a security boundary.
- **The HMAC-SHA1 signature is request-integrity over the URL, not
  request encryption.** Transport encryption is TLS; if the operator
  serves the API over `http://`, the signature still validates but the
  whole request (including the secret-derived signature) is visible to
  the network.
- **The console-proxy URL is a bearer credential.** Anyone who sees the
  URL (in logs, in a proxy, in a shoulder-surf) holds the console for
  the URL's lifetime.
- **`list*` filtering is a per-call authorization view, not an
  information-flow channel.** Existence of a resource that the principal
  cannot see may leak through error messages, async-job status, event
  logs, or by-ID lookup probing *(inferred — §14 Q28)*.
- **The integration API port is not a "trusted" port in the sense of
  Kerberos `auth-int` — it is *no authentication at all***. The name
  invites confusion.
- **JaSypt-encrypted DB columns are *(documented)* protected against a
  DB-only read.** They are *not* protected against an attacker who
  obtains both the DB and the encryption-key file.

### Well-known attack classes the project does not defend against

- **Cross-tenant VM-ID guessing / template-name enumeration**: §10 misuse,
  not engine breakage.
- **Decompression / decoding bombs in uploaded templates and ISOs**.
- **Hypervisor side-channel attacks between co-tenant VMs**.
- **Confused-deputy between RBAC role and resource ownership** — e.g. a
  domain admin's role permits a command, but the resource named is in a
  child domain they should not touch *(inferred — §14 Q28)*.
- **Time-of-check-to-time-of-use** between RBAC check at API entry and
  the actual orchestration on the agent fleet — policy revocations
  mid-job are not retroactively enforced *(inferred — §14 Q31)*.

## §10 Downstream responsibilities

The operator deploying CloudStack in production **must**:

1. Set `ca.plugin.root.auth.strictness = true` and
   `ca.plugin.root.allow.expired.cert = false`. Without these, agent and
   cluster-peer ports accept peers without a cert / with expired certs
   *(documented: `RootCAProvider.java`; pending §14 Q12 ruling)*.
2. Restrict the management network at L2/L3 so that `:8250` (agent),
   `:9090` (cluster), and the MariaDB port are reachable only from the
   intended peers *(inferred — §14 Q13)*.
3. Restrict the integration API port `:8096` — either disable it entirely
   or limit it to a localhost/management subnet *(inferred — §14 Q20)*.
4. Terminate TLS for the JSON API and Web UI on `:8443` (not `:8080`); if
   `:8080` is exposed at all, only behind a TLS-terminating reverse
   proxy *(inferred — §14 Q32)*.
5. When using a reverse proxy, set `useforwardheader = true` *and*
   `proxy.forward.list` to the proxy's CIDR — failing to set
   `proxy.forward.list` means the header is ignored (safe-default per P6),
   but a misconfigured wide CIDR is a trust-bypass.
6. Protect the `security.encryption.key` / `security.encryption.iv`
   files, the JaSypt-encrypted DB, the Root CA private key, and the
   `cloudstack-management` Unix user's home directory at OS level.
7. Configure a password authenticator from the supported set (bcrypt /
   pbkdf2 / sha256salted) — **not** `md5` or `plain-text` *(pending §14
   Q19 ruling on whether the legacy plugins are supported)*.
8. Enable 2FA (`totp` or `static-pin`) for administrators and ideally for
   all users *(pending §14 Q18 ruling)*.
9. Rotate per-user API secret keys on personnel change and on suspected
   compromise.
10. Treat user-uploaded templates and ISOs as crossing a trust boundary —
    scan / quarantine before allowing into the supported-template set.
11. Apply per-account resource limits (vCPU / RAM / volume size / image
    size) to bound decompression-bomb and orchestration-DoS attacks.
12. Configure storage-layer encryption (Ceph RBD encryption, LUKS at KVM,
    vSphere VM Encryption, etc.) if data-at-rest encryption is required.
13. Secure each `cloudstack-agent` host: `cloudstack` Unix user, agent
    keystore under `/etc/cloudstack/agent/`, root account, libvirt /
    vSphere admin credentials.
14. Restrict console-proxy URLs: do not log them, do not embed them in
    public responses, set a short token lifetime.
15. Audit API call logs (via the event-bus plugin) for anomalous patterns.

## §11 Known misuse patterns

- **Leaving `:8250` open to the world with `ca.plugin.root.auth.strictness=false`.**
  Any peer can connect as an agent on the default. Pending §14 Q12,
  this is either a `VALID` operator-hardening report or `OUT-OF-MODEL:
  non-default-build`.
- **Exposing `:8096` (integration API) publicly.** Anyone reaching the
  port executes admin API commands without auth.
- **Exposing `:8080` (HTTP JSON API) publicly without a TLS-terminating
  reverse proxy.** Signed-request integrity holds, but the API secret-
  key-derived signature is visible to any wire observer; replay within
  the `expires` window is trivial.
- **Setting `useforwardheader=true` with `proxy.forward.list` wider than
  the actual reverse-proxy CIDR.** An attacker outside the proxy can
  spoof `X-Forwarded-For` and claim any IP address for audit logs and
  authentication-IP checks.
- **Using the `md5` or `plain-text` user authenticator plugin in
  production.** Both still ship in `plugins/user-authenticators/`. Pending
  §14 Q19 on whether they are legacy-compat-only.
- **Granting domain admin to too many users.** A domain admin can manage
  all accounts within the domain — including reading guest console URLs.
- **Embedding console-proxy URLs in screenshots, ticketing systems, or
  chat.** Tokens are bearer credentials.
- **Re-using `security.encryption.key` across environments of different
  trust levels.** A staging-env leak becomes a production-env decrypt
  primitive *(inferred — §14 Q33)*.
- **Disabling the cluster-peer TLS by leaving `ca.plugin.root.auth.strictness`
  default in a multi-management-server deployment.** A peer can join the
  cluster without a cert.
- **Uploading large or pathological templates and relying on hypervisor
  to enforce size.** Per-account resource limits, not the engine, are the
  enforcement.

## §11a Known non-findings (recurring false positives)

This section is the highest-leverage input for automated agentic security
scans. Each entry: tool symptom, why it is safe under the model, the §
that licenses the call.

- **"Management ↔ agent port `:8250` accepts plaintext / no client cert"
  against a default `ca.plugin.root.auth.strictness=false`.** This is the
  *documented default*. Pending §14 Q12, this is either `OUT-OF-MODEL:
  non-default-build` (operator must flip) or `VALID-HARDENING` (default
  should change). The model assumes the former until the PMC rules
  otherwise.
- **"Integration port `:8096` is unauthenticated."** The port is
  unauthenticated by design; operator responsibility per §10 to close /
  bind to localhost. → `OUT-OF-MODEL: non-default-build` once the PMC
  confirms.
- **"HMAC-SHA1 signature uses SHA1."** SHA1-HMAC is **not** broken for
  HMAC use; collision attacks on SHA1 do not extend to HMAC-SHA1
  *(documented: cryptographic literature; CloudStack uses
  `Mac.getInstance("HmacSHA1")` — `ApiServer.java` line 1130)*. → `KNOWN-NON-FINDING`.
- **"Constant-time string compare for the signature."** Already done —
  `ConstantTimeComparator.compareStrings` per `ApiServer.java` line 1137.
  → `KNOWN-NON-FINDING` (a finding flagging this is wrong).
- **"Root CA private key is on the management server."** By design — the
  management server *is* the CA. → `BY-DESIGN: property-disclaimed`.
- **"Self-signed Root CA cert."** By design — the CA is generated at
  first boot per `RootCAProvider.java`. Browsers will warn until the
  operator bootstraps trust. → `BY-DESIGN: property-disclaimed`.
- **"Expired agent cert is accepted (`ca.plugin.root.allow.expired.cert=true`)."**
  Documented default. Pending §14 Q12. → `OUT-OF-MODEL:
  non-default-build` once the PMC confirms.
- **"Hardcoded password / keytab in `tools/marvin/`, `test/`, `developer/`,
  `quickcloud/`."** These directories are unsupported components per §3
  item 7. → `OUT-OF-MODEL: unsupported-component`.
- **"User-data / template contents execute arbitrary code in the guest
  VM."** Templates are run as their own OS by the hypervisor; cloud-init
  / user-data is intentionally a code-execution channel into the guest.
  → `BY-DESIGN: property-disclaimed` per §9.
- **"Root admin can change global config / register plugins / upload
  arbitrary templates."** Documented and intentional. → `BY-DESIGN:
  property-disclaimed` per §9 / §3 item 4.
- **"DoS via expensive list call on a large CloudStack."** Pagination is
  present; further bounds are admission-control / quota. → `BY-DESIGN:
  property-disclaimed` per §9.
- **"Decompression bomb in an uploaded QCOW2 / template."** Per-account
  resource limits are the bound. → `VALID-HARDENING` at most, unless the
  decompression reaches §8 P9 memory-safety violations.
- **"Vendored Bouncy Castle / JaSypt / noVNC / `pako` has CVE-X."** Report
  upstream; CloudStack picks up the fix on the next vendored sync. →
  `OUT-OF-MODEL: unsupported-component` (upstream pointer) *(inferred —
  §14 Q5)*.
- **"`X-Forwarded-For` is honoured without authentication."** Honoured
  only if (a) `useforwardheader=true` *and* (b) source IP ∈
  `proxy.forward.list`. → `KNOWN-NON-FINDING`.
- **"Session-fixation: a session ID is reusable after failed login."**
  `invalidateHttpSession` is called on each auth failure path per
  `ApiServlet.java`. → `KNOWN-NON-FINDING` (verify the symptom; if
  reproducible, escalate to `MODEL-GAP`).

## §12 Conditions that would change this model

Revise this document when any of the following lands:

- A new authentication mechanism on a client-facing surface (e.g.
  mTLS-as-API-auth on the JSON API, WebAuthn, OIDC).
- A new RBAC backend beyond the three included ACL plugins (e.g. OPA
  integration, policy-engine integration).
- A new data-at-rest encryption story at the CloudStack layer (currently
  delegated; see §9).
- A change in the default of any §5a flag, *especially*
  `ca.plugin.root.auth.strictness` and `ca.plugin.root.allow.expired.cert`.
- Removal or change of the legacy `md5` / `plain-text` user-authenticator
  plugins.
- A change in the signing algorithm or signature scheme on the JSON API
  (e.g. SHA1 → SHA256 by default).
- A new hypervisor or system VM that adds a new trust boundary.
- A new external-data surface (a new SDN controller integration, a new
  storage provider, a new backup provider).
- A vulnerability report that cannot be cleanly routed to one of the §13
  dispositions: that is evidence the model is incomplete.

## §13 Triage dispositions

A report against `apache/cloudstack` receives exactly one of the
following:

| Disposition | Meaning | Licensed by |
| --- | --- | --- |
| `VALID` | Violates a §8 property via an in-scope §7 adversary using an in-scope §6 input. | §8, §6, §7 |
| `VALID-HARDENING` | No §8 property violated, but a §11 misuse pattern can be made harder to fall into by code change. Fixed at maintainer discretion, typically no CVE. | §11 |
| `OUT-OF-MODEL: trusted-input` | Requires attacker control of a §6 parameter the model marks trusted (e.g. operator-supplied config flag, hostile LDAP/SAML/NSX/etc.). | §6 |
| `OUT-OF-MODEL: adversary-not-in-scope` | Requires a §7 actor the model excludes (operator, hostile hypervisor, hostile external IdP / SDN, Byzantine peer, side-channel observer, same-host non-`cloudstack` `root`). | §7 |
| `OUT-OF-MODEL: unsupported-component` | Lands in `tools/marvin/`, `test/`, `developer/`, `quickcloud/`, vendored upstream code, `simulator` hypervisor, etc. | §3 items 7–8 |
| `OUT-OF-MODEL: non-default-build` | Only manifests under a §5a flag the maintainer has ruled is dev/test (e.g. `ca.plugin.root.auth.strictness=false` if so ruled, integration port `:8096` open). | §5a |
| `OUT-OF-MODEL: equivalent-harm` | An actor already-authorized under the model can cause the same harm via a documented path (root admin doing root-admin things, RBAC-licensed user using their RBAC-licensed commands). | §3 items 4, 5 |
| `BY-DESIGN: property-disclaimed` | Concerns a §9 property the project explicitly does not provide (template sandboxing, side-channel resistance, hypervisor isolation, etc.). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a recurring false positive. | §11a |
| `MODEL-GAP` | Cannot be cleanly routed to any of the above — triggers §12 model revision. | §12 |

## §14 Open questions for the maintainers

Every *(inferred)* tag in the body maps to one of these. Proposed answers
are inline; please confirm, correct, or strike.

### Wave 1 — scope, intended use, the two big insecure defaults

**Q1.** The model assumes CloudStack is "a clustered distributed
control plane deployed inside an operator-controlled datacenter
network", not a single-host appliance or a hosted SaaS. Confirm? *(maps
to §2)*

**Q2.** Are the SecondaryStorageVM, ConsoleProxyVM, and Virtual Router
treated as trusted-once-enrolled peers (proposed: **yes**, same shape as
agents), or do they get their own trust tier? *(maps to §2, §4 B5)*

**Q3.** Are external integrations (LDAP, SAML2 IdP, OAuth2 IdP, NSX
controller, Netscaler, Tungsten, S3-compatible storage, backup
providers) modeled as trusted control-plane peers (proposed: **yes**)? If
trusted, that licenses §3 item 2 and §11a trusted-input dispositions.
*(maps to §2, §3, §11a)*

**Q4.** SecondaryStorageVM HTTP download surface — is the URL token
per-template ACL-checked, or is the SSVM URL itself a bearer credential
that any holder can replay? *(maps to §6, §11a)*

**Q5.** Vendored upstream code under `systemvm/agent/noVNC/vendor/pako`
and bundled JaSypt / Bouncy Castle / JSch — is the policy "report
upstream; we pick up fixes on next sync" (proposed)? *(maps to §3 item 8,
§11a)*

**Q6.** Is "an operator with `root` on a management-server host, the
JCEKS keystore + encryption keys, the Root CA private key, or MariaDB
credentials" out of scope (proposed: **yes**, `OUT-OF-MODEL:
adversary-not-in-scope`)? *(maps to §3 item 1, §9)*

**Q7.** Hypervisor bugs (libvirt / vSphere SDK / XenAPI / Hyper-V API /
KVM/QEMU itself) — out of scope, report upstream (proposed)? *(maps to
§3 item 3)*

### Wave 2 — the two big insecure defaults

**Q12.** **Highest-leverage question in the model.** Two Root-CA defaults:

- `ca.plugin.root.auth.strictness` = `false` *(documented:
  `RootCAProvider.java` line 132)* — the management server does **not**
  require a client cert from peers on `:8250` (agent port) and cluster
  ports by default. Is this **(a)** the supported production posture (so
  a report of "agent port accepts un-certed peer" is `VALID` against the
  default, meaning the default should be flipped), or **(b)** a dev/test
  convenience that operators are documented as required to flip per
  §10 (so the report is `OUT-OF-MODEL: non-default-build`)?
- `ca.plugin.root.allow.expired.cert` = `true` *(documented: same file
  line 138)* — expired client certs are accepted. Same question.

This single ruling reshapes §3 item 1, §5a, §7 (the un-certed peer
row), §8 P5, §10, §11 first/penultimate bullets, §11a first two
bullets, and §13. The text of §3 / §10 / §11a in this draft
**assumes the answer is (b)** — operator must flip both per §10.
*(maps to §5a, §10, §11a, §13)*

### Wave 3 — adjacent insecure defaults and admin-only surfaces

**Q8.** Is "a root admin with full RBAC role causes harm Y via a
documented path Z" out of scope (proposed: **yes**, `OUT-OF-MODEL:
equivalent-harm`)? In particular: `runCustomAction`, template upload,
plugin registration, global config change, system-VM patching, system-VM
console access. *(maps to §3 item 4, §9)*

**Q9.** Guest VM workloads — confirm that hypervisor-mediated side
channels and resource-exhaustion-within-allocation are out of scope, and
that the in-scope orchestration concerns are limited to "did CloudStack
place the VM in the right VLAN / apply the right security group / route
the right IP" (proposed)? *(maps to §3 item 5, §7, §9)*

**Q10.** Templates / ISOs / user-data — confirm that there is no
sandboxing of user-supplied OS images, and that user-data is intentionally
a code-execution channel into the guest (proposed)? *(maps to §3 item 6,
§9)*

**Q11.** Confirm the unsupported-component list: `tools/marvin/`,
`test/`, `developer/`, `quickcloud/`, `cloud-cli/`,
`tools/{devcloud4,devcloud-kvm,appliance,checkstyle,transifex,bugs-wiki,...}`,
`simulator` hypervisor plugin. Anything to add or remove? *(maps to §3
item 7)*

**Q17.** `useforwardheader` (`use.forward.header`) default — proposed:
**`false`** by default; operator turns it on only when a reverse proxy
is in front. Confirm, and confirm `proxy.forward.list` is required when
the flag is on. *(maps to §5a, §6, §10)*

**Q18.** 2FA — proposed: off by default, operator turns it on per
domain / per user via `enable.2fa.*`. Confirm; and is "2FA disabled in
production" a §10 violation or a deployment choice? *(maps to §5a,
§10)*

**Q19.** User-authenticator plugins — the repo still ships
`plain-text`, `md5`, `sha256salted`, `pbkdf2`, plus `ldap`, `saml2`,
`oauth2`. Which are supported for new production deployments? Proposed:
`pbkdf2` and `sha256salted` are supported; `md5` and `plain-text` are
legacy-compat for upgrade paths only and a report against them in a
greenfield install is `OUT-OF-MODEL: non-default-build`. Confirm.
*(maps to §5a, §10, §11)*

**Q20.** Integration API port `:8096` — proposed: closed (port-zero) by
default in production packaging, open only when explicitly configured;
when open, it is unauthenticated by design. A report of "integration
port allows admin commands without auth" is `OUT-OF-MODEL:
non-default-build` *if* the operator opened it, else `VALID`. Confirm
the default. *(maps to §5a, §10, §11a)*

### Wave 4 — environment, distributed model, false-friends

**Q13.** Network-fabric assumptions — proposed: at least four logical
networks (management, public, guest, storage), with the management
network as the trusted control plane. Is that the canonical model, or
do you support more compressed topologies (single-fabric) in production?
*(maps to §5, §10)*

**Q14.** Clock-skew assumption for signature v3 `expires` enforcement —
proposed: operator's responsibility to keep client + management-server
clocks roughly in sync. Confirm. *(maps to §5)*

**Q15.** Confirm the filesystem-permissions inventory for sensitive
files: JCEKS keystore, Root CA private key, JaSypt key + IV,
`db.properties`. Who owns them, what mode? *(maps to §5, §10)*

**Q16.** Confirm the "what CloudStack does not do to its host" inventory
in §5: no child processes besides agent `Script` invocations / system
VM provisioning; signal-handlers via servlet container default;
environment-variable consumption confined to documented set. Anything to
add? *(maps to §5)*

**Q21.** API request size cap and cluster/agent RPC payload size cap —
are these explicitly bounded, or "whatever Jetty / NIO defaults give"?
*(maps to §6, §9)*

**Q22.** `api.throttling.*` and per-account resource limits — proposed:
these are the entire DoS-protection surface, with no engine-level
guard. Confirm. *(maps to §6, §9, §10)*

**Q23.** Decompression behaviour on uploaded QCOW2 / RAW / OVA — proposed:
no engine-side cap; per-account storage limits + hypervisor limits are
the bound. Confirm. *(maps to §6, §9)*

**Q24.** Same-host non-`cloudstack` UID — proposed: game-over, no defence
claimed. Confirm. *(maps to §7, §9)*

**Q25.** Side-channel observers (cache, branch, hypervisor-shared) — out
of scope (proposed). *(maps to §7, §9)*

**Q26.** Byzantine-internal-peer threshold — confirm CloudStack makes no
BFT claim, so any compromised cluster peer or agent with a valid
Root-CA-issued cert is unbounded (proposed). *(maps to §7, §9)*

**Q27.** §8 P9 memory-safety — JVM-bounded; is the reachability
boundary correctly "in-model for the JSON API + B5 input; out-of-model
for native hypervisor SDK bugs that surface as `Throwable`"? *(maps to
§8 P9, §9)*

**Q28.** §8 P10 listing-scope — confirm the §10 invariant "`list*`
responses are scoped to the principal's domain/account/project". And:
is information leak via error messages / async-job status / event log
an in-model concern, or accepted? *(maps to §8 P10, §9, §11)*

**Q29.** Data-at-rest encryption — confirm CloudStack delegates entirely
to storage layer / hypervisor (LUKS, Ceph encryption, vSphere VM
Encryption); no CloudStack-layer encryption of guest volumes. *(maps to
§9)*

**Q30.** Constant-time comparison — confirm that *only* the API
signature path uses `ConstantTimeComparator`. Login password compare,
session cookie compare, console-token compare — none documented
constant-time. Is that intentional? *(maps to §8, §9)*

**Q31.** Time-of-check-to-time-of-use between RBAC check at API entry
and orchestration on agent fleet — confirm mid-job RBAC revocation is
**not** retroactively enforced (proposed). *(maps to §9)*

**Q32.** TLS posture on `:8080` vs `:8443` — confirm production deploys
behind TLS on `:8443` or behind a TLS-terminating reverse proxy; a bare
`:8080` HTTP API is dev-only. *(maps to §5a, §10)*

**Q33.** `security.encryption.key` reuse across environments — confirm
that re-using the JaSypt key + IV across staging and production is a
documented misuse. *(maps to §11)*

### Wave 5 — meta

**Q34.** Should this document live at `docs/threat-model.md` in
`apache/cloudstack`, or as a page on `cloudstack.apache.org/security/`?
Or both, with one canonical and the other linked? *(meta)*

**Q35.** Is there an existing CloudStack threat-model document
(Confluence, internal, or a `[SECURITY]`-tagged dev@ thread) that this
should reconcile against rather than supersede? *(meta — §3.1a of the
rubric)*

**Q36.** What kind of change should trigger a revision (proposed list in
§12 — confirm or correct)? *(meta, §12)*

**Q37.** §11a is the highest-leverage section for the scan agent's
suppression list. The current draft has 15 patterns; could the PMC
populate §11a from recurring "not a vuln" closures on the
`security@apache.org` ↔ CloudStack triage queue and on
`https://cloudstack.apache.org/security.html`? Concrete asks: 3–5
patterns the PMC sees recur in inbound reports (e.g. "SSL bare on
`:8080` in a dev cluster", "agent port open without strictness flipped",
"`md5` authenticator left enabled after upgrade", "console URL appears
in support ticket"). *(meta — §11a)*

**Q38.** Confirm the structural decision to keep the four satellite repos
as separate delta models (`cloudstack-go-threat-model-draft.md`,
`cloudstack-cloudmonkey-threat-model-draft.md`,
`cloudstack-terraform-provider-threat-model-draft.md`,
`cloudstack-kubernetes-provider-threat-model-draft.md`) inheriting §3
/ §4 / §7 from this document. *(meta, §3 item 9)*

---

## Appendix: SECURITY.md → §x back-map

CloudStack does not currently ship an in-repo `SECURITY.md`; the `README.md`
section "Reporting Security Vulnerabilities" points to
`https://cloudstack.apache.org/security.html` as the canonical disclosure
landing page. The de facto security-policy artifacts are scattered:

| Source | Claim | Lands in |
| --- | --- | --- |
| `README.md` "Reporting Security Vulnerabilities" | report to `security@apache.org`; canonical page at `cloudstack.apache.org/security.html` | §1 reporting cross-reference |
| `README.md` "Notice of Cryptographic Software" | JaSypt, Bouncy Castle, JSch, OpenSwan, MySQL native encryption | §5 cryptography assumption, §8 P8 |
| `agent/conf/agent.properties` (`host`, `port`, `ssl.handshake.timeout`, …) | agent ↔ management server transport on `:8250` | §2 component table, §4 B5 |
| `server/src/main/java/com/cloud/api/ApiServer.java` `verifyRequest` (lines ~980–1156) | HMAC-SHA1 signature + `expires` enforcement + constant-time compare | §8 P1, §8 P3, §5a "api.signature.version", §11a "SHA1 / constant-time" entries |
| `server/src/main/java/com/cloud/api/ApiServlet.java` `getClientAddress` (lines 700–725) | forward-header gating by `proxy.forward.list` | §8 P6, §5a "useforwardheader" row |
| `server/src/main/java/com/cloud/api/ApiServlet.java` 2FA path (lines 360–582) | password + 2FA flow | §8 P2 |
| `framework/ca/.../CAService.java`, `plugins/ca/root-ca/.../RootCAProvider.java` | Root CA generated at first boot; agent enrolment via `SetupKeyStoreCommand` | §4 B5, §8 P5, §5a strictness/allow-expired rows |
| `plugins/ca/root-ca/.../RootCACustomTrustManager.java` | `authStrictness` and `allowExpiredCertificate` semantics | §5a, §8 P5 |
| `plugins/acl/{static,dynamic,project}-role-based` | RBAC backends | §8 P4 |
| `plugins/user-authenticators/{md5,sha256salted,pbkdf2,plain-text,ldap,saml2,oauth2}` | pluggable user auth | §2 caller-roles row, §5a "auth.password.algorithm", §10 item 7 |
| `plugins/user-two-factor-authenticators/{static-pin,totp}` | 2FA backends | §5a "enable.2fa.*", §10 item 8 |
| `framework/security/.../KeysManager.java`, `KeystoreManager.java` | `security.encryption.key`, `security.encryption.iv` (Hidden), application-secret JaSypt encryption | §8 P8, §5a, §10 item 6 |
| `agent/src/main/java/com/cloud/agent/Agent.java` `setupAgentKeystore` (lines ~793–916) | agent receives Root CA-signed cert via `SetupKeyStoreCommand` and imports it | §4 B5, §8 P5 |
| `server/src/main/java/com/cloud/servlet/ConsoleProxyServlet.java`, `ConsoleProxyPasswordBasedEncryptor.java` | signed encrypted console-proxy URL token | §4 B3, §8 P7 |
| `https://cloudstack.apache.org/security.html` (website) | canonical disclosure landing page | §1 reporting cross-reference (note: not accessible from the producer's network at draft time; verify content with PMC) |
