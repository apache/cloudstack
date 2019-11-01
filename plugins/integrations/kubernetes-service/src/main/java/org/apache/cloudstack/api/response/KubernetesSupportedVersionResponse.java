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
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.kubernetesversion.KubernetesSupportedVersion;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
@EntityReference(value = {KubernetesSupportedVersion.class})
public class KubernetesSupportedVersionResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the Kubernetes supported version")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the Kubernetes supported version")
    private String name;

    @SerializedName(ApiConstants.ISO_ID)
    @Param(description = "the id of the binaries ISO for Kubernetes supported version")
    private String isoId;

    @SerializedName("isoname")
    @Param(description = "the name of the binaries ISO for Kubernetes supported version")
    private String isoName;

    @SerializedName("isostate")
    @Param(description = "the state of the binaries ISO for Kubernetes supported version")
    private String isoState;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the id of the zone in which Kubernetes supported version is available")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the name of the zone in which Kubernetes supported version is available")
    private String zoneName;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIsoId() {
        return isoId;
    }

    public void setIsoId(String isoId) {
        this.isoId = isoId;
    }

    public String getIsoName() {
        return isoName;
    }

    public void setIsoName(String isoName) {
        this.isoName = isoName;
    }

    public String getIsoState() {
        return isoState;
    }

    public void setIsoState(String isoState) {
        this.isoState = isoState;
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
}
