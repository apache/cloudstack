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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ResourceScheduleResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.schedule.ResourceScheduleManager;

import javax.inject.Inject;
import java.util.Date;
import java.util.Map;

@APICommand(name = "updateResourceSchedule", description = "Update Resource Schedule", responseObject = ResourceScheduleResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.23.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class UpdateResourceScheduleCmd extends BaseCmd {

    @Inject
    ResourceScheduleManager resourceScheduleManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ResourceScheduleResponse.class, required = true, description = "ID of the schedule to be updated")
    private Long id;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, required = false, description = "Description of the schedule")
    private String description;

    @Parameter(name = ApiConstants.SCHEDULE, type = CommandType.STRING, required = false, description = "Schedule for action on resource in cron format.")
    private String schedule;

    @Parameter(name = ApiConstants.TIMEZONE, type = CommandType.STRING, required = false, description = "Specifies a timezone for this command.")
    private String timeZone;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = false, description = "Start date from which the schedule becomes active.")
    private Date startDate;

    @Parameter(name = ApiConstants.END_DATE, type = CommandType.DATE, required = false, description = "End date after which the schedule becomes inactive.")
    private Date endDate;

    @Parameter(name = ApiConstants.ENABLED, type = CommandType.BOOLEAN, required = false, description = "Enable or disable the schedule.")
    private Boolean enabled;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, required = false, description = "Map of (key/value pairs) details for the schedule.")
    private Map details;

    public Long getId() {
        return id;
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

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Map<String, String> getDetails() {
        return convertDetailsToMap(details);
    }

    @Override
    public void execute() {
        ResourceScheduleResponse response = resourceScheduleManager.updateSchedule(getId(), getDescription(), getSchedule(),
                getTimeZone(), getStartDate(), getEndDate(), getEnabled(), getDetails());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getAccountId();
    }
}
