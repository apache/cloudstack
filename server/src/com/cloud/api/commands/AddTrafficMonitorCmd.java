/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.network.NetworkUsageManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.server.api.response.TrafficMonitorResponse;
import com.cloud.user.Account;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(description="Adds Traffic Monitor Host for Direct Network Usage", responseObject = ExternalFirewallResponse.class)
public class AddTrafficMonitorCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AddTrafficMonitorCmd.class.getName());	
	private static final String s_name = "addtrafficmonitorresponse";	
	
	/////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required = true, description="Zone in which to add the external firewall appliance.")
	private Long zoneId;

	
	@Parameter(name=ApiConstants.URL, type=CommandType.STRING, required = true, description="URL of the traffic monitor Host")
	private String url;	 
	
	///////////////////////////////////////////////////
	/////////////////// Accessors ///////////////////////
	/////////////////////////////////////////////////////
	 
	public Long getZoneId() {
	    return zoneId;
	}

	public String getUrl() {
		return url;
	}
	
	/////////////////////////////////////////////////////
	/////////////// API Implementation///////////////////
	/////////////////////////////////////////////////////

	@Override
	public String getCommandName() {
		return s_name;
	}
	
	@Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
	 
	@Override
    public void execute(){
		try {
		    ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
		    NetworkUsageManager networkUsageMgr = locator.getManager(NetworkUsageManager.class);
			Host trafficMoinitor = networkUsageMgr.addTrafficMonitor(this);
			TrafficMonitorResponse response = networkUsageMgr.getApiResponse(trafficMoinitor);
			response.setObjectName("trafficmonitor");
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (InvalidParameterValueException ipve) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, ipve.getMessage());
		} catch (CloudRuntimeException cre) {
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, cre.getMessage());
		}
    }
}

