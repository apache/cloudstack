package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
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
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenProviderService;

import javax.inject.Inject;

@APICommand(name = "deleteTungstenProvider",
        description = "Delete tungsten provider",
        responseObject = SuccessResponse.class,
        entityType = {TungstenProvider.class},
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false)
public class DeleteTungstenProviderCmd extends BaseCmd {

    private static final String s_name = "deletetungstenproviderresponse";

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, required = true, entityType = ZoneResponse.class)
    private Long zoneId;
    @Parameter(name = ApiConstants.TUNGSTEN_PROVIDER_UUID, type = CommandType.STRING, required = true, description = "The UUID of the tungsten provider")
    private String tungstenProviderUuid;

    public String getTungstenProviderUuid() {
        return tungstenProviderUuid;
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Inject
    private TungstenProviderService tungstenProviderService;


    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            tungstenProviderService.deleteTungstenProvider(this);
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
        } catch (InvalidParameterValueException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete tungsten provider");
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
