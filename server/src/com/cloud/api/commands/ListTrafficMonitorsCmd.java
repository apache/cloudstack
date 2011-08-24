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
import com.cloud.network.NetworkUsageManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.ExternalFirewallResponse;
import com.cloud.server.api.response.TrafficMonitorResponse;
import com.cloud.utils.component.ComponentLocator;

@Implementation(description="List traffic monitor Hosts.", responseObject = ExternalFirewallResponse.class)
public class ListTrafficMonitorsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());
    private static final String s_name = "listtrafficmonitorsresponse";

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
        NetworkUsageManager networkUsageMgr = locator.getManager(NetworkUsageManager.class);
    	List<? extends Host> trafficMonitors = networkUsageMgr.listTrafficMonitors(this);

        ListResponse<TrafficMonitorResponse> listResponse = new ListResponse<TrafficMonitorResponse>();
        List<TrafficMonitorResponse> responses = new ArrayList<TrafficMonitorResponse>();
        for (Host trafficMonitor : trafficMonitors) {
            TrafficMonitorResponse response = networkUsageMgr.getApiResponse(trafficMonitor);
        	response.setObjectName("trafficmonitor");
        	response.setResponseName(getCommandName());
        	responses.add(response);
        }

        listResponse.setResponses(responses);
        listResponse.setResponseName(getCommandName());
        this.setResponseObject(listResponse);
    }
}
