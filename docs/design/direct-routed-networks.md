# Direct Routed Networks

**Status:** design agreed — open items are implementation and verification only
**Branch:** `direct-routed-network`
**Author:** Wido den Hollander
**Last updated:** 2026-09-04

> **Revision 2026-09-04.** The isolation model changed: direct routed networks now live on a
> **dedicated physical network with isolation method `ROUTED`**, and every network carries a
> broadcast domain of the new type **`routed://<id>`**, whose id names the network's bridge
> (`brdr-<id>`). This reverses two earlier decisions — "no isolation method" and "no broadcast
> domain" — recorded with rationale in §6.7 and §9.2.1 and in the decision log (§16).

---

## 1. Summary

A new guest network type in which the **hypervisor performs L3 routing for the guest**. There is
no Virtual Router, no NAT, and **no DHCP** — the defining characteristic of this network type.

The operator creates a network and adds a subnet to it, as for a Shared network. CloudStack then
allocates **individual addresses** out of that subnet to guest NICs, and hands each NIC:

* its IPv4 address as a **/32** and its IPv6 address as a **/128**
* a **shared, host-independent gateway**: `169.254.0.1` (IPv4) and `fe80::1` (IPv6), configured on
  the network's bridge on every hypervisor
* the whole configuration via **ConfigDrive / cloud-init** — mandatory, since without DHCP or RA
  there is no other way for the guest to learn its address

The hypervisor's CloudStack agent installs, per guest address, a host route and a static neighbour
entry on the guest bridge, binding the address to that guest's MAC. This reuses the existing
`modifymacip.sh` hook unchanged (§9.1). A routing daemon on the host advertises those addresses to
the fabric. **The routing daemon is out of scope for this feature** — see §10.

Direct routed networks are the network operator's choice, expressed in the zone design: they live
on a **dedicated physical network whose isolation method is `ROUTED`**. Each network carries a
broadcast domain of the new type **`routed://<id>`** — for example `routed://5828` — and that id
names the network's bridge on every hypervisor: `brdr-5828`. The id is not an encapsulation and
never appears on the wire; it is the stable, operator-controlled handle that ties a network to
host-side routing policy. The operator either picks it at network creation or lets CloudStack
allocate one from the range configured on the physical network (§6.7, §9.2).

## 2. Motivation

Compared with what CloudStack offers today:

| | Shared | Isolated (NATTED) | Isolated (ROUTED, 4.20+) | **Direct Routed** |
|---|---|---|---|---|
| VR in data path | for DHCP/DNS | yes | yes | **no** |
| DHCP | yes | yes | yes | **no** |
| VM gets routable IP | yes | no (NAT) | yes | yes |
| Guest netmask | subnet mask | subnet mask | subnet mask | **/32, /128** |
| Shared broadcast domain | yes | yes | yes | **no** |
| Guest-to-guest | switched | switched | switched | **routed by host** |
| Isolation ID | VLAN/VXLAN | VLAN/VXLAN | VLAN/VXLAN | **routed id — a label, no encapsulation** |

What this closes:

* **No VR in the data path.** `ROUTED` networks (4.20) removed NAT but kept a VR in the path and an
  L2 segment per network. Throughput, failover, and per-network VR footprint remain concerns.
* **No shared broadcast domain between networks.** Each network gets its own uplink-less bridge
  (§9.2), so there is no L2 path of any kind between networks — no ARP spoofing, no rogue DHCP
  server, no rogue RA reaching another tenant. Isolation is topological, so it does not depend on a
  rule set being correct, and an administrator can turn security groups off for a network without
  weakening it. Within a single network guests still share a bridge; that residual exposure is
  intra-tenant and is documented in §12.3.
* **No encapsulation at all.** No VLAN on the wire, no VXLAN, no tunnel. The routed id in
  `routed://<id>` is a **label, not a fabric resource**: it exists only to name the per-network
  bridge (`brdr-<id>`), consumes nothing on the switches, and is either chosen by the operator or
  allocated from a range the operator configures on the `ROUTED` physical network (§6.7, §9.2.1).
  Guest network count is bounded only by that range, and each network still gets a real L2 boundary.
* **Per-network routing policy on the hypervisor.** Each network is a distinct, named L3 interface
  (`brdr-5828`), so the operator can apply different route-maps, redistribution filters, policy
  routing or QoS per network in the host's own configuration. CloudStack does not need to know or
  care; it is entirely a local network design decision (§9.2). Because the operator can pick the
  routed id at network creation, the interface name is **plannable in advance** — host policy can
  exist before the network does (§6.7.2).
* **IP mobility.** Because the gateway is identical on every hypervisor, the guest's network
  configuration is entirely host-independent. A VM can start, stop, and migrate anywhere in the
  routing domain with no reconfiguration — its address follows it as an advertised route.
* **Fits L3-to-the-host fabrics.** Operators already running BGP or OSPF on the hypervisor get an
  addressing model that matches their fabric instead of fighting it with stretched VLANs.
* **Denser subnet use.** No broadcast domain means no network or broadcast address to reserve —
  every address in the subnet is usable (§6.3.1).

## 3. Non-goals

* Not a replacement for `NetworkMode.ROUTED` + BGP-per-network (4.20). That stays.
* No NAT, source NAT, static NAT, port forwarding, LB, or VPN in this network type.
* No VR at all — not even for DHCP/DNS/UserData.
* No DHCP, DHCPv6, or SLAAC/RA for guest addressing.
* No support for guests that cannot consume ConfigDrive (§6.5).
* **No routing-daemon management.** CloudStack does not install, configure, or monitor FRR/BIRD
  (§10).
* No per-tenant VRFs, and therefore no overlapping subnets between networks (§6.3.2).
* **Never part of a VPC** — VPC already covers BGP-routed subnets with a VR; this is the different
  case of a public address on the Instance itself (§6.6).

## 4. Terminology

| Term | Meaning |
|---|---|
| Direct routed network | The new guest network type described here |
| `ROUTED` | The new isolation method; a physical network carrying it is where direct routed networks live (§6.7) |
| Routed id | The number in the network's `routed://<id>` broadcast URI — operator-chosen at creation, or allocated from the `ROUTED` physical network's id range (§6.7.2) |
| `brdr-<id>` | Bridge-DirectRouted: the uplink-less per-network bridge, named from the network's routed id; created by `modifybrdr.sh` |
| Shared gateway | `169.254.0.1` / `fe80::1`, present on **every** `brdr-*` bridge on **every** host |
| Host route | `ip route replace <vm-ip>/32 dev <bridge>`, installed by `modifymacip.sh` |
| Static neighbour | `ip neigh replace <vm-ip> lladdr <mac> dev <bridge> nud permanent` |
| Routing daemon | FRR/BIRD/other, run and configured by the operator — not by CloudStack |

## 5. Network model

### 5.1 Addressing

* Guest NIC: `203.0.113.55/32`, `2001:db8:1::55/128`
* Guest default route: `default via 169.254.0.1 dev eth0 onlink` and `default via fe80::1 dev eth0`
* Host guest bridge: `169.254.0.1/32` and `fe80::1/64` — identical on every host
* Host, per guest address: a /32 (or /128) route and a permanent neighbour entry, both on the
  **bridge** (§9.1.2)

### 5.2 Packet walk — guest to elsewhere

1. The guest has no on-link neighbours (it is a /32) → everything goes to the default route.
2. The guest ARPs for `169.254.0.1` — permitted because the route is `onlink` — or ND-solicits
   `fe80::1`. The host bridge answers.
3. The host routes the packet per its own routing table, out to the fabric.

### 5.3 Packet walk — fabric to guest

1. The fabric has learned `203.0.113.55/32` from this host via BGP/OSPF.
2. The packet arrives; the host matches `203.0.113.55/32 dev <bridge>`.
3. The host does **not** need to ARP — `modifymacip.sh` installed a permanent neighbour entry
   mapping the address to the guest's MAC. The frame is handed to the bridge, which forwards it to
   the port where that MAC was learned.
4. Delivered.

Static neighbour entries rather than ARP are deliberate: they remove a resolution round-trip from
VM start, make host→guest delivery independent of whether the guest answers ARP, and close off
ARP-based address takeover between guests on the same host.

Note the address is pinned to a **MAC**, not to a port — the MAC-to-port mapping comes from ordinary
bridge FDB learning. See §12.1 for why that is still sound, and what it depends on.

### 5.4 Packet walk — guest to guest, same host

Between guests of **different** networks, the two addresses are on different bridges (§9.2), so the
host routes between them and there is no L2 path at all.

Between guests of the **same** network, both host routes are local to one bridge and the host
hairpins via that bridge. Either way the traffic passes through the host's forwarding table, and
therefore through security group filtering where those are in use (§12.2).

Remaining asymmetry: same-host traffic never reaches the fabric, so fabric-level policy does not see
it. Since security groups are optional here, same-network guest-to-guest traffic on one host may be
unfiltered — accepted for v1, see §12.3.

### 5.5 What the network object looks like

Like a **Shared** network, not like L2: the network has a subnet. The operator supplies the IPv4
subnet (`cidr`, or `netmask` plus `startip`/`endip`) and/or an IPv6 prefix (`ip6cidr`) at
`createNetwork`; they are stored via the usual IP-range mechanism (`vlan` rows — `vlan_netmask`,
`ip4_range`, `ip6_cidr` in `engine/schema/src/main/java/com/cloud/dc/VlanVO.java`).

The difference is what CloudStack does with it: the subnet is an **allocation pool that is routed to
the hypervisors**, not a broadcast domain. Guests never see the subnet mask or a subnet gateway.

**No subnet gateway exists. DECIDED — revised 2026-09-09 (supersedes "required, and ignored").**

`vlan_gateway` / `ip6_gateway` are meaningless for this network type: the guest's gateway is always
the shared link-local address and the whole subnet is routed to the hosts. Requiring one only burnt
an address, so guest L3 networks store **NULL** for both and `gateway`/`ip6gateway` given to
`createNetwork` are accepted for API compatibility and ignored (`NetworkServiceImpl.createGuestNetwork()`,
`ConfigurationManagerImpl.createVlanAndPublicIpRange()` — the gateway-less L3 branch). Every code
path that keyed on the gateway's presence keys on the cidr instead (§6.3).

The one place a gateway is still typed is the **routed public IP range** for SystemVMs (§8.5):
`createVlanIpRange` shares its validation with every other public range and still requires an
IPv4 gateway outside the range (and an IPv6 gateway when `ip6cidr` is given). The guru replaces
both with the link-local gateway on the NIC, so the typed addresses are never configured anywhere.
Relaxing that stays available later; it is a validation change only.

## 6. Design decisions

### 6.1 How is the network type modelled? **DECIDED — `GuestType.L3`**

`Network.GuestType` becomes `Shared, Isolated, L2, L3`
(`api/src/main/java/com/cloud/network/Network.java:45`).

Chosen because it is self-describing and symmetrical with the existing `L2`, and because every
existing `GuestType.L2` / `Shared` branch then becomes an obvious place to decide what `L3` should
do. The cost is accepted: a new enum value touches API responses, UI and upgrade, and roughly a
dozen files special-case `L2` with a similar number special-casing `Shared`.

Rejected alternatives, for the record:

* **`GuestType.Shared` plus a flag or `NetworkMode` on the offering** — no new enum, and it would
  inherit Shared's subnet and allocation handling for free, but it overloads `NetworkMode.ROUTED`
  which already means "VR routes, no NAT". Every Shared code path would have to ask "but is it the
  routed kind?", which is worse than a new value.
* **`GuestType.DirectRouted`** — explicit, but mixes a topology concept (`L2`) with a routing one in
  the same enum.

Files to review — those branching on `GuestType.L2`:

* `server/src/main/java/com/cloud/network/NetworkServiceImpl.java`
* `server/src/main/java/com/cloud/network/NetworkModelImpl.java`
* `server/src/main/java/com/cloud/network/guru/GuestNetworkGuru.java`
* `engine/orchestration/src/main/java/org/apache/cloudstack/engine/orchestration/NetworkOrchestrator.java`
* `engine/orchestration/src/main/java/com/cloud/vm/VirtualMachineManagerImpl.java`
* `server/src/main/java/com/cloud/vm/UserVmManagerImpl.java`
* `api/src/main/java/org/apache/cloudstack/api/command/user/network/CreateNetworkCmd.java`
* `engine/schema/src/main/java/com/cloud/offerings/dao/NetworkOfferingDaoImpl.java`
* `plugins/network-elements/vxlan/.../VxlanGuestNetworkGuru.java`
* `server/src/main/java/org/apache/cloudstack/vm/UnmanagedVMsManagerImpl.java`

…plus the `GuestType.Shared` branches, which is where subnet and IP-range handling lives.

### 6.2 Which gateway address? **DECIDED — static `169.254.0.1` / `fe80::1`**

Fixed, not configurable. Requirements met: identical on every host, never globally routable, cannot
collide with guest address space.

`fe80::1` is unambiguously correct — link-local next-hops are the norm in IPv6 and need no special
handling.

`169.254.0.1` requires `onlink` on the guest route because it falls outside the guest's /32 (§8.1),
and that is fine: **the address is a /32, so the guest has to treat its gateway as on-link whatever
that gateway is.** There is no configuration of the gateway address that would avoid needing
`onlink`, so making it configurable would buy nothing — any guest that can work at all here can use
an on-link next-hop.

An earlier draft left open whether to add a global setting for operators whose images might refuse a
link-local next-hop. Closed: no setting. Keeping it fixed preserves the "guest configuration is
completely host-independent" property by construction, and removes a knob that could only ever be set
wrong.

Consequences:

* §8.1's rule — emit `on-link: true` when the gateway is in `169.254.0.0/16` — is now permanently
  sufficient. There is no need to generalise it to "gateway outside the address's own prefix",
  because the gateway will never be anything else.
* `modifybrdr.sh` keeps its `-4` / `-6` options with these values as defaults, **deliberately**.
  Nothing calls them with anything other than the defaults today, and they are not a CloudStack
  setting — but they cost nothing, make the script testable in isolation, and mean a future change of
  heart is a caller-side change rather than a script rewrite. They should not be removed as dead
  options.

### 6.3 Where do the addresses come from? **DECIDED**

Users add a subnet to the network; CloudStack assigns **individual** IPv4 and IPv6 addresses out of
it — but the two families work fundamentally differently, and both mechanisms are reused unchanged:

* IPv4 → drawn from a **pre-populated pool**: `createVlanIpRange` writes every address of the range
  into `user_ip_address` (`engine/schema/src/main/java/com/cloud/network/dao/IPAddressVO.java`) and
  allocation marks rows.
* IPv6 → **computed, never pooled**: the address is calculated from the subnet and the NIC's MAC
  (EUI-64) at allocation time and stored only on the NIC — see §6.3.4.
* Subnet definition → `vlan` rows, as for Shared networks

This is the mechanism Shared networks already use: `DirectNetworkGuru.allocateDirectIp()`
(`server/src/main/java/com/cloud/network/guru/DirectNetworkGuru.java:316`) →
`IpAddressManagerImpl.allocateDirectIp()`
(`server/src/main/java/com/cloud/network/IpAddressManagerImpl.java:2434`).

**Each family is optional (added 2026-09-09): IPv6-only networks are supported.** Nothing on an
L3 network depends on IPv4 — no DHCP, no password or metadata service, and ConfigDrive carries
whatever families exist — so network creation requires only that at least one family is given and
that a given family is complete: IPv4 is a subnet with a pool (v4 addresses are drawn from one),
IPv6 is ip6cidr alone (no range — §6.3.4). Enforced by
`NetworkServiceImpl.validateL3AddressFamilies()`; the allocation chain gates on the cidrs.
IPv4-only networks work symmetrically.

**The IPv4 subnet is given as `cidr`, or as `netmask` + `startip` (+ `endip`).** `createNetwork`
gained an optional `cidr` parameter for L3 networks (the IPv4 counterpart of `ip6cidr`; rejected
for other guest types). `NetworkServiceImpl.expandL3Ipv4Cidr()` expands it into the
netmask/startip/endip triple the rest of the flow uses: without `startip` the range defaults to
the subnet's usable addresses (network and broadcast excluded — give `startip`/`endip` explicitly
to include them, §6.3.1); an explicit `startip`/`endip` must lie inside the given cidr, and
`endip` without `startip` is rejected. `cidr` and `netmask` are mutually exclusive. With
`netmask` + `startip`, the subnet derives from the two.

**Gateways play no part at all (revised 2026-09-09).** The Instance's gateway is always the
shared link-local address (§6.2), so declaring a subnet gateway only burnt an address the
operator then had to keep free. `gateway` and `ip6gateway` are accepted for API compatibility
but ignored and stored as NULL; start/end IPs are validated against the subnet itself. The two
allocation gates that historically keyed on the gateway (`allocateDirectIp()` for IPv4,
`setNicIp6Address()` for IPv6) gate on the L3 network's cidr instead.

Consequences, all good:

* No new tables, no new allocation logic, no new capacity accounting.
* Existing IP reservation, `listPublicIpAddresses`, and quota/usage machinery apply.
* Requested-IP (`ipaddress=` on `deployVirtualMachine`) works for free.

The one thing the new guru **must** override: `allocateDirectIp()` sets the NIC's gateway and
netmask from the vlan row (lines 2459–2461). For this network type they must instead be forced to
`255.255.255.255` / `169.254.0.1` and `/128` / `fe80::1`.

#### 6.3.1 Network and broadcast addresses are usable **DECIDED — must work**

There is no broadcast domain, so the first and last address of a subnet are ordinary, routable,
assignable addresses. `203.0.113.0` and `203.0.113.255` in a /24 are just addresses; nothing
broadcasts to them, and every guest is a /32 behind the host's routing table.

**They must be assignable.** The gain is proportionally largest exactly where address space is
tightest, which is the common case for this feature:

| Subnet | Addresses | Usable today (minus network, broadcast, gateway) | Usable here | Gain |
|---|---|---|---|---|
| /29 | 8 | 5 | 8 | +60% |
| /28 | 16 | 13 | 16 | +23% |
| /27 | 32 | 29 | 32 | +10% |
| /24 | 256 | 253 | 256 | +1.2% |

(No gateway is deducted either, since §5.5 stores none.)

**Verified during implementation: no code change was needed.** The earlier draft assumed the
exclusion lived in the `NetUtils` CIDR helpers (`getIpRangeFromCidr()` and friends, whose
`start = (ip & netmask) + 1` / `end - 2` arithmetic does skip the two addresses) and would need
inclusive variants. Tracing the actual path an operator-supplied range takes showed those helpers
are only used where CloudStack *derives* a range from a CIDR — Isolated networks. The explicit
start/end path this network type uses (Shared-style `createVlanIpRange`) validates with
`sameSubnet()` — a plain bitmask comparison that `.0` and `.255` pass — plus start≤end and
gateway-not-in-range, and `savePublicIPRange()` then iterates the range without exclusions into
`user_ip_address`. Allocation is row-based from that table and never re-derives from the CIDR.

**`.0` and `.255` are therefore already assignable end-to-end on this path** — when the operator
gives `startip`/`endip` explicitly. The `cidr`-only form defaults the range to the usable
addresses and so excludes them (§6.3, deliberately conservative). `test_l3_networks.py` asserts
an Instance actually receives `.0`, since this rests on tracing rather than a guarantee anyone
maintains.

#### 6.3.2 Subnets must be unique across the routing domain **DECIDED — constraint**

Because every direct routed network on a host shares one routing table, and all subnets are
advertised into one fabric, **subnets cannot overlap between networks**. Two tenants cannot both
use `10.0.0.0/24`.

This follows from §9.2: networks separate tenants in the API and UI, not in address space. There is
no VRF, no per-tenant routing table, and no NAT to hide behind. Addresses must be unique and
routable within the routing domain.

Implications:

* Tenants cannot bring their own overlapping RFC1918 space. Operators assign from a pool they
  control — public space, or private space that is unique zone-wide.
* **Validation must reject a new subnet that overlaps an existing direct routed subnet anywhere in
  the zone. Required — an overlap is an address conflict, not a policy preference.** Two networks
  sharing a subnet would produce duplicate /32s in one host routing table and duplicate
  advertisements into the fabric, with traffic delivered to whichever Instance the host resolved
  last.
* **Implemented:** the IPv6 vlan overlap check was already zone-wide (`_vlanDao.listByZone()`).
  IPv4 was not — the `user_ip_address` unique key is `(public_ip_address, source_network_id)`,
  i.e. per network — so the range-creation path L3 networks take
  (`ConfigurationManagerImpl.createVlanAndPublicIpRange()`, the long-argument form called from
  `NetworkServiceImpl.commitNetwork()`) calls the existing zone-wide `checkOverlapPublicIpRange()`
  for L3 networks: an L3 range may not contain any address already present in the zone, whether
  it belongs to a public range, a Shared network or another L3 network. The reverse direction is
  covered by `checkZoneVlanIpOverlap()`, which now also considers gateway-less (L3) vlan rows and
  treats a subnet overlap with an L3 network like one with a public range, i.e. rejects it. Shared
  networks among themselves keep their historical behaviour, where the same IPv4 range in two
  VLANs is legitimate. (A first version placed the L3 call in the `createVlanIpRange` API path,
  which L3 networks never take; the smoke test that should have caught it swallowed its own
  assertion. Both are fixed.)
* This is the main user-visible limitation of "networks separate tenants administratively only" and
  must be explicit in the documentation.
* Per-tenant VRFs would lift the restriction but mean per-VRF routing tables on the host and
  per-VRF sessions in the routing daemon — well beyond v1 (§15).

#### 6.3.3 Route scale **DECIDED — out of scope**

Each guest address is advertised as an individual /32 or /128, so a zone with 50k Instances puts 50k
routes into the fabric. This is inherent to the design: any aggregation scheme pins addresses to
hosts and breaks migration (§11), so per-address advertisement is deliberate.

**How that scales is the operator's concern, not CloudStack's.** Fabric capacity, aggregation and
route policy are local network design — the same boundary already drawn around the routing daemon in
§10. CloudStack states no supported ceiling, because it has no way to know one: the answer depends
entirely on the hardware and topology in front of it.

For context rather than as a commitment: route counts in the order of 100k are not usually a problem
for modern equipment. Operators running L3-to-the-host fabrics are typically already carrying host
routes at that scale.

#### 6.3.4 IPv6 is calculated from subnet + MAC, never drawn from a stored pool **DECIDED**

**The IPv6 address of a NIC is computed with EUI-64 from the network's IPv6 subnet and the NIC's
MAC address, at allocation time. CloudStack never stores IPv6 addresses that are *to be* allocated;
it stores only what *was* allocated, on the NIC itself (`nics.ip6_address`).** This applies to
regular Instances and to SystemVMs alike.

Verified in the code — this is already how CloudStack behaves, on both paths this feature uses:

* **Guest NICs** (the `DirectNetworkGuru` lineage): `IpAddressManagerImpl.allocateDirectIp()`
  (`:2479`) calls `Ipv6AddressManagerImpl.setNicIp6Address()`
  (`server/src/main/java/com/cloud/network/Ipv6AddressManagerImpl.java:206`), which computes
  `NetUtils.EUI64Address(network.getIp6Cidr(), nic.getMacAddress())` and sets the result on the
  `NicProfile`. `DirectRoutedNetworkGuru.applyDirectRoutedAddressing()` then reshapes it to `/128`
  + `fe80::1`.
* **Public NICs** (SystemVMs, §8.5): `PublicNetworkGuru.getIp()` calls
  `Ipv6ServiceImpl.updateNicIpv6()` (`Ipv6ServiceImpl.java:440`), which selects the range's
  `ip6_cidr`, narrows it to a /64 if it is larger, and computes
  `NetUtils.EUI64Address(ipv6Network, nicMacAddress)` (`:236`); the reservation is a placeholder
  NIC, i.e. again a NIC row, not a pool entry.
* **`user_ipv6_address` is not an allocation pool.** No production code path inserts rows into it
  (verified by search: only reads — taken-checks and counts — remain). §6.3's earlier draft listed
  it as the IPv6 counterpart of `user_ip_address`; that was wrong and is corrected above. The table
  stays untouched by this feature.

Why this is the right model, stated so it survives review:

* **Recomputable = host- and database-independent.** Subnet + MAC always yields the same address:
  what SLAAC would have produced, what the guest can verify, and what migration preserves for free.
* **Uniqueness needs no coordination.** MACs are unique per network, subnets are unique zone-wide
  (§6.3.2), so EUI-64 addresses collide nowhere — with no counter, no lock and no pool to maintain.
* **Nothing to pre-populate or garbage-collect.** A /64 has 2⁶⁴ addresses; a pool table for that is
  a non-starter anyway. The IPv4 pool model stays where it belongs, on IPv4.
* A user-requested IPv6 (`ipaddress6=` on deploy) is rejected when it is EUI-64-shaped
  (`Ipv6AddressManagerImpl.acquireGuestIpv6Address()`, `:112`), so a manual address can never
  collide with a computed one.

Consequences:

* **The IPv6 subnet must be /64 or larger.** `NetUtils.EUI64Address()` throws for prefixes longer
  than /64 (`NetUtils.java:1731`) — the interface identifier needs 64 bits. Today an L3 network's
  IPv6 CIDR skips the Shared-network "/64 exactly" check, which leaves a longer prefix (e.g. /80)
  to blow up at Instance deploy instead of at network creation. **Validation to add:** reject an
  IPv6 CIDR with a prefix longer than /64 at `createNetwork`/`createVlanIpRange` time for L3
  networks.
* The IPv6 usable-address accounting of §6.3.1 is moot for v6 — there is no pool whose ends could
  be excluded; every EUI-64 result inside the subnet is valid.

### 6.4 Service/provider matrix for the offering **DECIDED**

`ConfigDriveNetworkElement` advertises `UserData`, `Dhcp`, `Dns`
(`server/src/main/java/com/cloud/network/element/ConfigDriveNetworkElement.java:203`). This type uses
two of the three.

| Service | Provider | Note |
|---|---|---|
| `UserData` | `ConfigDrive` | **mandatory** |
| `Dns` | `ConfigDrive` | optional but **strongly recommended** — the only way a guest learns its resolvers unless its template already has them (§6.5) |
| `SecurityGroup` | `SecurityGroupProvider` | optional (§12.2) |
| `Dhcp` | — | **not supported, and not needed** |
| `SourceNat`, `StaticNat`, `PortForwarding`, `Lb`, `Firewall`, `Vpn`, `NetworkACL`, `Gateway` | — | not supported |

* `specifyIpRanges` → `true` (the operator supplies the subnet)
* `specifyVlan` → the operator's choice, exactly as for Shared offerings: `true` means the routed
  id is given at network creation (via the existing `vlan` parameter), `false` means CloudStack
  allocates one from the `ROUTED` physical network's id range (§6.7.2)
* `NetworkMode` → not applicable; reject `NATTED`/`ROUTED` for this guest type. (The *isolation
  method* `ROUTED` (§6.7) is a different axis: `NetworkMode.ROUTED` says how a VR routes, the
  isolation method says which physical network these VR-less networks live on.)

**Why DNS is recommended rather than mandatory.** There is no VR and no resolver on the host, so
`network_data.json` `services` is the only channel by which CloudStack can tell a guest its DNS
servers (§8.3). The `Dns` service on the offering is what makes the offering *declare* that it
delivers resolvers; a template that already carries its own is unusual but legitimate and the
operator's call (§6.5). Note, as verified in review, that `ConfigDriveBuilder` writes the
`services` entries whenever the NIC profile carries DNS servers, which the allocation path sets
from the network or the zone regardless of the `Dns` service (`getServicesJsonArrayForNic()` never
consults the service list). In practice an L3 Instance therefore receives resolvers whenever the
network or zone has any, with or without `Dns` on the offering. That is pre-existing ConfigDrive
behaviour shared with every other network type and is left alone here.

**Why DHCP is not merely unsupported but unnecessary.** ConfigDrive delivers the address, netmask,
gateway and routes directly (§8.1). DHCP would have nothing left to hand out, and offering it would
reintroduce exactly the L2 broadcast dependency this network type removes.

Validation to add:

* reject the offering unless `UserData` is provided by `ConfigDrive`; `Dns` is permitted but not
  required (§6.5)
* reject `Dhcp` outright for this guest type
* `NetworkServiceImpl.java:657` currently rejects DNS on L2 networks. This type *requires* DNS, so
  that branch must distinguish L2 from L3 rather than treating them alike.

### 6.5 ConfigDrive is mandatory, DNS is not **DECIDED**

**`UserData` via ConfigDrive is mandatory. `Dns` is optional but strongly recommended.**

ConfigDrive itself cannot be optional: with no DHCP and no RA, it is the only channel that carries
the address, netmask, gateway and routes. A guest that ignores it comes up with no addresses at all,
and nothing in CloudStack reports an error.

DNS is a different matter. A template may already have resolvers baked in, or be configured by
whatever provisions it afterwards. That is unusual, but it is the operator's call, not something the
network type needs to enforce — so an offering may omit `Dns`. The documentation should recommend
`UserData` + `Dns` together, since omitting DNS leaves an Instance with connectivity but no name
resolution unless its template handles it.

**This had a hard prerequisite — see §8.2, implemented.** `ConfigDriveBuilder.needForGeneratingNetworkData()`
used to write network data only when the network supports `Dhcp` **or** `Dns`. Since this type
never has `Dhcp`, an offering without `Dns` would have produced an **empty `network_data.json` and
an Instance with no addressing at all** — a far worse outcome than missing resolvers. Network data
is now always generated when a direct routed NIC is present.

The IPv6 zone-DNS requirement that `createNetwork` applies to IPv6 Shared networks ("the zone has
no IPv6 DNS") is not applied to L3 networks, for the same reason: DNS is optional here.

**No warning is raised when a template ignores ConfigDrive. DECIDED.** Whether cloud-init is present
and configured inside the guest is the operator's responsibility, not something CloudStack should
police. There is no reliable way to detect it from outside the Instance in any case, so any check
would be a guess presented as a fact. Documented as a requirement of the network type; not enforced,
not warned about.

### 6.6 VPC support **DECIDED — never**

**Not out of scope for v1; out of scope permanently.** Standalone networks only.

The purpose of this network type is to route a public IPv4 and IPv6 address directly to an Instance.
A VPC is the opposite proposition: a private, self-contained address space with a VR, tiers holding
their own CIDRs, and ACLs between them. Every one of those is something this type deliberately
removes, so "direct routed inside a VPC" would not be a reduced VPC — it would be a contradiction.

The case that *sounds* like it overlaps is already covered: **VPC supports BGP routing with subnets
today** (`NetworkMode.ROUTED`, 4.20). An operator who wants tenant subnets advertised into the fabric
with a VR in the path should use that. This feature addresses the different case where the Instance
itself holds the public address and there is no VR at all.

Keeping the two apart is deliberate. They are not two settings of one feature, and treating them as
such would make both harder to reason about.

### 6.7 What makes a network direct routed **DECIDED — revised 2026-09-04**

**The offering's guest type (`L3`) on a physical network whose isolation method is `ROUTED`.**
The network operator is in the lead: direct routed is a property of the zone's fabric — hosts that
route, a routing daemon, an id plan for the bridges — so it is declared where fabric properties are
declared, on a **dedicated physical network**, not inferred from an offering that any admin can
create. Every direct routed network then carries a broadcast domain of the new type
**`routed://<id>`** (`BroadcastDomainType.Routed`, scheme `routed`), and that id names the bridge:
`routed://5828` → `brdr-5828` (§9.2).

* **Guru selection follows the standard contract.** `canHandle()` tests the zone type,
  `isMyTrafficType()`, `offering.getGuestType() == GuestType.L3` **and** `isMyIsolationMethod()`,
  exactly like its siblings (`DirectNetworkGuru.java:147`, `VxlanGuestNetworkGuru.java:56`). The
  guru registers `new IsolationMethod("ROUTED")`.
* **A dedicated physical network.** The operator creates a guest physical network with isolation
  method `ROUTED` (zone wizard or `createPhysicalNetwork isolationmethods=ROUTED`), gives it an id
  range (the physical network's existing VLAN/VNI range field — here it is the routed-id pool), and
  steers L3 offerings to it with tags, as for any other physical network. The traffic label is
  irrelevant for these networks — the bridges have no uplink (§9.3) — but the physical network is
  where the id range, the tags and the operational statement "this zone routes to the host" live.
* **The isolation method is named `ROUTED`, deliberately.** It shares the word with
  `NetworkMode.ROUTED` (4.20) but sits on a different axis: the network mode describes how a
  Virtual Router routes an Isolated/VPC network; the isolation method marks the physical network
  that carries these VR-less, hypervisor-routed networks. The documentation must state the
  distinction once, plainly.

**This reverses the earlier decision** ("the offering, and nothing else" — no isolation method, no
broadcast domain, bridge named from `networks.id`). What changed the call:

* **Operator opt-in became explicit.** Previously any admin creating an L3 offering implicitly
  asserted that every host in the zone runs a routing daemon. A physical network with `ROUTED` makes
  that a deliberate, zone-level act by the network operator, and an L3 network simply cannot be
  designed in a zone whose operator has not made it.
* **The id became operator property.** `networks.id` was unique but uncontrollable: bridge names
  could only be discovered after creation, never planned. With `routed://<id>` the id is chosen by
  the operator or drawn from a range the operator sized — host routing policy per `brdr-<id>` can be
  written before the network exists (§6.7.2).
* **The agent gets an explicit signal.** `BroadcastDomainType.Routed` on the `NicTO` replaces
  inferring "this is direct routed" from the /32-plus-link-local-gateway address form (§9.1.3).
* **No new plumbing after all.** The earlier draft rejected this model as "an allocation mechanism,
  a range to configure, and a choice to make". Working through the code showed all three already
  exist for Shared networks — the `vlan` parameter, the physical network's vnet range and
  `_dcDao.allocateVnet()`, and `specifyVlan` — and are reused verbatim (§6.7.2). What remains new is
  one enum value and one registered isolation method.

Offering creation rejects `Dhcp` for this guest type (§6.4), so a direct routed offering cannot be
built with DHCP in the first place.

#### 6.7.1 Changing a network's offering afterwards **ACCEPTED**

`NetworkServiceImpl.canUpgrade()` (`server/src/main/java/com/cloud/network/NetworkServiceImpl.java:4193`)
gates `updateNetwork`'s offering change on SecurityGroup parity, tag equality, `specifyVlan`
equality, `NetworkMode` equality, and `canMoveToPhysicalNetwork()`. It does **not** compare guest
type, so an administrator can in principle move a network onto an offering of a different type — for
example one without ConfigDrive, leaving Instances with no way to get their addressing.

**Accepted, not guarded.** This is an administrative action with an obvious cause and effect; adding
a special case to `canUpgrade()` for this type is not worth the complexity. Worth a line in the
documentation, nothing more.

One incidental note from reading that method: the **SecurityGroup parity check** means security
groups cannot be toggled on a live network by swapping offerings — enabling them is a choice made
when the network is created.

#### 6.7.2 Where the routed id comes from **DECIDED — Shared-network mechanics, reused**

The id lifecycle is exactly the Shared network's VLAN lifecycle, with a different URI scheme. No
new allocation mechanism is introduced:

* **`specifyVlan=true` on the offering** — the operator passes the id at network creation through
  the existing `vlan` parameter: `createNetwork ... vlan=5828` → `broadcast_uri=routed://5828` →
  `brdr-5828`. This is the flagship path: the operator plans the id space and can pre-provision
  per-bridge routing policy on the hosts.
* **`specifyVlan=false`** — CloudStack allocates a free id from the `ROUTED` physical network's
  range at creation (`_dcDao.allocateVnet()`, the same call Shared networks without `specifyVlan`
  use — `NetworkServiceImpl.commitNetwork()`), and releases it when the network is deleted (the
  existing release path in `NetworkOrchestrator`).

The value must be numeric — `routed://<id>` carries a number, nothing else — and is enforced:
`Networks.BroadcastDomainType.getRoutedId()` accepts a positive integer of at most ten digits
without leading zeros (so `brdr-<id>` fits the 15-character interface-name limit and the bridge
MAC derives from five bytes, §9.2), given bare or as `routed://<id>`, and canonicalises it.
`NetworkServiceImpl.canonicalizeRoutedId()` applies it to the `vlan` parameter of an L3 network,
`ConfigurationManagerImpl.canonicalizeRoutedRangeId()` to a routed public range (§8.5). Without
this, the generic URI fallback accepted anything parseable (`routed://abc`, `routed://0534`),
which bypassed the string-compare uniqueness checks. The existing Shared-network checks then
apply unchanged: an operator-specified id must not fall inside the dynamic range
(`_dcDao.findVnet()`), and must not collide with another network's broadcast URI.

**Uniqueness must be zone-wide, not per physical network.** Bridge names are global on a host, so
two networks with routed id 5828 anywhere in the zone would share `brdr-5828` and merge their L2
domains. The existing zone-wide URI overlap check
(`_networksDao.listByZoneAndUriAndGuestType()`) covers guest-vs-guest; with a single `ROUTED`
physical network per zone — the expected deployment — it is equivalent to the per-physnet check
anyway. **Guest networks and public ranges (§8.5) share the same id space** and are guarded in
both directions: creating a guest network whose routed id a public range already carries is
rejected (`NetworkOrchestrator`, `_vlanDao.findByZoneAndVlanId()`), and so is creating a public
range whose id a guest network holds — or that lies inside the routed-id range of any `ROUTED`
physical network in the zone, since auto-allocated networks draw from that range and a later
random draw would otherwise collide with the public range
(`ConfigurationManagerImpl.canonicalizeRoutedRangeId()`).

Either way the URI is set at creation and **stable for the network's life**: the bridge name never
changes, which is what makes it something host policy can reference.

### 6.8 Hypervisor support

**KVM only for v1.** The host must program routes and neighbour entries and run a routing daemon;
that is only realistic where the host OS is ours to configure. Other hypervisors: reject at network
creation with a clear error.

## 7. API and data model changes

### 7.1 API

* `createPhysicalNetwork` / `updatePhysicalNetwork` — accept `ROUTED` as an isolation method; the
  physical network's VLAN/VNI range field doubles as the routed-id pool (§6.7.2). UI: add `ROUTED`
  to the isolation method choices (zone wizard and physical network form).
* `createNetworkOffering` — accept the new `guestiptype`; validate the service matrix (§6.4);
  `specifyVlan` is a free choice (§6.7.2).
* `createNetwork` — accept the subnet as for Shared networks; the existing `vlan` parameter carries
  the routed id when the offering has `specifyVlan=true` (§6.7.2); `createVlanIpRange` gateway
  handling per §5.5; zone-wide overlap validation per §6.3.2.
* `listNetworks` / `NetworkResponse` — expose the type and the `broadcasturi` (`routed://5828`),
  which is how a user reads off the bridge name. The network's CIDR is meaningful (it is the
  pool) and should be shown; its gateway is not.
* `listNics` / `NicResponse` — report the /32 and /128 plus the shared gateway. `netmask` is
  already a dotted quad, so `255.255.255.255` needs no schema change.
* `listPublicIpAddresses` — works as for Shared networks.
* New APIs: **none.**

### 7.2 Database

* `network_offerings.guest_type` — new enum value (`L3`).
* `networks.broadcast_domain_type` — new enum value (`Routed`); `broadcast_uri` holds
  `routed://<id>`. Both columns are strings, so no schema change.
* `physical_network_isolation_methods` — new value `ROUTED`; a plain string, no schema change.
* `op_dc_vnet_alloc` — reused unchanged as the routed-id pool for `specifyVlan=false` (§6.7.2).
* `nics` — all needed columns exist: `ip4_address`, `netmask`, `gateway`, `ip6_address`,
  `ip6_cidr`, `ip6_gateway` (`engine/schema/src/main/java/com/cloud/vm/NicVO.java:55-108`).
* `user_ip_address`, `vlan` — reused unchanged. `user_ipv6_address` stays untouched: IPv6 is
  computed from subnet + MAC and stored only on the NIC (§6.3.4).
* Upgrade: new enum values only. No data migration.

### 7.3 New components

* **Network guru — a subclass of `DirectNetworkGuru`. DECIDED.** `DirectNetworkGuru` already
  implements "operator defines a subnet, CloudStack assigns individual v4 and v6 addresses", which
  is exactly the allocation behaviour wanted, so the allocate/release lifecycle is inherited rather
  than duplicated. The subclass registers `IsolationMethod("ROUTED")` and overrides:
  * `canHandle()` — accept `GuestType.L3` on a physical network with isolation method `ROUTED`
    (`isMyIsolationMethod()`, as its siblings do — §6.7)
  * the `NicProfile` after allocation — force `255.255.255.255` / `169.254.0.1` and `/128` /
    `fe80::1` over the vlan row's values (`IpAddressManagerImpl.allocateDirectIp()` lines 2459–2461,
    §6.3)
  * `design()` — broadcast domain type `Routed`, broadcast URI `routed://<id>` from the
    operator-specified or allocated id (§6.7.2, §9.2.1)

  The known cost of subclassing is inheriting `DirectNetworkGuru`'s Shared-network assumptions.
  Accepted: the alternative is duplicating the address lifecycle, which is the part most likely to
  drift and the part where bugs are least visible. **TODO:** during implementation, note any
  inherited behaviour that only makes sense for `GuestType.Shared` and override it explicitly rather
  than letting it apply by accident.
* **Network element** — none new. `ConfigDriveNetworkElement` covers UserData/DNS; the security
  group element covers filtering; host routes ride on NIC plug/unplug (§9), not on element
  `implement()`.

## 8. Guest configuration via ConfigDrive

### 8.1 `on-link` for the link-local gateway **DECIDED**

`ConfigDriveBuilder.getNetworksJsonArrayForNic()`
(`engine/storage/configdrive/src/main/java/org/apache/cloudstack/storage/configdrive/ConfigDriveBuilder.java:365`)
today emits OpenStack **network_data.json v1**:

```json
{"id":"eth0","ip_address":"...","netmask":"...","link":"eth0","type":"ipv4",
 "routes":[{"gateway":"...","netmask":"0.0.0.0","network":"0.0.0.0"}]}
```

A /32 address with a gateway outside its own subnet cannot be expressed here — v1 has no way to
mark a next-hop as on-link, and the guest kernel will reject the resulting config with
`Nexthop has invalid gateway`.

**Resolved — revised 2026-09-08 after testing on Ubuntu 26.04 / cloud-init 26.1: CloudStack
emits a network-level `gateway` key for direct routed IPv4 networks.**

The earlier resolution ("no on-link plumbing is needed; cloud-init detects a link-local gateway
itself") turned out to be wrong in a subtle way. cloud-init *does* have on-link detection
(`should_add_gateway_onlink_flag()`, gateway outside the interface subnet), but only on one path:

1. It is applied **only to a subnet-level `gateway` key** — in the netplan renderer since 23.1
   (`network/netplan: add gateways as on-link when necessary`, LP #2000596) and the networkd
   renderer since 24.2. It is **never applied to entries of the `routes` list**, in any renderer.
2. cloud-init's OpenStack converter (`cloudinit/sources/helpers/openstack.py`,
   `convert_net_json()`) keeps `network_data.json` routes as subnet `routes` — a `0.0.0.0/0`
   route is never promoted to the subnet `gateway`. So the v1 default-route emission always took
   the flagless path, and the guest kernel rejected the IPv4 default route
   (`Nexthop has invalid gateway`) while IPv6 via `fe80::1` (on-link by definition) worked.
3. The `sysconfig`, `network_manager` and `eni` renderers have no on-link logic at all.

The fix uses the one path that works: `convert_net_json()` whitelists `gateway` as a key on the
network object (`valid_keys["subnet"]`). `ConfigDriveBuilder.getNetworksJsonArrayForNic()`
therefore emits, **for direct routed IPv4 networks only**, a network-level
`"gateway": "169.254.0.1"` *instead of* the default-route entry — instead of, not alongside:
both together would render two default routes, one of them still flagless. cloud-init carries
the key into the v1 subnet's `gateway`, and the renderer's gateway path adds `on-link: true`.
Every other network type keeps the historical `routes` emission byte-for-byte; the IPv6 route
also stays in `routes` form, since a link-local next hop needs no flag.

The guest-side requirement is thus **cloud-init >= 23.1 with the netplan renderer, or >= 24.2
with networkd**. Guests on the sysconfig/NetworkManager/eni renderers still lack on-link support
regardless of what CloudStack emits — an upstream cloud-init contribution (apply
`should_add_gateway_onlink_flag()` in the routes loop and the remaining renderers, the same
one-liner as LP #2000596) is the path to closing that, tracked in §15.

Target guest config:

```yaml
version: 2
ethernets:
  eth0:
    match: {macaddress: "02:00:...:5a"}
    addresses: [203.0.113.55/32, "2001:db8:1::55/128"]
    routes:
      - to: default
        via: 169.254.0.1
        on-link: true
      - to: default
        via: "fe80::1"
    nameservers:
      addresses: [...]
```

The failing behaviour was verified on Ubuntu 26.04 (cloud-init 26.1, netplan renderer): the
routes-list form boots with IPv6 up and no IPv4 default route. The `gateway`-key emission was
verified on the same image in the September 2026 lab: the IPv4 default route is installed with
`on-link`, dual-stack and IPv6-only.

### 8.2 network_data generation was gated on DHCP or DNS **IMPLEMENTED**

```java
static boolean needForGeneratingNetworkData(Map<Long, List<Network.Service>> supportedServices) {
    return supportedServices.values().stream()
        .anyMatch(services -> services.contains(Network.Service.Dhcp)
                           || services.contains(Network.Service.Dns));
}
```
(`engine/storage/configdrive/src/main/java/org/apache/cloudstack/storage/configdrive/ConfigDriveBuilder.java`,
called from `writeNetworkData()`.)

If neither service is supported, `writeNetworkData()` wrote an empty `{}` and **the guest received
no network configuration whatsoever** — no address, no netmask, no gateway, no routes.

That gate was wrong for this network type. It equates "does this network have DHCP or DNS?" with
"does this NIC need its addressing written into ConfigDrive?", which held while ConfigDrive was a
supplement to a VR but does not hold when ConfigDrive is the *only* channel. This type never has
`Dhcp`, and §6.5 makes `Dns` optional, so the two conditions can both be false while the NIC still
very much needs its /32 written.

**Implemented:** `writeNetworkData()` generates network data whenever the historical gate is met
*or* any NIC of the Instance is direct routed (recognised by its host-route address form,
`isDirectRoutedNic()`). When a direct routed NIC forces generation, **all** NICs of the Instance
are written, matching the historical all-or-nothing semantics: an explicit network config that
listed only the L3 NIC would stop cloud-init from configuring the Instance's other interfaces.
The Javadoc on `writeNetworkData()` records the coupling, because it is invisible from the
offering side.

### 8.3 DNS **DECIDED — per network, falling back to the zone**

DNS servers reach the guest through `network_data.json` `services`
(`getServicesJsonArrayForNic`). No resolver on the host, no VR.

Resolvers come from the network when set, and from the zone otherwise — so an operator configures
DNS once per zone and overrides it only on the networks that need something different.

**This already works and needs no new code.** `NetworkModelImpl.getNetworkIp4Dns()` and
`getNetworkIp6Dns()` (`server/src/main/java/com/cloud/network/NetworkModelImpl.java:3023` and
`:3037`) implement exactly that precedence: network `dns1`/`dns2` if set, then the VPC, then the
zone. The VPC branch is simply never reached here (§6.6). The `networks` table already carries
`dns1`, `dns2`, `ip6_dns1`, `ip6_dns2`, and `createNetwork`/`updateNetwork` already expose them.

Note the interaction with §6.5: DNS is optional on the offering, but `ConfigDriveBuilder` writes
the `services` entries from the NIC profile, which carries the network's or zone's resolvers
whether or not the offering lists `Dns` (§6.4). Omitting `Dns` therefore does not suppress them.

### 8.4 Metadata service **DECIDED — none in v1**

With no VR there is no `data-server` at the gateway and no link-local metadata endpoint. **The
ConfigDrive ISO is the only source of metadata and user data**, and there is no
`169.254.169.254`-style HTTP endpoint.

**Note this explicitly for operators**, because it is a real behavioural difference from other
network types rather than an omission. Tooling that expects to `curl 169.254.169.254` — cloud-init
in some configurations, Kubernetes cloud providers, various agents — will not work unmodified. A
template that reads its metadata from ConfigDrive is fine; one that assumes the HTTP endpoint is not.

A host-side responder on the `brdr-*` bridge is entirely feasible later: the gateway address is
already there, the host already routes for the guest, and the data is already assembled for the ISO.
It is deferred rather than ruled out — **a candidate for v2** (§15).

### 8.5 SystemVMs: boot args, not ConfigDrive **REQUIRED CHANGES — dual-stack in v1**

Console proxy and secondary storage VM are still needed in a direct routed zone, and their public
interface is directly routed like everything else — a `/32` + `169.254.0.1` on-link gateway **and**
a `/128` + `fe80::1`. **IPv6 for the SystemVMs must work from the start**; it is not deferred.

SystemVMs do not consume ConfigDrive: their addressing is passed down from the hypervisor as boot
arguments, parsed by `systemvm/debian/opt/cloud/bin/setup/common.sh` and applied by
`setup_common()`. For both VM types the call is `setup_common eth0 eth1 eth2` (`init.sh:166`), so
`eth2` is the public interface **and** the default-gateway device.

Walking the chain end to end, four gaps stand between the host-route NIC form and a working
systemvm:

1. **The IPv4 default route needs `onlink`.** The values already flow — both builders pass
   `eth2ip`, `eth2mask` and `gateway` straight from the `NicProfile`
   (`ConsoleProxyManagerImpl.finalizeVirtualMachineProfile()`, `SecondaryStorageManagerImpl`
   likewise), so a NIC stamped in host-route form arrives correctly. But `setup_common()` then runs
   `ip route add default via $GW dev $gwdev` (`common.sh:399`), which the kernel rejects with
   `Nexthop has invalid gateway` when `$GW` lies outside the /32. Append `onlink` when `$GW` falls
   in `169.254.0.0/16` — the same trigger rule §8.1 leans on in cloud-init, applied by hand here
   because a systemvm has no cloud-init. The `/etc/network/interfaces` stanza itself (address +
   `netmask 255.255.255.255`, no gateway line) is fine as is, and the VMware ping-the-gateway
   workaround right after the route works once the on-link route exists.
2. **IPv6 boot args are never sent for CPVM/SSVM.** Only the VR's builder emits them
   (`VirtualNetworkApplianceManagerImpl.java:1935–1946`: `eth<N>ip6`, `eth<N>ip6prelen`,
   `ip6gateway`). The CPVM and SSVM builders emit IPv4 only and must add the same three appends for
   NICs that carry IPv6, from values the guru already stamped — prefix length via
   `NetUtils.getIp6CidrSize(nic.getIPv6Cidr())` (→ 128), `ip6gateway` from the default NIC. The
   consumer side already exists: `common.sh` parses `eth0ip6`/`eth2ip6`(+`prelen`) and
   `ip6gateway` → `IP6GW` today.
3. **The systemvm never installs an IPv6 default route.** `IP6GW` is parsed and then consumed
   nowhere; `setup_interface_ipv6()` (`common.sh:139`) writes only the address and prefix length
   and leans on `accept_ra 1` — and on a direct routed bridge **no RA ever arrives** (`accept_ra=0`
   on the bridge, §9.2, and nothing sends them). Add next to the v4 default in `setup_common()`:
   `ip -6 route replace default via $IP6GW dev $gwdev` whenever `IP6GW` is set. `fe80::1` is
   link-local, so `dev` is mandatory and no on-link handling exists or is needed — a link-local
   next-hop is on-link by definition. *Note this gap predates the feature:* a classic VLAN public
   network with IPv6 also leaves CPVM/SSVM without a v6 default unless a fabric router happens to
   send RAs. The fix is useful independently of direct routed and should not be gated on it.
4. **The public NIC must be stamped and plugged like a guest's.** `PublicNetworkGuru.getIp()`
   (`PublicNetworkGuru.java:146–156`) sets gateway/netmask from the public range's vlan row and
   hard-codes a `Vlan`/`Vxlan` broadcast URI on the NIC. For a direct routed public range the NIC
   must instead receive the host-route form (as `DirectRoutedNetworkGuru.applyDirectRoutedAddressing()`
   does for guests) plus `BroadcastDomainType.Routed` and a `routed://<id>` URI — which is also
   exactly what steers `BridgeVifDriver`'s Public branch into the brdr-bridge + `modifymacip.sh`
   path instead of the public bridge (today that handling sits only in the Guest branch). **IPv6
   is computed directly in the guru** — `NetUtils.EUI64Address(range ip6_cidr, NIC MAC)` per
   §6.3.4, then the /128 + `fe80::1` form. Deliberately *not* via `ipv6Service.updateNicIpv6()`:
   auditing that path showed it is gated on the public network offering's internet protocol
   (never set on the system public offering, so a no-op in practice) and reserves through **one
   placeholder NIC per network** — on the shared Public network every SystemVM would receive the
   *same* address. Neither the gate nor the reservation decides anything here: the MAC already
   makes the address unique. Mechanism: the public IP range's vlan row carries `routed://<id>` as
   its tag, which `getIp()` copies into the broadcast URI; the zone's public network carries one
   routed id (and thus one `brdr-<id>`) of its own, and systemvm /32s and /128s are advertised by
   the host's routing daemon exactly like guest addresses.

Minor, noted for completeness: `setup_interface_ipv6()` writes `accept_ra 1` — harmless on an
RA-less bridge, but the static default must not depend on it (and may be set to 0 for direct
routed interfaces later); SSVM's apache vhost binds `$ETH2_IP` (IPv4) — pre-existing; the SSVM
serves its HTTP endpoints on IPv4, while its own outbound traffic and reachability for
management are dual-stack.

## 9. Hypervisor (KVM) implementation

### 9.1 The agent's entire job

Per guest address, on NIC plug: install a static neighbour entry and a host route. On unplug / VM
stop / migrate-away: remove them. **That is the whole contract.** Nothing else on the host is
CloudStack's concern.

#### 9.1.1 This already exists — reuse `modifymacip.sh` **DECIDED**

The mechanism was added to `main` by `4816e059383` ("KVM: add configurable MAC/IP script hook for
static ARP/NDP and routes", PR #13495, 2026-07-10) for the VXLAN/EVPN static MAC-IP work. It does
almost exactly what this design needs:

* `scripts/vm/network/vnet/modifymacip.sh` — `-o add -b <dev> -m <mac> [-4 <ipv4>]... [-6 <ipv6>]...`
  runs `ip neigh replace <addr> lladdr <mac> dev <dev> nud permanent` and
  `ip route replace <addr>/32 dev <dev>`, and the `-6` equivalents with `/128`. `-o delete -b <dev>
  -m <mac>` discovers the addresses to remove by querying the neighbour table for that MAC, so no
  state file is needed.
* `BridgeVifDriver.executeMacIpScript()`
  (`plugins/hypervisors/kvm/src/main/java/com/cloud/hypervisor/kvm/resource/BridgeVifDriver.java:437`
  for add, `:418` for delete) is already wired into NIC plug (`:291`) and unplug (`:298`).
* It also passes the MAC-derived IPv6 link-local automatically
  (`NetUtils.ipv6LinkLocal(mac)`), handles secondary IPs, and sets
  `net.ipv6.conf.<dev>.disable_ipv6=0` before installing NDP entries.
* Gated by the agent property `vm.network.macip.static`
  (`AgentProperties.VM_NETWORK_MACIP_STATIC`, default `false`).

So §9.1 is largely an integration exercise rather than new code. Four gaps to close.

#### 9.1.2 Bridge, not tap **DECIDED**

Routes and neighbour entries go on the **bridge**, exactly as `modifymacip.sh` and the existing
`BridgeVifDriver` hook already do (`intf.getBrName()` is passed at `:291`). **No change to the
script or the hook for this.** The same code serves both the EVPN case and this one; there is no
reason to write it twice.

Consequence to be aware of: the host route pins each address to the bridge, and the static neighbour
entry pins it to a **MAC** — but the MAC-to-port mapping comes from ordinary bridge FDB learning, so
the routing table alone is not an anti-spoofing boundary. §12.1 sets out why the design is still
sound (short version: the per-network bridge is the isolation boundary, so MAC-to-port pinning only
matters within one account's own network), and §12.3 for what remains exposed.

Per-tap routes were considered and rejected for v1. Recorded for completeness in case the anti-spoof
story needs tightening later: `-b` is passed verbatim as the `dev` argument to every `ip` command,
and the delete path's `ip neigh show dev <dev>` works on a tap too, so targeting a tap would need no
script change at all — only a rename of `-b` to something less misleading.

#### 9.1.3 Gating is per NIC, on the broadcast type **DECIDED — revised 2026-09-04**

`vm.network.macip.static` is resolved once in `configure()` (`BridgeVifDriver.java:87`) and is
all-or-nothing for the host. A host runs direct routed guests *and* ordinary bridged guests side by
side, so the behaviour is decided **per NIC** — not from a host property.

**The selector is now explicit.** With §6.7 giving every direct routed network a
`routed://<id>` broadcast domain, the `NicTO` carries `broadcastType == BroadcastDomainType.Routed`
and the URI itself — populated for every NIC by `HypervisorGuruBase.toNicTO()`. That is the test:
one decision, used twice. It picks the bridge (`brdr-<id>` from the URI's value, §9.2.1) **and**
gates the MAC/IP hook. They are not separate decisions — if the driver has chosen a `brdr-` bridge,
the hook applies.

**The earlier inferred contract is superseded.** Before the revision these NICs were
`BroadcastDomainType.Native` — indistinguishable from other untagged cases — so the agent inferred
"direct routed" from the address form no other network type produces: netmask `255.255.255.255`
(and/or a `/128`) with a gateway in `169.254.0.0/16`. That inference was flagged at the time as an
implicit contract a future change could trip over; the broadcast type closes it. The address form
remains meaningful where it is genuinely about addressing — cloud-init's `on-link` handling (§8.1)
keys on the link-local gateway, and `ConfigDriveBuilder` recognises the host-route shape — but the
agent's *routing-behaviour* decisions (bridge choice, MAC/IP hook, `--directrouted` to
`security_group.py`) all key on `BroadcastDomainType.Routed`.

The agent property `vm.network.macip.static` stays as an independent host-wide opt-in for the EVPN
use case and is unaffected.

#### 9.1.4 Silent failures are kept **DECIDED — accepted**

Both `executeMacIpScript()` overloads catch everything and only log, deliberately — "managing host
neighbour/route entries is best-effort and must never break VM lifecycle operations"
(`BridgeVifDriver.java:432`).

**That behaviour is kept unchanged.** No new failure handling for this network type in v1, which
also means the existing EVPN path cannot be regressed by this feature.

The consequence, stated so it is not a surprise later: if the route or neighbour install fails, the
Instance starts normally and appears healthy, but has **no connectivity at all**, and nothing in
CloudStack reports why. Diagnosis means looking at the agent log for the warning from
`executeMacIpScript()`, or checking `ip route` / `ip neigh` on the host.

Making it fatal to the NIC plug, or raising an alert, remains available later (§15) and would be a
small change — the call sites already distinguish success from failure, they simply do not act on it.

#### 9.1.5 Secondary IPs **IMPLEMENTED**

CloudStack lets an Instance hold secondary IPv4 and IPv6 addresses on a NIC, and they need the
same treatment as the primary: a host route and a static neighbour entry, or the address is not
reachable.

**At Instance start this already worked.** `modifymacip.sh` accepts repeated `-4`/`-6`, and
`BridgeVifDriver` passes `nic.getNicSecIps()` alongside the primary addresses, so every address the
NIC holds is installed on plug. Unplug removes them all by MAC.

**Adding or removing one on a *running* Instance did not, and was fixed.** That path
(`NetworkRulesVmSecondaryIpCommand`) only ever updated ipsets and ebtables, and the management
server only sent it when security groups were in play — three separate gates
(`SecurityGroupManagerImpl`: Instance in no security group, network without the SG service;
`NetworkServiceImpl.configureNicSecondaryIp` / `RemoveIpFromVmNicCmd`: zone without SG). On a
Direct Routed network with security groups disabled, none of them fired, so a newly added secondary
IP stayed dark until the Instance was restarted — and a removed one kept being routed and
advertised.

The command now carries `directRouted` and `applySecurityGroupRules`; the agent installs or removes
the host route and neighbour entry for the address whenever the former is set, independently of
`canBridgeFirewall`, and skips the security group script when the latter is not. `modifymacip.sh`
gained per-address delete (`-o delete` with `-4`/`-6` removes just those; without them it keeps its
delete-everything-for-this-MAC behaviour, which is what unplug uses).

**The allocation itself needed enabling too (found in review).**
`NetworkServiceImpl.allocateSecondaryGuestIP()` handled only Isolated and Shared networks and
logged "not supported" for anything else, so none of the above was reachable for L3. L3 now takes
the Shared branch: IPv4 secondaries come from the network's pool, an IPv6 secondary is a
user-chosen address inside `ip6cidr`, validated as for Shared networks.

**The ipset-based dispatch pays off here.** Because to-Instance traffic on L3 matches
`--match-set <ipset> dst` (§12.2), and secondary IPs are added to that same ipset, security group
dispatch covers a new secondary IP with no rule changes at all.

#### 9.1.6 Not a gap: the shared gateway addresses

`modifymacip.sh` does not configure `169.254.0.1` / `fe80::1` — that is `modifybrdr.sh`'s job when it
creates the network's bridge (§9.2). The two scripts have a clean split: `modifybrdr.sh` owns the
bridge and its gateway addresses, `modifymacip.sh` owns per-guest routes and neighbour entries on it.

Minor robustness note: because the delete path derives addresses from the neighbour table, a route
leaks if its neighbour entry has already been flushed. Reconciliation (§9.6) covers this.

### 9.2 One bridge per network **DECIDED**

**Each direct routed network gets its own bridge on every hypervisor that runs one of its
Instances**, named `brdr-<id>` (Bridge-DirectRouted) — for example `brdr-5828`.

The `<id>` is the network's **routed id** — the value of its `routed://<id>` broadcast domain,
operator-chosen or allocated from the `ROUTED` physical network's range at creation, and stable for
the network's life (§6.7.2, §9.2.1).

The bridge is created and removed by a new script,
`scripts/vm/network/vnet/modifybrdr.sh`, modelled on `modifyvxlan.sh`:

```
modifybrdr.sh -o add    -n <routed id> [-4 <ipv4 gateway>] [-6 <ipv6 gateway>]   → prints the bridge name
modifybrdr.sh -o delete -b <bridge name>                                          → notmine | kept | deleted
modifybrdr.sh -o query  -b <bridge name>                                          → mine | notmine
```

Every operation prints exactly one token on stdout; all diagnostics go to stderr (the agent's
`Script` runner merges the two streams, so the agent reads the **last** non-blank line and
validates it before use — an earlier version read the first line, which a stray `sysctl` warning
could turn into a bogus bridge name in the domain XML). Exit code 0 means the token is valid, 1
that the operation failed (every `ip` and `sysctl` step is checked), 2 bad arguments. Inputs are
validated: the id is a positive integer of at most ten digits (§6.7.2), the bridge name must match
the script's own naming or the answer is `notmine` before anything is done with it, and the
gateway addresses must be well-formed.

On `add` it creates the bridge if absent (STP off, `forward_delay 0` — there is no uplink, so no
loop to detect and no reason to hold ports down at Instance start), enables IPv4/IPv6 forwarding on
it, disables RA acceptance, and configures the gateway addresses. On `delete` it removes the bridge,
but only after confirming nothing is still attached — an Instance may have started on the network
while the last one was stopping. The whole script runs under `flock`, like `modifyvxlan.sh`, because
concurrent Instance starts on one network will race to create the bridge.

Consequences:

* **Networks are isolated from each other at layer 2 by the topology**, not by filtering. This is
  the first reason for the design: a guest on `brdr-5828` has no L2 path of any kind to a guest on
  `brdr-5829`, and no rule set has to be correct for that to hold (§12).
* **Each network becomes a named L3 interface on the hypervisor.** This is the second reason, and it
  is an operational one: `brdr-5828` is something the operator can attach local policy to. Different
  route-maps or redistribution filters per network, per-network policy routing, QoS, or later a VRF
  per bridge — all expressible in the host's own network configuration, matched on interface name,
  with no involvement from CloudStack. The routing daemon is already the operator's (§10); giving
  each network its own interface is what makes per-network routing decisions possible at all. And
  because the operator can choose the id (§6.7.2), that policy can be written **before** the network
  exists.
* **Bridge name is identical on every host** — it derives only from the routed id — which is what
  keeps migration a no-op for the guest.
* **The vif driver now needs work.** The earlier shared-bridge design could ride on
  `BridgeVifDriver`'s existing `brname = trafficLabel` fallback
  (`plugins/hypervisors/kvm/src/main/java/com/cloud/hypervisor/kvm/resource/BridgeVifDriver.java:255`);
  that no longer applies. The driver must take the routed id from the NIC's broadcast URI
  (`BroadcastDomainType.getValue(nic.getBroadcastUri())`) and invoke `modifybrdr.sh` on plug, and on
  unplug when the last interface leaves — the same shape as its existing `createVnetBr()` handling
  for VXLAN.

#### 9.2.1 How the agent learns the bridge name **DECIDED — revised 2026-09-04**

**`NicTO` carries the broadcast URI.** The URI (`routed://5828`) and broadcast type
(`BroadcastDomainType.Routed`) are populated for every NIC by `HypervisorGuruBase.toNicTO()`, the
same way VLAN and VXLAN NICs receive theirs.

`BridgeVifDriver` selects on the broadcast type (§9.1.3), extracts the routed id from the URI and
passes it to `modifybrdr.sh`, which creates the bridge and **prints the name it chose** — the agent
uses whatever comes back. On unplug the agent first asks the script whether the interface's bridge
is one of its own (`-o query -b <name>` → `mine`/`notmine`); `notmine` sends the agent down its
regular unplug path. For its own bridges the agent then removes the Instance's routes and
neighbour entries (`modifymacip.sh -o delete`, while the bridge still exists) and only then asks
the script to delete the bridge (`-o delete -b <name>` → `kept` or `deleted`). **How the bridges
are named is known only to the script**; no `brdr-` prefix appears anywhere in Java. The query on
every unplug costs one script execution per NIC on every host, direct routed or not; that is the
price of keeping the naming out of Java and is accepted.

**This reverses the earlier decision**, which named the bridge from `networks.id` carried in
`NicTO.networkId`, precisely to avoid a broadcast domain, an isolation method and an id allocation.
The reversal rationale is in §6.7; the agent-side consequence is only that the number now comes from
the broadcast URI instead of the network-id field — the script contract (id in, name out, naming
private to the script) is unchanged.

Consequences:

* The network's `broadcast_uri` is `routed://<id>` and its broadcast domain type is `Routed`, set
  at creation and stable for the network's life. An auto-allocated id is released on network
  deletion (§6.7.2); an operator-specified one was never in the pool.
* `specifyVlan` on the offering selects between operator-specified and allocated ids (§6.4, §6.7.2).
* An operator who wants `brdr-5828` to be a *specific* number **can have it** — create the network
  from a `specifyVlan=true` offering with `vlan=5828`. Per-network host routing policy (§9.2) can
  therefore be provisioned before the network exists.

Per bridge, `modifybrdr.sh` sets:

* `169.254.0.1/32` and `fe80::1/64`
* `net.ipv4.conf.<brdr>.forwarding=1`, `net.ipv6.conf.<brdr>.forwarding=1`
* `net.ipv6.conf.<brdr>.disable_ipv6=0` and `accept_ra=0`
* `arp_ignore=1` / `arp_announce=2` — see §9.2.1
* **no physical uplink** — see §9.3, this is mandatory
* proxy ARP: **not needed** — the gateway addresses are local to the bridge, so the bridge answers
  guest ARP/ND directly, and host→guest neighbour entries are static rather than resolved

#### 9.2.2 The same gateway address on many bridges **DECIDED**

Every `brdr-*` bridge on a host carries the *same* `169.254.0.1` and `fe80::1`. This is intended,
and it is what makes a guest's configuration identical no matter which network or host it lands on.

For IPv6 it is unremarkable: link-local addresses are per-link and scoped by interface, so `fe80::1`
on twenty bridges is normal and correct.

For IPv4 the sysctls are what make it correct, and `modifybrdr.sh` sets both:

* `arp_ignore=1` — answer ARP only for addresses configured on the interface the request arrived on,
  so a request reaching `brdr-5828` is never answered on behalf of `brdr-5829`
* `arp_announce=2` — always source ARP from the address of the interface the request goes out of

Each bridge is its own L2 domain, so the ARP exchange stays within the right one regardless; the
sysctls remove the cases where the host might otherwise answer or source from the wrong interface.
The host's local route table gains one `local 169.254.0.1` entry per bridge, which is harmless — the
host never originates traffic from that address, it only replies on-link.

### 9.3 The bridges have no physical uplink **DECIDED — correctness requirement**

Unlike `cloudbr0`, a `brdr-*` bridge has **no physical port**. It is purely host-local: the only L3
presence on it is `169.254.0.1` / `fe80::1`, and all guest traffic leaves the host via the host's own
routed uplink rather than being bridged. `modifybrdr.sh` never enslaves an interface, so this holds
by construction as long as nothing else adds one.

If a bridge did have an uplink onto a shared L2 segment, two things break:

1. **Duplicate gateway addresses.** Every hypervisor configures `169.254.0.1` and `fe80::1` on every
   `brdr-*` bridge. Put those on a common segment and every host answers ARP/ND for the same
   addresses; guests would resolve the gateway to an arbitrary host's MAC.
2. **Isolation collapses.** Guests of that network across all hosts would share one broadcast domain,
   and the per-network bridge would stop being a boundary.

### 9.4 Layer 2 isolation comes from the bridges **DECIDED**

Separate bridges per network are the isolation mechanism. A guest on `brdr-5828` cannot send a frame of
any kind — ARP, raw L2, rogue RA, anything — to a guest on `brdr-5829`. There is no shared broadcast
domain, no shared FDB, and no path that filtering would have to police.

This replaces the earlier shared-bridge design, in which separate networks were only administrative
and L2 separation had to be recovered with filtering. **No libvirt nwfilter is used**: the
`no-mac-spoofing` / `clean-traffic` approach was for the shared-bridge model and is dropped entirely.

Two consequences worth stating plainly:

* **Isolation between networks needs no filtering at all.** The boundary is the bridge rather than
  a rule set. That was the motivation for this change.
* **Guests within one network still share a bridge**, so spoofing between them remains possible
  (§12.3). Since a network belongs to one account, that is intra-tenant exposure rather than
  cross-tenant.

Bridge port isolation (`bridge link set dev vnetX isolated on`) remains available as a further step
if intra-network isolation is ever wanted; it is not applied in v1 (§12.3, §15).

### 9.5 Sysctls

Routing happens on the bridge, so the bridge is the L3 input interface for guest traffic and these
apply per `brdr-*` bridge rather than per tap. `modifybrdr.sh` sets them at creation:

* `net.ipv4.conf.<brdr>.forwarding=1`, `net.ipv6.conf.<brdr>.forwarding=1`
* `net.ipv6.conf.<brdr>.disable_ipv6=0` — also set by `modifymacip.sh` before installing NDP entries
* `net.ipv6.conf.<brdr>.accept_ra=0` — a guest must never be able to send an RA the host acts on
* `net.ipv4.conf.<brdr>.arp_ignore=1` / `arp_announce=2` — required because every `brdr-*` bridge on
  the host carries the same gateway address (§9.2.2)

**`rp_filter=1` (strict) is set on every `brdr-*` bridge. DECIDED.** An Instance may only send from
an address that routes back out of the bridge it arrived on — which, given per-guest /32 routes, is
its own address. Source spoofing is therefore blocked at the host.

Set **on the bridge only, never on `all`**, so no other interface on the host changes behaviour. The
kernel takes `max(conf.all.rp_filter, conf.<dev>.rp_filter)`, so a per-bridge value of 1 is effective
regardless of what `all` is, without touching the uplink — which matters, because strict mode on a
host uplink can break legitimately asymmetric fabric routing.

Strict mode is safe for the paths this design creates:

* an Instance's own traffic — source `S`, route `S/32 dev brdr-N`, arrives on `brdr-N` → passes
* same-network hairpin (§5.4) — arrives on `brdr-N` from an address routed via `brdr-N` → passes
* cross-network — arrives on `brdr-N`, forwarded out `brdr-M`; the check is on ingress only → passes

**The sysctl is IPv4 only.** The kernel has no IPv6 `rp_filter`, so `modifybrdr.sh` installs the
netfilter counterpart once per host: `ip6tables -t raw PREROUTING -i brdr-+ -m rpfilter --invert
-j DROP`. An Instance can then not spoof an IPv6 source either, with or without security groups.

**The host protects itself from its Instances (added 2026-09-10).** Because the host routes for
them, an Instance's packets enter the host's own IP stack — something classic bridging never
exposed. Without a rule an Instance could reach the hypervisor's management and storage addresses
and everything the host routes to, including the `169.254.0.0/16` control network of the
SystemVMs on `cloud0`. `modifybrdr.sh` therefore installs, once per host and shared by all
`brdr-*` bridges, an `INPUT` chain hooked with `-i brdr-+` that accepts only what the gateway
function needs — conntrack `ESTABLISHED,RELATED`, IPv4 ICMP echo request, ICMPv6 neighbour
solicitation/advertisement and echo request — and drops everything else. Both rule sets are
removed when the last `brdr-*` bridge on the host is deleted. Operators running their own
default-DROP `INPUT` policy must still admit neighbour discovery for `fe80::1` and ICMP echo from
`brdr-+`, or Instances cannot resolve or ping their gateway. Forwarding from `brdr-*` to the
management and storage subnets is **not** filtered by CloudStack — that policy is the operator's
host firewall, exactly like the routing daemon (§10), and must be stated in the operator
documentation. `FORWARD` rules remain the exclusive domain of `security_group.py` (§12.2).

Forwarding itself needs `net.ipv4.ip_forward=1` and `net.ipv6.conf.all.forwarding=1` on the host:
the per-bridge `forwarding` sysctls the script sets do not enable it on their own (IPv6 forwards
only when `all.forwarding` is set; IPv4's per-interface flag governs packets entering the bridge,
not the uplink). A host running a routing daemon has both; they are a documented prerequisite.

### 9.6 No reconciliation on agent restart **DECIDED**

**The agent does not scan or rebuild routes and neighbour entries at startup.** The existing hook
fires only on NIC plug/unplug (`BridgeVifDriver.java:291,298`), and that is sufficient.

The reasoning, which is worth writing down because "kernel state is not persistent" invites the
opposite conclusion:

* **Host reboot** clears the routes — but it also destroys every Instance on that host. The
  management server starts them again, each start goes through the plug path, and the routes are
  reinstalled as a side effect. There is nothing to reconcile, because there is nothing running whose
  state could be missing.
* **Agent restart without a host reboot** does not clear anything. Routes and neighbour entries live
  in the kernel, not in the agent, so running Instances keep working across an agent restart with no
  action at all.
* **While the agent is down**, CloudStack cannot stop or migrate Instances on that host, so host
  state cannot drift from what the management server believes.

`modifymacip.sh` uses `ip route replace` / `ip neigh replace`, so any repeated plug is idempotent
regardless.

**Residual, accepted:** if an Instance disappears without an unplug — libvirt kills it, or it crashes
in a way that skips the normal path — its route and neighbour entry linger. The practical risk is not
the stale kernel state itself but that the routing daemon keeps advertising that /32, so if the
Instance is started on another host the fabric may see the address from two places. Rare, and not
worth a startup sweep to prevent; noted so it is recognisable if it ever shows up.

## 10. Routing daemon — explicitly out of scope

**CloudStack does not install, configure, or monitor the routing daemon.**

The contract is one-directional: *CloudStack guarantees the correct routes and neighbour entries
exist in the host kernel.* The operator configures their daemon to redistribute them, e.g. with
FRR:

```
router bgp 65001
  address-family ipv4 unicast
    redistribute kernel
  address-family ipv6 unicast
    redistribute kernel
```

Why this is the right boundary:

* Identical behaviour for FRR, BIRD, or anything else that redistributes kernel routes.
* Identical behaviour for BGP, OSPF, or IS-IS.
* No daemon version coupling, no config-file ownership conflict with the operator's automation, no
  `vtysh`/reload dependency in the agent.
* Nothing new to fail: if the route is in the kernel the guest works locally, and advertisement is
  the fabric's business.

Consequences to accept and document:

* We ship **documentation and reference configuration**, not code. Redistribution filtering via
  route-maps is the operator's job.
* **No feedback loop.** CloudStack cannot tell whether a guest's address is reachable from the
  fabric. Accepted as a known gap for v1; a health signal from the agent is listed under future work
  (§15) rather than treated as an open question.
* This is the **opposite** choice from the 4.20 BGP work, where CloudStack manages peers
  (`BgpPeerVO`, `SetBgpPeersCommand`, `systemvm/debian/opt/cloud/bin/cs/CsBgpPeers.py`). The doc
  should say why explicitly — reviewers will ask.
* Design should not preclude an optional managed mode later, but v1 does not have one.

## 11. Live migration

The guest's configuration is host-independent, so the guest needs no reconfiguration. What moves is
host state:

1. Destination host installs the route and neighbour entry when the NIC is plugged.
2. Source host removes both when the NIC goes away.
3. Each host's routing daemon advertises/withdraws; the fabric reconverges.

To work through:

* **Ordering — confirmed.** Install-then-remove is safer than remove-then-install; a transient
  duplicate advertisement is less harmful than a black hole. The destination plugs (and so
  installs) in `PrepareForMigration`; the source unplugs only after the migration succeeded
  (`LibvirtMigrateCommandWrapper`). A failed prepare unplugs what it plugged; a migration that
  fails *after* a successful prepare leaves the destination's entries in place — deferred (§15).
* **Convergence gap.** Traffic may be black-holed until the fabric reconverges. **TODO:** quantify
  on a normal iBGP/OSPF setup — is sub-second realistic?
* **No GARP needed — but the reverse direction bit us (fixed 2026-09-08).** Normally a migrating
  VM sends a gratuitous ARP to update switch tables. Here there is no L2 path to update, and the
  destination host's neighbour entry for the guest is installed statically by the agent — that
  direction is fine. The *guest's* entry for the gateway, however, is learned: it held the source
  host's bridge MAC, and after migration the destination bridge (with a different MAC) silently
  dropped frames addressed to it for ~30 s until the guest's neighbour entry expired. Resolved by
  giving every brdr bridge a MAC derived deterministically from the routed id
  (`0e:` + 5 id bytes, set by modifybrdr.sh on every add), so the gateway MAC — like the gateway
  addresses — is identical on every hypervisor and the guest's cache stays valid.
* **Per-address advertisement is mandatory.** Any aggregation scheme that pins prefixes to hosts
  breaks migration. This is the price of §6.3.3.
* **Routing domain boundaries.** Migration works as long as source and destination are in the same
  routing domain. **Left to the operator, not validated by CloudStack** — consistent with §10 and
  §6.3.3, where fabric topology is local network design. CloudStack has no model of routing domains
  and inventing one to police migration would be a larger change than the problem warrants.

## 12. Security

### 12.1 The isolation model

The boundary between networks is **topological**: one bridge per network (§9.2, §9.4), with no
uplink and therefore no path between bridges except through the host's routing table. Nothing has to
be configured correctly for that to hold, and nothing degrades if filtering is disabled.

Within one network the guests still share a bridge, so the properties there are weaker:

* **No L3 adjacency.** Each guest is a /32 behind the host's routing table; there is no
  subnet-mates relationship to exploit even between guests on the same bridge.
* **Static neighbour entries** prevent ARP-based address takeover on the host→guest path: the
  IP-to-MAC mapping is asserted by `modifymacip.sh`, never learned (§5.3). The host is therefore not
  susceptible to a guest claiming another guest's address.
* **`rp_filter=1` on the bridge** (§9.5) confines an Instance to sending from its own /32, since
  that is the only address routed back out of that bridge. IPv4 only.

What remains open inside a network is set out in §12.3. Because a network belongs to one account,
that is intra-tenant exposure.

### 12.2 Security groups — supported, via the unified script rules **DECIDED**

Security groups are supported on L3 networks, programmed by `security_group.py` as for Shared
networks. There is **no separate L3 rule function**: the classic and Direct Routed paths share one
implementation, parameterized only by how each direction identifies the Instance, plus small
conditionals for what does not exist on L3 (DHCP, DHCPv6, router advertisements towards guests).

| Direction | Classic bridge | Direct Routed bridge |
|---|---|---|
| from the Instance | `-m physdev --physdev-in <vif>` | same rule, identical |
| towards the Instance | `-m physdev --physdev-is-bridged --physdev-out <vif>` | `-m set --match-set <instance ipset> dst` |

**The `--physdev-is-bridged` question, settled in kernel source** (`net/netfilter/xt_physdev.c`):

* On `--physdev-in` rules the flag was **removable**. Match-time semantics: a bridged-then-routed
  packet carries bridge info with the ingress port set and no bridged egress port, so a plain
  `--physdev-in` matches it while `--physdev-is-bridged` excludes it. On classic bridges removing
  the flag changes nothing observable — the BF- framework hook (which keeps the flag) already
  restricts what reaches the per-VM chains to bridged traffic. Removing it is what lets both paths
  share the from-Instance rules verbatim. The golden test was regenerated once, deliberately, for
  exactly this diff.
* On `--physdev-out` rules the flag **stays**: for a routed packet no bridge info exists at all at
  FORWARD time (`nf_bridge_info_exists()` is false and every physdev variant returns false), so the
  kernel is structurally incapable of identifying the bridged egress port there. Removing the flag
  would change nothing and merely deviate from upstream convention. This is why the to-Instance
  direction matches the destination against the Instance's ipsets instead — exact, since L3
  addresses are /32s and /128s.

The IPv6 source-spoof drop in the shared rules doubles as the missing IPv6 `rp_filter` (§9.5).
Teardown uses one awk pattern on the chain names, which also covers rules created by older versions
that still carry the flag on `physdev-in` lines.

**Framework setup is shared too.** `enable_bridge_netfilter()`, `create_bridge_fw_chains()` and
`add_notrack_ipset_rules()` were extracted from `add_fw_framework()` and are used by both it and
`add_l3_fw_framework()`; the two differ only in their FORWARD hooks (classic gates on
`physdev-is-bridged` and consults chain reference counts; L3 jumps unconditionally, with the same
default-deny backstop). A second golden test pins `add_fw_framework`'s command stream, proving the
extraction left it byte-identical — 44 commands, unchanged.

**How the script knows: the Agent tells it.** `security_group.py` performs no classification of its
own — no bridge-name check, no gateway inspection. The Agent already identifies these NICs for the
bridge and MAC/IP hook (§9.1.3), so it passes `--directrouted` on `default_network_rules` and
`add_network_rules`; the script's own re-entry points thread the flag through. This keeps the
decision in exactly one host-side place and leaves the script with no inference to get wrong.

`network_rules_for_rebooted_vm` is dead code (its only caller has been commented out upstream for
years) and carries no L3 handling; a rebooted Instance is reprogrammed by the Agent through the
normal `default_network_rules`/`add_network_rules` path, which receives the flag.

**Two-pass filtering on the routed path (added 2026-09-10).** A routed packet between two
Instances on one host enters on one `brdr-*` bridge and leaves on another (or the same), so a
single per-bridge FORWARD jump can only ever evaluate one of them — whichever bridge's rule came
first decided terminally, which let an Instance bypass the ingress rules of an Instance in
another account. The L3 framework therefore uses one FORWARD hook into a shared `BF-L3` chain
that runs two passes: `BF-L3-IN` dispatches on `-i <bridge>` to the source Instance's egress
rules, `BF-L3-OUT` on `-o <bridge>` to the destination Instance's ingress rules. On the source
side an allowed egress does not ACCEPT but sets packet-mark bit `0x40000000` and returns; a
packet that leaves `BF-L3-IN` unmarked (denied, spoofed, or from a port with no registered
Instance) is dropped there. The destination side gives the terminal verdict; a destination on an
L3 bridge no Instance claims is dropped; a marked packet not addressed to an L3 Instance on this
host is accepted towards the fabric. The bit is cleared on entry so no other mark can approve
traffic, and it is reserved on hosts running L3 networks (`MARK` in the filter table needs
kernel >= 2.6.29 / iptables >= 1.4.3). Non-L3 traffic falls through `BF-L3` untouched, so classic
bridges are unaffected; a bridge's rules are removed when its last Instance is destroyed, the
shared chains and hook stay. Unit tests walk packets through the generated rules for every case
in both bridge creation orders (`scripts/vm/network/tests/test_security_group.py`).

**Still to verify in the lab:** on a real host, `iptables -C` against `-j MARK --set-xmark` and
`-m mark ! --mark` rules (idempotency), that `iptables-save` renders them in the form
`verify_network_rules` expects, and the four traffic cases end to end with two Instances in
different accounts and networks on one host, including the same-bridge hairpin and IPv6 DAD.

**Investigated and rejected: libvirt nwfilters.** An nwfilter-based implementation was built and
then discarded. Its ebtables layer would have served anti-spoofing well (it sees routed delivery),
but stateful ingress cannot work: libvirt's own to-Instance iptables hook is hard-coded as
`-m physdev --physdev-is-bridged --physdev-out` (`src/nwfilter/nwfilter_ebiptables_driver.c`), the
same structural blindness to routed delivery — inside libvirt, where it cannot be patched or
augmented with destination matching. The script approach filters correctly in both directions and,
after the unification above, without duplicate code.

### 12.3 Residual risk within one network **ACCEPTED for v1**

Guests of the same network share a bridge. With security groups enabled, RA and gateway
impersonation between them are handled by the shared rules (ebtables ARP pinning, NDP source
checks, RA drop) and source spoofing is bounded by the ipset checks plus `rp_filter` (§9.5). An
operator who disables security groups accepts RA and gateway impersonation between guests of that
one network — a deliberate operator choice; isolation between *tenants* is topological and
unaffected (§12.1). Source-address spoofing is bounded with or without security groups: strict
`rp_filter` for IPv4 and the `rpfilter` netfilter rule for IPv6 (§9.5). The host itself is immune
either way: its neighbour entries are static (§5.3) and its `INPUT` path from `brdr-*` bridges is
closed (§9.5).

### 12.4 Sharp edge

**Guests are directly reachable from the fabric.** No NAT, no VR firewall. Whatever the fabric
permits reaches the guest, subject only to the guest's security groups if they are in use. This is a
meaningful change in default posture versus an Isolated network and must be prominent in the
documentation.

## 13. Orchestration touchpoints

Status of the implementation checklist (September 2026):

- [x] `NetworkOrchestrator.allocate()` / `prepare()` / `release()` — address lifecycle inherited
      from `DirectNetworkGuru` (§7.3)
- [x] `NetworkOrchestrator` and `VirtualMachineManagerImpl` — `L2` and `Shared` branches reviewed;
      L3 follows Shared where a subnet exists and L2 nowhere
- [x] `UserVmManagerImpl` — `addNicToVm` works; `updateVmNicIp` is rejected for L3 (host-route
      form would need re-stamping); secondary IPs via `allocateSecondaryGuestIP` (§9.1.5)
- [x] `NetworkModelImpl` — `canUseForDeploy()`, `checkSecurityGroupSupportForNetwork()` extended
- [x] `IpAddressManagerImpl.allocateDirectIp()` / `Ipv6AddressManagerImpl.setNicIp6Address()` —
      gate on the cidr for L3; the guru forces /32 and /128 + link-local gateways (§6.3)
- [x] `Networks.BroadcastDomainType.Routed` and `getRoutedId()` (§6.7, §6.7.2)
- [x] `BridgeVifDriver` — selects on `BroadcastDomainType.Routed` (or a `routed://` URI, §9.1.3),
      creates the bridge via `modifybrdr.sh`, runs the MAC/IP hook, unplugs in query → MAC/IP
      delete → bridge delete order (§9.2.1)
- [x] `scripts/vm/network/vnet/modifybrdr.sh` — bridge lifecycle, host protection (§9.2, §9.5)
- [x] Guru registers `IsolationMethod("ROUTED")`; `canHandle()` on guest type + isolation method;
      `design()` sets `Routed` (§6.7)
- [x] `NetworkOrchestrator.encodeVlanIdIntoBroadcastUri()` — `ROUTED` physical network →
      `routed://` URI; broadcast domain type derived from the URI scheme (§6.7.2)
- [x] `NetworkServiceImpl.commitNetwork()` — vnet auto-allocation and release extended to L3
- [x] Offering validation — `validateL3NetworkOffering()` (§6.4)
- [x] `ConfigDriveBuilder.writeNetworkData()` — always generated with a direct routed NIC, for all
      NICs (§8.2); network-level `gateway` key for direct routed IPv4 (§8.1)
- [x] CPVM/SSVM boot args carry IPv6; `common.sh` installs `onlink` v4 and static v6 defaults (§8.5)
- [x] `PublicNetworkGuru` — routed public ranges, EUI-64 IPv6 in the guru; routed ids guarded
      against guest/public collisions in both directions and against `ROUTED` vnet ranges (§8.5,
      §6.7.2)
- [x] IPv6 CIDR longer than /64 rejected for L3 (§6.3.4)
- [x] DNS on L3 offerings allowed; zone IPv6 DNS not required for L3 (§6.4, §6.5)
- [x] `security_group.py` — unified rules with the L3 dispatch structure of §12.2; **lab
      verification of live traffic in both directions still required after the September 2026
      restructure**
- [x] Zone-wide IPv4 overlap validation, both directions (§6.3.2)
- [x] `.0`/`.255` assignable on the explicit range path; no `NetUtils` change needed (§6.3.1)
- [ ] VM import (`importNic` selects IPs the Isolated way for L3) — deferred (§15)
- [x] Network restart — no VR, nothing to restart; the element implementations are no-ops
- [ ] IP capacity reporting and usage records — L3 addresses are `user_ip_address` rows like
      Shared-network addresses and are reported and billed the same way; not separately reviewed
- [x] UI: `ROUTED` in the isolation method lists, L3 offering form (specifyVlan as routed id),
      L3 network form with physical network selector, Instance list addresses

## 14. Upgrade and compatibility

* Additive: new enum values (`GuestType.L3`, `BroadcastDomainType.Routed`), new isolation method
  string, new offering type. No change to existing networks, no data migration.
* The pre-revision implementation on this branch (L3 networks with `Native` broadcast domain and an
  empty `broadcast_uri`) was never released, so no migration from it is provided; development
  deployments recreate their L3 networks.
* **Agent version gating is out of scope. DECIDED.** No capability flag, no version check, and no
  management-server logic to keep Instances of this type away from agents that predate it. Operators
  are expected to upgrade their agents as part of upgrading CloudStack, as they already are. The
  failure mode if they do not is that `modifybrdr.sh` is missing on the old host and the Instance
  fails to get connectivity — visible in the agent log, consistent with §9.1.4.
* Downgrade unsupported once networks of this type exist, as usual.

## 15. Future work / explicitly deferred

* Non-KVM hypervisors
* Optional CloudStack-managed routing daemon configuration
* **Host-side metadata service** (`169.254.169.254`) — a v2 candidate; the gateway address and the
  data are both already present, so it is a natural addition (§8.4)
* Multiple addresses per NIC — additional /32s fit the model naturally, but v1 is one v4 + one v6
* Reachability/health feedback from the routing daemon
* Network Config v2 `network-config` emission for NoCloud-configured images — a later PR (§8.1)
* Upstream cloud-init contribution: apply the on-link flag to routes-list entries and in the
  sysconfig/NetworkManager/eni renderers, so guests beyond netplan/networkd work too (§8.1)
* Making a failed route/neighbour install fatal or alert-raising, rather than silent (§9.1.4)
* Closing the §12.3 intra-network spoofing gaps — bridge port isolation (§9.4) first, or libvirt
  nwfilter if a narrower fix is preferred
* **Per-tenant VRFs**, which would lift the non-overlapping-subnet constraint of §6.3.2

Found in the September 2026 review and consciously deferred, not forgotten:

* **Failed live migration** after a successful `PrepareForMigration`: the destination keeps the
  host route and neighbour entry (and its daemon keeps advertising) for an Instance that stayed on
  the source, so the fabric may black-hole it until the next start/stop. The orchestrator should
  send the rollback form of `PrepareForMigrationCommand` on any migrate failure and the agent's
  rollback should unplug the NICs (§11).
* **Who may create L3 networks.** `validateNetworkOfferingForNonRootAdminUser()` admits Isolated,
  L2 and Shared-without-specifyVlan only, so L3 networks are root-admin-only today. Security-group
  enabled Advanced zones reject L3 in two places (`NetworkOrchestrator`, `UserVmManagerImpl`).
  Decide and either add L3 or document root-admin-only.
* **KVM-only is not enforced** (§6.8): no hypervisor check at network creation or placement.
* **IPv6 start/end ranges** are accepted for L3 though §6.3 says `ip6cidr` alone; with a range
  two networks can share a /64 and EUI-64 addresses could collide. Reject the range.
* **Routed public ranges** still type an IPv4 and IPv6 gateway that nothing uses (§5.5).
* `deleteVlanIpRange`/`updateVlanIpRange` key on gateway presence and so mishandle gateway-less L3
  rows (stale `networks.cidr`, "IPv4 is not supported in this IP range").
* `updateNetwork` cannot set IPv6 DNS on an L3 network (`isIpv6` keys on Shared).
* `network_rules_for_rebooted_vm` in `security_group.py` is dead code; `verify_network_rules`
  expects the pre-2026 rule stream (§12.2).
* Per-interface `forwarding` sysctls do not enable forwarding by themselves: `net.ipv4.ip_forward=1`
  and `net.ipv6.conf.all.forwarding=1` are host prerequisites (a host running a routing daemon has
  them), to be stated in the operator documentation (§9.5).
* SystemVMs with a security-group NIC on a routed public range would hit the classic framework in
  `default_network_rules_systemvm`; VRs are not expected on routed public ranges.
* The routed-id pool of a `ROUTED` physical network is capped at 1–4094 because the vnet-range
  validation treats it like VLAN (§6.7.2).
* `handleVmStartFailure` in the KVM start wrapper unplugs the NICs of a domain that may have
  started before a late `RuntimeException`; checking the domain state first would avoid removing
  a running Instance's routes ahead of the orchestrator's Stop.
* VM import (`importNic`) treats L3 like Isolated when selecting an IP; it should follow Shared.

## 16. Decision log and open questions

### Revised 2026-09-04 — isolation model

Three decisions from the 2026-07-30 design were reversed together; rationale in §6.7:

* **Isolation method:** was "none to register"; now the guru registers `IsolationMethod("ROUTED")`
  and direct routed networks live on a dedicated physical network carrying it. The network operator
  opts the zone in explicitly.
* **Broadcast domain:** was "`Native`, empty URI"; now `BroadcastDomainType.Routed` with
  `routed://<id>`, set at creation and stable for the network's life.
* **Bridge naming:** was `brdr-<networks.id>` (unchoosable); now `brdr-<routed id>`, with the id
  operator-specified (`specifyVlan=true` + `vlan` parameter) or allocated from the physical
  network's range (`specifyVlan=false`) — the Shared network's VLAN mechanics, reused (§6.7.2).

The agent's inferred /32-plus-link-local-gateway gating became explicit gating on the broadcast
type as a consequence (§9.1.3). Entries below are updated in place; superseded wording is kept in
the relevant sections as "reverses the earlier decision" notes.

### Settled

* Network type is "no DHCP", not "like L2" (§1)
* IPv4 addresses come from the `user_ip_address` pool with an operator-supplied subnet (§6.3);
  subnets must not overlap zone-wide (§6.3.2)
* **IPv6 is calculated (EUI-64) from the subnet and the NIC's MAC** — never drawn from a stored
  pool; only the allocated result is stored, on the NIC. Already how both the guest and the public
  path behave; requires the subnet to be /64 or larger, to be validated at creation (§6.3.4)
* New `GuestType.L3`, chosen over overloading `Shared`/`NetworkMode` (§6.1)
* Gateway is a static, non-configurable `169.254.0.1` / `fe80::1`; a /32 means the guest must treat
  its gateway as on-link regardless, so configurability would buy nothing (§6.2)
* ConfigDrive emits a network-level `gateway` key for direct routed IPv4, from which cloud-init
  derives `on-link` (§8.1)
* The agent writes only routes and neighbour entries; FRR is out of scope (§9.1, §10)
* That work is done by reusing `modifymacip.sh` + the `BridgeVifDriver` hook already in main from
  `4816e059383` / PR #13495, rather than new code (§9.1.1)
* Routes and neighbour entries go on the **bridge**, not the tap — the script and hook are reused
  unchanged (§9.1.2)
* **One bridge per network**, named `brdr-<routed id>`, created and removed by the new
  `scripts/vm/network/vnet/modifybrdr.sh` (§9.2)
* **Isolation method `ROUTED` on a dedicated physical network** selects the guru; the network's
  broadcast domain is `routed://<id>` and the id is operator-specified or allocated from the
  physical network's range — reversing the earlier "no isolation method, no broadcast domain"
  decision (§6.7, §6.7.2, §9.2.1; revision block above)
* Per-network bridges also give the operator a named interface per network to hang local routing
  policy off — a deliberate benefit, not just a side effect (§9.2)
* Those bridges have no physical uplink (§9.3)
* L2 isolation between networks is topological — separate bridges — not filtering. **No libvirt
  nwfilter is used**; the earlier `no-mac-spoofing` / `clean-traffic` plan is dropped (§9.4)
* Security groups are supported on L3 via **one unified rule implementation** shared with classic
  bridges: `--physdev-is-bridged` dropped from `physdev-in` rules (verified harmless in kernel
  source), destination-ipset matching towards the Instance. nwfilter was investigated and rejected
  — libvirt's own to-Instance hook has the same physdev-is-bridged blindness (§12.2)
* Within one network, RA and gateway-impersonation protection depends on security groups being
  enabled; leaving them off is a deliberate operator choice (§12.3)
* The `--physdev-is-bridged` rework is **required** — L3 filtering must work — and must be strictly
  additive so existing Basic-zone and Shared-network rules are unchanged (§12.2)
* A network is direct routed when its offering's guest type is `L3` **and** it lives on a
  physical network with isolation method `ROUTED` — guru selection follows the standard
  isolation-method contract. Changing a network's offering afterwards is not guarded (§6.7, §6.7.1)
* The offering requires ConfigDrive `UserData`; `Dns` is optional but strongly recommended, and
  `SecurityGroup` is optional. `Dhcp` is rejected — not just unsupported but unnecessary (§6.4, §6.5)
* `rp_filter=1` is set on each `brdr-*` bridge, bounding IPv4 source spoofing to the Instance's own
  address; IPv6 has no kernel equivalent (§9.5)
* **No reconciliation at agent startup** — a host reboot destroys the Instances too, and an agent
  restart does not clear kernel state (§9.6)
* The MAC/IP hook is gated **per NIC, on `BroadcastDomainType.Routed`** rather than a host
  property; the same test picks the bridge. (Originally inferred from the /32 + link-local-gateway
  address form; superseded by the explicit broadcast type — §9.1.3)
* The same `169.254.0.1` on every `brdr-*` bridge is **correct as designed**; `arp_ignore=1` and
  `arp_announce=2` handle it (§9.2.2)
* The guru is a **subclass of `DirectNetworkGuru`**, inheriting the address lifecycle rather than
  duplicating it (§7.3)
* **On-link via the network-level `gateway` key** — cloud-init applies its on-link detection only
  to that key, never to routes-list entries, so CloudStack emits it for direct routed IPv4
  (verified on Ubuntu 26.04 / cloud-init 26.1); the v2 `network-config` file is deferred to a
  later PR (§8.1, §15)
* DNS is **per network, falling back to the zone**; already implemented by
  `NetworkModelImpl.getNetworkIp4Dns()` (§8.3)
* **No metadata service** in v1 — ConfigDrive only, no `169.254.169.254`; a v2 candidate (§8.4)
* Route/neighbour install failures **stay silent** for v1, as they are for EVPN (§9.1.4)
* Route scale is **out of scope** — fabric capacity and aggregation are local network design;
  ~100k routes is not usually a problem on modern equipment, but CloudStack states no ceiling (§6.3.3)
* Network and broadcast addresses **must be assignable** — true on the explicit start/end range
  path and asserted by the smoke test; the `cidr`-only form defaults to the usable range (§6.3.1)
* **No subnet gateway** for guest L3 networks: `gateway`/`ip6gateway` are ignored and stored as
  NULL; routed public ranges still type one, which the guru replaces (§5.5, §6.3)
* The routed id is a **positive integer of at most ten digits**, canonicalised on input; public
  ranges may not take an id inside a `ROUTED` physical network's range (§6.7.2)
* IPv4 range overlap is checked **zone-wide in both directions** on the range-creation path L3
  networks actually take (§6.3.2)
* The `--physdev-is-bridged` rework **lands in v1**; security groups are not shipped half-working
  (§12.2)
* Zone-wide subnet overlap validation is **required** — an overlap is an address conflict (§6.3.2)
* **No warning** when a template ignores ConfigDrive; cloud-init inside the guest is the operator's
  responsibility (§6.5)
* **Agent version gating is out of scope** — operators upgrade agents with CloudStack (§14)
* KVM only for v1 (§6.8); **VPC never** — it is a separate use case already served by VPC's own
  BGP-routed subnets (§6.6)
* **SystemVMs stay, configured via boot args, dual-stack from the start** — the v4 default route
  gains `onlink` for link-local gateways, the CPVM/SSVM builders emit the IPv6 args the VR builder
  already emits, the systemvm installs the v6 default from `ip6gateway` instead of hoping for an
  RA, and the public NIC is stamped/plugged in the same host-route + `routed://` form as a guest
  NIC — its IPv6 coming from the same EUI-64 computation (§8.5, §6.3.4)

### Still open

**None.** Every design question raised in this document has been decided.

What remains is implementation work, tracked in §13 and as `TODO` markers in the sections above.
Three of those are verification rather than coding, and are the ones most likely to change a
decision if they come out badly:

* §12.2 — confirm on a real host that the unified rules match live traffic in both directions on
  both bridge types, and that ARP for the gateway and neighbour discovery pass
* §6.3.1 — confirm nothing downstream of `createVlanIpRange` re-derives the usable range and
  re-excludes `.0` and `.255`
* §6.3.2 — confirm the existing overlap checks are zone-wide, and widen them if not
