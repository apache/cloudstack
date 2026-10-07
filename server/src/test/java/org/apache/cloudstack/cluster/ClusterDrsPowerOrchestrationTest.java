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
package org.apache.cloudstack.cluster;

import java.util.Arrays;
import java.util.Collections;

import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.outofbandmanagement.OutOfBandManagementService;
import org.apache.cloudstack.cluster.dao.ClusterDrsPlanDao;
import com.cloud.agent.AgentManager;
import com.cloud.host.Status;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.host.DetailVO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.resource.ResourceState;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDrsPowerOrchestrationTest {

    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private OutOfBandManagementService outOfBandManagementService;
    @Mock
    private HostDao hostDao;
    @Mock
    private HostDetailsDao hostDetailsDao;
    @Mock
    private ClusterDrsPlanDao drsPlanDao;
    @Mock
    private AgentManager agentManager;

    @InjectMocks
    private ClusterDrsServiceImpl drs = new ClusterDrsServiceImpl();

    private ClusterVO cluster(long id) {
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.lenient().when(cluster.getId()).thenReturn(id);
        return cluster;
    }

    private VMInstanceVO vm(long id, VirtualMachine.Type type, VirtualMachine.State state) {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(vm.getId()).thenReturn(id);
        Mockito.lenient().when(vm.getType()).thenReturn(type);
        Mockito.lenient().when(vm.getState()).thenReturn(state);
        return vm;
    }

    private HostVO host(long id) {
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.lenient().when(host.getId()).thenReturn(id);
        return host;
    }

    @Test
    public void hostEvacuatableTrueForRunningUserVmsOnly() {
        VMInstanceVO a = vm(1L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        VMInstanceVO b = vm(2L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        Mockito.when(vmInstanceDao.listByHostId(10L)).thenReturn(Arrays.asList(a, b));
        Assert.assertTrue(drs.hostEvacuatable(host(10L)));
    }

    @Test
    public void hostEvacuatableFalseWhenASystemVmIsPresent() {
        VMInstanceVO user = vm(1L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        VMInstanceVO router = vm(3L, VirtualMachine.Type.DomainRouter, VirtualMachine.State.Running);
        Mockito.when(vmInstanceDao.listByHostId(11L)).thenReturn(Arrays.asList(user, router));
        Assert.assertFalse(drs.hostEvacuatable(host(11L)));
    }

    @Test
    public void hostEvacuatableFalseWhenAVmIsNotRunning() {
        VMInstanceVO stopping = vm(4L, VirtualMachine.Type.User, VirtualMachine.State.Stopping);
        Mockito.when(vmInstanceDao.listByHostId(12L)).thenReturn(Collections.singletonList(stopping));
        Assert.assertFalse(drs.hostEvacuatable(host(12L)));
    }

    @Test
    public void hostEvacuatableFalseWhenEmpty() {
        Mockito.when(vmInstanceDao.listByHostId(13L)).thenReturn(Collections.emptyList());
        Assert.assertFalse(drs.hostEvacuatable(host(13L)));
    }

    @Test
    public void vmResourceNeedZeroWhenOfferingMissing() {
        VMInstanceVO vm = vm(5L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(Mockito.eq(5L), Mockito.anyLong())).thenReturn(null);
        Assert.assertEquals(0d, drs.vmResourceNeed(vm, true), 0.0001d);
    }

    @Test
    public void vmResourceNeedComputesCpuAndMemory() {
        VMInstanceVO vm = vm(6L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(99L);
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(offering.getCpu()).thenReturn(4);
        Mockito.when(offering.getSpeed()).thenReturn(2000);
        Mockito.when(offering.getRamSize()).thenReturn(2048);
        Mockito.when(serviceOfferingDao.findByIdIncludingRemoved(6L, 99L)).thenReturn(offering);

        Assert.assertEquals(8000d, drs.vmResourceNeed(vm, true), 0.0001d);
        Assert.assertEquals(2048d * 1024L * 1024L, drs.vmResourceNeed(vm, false), 0.0001d);
    }

    @Test
    public void isPowerManageableReflectsOobmEnabled() {
        HostVO h = host(20L);
        Mockito.when(outOfBandManagementService.isOutOfBandManagementEnabled(h)).thenReturn(true);
        Assert.assertTrue(drs.isPowerManageable(h));
    }

    @Test
    public void isPowerManageableFalseWhenOobmLookupThrows() {
        HostVO h = host(21L);
        Mockito.when(outOfBandManagementService.isOutOfBandManagementEnabled(h)).thenThrow(new RuntimeException("boom"));
        Assert.assertFalse(drs.isPowerManageable(h));
    }

    @Test
    public void powerOffPersistsWakeMarkerBeforeThePowerOff() {
        HostVO h = host(30L);
        Mockito.when(hostDao.updateResourceState(ResourceState.Enabled, ResourceState.Event.Disable, ResourceState.Disabled, h)).thenReturn(true);

        drs.powerOffHost(h, cluster(1L));

        // the durable wake marker must be written BEFORE the irreversible power-off, so a crash in between
        // still leaves a host that can be recognised and powered back on; and the agent is detached without
        // investigation before the power is cut so the link drop is not reported as a host-down failure.
        InOrder inOrder = Mockito.inOrder(hostDao, hostDetailsDao, agentManager, outOfBandManagementService);
        inOrder.verify(hostDao).updateResourceState(ResourceState.Enabled, ResourceState.Event.Disable, ResourceState.Disabled, h);
        inOrder.verify(hostDetailsDao).persist(Mockito.any(DetailVO.class));
        inOrder.verify(agentManager).disconnectWithoutInvestigation(30L, Status.Event.ShutdownRequested);
        inOrder.verify(outOfBandManagementService).executePowerOperation(Mockito.eq(h), Mockito.eq(OutOfBandManagement.PowerOperation.OFF), Mockito.any());
    }

    @Test
    public void powerOffAbortsWhenTheHostCannotBeDisabled() {
        HostVO h = host(31L);
        Mockito.when(hostDao.updateResourceState(ResourceState.Enabled, ResourceState.Event.Disable, ResourceState.Disabled, h)).thenReturn(false);

        drs.powerOffHost(h, cluster(1L));

        // a host that could not be disabled must never be powered off, and must not be marked.
        Mockito.verify(outOfBandManagementService, Mockito.never()).executePowerOperation(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(hostDetailsDao, Mockito.never()).persist(Mockito.any(DetailVO.class));
    }

    @Test
    public void powerOffLeavesHostDisabledAndMarkedWhenThePowerOffFails() {
        HostVO h = host(32L);
        Mockito.when(hostDao.updateResourceState(ResourceState.Enabled, ResourceState.Event.Disable, ResourceState.Disabled, h)).thenReturn(true);
        Mockito.doThrow(new RuntimeException("oobm down")).when(outOfBandManagementService)
                .executePowerOperation(Mockito.eq(h), Mockito.eq(OutOfBandManagement.PowerOperation.OFF), Mockito.any());

        drs.powerOffHost(h, cluster(1L));

        // a failed power-off must not roll back: the host stays disabled and marked so the next poll reconciles it,
        // because the failure does not prove the chassis is still on. No marker removal, no re-enable.
        Mockito.verify(hostDetailsDao, Mockito.never()).remove(Mockito.anyLong());
        Mockito.verify(hostDao, Mockito.never()).updateResourceState(ResourceState.Disabled, ResourceState.Event.Enable, ResourceState.Enabled, h);
    }

    @Test
    public void powerOnKeepsTheMarkerAndDoesNotEnableUntilTheHostIsUp() {
        HostVO h = host(33L);

        drs.powerOnHost(h, cluster(1L));

        // the power-on is issued, but the marker is kept and the host is not re-enabled: an unconfirmed power-on
        // must not look done, or a host that never actually boots is stranded out of the wake set.
        Mockito.verify(outOfBandManagementService).executePowerOperation(Mockito.eq(h), Mockito.eq(OutOfBandManagement.PowerOperation.ON), Mockito.any());
        Mockito.verify(hostDetailsDao, Mockito.never()).remove(Mockito.anyLong());
        Mockito.verify(hostDao, Mockito.never()).updateResourceState(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void hasInFlightDrsPlanTrueWhenAPlanIsInProgress() {
        Mockito.when(drsPlanDao.listByClusterIdAndStatus(5L, ClusterDrsPlan.Status.UNDER_REVIEW)).thenReturn(Collections.emptyList());
        Mockito.when(drsPlanDao.listByClusterIdAndStatus(5L, ClusterDrsPlan.Status.READY)).thenReturn(Collections.emptyList());
        Mockito.when(drsPlanDao.listByClusterIdAndStatus(5L, ClusterDrsPlan.Status.IN_PROGRESS))
                .thenReturn(Collections.singletonList(Mockito.mock(ClusterDrsPlanVO.class)));
        Assert.assertTrue(drs.hasInFlightDrsPlan(5L));
    }

    @Test
    public void hasInFlightDrsPlanFalseWhenNoPlansArePending() {
        Mockito.when(drsPlanDao.listByClusterIdAndStatus(Mockito.eq(5L), Mockito.any())).thenReturn(Collections.emptyList());
        Assert.assertFalse(drs.hasInFlightDrsPlan(5L));
    }

    @Test
    public void hasMigratingVmDetectsAnInFlightMigration() {
        VMInstanceVO running = vm(1L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        VMInstanceVO migrating = vm(2L, VirtualMachine.Type.User, VirtualMachine.State.Migrating);
        Mockito.when(vmInstanceDao.listByHostId(40L)).thenReturn(Arrays.asList(running, migrating));
        Assert.assertTrue(drs.hasMigratingVm(40L));
    }

    @Test
    public void hasMigratingVmFalseWhenNothingIsMigrating() {
        VMInstanceVO running = vm(1L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        Mockito.when(vmInstanceDao.listByHostId(40L)).thenReturn(Collections.singletonList(running));
        Assert.assertFalse(drs.hasMigratingVm(40L));
    }

    @Test
    public void isDrainingByDrsTrueOnlyForTheDrainingMarker() {
        HostVO h = host(41L);
        Mockito.when(hostDetailsDao.findDetail(41L, "drs.power.state"))
                .thenReturn(new DetailVO(41L, "drs.power.state", "draining"));
        Assert.assertTrue(drs.isDrainingByDrs(h));
    }

    @Test
    public void drainBatchWaitsWhileAMigrationIsInFlight() {
        HostVO candidate = host(42L);
        VMInstanceVO migrating = vm(1L, VirtualMachine.Type.User, VirtualMachine.State.Migrating);
        Mockito.when(vmInstanceDao.listByHostId(42L)).thenReturn(Collections.singletonList(migrating));
        drs.drainingVmCountByHost.put(42L, 3);

        drs.drainHostBatch(cluster(1L), candidate, Collections.emptyList(), Collections.emptyMap(), 0.75f, true);

        // a batch is still running: do not touch the marker or the recorded progress, just wait.
        Mockito.verify(hostDetailsDao, Mockito.never()).remove(Mockito.anyLong());
        Assert.assertEquals(Integer.valueOf(3), drs.drainingVmCountByHost.get(42L));
    }

    @Test
    public void drainBatchAbandonsWhenNoProgressSinceLastBatch() {
        HostVO candidate = host(43L);
        VMInstanceVO a = vm(1L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        VMInstanceVO b = vm(2L, VirtualMachine.Type.User, VirtualMachine.State.Running);
        Mockito.when(vmInstanceDao.listByHostId(43L)).thenReturn(Arrays.asList(a, b));
        Mockito.when(hostDetailsDao.findDetail(43L, "drs.power.state"))
                .thenReturn(new DetailVO(43L, "drs.power.state", "draining"));
        drs.drainingVmCountByHost.put(43L, 2); // same count as now: the last batch moved nothing

        drs.drainHostBatch(cluster(1L), candidate, Collections.emptyList(), Collections.emptyMap(), 0.75f, true);

        // no progress: the drain is abandoned (marker cleared, progress forgotten) instead of re-submitted forever.
        Mockito.verify(hostDetailsDao).remove(Mockito.anyLong());
        Assert.assertFalse(drs.drainingVmCountByHost.containsKey(43L));
    }

    @Test
    public void drainBatchAbandonsWhenTheHostIsAlreadyEmpty() {
        HostVO candidate = host(44L);
        Mockito.when(vmInstanceDao.listByHostId(44L)).thenReturn(Collections.emptyList());
        Mockito.when(hostDetailsDao.findDetail(44L, "drs.power.state"))
                .thenReturn(new DetailVO(44L, "drs.power.state", "draining"));
        drs.drainingVmCountByHost.put(44L, 1);

        drs.drainHostBatch(cluster(1L), candidate, Collections.emptyList(), Collections.emptyMap(), 0.75f, true);

        Mockito.verify(hostDetailsDao).remove(Mockito.anyLong());
        Assert.assertFalse(drs.drainingVmCountByHost.containsKey(44L));
    }
}
