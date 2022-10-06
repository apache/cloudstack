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
package org.apache.cloudstack.api.command.user.userdata;

import java.util.ArrayList;
import java.util.List;

import com.cloud.user.UserData;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListProjectAndAccountResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.utils.Pair;

@APICommand(name = "listUserData", description = "List registered userdatas", responseObject = UserDataResponse.class, entityType = {UserData.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.18")
public class ListUserDataCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListUserDataCmd.class.getName());
    private static final String s_name = "listuserdataresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = UserDataResponse.class, description = "the ID of the Userdata")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Userdata name to look for")
    private String name;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends UserData>, Integer> resultList = _mgr.listUserDatas(this);
        List<UserDataResponse> responses = new ArrayList<>();
        for (UserData result : resultList.first()) {
            UserDataResponse r = _responseGenerator.createUserDataResponse(result);
            r.setObjectName(ApiConstants.USER_DATA);
            responses.add(r);
        }

        ListResponse<UserDataResponse> response = new ListResponse<>();
        response.setResponses(responses, resultList.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
