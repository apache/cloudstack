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

import java.util.ArrayList;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.EventTypeResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.user.Account;

@APICommand(name = "listEventTypes", description = "List Event Types", responseObject = EventTypeResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListEventTypesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListEventTypesCmd.class.getName());

    @Override
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
