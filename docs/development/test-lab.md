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

# Nested KVM test lab

A single Linux host runs a complete CloudStack zone in libvirt VMs: a
management server, three KVM hypervisors, a Ceph cluster and an external test
client. CloudStack's own guests (user VMs, virtual routers, system VMs, CKS
nodes) run nested inside the hypervisor VMs. The lab reproduces bugs that need
real hypervisors, real storage and real networking, and tests upgrades between
releases.

Everything here was tested with Ubuntu 24.04 on the host and in every lab VM.
The lab is disposable: it uses a single password everywhere and keeps its
networks host-only.

## Contents

- [Host requirements](#host-requirements)
- [Topology](#topology)
- [Build steps](#build-steps)
- [Lab reset](#lab-reset)
- [Upgrade testing](#upgrade-testing)
- [Fault injection](#fault-injection)
- [CKS extras](#cks-extras)
- [Gotchas](#gotchas)
- [Smaller options](#smaller-options)

## Host requirements

| Item | Tested value |
| --- | --- |
| OS | Ubuntu 24.04 |
| CPU | 128 vCPU, AMD EPYC |
| RAM | 122 GB, no swap |
| Disk | 1 TB, one filesystem; lab VM disks are thin qcow2 |
| Virtualization | libvirt, QEMU/KVM, nested virtualization on |
| Extra packages | `virtinst`, `cloud-image-utils`, `nginx`, `jq`, `chrony`, `libguestfs-tools`, Docker, `cmk` (CloudMonkey) |

The host itself can be a VM. The tested host was a KVM guest, so CloudStack's
guests ran three levels deep. Nested guests measured about 91% of the
hypervisor VM's CPU and memory throughput, 45% of its 4k random-read IOPS and
77% of its fsync write rate: enough for CKS nodes.

Check nested virtualization on the host, then inside each hypervisor VM:

```bash
cat /sys/module/kvm_amd/parameters/nested     # kvm_intel on Intel; expect 1 or Y
kvm-ok                                        # inside a hypervisor VM (package cpu-checker)
```

The VMs commit 114 GB of RAM. With no swap, the memory that nested guests
actually touch must stay about 3 GB below each hypervisor VM's RAM, or the
host's OOM killer stops a whole hypervisor. Allocated but idle guest memory is
fine.

## Topology

### Lab VMs

| VM | vCPU | RAM | Disk | Role |
| --- | --- | --- | --- | --- |
| `mgmt` | 8 | 12 GB | 200 GB | Management server, MySQL 8.0, NFS exports for secondary (and optional primary) storage |
| `kvm1`, `kvm2`, `kvm3` | 32 each | 30 GB each | 300 GB each | KVM hypervisors, one cluster |
| `ceph` | 8 | 10 GB | 40 GB + 3 x 100 GB | Single-node MicroCeph (squid), 3 OSDs, RBD pool for primary storage |
| `ext` | 2 | 2 GB | 20 GB | External client on the public network (IPv4 and IPv6): `scapy`, `hping3`, `nc`, `curl`, `iperf3`, `nmap`, `tcpdump` |

All VMs use `host-passthrough` CPU, virtio disks and NICs, and autostart.

### Bridges and traffic

| Host bridge | Address | Carries | Zone traffic type |
| --- | --- | --- | --- |
| `cloudbr0` | Lab gateway, NAT to the uplink | Management, public, storage (Ceph, NFS), untagged | Management and Public, KVM label `cloudbr0` |
| `cloudbr1` | None, no uplink | Guest VLAN trunk | Guest, KVM label `cloudbr1` |
| `cloudbr1.<vlan>` | Gateway of a shared network | One guest VLAN outside the zone's range | Shared network |

Each hypervisor VM has two NICs, one on each host bridge. Inside the VM,
netplan builds bridges with the same names: `cloudbr0` holds `eth0` and the
host's IP, and `cloudbr1` holds `eth1` with no IP. The other VMs have one NIC
on `cloudbr0`. The host runs NTP, NAT and an HTTP mirror on its `cloudbr0`
address.

### Address plan

The ranges are examples; any private range that is free on the host works.

| Range | Use |
| --- | --- |
| `172.16.0.1/16` | Host on `cloudbr0`: gateway, NTP, HTTP mirror, NAT |
| `172.16.0.10`-`.30` | Lab VMs |
| `172.16.1.10`-`172.16.1.250` | Pod range (system VM management IPs) |
| `172.16.10.1`-`172.16.11.250` | Public IPv4, untagged |
| `fd00:16::/64`, gateway `fd00:16::1` | Public IPv6, range `fd00:16::1:0`-`fd00:16::1:ffff` |
| `fd00:17::/56` | Zone guest IPv6 prefix |
| `10.200.0.0/16` | IPv4 block for routed-mode networks |
| `10.1.0.0/16` | Zone guest CIDR for isolated networks |
| VLAN 100-299 | Zone guest VLAN range |
| VLAN 60, `10.60.0.0/24` | Shared network, host as gateway |

IPv6 stays inside the lab: the host, `ext`, the virtual routers and the guests
reach each other, which is enough for ACL and firewall tests.

## Build steps

### 1. Host network

1. Create `cloudbr0` and `cloudbr1` as NetworkManager bridges with STP off and
   forward delay 0, plus the VLAN interface for the shared network.
2. Set `unmanaged-devices=interface-name:vnet*` in
   `/etc/NetworkManager/conf.d/` so NetworkManager leaves libvirt taps alone.
3. Turn on IPv4 and IPv6 forwarding in `/etc/sysctl.d/`.
4. Load an nftables table that masquerades the lab ranges (bridge, shared
   network, routed block) out of the uplink, from a oneshot systemd unit
   ordered after Docker.
5. Insert `ACCEPT` rules for the same ranges into Docker's `DOCKER-USER` chain.
6. Add `allow 172.16.0.0/16` to chrony.
7. Create a lab SSH key, an `~/.ssh/config` block for the lab VMs (user root)
   and `/etc/hosts` entries.

### 2. Lab VMs

1. Download the Ubuntu 24.04 cloud image once as a shared, read-only base.
2. For each VM, create a qcow2 overlay on the base with `qemu-img create -b`.
3. Write a cloud-init seed with `cloud-localds -N`:
   - netplan with a static address, fixed MACs and, on the hypervisors, the two bridges;
   - the lab SSH key and password for root, with `PermitRootLogin yes` and password SSH;
   - `chrony` pointed at the host, with `makestep 1 -1`, and `qemu-guest-agent`.
4. Create the VM with `virt-install --import --cpu host-passthrough --autostart`.
   Root disks use `cache=none,io=native`. The Ceph OSD disks use
   `cache=unsafe,io=threads,discard=unmap` and a `serial=` so they appear
   under `/dev/disk/by-id/`.
5. Wait for SSH and `cloud-init status --wait` on each VM.

### 3. Roles

| VM | Setup |
| --- | --- |
| `mgmt` | `mysql-server`, `nfs-kernel-server`, `openjdk-17-jre-headless` and the Python modules the setup scripts use. MySQL settings from the install guide (`server_id`, `innodb_rollback_on_timeout=1`, `innodb_lock_wait_timeout=600`, `max_connections=350`, ROW binlog). Root password authentication. NFS export of `/export` with `/export/secondary` and `/export/primary` |
| `kvm1`-`kvm3` | `qemu-kvm`, `libvirt-daemon-system`, `libvirt-daemon-driver-storage-rbd`, `ceph-common`, `openjdk-17-jdk-headless`, `nftables`, `tcpdump`. libvirtd on TCP 16509 with `--listen` and socket units masked; libvirt AppArmor profiles disabled; default libvirt network removed; `ufw` off; `8021q` loaded |
| `ceph` | `snap install microceph --channel=squid/stable`, bootstrap, three OSDs from the `by-id` disks. One host, so a CRUSH rule with failure domain `osd`. Pool `cloudstack`, size 3, min_size 2, 64 PGs, `rbd pool init`. Client `client.cloudstack` with `profile rbd` on that pool |
| `ext` | The probe tools in the VM table |

Copy `ceph.conf`, the `client.cloudstack` keyring and the admin keyring to
`/etc/ceph/` on every hypervisor, so `rbd` and `ceph tell` work there. Keep the
`client.cloudstack` key for the zone's primary storage URL.

### 4. Mirror and downloads

Serve a directory with nginx on the host's `cloudbr0` address, so the SSVM
downloads templates and ISOs at bridge speed. Add a systemd drop-in that
orders nginx after `network-online.target` and restarts it on failure.

| File | Source |
| --- | --- |
| System VM template, `systemvmtemplate-<x.y.z>-x86_64-kvm.qcow2.bz2` and `sha512sum.txt` | `https://download.cloudstack.org/systemvm/<x.y>/`; verify with `sha512sum -c` |
| CirrOS 0.6.3 and copies of it | CirrOS releases; separate copies for tests that need several templates |
| Ubuntu 24.04 test template | Built in a throwaway VM from the cloud image: `nginx`, `python3`, `stress-ng`, `iperf3`, `qemu-guest-agent`, then `cloud-init clean`, an empty `/etc/machine-id` and `qemu-img convert -c` |
| `cmk.linux.x86-64` | `apache/cloudstack-cloudmonkey` GitHub releases |
| CKS ISOs | [CKS extras](#cks-extras) |

### 5. Packages

Use released debs, or build them from a branch in an `ubuntu:24.04` container
so the host needs no toolchain. The tested image adds:

- `build-essential`, `devscripts`, `debhelper`, `lsb-release`, `genisoimage`, `jq`, `maven`;
- Node.js 22 from NodeSource;
- a JDK 11 (Temurin 11), `python3-mysql.connector`, `python3-setuptools`,
  `libws-commons-util-java`, `libcommons-codec-java`, `libcommons-httpclient-java`;
- `git config --system --add safe.directory '*'`.

```bash
docker run --rm --memory 24g --memory-swap 24g \
  -v "$HOME/build:/work" -v "$HOME/build/m2:/root/.m2" \
  -e ACS_BUILD_OPTS="-DskipTests -T 16" -e MAVEN_OPTS=-Xmx6g \
  -e NODE_OPTIONS="--openssl-legacy-provider --max-old-space-size=4096" \
  deb-builder bash -c 'cd /work/cloudstack && dpkg-checkbuilddeps &&
    cd packaging && ./build-deb.sh --brand lab.$(git rev-parse --short HEAD) -o /work/dist'
```

- Mount the parent of the source tree: `dpkg-buildpackage` writes there.
- `debian/rules` passes `ACS_BUILD_OPTS` to Maven. `-Dnoredist` adds the
  non-redistributable plugins and needs those libraries installed first.
- `debian/rules` builds with `-Dsystemvm` only, so the management deb bundles
  no KVM system VM template. `-Dsystemvm-kvm` in `ACS_BUILD_OPTS` bundles it.
- The lab installs five debs: `cloudstack-common`, `-management`, `-ui`,
  `-usage` and `-agent`.
- The container writes as root; `chown` the source tree and output afterwards.

### 6. Install

1. On `mgmt`, `apt-get install` the common, management and ui debs (plus usage if needed).
2. Run `cloudstack-setup-databases cloud:<pw>@localhost --deploy-as=root:<pw> -i <mgmt-ip>`.
3. Run `cloudstack-setup-management`.
4. On each hypervisor, `apt-get install` the common and agent debs. The agent
   configures libvirt and its TLS certificates when the host is added.
5. If `/usr/share/cloudstack-management/templates/systemvm/` holds no KVM
   template, seed secondary storage from the mirror:

```bash
/usr/share/cloudstack-common/scripts/storage/secondary/cloud-install-sys-tmplt \
  -m /export/secondary -u http://172.16.0.1/systemvm/<template>.qcow2.bz2 -h kvm -F -d <db-pw>
```

6. Wait for `http://<mgmt-ip>:8080/client/`.

### 7. Zone

1. Set global settings, then restart `cloudstack-management`:

| Setting | Value | Reason |
| --- | --- | --- |
| `host` | Management server IP | Address system VMs and agents connect to |
| `endpoint.url` | `http://<mgmt-ip>:8080/client/api` | API URL that CKS uses |
| `secstorage.allowed.internal.sites` | `172.16.0.0/16` | Lets the SSVM download from the private mirror |
| `kvm.snapshot.enabled` | `true` | Volume snapshots of running KVM VMs |
| `cloud.kubernetes.service.enabled` | `true` | CKS |
| `ipv6.offering.enabled` | `true` | IPv6 offerings |
| `expunge.interval`, `expunge.delay` | `60` | Fast cleanup between test runs |

2. Create the zone with `cmk`:

```bash
cmk create zone name=lab-zone networktype=Advanced dns1=<dns> internaldns1=<dns> \
    ip6dns1=fd00:16::1 guestcidraddress=10.1.0.0/16 securitygroupenabled=false
cmk create physicalnetwork zoneid=$Z name=phy-mgmt-public isolationmethods=VLAN
cmk add traffictype physicalnetworkid=$P1 traffictype=Management kvmnetworklabel=cloudbr0
cmk add traffictype physicalnetworkid=$P1 traffictype=Public kvmnetworklabel=cloudbr0
cmk create physicalnetwork zoneid=$Z name=phy-guest isolationmethods=VLAN vlan=100-299
cmk add traffictype physicalnetworkid=$P2 traffictype=Guest kvmnetworklabel=cloudbr1
```

3. Enable both physical networks. On each, enable the `VirtualRouter`,
   `VpcVirtualRouter` and `InternalLbVm` providers: `configure
   virtualrouterelement` or `configure internalloadbalancerelement`, then
   `update networkserviceprovider state=Enabled`.
4. Add the pod, the public ranges and the guest IPv6 prefix:

```bash
cmk create pod zoneid=$Z name=pod1 gateway=172.16.0.1 netmask=255.255.0.0 \
    startip=172.16.1.10 endip=172.16.1.250
cmk create vlaniprange zoneid=$Z physicalnetworkid=$P1 forvirtualnetwork=true vlan=untagged \
    gateway=172.16.0.1 netmask=255.255.0.0 startip=172.16.10.1 endip=172.16.11.250
cmk create vlaniprange zoneid=$Z physicalnetworkid=$P1 forvirtualnetwork=true vlan=untagged \
    ip6gateway=fd00:16::1 ip6cidr=fd00:16::/64 startipv6=fd00:16::1:0 endipv6=fd00:16::1:ffff
cmk create guestnetworkipv6prefix zoneid=$Z prefix=fd00:17::/56
```

5. Add the cluster, the hosts and storage, then enable the zone:

```bash
cmk add cluster zoneid=$Z podid=$POD clustername=cluster1 hypervisor=KVM clustertype=CloudManaged
cmk add host zoneid=$Z podid=$POD clusterid=$CL hypervisor=KVM url=http://172.16.0.11 \
    username=root password=<pw>                       # repeat per hypervisor
cmk create storagepool zoneid=$Z scope=zone hypervisor=KVM name=ceph-rbd provider=DefaultPrimary \
    url="rbd://cloudstack:<key>@172.16.0.20/cloudstack" tags=rbd
cmk add imagestore zoneid=$Z name=nfs-secondary provider=NFS url=nfs://172.16.0.10/export/secondary
cmk update zone id=$Z allocationstate=Enabled
```

6. Wait until `cmk list systemvms filter=name,state,agentstate` shows the SSVM
   and CPVM Running and Up.

The NFS export `nfs://<mgmt-ip>/export/primary` serves as a second primary pool
for cross-pool tests.

### 8. Verification

A smoke test exercises the hypervisors, Ceph, secondary storage and the
virtual router together:

1. Register CirrOS from the mirror (`format=QCOW2 hypervisor=KVM`) and wait for `isready=true`.
2. Create an isolated network from `DefaultIsolatedNetworkOfferingWithSourceNatService`.
3. Deploy a VM with `Small Instance`, and check that its root volume is on `ceph-rbd`.
4. Live-migrate it to a host from `cmk find hostsformigration`.
5. Snapshot its root volume.
6. Delete the snapshot, VM and network.

On the tested lab, deploy took about 45 s, live migration 4-7 s and the
snapshot under 5 s.

### 9. Snapshot

Save a crash-consistent copy of the lab after each install or upgrade:

1. `mysqldump --single-transaction --routines --triggers --databases cloud cloud_usage`.
2. Save `/etc/cloudstack` from every VM, `libvirtd.conf` and `qemu.conf` from
   the hypervisors, `virsh dumpxml --inactive` of every VM and a `cmk`
   inventory (hosts, system VMs, routers, VMs, storage pools).
3. `virsh suspend` every lab VM.
4. For each VM, take an external disk-only snapshot of every disk except the
   seed ISO:

```bash
virsh snapshot-create-as <vm> <label> --disk-only --atomic \
  --diskspec vda,snapshot=external,file=<dir>/root.<label>.qcow2 ...
```

5. `virsh resume` every VM, then confirm `virsh domstate` is `running` for each.
   Resume from a shell `trap`, so a failure never leaves the lab paused.

All VMs pause together for a few seconds, so the set is crash-consistent
across the database, Ceph and the hypervisors.

## Lab reset

| Reset | How |
| --- | --- |
| Reinstall | Stop the services, drop `cloud` and `cloud_usage`, and purge the packages and `/etc/cloudstack`. On each hypervisor, also remove the `i-`, `r-`, `s-` and `v-` domains, libvirt storage pools other than `default`, libvirt secrets and NFS mounts under `/mnt`. Empty the RBD pool (unprotect and purge snapshots, remove images, empty the trash) and the NFS exports. Then rerun steps 6 and 7. The install step refuses to run while the `cloud` database exists |
| Rebuild VMs | `virsh destroy`, `virsh undefine` and delete the VM's disk directory, then rerun steps 2 and 3. Host networking and the mirror survive |

## Upgrade testing

1. Install the old release (steps 6-8) and create the state the test needs.
2. Snapshot (step 9).
3. If the new management deb bundles no KVM system VM template
   (`dpkg-deb -c <deb> | grep 'templates/systemvm/.*x86_64-kvm'`), register the
   one the new release expects and wait for Ready before upgrading:

```bash
cmk register template zoneid=$Z name=systemvm-kvm-<x.y.z>-x86_64 displaytext=systemvm-kvm-<x.y.z>-x86_64 \
    format=QCOW2 hypervisor=KVM arch=x86_64 ostypeid=<debian-12-64-bit> \
    url=http://172.16.0.1/systemvm/systemvmtemplate-<x.y.z>-x86_64-kvm.qcow2.bz2 \
    checksum="{SHA-512}<hash>" isextractable=true ispublic=false passwordenabled=false
```

4. Back up `cloud` and `cloud_usage` with `mysqldump`.
5. Stop `cloudstack-usage` and `cloudstack-management`, install the new debs
   with `-o Dpkg::Options::=--force-confold`, and start the services.
6. Watch `management-server.log` for `Unable to upgrade the database` until the
   API answers.
7. Upgrade the agents one host at a time; wait for each host to be Up.
8. Destroy the SSVM and CPVM so they come back on the new template, and run
   `cmk upgrade routertemplate zoneid=$Z`.
9. Check `cmk list routers filter=name,version,requiresupgrade`, then rerun step 8.

Tested paths: 4.20.3.1 to 4.22.1.x to `main`, and 4.22.1.x to `main`. Each DB
upgrade took under 4 minutes, plus about 2 minutes for the system VMs.

## Fault injection

Run these on the host against one hypervisor VM.

| Fault | Command | Effect |
| --- | --- | --- |
| Storage unreachable | `ssh kvmN "nft add table inet labfault; nft 'add chain inet labfault out { type filter hook output priority 0; }'; nft add rule inet labfault out ip daddr <ceph-ip> tcp dport '{3300, 6789, 6800-7568}' drop"` | Ceph mon and OSD traffic dropped; `rbd_open` hangs |
| Slow storage | `tc qdisc add dev eth0 root handle 1: prio; tc qdisc add dev eth0 parent 1:3 handle 30: netem delay 2000ms; tc filter add dev eth0 protocol ip parent 1:0 prio 3 u32 match ip dst <ceph-ip>/32 flowid 1:3` | Latency on packets to Ceph only |
| Agent disconnect | `nft add rule inet labmgmt out ip daddr <mgmt-ip> tcp dport 8250 reject with tcp reset` (in a table and chain like the first row) | Agent loses the management server; host and VMs keep running |
| Network partition | `virsh domif-setlink kvmN <cloudbr0-mac> down` | Management, public and storage all drop; guest VLANs on `cloudbr1` stay up |
| Agent down | `systemctl stop cloudstack-agent` | Agent process stopped; host and VMs keep running |

Undo with `nft delete table inet <name>`, `tc qdisc del dev eth0 root`,
`domif-setlink ... up` or `systemctl start cloudstack-agent`.

Each hypervisor has `/etc/libvirt/hooks/`: an executable `qemu` hook there
takes effect after `systemctl restart libvirtd`. The JDK's `jstack` is
installed for thread dumps of the agent.

Routed-mode and IPv6 tiers need a host route to their virtual router's public
address, as the network's routes view shows:

```bash
sudo ip route add 10.200.1.0/24 via <vr-public-ip> dev cloudbr0
sudo ip -6 route add fd00:17:0:1::/64 via <vr-public-ipv6> dev cloudbr0
```

## CKS extras

| Item | Detail |
| --- | --- |
| Prebuilt ISOs | `https://download.cloudstack.org/cks/setup-v<k8s>-calico-x86_64.iso`; they ship `dashboard.yaml` |
| ISOs from source | `scripts/util/create-kubernetes-binaries-iso.sh OUTPUT_PATH KUBERNETES_VERSION CNI_VERSION CRICTL_VERSION NETWORK_YAML HEADLAMP_VERSION BUILD_NAME [ARCH]`, run on a host with containerd (`ctr`); `main`'s script ships `headlamp.yaml` |
| Checksums | Publish a `.sha256` next to each ISO |
| Registration | `cmk add kubernetessupportedversion zoneid=$Z semanticversion=<k8s> name=v<k8s> mincpunumber=2 minmemory=2048 url=<mirror-url> checksum="{SHA-256}<hash>"` |
| Node offering | 2 vCPU, 4 GB |
| Cluster | `cmk create kubernetescluster name=<n> zoneid=$Z kubernetesversionid=<id> serviceofferingid=<id> size=2 controlnodes=3 keypair=<key> noderootdisksize=20` |
| Node SSH | `ssh -i <key> -p 2222 cloud@<cluster-public-ip>`; port 2222+n reaches node n |
| etcd | Needs fsync p99 under 10 ms; on this lab RBD gets there only with `cache=unsafe` on the OSD disks |

Example ISO build, Kubernetes 1.33.13:

```bash
bash scripts/util/create-kubernetes-binaries-iso.sh ./out 1.33.13 1.9.1 1.33.0 \
  https://raw.githubusercontent.com/projectcalico/calico/v3.30.7/manifests/calico.yaml \
  0.40.1 setup-v1.33.13-calico x86_64
```

## Gotchas

| Symptom | Cause / fix |
| --- | --- |
| Every VM loses its network at once | `nmcli con up` on a live bridge detaches all taps; never re-activate it, and reattach taps with `ip link set <vnetN> master <bridge>` |
| Lab VMs lose egress after Docker starts | Docker sets `FORWARD` to DROP; add the lab ranges to `DOCKER-USER` |
| Docker networks collide with the lab range | Docker skips subnets that have a route; keep `cloudbr0` up, or pick a range outside Docker's pool |
| VMs fail to start after a disk snapshot and a host reboot | The snapshot's domain XML drops the shared base image from the backing chain, so AppArmor denies it; add `<base-image-path> rk,` under `/etc/apparmor.d/abstractions/libvirt-qemu.d/`, and check `/var/log/audit/audit.log` for `DENIED` first |
| The lab stays paused for many minutes during a snapshot | Internal snapshots with memory write at about 17 MB/s and can't be aborted; take external disk-only snapshots |
| Clocks are minutes off after a snapshot | VMs were paused; `makestep 1 -1` in chrony steps any offset over 1 s |
| etcd on CKS nodes misses its fsync target | RBD fsync p99 is 68 ms with `cache=none` on the OSD disks; use `cache=unsafe` there, acceptable in a disposable lab |
| `cloudstack-setup-databases --deploy-as=root:<pw>` fails | Ubuntu's MySQL root uses socket auth; set a root password with `caching_sha2_password` |
| The SSVM fails to download from the mirror | Private addresses are blocked by default; set `secstorage.allowed.internal.sites` |
| ISO fails with `checksum "{MD5}..." didn't match` | A checksum without a prefix is read as MD5; pass `{SHA-256}<hash>` |
| `KVM Snapshot is not supported for Running VMs` | Off by default; set `kvm.snapshot.enabled=true` |
| Upgrade fails with `Failed to register <x.y.z> templates for hypervisors: [kvm-x86_64]` | The pre-registered template name lacks the `-x86_64` suffix from `metadata.ini`; the schema steps are already committed, so restore the backup, rename the template and retry |
| `Database upgrade is required but the management server is running in a clustered environment` | The restored dump came from a running server and `mshost` still says Up; set that row's state to Down |
| New debs start without a KVM system VM template | `debian/rules` builds without `-Dsystemvm-kvm`; register the template before upgrading, or seed it with `cloud-install-sys-tmplt` on a fresh install |
| A hypervisor VM dies under memory overcommit | No swap on the host; keep touched guest memory under the hypervisor VM's RAM minus about 3 GB |
| A package build gets OOM-killed or starves the lab | Builds share the host's RAM; cap the container (`--memory`) and `MAVEN_OPTS`, and keep 30 GB free before starting |
| `libguestfs` can't install packages into an image | Its appliance had no working network on this host; build templates in a throwaway VM on `cloudbr0` |
| `cmk` prints `Failed to read API cache` | A new profile has no API cache; run `cmk sync` |

## Smaller options

API-level behavior needs no lab: the simulator runs from source
([building.md](./building.md#simulator-from-source)) or in Docker
([tools/docker/README.md](../../tools/docker/README.md)). A committer can also
start a Trillian run of the full smoke suites on real hypervisors for a PR.
