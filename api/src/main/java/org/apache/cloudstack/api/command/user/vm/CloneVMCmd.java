package org.apache.cloudstack.api.command.user.vm;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.log4j.Logger;

@APICommand(name = "cloneVirtualMachine", responseObject = UserVmResponse.class, description = "clone a virtual VM in full clone mode",
        responseView = ResponseObject.ResponseView.Restricted, requestHasSensitiveInfo = false, responseHasSensitiveInfo = true, entityType = {VirtualMachine.class})
public class CloneVMCmd extends BaseAsyncCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(CloneVMCmd.class.getName());
    private static final String s_name = "clonevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType=UserVmResponse.class,
            required = true, description = "The ID of the virtual machine")
    private Long id;

    public Long getId() {
        return this.id;
    }
    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_CLONE;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.VirtualMachine;
    }

    @Override
    public String getEventDescription() {
        return "Cloning user VM: " + this._uuidMgr.getUuid(VirtualMachine.class, getId());
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", null).get(0);
        response.setResponseName("test_clone");
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        UserVm vm = this._responseGenerator.findUserVmById(getId());
        if (vm != null) {
            return vm.getAccountId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
