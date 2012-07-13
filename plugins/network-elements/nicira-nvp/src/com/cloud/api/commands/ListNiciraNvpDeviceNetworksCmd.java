package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.PlugService;
import com.cloud.api.ServerApiException;
import com.cloud.api.BaseCmd.CommandType;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.NetworkResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.element.NiciraNvpElementService;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(responseObject=NetworkResponse.class, description="lists network that are using a nicira nvp device")
public class ListNiciraNvpDeviceNetworksCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(ListNiciraNvpDeviceNetworksCmd.class.getName());
    private static final String s_name = "listniciranvpdevicenetworks";
    @PlugService NiciraNvpElementService _niciraNvpElementService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="external_nicira_nvp_devices")
    @Parameter(name=ApiConstants.NICIRA_NVP_DEVICE_ID, type=CommandType.LONG, required = true, description="nicira nvp device ID")
    private Long niciraNvpDeviceId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getNiciraNvpDeviceId() {
        return niciraNvpDeviceId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException {
        try {
            List<? extends Network> networks  = _niciraNvpElementService.listNiciraNvpDeviceNetworks(this);
            ListResponse<NetworkResponse> response = new ListResponse<NetworkResponse>();
            List<NetworkResponse> networkResponses = new ArrayList<NetworkResponse>();

            if (networks != null && !networks.isEmpty()) {
                for (Network network : networks) {
                    NetworkResponse networkResponse = _responseGenerator.createNetworkResponse(network);
                    networkResponses.add(networkResponse);
                }
            }

            response.setResponses(networkResponses);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        }  catch (InvalidParameterValueException invalidParamExcp) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, invalidParamExcp.getMessage());
        } catch (CloudRuntimeException runtimeExcp) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, runtimeExcp.getMessage());
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

}
