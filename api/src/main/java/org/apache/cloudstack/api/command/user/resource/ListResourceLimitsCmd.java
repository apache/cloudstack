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

import com.cloud.configuration.Resource;
import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceLimitResponse;
import org.apache.log4j.Logger;

import com.cloud.configuration.ResourceLimit;

@APICommand(name = "listResourceLimits", description = "Lists resource limits.", responseObject = ResourceLimitResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListResourceLimitsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListResourceLimitsCmd.class.getName());

    private static final String s_name = "listresourcelimitsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "Lists resource limits by ID.")
    private Long id;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.INTEGER, description = "Type of resource. Values are 0, 1, 2, 3, 4, 6, 7, 8, 9, 10 and 11. "
        + "0 - Instance. Number of instances a user can create. "
        + "1 - IP. Number of public IP addresses an account can own. "
        + "2 - Volume. Number of disk volumes an account can own. "
        + "3 - Snapshot. Number of snapshots an account can own. "
        + "4 - Template. Number of templates an account can register/create. "
        + "5 - Project. Number of projects an account can own. "
        + "6 - Network. Number of networks an account can own. "
        + "7 - VPC. Number of VPC an account can own. "
        + "8 - CPU. Number of CPU an account can allocate for their resources. "
        + "9 - Memory. Amount of RAM an account can allocate for their resources. "
        + "10 - PrimaryStorage. Total primary storage space (in GiB) a user can use. "
        + "11 - SecondaryStorage. Total secondary storage space (in GiB) a user can use. ")
    private Integer resourceType;

    @Parameter(name = ApiConstants.RESOURCE_TYPE_NAME, type = CommandType.STRING, description = "Type of resource (wins over resourceType if both are provided). Values are: "
            + "user_vm - Instance. Number of instances a user can create. "
            + "public_ip - IP. Number of public IP addresses an account can own. "
            + "volume - Volume. Number of disk volumes an account can own. "
            + "snapshot - Snapshot. Number of snapshots an account can own. "
            + "template - Template. Number of templates an account can register/create. "
            + "project - Project. Number of projects an account can own. "
            + "network - Network. Number of networks an account can own. "
            + "vpc - VPC. Number of VPC an account can own. "
            + "cpu - CPU. Number of CPU an account can allocate for their resources. "
            + "memory - Memory. Amount of RAM an account can allocate for their resources. "
            + "primary_storage - PrimaryStorage. Total primary storage space (in GiB) a user can use. "
            + "secondary_storage - SecondaryStorage. Total secondary storage space (in GiB) a user can use. ")
    private String resourceTypeName;

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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        List<? extends ResourceLimit> result =
                _resourceLimitService.searchForLimits(id, _accountService.finalyzeAccountId(this.getAccountName(), this.getDomainId(), this.getProjectId(), false), this.getDomainId(),
                        getResourceTypeEnum(), this.getStartIndex(), this.getPageSizeVal());
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
