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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.host.HostVO;

public class ClusterBasedAgentLoadBalancerPlannerTest {

    private static List<HostVO> hosts(int count) {
        List<HostVO> hosts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            hosts.add(Mockito.mock(HostVO.class));
        }
        return hosts;
    }

    private static List<Long> sortedClusterIds(Map<Long, List<HostVO>> hostToClusterMap) {
        LinkedHashMap<Long, List<HostVO>> sorted = ClusterBasedAgentLoadBalancerPlanner.sortByClusterSize(hostToClusterMap);
        return new ArrayList<>(sorted.keySet());
    }

    @Test
    public void sortByClusterSizePutsLargestClusterFirst() {
        Map<Long, List<HostVO>> hostToClusterMap = new HashMap<>();
        hostToClusterMap.put(1L, hosts(1));
        hostToClusterMap.put(2L, hosts(3));
        hostToClusterMap.put(3L, hosts(2));

        Assert.assertEquals(Arrays.asList(2L, 3L, 1L), sortedClusterIds(hostToClusterMap));
    }

    @Test
    public void sortByClusterSizeOrdersManyClustersBySizeDescending() {
        Map<Long, List<HostVO>> hostToClusterMap = new HashMap<>();
        int[] sizes = {4, 1, 7, 2, 9, 3, 5, 8, 6, 10};
        for (int i = 0; i < sizes.length; i++) {
            hostToClusterMap.put((long) i + 1, hosts(sizes[i]));
        }

        List<Long> sortedIds = sortedClusterIds(hostToClusterMap);

        Assert.assertEquals(sizes.length, sortedIds.size());
        for (int i = 1; i < sortedIds.size(); i++) {
            int previousSize = hostToClusterMap.get(sortedIds.get(i - 1)).size();
            int currentSize = hostToClusterMap.get(sortedIds.get(i)).size();
            Assert.assertTrue("clusters are not ordered by size: " + sortedIds, previousSize >= currentSize);
        }
    }

    @Test
    public void sortByClusterSizePutsClustersWithoutHostListLast() {
        Map<Long, List<HostVO>> hostToClusterMap = new HashMap<>();
        hostToClusterMap.put(1L, null);
        hostToClusterMap.put(2L, hosts(1));
        hostToClusterMap.put(3L, hosts(2));

        Assert.assertEquals(Arrays.asList(3L, 2L, 1L), sortedClusterIds(hostToClusterMap));
    }

    @Test
    public void sortByClusterSizeKeepsAllEntries() {
        Map<Long, List<HostVO>> hostToClusterMap = new HashMap<>();
        List<HostVO> cluster1 = hosts(2);
        List<HostVO> cluster2 = hosts(5);
        hostToClusterMap.put(1L, cluster1);
        hostToClusterMap.put(2L, cluster2);

        LinkedHashMap<Long, List<HostVO>> sorted = ClusterBasedAgentLoadBalancerPlanner.sortByClusterSize(hostToClusterMap);

        Assert.assertEquals(2, sorted.size());
        Assert.assertSame(cluster1, sorted.get(1L));
        Assert.assertSame(cluster2, sorted.get(2L));
    }
}
