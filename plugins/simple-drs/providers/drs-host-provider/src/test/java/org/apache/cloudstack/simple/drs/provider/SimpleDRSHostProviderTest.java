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
package org.apache.cloudstack.simple.drs.provider;

import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import org.apache.cloudstack.api.command.admin.host.ListHostsCmd;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.query.QueryService;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleDRSHostProviderTest {

    @Mock
    ResourceManager resourceManager;
    @Mock
    QueryService queryService;

    @Spy
    @InjectMocks
    private SimpleDRSHostProvider hostProvider = new SimpleDRSHostProvider();

    @Mock
    HostResponse hostResponse1;
    @Mock
    HostResponse hostResponse2;
    @Mock
    HostResponse hostResponse3;
    @Mock
    HostResponse hostResponse4;
    @Mock
    Cluster cluster;

    private List<HostResponse> hostResponses;
    private ListResponse<HostResponse> queryResponse;

    private static final Long clusterId = 1L;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        hostResponses = Arrays.asList(hostResponse1, hostResponse2, hostResponse3, hostResponse4);
        queryResponse = new ListResponse<>();
        queryResponse.setResponses(hostResponses);
        for (HostResponse r : hostResponses) {
            Mockito.when(r.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM);
            Mockito.when(r.getHostType()).thenReturn(Host.Type.Routing);
            Mockito.when(r.getCpuAllocated()).thenReturn("25%");
        }
        Mockito.when(resourceManager.getCluster(clusterId)).thenReturn(cluster);
        Mockito.when(cluster.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        Mockito.when(queryService.searchForServers(Mockito.any(ListHostsCmd.class))).thenReturn(queryResponse);
    }

    @Test
    public void testGetNormalizedMetricsListFromHosts() {
        double[] values = hostProvider.getNormalizedMetricsListFromHosts(hostResponses);
        Assert.assertTrue(ArrayUtils.isNotEmpty(values));
        for (double v : values) {
            Assert.assertTrue(v >= 0 && v <= 1);
        }
    }

    @Test
    public void testGetNormalizedMetricsListFromHostsEmptyList() {
        double[] values = hostProvider.getNormalizedMetricsListFromHosts(new ArrayList<>());
        Assert.assertTrue(ArrayUtils.isEmpty(values));
    }

    @Test
    public void testGetNormalizedMetricsListFromHostsNullList() {
        double[] values = hostProvider.getNormalizedMetricsListFromHosts(null);
        Assert.assertTrue(ArrayUtils.isEmpty(values));
    }

    @Test
    public void testGetNormalizedMetricsListFromHostsDispersedValues() {
        Mockito.when(hostResponse1.getCpuAllocated()).thenReturn("25.11%");
        Mockito.when(hostResponse2.getCpuAllocated()).thenReturn("10.43%");
        Mockito.when(hostResponse3.getCpuAllocated()).thenReturn("22%");
        Mockito.when(hostResponse4.getCpuAllocated()).thenReturn("73.20%");
        double[] values = hostProvider.getNormalizedMetricsListFromHosts(hostResponses);
        Assert.assertTrue(ArrayUtils.isNotEmpty(values));
        for (double v : values) {
            Assert.assertTrue(v >= 0 && v <= 1);
        }
    }

    @Test
    public void testCalculateClusterImbalanceNoDeviation() {
        double imbalance = hostProvider.calculateClusterImbalance(clusterId);
        Assert.assertEquals(0, imbalance, 0);
    }

    @Test
    public void testCalculateClusterImbalanceExtremeDeviation() {
        Mockito.when(hostResponse1.getCpuAllocated()).thenReturn("0%");
        Mockito.when(hostResponse2.getCpuAllocated()).thenReturn("0%");
        Mockito.when(hostResponse3.getCpuAllocated()).thenReturn("100%");
        Mockito.when(hostResponse4.getCpuAllocated()).thenReturn("100%");
        double imbalance = hostProvider.calculateClusterImbalance(clusterId);
        Assert.assertEquals(1, imbalance, 0);
    }

    @Test
    public void testCalculateClusterImbalanceDistributedValues() {
        Mockito.when(hostResponse1.getCpuAllocated()).thenReturn("25.11%");
        Mockito.when(hostResponse2.getCpuAllocated()).thenReturn("10.43%");
        Mockito.when(hostResponse3.getCpuAllocated()).thenReturn("22%");
        Mockito.when(hostResponse4.getCpuAllocated()).thenReturn("73.20%");
        double imbalance = hostProvider.calculateClusterImbalance(clusterId);
        Assert.assertEquals(0.73, imbalance, 0.01);
    }
}
