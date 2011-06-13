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

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.response.EventTypeResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.user.Account;

@Implementation(description = "List Event Types", responseObject = EventTypeResponse.class)
public class ListEventTypesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListEventTypesCmd.class.getName());
    private static final String s_name = "listeventtypesresponse";

    @Override
    public String getCommandName() {
        return s_name;
    }

    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        String[] result = _mgr.listEventTypes();
        ListResponse<EventTypeResponse> response = new ListResponse<EventTypeResponse>();
        ArrayList<EventTypeResponse> responses = new ArrayList<EventTypeResponse>();
        if (result != null) {
            for (String eventType : result) {
                EventTypeResponse eventTypeResponse = new EventTypeResponse();
                eventTypeResponse.setName(eventType);
                eventTypeResponse.setObjectName("eventtype");
                responses.add(eventTypeResponse);
            }
        }
        response.setResponses(responses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
