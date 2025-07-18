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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.backup.BackupOffering;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = BackupOffering.class)
public class BackupOfferingResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the backup offering")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name for the backup offering")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "description for the backup offering")
    private String description;

    @SerializedName(ApiConstants.PROVIDER)
    @Param(description = "provider name", since = "4.21.0")
    private String provider;

    @SerializedName(ApiConstants.EXTERNAL_ID)
    @Param(description = "external ID on the provider side")
    private String externalId;

    @SerializedName(ApiConstants.ALLOW_USER_DRIVEN_BACKUPS)
    @Param(description = "whether offering allows user driven ad-hoc/scheduled backups")
    private Boolean userDrivenBackups;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "zone ID")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "zone name")
    private String zoneName;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date this backup offering was created")
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setUserDrivenBackups(Boolean userDrivenBackups) {
        this.userDrivenBackups = userDrivenBackups;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
