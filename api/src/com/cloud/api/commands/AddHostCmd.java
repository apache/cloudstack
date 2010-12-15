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
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.HostResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;

@Implementation(description="Adds a new host.", responseObject=HostResponse.class)
public class AddHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddHostCmd.class.getName());

    private static final String s_name = "addhostresponse";
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.CLUSTER_ID, type=CommandType.LONG, description="the cluster ID for the host")
    private Long clusterId;

    @Parameter(name=ApiConstants.CLUSTER_NAME, type=CommandType.STRING, description="the cluster name for the host")
    private String clusterName;

    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required=true, description="the password for the host")
    private String password;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the Pod ID for the host")
    private Long podId;

    @Parameter(name=ApiConstants.URL, type=CommandType.STRING, required=true, description="the host URL")
    private String url;

    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required=true, description="the username for the host")
    private String username;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, required=true, description="the Zone ID for the host")
    private Long zoneId;
    
    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, required=false, description="hypervisor type of the host")
    private String hypervisor;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getPassword() {
        return password;
    }

    public Long getPodId() {
        return podId;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public Long getZoneId() {
        return zoneId;
    }
    
    public String getHypervisor() {
    	return hypervisor;
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
        try {
            List<? extends Host> result = _resourceService.discoverHosts(this);
            ListResponse<HostResponse> response = new ListResponse<HostResponse>();
            List<HostResponse> hostResponses = new ArrayList<HostResponse>();
            if (result != null) {
                for (Host host : result) {
                    HostResponse hostResponse = _responseGenerator.createHostResponse(host);
                    hostResponses.add(hostResponse);
                }
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to add host");
            }

            response.setResponses(hostResponses);
            response.setResponseName(getCommandName());
            
            this.setResponseObject(response);
        } catch (DiscoveryException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
