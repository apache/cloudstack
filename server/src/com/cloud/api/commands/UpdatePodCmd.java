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

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;

@Implementation(method="editPod", manager=Manager.ConfigManager)
public class UpdatePodCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdatePodCmd.class.getName());

    private static final String s_name = "updatepodresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="cidr", type=CommandType.STRING)
    private String cidr;

    @Parameter(name="endip", type=CommandType.STRING)
    private String endIp;

    @Parameter(name="gateway", type=CommandType.STRING)
    private String gateway;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String podName;

    @Parameter(name="startip", type=CommandType.STRING)
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

	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//    	Long podId = (Long) params.get(BaseCmd.Properties.ID.getName());
//    	String podName = (String) params.get(BaseCmd.Properties.NAME.getName());
//    	String gateway = (String) params.get(BaseCmd.Properties.GATEWAY.getName());
//    	String cidr = (String) params.get(BaseCmd.Properties.CIDR.getName());
//    	String startIp = (String) params.get(BaseCmd.Properties.START_IP.getName());
//    	String endIp = (String) params.get(BaseCmd.Properties.END_IP.getName());
//    	Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//    	
//    	if (userId == null) {
//            userId = Long.valueOf(User.UID_SYSTEM);
//        }
//    	
//    	//verify parameters
//    	HostPodVO pod = getManagementServer().findHostPodById(podId);
//    	if (pod == null) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find pod by id " + podId);
//    	}
//    	
//    	long zoneId = pod.getDataCenterId();
//    	DataCenterVO zone = getManagementServer().findDataCenterById(zoneId);
//    	if (zone == null) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find zone by id " + zoneId);
//    	}
//    	
//    	if (endIp != null && startIp == null) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "If an end IP is specified, a start IP must be specified.");
//    	}
//    	
//    	HostPodVO updatedPod = null;
//        try {
//             updatedPod = getManagementServer().editPod(userId, podId, podName, gateway, cidr, startIp, endIp);
//        } catch (Exception ex) {
//            s_logger.error("Exception updating pod", ex);
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
//        }
//
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        
//        if (updatedPod == null) {
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update pod; internal error.");
//        } else {
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), "true"));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), "Successfully updated pod."));
//        }
//        
//        return returnValues;
//    }
}
