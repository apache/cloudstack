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
package org.apache.cloudstack.api.command.user.schedule;

import com.cloud.exception.InvalidParameterValueException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ResourceScheduleResponse;
import org.apache.cloudstack.schedule.ResourceScheduleManager;
import org.apache.commons.lang3.EnumUtils;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "listResourceSchedule", description = "List Resource Schedules", responseObject = ResourceScheduleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListResourceScheduleCmd extends BaseListCmd {

    @Inject
    ResourceScheduleManager resourceScheduleManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ResourceScheduleResponse.class, required = false, description = "ID of the schedule to be updated")
    private Long id;

    @Parameter(name = ApiConstants.IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = ResourceScheduleResponse.class, required = false, description = "comma separated list of schedule ids to be deleted")
    private List<Long> ids;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true, description = "Type of the resource")
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING, required = true, description = "ID of the resource for which schedule is to be defined")
    private String resourceId;

    @Parameter(name = ApiConstants.ACTION, type = CommandType.STRING, required = false, description = "Action to take on the resource.")
    private String action;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, required = false, description = "Enable schedule. Defaults to true")
    private Boolean enabled;

    public Long getId() {
        return id;
    }

    public List<Long> getIds() {
        return ids;
    }

    public ApiCommandResourceType getResourceType() {
        ApiCommandResourceType type = EnumUtils.getEnumIgnoreCase(ApiCommandResourceType.class, resourceType);
        if (type == null) {
            throw new InvalidParameterValueException("Unknown resource type: " + resourceType);
        }
        return type;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getAction() {
        return action;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public void execute() {
        ListResponse<ResourceScheduleResponse> response = resourceScheduleManager.listSchedule(
                getId(), getIds(), getResourceType(), getResourceId(), getAction(), getEnabled(),
                getStartIndex(), getPageSizeVal()
        );
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
