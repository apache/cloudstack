package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SystemVmResponse;
import com.cloud.async.AsyncJob;
import com.cloud.event.EventTypes;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.vm.VirtualMachine;

@Implementation(responseObject=SystemVmResponse.class, description="Destroyes a system virtual machine.")
public class DestroySystemVmCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(DestroySystemVmCmd.class.getName());
    
    private static final String s_name = "destroysystemvmresponse";

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="The ID of the system virtual machine")
    private Long id;
    
    public Long getId() {
        return id;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
        return "systemvm"; 
    }

    @Override
    public long getEntityOwnerId() {
        Account account = UserContext.current().getCaller();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_SSVM_START;
    }

    @Override
    public String getEventDescription() {
        return  "destroying system vm: " + getId();
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
        return AsyncJob.Type.SystemVm;
    }
    
    @Override
    public Long getInstanceId() {
        return getId();
    }
    
    @Override
    public void execute(){
        VirtualMachine instance = _mgr.destroySystemVM(this);
        if (instance != null) {
            SystemVmResponse response = _responseGenerator.createSystemVmResponse(instance);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Fail to destroy system vm");
        }
    }
}
