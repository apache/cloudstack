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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.commons.collections.MapUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.api.query.dao.UserVmJoinDao;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
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
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Journal;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class VirtualMachineManagerImplTest {

    @Spy
    @InjectMocks
    private VirtualMachineManagerImpl virtualMachineManagerImpl = new VirtualMachineManagerImpl();
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
    @Mock
    private NetworkDao networkDao;
    @Mock
    private AccountDao accountDao;
    @Mock
    private DomainDao domainDao;
    @Mock
    private DataCenterDao dcDao;
    @Mock
    private VpcDao vpcDao;
    @Mock
    private EntityManager _entityMgr;
    @Mock
    private DeploymentPlanningManager _dpMgr;
    @Mock
    private HypervisorGuruManager _hvGuruMgr;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private ClusterDetailsDao _clusterDetailsDao;
    @Mock
    private UserVmDetailsDao userVmDetailsDao;
    @Mock
    private ItWorkDao _workDao;
    @Mock
    protected StateMachine2<State, VirtualMachine.Event, VirtualMachine> _stateMachine;

    private ConfigDepotImpl configDepotImpl;
    private boolean updatedConfigKeyDepot = false;

    @Before
    public void setup() {
        ReflectionTestUtils.getField(VirtualMachineManager.VmMetadataManufacturer, "s_depot");
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

    @After
    public void cleanup() {
        if (updatedConfigKeyDepot) {
            ReflectionTestUtils.setField(VirtualMachineManager.VmMetadataManufacturer, "s_depot", configDepotImpl);
        }
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
    public void allowVolumeMigrationsForPowerFlexStorage() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();
        Mockito.doReturn(Storage.StoragePoolType.PowerFlex).when(storagePoolVoMock).getPoolType();

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolProvided(storagePoolVoMock, volumeVoMock, Mockito.mock(StoragePoolVO.class));

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolVoMock, Mockito.times(0)).getId();
    }

    @Test
    public void executeManagedStorageChecksWhenTargetStoragePoolProvidedTestCurrentStoragePoolEqualsTargetPool() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();
        // return any storage type except powerflex/scaleio
        List<Storage.StoragePoolType> values = Arrays.asList(Storage.StoragePoolType.values());
        when(storagePoolVoMock.getPoolType()).thenAnswer((Answer<Storage.StoragePoolType>) invocation -> {
            List<Storage.StoragePoolType> filteredValues = values.stream().filter(v -> v != Storage.StoragePoolType.PowerFlex).collect(Collectors.toList());
            int randomIndex = new Random().nextInt(filteredValues.size());
            return filteredValues.get(randomIndex); });

        virtualMachineManagerImpl.executeManagedStorageChecksWhenTargetStoragePoolProvided(storagePoolVoMock, volumeVoMock, storagePoolVoMock);

        Mockito.verify(storagePoolVoMock).isManaged();
        Mockito.verify(storagePoolVoMock, Mockito.times(2)).getId();
    }

    @Test(expected = CloudRuntimeException.class)
    public void executeManagedStorageChecksWhenTargetStoragePoolProvidedTestCurrentStoragePoolNotEqualsTargetPool() {
        Mockito.doReturn(true).when(storagePoolVoMock).isManaged();
        // return any storage type except powerflex/scaleio
        List<Storage.StoragePoolType> values = Arrays.asList(Storage.StoragePoolType.values());
        when(storagePoolVoMock.getPoolType()).thenAnswer((Answer<Storage.StoragePoolType>) invocation -> {
            List<Storage.StoragePoolType> filteredValues = values.stream().filter(v -> v != Storage.StoragePoolType.PowerFlex).collect(Collectors.toList());
            int randomIndex = new Random().nextInt(filteredValues.size());
            return filteredValues.get(randomIndex); });

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

        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolProvided(any(StoragePoolVO.class), any(VolumeVO.class), any(StoragePoolVO.class));
        Mockito.doReturn(null).when(storagePoolHostDaoMock).findByPoolHost(storagePoolVoMockId, hostMockId);

        virtualMachineManagerImpl.buildMapUsingUserInformation(virtualMachineProfileMock, hostMock, userDefinedVolumeToStoragePoolMap);

    }

    @Test
    public void buildMapUsingUserInformationTestTargetHostHasAccessToPool() {
        HashMap<Long, Long> userDefinedVolumeToStoragePoolMap = Mockito.spy(new HashMap<>());
        userDefinedVolumeToStoragePoolMap.put(volumeMockId, storagePoolVoMockId);

        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolProvided(any(StoragePoolVO.class), any(VolumeVO.class),
                any(StoragePoolVO.class));
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
        Mockito.verify(storagePoolHostDaoMock, Mockito.times(0)).findByPoolHost(anyLong(), anyLong());
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
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(anyLong());

        Mockito.doReturn(true).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

        assertEquals(1, poolList.size());
        assertEquals(storagePoolVoMock, poolList.get(0));
    }

    @Test
    public void getCandidateStoragePoolsToMigrateLocalVolumeTestCrossClusterMigration() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(anyLong());

        Mockito.doReturn(false).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(true).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(clusterMockId, storagePoolVoMock);
        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

        assertEquals(1, poolList.size());
        assertEquals(storagePoolVoMock, poolList.get(0));
    }

    @Test
    public void getCandidateStoragePoolsToMigrateLocalVolumeTestWithinClusterMigration() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(anyLong());

        Mockito.doReturn(false).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

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

        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(anyLong());

        Mockito.doReturn(false).when(storagePoolVoMock).isLocal();

        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);

        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(null).when(storagePoolAllocatorMock2).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(new ArrayList<>()).when(storagePoolAllocatorMock3).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));

        Mockito.doReturn(false).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(clusterMockId, storagePoolVoMock);
        List<StoragePool> poolList = virtualMachineManagerImpl.getCandidateStoragePoolsToMigrateLocalVolume(virtualMachineProfileMock, dataCenterDeploymentMock, volumeVoMock);

        Assert.assertTrue(poolList.isEmpty());

        Mockito.verify(storagePoolAllocatorMock).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));
        Mockito.verify(storagePoolAllocatorMock2).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));
        Mockito.verify(storagePoolAllocatorMock3).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(StoragePoolAllocator.RETURN_UPTO_ALL));
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
        Mockito.doNothing().when(virtualMachineManagerImpl).executeManagedStorageChecksWhenTargetStoragePoolNotProvided(any(), any(), any());
        Mockito.doReturn(false).when(virtualMachineManagerImpl).isStorageCrossClusterMigration(anyLong(), any());

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
                Mockito.anyMap());

        Mockito.doReturn(volumesNotMapped).when(virtualMachineManagerImpl).findVolumesThatWereNotMappedByTheUser(virtualMachineProfileMock, volumeToPoolObjectMap);
        Mockito.doNothing().when(virtualMachineManagerImpl).createStoragePoolMappingsForVolumes(Mockito.eq(virtualMachineProfileMock),
                any(DataCenterDeployment.class), Mockito.eq(volumeToPoolObjectMap), Mockito.eq(volumesNotMapped));

        Map<Volume, StoragePool> mappingVolumeAndStoragePool = virtualMachineManagerImpl.createMappingVolumeAndStoragePool(virtualMachineProfileMock, hostMock, new HashMap<>());

        assertEquals(mappingVolumeAndStoragePool, volumeToPoolObjectMap);

        InOrder inOrder = Mockito.inOrder(virtualMachineManagerImpl);
        inOrder.verify(virtualMachineManagerImpl).buildMapUsingUserInformation(Mockito.eq(virtualMachineProfileMock), Mockito.eq(hostMock), Mockito.anyMap());
        inOrder.verify(virtualMachineManagerImpl).findVolumesThatWereNotMappedByTheUser(virtualMachineProfileMock, volumeToPoolObjectMap);
        inOrder.verify(virtualMachineManagerImpl).createStoragePoolMappingsForVolumes(Mockito.eq(virtualMachineProfileMock),
                any(DataCenterDeployment.class), Mockito.eq(volumeToPoolObjectMap), Mockito.eq(volumesNotMapped));
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
        Mockito.doReturn(storagePoolVoMock).when(storagePoolDaoMock).findById(anyLong());
        Mockito.doReturn(scope).when(storagePoolVoMock).getScope();
        List<VolumeVO> mockedVolumes = new ArrayList<>();
        mockedVolumes.add(volumeVoMock);
        Mockito.doReturn(mockedVolumes).when(volumeDaoMock).findByInstanceAndType(anyLong(), any());

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
        Mockito.doReturn(isRootOnLocal).when(virtualMachineManagerImpl).isRootVolumeOnLocalStorage(anyLong());
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

    @Test
    public void checkAndAttemptMigrateVmAcrossClusterNonValid() {
        // Below scenarios shouldn't result in VM migration

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        virtualMachineManagerImpl.checkAndAttemptMigrateVmAcrossCluster(vm, 1L, new HashMap<>());

        Mockito.when(vm.getHypervisorType()).thenReturn(HypervisorType.VMware);
        Mockito.when(vm.getLastHostId()).thenReturn(null);
        virtualMachineManagerImpl.checkAndAttemptMigrateVmAcrossCluster(vm, 1L, new HashMap<>());

        Long destinationClusterId = 10L;
        Mockito.when(vm.getLastHostId()).thenReturn(1L);
        HostVO hostVO = Mockito.mock(HostVO.class);
        Mockito.when(hostVO.getClusterId()).thenReturn(destinationClusterId);
        Mockito.when(hostDaoMock.findById(1L)).thenReturn(hostVO);
        virtualMachineManagerImpl.checkAndAttemptMigrateVmAcrossCluster(vm, destinationClusterId, new HashMap<>());

        destinationClusterId = 20L;
        Map<Volume, StoragePool> map = new HashMap<>();
        StoragePool pool1 = Mockito.mock(StoragePool.class);
        Mockito.when(pool1.getClusterId()).thenReturn(10L);
        map.put(Mockito.mock(Volume.class), pool1);
        StoragePool pool2 = Mockito.mock(StoragePool.class);
        Mockito.when(pool2.getClusterId()).thenReturn(null);
        map.put(Mockito.mock(Volume.class), pool2);
        virtualMachineManagerImpl.checkAndAttemptMigrateVmAcrossCluster(vm, destinationClusterId, map);
    }

    @Test
    public void checkIfVmNetworkDetailsReturnedIsCorrect() {
        VMInstanceVO vm = new VMInstanceVO(1L, 1L, "VM1", "i-2-2-VM",
                VirtualMachine.Type.User, 1L, HypervisorType.KVM, 1L, 1L, 1L,
                1L, false, false);

        VirtualMachineTO vmTO = new VirtualMachineTO() {
        };
        UserVmJoinVO userVm = new UserVmJoinVO();
        NetworkVO networkVO = mock(NetworkVO.class);
        AccountVO accountVO = mock(AccountVO.class);
        DomainVO domainVO = mock(DomainVO.class);
        domainVO.setName("testDomain");
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);
        VpcVO vpcVO = mock(VpcVO.class);

        networkVO.setAccountId(1L);
        networkVO.setName("testNet");
        networkVO.setVpcId(1L);

        accountVO.setAccountName("testAcc");

        vpcVO.setName("VPC1");


        List<UserVmJoinVO> userVms = List.of(userVm);
        Mockito.when(userVmJoinDaoMock.searchByIds(anyLong())).thenReturn(userVms);
        Mockito.when(networkDao.findById(anyLong())).thenReturn(networkVO);
        Mockito.when(accountDao.findById(anyLong())).thenReturn(accountVO);
        Mockito.when(domainDao.findById(anyLong())).thenReturn(domainVO);
        Mockito.when(dcDao.findById(anyLong())).thenReturn(dataCenterVO);
        Mockito.when(vpcDao.findById(anyLong())).thenReturn(vpcVO);
        Mockito.when(dataCenterVO.getId()).thenReturn(1L);
        when(accountVO.getId()).thenReturn(2L);
        Mockito.when(domainVO.getId()).thenReturn(3L);
        Mockito.when(vpcVO.getId()).thenReturn(4L);
        Mockito.when(networkVO.getId()).thenReturn(5L);
        virtualMachineManagerImpl.setVmNetworkDetails(vm, vmTO);
        assertEquals(1, vmTO.getNetworkIdToNetworkNameMap().size());
        assertEquals("D3-A2-Z1-V4-S5", vmTO.getNetworkIdToNetworkNameMap().get(5L));
    }

    @Test
    public void testOrchestrateStartNonNullPodId() throws Exception {
        VMInstanceVO vmInstance = new VMInstanceVO();
        ReflectionTestUtils.setField(vmInstance, "id", 1L);
        ReflectionTestUtils.setField(vmInstance, "accountId", 1L);
        ReflectionTestUtils.setField(vmInstance, "uuid", "vm-uuid");
        ReflectionTestUtils.setField(vmInstance, "serviceOfferingId", 2L);
        ReflectionTestUtils.setField(vmInstance, "instanceName", "myVm");
        ReflectionTestUtils.setField(vmInstance, "hostId", 2L);
        ReflectionTestUtils.setField(vmInstance, "type", VirtualMachine.Type.User);
        ReflectionTestUtils.setField(vmInstance, "dataCenterId", 1L);
        ReflectionTestUtils.setField(vmInstance, "hypervisorType", HypervisorType.KVM);

        VirtualMachineGuru vmGuru = mock(VirtualMachineGuru.class);

        User user = mock(User.class);

        Account account = mock(Account.class);
        Account owner = mock(Account.class);

        ReservationContext ctx = mock(ReservationContext.class);

        ItWorkVO work = mock(ItWorkVO.class);

        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);

        VirtualMachineTemplate template = mock(VirtualMachineTemplate.class);
        when(template.isDeployAsIs()).thenReturn(false);

        DataCenterDeployment plan = mock(DataCenterDeployment.class);
        when(plan.getDataCenterId()).thenReturn(1L);
        when(plan.getPodId()).thenReturn(1L);

        Map<VirtualMachineProfile.Param, Object> params = new HashMap<>();

        DeploymentPlanner planner = mock(DeploymentPlanner.class);

        when(vmInstanceDaoMock.findByUuid("vm-uuid")).thenReturn(vmInstance);

        doReturn(vmGuru).when(virtualMachineManagerImpl).getVmGuru(vmInstance);

        Ternary<VMInstanceVO, ReservationContext, ItWorkVO> start = new Ternary<>(vmInstance, ctx, work);
        Mockito.doReturn(start).when(virtualMachineManagerImpl).changeToStartState(vmGuru, vmInstance, user, account, owner, serviceOffering, template);

        when(ctx.getJournal()).thenReturn(Mockito.mock(Journal.class));

        when(serviceOfferingDaoMock.findById(vmInstance.getId(), vmInstance.getServiceOfferingId())).thenReturn(serviceOffering);

        when(_entityMgr.findById(Account.class, vmInstance.getAccountId())).thenReturn(owner);
        when(_entityMgr.findByIdIncludingRemoved(VirtualMachineTemplate.class, vmInstance.getTemplateId())).thenReturn(template);

        Host destHost = mock(Host.class);
        Pod destPod = mock(Pod.class);
        DeployDestination dest = mock(DeployDestination.class);
        when(dest.getHost()).thenReturn(destHost);
        when(dest.getPod()).thenReturn(destPod);
        when(dest.getCluster()).thenReturn(mock(Cluster.class));
        when(destHost.getId()).thenReturn(1L);
        when(destPod.getId()).thenReturn(2L);
        when(_dpMgr.planDeployment(any(VirtualMachineProfileImpl.class), any(DataCenterDeployment.class), any(ExcludeList.class), any(DeploymentPlanner.class))).thenReturn(dest);

        doNothing().when(virtualMachineManagerImpl).checkIfTemplateNeededForCreatingVmVolumes(vmInstance);

        when(_workDao.updateStep(any(), any())).thenReturn(true);
        when(_stateMachine.transitTo(vmInstance, VirtualMachine.Event.OperationRetry, new Pair(vmInstance.getHostId(), 1L), vmInstanceDaoMock)).thenThrow(new CloudRuntimeException("Error while transitioning"));
        when(_stateMachine.transitTo(vmInstance, VirtualMachine.Event.OperationFailed, new Pair(vmInstance.getHostId(), null), vmInstanceDaoMock)).thenReturn(true);


        Cluster cluster = mock(Cluster.class);
        when(dest.getCluster()).thenReturn(cluster);
        ClusterDetailsVO cluster_detail_cpu = mock(ClusterDetailsVO.class);
        ClusterDetailsVO cluster_detail_ram = mock(ClusterDetailsVO.class);
        when(cluster.getId()).thenReturn(1L);
        when(_clusterDetailsDao.findDetail(1L, VmDetailConstants.CPU_OVER_COMMIT_RATIO)).thenReturn(cluster_detail_cpu);
        when(_clusterDetailsDao.findDetail(1L, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO)).thenReturn(cluster_detail_ram);
        when(userVmDetailsDao.findDetail(anyLong(), Mockito.anyString())).thenReturn(null);
        when(cluster_detail_cpu.getValue()).thenReturn("1.0");
        when(cluster_detail_ram.getValue()).thenReturn("1.0");
        doReturn(false).when(virtualMachineManagerImpl).areAllVolumesAllocated(Mockito.anyLong());

        CallContext callContext = mock(CallContext.class);
        when(callContext.getCallingAccount()).thenReturn(account);
        when(callContext.getCallingUser()).thenReturn(user);
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            when(CallContext.current()).thenReturn(callContext);

            try {
                virtualMachineManagerImpl.orchestrateStart("vm-uuid", params, plan, planner);
            } catch (CloudRuntimeException e) {
                assertEquals(e.getMessage(), "Error while transitioning");
            }
        }

        assertEquals(vmInstance.getPodIdToDeployIn(), (Long) destPod.getId());
    }

    @Test
    public void testOrchestrateStartNullPodId() throws Exception {
        VMInstanceVO vmInstance = new VMInstanceVO();
        ReflectionTestUtils.setField(vmInstance, "id", 1L);
        ReflectionTestUtils.setField(vmInstance, "accountId", 1L);
        ReflectionTestUtils.setField(vmInstance, "uuid", "vm-uuid");
        ReflectionTestUtils.setField(vmInstance, "serviceOfferingId", 2L);
        ReflectionTestUtils.setField(vmInstance, "instanceName", "myVm");
        ReflectionTestUtils.setField(vmInstance, "hostId", 2L);
        ReflectionTestUtils.setField(vmInstance, "type", VirtualMachine.Type.User);
        ReflectionTestUtils.setField(vmInstance, "dataCenterId", 1L);
        ReflectionTestUtils.setField(vmInstance, "hypervisorType", HypervisorType.KVM);

        VirtualMachineGuru vmGuru = mock(VirtualMachineGuru.class);

        User user = mock(User.class);

        Account account = mock(Account.class);
        Account owner = mock(Account.class);

        ReservationContext ctx = mock(ReservationContext.class);

        ItWorkVO work = mock(ItWorkVO.class);

        ServiceOfferingVO serviceOffering = mock(ServiceOfferingVO.class);

        VirtualMachineTemplate template = mock(VirtualMachineTemplate.class);
        when(template.isDeployAsIs()).thenReturn(false);

        DataCenterDeployment plan = mock(DataCenterDeployment.class);
        when(plan.getDataCenterId()).thenReturn(1L);
        when(plan.getPodId()).thenReturn(1L);

        Map<VirtualMachineProfile.Param, Object> params = new HashMap<>();

        DeploymentPlanner planner = mock(DeploymentPlanner.class);

        when(vmInstanceDaoMock.findByUuid("vm-uuid")).thenReturn(vmInstance);

        doReturn(vmGuru).when(virtualMachineManagerImpl).getVmGuru(vmInstance);

        Ternary<VMInstanceVO, ReservationContext, ItWorkVO> start = new Ternary<>(vmInstance, ctx, work);
        Mockito.doReturn(start).when(virtualMachineManagerImpl).changeToStartState(vmGuru, vmInstance, user, account, owner, serviceOffering, template);

        when(ctx.getJournal()).thenReturn(Mockito.mock(Journal.class));

        when(serviceOfferingDaoMock.findById(vmInstance.getId(), vmInstance.getServiceOfferingId())).thenReturn(serviceOffering);

        when(_entityMgr.findById(Account.class, vmInstance.getAccountId())).thenReturn(owner);
        when(_entityMgr.findByIdIncludingRemoved(VirtualMachineTemplate.class, vmInstance.getTemplateId())).thenReturn(template);

        Host destHost = mock(Host.class);
        Pod destPod = mock(Pod.class);
        DeployDestination dest = mock(DeployDestination.class);
        when(dest.getHost()).thenReturn(destHost);
        when(dest.getPod()).thenReturn(destPod);
        when(dest.getCluster()).thenReturn(mock(Cluster.class));
        when(destHost.getId()).thenReturn(1L);
        when(destPod.getId()).thenReturn(2L);
        when(_dpMgr.planDeployment(any(VirtualMachineProfileImpl.class), any(DataCenterDeployment.class), any(ExcludeList.class), any(DeploymentPlanner.class))).thenReturn(dest);

        doNothing().when(virtualMachineManagerImpl).checkIfTemplateNeededForCreatingVmVolumes(vmInstance);

        when(_workDao.updateStep(any(), any())).thenReturn(true);
        when(_stateMachine.transitTo(vmInstance, VirtualMachine.Event.OperationRetry, new Pair(vmInstance.getHostId(), 1L), vmInstanceDaoMock)).thenThrow(new CloudRuntimeException("Error while transitioning"));
        when(_stateMachine.transitTo(vmInstance, VirtualMachine.Event.OperationFailed, new Pair(vmInstance.getHostId(), null), vmInstanceDaoMock)).thenReturn(true);


        Cluster cluster = mock(Cluster.class);
        when(dest.getCluster()).thenReturn(cluster);
        ClusterDetailsVO cluster_detail_cpu = mock(ClusterDetailsVO.class);
        ClusterDetailsVO cluster_detail_ram = mock(ClusterDetailsVO.class);
        when(cluster.getId()).thenReturn(1L);
        when(_clusterDetailsDao.findDetail(1L, VmDetailConstants.CPU_OVER_COMMIT_RATIO)).thenReturn(cluster_detail_cpu);
        when(_clusterDetailsDao.findDetail(1L, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO)).thenReturn(cluster_detail_ram);
        when(userVmDetailsDao.findDetail(anyLong(), Mockito.anyString())).thenReturn(null);
        when(cluster_detail_cpu.getValue()).thenReturn("1.0");
        when(cluster_detail_ram.getValue()).thenReturn("1.0");
        doReturn(true).when(virtualMachineManagerImpl).areAllVolumesAllocated(Mockito.anyLong());

        CallContext callContext = mock(CallContext.class);
        when(callContext.getCallingAccount()).thenReturn(account);
        when(callContext.getCallingUser()).thenReturn(user);
        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            when(CallContext.current()).thenReturn(callContext);

            try {
                virtualMachineManagerImpl.orchestrateStart("vm-uuid", params, plan, planner);
            } catch (CloudRuntimeException e) {
                assertEquals(e.getMessage(), "Error while transitioning");
            }
        }

        assertNull(vmInstance.getPodIdToDeployIn());
    }

    @Test
    public void testIsDiskOfferingSuitableForVmSuccess() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(anyLong());
        List<StoragePool> poolListMock = new ArrayList<>();
        poolListMock.add(storagePoolVoMock);
        Mockito.doReturn(poolListMock).when(storagePoolAllocatorMock).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(1));
        boolean result = virtualMachineManagerImpl.isDiskOfferingSuitableForVm(vmInstanceMock, virtualMachineProfileMock, 1L, 1L, 1L, 1L);
        assertTrue(result);
    }

    @Test
    public void testIsDiskOfferingSuitableForVmNegative() {
        Mockito.doReturn(Mockito.mock(DiskOfferingVO.class)).when(diskOfferingDaoMock).findById(anyLong());
        Mockito.doReturn(new ArrayList<>()).when(storagePoolAllocatorMock).allocateToPool(any(DiskProfile.class), any(VirtualMachineProfile.class), any(DeploymentPlan.class),
                any(ExcludeList.class), Mockito.eq(1));
        boolean result = virtualMachineManagerImpl.isDiskOfferingSuitableForVm(vmInstanceMock, virtualMachineProfileMock, 1L, 1L, 1L, 1L);
        assertFalse(result);
    }

    @Test
    public void testGetDiskOfferingSuitabilityForVm() {
        Mockito.doReturn(vmInstanceMock).when(vmInstanceDaoMock).findById(1L);
        Mockito.when(vmInstanceMock.getHostId()).thenReturn(1L);
        Mockito.doReturn(hostMock).when(hostDaoMock).findById(1L);
        Mockito.when(hostMock.getClusterId()).thenReturn(1L);
        ClusterVO cluster = Mockito.mock(ClusterVO.class);
        Mockito.when(cluster.getPodId()).thenReturn(1L);
        Mockito.doReturn(cluster).when(clusterDao).findById(1L);
        List<Long> diskOfferingIds = List.of(1L, 2L);
        Mockito.doReturn(false).when(virtualMachineManagerImpl)
                .isDiskOfferingSuitableForVm(eq(vmInstanceMock), any(VirtualMachineProfile.class),
                        eq(1L), eq(1L), eq(1L), eq(1L));
        Mockito.doReturn(true).when(virtualMachineManagerImpl)
                .isDiskOfferingSuitableForVm(eq(vmInstanceMock), any(VirtualMachineProfile.class),
                        eq(1L), eq(1L), eq(1L), eq(2L));
        Map<Long, Boolean> result = virtualMachineManagerImpl.getDiskOfferingSuitabilityForVm(1L, diskOfferingIds);
        assertTrue(MapUtils.isNotEmpty(result));
        assertEquals(2, result.keySet().size());
        assertFalse(result.get(1L));
        assertTrue(result.get(2L));
    }

    private void overrideVmMetadataConfigValue(final String manufacturer, final String product) {
        ConfigKey configKey = VirtualMachineManager.VmMetadataManufacturer;
        this.configDepotImpl = (ConfigDepotImpl)ReflectionTestUtils.getField(configKey, "s_depot");
        ConfigDepotImpl configDepot = Mockito.mock(ConfigDepotImpl.class);
        Mockito.when(configDepot.getConfigStringValue(Mockito.eq(configKey.key()),
                Mockito.eq(ConfigKey.Scope.Zone), Mockito.anyLong())).thenReturn(manufacturer);
        Mockito.when(configDepot.getConfigStringValue(Mockito.eq(VirtualMachineManager.VmMetadataProductName.key()),
                Mockito.eq(ConfigKey.Scope.Zone), Mockito.anyLong())).thenReturn(product);
        ReflectionTestUtils.setField(configKey, "s_depot", configDepot);
        updatedConfigKeyDepot = true;
    }

    private Pair<VirtualMachineTO, VMInstanceVO> getDummyVmTOAndVm() {
        VirtualMachineTO virtualMachineTO = new VirtualMachineTO(1L, "VM", VirtualMachine.Type.User, 1,
                1000, 256, 512, VirtualMachineTemplate.BootloaderType.HVM, "OS",
                false, false, "Pass");
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getDataCenterId()).thenReturn(1L);
        Mockito.when(vm.getHypervisorType()).thenReturn(HypervisorType.KVM);
        return new Pair<>(virtualMachineTO, vm);
    }

    @Test
    public void testUpdateVmMetadataManufacturerAndProductDefaultManufacturer() {
        overrideVmMetadataConfigValue("", "");
        Pair<VirtualMachineTO, VMInstanceVO> pair = getDummyVmTOAndVm();
        VirtualMachineTO to = pair.first();
        virtualMachineManagerImpl.updateVmMetadataManufacturerAndProduct(to, pair.second());
        Assert.assertEquals(VirtualMachineManager.VmMetadataManufacturer.defaultValue(), to.getMetadataManufacturer());
    }

    @Test
    public void testUpdateVmMetadataManufacturerAndProductCustomManufacturerDefaultProduct() {
        String manufacturer = "Custom";
        overrideVmMetadataConfigValue(manufacturer, "");
        Pair<VirtualMachineTO, VMInstanceVO> pair = getDummyVmTOAndVm();
        VirtualMachineTO to = pair.first();
        virtualMachineManagerImpl.updateVmMetadataManufacturerAndProduct(to, pair.second());
        Assert.assertEquals(manufacturer, to.getMetadataManufacturer());
        Assert.assertEquals("CloudStack KVM Hypervisor", to.getMetadataProductName());
    }

    @Test
    public void testUpdateVmMetadataManufacturerAndProductCustomManufacturer() {
        String manufacturer = UUID.randomUUID().toString();
        String product = UUID.randomUUID().toString();
        overrideVmMetadataConfigValue(manufacturer, product);
        Pair<VirtualMachineTO, VMInstanceVO> pair = getDummyVmTOAndVm();
        VirtualMachineTO to = pair.first();
        virtualMachineManagerImpl.updateVmMetadataManufacturerAndProduct(to, pair.second());
        Assert.assertEquals(manufacturer, to.getMetadataManufacturer());
        Assert.assertEquals(product, to.getMetadataProductName());
    }
}
