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
package com.cloud.vm;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.configuration.ManagementServiceConfiguration;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachinePowerStateSyncImplTest {
    @Mock
    MessageBus messageBus;
    @Mock
    VMInstanceDao instanceDao;
    @Mock
    HostDao hostDao;
    @Mock
    ManagementServiceConfiguration mgmtServiceConf;

    @InjectMocks
    VirtualMachinePowerStateSyncImpl virtualMachinePowerStateSync = new VirtualMachinePowerStateSyncImpl();

    @Before
    public void setup() {
        Mockito.lenient().when(instanceDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(VMInstanceVO.class));
        Mockito.lenient().when(hostDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(HostVO.class));
    }

    @Test
    public void test_updateAndPublishVmPowerStates_emptyStates() {
        virtualMachinePowerStateSync.updateAndPublishVmPowerStates(1L, new HashMap<>(), new Date());
        Mockito.verify(instanceDao, Mockito.never()).updatePowerState(Mockito.anyMap(), Mockito.anyLong(),
                Mockito.any(Date.class));
    }

    @Test
    public void test_updateAndPublishVmPowerStates_moreNotUpdated() {
        Map<Long, VirtualMachine.PowerState> powerStates = new HashMap<>();
        powerStates.put(1L, VirtualMachine.PowerState.PowerOff);
        Map<Long, VirtualMachine.PowerState> notUpdated = new HashMap<>(powerStates);
        notUpdated.put(2L, VirtualMachine.PowerState.PowerOn);
        Mockito.when(instanceDao.updatePowerState(Mockito.anyMap(), Mockito.anyLong(),
                Mockito.any(Date.class))).thenReturn(notUpdated);
        virtualMachinePowerStateSync.updateAndPublishVmPowerStates(1L, powerStates, new Date());
        Mockito.verify(messageBus, Mockito.never()).publish(Mockito.nullable(String.class), Mockito.anyString(),
                Mockito.any(PublishScope.class), Mockito.anyLong());
    }

    @Test
    public void test_updateAndPublishVmPowerStates_allUpdated() {
        Map<Long, VirtualMachine.PowerState> powerStates = new HashMap<>();
        powerStates.put(1L, VirtualMachine.PowerState.PowerOff);
        Mockito.when(instanceDao.updatePowerState(Mockito.anyMap(), Mockito.anyLong(),
                Mockito.any(Date.class))).thenReturn(new HashMap<>());
        virtualMachinePowerStateSync.updateAndPublishVmPowerStates(1L, powerStates, new Date());
        Mockito.verify(messageBus, Mockito.times(1)).publish(null,
                VirtualMachineManager.Topics.VM_POWER_STATE,
                PublishScope.GLOBAL,
                1L);
    }

    @Test
    public void test_updateAndPublishVmPowerStates_partialUpdated() {
        Map<Long, VirtualMachine.PowerState> powerStates = new HashMap<>();
        powerStates.put(1L, VirtualMachine.PowerState.PowerOn);
        powerStates.put(2L, VirtualMachine.PowerState.PowerOff);
        Map<Long, VirtualMachine.PowerState> notUpdated = new HashMap<>();
        notUpdated.put(2L, VirtualMachine.PowerState.PowerOff);
        Mockito.when(instanceDao.updatePowerState(Mockito.anyMap(), Mockito.anyLong(),
                Mockito.any(Date.class))).thenReturn(notUpdated);
        virtualMachinePowerStateSync.updateAndPublishVmPowerStates(1L, powerStates, new Date());
        Mockito.verify(messageBus, Mockito.times(1)).publish(null,
                VirtualMachineManager.Topics.VM_POWER_STATE,
                PublishScope.GLOBAL,
                1L);
        Mockito.verify(messageBus, Mockito.never()).publish(null,
                VirtualMachineManager.Topics.VM_POWER_STATE,
                PublishScope.GLOBAL,
                2L);
    }

    private VMInstanceVO instanceWithUpdateTime(Date updateTime) {
        VMInstanceVO instance = Mockito.mock(VMInstanceVO.class);
        Mockito.when(instance.getUpdateTime()).thenReturn(updateTime);
        return instance;
    }

    @Test
    public void test_hasRecentStateChange_withinGracefulPeriod() {
        Date now = new Date();
        VMInstanceVO instance = instanceWithUpdateTime(new Date(now.getTime() - 1000L));
        Assert.assertTrue(virtualMachinePowerStateSync.hasRecentStateChange(instance, now, 120000L));
    }

    @Test
    public void test_hasRecentStateChange_outsideGracefulPeriod() {
        Date now = new Date();
        VMInstanceVO instance = instanceWithUpdateTime(new Date(now.getTime() - 300000L));
        Assert.assertFalse(virtualMachinePowerStateSync.hasRecentStateChange(instance, now, 120000L));
    }

    @Test
    public void test_hasRecentStateChange_nullUpdateTime() {
        VMInstanceVO instance = instanceWithUpdateTime(null);
        Assert.assertFalse(virtualMachinePowerStateSync.hasRecentStateChange(instance, new Date(), 120000L));
    }

    @Test
    public void test_convertVmStateReport_mapsKnownAndSkipsUnknown() {
        Map<String, HostVmStateReportEntry> report = new HashMap<>();
        report.put("i-2-1-VM", new HostVmStateReportEntry(VirtualMachine.PowerState.PowerOn, "host"));
        report.put("i-2-2-VM", new HostVmStateReportEntry(VirtualMachine.PowerState.PowerOn, "host"));
        Map<String, Long> nameIdMap = new HashMap<>();
        nameIdMap.put("i-2-1-VM", 1L);
        Mockito.when(instanceDao.getNameIdMapForVmInstanceNames(Mockito.anyCollection())).thenReturn(nameIdMap);

        Map<Long, VirtualMachine.PowerState> result = virtualMachinePowerStateSync.convertVmStateReport(1L, report);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(VirtualMachine.PowerState.PowerOn, result.get(1L));
    }

    @Test
    public void test_convertVmStateReport_emptyReport() {
        Map<Long, VirtualMachine.PowerState> result =
                virtualMachinePowerStateSync.convertVmStateReport(1L, new HashMap<>());
        Assert.assertTrue(result.isEmpty());
        Mockito.verify(instanceDao, Mockito.never()).getNameIdMapForVmInstanceNames(Mockito.anyCollection());
    }

    @Test
    public void test_reportUnknownInstances_onlyReportsChanges() {
        Set<String> unknown = new HashSet<>();
        unknown.add("i-2-3-VM");
        // first sighting is reported, an identical set afterwards is not
        Assert.assertTrue(virtualMachinePowerStateSync.reportUnknownInstances(1L, unknown));
        Assert.assertFalse(virtualMachinePowerStateSync.reportUnknownInstances(1L, new HashSet<>(unknown)));

        // a new name in the set is a change and is reported again
        Set<String> grown = new HashSet<>(unknown);
        grown.add("i-2-4-VM");
        Assert.assertTrue(virtualMachinePowerStateSync.reportUnknownInstances(1L, grown));

        // clearing is reported once, then stays quiet
        Assert.assertTrue(virtualMachinePowerStateSync.reportUnknownInstances(1L, new HashSet<>()));
        Assert.assertFalse(virtualMachinePowerStateSync.reportUnknownInstances(1L, new HashSet<>()));
    }

    @Test
    public void test_reportUnknownInstances_noneEverSeen() {
        Assert.assertFalse(virtualMachinePowerStateSync.reportUnknownInstances(2L, new HashSet<>()));
    }

    private VMInstanceVO missingInstance(long id, Date updateTime) {
        VMInstanceVO instance = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(instance.getId()).thenReturn(id);
        Mockito.lenient().when(instance.getUuid()).thenReturn("uuid-" + id);
        Mockito.lenient().when(instance.getPowerStateUpdateTime()).thenReturn(null);
        Mockito.lenient().when(instance.getUpdateTime()).thenReturn(updateTime);
        return instance;
    }

    /**
     * The instance changed state a moment ago, so an in-flight report that does not mention it proves nothing.
     * This must hold even for a forced report, which is the bug being fixed: it fails if the
     * hasRecentStateChange() call in processMissingVmReport() is removed.
     */
    @Test
    public void test_processMissingVmReport_forcedReportDoesNotOverrideRecentStateChange() {
        VMInstanceVO instance = missingInstance(1L, new Date(System.currentTimeMillis() - 5000L));
        Mockito.when(mgmtServiceConf.getPingInterval()).thenReturn(60);
        Mockito.when(instanceDao.findByHostInStatesExcluding(Mockito.anyLong(), Mockito.anyCollection(),
                Mockito.eq(VirtualMachine.State.Running), Mockito.eq(VirtualMachine.State.Stopping),
                Mockito.eq(VirtualMachine.State.Starting))).thenReturn(Collections.singletonList(instance));

        virtualMachinePowerStateSync.processHostVmStatePingReport(1L, new HashMap<>(), true);

        Mockito.verify(instanceDao, Mockito.never()).updatePowerState(Mockito.anyMap(), Mockito.anyLong(),
                Mockito.any(Date.class));
    }

    /**
     * Same forced report, but the instance has been in its current state for longer than the graceful period.
     * It is genuinely missing and must still be reported, so out-of-band stop detection keeps working.
     */
    @Test
    public void test_processMissingVmReport_forcedReportStillReportsSettledInstance() {
        VMInstanceVO instance = missingInstance(1L, new Date(System.currentTimeMillis() - 600000L));
        Mockito.when(mgmtServiceConf.getPingInterval()).thenReturn(60);
        Mockito.when(instanceDao.findByHostInStatesExcluding(Mockito.anyLong(), Mockito.anyCollection(),
                Mockito.eq(VirtualMachine.State.Running), Mockito.eq(VirtualMachine.State.Stopping),
                Mockito.eq(VirtualMachine.State.Starting))).thenReturn(Collections.singletonList(instance));
        Mockito.when(instanceDao.updatePowerState(Mockito.anyMap(), Mockito.anyLong(),
                Mockito.any(Date.class))).thenReturn(new HashMap<>());

        virtualMachinePowerStateSync.processHostVmStatePingReport(1L, new HashMap<>(), true);

        Mockito.verify(instanceDao, Mockito.times(1)).updatePowerState(
                Mockito.argThat(states -> VirtualMachine.PowerState.PowerReportMissing.equals(states.get(1L))),
                Mockito.eq(1L), Mockito.any(Date.class));
    }

    /**
     * An unforced report must keep honouring the graceful period for a settled instance that is only briefly absent.
     */
    @Test
    public void test_processMissingVmReport_unforcedReportHonoursGracefulPeriod() {
        Mockito.when(mgmtServiceConf.getPingInterval()).thenReturn(60);
        VMInstanceVO instance = missingInstance(1L, new Date(System.currentTimeMillis() - 5000L));
        Mockito.when(instanceDao.findByHostInStatesExcluding(Mockito.anyLong(), Mockito.anyCollection(),
                Mockito.eq(VirtualMachine.State.Running), Mockito.eq(VirtualMachine.State.Stopping),
                Mockito.eq(VirtualMachine.State.Starting))).thenReturn(Collections.singletonList(instance));
        Mockito.lenient().when(instanceDao.isPowerStateUpToDate(instance)).thenReturn(true);

        virtualMachinePowerStateSync.processHostVmStatePingReport(1L, new HashMap<>(), false);

        Mockito.verify(instanceDao, Mockito.never()).updatePowerState(Mockito.anyMap(), Mockito.anyLong(),
                Mockito.any(Date.class));
    }

    /**
     * An unknown name in a report must be recorded, so a second sighting of the same set says nothing new.
     */
    @Test
    public void test_convertVmStateReport_recordsUnknownInstances() {
        Map<String, HostVmStateReportEntry> report = new HashMap<>();
        report.put("i-2-2-VM", new HostVmStateReportEntry(VirtualMachine.PowerState.PowerOn, "host"));
        Mockito.when(instanceDao.getNameIdMapForVmInstanceNames(Mockito.anyCollection()))
                .thenReturn(new HashMap<>());

        virtualMachinePowerStateSync.convertVmStateReport(1L, report);

        Assert.assertFalse(virtualMachinePowerStateSync.reportUnknownInstances(1L,
                new HashSet<>(Arrays.asList("i-2-2-VM"))));
    }
}
