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
import com.cloud.host.HostLoadService;
import com.cloud.host.HostVO;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.host.dao.HostDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.dao.VolumeDao;
import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceDetailVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
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
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.ConfigurationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

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

    @Mock
    private AffinityGroupVMMapDao affinityGroupVMMapDao;

    @Mock
    private AsyncJobManager asyncJobManager;

    @Mock
    private VolumeDao volumeDao;

    @Mock
    private HostLoadService hostLoadService;

    @Mock
    private VMInstanceDetailsDao vmInstanceDetailsDao;

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
        Mockito.when(vm1.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm1.getState()).thenReturn(VirtualMachine.State.Running);

        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm2.getHostId()).thenReturn(2L);
        Mockito.when(vm2.getId()).thenReturn(2L);
        Mockito.when(vm2.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm2.getState()).thenReturn(VirtualMachine.State.Running);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);

        HostJoinVO hostJoin2 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin2.getId()).thenReturn(2L);
        Mockito.when(hostJoin2.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin2.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin2.getMemUsedCapacity()).thenReturn(1024L);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);
        vmList.add(vm2);

        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOffering.getCpu()).thenReturn(1);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(1024);
        Mockito.when(serviceOffering.getSpeed()).thenReturn(1000);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(
                true, false);

        Mockito.doReturn(new Pair<>(vm1, host2)).when(clusterDrsService).getBestMigration(
                Mockito.any(Cluster.class), Mockito.any(ClusterDrsAlgorithm.class),
                Mockito.anyList(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap(),
                Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap());
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn(
                serviceOffering);
        Mockito.when(hostJoinDao.searchByIds(host1.getId(), host2.getId())).thenReturn(List.of(hostJoin1, hostJoin2));

        List<Ternary<VirtualMachine, Host, Host>> iterations = clusterDrsService.getDrsPlan(cluster, 5);

        Mockito.verify(hostDao, Mockito.times(1)).findByClusterId(1L);
        Mockito.verify(vmInstanceDao, Mockito.times(1)).listByClusterId(1L);
        Mockito.verify(balancedAlgorithm, Mockito.times(2)).needsDrs(Mockito.any(), Mockito.anyMap(),
                Mockito.anyMap(), Mockito.anyMap());

        assertEquals(1, iterations.size());
    }

    @Test
    public void testGetDrsPlanWithDisabledCluster() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetDrsPlanWithZeroMaxIterations() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetDrsPlanWithNegativeMaxIterations() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, -1);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetDrsPlanWithSystemVMs() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        VMInstanceVO systemVm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(systemVm.getId()).thenReturn(1L);
        Mockito.when(systemVm.getHostId()).thenReturn(1L);
        Mockito.when(systemVm.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(systemVm);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getCpus()).thenReturn(4);
        Mockito.when(hostJoin1.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin1.getMemReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getTotalMemory()).thenReturn(8192L);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(true);
        Mockito.when(hostJoinDao.searchByIds(Mockito.any())).thenReturn(List.of(hostJoin1));

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(0, result.size());
        Mockito.verify(managementServer, Mockito.never()).listHostsForMigrationOfVM(
                Mockito.eq(systemVm), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.anyList());
    }

    @Test
    public void testGetDrsPlanWithNonRunningVMs() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        VMInstanceVO stoppedVm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(stoppedVm.getId()).thenReturn(1L);
        Mockito.when(stoppedVm.getHostId()).thenReturn(1L);
        Mockito.when(stoppedVm.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(stoppedVm.getState()).thenReturn(VirtualMachine.State.Stopped);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(stoppedVm);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getCpus()).thenReturn(4);
        Mockito.when(hostJoin1.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin1.getMemReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getTotalMemory()).thenReturn(8192L);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(true);
        Mockito.when(hostJoinDao.searchByIds(Mockito.any())).thenReturn(List.of(hostJoin1));

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(0, result.size());
        Mockito.verify(managementServer, Mockito.never()).listHostsForMigrationOfVM(
                Mockito.eq(stoppedVm), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.anyList());
    }

    @Test
    public void testGetDrsPlanWithSkipDrsFlag() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        VMInstanceVO skippedVm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(skippedVm.getId()).thenReturn(1L);
        Mockito.when(skippedVm.getHostId()).thenReturn(1L);
        Mockito.when(skippedVm.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(skippedVm.getState()).thenReturn(VirtualMachine.State.Running);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(skippedVm);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getCpus()).thenReturn(4);
        Mockito.when(hostJoin1.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin1.getMemReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getTotalMemory()).thenReturn(8192L);

        // Return the SKIP_DRS detail for skippedVm so the flag is actually honoured
        VMInstanceDetailVO skipDrsDetail = new VMInstanceDetailVO(1L, VmDetailConstants.SKIP_DRS, "true", true);
        Mockito.when(vmInstanceDetailsDao.listDetailsForResourceIdsAndKey(Mockito.anyList(),
                Mockito.eq(VmDetailConstants.SKIP_DRS))).thenReturn(List.of(skipDrsDetail));

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(true);
        Mockito.when(hostJoinDao.searchByIds(Mockito.any())).thenReturn(List.of(hostJoin1));

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(0, result.size());
        // Verify the VM was skipped before any host-compatibility lookup was attempted
        Mockito.verify(managementServer, Mockito.never()).listHostsForMigrationOfVM(
                Mockito.eq(skippedVm), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.anyList());
    }

    @Test
    public void testGetDrsPlanWithNoCompatibleHosts() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(1L);
        Mockito.when(vm1.getHostId()).thenReturn(1L);
        Mockito.when(vm1.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm1.getState()).thenReturn(VirtualMachine.State.Running);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getCpus()).thenReturn(4);
        Mockito.when(hostJoin1.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin1.getMemReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getTotalMemory()).thenReturn(8192L);

        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(true);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn(serviceOffering);
        Mockito.when(hostJoinDao.searchByIds(Mockito.any())).thenReturn(List.of(hostJoin1));
        // Return a Ternary with an empty suitable-hosts list to exercise the "no compatible hosts" path
        Mockito.when(managementServer.listHostsForMigrationOfVM(Mockito.eq(vm1), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.any(), Mockito.anyList()))
                .thenReturn(new Ternary<>(new Pair<>(Collections.emptyList(), 0), Collections.emptyList(), Collections.emptyMap()));

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(0, result.size());
        Mockito.verify(managementServer, Mockito.times(1)).listHostsForMigrationOfVM(Mockito.eq(vm1), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.anyList());
    }

    @Test
    public void testGetDrsPlanWithExceptionInCompatibilityCheck() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(1L);
        Mockito.when(vm1.getHostId()).thenReturn(1L);
        Mockito.when(vm1.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm1.getState()).thenReturn(VirtualMachine.State.Running);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getCpus()).thenReturn(4);
        Mockito.when(hostJoin1.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin1.getMemReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getTotalMemory()).thenReturn(8192L);

        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(true);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn(serviceOffering);
        Mockito.when(hostJoinDao.searchByIds(Mockito.any())).thenReturn(List.of(hostJoin1));
        // Throw an explicit exception so the catch-and-log path is exercised intentionally
        Mockito.when(managementServer.listHostsForMigrationOfVM(Mockito.eq(vm1), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.any(), Mockito.anyList()))
                .thenThrow(new RuntimeException("Simulated host compatibility check failure"));

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(0, result.size());
        // Exception should be caught and logged, not propagated
        Mockito.verify(managementServer, Mockito.times(1)).listHostsForMigrationOfVM(Mockito.eq(vm1), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.anyList());
    }

    @Test
    public void testGetDrsPlanWithNoBestMigration() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        VMInstanceVO vm1 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm1.getId()).thenReturn(1L);
        Mockito.when(vm1.getHostId()).thenReturn(1L);
        Mockito.when(vm1.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm1.getState()).thenReturn(VirtualMachine.State.Running);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getCpus()).thenReturn(4);
        Mockito.when(hostJoin1.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin1.getMemReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getTotalMemory()).thenReturn(8192L);

        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(true);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn(serviceOffering);
        Mockito.when(hostJoinDao.searchByIds(Mockito.any())).thenReturn(List.of(hostJoin1));

        HostVO compatibleHost = Mockito.mock(HostVO.class);

        // Return null migration (no best migration found)
        Mockito.doReturn(new Pair<>(null, null)).when(clusterDrsService).getBestMigration(
                Mockito.any(Cluster.class), Mockito.any(ClusterDrsAlgorithm.class),
                Mockito.anyList(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap(),
                Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap());

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetDrsPlanWithMultipleIterations() throws ConfigurationException {
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
        Mockito.when(vm1.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm1.getState()).thenReturn(VirtualMachine.State.Running);

        VMInstanceVO vm2 = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm2.getId()).thenReturn(2L);
        Mockito.when(vm2.getHostId()).thenReturn(1L);
        Mockito.when(vm2.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm2.getState()).thenReturn(VirtualMachine.State.Running);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);
        vmList.add(vm2);

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin1.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getCpus()).thenReturn(4);
        Mockito.when(hostJoin1.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin1.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin1.getMemReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin1.getTotalMemory()).thenReturn(8192L);

        HostJoinVO hostJoin2 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin2.getId()).thenReturn(2L);
        Mockito.when(hostJoin2.getCpuUsedCapacity()).thenReturn(1000L);
        Mockito.when(hostJoin2.getCpuReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin2.getCpus()).thenReturn(4);
        Mockito.when(hostJoin2.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin2.getMemUsedCapacity()).thenReturn(1024L);
        Mockito.when(hostJoin2.getMemReservedCapacity()).thenReturn(0L);
        Mockito.when(hostJoin2.getTotalMemory()).thenReturn(8192L);

        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOffering.getCpu()).thenReturn(1);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(1024);
        Mockito.when(serviceOffering.getSpeed()).thenReturn(1000);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(
                true, true, false);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn(serviceOffering);
        Mockito.when(hostJoinDao.searchByIds(1L, 2L)).thenReturn(List.of(hostJoin1, hostJoin2));

        // Return migrations for first two iterations, then null
        Mockito.doReturn(new Pair<>(vm1, host2), new Pair<>(vm2, host2), new Pair<>(null, null))
                .when(clusterDrsService).getBestMigration(
                        Mockito.any(Cluster.class), Mockito.any(ClusterDrsAlgorithm.class),
                        Mockito.anyList(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap(),
                        Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap());

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(2, result.size());
        Mockito.verify(balancedAlgorithm, Mockito.times(3)).needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap());
    }

    @Test
    public void testGetDrsPlanWithMigrationToOriginalHost() throws ConfigurationException {
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
        Mockito.when(vm1.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(vm1.getState()).thenReturn(VirtualMachine.State.Running);

        List<HostVO> hostList = new ArrayList<>();
        hostList.add(host1);
        hostList.add(host2);

        List<VMInstanceVO> vmList = new ArrayList<>();
        vmList.add(vm1);

        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(hostList);
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap())).thenReturn(true);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn(serviceOffering);

        // Return migration to original host (host1) - should break the loop
        Mockito.doReturn(new Pair<>(vm1, host1)).when(clusterDrsService).getBestMigration(
                Mockito.any(Cluster.class), Mockito.any(ClusterDrsAlgorithm.class),
                Mockito.anyList(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap(),
                Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap());

        List<Ternary<VirtualMachine, Host, Host>> result = clusterDrsService.getDrsPlan(cluster, 5);
        assertEquals(0, result.size());
        // Should break early when VM would migrate to original host
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
        Mockito.when(cmd.getMaxMigrations()).thenReturn(5);

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

        try (MockedStatic<ActionEventUtils> ignored = Mockito.mockStatic(ActionEventUtils.class)) {
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
    public void testGetBestMigration() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);

        HostVO destHost = Mockito.mock(HostVO.class);
        Mockito.when(destHost.getClusterId()).thenReturn(1L);

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

        // Create caches for the new method signature
        Map<Long, List<? extends Host>> vmToCompatibleHostsCache = new HashMap<>();
        vmToCompatibleHostsCache.put(vm1.getId(), List.of(destHost));
        vmToCompatibleHostsCache.put(vm2.getId(), List.of(destHost));

        Map<Long, Map<Host, Boolean>> vmToStorageMotionCache = new HashMap<>();
        vmToStorageMotionCache.put(vm1.getId(), Map.of(destHost, false));
        vmToStorageMotionCache.put(vm2.getId(), Map.of(destHost, false));

        Map<Long, com.cloud.deploy.DeploymentPlanner.ExcludeList> vmToExcludesMap = new HashMap<>();
        vmToExcludesMap.put(vm1.getId(), Mockito.mock(com.cloud.deploy.DeploymentPlanner.ExcludeList.class));
        vmToExcludesMap.put(vm2.getId(), Mockito.mock(com.cloud.deploy.DeploymentPlanner.ExcludeList.class));

        // Create capacity maps with dummy data for getClusterImbalance (include both source and dest hosts)
        Map<Long, Ternary<Long, Long, Long>> hostCpuCapacityMap = new HashMap<>();
        hostCpuCapacityMap.put(host.getId(), new Ternary<>(2000L, 0L, 3000L)); // Source host
        hostCpuCapacityMap.put(destHost.getId(), new Ternary<>(1000L, 0L, 2000L)); // Dest host
        Map<Long, Ternary<Long, Long, Long>> hostMemoryCapacityMap = new HashMap<>();
        hostMemoryCapacityMap.put(host.getId(), new Ternary<>(2L * 1024L * 1024L * 1024L, 0L, 3L * 1024L * 1024L * 1024L)); // Source host
        hostMemoryCapacityMap.put(destHost.getId(), new Ternary<>(1024L * 1024L * 1024L, 0L, 2L * 1024L * 1024L * 1024L)); // Dest host

        // Mock getMetrics for the optimized 10-parameter version used by getBestMigration
        // Return better improvement for vm1, worse for vm2
        Mockito.doReturn(new Ternary<>(1.0, 0.5, 1.5)).when(balancedAlgorithm).getMetrics(
                Mockito.eq(cluster), Mockito.eq(vm1), Mockito.any(ServiceOffering.class),
                Mockito.eq(destHost), Mockito.eq(hostCpuCapacityMap), Mockito.eq(hostMemoryCapacityMap), Mockito.any(Boolean.class),
                Mockito.any(Double.class), Mockito.any(double[].class), Mockito.any(Map.class));
        Mockito.doReturn(new Ternary<>(0.5, 2.5, 1.5)).when(balancedAlgorithm).getMetrics(
                Mockito.eq(cluster), Mockito.eq(vm2), Mockito.any(ServiceOffering.class),
                Mockito.eq(destHost), Mockito.eq(hostCpuCapacityMap), Mockito.eq(hostMemoryCapacityMap), Mockito.any(Boolean.class),
                Mockito.any(Double.class), Mockito.any(double[].class), Mockito.any(Map.class));

                Pair<VirtualMachine, Host> bestMigration = clusterDrsService.getBestMigration(cluster, balancedAlgorithm,
                vmList, vmIdServiceOfferingMap, hostCpuCapacityMap, hostMemoryCapacityMap,
                vmToCompatibleHostsCache, vmToStorageMotionCache, vmToExcludesMap);

        assertEquals(destHost, bestMigration.second());
        assertEquals(vm1, bestMigration.first());
    }

    @Test
    public void testGetBestMigrationDifferentCluster() throws ConfigurationException {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);

        HostVO destHost = Mockito.mock(HostVO.class);
        Mockito.when(destHost.getClusterId()).thenReturn(2L);

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

        // Create caches for the new method signature
        Map<Long, List<? extends Host>> vmToCompatibleHostsCache = new HashMap<>();
        vmToCompatibleHostsCache.put(vm1.getId(), List.of(destHost));
        vmToCompatibleHostsCache.put(vm2.getId(), List.of(destHost));

        Map<Long, Map<Host, Boolean>> vmToStorageMotionCache = new HashMap<>();
        vmToStorageMotionCache.put(vm1.getId(), Map.of(destHost, false));
        vmToStorageMotionCache.put(vm2.getId(), Map.of(destHost, false));

        Map<Long, com.cloud.deploy.DeploymentPlanner.ExcludeList> vmToExcludesMap = new HashMap<>();
        vmToExcludesMap.put(vm1.getId(), Mockito.mock(com.cloud.deploy.DeploymentPlanner.ExcludeList.class));
        vmToExcludesMap.put(vm2.getId(), Mockito.mock(com.cloud.deploy.DeploymentPlanner.ExcludeList.class));

        // Create capacity maps with dummy data for getClusterImbalance
        Map<Long, Ternary<Long, Long, Long>> hostCpuCapacityMap = new HashMap<>();
        hostCpuCapacityMap.put(destHost.getId(), new Ternary<>(1000L, 0L, 2000L));
        Map<Long, Ternary<Long, Long, Long>> hostMemoryCapacityMap = new HashMap<>();
        hostMemoryCapacityMap.put(destHost.getId(), new Ternary<>(1024L * 1024L * 1024L, 0L, 2L * 1024L * 1024L * 1024L));

        Pair<VirtualMachine, Host> bestMigration = clusterDrsService.getBestMigration(cluster, balancedAlgorithm,
                vmList, vmIdServiceOfferingMap, hostCpuCapacityMap, hostMemoryCapacityMap,
                vmToCompatibleHostsCache, vmToStorageMotionCache, vmToExcludesMap);

        assertNull(bestMigration.second());
        assertNull(bestMigration.first());
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

    private VMInstanceVO vmWithAffinityGroup(long id, Long hostId) {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(vm.getId()).thenReturn(id);
        Mockito.lenient().when(vm.getHostId()).thenReturn(hostId);
        Mockito.lenient().when(vm.getServiceOfferingId()).thenReturn(5L);
        Mockito.lenient().when(affinityGroupVMMapDao.listByInstanceId(id))
                .thenReturn(Collections.singletonList(Mockito.mock(AffinityGroupVMMapVO.class)));
        Mockito.lenient().when(serviceOfferingDao.findByIdIncludingRemoved(id, 5L))
                .thenReturn(Mockito.mock(ServiceOfferingVO.class));
        return vm;
    }

    private HostVO host(long id) {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.lenient().when(host.getId()).thenReturn(id);
        return host;
    }

    private void affinityExcludes(Long... hostIds) {
        ExcludeList excludes = new ExcludeList();
        for (Long hostId : hostIds) {
            excludes.addHost(hostId);
        }
        Mockito.when(managementServer.applyAffinityConstraints(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(excludes);
    }

    @Test
    public void testDestinationAllowedWhenVmHasNoAffinityGroups() {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getHostId()).thenReturn(10L);
        Mockito.when(affinityGroupVMMapDao.listByInstanceId(1L)).thenReturn(Collections.emptyList());

        assertFalse(clusterDrsService.destinationViolatesAffinity(vm, host(20L),
                Collections.emptyList(), Collections.emptyList()));
        Mockito.verify(managementServer, Mockito.never())
                .applyAffinityConstraints(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testDestinationRefusedWhenAffinityExcludesIt() {
        VMInstanceVO vm = vmWithAffinityGroup(1L, 10L);
        affinityExcludes(20L);

        assertTrue(clusterDrsService.destinationViolatesAffinity(vm, host(20L),
                Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    public void testDestinationAllowedWhenAffinityExcludesSomewhereElse() {
        VMInstanceVO vm = vmWithAffinityGroup(1L, 10L);
        affinityExcludes(21L);

        assertFalse(clusterDrsService.destinationViolatesAffinity(vm, host(20L),
                Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    public void testDestinationRefusedWhenVmIsNoLongerRunning() {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getHostId()).thenReturn(null);

        assertTrue("a plan for a VM that has since stopped is out of date",
                clusterDrsService.destinationViolatesAffinity(vm, host(20L),
                        Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    public void testDestinationRefusedWhileItIsStillOccupiedByAQueuedMigration() {
        // the swap case: A is queued to leave host1, so host1 is not free for B yet - and if A's
        // migration fails, A never leaves at all
        VMInstanceVO b = vmWithAffinityGroup(2L, 11L);

        assertTrue("a host is not free until the VM leaving it has actually gone",
                clusterDrsService.destinationViolatesAffinity(b, host(10L),
                        Collections.emptyList(), Collections.singletonList(10L)));
        Mockito.verify(managementServer, Mockito.never())
                .applyAffinityConstraints(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testExecuteDrsPlanKeepsSourceHostsOccupiedAcrossMigrations() {
        // plan: A host10 -> host12, B host11 -> host10. B must not be sent to host10.
        ClusterDrsPlanVO plan = Mockito.mock(ClusterDrsPlanVO.class);
        Mockito.when(plan.getId()).thenReturn(1L);

        ClusterDrsPlanMigrationVO first = Mockito.mock(ClusterDrsPlanMigrationVO.class);
        Mockito.when(first.getVmId()).thenReturn(1L);
        Mockito.when(first.getDestHostId()).thenReturn(12L);
        ClusterDrsPlanMigrationVO second = Mockito.mock(ClusterDrsPlanMigrationVO.class);
        Mockito.when(second.getId()).thenReturn(8L);
        Mockito.when(second.getVmId()).thenReturn(2L);
        Mockito.when(second.getDestHostId()).thenReturn(10L);
        Mockito.when(drsPlanMigrationDao.listPlanMigrationsToExecute(1L))
                .thenReturn(Arrays.asList(first, second));

        VMInstanceVO a = Mockito.mock(VMInstanceVO.class);
        Mockito.when(a.getHostId()).thenReturn(10L);
        VMInstanceVO b = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(a);
        Mockito.when(vmInstanceDao.findById(2L)).thenReturn(b);
        HostVO host12 = host(12L);
        HostVO host10 = host(10L);
        Mockito.when(hostDao.findById(12L)).thenReturn(host12);
        Mockito.when(hostDao.findById(10L)).thenReturn(host10);

        Mockito.doReturn(false).when(clusterDrsService).destinationViolatesAffinity(
                Mockito.eq(a), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(1L).when(clusterDrsService)
                .createMigrateVMAsyncJob(Mockito.any(), Mockito.any(), Mockito.anyLong());
        Mockito.when(asyncJobManager.getAsyncJob(1L)).thenReturn(Mockito.mock(AsyncJobVO.class));

        clusterDrsService.executeDrsPlan(plan);

        // A's source host must have been carried into the check for B
        ArgumentCaptor<List> sources = ArgumentCaptor.forClass(List.class);
        Mockito.verify(clusterDrsService).destinationViolatesAffinity(
                Mockito.eq(b), Mockito.any(), Mockito.any(), sources.capture());
        assertTrue("the host A is leaving must still count as occupied",
                sources.getValue().contains(10L));
    }

    @Test
    public void testExecuteDrsPlanSkipsMigrationThatViolatesAffinity() {
        ClusterDrsPlanVO plan = Mockito.mock(ClusterDrsPlanVO.class);
        Mockito.when(plan.getId()).thenReturn(1L);

        ClusterDrsPlanMigrationVO migration = Mockito.mock(ClusterDrsPlanMigrationVO.class);
        Mockito.when(migration.getId()).thenReturn(7L);
        Mockito.when(migration.getVmId()).thenReturn(1L);
        Mockito.when(migration.getDestHostId()).thenReturn(20L);
        Mockito.when(drsPlanMigrationDao.listPlanMigrationsToExecute(1L))
                .thenReturn(Collections.singletonList(migration));

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        HostVO host20 = host(20L);
        Mockito.when(vmInstanceDao.findById(1L)).thenReturn(vm);
        Mockito.when(hostDao.findById(20L)).thenReturn(host20);

        Mockito.doReturn(true).when(clusterDrsService)
                .destinationViolatesAffinity(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        clusterDrsService.executeDrsPlan(plan);

        Mockito.verify(migration).setStatus(JobInfo.Status.CANCELLED);
        Mockito.verify(clusterDrsService, Mockito.never())
                .createMigrateVMAsyncJob(Mockito.any(), Mockito.any(), Mockito.anyLong());
    }

    @Test
    public void testNonStrictAntiAffinityIsHonoured() {
        // a non-strict group lowers a host's priority rather than excluding it. rebalancing is
        // never a reason to break it, since not migrating is always available.
        DataCenterDeployment plan = new DataCenterDeployment(1L, 1L, 1L, null, null, null);
        plan.adjustHostPriority(30L, DeploymentPlan.HostPriorityAdjustment.LOWER);
        plan.adjustHostPriority(31L, DeploymentPlan.HostPriorityAdjustment.HIGHER);

        ExcludeList excludes = new ExcludeList();
        clusterDrsService.excludeHostsDispreferredByAffinity(plan, excludes);

        assertTrue("a dispreferred host must not be a DRS destination",
                excludes.getHostsToAvoid().contains(30L));
        assertFalse("a preferred host must stay available",
                excludes.getHostsToAvoid().contains(31L));
    }

    @Test
    public void testEquivalentVmsShareOneCandidateHostLookup() throws ConfigurationException {
        // the expensive call is listHostsForMigrationOfVM. Two interchangeable VMs must cost one
        // pass, not two - otherwise the grouping is not actually doing anything.
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);

        HostVO host1 = Mockito.mock(HostVO.class);
        Mockito.when(host1.getId()).thenReturn(1L);

        List<VMInstanceVO> vmList = new ArrayList<>();
        for (long id : new long[] {1L, 2L}) {
            VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
            Mockito.when(vm.getId()).thenReturn(id);
            Mockito.when(vm.getHostId()).thenReturn(1L);
            Mockito.when(vm.getType()).thenReturn(VirtualMachine.Type.User);
            Mockito.when(vm.getState()).thenReturn(VirtualMachine.State.Running);
            Mockito.when(vm.getServiceOfferingId()).thenReturn(9L);
            Mockito.lenient().when(vm.getTemplateId()).thenReturn(8L);
            vmList.add(vm);
        }

        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.isDynamic()).thenReturn(false);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(offering);
        Mockito.when(vmInstanceDetailsDao.listDetailsKeyPairs(Mockito.anyLong()))
                .thenReturn(Collections.emptyMap());
        Mockito.when(volumeDao.findCreatedByInstance(Mockito.anyLong())).thenReturn(Collections.emptyList());

        HostJoinVO hostJoin1 = Mockito.mock(HostJoinVO.class);
        Mockito.when(hostJoin1.getId()).thenReturn(1L);
        Mockito.when(hostJoin1.getCpus()).thenReturn(4);
        Mockito.when(hostJoin1.getSpeed()).thenReturn(1000L);
        Mockito.when(hostJoin1.getTotalMemory()).thenReturn(8192L);

        Mockito.when(hostDao.findByClusterId(1L)).thenReturn(List.of(host1));
        Mockito.when(vmInstanceDao.listByClusterId(1L)).thenReturn(vmList);
        Mockito.when(hostJoinDao.searchByIds(Mockito.any())).thenReturn(List.of(hostJoin1));
        Mockito.when(balancedAlgorithm.needsDrs(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.anyMap()))
                .thenReturn(false);
        Mockito.when(managementServer.listHostsForMigrationOfVM(Mockito.any(), Mockito.anyLong(),
                        Mockito.anyLong(), Mockito.any(), Mockito.anyList()))
                .thenReturn(new Ternary<>(new Pair<>(Collections.emptyList(), 0),
                        List.of(host1), Collections.emptyMap()));

        clusterDrsService.getDrsPlan(cluster, 5);

        Mockito.verify(managementServer, Mockito.times(1)).listHostsForMigrationOfVM(
                Mockito.any(), Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.anyList());
    }
}