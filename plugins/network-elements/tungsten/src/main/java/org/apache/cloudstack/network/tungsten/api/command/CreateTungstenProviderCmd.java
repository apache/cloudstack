package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.TungstenProvider;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ProviderResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenProviderService;

import javax.inject.Inject;

@APICommand(name = "createTungstenProvider",
        description = "Create tungsten provider in cloudstack",
        responseObject = TungstenProviderResponse.class)
public class CreateTungstenProviderCmd extends BaseAsyncCreateCmd {

    private static final String s_name = "createtungstenproviderresponse";

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

    @Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, required = true, description = "the Physical Network ID")
    private Long physicalNetworkId;

    @Parameter(name = ApiConstants.NSP_ID, type = CommandType.UUID, entityType = ProviderResponse.class, description = "network service provider id")
    private Long nspId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Tungsten provider name")
    private String name;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_HOSTNAME, type = CommandType.STRING, required = true, description = "Tungsten provider hostname")
    private String hostname;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_PORT, type = CommandType.STRING, required = true, description = "Tungsten provider port")
    private String port;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_VROUTER, type = CommandType.STRING, required = true, description = "Tungsten provider vrouter")
    private String vrouter;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_VROUTER_PORT, type = CommandType.STRING, required = true, description = "Tungsten provider vrouter port")
    private String vrouterPort;

    public Long getNspId() {
        return nspId;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPort() {
        return port;
    }

    public String getVrouter() {
        return vrouter;
    }

    public String getVrouterPort() {
        return vrouterPort;
    }

    public Long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    @Inject
    private TungstenProviderService tungstenProviderService;

    @Override
    public void create() throws ResourceAllocationException {
        TungstenProvider tungstenProvider = tungstenProviderService.addProvider(this);
        if (tungstenProvider != null) {
            setEntityId(tungstenProvider.getId());
            setEntityUuid(tungstenProvider.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create tungsten provider");
        }
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_TUNGSTEN_ADD_PROVIDER;
    }

    @Override
    public String getEventDescription() {
        return "Create tungsten provider in cloudstack";
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenProviderResponse tungstenProviderResponse = tungstenProviderService.getTungstenProvider();
        if (tungstenProviderResponse == null)
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create tungsten provider");
        else {
            tungstenProviderResponse.setResponseName(getCommandName());
            setResponseObject(tungstenProviderResponse);
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
