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
package org.apache.cloudstack.api.command.admin.resource;

import com.cloud.user.Account;
import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.response.AlertResponse;
import org.apache.cloudstack.api.response.AlertTypeResponse;
import org.apache.cloudstack.api.response.ListResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@APICommand(name = "listAlertTypes", description = "Lists all alerts types", responseObject = AlertResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListAlertTypesCmd extends BaseCmd {

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        Set<AlertService.AlertType> result = AlertService.AlertType.getAlertTypes();
        ListResponse<AlertTypeResponse> response = new ListResponse<>();
        List<AlertTypeResponse> typeResponseList = new ArrayList<>();
        for (AlertService.AlertType alertType : result) {
            AlertTypeResponse alertResponse = new AlertTypeResponse(alertType.getType(), alertType.getName());
            alertResponse.setObjectName("alerttype");
            typeResponseList.add(alertResponse);
        }
        response.setResponses(typeResponseList, result.size());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
