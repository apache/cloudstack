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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.SuccessResponse;
import com.cloud.configuration.ConfigurationManager;

@Implementation(method="editPod", manager=ConfigurationManager.class, description="Updates a Pod.")
public class UpdatePodCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdatePodCmd.class.getName());

    private static final String s_name = "updatepodresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.CIDR, type=CommandType.STRING, description="the CIDR notation for the base IP address of the Pod")
    private String cidr;

    @Parameter(name=ApiConstants.END_IP, type=CommandType.STRING, description="the ending IP address for the Pod")
    private String endIp;

    @Parameter(name=ApiConstants.GATEWAY, type=CommandType.STRING, description="the gateway for the Pod")
    private String gateway;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the ID of the Pod")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the Pod")
    private String podName;

    @Parameter(name=ApiConstants.START_IP, type=CommandType.STRING, description="the starting IP address for the Pod")
    private String startIp;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCidr() {
        return cidr;
    }

    public String getEndIp() {
        return endIp;
    }

    public String getGateway() {
        return gateway;
    }

    public Long getId() {
        return id;
    }

    public String getPodName() {
        return podName;
    }

    public String getStartIp() {
        return startIp;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }

	@Override @SuppressWarnings("unchecked")
	public SuccessResponse getResponse() {
	    /* Not sure why we aren't returning the Pod here, but I'll keep the old "success" response we used to have
        HostPodVO pod = (HostPodVO)getResponseObject();

        PodResponse response = new PodResponse();
        response.setId(pod.getId());
        response.setCidr(pod.getCidrAddress() + "/" + pod.getCidrSize());
        response.setEndIp(endIp == null ? "" : endIp);
        response.setStartIp(startIp);
        response.setZoneName(ApiDBUtils.findZoneById(pod.getDataCenterId()).getName());
        response.setGateway(pod.getGateway());
        response.setName(pod.getName());
        response.setZoneId(pod.getDataCenterId());

        response.setResponseName(getName());
        return response;
        */
	    SuccessResponse response = new SuccessResponse();
	    response.setSuccess(Boolean.TRUE);
	    //response.setDisplayText("Successfully updated pod.");
	    response.setResponseName(getName());
	    return response;
	}
}
