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

package org.apache.cloudstack.api.response;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.vm.snapshot.VMSnapshot;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = VMSnapshot.class)
public class VMSnapshotResponse extends BaseResponseWithTagInformation implements ControlledEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "The ID of the Instance Snapshot")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The name of the Instance Snapshot")
    private String name;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "The state of the Instance Snapshot")
    private VMSnapshot.State state;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "The description of the Instance Snapshot")
    private String description;

    @SerializedName(ApiConstants.DISPLAY_NAME)
    @Param(description = "The display name of the Instance Snapshot")
    private String displayName;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "The Zone ID of the Instance Snapshot")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "The Zone name of the Instance Snapshot", since = "4.15.1")
    private String zoneName;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "The Instance ID of the Instance Snapshot")
    private String virtualMachineId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_NAME)
    @Param(description = "The Instance name of the Instance Snapshot", since = "4.15.1")
    private String virtualMachineName;

    @SerializedName("parent")
    @Param(description = "The parent ID of the Instance Snapshot")
    private String parent;

    @SerializedName("parentName")
    @Param(description = "The parent displayName of the Instance Snapshot")
    private String parentName;

    @SerializedName("current")
    @Param(description = "Indicates if this is current Snapshot")
    private Boolean current;

    @SerializedName("type")
    @Param(description = "Instance Snapshot type")
    private String type;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "The create date of the Instance Snapshot")
    private Date created;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "The Account associated with the disk volume")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "The project ID of the VPN")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "The project name of the VPN")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "The ID of the domain associated with the disk volume")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The domain associated with the disk volume")
    private String domainName;

    @SerializedName(ApiConstants.DOMAIN_PATH)
    @Param(description = "Path of the domain to which the disk volume belongs", since = "4.19.2.0")
    private String domainPath;

    @SerializedName(ApiConstants.HYPERVISOR)
    @Param(description = "The type of hypervisor on which Snapshot is stored")
    private String hypervisor;

    public VMSnapshotResponse() {
        tags = new LinkedHashSet<ResourceTagResponse>();
    }

    @Override
    public String getObjectId() {
        return getId();
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getVirtualMachineName() {
        return virtualMachineName;
    }

    public void setVirtualMachineName(String virtualMachineName) {
        this.virtualMachineName = virtualMachineName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setState(VMSnapshot.State state) {
        this.state = state;
    }

    public VMSnapshot.State getState() {
        return state;
    }

    public Boolean getCurrent() {
        return current;
    }

    public void setCurrent(Boolean current) {
        this.current = current;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getParentName() {
        return parentName;
    }

    public String getParent() {
      return parent;
    }

    public void setParent(String parent) {
      this.parent = parent;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void setAccountName(String accountName) {
        this.accountName = accountName;

    }

    @Override
    public void setProjectId(String projectId) {
        this.projectId = projectId;

    }

    @Override
    public void setProjectName(String projectName) {
        this.projectName = projectName;

    }

    @Override
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    @Override
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    @Override
    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }
}
