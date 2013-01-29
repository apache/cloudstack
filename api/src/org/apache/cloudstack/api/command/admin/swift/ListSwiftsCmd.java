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
package org.apache.cloudstack.api.command.admin.swift;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SwiftResponse;
import com.cloud.storage.Swift;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "listSwifts", description = "List Swift.", responseObject = HostResponse.class, since="3.0.0")
public class ListSwiftsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSwiftsCmd.class.getName());
    private static final String s_name = "listswiftsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, description = "the id of the swift")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }


    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute(){
        Pair<List<? extends Swift>, Integer> result = _resourceService.listSwifts(this);
        ListResponse<SwiftResponse> response = new ListResponse<SwiftResponse>();
        List<SwiftResponse> swiftResponses = new ArrayList<SwiftResponse>();

        if (result != null) {
            for (Swift swift : result.first()) {
                SwiftResponse swiftResponse = _responseGenerator.createSwiftResponse(swift);
                swiftResponse.setResponseName(getCommandName());
                swiftResponse.setObjectName("swift");
                swiftResponses.add(swiftResponse);
            }
        }
        response.setResponses(swiftResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
