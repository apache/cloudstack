package com.cloud.api.commands;

import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.network.ExternalNetworkDeviceManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.NetworkDeviceResponse;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(description="Adds a network device of one of the following types: ExternalDhcp, ExternalFirewall, ExternalLoadBalancer, PxeServer", responseObject = NetworkDeviceResponse.class)
public class AddNetworkDeviceCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AddNetworkDeviceCmd.class);
	private static final String s_name = "addnetworkdeviceresponse";
	
	// ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////
	
	@Parameter(name = ApiConstants.NETWORK_DEVICE_TYPE, type = CommandType.STRING, description = "Network device type, now supports ExternalDhcp, PxeServer, NetscalerLoadBalancer, F5BigIpLoadBalancer, JuniperSRXFirewall")
    private String type;
	
	@Parameter(name = ApiConstants.NETWORK_DEVICE_PARAMETER_LIST, type = CommandType.MAP, description = "parameters for network device")
    private Map paramList;
	
	
	public String getType() {
		return type;
	}
	
	public Map getParamList() {
		return paramList;
	}
	
	@Override
	public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
			ResourceAllocationException {
		try {
			ExternalNetworkDeviceManager nwDeviceMgr;
			ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
			nwDeviceMgr = locator.getManager(ExternalNetworkDeviceManager.class);
			Host device = nwDeviceMgr.addNetworkDevice(this);
			NetworkDeviceResponse response = nwDeviceMgr.getApiResponse(device);
			response.setObjectName("networkdevice");
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (InvalidParameterValueException ipve) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, ipve.getMessage());
		} catch (CloudRuntimeException cre) {
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, cre.getMessage());
		}

	}

	@Override
	public String getCommandName() {
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
		// TODO Auto-generated method stub
		return 0;
	}

}
