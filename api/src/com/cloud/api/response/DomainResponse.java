/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DomainResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the ID of the domain")
    private IdentityProxy id = new IdentityProxy("domain");

    @SerializedName(ApiConstants.NAME) @Param(description="the name of the domain")
    private String domainName;

    @SerializedName(ApiConstants.LEVEL) @Param(description="the level of the domain")
    private Integer level;

    @SerializedName("parentdomainid") @Param(description="the domain ID of the parent domain")
    private IdentityProxy parentDomainId = new IdentityProxy("domain");

    @SerializedName("parentdomainname") @Param(description="the domain name of the parent domain")
    private String parentDomainName;

    @SerializedName("haschild") @Param(description="whether the domain has one or more sub-domains")
    private boolean hasChild;
    
    @SerializedName(ApiConstants.NETWORK_DOMAIN) @Param(description="the network domain")
    private String networkDomain;

    @SerializedName(ApiConstants.PATH) @Param(description="the path of the domain")
    private String path;
    
    public Long getId() {
        return id.getValue();
    }

    public void setId(Long id) {
        this.id.setValue(id);
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

    public Long getParentDomainId() {
        return parentDomainId.getValue();
    }

    public void setParentDomainId(Long parentDomainId) {
        this.parentDomainId.setValue(parentDomainId);
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
