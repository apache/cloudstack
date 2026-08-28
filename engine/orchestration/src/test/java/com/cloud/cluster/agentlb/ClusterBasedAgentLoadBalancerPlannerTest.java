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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Test;

import com.cloud.host.HostVO;

public class ClusterBasedAgentLoadBalancerPlannerTest {

    private final ClusterBasedAgentLoadBalancerPlanner planner = new ClusterBasedAgentLoadBalancerPlanner();

    private HostVO host(long clusterId) {
        HostVO host = new HostVO("guid-" + clusterId + "-" + System.nanoTime());
        host.setClusterId(clusterId);
        return host;
    }

    private LinkedHashMap<Long, List<HostVO>> clusterOf(int... clusterSizes) {
        LinkedHashMap<Long, List<HostVO>> map = new LinkedHashMap<>();
        long clusterId = 1;
        for (int size : clusterSizes) {
            List<HostVO> hosts = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                hosts.add(host(clusterId));
            }
            map.put(clusterId, hosts);
            clusterId++;
        }
        return map;
    }

    @Test
    public void singleOversizedClusterGivesAwayPartialHosts() {
        LinkedHashMap<Long, List<HostVO>> hostToClusterMap = clusterOf(2);
        int totalDirectHosts = 2;
        int avLoad = 1;
        int hostsToGive = totalDirectHosts - avLoad;

        List<HostVO> hostsToReturn = planner.selectHostsToGiveAway(hostToClusterMap, hostsToGive, totalDirectHosts);

        assertEquals("a lone oversized cluster must still give away hosts to satisfy the quota", 1, hostsToReturn.size());
    }

    @Test
    public void wholeClusterIsPreferredWhenItExactlyFitsTheQuota() {
        LinkedHashMap<Long, List<HostVO>> hostToClusterMap = clusterOf(2, 3);
        int totalDirectHosts = 5;
        int hostsToGive = 2;

        List<HostVO> hostsToReturn = planner.selectHostsToGiveAway(hostToClusterMap, hostsToGive, totalDirectHosts);

        assertEquals(2, hostsToReturn.size());
        assertTrue("should take the whole 2-host cluster rather than split the larger one", hostToClusterMap.get(1L).containsAll(hostsToReturn));
    }

    @Test
    public void smallerClusterIsPreferredOverSplittingWhenBothCanSatisfyQuota() {
        LinkedHashMap<Long, List<HostVO>> hostToClusterMap = clusterOf(5, 2);
        int totalDirectHosts = 7;
        int hostsToGive = 2;

        List<HostVO> hostsToReturn = planner.selectHostsToGiveAway(hostToClusterMap, hostsToGive, totalDirectHosts);

        assertEquals(2, hostsToReturn.size());
        assertTrue("should skip the oversized cluster and take the smaller cluster whole", hostToClusterMap.get(2L).containsAll(hostsToReturn));
    }

    @Test
    public void noHostsGivenAwayWhenAlreadyUnderThreshold() {
        LinkedHashMap<Long, List<HostVO>> hostToClusterMap = clusterOf(1);

        List<HostVO> hostsToReturn = planner.selectHostsToGiveAway(hostToClusterMap, 0, 1);

        assertTrue(hostsToReturn.isEmpty());
    }
}
