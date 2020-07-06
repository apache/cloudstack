package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVmInterfaceResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenManager;
import org.apache.cloudstack.network.tungsten.service.TungstenResponseHelper;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

@APICommand(name = "createTungstenVmInterface",
        description = "Create tungsten virtual machine interface",
        responseObject = TungstenVmInterfaceResponse.class)
public class CreateTungstenVmInterfaceCmd extends BaseAsyncCreateCmd {

    private static final String s_name = "createtungstenvminterfaceresponse";

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

    @Parameter(name = ApiConstants.TUNGSTEN_NETWORK_UUID, type = CommandType.STRING, description = "Tungsten Network UUID")
    private String tungstenNetworkUuid;

    @Parameter(name = ApiConstants.TUNGSTEN_VIRTUAL_MACHINE_UUID, type = CommandType.STRING, description = "Tungsten virtual machine UUID")
    private String tungstenVirtualMachineUuid;

    @Parameter(name = ApiConstants.TUNGSTEN_SECURITY_GROUP_UUID, type = CommandType.STRING, description = "Tungsten security group UUID")
    private String tungstenSecurityGroupUuid;

    @Parameter(name = ApiConstants.TUNGSTEN_PROJECT_UUID, type = CommandType.STRING, description = "Tungsten project UUID")
    private String tungstenProjectUuid;

    @Parameter(name = ApiConstants.TUNGSTEN_VM_INTERFACE_MAC_ADDRESSES, type = CommandType.LIST, collectionType = CommandType.STRING, description = "Tungsten virtual machine interface mac addresses")
    private List<String> tungstenVmInterfaceMacAddresses;

    public String getName() {
        return name;
    }

    public String getTungstenNetworkUuid() {
        return tungstenNetworkUuid;
    }

    public String getTungstenVirtualMachineUuid() {
        return tungstenVirtualMachineUuid;
    }

    public String getTungstenSecurityGroupUuid() {
        return tungstenSecurityGroupUuid;
    }

    public String getTungstenProjectUuid() {
        return tungstenProjectUuid;
    }

    public List<String> getTungstenVmInterfaceMacAddresses() {
        return tungstenVmInterfaceMacAddresses;
    }

    @Inject
    TungstenManager tungstenManager;

    @Override
    public void create(){
        VirtualMachineInterface virtualMachineInterface = tungstenManager.createTungstenVirtualMachineInterface(this);
        if(virtualMachineInterface != null) {
            setEntityId(1L);
            setEntityUuid(virtualMachineInterface.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create virtual machine interface into tungsten.");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_VM_INTERFACE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "Create tungsten virtual machine interface.";
    }

    @Override
    public void execute() throws ServerApiException, ConcurrentOperationException{
        try{
            VirtualMachineInterface virtualMachineInterface = (VirtualMachineInterface) tungstenManager.getTungstenObjectByUUID(VirtualMachineInterface.class, getEntityUuid());
            TungstenVmInterfaceResponse response = TungstenResponseHelper.createTungstenVmInterfaceResponse(virtualMachineInterface);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create virtual machine interface into tungsten.");
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
