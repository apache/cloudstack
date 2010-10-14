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
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;

public class ListVlanIpRangesCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(ListVlanIpRangesCmd.class.getName());

    private static final String s_name = "listvlaniprangesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VLAN, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Long id = (Long) params.get(BaseCmd.Properties.ID.getName());
    	String vlanId = (String) params.get(BaseCmd.Properties.VLAN.getName());
    	Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
    	String accountName = (String) params.get(BaseCmd.Properties.ACCOUNT.getName());
    	Long domainId = (Long) params.get(BaseCmd.Properties.DOMAIN_ID.getName());
    	Long podId = (Long) params.get(BaseCmd.Properties.POD_ID.getName());
    	String name = (String) params.get(BaseCmd.Properties.NAME.getName());
    	String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
    	Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
    	Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        
    	
    	Long startIndex = Long.valueOf(0);
    	int pageSizeNum = 50;
    	if (pageSize != null) {
    		pageSizeNum = pageSize.intValue();
    	}
	    
    	if (page != null) {
    		int pageNum = page.intValue();
    		if (pageNum > 0) {
    			startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
    	}
    	
    	// If an account name and domain ID are specified, look up the account
    	Long accountId = null;
    	if (accountName != null && domainId != null) {
    		Account account = getManagementServer().findAccountByName(accountName, domainId);
    		if (account == null) {
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Please specify a valid account.");
    		} else {
    			accountId = account.getId();
    		}
    	} 

    	Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));

    	c.addCriteria(Criteria.KEYWORD, keyword);
    	c.addCriteria(Criteria.ID, id);
    	c.addCriteria(Criteria.VLAN, vlanId);
    	c.addCriteria(Criteria.DATACENTERID, zoneId);
    	c.addCriteria(Criteria.ACCOUNTID, accountId);
    	c.addCriteria(Criteria.PODID, podId);        	

    	List<? extends VlanVO> vlans = getManagementServer().searchForVlans(c);

    	if (vlans == null) {
    		throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "unable to find vlans");
    	}
        
    	Object[] vlanTag = new Object[vlans.size()];
    	int i = 0;
       
    	for (VlanVO vlan : vlans) {
    		accountId = getManagementServer().getAccountIdForVlan(vlan.getId());
    		podId = getManagementServer().getPodIdForVlan(vlan.getId());
    		
    		List<Pair<String, Object>> vlanData = new ArrayList<Pair<String, Object>>();
    		vlanData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), vlan.getId()));
    		vlanData.add(new Pair<String, Object>(BaseCmd.Properties.FOR_VIRTUAL_NETWORK.getName(), (vlan.getVlanType().equals(VlanType.VirtualNetwork))));
    		vlanData.add(new Pair<String, Object>(BaseCmd.Properties.VLAN.getName(), vlan.getVlanId()));
    		vlanData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), vlan.getDataCenterId()));
    		
    		if (accountId != null) {
    			Account account = getManagementServer().findAccountById(accountId);
            	vlanData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), account.getAccountName()));
            	vlanData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), account.getDomainId()));
            	vlanData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(account.getDomainId()).getName()));
            }
            
            if (podId != null) {
            	HostPodVO pod = getManagementServer().findHostPodById(podId);
            	vlanData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), podId));
            	vlanData.add(new Pair<String, Object>(BaseCmd.Properties.POD_NAME.getName(), pod.getName()));
            }
            
    		vlanData.add(new Pair<String, Object>(BaseCmd.Properties.GATEWAY.getName(), vlan.getVlanGateway()));
    		vlanData.add(new Pair<String, Object>(BaseCmd.Properties.NETMASK.getName(), vlan.getVlanNetmask()));
    		vlanData.add(new Pair<String, Object>(BaseCmd.Properties.DESCRIPTION.getName(), vlan.getIpRange()));
    		vlanTag[i++] = vlanData;
    	}
    
    	List<Pair<String, Object>> returnTags = new ArrayList<Pair<String, Object>>();
    	Pair<String, Object> vlanTags = new Pair<String, Object>("vlaniprange", vlanTag);
    	returnTags.add(vlanTags);
    	return returnTags;

    } 	
}
