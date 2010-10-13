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
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.domain.DomainVO;
import com.cloud.network.IPAddressVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListPublicIpAddressesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListPublicIpAddressesCmd.class.getName());

    private static final String s_name = "listpublicipaddressesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ALLOCATED_ONLY, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VLAN_DB_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.IP_ADDRESS, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.FOR_VIRTUAL_NETWORK, Boolean.FALSE));
    }

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	String accountName = (String) params.get(BaseCmd.Properties.ACCOUNT.getName());
    	Long domainId = (Long) params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Account account = (Account) params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Boolean allocatedOnly = (Boolean) params.get(BaseCmd.Properties.ALLOCATED_ONLY.getName());
        Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
        Long vlanDbId = (Long) params.get(BaseCmd.Properties.VLAN_DB_ID.getName());
        String ip = (String) params.get(BaseCmd.Properties.IP_ADDRESS.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());
        Boolean forVirtualNetwork = (Boolean) params.get(BaseCmd.Properties.FOR_VIRTUAL_NETWORK.getName());
        boolean isAdmin = false;

        if (allocatedOnly == null)
        	allocatedOnly = new Boolean(true);

        Long accountId = null;
        if ((account == null) || isAdmin(account.getType())) {
            isAdmin = true;
            // validate domainId before proceeding
            if (domainId != null) {
                if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list IP addresses.");
                }
                if (accountName != null) {
                    Account userAccount = getManagementServer().findAccountByName(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                domainId = ((account == null) ? DomainVO.ROOT_DOMAIN : account.getDomainId());
            }
        } else {
            accountId = account.getId();
        }

        Long[] accountIds = null;
        if (accountId != null) {
            accountIds = new Long[1];
            accountIds[0] = accountId;
        }

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

        Criteria c = new Criteria("address", Boolean.FALSE, startIndex, Long.valueOf(pageSizeNum));

        c.addCriteria(Criteria.ACCOUNTID, accountIds);
        if (allocatedOnly) {
        	c.addCriteria(Criteria.ISALLOCATED, allocatedOnly.booleanValue());
        }

        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.FOR_VIRTUAL_NETWORK, forVirtualNetwork);
        c.addCriteria(Criteria.DATACENTERID, zoneId);
        c.addCriteria(Criteria.IPADDRESS, ip);
        if (isAdmin) {
            c.addCriteria(Criteria.DOMAINID, domainId);
            c.addCriteria(Criteria.VLAN, vlanDbId);
        }

        List<IPAddressVO> result = getManagementServer().searchForIPAddresses(c);

        if (result == null) {
            throw new ServerApiException(BaseCmd.NET_LIST_ERROR, "unable to find IP Addresses for account: " + accountId);
        }
        List<Pair<String, Object>> ipAddrTags = new ArrayList<Pair<String, Object>>();
        Object[] ipTag = new Object[result.size()];
        int i = 0;
        for (IPAddressVO ipAddress : result) {
        	VlanVO vlan  = getManagementServer().findVlanById(ipAddress.getVlanDbId());
        	boolean forVirtualNetworks = vlan.getVlanType().equals(VlanType.VirtualNetwork);
        	
            List<Pair<String, Object>> ipAddrData = new ArrayList<Pair<String, Object>>();
            ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.IP_ADDRESS.getName(), ipAddress.getAddress()));
            if (ipAddress.getAllocated() != null) {
                ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.ALLOCATED.getName(), getDateString(ipAddress.getAllocated())));
            }
            ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), Long.valueOf(ipAddress.getDataCenterId()).toString()));
            ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().findDataCenterById(ipAddress.getDataCenterId()).getName()));
            ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.IS_SOURCE_NAT.getName(), Boolean.valueOf(ipAddress.isSourceNat()).toString()));
            //get account information
            Account accountTemp = getManagementServer().findAccountById(ipAddress.getAccountId());
            if (accountTemp !=null){
            	ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
                ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
                ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
            } 
            
            ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.FOR_VIRTUAL_NETWORK.getName(), forVirtualNetworks));
            //show this info to admin only
            if (isAdmin == true) {
            	ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.VLAN_DB_ID.getName(), Long.valueOf(ipAddress.getVlanDbId()).toString()));
                ipAddrData.add(new Pair<String, Object>(BaseCmd.Properties.VLAN_ID.getName(), getManagementServer().findVlanById(ipAddress.getVlanDbId()).getVlanId()));
            }
            ipTag[i++] = ipAddrData;
        }
        Pair<String, Object> ipAddrTag = new Pair<String, Object>("publicipaddress", ipTag);
        ipAddrTags.add(ipAddrTag);
        return ipAddrTags;
    }
}
