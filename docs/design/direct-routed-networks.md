# Direct Routed Networks

**Status:** design agreed — open items are implementation and verification only
**Branch:** `direct-routed-network`
**Author:** Wido den Hollander
**Last updated:** 2026-07-30

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
| Isolation ID | VLAN/VXLAN | VLAN/VXLAN | VLAN/VXLAN | **none** |

What this closes:

* **No VR in the data path.** `ROUTED` networks (4.20) removed NAT but kept a VR in the path and an
  L2 segment per network. Throughput, failover, and per-network VR footprint remain concerns.
* **No shared broadcast domain between networks.** Each network gets its own uplink-less bridge
  (§9.2), so there is no L2 path of any kind between networks — no ARP spoofing, no rogue DHCP
  server, no rogue RA reaching another tenant. Isolation is topological, so it does not depend on a
  rule set being correct, and an administrator can turn security groups off for a network without
  weakening it. Within a single network guests still share a bridge; that residual exposure is
  intra-tenant and is documented in §12.3.
* **No isolation id at all.** No VLAN, no VXLAN, no encapsulation, nothing to allocate or size — the
  bridge is named from the network's own database id (§9.2.1). Guest network count is bounded by no
  ID space, and each network still gets a real L2 boundary.
* **Per-network routing policy on the hypervisor.** Each network is a distinct, named L3 interface
  (`brdr-42`), so the operator can apply different route-maps, redistribution filters, policy
  routing or QoS per network in the host's own configuration. CloudStack does not need to know or
  care; it is entirely a local network design decision (§9.2).
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
| `brdr-<id>` | Bridge-DirectRouted: the uplink-less per-network bridge, named from the network's database id; created by `modifybrdr.sh` |
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

Like a **Shared** network, not like L2: the network has a subnet. The operator supplies the CIDR,
and optionally an IPv6 prefix, via the usual IP-range mechanism (`vlan` rows — `vlan_gateway`,
`vlan_netmask`, `ip4_range`, `ip6_gateway`, `ip6_cidr`, `ip6_range` in
`engine/schema/src/main/java/com/cloud/dc/VlanVO.java`).

The difference is what CloudStack does with it: the subnet is an **allocation pool that is routed to
the hypervisors**, not a broadcast domain. Guests never see the subnet mask or its gateway.

**The subnet gateway is required, and ignored. DECIDED for v1.**

`vlan_gateway` / `ip6_gateway` are meaningless for this network type — nothing reads them, because
the guest's gateway is always the shared link-local address and the whole subnet is routed to the
hosts. `createVlanIpRange` requires a gateway today, and it stays required: relaxing it would mean
touching validation shared with every other network type for no functional gain.

The cost is a small wart — the operator has to nominate an address in the subnet that will never be
configured anywhere or answer anything. Two practical notes:

* It should be **documented as unused**, so nobody wastes time debugging why traffic is not reaching
  it, and so nobody assumes reserving it is necessary.
* Whichever address is given still gets consumed from the allocation pool unless explicitly kept
  out of the IP range. Since every address in the subnet is otherwise usable (§6.3.1), an operator
  can simply give the range as the full subnet and point the gateway at an address inside it — but
  confirm existing validation does not reject a gateway that falls within the range.

Making it optional remains available later if it proves annoying; it is a validation change, not a
model change, so nothing here forecloses it.

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

Users add a subnet to the network; CloudStack picks **individual** IPv4 and IPv6 addresses from the
existing address tables:

* IPv4 → `user_ip_address` (`engine/schema/src/main/java/com/cloud/network/dao/IPAddressVO.java`)
* IPv6 → `user_ipv6_address` (`engine/schema/src/main/java/com/cloud/network/UserIpv6AddressVO.java`)
* Subnet definition → `vlan` rows, as for Shared networks

This is the mechanism Shared networks already use: `DirectNetworkGuru.allocateDirectIp()`
(`server/src/main/java/com/cloud/network/guru/DirectNetworkGuru.java:316`) →
`IpAddressManagerImpl.allocateDirectIp()`
(`server/src/main/java/com/cloud/network/IpAddressManagerImpl.java:2434`).

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
| /29 | 8 | 5 | 7 | +40% |
| /28 | 16 | 13 | 15 | +15% |
| /27 | 32 | 29 | 31 | +7% |
| /24 | 256 | 253 | 255 | +0.8% |

(The gateway is still deducted because §5.5 keeps it required, even though nothing uses it.)

**Verified during implementation: no code change was needed.** The earlier draft assumed the
exclusion lived in the `NetUtils` CIDR helpers (`getIpRangeFromCidr()` and friends, whose
`start = (ip & netmask) + 1` / `end - 2` arithmetic does skip the two addresses) and would need
inclusive variants. Tracing the actual path an operator-supplied range takes showed those helpers
are only used where CloudStack *derives* a range from a CIDR — Isolated networks. The explicit
start/end path this network type uses (Shared-style `createVlanIpRange`) validates with
`sameSubnet()` — a plain bitmask comparison that `.0` and `.255` pass — plus start≤end and
gateway-not-in-range, and `savePublicIPRange()` then iterates the range without exclusions into
`user_ip_address`. Allocation is row-based from that table and never re-derives from the CIDR.

**`.0` and `.255` are therefore already assignable end-to-end on this path.** The smoke test should
still assert it (an Instance actually receiving `.0` or `.255` and working), since this rests on
tracing rather than an existing guarantee anyone maintains.

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
* **Implemented and verified:** the IPv6 vlan overlap check was already zone-wide
  (`_vlanDao.listByZone()`). IPv4 was not — the `user_ip_address` unique key is
  `(public_ip_address, source_network_id)`, i.e. per network — so `createVlanAndPublicIpRange` now
  calls the existing zone-wide `checkOverlapPublicIpRange()` for L3 networks. Shared networks keep
  their historical behaviour, where the same IPv4 range in two VLANs is legitimate.
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
* `specifyVlan` → `false`; there is no isolation id of any kind to specify (§9.2.1)
* `NetworkMode` → not applicable; reject `NATTED`/`ROUTED` for this guest type

**Why DNS is recommended rather than mandatory.** There is no VR and no resolver on the host, so
`network_data.json` `services` is the only channel by which CloudStack can tell a guest its DNS
servers (§8.3). An offering without it leaves Instances with addresses and routing but no name
resolution — unless the template already carries resolvers, which is unusual but legitimate and the
operator's call (§6.5). Note this depends on the §8.2 gate being fixed first.

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

**This has a hard prerequisite — see §8.2.** `ConfigDriveBuilder.needForGeneratingNetworkData()`
currently writes network data only when the network supports `Dhcp` **or** `Dns`. Since this type
never has `Dhcp`, an offering without `Dns` would today produce an **empty `network_data.json` and an
Instance with no addressing at all** — a far worse outcome than missing resolvers. Making `Dns`
optional therefore requires changing that gate first; it is not merely a validation relaxation.

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

### 6.7 What makes a network direct routed **DECIDED**

**The network offering, and nothing else.** There is no broadcast domain type, no isolation method,
no id to allocate, and nothing for the user to choose — the same shape as an L2 network, where the
offering's guest type is the whole story.

* **Guru selection is on guest type alone.** `canHandle()` tests the zone type, `isMyTrafficType()`
  and `offering.getGuestType() == GuestType.L3`. It deliberately does **not** call
  `isMyIsolationMethod()`, unlike its siblings
  (`DirectNetworkGuru.java:147`, `VxlanGuestNetworkGuru.java:56`), because there is no isolation to
  select — no VLAN, no VXLAN, no encapsulation of any kind. `GuestType.L3` is new, so no other guru
  claims it and there is no ambiguity to resolve.
* **No isolation method to register.** An earlier draft proposed `new IsolationMethod("ROUTED")`.
  Dropped: it would force the operator to add an isolation method to the physical network for no
  benefit, since nothing would ever be selected by it.
* **No broadcast domain type.** An earlier draft proposed `routed://<id>` as a new
  `BroadcastDomainType`. Also dropped — see §9.2.1 for how the bridge name is derived instead, which
  needs no new plumbing at all.
* **No id for the user to pick.** The bridge is named from the network's own database id, which is
  unique by construction and requires no allocation, no range to configure, and no `specifyVlan`
  handling.

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

### 6.8 Hypervisor support

**KVM only for v1.** The host must program routes and neighbour entries and run a routing daemon;
that is only realistic where the host OS is ours to configure. Other hypervisors: reject at network
creation with a clear error.

## 7. API and data model changes

### 7.1 API

* `createNetworkOffering` — accept the new `guestiptype`; validate the service matrix (§6.4).
* `createNetwork` — accept the subnet as for Shared networks; `createVlanIpRange` gateway handling
  per §5.5; zone-wide overlap validation per §6.3.2.
* `listNetworks` / `NetworkResponse` — expose the type. The network's CIDR is meaningful (it is the
  pool) and should be shown; its gateway is not.
* `listNics` / `NicResponse` — report the /32 and /128 plus the shared gateway. `netmask` is
  already a dotted quad, so `255.255.255.255` needs no schema change.
* `listPublicIpAddresses` — works as for Shared networks.
* New APIs: **none.**

### 7.2 Database

* `network_offerings.guest_type` — new enum value (`L3`).
* `nics` — all needed columns exist: `ip4_address`, `netmask`, `gateway`, `ip6_address`,
  `ip6_cidr`, `ip6_gateway` (`engine/schema/src/main/java/com/cloud/vm/NicVO.java:55-108`).
* `user_ip_address`, `user_ipv6_address`, `vlan` — reused unchanged.
* Upgrade: new enum value only. No data migration.

### 7.3 New components

* **Network guru — a subclass of `DirectNetworkGuru`. DECIDED.** `DirectNetworkGuru` already
  implements "operator defines a subnet, CloudStack assigns individual v4 and v6 addresses", which
  is exactly the allocation behaviour wanted, so the allocate/release lifecycle is inherited rather
  than duplicated. The subclass overrides:
  * `canHandle()` — accept `GuestType.L3`, and drop the `isMyIsolationMethod()` test (§6.7)
  * the `NicProfile` after allocation — force `255.255.255.255` / `169.254.0.1` and `/128` /
    `fe80::1` over the vlan row's values (`IpAddressManagerImpl.allocateDirectIp()` lines 2459–2461,
    §6.3)
  * `design()` — no broadcast domain or isolation to assign (§9.2.1)

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

**Resolved — verified against cloud-init: no on-link plumbing is needed at all.**

Two findings settled this, one from code and one from testing:

1. The ISO is labelled `config-2` (`VirtualMachineManager.VmConfigDriveLabel`), so cloud-init
   selects its **OpenStack/ConfigDrive datasource**, which takes network configuration from
   `openstack/latest/network_data.json`. A `network-config` file (Network Config v2) is only read
   by the NoCloud datasource (label `cidata`) and would be ignored on the default boot path.
2. **Verified by Wido:** when consuming `network_data.json`, cloud-init itself detects an IPv4
   gateway inside `169.254.0.0/16` and sets the on-link flag on the route it renders. The existing
   v1 emission — a plain default route via the gateway — therefore works unchanged.

So ConfigDrive's route generation is **not modified at all**. What did change (§8.2) is only the
gate: network data is now always generated for a direct routed NIC, since ConfigDrive is its only
addressing channel.

Emitting a Network Config v2 `network-config` file for NoCloud-configured images was implemented
and then removed — deferred to a **later PR** (§15). A v1 host-route-to-the-gateway workaround was
likewise removed as redundant: cloud-init handles the case natively, and extra routes with a
`0.0.0.0` gateway are the kind of thing that can confuse a renderer.

The guest-side requirement is accordingly not "Network Config v2 support" but **a cloud-init recent
enough to apply on-link for link-local gateways from OpenStack network data** — which the smoke
test must pin down to a minimum version for the documentation.

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

Notes and follow-ups:

* The on-link trigger — the gateway matching `169.254.0.0/16` — lives in **cloud-init**, not in
  CloudStack. It is a property of the address, so it stays correct however the guest receives it.
* IPv6 needs no `on-link` — `fe80::1` is link-local by definition and always on-link.
* **The guest requirement is a cloud-init recent enough to set on-link for link-local IPv4
  gateways when consuming OpenStack network data.** Guests whose cloud-init predates that are not
  supported on this network type, in the same way that guests without ConfigDrive are not (§6.5).
* **TODO:** verify against the images used in the smoke tests, and state the minimum cloud-init
  version in the documentation so the requirement is visible to operators rather than discovered at
  boot.

### 8.2 network_data generation is gated on DHCP or DNS **REQUIRED CHANGE**

```java
static boolean needForGeneratingNetworkData(Map<Long, List<Network.Service>> supportedServices) {
    return supportedServices.values().stream()
        .anyMatch(services -> services.contains(Network.Service.Dhcp)
                           || services.contains(Network.Service.Dns));
}
```
(`engine/storage/configdrive/src/main/java/org/apache/cloudstack/storage/configdrive/ConfigDriveBuilder.java:266`,
called from `writeNetworkData()` at `:252`.)

If neither service is supported, `writeNetworkData()` writes an empty `{}` and **the guest receives
no network configuration whatsoever** — no address, no netmask, no gateway, no routes.

That gate is wrong for this network type. It equates "does this network have DHCP or DNS?" with
"does this NIC need its addressing written into ConfigDrive?", which held while ConfigDrive was a
supplement to a VR but does not hold when ConfigDrive is the *only* channel. This type never has
`Dhcp`, and §6.5 makes `Dns` optional, so the two conditions can both be false while the NIC still
very much needs its /32 written.

**Required:** extend the condition so network data is generated whenever the NIC belongs to a direct
routed network — or, more generally, whenever the NIC has an address to convey and no other means of
conveying it. Until that lands, a `Dns`-less offering is not merely degraded, it is non-functional.

Worth a code comment either way, because the coupling is invisible from the offering side.

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

Note the interaction with §6.5: DNS is optional on the offering. If the offering omits the `Dns`
service, these values are never written to ConfigDrive regardless of being configured on the network
or zone.

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

#### 9.1.3 Gating is per NIC, inferred **DECIDED**

`vm.network.macip.static` is resolved once in `configure()` (`BridgeVifDriver.java:87`) and is
all-or-nothing for the host. A host runs direct routed guests *and* ordinary bridged guests side by
side, so the behaviour is decided **per NIC**, inferred from what the `NicTO` already carries — not
from a host property, and without adding a flag.

**What the agent can actually see.** `NicTO`/`NetworkTO` expose `broadcastType`, `type`
(TrafficType), `networkId`, `gateway`, `netmask`, `ip6Cidr` and `securityGroupEnabled`
(`api/src/main/java/com/cloud/agent/api/to/NicTO.java`,
`api/src/main/java/com/cloud/agent/api/to/NetworkTO.java`). Two things it does **not** carry:

* **Guest type is not on the TO at all.** There is no `GuestType` field, so `GuestType.L3` cannot be
  tested directly on the agent.
* **Broadcast type is ambiguous.** Since §9.2.1 dropped the broadcast domain, these NICs are
  `BroadcastDomainType.Native`, which is shared with other untagged cases.

**The signature to infer from** is therefore the addressing itself, which no other CloudStack network
type produces: an IPv4 netmask of `255.255.255.255` (and/or an IPv6 `/128`) together with a gateway
inside `169.254.0.0/16`. Shared and Isolated networks always hand out a real subnet mask; L2 hands
out none. This is the same key §8.1 uses to decide the ConfigDrive `on-link` rule, so the two stay
consistent by construction.

**One inference, used twice.** The same test decides both which bridge to use (`brdr-<networkId>`,
§9.2.1) and whether to run the MAC/IP hook. They are not separate decisions — if the driver has
chosen a `brdr-` bridge, the hook applies.

The agent property `vm.network.macip.static` stays as an independent host-wide opt-in for the EVPN
use case and is unaffected.

**Caveat, worth stating:** this is an implicit contract. Nothing stops a future change from handing a
guest a /32 for some other reason and silently activating this path. If that becomes a real risk, the
explicit alternative is a boolean on `NicTO` set by the guru — a small change, deliberately not taken
now because it adds plumbing for a case that does not yet exist.

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
Instances**, named `brdr-<id>` (Bridge-DirectRouted) — for example `brdr-42`.

The `<id>` is simply `networks.id`, the network's numeric database id — unique by construction, with
nothing to allocate and nothing for the user to choose (§9.2.1).

The bridge is created and removed by a new script,
`scripts/vm/network/vnet/modifybrdr.sh`, modelled on `modifyvxlan.sh`:

```
modifybrdr.sh -o add    -n <network id> [-4 <ipv4 gateway>] [-6 <ipv6 gateway>]
modifybrdr.sh -o delete -n <network id>
```

On `add` it creates the bridge if absent (STP off, `forward_delay 0` — there is no uplink, so no
loop to detect and no reason to hold ports down at Instance start), enables IPv4/IPv6 forwarding on
it, disables RA acceptance, and configures the gateway addresses. On `delete` it removes the bridge,
but only after confirming nothing is still attached — an Instance may have started on the network
while the last one was stopping. The whole script runs under `flock`, like `modifyvxlan.sh`, because
concurrent Instance starts on one network will race to create the bridge.

Consequences:

* **Networks are isolated from each other at layer 2 by the topology**, not by filtering. This is
  the first reason for the design: a guest on `brdr-42` has no L2 path of any kind to a guest on
  `brdr-43`, and no rule set has to be correct for that to hold (§12).
* **Each network becomes a named L3 interface on the hypervisor.** This is the second reason, and it
  is an operational one: `brdr-42` is something the operator can attach local policy to. Different
  route-maps or redistribution filters per network, per-network policy routing, QoS, or later a VRF
  per bridge — all expressible in the host's own network configuration, matched on interface name,
  with no involvement from CloudStack. The routing daemon is already the operator's (§10); giving
  each network its own interface is what makes per-network routing decisions possible at all.
* **Bridge name is identical on every host** — it derives only from the network id — which is what
  keeps migration a no-op for the guest.
* **The vif driver now needs work.** The earlier shared-bridge design could ride on
  `BridgeVifDriver`'s existing `brname = trafficLabel` fallback
  (`plugins/hypervisors/kvm/src/main/java/com/cloud/hypervisor/kvm/resource/BridgeVifDriver.java:255`);
  that no longer applies. The driver must derive `brdr-<network id>` from `NicTO.getNetworkId()` and invoke
  `modifybrdr.sh` on plug, and on unplug when the last interface leaves — the same shape as its
  existing `createVnetBr()` handling for VXLAN.

#### 9.2.1 How the agent learns the bridge name **DECIDED**

**`NicTO` already carries the network id.** `NicTO.networkId` (`api/src/main/java/com/cloud/agent/api/to/NicTO.java:35`)
is populated for every NIC by `HypervisorGuruBase.toNicTO()`
(`server/src/main/java/com/cloud/hypervisor/HypervisorGuruBase.java:208`,
`to.setNetworkId(profile.getNetworkId())`).

`BridgeVifDriver` passes the network id to `modifybrdr.sh`, which creates the bridge and **prints
the name it chose** — the agent uses whatever comes back. On unplug the agent asks the same script
(`-o delete -b <name>`), which answers `notmine`, `kept` or `deleted`; `notmine` sends the agent
down its regular unplug path. **How the bridges are named is known only to the script**; no
`brdr-` prefix appears anywhere in Java.

That is the entire mechanism. No new `BroadcastDomainType`, no broadcast URI, no isolation method, no
id allocation, no new field on any TO — and nothing the user has to choose. The id is unique by
construction because it is the network's primary key.

Earlier drafts proposed a `routed://<id>` broadcast domain with an operator-choosable id allocated
from the physical network's VNET range. Dropped: it added an allocation mechanism, a range to
configure, and a choice to make, all to convey a number the agent already has.

Consequences:

* The network's `broadcast_uri` stays empty and its broadcast domain type is `Native`. Nothing is
  allocated, so nothing has to be released on network deletion.
* `specifyVlan` is `false` and stays that way (§6.4).
* An operator who wants `brdr-42` to be a *specific* number cannot have it. Bridge names are
  discoverable from the network's id in the UI and API, so per-network host routing policy (§9.2) is
  still perfectly writable — it just has to be written after the network exists rather than chosen
  in advance.

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
  so a request reaching `brdr-42` is never answered on behalf of `brdr-43`
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

Separate bridges per network are the isolation mechanism. A guest on `brdr-42` cannot send a frame of
any kind — ARP, raw L2, rogue RA, anything — to a guest on `brdr-43`. There is no shared broadcast
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

**IPv4 only.** The kernel has no IPv6 `rp_filter`, so IPv6 source spoofing is not bounded by this and
must be handled by security groups (§12.2) where it matters.

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

* **Ordering.** Install-then-remove is safer than remove-then-install; a transient duplicate
  advertisement is less harmful than a black hole. Confirm the plug/unplug sequence during
  migration actually gives that ordering.
* **Convergence gap.** Traffic may be black-holed until the fabric reconverges. **TODO:** quantify
  on a normal iBGP/OSPF setup — is sub-second realistic?
* **No GARP needed.** Normally a migrating VM sends a gratuitous ARP to update switch tables. Here
  there is no L2 path to update, and the destination host's neighbour entry is installed statically
  by the agent rather than learned — so this problem disappears. Verify.
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

One path cannot receive the flag: `network_rules_for_rebooted_vm` runs without the Agent. It
restores dispatch rules in whichever form the Instance's per-VM chain already uses (destination
ipset for Direct Routed, `physdev-out` for bridged), so a rebooted Instance is reprogrammed
correctly either way.

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
operator who disables security groups accepts intra-tenant spoofing between guests of that one
network — a deliberate operator choice; isolation between *tenants* is topological and unaffected
(§12.1). The host itself is immune either way: its neighbour entries are static (§5.3).

### 12.4 Sharp edge

**Guests are directly reachable from the fabric.** No NAT, no VR firewall. Whatever the fabric
permits reaches the guest, subject only to the guest's security groups if they are in use. This is a
meaningful change in default posture versus an Isolated network and must be prominent in the
documentation.

## 13. Orchestration touchpoints

Checklist to work through:

- [ ] `NetworkOrchestrator.allocate()` / `prepare()` / `release()` — address lifecycle
- [ ] `NetworkOrchestrator` and `VirtualMachineManagerImpl` — `L2` and `Shared` branches
- [ ] `UserVmManagerImpl` — `addNicToVm`, `updateDefaultNic`, IP change
- [ ] `NetworkModelImpl` — capability lookups, `getNetworkTag`, `isSecurityGroupSupportedInNetwork`
- [ ] `IpAddressManagerImpl.allocateDirectIp()` — gateway/netmask override for /32 and /128
- [ ] `BridgeVifDriver` — infer direct-routed from the /32 + link-local gateway; gate the MAC/IP
      hook and bridge selection on it. Script and bridge targeting reused unchanged (§9.1.3)
- [ ] `scripts/vm/network/vnet/modifybrdr.sh` — per-network bridge lifecycle (§9.2)
- [ ] `BridgeVifDriver` — derive `brdr-<nic.getNetworkId()>`, call `modifybrdr.sh` on plug and last unplug
- [ ] New guru with `canHandle()` on guest type alone, no isolation method (§6.7)
- [ ] Offering validation — require ConfigDrive `UserData`, reject `Dhcp` (§6.4)
- [ ] `ConfigDriveBuilder.needForGeneratingNetworkData()` — must not gate on Dhcp/Dns (§8.2)
- [ ] `NetworkServiceImpl.java:657` — stop rejecting DNS for this type as it does for L2 (§6.4)
- [ ] `security_group.py` unified rules — verify on a real host that both directions match live
      traffic on classic and Direct Routed bridges, and that ARP for `169.254.0.1` / ND for
      `fe80::1` pass (§12.2)
- [ ] `createVlanIpRange` — zone-wide subnet overlap validation (§6.3.2)
- [ ] `NetUtils` — inclusive range variants so `.0` and `.255` are assignable (§6.3.1)
- [ ] VM snapshot / restore, VM import (`UnmanagedVMsManagerImpl`), template creation
- [ ] Network restart — no-op without a VR?
- [ ] IP capacity reporting and usage records — is a directly routed address a billable public IP?
- [ ] UI: network creation wizard, network detail page, NIC display, offering creation

## 14. Upgrade and compatibility

* Additive: new enum value, new offering type. No change to existing networks.
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
* Making a failed route/neighbour install fatal or alert-raising, rather than silent (§9.1.4)
* Closing the §12.3 intra-network spoofing gaps — bridge port isolation (§9.4) first, or libvirt
  nwfilter if a narrower fix is preferred
* **Per-tenant VRFs**, which would lift the non-overlapping-subnet constraint of §6.3.2

## 16. Decision log and open questions

### Settled

* Network type is "no DHCP", not "like L2" (§1)
* Addresses come from `user_ip_address` / `user_ipv6_address` with an operator-supplied subnet
  (§6.3); subnets must not overlap zone-wide (§6.3.2)
* New `GuestType.L3`, chosen over overloading `Shared`/`NetworkMode` (§6.1)
* Gateway is a static, non-configurable `169.254.0.1` / `fe80::1`; a /32 means the guest must treat
  its gateway as on-link regardless, so configurability would buy nothing (§6.2)
* ConfigDrive emits `on-link: true` for gateways in `169.254.0.0/16` (§8.1)
* The agent writes only routes and neighbour entries; FRR is out of scope (§9.1, §10)
* That work is done by reusing `modifymacip.sh` + the `BridgeVifDriver` hook already in main from
  `4816e059383` / PR #13495, rather than new code (§9.1.1)
* Routes and neighbour entries go on the **bridge**, not the tap — the script and hook are reused
  unchanged (§9.1.2)
* **One bridge per network**, named `brdr-<networks.id>`, created and removed by the new
  `scripts/vm/network/vnet/modifybrdr.sh` (§9.2)
* No broadcast domain, no isolation method, no id allocation and nothing for the user to choose —
  the agent already has the network id on `NicTO` (§9.2.1)
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
* The **offering alone** determines that a network is direct routed, on guest type — as for an L2
  network. Changing a network's offering afterwards is not guarded (§6.7, §6.7.1)
* The offering requires ConfigDrive `UserData`; `Dns` is optional but strongly recommended, and
  `SecurityGroup` is optional. `Dhcp` is rejected — not just unsupported but unnecessary (§6.4, §6.5)
* `rp_filter=1` is set on each `brdr-*` bridge, bounding IPv4 source spoofing to the Instance's own
  address; IPv6 has no kernel equivalent (§9.5)
* **No reconciliation at agent startup** — a host reboot destroys the Instances too, and an agent
  restart does not clear kernel state (§9.6)
* The MAC/IP hook is gated **per NIC, inferred** from the /32 + link-local-gateway signature rather
  than a host property or a new `NicTO` flag; the same test picks the bridge (§9.1.3)
* The same `169.254.0.1` on every `brdr-*` bridge is **correct as designed**; `arp_ignore=1` and
  `arp_announce=2` handle it (§9.2.2)
* The guru is a **subclass of `DirectNetworkGuru`**, inheriting the address lifecycle rather than
  duplicating it (§7.3)
* **No on-link plumbing in ConfigDrive** — cloud-init itself sets on-link for an IPv4 gateway in
  `169.254.0.0/16` when consuming `network_data.json` (verified); the v2 `network-config` file is
  deferred to a later PR (§8.1, §15)
* DNS is **per network, falling back to the zone**; already implemented by
  `NetworkModelImpl.getNetworkIp4Dns()` (§8.3)
* **No metadata service** in v1 — ConfigDrive only, no `169.254.169.254`; a v2 candidate (§8.4)
* Route/neighbour install failures **stay silent** for v1, as they are for EVPN (§9.1.4)
* Route scale is **out of scope** — fabric capacity and aggregation are local network design;
  ~100k routes is not usually a problem on modern equipment, but CloudStack states no ceiling (§6.3.3)
* Network and broadcast addresses **must be assignable** — verified already true on the explicit
  start/end range path; the `NetUtils` exclusion only affects CIDR-derived Isolated ranges (§6.3.1)
* The subnet's gateway stays **required and ignored**; relaxing validation shared with other
  network types is not worth it for v1 (§5.5)
* The `--physdev-is-bridged` rework **lands in v1**; security groups are not shipped half-working
  (§12.2)
* Zone-wide subnet overlap validation is **required** — an overlap is an address conflict (§6.3.2)
* **No warning** when a template ignores ConfigDrive; cloud-init inside the guest is the operator's
  responsibility (§6.5)
* **Agent version gating is out of scope** — operators upgrade agents with CloudStack (§14)
* KVM only for v1 (§6.8); **VPC never** — it is a separate use case already served by VPC's own
  BGP-routed subnets (§6.6)

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
