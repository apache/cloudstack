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
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.EventResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.event.Event;

@Implementation(description="A command to list events.", responseObject=EventResponse.class)
public class ListEventsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListEventsCmd.class.getName());

    private static final String s_name = "listeventsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account for the event. Must be used with the domainId parameter.")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID for the event. If used with the account parameter, returns all events for an account in the specified domain ID.")
    private Long domainId;

    @IdentityMapper(entityTableName="event")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the ID of the event")
    private Long id;
    
    @Parameter(name=ApiConstants.DURATION, type=CommandType.INTEGER, description="the duration of the event")
    private Integer duration;

    @Parameter(name=ApiConstants.END_DATE, type=CommandType.DATE, description="the end date range of the list you want to retrieve (use format \"yyyy-MM-dd\" or the new format \"yyyy-MM-dd HH:mm:ss\")")
    private Date endDate;

    @Parameter(name=ApiConstants.ENTRY_TIME, type=CommandType.INTEGER, description="the time the event was entered")
    private Integer entryTime;

    @Parameter(name=ApiConstants.LEVEL, type=CommandType.STRING, description="the event level (INFO, WARN, ERROR)")
    private String level;

    @Parameter(name=ApiConstants.START_DATE, type=CommandType.DATE, description="the start date range of the list you want to retrieve (use format \"yyyy-MM-dd\" or the new format \"yyyy-MM-dd HH:mm:ss\")")
    private Date startDate;

    @Parameter(name=ApiConstants.TYPE, type=CommandType.STRING, description="the event type (see event types)")
    private String type;
    
    @IdentityMapper(entityTableName="projects")
    @Parameter(name=ApiConstants.PROJECT_ID, type=CommandType.LONG, description="list events by projectId")
    private Long projectId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

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
    
    public Long getProjectId() {
        return projectId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        List<? extends Event> result = _mgr.searchForEvents(this);
        ListResponse<EventResponse> response = new ListResponse<EventResponse>();
        List<EventResponse> eventResponses = new ArrayList<EventResponse>();
        for (Event event : result) {
            eventResponses.add(_responseGenerator.createEventResponse(event));
        }

        response.setResponses(eventResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
