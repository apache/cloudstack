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
package org.apache.cloudstack.api.command.user.event;

import java.util.Date;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.EventResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

import com.cloud.event.Event;

@APICommand(name = "listEvents", description = "A command to list events.", responseObject = EventResponse.class, entityType = {Event.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListEventsCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListEventsCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = EventResponse.class, description = "the ID of the event")
    private Long id;

    @Parameter(name = ApiConstants.DURATION, type = CommandType.INTEGER, description = "the duration of the event")
    private Integer duration;

    @Parameter(name = ApiConstants.END_DATE,
               type = CommandType.DATE,
               description = "the end date range of the list you want to retrieve (use format \"yyyy-MM-dd\" or the new format \"yyyy-MM-dd HH:mm:ss\")")
    private Date endDate;

    @Parameter(name = ApiConstants.ENTRY_TIME, type = CommandType.INTEGER, description = "the time the event was entered")
    private Integer entryTime;

    @Parameter(name = ApiConstants.LEVEL, type = CommandType.STRING, description = "the event level (INFO, WARN, ERROR)")
    private String level;

    @Parameter(name = ApiConstants.START_DATE,
               type = CommandType.DATE,
               description = "the start date range of the list you want to retrieve (use format \"yyyy-MM-dd\" or the new format \"yyyy-MM-dd HH:mm:ss\")")
    private Date startDate;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "the event type (see event types)")
    private String type;

    @Parameter(name = ApiConstants.START_ID, type = CommandType.UUID, entityType = EventResponse.class, description = "the parent/start ID of the event, when provided this will list all the events with the start/parent ID including the parent event")
    private Long startId;

    @Parameter(name = ApiConstants.RESOURCE_ID, type = CommandType.STRING, description = "the ID of the resource associated with the event", since="4.17.0")
    private String resourceId;

    @Parameter(name = ApiConstants.RESOURCE_TYPE, type = CommandType.STRING, description = "the type of the resource associated with the event", since="4.17.0")
    private String resourceType;

    @Parameter(name = ApiConstants.ARCHIVED, type = CommandType.BOOLEAN, description = "true to list archived events otherwise false", since="4.19.0")
    private Boolean archived;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Integer getDuration() {
        return duration;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Integer getEntryTime() {
        return entryTime;
    }

    public String getLevel() {
        return level;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getType() {
        return type;
    }

    public Long getStartId() {
        return startId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public boolean getArchived() {
        return archived != null && archived;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {

        ListResponse<EventResponse> response = _queryService.searchForEvents(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
