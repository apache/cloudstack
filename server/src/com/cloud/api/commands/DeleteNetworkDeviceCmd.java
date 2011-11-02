package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkDeviceManager;
import com.cloud.server.ManagementService;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(description="Deletes network device.", responseObject=SuccessResponse.class)
public class DeleteNetworkDeviceCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(DeleteNetworkDeviceCmd.class);
	private static final String s_name = "deletenetworkdeviceresponse";
	
	/////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="host")
	@Parameter(name = ApiConstants.ID, type = CommandType.LONG, required=true, description = "Id of network device to delete")
    private Long id;
    
	
	public Long getId() {
		return id;
	}
	
	@Override
	public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
			ResourceAllocationException {
		try {
			NetworkDeviceManager nwDeviceMgr;
			ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
			nwDeviceMgr = locator.getManager(NetworkDeviceManager.class);
			boolean result = nwDeviceMgr.deleteNetworkDevice(this);
			if (result) {
				SuccessResponse response = new SuccessResponse(getCommandName());
				response.setResponseName(getCommandName());
				this.setResponseObject(response);
			} else {
				throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete network device:" + getId());
			}
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
