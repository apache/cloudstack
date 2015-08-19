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

import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AutoScalePolicyResponse;
import org.apache.cloudstack.api.response.ConditionResponse;

import com.cloud.domain.Domain;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.Condition;
import com.cloud.user.Account;

@APICommand(name = "createAutoScalePolicy",
            description = "Creates an autoscale policy for a provision or deprovision action, the action is taken when the all the conditions evaluates to true for the specified duration. The policy is in effect once it is attached to a autscale vm group.",
        responseObject = AutoScalePolicyResponse.class, entityType = {AutoScalePolicy.class},
            requestHasSensitiveInfo = false,
            responseHasSensitiveInfo = false)
public class CreateAutoScalePolicyCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateAutoScalePolicyCmd.class.getName());

    private static final String s_name = "autoscalepolicyresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACTION,
               type = CommandType.STRING,
               required = true,
               description = "the action to be executed if all the conditions evaluate to true for the specified duration.")
    private String action;

    @Parameter(name = ApiConstants.DURATION,
               type = CommandType.INTEGER,
               required = true,
               description = "the duration for which the conditions have to be true before action is taken")
    private int duration;

    @Parameter(name = ApiConstants.QUIETTIME,
               type = CommandType.INTEGER,
               description = "the cool down period for which the policy should not be evaluated after the action has been taken")
    private Integer quietTime;

    @Parameter(name = ApiConstants.CONDITION_IDS,
               type = CommandType.LIST,
               collectionType = CommandType.UUID,
               entityType = ConditionResponse.class,
               required = true,
               description = "the list of IDs of the conditions that are being evaluated on every interval")
    private List<Long> conditionIds;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    private Long conditionDomainId;
    private Long conditionAccountId;

    public int getDuration() {
        return duration;
    }

    public Integer getQuietTime() {
        return quietTime;
    }

    public String getAction() {
        return action;
    }

    public List<Long> getConditionIds() {
        return conditionIds;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "autoscalepolicy";
    }

    public long getAccountId() {
        if (conditionAccountId == null)
            getEntityOwnerId();
        return conditionAccountId;
    }

    public long getDomainId() {
        if (conditionDomainId == null) {
            getEntityOwnerId();
        }

        return conditionDomainId;
    }

    @Override
    public long getEntityOwnerId() {
        if (conditionAccountId != null) {
            return conditionAccountId;
        }
        long conditionId = getConditionIds().get(0);
        Condition condition = _entityMgr.findById(Condition.class, conditionId);
        if (condition == null) {
            // it is an invalid condition, return system account, error will be thrown later.
            conditionDomainId = Domain.ROOT_DOMAIN;
            conditionAccountId = Account.ACCOUNT_ID_SYSTEM;
        } else {
            conditionDomainId = condition.getDomainId();
            conditionAccountId = condition.getAccountId();
        }

        return conditionAccountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_AUTOSCALEPOLICY_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating AutoScale Policy";
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.AutoScalePolicy;
    }

    @Override
    public void execute() {
        AutoScalePolicy result = _entityMgr.findById(AutoScalePolicy.class, getEntityId());
        AutoScalePolicyResponse response = _responseGenerator.createAutoScalePolicyResponse(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public void create() throws ResourceAllocationException {
        AutoScalePolicy result = _autoScaleService.createAutoScalePolicy(this);
        if (result != null) {
            setEntityId(result.getId());
            setEntityUuid(result.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create AutoScale Policy");
        }
    }
}
