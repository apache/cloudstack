package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenProviderService;

import javax.inject.Inject;
import java.util.Arrays;

@APICommand(name = "listTungstenProviders", responseObject = TungstenProviderResponse.class, description = "Lists Tungsten providers",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListTungstenProvidersCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, required = true, entityType = PhysicalNetworkResponse.class)
    private Long physicalNetworkId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return "listTungstenProviders";
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Inject
    private TungstenProviderService tungstenProviderService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        TungstenProviderResponse provider = tungstenProviderService.listTungstenProvider(this);
        ListResponse<TungstenProviderResponse> responseList = new ListResponse<TungstenProviderResponse>();
        responseList.setResponseName(getCommandName());
        responseList.setResponses(Arrays.asList(provider));
        setResponseObject(responseList);
    }

}