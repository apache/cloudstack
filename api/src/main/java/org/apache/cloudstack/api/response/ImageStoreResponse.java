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
import org.apache.cloudstack.api.BaseResponseWithAnnotations;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.ImageStore;
import com.cloud.storage.ScopeType;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = ImageStore.class)
public class ImageStoreResponse extends BaseResponseWithAnnotations {
    @SerializedName("id")
    @Param(description = "the ID of the image store")
    private String id;

    @SerializedName("zoneid")
    @Param(description = "the Zone ID of the image store")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME)
    @Param(description = "the Zone name of the image store")
    private String zoneName;

    @SerializedName("name")
    @Param(description = "the name of the image store")
    private String name;

    @SerializedName("url")
    @Param(description = "the url of the image store")
    private String url;

    @SerializedName("protocol")
    @Param(description = "the protocol of the image store")
    private String protocol;

    @SerializedName("providername")
    @Param(description = "the provider name of the image store")
    private String providerName;

    @SerializedName("scope")
    @Param(description = "the scope of the image store")
    private ScopeType scope;

    @SerializedName("readonly")
    @Param(description = "defines if store is read-only")
    private Boolean readonly;

    @SerializedName("disksizetotal")
    @Param(description = "the total disk size of the host")
    private Long diskSizeTotal;

    @SerializedName("disksizeused")
    @Param(description = "the host's currently used disk size")
    private Long diskSizeUsed;

    public ImageStoreResponse() {
    }

    @Override
    public String getObjectId() {
        return this.getId();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public ScopeType getScope() {
        return scope;
    }

    public void setScope(ScopeType type) {
        this.scope = type;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Boolean getReadonly() {
        return readonly;
    }

    public void setReadonly(Boolean readonly) { this.readonly = readonly; }

    public void setDiskSizeTotal(Long diskSizeTotal) {
        this.diskSizeTotal = diskSizeTotal;
    }

    public void setDiskSizeUsed(Long diskSizeUsed) {
        this.diskSizeUsed = diskSizeUsed;
    }
}
