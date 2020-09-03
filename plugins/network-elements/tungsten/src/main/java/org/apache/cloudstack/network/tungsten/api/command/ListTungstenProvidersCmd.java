package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.element.TungstenProviderVO;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenProviderService;
import org.apache.cloudstack.network.tungsten.service.TungstenResponseHelper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listTungstenProviders", responseObject = TungstenProviderResponse.class, description = "Lists Tungsten providers",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListTungstenProvidersCmd extends BaseCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_UUID, type = CommandType.UUID, entityType = TungstenProviderResponse.class, required = false,
            description = "the ID of a tungsten provider")
    private Long tungstenProviderUuid;

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

    public Long getTungstenProviderUuid() {
        return tungstenProviderUuid;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Inject
    private TungstenProviderService tungstenProviderService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException,
            NetworkRuleConflictException {
        List<TungstenProviderVO> providers = tungstenProviderService.listProviders(this);

        List<TungstenProviderResponse> providerList = new ArrayList<>();
        for (TungstenProviderVO provider : providers) {
            TungstenProviderResponse responseObject = TungstenResponseHelper.createTungstenProviderResponse(provider);
            providerList.add(responseObject);
        }
        ListResponse<TungstenProviderResponse> responseList = new ListResponse<TungstenProviderResponse>();
        responseList.setResponseName(getCommandName());
        responseList.setResponses(providerList);
        setResponseObject(responseList);
    }

}