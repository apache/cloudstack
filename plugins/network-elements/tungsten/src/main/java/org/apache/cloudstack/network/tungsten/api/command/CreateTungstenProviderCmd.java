package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.TungstenProvider;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.api.response.TungstenProviderResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenProviderService;

import javax.inject.Inject;

@APICommand(name = "createTungstenProvider", description = "Create tungsten provider in cloudstack", responseObject =
    TungstenProviderResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateTungstenProviderCmd extends BaseCmd {

    private static final String s_name = "createtungstenproviderresponse";

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true,
        description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "Tungsten provider"
        + " name")
    private String name;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_HOSTNAME, type = CommandType.STRING, required = true,
        description = "Tungsten provider hostname")
    private String hostname;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_PORT, type = CommandType.STRING, required = true, description =
        "Tungsten provider port")
    private String port;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_VROUTER, type = CommandType.STRING, required = true,
        description = "Tungsten provider vrouter")
    private String vrouter;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_VROUTER_PORT, type = CommandType.STRING, required = true,
        description = "Tungsten provider vrouter port")
    private String vrouterPort;

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

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final Long zoneId) {
        this.zoneId = zoneId;
    }

    @Inject
    private TungstenProviderService tungstenProviderService;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
        ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        TungstenProvider tungstenProvider = tungstenProviderService.addProvider(this);
        TungstenProviderResponse tungstenProviderResponse = tungstenProviderService.getTungstenProvider(zoneId);
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
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
