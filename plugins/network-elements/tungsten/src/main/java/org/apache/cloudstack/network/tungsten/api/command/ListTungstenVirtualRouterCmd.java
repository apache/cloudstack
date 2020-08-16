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
import org.apache.cloudstack.network.tungsten.api.response.TungstenVirtualRouterResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenManager;

import javax.inject.Inject;
import java.io.IOException;

@APICommand(name = "listTungstenVirtualRouters",
        description = "List tungsten virtual routers",
        responseObject = TungstenVirtualRouterResponse.class)
public class ListTungstenVirtualRouterCmd extends BaseListTaggedResourcesCmd implements UserCmd {

    private static final String s_name = "listtungstenvirtualroutersresponse";

    @Parameter(name = ApiConstants.TUNGSTEN_VIRTUAL_ROUTER_UUID, type = CommandType.STRING, description = "list tungsten virtual routers by uuid")
    private String virtualRouterUuid;

    public String getVirtualRouterUuid() {
        return virtualRouterUuid;
    }

    @Inject
    TungstenManager tungstenManager;


    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try{
            ListResponse<TungstenVirtualRouterResponse> response = tungstenManager.getVirtualRouters(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to retrieve virtual routers from tungsten.");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }
}
