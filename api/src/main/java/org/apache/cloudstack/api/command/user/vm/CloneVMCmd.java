package org.apache.cloudstack.api.command.user.vm;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import java.util.Optional;

@APICommand(name = "cloneVirtualMachine", responseObject = UserVmResponse.class, description = "clone a virtual VM in full clone mode",
        responseView = ResponseObject.ResponseView.Restricted, requestHasSensitiveInfo = false, responseHasSensitiveInfo = true, entityType = {VirtualMachine.class})
public class CloneVMCmd extends BaseAsyncCreateCustomIdCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(CloneVMCmd.class.getName());
    private static final String s_name = "clonevirtualmachineresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @ACL(accessType = AccessType.OperateEntry)
    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType=UserVmResponse.class,
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
    public void create() throws ResourceAllocationException {
        VirtualMachineTemplate template = null;
        try {
            _userVmService.checkCloneCondition(this);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, e.getMessage());
        }
        template = _templateService.createPrivateTemplateRecord(this, _accountService.getAccount(getEntityOwnerId()));
        if (template == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Unable to generate a template during clone!");
        }
    }

    public String getVMName() {
        return getTargetVM().getInstanceName();
    }

    public String getTemplateName() {
        return getVMName() + "-QA";
    }

    @Override
    public void execute() {
        Optional<UserVm> result;
        try {
            CallContext.current().setEventDetails("Vm Id for full clone: " + getId());
            result = _userVmService.cloneVirtualMachine(this);
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
        result.ifPresentOrElse((userVm)-> {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", userVm).get(0);
            response.setResponseName("test_clone");
            setResponseObject(response);
        }, ()-> {
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, "failed to clone VM: " + getId());
        });
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

    public UserVm getTargetVM() {
        return this._userVmService.getUserVm(getId());
    }
}
