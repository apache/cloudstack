/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.dc.ClusterVO;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.storage.GuestOSCategoryVO;

@Implementation(method="discoverHosts", manager=Manager.AgentManager, description="Adds secondary storage.")
public class AddSecondaryStorageCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddSecondaryStorageCmd.class.getName());
    private static final String s_name = "addsecondarystorageresponse";
     
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="url", type=CommandType.STRING, required=true)
    private String url;

    @Parameter(name="zoneid", type=CommandType.LONG)
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUrl() {
        return url;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
    	return s_name;
    }
    
    @Override @SuppressWarnings("unchecked")
    public ListResponse<HostResponse> getResponse() {
		List<HostVO> hosts = (List<HostVO>)getResponseObject();

        ListResponse<HostResponse> response = new ListResponse<HostResponse>();
		List<HostResponse> hostResponses = new ArrayList<HostResponse>();
	    if (hosts != null) {
	        for (HostVO host : hosts) {
	        	HostResponse hostResponse = new HostResponse();
	        	hostResponse.setId(host.getId());
	            hostResponse.setCapabilities(host.getCapabilities());
	            hostResponse.setClusterId(host.getClusterId());
	            hostResponse.setCpuNumber(host.getCpus());
	            hostResponse.setZoneId(host.getDataCenterId());
	            hostResponse.setDisconnectedOn(host.getDisconnectedOn());
	            hostResponse.setHypervisor(host.getHypervisorType());
	            hostResponse.setHostType(host.getType());
	            hostResponse.setLastPinged(new Date(host.getLastPinged()));
	            hostResponse.setManagementServerId(host.getManagementServerId());
	            hostResponse.setName(host.getName());
	            hostResponse.setPodId(host.getPodId());
	            hostResponse.setCreated(host.getCreated());
	            hostResponse.setRemoved(host.getRemoved());
	            hostResponse.setCpuSpeed(host.getSpeed());
	            hostResponse.setState(host.getStatus());
	            hostResponse.setIpAddress(host.getPrivateIpAddress());
	            hostResponse.setVersion(host.getVersion());

	            GuestOSCategoryVO guestOSCategory = ApiDBUtils.getHostGuestOSCategory(host.getId());
	            if (guestOSCategory != null) {
	                hostResponse.setOsCategoryId(guestOSCategory.getId());
	                hostResponse.setOsCategoryName(guestOSCategory.getName());
	            }
	            hostResponse.setZoneName(ApiDBUtils.findZoneById(host.getDataCenterId()).getName());

	            if (host.getPodId() != null) {
	                hostResponse.setPodName(ApiDBUtils.findPodById(host.getPodId()).getName());
	            }

	            if (host.getType().toString().equals("Storage")) {
	                hostResponse.setDiskSizeTotal(host.getTotalSize());
	                hostResponse.setDiskSizeAllocated(0L);
	            }
	
	            if (host.getClusterId() != null) {
	                ClusterVO cluster = ApiDBUtils.findClusterById(host.getClusterId());
	                hostResponse.setClusterName(cluster.getName());
	            }
	
	            hostResponse.setLocalStorageActive(ApiDBUtils.isLocalStorageActiveOnHost(host));
	
	            Set<Event> possibleEvents = host.getStatus().getPossibleEvents();
	            if ((possibleEvents != null) && !possibleEvents.isEmpty()) {
	                String events = "";
	                Iterator<Event> iter = possibleEvents.iterator();
	                while (iter.hasNext()) {
	                    Event event = iter.next();
	                    events += event.toString();
	                    if (iter.hasNext()) {
	                        events += "; ";
	                    }
	                }
	                hostResponse.setEvents(events);
	            }
	            hostResponse.setResponseName("secondarystorage");
	            hostResponses.add(hostResponse);
	        }
	    } else {
	        throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add secondary storage");
	    }

	    response.setResponses(hostResponses);
	    response.setResponseName(getName());
	    return response;
	    //return ApiResponseSerializer.toSerializedString(response);
    }
}
