// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.cluster.agentlb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class ClusterBasedAgentLoadBalancerPlanner extends AdapterBase implements AgentLoadBalancerPlanner {

    @Inject
    HostDao _hostDao = null;

    @Override
    public List<HostVO> getHostsToRebalance(long msId, int avLoad) {
        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        sc.and(sc.entity().getType(), Op.EQ, Host.Type.Routing);
        sc.and(sc.entity().getManagementServerId(), Op.EQ, msId);
        List<HostVO> allHosts = sc.list();

        if (allHosts.size() <= avLoad) {
            logger.debug("Agent load = " + allHosts.size() + " for management server " + msId + " doesn't exceed average system agent load = " + avLoad +
                "; so it doesn't participate in agent rebalancing process");
            return null;
        }

        sc = QueryBuilder.create(HostVO.class);
        sc.and(sc.entity().getManagementServerId(), Op.EQ, msId);
        sc.and(sc.entity().getType(), Op.EQ, Host.Type.Routing);
        sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
        List<HostVO> directHosts = sc.list();

        if (directHosts.isEmpty()) {
            logger.debug("No direct agents in status " + Status.Up + " exist for the management server " + msId +
                "; so it doesn't participate in agent rebalancing process");
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

        logger.debug("Management server " + msId + " can give away " + hostsToGive + " as it currently owns " + allHosts.size() +
            " and the average agent load in the system is " + avLoad + "; finalyzing list of hosts to give away...");
        for (Long cluster : hostToClusterMap.keySet()) {
            List<HostVO> hostsInCluster = hostToClusterMap.get(cluster);
            hostsLeft = hostsLeft - hostsInCluster.size();
            if (hostsToReturn.size() < hostsToGive) {
                logger.debug("Trying cluster id=" + cluster);

                if (hostsInCluster.size() > hostsLeftToGive) {
                    logger.debug("Skipping cluster id=" + cluster + " as it has more hosts than we need: " + hostsInCluster.size() + " vs " + hostsLeftToGive);
                    if (hostsLeft >= hostsLeftToGive) {
                        continue;
                    } else {
                        break;
                    }
                } else {
                    logger.debug("Taking all " + hostsInCluster.size() + " hosts: " + hostsInCluster + " from cluster id=" + cluster);
                    hostsToReturn.addAll(hostsInCluster);
                    hostsLeftToGive = hostsLeftToGive - hostsInCluster.size();
                }
            } else {
                break;
            }
        }

        logger.debug("Management server " + msId + " is ready to give away " + hostsToReturn.size() + " hosts");
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
