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
4. [Always-present payload fields](#always-present-payload-fields)
5. [Shared payload fields](#shared-payload-fields)
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
   - [prepare-nic / release-nic](#prepare-nic--release-nic)
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
        │  exec  <extension-path>/<ext-name>.sh  <command>  <payload-file>  <timeout-seconds>
        │        payload-file contains JSON for the command invocation
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
    "details[1].value=$(cat my-sdn-capabilities.json)" \
    "details[2].key=network.isolation.method" \
    "details[2].value=NetworkExtension"
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
    "details[0].key=hosts"             "details[0].value=192.168.1.10,192.168.1.11" \
    "details[1].key=username"          "details[1].value=admin" \
    "details[2].key=password"          "details[2].value=s3cr3t"
```

Any key/value pairs you pass here are stored with the physical-network
registration as extension metadata. The `custom-action` path embeds them
directly into the payload file under `physical-network-extension-details`.
The schema is entirely yours — CloudStack treats it as opaque.

> **`isolation_method=NetworkExtension`** tells CloudStack to select
> `NetworkExtensionGuestNetworkGuru` when designing guest networks backed by
> this extension.  This is required whenever your `implement-network` handler
> outputs JSON that updates the network's broadcast domain type (e.g.
> `"network.broadcast_domain_type": "Lswitch"` for OVN-backed extensions).
> Without it the script output is accepted but silently ignored — the network
> record in the CloudStack database is not updated.

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

## Always-present payload fields

For all standard network / VPC commands, CloudStack now executes the script as:

```text
<extension-path>/<extension-name>.sh <command> <payload-file> <timeout-seconds>
```

`payload-file` contains a JSON object with this envelope:

```json
{
  "physical-network-extension-details": {},
  "network-extension-details": {},
  "payload": {}
}
```

| Field | Value |
|---|---|
| `physical-network-extension-details` | Physical-network extension metadata registered on the physical network, enriched with `physicalnetworkname`. |
| `network-extension-details` | Additional network or VPC details stored in CloudStack and forwarded as a JSON object. |
| `payload` | Command-specific JSON object for the command being executed. |

`timeout-seconds` is currently `60`.

> **Important:** `custom-action` is still the exception in shape. It uses its
> own top-level payload structure and does **not** wrap command-specific fields
> under a nested `payload` object. Use top-level `action-params` for
> command-specific parameters.

---

## Shared payload fields

The following names appear repeatedly inside the nested `payload` object.

### Network-level fields (added by `addNetworkToPayload`)

| Field | Description |
|---|---|
| `network_id` | CloudStack numeric network ID. |
| `vlan` | Guest VLAN tag (for example `100`). Extracted from the broadcast URI. May be empty for flat networks. |
| `zone_id` | CloudStack zone ID. |
| `guest_type` | Guest network type: `"isolated"`, `"shared"`, or `"l2"`. Scripts should use this to skip NAT/firewall operations that are not applicable to Shared or L2 networks. |
| `gateway` | Guest network gateway (for example `10.0.0.1`). Omitted when blank. |
| `cidr` | Guest network CIDR (for example `10.0.0.0/24`). Omitted when blank. |
| `vpc_id` | CloudStack numeric VPC ID. Present for VPC tier networks and VPC-scoped commands. |
| `network_ip6_gateway` | Guest network IPv6 gateway, when the network has IPv6 configured. |
| `network_ip6_cidr` | Guest network IPv6 CIDR, when the network has IPv6 configured. |

### NIC-level fields (added by `addNicToPayload`)

| Field | Description |
|---|---|
| `nic_id` | CloudStack numeric NIC ID. |
| `nic_uuid` | NIC UUID — matches `external_ids:iface-id` written by the KVM agent for OVN port binding. |
| `mac` | VM NIC MAC address. |
| `ip` | VM NIC IPv4 address. |
| `gateway` | VM NIC IPv4 gateway (NIC-level; equals the network gateway for normal guest networks). |
| `netmask` | VM NIC IPv4 netmask (for example `255.255.255.0`). |
| `default_nic` | Stringified boolean — `"false"` for secondary NICs. |
| `device_id` | NIC device index in the VM (slot number). |
| `ip6_address` | VM NIC IPv6 address, when the NIC has IPv6 configured. |
| `ip6_gateway` | VM NIC IPv6 gateway, when available. |
| `ip6_cidr` | VM NIC IPv6 CIDR, when available. |

### Public-IP fields (added by `addPublicIpToPayload`)

| Field | Description |
|---|---|
| `public_ip` | A public IP address. |
| `public_vlan` | VLAN tag of the public IP segment. |
| `public_gateway` | Gateway of the public IP segment. |
| `public_cidr` | CIDR of the public IP (for example `203.0.113.0/24`). |
| `source_nat` | Stringified boolean (`"true"` / `"false"`) indicating whether the public IP is the source-NAT IP. |
| `private_ip` | A VM's private guest-network IP address (NAT target). |

### DNS / extension-IP fields

| Field | Description |
|---|---|
| `extension_ip` | The IP the extension device uses on the guest side. Equals the gateway when SourceNat/Gateway is provided; otherwise it is a dedicated IP from the guest subnet. |
| `dns` | Comma-separated DNS server list. |
| `domain` | Network domain suffix. |

---

## Command Reference

### `ensure-network-device`

**Called:** Before every network operation, automatically by
`NetworkExtensionElement`.

**Purpose:** Select (or re-validate) the device/host that will handle this
network.  Perform failover to another host if the current one is unreachable.
The returned JSON is stored in `network_details` under key `extension.details` and
passed back to later `ensure-network-device` calls as `payload.current_details`.

**Payload file shape:**

```json
{
  "physical-network-extension-details": {},
  "network-extension-details": {},
  "payload": {
    "network_id": "42",
    "vlan": "100",
    "zone_id": "1",
    "vpc_id": "7",
    "current_details": "{}"
  }
}
```

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. Omitted for VPC-level calls. |
| `vlan` | Guest VLAN. Present only for network-level calls. |
| `zone_id` | CloudStack zone ID. |
| `vpc_id` | VPC ID for VPC-level calls, and also present for VPC tier networks. |
| `current_details` | Previously stored `extension.details` JSON string (`{}` on first call). |

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

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `extension_ip` | Device IP on the guest network. |
| `vpc_id` | Present for VPC tier networks. |

> **IPv6 note:** For network-scoped commands that already include `gateway`/`cidr`,
> CloudStack now also includes `network_ip6_gateway` and `network_ip6_cidr`
> when the guest network has IPv6 configured.

**Stdout (required when `isolation_method=NetworkExtension`):**

When the physical-network registration detail `isolation_method=NetworkExtension`
is set, CloudStack selects `NetworkExtensionGuestNetworkGuru` and applies the
JSON object printed to stdout back to the network record.  The following two
fields **must** be present in the output so that CloudStack stores the correct
broadcast type and URI — without them the KVM agent (`OvsVifDriver`) will not
set `external_ids:iface-id` on the OVS tap port and OVN port-binding will fail:

| Output key | Required value | Description |
|---|---|---|
| `network.broadcast_domain_type` | `"Lswitch"` (OVN) or appropriate type | Sets `BroadcastDomainType` on the network record. |
| `network.broadcast_uri` | e.g. `"ovn://cs-net-<networkId>"` | Sets the broadcast URI used by the hypervisor agent. |

Example stdout:

```json
{"network.broadcast_domain_type": "Lswitch", "network.broadcast_uri": "ovn://cs-net-42"}
```

> **Note:** These fields are only consumed when `isolation_method=NetworkExtension`
> is set on the physical-network registration.  Without that detail, the script
> output is accepted but the network record is **not** updated (the update is
> silently skipped).

---

### `shutdown-network`

**Called:** When a network is shut down (e.g., all VMs removed, before
deletion).

**Purpose:** Tear down the network segment; release resources.  The
per-network `extension.details` blob is removed from CloudStack after a successful
return.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `vpc_id` | Present for VPC tier networks. |

---

### `destroy-network`

**Called:** When a network is permanently deleted.

**Purpose:** Same as `shutdown-network`, but called at hard-delete time.  The
placeholder NIC IP (if any) and the `extension.details` blob are cleaned up
automatically by CloudStack after a successful return.

**Payload fields (`payload` object):** Identical to `shutdown-network`.

---

### `implement-vpc`

**Called:** When a VPC is implemented (before any tier is set up).

**Purpose:** Create the VPC-level networking state on the device (e.g., a
shared namespace or VRF that all tiers will attach to).  If a source-NAT IP
is already allocated for the VPC, its details are also included so the script
can set up the VPC-level SNAT rule at this stage.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `vpc_id` | VPC ID. |
| `vpc_cidr` | VPC supernet CIDR. |
| `public_ip` | Source-NAT IP, when already allocated. |
| `public_vlan` | VLAN of the source-NAT IP, when present. |
| `public_gateway` | Gateway of the source-NAT IP segment, when present. |
| `public_cidr` | CIDR of the source-NAT IP, when present. |
| `source_nat` | `"true"` when the public IP fields are present. |

---

### `shutdown-vpc`

**Called:** During `shutdownVpc` after all tier networks have been destroyed.

**Purpose:** Remove the VPC-level namespace / VRF and all associated state.
The `extension.details` blob is removed from CloudStack after a successful return.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `vpc_id` | VPC ID. |

---

### `update-vpc-source-nat-ip`

**Called:** When the VPC source-NAT IP changes
(`updateVpcSourceNatIp` API).

**Purpose:** Update the VPC-level SNAT rule to point to the new public IP.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `vpc_id` | VPC ID. |
| `vpc_cidr` | VPC supernet CIDR. |
| `public_ip` | New source-NAT IP. |
| `public_vlan` | VLAN of the new source-NAT IP. |
| `public_gateway` | Gateway of the new source-NAT IP segment. |
| `public_cidr` | CIDR of the new source-NAT IP. |
| `source_nat` | Always `"true"`. |

---

### `assign-ip` / `release-ip`

**Called:** When a public IP is associated with or disassociated from a
network (source NAT, static NAT, PF, LB allocation).

**Purpose:**
- `assign-ip` — attach the public IP to the device; add the necessary routing
  entry so the device can receive traffic for this IP.
- `release-ip` — detach the public IP; remove routing.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN. |
| `public_ip` | The public IP being assigned or released. |
| `source_nat` | `"true"` if this is the source NAT IP. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `public_gateway` | Gateway of the public IP segment. |
| `public_cidr` | CIDR of the public IP. |
| `public_vlan` | Public VLAN tag. |
| `vpc_id` | Present for VPC tier networks. |

---

### `add-static-nat` / `delete-static-nat`

**Called:** When a static NAT rule is created or deleted
(`enableStaticNat` / `disableStaticNat` API).

**Purpose:** Configure a 1:1 bidirectional NAT mapping between a public IP
and a VM private IP.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `public_ip` | Public IP. |
| `public_cidr` | Public IP CIDR. |
| `public_vlan` | Public VLAN tag. |
| `private_ip` | VM private IP (DNAT destination). |
| `vpc_id` | Present for VPC tier networks. |

---

### `add-port-forward` / `delete-port-forward`

**Called:** When a port forwarding rule is created or deleted
(`createPortForwardingRule` / `deletePortForwardingRule` API).

**Purpose:** Configure a DNAT rule from `public-ip:public-port` to
`private-ip:private-port`.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `public_ip` | Public IP. |
| `public_cidr` | Public IP CIDR. |
| `public_vlan` | Public VLAN tag. |
| `public_port` | Port range on the public IP, for example `22` or `8080-8090`. |
| `private_ip` | VM private IP. |
| `private_port` | Destination port range on the VM. |
| `protocol` | Protocol such as `tcp` or `udp`. |
| `vpc_id` | Present for VPC tier networks. |

---

### `apply-fw-rules`

**Called:** When any firewall rule is created, deleted, or updated for the
network (`createFirewallRule`, `deleteFirewallRule`, `updateEgressFirewallRule`
APIs, and during network restart).

**Purpose:** Rebuild the entire firewall policy for the network from scratch.
CloudStack calls this with a *narrow* scope (one IP's ingress rules or egress
rules per call), but the `fw_rules` payload field always contains **all** active
rules for the network, so a full rebuild is always safe.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `fw_rules` | JSON object containing the firewall payload shown below. |
| `vpc_id` | Present for VPC tier networks. |

**`fw_rules` JSON:**

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

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `acl_rules` | JSON array of ACL rules shown below. |
| `vpc_id` | VPC ID. Always present for VPC tiers. |

**`acl_rules` JSON:**

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

### `prepare-nic` / `release-nic`

**Called:** On every NIC attach (`prepare`) and detach (`release`) regardless
of which services the extension provides.

**Purpose:**
- `prepare-nic` — set up per-NIC state before the VM boots: create the port
  binding on the device (OVN `Logical_Switch_Port`, dnsmasq entry, …).
- `release-nic` — tear down per-NIC state after the VM is destroyed: remove the
  port binding and associated metadata.

These commands fire for **all** NICs on extension-managed networks, not just
those belonging to DHCP/DNS-enabled offerings.

**`prepare-nic` payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `mac` | VM NIC MAC address. |
| `ip` | VM NIC IPv4 address. |
| `nic_ip6_address` | NIC IPv6 address, when configured. |
| `nic_ip6_gateway` | NIC IPv6 gateway, when available. |
| `nic_ip6_cidr` | NIC IPv6 CIDR, when available. |
| `nic_uuid` | NIC UUID — matches `external_ids:iface-id` written by the KVM agent for OVN port binding. |
| `default_nic` | Stringified boolean — `"false"` for secondary NICs. |
| `hostname` | VM hostname. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `extension_ip` | Extension IP. |
| `vpc_id` | Present for VPC tier networks. |

**`release-nic` payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `mac` | VM NIC MAC address. |
| `ip` | VM NIC IPv4 address. |
| `nic_ip6_address` | NIC IPv6 address, when configured. |
| `nic_ip6_gateway` | NIC IPv6 gateway, when available. |
| `nic_ip6_cidr` | NIC IPv6 CIDR, when available. |
| `nic_uuid` | NIC UUID. |
| `extension_ip` | Extension IP. |
| `vpc_id` | Present for VPC tier networks. |

---

### `add-dhcp-entry` / `remove-dhcp-entry`

**Called:** When a VM NIC is reserved (`add`) or released (`remove`) on a
network whose DHCP service is provided by this extension.

**Purpose:** Add or remove a static DHCP lease for the VM.

> **IPv6 note:** For NIC-scoped commands (`add/remove-dhcp-entry`,
> `add-dns-entry`, `save-vm-data`, `save-password`, `save-userdata`,
> `save-sshkey`, `save-hypervisor-hostname`), CloudStack includes
> `nic_ip6_address`, `nic_ip6_gateway`, and `nic_ip6_cidr` when the NIC
> has IPv6 information.

**`add-dhcp-entry` payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `mac` | VM NIC MAC address, for example `02:00:00:00:00:01`. |
| `ip` | VM assigned IP. |
| `hostname` | VM hostname. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `dns` | Comma-separated DNS server list. |
| `default_nic` | Stringified boolean indicating whether this NIC is the default NIC. |
| `domain` | Network domain suffix. |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

**`remove-dhcp-entry` payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `mac` | VM NIC MAC address. |
| `ip` | VM assigned IP. |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

---

### `config-dhcp-subnet` / `remove-dhcp-subnet`

**Called:** When a shared-network subnet is configured or removed.

**Purpose:** Configure the DHCP scope (pool, gateway, DNS) for a subnet
without tying it to a specific VM.

**`config-dhcp-subnet` payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `dns` | Comma-separated DNS server list. |
| `vlan` | Guest VLAN tag. |
| `domain` | Network domain suffix. |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

**`remove-dhcp-subnet` payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `extension_ip` | Extension IP. |
| `vpc_id` | Present for VPC tier networks. |

---

### `set-dhcp-options`

**Called:** When extra DHCP options are set on a NIC
(`updateNicExtraDhcpOption` API).

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `nic_id` | CloudStack NIC ID. |
| `options` | Compact JSON string such as `{"15":"example.com","119":"search.example.com"}`. |
| `extension_ip` | Extension IP. |
| `vpc_id` | Present for VPC tier networks. |

---

### `add-dns-entry`

**Called:** When a VM NIC is reserved on a network whose DNS service is
provided by this extension.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `ip` | VM IP. |
| `hostname` | VM hostname. |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

---

### `config-dns-subnet` / `remove-dns-subnet`

**Called:** When a DNS scope is configured or removed for a subnet.

**`config-dns-subnet` payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `dns` | Comma-separated DNS server list. |
| `vlan` | Guest VLAN tag. |
| `domain` | Network domain suffix. |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

**`remove-dns-subnet` payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `extension_ip` | Extension IP. |
| `vpc_id` | Present for VPC tier networks. |

---

### `save-vm-data`

**Called:** When a VM is deployed or updated with user data, SSH keys, or
password on a network whose UserData service is provided by this extension.

**Purpose:** Store the complete cloud-init metadata set (user-data,
meta-data/*, password) for the VM so the metadata HTTP server can serve it.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `ip` | VM IP. |
| `gateway` | Gateway of the VM NIC on this network. |
| `extension_ip` | Extension IP. |
| `vm_data` | JSON array shown below. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

**`vm_data` JSON:**

```json
[
  {"dir":"userdata",   "file":"user-data",       "content":"<plain text>"},
  {"dir":"meta-data",  "file":"instance-id",      "content":"<plain text>"},
  {"dir":"meta-data",  "file":"local-hostname",   "content":"<plain text>"},
  {"dir":"meta-data",  "file":"public-keys/0/openssh-key", "content":"<plain text>"},
  {"dir":"password",   "file":"vm_password",      "content":"<plain text>"}
]
```

Each `content` field is a plain UTF-8 string.  Write it directly to disk.

Your metadata HTTP server should serve each entry at:
`http://<extension-ip>/latest/<dir>/<file>`

---

### `save-password`

**Called:** When a password reset is requested for a VM
(`resetPasswordForVirtualMachine` API).

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `ip` | VM IP. |
| `gateway` | Gateway of the VM NIC. |
| `password` | Plain-text new password. |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

---

### `save-userdata`

**Called:** When a VM's user data is updated
(`updateVirtualMachine` with `userdata`).

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `ip` | VM IP. |
| `gateway` | Gateway of the VM NIC. |
| `userdata` | User-data as plain text. |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

---

### `save-sshkey`

**Called:** When an SSH public key is reset for a VM
(`resetSSHKeyForVirtualMachine` API).

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `ip` | VM IP. |
| `gateway` | Gateway of the VM NIC. |
| `sshkey` | SSH public key (plain text). |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

---

### `save-hypervisor-hostname`

**Called:** When a VM is deployed and UserData service is active.

**Purpose:** Store the hypervisor hostname in the metadata so VMs can identify
which host they run on (cloud-init `availability-zone` / host detection).

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `ip` | VM IP. |
| `gateway` | Gateway of the VM NIC. |
| `hypervisor_hostname` | Hypervisor node hostname. |
| `extension_ip` | Extension IP. |
| `nic_uuid` | NIC UUID, when available. |
| `vpc_id` | Present for VPC tier networks. |

---

### `apply-lb-rules`

**Called:** When a load balancer rule is created, deleted, or its members
change (`createLoadBalancerRule`, `deleteLoadBalancerRule`,
`assignToLoadBalancerRule`, `removeFromLoadBalancerRule` APIs).

**Purpose:** Configure the load balancer on the device: create/update/delete
virtual server → backend pool mappings.

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `vlan` | Guest VLAN tag. |
| `lb_rules` | JSON array of LB rules shown below. |
| `vpc_id` | Present for VPC tier networks. |

**Decoded `lb_rules` JSON:**

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

**Payload fields (`payload` object):**

| Field | Description |
|---|---|
| `network_id` | Network ID. |
| `gateway` | Guest network gateway. |
| `cidr` | Guest network CIDR. |
| `vlan` | Guest VLAN tag. |
| `extension_ip` | Extension IP. |
| `dns` | Comma-separated DNS server list. |
| `domain` | Network domain suffix. |
| `restore_data` | JSON restore payload shown below. |
| `vpc_id` | Present for VPC tier networks. |

**`restore_data` JSON:**

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
        {"dir": "userdata",  "file": "user-data",    "content": "<plain text>"},
        {"dir": "meta-data", "file": "instance-id",  "content": "<plain text>"},
        {"dir": "meta-data", "file": "local-hostname","content": "<plain text>"}
      ]
    }
  ]
}
```

Each `vm_data[].content` is a plain UTF-8 string (same encoding as in `save-vm-data`).

---

### `custom-action`

**Called:** Via the `runNetworkCustomAction` API.

**Purpose:** Allows operators to trigger ad-hoc operations on the device
without defining new CloudStack API calls.

CloudStack writes the full custom-action request to a temporary JSON payload
file and passes that file directly to the script. Unlike the other commands,
`custom-action` does **not** use the nested `{ "payload": ... }` envelope;
command-specific inputs are provided in top-level `action-params`.

It still includes the same top-level extension detail objects used elsewhere,
including `physical-network-extension-details` and `network-extension-details`.

**Top-level payload keys (network-level):**

| Key | Description |
|---|---|
| `network_id` | The CloudStack network ID. |
| `vpc_id` | Present when the network belongs to a VPC. |
| `action` | The action name passed by the operator. |
| `action-params` | JSON object with arbitrary key/value parameters. |
| `physical-network-extension-details` | Physical-network extension details JSON. |
| `network-extension-details` | Stored `extension.details` JSON for the network. |

**Top-level payload keys (VPC-level):**

| Key | Description |
|---|---|
| `vpc_id` | The CloudStack VPC ID. |
| `action` | The action name passed by the operator. |
| `action-params` | JSON object with arbitrary key/value parameters. |
| `physical-network-extension-details` | Physical-network extension details JSON. |
| `network-extension-details` | Stored `extension.details` JSON for the VPC. |

Hook scripts should parse the payload file directly.

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
| *(NIC lifecycle — all)* | `prepare-nic`, `release-nic` |
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

For networks that belong to a VPC, `vpc_id` is added to the nested command
payload. Use it to share state across all tiers of the same VPC (e.g., a
single VRF or namespace per VPC instead of per tier).

In `ensure-network-device`, use `vpc_id` (when present) as the hash key for
host selection so all tiers of a VPC always land on the same device.

VPC lifecycle commands (`implement-vpc`, `shutdown-vpc`,
`update-vpc-source-nat-ip`) are invoked at the VPC level with no
`network_id` — only `vpc_id`. Tier-level commands such as
`implement-network` and `destroy-network` still receive both
`network_id` and `vpc_id`.

To use this extension as a VPC provider:

1. Create the NetworkOffering with `useVpc=on`.
2. Create a VpcOffering with the extension as provider.

---

## Extension IP

`extension_ip` is the IP the device presents on the guest network side:

- **With SourceNat or Gateway service:** equals `gateway` (the device is the
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

COMMAND="${1:-}"
PAYLOAD_FILE="${2:-}"
TIMEOUT_SECONDS="${3:-60}"

if [[ -z "${COMMAND}" || -z "${PAYLOAD_FILE}" ]]; then
    echo "Usage: $0 <command> <payload-file> <timeout-seconds>" >&2
    exit 1
fi

root_field() {
    python3 - "$PAYLOAD_FILE" "$1" <<'PY'
import json, sys
with open(sys.argv[1], encoding='utf-8') as fh:
    data = json.load(fh)
value = data.get(sys.argv[2], "")
if isinstance(value, (dict, list)):
    print(json.dumps(value, separators=(",", ":")))
elif value is None:
    print("")
else:
    print(value)
PY
}

payload_field() {
    python3 - "$PAYLOAD_FILE" "$1" <<'PY'
import json, sys
with open(sys.argv[1], encoding='utf-8') as fh:
    data = json.load(fh)
value = data.get('payload', {}).get(sys.argv[2], "")
if isinstance(value, (dict, list)):
    print(json.dumps(value, separators=(",", ":")))
elif value is None:
    print("")
else:
    print(value)
PY
}

case "${COMMAND}" in

    ensure-network-device)
        # TODO: check that the device is reachable; select/validate host
        # Print per-network JSON to stdout (stored by CloudStack)
        printf '{"device":"%s"}\n' "$(payload_field network_id)"
        ;;

    implement-network)
        # TODO: create virtual segment (VRF / VLAN / namespace / …)
        # TODO: configure gateway IP $(payload_field extension_ip) on $(payload_field cidr)
        ;;

    shutdown-network|destroy-network)
        # TODO: tear down the virtual segment
        ;;

    implement-vpc)
        # TODO: create VPC-level namespace / VRF for vpc=$(payload_field vpc_id)
        # Optional source-NAT IP: $(payload_field public_ip)
        ;;

    shutdown-vpc)
        # TODO: remove VPC namespace / VRF for vpc=$(payload_field vpc_id)
        ;;

    update-vpc-source-nat-ip)
        # TODO: update VPC SNAT rule to use new public IP $(payload_field public_ip)
        ;;

    assign-ip)
        # TODO: attach public IP $(payload_field public_ip) to the device
        ;;

    release-ip)
        # TODO: remove public IP $(payload_field public_ip) from the device
        ;;

    add-static-nat)
        # TODO: DNAT $(payload_field public_ip) → $(payload_field private_ip)
        ;;

    delete-static-nat)
        # TODO: remove DNAT for $(payload_field public_ip)
        ;;

    add-port-forward)
        # TODO: DNAT $(payload_field public_ip):$(payload_field public_port) → $(payload_field private_ip):$(payload_field private_port)
        ;;

    delete-port-forward)
        # TODO: remove the port-forwarding DNAT rule
        ;;

    apply-fw-rules)
        FW_JSON=$(payload_field fw_rules)
        # TODO: parse $FW_JSON and apply to device
        ;;

    apply-network-acl)
        ACL_JSON=$(payload_field acl_rules)
        # TODO: parse $ACL_JSON and apply to VPC tier
        ;;

    prepare-nic)
        # TODO: create port binding mac=$(payload_field mac) ip=$(payload_field ip) nic_uuid=$(payload_field nic_uuid)
        ;;

    release-nic)
        # TODO: remove port binding mac=$(payload_field mac) ip=$(payload_field ip)
        ;;

    add-dhcp-entry)
        # TODO: add static lease mac=$(payload_field mac) ip=$(payload_field ip)
        ;;

    remove-dhcp-entry)
        # TODO: remove static lease for mac=$(payload_field mac)
        ;;

    config-dhcp-subnet|remove-dhcp-subnet) ;;

    set-dhcp-options) ;;

    add-dns-entry)
        # TODO: add A record hostname=$(payload_field hostname) ip=$(payload_field ip)
        ;;

    config-dns-subnet|remove-dns-subnet) ;;

    save-vm-data)
        VM_DATA_JSON=$(payload_field vm_data)
        # TODO: iterate entries and write to metadata store
        ;;

    save-password|save-userdata|save-sshkey|save-hypervisor-hostname) ;;

    apply-lb-rules)
        LB_JSON=$(payload_field lb_rules)
        # TODO: parse $LB_JSON and configure load balancer
        ;;

    restore-network)
        RESTORE_JSON=$(payload_field restore_data)
        # TODO: iterate vms and restore leases / DNS / metadata
        ;;

    custom-action)
        ACTION_NAME=$(root_field action)
        ACTION_PARAMS=$(root_field action-params)
        # TODO: handle $ACTION_NAME with params $ACTION_PARAMS
        echo "custom action ${ACTION_NAME} not implemented"
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
- `network-namespace-wrapper.sh` — KVM-host wrapper that implements all commands using Linux network namespaces.
