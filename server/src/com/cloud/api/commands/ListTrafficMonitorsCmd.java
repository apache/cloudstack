// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.command.user.offering.ListServiceOfferingsCmd;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import com.cloud.host.Host;
import com.cloud.network.NetworkUsageManager;
import com.cloud.server.ManagementService;
import org.apache.cloudstack.api.response.TrafficMonitorResponse;
import com.cloud.utils.component.ComponentLocator;

@APICommand(name = "listTrafficMonitors", description="List traffic monitor Hosts.", responseObject = TrafficMonitorResponse.class)
public class ListTrafficMonitorsCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListServiceOfferingsCmd.class.getName());
    private static final String s_name = "listtrafficmonitorsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            required = true, description="zone Id")
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
