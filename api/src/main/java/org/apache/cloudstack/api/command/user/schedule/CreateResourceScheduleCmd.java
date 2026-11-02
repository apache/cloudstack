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
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ResourceScheduleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.schedule.ResourceScheduleManager;
import org.apache.commons.lang3.EnumUtils;

import javax.inject.Inject;
import java.util.Date;
import java.util.Map;

@APICommand(name = "createResourceSchedule", description = "Create Resource Schedule", responseObject = ResourceScheduleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateResourceScheduleCmd extends BaseCmd {

    @Inject
    ResourceScheduleManager resourceScheduleManager;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, required = true, description = "Type of the resource")
    private String resourceType;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING, required = true, description = "ID of the resource for which schedule is to be defined")
    private String resourceId;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, required = false, description = "Description of the schedule")
    private String description;

    @Parameter(name = ApiConstants.SCHEDULE, type = CommandType.STRING, required = true, description = "Schedule for action on resource in cron format. e.g. '0 15 10 * *' for 'at 15:00 on 10th day of every month'")
    private String schedule;

    @Parameter(name = ApiConstants.TIMEZONE, type = CommandType.STRING, required = true, description = "Specifies a timezone for this command. For more information on the timezone parameter, see TimeZone Format.")
    private String timeZone;

    @Parameter(name = ApiConstants.ACTION, type = CommandType.STRING, required = true, description = "Action to take on the resource.")
    private String action;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = false, description = "Start date from which the schedule becomes active. Defaults to current date plus 1 minute. (Format \"yyyy-MM-dd hh:mm:ss\")")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, required = false, description = "End date after which the schedule becomes inactive. (Format \"yyyy-MM-dd hh:mm:ss\")")
    private Date endDate;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, required = false, description = "Enable schedule. Defaults to true")
    private Boolean enabled;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, required = false, description = "Map of (key/value pairs) details for the schedule.")
    private Map details;

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

    public String getDescription() {
        return description;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public String getAction() {
        return action;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Boolean getEnabled() {
        if (enabled == null) {
            enabled = true;
        }
        return enabled;
    }

    public Map<String, String> getDetails() {
        return convertDetailsToMap(details);
    }

    @Override
    public void execute() {
        ResourceScheduleResponse response = resourceScheduleManager.createSchedule(getResourceType(), getResourceId(),
                getDescription(), getSchedule(), getTimeZone(), getAction(), getStartDate(), getEndDate(), getEnabled(), getDetails());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getAccountId();
    }
}
