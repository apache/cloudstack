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

import com.cloud.server.ResourceTag;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceDetailResponse;
import org.apache.cloudstack.api.response.ResourceTagResponse;

import java.util.List;

@APICommand(name = "listResourceDetails", description = "List resource detail(s)", responseObject = ResourceTagResponse.class, since = "4.2")
public class ListResourceDetailsCmd extends BaseListProjectAndAccountResourcesCmd{
    private static final String s_name = "listresourcedetailsresponse";

    @Parameter(name=ApiConstants.RESOURCE_TYPE, type=CommandType.STRING, description="list by resource type")
    private String resourceType;

    @Parameter(name=ApiConstants.RESOURCE_ID, type=CommandType.STRING, description="list by resource id")
    private String resourceId;

    @Parameter(name=ApiConstants.KEY, type=CommandType.STRING, description="list by key")
    private String key;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {

        ListResponse<ResourceDetailResponse> response = new ListResponse<ResourceDetailResponse>();
        List<ResourceDetailResponse> resourceDetailResponse = _queryService.listResource(this);
        response.setResponses(resourceDetailResponse);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    public ResourceTag.ResourceObjectType getResourceType() {
        return _taggedResourceService.getResourceType(resourceType);
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getKey() {
        return key;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
