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
package org.apache.cloudstack.api.command.admin.alert;

import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.alert.AlertService.AlertType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;

@APICommand(name = "generateAlert", description = "Generates an alert", responseObject = SuccessResponse.class, since = "4.3",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GenerateAlertCmd extends BaseAsyncCmd {

    public static final Logger s_logger = Logger.getLogger(GenerateAlertCmd.class.getName());


    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.TYPE, type = CommandType.SHORT, description = "Type of the alert", required = true)
    private Short type;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Name of the alert", required = true)
    private String name;

    @Parameter(name = ApiConstants.DESCRIPTION, type = CommandType.STRING, description = "Alert description", required = true, length = 999)
    private String description;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "Zone id for which alert is generated")
    private Long zoneId;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "Pod id for which alert is generated")
    private Long podId;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////
    public Short getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getZoneId() {
        if (zoneId == null) {
            return 0L;
        }
        return zoneId;
    }

    public Long getPodId() {
        return podId;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        AlertType alertType = AlertService.AlertType.generateAlert(getType(), getName());
        if (_alertSvc.generateAlert(alertType, getZoneId(), getPodId(), getDescription())) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to generate an alert");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.ALERT_GENERATE;
    }

    @Override
    public String getEventDescription() {
        return "Generating alert of type " + type + "; name " + name;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
