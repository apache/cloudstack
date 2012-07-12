package com.cloud.api.commands;

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.AutoScalePolicyResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.network.as.AutoScalePolicy;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description = "Updates an existing autoscale policy.", responseObject = AutoScalePolicyResponse.class)
public class UpdateAutoScalePolicyCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateAutoScalePolicyCmd.class.getName());

    private static final String s_name = "updateautoscalepolicyresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DURATION, type = CommandType.INTEGER, description = "the duration for which the conditions have to be true before action is taken")
    private Integer duration;

    @Parameter(name = ApiConstants.QUIETTIME, type = CommandType.INTEGER, description = "the cool down period for which the policy should not be evaluated after the action has been taken")
    private Integer quietTime;

    @IdentityMapper(entityTableName = "conditions")
    @Parameter(name = ApiConstants.CONDITION_IDS, type = CommandType.LIST, collectionType = CommandType.LONG, required = true, description = "the list of IDs of the conditions that are being evaluated on every interval")
    private List<Long> conditionIds;

    @IdentityMapper(entityTableName = "autoscale_policies")
    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = true, description = "the ID of the autoscale policy")
    private Long id;

    @Override
    public void execute() throws ServerApiException {
        UserContext.current().setEventDetails("AutoScale Policy Id: " + getId());
        AutoScalePolicy result = _autoScaleService.updateAutoScalePolicy(this);
        if (result != null) {
            AutoScalePolicyResponse response = _responseGenerator.createAutoScalePolicyResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update autoscale policy");
        }
    }

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Integer getDuration() {
        return duration;
    }

    public Integer getQuietTime() {
        return quietTime;
    }


    public List<Long> getConditionIds() {
        return conditionIds;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        AutoScalePolicy autoScalePolicy = _entityMgr.findById(AutoScalePolicy.class, getId());
        if (autoScalePolicy != null) {
            return autoScalePolicy.getAccountId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are
        // tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_AUTOSCALEPOLICY_UPDATE;
    }

    @Override
    public String getEventDescription() {
        return "Updating Auto Scale Policy.";
    }

    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.AutoScalePolicy;
    }
}
