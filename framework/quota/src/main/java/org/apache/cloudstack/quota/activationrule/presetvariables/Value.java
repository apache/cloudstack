// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.cloudstack.quota.activationrule.presetvariables;

import java.util.List;
import java.util.Map;

import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.vm.snapshot.VMSnapshot;
import org.apache.cloudstack.quota.constant.QuotaTypes;

public class Value extends GenericPresetVariable {

    @PresetVariableDefinition(description = "ID of the resource.", supportedTypes = {QuotaTypes.ALLOCATED_VM, QuotaTypes.RUNNING_VM, QuotaTypes.VOLUME, QuotaTypes.TEMPLATE,
            QuotaTypes.ISO, QuotaTypes.SNAPSHOT, QuotaTypes.NETWORK_OFFERING, QuotaTypes.VM_SNAPSHOT})
    private String id;

    @PresetVariableDefinition(description = "Name of the resource.", supportedTypes = {QuotaTypes.ALLOCATED_VM, QuotaTypes.RUNNING_VM, QuotaTypes.VOLUME, QuotaTypes.TEMPLATE,
            QuotaTypes.ISO, QuotaTypes.SNAPSHOT, QuotaTypes.NETWORK_OFFERING, QuotaTypes.VM_SNAPSHOT})
    private String name;

    @PresetVariableDefinition(description = "Host where the VM is running.", supportedTypes = {QuotaTypes.RUNNING_VM})
    private Host host;

    @PresetVariableDefinition(description = "OS of the VM/template.", supportedTypes = {QuotaTypes.RUNNING_VM, QuotaTypes.ALLOCATED_VM, QuotaTypes.TEMPLATE, QuotaTypes.ISO})
    private String osName;

    @PresetVariableDefinition(description = "A list of resources of the account between the start and end date of the usage record being calculated " +
            "(i.e.: [{zoneId: ..., domainId:...}]).")
    private List<Resource> accountResources;

    @PresetVariableDefinition(supportedTypes = {QuotaTypes.ALLOCATED_VM, QuotaTypes.RUNNING_VM, QuotaTypes.VOLUME, QuotaTypes.TEMPLATE, QuotaTypes.ISO, QuotaTypes.SNAPSHOT,
            QuotaTypes.VM_SNAPSHOT}, description = "List of tags of the resource in the format key:value (i.e.: {\"a\":\"b\", \"c\":\"d\"}).")
    private Map<String, String> tags;

    @PresetVariableDefinition(description = "Tag of the network offering.", supportedTypes = {QuotaTypes.NETWORK_OFFERING})
    private String tag;

    @PresetVariableDefinition(description = "Size of the resource (in MiB).", supportedTypes = {QuotaTypes.TEMPLATE, QuotaTypes.ISO, QuotaTypes.VOLUME, QuotaTypes.SNAPSHOT,
            QuotaTypes.BACKUP})
    private Long size;

    @PresetVariableDefinition(description = "Virtual size of the backup.", supportedTypes = {QuotaTypes.BACKUP})
    private Long virtualSize;

    @PresetVariableDefinition(description = "Provisioning type of the resource. Values can be: thin, sparse or fat.", supportedTypes = {QuotaTypes.VOLUME})
    private ProvisioningType provisioningType;

    @PresetVariableDefinition(description = "Type of the snapshot. Values can be: MANUAL, RECURRING, HOURLY, DAILY, WEEKLY and MONTHLY.", supportedTypes = {QuotaTypes.SNAPSHOT})
    private Snapshot.Type snapshotType;

    @PresetVariableDefinition(description = "Type of the VM snapshot. Values can be: Disk or DiskAndMemory.", supportedTypes = {QuotaTypes.VM_SNAPSHOT})
    private VMSnapshot.Type vmSnapshotType;

    @PresetVariableDefinition(description = "Computing offering of the VM.", supportedTypes = {QuotaTypes.RUNNING_VM, QuotaTypes.ALLOCATED_VM})
    private ComputeOffering computeOffering;

    @PresetVariableDefinition(description = "Template/ISO with which the VM was created.", supportedTypes = {QuotaTypes.RUNNING_VM, QuotaTypes.ALLOCATED_VM})
    private GenericPresetVariable template;

    @PresetVariableDefinition(description = "Disk offering of the volume.", supportedTypes = {QuotaTypes.VOLUME})
    private GenericPresetVariable diskOffering;

    @PresetVariableDefinition(description = "Storage where the volume or snapshot is. While handling with snapshots, this value can be from the primary storage if the global " +
            "setting 'snapshot.backup.to.secondary' is false, otherwise it will be from secondary storage.", supportedTypes = {QuotaTypes.VOLUME, QuotaTypes.SNAPSHOT})
    private Storage storage;

    @PresetVariableDefinition(description = "Computing resources consumed by the VM.", supportedTypes = {QuotaTypes.RUNNING_VM})
    private ComputingResources computingResources;

    @PresetVariableDefinition(description = "Backup offering of the backup.", supportedTypes = {QuotaTypes.BACKUP})
    private BackupOffering backupOffering;

    @PresetVariableDefinition(description = "The hypervisor where the resource was deployed. Values can be: XenServer, KVM, VMware, Hyperv, BareMetal, Ovm, Ovm3 and LXC.",
            supportedTypes = {QuotaTypes.RUNNING_VM, QuotaTypes.ALLOCATED_VM, QuotaTypes.VM_SNAPSHOT, QuotaTypes.SNAPSHOT})
    private String hypervisorType;

    @PresetVariableDefinition(description = "The volume format. Values can be: RAW, VHD, VHDX, OVA and QCOW2.", supportedTypes = {QuotaTypes.VOLUME, QuotaTypes.VOLUME_SECONDARY})
    private String volumeFormat;
    private String state;

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
        fieldNamesToIncludeInToString.add("host");
    }

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
        fieldNamesToIncludeInToString.add("osName");
    }

    public List<Resource> getAccountResources() {
        return accountResources;
    }

    public void setAccountResources(List<Resource> accountResources) {
        this.accountResources = accountResources;
        fieldNamesToIncludeInToString.add("accountResources");
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
        fieldNamesToIncludeInToString.add("tags");
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
        fieldNamesToIncludeInToString.add("tag");
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
        fieldNamesToIncludeInToString.add("size");
    }

    public ProvisioningType getProvisioningType() {
        return provisioningType;
    }

    public void setProvisioningType(ProvisioningType provisioningType) {
        this.provisioningType = provisioningType;
        fieldNamesToIncludeInToString.add("provisioningType");
    }

    public Snapshot.Type getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(Snapshot.Type snapshotType) {
        this.snapshotType = snapshotType;
        fieldNamesToIncludeInToString.add("snapshotType");
    }

    public VMSnapshot.Type getVmSnapshotType() {
        return vmSnapshotType;
    }

    public void setVmSnapshotType(VMSnapshot.Type vmSnapshotType) {
        this.vmSnapshotType = vmSnapshotType;
        fieldNamesToIncludeInToString.add("vmSnapshotType");
    }

    public ComputeOffering getComputeOffering() {
        return computeOffering;
    }

    public void setComputeOffering(ComputeOffering computeOffering) {
        this.computeOffering = computeOffering;
        fieldNamesToIncludeInToString.add("computeOffering");
    }

    public GenericPresetVariable getTemplate() {
        return template;
    }

    public void setTemplate(GenericPresetVariable template) {
        this.template = template;
        fieldNamesToIncludeInToString.add("template");
    }

    public GenericPresetVariable getDiskOffering() {
        return diskOffering;
    }

    public void setDiskOffering(GenericPresetVariable diskOffering) {
        this.diskOffering = diskOffering;
        fieldNamesToIncludeInToString.add("diskOffering");
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
        fieldNamesToIncludeInToString.add("storage");
    }

    public ComputingResources getComputingResources() {
        return computingResources;
    }

    public void setComputingResources(ComputingResources computingResources) {
        this.computingResources = computingResources;
        fieldNamesToIncludeInToString.add("computingResources");
    }

    public Long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(Long virtualSize) {
        this.virtualSize = virtualSize;
        fieldNamesToIncludeInToString.add("virtualSize");
    }

    public BackupOffering getBackupOffering() {
        return backupOffering;
    }

    public void setBackupOffering(BackupOffering backupOffering) {
        this.backupOffering = backupOffering;
        fieldNamesToIncludeInToString.add("backupOffering");
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
        fieldNamesToIncludeInToString.add("hypervisorType");
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setVolumeFormat(String volumeFormat) {
        this.volumeFormat = volumeFormat;
        fieldNamesToIncludeInToString.add("volumeFormat");
    }

    public String getVolumeFormat() {
        return volumeFormat;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
        fieldNamesToIncludeInToString.add("state");
    }
}
