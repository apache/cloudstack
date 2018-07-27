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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.ScopeType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineManagerImplTest {

    @Spy
    @InjectMocks
    private VirtualMachineManagerImpl virtualMachineManagerImpl;
    @Mock
    private AgentManager agentManagerMock;
    @Mock
    private VMInstanceDao vmInstanceDaoMock;
    @Mock
    private ServiceOfferingDao serviceOfferingDaoMock;
    @Mock
    private VolumeDao volumeDaoMock;
    @Mock
    private PrimaryDataStoreDao storagePoolDaoMock;
    @Mock
    private VMInstanceVO vmInstanceMock;
    private long vmInstanceVoMockId = 1L;

    @Mock
    private ServiceOfferingVO serviceOfferingMock;
    @Mock
    private HostVO hostMock;
    @Mock
    private VirtualMachineProfile virtualMachineProfileMock;
    @Mock
    private StoragePoolVO storagePoolVoMock;
    private long storagePoolVoMockId = 11L;
    private long storagePoolVoMockClusterId = 234L;

    @Mock
    private VolumeVO volumeVoMock;
    private long volumeMockId = 1111L;

    @Before
    public void setup() {
        virtualMachineManagerImpl.setHostAllocators(new ArrayList<>());

        when(vmInstanceMock.getId()).thenReturn(vmInstanceVoMockId);
        when(vmInstanceMock.getServiceOfferingId()).thenReturn(2L);
        when(vmInstanceMock.getInstanceName()).thenReturn("myVm");
        when(vmInstanceMock.getHostId()).thenReturn(2L);
        when(vmInstanceMock.getType()).thenReturn(VirtualMachine.Type.User);
        when(hostMock.getId()).thenReturn(1L);

        Mockito.doReturn(vmInstanceVoMockId).when(virtualMachineProfileMock).getId();

        Mockito.doReturn(storagePoolVoMockId).when(storagePoolVoMock).getId();
        Mockito.doReturn(storagePoolVoMockClusterId).when(storagePoolVoMock).getClusterId();

        Mockito.doReturn(volumeMockId).when(volumeVoMock).getId();
        Mockito.doReturn(storagePoolVoMockId).when(volumeVoMock).getPoolId();
    }

    @Test(expected = CloudRuntimeException.class)
    public void testScaleVM3() throws Exception {
        when(vmInstanceMock.getHostId()).thenReturn(null);
        when(vmInstanceDaoMock.findById(anyLong())).thenReturn(vmInstanceMock);
        when(vmInstanceDaoMock.findByUuid(any(String.class))).thenReturn(vmInstanceMock);
        DeploymentPlanner.ExcludeList excludeHostList = new DeploymentPlanner.ExcludeList();
        virtualMachineManagerImpl.findHostAndMigrate(vmInstanceMock.getUuid(), 2l, excludeHostList);
    }

    @Test
    public void testSendStopWithOkAnswer() throws Exception {
        VirtualMachineGuru guru = mock(VirtualMachineGuru.class);
        VirtualMachine vm = mock(VirtualMachine.class);
        VirtualMachineProfile profile = mock(VirtualMachineProfile.class);
        StopAnswer answer = new StopAnswer(new StopCommand(vm, false, false), "ok", true);
        when(profile.getVirtualMachine()).thenReturn(vm);
        when(vm.getHostId()).thenReturn(1L);
        when(agentManagerMock.send(anyLong(), (Command)any())).thenReturn(answer);

        boolean actual = virtualMachineManagerImpl.sendStop(guru, profile, false, false);

        Assert.assertTrue(actual);
    }

    @Test
    public void testSendStopWithFailAnswer() throws Exception {
        VirtualMachineGuru guru = mock(VirtualMachineGuru.class);
        VirtualMachine vm = mock(VirtualMachine.class);
        VirtualMachineProfile profile = mock(VirtualMachineProfile.class);
        StopAnswer answer = new StopAnswer(new StopCommand(vm, false, false), "fail", false);
        when(profile.getVirtualMachine()).thenReturn(vm);
        when(vm.getHostId()).thenReturn(1L);
        when(agentManagerMock.send(anyLong(), (Command)any())).thenReturn(answer);

        boolean actual = virtualMachineManagerImpl.sendStop(guru, profile, false, false);

        Assert.assertFalse(actual);
    }

    @Test
    public void testSendStopWithNullAnswer() throws Exception {
        VirtualMachineGuru guru = mock(VirtualMachineGuru.class);
        VirtualMachine vm = mock(VirtualMachine.class);
        VirtualMachineProfile profile = mock(VirtualMachineProfile.class);
        when(profile.getVirtualMachine()).thenReturn(vm);
        when(vm.getHostId()).thenReturn(1L);
        when(agentManagerMock.send(anyLong(), (Command)any())).thenReturn(null);

        boolean actual = virtualMachineManagerImpl.sendStop(guru, profile, false, false);

        Assert.assertFalse(actual);
    }

    @Test
    public void testExeceuteInSequence() {
        assertTrue(virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.XenServer) == false);
        assertTrue(virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.KVM) == false);
        assertTrue(virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.VMware) == HypervisorGuru.VmwareFullClone.value());
        assertTrue(virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.Ovm3) == VirtualMachineManager.ExecuteInSequence.value());
    }

    @Test
    public void testCheckIfCanUpgrade() throws Exception {
        when(vmInstanceMock.getState()).thenReturn(State.Stopped);
        when(serviceOfferingMock.isDynamic()).thenReturn(true);
        when(vmInstanceMock.getServiceOfferingId()).thenReturn(1l);
        when(serviceOfferingMock.getId()).thenReturn(2l);

        ServiceOfferingVO mockCurrentServiceOffering = mock(ServiceOfferingVO.class);

        when(serviceOfferingDaoMock.findByIdIncludingRemoved(anyLong(), anyLong())).thenReturn(mockCurrentServiceOffering);
        when(mockCurrentServiceOffering.getUseLocalStorage()).thenReturn(true);
        when(serviceOfferingMock.getUseLocalStorage()).thenReturn(true);
        when(mockCurrentServiceOffering.getSystemUse()).thenReturn(true);
        when(serviceOfferingMock.getSystemUse()).thenReturn(true);
        when(mockCurrentServiceOffering.getTags()).thenReturn("x,y");
        when(serviceOfferingMock.getTags()).thenReturn("z,x,y");

        virtualMachineManagerImpl.checkIfCanUpgrade(vmInstanceMock, serviceOfferingMock);
    }

    @Test
    public void executeManagedStorageChecksWhenTargetStoragePoolNotProvidedTestNonManagedStorage() {
        Mockito.doReturn(false).when(storagePoolVoMock).isManaged();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolVoMock, Mockito.times(0)).getClusterId();
        Mockito.verify(storagePoolVoMock, Mockito.times(0)).getScope();
    }

    @Test
    public void executeManagedStorageChecksWhenTargetStoragePoolNotProvidedTestManagedStorageAndSameClusterAsTargetHost() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();
        Mockito.doReturn(1L).when(hostMock).getClusterId();
        Mockito.doReturn(1L).when(storagePoolVoMock).getClusterId();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolVoMock, Mockito.times(1)).getClusterId();
        Mockito.verify(storagePoolVoMock, Mockito.times(1)).getScope();
    }

    @Test
    public void executeManagedStorageChecksWhenTargetStoragePoolNotProvidedTestManagedStorageAndDifferentClusterAsTargetHostWithZoneWideStorage() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();
        Mockito.doReturn(1L).when(hostMock).getClusterId();
        Mockito.doReturn(2L).when(storagePoolVoMock).getClusterId();
        Mockito.doReturn(ScopeType.ZONE).when(storagePoolVoMock).getScope();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolVoMock, Mockito.times(0)).getClusterId();
        Mockito.verify(storagePoolVoMock, Mockito.times(1)).getScope();
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeManagedStorageChecksWhenTargetStoragePoolNotProvidedTestManagedStorageAndDifferentClusterAsTargetHostWithClusterWideStorage() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();
        Mockito.doReturn(1L).when(hostMock).getClusterId();
        Mockito.doReturn(2L).when(storagePoolVoMock).getClusterId();
        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
    }

    @Test
    public void isStorageCrossClusterMigrationTestStorageTypeEqualsCluster() {
        Mockito.doReturn(1L).when(hostMock).getClusterId();
        Mockito.doReturn(2L).when(storagePoolVoMock).getClusterId();
        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();

        boolean returnedValue = virtualMachineManagerImpl.isStorageCrossClusterMigration(hostMock, storagePoolVoMock);

        Assert.assertTrue(returnedValue);
    }

    @Test
    public void isStorageCrossClusterMigrationTestStorageSameCluster() {
        Mockito.doReturn(1L).when(hostMock).getClusterId();
        Mockito.doReturn(1L).when(storagePoolVoMock).getClusterId();
        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();

        boolean returnedValue = virtualMachineManagerImpl.isStorageCrossClusterMigration(hostMock, storagePoolVoMock);

        Assert.assertFalse(returnedValue);
    }

    @Test
    public void isStorageCrossClusterMigrationTestStorageTypeEqualsZone() {
        Mockito.doReturn(1L).when(hostMock).getClusterId();
        Mockito.doReturn(2L).when(storagePoolVoMock).getClusterId();
        Mockito.doReturn(ScopeType.ZONE).when(storagePoolVoMock).getScope();

        boolean returnedValue = virtualMachineManagerImpl.isStorageCrossClusterMigration(hostMock, storagePoolVoMock);

        Assert.assertFalse(returnedValue);
    }
}