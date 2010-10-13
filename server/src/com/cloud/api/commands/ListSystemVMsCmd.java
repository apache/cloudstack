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
import com.cloud.async.AsyncJobVO;
import com.cloud.server.Criteria;
import com.cloud.utils.Pair;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.SecondaryStorageVmVO;

public class ListSystemVMsCmd extends BaseCmd  {
    public static final Logger s_logger = Logger.getLogger(ListSystemVMsCmd.class.getName());
	
    private static final String s_name = "listsystemvmsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();
    
    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.POD_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.HOST_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.STATE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SYSTEM_VM_TYPE, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Long id = (Long)params.get(BaseCmd.Properties.ID.getName());
        Long zoneId = (Long)params.get(BaseCmd.Properties.ZONE_ID.getName());
    	Long podId = (Long)params.get(BaseCmd.Properties.POD_ID.getName());
    	Long hostId = (Long)params.get(BaseCmd.Properties.HOST_ID.getName());
    	String name = (String)params.get(BaseCmd.Properties.NAME.getName());
    	String state = (String)params.get(BaseCmd.Properties.STATE.getName());
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
        
        Criteria c = new Criteria("id", Boolean.TRUE, startIndex, Long.valueOf(pageSizeNum));
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.DATACENTERID, zoneId);
        c.addCriteria(Criteria.PODID, podId);
        c.addCriteria(Criteria.HOSTID, hostId);
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.STATE, state);
        
        List<ConsoleProxyVO> proxies = getManagementServer().searchForConsoleProxy(c);
        List<SecondaryStorageVmVO> ssVms = getManagementServer().searchForSecondaryStorageVm(c);
        List<Pair<String, Object>> proxiesTags = new ArrayList<Pair<String, Object>>();

        Object[] proxyDataArray = new Object[proxies.size() + ssVms.size()];
        int i = 0;
        
        for (ConsoleProxyVO proxy : proxies) {
        	List<Pair<String, Object>> proxyData = new ArrayList<Pair<String, Object>>();
        	proxyData.add(new Pair<String, Object>(BaseCmd.Properties.SYSTEM_VM_TYPE.getName(), "consoleproxy"));

            if (proxy.getId() != null) {
            	proxyData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), proxy.getId().toString()));
            }
            
            AsyncJobVO asyncJob = getManagementServer().findInstancePendingAsyncJob("console_proxy", proxy.getId());
            if(asyncJob != null) {
                proxyData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), asyncJob.getId().toString()));
                proxyData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_STATUS.getName(), String.valueOf(asyncJob.getStatus())));
            } 
            
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), Long.valueOf(proxy.getDataCenterId()).toString()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().findDataCenterById(proxy.getDataCenterId()).getName()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.DNS1.getName(), proxy.getDns1()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.DNS2.getName(), proxy.getDns2()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_DOMAIN.getName(), proxy.getDomain()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.GATEWAY.getName(), proxy.getGateway()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), proxy.getName()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), Long.valueOf(proxy.getPodId()).toString()));
            if (proxy.getHostId() != null) {
            	proxyData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_ID.getName(), proxy.getHostId().toString()));
            	proxyData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_NAME.getName(), getManagementServer().getHostBy(proxy.getHostId()).getName()));
            } 
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_IP.getName(), proxy.getPrivateIpAddress()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_MAC_ADDRESS.getName(), proxy.getPrivateMacAddress()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_NETMASK.getName(), proxy.getPrivateNetmask()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_IP.getName(), proxy.getPublicIpAddress()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_MAC_ADDRESS.getName(), proxy.getPublicMacAddress()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_NETMASK.getName(), proxy.getPublicNetmask()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_ID.getName(), Long.valueOf(proxy.getTemplateId()).toString()));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(proxy.getCreated())));
            proxyData.add(new Pair<String, Object>(BaseCmd.Properties.ACTIVE_VIEWER_SESSIONS.getName(), 
            	String.valueOf(proxy.getActiveSession())));
            
            if (proxy.getState() != null) {
            	proxyData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), proxy.getState().toString()));
            }
            
            proxyDataArray[i++] = proxyData;
        }
        
        
        for (SecondaryStorageVmVO ssVm : ssVms) {
        	List<Pair<String, Object>> ssvmData = new ArrayList<Pair<String, Object>>();
        	ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.SYSTEM_VM_TYPE.getName(), "secondarystoragevm"));
            if (ssVm.getId() != null) {
            	ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), ssVm.getId().toString()));
            }
            
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_ID.getName(), Long.valueOf(ssVm.getDataCenterId()).toString()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.ZONE_NAME.getName(), getManagementServer().findDataCenterById(ssVm.getDataCenterId()).getName()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.DNS1.getName(), ssVm.getDns1()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.DNS2.getName(), ssVm.getDns2()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.NETWORK_DOMAIN.getName(), ssVm.getDomain()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.GATEWAY.getName(), ssVm.getGateway()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), ssVm.getName()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.POD_ID.getName(), Long.valueOf(ssVm.getPodId()).toString()));
            if (ssVm.getHostId() != null) {
            	ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_ID.getName(), ssVm.getHostId().toString()));
            	ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.HOST_NAME.getName(), getManagementServer().getHostBy(ssVm.getHostId()).getName()));
            } 
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_IP.getName(), ssVm.getPrivateIpAddress()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_MAC_ADDRESS.getName(), ssVm.getPrivateMacAddress()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.PRIVATE_NETMASK.getName(), ssVm.getPrivateNetmask()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_IP.getName(), ssVm.getPublicIpAddress()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_MAC_ADDRESS.getName(), ssVm.getPublicMacAddress()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.PUBLIC_NETMASK.getName(), ssVm.getPublicNetmask()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.TEMPLATE_ID.getName(), Long.valueOf(ssVm.getTemplateId()).toString()));
            ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(ssVm.getCreated())));
            
            if (ssVm.getState() != null) {
            	ssvmData.add(new Pair<String, Object>(BaseCmd.Properties.STATE.getName(), ssVm.getState().toString()));
            }
            
            proxyDataArray[i++] = ssvmData;
        }
        proxiesTags.add(new Pair<String, Object>("systemvm", proxyDataArray));
        return proxiesTags;
    }
}
