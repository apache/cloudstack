package com.cloud.api;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.PxeServerResponse;
import com.cloud.baremetal.LinMinPxeServerManager;
import com.cloud.baremetal.PxeServerManager;
import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.server.ManagementService;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(description="Adds a PXE server appliance", responseObject = PxeServerResponse.class)
public class AddPxeServerCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AddPxeServerCmd.class.getName());	
	private static final String s_name = "addpxeserverresponse";	
	
	/////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required = true, description="Zone in which to add the external firewall appliance.")
	private Long zoneId;
	
	@Parameter(name=ApiConstants.URL, type=CommandType.STRING, required = true, description="URL of the PXE server appliance.")
	private String url;	 
	
	@Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="Username of PXE server appliance.")
	private String username;	 
	
	@Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="Password of the PXE server appliance.")
	private String password;
	
	@Parameter(name=ApiConstants.PXE_SERVER_TYPE, type=CommandType.STRING, required = true, description="Type of PXE server. Current values are LinMin, DMCD")
	private String type;

	///////////////////////////////////////////////////
	/////////////////// Accessors ///////////////////////
	/////////////////////////////////////////////////////
	
	public Long getZoneId() {
		return zoneId;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getType() {
		return type;
	}
	 
	/////////////////////////////////////////////////////
	/////////////// API Implementation///////////////////
	/////////////////////////////////////////////////////
	
	@Override
	public String getCommandName() {
		return s_name;
	}
	
	@Override
	public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
			ResourceAllocationException {
		try {
			PxeServerManager pxeServerMgr;
			ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
			if (getType().equalsIgnoreCase(PxeServerType.LinMin.getName())) {
				pxeServerMgr = locator.getManager(LinMinPxeServerManager.class);
			} else {
				throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unsupport PXE server type "  + getType());
			}
			Host pxeServer = pxeServerMgr.addPxeServer(this);
			PxeServerResponse response = pxeServerMgr.getApiResponse(pxeServer);
			response.setObjectName("pxeserver");
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (InvalidParameterValueException ipve) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, ipve.getMessage());
		} catch (CloudRuntimeException cre) {
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, cre.getMessage());
		}
	}
}
