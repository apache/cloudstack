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

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.ObjectStore;
import com.cloud.storage.ScopeType;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=ObjectStore.class)
public class ObjectStoreResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the object store")
    private String id;

    @SerializedName("zoneid") @Param(description="the Zone ID of the object store")
    private String zoneId;

    @SerializedName(ApiConstants.ZONE_NAME) @Param(description="the Zone name of the object store")
    private String zoneName;

    @SerializedName("regionid") @Param(description="the Region ID of the object store")
    private Long regionId;

    @SerializedName("regionname") @Param(description="the Region name of the object store")
    private String regionName;

    @SerializedName("name") @Param(description="the name of the object store")
    private String name;

    @SerializedName("url") @Param(description="the url of the object store")
    private String url;

    @SerializedName("protocol") @Param(description="the protocol of the object store")
    private String protocol;

    @SerializedName("providername") @Param(description="the provider name of the object store")
    private String providerName;

    @SerializedName("scope") @Param(description="the scope of the object store")
    private ScopeType scope;

    @SerializedName("details") @Param(description="the details of the object store")
    private Set<ObjectStoreDetailResponse> details;


    public ObjectStoreResponse(){
        this.details = new LinkedHashSet<ObjectStoreDetailResponse>();
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


    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
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

    public Set<ObjectStoreDetailResponse> getDetails() {
        return details;
    }

    public void setDetails(Set<ObjectStoreDetailResponse> details) {
        this.details = details;
    }

    public void addDetail(ObjectStoreDetailResponse detail){
        this.details.add(detail);
    }



}
