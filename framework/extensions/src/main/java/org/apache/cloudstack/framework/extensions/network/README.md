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

# Network Extension Script Protocol

This document describes the complete interface between Apache CloudStack's
`NetworkExtensionElement` and the external script (Bash, Python, or any
executable) that implements network services for a custom device.

Any executable that handles the commands listed below can be registered as a
**NetworkOrchestrator extension** and used as the provider for one or more
CloudStack network services (DHCP, DNS, UserData, SourceNat, StaticNat,
PortForwarding, Firewall, Lb, NetworkACL, Gateway).

The reference implementation is the `network-namespace` extension at
`extensions/network-namespace/`, which uses Linux network namespaces on KVM
hosts.  Use it as a working example.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Script Placement Convention](#script-placement-convention)
3. [CloudStack Setup Steps](#cloudstack-setup-steps)
4. [Always-present CLI Arguments](#always-present-cli-arguments)
5. [Shared Arguments Reference](#shared-arguments-reference)
6. [Command Reference](#command-reference)
   - [ensure-network-device](#ensure-network-device)
   - [implement-network](#implement-network)
   - [shutdown-network](#shutdown-network)
   - [destroy-network](#destroy-network)
   - [implement-vpc](#implement-vpc)
   - [shutdown-vpc](#shutdown-vpc)
   - [update-vpc-source-nat-ip](#update-vpc-source-nat-ip)
   - [assign-ip / release-ip](#assign-ip--release-ip)
   - [add-static-nat / delete-static-nat](#add-static-nat--delete-static-nat)
   - [add-port-forward / delete-port-forward](#add-port-forward--delete-port-forward)
   - [apply-fw-rules](#apply-fw-rules)
   - [apply-network-acl](#apply-network-acl)
   - [add-dhcp-entry / remove-dhcp-entry](#add-dhcp-entry--remove-dhcp-entry)
   - [config-dhcp-subnet / remove-dhcp-subnet](#config-dhcp-subnet--remove-dhcp-subnet)
   - [set-dhcp-options](#set-dhcp-options)
   - [add-dns-entry](#add-dns-entry)
   - [config-dns-subnet / remove-dns-subnet](#config-dns-subnet--remove-dns-subnet)
   - [save-vm-data](#save-vm-data)
   - [save-password](#save-password)
   - [save-userdata](#save-userdata)
   - [save-sshkey](#save-sshkey)
   - [save-hypervisor-hostname](#save-hypervisor-hostname)
   - [apply-lb-rules](#apply-lb-rules)
   - [restore-network](#restore-network)
   - [custom-action](#custom-action)
7. [Service-to-Command Mapping](#service-to-command-mapping)
8. [Capabilities Configuration](#capabilities-configuration)
9. [VPC Networks](#vpc-networks)
10. [Extension IP](#extension-ip)
11. [Exit Codes](#exit-codes)
12. [Minimal Script Skeleton](#minimal-script-skeleton)

---

## Architecture Overview

```
CloudStack Management Server
        │
        │  exec  <extension-path>/<ext-name>.sh  <command>  [args...]
        │        --physical-network-extension-details '{...}'
        │        --network-extension-details '{...}'
        ▼
   Your Script (Bash / Python / Go / …)
        │
        │  configures / queries your device:
        │    • KVM host over SSH
        │    • SDN controller REST API
        │    • Hardware appliance CLI
        │    • Cloud provider API
        ▼
   External Network Device
```

CloudStack calls the script synchronously (blocking process execution) on the
**management server** for every network event.  The script is responsible for
translating those events into configuration changes on the actual device.

The script must:

- **Exit 0** on success.
- **Exit non-zero** on failure (CloudStack will log the error and may retry).
- For `ensure-network-device` only, **print a single-line JSON object** to
  stdout (see [ensure-network-device](#ensure-network-device)).

All other commands must produce no output on stdout (any output is logged at
DEBUG level and ignored).

---

## Script Placement Convention

CloudStack resolves the executable in this order (first match wins):

1. **`<extensionPath>/<extensionName>.sh`** — preferred convention.
   Example: extension named `my-sdn` → script at
   `.../my-sdn/my-sdn.sh`.
2. **`<extensionPath>` itself**, if it is a regular file and is executable.

The `<extensionPath>` is the `path` field returned by `listExtensions` after
the extension is created.  CloudStack sets it to:

```
/usr/share/cloudstack-management/extensions/<extensionName>/
```

> **Tip:** Your script does not have to live on the management server — it can
> be a thin proxy that SSHes into a remote appliance.  The
> `network-namespace.sh` entry-point is exactly that: it SSHes into the target
> KVM host and calls the wrapper script there.

---

## CloudStack Setup Steps

### Step 1 – Create the Extension

```bash
cmk createExtension \
    name=my-sdn \
    type=NetworkOrchestrator \
    "details[0].key=network.services" \
    "details[0].value=SourceNat,StaticNat,PortForwarding,Firewall,Lb,Dhcp,Dns,UserData" \
    "details[1].key=network.service.capabilities" \
    "details[1].value=$(cat my-sdn-capabilities.json)"
```

`network.service.capabilities` is a JSON object — see
[Capabilities Configuration](#capabilities-configuration).

### Step 2 – Deploy the Script

Copy your executable to the path reported by `listExtensions`:

```bash
SCRIPT_PATH=$(cmk listExtensions name=my-sdn | jq -r '.[0].path')
# e.g. /usr/share/cloudstack-management/extensions/my-sdn/
mkdir -p "${SCRIPT_PATH}"
cp my-sdn.sh "${SCRIPT_PATH}/my-sdn.sh"
chmod 755    "${SCRIPT_PATH}/my-sdn.sh"
```

If you have multiple management servers, deploy the script to **every** one.

### Step 3 – Register the Extension to a Physical Network

```bash
PHYS_ID=$(cmk listPhysicalNetworks | jq -r '.[0].id')
EXT_ID=$(cmk listExtensions name=my-sdn | jq -r '.[0].id')

cmk registerExtension \
    id=${EXT_ID} \
    resourcetype=PhysicalNetwork \
    resourceid=${PHYS_ID} \
    "details[0].key=hosts"    "details[0].value=192.168.1.10,192.168.1.11" \
    "details[1].key=username" "details[1].value=admin" \
    "details[2].key=password" "details[2].value=s3cr3t"
```

Any key/value pairs you pass here will be forwarded to every script
invocation as `--physical-network-extension-details`.  The schema is entirely
yours — CloudStack treats it as opaque.

### Step 4 – Enable the Network Service Provider

```bash
NSP_ID=$(cmk listNetworkServiceProviders physicalnetworkid=${PHYS_ID} \
         name=my-sdn | jq -r '.[0].id')
cmk updateNetworkServiceProvider id=${NSP_ID} state=Enabled
```

### Step 5 – Create a Network Offering

```bash
cmk createNetworkOffering \
    name="My-SDN-Offering" \
    displaytext="My SDN network offering" \
    guestiptype=Isolated \
    traffictype=GUEST \
    supportedservices=Dhcp,Dns,UserData,SourceNat,StaticNat,PortForwarding,Firewall,Lb \
    "serviceProviderList[Dhcp]=my-sdn" \
    "serviceProviderList[Dns]=my-sdn" \
    "serviceProviderList[UserData]=my-sdn" \
    "serviceProviderList[SourceNat]=my-sdn" \
    "serviceProviderList[StaticNat]=my-sdn" \
    "serviceProviderList[PortForwarding]=my-sdn" \
    "serviceProviderList[Firewall]=my-sdn" \
    "serviceProviderList[Lb]=my-sdn" \
    "serviceCapabilityList[SourceNat][SupportedSourceNatTypes]=peraccount"
cmk updateNetworkOffering id=<offering-id> state=Enabled
```

---

## Always-present CLI Arguments

Every command invocation appends these two named arguments **after** all
command-specific arguments:

| Argument | Value |
|---|---|
| `--physical-network-extension-details` | JSON object — all key/value pairs registered via `registerExtension` **plus** `physicalnetworkname` (auto-enriched by CloudStack). |
| `--network-extension-details` | JSON object — the per-network opaque blob last written by `ensure-network-device`; `{}` until the first successful call. |

Example call line built by CloudStack:

```
/usr/share/cloudstack-management/extensions/my-sdn/my-sdn.sh \
    implement-network \
    --network-id 42 \
    --vlan 100 \
    --gateway 10.0.0.1 \
    --cidr 10.0.0.0/24 \
    --extension-ip 10.0.0.1 \
    --physical-network-extension-details '{"hosts":"192.168.1.10","username":"admin","password":"s3cr3t","physicalnetworkname":"net1"}' \
    --network-extension-details '{"host":"192.168.1.10","device_id":"vrf-42"}'
```

> **Security note:** `password` and `sshkey` values are present verbatim in
> `--physical-network-extension-details` but are **redacted** in CloudStack
> log output.  Treat them as secrets; do not log them in your script either.

---

## Shared Arguments Reference

The following arguments appear in multiple commands.  Descriptions apply
everywhere they are used.

| Argument | Description |
|---|---|
| `--network-id <N>` | CloudStack numeric network ID. |
| `--vpc-id <N>` | CloudStack numeric VPC ID. Present only for VPC-tier networks. |
| `--vlan <tag>` | Guest VLAN tag (e.g. `100`). Extracted from the broadcast URI. May be empty for flat networks. |
| `--gateway <ip>` | Guest network gateway (e.g. `10.0.0.1`). |
| `--cidr <cidr>` | Guest network CIDR (e.g. `10.0.0.0/24`). |
| `--extension-ip <ip>` | The IP the extension device uses on the guest side. Equals `--gateway` when SourceNat/Gateway service is provided; otherwise a dedicated allocated IP from the guest subnet (see [Extension IP](#extension-ip)). |
| `--public-ip <ip>` | A public (floating) IP address. |
| `--public-cidr <cidr>` | CIDR of the public IP (e.g. `203.0.113.5/24`). |
| `--public-vlan <tag>` | VLAN tag of the public IP's network segment. |
| `--public-gateway <ip>` | Gateway of the public IP's network segment. |
| `--private-ip <ip>` | A VM's private IP address inside the guest network. |

---

## Command Reference

### `ensure-network-device`

**Called:** Before every network operation, automatically by
`NetworkExtensionElement`.

**Purpose:** Select (or re-validate) the device/host that will handle this
network.  Perform failover to another host if the current one is unreachable.
The returned JSON is stored in `network_details` under key `extension.details` and
forwarded back as `--network-extension-details` on every future call.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | Network ID. (omitted for VPC-level calls; `--vpc-id` is used instead) |
| `--vlan <tag>` | Guest VLAN. (network-level calls only) |
| `--zone-id <N>` | CloudStack zone ID. |
| `--vpc-id <N>` | VPC ID (optional for network-level calls; sole identifier for VPC-level calls). |
| `--current-details <json>` | Previously stored per-network blob (`{}` on first call). |
| `--physical-network-extension-details <json>` | Physical network details. |
| `--network-extension-details <json>` | Same as `--current-details`. |

**Stdout:** A single-line JSON object.  CloudStack stores this verbatim.
You can put any fields your script needs (host selection, device ID, segment
ID, namespace name, etc.).

```json
{"host":"192.168.1.10","device_id":"vrf-42","namespace":"cs-net-42"}
```

**Exit 0:** JSON written to stdout is persisted.  
**Exit non-zero:** Existing details are kept unchanged; a warning is logged.

---

### `implement-network`

**Called:** When a network is implemented (first VM deployed, or network
restart).

**Purpose:** Create / bring up the network segment on the device: create the
virtual segment (VRF, namespace, VLAN, …), attach the guest interface, and
configure the gateway.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vlan <tag>` | |
| `--gateway <ip>` | |
| `--cidr <cidr>` | |
| `--extension-ip <ip>` | Device's IP on the guest network. |
| `--vpc-id <N>` | (optional) |

---

### `shutdown-network`

**Called:** When a network is shut down (e.g., all VMs removed, before
deletion).

**Purpose:** Tear down the network segment; release resources.  The
per-network `extension.details` blob is removed from CloudStack after a successful
return.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vlan <tag>` | |
| `--vpc-id <N>` | (optional) |

---

### `destroy-network`

**Called:** When a network is permanently deleted.

**Purpose:** Same as `shutdown-network`, but called at hard-delete time.  The
placeholder NIC IP (if any) and the `extension.details` blob are cleaned up
automatically by CloudStack after a successful return.

**Arguments:** Identical to `shutdown-network`.

---

### `implement-vpc`

**Called:** When a VPC is implemented (before any tier is set up).

**Purpose:** Create the VPC-level networking state on the device (e.g., a
shared namespace or VRF that all tiers will attach to).  If a source-NAT IP
is already allocated for the VPC, its details are also included so the script
can set up the VPC-level SNAT rule at this stage.

**Arguments:**

| Argument | Description |
|---|---|
| `--vpc-id <N>` | |
| `--cidr <cidr>` | VPC supernet CIDR. |
| `--public-ip <ip>` | Source-NAT IP (optional; present only if already allocated). |
| `--public-vlan <tag>` | VLAN of the source-NAT IP (optional). |
| `--public-gateway <ip>` | Gateway of the source-NAT IP segment (optional). |
| `--public-cidr <cidr>` | CIDR of the source-NAT IP (optional). |
| `--source-nat <true>` | Always `true` when public IP args are present. |

---

### `shutdown-vpc`

**Called:** During `shutdownVpc` after all tier networks have been destroyed.

**Purpose:** Remove the VPC-level namespace / VRF and all associated state.
The `extension.details` blob is removed from CloudStack after a successful return.

**Arguments:**

| Argument | Description |
|---|---|
| `--vpc-id <N>` | |

---

### `update-vpc-source-nat-ip`

**Called:** When the VPC source-NAT IP changes
(`updateVpcSourceNatIp` API).

**Purpose:** Update the VPC-level SNAT rule to point to the new public IP.

**Arguments:**

| Argument | Description |
|---|---|
| `--vpc-id <N>` | |
| `--cidr <cidr>` | VPC supernet CIDR. |
| `--public-ip <ip>` | New source-NAT IP. |
| `--public-vlan <tag>` | VLAN of the new source-NAT IP. |
| `--public-gateway <ip>` | Gateway of the new source-NAT IP segment. |
| `--public-cidr <cidr>` | CIDR of the new source-NAT IP. |
| `--source-nat <true>` | Always `true`. |

---

### `assign-ip` / `release-ip`

**Called:** When a public IP is associated with or disassociated from a
network (source NAT, static NAT, PF, LB allocation).

**Purpose:**  
- `assign-ip` — attach the public IP to the device; add the necessary routing
  entry so the device can receive traffic for this IP.  
- `release-ip` — detach the public IP; remove routing.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vlan <tag>` | Guest VLAN. |
| `--public-ip <ip>` | The public IP being assigned/released. |
| `--source-nat <true\|false>` | `true` if this is the source NAT IP. |
| `--gateway <ip>` | Guest network gateway. |
| `--cidr <cidr>` | Guest network CIDR. |
| `--public-gateway <ip>` | Gateway of the public IP's segment. |
| `--public-cidr <cidr>` | CIDR of the public IP (e.g. `203.0.113.5/24`). |
| `--public-vlan <tag>` | Public VLAN tag. |
| `--vpc-id <N>` | (optional) |

---

### `add-static-nat` / `delete-static-nat`

**Called:** When a static NAT rule is created or deleted
(`enableStaticNat` / `disableStaticNat` API).

**Purpose:** Configure a 1:1 bidirectional NAT mapping between a public IP
and a VM private IP.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vlan <tag>` | |
| `--public-ip <ip>` | |
| `--public-cidr <cidr>` | |
| `--public-vlan <tag>` | |
| `--private-ip <ip>` | VM's private IP (DNAT destination). |
| `--vpc-id <N>` | (optional) |

---

### `add-port-forward` / `delete-port-forward`

**Called:** When a port forwarding rule is created or deleted
(`createPortForwardingRule` / `deletePortForwardingRule` API).

**Purpose:** Configure a DNAT rule from `public-ip:public-port` to
`private-ip:private-port`.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vlan <tag>` | |
| `--public-ip <ip>` | |
| `--public-cidr <cidr>` | |
| `--public-vlan <tag>` | |
| `--public-port <range>` | Port range on the public IP, e.g. `22` or `8080-8090`. |
| `--private-ip <ip>` | VM's private IP. |
| `--private-port <range>` | Destination port range on the VM, e.g. `22`. |
| `--protocol <tcp\|udp>` | |
| `--vpc-id <N>` | (optional) |

---

### `apply-fw-rules`

**Called:** When any firewall rule is created, deleted, or updated for the
network (`createFirewallRule`, `deleteFirewallRule`, `updateEgressFirewallRule`
APIs, and during network restart).

**Purpose:** Rebuild the entire firewall policy for the network from scratch.
CloudStack calls this with a *narrow* scope (one IP's ingress rules or egress
rules per call), but the `--fw-rules-file` payload always contains **all** active
rules for the network, so a full rebuild is always safe.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vlan <tag>` | |
| `--gateway <ip>` | |
| `--cidr <cidr>` | |
| `--fw-rules-file <path>` | Path to a temporary file containing the Base64-encoded JSON firewall payload (see below). |
| `--vpc-id <N>` | (optional) |

> **Note:** The payload is written to a temporary file to avoid shell argument
> length limits for large rule sets.  Read the file contents and then
> Base64-decode to obtain the JSON.

**`--fw-rules-file` payload** (read file, decode base64, then parse JSON):

```json
{
  "default_egress_allow": true,
  "cidr": "10.0.0.0/24",
  "rules": [
    {
      "id": 1,
      "type": "ingress",
      "protocol": "tcp",
      "portStart": 22,
      "portEnd": 22,
      "publicIp": "203.0.113.5",
      "sourceCidrs": ["0.0.0.0/0"],
      "destCidrs": []
    },
    {
      "id": 2,
      "type": "egress",
      "protocol": "icmp",
      "icmpType": -1,
      "icmpCode": -1,
      "sourceCidrs": [],
      "destCidrs": []
    }
  ]
}
```

| Field | Description |
|---|---|
| `default_egress_allow` | `true` = permissive egress by default (explicit rules are deny rules); `false` = restrictive (explicit rules are allow rules). |
| `cidr` | Guest network CIDR. |
| `rules[].type` | `"ingress"` or `"egress"`. |
| `rules[].protocol` | `"tcp"`, `"udp"`, `"icmp"`, `"all"`. |
| `rules[].portStart` / `portEnd` | TCP/UDP port range (absent for ICMP/all). |
| `rules[].icmpType` / `icmpCode` | ICMP type/code (`-1` = any; absent for TCP/UDP). |
| `rules[].publicIp` | For ingress: the public IP the rule applies to. |
| `rules[].sourceCidrs` | Allowed source IP ranges (ingress: external; egress: VM). |
| `rules[].destCidrs` | Allowed destination IP ranges (egress only). |

---

### `apply-network-acl`

**Called:** When a VPC network ACL is applied to a tier network
(`createNetworkACLList`, `replaceNetworkACLList`, `updateNetworkACLItem`,
`moveNetworkAclItem` APIs, and during network restart).

**Purpose:** Rebuild the entire ACL policy for the VPC tier from scratch.
Rules are applied in ascending `number` order.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vlan <tag>` | |
| `--gateway <ip>` | |
| `--cidr <cidr>` | |
| `--acl-rules-file <path>` | Path to a temporary file containing the Base64-encoded JSON ACL rules array (see below). |
| `--vpc-id <N>` | (VPC tier; always present) |

**`--acl-rules-file` payload** (read file, decode base64, then parse JSON):

```json
[
  {
    "number":      10,
    "action":      "allow",
    "trafficType": "ingress",
    "protocol":    "tcp",
    "portStart":   22,
    "portEnd":     22,
    "sourceCidrs": ["0.0.0.0/0"]
  },
  {
    "number":      20,
    "action":      "deny",
    "trafficType": "egress",
    "protocol":    "all",
    "sourceCidrs": []
  }
]
```

| Field | Description |
|---|---|
| `number` | Rule priority (lower number = higher priority). |
| `action` | `"allow"` or `"deny"`. |
| `trafficType` | `"ingress"` or `"egress"`. |
| `protocol` | `"tcp"`, `"udp"`, `"icmp"`, `"all"`. |
| `portStart` / `portEnd` | TCP/UDP port range (absent for ICMP/all). |
| `icmpType` / `icmpCode` | ICMP type/code (absent for TCP/UDP). |
| `sourceCidrs` | Source CIDR filter list. |

---

### `add-dhcp-entry` / `remove-dhcp-entry`

**Called:** When a VM NIC is reserved (`add`) or released (`remove`) on a
network whose DHCP service is provided by this extension.

**Purpose:** Add or remove a static DHCP lease for the VM.

**`add-dhcp-entry` arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--mac <addr>` | VM NIC MAC address, e.g. `02:00:00:00:00:01`. |
| `--ip <ip>` | VM's assigned IP. |
| `--hostname <name>` | VM hostname. |
| `--gateway <ip>` | |
| `--cidr <cidr>` | |
| `--dns <list>` | Comma-separated DNS server IPs, e.g. `8.8.8.8,8.8.4.4`. |
| `--default-nic <bool>` | `true` if this is the VM's default NIC. |
| `--domain <name>` | Network domain suffix (e.g. `cs.example.com`). |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

**`remove-dhcp-entry` arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--mac <addr>` | VM NIC MAC address. |
| `--ip <ip>` | VM's assigned IP. |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `config-dhcp-subnet` / `remove-dhcp-subnet`

**Called:** When a shared-network subnet is configured or removed.

**Purpose:** Configure the DHCP scope (pool, gateway, DNS) for a subnet
without tying it to a specific VM.

**`config-dhcp-subnet` arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--gateway <ip>` | |
| `--cidr <cidr>` | |
| `--dns <list>` | |
| `--vlan <tag>` | |
| `--domain <name>` | |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

**`remove-dhcp-subnet` arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `set-dhcp-options`

**Called:** When extra DHCP options are set on a NIC
(`updateNicExtraDhcpOption` API).

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--nic-id <N>` | CloudStack NIC ID. |
| `--options <json>` | JSON object `{"<dhcp-code>":"<value>", …}`, e.g. `{"15":"example.com","119":"search.example.com"}`. |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `add-dns-entry`

**Called:** When a VM NIC is reserved on a network whose DNS service is
provided by this extension.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--ip <ip>` | VM's IP. |
| `--hostname <name>` | VM hostname. |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `config-dns-subnet` / `remove-dns-subnet`

**Called:** When a DNS scope is configured or removed for a subnet.

**`config-dns-subnet` arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--gateway <ip>` | |
| `--cidr <cidr>` | |
| `--dns <list>` | |
| `--vlan <tag>` | |
| `--domain <name>` | |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

**`remove-dns-subnet` arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `save-vm-data`

**Called:** When a VM is deployed or updated with user data, SSH keys, or
password on a network whose UserData service is provided by this extension.

**Purpose:** Store the complete cloud-init metadata set (user-data,
meta-data/*, password) for the VM so the metadata HTTP server can serve it.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--ip <ip>` | VM's IP. |
| `--gateway <ip>` | |
| `--vm-data-file <path>` | Path to a temporary file containing the Base64-encoded JSON array of metadata entries (see below). |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

> **Note:** The payload is written to a temporary file to avoid shell argument
> length limits for large user-data blobs.  Read the file contents and then
> Base64-decode to obtain the JSON array.

**`--vm-data-file` payload** (read file, decode base64, then parse JSON):

```json
[
  {"dir":"userdata",   "file":"user-data",       "content":"<base64>"},
  {"dir":"meta-data",  "file":"instance-id",      "content":"<base64>"},
  {"dir":"meta-data",  "file":"local-hostname",   "content":"<base64>"},
  {"dir":"meta-data",  "file":"public-keys/0/openssh-key", "content":"<base64>"},
  {"dir":"password",   "file":"vm_password",      "content":"<base64>"}
]
```

Each `content` field is **base64-encoded** binary (raw bytes for user-data;
UTF-8 text for all others).  Decode `content` before writing to disk.

Your metadata HTTP server should serve each entry at:
`http://<extension-ip>/latest/<dir>/<file>`

---

### `save-password`

**Called:** When a password reset is requested for a VM
(`resetPasswordForVirtualMachine` API).

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--ip <ip>` | VM's IP. |
| `--gateway <ip>` | |
| `--password <pw>` | Plain-text new password. |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `save-userdata`

**Called:** When a VM's user data is updated
(`updateVirtualMachine` with `userdata`).

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--ip <ip>` | VM's IP. |
| `--gateway <ip>` | |
| `--userdata <base64>` | Base64-encoded raw user-data bytes. |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `save-sshkey`

**Called:** When an SSH public key is reset for a VM
(`resetSSHKeyForVirtualMachine` API).

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--ip <ip>` | VM's IP. |
| `--gateway <ip>` | |
| `--sshkey <base64>` | Base64-encoded SSH public key (UTF-8 text). Decode to get the key string. |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `save-hypervisor-hostname`

**Called:** When a VM is deployed and UserData service is active.

**Purpose:** Store the hypervisor hostname in the metadata so VMs can identify
which host they run on (cloud-init `availability-zone` / host detection).

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--ip <ip>` | VM's IP. |
| `--gateway <ip>` | |
| `--hypervisor-hostname <name>` | Hypervisor node hostname. |
| `--extension-ip <ip>` | |
| `--vpc-id <N>` | (optional) |

---

### `apply-lb-rules`

**Called:** When a load balancer rule is created, deleted, or its members
change (`createLoadBalancerRule`, `deleteLoadBalancerRule`,
`assignToLoadBalancerRule`, `removeFromLoadBalancerRule` APIs).

**Purpose:** Configure the load balancer on the device: create/update/delete
virtual server → backend pool mappings.

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vlan <tag>` | |
| `--lb-rules <json>` | JSON array of LB rules (see below). **Not** base64 encoded. |
| `--vpc-id <N>` | (optional) |

**`--lb-rules` format:**

```json
[
  {
    "id":          1,
    "name":        "lb-web",
    "publicIp":    "203.0.113.5",
    "publicPort":  80,
    "privatePort": 8080,
    "protocol":    "tcp",
    "algorithm":   "roundrobin",
    "revoke":      false,
    "backends": [
      {"ip": "10.0.0.10", "port": 8080, "revoked": false},
      {"ip": "10.0.0.11", "port": 8080, "revoked": true}
    ]
  }
]
```

| Field | Description |
|---|---|
| `revoke` | `true` → delete this rule; `false` → create/update. |
| `backends[].revoked` | `true` → this backend has been removed from the rule. |
| `algorithm` | `roundrobin`, `leastconn`, or `source`. |
| `protocol` | `tcp`, `udp`, or `tcp-proxy`. |

---

### `restore-network`

**Called:** During a `restartNetwork(cleanup=true)` or `restartVPC(cleanup=true)`
operation, after all rules (firewall/NAT/LB) have been re-applied.

**Purpose:** Batch-restore all DHCP leases, DNS entries, and metadata for
every VM currently on the network in a single call (instead of N per-VM
calls).

**Arguments:**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--gateway <ip>` | |
| `--cidr <cidr>` | |
| `--vlan <tag>` | |
| `--extension-ip <ip>` | |
| `--dns <list>` | |
| `--domain <name>` | |
| `--restore-data-file <path>` | Path to a temporary file containing the Base64-encoded JSON restore payload (see below). |
| `--vpc-id <N>` | (optional) |

> **Note:** The payload is written to a temporary file to avoid shell argument
> length limits for large networks.  Read the file contents and then
> Base64-decode to obtain the JSON.

**`--restore-data-file` payload** (read file, decode base64, then parse JSON):

```json
{
  "dhcp_enabled":     true,
  "dns_enabled":      true,
  "userdata_enabled": true,
  "vms": [
    {
      "ip":          "10.0.0.10",
      "mac":         "02:00:00:00:00:01",
      "hostname":    "vm-1",
      "default_nic": true,
      "vm_data": [
        {"dir": "userdata",  "file": "user-data",    "content": "<base64>"},
        {"dir": "meta-data", "file": "instance-id",  "content": "<base64>"},
        {"dir": "meta-data", "file": "local-hostname","content": "<base64>"}
      ]
    }
  ]
}
```

Each `vm_data[].content` is **base64-encoded** (same as in `save-vm-data`).

---

### `custom-action`

**Called:** Via the `runNetworkCustomAction` API.

**Purpose:** Allows operators to trigger ad-hoc operations on the device
without defining new CloudStack API calls.

**Arguments (network-level):**

| Argument | Description |
|---|---|
| `--network-id <N>` | |
| `--vpc-id <N>` | (optional, for VPC-tier networks) |
| `--action <name>` | The action name passed by the operator. |
| `--action-params <json>` | JSON object with arbitrary key/value parameters. |
| `--physical-network-extension-details <json>` | |
| `--network-extension-details <json>` | |

**Arguments (VPC-level):**

| Argument | Description |
|---|---|
| `--vpc-id <N>` | |
| `--action <name>` | The action name passed by the operator. |
| `--action-params <json>` | JSON object with arbitrary key/value parameters. |
| `--physical-network-extension-details <json>` | |
| `--network-extension-details <json>` | |

**Stdout:** Returned verbatim to the API caller.

---

## Service-to-Command Mapping

| CloudStack Network Service | Commands triggered |
|---|---|
| **SourceNat / Gateway** | `assign-ip`, `release-ip` |
| **StaticNat** | `add-static-nat`, `delete-static-nat` |
| **PortForwarding** | `add-port-forward`, `delete-port-forward` |
| **Firewall** | `apply-fw-rules` |
| **Lb** | `apply-lb-rules` |
| **NetworkACL** | `apply-network-acl` |
| **Dhcp** | `add-dhcp-entry`, `remove-dhcp-entry`, `config-dhcp-subnet`, `remove-dhcp-subnet`, `set-dhcp-options` |
| **Dns** | `add-dns-entry`, `config-dns-subnet`, `remove-dns-subnet` |
| **UserData** | `save-vm-data`, `save-password`, `save-userdata`, `save-sshkey`, `save-hypervisor-hostname` |
| *(network lifecycle — all)* | `ensure-network-device`, `implement-network`, `shutdown-network`, `destroy-network`, `restore-network` |
| *(VPC lifecycle)* | `ensure-network-device`, `implement-vpc`, `shutdown-vpc`, `update-vpc-source-nat-ip` |
| *(operator)* | `custom-action` |

Your script only needs to implement the commands for the services it declares
in `network.services`.  All other commands can safely be no-ops (exit 0).

---

## Capabilities Configuration

When creating the extension, set `network.service.capabilities` to a JSON
object keyed by service name.  The values are capability name → value string
maps.

```json
{
  "SourceNat": {
    "SupportedSourceNatTypes": "peraccount",
    "RedundantRouter":         "false"
  },
  "StaticNat": {
    "Supported": "true"
  },
  "PortForwarding": {
    "SupportedProtocols": "tcp,udp"
  },
  "Firewall": {
    "TrafficStatistics":       "per public ip",
    "SupportedProtocols":      "tcp,udp,icmp",
    "SupportedEgressProtocols":"tcp,udp,icmp,all",
    "SupportedTrafficDirection":"ingress,egress",
    "MultipleIps":             "true"
  },
  "Lb": {
    "SupportedLBAlgorithms":       "roundrobin,leastconn,source",
    "SupportedLBIsolation":        "dedicated",
    "SupportedProtocols":          "tcp,udp,tcp-proxy",
    "SupportedStickinessMethods":  "lbcookie,appsession",
    "LbSchemes":                   "Public",
    "SslTermination":              "false",
    "VmAutoScaling":               "false"
  },
  "Dhcp": {
    "DhcpAccrossMultipleSubnets": "true"
  },
  "Dns": {
    "AllowDnsSuffixModification": "true",
    "ExternalDns":                "true"
  },
  "Gateway": {
    "RedundantRouter": "false"
  },
  "NetworkACL": {
    "SupportedProtocols": "tcp,udp,icmp"
  },
  "UserData": {
    "Supported": "true"
  }
}
```

Only declare services and capabilities your implementation actually supports.

---

## VPC Networks

For networks that belong to a VPC, `--vpc-id <N>` is appended to every
command.  Use it to share state across all tiers of the same VPC (e.g., a
single VRF or namespace per VPC instead of per tier).

In `ensure-network-device`, use `--vpc-id` (when present) as the hash key for
host selection so all tiers of a VPC always land on the same device.

VPC lifecycle commands (`implement-vpc`, `shutdown-vpc`,
`update-vpc-source-nat-ip`) are invoked at the VPC level with no
`--network-id` — only `--vpc-id`.  Tier-level commands such as
`implement-network` and `destroy-network` still receive both
`--network-id` and `--vpc-id`.

To use this extension as a VPC provider:

1. Create the NetworkOffering with `useVpc=on`.
2. Create a VpcOffering with the extension as provider.

---

## Extension IP

`--extension-ip` is the IP the device presents on the guest network side:

- **With SourceNat or Gateway service:** equals `--gateway` (the device is the
  gateway; no separate IP needed).
- **Without SourceNat/Gateway** (Dhcp/Dns/UserData only, e.g. a shared network
  helper): CloudStack allocates a dedicated IP from the guest subnet and passes
  it.  The device must listen on this IP for DHCP, DNS, and metadata (port 80)
  requests.

---

## Exit Codes

| Exit code | Meaning |
|---|---|
| `0` | Success. |
| Any non-zero | Failure. CloudStack logs the exit code and script output, and treats the operation as failed. |

For SSH-proxy scripts you may use sub-codes for diagnostics (they are logged
but not interpreted differently by CloudStack):

| Suggested code | Suggested meaning |
|---|---|
| `1` | Usage / configuration error. |
| `2` | SSH connection / authentication failure. |
| `3` | Remote script returned non-zero. |

---

## Minimal Script Skeleton

The following Bash skeleton handles all commands and can be used as a starting
point.  Replace each `TODO` block with your device's API calls.

```bash
#!/bin/bash
# my-sdn.sh — CloudStack NetworkOrchestrator extension entry-point
set -euo pipefail

COMMAND="${1:-}"; shift || true

# Parse arguments into an associative array
declare -A ARGS
while [[ $# -gt 0 ]]; do
    case "$1" in
        --*) ARGS["${1#--}"]="${2:-}"; shift 2 ;;
        *)   shift ;;
    esac
done

# Helpers
phys()    { echo "${ARGS[physical-network-extension-details]:-{}}"; }
netdetail(){ echo "${ARGS[network-extension-details]:-{}}"; }
arg()     { echo "${ARGS[$1]:-}"; }

# Read a file-payload argument, base64-decode, and echo the JSON
read_payload() {
    local filepath="${ARGS[$1]:-}"
    if [[ -z "${filepath}" || ! -f "${filepath}" ]]; then
        echo "{}"
        return
    fi
    base64 -d < "${filepath}"
}

case "${COMMAND}" in

    ensure-network-device)
        # TODO: check that the device is reachable; select/validate host
        # Print per-network JSON to stdout (stored by CloudStack)
        printf '{"device":"%s"}\n' "$(arg hosts | cut -d, -f1)"
        ;;

    implement-network)
        # TODO: create virtual segment (VRF / VLAN / namespace / …)
        # TODO: configure gateway IP $(arg extension-ip) on $(arg cidr)
        ;;

    shutdown-network|destroy-network)
        # TODO: tear down the virtual segment
        ;;

    implement-vpc)
        # TODO: create VPC-level namespace / VRF for vpc=$(arg vpc-id)
        # Optional source-NAT IP: $(arg public-ip)
        ;;

    shutdown-vpc)
        # TODO: remove VPC namespace / VRF for vpc=$(arg vpc-id)
        ;;

    update-vpc-source-nat-ip)
        # TODO: update VPC SNAT rule to use new public IP $(arg public-ip)
        ;;

    assign-ip)
        # TODO: attach public IP $(arg public-ip) to the device
        ;;

    release-ip)
        # TODO: remove public IP $(arg public-ip) from the device
        ;;

    add-static-nat)
        # TODO: DNAT $(arg public-ip) → $(arg private-ip)
        ;;

    delete-static-nat)
        # TODO: remove DNAT for $(arg public-ip)
        ;;

    add-port-forward)
        # TODO: DNAT $(arg public-ip):$(arg public-port) → $(arg private-ip):$(arg private-port)
        ;;

    delete-port-forward)
        # TODO: remove the port-forwarding DNAT rule
        ;;

    apply-fw-rules)
        # Read base64 payload from file, decode, then apply
        FW_JSON=$(read_payload fw-rules-file)
        # TODO: parse $FW_JSON and apply to device
        ;;

    apply-network-acl)
        # Read base64 payload from file, decode, then apply
        ACL_JSON=$(read_payload acl-rules-file)
        # TODO: parse $ACL_JSON and apply to VPC tier
        ;;

    add-dhcp-entry)
        # TODO: add static lease mac=$(arg mac) ip=$(arg ip)
        ;;

    remove-dhcp-entry)
        # TODO: remove static lease for mac=$(arg mac)
        ;;

    config-dhcp-subnet|remove-dhcp-subnet) ;;

    set-dhcp-options) ;;

    add-dns-entry)
        # TODO: add A record hostname=$(arg hostname) ip=$(arg ip)
        ;;

    config-dns-subnet|remove-dns-subnet) ;;

    save-vm-data)
        # Read base64 payload from file, decode, then store metadata for ip=$(arg ip)
        VM_DATA_JSON=$(read_payload vm-data-file)
        # TODO: iterate entries and write to metadata store
        ;;

    save-password|save-userdata|save-sshkey|save-hypervisor-hostname) ;;

    apply-lb-rules)
        # TODO: parse --lb-rules JSON; configure load balancer
        ;;

    restore-network)
        # Read base64 payload from file, decode, then rebuild DHCP/DNS/metadata
        RESTORE_JSON=$(read_payload restore-data-file)
        # TODO: iterate vms and restore leases / DNS / metadata
        ;;

    custom-action)
        # TODO: handle $(arg action) with params $(arg action-params)
        echo "custom action $(arg action) not implemented"
        exit 1
        ;;

    *)
        echo "Unknown command: ${COMMAND}" >&2
        exit 1
        ;;
esac

exit 0
```

For a full production implementation see https://github.com/apache/cloudstack-extensions/tree/network-namespace/Network-Namespace:
- `network-namespace.sh` — management-server entry-point (SSH proxy).
- `enetwork-namespace-wrapper.sh` — KVM-host wrapper that implements all commands using Linux network namespaces.
