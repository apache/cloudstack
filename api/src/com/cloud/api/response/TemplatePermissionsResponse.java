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

import java.util.List;

import com.cloud.api.ApiConstants;
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class TemplatePermissionsResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the template ID")
    private IdentityProxy id = new IdentityProxy("vm_template");

    @SerializedName(ApiConstants.IS_PUBLIC) @Param(description="true if this template is a public template, false otherwise")
    private Boolean publicTemplate;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the domain to which the template belongs")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the list of accounts the template is available for")
    private List<String> accountNames;
    
    @SerializedName(ApiConstants.PROJECT_IDS) @Param(description="the list of projects the template is available for")
    private List<String> projectIds;
    

    public void setId(Long id) {
        this.id.setValue(id);
    }

    public void setPublicTemplate(Boolean publicTemplate) {
        this.publicTemplate = publicTemplate;
    }

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }

    public void setAccountNames(List<String> accountNames) {
        this.accountNames = accountNames;
    }

    public void setProjectIds(List<String> projectIds) {
        this.projectIds = projectIds;
    }
}
