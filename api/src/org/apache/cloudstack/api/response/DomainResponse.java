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

import com.cloud.domain.Domain;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = Domain.class)
public class DomainResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the domain")
    private String id;

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the domain")
    private String domainName;

    @SerializedName(ApiConstants.LEVEL) @Param(description="the level of the domain")
    private Integer level;

    @SerializedName("parentdomainid") @Param(description="the domain ID of the parent domain")
    private String parentDomainId;

    @SerializedName("parentdomainname") @Param(description="the domain name of the parent domain")
    private String parentDomainName;

    @SerializedName("haschild") @Param(description="whether the domain has one or more sub-domains")
    private boolean hasChild;

    @SerializedName(ApiConstants.NETWORK_DOMAIN) @Param(description="the network domain")
    private String networkDomain;

    @SerializedName(ApiConstants.PATH) @Param(description="the path of the domain")
    private String path;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getParentDomainId() {
        return parentDomainId;
    }

    public void setParentDomainId(String parentDomainId) {
        this.parentDomainId = parentDomainId;
    }

    public String getParentDomainName() {
        return parentDomainName;
    }

    public void setParentDomainName(String parentDomainName) {
        this.parentDomainName = parentDomainName;
    }

    public boolean getHasChild() {
        return hasChild;
    }

    public void setHasChild(boolean hasChild) {
        this.hasChild = hasChild;
    }

    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
