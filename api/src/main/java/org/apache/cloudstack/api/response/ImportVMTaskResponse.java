//
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
//
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.Date;

public class ImportVMTaskResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of importing task")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the Zone ID")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the Zone name")
    private String zoneName;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account name")
    private String accountName;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "the ID of account")
    private String accountId;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "the ID of the imported VM (after task is completed)")
    private String virtualMachineId;

    @SerializedName(ApiConstants.DISPLAY_NAME)
    @Param(description = "the display name of the importing VM")
    private String displayName;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the importing VM task")
    private String state;

    @SerializedName(ApiConstants.VCENTER)
    @Param(description = "the vcenter name of the importing VM task")
    private String vcenter;

    @SerializedName(ApiConstants.DATACENTER_NAME)
    @Param(description = "the datacenter name of the importing VM task")
    private String datacenterName;

    @SerializedName("sourcevmname")
    @Param(description = "the source VM name")
    private String sourceVMName;

    @SerializedName("step")
    @Param(description = "the current step on the importing VM task")
    private String step;

    @SerializedName("stepduration")
    @Param(description = "the duration of the current step")
    private String stepDuration;

    @SerializedName(ApiConstants.DURATION)
    @Param(description = "the total task duration")
    private String duration;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the current step description on the importing VM task")
    private String description;

    @SerializedName(ApiConstants.CONVERT_INSTANCE_HOST_ID)
    @Param(description = "the ID of the host on which the instance is being converted")
    private String convertInstanceHostId;

    @SerializedName("convertinstancehostname")
    @Param(description = "the name of the host on which the instance is being converted")
    private String convertInstanceHostName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the create date of the importing task")
    private Date created;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "the last updated date of the importing task")
    private Date lastUpdated;

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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getVcenter() {
        return vcenter;
    }

    public void setVcenter(String vcenter) {
        this.vcenter = vcenter;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public void setDatacenterName(String datacenterName) {
        this.datacenterName = datacenterName;
    }

    public String getSourceVMName() {
        return sourceVMName;
    }

    public void setSourceVMName(String sourceVMName) {
        this.sourceVMName = sourceVMName;
    }

    public String getStep() {
        return step;
    }

    public void setStep(String step) {
        this.step = step;
    }

    public String getStepDuration() {
        return stepDuration;
    }

    public void setStepDuration(String stepDuration) {
        this.stepDuration = stepDuration;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConvertInstanceHostId() {
        return convertInstanceHostId;
    }

    public void setConvertInstanceHostId(String convertInstanceHostId) {
        this.convertInstanceHostId = convertInstanceHostId;
    }

    public String getConvertInstanceHostName() {
        return convertInstanceHostName;
    }

    public void setConvertInstanceHostName(String convertInstanceHostName) {
        this.convertInstanceHostName = convertInstanceHostName;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
