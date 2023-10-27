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
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.InvalidParameterValueException;
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
import org.apache.cloudstack.api.response.ClusterDrsPlanMigrationResponse;
import org.apache.cloudstack.api.response.ClusterDrsPlanResponse;
import org.apache.cloudstack.cluster.dao.ClusterDrsPlanDao;
import org.apache.cloudstack.cluster.dao.ClusterDrsPlanMigrationDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.ConfigurationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(MockitoJUnitRunner.class)
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
    private ClusterDrsPlanDao drsPlanDao;

    @Mock
    private ClusterDrsPlanMigrationDao drsPlanMigrationDao;

    @Mock
    private EventDao eventDao;

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

    private MockedStatic<GlobalLock> globalLockMocked;

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

        globalLockMocked = Mockito.mockStatic(GlobalLock.class);
        GlobalLock lock = Mockito.mock(GlobalLock.class);
        Mockito.when(GlobalLock.getInternLock("cluster.drs.1")).thenReturn(lock);
    }

    @After
    public void tearDown() throws Exception {
        globalLockMocked.close();
        closeable.close();
    }

    @Test
    public void testGetCommands() {
        assertFalse(clusterDrsService.getCommands().isEmpty());
    }

    @Test
    public void testGetDrsPlan() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        HostVO host2 = Mockito.mock(HostVO.class);
        Mockito.when(host2.getId()).thenReturn(2L);

        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(1L);
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
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList())).thenReturn(
                true, false);
        Mockito.when(
                clusterDrsService.getBestMigration(Mockito.any(Cluster.class), Mockito.any(ClusterDrsAlgorithm.class),
                        Mockito.anyList(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(
                new Pair<>(vm1, host2));
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn(
                serviceOffering);
        Mockito.when(hostJoinDao.searchByIds(host1.getId(), host2.getId())).thenReturn(List.of(hostJoin1, hostJoin2));

        List<Ternary<VirtualMachine, Host, Host>> iterations = clusterDrsService.getDrsPlan(cluster, 5);

        Mockito.verify(hostDao, Mockito.times(1)).findByClusterId(1L);
        Mockito.verify(vmInstanceDao, Mockito.times(1)).listByClusterId(1L);
        Mockito.verify(balancedAlgorithm, Mockito.times(2)).needsDrs(Mockito.anyLong(), Mockito.anyList(),
                Mockito.anyList());

        assertEquals(1, iterations.size());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGenerateDrsPlanClusterNotFound() {
        Mockito.when(clusterDao.findById(1L)).thenReturn(null);
        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGenerateDrsPlanClusterDisabled() {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);

        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGenerateDrsPlanClusterNotCloudManaged() {

        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);

        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGenerateDrsPlanInvalidIterations() {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getName()).thenReturn("testCluster");
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);
        Mockito.when(cmd.getMaxMigrations()).thenReturn(0);

        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGenerateDrsPlanConfigurationException() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);
        Mockito.when(clusterDrsService.getDrsPlan(cluster, 5)).thenThrow(new ConfigurationException("test"));
        Mockito.when(cmd.getMaxMigrations()).thenReturn(1);

        clusterDrsService.generateDrsPlan(cmd);
    }

    @Test
    public void testGenerateDrsPlan() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(1L);

        Host srcHost = Mockito.mock(Host.class);
        Mockito.when(srcHost.getId()).thenReturn(1L);

        Host destHost = Mockito.mock(Host.class);
        Mockito.when(destHost.getId()).thenReturn(2L);

        Mockito.when(clusterDao.findById(1L)).thenReturn(cluster);
        Mockito.when(eventDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(EventVO.class));
        Mockito.when(cmd.getMaxMigrations()).thenReturn(2);
        Mockito.doReturn(List.of(new Ternary<>(vm, srcHost,
                destHost))).when(clusterDrsService).getDrsPlan(Mockito.any(Cluster.class), Mockito.anyInt());

        ClusterDrsPlanMigrationResponse migrationResponse = Mockito.mock(ClusterDrsPlanMigrationResponse.class);

        Mockito.when(clusterDrsService.getResponseObjectForMigrations(Mockito.anyList())).thenReturn(
                List.of(migrationResponse));

        try(MockedStatic<ActionEventUtils> ignored = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(),
                    Mockito.anyLong(), Mockito.anyString())).thenReturn(1L);

            ClusterDrsPlanResponse response = clusterDrsService.generateDrsPlan(
                    cmd);

            assertEquals(1L, response.getMigrationPlans().size());
            assertEquals(migrationResponse, response.getMigrationPlans().get(0));
        }
    }

    @Test
    public void testPoll() {
        Mockito.doNothing().when(clusterDrsService).updateOldPlanMigrations();
        Mockito.doNothing().when(clusterDrsService).processPlans();
        Mockito.doNothing().when(clusterDrsService).generateDrsPlanForAllClusters();
        Mockito.doNothing().when(clusterDrsService).cleanUpOldDrsPlans();

        GlobalLock lock = Mockito.mock(GlobalLock.class);
        Mockito.when(lock.lock(Mockito.anyInt())).thenReturn(true);

        Mockito.when(GlobalLock.getInternLock(Mockito.anyString())).thenReturn(lock);

        clusterDrsService.poll(new Date());

        Mockito.verify(clusterDrsService, Mockito.times(1)).updateOldPlanMigrations();
        Mockito.verify(clusterDrsService, Mockito.times(2)).processPlans();
        Mockito.verify(clusterDrsService, Mockito.times(1)).generateDrsPlanForAllClusters();
    }

    @Test
    public void testUpdateOldPlanMigrations() {
        ClusterDrsPlanVO drsPlan1 = Mockito.mock(ClusterDrsPlanVO.class);
        ClusterDrsPlanVO drsPlan2 = Mockito.mock(ClusterDrsPlanVO.class);

        Mockito.when(drsPlanDao.listByStatus(ClusterDrsPlan.Status.IN_PROGRESS)).thenReturn(
                List.of(drsPlan1, drsPlan2));

        Mockito.doNothing().when(clusterDrsService).updateDrsPlanMigrations(drsPlan1);
        Mockito.doNothing().when(clusterDrsService).updateDrsPlanMigrations(drsPlan2);

        clusterDrsService.updateOldPlanMigrations();

        Mockito.verify(clusterDrsService, Mockito.times(2)).updateDrsPlanMigrations(
                Mockito.any(ClusterDrsPlanVO.class));
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
        Mockito.when(vm1.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm1.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(vm1.getDetails()).thenReturn(Collections.emptyMap());

        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm2.getId()).thenReturn(2L);
        Mockito.when(vm2.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm2.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(vm2.getDetails()).thenReturn(Collections.emptyMap());

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

        Mockito.when(managementServer.listHostsForMigrationOfVM(vm1, 0L, 500L, null, vmList)).thenReturn(
                new Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>>(
                        new Pair<>(List.of(destHost), 1), List.of(destHost), Map.of(destHost,
                        false)));
        Mockito.when(managementServer.listHostsForMigrationOfVM(vm2, 0L, 500L, null, vmList)).thenReturn(
                new Ternary<Pair<List<? extends Host>, Integer>, List<? extends Host>, Map<Host, Boolean>>(
                        new Pair<>(List.of(destHost), 1), List.of(destHost), Map.of(destHost,
                        false)));
        Mockito.when(balancedAlgorithm.getMetrics(cluster.getId(), vm1, serviceOffering, destHost, new HashMap<>(),
                new HashMap<>(), false)).thenReturn(new Ternary<>(1.0, 0.5, 1.5));

        Mockito.when(balancedAlgorithm.getMetrics(cluster.getId(), vm2, serviceOffering, destHost, new HashMap<>(),
                new HashMap<>(), false)).thenReturn(new Ternary<>(1.0, 2.5, 1.5));

        Pair<VirtualMachine, Host> bestMigration = clusterDrsService.getBestMigration(cluster, balancedAlgorithm,
                vmList, vmIdServiceOfferingMap, new HashMap<>(), new HashMap<>());

        assertEquals(destHost, bestMigration.second());
        assertEquals(vm1, bestMigration.first());
    }

    @Test
    public void testSavePlan() {
        Mockito.when(drsPlanDao.persist(Mockito.any(ClusterDrsPlanVO.class))).thenReturn(
                Mockito.mock(ClusterDrsPlanVO.class));
        Mockito.when(drsPlanMigrationDao.persist(Mockito.any(ClusterDrsPlanMigrationVO.class))).thenReturn(
                Mockito.mock(ClusterDrsPlanMigrationVO.class));

        clusterDrsService.savePlan(1L,
                List.of(new Ternary<>(Mockito.mock(VirtualMachine.class), Mockito.mock(Host.class),
                                Mockito.mock(Host.class)),
                        new Ternary<>(Mockito.mock(VirtualMachine.class), Mockito.mock(Host.class),
                                Mockito.mock(Host.class))), 1L, ClusterDrsPlan.Type.AUTOMATED,
                ClusterDrsPlan.Status.READY);

        Mockito.verify(drsPlanDao, Mockito.times(1)).persist(Mockito.any(ClusterDrsPlanVO.class));
        Mockito.verify(drsPlanMigrationDao, Mockito.times(2)).persist(Mockito.any(ClusterDrsPlanMigrationVO.class));
    }

    @Test
    public void testProcessPlans() {
        Mockito.when(drsPlanDao.listByStatus(ClusterDrsPlan.Status.READY)).thenReturn(
                List.of(Mockito.mock(ClusterDrsPlanVO.class), Mockito.mock(ClusterDrsPlanVO.class)));

        Mockito.doNothing().when(clusterDrsService).executeDrsPlan(Mockito.any(ClusterDrsPlanVO.class));

        clusterDrsService.processPlans();

        Mockito.verify(clusterDrsService, Mockito.times(2)).executeDrsPlan(Mockito.any(ClusterDrsPlanVO.class));
    }
}
