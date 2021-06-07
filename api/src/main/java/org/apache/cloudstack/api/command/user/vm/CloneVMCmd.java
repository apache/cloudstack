package org.apache.cloudstack.api.command.user.vm;

import com.cloud.exception.*;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserVmResponse;

@APICommand(name = "cloneVirtualMachine", responseObject = UserVmResponse.class, description = "clone a virtual VM in full clone mode",
        responseView = ResponseObject.ResponseView.Restricted, requestHasSensitiveInfo = false, responseHasSensitiveInfo = true)
public class CloneVMCmd extends BaseAsyncCmd implements UserCmd {

    @Override
    public String getEventType() {
        return null;
    }

    @Override
    public String getEventDescription() {
        return null;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {

    }

    @Override
    public String getCommandName() {
        return null;
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
