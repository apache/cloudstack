package com.cloud.ucs.manager;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
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
@APICommand(description="List ucs manager", responseObject=ListUcsManagerResponse.class)
public class ListUcsManagerCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListUcsManagerCmd.class);
    
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the zone id", required=true)
    private Long zoneId;
    
    @Inject
    private UcsManager mgr;
    
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException, NetworkRuleConflictException {
        try {
            ListResponse<ListUcsManagerResponse> response  = mgr.listUcsManager(this);            
            response.setResponseName(getCommandName());
            response.setObjectName("ucsmanager");
            this.setResponseObject(response);
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());  
        }
    }

    @Override
    public String getCommandName() {
        return "listucsmanagerreponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }
}
