package org.apache.cloudstack.api.command.user.vm;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCustomIdCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
//import org.apache.cloudstack.context.CallContext;
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

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    private Long temporaryTemlateId;

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

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

    public Long getTemporaryTemlateId() {
        return this.temporaryTemlateId;
    }

    public void setTemporaryTemlateId(long tempId) {
        this.temporaryTemlateId = tempId;
    }

    @Override
    public void create() throws ResourceAllocationException {
        try {
            _userVmService.checkCloneCondition(this);
            VirtualMachineTemplate template = _templateService.createPrivateTemplateRecord(this, _accountService.getAccount(getEntityOwnerId()));
            if (template == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "failed to create a template to db");
            }
            s_logger.info("The template id recorded is: " + template.getId());
            UserVm vmRecord = _userVmService.recordVirtualMachineToDB(this);
            if (vmRecord == null) {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "unable to record a new VM to db!");
            }
            setTemporaryTemlateId(template.getId());
            setEntityId(vmRecord.getId());
            setEntityUuid(String.valueOf(template.getId()));
        } catch (ResourceUnavailableException | InsufficientCapacityException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, e.getMessage());
        } catch (InvalidParameterValueException e) {
            s_logger.warn("Exception: ", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        } finally {
            if (getTemporaryTemlateId() != null) {
                // TODO: delete template in the service
                s_logger.warn("clearing the temporary template: " + getTemporaryTemlateId());
            }
        }
    }

    public boolean isPublic() {
        return false;
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
            CallContext.current().setEventDetails("Vm Id for full clone: " + getEntityId());
            s_logger.info("creating actual template id: " + getEntityUuid());
            s_logger.info("starting actual VM id: " + getEntityId());
            _templateService.createPrivateTemplate(this);
            result = _userVmService.cloneVirtualMachine(this);
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        } catch (ConcurrentOperationException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
//        catch (ResourceAllocationException ex) {
//            s_logger.warn("Exception: ", ex);
//            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
//        }
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
