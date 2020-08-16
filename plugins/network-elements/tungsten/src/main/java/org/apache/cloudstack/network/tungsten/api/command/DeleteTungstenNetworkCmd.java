package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;
import net.juniper.contrail.api.types.VirtualNetwork;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.network.tungsten.service.TungstenManager;

import javax.inject.Inject;
import java.io.IOException;

@APICommand(name = "deleteTungstenNetwork",
        description = "Delete tungstens networks",
        responseObject = SuccessResponse.class)
public class DeleteTungstenNetworkCmd extends BaseCmd {

    private static final String s_name = "deletetungstennetworkresponse";

    @Parameter(name= ApiConstants.TUNGSTEN_NETWORK_UUID, type=CommandType.STRING, required=true, description="The UUID of the tungsten network")
    private String tungstenNetworkUuid;

    public String getTungstenNetworkUuid() {
        return tungstenNetworkUuid;
    }

    @Inject
    TungstenManager tungstenManager;

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        try {
            VirtualNetwork network = tungstenManager.deleteTungstenNetwork(this);
            if(network != null){
                SuccessResponse response = new SuccessResponse(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete tungsten network");
            }
        } catch (IOException e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to delete tungsten network");
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
