package com.cloud.ucs.manager;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
@APICommand(description="associate a profile to a blade", responseObject=AssociateUcsProfileToBladesInClusterResponse.class)
public class AssociateUcsProfileToBladeCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AssociateUcsProfileToBladeCmd.class);
    
    @Inject
    private UcsManager mgr;
    
    private Long ucsManagerId;
    private String profileDn;
    private Long bladeId;
    
    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
            ResourceAllocationException, NetworkRuleConflictException {
        try {
            mgr.associateProfileToBlade(this);
            AssociateUcsProfileToBladesInClusterResponse rsp = new AssociateUcsProfileToBladesInClusterResponse();
            rsp.setResponseName(getCommandName());
            rsp.setObjectName("associateucsprofiletobalde");
            this.setResponseObject(rsp);
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());   
        }
    }

    @Override
    public String getCommandName() {
        return "associateucsprofiletobladeresponse";
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    public Long getUcsManagerId() {
        return ucsManagerId;
    }

    public void setUcsManagerId(Long ucsManagerId) {
        this.ucsManagerId = ucsManagerId;
    }

    public String getProfileDn() {
        return profileDn;
    }

    public void setProfileDn(String profileDn) {
        this.profileDn = profileDn;
    }

    public Long getBladeId() {
        return bladeId;
    }

    public void setBladeId(Long bladeId) {
        this.bladeId = bladeId;
    }
}
