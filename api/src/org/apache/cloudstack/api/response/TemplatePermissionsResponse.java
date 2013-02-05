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

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.template.VirtualMachineTemplate;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=VirtualMachineTemplate.class)
@SuppressWarnings("unused")
public class TemplatePermissionsResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the template ID")
    private String id;

    @SerializedName(ApiConstants.IS_PUBLIC) @Param(description="true if this template is a public template, false otherwise")
    private Boolean publicTemplate;

    @SerializedName(ApiConstants.DOMAIN_ID) @Param(description="the ID of the domain to which the template belongs")
    private String domainId;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="the list of accounts the template is available for")
    private List<String> accountNames;

    @SerializedName(ApiConstants.PROJECT_IDS) @Param(description="the list of projects the template is available for")
    private List<String> projectIds;


    public void setId(String id) {
        this.id = id;
    }

    public void setPublicTemplate(Boolean publicTemplate) {
        this.publicTemplate = publicTemplate;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public void setAccountNames(List<String> accountNames) {
        this.accountNames = accountNames;
    }

    public void setProjectIds(List<String> projectIds) {
        this.projectIds = projectIds;
    }
}
