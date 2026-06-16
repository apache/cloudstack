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
> The satellite clients' interfaces point **inward** at the management-server
> API; some satellites additionally expose **outward** interfaces that are
> designed to be safe to expose *(maintainer: DaanHoogland)*.
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
- **Draft confidence (provenance-tag tally):** 51 *(documented)* / 42
  *(maintainer)* / 38 *(inferred)*. Eleven formerly-open questions (Q1,
  Q2, Q4, Q5, Q12 — including the highest-leverage Root-CA strictness
  default — plus Q8, Q9, Q10, Q17, Q18, Q19 from the 2026-06-08 review)
  were resolved by the CloudStack PMC review (DaanHoogland, vishesh92) and
  their tags promoted from *(inferred)* to *(maintainer)*.

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
one or more management-server instances — **a single management-server
instance for smaller clouds, or a cluster behind a load balancer for
larger deployments** *(maintainer: DaanHoogland)* — a MariaDB/MySQL
database, one usage server, an optional
SecondaryStorageVM/ConsoleProxyVM/VirtualRouter set of system VMs, and a
per-hypervisor-host `cloudstack-agent` (for KVM/baremetal) or
out-of-process resource bridges (for VMware / XenServer / XCP-ng / Hyper-V).
The operator owns the surrounding L2/L3 network (the **management network**,
the **public network**, the **guest network**, the **storage network**)
and the physical hosts. The threat model is therefore that of a
distributed service (single-instance or clustered), not a library
*(maintainer: DaanHoogland — confirms the distributed control-plane shape;
single-instance is also a supported topology)*.

### Caller roles

| Role | Trust level | Notes |
| --- | --- | --- |
| **End-user API client / Web UI user** | untrusted but authenticated | Identity verified via Apache CloudStack-native (password + HMAC-SHA1 signed request), LDAP, SAML2, OAuth2, or pluggable `APIAuthenticator` *(documented: `plugins/user-authenticators/{ldap,saml2,oauth2,...}`, `server/src/main/java/com/cloud/api/ApiServer.java` `verifyRequest`)*. |
| **Domain / Project admin** | partial trust within their domain | Bounded by RBAC (`plugins/acl/{static,dynamic,project}-role-based`) and the domain hierarchy; can manage users / VMs / networks within a domain. |
| **Root admin** | trusted control plane | Global RBAC role; can change global configuration, upload templates/ISOs, run privileged orchestration. |
| **Operator / cluster admin** | trusted | OS-level access to management-server hosts, the MariaDB database, the keystore, and the agent hosts. Sets `agent.properties`, manages `cloudstack-agent` packages, manages the JCEKS keystore used by the agent for TLS *(documented: `agent/conf/agent.properties`, `framework/security/.../KeystoreManager.java`)*. |
| **Hypervisor agent (cloudstack-agent on KVM/baremetal)** | trusted-once-enrolled peer | Mutually authenticated via X.509 client cert signed by the management server's Root CA *(documented: `framework/ca/`, `plugins/ca/root-ca/`, `agent/src/main/java/com/cloud/agent/Agent.java` `setupAgentKeystore`)*. |
| **System VM (SSVM / CPVM / VR)** | trusted-once-enrolled peer | Same X.509 enrolment shape as the agent; carries the agent binary inside *(maintainer: confirmed — same trust tier as agents, not a separate tier)*. |
| **Hypervisor host (the underlying KVM/VMware/etc.)** | trusted by virtue of operator-controlled provisioning | CloudStack expects to drive the hypervisor via libvirt / VMware vSphere SDK / XenAPI as a privileged user *(documented: `plugins/hypervisors/kvm/`, `plugins/hypervisors/vmware/`, `plugins/hypervisors/xenserver/`)*. |
| **Hypervisor-managed guest VM (end-user workload)** | **untrusted** | A guest VM is an attacker's workload; the model defends against it. |
| **Reverse proxy / load balancer in front of management server** | trusted *(if `proxy.header.verify=true`)* | When the operator enables forward-header processing, only requests whose `Remote_Addr` ∈ `proxy.cidr` have their `proxy.header.names` header honoured *(documented: `server/src/main/java/com/cloud/api/ApiServlet.java` `getClientAddress`; setting names maintainer: vishesh92)*. |
| **Underlying storage (primary / secondary)** | trusted by virtue of operator-granted credentials | CloudStack reads/writes via NFS / RBD / iSCSI / S3 with operator-supplied credentials *(documented: primary/secondary storage plugins under `plugins/storage/`)*. |
| **External integrations (Tungsten, NSX, Netscaler, Palo Alto, …)** | trusted control-plane peers | Operator-configured; CloudStack assumes truthful responses *(inferred — §14 Q3)*. |

### Component-family table

| Family | Representative entry point | Touches outside the process? | In-model? |
| --- | --- | --- | --- |
| Management server JSON API | `client/.../ApiServlet`, HTTPS on `:8080` (admin), `:8080/client/api` (user), HTTPS on `:8443` integration port *(documented: `server/src/main/java/com/cloud/api/ApiServlet.java`, `client/`)* | network (TCP, optionally TLS) | **yes** |
| Management server Web UI | Vue.js SPA under `ui/`, served by the same servlet container *(documented: `ui/`)* | network | **yes** (auth is the API auth) |
| Management server cluster RPC (peer-to-peer) | NIO + TLS between management-server replicas, `:9090` *(documented: `framework/cluster/`, `utils/.../nio/`)* | network | **yes** (peer auth via Root CA) |
| Management server → agent RPC | NIO + TLS on `:8250` (default `agent.properties`) *(documented: `agent/conf/agent.properties` line 47, `utils/.../nio/NioServer.java`)* | network | **yes** (mutually authenticated via Root CA) |
| `cloudstack-agent` (KVM/baremetal) | reverse-connects to management server, runs commands via libvirt / hypervisor SDK *(documented: `agent/`, `plugins/hypervisors/kvm/`)* | network + hypervisor + OS | **yes** |
| System VMs — SecondaryStorageVM, ConsoleProxyVM, Virtual Router | shipped images under `systemvm/`; agent binaries inside them *(documented: `systemvm/`)* | network (storage / public / guest) | **yes** |
| Console proxy data path | browser ↔ ConsoleProxyVM ↔ hypervisor VNC/SPICE socket; signed token issued by management server *(documented: `server/src/main/java/com/cloud/servlet/ConsoleProxyServlet.java`, `server/src/main/java/com/cloud/servlet/ConsoleProxyPasswordBasedEncryptor.java`)* | network | **yes** |
| Secondary-storage HTTP (templates, ISO downloads, snapshot copies) | download links are UUID-named symlinks served by an Apache httpd, with **no auth on the link**; the UUID format prevents enumeration and the symlinks are removed after a period *(maintainer: vishesh92, DaanHoogland)* | network | **yes** |
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
| `systemvm/agent/noVNC` (a vendored fork of `github.com/novnc/novnc` with CloudStack-specific changes on top *(maintainer: vishesh92)*), `…/vendor/pako`, other vendored JS / shell scripts | vendored upstream | n/a | in-model only at the wrapper boundary; upstream bugs go upstream. No automated vendored-dependency update procedure exists today (dependabot does not produce viable PRs); the PMC would prefer to have one *(maintainer: DaanHoogland)* |

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
   authorized to do is not a vulnerability *(maintainer: vishesh92 — §14 Q8)*. →
   `OUT-OF-MODEL: equivalent-harm`.
5. **A defender against a guest VM doing things the hypervisor allows it
   to do.** A guest VM consuming CPU, memory, or disk up to its allocated
   limit, sending arbitrary IP traffic within its assigned VLAN / VXLAN /
   security group, or exploiting another VM via the hypervisor's own
   shared surfaces (sidechannel, RowHammer, GPU passthrough leak) is out
   of model. CloudStack is responsible only for the orchestration that
   *places* the guest, not for hypervisor-level isolation *(maintainer:
   vishesh92 — §14 Q9; the in-model case is CloudStack applying
   wrong/insecure hypervisor settings — Daan to confirm boundary)*. → `OUT-OF-MODEL: adversary-not-in-scope` for the
   side-channel case, `BY-DESIGN: property-disclaimed` for the
   resource-limit case.
6. **A sandbox for templates, ISO images, or user-data scripts.** A
   user-uploaded template (via `registerTemplate`) is run by the
   hypervisor with the privileges the offering grants. cloud-init /
   user-data / metadata is passed through to the guest; CloudStack does
   not parse or sanitize its semantics *(documented: kubernetes-service
   plugin `userdata` references; maintainer: vishesh92 — §14 Q10, end-user guest customization)*. →
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
   Software notice)*. `systemvm/agent/noVNC` is specifically a **vendored
   fork of `github.com/novnc/novnc`** carrying CloudStack-specific changes
   *(maintainer: vishesh92)*. Where CloudStack vendors source, the vendored
   code is modeled at the wrapper boundary; vulnerabilities intrinsic to the
   upstream project should be reported upstream. There is currently **no
   automated procedure** to pull upstream fixes into the vendored copies
   (dependabot has not produced viable PRs); the PMC would prefer to
   establish one *(maintainer: DaanHoogland)*.
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
| B5 | Management server → `cloudstack-agent` (KVM/baremetal) | NIO + TLS on `:8250`; agent uses X.509 client cert issued by Root CA on first connect; cert provisioning is the `SetupKeyStoreCommand` shape *(documented: `agent/src/main/java/com/cloud/agent/Agent.java` `setupAgentKeystore`, `framework/ca/.../CAService.java`, `plugins/ca/root-ca/.../RootCAProvider.java`)*; trust strictness governed by `ca.plugin.root.auth.strictness` (**default `true` for new setups; `false` only on upgrade from pre-Aug-2017 versions** — see §5a) and `ca.plugin.root.allow.expired.cert` | peer-trust by valid cert |
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
  enrolled across peers; a *cleartext*/un-certed cluster RPC finding is
  gated by `ca.plugin.root.auth.strictness`, which defaults to `true` on
  new setups (see §5a).
- **Management ↔ agent (B5)**: reachable from a peer that presents a
  Root-CA-signed certificate the management server accepts. By default on
  new setups `ca.plugin.root.auth.strictness = true`, so the management
  server **does require** a client certificate from the connecting agent
  *(maintainer: vishesh92 —
  `https://github.com/apache/cloudstack/pull/2239`)*. The value remains
  `false` only when upgrading from versions released before Aug 2017 that
  predate the setting; that upgrade case is documented in the upgrade
  instructions and is therefore not a concern *(maintainer: DaanHoogland)*
  *(documented: `plugins/ca/root-ca/.../RootCAProvider.java`,
  `RootCACustomTrustManager.java`)*.
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
- **Operating system (agent)**: same family on KVM/baremetal hosts;
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
| `ca.plugin.root.auth.strictness` | **`true` for new setups; `false` only on upgrade from pre-Aug-2017 versions** *(maintainer: vishesh92 — `https://github.com/apache/cloudstack/pull/2239`)* | New setups are strict by default; the `false`-on-upgrade case is called out in the upgrade instructions and is therefore not a concern *(maintainer: DaanHoogland)* | When `false`, the management server's `RootCACustomTrustManager` does **not** require a client certificate from a peer attempting to connect on `:8250` (agent port) or cluster ports. A peer without a cert is allowed in. |
| `ca.plugin.root.allow.expired.cert` | **`true`** *(documented: `RootCAProvider.java`)* | operational default to survive cert-rotation lag *(maintainer: paired with the strictness ruling above)* | When `true`, an expired client cert is accepted during SSL handshake. |
| `ca.plugin.root.issuer.dn` | `CN=ca.cloudstack.apache.org` *(documented: same file)* | configured at first management-server boot | Subject DN of the auto-generated self-signed Root CA. |
| `proxy.header.verify` | `false` by default *(maintainer: vishesh92 — §14 Q17)* | When on, the operator must restrict `proxy.cidr` to the trusted reverse-proxy CIDR | When set, `ApiServlet.getClientAddress` honours proxy-set forward headers *only* for source IPs in `proxy.cidr` *(documented: `server/src/main/java/com/cloud/api/ApiServlet.java` `getClientAddress`; setting name maintainer: vishesh92)*. |
| `proxy.header.names` | list of header names; semantics: names to check for allowed IP addresses from a proxy-set header *(maintainer: vishesh92)* | list of header names to consult for the allowed client address when set by a proxy | Names the request header(s) carrying the proxy-set client IP. |
| `proxy.cidr` | unset *(maintainer: vishesh92 — §14 Q17; headers honoured only when `Remote_Addr` ∈ this list)* | required when `proxy.header.verify` is on | List of CIDRs for which `proxy.header.names` headers are honoured when the connecting `Remote_Addr` is in this list *(semantics maintainer: vishesh92)*. |
| `enable.user.2fa` / `mandate.user.2fa` | both default `false`; domain-configurable *(maintainer: vishesh92 — §14 Q18)* | `enable.user.2fa` turns 2FA on; `mandate.user.2fa` makes it mandatory (only when `enable.user.2fa` is true) — a deployment choice, not a §10 violation when off | When on, users must complete static-pin or TOTP 2FA after login. |
| `security.encryption.key`, `security.encryption.iv` | auto-generated at first boot *(documented: `framework/security/.../KeysManager.java`)* | trusted secret | Base64-encoded JaSypt master key + IV used to encrypt application-level secrets in the DB. |
| `user.password.encoders.order` | `PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT` *(maintainer: vishesh92)* | first encoder in the order is used to hash new passwords; the list also defines the verification fall-through order | Governs how user passwords are stored and which encoders are accepted on verify. |
| `user.password.encoders.exclude` | `MD5,LDAP,PLAINTEXT` *(maintainer: vishesh92)* | excluded encoders are not used to (re)hash passwords | Excludes weak/legacy encoders from being chosen, even though they remain in the order list for verifying already-stored hashes. |
| `enforce.post.requests.and.timestamps` | per `isPostRequestsAndTimestampsEnforced` *(documented: `ApiServer.java`; setting name maintainer: vishesh92)* | bounds `expires` to a maximum future offset | Prevents an attacker who steals a signed URL with a 10-year expiration from using it forever. |
| `integration.api.port` (`:8096`) | typically disabled *(inferred — §14 Q20)* | When non-zero, exposes an *unauthenticated* admin API for integration testing | An open integration port is a complete RBAC bypass on the management server. |
| Hypervisor enablement (which `plugins/hypervisors/*` are installed and configured) | per zone | operator-driven | An unused hypervisor plugin still ships but is not connected to any host. |
| Hostname / SAN of management-server cert (`ca.framework.cert.management.custom.san`) | unset *(maintainer: vishesh92)* | when set, included in the auto-generated cert SAN | governs which hostnames clients can use to reach the management server. |
| SAML2 / OAuth2 enablement (`plugins/user-authenticators/{saml2,oauth2}`) | off *(inferred — §14 Q19)* | turning on adds an external IdP trust dependency | adds B6 transitions. |
| LDAP enablement (`plugins/user-authenticators/ldap`) | off *(inferred — §14 Q19)* | turning on adds an external LDAP trust dependency | adds B6 transitions. |

**The Root-CA strictness default (resolved).** Earlier drafts treated
`ca.plugin.root.auth.strictness = false` as the shipped default and the
single highest-leverage open question. The PMC has clarified that **new
setups default to `true`** — the management server *does* require a
Root-CA-signed client cert on `:8250` and the cluster ports — and the
value is `false` **only** when upgrading from versions released before
Aug 2017 that predate the setting *(maintainer: vishesh92 —
`https://github.com/apache/cloudstack/pull/2239`)*. That upgrade case is
documented in the upgrade instructions, so a leftover `false` after such
an upgrade is an operator-hardening/upgrade-hygiene item, not a shipped
insecure default *(maintainer: DaanHoogland)*. A report against an open
`:8250` accepting an un-certed peer on a **new** install is therefore
`MODEL-GAP`/`VALID` (strictness should be on), whereas the same on an
**upgraded** pre-2017 install is `OUT-OF-MODEL: non-default-build`
(documented upgrade step not applied). `ca.plugin.root.allow.expired.cert`
remains `true` as an operational concession to cert-rotation lag.

## §6 Assumptions about inputs

### Per-endpoint trust table (network surfaces)

| Surface / route | Parameter | Attacker-controllable? | Caller must enforce |
| --- | --- | --- | --- |
| Management server `:8080`/`:8443` JSON API | command name + params | **yes** | nothing — CloudStack parses, authenticates (B1), applies RBAC, dispatches |
| Management server `:8080`/`:8443` JSON API | `signature` parameter | **yes** | HMAC-SHA1 verified *constant-time* against expected signature *(documented: `ApiServer.java` line 1137 `ConstantTimeComparator.compareStrings`)* |
| Management server `:8080`/`:8443` JSON API | `expires` parameter (sig v3) | **yes** | rejected if past, or beyond the `enforce.post.requests.and.timestamps` ceiling *(documented: same file; setting name maintainer: vishesh92)* |
| Management server `:8080`/`:8443` JSON API | proxy-set forward headers (`proxy.header.names`) | **yes** if `proxy.header.verify=true` | honoured **only** if the connecting `Remote_Addr` ∈ `proxy.cidr` *(documented: `ApiServlet.java` `getClientAddress`; setting names maintainer: vishesh92)* |
| Management server `:8080`/`:8443` Web UI | session cookie | **yes** | session-fixation / invalidation handled via `invalidateHttpSession` on auth failure *(documented: `ApiServlet.java` line 418)* |
| Integration API `:8096` (if enabled) | command name + params | **yes** | **no signature check** — integration port is unauthenticated by design |
| Management ↔ agent `:8250` | NIO Thrift-like payload | **only by a peer that has cleared B5 trust** | client cert via `RootCACustomTrustManager` |
| Management ↔ cluster peer | NIO payload | **only by a peer that has cleared B4 trust** | client cert via `RootCACustomTrustManager` |
| Console proxy URL | encrypted token (containing VM identity + endpoint + duration) | **yes** | token MUST decrypt + verify with `ConsoleProxyPasswordBasedEncryptor` keys *(documented: `ConsoleProxyPasswordBasedEncryptor.java`)* |
| Secondary-storage HTTP download URL | UUID-named symlink path | **yes** | **no auth on the download link**; the UUID format is the anti-enumeration control and the symlink is removed after a period — timed availability of the download token is the mitigation *(maintainer: vishesh92, DaanHoogland)* |
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
| Unauthenticated peer reaching `:8250` (agent port) | **only if** `ca.plugin.root.auth.strictness = false`, which on new setups it is **not** (default `true`); `false` arises only on un-remediated pre-Aug-2017 upgrades (§5a) | TCP to the listening port; may attempt to connect as a peer without presenting a cert |
| Unauthenticated peer reaching `:8096` (integration port) | **yes** *if* the port is open (typically not in production) | full unauthenticated admin API |
| Authenticated end user with limited RBAC role | **yes** | call APIs their role permits; manage VMs/networks/storage in their domain/account/project |
| Authenticated end user with broad RBAC role | partial | only RBAC-envelope escapes are in scope |
| Authenticated domain admin | **yes** | full management within their domain; cross-domain leakage is in scope |
| Authenticated root admin | **out of scope** — see §3 item 4 | unbounded by design |
| Co-tenant (different account in same domain or different domain on same CloudStack) | **yes** | cross-tenant leakage (VM ID guessing, network bleed, storage bleed, template visibility) is in scope |
| Guest VM workload | **partial** | hypervisor-mediated; out-of-scope for hypervisor isolation bugs (§3 item 5), in-scope for the orchestration that placed the VM (security-group rule application, VLAN tagging, public IP routing) |
| Browser holding a valid console-proxy URL | **yes** | the URL is a bearer credential; scope of harm is one VM's console for the URL's lifetime |
| Operator | **out of scope** | see §3 item 1 |
| Hostile hypervisor | **out of scope** | see §3 item 3 |
| Hostile LDAP / SAML / OAuth IdP, hostile NSX/Netscaler/Tungsten, hostile S3 endpoint | **out of scope** | see §3 item 2 |
| Reverse proxy that should be trusted but is not in `proxy.cidr` | **out of scope** | its forward headers are not honoured |
| Local process on the management-server host running as a different UID | **partial** *(inferred — §14 Q24)* | same-host attackers with non-cloudstack UID can reach `:8080` unless host firewalling forbids; CloudStack does not defend against same-host `root` |
| Side-channel observer (cache timing, network timing, hypervisor side channels) | **out of scope** *(inferred — §14 Q25)* | n/a |
| Quantum adversary | **out of scope** | n/a |

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

- **Condition**: `ca.plugin.root.auth.strictness = true` — **the default
  on new setups** *(maintainer: vishesh92 —
  `https://github.com/apache/cloudstack/pull/2239`)*. Pre-Aug-2017
  upgrades may leave it `false` until the documented upgrade step is
  applied *(maintainer: DaanHoogland)*. `ca.plugin.root.allow.expired.cert`
  remains `true` (cert-rotation concession), so the property covers
  *peer-cert presence and Root-CA chain*, not cert freshness.
- **Violation symptom**: a peer without a Root-CA-issued cert successfully
  completes a session on `:8250` or the cluster port on a setup where
  strictness is on.
- **Severity**: **security-critical**, `VALID` per §13.
- *(documented; default resolved by maintainer.)*

### P6 — Reverse-proxy IP-trust gating for forward headers

- **Condition**: `proxy.header.verify` on (default `false`) *(maintainer:
  vishesh92 — §14 Q17)*;
  only requests whose `Remote_Addr` falls in `proxy.cidr` have their
  `proxy.header.names` forward header(s) consulted *(documented:
  `ApiServlet.java` `getClientAddress` `NetUtils.isIpInCidrList`; setting
  names maintainer: vishesh92)*.
- **Violation symptom**: a request from a source IP **outside**
  `proxy.cidr` succeeds with an attacker-supplied forward header
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
  SDKs (libvirt / vSphere / XenAPI). CloudStack presumes **no limitation
  on implementation language** — ocaml, python and bash run on hypervisors
  and go is used on the management server (the set may grow); the
  memory-safety claims here hold for the **JVM components**, to which the
  JVM-conformance condition applies *(maintainer: DaanHoogland — §14 Q27)*.
- **Violation symptom**: heap corruption, OOM-via-input-size attack on a
  surface where the input source is `:8080` / `:8443` / B5; JVM-side
  crashes from a request a normally-RBAC'd user could send.
- **Severity**: **security-critical** when reachable from network input;
  **`VALID-HARDENING`** when reachable only by a writer who already
  controls the bytes (§3 item 5).
- *(maintainer: DaanHoogland — §14 Q27)*

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
  Note the value is `true` on new setups *(maintainer: vishesh92)*; a
  scanner that flags "client cert not requested" is only correct on an
  un-remediated pre-Aug-2017 upgrade, and even then it identifies a
  documented upgrade step, not a transport-encryption bug.
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

1. Keep `ca.plugin.root.auth.strictness = true` (the default on new
   setups). When **upgrading from a pre-Aug-2017 version**, follow the
   documented upgrade step to turn strictness on — otherwise agent and
   cluster-peer ports accept peers without a cert *(maintainer: vishesh92,
   DaanHoogland — `https://github.com/apache/cloudstack/pull/2239`)*.
   Consider tightening `ca.plugin.root.allow.expired.cert` (default `true`)
   once cert rotation is reliable.
2. Restrict the management network at L2/L3 so that `:8250` (agent),
   `:9090` (cluster), and the MariaDB port are reachable only from the
   intended peers *(inferred — §14 Q13)*.
3. Restrict the integration API port `:8096` — either disable it entirely
   or limit it to a localhost/management subnet *(inferred — §14 Q20)*.
4. Terminate TLS for the JSON API and Web UI on `:8443` (not `:8080`); if
   `:8080` is exposed at all, only behind a TLS-terminating reverse
   proxy *(inferred — §14 Q32)*.
5. When using a reverse proxy, set `proxy.header.verify = true`,
   `proxy.header.names` to the forward header(s) the proxy sets, *and*
   `proxy.cidr` to the proxy's CIDR — leaving `proxy.cidr` unset/empty
   means the header is ignored (safe-default per P6), but a misconfigured
   wide CIDR is a trust-bypass *(setting names maintainer: vishesh92)*.
6. Protect the `security.encryption.key` / `security.encryption.iv`
   files, the JaSypt-encrypted DB, the Root CA private key, and the
   `cloudstack-management` Unix user's home directory at OS level.
7. Keep the password-encoder configuration at safe defaults:
   `user.password.encoders.order` defaults to
   `PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT` (so PBKDF2 is used to hash
   new passwords) and `user.password.encoders.exclude` defaults to
   `MD5,LDAP,PLAINTEXT` (so the weak encoders are not chosen for hashing,
   only retained for verifying already-stored hashes) *(maintainer:
   vishesh92)*. Do not remove `MD5`/`PLAINTEXT` from the exclude list in
   production — the supported greenfield encoder set is
   `PBKDF2,SHA256SALT,SAML2` *(maintainer: vishesh92 — §14 Q19)*.
8. Enable 2FA (`totp` or `static-pin`) for administrators and ideally for
   all users — 2FA on/off is a deployment choice via `enable.user.2fa`
   and `mandate.user.2fa` (both default `false`) *(maintainer: vishesh92 —
   §14 Q18)*.
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

- **Leaving `:8250` open to the world with `ca.plugin.root.auth.strictness=false`
  on an upgraded pre-Aug-2017 cluster.** New setups default to `true`;
  the `false` value only survives an upgrade where the documented step
  was skipped *(maintainer: vishesh92, DaanHoogland)*. In that state any
  peer can connect as an agent — an upgrade-hygiene gap, dispositioned
  `OUT-OF-MODEL: non-default-build` (documented upgrade step not applied).
- **Exposing `:8096` (integration API) publicly.** Anyone reaching the
  port executes admin API commands without auth.
- **Exposing `:8080` (HTTP JSON API) publicly without a TLS-terminating
  reverse proxy.** Signed-request integrity holds, but the API secret-
  key-derived signature is visible to any wire observer; replay within
  the `expires` window is trivial.
- **Setting `proxy.header.verify=true` with `proxy.cidr` wider than
  the actual reverse-proxy CIDR.** An attacker outside the proxy can
  spoof a `proxy.header.names` header and claim any IP address for audit
  logs and authentication-IP checks *(setting names maintainer: vishesh92)*.
- **Removing `MD5`/`PLAINTEXT` from `user.password.encoders.exclude` (or
  reordering them to the front of `user.password.encoders.order`) in
  production.** The encoders ship for verifying legacy hashes; promoting
  them to hash new passwords stores weakly-protected credentials
  *(maintainer: vishesh92 — §14 Q19; the supported greenfield encoder set is `PBKDF2,SHA256SALT,SAML2`)*.
- **Granting domain admin to too many users.** A domain admin can manage
  all accounts within the domain — including reading guest console URLs.
- **Embedding console-proxy URLs in screenshots, ticketing systems, or
  chat.** Tokens are bearer credentials.
- **Reusing `security.encryption.key` across environments of different
  trust levels.** A staging-env leak becomes a production-env decrypt
  primitive *(inferred — §14 Q33)*.
- **Leaving `ca.plugin.root.auth.strictness=false` after a pre-Aug-2017
  upgrade in a multi-management-server deployment.** A peer can join the
  cluster without a cert until the documented upgrade step flips it to the
  new-setup default of `true` *(maintainer: vishesh92, DaanHoogland)*.
- **Uploading large or pathological templates and relying on hypervisor
  to enforce size.** Per-account resource limits, not the engine, are the
  enforcement.

## §11a Known non-findings (recurring false positives)

This section is the highest-leverage input for automated agentic security
scans. Each entry: tool symptom, why it is safe under the model, the §
that licenses the call.

- **"Management ↔ agent port `:8250` accepts no client cert" reported
  against a setup with `ca.plugin.root.auth.strictness=false`.** New setups
  default to `true` and **do** require a Root-CA-signed client cert
  *(maintainer: vishesh92 — `https://github.com/apache/cloudstack/pull/2239`)*.
  The value is `false` only on an upgrade from a pre-Aug-2017 version that
  predates the setting, and the upgrade instructions document turning it on
  *(maintainer: DaanHoogland)*. → On a new install: `KNOWN-NON-FINDING`
  (strictness is on). On an upgraded install with the step skipped:
  `OUT-OF-MODEL: non-default-build` (documented upgrade step not applied).
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
  Documented default — an operational concession to cert-rotation lag, paired
  with the strictness default *(maintainer: vishesh92, DaanHoogland)*. →
  `VALID-HARDENING` at most; tightening it is an operator choice per §10.
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
  upstream; `systemvm/agent/noVNC` is a vendored fork of
  `github.com/novnc/novnc` with CloudStack changes, and there is no
  automated sync procedure today *(maintainer: vishesh92, DaanHoogland)*. →
  `OUT-OF-MODEL: unsupported-component` (upstream pointer); a
  CloudStack-introduced change *to* the fork is in-model.
- **"Secondary-storage download URL has no authentication / can be replayed."**
  By design: download links are UUID-named symlinks served by an Apache
  httpd with no auth on the link; the UUID format defeats enumeration and
  the symlink is removed after a period, so timed availability is the
  mitigation *(maintainer: vishesh92, DaanHoogland)*. → `BY-DESIGN:
  property-disclaimed` for the no-auth aspect; a link that is *not* removed
  after its window, or a guessable (non-UUID) name, is `VALID-HARDENING`.
- **"A proxy-set forward header is honoured without authentication."**
  Honoured only if (a) `proxy.header.verify=true`, (b) the header is one of
  `proxy.header.names`, *and* (c) the connecting `Remote_Addr` ∈
  `proxy.cidr` *(setting names maintainer: vishesh92)*. → `KNOWN-NON-FINDING`.
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
- A change in the extension mechanisms implemented by CloudStack
  *(maintainer: DaanHoogland — §14 Q36)*.
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
| `OUT-OF-MODEL: non-default-build` | Only manifests under a §5a flag that is not the new-setup default (e.g. `ca.plugin.root.auth.strictness=false` surviving an un-remediated pre-Aug-2017 upgrade, integration port `:8096` open). | §5a |
| `OUT-OF-MODEL: equivalent-harm` | An actor already-authorized under the model can cause the same harm via a documented path (root admin doing root-admin things, RBAC-licensed user using their RBAC-licensed commands). | §3 items 4, 5 |
| `BY-DESIGN: property-disclaimed` | Concerns a §9 property the project explicitly does not provide (template sandboxing, side-channel resistance, hypervisor isolation, etc.). | §9 |
| `KNOWN-NON-FINDING` | Matches a §11a recurring false positive. | §11a |
| `MODEL-GAP` | Cannot be cleanly routed to any of the above — triggers §12 model revision. | §12 |

## §14 Open questions for the maintainers

Every *(inferred)* tag in the body maps to one of these. Proposed answers
are inline; please confirm, correct, or strike.

### Wave 1 — scope, intended use, the two big insecure defaults

**Q1.** ~~The model assumes CloudStack is "a clustered distributed
control plane deployed inside an operator-controlled datacenter
network", not a single-host appliance or a hosted SaaS. Confirm?~~
**RESOLVED** *(maintainer: DaanHoogland)* — distributed control plane;
**both** a single management-server instance (smaller clouds) and a
clustered deployment are supported topologies. Folded into §2.

**Q2.** ~~Are the SecondaryStorageVM, ConsoleProxyVM, and Virtual Router
treated as trusted-once-enrolled peers, or do they get their own trust
tier?~~ **RESOLVED** *(maintainer)* — **yes**, same trust tier as agents,
not a separate tier. Folded into §2 caller-roles.

**Q3.** ~~Are external integrations (LDAP, SAML2 IdP, OAuth2 IdP, NSX
controller, Netscaler, Tungsten, S3-compatible storage, backup
providers) modeled as trusted control-plane peers?~~ **RESOLVED**
*(maintainer: DaanHoogland — yes)* — trusted control-plane peers; this
licenses §3 item 2 and §11a trusted-input dispositions. *(maps to §2, §3, §11a)*

**Q4.** ~~SecondaryStorageVM HTTP download surface — is the URL token
per-template ACL-checked, or is the SSVM URL itself a bearer credential?~~
**RESOLVED** *(maintainer: vishesh92, DaanHoogland)* — download links are
UUID-named symlinks served by an Apache httpd with **no auth on the link**;
the UUID format defeats enumeration and the symlink is removed after a
period (timed availability is the mitigation). The PMC noted this should
be re-tested/confirmed in code. Folded into §6, §11a. *(Daan also asked
why static code analysis did not surface this — a note for the scan
agent, not a model gap.)*

**Q5.** ~~Vendored upstream code under `systemvm/agent/noVNC` and bundled
JaSypt / Bouncy Castle / JSch — is the policy "report upstream; we pick up
fixes on next sync"?~~ **RESOLVED** *(maintainer: vishesh92, DaanHoogland)*
— `systemvm/agent/noVNC` is a **vendored fork of `github.com/novnc/novnc`**
with CloudStack changes; vendored bugs go upstream. There is **no automated
update procedure today** (dependabot has not produced viable PRs); the PMC
would prefer to establish one. Folded into §3 item 8, §11a.

**Q6.** ~~Is "an operator with `root` on a management-server host, the
JCEKS keystore + encryption keys, the Root CA private key, or MariaDB
credentials" out of scope?~~ **RESOLVED** *(maintainer: DaanHoogland — yes)*
— `OUT-OF-MODEL: adversary-not-in-scope`. *(maps to §3 item 1, §9)*

**Q7.** ~~Hypervisor bugs (libvirt / vSphere SDK / XenAPI / Hyper-V API /
KVM/QEMU itself) — out of scope, report upstream?~~ **RESOLVED**
*(maintainer: DaanHoogland — yes, out of scope; report upstream)*. *(maps to §3 item 3)*

### Wave 2 — the two big insecure defaults

**Q12.** ~~**Highest-leverage question in the model.** Are
`ca.plugin.root.auth.strictness` and `ca.plugin.root.allow.expired.cert`
shipped insecure-by-default?~~ **RESOLVED** *(maintainer: vishesh92,
DaanHoogland — `https://github.com/apache/cloudstack/pull/2239`)*:

- `ca.plugin.root.auth.strictness` defaults to **`true` on new setups** —
  the management server **does** require a Root-CA-signed client cert on
  `:8250` and the cluster ports. It is `false` **only** after upgrading
  from a version released before Aug 2017 that predates the setting; the
  upgrade instructions document turning it on, so a leftover `false` is an
  upgrade-hygiene gap, not a shipped insecure default.
- `ca.plugin.root.allow.expired.cert` defaults to `true` as an operational
  concession to cert-rotation lag.

This resolution reshaped §3 item 1, §5a, §7 (the un-certed peer row),
§8 P5, §9 false-friends, §10, §11, §11a, and §13. The earlier
"assumes operator must flip per §10" framing is withdrawn.

### Wave 3 — adjacent insecure defaults and admin-only surfaces

**Q8.** ~~Is "a root admin with full RBAC role causes harm Y via a
documented path Z" out of scope (proposed: **yes**, `OUT-OF-MODEL:
equivalent-harm`)? In particular: `runCustomAction`, template upload,
plugin registration, global config change, system-VM patching, system-VM
console access.~~ **RESOLVED** *(maintainer: vishesh92)* — yes; a root
admin generally has direct access to most of these resources anyway →
`OUT-OF-MODEL: equivalent-harm`. *(maps to §3 item 4, §9)*

**Q9.** ~~Guest VM workloads — confirm that hypervisor-mediated side
channels and resource-exhaustion-within-allocation are out of scope, and
that the in-scope orchestration concerns are limited to "did CloudStack
place the VM in the right VLAN / apply the right security group / route
the right IP" (proposed)?~~ **RESOLVED** *(maintainer: vishesh92)* — yes;
side channels + resource-exhaustion-within-allocation are out of scope.
The one in-model case: CloudStack applying a wrong/insecure setting while
launching or managing the guest (CloudStack must use correct/secure
hypervisor settings). *(DaanHoogland to confirm the boundary.)* *(maps to
§3 item 5, §7, §9)*

**Q10.** ~~Templates / ISOs / user-data — confirm that there is no
sandboxing of user-supplied OS images, and that user-data is intentionally
a code-execution channel into the guest (proposed)?~~ **RESOLVED**
*(maintainer: vishesh92)* — yes; userdata is the end user customizing
their own guest OS (tenant-controlled data inside their own boundary), not
a CloudStack-side injection surface. *(maps to §3 item 6, §9)*

**Q11.** Confirm the unsupported-component list: `tools/marvin/`,
`test/`, `developer/`, `quickcloud/`, `cloud-cli/`,
`tools/{devcloud4,devcloud-kvm,appliance,checkstyle,transifex,bugs-wiki,...}`,
`simulator` hypervisor plugin. Anything to add or remove? **RESOLVED** *(maintainer: DaanHoogland)* — exclude `simulator` and `tools/appliance` explicitly (out of scope for now; a future security-purpose tooling effort may revisit). *(maps to §3 item 7)*

**Q17.** Forward-header gating — the **setting names are confirmed**
*(maintainer: vishesh92)*: `proxy.header.verify` (the on/off gate),
`proxy.header.names` (header names to consult), and `proxy.cidr` (CIDRs of
the `Remote_Addr` values for which those headers are honoured). **RESOLVED** *(maintainer: vishesh92)* — `proxy.header.verify` is
**`false` by default**; only when the connecting `Remote_Addr` ∈
`proxy.cidr` does CloudStack read the client IP from `proxy.header.names`.
*(maps to §5a, §6, §10)*

**Q18.** ~~2FA — proposed: off by default, operator turns it on per
domain / per user via `enable.2fa.*`. Confirm; and is "2FA disabled in
production" a §10 violation or a deployment choice?~~ **RESOLVED**
*(maintainer: vishesh92)* — deployment choice, not a §10 violation. Two
domain-configurable global settings: `enable.user.2fa` (default `false`;
whether 2FA is enabled) and `mandate.user.2fa` (default `false`; whether
2FA is mandatory — applies only when `enable.user.2fa` is true). *(maps to
§5a, §10)*

**Q19.** User-authenticator plugins — encoder selection is governed by
`user.password.encoders.order` (default
`PBKDF2,SHA256SALT,MD5,LDAP,SAML2,PLAINTEXT`) and
`user.password.encoders.exclude` (default `MD5,LDAP,PLAINTEXT`), so PBKDF2
is the effective hashing default and `MD5`/`PLAINTEXT` are retained only
for verifying legacy hashes *(maintainer: vishesh92)*. **RESOLVED** *(maintainer: vishesh92)* — a
report against `md5`/`plain-text` hashing *new* passwords in a greenfield
install is `OUT-OF-MODEL: non-default-build`: the default
`user.password.encoders.exclude` (`MD5,LDAP,PLAINTEXT`) removes them from
the effective set, so the supported greenfield encoders are
`PBKDF2,SHA256SALT,SAML2`. *(maps to §5a, §10, §11)*

**Q20.** Integration API port `:8096` — proposed: closed (port-zero) by
default in production packaging, open only when explicitly configured;
when open, it is unauthenticated by design. A report of "integration
port allows admin commands without auth" is `OUT-OF-MODEL:
non-default-build` *if* the operator opened it, else `VALID`. Confirm the default. **RESOLVED** *(maintainer: DaanHoogland)* — default is `0` (disabled); `8096` is set only in test configurations. *(maps to §5a, §10, §11a)*

### Wave 4 — environment, distributed model, false-friends

**Q13.** Network-fabric assumptions — proposed: at least four logical
networks (management, public, guest, storage), with the management
network as the trusted control plane. Is that the canonical model, or
do you support more compressed topologies (single-fabric) in production? **RESOLVED** *(maintainer: DaanHoogland)* — there are four logical networks (management, public, guest, storage); each may have multiple instances across topologies (e.g. multiple zones) and may be combined within physical networks, but all four logical types must be present for a functional system. *(maps to §5, §10)*

**Q14.** Clock-skew assumption for signature v3 `expires` enforcement —
proposed: operator's responsibility to keep client + management-server clocks roughly in sync. Confirm. **RESOLVED** *(maintainer: DaanHoogland)* — confirmed; operator responsibility (PMC to add to the security model page). *(maps to §5)*

**Q15.** Confirm the filesystem-permissions inventory for sensitive
files: JCEKS keystore, Root CA private key, JaSypt key + IV,
`db.properties`. Who owns them, what mode? **CLARIFIED** *(producer)* — not a CSV inventory of every file in a running system; only the four sensitive artifacts named here (JCEKS keystore, Root CA private key, JaSypt key + IV, `db.properties`), each with its owning UID and file mode. *(awaiting the per-file owner/mode values from the PMC.)* *(maps to §5, §10)*

**Q16.** Confirm the "what CloudStack does not do to its host" inventory
in §5: no child processes besides agent `Script` invocations / system
VM provisioning; signal-handlers via servlet container default;
environment-variable consumption confined to documented set. Anything to add? **RESOLVED** *(maintainer: DaanHoogland)* — confirmed; nothing to add. *(maps to §5)*

**Q21.** API request size cap and cluster/agent RPC payload size cap —
are these explicitly bounded, or "whatever Jetty / NIO defaults give"? **RESOLVED** *(maintainer: DaanHoogland)* — the UI server sets an explicit cap, `org.apache.cloudstack.ServerDaemon.DEFAULT_REQUEST_CONTENT_SIZE = 1048576` (1 MiB); for other components the sizes are capped by the upstream components used. *(maps to §6, §9)*

**Q22.** `api.throttling.*` and per-account resource limits — proposed:
these are the entire DoS-protection surface, with no engine-level guard. Confirm. **RESOLVED** *(maintainer: DaanHoogland)* — confirmed; enforced at the API access check, and `api.throttling.enabled` is **`false` by default**. *(maps to §6, §9, §10)*

**Q23.** Decompression behaviour on uploaded QCOW2 / RAW / OVA — proposed:
no engine-side cap; per-account storage limits + hypervisor limits are the bound. Confirm. **RESOLVED** *(maintainer: DaanHoogland)* — correct. *(maps to §6, §9)*

**Q24.** Same-host non-`cloudstack` UID — proposed: game-over, no defence
claimed. Confirm. *(maintainer: DaanHoogland notes there is a refusal to add a host with the same IP; whether that also includes a UID check is open — @vishesh92 to confirm. Disposition unchanged pending that.)* *(maps to §7, §9)*

**Q25.** Side-channel observers (CPU cache timing, branch-predictor / speculative-execution channels e.g. Spectre-class, hypervisor-shared microarchitectural channels) — out of scope (proposed). **RESOLVED** *(maintainer: DaanHoogland)* — agreed, out of scope. *("branch" = branch-predictor / speculative-execution side channels — clarified by producer.)* *(maps to §7, §9)*

**Q26.** Byzantine-internal-peer threshold — confirm CloudStack makes no
BFT claim, so any compromised cluster peer or agent with a valid
Root-CA-issued cert is unbounded (proposed). **RESOLVED** *(maintainer: DaanHoogland)* — agreed; no BFT claim. (A quorum-style mitigation would only be meaningful in larger clusters, not single/dual-node — possible future feature proposal.) *(maps to §7, §9)*

**Q27.** §8 P9 memory-safety — JVM-bounded; is the reachability
boundary correctly "in-model for the JSON API + B5 input; out-of-model
for native hypervisor SDK bugs that surface as `Throwable`"? **RESOLVED** *(maintainer: DaanHoogland)* — the reachability boundary is right, but **§8 P9 must not imply CloudStack is Java-only** — no implementation-language limitation is presumed (ocaml, python, bash run on hypervisors; go is used on the management server; the set may grow). The memory-safety claims hold for the JVM components only. *(reflected in §8 P9.)* *(maps to §8 P9, §9)*

**Q28.** §8 P10 listing-scope — confirm the §10 invariant "`list*`
responses are scoped to the principal's domain/account/project". And:
is information leak via error messages / async-job status / event log an in-model concern, or accepted? **RESOLVED** *(maintainer: DaanHoogland)* — in-model: regular system logs (e.g. log4j) are exempt, but other than those, information leaks (via error messages, async-job status, event log) are a concern. *(maps to §8 P10, §9, §11)*

**Q29.** Data-at-rest encryption — confirm CloudStack delegates entirely
to storage layer / hypervisor (LUKS, Ceph encryption, vSphere VM
Encryption); no CloudStack-layer encryption of guest volumes. **RESOLVED** *(maintainer: DaanHoogland)* — correct (delegated to storage layer / hypervisor); *(vishesh92 to confirm)*. *(maps to §9)*

**Q30.** Constant-time comparison — confirm that *only* the API
signature path uses `ConstantTimeComparator`. Login password compare,
session cookie compare, console-token compare — none documented
constant-time. Is that intentional? **RESOLVED** *(maintainer: DaanHoogland)* — not intentional — the absence of constant-time comparison on the login-password / session-cookie / console-token paths is a lack of feature (hardening opportunity), not a by-design decision. *(maps to §8, §9)*

**Q31.** Time-of-check-to-time-of-use between RBAC check at API entry
and orchestration on agent fleet — confirm mid-job RBAC revocation is
**not** retroactively enforced (proposed). **RESOLVED** *(maintainer: DaanHoogland)* — agreed/confirmed. *(maps to §9)*

**Q32.** TLS posture on `:8080` vs `:8443` — confirm production deploys
behind TLS on `:8443` or behind a TLS-terminating reverse proxy; a bare
`:8080` HTTP API is dev-only. **RESOLVED** *(maintainer: DaanHoogland)* — confirmed. *(maps to §5a, §10)*

**Q33.** `security.encryption.key` reuse across environments — confirm
that reusing the JaSypt key + IV across staging and production is a
documented misuse. **RESOLVED** *(maintainer: DaanHoogland)* — indeed — confirmed misuse. *(maps to §11)*

### Wave 5 — meta

**Q34.** Should this document live at `docs/threat-model.md` in
`apache/cloudstack`, or as a page on `cloudstack.apache.org/security/`?
Or both, with one canonical and the other linked? **RESOLVED** *(maintainer: DaanHoogland, vishesh92)* — both: this document is the source of truth, and `cloudstack.apache.org/security` carries an excerpt plus a link to it. *(meta)*

**Q35.** Is there an existing CloudStack threat-model document
(Confluence, internal, or a `[SECURITY]`-tagged dev@ thread) that this
should reconcile against rather than supersede? **RESOLVED** *(maintainer: DaanHoogland)* — `cloudstack.apache.org/security/` is the only existing security model today; this document becomes its source of truth, with the page linking to it. *(meta — §3.1a of the rubric)*

**Q36.** What kind of change should trigger a revision (proposed list in
§12 — confirm or correct)? **RESOLVED** *(maintainer: DaanHoogland)* — confirmed, plus add: a change in the extension mechanisms implemented by CloudStack (now reflected in §12). *(meta, §12)*

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
/ §4 / §7 from this document. **RESOLVED** *(maintainer: DaanHoogland)* — confirmed; the satellites are not the system core (the core runs without them, they cannot run without the core), and there is an added hierarchy — `cloudstack-go` is a dependency of the other three. *(meta, §3 item 9)*

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
| `server/src/main/java/com/cloud/api/ApiServer.java` `verifyRequest` (lines ~980–1156) | HMAC-SHA1 signature + `expires` enforcement (`enforce.post.requests.and.timestamps`) + constant-time compare | §8 P1, §8 P3, §5a "enforce.post.requests.and.timestamps" row, §11a "SHA1 / constant-time" entries |
| `server/src/main/java/com/cloud/api/ApiServlet.java` `getClientAddress` (lines 700–725) | forward-header gating by `proxy.cidr` / `proxy.header.names` when `proxy.header.verify=true` | §8 P6, §5a "proxy.header.verify" row |
| `server/src/main/java/com/cloud/api/ApiServlet.java` 2FA path (lines 360–582) | password + 2FA flow | §8 P2 |
| `framework/ca/.../CAService.java`, `plugins/ca/root-ca/.../RootCAProvider.java` | Root CA generated at first boot; agent enrolment via `SetupKeyStoreCommand` | §4 B5, §8 P5, §5a strictness/allow-expired rows |
| `plugins/ca/root-ca/.../RootCACustomTrustManager.java` | `authStrictness` and `allowExpiredCertificate` semantics | §5a, §8 P5 |
| `plugins/acl/{static,dynamic,project}-role-based` | RBAC backends | §8 P4 |
| `plugins/user-authenticators/{md5,sha256salted,pbkdf2,plain-text,ldap,saml2,oauth2}` | pluggable user auth; selection via `user.password.encoders.order` / `user.password.encoders.exclude` | §2 caller-roles row, §5a "user.password.encoders.*" rows, §10 item 7 |
| `plugins/user-two-factor-authenticators/{static-pin,totp}` | 2FA backends | §5a "enable.user.2fa / mandate.user.2fa", §10 item 8 |
| `framework/security/.../KeysManager.java`, `KeystoreManager.java` | `security.encryption.key`, `security.encryption.iv` (Hidden), application-secret JaSypt encryption | §8 P8, §5a, §10 item 6 |
| `agent/src/main/java/com/cloud/agent/Agent.java` `setupAgentKeystore` (lines ~793–916) | agent receives Root CA-signed cert via `SetupKeyStoreCommand` and imports it | §4 B5, §8 P5 |
| `server/src/main/java/com/cloud/servlet/ConsoleProxyServlet.java`, `ConsoleProxyPasswordBasedEncryptor.java` | signed encrypted console-proxy URL token | §4 B3, §8 P7 |
| `https://cloudstack.apache.org/security.html` (website) | canonical disclosure landing page | §1 reporting cross-reference (note: not accessible from the producer's network at draft time; verify content with PMC) |
