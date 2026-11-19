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

# OVN NetworkOrchestrator extension for Apache CloudStack

Puts guest networks on an [OVN](https://www.ovn.org/) logical fabric, driven entirely
by this script through `ovn-nbctl`. It needs **no changes to CloudStack** and no agent
on the hypervisor beyond `ovn-controller`.

| Service | Implemented with |
| --- | --- |
| `Connectivity`, `Dhcp` | a logical switch per network, native OVN DHCP |
| `SourceNat`, `StaticNat` | `NAT` rows on a per-network logical router |
| `PortForwarding` | `Load_Balancer` VIPs — OVN's NAT has no L4 translation |
| `Firewall` | `ACL` rows on the guest switch |
| VPC + `NetworkACL` | one logical router per VPC, a leg and ACLs per tier |
| EVPN (optional, off by default) | `external_ids` tags read by a companion `ovn-bgp-agent` |

Private gateways, VPC static routes and private-gateway ACLs are implemented as well,
but depend on a CloudStack change that is not in main — see §7.

Requires the network-extension framework
([PR #13032](https://github.com/apache/cloudstack/pull/13032), merged April 2026 —
main / 24.0.0-SNAPSHOT at the time of writing).

> **Status:** exercised against a real OVN 24.03 — northbound database, `ovn-northd`
> and a live `ovn-controller` chassis. All 25 commands run clean (`./test-with-ovn.sh`,
> 87 assertions), and a VIF carrying `external_ids:iface-id` binds to the logical
> switch port exactly as §1 claims, with real packets forwarded between VIFs and OVN
> answering DHCP itself. It has **not** been run against a real CloudStack management
> server. §9 is precise about where that boundary falls.

---

## 1. Why this works without patching CloudStack

```text
  implement-network  ──► returns  broadcast_domain_type = "Lswitch"
                                  broadcast_uri         = "lswitch:cs-net-<id>"
                                        │
                                        ▼
            CloudStack stamps that onto the network and every NIC
                                        │
  prepare-nic  ──► creates OVN Logical_Switch_Port named <nic_uuid>
                                        │
                                        ▼
   OvsVifDriver sees BroadcastDomainType.Lswitch and emits
       <virtualport type='openvswitch'>
         <parameters interfaceid='<nic_uuid>'/>     ← same uuid
       </virtualport>
                                        │
                     libvirt ──► external_ids:iface-id on the br-int port
                                        │
                     ovn-controller ──► binds it to the LSP of that name
```

The two uuids are the same one: `HypervisorGuruBase.toNicTO()` copies
`NicVO.getUuid()` into `NicTO`, under a comment that still says *"the UUID we need for
Nicira integration"*. OVN inherited NVP's port-binding convention, so the plumbing
CloudStack shipped for NSX-MH a decade ago is exactly what OVN needs.

## 2. Topology

One logical router per isolated network; one external logical switch shared by every
router on the same public VLAN.

```text
                        internet
                           │
            ┌──────────────┴──────────────┐
            │ cs-ext-<vlan>               │   shared external logical switch
            │   cs-ext-ln-<vlan>          │   localnet, via ovn-bridge-mappings
            └──────────────┬──────────────┘
                           │ cs-ext-lsp-<id>    (LSP type=router)
            ┌──────────────┴──────────────┐
            │ cs-rtr-<id>                 │   logical router
            │   cs-ext-lrp-<id>           │   distributed gateway port,
            │                             │   holds the source-NAT IP
            └──────────────┬──────────────┘
                           │ cs-lrp-<id>        (guest gateway IP)
                           │ cs-lsp-rtr-<id>    (LSP type=router)
            ┌──────────────┴──────────────┐
            │ cs-net-<id>                 │   guest logical switch
            │   <nic-uuid> …              │   one LSP per VM NIC
            └─────────────────────────────┘
```

| CloudStack concept | OVN object |
| --- | --- |
| Guest network | `Logical_Switch cs-net-<network_id>` |
| VM NIC | `Logical_Switch_Port <nic_uuid>` |
| DHCP | `DHCP_Options` tagged `cs-network-id`, attached per LSP |
| Network gateway | `Logical_Router cs-rtr-<network_id>` + `cs-lrp-<network_id>` |
| Source NAT | `NAT type=snat`, external IP on the gateway port |
| Static NAT | `NAT type=dnat_and_snat` |
| Port forwarding | `Load_Balancer`, one VIP entry per forwarded port |
| Egress/ingress firewall | `ACL` rows on the guest switch |
| VPC | one `Logical_Router cs-vpc-rtr-<vpc_id>` shared by every tier |
| VPC tier | its own `cs-net-<network_id>` switch and its own leg on that router |
| VPC network ACL | `ACL` rows, priority `30000 - rule number` |

---

## 3. Prerequisites

`ovn-host-setup.sh` does all of this, idempotently, and is the quickest way to get it
right. Everything it does is spelled out below so you can do it yourself, or express it
in whatever configuration management you already run — which, for anything beyond a
lab, you should.

```bash
# on each management server
./ovn-host-setup.sh --role management --nb-db tcp:10.0.0.1:6641

# on each KVM host
./ovn-host-setup.sh --role host --sb-db tcp:10.0.0.1:6642 --encap-ip 10.1.0.11

# on the hosts that may carry north-south traffic
./ovn-host-setup.sh --role gateway --sb-db tcp:10.0.0.1:6642 --encap-ip 10.1.0.11 \
                    --public-nic eth1 --physnet physnet1

# or drive the whole estate over ssh, one host per line ("name" or "name encap-ip")
./ovn-host-setup.sh --hosts kvm-hosts.txt --role host --sb-db tcp:10.0.0.1:6642

# check an existing host without changing anything
./ovn-host-setup.sh --verify
```

`--dry-run` prints what would change and touches nothing. Re-running is safe: it only
acts on what differs and says what the previous value was. It will not enslave the NIC
carrying the host's default route without `--force`, will not restart `cloudstack-agent`
without `--restart-agent`, merges into any existing `ovn-bridge-mappings` instead of
overwriting another provider, and backs up `agent.properties` before editing it.

It deliberately stops short of the CloudStack API: creating the extension, registering
it on the physical network and setting the traffic label are §4, not host setup.

**OVN control plane** — an OVN cluster reachable from every management server.
Three `ovsdb-server` nodes in RAFT for NB and SB is the normal shape. The NB DB must
accept connections, e.g. `ovn-nbctl set-connection ptcp:6641`. Use SSL outside a lab.

**Management servers** — `ovn-nbctl` (package `ovn-common` / `ovn-central`) and `jq`
on **every** management server. The script runs there, not on the hosts.

**KVM hosts** — `openvswitch-switch` + `ovn-host`:

```bash
ovs-vsctl set open_vswitch . \
  external_ids:ovn-remote="tcp:10.0.0.1:6642,tcp:10.0.0.2:6642,tcp:10.0.0.3:6642" \
  external_ids:ovn-encap-type=geneve \
  external_ids:ovn-encap-ip=<this host's overlay IP> \
  external_ids:system-id=<stable host id>
systemctl enable --now ovn-controller
```

**Gateway chassis** — the hosts that may carry north-south traffic need a bridge
mapping from the provider network name to the OVS bridge holding the public NIC:

```bash
ovs-vsctl add-br br-ex
ovs-vsctl add-port br-ex eth1          # the public NIC
ovs-vsctl set open_vswitch . external_ids:ovn-bridge-mappings=physnet1:br-ex
```

`physnet1` must match `ovn_public_physnet` (§4.4). Note the caveat in §7 — the gateway
port lives on **one** chassis at a time.

**CloudStack agent** — `agent.properties` on each KVM host must select the OVS VIF
driver, or `plug()` will never take the `Lswitch` branch:

```properties
libvirt.vif.driver=com.cloud.hypervisor.kvm.resource.OvsVifDriver
network.bridge.type=openvswitch
```

**Physical network traffic label** — set the guest traffic label to `br-int`.
`OvsVifDriver.getPifs()` registers every bridge from `ovs-vsctl list-br` under its own
name, so `br-int` resolves and VIFs land on the integration bridge.

---

## 4. Install and register

### 4.1 Install

On every management server:

```bash
install -d -m 0755 /usr/share/cloudstack-management/extensions/ovn
install -m 0755 ovn.sh /usr/share/cloudstack-management/extensions/ovn/ovn.sh
install -d -o cloud -g cloud -m 0755 /var/log/cloudstack/management/extensions
```

Directory and script name are derived from the extension name:
`Extension.getDirectoryName("OVN")` lowercases it, so the relative path CloudStack
resolves is `ovn/ovn.sh` — note the lowercase directory, even though this source tree
ships the file under `extensions/OVN/` to match its siblings. CloudStack checksums the
file and marks the extension **Not Ready** if it differs between management servers, so
deploy it with config management.

### 4.2 Create the extension

```bash
cmk create extension name=OVN type=NetworkOrchestrator \
  description='OVN logical networking' \
  details[0].key=network.services \
  details[0].value=Connectivity,Dhcp,SourceNat,StaticNat,PortForwarding,Firewall \
  details[1].key=network.allocate.extension.ip \
  details[1].value=false
```

`network.services` decides which services the synthesised provider advertises. Keep it
to what the script implements — anything else routes into `cmd_unsupported`, which
logs and returns success rather than wedging the deployment.

`network.allocate.extension.ip=false` stops CloudStack reserving a placeholder IP from
the subnet pool. This is the exact case the framework documents for OVN: *"the
extension handles its own data-plane addressing … (e.g. OVN, which programs port
bindings in-fabric)"*.

### 4.3 Register on the physical network

```bash
cmk register extension id=<extension-uuid> \
  resourcetype=PhysicalNetwork resourceid=<physical-network-uuid> \
  details[0].key=network.isolation.method  details[0].value=NetworkExtension \
  details[1].key=ovn_nb_db                 details[1].value='tcp:10.0.0.1:6641,tcp:10.0.0.2:6641,tcp:10.0.0.3:6641' \
  details[2].key=ovn_public_physnet        details[2].value='physnet1' \
  details[3].key=ovn_gateway_chassis       details[3].value='kvm01,kvm02' \
  details[4].key=ovn_dns_servers           details[4].value='8.8.8.8,8.8.4.4' \
  details[5].key=ovn_mtu                   details[5].value='1442'
```

`network.isolation.method=NetworkExtension` is **required**. It is what makes
`NetworkExtensionGuestNetworkGuru.canHandle()` claim the network; without it the guru
never engages and the broadcast URI the script returns is silently ignored.

`ovn_gateway_chassis` is a comma-separated list of chassis `system-id`s in priority
order. Without it the distributed gateway port is never scheduled and north-south
traffic will not work — the script logs a warning but does not fail.

Then enable the provider:

```bash
cmk list networkserviceproviders physicalnetworkid=<physical-network-uuid> name=OVN
cmk update networkserviceprovider id=<nsp-uuid> state=Enabled
```

### 4.4 `physical-network-extension-details` reference

| Key | Required | Default | Meaning |
| --- | --- | --- | --- |
| `ovn_nb_db` | **yes** | — | NB DB target(s), `ovn-nbctl --db` syntax. Comma-separate a RAFT cluster. |
| `ovn_public_physnet` | for L3 | `physnet1` | Provider name in each host's `ovn-bridge-mappings` |
| `ovn_gateway_chassis` | for L3 | — | Chassis `system-id`s, highest priority first |
| `ovn_nb_private_key` | no | — | SSL client key (`--private-key`) |
| `ovn_nb_certificate` | no | — | SSL client cert (`--certificate`) |
| `ovn_nb_ca_cert` | no | — | SSL CA cert (`--ca-cert`) |
| `ovn_dns_servers` | no | the gateway | DHCP `dns_server` option |
| `ovn_mtu` | no | unset | DHCP `mtu` option |
| `ovn_dhcp_lease_time` | no | `86400` | DHCP `lease_time` option |
| `ovn_port_security` | no | `true` | Set `port_security` on each LSP |
| `ovn_pf_max_ports` | no | `128` | Max VIPs one port-forward rule may expand into |
| `ovn_private_physnet` | no | `ovn_public_physnet` | Provider name for VPC private gateways |
| `ovn_evpn_enabled` | no | `false` | Tag switches for `ovn-bgp-agent` |
| `ovn_evpn_type` | no | `l3` | `l3` (route the subnet into a VRF) or `l2` (stretch the segment) |
| `ovn_evpn_vni_base` | no | `0` | VNI = base + VPC id (L3) or + network id (L2) |
| `ovn_evpn_key_prefix` | no | `neutron_bgpvpn` | `external_ids` key prefix the agent reads |

### 4.5 Offering and network

```bash
cmk create networkoffering name=OVN-Isolated displaytext='OVN isolated network' \
  guestiptype=Isolated traffictype=Guest \
  supportedservices=Connectivity,Dhcp,SourceNat,StaticNat,PortForwarding,Firewall \
  serviceproviderlist[0].service=Connectivity   serviceproviderlist[0].provider=OVN \
  serviceproviderlist[1].service=Dhcp           serviceproviderlist[1].provider=OVN \
  serviceproviderlist[2].service=SourceNat      serviceproviderlist[2].provider=OVN \
  serviceproviderlist[3].service=StaticNat      serviceproviderlist[3].provider=OVN \
  serviceproviderlist[4].service=PortForwarding serviceproviderlist[4].provider=OVN \
  serviceproviderlist[5].service=Firewall       serviceproviderlist[5].provider=OVN \
  specifyvlan=false egressdefaultpolicy=true state=Enabled

cmk create network name=ovn-test displaytext='OVN test network' \
  networkofferingid=<offering-uuid> zoneid=<zone-uuid> \
  gateway=10.1.1.1 netmask=255.255.255.0 startip=10.1.1.10 endip=10.1.1.200
```

`egressdefaultpolicy` maps straight onto the ACL base policy: `true` → permissive base
with explicit egress rules as denies; `false` → base drop with explicit rules as allows.

---

## 5. Verify

```bash
NB='--db=tcp:10.0.0.1:6641'

ovn-nbctl $NB show                                  # whole topology
ovn-nbctl $NB lsp-list cs-net-<network-id>          # a port per NIC
ovn-nbctl $NB lr-nat-list cs-rtr-<network-id>       # snat + dnat_and_snat
ovn-nbctl $NB lr-lb-list cs-rtr-<network-id>        # port forwards
ovn-nbctl $NB acl-list cs-net-<network-id>          # firewall
ovn-nbctl $NB lrp-get-gateway-chassis cs-ext-lrp-<network-id>

# On the KVM host
ovs-vsctl --columns=name,external_ids list interface | grep iface-id
ovn-sbctl --db=tcp:10.0.0.1:6642 show               # port bindings per chassis
```

The decisive check for L2 is that `external_ids:iface-id` on the host equals a
`Logical_Switch_Port.name` in the NB DB, with `ovn-sbctl show` listing that port under
the chassis the VM landed on. For L3, check that `lrp-get-gateway-chassis` returns a
chassis and that a VM can reach the internet through the source-NAT IP.

Script activity, including every `ovn-nbctl` argv and any stderr it produced, goes to
`/var/log/cloudstack/management/extensions/ovn.log`.

---

## 6. Design notes

**Naming is derived from `network_id`.** The payload carries `network_id` (the internal
long), **not** `network_uuid`, so that is the only stable network identity available.
Logical switch ports are named with `nic_uuid` — that name is load-bearing, not
cosmetic: it must equal `external_ids:iface-id`.

**Port forwarding uses load balancers, not NAT.** OVN's NAT table has no L4 port
translation. The standard answer — also what OpenStack does for floating-IP port
forwarding — is a `Load_Balancer` VIP with a single backend. OVN has no port-range
DNAT either, so a range expands into one VIP per port, capped by `ovn_pf_max_ports`
(default 128). Beyond the cap the script fails with an explicit message rather than
quietly creating thousands of entries.

**Ingress firewall rules are resolved through NAT.** CloudStack gives an ingress rule a
`publicIp`, but ACLs on the guest switch see post-DNAT traffic. The script reads the
router's `dnat_and_snat` entries back to find the private IP behind that public IP and
targets the rule at that VM. If no mapping exists, it widens the rule to the guest CIDR
and says so in the log.

**Firewall state is rebuilt, not diffed.** CloudStack sends the complete active rule
set on every `apply-fw-rules` call, so the script deletes every ACL it owns (tagged
`external_ids:cs-network-id`) and recreates them. DHCP is allowed at a priority above
everything so a restrictive egress policy can never break addressing.

**`shutdown-network` is deliberately a no-op.** CloudStack shuts a network down while
its NICs may still exist, and there is no guarantee `prepare-nic` runs again for
already-running VMs on restart. Deleting the logical switch there would blackhole them.
An idle logical switch costs nothing in OVN, so cleanup happens only in
`destroy-network`.

**NAT rules fail loudly if L3 is not up.** CloudStack applies the source-NAT IP during
`implement()`, so a static NAT arriving before the router exists is a real ordering
problem. Half-configuring it would be worse than stopping.

**Everything is idempotent.** `--may-exist` / `--if-exists` throughout, multi-command
`ovn-nbctl` transactions so related changes land atomically, and `prepare-nic`
recreates the logical switch if it is missing.

**stdout discipline.** The management server merges the script's stderr into stdout
(`ProcessBuilder.redirectErrorStream(true)`) and JSON-parses the whole thing, so a
single stray log line breaks `implement-network`. The script writes its result JSON to
a saved fd 3, never fd 1 — a call site that redirects stdout cannot swallow it — and
captures `ovn-nbctl`'s stderr into the log file.

---

## 7. Known gaps and caveats

**Gateway concentration.** OVN NAT and load balancing require exactly one distributed
gateway port per router. North-south traffic for a network is therefore pinned to one
chassis; east-west routing stays distributed. `ovn_gateway_chassis` sets the failover
order, but a chassis loss affects every network homed there. Capacity-plan and
load-test this before production — it is the single biggest operational difference from
a per-network virtual router.

When you do test failover, note that **BFD is answered by `ovs-vswitchd`, not by
`ovn-controller`**. Stopping `ovn-controller` on the active gateway chassis does not
fail anything over: the peer still sees BFD up and correctly leaves the port where it
is. Take down the switch, the host, or the tunnel path instead. This cost real time to
work out and is easy to mistake for the feature being broken.

**No VPN.** OVN has no IPsec site-to-site or remote-access VPN. Do not put `Vpn` in the
offering.

**No user data or metadata.** OVN has no metadata service. Use **ConfigDrive** as the
`UserData` provider; it is a first-class CloudStack provider and keeps the VR out of
the path entirely.

**Load balancing is not wired up.** OVN's load balancer is L4 only, with IPv4-only
health checks and none of the stickiness or SSL-offload capabilities a CloudStack LB
offering advertises. `Lb` is deliberately absent from `network.services`.

**IPv4 only.** `network_ip6_gateway` / `network_ip6_cidr` arrive in the payload but no
DHCPv6 options, RA config or IPv6 ACLs are created.

**Shared networks are untested.** The LS/LSP path should work, but
`NetworkExtensionGuestNetworkGuru` delegates shared-network allocation to
`DirectNetworkGuru` and that combination has not been exercised.

**Security groups are out of scope.** CloudStack implements them with host-side
iptables/ebtables on KVM; OVN would use `Port_Group` + `ACL`. Running both is at best
redundant. Use isolated networks only.

**No reconciliation.** If `release-nic` is missed on some VM-destroy path, the LSP
leaks. A sweeper comparing NB ports tagged `external_ids:cs-network-id` against
CloudStack's NIC table is still to be written.

**Private gateways, VPC static routes and private-gateway ACLs need a CloudStack
patch.** `NetworkExtensionElement.createPrivateGateway()`,
`deletePrivateGateway()`, `applyStaticRoutes()` and `applyACLItemsToPrivateGw()`
throw `UnsupportedOperationException` in stock CloudStack, so no network
extension can offer them, whatever its backend.

The script implements the four verbs anyway — `create-private-gateway`,
`delete-private-gateway`, `apply-static-routes`, `apply-private-gw-acl` — against a
companion change that wires those methods up the same way as their neighbours, adding
the four script commands and a `NetworkExtensionElementTest` covering them. **That
change is not in main and has not been submitted upstream.** On stock CloudStack these
four verbs are simply never dispatched, so the script behaves exactly as it does
without them — nothing here depends on the patch being present.

Private-gateway ACLs are scoped to the gateway's own subnet, derived from its
address and netmask. OVN masks the host bits, so a match written as
`ip4.src == 172.16.0.5/24` means the enclosing `/24`.

---

## 8. EVPN

Optional, off by default. OVN keeps using Geneve between chassis; EVPN is about
exposing those networks into a physical BGP/EVPN fabric.

### How it works

The translation is done out of band by **`ovn-bgp-agent`** (`nb_ovn_bgp_driver`,
`exposing_method = vrf`) running on each host. It watches the OVN NB DB and drives
FRR plus Linux VRF and VXLAN devices. The CMS's entire job is to set two
`external_ids` on a logical switch, so that is all this script does:

```text
external_ids:neutron_bgpvpn\:type = l3 | l2
external_ids:neutron_bgpvpn\:vni  = <vni>
```

Those key names are fixed by the agent. A
[Neutron RFE](https://bugs.launchpad.net/neutron/+bug/2067401) notes that writing
them with `ovn-nbctl` is currently the only supported way to set them — which is
exactly what an extension is able to do. `ovn_evpn_key_prefix` exists so a rename
upstream does not require editing the script.

### L3 vs L2

| | `ovn_evpn_type=l3` | `ovn_evpn_type=l2` |
| --- | --- | --- |
| Tagged object | the provider switch `cs-ext-<vlan>-vni<N>` | the guest switch `cs-net-<id>` |
| VNI scope | one per VPC (one per network when standalone) | one per logical switch |
| Effect | the tenant's subnets are routed into a VRF in the fabric | the L2 segment is stretched into the fabric |

L3 is the default and is what the OpenStack example documents. Because
`ovn-bgp-agent` maps **one provider network to one VNI**, enabling L3 EVPN changes
the external switch from being shared per public VLAN to being **scoped per VRF** —
`cs-ext-101` becomes `cs-ext-101-vni100007`. That is what gives each VPC its own
routing domain in the fabric rather than dumping every tenant into one VRF. With
EVPN off, naming is unchanged.

### Setup

```bash
cmk update registered extension id=<ext-uuid> resourcetype=PhysicalNetwork \
  resourceid=<physnet-uuid> \
  details[0].key=ovn_evpn_enabled   details[0].value=true \
  details[1].key=ovn_evpn_type      details[1].value=l3 \
  details[2].key=ovn_evpn_vni_base  details[2].value=100000
```

VNIs are then `100000 + <vpc id>` (L3) or `100000 + <network id>` (L2). Reserve
that range in the fabric. On each host, `ovn-bgp-agent` and FRR must be configured
per the [OpenStack example](https://docs.openstack.org/ovn-bgp-agent/latest/examples/nb_evpn_vrf.html);
this script does not touch FRR.

### Relationship to CloudStack's existing VXLAN/EVPN support

CloudStack already ships a [VXLAN plugin with EVPN](http://docs.cloudstack.apache.org/en/latest/plugins/vxlan.html)
(`scripts/vm/network/vnet/modifyvxlan-evpn.sh` plus FRR on each host). **It is not
reusable here and the two cannot run on the same host**, because that path requires
`BridgeVifDriver` and Linux bridges, while this extension requires `OvsVifDriver`
and `br-int`. `libvirt.vif.driver` is a single per-host setting. The VXLAN plugin
also owns its own VNI allocation through the physical network's VNI range, whereas
an extension network uses `Lswitch` isolation and is allocated no VNI at all —
hence `ovn_evpn_vni_base`.

It is still worth reading as prior art: it shows the FRR configuration shape
(BGP unnumbered to ToRs, `advertise-all-vni`, per-hypervisor ASN, /32 loopback
VTEP) that the underlay needs either way.

### Caveats

- **Untested.** The tagging is verified against a stub; no `ovn-bgp-agent` has
  seen it.
- **VNI derivation is arithmetic, not allocation.** Nothing checks the VNI is free
  in the fabric or that two zones do not overlap. Pick disjoint bases per zone.
- **No API surface.** VNIs cannot be chosen per network; CloudStack passes no such
  field and the extension framework has nowhere to put one short of a custom action.
- **L3 EVPN changes external switch naming**, so turning it on for an existing
  deployment strands the old `cs-ext-<vlan>` switch. Enable it before building
  networks, or clean up by hand.
- The OpenStack docs' limitations apply: one flat provider network per VNI, no
  reusing a VLAN id within a VNI, and tenant networks behind a gateway cannot be
  split into separate VRFs without address-scope discipline.

---

## 9. Testing status

### Against a real OVN

`test-with-ovn.sh` drives the script through a full isolated-network and VPC lifecycle
against a live OVN 24.03 northbound database, asserts on the resulting NB objects, and
then checks `ovn-northd`'s log — the NB DB will happily store a configuration that
northd cannot translate, so that last check is the one that proves anything. 87
assertions, all passing, 0 northd errors, 223 logical flows generated. To reproduce:

```bash
# in a container or VM with ovn-central installed
/usr/share/ovn/scripts/ovn-ctl start_northd
./test-with-ovn.sh
```

This found two real bugs that the stub could not, both the same mistake:

**`ACL` and `Logical_Router_Static_Route` are not root tables.** The script created a
row and then attached it to its parent in a second `ovn-nbctl` call. OVSDB garbage
collects a row in a non-root table that nothing references by the end of the
transaction that created it, so the row was gone before the second call ran:

```text
ovn-nbctl: applying "create" command to table ACL without --id option will have no effect
transaction error: ... references nonexistent row ... "error":"referential integrity violation"
```

That broke **every firewall rule and every network ACL**, and every VPC static route —
most of the `Firewall` and `NetworkACL` services. Both now create and attach in a
single transaction using `--id=@name`. `DHCP_Options` and `Load_Balancer` are root
tables, which is why the same pattern was correct for them and the fault was invisible
until a real database enforced it.

**What this does not cover:** no CloudStack management server, no hypervisor, no
`ovn-controller`, no VM and no traffic. It proves the script speaks OVSDB correctly and
that northd accepts what it writes. It proves nothing about port binding to a real VIF,
gateway-chassis failover, or whether packets actually flow. One observation from the
teardown path: deleting a private gateway leaves any static route whose next hop sat
behind it unresolvable, and northd says so — harmless here because CloudStack revokes
those routes itself, but worth knowing.

### Against payloads CloudStack actually produced

Everything above feeds the script payloads written by hand. That leaves one seam
untested: the Java element writes the JSON, this script reads it, and each side's
tests are checked against a fixture written by the same person. Both suites would
still pass if a key name drifted.

`test-with-ovn.sh --payloads <dir>` closes it. CloudStack's
`NetworkExtensionElementPayloadContractTest` writes the payloads
`NetworkExtensionElement` really produces to
`framework/extensions/target/extension-payloads/`; this mode replays those files
against a live OVN, substituting only `physical-network-extension-details`, since
that describes the backend rather than the contract. Everything under `.payload` is
the element's own output, untouched.

```bash
# in the CloudStack tree
mvn -pl framework/extensions test -Dtest=NetworkExtensionElementPayloadContractTest
# then, where OVN is running
./test-with-ovn.sh --payloads /path/to/framework/extensions/target/extension-payloads
```

Six commands are captured and replayed as a whole VPC lifecycle — 21 assertions, all
passing. The element's `implement-vpc` payload creates the router (with no source NAT
IP, the state a VPC is first implemented in); its `apply-network-acl` payload produces
the tier's ACLs, with rule 10 ahead of rule 20 at OVN priority 29990 and the source
CIDRs intact; its `create-private-gateway` payload produces a router port carrying its
address; its `apply-static-routes` payload installs routes `ovn-northd` resolves; its
`apply-private-gw-acl` payload builds the gateway ACLs; and its
`delete-private-gateway` payload removes the port.

Two steps in that run are still harness-built — the tier's `implement-network`, and
nothing captures `update-vpc-source-nat-ip` — so the replay is not yet the complete
VPC path. The four private gateway and static route commands need the companion
CloudStack change described in §7: they do not exist in stock CloudStack, so there is
nothing to capture without it.

### Across two chassis

A second chassis was added and the two were put on a real Geneve tunnel — BFD up in
both directions, `forwarding=true` — so this is genuine host-to-host overlay rather
than one machine talking to itself.

Confirmed:

- `ovn_gateway_chassis=chassis-a,chassis-b` produces `Gateway_Chassis` rows at
  priority 100 and 90 in the order given, and `ovn-northd` turns them into an
  `HA_Chassis_Group` for the port.
- The highest-priority chassis claims the distributed gateway port:
  `cr-cs-ext-lrp-1000 -> chassis-a`.
- When that chassis is taken down, the survivor's BFD session reports
  `Control Detection Time Expired` and `state=down`, which is the signal OVN acts on.

**Automatic takeover was not demonstrated**, and the reason is the lab rather than the
configuration: the backup's `ovn-controller` never finishes its startup handshake with
`ovs-vswitchd` in a container, logging `OVS frw cleared, restarting flow-restore-wait
sequence` in a loop, and a controller stuck in `flow-restore-wait` claims nothing. The
pieces OVN needs are all present and correct in the databases; what is missing is a
backup chassis healthy enough to act on them. Proving failover end to end needs two
real hosts.

### Under concurrency

```bash
./test-with-ovn.sh --concurrency 12
```

Fires N callers at the same network at once — `implement-network` and
`prepare-nic` together, which is what a network coming up while instances are
already deploying looks like — then checks that exactly one of each per-network
singleton exists and that every port shares one `DHCP_Options` row.

This found the worst bug so far. **Only `Logical_Switch_Port` and
`Logical_Router_Port` have a name index in the NB schema.** `Logical_Switch`,
`Logical_Router`, `DHCP_Options`, `Load_Balancer` and `ACL` do not, so OVSDB will
not refuse a duplicate, and `ovn-nbctl --may-exist` is a read followed by a write
with nothing in between to stop two callers both reading "absent". Eight parallel
`ovn-nbctl --may-exist ls-add` calls produce three switches of the same name. That
is not this script misusing the tool; it is what the tool offers.

Left alone it produced up to eight `DHCP_Options` rows for one network with
instances bound across two or three of them, which is quiet rather than loud: a
later `config-dhcp-subnet` updates one row, and the instances on the others keep
being handed stale DNS, MTU or lease settings forever.

The fix has two halves, because they cover different failure domains:

- **A per-command file lock**, keyed on the network — or on the VPC, since a
  VPC's tiers share one router and would otherwise both create it. This makes a
  single management server correct, which is the ordinary case.
- **Deterministic collapse of duplicate `DHCP_Options` rows**, because a second
  management server cannot see the first one's lock file. Every racer picks the
  same winner and re-points ports onto it. `dhcpv4_options` is a *weak*
  reference, so a port pointing at a row that is destroyed is silently left with
  no DHCP at all — ports are re-pointed before the row goes, and a sweep repairs
  any that a concurrent collapse orphaned anyway.

**What is still not safe, and cannot be made safe here:** two management servers
creating the same logical switch at the same instant can still produce two
switches of that name, which splits the network in half. The script now says so
in its log rather than leaving it to be discovered. A real fix needs a
compare-and-swap, which means OVSDB `wait` operations and a native client —
precisely the argument for building this as a Java plugin instead. In practice
CloudStack serialises most per-network work with its own locks, so this is
defence in depth rather than a routine hazard.

### Against a real chassis

`ovn-controller` and `ovs-vswitchd` (userspace datapath) were then brought up in the
same container and registered as chassis `chassis-a`, which is what turns the NB
configuration into forwarding state.

**The port binding convention §1 rests on holds.** Two OVS internal ports were created
on `br-int` carrying nothing but `external_ids:iface-id=<the NIC uuid>` — precisely what
libvirt writes when `OvsVifDriver` emits `<parameters interfaceid='...'/>`.
`ovn-controller` bound both to the logical switch ports the script had created, and the
NB `Logical_Switch_Port`s went `up`:

```text
  nic-bind-0001    Port_Binding.chassis=chassis-a    Logical_Switch_Port up=up
  nic-bind-0002    Port_Binding.chassis=chassis-a    Logical_Switch_Port up=up
```

`ovn_gateway_chassis` works too: the controller claimed the distributed gateway port
on its own — `cr-cs-vpc-ext-lrp-8: Claiming 02:03:00:00:00:08 192.0.2.30/24`.

`ovn-trace` then confirms the pipelines OVN built from the script's configuration:

- **VM to VM** on a tier is delivered to the right port, through port security.
- **DHCP** is answered by OVN itself, with every value traceable to the script's
  input — `put_dhcp_opts(offerip = 10.5.1.10, dns_server = {8.8.8.8},
  lease_time = 86400, mtu = 1442, netmask = 255.255.255.0, router = 10.5.1.1,
  server_id = 10.5.1.1)`. `offerip` comes from `prepare-nic`, `dns_server` and `mtu`
  from the physical network details, the rest from the network's gateway and CIDR.
- **VM to the internet** reaches `ct_snat(192.0.2.60)` on the gateway port and leaves
  through the localnet port into `physnet1`.
- **Port forwarding** is programmed as
  `ct_lb_mark(backends=10.5.1.10:80)` at `lr_in_dnat`, and the router answers ARP for
  the VIP. (`ovn-trace` itself shows a plain `ct_dnat` here because it cannot model
  conntrack state; the generated logical flow is the thing to read.)

Traces describe intent; the flow counters show what the switch actually did. Packets
built by hand and injected with `ovs-ofctl packet-out` were forwarded by the installed
datapath:

- **Three ICMP echo requests** sent in on the first VIF's OpenFlow port came out the
  second's — `n_packets=0 → 3`, `n_bytes=183`, which is 3 × the 61-byte packet.
- **Two DHCP DISCOVERs** (292 bytes each) produced two **328-byte** packets delivered
  back to the client port, `n_bytes=656`. The other VIF's counter never moved, so these
  are OVN's own DHCP OFFERs rather than the broadcast being flooded — the responder
  runs inside `ovn-controller`, with no DHCP server anywhere in the picture.

Final state: 326 logical flows, 898 OpenFlow rules on `br-int`, 4 bound port bindings,
and **zero errors from both `ovn-northd` and `ovn-controller`**.

**Still not covered.** Packets were injected at the OpenFlow layer rather than sent by
a guest OS through a libvirt-managed VIF, so nothing here exercises CloudStack's agent
or `OvsVifDriver` itself — only the convention they rely on. There is no CloudStack
management server, no second chassis (so no gateway-chassis failover), and no
`ovn-bgp-agent` (so EVPN remains tag-only).

### Against a stub

`ovn.sh` was also exercised against a stateful stub `ovn-nbctl` that records every
invocation and models routers, NAT entries, load balancers and ACLs. Covered:

- all 25 commands the script dispatches, each returning exactly one JSON object
- source-NAT assignment end to end: router, gateway port, localnet, default route, SNAT
- static NAT add/delete, including re-pointing an existing external IP
- port forwarding: single port, range expansion with correct port pairing, over-cap
  range rejection, unsupported protocol, mismatched range sizes
- firewall ACL generation under both egress default policies, with source/dest CIDR
  sets, port ranges, ICMP type/code, and public-IP-to-private-IP resolution
- teardown: partial and final VIP removal with load-balancer cleanup, source-NAT
  release, full network destroy
- failure modes: missing `ovn_nb_db`, missing required payload fields, malformed
  payload, unwritable log directory, `ovn-nbctl` failing, `ovn-nbctl` writing noise to
  both stdout and stderr, and NAT arriving before the router exists
- VPC: one shared router across two tiers with distinct legs and MACs, network ACL
  priority inversion, tier teardown leaving the router intact, `shutdown-vpc`
  removing it last, and source-NAT IP re-pointing
- EVPN: L3 tagging on a per-VRF provider switch, L2 tagging per guest switch,
  correct VNI scoping in both modes, and a clean no-op when disabled

Five bugs were found and fixed this way, most of which would have been quiet
misconfigurations rather than visible errors:

1. `jq '.default_egress_allow // true'` — the alternative operator treats a literal
   `false` as empty, so a **restrictive egress policy was being applied as permissive**.
2. `IFS=$'\t'` with `read` — tab is an IFS whitespace character, so bash collapsed runs
   of tabs and every field after an empty one shifted left. Source CIDRs and public IPs
   were silently dropped from firewall rules. Now joined with `\u001f`.
3. `emit_failure` wrote to stdout, which call sites redirect to `/dev/null` — failures
   returned an empty body. Now written to a saved fd 3.
4. The gateway leg was only created by `assign-ip`, so **a VPC tier that never
   received a public IP of its own had no gateway at all** — DHCP advertised a
   router address that nothing answered. `implement-network` now attaches it.
5. L2 EVPN derived its VNI from the VPC id, which would have given every tier of a
   VPC the same L2 VNI and merged them into one broadcast domain in the fabric.
   L2 VNIs are now always per logical switch.

The stub proves sequencing and I/O discipline. It cannot prove that `ovn-nbctl`
accepts the transaction syntax — as the two bugs above show, it accepted syntax a real
database rejects — and it says nothing about whether traffic flows. A CloudStack
management server driving a real zone, with VMs on real hypervisors, is still the next
step.
