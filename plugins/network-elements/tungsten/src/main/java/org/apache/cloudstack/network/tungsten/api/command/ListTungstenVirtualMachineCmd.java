package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualMachineResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenManager;

import javax.inject.Inject;
import java.io.IOException;

@APICommand(name = "listTungstenVirtualMachines",
        description = "List tungsten virtual machines",
        responseObject = TungstenVirtualMachineResponse.class)
public class ListTungstenVirtualMachineCmd extends BaseListTaggedResourcesCmd implements UserCmd {

    private static final String s_name = "listtungstenvirtualmachinesresponse";

    @Parameter(name = ApiConstants.TUNGSTEN_VIRTUAL_MACHINE_UUID, type = CommandType.STRING, description = "list tungsten virtual machine by uuid")
    private String virtualMachineUUID;

    public String getVirtualMachineUUID() {
        return virtualMachineUUID;
    }

    @Inject
    TungstenManager tungstenManager;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try{
            ListResponse<TungstenVirtualMachineResponse> response = tungstenManager.getVirtualMachines(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to retrieve virtual machines from tungsten.");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
