package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import net.juniper.contrail.api.types.VirtualMachine;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualMachineResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenManager;
import org.apache.cloudstack.network.tungsten.service.TungstenResponseHelper;

import javax.inject.Inject;
import java.io.IOException;

@APICommand(name = "createTungstenVirtualMachine",
        description = "Create tungsten virtual machine",
        responseObject = TungstenVirtualMachineResponse.class)
public class CreateTungstenVirtualMachineCmd extends BaseAsyncCreateCmd {

    private static final String s_name = "createtungstenvirtualmachineresponse";

    //Owner information
    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "An optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "An optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Project ID for the service instance")
    private Long projectId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Tungsten virtual machine interface name")
    private String name;

    @Parameter(name = ApiConstants.TUNGSTEN_VIRTUAL_MACHINE_UUID, type = CommandType.STRING, description = "Tungsten virtual machine uuid. If null tungsten will generate an uuid for the virtual machine")
    private String tungstenVmUuid;

    public String getName() {
        return name;
    }

    public String getTungstenVmUuid() {
        return tungstenVmUuid;
    }

    @Inject
    TungstenManager tungstenManager;

    @Override
    public void create() throws ResourceAllocationException {
        VirtualMachine virtualMachine = tungstenManager.createTungstenVirtualMachine(this);
        if(virtualMachine != null) {
            setEntityId(1L);
            setEntityUuid(virtualMachine.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create virtual machine into tungsten.");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_VIRTUAL_MACHINE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Create tungsten virtual machine";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try{
            VirtualMachine virtualMachine = (VirtualMachine) tungstenManager.getTungstenObjectByUUID(VirtualMachine.class, getEntityUuid());
            TungstenVirtualMachineResponse response = TungstenResponseHelper.createTungstenVirtualMachineResponse(virtualMachine);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create virtual machine into tungsten.");
        }

    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }
}
