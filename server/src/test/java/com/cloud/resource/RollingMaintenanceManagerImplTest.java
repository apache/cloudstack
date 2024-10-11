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

package com.cloud.resource;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.org.Cluster;
import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.After;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RollingMaintenanceManagerImplTest {

    @Mock
    HostDao hostDao;
    @Mock
    HostVO host1;
    @Mock
    HostVO host2;
    @Mock
    HostVO host3;
    @Mock
    HostVO host4;
    @Mock
    Cluster cluster;

    @Spy
    @InjectMocks
    private RollingMaintenanceManagerImpl manager = new RollingMaintenanceManagerImpl();

    // Hosts in cluster 1
    private static final long hostId1 = 1L;
    private static final long hostId2 = 2L;

    // Hosts in cluster 2
    private static final long hostId3 = 3L;
    private static final long hostId4 = 4L;

    private static final long clusterId1 = 1L;
    private static final long clusterId2 = 2L;

    private static final long podId = 1L;
    private static final long zoneId = 1L;

    private AutoCloseable closeable;

    @Before
    public void setup() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        Mockito.when(hostDao.findByClusterId(clusterId1)).thenReturn(Arrays.asList(host1, host2));
        Mockito.when(hostDao.findByClusterId(clusterId2)).thenReturn(Arrays.asList(host3, host4));
        List<HostVO> hosts = Arrays.asList(host1, host2, host3, host4);
        Mockito.when(hostDao.findByPodId(podId)).thenReturn(hosts);
        Mockito.when(hostDao.findByDataCenterId(zoneId)).thenReturn(hosts);
        for (HostVO host : hosts) {
            Mockito.when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
            Mockito.when(host.getState()).thenReturn(Status.Up);
            Mockito.when(host.isInMaintenanceStates()).thenReturn(false);
        }
        Mockito.when(host1.getClusterId()).thenReturn(clusterId1);
        Mockito.when(host2.getClusterId()).thenReturn(clusterId1);

        Mockito.when(host3.getClusterId()).thenReturn(clusterId2);
        Mockito.when(host4.getClusterId()).thenReturn(clusterId2);

        Mockito.when(hostDao.findById(hostId1)).thenReturn(host1);
        Mockito.when(hostDao.findById(hostId2)).thenReturn(host2);
        Mockito.when(hostDao.findById(hostId3)).thenReturn(host3);
        Mockito.when(hostDao.findById(hostId4)).thenReturn(host4);

        Mockito.when(host1.getStatus()).thenReturn(Status.Up);
        Mockito.when(host2.getStatus()).thenReturn(Status.Up);
        Mockito.when(host1.getResourceState()).thenReturn(ResourceState.Enabled);
        Mockito.when(host2.getResourceState()).thenReturn(ResourceState.Enabled);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    private void checkResults(Map<Long, List<Host>> result) {
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.containsKey(clusterId1));
        Assert.assertTrue(result.containsKey(clusterId2));
        List<Host> cluster1Hosts = result.get(clusterId1);
        List<Host> cluster2Hosts = result.get(clusterId2);
        Assert.assertEquals(2, cluster1Hosts.size());
        Assert.assertTrue(cluster1Hosts.contains(host1));
        Assert.assertTrue(cluster1Hosts.contains(host2));
        Assert.assertEquals(2, cluster2Hosts.size());
        Assert.assertTrue(cluster2Hosts.contains(host3));
        Assert.assertTrue(cluster2Hosts.contains(host4));
    }

    @Test
    public void testGetHostsByClusterForRollingMaintenanceZoneScope() {
        Map<Long, List<Host>> result = manager.getHostsByClusterForRollingMaintenance(RollingMaintenanceManager.ResourceType.Zone, Collections.singletonList(zoneId));
        checkResults(result);
    }

    @Test
    public void testGetHostsByClusterForRollingMaintenancePodScope() {
        Map<Long, List<Host>> result = manager.getHostsByClusterForRollingMaintenance(RollingMaintenanceManager.ResourceType.Pod, Collections.singletonList(podId));
        checkResults(result);
    }

    @Test
    public void testGetHostsByClusterForRollingMaintenanceClusterScope() {
        List<Long> clusterIds = Arrays.asList(clusterId1, clusterId2);
        Map<Long, List<Host>> result = manager.getHostsByClusterForRollingMaintenance(RollingMaintenanceManager.ResourceType.Cluster, clusterIds);
        checkResults(result);
    }

    @Test
    public void testGetHostsByClusterForRollingMaintenanceHostScope() {
        List<Long> hostIds = Arrays.asList(hostId1, hostId2, hostId3, hostId4);
        Map<Long, List<Host>> result = manager.getHostsByClusterForRollingMaintenance(RollingMaintenanceManager.ResourceType.Host, hostIds);
        checkResults(result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testPerformStateChecksNotForce() {
        List<Host> hosts = Arrays.asList(host1, host2);
        Mockito.when(host1.getStatus()).thenReturn(Status.Error);
        manager.performStateChecks(cluster, hosts, false, new ArrayList<>());
    }

    @Test
    public void testPerformStateChecksForce() {
        List<Host> hosts = new ArrayList<>();
        hosts.add(host1);
        hosts.add(host2);
        Mockito.when(host1.getStatus()).thenReturn(Status.Error);
        List<RollingMaintenanceManager.HostSkipped> skipped = new ArrayList<>();
        manager.performStateChecks(cluster, hosts, true, skipped);

        Assert.assertFalse(skipped.isEmpty());
        Assert.assertEquals(1, skipped.size());
        Assert.assertEquals(host1, skipped.get(0).getHost());

        Assert.assertEquals(1, hosts.size());
    }
}
