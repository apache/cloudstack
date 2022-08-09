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

public class Value extends GenericPresetVariable {
    private Host host;
    private String osName;
    private List<Resource> accountResources;
    private Map<String, String> tags;
    private String tag;
    private Long size;
    private Long virtualSize;
    private ProvisioningType provisioningType;
    private Snapshot.Type snapshotType;
    private VMSnapshot.Type vmSnapshotType;
    private ComputeOffering computeOffering;
    private GenericPresetVariable template;
    private GenericPresetVariable diskOffering;
    private Storage storage;
    private ComputingResources computingResources;
    private BackupOffering backupOffering;

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
}
