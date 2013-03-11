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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AlertResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;

@APICommand(name = "deleteAlerts", description = "Delete one or more alerts.", responseObject = SuccessResponse.class)
public class DeleteAlertsCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(DeleteAlertsCmd.class.getName());

    private static final String s_name = "deletealertsresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.IDS, type = CommandType.LIST,  collectionType = CommandType.UUID, entityType = AlertResponse.class,
            description = "the IDs of the alerts")
    private List<Long> ids;

    @Parameter(name=ApiConstants.OLDER_THAN, type=CommandType.DATE, description="delete alerts older than (including) this date (use format \"yyyy-MM-dd\")")
    private Date olderThan;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "delete by alert type")
    private String type;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public List<Long> getIds() {
        return ids;
    }

    public Date getOlderThan() {
        return olderThan;
    }

    public String getType() {
        return type;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        if(ids == null && type == null && olderThan == null) {
            throw new InvalidParameterValueException("either ids, type or olderthan must be specified");
        }
        boolean result = _mgr.deleteAlerts(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to delete Alerts, one or more parameters has invalid values");
        }
    }
}
