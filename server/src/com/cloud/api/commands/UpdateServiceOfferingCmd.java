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

@Implementation(method="updateServiceOffering", manager=Manager.ConfigManager)
public class UpdateServiceOfferingCmd extends BaseCmd
{
    public static final Logger s_logger = Logger.getLogger(UpdateServiceOfferingCmd.class.getName());
    private static final String s_name = "updateserviceofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="displaytext", type=CommandType.STRING)
    private String displayText;

    @Parameter(name="id", type=CommandType.LONG, required=true)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String serviceOfferingName;

    @Parameter(name="offerha", type=CommandType.BOOLEAN)
    private Boolean offerHa;

    @Parameter(name="tags", type=CommandType.STRING)
    private String tags;

    @Parameter(name="usevirtualnetwork", type=CommandType.BOOLEAN)
    private Boolean useVirtualNetwork;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public String getServiceOfferingName() {
        return serviceOfferingName;
    }

    public Boolean getOfferHa() {
        return offerHa;
    }

    public String getTags() {
        return tags;
    }

    public Boolean getUseVirtualNetwork() {
        return useVirtualNetwork;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
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
//        Long offeringId = (Long)params.get(BaseCmd.Properties.ID.getName());
//        String name = (String)params.get(BaseCmd.Properties.NAME.getName());
//        String displayText = (String)params.get(BaseCmd.Properties.DISPLAY_TEXT.getName());
//        Boolean offerHA = (Boolean) params.get(BaseCmd.Properties.OFFER_HA.getName());
//        Boolean useVirtualNetwork = (Boolean) params.get(BaseCmd.Properties.USE_VIRTUAL_NETWORK.getName());
//        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
//        String tags = (String)params.get(BaseCmd.Properties.TAGS.getName());
//        
//        if (userId == null) {
//            userId = Long.valueOf(User.UID_SYSTEM);
//        }
//        
//        // Verify input parameters
//        ServiceOfferingVO offering = getManagementServer().findServiceOfferingById(offeringId);
//    	if (offering == null) {
//    		throw new ServerApiException(BaseCmd.PARAM_ERROR, "unable to find service offering " + offeringId);
//    	}
//
//    	
//        try {     
//        	offering = getManagementServer().updateServiceOffering(userId, offeringId, name, displayText, offerHA, useVirtualNetwork, tags);
//        } catch (Exception ex) {
//            s_logger.error("Exception updating service offering", ex);
//            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update service offering " + offeringId + ":  internal error.");
//        }
//
//        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
//        if (offering != null) {
//        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), offeringId.toString()));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), offering.getName()));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DISPLAY_TEXT.getName(), offering.getDisplayText()));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CPU_NUMBER.getName(), Integer.valueOf(offering.getCpu()).toString()));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CPU_SPEED.getName(), Integer.valueOf(offering.getSpeed()).toString()));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.MEMORY.getName(), Integer.valueOf(offering.getRamSize()).toString()));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(offering.getCreated())));
//            String storageType = offering.getUseLocalStorage() ? "local" : "shared";
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.STORAGE_TYPE.getName(), storageType));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.OFFER_HA.getName(), offering.getOfferHA()));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.USE_VIRTUAL_NETWORK.getName(), (offering.getGuestIpType().equals(GuestIpType.Virtualized))));
//            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.TAGS.getName(), offering.getTags()));
//        } else {
//        	throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update service offering " + offeringId);
//        }
//        return returnValues;
//    }  
}
