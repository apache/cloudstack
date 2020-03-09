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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolHostDao;
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

    private long hostMockId = 1L;
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

    @Mock
    private StoragePoolHostDao storagePoolHostDaoMock;

    @Mock
    private StoragePoolAllocator storagePoolAllocatorMock;

    @Mock
    private DiskOfferingDao diskOfferingDaoMock;

    @Before
    public void setup() {
        virtualMachineManagerImpl.setHostAllocators(new ArrayList<>());

        when(vmInstanceMock.getId()).thenReturn(vmInstanceVoMockId);
        when(vmInstanceMock.getServiceOfferingId()).thenReturn(2L);
        when(vmInstanceMock.getInstanceName()).thenReturn("myVm");
        when(vmInstanceMock.getHostId()).thenReturn(2L);
        when(vmInstanceMock.getType()).thenReturn(VirtualMachine.Type.User);
        when(hostMock.getId()).thenReturn(hostMockId);

        Mockito.doReturn(vmInstanceVoMockId).when(virtualMachineProfileMock).getId();

        Mockito.doReturn(storagePoolVoMockId).when(storagePoolVoMock).getId();
        Mockito.doReturn(storagePoolVoMockClusterId).when(storagePoolVoMock).getClusterId();

        Mockito.doReturn(volumeMockId).when(volumeVoMock).getId();
        Mockito.doReturn(storagePoolVoMockId).when(volumeVoMock).getPoolId();

        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(volumeMockId);
        Mockito.doReturn(storagePoolVoMock).when(storagePoolDaoMock).findById(storagePoolVoMockId);

        ArrayList<StoragePoolAllocator> storagePoolAllocators = new ArrayList<>();
        storagePoolAllocators.add(storagePoolAllocatorMock);
        virtualMachineManagerImpl.setStoragePoolAllocators(storagePoolAllocators);
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

        assertFalse(actual);
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

        assertFalse(actual);
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
        when(mockCurrentServiceOffering.isUseLocalStorage()).thenReturn(true);
        when(serviceOfferingMock.isUseLocalStorage()).thenReturn(true);
        when(mockCurrentServiceOffering.isSystemUse()).thenReturn(true);
        when(serviceOfferingMock.isSystemUse()).thenReturn(true);
        when(mockCurrentServiceOffering.getTags()).thenReturn("x,y");
        when(serviceOfferingMock.getTags()).thenReturn("z,x,y");

        virtualMachineManagerImpl.checkIfCanUpgrade(vmInstanceMock, serviceOfferingMock);
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

        assertFalse(returnedValue);
    }

    @Test
    public void isStorageCrossClusterMigrationTestStorageTypeEqualsZone() {
        Mockito.doReturn(1L).when(hostMock).getClusterId();
        Mockito.doReturn(2L).when(storagePoolVoMock).getClusterId();
        Mockito.doReturn(ScopeType.ZONE).when(storagePoolVoMock).getScope();

        boolean returnedValue = virtualMachineManagerImpl.isStorageCrossClusterMigration(hostMock, storagePoolVoMock);

        assertFalse(returnedValue);
    }

    @Test
    public void executeManagedStorageChecksWhenTargetStoragePoolProvidedTestCurrentStoragePoolNotManaged() {
        Mockito.doReturn(false).when(storagePoolVoMock).isManaged();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolProvided(storagePoolVoMock, volumeVoMock, Mockito.mock(StoragePoolVO.class));

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolVoMock, Mockito.times(0)).getId();
    }

    @Test
    public void executeManagedStorageChecksWhenTargetStoragePoolProvidedTestCurrentStoragePoolEqualsTargetPool() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolProvided(storagePoolVoMock, volumeVoMock, storagePoolVoMock);

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolVoMock, Mockito.times(2)).getId();
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeManagedStorageChecksWhenTargetStoragePoolProvidedTestCurrentStoragePoolNotEqualsTargetPool() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolProvided(storagePoolVoMock, volumeVoMock, Mockito.mock(StoragePoolVO.class));
    }

    @Test
    public void buildMapUsingUserInformationTestUserDefinedMigrationMapEmpty() {
        HashMap<Long, Long> userDefinedVolumeToStoragePoolMap = Mockito.spy(new HashMap<>());

        Map<Volume, StoragePool> volumeToPoolObjectMap = virtualMachineManagerImpl.buildMapUsingUserInformation(virtualMachineProfileMock, hostMock, userDefinedVolumeToStoragePoolMap);

        Assert.assertTrue(volumeToPoolObjectMap.isEmpty());

        Mockito.verify(userDefinedVolumeToStoragePoolMap, times(0)).keySet();
    }

    @Test(expected = CloudRuntimeException.class)
    public void buildMapUsingUserInformationTestTargetHostDoesNotHaveAccessToPool() {
        HashMap<Long, Long> userDefinedVolumeToStoragePoolMap = new HashMap<>();
        userDefinedVolumeToStoragePoolMap.put(volumeMockId, storagePoolVoMockId);

        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolProvided(Mockito.any(StoragePoolVO.class), Mockito.any(VolumeVO.class), Mockito.any(StoragePoolVO.class));
        Mockito.doReturn(null).when(storagePoolHostDaoMock).findByPoolHost(storagePoolVoMockId, hostMockId);

        virtualMachineManagerImpl.buildMapUsingUserInformation(virtualMachineProfileMock, hostMock, userDefinedVolumeToStoragePoolMap);

    }

    @Test
    public void buildMapUsingUserInformationTestTargetHostHasAccessToPool() {
        HashMap<Long, Long> userDefinedVolumeToStoragePoolMap = Mockito.spy(new HashMap<>());
        userDefinedVolumeToStoragePoolMap.put(volumeMockId, storagePoolVoMockId);

        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolProvided(Mockito.any(StoragePoolVO.class), Mockito.any(VolumeVO.class),
                Mockito.any(StoragePoolVO.class));
        Mockito.doReturn(Mockito.mock(StoragePoolHostVO.class)).when(storagePoolHostDaoMock).findByPoolHost(storagePoolVoMockId, hostMockId);

        Map<Volume, StoragePool> volumeToPoolObjectMap = virtualMachineManagerImpl.buildMapUsingUserInformation(virtualMachineProfileMock, hostMock, userDefinedVolumeToStoragePoolMap);

        assertFalse(volumeToPoolObjectMap.isEmpty());
        Assert.assertEquals(storagePoolVoMock, volumeToPoolObjectMap.get(volumeVoMock));

        Mockito.verify(userDefinedVolumeToStoragePoolMap, times(1)).keySet();
    }

    @Test
    public void findVolumesThatWereNotMappedByTheUserTest() {
        Map<Volume, StoragePool> volumeToStoragePoolObjectMap = Mockito.spy(new HashMap<>());
        volumeToStoragePoolObjectMap.put(volumeVoMock, storagePoolVoMock);

        Volume volumeVoMock2 = Mockito.mock(Volume.class);

        List<Volume> volumesOfVm = new ArrayList<>();
        volumesOfVm.add(volumeVoMock);
        volumesOfVm.add(volumeVoMock2);

        Mockito.doReturn(volumesOfVm).when(volumeDaoMock).findUsableVolumesForInstance(vmInstanceVoMockId);
        List<Volume> volumesNotMapped = virtualMachineManagerImpl.findVolumesThatWereNotMappedByTheUser(virtualMachineProfileMock, volumeToStoragePoolObjectMap);

        Assert.assertEquals(1, volumesNotMapped.size());
        Assert.assertEquals(volumeVoMock2, volumesNotMapped.get(0));
    }

    @Test
    public void executeManagedStorageChecksWhenTargetStoragePoolNotProvidedTestCurrentStoragePoolNotManaged() {
        Mockito.doReturn(false).when(storagePoolVoMock).isManaged();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolHostDaoMock, Mockito.times(0)).findByPoolHost(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void executeManagedStorageChecksWhenTargetStoragePoolNotProvidedTestCurrentStoragePoolManagedIsConnectedToHost() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();
        Mockito.doReturn(Mockito.mock(StoragePoolHostVO.class)).when(storagePoolHostDaoMock).findByPoolHost(storagePoolVoMockId, hostMockId);

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolHostDaoMock, Mockito.times(1)).findByPoolHost(storagePoolVoMockId, hostMockId);
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeManagedStorageChecksWhenTargetStoragePoolNotProvidedTestCurrentStoragePoolManagedIsNotConnectedToHost() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();
        Mockito.doReturn(null).when(storagePoolHostDaoMock).findByPoolHost(storagePoolVoMockId, hostMockId);

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
    }

    @Test
    public void getCandidateStoragePoolsToMigrateLocalVolumeTestLocalVolume() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(Mockito.anyLong());

        Mockito.doReturn(true).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, hostMock, volumeVoMock);

        Assert.assertEquals(1, poolList.size());
        Assert.assertEquals(storagePoolVoMock, poolList.get(0));
    }

    @Test
    public void getCandidateStoragePoolsToMigrateLocalVolumeTestCrossClusterMigration() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(Mockito.anyLong());

        Mockito.doReturn(false).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(true).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(hostMock, storagePoolVoMock);
        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, hostMock, volumeVoMock);

        Assert.assertEquals(1, poolList.size());
        Assert.assertEquals(storagePoolVoMock, poolList.get(0));
    }

    @Test
    public void getCandidateStoragePoolsToMigrateLocalVolumeTestWithinClusterMigration() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(Mockito.anyLong());

        Mockito.doReturn(false).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(false).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(hostMock, storagePoolVoMock);
        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, hostMock, volumeVoMock);

        Assert.assertTrue(poolList.isEmpty());
    }

    @Test
    public void getCandidateStoragePoolsToMigrateLocalVolumeTestMoreThanOneAllocator() {
        StoragePoolAllocator storagePoolAllocatorMock2 = Mockito.mock(StoragePoolAllocator.class);
        StoragePoolAllocator storagePoolAllocatorMock3 = Mockito.mock(StoragePoolAllocator.class);

        List<StoragePoolAllocator> storagePoolAllocatorsMock = new ArrayList<>();
        storagePoolAllocatorsMock.add(storagePoolAllocatorMock);
        storagePoolAllocatorsMock.add(storagePoolAllocatorMock2);
        storagePoolAllocatorsMock.add(storagePoolAllocatorMock3);

        virtualMachineManagerImpl.setStoragePoolAllocators(storagePoolAllocatorsMock);

        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(Mockito.anyLong());

        Mockito.doReturn(false).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(null).when(storagePoolAllocatorMock2).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(new ArrayList<>()).when(storagePoolAllocatorMock3).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(false).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(hostMock, storagePoolVoMock);
        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, hostMock, volumeVoMock);

        Assert.assertTrue(poolList.isEmpty());

        Mockito.verify(storagePoolAllocatorMock).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));
        Mockito.verify(storagePoolAllocatorMock2).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));
        Mockito.verify(storagePoolAllocatorMock3).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));
    }

    @Test(expected = CloudRuntimeException.class)
    public void createVolumeToStoragePoolMappingIfPossibleTestNotStoragePoolsAvailable() {
        Mockito.doReturn(null).when(virtualMachineManagerImpl).getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, hostMock, volumeVoMock);

        virtualMachineManagerImpl.createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, new HashMap<>(), volumeVoMock, storagePoolVoMock);
    }

    @Test
    public void createVolumeToStoragePoolMappingIfPossibleTestTargetHostAccessCurrentStoragePool() {
        List<StoragePool> storagePoolList = new ArrayList<>();
        storagePoolList.add(storagePoolVoMock);

        Mockito.doReturn(storagePoolList).when(virtualMachineManagerImpl).getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, hostMock, volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();
        virtualMachineManagerImpl.createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);

        Assert.assertTrue(volumeToPoolObjectMap.isEmpty());
    }

    @Test
    public void createVolumeToStoragePoolMappingIfPossibleTestTargetHostDoesNotAccessCurrentStoragePool() {
        StoragePoolVO storagePoolMockOther = Mockito.mock(StoragePoolVO.class);
        String storagePoolMockOtherUuid = "storagePoolMockOtherUuid";
        Mockito.doReturn(storagePoolMockOtherUuid).when(storagePoolMockOther).getUuid();
        Mockito.doReturn(storagePoolMockOther).when(storagePoolDaoMock).findByUuid(storagePoolMockOtherUuid);

        List<StoragePool> storagePoolList = new ArrayList<>();
        storagePoolList.add(storagePoolMockOther);

        Mockito.doReturn(storagePoolList).when(virtualMachineManagerImpl).getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, hostMock, volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();
        virtualMachineManagerImpl.createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);

        assertFalse(volumeToPoolObjectMap.isEmpty());
        Assert.assertEquals(storagePoolMockOther, volumeToPoolObjectMap.get(volumeVoMock));
    }

    @Test
    public void createStoragePoolMappingsForVolumesTestLocalStoragevolume() {
        ArrayList<Volume> allVolumes = new ArrayList<>();
        allVolumes.add(volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();

        Mockito.doReturn(ScopeType.HOST).when(storagePoolVoMock).getScope();
        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.doNothing().when(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumeVoMock,
                storagePoolVoMock);

        virtualMachineManagerImpl.createStoragePoolMappingsForVolumes(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, allVolumes);

        Assert.assertTrue(volumeToPoolObjectMap.isEmpty());
        Mockito.verify(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.verify(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);
    }

    @Test
    public void createStoragePoolMappingsForVolumesTestCrossCluterMigration() {
        ArrayList<Volume> allVolumes = new ArrayList<>();
        allVolumes.add(volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();

        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();
        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.doNothing().when(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);
        Mockito.doReturn(true).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(hostMock, storagePoolVoMock);

        virtualMachineManagerImpl.createStoragePoolMappingsForVolumes(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, allVolumes);

        Assert.assertTrue(volumeToPoolObjectMap.isEmpty());
        Mockito.verify(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.verify(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);
        Mockito.verify(virtualMachineManagerImpl).isStorageCrossClusterMigration(hostMock, storagePoolVoMock);
    }

    @Test
    public void createStoragePoolMappingsForVolumesTestNotCrossCluterMigrationWithClusterStorage() {
        ArrayList<Volume> allVolumes = new ArrayList<>();
        allVolumes.add(volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();

        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();
        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.doNothing().when(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);
        Mockito.doReturn(false).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(hostMock, storagePoolVoMock);

        virtualMachineManagerImpl.createStoragePoolMappingsForVolumes(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, allVolumes);

        assertFalse(volumeToPoolObjectMap.isEmpty());
        Assert.assertEquals(storagePoolVoMock, volumeToPoolObjectMap.get(volumeVoMock));

        Mockito.verify(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.verify(virtualMachineManagerImpl).isStorageCrossClusterMigration(hostMock, storagePoolVoMock);
        Mockito.verify(virtualMachineManagerImpl, Mockito.times(0)).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumeVoMock,
                storagePoolVoMock);
    }

    @Test
    public void createMappingVolumeAndStoragePoolTest() {
        Map<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();
        List<Volume> volumesNotMapped = new ArrayList<>();

        Mockito.doReturn(volumeToPoolObjectMap).when(virtualMachineManagerImpl).buildMapUsingUserInformation(Mockito.eq(virtualMachineProfileMock), Mockito.eq(hostMock),
                Mockito.anyMapOf(Long.class, Long.class));

        Mockito.doReturn(volumesNotMapped).when(virtualMachineManagerImpl).findVolumesThatWereNotMappedByTheUser(virtualMachineProfileMock, volumeToPoolObjectMap);
        Mockito.doNothing().when(virtualMachineManagerImpl).createStoragePoolMappingsForVolumes(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumesNotMapped);

        Map<Volume, StoragePool> mappingVolumeAndStoragePool = virtualMachineManagerImpl.createMappingVolumeAndStoragePool(virtualMachineProfileMock, hostMock, new HashMap<>());

        Assert.assertEquals(mappingVolumeAndStoragePool, volumeToPoolObjectMap);

        InOrder inOrder = Mockito.inOrder(virtualMachineManagerImpl);
        inOrder.verify(virtualMachineManagerImpl).buildMapUsingUserInformation(Mockito.eq(virtualMachineProfileMock), Mockito.eq(hostMock), Mockito.anyMapOf(Long.class, Long.class));
        inOrder.verify(virtualMachineManagerImpl).findVolumesThatWereNotMappedByTheUser(virtualMachineProfileMock, volumeToPoolObjectMap);
        inOrder.verify(virtualMachineManagerImpl).createStoragePoolMappingsForVolumes(virtualMachineProfileMock, hostMock, volumeToPoolObjectMap, volumesNotMapped);
    }

    @Test
    public void matchesOfSorts() {
        List<String> nothing = null;
        List<String> empty = new ArrayList<>();
        List<String> tag = Arrays.asList("bla");
        List<String> tags = Arrays.asList("bla", "blob");
        List<String> others = Arrays.asList("bla", "blieb");
        List<String> three = Arrays.asList("bla", "blob", "blieb");

        // single match
        assertTrue(VirtualMachineManagerImpl.matches(tag,tags));
        assertTrue(VirtualMachineManagerImpl.matches(tag,others));

        // no requirements
        assertTrue(VirtualMachineManagerImpl.matches(nothing,tags));
        assertTrue(VirtualMachineManagerImpl.matches(empty,tag));

        // mis(sing)match
        assertFalse(VirtualMachineManagerImpl.matches(tags,tag));
        assertFalse(VirtualMachineManagerImpl.matches(tag,nothing));
        assertFalse(VirtualMachineManagerImpl.matches(tag,empty));

        // disjunct sets
        assertFalse(VirtualMachineManagerImpl.matches(tags,others));
        assertFalse(VirtualMachineManagerImpl.matches(others,tags));

        // everything matches the larger set
        assertTrue(VirtualMachineManagerImpl.matches(nothing,three));
        assertTrue(VirtualMachineManagerImpl.matches(empty,three));
        assertTrue(VirtualMachineManagerImpl.matches(tag,three));
        assertTrue(VirtualMachineManagerImpl.matches(tags,three));
        assertTrue(VirtualMachineManagerImpl.matches(others,three));
    }
}
