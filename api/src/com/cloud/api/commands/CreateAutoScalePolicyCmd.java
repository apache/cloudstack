// Copyright 2012 Citrix Systems, Inc. Licensed under the
// Apache License, Version 2.0 (the "License"); you may not use this
// file except in compliance with the License.  Citrix Systems, Inc.
// reserves all rights not expressly granted by the License.
// You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.AutoScalePolicyResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.network.as.Condition;

@Implementation(description = "Creates an autoscale policy for a provision or deprovision action, the action is taken when the all the conditions evaluates to true for the specified duration. The policy is in effect once it is attached to a autscale vm group.", responseObject = AutoScalePolicyResponse.class)
public class CreateAutoScalePolicyCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateAutoScalePolicyCmd.class.getName());

    private static final String s_name = "autoscalepolicyresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACTION, type = CommandType.STRING, required = true, description = "the action to be executed if all the conditions evaluate to true for the specified duration.")
    private String action;

    @Parameter(name = ApiConstants.DURATION, type = CommandType.INTEGER, required = true, description = "the duration for which the conditions have to be true before action is taken")
    private int duration;

    @Parameter(name = ApiConstants.QUIETTIME, type = CommandType.INTEGER, description = "the cool down period for which the policy should not be evaluated after the action has been taken")
    private Integer quietTime;

    @IdentityMapper(entityTableName = "conditions")
    @Parameter(name = ApiConstants.CONDITION_IDS, type = CommandType.LIST, collectionType = CommandType.LONG, required = true, description = "the list of IDs of the conditions that are being evaluated on every interval")
    private List<Long> conditionIds;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    private Long conditionDomainId;
    private Long conditionAccountId;

    @Override
    public String getEntityTable() {
        return "autoscale_policies";
    }

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

    public long getAccountId()
    {
        if (conditionAccountId == null)
            getEntityOwnerId();
        return conditionAccountId;
    }

    public long getDomainId()
    {
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
        conditionDomainId = condition.getDomainId();
        conditionAccountId = condition.getAccountId();

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
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.AutoScalePolicy;
    }

    @Override
    public void execute() {
        AutoScalePolicy result = _entityMgr.findById(AutoScalePolicy.class, getEntityId());
        AutoScalePolicyResponse response = _responseGenerator.createAutoScalePolicyResponse(result);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public void create() throws ResourceAllocationException {
        AutoScalePolicy result = _autoScaleService.createAutoScalePolicy(this);
        if (result != null) {
            this.setEntityId(result.getId());
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create AutoScale Policy");
        }
    }
}
