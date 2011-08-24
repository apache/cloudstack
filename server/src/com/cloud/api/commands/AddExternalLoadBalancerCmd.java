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
import com.cloud.network.ExternalLoadBalancerManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.ExternalLoadBalancerResponse;
import com.cloud.user.Account;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;

@Implementation(description="Adds an external load balancer appliance.", responseObject = ExternalLoadBalancerResponse.class)
public class AddExternalLoadBalancerCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(AddExternalLoadBalancerCmd.class.getName());
	private static final String s_name = "addexternalloadbalancerresponse";
	
	/////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
	
	@Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required = true, description="Zone in which to add the external load balancer appliance.")
	private Long zoneId;
	
	@Parameter(name=ApiConstants.URL, type=CommandType.STRING, required = true, description="URL of the external load balancer appliance.")
	private String url;	 
	
	@Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="Username of the external load balancer appliance.")
	private String username;	 
	
	@Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="Password of the external load balancer appliance.")
	private String password;	 

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
		    ExternalLoadBalancerManager externalLoadBalancerMgr = locator.getManager(ExternalLoadBalancerManager.class);
			Host externalLoadBalancer = externalLoadBalancerMgr.addExternalLoadBalancer(this);
			ExternalLoadBalancerResponse response = externalLoadBalancerMgr.getApiResponse(externalLoadBalancer);
			response.setObjectName("externalloadbalancer");
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (InvalidParameterValueException ipve) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, ipve.getMessage());
		} catch (CloudRuntimeException cre) {
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, cre.getMessage());
		}
    }

}

