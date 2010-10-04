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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.EventResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.event.EventVO;
import com.cloud.user.User;

@Implementation(method="searchForEvents")
public class ListEventsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListEventsCmd.class.getName());

    private static final String s_name = "listeventsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="duration", type=CommandType.INTEGER)
    private Integer duration;

    @Parameter(name="enddate", type=CommandType.DATE)
    private Date endDate;

    @Parameter(name="entrytime", type=CommandType.INTEGER)
    private Integer entryTime;

    @Parameter(name="level", type=CommandType.STRING)
    private String level;

    @Parameter(name="startdate", type=CommandType.DATE)
    private Date startDate;

    @Parameter(name="type", type=CommandType.STRING)
    private String type;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
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

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<EventResponse> getResponse() {
        List<EventVO> events = (List<EventVO>)getResponseObject();

        ListResponse<EventResponse> response = new ListResponse<EventResponse>();
        List<EventResponse> eventResponses = new ArrayList<EventResponse>();
        for (EventVO event : events) {
            EventResponse responseEvent = new EventResponse();
            responseEvent.setAccountName(event.getAccountName());
            responseEvent.setCreated(event.getCreateDate());
            responseEvent.setDescription(event.getDescription());
            responseEvent.setDomainId(event.getDomainId());
            responseEvent.setEventType(event.getType());
            responseEvent.setId(event.getId());
            responseEvent.setLevel(event.getLevel());
            responseEvent.setParentId(event.getStartId());
            responseEvent.setState(event.getState());
            responseEvent.setDomainName(ApiDBUtils.findDomainById(event.getDomainId()).getName());
            User user = ApiDBUtils.findUserById(event.getUserId());
            if (user != null) {
                responseEvent.setUsername(user.getUsername());
            }

            responseEvent.setResponseName("event");
            eventResponses.add(responseEvent);
        }

        response.setResponses(eventResponses);
        response.setResponseName(getName());
        return response;
    }
}
