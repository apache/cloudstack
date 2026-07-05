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

# VMware CBT migration to KVM

This document describes the proposed VMware Changed Block Tracking (CBT)
migration flow for Apache CloudStack and the host-level prerequisites required
on KVM migration hosts.

The goal of this flow is to reduce the downtime and bandwidth needed when
migrating VMware guests to KVM. Instead of repeatedly copying the full VMware
disk, CloudStack performs one initial full synchronization and then uses VMware
CBT to copy only changed disk ranges until the VM is quiet enough for final
cutover.

No additional CloudStack software is installed on the VMware ESXi hosts or
vCenter for this flow. The extra migration tooling is installed on the selected
CloudStack KVM host that performs the VDDK read, virt-v2v conversion, and CBT
delta application work.

## Current implementation status

The current implementation contains the CloudStack management model, API
surface, state tracking, cutover policy, VMware changed-block query
orchestration, KVM agent initial sync, KVM agent delta sync, cutover
finalization, import retry handling, delete/cancel cleanup, and UI status
tracking.

## High-level flow

1. Start a VMware CBT migration session.
   CloudStack records the destination zone, KVM cluster, optional conversion
   host, optional storage pool, source vCenter/ESXi details, and discovered
   VMware disks.

2. Run the initial full sync.
   The initial full copy should use the existing VMware-to-KVM VDDK and
   virt-v2v workflow. The result must be one target disk per VMware source disk
   on KVM primary storage.

3. Register target disks.
   The management server records the target disk path, format, initial VMware
   CBT change ID, and snapshot reference for each migrated disk.

4. Run CBT delta synchronization cycles.
   For each cycle CloudStack creates a VMware snapshot, calls
   `QueryChangedDiskAreas()` for the source disks, dispatches changed byte
   ranges to the selected KVM host, records copied bytes and dirty rate, and
   removes the VMware snapshot.

5. Decide when to cut over.
   The decision is controlled by global settings:

   * `vmware.cbt.migration.min.cycles`
   * `vmware.cbt.migration.max.cycles`
   * `vmware.cbt.migration.quiet.cycles`
   * `vmware.cbt.migration.quiet.bytes`
   * `vmware.cbt.migration.quiet.dirty.rate`

   Long-running KVM agent commands are controlled separately by:

   * `vmware.cbt.migration.agent.command.timeout`

6. Final cutover.
   After the operator powers down the VMware VM, CloudStack performs the final
   delta sync and imports/registers the VM on KVM.

## Long-running operation timeout

CBT initial full sync, regular delta sync, final delta sync, and cutover
finalization run as KVM agent commands. The command wait value and the
agent-side child process timeout are both controlled by:

```text
vmware.cbt.migration.agent.command.timeout=86400
```

The value is in seconds. The default is 24 hours. It is intentionally separate
from `convert.vmware.instance.to.kvm.timeout`, which applies to the older
OVF/VDDK `ConvertInstanceCommand` import path, and from
`remote.kvm.instance.disks.copy.timeout`, which applies to remote KVM unmanaged
disk copy imports.

For very large source disks, set
`vmware.cbt.migration.agent.command.timeout` above the worst-case expected
duration. A 1 TiB initial sync can easily require multiple hours depending on
VDDK transport, ESXi storage, destination storage, and sparse extent behavior.
The setting is finite because the shared CloudStack script runner treats zero
as its default one-hour timeout, not as "no timeout".

## KVM migration host selection

CBT migration runs on a KVM host in the destination cluster. If the API call
does not specify a conversion host, CloudStack selects an enabled KVM routing
host in the destination cluster that reports:

```text
host.vmware.cbt.support=true
```

The KVM agent reports this capability during host startup. It is true only when
the host satisfies the required VDDK, virt-v2v, qemu-img, and qemu-nbd checks.

Related host detail keys:

```text
host.vddk.support
vddk.lib.dir
host.vddk.version
host.vmware.cbt.support
host.qemu.img.version
host.qemu.nbd.version
host.virtv2v.version
```

Restart `cloudstack-agent` after installing packages or changing
`agent.properties` so these details are refreshed on the management server.

## Required host capabilities

Each KVM host that can be selected for VMware CBT migration needs:

* A normal CloudStack KVM agent installation.
* `virt-v2v` for the initial VMware-to-KVM conversion and final conversion
  steps.
* `nbdkit` and an `nbdkit` VDDK plugin.
* VMware VDDK libraries, with a directory containing
  `lib64/libvixDiskLib.so`.
* `qemu-img` for target image inspection, creation, and manipulation.
* `qemu-nbd` for block device exposure during incremental copy workflows.
* Network access from the KVM host to vCenter and ESXi hosts.
* Access to the destination KVM primary storage pool.
* `virtio-win` drivers when migrating Windows guests.

You do not have to install these packages on every KVM host in the zone unless
you want every KVM host to be eligible for VMware CBT migration. A common
deployment model is to prepare a smaller set of conversion-capable KVM hosts and
explicitly select one of them for migration work.

The current KVM agent checks these commands:

```bash
virt-v2v --version
nbdkit vddk --version
qemu-img --version
qemu-nbd --version
```

On Ubuntu and Debian hosts the current conversion support check also verifies
that the `nbdkit` package is installed with `dpkg -l nbdkit`.

## VMware VDDK installation

Install VMware VDDK on every KVM host that may run a VMware-to-KVM conversion or
CBT replication session. A common layout is:

```bash
mkdir -p /opt/vmware
tar -xzf VMware-vix-disklib-*.tar.gz -C /opt/vmware
```

The extracted directory is usually named:

```text
/opt/vmware/vmware-vix-disklib-distrib
```

CloudStack auto-detects the first directory named
`vmware-vix-disklib-distrib`. If auto-detection is not desirable, configure the
path explicitly in the KVM agent properties:

```properties
vddk.lib.dir=/opt/vmware/vmware-vix-disklib-distrib
```

Optional VDDK settings:

```properties
vddk.transports=nbdssl:nbd
vddk.thumbprint=<vcenter-sha1-thumbprint>
```

If `vddk.thumbprint` is not set, CloudStack attempts to compute the vCenter
thumbprint on the KVM host.

## EL8 and Oracle Linux 8 host packages

On an EL8-compatible KVM host, the CloudStack agent package already brings in
the normal KVM runtime dependencies such as libvirt, qemu-kvm, qemu-img, and
Python libvirt bindings. The VMware CBT path additionally needs virt-v2v,
nbdkit, the nbdkit VDDK plugin, qemu-nbd support, and VDDK itself.

Example package installation:

```bash
dnf install -y cloudstack-agent
dnf install -y virt-v2v nbdkit qemu-img qemu-kvm-core qemu-kvm-block-curl
dnf install -y nbdkit-plugin-vddk || dnf install -y nbdkit-vddk-plugin
```

Package names can differ between RHEL, Oracle Linux, Rocky, AlmaLinux, and
enabled module streams. The important validation is not the exact package name,
but that these commands work:

```bash
virt-v2v --version
qemu-img --version
qemu-nbd --version
nbdkit vddk --version
```

For Windows guests, install a `virtio-win` package or otherwise make the
VirtIO drivers available to virt-v2v. Some EL8 environments provide this from
an additional repository rather than the base distribution repositories.

If the nbdkit VDDK plugin package conflicts with the installed nbdkit module
stream, align the enabled AppStream or vendor repository so `nbdkit` and the
VDDK plugin come from compatible builds.

## Ubuntu 24.04 host packages

On Ubuntu 24.04, the base KVM and conversion packages are available from the
standard repositories, but the nbdkit VDDK plugin is not necessarily provided by
the default Ubuntu archive. Plan for an internal package, a vendor-provided
package, or a locally built nbdkit VDDK plugin.

Example base package installation:

```bash
apt-get update
apt-get install -y cloudstack-agent
apt-get install -y qemu-utils qemu-system-x86 libvirt-daemon-system libvirt-clients
apt-get install -y virt-v2v nbdkit libguestfs-tools
```

Then install or build the nbdkit VDDK plugin and verify:

```bash
nbdkit vddk --version
```

`qemu-img` and `qemu-nbd` are provided by `qemu-utils` on Ubuntu 24.04.

For Windows guests, ensure a `virtio-win` package or equivalent VirtIO driver
bundle is available. The current KVM agent check looks for a package named
`virtio-win` on Debian-based hosts, so an internal package with that name is
the easiest way to satisfy the current check.

## Network and VMware requirements

The selected KVM migration host must be able to reach:

* vCenter HTTPS, normally TCP 443.
* ESXi hosts used for VDDK/NFC access, commonly TCP 902 for NFC paths.
* Any network paths required by the selected VDDK transport mode.
* The destination primary storage backend.

The VMware account used for migration must be able to discover the VM, create
and remove snapshots, read virtual disk data, and query changed disk areas.

VMware CBT must be enabled and valid for the source VM disks before incremental
cycles can be trusted. A full resync is required when VMware invalidates CBT
state, for example after CBT reset, certain disk changes, failed snapshot
consolidation, or storage operations that change disk backing identity.

Avoid or explicitly handle VMware disk types that do not participate cleanly in
CBT, such as independent disks and unsupported raw device mappings.

## Validation checklist

Run this checklist on every candidate KVM migration host:

```bash
virt-v2v --version
qemu-img --version
qemu-nbd --version
nbdkit vddk --version
test -f /opt/vmware/vmware-vix-disklib-distrib/lib64/libvixDiskLib.so
systemctl restart cloudstack-agent
```

Then confirm the host details from the CloudStack API or UI show:

```text
host.vddk.support=true
host.vmware.cbt.support=true
host.qemu.img.version=<detected>
host.qemu.nbd.version=<detected>
host.virtv2v.version=<detected>
```

If `host.vmware.cbt.support` is false, check the agent log on the KVM host and
validate each command above manually.

## Operational notes

* Prefer `nbdssl` or another encrypted VDDK transport where supported.
* Run migrations from hosts with fast access to destination primary storage.
* Keep enough temporary space for virt-v2v and libguestfs scratch data; use
  `convert.instance.env.tmpdir` and `convert.instance.env.virtv2v.tmpdir` when
  the default temporary filesystem is too small.
* Use the current quiet-cycle settings to avoid cutting over while the source
  VM is still producing a high dirty rate.
* Increase `vmware.cbt.migration.agent.command.timeout` before migrating very
  large VMs if the baseline copy or cutover finalization can exceed 24 hours.
* Always clean up VMware snapshots after failed or cancelled cycles.
* Treat CBT change IDs as per-disk state. If any disk loses valid CBT state,
  restart that disk from a full sync.
