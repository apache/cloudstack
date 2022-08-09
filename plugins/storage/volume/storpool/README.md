# StorPool CloudStack Integration

## CloudStack Overview

### Primary and Secondary storage

Primary storage is associated with a cluster or zone, and it stores the virtual disks for all the VMs running on hosts in that cluster/zone.

Secondary storage stores the following:
* Templates — OS images that can be used to boot VMs and can include additional configuration information, such as installed applications
* ISO images — disc images containing data or bootable media for operating systems
* Disk volume snapshots — saved copies of VM data which can be used for data recovery or to create new templates


### ROOT and DATA volumes

ROOT volumes correspond to the boot disk of a VM. They are created automatically by CloudStack during VM creation.
ROOT volumes are created based on a system disk offering, corresponding to the service offering the user VM
is based on. We may change the ROOT volume disk offering but only to another system created disk offering.

DATA volumes correspond to additional disks. These can be created by users and then attached/detached to VMs.
DATA volumes are created based on a user-defined disk offering.


## Plugin Organization

The StorPool plugin consists of two parts:

### KVM hypervisor plugin patch

Source directory: ./apache-cloudstack-4.17-src/plugins/hypervisors/kvm

### StorPool primary storage plugin

Source directory: ./apache-cloudstack-4.17.0-src/plugins/storage/volume

There is one plugin for both the CloudStack management and agents, in the hope that having all the source
in one place will ease development and maintenance. The plugin itself though is separated into two mainly
independent parts:

  * ./src/com/... directory tree: agent related classes and commands send from management to agent
  * ./src/org/... directory tree: management related classes

The plugin is intended to be self contained and non-intrusive, thus ideally deploying it would consist of only
dropping the jar file into the appropriate places. This is the reason why all StorPool related communication
(ex. data copying, volume resize) is done with StorPool specific commands even when there is a CloudStack command
that does pretty much the same.

Note that for the present the StorPool plugin may only be used for a single primary storage cluster; support for
multiple clusters is planned.


## Build, Install, Setup

### Build

Go to the source directory and run:

    mvn -Pdeveloper -DskipTests install

The resulting jar file is located in the target/ subdirectory.

Note: checkstyle errors: before compilation a code style check is performed; if this fails compilation is aborted.
In short: no trailing whitespace, indent using 4 spaces, not tabs, comment-out or remove unused imports.

Note: Need to build both the KVM plugin and the StorPool plugin proper.

### Install

#### StorPool primary storage plugin

For each CloudStack management host:

```bash
scp ./target/cloud-plugin-storage-volume-storpool-{version}.jar {MGMT_HOST}:/usr/share/cloudstack-management/lib/
```

For each CloudStack agent host:

```bash
scp ./target/cloud-plugin-storage-volume-storpool-{version}.jar {AGENT_HOST}:/usr/share/cloudstack-agent/plugins/
```

Note: CloudStack managements/agents services must be restarted after adding the plugin to the respective directories

Note: Agents should have access to the StorPool management API, since attach and detach operations happens on the agent.
This is a CloudStack design issue, can't do much about it.

### Setup

#### Setting up StorPool

Perform the StorPool installation following the StorPool Installation Guide.

Create a template to be used by CloudStack. Must set *placeHead*, *placeAll*, *placeTail* and *replication*.
No need to set default volume size because it is determined by the CloudStack disks and services offering.

#### Setting up a StorPool PRIMARY storage pool in CloudStack

From the WEB UI, go to Infrastructure -> Primary Storage -> Add Primary Storage

Scope: select Zone-Wide
Hypervisor: select KVM
Zone: pick appropriate zone.
Name: user specified name

Protocol: select *SharedMountPoint*
Path: enter */dev/storpool* (required argument, actually not needed in practice).

Provider: select *StorPool*
Managed: leave unchecked (currently ignored)
Capacity Bytes: used for accounting purposes only. May be more or less than the actual StorPool template capacity.
Capacity IOPS: currently not used (may use for max IOPS limitations on volumes from this pool).
URL: enter SP_API_HTTP=address:port;SP_AUTH_TOKEN=token;SP_TEMPLATE=template_name. At present one template can be used for at most one Storage Pool.

SP_API_HTTP - address of StorPool Api
SP_AUTH_TOKEN - StorPool's token
SP_TEMPLATE - name of StorPool's template

Storage Tags: If left blank, the StorPool storage plugin will use the pool name to create a corresponding storage tag.
This storage tag may be used later, when defining service or disk offerings.


## Plugin Functionality

<table cellpadding="5">
<tr>
  <th>Plugin Action</th>
  <th>CloudStack Action</th>
  <th>management/agent</th>
  <th>impl. details</th>
</tr>
<tr>
  <td>Create ROOT volume from ISO</td>
  <td>create VM from ISO</td>
  <td>management</td>
  <td>createVolumeAsync</td>
</tr>
<tr>
  <td>Create ROOT volume from Template</td>
  <td>create VM from Template</td>
  <td>management + agent</td>
  <td>copyAsync (T => T, T => V)</td>
</tr>
<tr>
  <td>Create DATA volume</td>
  <td>create Volume</td>
  <td>management</td>
  <td>createVolumeAsync</td>
</tr>
<tr>
  <td>Attach ROOT/DATA volume</td>
  <td>start VM (+attach/detach Volume)</td>
  <td>agent</td>
  <td>connectPhysicalDisk</td>
</tr>
<tr>
  <td>Detach ROOT/DATA volume</td>
  <td>stop VM</td>
  <td>agent</td>
  <td>disconnectPhysicalDiskByPath</td>
</tr>
<tr>
  <td>&nbsp;</td>
  <td>Migrate VM</td>
  <td>agent</td>
  <td>attach + detach</td>
</tr>
<tr>
  <td>Delete ROOT volume</td>
  <td>destroy VM (expunge)</td>
  <td>management</td>
  <td>deleteAsync</td>
</tr>
<tr>
  <td>Delete DATA volume</td>
  <td>delete Volume (detached)</td>
  <td>management</td>
  <td>deleteAsync</td>
</tr>
<tr>
  <td>Create ROOT/DATA volume snapshot</td>
  <td>snapshot volume</td>
  <td>management + agent</td>
  <td>takeSnapshot + copyAsync (S => S)</td>
</tr>
<tr>
  <td>Create volume from snapshoot</td>
  <td>create volume from snapshot</td>
  <td>management + agent(?)</td>
  <td>copyAsync (S => V)</td>
</tr>
<tr>
  <td>Create TEMPLATE from ROOT volume</td>
  <td>create template from volume</td>
  <td>management + agent</td>
  <td>copyAsync (V => T)</td>
</tr>
<tr>
  <td>Create TEMPLATE from snapshot</td>
  <td>create template from snapshot</td>
  <td>SECONDARY STORAGE</td>
  <td>&nbsp;</td>
</tr>
<tr>
  <td>Download volume</td>
  <td>download volume</td>
  <td>management + agent</td>
  <td>copyAsync (V => V)</td>
</tr>
<tr>
  <td>Revert ROOT/DATA volume to snapshot</td>
  <td>revert to snapshot</td>
  <td>management</td>
  <td>revertSnapshot</td>
</tr>
<tr>
  <td>(Live) resize ROOT/DATA volume</td>
  <td>resize volume</td>
  <td>management + agent</td>
  <td>resize + StorpoolResizeCmd</td>
</tr>
<tr>
  <td>Delete SNAPSHOT (ROOT/DATA)</td>
  <td>delete snapshot</td>
  <td>management</td>
  <td>StorpoolSnapshotStrategy</td>
</tr>
<tr>
  <td>Delete TEMPLATE</td>
  <td>delete template</td>
  <td>agent</td>
  <td>deletePhysicalDisk</td>
</tr>
<tr>
  <td>migrate VM/volume</td>
  <td>migrate VM/volume to another storage</td>
  <td>management/management + agent</td>
  <td>copyAsync (V => V)</td>
</tr>
<tr>
  <td>VM snapshot</td>
  <td>group snapshot of VM's disks</td>
  <td>management</td>
  <td>StorpoolVMSnapshotStrategy takeVMSnapshot</td>
</tr>
<tr>
  <td>revert VM snapshot</td>
  <td>revert group snapshot of VM's disks</td>
  <td>management</td>
  <td>StorpoolVMSnapshotStrategy revertVMSnapshot</td>
</tr>
<tr>
  <td>delete VM snapshot</td>
  <td>delete group snapshot of VM's disks</td>
  <td>management</td>
  <td>StorpoolVMSnapshotStrategy deleteVMSnapshot</td>
</tr>
<tr>
  <td>VM vc_policy tag</td>
  <td>vc_policy tag for all disks attached to VM</td>
  <td>management</td>
  <td>StorPoolCreateTagsCmd</td>
</tr>
<tr>
  <td>delete VM vc_policy tag</td>
  <td>remove vc_policy tag for all disks attached to VM</td>
  <td>management</td>
  <td>StorPoolDeleteTagsCmd</td>
</tr>
</table>

>NOTE: When using multicluster for each CloudStack cluster in its settings set the value of StorPool's SP_CLUSTER_ID in "sp.cluster.id".
>

>NOTE: Secondary storage could be bypassed with Configuration setting "sp.bypass.secondary.storage" set to true. </br>
In this case only snapshots won't be downloaded to secondary storage.
>

### Creating template from snapshot

#### If bypass option is enabled

The snapshot exists only on PRIMARY (StorPool) storage. From this snapshot it will be created a template on SECONADRY.

#### If bypass option is disabled

TODO: Maybe we should not use CloudStack functionality, and to use that one when bypass option is enabled

This is independent of StorPool as snapshots exist on secondary.

### Creating ROOT volume from templates

When creating the first volume based on the given template, if snapshot of the template does not exists on StorPool it will be first downloaded (cached) to PRIMARY storage.
This is mapped to a StorPool snapshot so, creating succecutive volumes from the same template does not incur additional 
copying of data to PRIMARY storage.

This cached snapshot is garbage collected when the original template is deleted from CloudStack. This cleanup is done
by a background task in CloudStack.

### Creating a ROOT volume from an ISO image

We just need to create the volume. The ISO installation is handled by CloudStack.

### Creating a DATA volume

DATA volumes are created by CloudStack the first time it is attached to a VM.

### Creating volume from snapshot

We use the fact that the snapshot already exists on PRIMARY, so no data is copied. We will copy snapshots from SECONDARY to StorPool PRIMARY,
when there is no corresponding StorPool snapshot.

### Resizing volumes

We need to send a resize cmd to agent, where the VM the volume is attached to is running, so that
the resize is visible by the VM.

### Creating snapshots

The snapshot is first created on the PRIMARY storage (i.e. StorPool), then backed-up on SECONDARY storage
(tested with NFS secondary) if bypass option is not enabled. The original StorPool snapshot is kept, so that creating volumes from the snapshot does not need to copy
the data again to PRIMARY. When the snapshot is deleted from CloudStack so is the corresponding StorPool snapshot.

Currently snapshots are taken in RAW format.

### Reverting volume to snapshot

It's handled by StorPool

### Migrating volumes to other Storage pools

Tested with storage pools on NFS only.

### Virtual Machine Snapshot/Group Snapshot

StorPool supports consistent snapshots of volumes attached to a virtual machine.

### BW/IOPS limitations

Max IOPS are kept in StorPool's volumes with the help of custom service offerings, by adding IOPS limits to the
corresponding system disk offering.

CloudStack has no way to specify max BW. Do they want to be able to specify max BW only is sufficient.
