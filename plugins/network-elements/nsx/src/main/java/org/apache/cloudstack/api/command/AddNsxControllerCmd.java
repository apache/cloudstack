package org.apache.cloudstack.api.command;

import com.cloud.network.NsxProvider;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.response.NsxControllerResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.service.NsxProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


@APICommand(name = AddNsxControllerCmd.APINAME, description = "Add NSX Controller to CloudStack",
        responseObject = NsxControllerResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class AddNsxControllerCmd extends BaseCmd {
    public static final String APINAME = "addNsxController";
    public static final Logger LOGGER = LoggerFactory.getLogger(AddNsxControllerCmd.class.getName());

    @Inject
    NsxProviderService nsxProviderService;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true,
            description = "the ID of zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "NSX controller / provider name")
    private String name;

    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_HOSTNAME, type = CommandType.STRING, required = true, description = "NSX controller hostname / IP address")
    private String hostname;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "Username to log into NSX controller")
    private String username;
    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "Password to login into NSX controller")
    private String password;

    @Parameter(name = ApiConstants.TIER0_GATEWAY, type = CommandType.STRING, required = true, description = "Tier-0 Gateway address")
    private String tier0Gateway;

    @Parameter(name = ApiConstants.EDGE_CLUSTER, type = CommandType.STRING, required = true, description = "Edge Cluster name")
    private String edgeCluster;

    public NsxProviderService getNsxProviderService() {
        return nsxProviderService;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getName() {
        return name;
    }

    public String getHostname() {
        return hostname;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getTier0Gateway() {
        return tier0Gateway;
    }

    public String getEdgeCluster() {
        return edgeCluster;
    }

    @Override
    public void execute() throws ServerApiException {
        NsxProvider nsxProvider = nsxProviderService.addProvider(this);
        NsxControllerResponse nsxControllerResponse =
                nsxProviderService.createNsxControllerResponse(
                        nsxProvider);
        if (nsxControllerResponse == null)
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add NSX controller");
        else {
            nsxControllerResponse.setResponseName(getCommandName());
            setResponseObject(nsxControllerResponse);
        }
    }

    @Override
    public long getEntityOwnerId() {
        return 0;
    }
}
