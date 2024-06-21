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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponseWithTagInformation;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.storage.fileshare.FileShare;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;


@EntityReference(value = FileShare.class)
public class FileShareResponse extends BaseResponseWithTagInformation implements ControlledViewEntityResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the file share")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the file share")
    private String name;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "ID of the availability zone")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "Name of the availability zone")
    private String zoneName;

    @SerializedName(ApiConstants.VIRTUAL_MACHINE_ID)
    @Param(description = "id of the storage fs vm")
    private String virtualMachineId;

    @SerializedName(ApiConstants.VOLUME_ID)
    @Param(description = "id of the storage fs data volume")
    private String volumeId;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "the state of the file share")
    private String state;

    @SerializedName(ApiConstants.ACCOUNT)
    @Param(description = "the account associated with the file share")
    private String accountName;

    @SerializedName(ApiConstants.PROJECT_ID)
    @Param(description = "the project id of the file share")
    private String projectId;

    @SerializedName(ApiConstants.PROJECT)
    @Param(description = "the project name of the file share")
    private String projectName;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the ID of the domain associated with the file share")
    private String domainId;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "the domain associated with the file share")
    private String domainName;

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

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setVirtualMachineId(String virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }
}
