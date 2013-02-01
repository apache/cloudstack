package com.cloud.ucs.manager;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseCmd.CommandType;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.server.ManagementService;
import com.cloud.user.Account;
@APICommand(description="List profile in ucs manager", responseObject=ListUcsProfileResponse.class)
public class ListUcsProfileCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListUcsProfileCmd.class);
    
    @Inject UcsManager mgr;
    
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the id for the ucs manager", required=true)
    private Long ucsManagerId;

    public Long getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(Long ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException, NetworkRuleConflictException {
        try {
            ListResponse<ListUcsProfileResponse> response = mgr.listUcsProfiles(this);
            response.setResponseName(getCommandName());
            response.setObjectName("ucsprofile");
            this.setResponseObject(response);
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());  
        }
    }

    @Override
    public String getCommandName() {
        return "listucsprofileresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
