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

import com.cloud.user.UserData;
import com.cloud.utils.Pair;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserDataResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listCniConfiguration", description = "List user data for CNI plugins", responseObject = UserDataResponse.class, entityType = {UserData.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.21.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListCniConfigurationCmd extends ListUserDataCmd {
    public static final Logger logger = LogManager.getLogger(ListCniConfigurationCmd.class.getName());

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends UserData>, Integer> resultList = _mgr.listUserDatas(this, true);
        List<UserDataResponse> responses = new ArrayList<>();
        for (UserData result : resultList.first()) {
            UserDataResponse r = _responseGenerator.createUserDataResponse(result);
            r.setObjectName(ApiConstants.CNI_CONFIG);
            responses.add(r);
        }

        ListResponse<UserDataResponse> response = new ListResponse<>();
        response.setResponses(responses, resultList.second());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }


}
