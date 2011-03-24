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
package com.cloud.agent.manager.allocator.impl;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Local;
import org.apache.log4j.Logger;
import com.cloud.agent.manager.allocator.PodAllocator;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.service.ServiceOffering;
import com.cloud.utils.component.Inject;

/*
 * Use this PodAllocator when the deployment is such that pod has homogeneous tags on hosts.
 * If any pod can contain hosts with any tag, this optimization will not yield much benefit.  
 */
@Local(value=PodAllocator.class)
public class HostTagBasedPodAllocator extends UserConcentratedAllocator {
    private final static Logger s_logger = Logger.getLogger(HostTagBasedPodAllocator.class);
    @Inject HostPodDao _podDao;
    
    public HostTagBasedPodAllocator() {
    }
 
    @Override
    protected List<HostPodVO> listPods(long zoneId, ServiceOffering offering){
    	List<HostPodVO> podsInZone = new ArrayList<HostPodVO>();
    	
    	if(offering != null && offering.getHostTag() != null){
    		String hostTag = offering.getHostTag();
    		podsInZone = _podDao.listPodsByHostTag(zoneId, hostTag);
    		
    		if(podsInZone.size() == 0){
    	    	if (s_logger.isInfoEnabled()) {
    	    		s_logger.info("No Pods found in Zone: '"+zoneId+"' having Hosts with host tag: '"+ hostTag +"'");
    	    	}
    		}else{
    	    	if (s_logger.isDebugEnabled()) {
    	    		s_logger.debug("Found "+podsInZone.size() +" Pods in Zone: '"+zoneId+"' having Hosts with host tag: '"+ hostTag +"'");
    	    	}
    		}
    	}else{
    		podsInZone = super.listPods(zoneId, offering);
    	}
    	
    	return podsInZone;
    }
}
