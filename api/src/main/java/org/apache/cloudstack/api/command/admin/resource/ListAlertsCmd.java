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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AlertResponse;
import org.apache.cloudstack.api.response.ListResponse;

import com.cloud.alert.Alert;
import com.cloud.utils.Pair;

@APICommand(name = "listAlerts", description = "Lists all alerts.", responseObject = AlertResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListAlertsCmd extends BaseListCmd {



    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = AlertResponse.class, description = "the ID of the alert")
    private Long id;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "list by alert type")
    private String type;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "list by alert name", since = "4.3")
    private String name;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends Alert>, Integer> result = _mgr.searchForAlerts(this);
        ListResponse<AlertResponse> response = new ListResponse<AlertResponse>();
        List<AlertResponse> alertResponseList = new ArrayList<AlertResponse>();
        for (Alert alert : result.first()) {
            AlertResponse alertResponse = new AlertResponse();
            alertResponse.setId(alert.getUuid());
            alertResponse.setAlertType(alert.getType());
            alertResponse.setDescription(alert.getSubject());
            alertResponse.setLastSent(alert.getLastSent());
            alertResponse.setName(alert.getName());

            alertResponse.setObjectName("alert");
            alertResponseList.add(alertResponse);
        }

        response.setResponses(alertResponseList, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
