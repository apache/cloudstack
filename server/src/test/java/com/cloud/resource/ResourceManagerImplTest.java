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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.StorageManager;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.cloud.resource.ResourceState.Event.InternalEnterMaintenance;
import static com.cloud.resource.ResourceState.Event.UnableToMigrate;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionEventUtils.class, ResourceManagerImpl.class})
public class ResourceManagerImplTest {

    @Mock
    private CapacityDao capacityDao;
    @Mock
    private StorageManager storageManager;
    @Mock
    private HighAvailabilityManager haManager;
    @Mock
    private UserVmDetailsDao userVmDetailsDao;
    @Mock
    private AgentManager agentManager;
    @Mock
    private HostDao hostDao;
    @Mock
    private VMInstanceDao vmInstanceDao;

    @Spy
    @InjectMocks
    private ResourceManagerImpl resourceManager = new ResourceManagerImpl();

    @Mock
    private HostVO host;
    @Mock
    private VMInstanceVO vm1;
    @Mock
    private VMInstanceVO vm2;

    @Mock
    private GetVncPortAnswer getVncPortAnswerVm1;
    @Mock
    private GetVncPortAnswer getVncPortAnswerVm2;
    @Mock
    private GetVncPortCommand getVncPortCommandVm1;
    @Mock
    private GetVncPortCommand getVncPortCommandVm2;

    private static long hostId = 1L;

    private static long vm1Id = 1L;
    private static String vm1InstanceName = "i-1-VM";
    private static long vm2Id = 2L;
    private static String vm2InstanceName = "i-2-VM";

    private static String vm1VncAddress = "10.2.2.2";
    private static int vm1VncPort = 5900;
    private static String vm2VncAddress = "10.2.2.2";
    private static int vm2VncPort = 5901;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(host.getType()).thenReturn(Host.Type.Routing);
        when(host.getId()).thenReturn(hostId);
        when(host.getResourceState()).thenReturn(ResourceState.Enabled);
        when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.VMware);
        when(hostDao.findById(hostId)).thenReturn(host);
        when(vm1.getId()).thenReturn(vm1Id);
        when(vm2.getId()).thenReturn(vm2Id);
        when(vm1.getInstanceName()).thenReturn(vm1InstanceName);
        when(vm2.getInstanceName()).thenReturn(vm2InstanceName);
        when(vmInstanceDao.listByHostId(hostId)).thenReturn(new ArrayList<>());
        when(vmInstanceDao.listVmsMigratingFromHost(hostId)).thenReturn(new ArrayList<>());
        when(vmInstanceDao.listNonMigratingVmsByHostEqualsLastHost(hostId)).thenReturn(new ArrayList<>());
        PowerMockito.mockStatic(ActionEventUtils.class);
        BDDMockito.given(ActionEventUtils.onCompletedActionEvent(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyLong()))
                .willReturn(1L);
        when(getVncPortAnswerVm1.getAddress()).thenReturn(vm1VncAddress);
        when(getVncPortAnswerVm1.getPort()).thenReturn(vm1VncPort);
        when(getVncPortAnswerVm2.getAddress()).thenReturn(vm2VncAddress);
        when(getVncPortAnswerVm2.getPort()).thenReturn(vm2VncPort);
        PowerMockito.whenNew(GetVncPortCommand.class).withArguments(vm1Id, vm1InstanceName).thenReturn(getVncPortCommandVm1);
        PowerMockito.whenNew(GetVncPortCommand.class).withArguments(vm2Id, vm2InstanceName).thenReturn(getVncPortCommandVm2);
        when(agentManager.easySend(eq(hostId), eq(getVncPortCommandVm1))).thenReturn(getVncPortAnswerVm1);
        when(agentManager.easySend(eq(hostId), eq(getVncPortCommandVm2))).thenReturn(getVncPortAnswerVm2);
    }

    @Test
    public void testCheckAndMaintainEnterMaintenanceMode() throws NoTransitionException {
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).isHostInMaintenance(host, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        verify(resourceManager).setHostIntoMaintenance(host);
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(InternalEnterMaintenance), anyLong());
        Assert.assertTrue(enterMaintenanceMode);
    }

    @Test
    public void testCheckAndMaintainErrorInMaintenanceRunningVms() throws NoTransitionException {
        when(vmInstanceDao.listByHostId(hostId)).thenReturn(Arrays.asList(vm1, vm2));
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).isHostInMaintenance(host, Arrays.asList(vm1, vm2), new ArrayList<>(), new ArrayList<>());
        Assert.assertFalse(enterMaintenanceMode);
    }

    @Test
    public void testCheckAndMaintainErrorInMaintenanceMigratingVms() throws NoTransitionException {
        when(vmInstanceDao.listVmsMigratingFromHost(hostId)).thenReturn(Arrays.asList(vm1, vm2));
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).isHostInMaintenance(host, new ArrayList<>(), Arrays.asList(vm1, vm2), new ArrayList<>());
        Assert.assertFalse(enterMaintenanceMode);
    }

    @Test
    public void testCheckAndMaintainErrorInMaintenanceFailedMigrations() throws NoTransitionException {
        when(vmInstanceDao.listNonMigratingVmsByHostEqualsLastHost(hostId)).thenReturn(Arrays.asList(vm1, vm2));
        boolean enterMaintenanceMode = resourceManager.checkAndMaintain(hostId);
        verify(resourceManager).isHostInMaintenance(host, new ArrayList<>(), new ArrayList<>(), Arrays.asList(vm1, vm2));
        verify(resourceManager).setHostIntoErrorInMaintenance(host, Arrays.asList(vm1, vm2));
        verify(resourceManager).resourceStateTransitTo(eq(host), eq(UnableToMigrate), anyLong());
        Assert.assertFalse(enterMaintenanceMode);
    }

    @Test
    public void testConfigureVncAccessForKVMHostFailedMigrations() {
        when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        List<VMInstanceVO> vms = Arrays.asList(vm1, vm2);
        resourceManager.configureVncAccessForKVMHostFailedMigrations(host, vms);
        verify(agentManager).pullAgentOutMaintenance(hostId);
        verify(resourceManager).setKVMVncAccess(hostId, vms);
        verify(agentManager, times(vms.size())).easySend(eq(hostId), any(GetVncPortCommand.class));
        verify(userVmDetailsDao).addDetail(eq(vm1Id), eq("kvm.vnc.address"), eq(vm1VncAddress), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm1Id), eq("kvm.vnc.port"), eq(String.valueOf(vm1VncPort)), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm2Id), eq("kvm.vnc.address"), eq(vm2VncAddress), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vm2Id), eq("kvm.vnc.port"), eq(String.valueOf(vm2VncPort)), anyBoolean());
        verify(agentManager).pullAgentToMaintenance(hostId);
    }
}
