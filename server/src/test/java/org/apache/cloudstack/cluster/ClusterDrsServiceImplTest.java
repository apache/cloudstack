/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster;

import com.cloud.api.query.dao.HostJoinDao;
import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.admin.cluster.GenerateClusterDrsPlanCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.ConfigurationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GlobalLock.class)
public class ClusterDrsServiceImplTest {

    @Mock
    ClusterDrsAlgorithm condensedAlgorithm;

    @Mock
    ManagementServer managementServer;

    @Mock
    ClusterDrsAlgorithm balancedAlgorithm;

    @Mock
    GenerateClusterDrsPlanCmd cmd;

    AutoCloseable closeable;

    @Mock
    private ClusterDao clusterDao;

    @Mock
    private HostDao hostDao;

    @Mock
    private HostJoinDao hostJoinDao;

    @Mock
    private ServiceOfferingDao serviceOfferingDao;

    @Mock
    private VMInstanceDao vmInstanceDao;

    @Spy
    @InjectMocks
    private ClusterDrsServiceImpl clusterDrsService = new ClusterDrsServiceImpl();

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        closeable = MockitoAnnotations.openMocks(this);

        HashMap<String, ClusterDrsAlgorithm> drsAlgorithmMap = new HashMap<>();
        drsAlgorithmMap.put("balanced", balancedAlgorithm);
        drsAlgorithmMap.put("condensed", condensedAlgorithm);

        clusterDrsService.setDrsAlgorithms(List.of(new ClusterDrsAlgorithm[]{balancedAlgorithm, condensedAlgorithm}));
        ReflectionTestUtils.setField(clusterDrsService, "drsAlgorithmMap", drsAlgorithmMap);
        Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(clusterDrsService.ClusterDrsAlgorithm, "balanced");
        Mockito.when(cmd.getId()).thenReturn(1L);

        PowerMockito.mockStatic(GlobalLock.class);
        GlobalLock lock = Mockito.mock(GlobalLock.class);
        PowerMockito.when(GlobalLock.getInternLock("cluster.drs.1")).thenReturn(lock);
        Mockito.when(lock.lock(Mockito.anyInt())).thenReturn(true);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(GenerateClusterDrsPlanCmd.class);
        assertEquals(cmdList, clusterDrsService.getCommands());
    }

    @Test
    public void testExecuteDrs() throws ConfigurationException, ManagementServerException, ResourceUnavailableException, VirtualMachineMigrationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(cluster.getClusterType()).thenReturn(Cluster.ClusterType.CloudManaged);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host2.getId()).thenReturn(2L);

        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(1L);
        Mockito.when(vm1.getInstanceName()).thenReturn("testVM1");
        Mockito.when(vm1.getHostId()).thenReturn(1L);

        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm2.getHostId()).thenReturn(2L);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin1.getMemReservedCapacity()).thenReturn(512L);

        HostJoinVO hostJoin2 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin2.getId()).thenReturn(2L);
        Mockito.when(hostJoin2.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin2.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin2.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin2.getMemReservedCapacity()).thenReturn(512L);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);
        vmList.add(vm2);

        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOffering.getCpu()).thenReturn(1);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(1024);
        Mockito.when(serviceOffering.getSpeed()).thenReturn(1000);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList())).thenReturn(true, false);
        Mockito.when(clusterDrsService.getBestMigration(Mockito.any(Cluster.class), Mockito.any(ClusterDrsAlgorithm.class), Mockito.anyList(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(new Pair<>(vm1, host2));
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn(serviceOffering);
        Mockito.when(hostJoinDao.searchByIds(host1.getId(), host2.getId())).thenReturn(List.of(hostJoin1, hostJoin2));

        List<Ternary<VirtualMachine, Host, Host>> iterations = clusterDrsService.getDrsPlan(cluster, 0.5);

        Mockito.verify(hostDao, Mockito.times(1)).findByClusterId(1L);
        Mockito.verify(vmInstanceDao, Mockito.times(1)).listByClusterId(1L);
        Mockito.verify(balancedAlgorithm, Mockito.times(1)).needsDrs(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList());

        assertEquals(1, iterations.size());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExecuteDrsClusterNotFound() {
        Mockito.when(clusterDao.findById(1L)).thenReturn(null);
        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExecuteDrsClusterDisabled() {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);

        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExecuteDrsClusterNotCloudManaged() {

        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(cluster.getClusterType()).thenReturn(Cluster.ClusterType.ExternalManaged);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);

        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testExecuteDrsInvalidIterations() {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(cluster.getClusterType()).thenReturn(Cluster.ClusterType.CloudManaged);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);
        Mockito.when(cmd.getIterations()).thenReturn(0.0);

        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testExecuteDrsConfigurationException() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(cluster.getClusterType()).thenReturn(Cluster.ClusterType.CloudManaged);
        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);
        Mockito.when(clusterDrsService.getDrsPlan(cluster, 0.5)).thenThrow(new CloudRuntimeException("test"));
        Mockito.when(cmd.getIterations()).thenReturn(0.5);

        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test
    public void testGetBestMigration() {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);

        HostVO destHost = Mockito.mock(HostVO.class);

        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(2L);

        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(1L);

        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm2.getId()).thenReturn(2L);

        List<VirtualMachine> vmList = new ArrayList<>();
        vmList.add(vm1);
        vmList.add(vm2);

        Map<Long, List<VirtualMachine>> hostVmMap = new HashMap<>();
        hostVmMap.put(host.getId(), new ArrayList<>());
        hostVmMap.get(host.getId()).add(vm1);
        hostVmMap.get(host.getId()).add(vm2);

        Map<Long, ServiceOffering> vmIdServiceOfferingMap = new HashMap<>();

        ServiceOffering serviceOffering = Mockito.mock(ServiceOffering.class);
        for (VirtualMachine vm : vmList) {

            vmIdServiceOfferingMap.put(vm.getId(), serviceOffering);
        }

        Map<Long, Long> hostCpuMap = new HashMap<>();

        Mockito.when(managementServer.listHostsForMigrationOfVM(vm1, 0L, null, null, vmList)).thenReturn(new Ternary<>(null, List.of(destHost), Map.of(destHost, false)));
        Mockito.when(managementServer.listHostsForMigrationOfVM(vm2, 0L, null, null, vmList)).thenReturn(new Ternary<>(null, Collections.emptyList(), Collections.emptyMap()));
        Mockito.when(balancedAlgorithm.getMetrics(cluster.getId(), vm1, serviceOffering, destHost, new HashMap<>(), new HashMap<>(), false)).thenReturn(new Ternary<>(1.0, 0.5, 1.5));

        Pair<VirtualMachine, Host> bestMigration = clusterDrsService.getBestMigration(cluster, balancedAlgorithm, vmList, vmIdServiceOfferingMap, new HashMap<>(), new HashMap<>());

        assertEquals(destHost, bestMigration.second());
        assertEquals(vm1, bestMigration.first());
    }
}
