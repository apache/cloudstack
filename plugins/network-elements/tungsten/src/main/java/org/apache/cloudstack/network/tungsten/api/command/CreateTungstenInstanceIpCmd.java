package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import net.juniper.contrail.api.types.InstanceIp;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tungsten.api.response.TungstenInstanceIpResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenManager;
import org.apache.cloudstack.network.tungsten.service.TungstenResponseHelper;

import javax.inject.Inject;
import java.io.IOException;

@APICommand(name = "createTungstenInstanceIp",
        description = "Create tungsten instance ip",
        responseObject = TungstenInstanceIpResponse.class)
public class CreateTungstenInstanceIpCmd extends BaseAsyncCreateCmd {

    private static final String s_name = "createtungsteninstanceipresponse";

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

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Tungsten network Name")
    private String name;

    @Parameter(name = ApiConstants.TUNGSTEN_NETWORK_UUID, type = CommandType.STRING, description = "Tungsten Network UUID")
    private String tungstenNetworkUuid;

    @Parameter(name = ApiConstants.TUNGSTEN_VM_INTERFACE_UUID, type = CommandType.STRING, description = "Tungsten Virtual Machine Interface UUID")
    private String tungstenVmInterfaceUuid;

    @Parameter(name = ApiConstants.TUNGSTEN_INSTANCE_IP_ADDRESS, type = CommandType.STRING, description = "Tungsten Instance IP Address")
    private String tungstenInstanceIpAddress;

    public String getName() {
        return name;
    }

    public String getTungstenNetworkUuid() {
        return tungstenNetworkUuid;
    }

    public String getTungstenVmInterfaceUuid() {
        return tungstenVmInterfaceUuid;
    }

    public String getTungstenInstanceIpAddress() {
        return tungstenInstanceIpAddress;
    }

    @Inject
    TungstenManager tungstenManager;

    @Override
    public void create() throws ResourceAllocationException {
        InstanceIp instanceIp = tungstenManager.createInstanceIp(this);
        if(instanceIp != null) {
            setEntityId(1L);
            setEntityUuid(instanceIp.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create instance ip into tungsten.");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_INSTANCE_IP;
    }

    @Override
    public String getEventDescription() {
        return "Create tungsten instance ip";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            InstanceIp instanceIp = (InstanceIp) tungstenManager.getTungstenObjectByUUID(InstanceIp.class, getEntityUuid());
            TungstenInstanceIpResponse response = TungstenResponseHelper.createTungstenInstanceIpResponse(instanceIp);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create instance ip into tungsten.");
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
