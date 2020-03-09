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
    @Param(description = "the ID of the vm snapshot")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the vm snapshot")
    private String name;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the vm snapshot")
    private VMSnapshot.State state;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the vm snapshot")
    private String description;

    @SerializedName(ApiConstants.DISPLAY_NAME)
    @Param(description = "the display name of the vm snapshot")
    private String displayName;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone ID of the vm snapshot")
    private String zoneId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "the vm ID of the vm snapshot")
    private String virtualMachineid;

    @SerializedName("parent")
    @Param(description = "the parent ID of the vm snapshot")
    private String parent;

    @SerializedName("parentName")
    @Param(description = "the parent displayName of the vm snapshot")
    private String parentName;

    @SerializedName("current")
    @Param(description = "indiates if this is current snapshot")
    private Boolean current;

    @SerializedName("type")
    @Param(description = "VM Snapshot type")
    private String type;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the create date of the vm snapshot")
    private Date created;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the disk volume")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the vpn")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the vpn")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the disk volume")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the disk volume")
    private String domainName;

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

    public String getVirtualMachineid() {
        return virtualMachineid;
    }

    public void setVirtualMachineid(String virtualMachineid) {
        this.virtualMachineid = virtualMachineid;
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

    public void setTags(Set<ResourceTagResponse> tags) {
        this.tags = tags;
    }
}
