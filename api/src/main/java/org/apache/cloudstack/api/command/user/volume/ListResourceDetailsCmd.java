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

package org.apache.cloudstack.api.command.user.volume;

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceDetailResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;

import com.cloud.server.ResourceTag;

@APICommand(name = "listResourceDetails", description = "List resource detail(s)", responseObject = ResourceTagResponse.class, since = "4.2",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListResourceDetailsCmd extends BaseListProjectAndAccountResourcesCmd {

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, description = "list by resource type", required = true)
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING, description = "list by resource id")
    private String resourceId;

    @Parameter(name = ApiConstants.KEY, type = CommandType.STRING, description = "list by key")
    private String key;

    @Parameter(name = ApiConstants.VALUE, type = CommandType.STRING, description = "list by key, value. Needs to be passed only along with key" ,
            since = "4.4", authorized = { RoleType.Admin })
    private String value;

    @Parameter(name = ApiConstants.FOR_DISPLAY, type = CommandType.BOOLEAN, description = "if set to true, only details marked with display=true, are returned."
            + " False by default", since = "4.3", authorized = { RoleType.Admin })
    private Boolean forDisplay;

    public String getResourceId() {
        return resourceId;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Boolean getDisplay() {
        if (forDisplay != null) {
            return forDisplay;
        }
        return super.getDisplay();
    }

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {

        ListResponse<ResourceDetailResponse> response = new ListResponse<ResourceDetailResponse>();
        List<ResourceDetailResponse> resourceDetailResponse = _queryService.listResourceDetails(this);
        response.setResponses(resourceDetailResponse);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    public ResourceTag.ResourceObjectType getResourceType() {
        return resourceManagerUtil.getResourceType(resourceType);
    }

}
