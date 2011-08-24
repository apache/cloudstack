/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.host.Host;
import com.cloud.network.ExternalFirewallManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.utils.component.ComponentLocator;

@Implementation(description="List external firewall appliances.", responseObject = ExternalFirewallResponse.class)
public class ListExternalFirewallsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());
    private static final String s_name = "listexternalfirewallsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required = true, description="zone Id")
    private long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
        ExternalFirewallManager externalFirewallMgr = locator.getManager(ExternalFirewallManager.class);
    	List<? extends Host> externalFirewalls = externalFirewallMgr.listExternalFirewalls(this);

        ListResponse<ExternalFirewallResponse> listResponse = new ListResponse<ExternalFirewallResponse>();
        List<ExternalFirewallResponse> responses = new ArrayList<ExternalFirewallResponse>();
        for (Host externalFirewall : externalFirewalls) {
        	ExternalFirewallResponse response = externalFirewallMgr.getApiResponse(externalFirewall);
        	response.setObjectName("externalfirewall");
        	response.setResponseName(getCommandName());
        	responses.add(response);
        }

        listResponse.setResponses(responses);
        listResponse.setResponseName(getCommandName());
        this.setResponseObject(listResponse);
    }
}
