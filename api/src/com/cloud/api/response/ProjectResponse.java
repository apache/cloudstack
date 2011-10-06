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
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ProjectResponse extends BaseResponse{
    
    @SerializedName(ApiConstants.ID) @Param(description="the id of the project")
    private Long id;
    
    @SerializedName(ApiConstants.NAME) @Param(description="the name of the project")
    private String name;
    
    @SerializedName(ApiConstants.DISPLAY_TEXT) @Param(description="the displaytext of the project")
    private String displaytext;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the domain id the project belongs to")
    private Long domainId;
    
    @SerializedName(ApiConstants.DOMAIN) @Param(description="the domain name where the project belongs to")
    private String domain;
    
    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the account name of the project's owner")
    private String ownerName;
    
    @SerializedName(ApiConstants.STATE) @Param(description="the state of the project")
    private String state;

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplaytext(String displaytext) {
        this.displaytext = displaytext;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setOwner(String owner) {
        this.ownerName = owner;
    }

    public void setState(String state) {
        this.state = state;
    }
}
