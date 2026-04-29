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
package org.apache.cloudstack.storage.datastore.adapter.primera;

import java.util.ArrayList;
import java.util.Date;

import org.apache.cloudstack.storage.datastore.adapter.ProviderSnapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraVolume implements ProviderSnapshot {
    @JsonIgnore
    private AddressType addressType = AddressType.FIBERWWN;
    @JsonIgnore
    private String connectionId;
    @JsonIgnore
    private Integer priority = 0;

    private String physParentId = null;
    private Integer parentId = null;
    private String copyOf = null;
    private Integer roChildId = null;
    private Integer rwChildId = null;
    private String snapCPG = null;
    private Long total = null;
    /**
     * Actions are enumerated and listed at
     * https://support.hpe.com/hpesc/public/docDisplay?docId=a00118636en_us&page=v25706371.html
     */
    private Integer action = null;
    private String comment = null;
    private Integer id = null;
    private String name = null;
    private Integer deduplicationState = null;
    private Integer compressionState = null;
    private Integer provisioningType = null;
    private Integer copyType = null;
    private Integer baseId = null;
    private Boolean readOnly = null;
    private Integer state = null;
    private ArrayList<Object> failedStates = null;
    private ArrayList<Object> degradedStates = null;
    private ArrayList<Object> additionalStates = null;
    private PrimeraVolumeAdminSpace adminSpace = null;
    private PrimeraVolumeSnapshotSpace snapshotSpace = null;
    private PrimeraVolumeUserSpace userSpace = null;
    private Integer totalReservedMiB = null;
    private Integer totalUsedMiB = null;
    private Integer sizeMiB = null;
    private Integer hostWriteMiB = null;
    private String wwn = null;
    private Integer creationTimeSec = null;
    private Date creationTime8601 = null;
    private Integer ssSpcAllocWarningPct;
    private Integer ssSpcAllocLimitPct = null;
    private Integer usrSpcAllocWarningPct = null;
    private Integer usrSpcAllocLimitPct = null;
    private PrimeraVolumePolicies policies = null;
    private String userCPG = null;
    private String uuid = null;
    private Integer sharedParentId = null;
    private Integer udid = null;
    private PrimeraVolumeCapacityEfficiency capacityEfficiency = null;
    private Integer rcopyStatus = null;
    private ArrayList<PrimeraVolumeLink> links = null;
    public String getPhysParentId() {
        return physParentId;
    }
    public void setPhysParentId(String physParentId) {
        this.physParentId = physParentId;
    }
    public Integer getParentId() {
        return parentId;
    }
    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }
    public String getCopyOf() {
        return copyOf;
    }
    public void setCopyOf(String copyOf) {
        this.copyOf = copyOf;
    }
    public Integer getRoChildId() {
        return roChildId;
    }
    public void setRoChildId(Integer roChildId) {
        this.roChildId = roChildId;
    }
    public Integer getRwChildId() {
        return rwChildId;
    }
    public void setRwChildId(Integer rwChildId) {
        this.rwChildId = rwChildId;
    }
    public String getSnapCPG() {
        return snapCPG;
    }
    public void setSnapCPG(String snapCPG) {
        this.snapCPG = snapCPG;
    }
    public Long getTotal() {
        return total;
    }
    public void setTotal(Long total) {
        this.total = total;
    }
    public Integer getAction() {
        return action;
    }
    public void setAction(Integer action) {
        this.action = action;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getDeduplicationState() {
        return deduplicationState;
    }
    public void setDeduplicationState(Integer deduplicationState) {
        this.deduplicationState = deduplicationState;
    }
    public Integer getCompressionState() {
        return compressionState;
    }
    public void setCompressionState(Integer compressionState) {
        this.compressionState = compressionState;
    }
    public Integer getProvisioningType() {
        return provisioningType;
    }
    public void setProvisioningType(Integer provisioningType) {
        this.provisioningType = provisioningType;
    }
    public Integer getCopyType() {
        return copyType;
    }
    public void setCopyType(Integer copyType) {
        this.copyType = copyType;
    }
    public Integer getBaseId() {
        return baseId;
    }
    public void setBaseId(Integer baseId) {
        this.baseId = baseId;
    }
    public Boolean getReadOnly() {
        return readOnly;
    }
    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }
    public String getState() {
        if (state != null) {
            return state.toString();
        }
        return null;
    }
    public void setState(Integer state) {
        this.state = state;
    }
    public ArrayList<Object> getFailedStates() {
        return failedStates;
    }
    public void setFailedStates(ArrayList<Object> failedStates) {
        this.failedStates = failedStates;
    }
    public ArrayList<Object> getDegradedStates() {
        return degradedStates;
    }
    public void setDegradedStates(ArrayList<Object> degradedStates) {
        this.degradedStates = degradedStates;
    }
    public ArrayList<Object> getAdditionalStates() {
        return additionalStates;
    }
    public void setAdditionalStates(ArrayList<Object> additionalStates) {
        this.additionalStates = additionalStates;
    }
    public PrimeraVolumeAdminSpace getAdminSpace() {
        return adminSpace;
    }
    public void setAdminSpace(PrimeraVolumeAdminSpace adminSpace) {
        this.adminSpace = adminSpace;
    }
    public PrimeraVolumeSnapshotSpace getSnapshotSpace() {
        return snapshotSpace;
    }
    public void setSnapshotSpace(PrimeraVolumeSnapshotSpace snapshotSpace) {
        this.snapshotSpace = snapshotSpace;
    }
    public PrimeraVolumeUserSpace getUserSpace() {
        return userSpace;
    }
    public void setUserSpace(PrimeraVolumeUserSpace userSpace) {
        this.userSpace = userSpace;
    }
    public Integer getTotalReservedMiB() {
        return totalReservedMiB;
    }
    public void setTotalReservedMiB(Integer totalReservedMiB) {
        this.totalReservedMiB = totalReservedMiB;
    }
    public Integer getTotalUsedMiB() {
        return totalUsedMiB;
    }
    public void setTotalUsedMiB(Integer totalUsedMiB) {
        this.totalUsedMiB = totalUsedMiB;
    }
    public Integer getSizeMiB() {
        return sizeMiB;
    }
    public void setSizeMiB(Integer sizeMiB) {
        this.sizeMiB = sizeMiB;
    }
    public Integer getHostWriteMiB() {
        return hostWriteMiB;
    }
    public void setHostWriteMiB(Integer hostWriteMiB) {
        this.hostWriteMiB = hostWriteMiB;
    }
    public String getWwn() {
        return wwn;
    }
    public void setWwn(String wwn) {
        this.wwn = wwn;
    }
    public Integer getCreationTimeSec() {
        return creationTimeSec;
    }
    public void setCreationTimeSec(Integer creationTimeSec) {
        this.creationTimeSec = creationTimeSec;
    }
    public Date getCreationTime8601() {
        return creationTime8601;
    }
    public void setCreationTime8601(Date creationTime8601) {
        this.creationTime8601 = creationTime8601;
    }
    public Integer getSsSpcAllocWarningPct() {
        return ssSpcAllocWarningPct;
    }
    public void setSsSpcAllocWarningPct(Integer ssSpcAllocWarningPct) {
        this.ssSpcAllocWarningPct = ssSpcAllocWarningPct;
    }
    public Integer getSsSpcAllocLimitPct() {
        return ssSpcAllocLimitPct;
    }
    public void setSsSpcAllocLimitPct(Integer ssSpcAllocLimitPct) {
        this.ssSpcAllocLimitPct = ssSpcAllocLimitPct;
    }
    public Integer getUsrSpcAllocWarningPct() {
        return usrSpcAllocWarningPct;
    }
    public void setUsrSpcAllocWarningPct(Integer usrSpcAllocWarningPct) {
        this.usrSpcAllocWarningPct = usrSpcAllocWarningPct;
    }
    public Integer getUsrSpcAllocLimitPct() {
        return usrSpcAllocLimitPct;
    }
    public void setUsrSpcAllocLimitPct(Integer usrSpcAllocLimitPct) {
        this.usrSpcAllocLimitPct = usrSpcAllocLimitPct;
    }
    public PrimeraVolumePolicies getPolicies() {
        return policies;
    }
    public void setPolicies(PrimeraVolumePolicies policies) {
        this.policies = policies;
    }
    public String getUserCPG() {
        return userCPG;
    }
    public void setUserCPG(String userCPG) {
        this.userCPG = userCPG;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public Integer getSharedParentId() {
        return sharedParentId;
    }
    public void setSharedParentId(Integer sharedParentId) {
        this.sharedParentId = sharedParentId;
    }
    public Integer getUdid() {
        return udid;
    }
    public void setUdid(Integer udid) {
        this.udid = udid;
    }
    public PrimeraVolumeCapacityEfficiency getCapacityEfficiency() {
        return capacityEfficiency;
    }
    public void setCapacityEfficiency(PrimeraVolumeCapacityEfficiency capacityEfficiency) {
        this.capacityEfficiency = capacityEfficiency;
    }
    public Integer getRcopyStatus() {
        return rcopyStatus;
    }
    public void setRcopyStatus(Integer rcopyStatus) {
        this.rcopyStatus = rcopyStatus;
    }
    public ArrayList<PrimeraVolumeLink> getLinks() {
        return links;
    }
    public void setLinks(ArrayList<PrimeraVolumeLink> links) {
        this.links = links;
    }
    @Override
    @JsonIgnore
    public Boolean isDestroyed() {
        return false;
    }
    @Override
    public void setId(String id) {
        this.id = Integer.parseInt(id);
    }
    public String getId() {
        if (id != null) {
            return Integer.toString(id);
        }
        return null;
    }
    @Override
    public Integer getPriority() {
        return priority;
    }
    @Override
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    @Override
    public AddressType getAddressType() {
        return addressType;
    }
    @Override
    public void setAddressType(AddressType addressType) {
        this.addressType = addressType;
    }
    @Override
    public String getAddress() {
        return this.wwn;
    }
    @Override
    @JsonIgnore
    public Long getAllocatedSizeInBytes() {
        if (this.getSizeMiB() != null) {
            return this.getSizeMiB() * PrimeraAdapter.BYTES_IN_MiB;
        }
        return 0L;
    }
    @Override
    @JsonIgnore
    public Long getUsedBytes() {
        if (this.getTotalReservedMiB() != null) {
            return this.getTotalReservedMiB() * PrimeraAdapter.BYTES_IN_MiB;
        }
        return 0L;
    }
    @Override
    @JsonIgnore
    public String getExternalUuid() {
        return uuid;
    }
    public void setExternalUuid(String uuid) {
        this.uuid = uuid;
    }
    @Override
    @JsonIgnore
    public String getExternalName() {
        return name;
    }
    public void setExternalName(String name) {
        this.name = name;
    }
    @Override
    @JsonIgnore
    public String getExternalConnectionId() {
        return connectionId;
    }
    public void setExternalConnection(String connectionId) {
        this.connectionId = connectionId;
    }
    @Override
    @JsonIgnore
    public Boolean canAttachDirectly() {
        return true;
    }
}
