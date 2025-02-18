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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

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
}
