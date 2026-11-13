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
package org.apache.cloudstack.api.command.user.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceLimitResponse;

import com.cloud.configuration.Resource;
import com.cloud.configuration.ResourceLimit;
import com.cloud.exception.InvalidParameterValueException;

@APICommand(name = "listResourceLimits", description = "Lists resource limits.", responseObject = ResourceLimitResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListResourceLimitsCmd extends BaseListProjectAndAccountResourcesCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "Lists resource limits by ID.")
    private Long id;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.INTEGER, description = "Type of resource. " + ApiConstants.RESOURCE_TYPE_DESCRIPTION)
    private Integer resourceType;

    @Parameter(name = ApiConstants.RESOURCE_TYPE_NAME, type = CommandType.STRING, description = ApiConstants.RESOURCE_TYPE_NAME_DESCRIPTION)
    private String resourceTypeName;

    @Parameter(name = ApiConstants.TAG, type = CommandType.STRING, description = "Tag for the resource type", since = "4.20.0")
    private String tag;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Integer getResourceType() {
        return resourceType;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    public String getTag() {
        return tag;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        List<? extends ResourceLimit> result =
                _resourceLimitService.searchForLimits(id, _accountService.finalizeAccountId(this.getAccountName(), this.getDomainId(), this.getProjectId(), false), this.getDomainId(),
                        getResourceTypeEnum(), getTag(), this.getStartIndex(), this.getPageSizeVal());
        ListResponse<ResourceLimitResponse> response = new ListResponse<ResourceLimitResponse>();
        List<ResourceLimitResponse> limitResponses = new ArrayList<ResourceLimitResponse>();
        for (ResourceLimit limit : result) {
            ResourceLimitResponse resourceLimitResponse = _responseGenerator.createResourceLimitResponse(limit);
            resourceLimitResponse.setObjectName("resourcelimit");
            limitResponses.add(resourceLimitResponse);
        }

        response.setResponses(limitResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    private Resource.ResourceType getResourceTypeEnum() {
        // Map resource type
        Resource.ResourceType resourceTypeResult = null;
        if (resourceTypeName != null) {
            try {
                resourceTypeResult = Resource.ResourceType.valueOf(resourceTypeName);
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("Please specify a valid resource type name.");
            }
        } else if (resourceType != null) {
            resourceTypeResult = Resource.ResourceType.fromOrdinal(resourceType);
            if (resourceTypeResult == null) {
                throw new InvalidParameterValueException("Please specify a valid resource type.");
            }
        }

        return resourceTypeResult;
    }
}
