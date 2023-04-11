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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.config.ConfigKey;
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
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
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
    private DiskOfferingVO diskOfferingMock;

    private long hostMockId = 1L;
    private long clusterMockId = 2L;
    private long zoneMockId = 3L;
    @Mock
    private HostVO hostMock;
    @Mock
    private DataCenterDeployment dataCenterDeploymentMock;

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
    @Mock
    VMTemplateDao templateDao;
    @Mock
    VMTemplateZoneDao templateZoneDao;

    @Mock
    private HostDao hostDaoMock;
    @Mock
    private UserVmJoinDao userVmJoinDaoMock;
    @Mock
    private UserVmDao userVmDaoMock;
    @Mock
    private UserVmVO userVmMock;

    @Before
    public void setup() {
        virtualMachineManagerImpl.setHostAllocators(new ArrayList<>());

        when(vmInstanceMock.getId()).thenReturn(vmInstanceVoMockId);
        when(vmInstanceMock.getServiceOfferingId()).thenReturn(2L);
        when(hostMock.getId()).thenReturn(hostMockId);
        when(dataCenterDeploymentMock.getHostId()).thenReturn(hostMockId);
        when(dataCenterDeploymentMock.getClusterId()).thenReturn(clusterMockId);

        when(hostMock.getDataCenterId()).thenReturn(zoneMockId);
        when(hostDaoMock.findById(any())).thenReturn(hostMock);

        when(userVmJoinDaoMock.searchByIds(any())).thenReturn(new ArrayList<>());
        when(userVmDaoMock.findById(any())).thenReturn(userVmMock);

        Mockito.doReturn(vmInstanceVoMockId).when(virtualMachineProfileMock).getId();

        Mockito.doReturn(storagePoolVoMockId).when(storagePoolVoMock).getId();

        Mockito.doReturn(volumeMockId).when(volumeVoMock).getId();
        Mockito.doReturn(storagePoolVoMockId).when(volumeVoMock).getPoolId();

        Mockito.doReturn(volumeVoMock).when(volumeDaoMock).findById(volumeMockId);
        Mockito.doReturn(storagePoolVoMock).when(storagePoolDaoMock).findById(storagePoolVoMockId);

        ArrayList<StoragePoolAllocator> storagePoolAllocators = new ArrayList<>();
        storagePoolAllocators.add(storagePoolAllocatorMock);
        virtualMachineManagerImpl.setStoragePoolAllocators(storagePoolAllocators);
    }

    @Test
    public void testaddHostIpToCertDetailsIfConfigAllows() {
        Host vmHost = mock(Host.class);
        ConfigKey testConfig = mock(ConfigKey.class);

        Long dataCenterId = 5L;
        String hostIp = "1.1.1.1";
        String routerIp = "2.2.2.2";
        Map<String, String> ipAddresses = new HashMap<>();
        ipAddresses.put(NetworkElementCommand.ROUTER_IP, routerIp);

        when(testConfig.valueIn(dataCenterId)).thenReturn(true);
        when(vmHost.getDataCenterId()).thenReturn(dataCenterId);
        when(vmHost.getPrivateIpAddress()).thenReturn(hostIp);

        virtualMachineManagerImpl.addHostIpToCertDetailsIfConfigAllows(vmHost, ipAddresses, testConfig);
        assertTrue(ipAddresses.containsKey(NetworkElementCommand.HYPERVISOR_HOST_PRIVATE_IP));
        assertEquals(hostIp, ipAddresses.get(NetworkElementCommand.HYPERVISOR_HOST_PRIVATE_IP));
        assertTrue(ipAddresses.containsKey(NetworkElementCommand.ROUTER_IP));
        assertEquals(routerIp, ipAddresses.get(NetworkElementCommand.ROUTER_IP));
    }

    @Test
    public void testaddHostIpToCertDetailsIfConfigAllowsWhenConfigFalse() {
        Host vmHost = mock(Host.class);
        ConfigKey testConfig = mock(ConfigKey.class);

        Long dataCenterId = 5L;
        String hostIp = "1.1.1.1";
        String routerIp = "2.2.2.2";
        Map<String, String> ipAddresses = new HashMap<>();
        ipAddresses.put(NetworkElementCommand.ROUTER_IP, routerIp);

        when(testConfig.valueIn(dataCenterId)).thenReturn(false);
        when(vmHost.getDataCenterId()).thenReturn(dataCenterId);

        virtualMachineManagerImpl.addHostIpToCertDetailsIfConfigAllows(vmHost, ipAddresses, testConfig);
        assertFalse(ipAddresses.containsKey(NetworkElementCommand.HYPERVISOR_HOST_PRIVATE_IP));
        assertTrue(ipAddresses.containsKey(NetworkElementCommand.ROUTER_IP));
        assertEquals(routerIp, ipAddresses.get(NetworkElementCommand.ROUTER_IP));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testScaleVM3() throws Exception {
        DeploymentPlanner.ExcludeList excludeHostList = new DeploymentPlanner.ExcludeList();
        virtualMachineManagerImpl.findHostAndMigrate(vmInstanceMock.getUuid(), 2l, null, excludeHostList);
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
        assertTrue(virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.Ovm3) == VirtualMachineManager.ExecuteInSequence.value());
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String value) throws IllegalAccessException, NoSuchFieldException {
        final Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(configKey, value);
    }

    @Test
    public void testExeceuteInSequenceVmware() throws IllegalAccessException, NoSuchFieldException {
        overrideDefaultConfigValue(StorageManager.VmwareCreateCloneFull, "false");
        overrideDefaultConfigValue(StorageManager.VmwareAllowParallelExecution, "false");
        assertFalse("no full clones so no need to execute in sequence", virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.VMware));
        overrideDefaultConfigValue(StorageManager.VmwareCreateCloneFull, "true");
        overrideDefaultConfigValue(StorageManager.VmwareAllowParallelExecution, "false");
        assertTrue("full clones and no explicit parallel execution allowed, should execute in sequence", virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.VMware));
        overrideDefaultConfigValue(StorageManager.VmwareCreateCloneFull, "true");
        overrideDefaultConfigValue(StorageManager.VmwareAllowParallelExecution, "true");
        assertFalse("execute in sequence should not be needed as parallel is allowed", virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.VMware));
        overrideDefaultConfigValue(StorageManager.VmwareCreateCloneFull, "false");
        overrideDefaultConfigValue(StorageManager.VmwareAllowParallelExecution, "true");
        assertFalse("double reasons to allow parallel execution", virtualMachineManagerImpl.getExecuteInSequence(HypervisorType.VMware));
    }

    @Test
    public void testCheckIfCanUpgrade() throws Exception {
        when(vmInstanceMock.getState()).thenReturn(State.Stopped);
        when(serviceOfferingMock.isDynamic()).thenReturn(true);
        when(vmInstanceMock.getServiceOfferingId()).thenReturn(1l);

        ServiceOfferingVO mockCurrentServiceOffering = mock(ServiceOfferingVO.class);
        DiskOfferingVO mockCurrentDiskOffering = mock(DiskOfferingVO.class);

        when(serviceOfferingDaoMock.findByIdIncludingRemoved(anyLong(), anyLong())).thenReturn(mockCurrentServiceOffering);
        when(diskOfferingDaoMock.findByIdIncludingRemoved(anyLong())).thenReturn(mockCurrentDiskOffering);
        when(diskOfferingDaoMock.findById(anyLong())).thenReturn(diskOfferingMock);
        when(diskOfferingMock.isUseLocalStorage()).thenReturn(false);
        when(mockCurrentServiceOffering.isSystemUse()).thenReturn(true);
        when(serviceOfferingMock.isSystemUse()).thenReturn(true);
        String[] oldDOStorageTags = {"x","y"};
        String[] newDOStorageTags = {"z","x","y"};
        when(mockCurrentDiskOffering.getTagsArray()).thenReturn(oldDOStorageTags);
        when(diskOfferingMock.getTagsArray()).thenReturn(newDOStorageTags);

        virtualMachineManagerImpl.checkIfCanUpgrade(vmInstanceMock, serviceOfferingMock);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckIfCanUpgradeFail() {
        when(serviceOfferingMock.getState()).thenReturn(ServiceOffering.State.Inactive);

        virtualMachineManagerImpl.checkIfCanUpgrade(vmInstanceMock, serviceOfferingMock);
    }

    @Test
    public void isStorageCrossClusterMigrationTestStorageTypeEqualsCluster() {
        Mockito.doReturn(2L).when(storagePoolVoMock).getClusterId();
        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();

        boolean returnedValue = virtualMachineManagerImpl.isStorageCrossClusterMigration(1L, storagePoolVoMock);

        Assert.assertTrue(returnedValue);
    }

    @Test
    public void isStorageCrossClusterMigrationTestStorageSameCluster() {
        Mockito.doReturn(1L).when(storagePoolVoMock).getClusterId();
        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();

        boolean returnedValue = virtualMachineManagerImpl.isStorageCrossClusterMigration(1L, storagePoolVoMock);

        assertFalse(returnedValue);
    }

    @Test
    public void isStorageCrossClusterMigrationTestStorageTypeEqualsZone() {
        Mockito.doReturn(ScopeType.ZONE).when(storagePoolVoMock).getScope();

        boolean returnedValue = virtualMachineManagerImpl.isStorageCrossClusterMigration(1L, storagePoolVoMock);

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
        assertEquals(storagePoolVoMock, volumeToPoolObjectMap.get(volumeVoMock));

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

        assertEquals(1, volumesNotMapped.size());
        assertEquals(volumeVoMock2, volumesNotMapped.get(0));
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

        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

        assertEquals(1, poolList.size());
        assertEquals(storagePoolVoMock, poolList.get(0));
    }

    @Test
    public void getCandidateStoragePoolsToMigrateLocalVolumeTestCrossClusterMigration() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(Mockito.anyLong());

        Mockito.doReturn(false).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(true).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(clusterMockId, storagePoolVoMock);
        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

        assertEquals(1, poolList.size());
        assertEquals(storagePoolVoMock, poolList.get(0));
    }

    @Test
    public void getCandidateStoragePoolsToMigrateLocalVolumeTestWithinClusterMigration() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(Mockito.anyLong());

        Mockito.doReturn(false).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(Mockito.any(DiskProfile.class), Mockito.any(VirtualMachineProfile.class), Mockito.any(DeploymentPlan.class),
                Mockito.any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(false).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(clusterMockId, storagePoolVoMock);
        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

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

        Mockito.doReturn(false).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(clusterMockId, storagePoolVoMock);
        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

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
        Mockito.doReturn(null).when(virtualMachineManagerImpl).getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

        virtualMachineManagerImpl.createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, dataCenterDeploymentMock, new HashMap<>(), volumeVoMock, storagePoolVoMock);
    }

    @Test
    public void createVolumeToStoragePoolMappingIfPossibleTestTargetHostAccessCurrentStoragePool() {
        List<StoragePool> storagePoolList = new ArrayList<>();
        storagePoolList.add(storagePoolVoMock);

        Mockito.doReturn(storagePoolList).when(virtualMachineManagerImpl).getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();
        virtualMachineManagerImpl.createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);

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

        Mockito.doReturn(storagePoolList).when(virtualMachineManagerImpl).getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();
        virtualMachineManagerImpl.createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);

        assertFalse(volumeToPoolObjectMap.isEmpty());
        assertEquals(storagePoolMockOther, volumeToPoolObjectMap.get(volumeVoMock));
    }

    @Test
    public void createStoragePoolMappingsForVolumesTestLocalStoragevolume() {
        ArrayList<Volume> allVolumes = new ArrayList<>();
        allVolumes.add(volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();

        Mockito.doReturn(ScopeType.HOST).when(storagePoolVoMock).getScope();
        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.doNothing().when(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, volumeVoMock,
                storagePoolVoMock);

        virtualMachineManagerImpl.createStoragePoolMappingsForVolumes(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, allVolumes);

        Assert.assertTrue(volumeToPoolObjectMap.isEmpty());
        Mockito.verify(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.verify(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);
    }

    @Test
    public void createStoragePoolMappingsForVolumesTestCrossCluterMigration() {
        ArrayList<Volume> allVolumes = new ArrayList<>();
        allVolumes.add(volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();

        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();
        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.doNothing().when(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);
        Mockito.doReturn(true).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(clusterMockId, storagePoolVoMock);

        virtualMachineManagerImpl.createStoragePoolMappingsForVolumes(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, allVolumes);

        Assert.assertTrue(volumeToPoolObjectMap.isEmpty());
        Mockito.verify(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.verify(virtualMachineManagerImpl).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, volumeVoMock, storagePoolVoMock);
        Mockito.verify(virtualMachineManagerImpl).isStorageCrossClusterMigration(clusterMockId, storagePoolVoMock);
    }

    @Test
    public void createStoragePoolMappingsForVolumesTestNotCrossCluterMigrationWithClusterStorage() {
        ArrayList<Volume> allVolumes = new ArrayList<>();
        allVolumes.add(volumeVoMock);

        HashMap<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();

        Mockito.doReturn(ScopeType.CLUSTER).when(storagePoolVoMock).getScope();
        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(false).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(Mockito.anyLong(), Mockito.any());

        virtualMachineManagerImpl.createStoragePoolMappingsForVolumes(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, allVolumes);

        assertFalse(volumeToPoolObjectMap.isEmpty());
        assertEquals(storagePoolVoMock, volumeToPoolObjectMap.get(volumeVoMock));

        Mockito.verify(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(hostMock, storagePoolVoMock, volumeVoMock);
        Mockito.verify(virtualMachineManagerImpl).isStorageCrossClusterMigration(clusterMockId, storagePoolVoMock);
        Mockito.verify(virtualMachineManagerImpl, Mockito.times(0)).createVolumeToStoragePoolMappingIfPossible(virtualMachineProfileMock, dataCenterDeploymentMock, volumeToPoolObjectMap, volumeVoMock,
                storagePoolVoMock);
    }

    @Test
    public void createMappingVolumeAndStoragePoolTest() {
        Map<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();
        List<Volume> volumesNotMapped = new ArrayList<>();

        Mockito.doReturn(volumeToPoolObjectMap).when(virtualMachineManagerImpl).buildMapUsingUserInformation(Mockito.eq(virtualMachineProfileMock), Mockito.eq(hostMock),
                Mockito.anyMapOf(Long.class, Long.class));

        Mockito.doReturn(volumesNotMapped).when(virtualMachineManagerImpl).findVolumesThatWereNotMappedByTheUser(virtualMachineProfileMock, volumeToPoolObjectMap);
        Mockito.doNothing().when(virtualMachineManagerImpl).createStoragePoolMappingsForVolumes(Mockito.eq(virtualMachineProfileMock),
                Mockito.any(DataCenterDeployment.class), Mockito.eq(volumeToPoolObjectMap), Mockito.eq(volumesNotMapped));

        Map<Volume, StoragePool> mappingVolumeAndStoragePool = virtualMachineManagerImpl.createMappingVolumeAndStoragePool(virtualMachineProfileMock, hostMock, new HashMap<>());

        assertEquals(mappingVolumeAndStoragePool, volumeToPoolObjectMap);

        InOrder inOrder = Mockito.inOrder(virtualMachineManagerImpl);
        inOrder.verify(virtualMachineManagerImpl).buildMapUsingUserInformation(Mockito.eq(virtualMachineProfileMock), Mockito.eq(hostMock), Mockito.anyMapOf(Long.class, Long.class));
        inOrder.verify(virtualMachineManagerImpl).findVolumesThatWereNotMappedByTheUser(virtualMachineProfileMock, volumeToPoolObjectMap);
        inOrder.verify(virtualMachineManagerImpl).createStoragePoolMappingsForVolumes(Mockito.eq(virtualMachineProfileMock),
                Mockito.any(DataCenterDeployment.class), Mockito.eq(volumeToPoolObjectMap), Mockito.eq(volumesNotMapped));
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

    @Test
    public void isRootVolumeOnLocalStorageTestOnLocal() {
        prepareAndTestIsRootVolumeOnLocalStorage(ScopeType.HOST, true);
    }

    @Test
    public void isRootVolumeOnLocalStorageTestCluster() {
        prepareAndTestIsRootVolumeOnLocalStorage(ScopeType.CLUSTER, false);
    }

    @Test
    public void isRootVolumeOnLocalStorageTestZone() {
        prepareAndTestIsRootVolumeOnLocalStorage(ScopeType.ZONE, false);
    }

    private void prepareAndTestIsRootVolumeOnLocalStorage(ScopeType scope, boolean expected) {
        StoragePoolVO storagePoolVoMock = Mockito.mock(StoragePoolVO.class);
        Mockito.doReturn(storagePoolVoMock).when(storagePoolDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(scope).when(storagePoolVoMock).getScope();
        List<VolumeVO> mockedVolumes = new ArrayList<>();
        mockedVolumes.add(volumeVoMock);
        Mockito.doReturn(mockedVolumes).when(volumeDaoMock).findByInstanceAndType(Mockito.anyLong(), Mockito.any());

        boolean result = virtualMachineManagerImpl.isRootVolumeOnLocalStorage(0l);

        assertEquals(expected, result);
    }

    @Test
    public void checkIfNewOfferingStorageScopeMatchesStoragePoolTestLocalLocal() {
        prepareAndRunCheckIfNewOfferingStorageScopeMatchesStoragePool(true, true);
    }

    @Test
    public void checkIfNewOfferingStorageScopeMatchesStoragePoolTestSharedShared() {
        prepareAndRunCheckIfNewOfferingStorageScopeMatchesStoragePool(false, false);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void checkIfNewOfferingStorageScopeMatchesStoragePoolTestLocalShared() {
        prepareAndRunCheckIfNewOfferingStorageScopeMatchesStoragePool(true, false);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void checkIfNewOfferingStorageScopeMatchesStoragePoolTestSharedLocal() {
        prepareAndRunCheckIfNewOfferingStorageScopeMatchesStoragePool(false, true);
    }

    private void prepareAndRunCheckIfNewOfferingStorageScopeMatchesStoragePool(boolean isRootOnLocal, boolean isOfferingUsingLocal) {
        Mockito.doReturn(isRootOnLocal).when(virtualMachineManagerImpl).isRootVolumeOnLocalStorage(Mockito.anyLong());
        Mockito.doReturn("vmInstanceMockedToString").when(vmInstanceMock).toString();
        Mockito.doReturn(isOfferingUsingLocal).when(diskOfferingMock).isUseLocalStorage();
        virtualMachineManagerImpl.checkIfNewOfferingStorageScopeMatchesStoragePool(vmInstanceMock, diskOfferingMock);
    }

    @Test
    public void checkIfTemplateNeededForCreatingVmVolumesExistingRootVolumes() {
        long vmId = 1L;
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(volumeDaoMock.findReadyRootVolumesByInstance(vmId)).thenReturn(List.of(Mockito.mock(VolumeVO.class)));
        virtualMachineManagerImpl.checkIfTemplateNeededForCreatingVmVolumes(vm);
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfTemplateNeededForCreatingVmVolumesMissingTemplate() {
        long vmId = 1L;
        long templateId = 1L;
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getTemplateId()).thenReturn(templateId);
        Mockito.when(volumeDaoMock.findReadyRootVolumesByInstance(vmId)).thenReturn(null);
        Mockito.when(templateDao.findById(templateId)).thenReturn(null);
        virtualMachineManagerImpl.checkIfTemplateNeededForCreatingVmVolumes(vm);
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfTemplateNeededForCreatingVmVolumesMissingZoneTemplate() {
        long vmId = 1L;
        long templateId = 1L;
        long dcId = 1L;
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getTemplateId()).thenReturn(templateId);
        Mockito.when(vm.getDataCenterId()).thenReturn(dcId);
        Mockito.when(volumeDaoMock.findReadyRootVolumesByInstance(vmId)).thenReturn(null);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(vm.getId()).thenReturn(templateId);
        Mockito.when(templateDao.findById(templateId)).thenReturn(template);
        virtualMachineManagerImpl.checkIfTemplateNeededForCreatingVmVolumes(vm);
    }

    @Test
    public void checkIfTemplateNeededForCreatingVmVolumesTemplateAvailable() {
        long vmId = 1L;
        long templateId = 1L;
        long dcId = 1L;
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getTemplateId()).thenReturn(templateId);
        Mockito.when(vm.getDataCenterId()).thenReturn(dcId);
        Mockito.when(volumeDaoMock.findReadyRootVolumesByInstance(vmId)).thenReturn(new ArrayList<>());
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.getId()).thenReturn(templateId);
        Mockito.when(templateDao.findById(templateId)).thenReturn(template);
        Mockito.when(templateZoneDao.findByZoneTemplate(dcId, templateId)).thenReturn(Mockito.mock(VMTemplateZoneVO.class));
        virtualMachineManagerImpl.checkIfTemplateNeededForCreatingVmVolumes(vm);
    }
}
