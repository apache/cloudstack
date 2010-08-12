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
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.Pair;

public class CreateVlanIpRangeCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateVlanIpRangeCmd.class.getName());

    private static final String s_name = "createvlaniprangeresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.FOR_VIRTUAL_NETWORK, Boolean.FALSE));	
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VLAN, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.GATEWAY, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NETMASK, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.START_IP, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.END_IP, Boolean.FALSE));     
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Boolean forVirtualNetwork = (Boolean) params.get(BaseCmd.Properties.FOR_VIRTUAL_NETWORK.getName());
    	String vlanId = (String) params.get(BaseCmd.Properties.VLAN.getName());
    	String vlanGateway = (String) params.get(BaseCmd.Properties.GATEWAY.getName());
    	String vlanNetmask = (String) params.get(BaseCmd.Properties.NETMASK.getName());
    	Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
    	String accountName = (String) params.get(BaseCmd.Properties.ACCOUNT.getName());
    	Long domainId = (Long) params.get(BaseCmd.Properties.DOMAIN_ID.getName());
    	Long podId = (Long) params.get(BaseCmd.Properties.POD_ID.getName());
    	String startIp = (String) params.get(BaseCmd.Properties.START_IP.getName());
    	String endIp = (String) params.get(BaseCmd.Properties.END_IP.getName());    
    	Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
    	
    	if (userId == null) {
            userId = Long.valueOf(User.UID_SYSTEM);
        }
    	    	
    	// If forVirtualNetworks isn't specified, default it to true
    	if (forVirtualNetwork == null) {
    		forVirtualNetwork = Boolean.TRUE;
    	}
    	
    	// If the VLAN id is null, default it to untagged
    	if (vlanId == null) {
    		vlanId = Vlan.UNTAGGED;
    	}
    	
    	// If an account name and domain ID are specified, look up the account
    	Long accountId = null;
    	if (accountName != null && domainId != null) {
    		Account account = getManagementServer().findAccountByName(accountName, domainId);    		
    		if (account == null || account.getRemoved() != null) {
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid account.");
    		} else {
    			accountId = account.getId();
    		}
    	}    	
    	
    	VlanType vlanType = forVirtualNetwork ? VlanType.VirtualNetwork : VlanType.DirectAttached;
     	    
    	// Create a VLAN and public IP addresses
    	VlanVO vlan = null;
    	try {
			vlan = getManagementServer().createVlanAndPublicIpRange(userId, vlanType, zoneId, accountId, podId, vlanId, vlanGateway, vlanNetmask, startIp, endIp);
		} catch (Exception e) {
			s_logger.error("Error adding VLAN: ", e);
			throw new ServerApiException (BaseCmd.INTERNAL_ERROR, e.getMessage());
		}    	
    	
    	List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();    	
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), vlan.getId()));
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.FOR_VIRTUAL_NETWORK.getName(), forVirtualNetwork));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.VLAN.getName(), vlan.getVlanId()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), vlan.getDataCenterId()));
        
        if (accountId != null) {
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountName));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), domainId));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(domainId).getName()));
        }
        
        if (podId != null) {
        	HostPodVO pod = getManagementServer().findHostPodById(podId);
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), podId));
        	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), pod.getName()));
        }
        
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.GATEWAY.getName(), vlan.getVlanGateway()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.NETMASK.getName(), vlan.getVlanNetmask()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), vlan.getIpRange()));
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.START_IP.getName(), startIp));
    	returnValues.add(new Pair<String, Object>(BaseCmd.Properties.END_IP.getName(), endIp));        
        return returnValues;
    }
}
