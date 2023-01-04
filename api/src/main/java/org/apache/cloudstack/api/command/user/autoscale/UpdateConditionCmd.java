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

package org.apache.cloudstack.api.command.user.autoscale;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ConditionResponse;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.as.Condition;
import com.cloud.user.Account;

@APICommand(name = "updateCondition", description = "Updates a condition for VM auto scaling", responseObject = SuccessResponse.class, entityType = {Condition.class},
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.18.0")
public class UpdateConditionCmd extends BaseAsyncCmd {
    public static final Logger LOGGER = Logger.getLogger(UpdateConditionCmd.class.getName());

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ConditionResponse.class, required = true, description = "the ID of the condition.")
    private Long id;

    @Parameter(name = ApiConstants.RELATIONAL_OPERATOR, type = CommandType.STRING, required = true, description = "Relational Operator to be used with threshold. Valid values are EQ, GT, LT, GE, LE.")
    private String relationalOperator;

    @Parameter(name = ApiConstants.THRESHOLD, type = CommandType.LONG, required = true, description = "Value for which the Counter will be evaluated with the Operator selected.")
    private Long threshold;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void execute() {
        try {
            Condition condition = _autoScaleService.updateCondition(this);
            ConditionResponse response = _responseGenerator.createConditionResponse(condition);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (ResourceInUseException ex) {
            LOGGER.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_IN_USE_ERROR, ex.getMessage());
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getRelationalOperator() {
        return relationalOperator;
    }

    public Long getThreshold() {
        return threshold;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Condition;
    }

    @Override
    public long getEntityOwnerId() {
        Condition condition = _entityMgr.findById(Condition.class, getId());
        if (condition != null) {
            return condition.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CONDITION_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating a condition.";
    }
}
