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
package com.cloud.cluster.agentlb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteria.Op;


@Local(value=AgentLoadBalancerPlanner.class)
public class ClusterBasedAgentLoadBalancerPlanner implements AgentLoadBalancerPlanner{
    private static final Logger s_logger = Logger.getLogger(AgentLoadBalancerPlanner.class);
    private String _name;
    
    @Inject HostDao _hostDao = null;
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    @Override
    public List<HostVO> getHostsToRebalance(long msId, int avLoad) {
    	SearchCriteria2<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
    	sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.Routing);
    	sc.addAnd(sc.getEntity().getManagementServerId(), Op.EQ, msId);
        List<HostVO> allHosts = sc.list();
 
        if (allHosts.size() <= avLoad) {
            s_logger.debug("Agent load = " + allHosts.size() + " for management server " + msId + " doesn't exceed average system agent load = " + avLoad + "; so it doesn't participate in agent rebalancing process");
            return null;
        }
        
        sc = SearchCriteria2.create(HostVO.class);
        sc.addAnd(sc.getEntity().getManagementServerId(), Op.EQ, msId);
        sc.addAnd(sc.getEntity().getStatus(), Op.EQ, Status.Up);
        List<HostVO> directHosts = sc.list();
        
        if (directHosts.isEmpty()) {
            s_logger.debug("No direct agents in status " + Status.Up + " exist for the management server " + msId + "; so it doesn't participate in agent rebalancing process");
            return null;
        } 
        
       
        Map<Long, List<HostVO>> hostToClusterMap = new HashMap<Long, List<HostVO>>();
        
        for (HostVO directHost : directHosts) {
            Long clusterId = directHost.getClusterId();
            List<HostVO> directHostsPerCluster = null;
            if (!hostToClusterMap.containsKey(clusterId)) {
                directHostsPerCluster = new ArrayList<HostVO>();
            } else {
                directHostsPerCluster = hostToClusterMap.get(clusterId);
            }
            directHostsPerCluster.add(directHost);
            hostToClusterMap.put(clusterId, directHostsPerCluster);
        }
        
        hostToClusterMap = sortByClusterSize(hostToClusterMap);
        
        int hostsToGive = allHosts.size() - avLoad;
        int hostsLeftToGive = hostsToGive;
        int hostsLeft = directHosts.size();
        List<HostVO> hostsToReturn = new ArrayList<HostVO>();
        
        s_logger.debug("Management server " + msId + " can give away " + hostsToGive + " as it currently owns " + allHosts.size() + " and the average agent load in the system is " + avLoad + "; finalyzing list of hosts to give away...");
        for (Long cluster : hostToClusterMap.keySet()) {
            List<HostVO> hostsInCluster = hostToClusterMap.get(cluster);
            hostsLeft = hostsLeft - hostsInCluster.size();
            if (hostsToReturn.size() < hostsToGive) {
                s_logger.debug("Trying cluster id=" + cluster);
                
                if (hostsInCluster.size() > hostsLeftToGive) {
                    if (hostsLeft >= hostsLeftToGive) {
                        s_logger.debug("Skipping cluster id=" + cluster + " as it has more hosts that we need: " + hostsInCluster.size() + " vs " + hostsLeftToGive);
                        continue;
                    } else {
                        //get all hosts that are needed from the cluster
                        s_logger.debug("Taking " + hostsLeftToGive + " from cluster " + cluster);
                        for (int i=0; i < hostsLeftToGive; i++) {
                            s_logger.trace("Taking host id=" + hostsInCluster.get(i) + " from cluster " + cluster);
                            hostsToReturn.add(hostsInCluster.get(i));
                        } 
                       
                        break;
                    }  
                } else {
                    s_logger.debug("Taking all " + hostsInCluster.size() + " hosts: " + hostsInCluster + " from cluster id=" + cluster);
                    hostsToReturn.addAll(hostsInCluster);
                    hostsLeftToGive = hostsLeftToGive - hostsInCluster.size();
                }
            } else {
                break;
            }
        }
        
        s_logger.debug("Management server " + msId + " is ready to give away " + hostsToReturn.size() + " hosts");
        return hostsToReturn;
    }
    
    public static LinkedHashMap<Long, List<HostVO>> sortByClusterSize(final Map<Long, List<HostVO>> hostToClusterMap) {
        List<Long> keys = new ArrayList<Long>();
        keys.addAll(hostToClusterMap.keySet());
        Collections.sort(keys, new Comparator<Long>() {
            @Override
            public int compare(Long o1, Long o2) {
                List<HostVO> v1 = hostToClusterMap.get(o1);
                List<HostVO> v2 = hostToClusterMap.get(o2);
                if (v1 == null) {
                    return (v2 == null) ? 0 : 1;
                }
                
                if (v1.size() < v2.size()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });
        
        LinkedHashMap<Long, List<HostVO>> sortedMap = new LinkedHashMap<Long, List<HostVO>>();
        for (Long key : keys) {
            sortedMap.put(key, hostToClusterMap.get(key));
        }
        return sortedMap;
    }

}
