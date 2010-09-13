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

@Implementation(method="deleteZone", manager=Manager.ConfigManager)
public class DeleteZoneCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteZoneCmd.class.getName());

    private static final String s_name = "deletezoneresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }

//    @Override
//    public List<Pair<String, Object>> execute(Map<String, Object> params) {
//    	Long zoneId = (Long) params.get(BaseCmd.Properties.ID.getName());
//    	Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//    	
//    	if (userId == null) {
//            userId = Long.valueOf(User.UID_SYSTEM);
//        }
//    	
//    	//verify input parameters
//    	DataCenterVO zone = getManagementServer().findDataCenterById(zoneId);
//    	if (zone == null) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find zone by id " + zoneId);
//    	}
//
//        try {
//             getManagementServer().deleteZone(userId, zoneId);
//        } catch (Exception ex) {
//            s_logger.error("Exception deleting zone", ex);
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, ex.getMessage());
//        }
//
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SUCCESS.getName(), "true"));
//        
//        return returnValues;
//    }


	@Override
	public String getResponse() {
		// TODO Auto-generated method stub
		return null;
	}
}
