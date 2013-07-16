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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ConditionResponse;
import org.apache.cloudstack.api.response.CounterResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.context.CallContext;

import org.apache.log4j.Logger;

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.as.Condition;

@APICommand(name = "createCondition", description = "Creates a condition", responseObject = ConditionResponse.class)
public class CreateConditionCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateConditionCmd.class.getName());
    private static final String s_name = "conditionresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.COUNTER_ID, type = CommandType.UUID, entityType = CounterResponse.class,
            required = true, description = "ID of the Counter.")
    private long counterId;

    @Parameter(name = ApiConstants.RELATIONAL_OPERATOR, type = CommandType.STRING, required = true, description = "Relational Operator to be used with threshold.")
    private String relationalOperator;

    @Parameter(name = ApiConstants.THRESHOLD, type = CommandType.LONG, required = true, description = "Threshold value.")
    private Long threshold;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "the account of the condition. " +
    "Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "the domain ID of the account.")
    private Long domainId;

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public void create() throws ResourceAllocationException {
        Condition condition = null;
        condition = _autoScaleService.createCondition(this);

        if (condition != null) {
            this.setEntityId(condition.getId());
            this.setEntityUuid(condition.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create condition.");
        }
    }

    @Override
    public void execute() {
        Condition condition  = _entityMgr.findById(Condition.class, getEntityId());
        ConditionResponse response = _responseGenerator.createConditionResponse(condition);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    // /////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public Long getCounterId() {
        return counterId;
    }

    public String getRelationalOperator() {
        return relationalOperator;
    }

    public String getAccountName() {
        if (accountName == null) {
            return CallContext.current().getCallingAccount().getAccountName();
        }

        return accountName;
    }

    public Long getDomainId() {
        if (domainId == null) {
            return CallContext.current().getCallingAccount().getDomainId();
        }
        return domainId;
    }

    public Long getThreshold() {
        return threshold;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Condition;
    }

    @Override
    public String getEventDescription() {
        return "creating a condition";
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_CONDITION_CREATE;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = finalyzeAccountId(accountName, domainId, null, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }


}
