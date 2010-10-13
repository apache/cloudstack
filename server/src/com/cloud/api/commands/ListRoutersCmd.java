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
import com.cloud.async.AsyncJobVO;
import com.cloud.domain.DomainVO;
import com.cloud.server.Criteria;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.DomainRouterVO;

public class ListRoutersCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListRoutersCmd.class.getName());

    private static final String s_name = "listroutersresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.HOST_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.STATE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
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
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
    	Long podId = (Long)params.get(BaseCmd.Properties.POD_ID.getName());
    	Long hostId = (Long)params.get(BaseCmd.Properties.HOST_ID.getName());
    	String name = (String)params.get(BaseCmd.Properties.NAME.getName());
    	String state = (String)params.get(BaseCmd.Properties.STATE.getName());
    	String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
    	Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());

        Long accountId = null;
        Long[] accountIds = null;

        // validate domainId before proceeding
        if (domainId != null) {
            if ((account != null) && !getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid domain id (" + domainId + ") given, unable to list routers");
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

        if (accountId != null) {
            accountIds = new Long[1];
            accountIds[0] = accountId;
        }

        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.ACCOUNTID, accountIds);
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.DOMAINID, domainId);
        c.addCriteria(Criteria.DATACENTERID, zoneId);
        c.addCriteria(Criteria.PODID, podId);
        c.addCriteria(Criteria.HOSTID, hostId);
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.STATE, state);

        List<DomainRouterVO> routers = getManagementServer().searchForRouters(c);

        List<Pair<String, Object>> routerTags = new ArrayList<Pair<String, Object>>();
        Object[] rTag = new Object[routers.size()];
        int i = 0;

        for (DomainRouterVO router : routers) {
        	List<Pair<String, Object>> routerData = new ArrayList<Pair<String, Object>>();
            if (router.getId() != null) {
            	routerData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), router.getId().toString()));
            }
            AsyncJobVO asyncJob = getManagementServer().findInstancePendingAsyncJob("domain_router", router.getId());
            if(asyncJob != null) {
            	routerData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), asyncJob.getId().toString()));
            	routerData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_STATUS.getName(), String.valueOf(asyncJob.getStatus())));
            } 
            
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), Long.valueOf(router.getDataCenterId()).toString()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().findDataCenterById(router.getDataCenterId()).getName()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.DNS1.getName(), router.getDns1()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.DNS2.getName(), router.getDns2()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_DOMAIN.getName(), router.getDomain()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.GATEWAY.getName(), router.getGateway()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), router.getName()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), Long.valueOf(router.getPodId()).toString()));
            if (router.getHostId() != null) {
            	routerData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_ID.getName(), router.getHostId().toString()));
            	routerData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_NAME.getName(), getManagementServer().getHostBy(router.getHostId()).getName()));
            } 
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_IP.getName(), router.getPrivateIpAddress()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_MAC_ADDRESS.getName(), router.getPrivateMacAddress()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_NETMASK.getName(), router.getPrivateNetmask()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_IP.getName(), router.getPublicIpAddress()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_MAC_ADDRESS.getName(), router.getPublicMacAddress()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_NETMASK.getName(), router.getPublicNetmask()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.GUEST_IP_ADDRESS.getName(), router.getGuestIpAddress()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.GUEST_MAC_ADDRESS.getName(), router.getGuestMacAddress()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.GUEST_NETMASK.getName(), router.getGuestNetmask()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_ID.getName(), Long.valueOf(router.getTemplateId()).toString()));
            routerData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(router.getCreated())));
            if (router.getHostId() != null) {
                routerData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_ID.getName(), router.getHostId().toString()));
                routerData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_NAME.getName(),getManagementServer().getHostBy(router.getHostId()).getName()));
            }
            if (router.getState() != null) {
                routerData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), router.getState().toString()));
            }

            Account accountTemp = getManagementServer().findAccountById(router.getAccountId());
            if (accountTemp != null) {
            	routerData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), accountTemp.getAccountName()));
            	routerData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), accountTemp.getDomainId()));
            	routerData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(accountTemp.getDomainId()).getName()));
            }

            rTag[i++] = routerData;
        }

        Pair<String, Object> routerTag = new Pair<String, Object>("router", rTag);
        routerTags.add(routerTag);
        return routerTags;
    }
}
