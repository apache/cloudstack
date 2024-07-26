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

package org.apache.cloudstack.vm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.vm.ImportUnmanagedInstanceCmd;
import org.apache.cloudstack.api.command.admin.vm.ImportVmCmd;
import org.apache.cloudstack.api.command.admin.vm.ListUnmanagedInstancesCmd;
import org.apache.cloudstack.api.command.admin.vm.ListVmsForImportCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckConvertInstanceAnswer;
import com.cloud.agent.api.CheckConvertInstanceCommand;
import com.cloud.agent.api.CheckVolumeAnswer;
import com.cloud.agent.api.CheckVolumeCommand;
import com.cloud.agent.api.ConvertInstanceAnswer;
import com.cloud.agent.api.ConvertInstanceCommand;
import com.cloud.agent.api.CopyRemoteVolumeAnswer;
import com.cloud.agent.api.CopyRemoteVolumeCommand;
import com.cloud.agent.api.GetRemoteVmsAnswer;
import com.cloud.agent.api.GetRemoteVmsCommand;
import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.configuration.Resource;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VmwareDatacenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VmwareDatacenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage;
import com.cloud.storage.StoragePool;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@RunWith(MockitoJUnitRunner.class)
public class UnmanagedVMsManagerImplTest {

    @InjectMocks
    private UnmanagedVMsManagerImpl unmanagedVMsManager = new UnmanagedVMsManagerImpl();

    @Mock
    private UserVmManager userVmManager;
    @Mock
    private ClusterDao clusterDao;
    @Mock
    private ClusterVO clusterVO;
    @Mock
    private UserVmVO userVm;
    @Mock
    private ResourceManager resourceManager;
    @Mock
    private VMTemplatePoolDao templatePoolDao;
    @Mock
    private AgentManager agentManager;
    @Mock
    private AccountService accountService;
    @Mock
    private UserDao userDao;
    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private VMTemplateDao templateDao;
    @Mock
    private VMInstanceDao vmDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private DiskOfferingDao diskOfferingDao;
    @Mock
    private NetworkDao networkDao;
    @Mock
    private NetworkOrchestrationService networkOrchestrationService;
    @Mock
    private VolumeOrchestrationService volumeManager;
    @Mock
    public ResponseGenerator responseGenerator;
    @Mock
    private VolumeDao volumeDao;
    @Mock
    private ResourceLimitService resourceLimitService;
    @Mock
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    private VolumeApiService volumeApiService;
    @Mock
    private NetworkModel networkModel;
    @Mock
    private ConfigurationDao configurationDao;
    @Mock
    private VMSnapshotDao vmSnapshotDao;
    @Mock
    private SnapshotDao snapshotDao;
    @Mock
    private UserVmDao userVmDao;
    @Mock
    private NicDao nicDao;
    @Mock
    private HostDao hostDao;
    @Mock
    private VmwareDatacenterDao vmwareDatacenterDao;
    @Mock
    private HypervisorGuruManager hypervisorGuruManager;
    @Mock
    private ImageStoreDao imageStoreDao;
    @Mock
    private DataStoreManager dataStoreManager;
    @Mock
    private StoragePoolHostDao storagePoolHostDao;

    @Mock
    private VMInstanceVO virtualMachine;
    @Mock
    private NicVO nicVO;
    @Mock
    EntityManager entityMgr;
    @Mock
    DeploymentPlanningManager deploymentPlanningManager;

    private static final long virtualMachineId = 1L;

    private AutoCloseable closeable;

    private MockedStatic<ActionEventUtils> actionEventUtilsMocked;

    private UnmanagedInstanceTO instance;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);
        actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class);
        BDDMockito.given(ActionEventUtils.onStartedActionEvent(anyLong(), anyLong(), anyString(), anyString(), anyLong(), anyString(), anyBoolean(), anyLong()))
                .willReturn(1L);
        BDDMockito.given(ActionEventUtils.onCompletedActionEvent(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong()))
                .willReturn(1L);

        AccountVO account = new AccountVO("admin", 1L, "", Account.Type.ADMIN, "uuid");
        UserVO user = new UserVO(1, "adminuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        instance = new UnmanagedInstanceTO();
        instance.setName("TestInstance");
        instance.setCpuCores(2);
        instance.setCpuCoresPerSocket(1);
        instance.setCpuSpeed(1000);
        instance.setMemory(1024);
        instance.setOperatingSystem("CentOS 7");
        List<UnmanagedInstanceTO.Disk> instanceDisks = new ArrayList<>();
        UnmanagedInstanceTO.Disk instanceDisk = new UnmanagedInstanceTO.Disk();
        instanceDisk.setDiskId("1000-1");
        instanceDisk.setLabel("DiskLabel");
        instanceDisk.setController("scsi");
        instanceDisk.setImagePath("[b6ccf44a1fa13e29b3667b4954fa10ee] TestInstance/ROOT-1.vmdk");
        instanceDisk.setCapacity(5242880L);
        instanceDisk.setDatastoreName("Test");
        instanceDisk.setDatastoreHost("Test");
        instanceDisk.setDatastorePath("Test");
        instanceDisk.setDatastoreType("NFS");
        instanceDisks.add(instanceDisk);
        instance.setDisks(instanceDisks);
        List<UnmanagedInstanceTO.Nic> instanceNics = new ArrayList<>();
        UnmanagedInstanceTO.Nic instanceNic = new UnmanagedInstanceTO.Nic();
        instanceNic.setNicId("NIC 1");
        instanceNic.setAdapterType("VirtualE1000E");
        instanceNic.setMacAddress("02:00:2e:0f:00:02");
        instanceNic.setVlan(1024);
        instanceNics.add(instanceNic);
        instance.setNics(instanceNics);
        instance.setPowerState(UnmanagedInstanceTO.PowerState.PowerOn);

        ClusterVO clusterVO = new ClusterVO(1L, 1L, "Cluster");
        clusterVO.setHypervisorType(Hypervisor.HypervisorType.VMware.toString());
        when(clusterDao.findById(anyLong())).thenReturn(clusterVO);
        when(configurationDao.getValue(Mockito.anyString())).thenReturn(null);
        doNothing().when(resourceLimitService).checkResourceLimit(any(Account.class), any(Resource.ResourceType.class), anyLong());
        List<HostVO> hosts = new ArrayList<>();
        HostVO hostVO = Mockito.mock(HostVO.class);
        when(hostVO.isInMaintenanceStates()).thenReturn(false);
        hosts.add(hostVO);
        when(resourceManager.listHostsInClusterByStatus(anyLong(), any(Status.class))).thenReturn(hosts);
        when(resourceManager.listAllUpAndEnabledHostsInOneZoneByHypervisor(any(Hypervisor.HypervisorType.class), anyLong())).thenReturn(hosts);
        List<VMTemplateStoragePoolVO> templates = new ArrayList<>();
        when(templatePoolDao.listAll()).thenReturn(templates);
        List<VolumeVO> volumes = new ArrayList<>();
        when(volumeDao.findIncludingRemovedByZone(anyLong())).thenReturn(volumes);
        GetUnmanagedInstancesCommand cmd = Mockito.mock(GetUnmanagedInstancesCommand.class);
        HashMap<String, UnmanagedInstanceTO> map = new HashMap<>();
        map.put(instance.getName(), instance);
        Answer answer = new GetUnmanagedInstancesAnswer(cmd, "", map);
        when(agentManager.easySend(anyLong(), any(GetUnmanagedInstancesCommand.class))).thenReturn(answer);
        GetRemoteVmsCommand remoteVmListcmd = Mockito.mock(GetRemoteVmsCommand.class);
        Answer remoteVmListAnswer = new GetRemoteVmsAnswer(remoteVmListcmd, "", map);
        when(agentManager.easySend(anyLong(), any(GetRemoteVmsCommand.class))).thenReturn(remoteVmListAnswer);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        when(zone.getId()).thenReturn(1L);
        when(dataCenterDao.findById(anyLong())).thenReturn(zone);
        when(accountService.getActiveAccountById(anyLong())).thenReturn(Mockito.mock(Account.class));
        List<UserVO> users = new ArrayList<>();
        users.add(Mockito.mock(UserVO.class));
        when(userDao.listByAccount(anyLong())).thenReturn(users);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(template.getName()).thenReturn("Template");
        when(templateDao.findById(anyLong())).thenReturn(template);
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        when(serviceOffering.getId()).thenReturn(1L);
        when(serviceOffering.isDynamic()).thenReturn(false);
        when(serviceOffering.getCpu()).thenReturn(instance.getCpuCores());
        when(serviceOffering.getRamSize()).thenReturn(instance.getMemory());
        when(serviceOffering.getSpeed()).thenReturn(instance.getCpuSpeed());
        when(serviceOfferingDao.findById(anyLong())).thenReturn(serviceOffering);
        when(serviceOfferingDao.findById(anyLong(), anyLong())).thenReturn(Mockito.mock(ServiceOfferingVO.class));
        DiskOfferingVO diskOfferingVO = Mockito.mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(anyLong())).thenReturn(diskOfferingVO);
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        when(userVm.getAccountId()).thenReturn(1L);
        when(userVm.getDataCenterId()).thenReturn(1L);
        when(userVm.getHostName()).thenReturn(instance.getName());
        when(userVm.getTemplateId()).thenReturn(1L);
        when(userVm.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.VMware);
        when(userVm.getUuid()).thenReturn("abcd");
        when(userVm.isDisplayVm()).thenReturn(true);
        // Skip usage publishing and resource increment for test
        userVm.setInstanceName(instance.getName());
        userVm.setHostName(instance.getName());
        StoragePoolVO poolVO = Mockito.mock(StoragePoolVO.class);
        when(poolVO.getDataCenterId()).thenReturn(1L);
        when(poolVO.getClusterId()).thenReturn(clusterVO.getId());
        List<StoragePoolVO> pools = new ArrayList<>();
        pools.add(poolVO);
        when(primaryDataStoreDao.listPoolByHostPath(Mockito.anyString(), Mockito.anyString())).thenReturn(pools);
        when(userVmManager.importVM(nullable(DataCenter.class), nullable(Host.class), nullable(VirtualMachineTemplate.class), nullable(String.class), nullable(String.class),
                nullable(Account.class), nullable(String.class), nullable(Account.class), nullable(Boolean.class), nullable(String.class),
                nullable(Long.class), nullable(Long.class), nullable(ServiceOffering.class), nullable(String.class),
                nullable(String.class), nullable(Hypervisor.HypervisorType.class), nullable(Map.class), nullable(VirtualMachine.PowerState.class), nullable(LinkedHashMap.class))).thenReturn(userVm);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.L2);
        when(networkVO.getBroadcastUri()).thenReturn(URI.create(String.format("vlan://%d", instanceNic.getVlan())));
        when(networkVO.getDataCenterId()).thenReturn(1L);
        when(networkDao.findById(anyLong())).thenReturn(networkVO);
        List<NetworkVO> networks = new ArrayList<>();
        networks.add(networkVO);
        when(networkDao.listByZone(anyLong())).thenReturn(networks);
        doNothing().when(networkModel).checkNetworkPermissions(any(Account.class), any(Network.class));
        NicProfile profile = Mockito.mock(NicProfile.class);
        Integer deviceId = 100;
        Pair<NicProfile, Integer> pair = new Pair<NicProfile, Integer>(profile, deviceId);
        when(networkOrchestrationService.importNic(nullable(String.class), nullable(Integer.class), nullable(Network.class), nullable(Boolean.class), nullable(VirtualMachine.class), nullable(Network.IpAddresses.class), nullable(DataCenter.class), anyBoolean())).thenReturn(pair);
        when(volumeDao.findByInstance(anyLong())).thenReturn(volumes);
        List<UserVmResponse> userVmResponses = new ArrayList<>();
        UserVmResponse userVmResponse = new UserVmResponse();
        userVmResponse.setInstanceName(instance.getName());
        userVmResponses.add(userVmResponse);
        when(responseGenerator.createUserVmResponse(any(ResponseObject.ResponseView.class), Mockito.anyString(), any(UserVm.class))).thenReturn(userVmResponses);

        when(vmDao.findById(virtualMachineId)).thenReturn(virtualMachine);
        when(virtualMachine.getState()).thenReturn(VirtualMachine.State.Running);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        actionEventUtilsMocked.close();
        CallContext.unregister();
    }

    @Test
    public void listUnmanagedInstancesTest() {
        ListUnmanagedInstancesCmd cmd = Mockito.mock(ListUnmanagedInstancesCmd.class);
        unmanagedVMsManager.listUnmanagedInstances(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void listUnmanagedInstancesInvalidHypervisorTest() {
        ListUnmanagedInstancesCmd cmd = Mockito.mock(ListUnmanagedInstancesCmd.class);
        ClusterVO cluster = new ClusterVO(1, 1, "Cluster");
        cluster.setHypervisorType(Hypervisor.HypervisorType.XenServer.toString());
        when(clusterDao.findById(anyLong())).thenReturn(cluster);
        unmanagedVMsManager.listUnmanagedInstances(cmd);
    }

    @Test(expected = PermissionDeniedException.class)
    public void listUnmanagedInstancesInvalidCallerTest() {
        CallContext.unregister();
        AccountVO account = new AccountVO("user", 1L, "", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        ListUnmanagedInstancesCmd cmd = Mockito.mock(ListUnmanagedInstancesCmd.class);
        unmanagedVMsManager.listUnmanagedInstances(cmd);
    }

    @Test
    public void importUnmanagedInstanceTest() {
        ImportUnmanagedInstanceCmd importUnmanageInstanceCmd = Mockito.mock(ImportUnmanagedInstanceCmd.class);
        when(importUnmanageInstanceCmd.getName()).thenReturn("TestInstance");
        when(importUnmanageInstanceCmd.getDomainId()).thenReturn(null);
        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
            unmanagedVMsManager.importUnmanagedInstance(importUnmanageInstanceCmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void importUnmanagedInstanceInvalidHostnameTest() {
        ImportUnmanagedInstanceCmd importUnmanageInstanceCmd = Mockito.mock(ImportUnmanagedInstanceCmd.class);
        when(importUnmanageInstanceCmd.getName()).thenReturn("TestInstance");
        when(importUnmanageInstanceCmd.getName()).thenReturn("some name");
        unmanagedVMsManager.importUnmanagedInstance(importUnmanageInstanceCmd);
    }

    @Test(expected = ServerApiException.class)
    public void importUnmanagedInstanceMissingInstanceTest() {
        ImportUnmanagedInstanceCmd importUnmanageInstanceCmd = Mockito.mock(ImportUnmanagedInstanceCmd.class);
        when(importUnmanageInstanceCmd.getName()).thenReturn("SomeInstance");
        when(importUnmanageInstanceCmd.getDomainId()).thenReturn(null);
        unmanagedVMsManager.importUnmanagedInstance(importUnmanageInstanceCmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void unmanageVMInstanceMissingInstanceTest() {
        long notExistingId = 10L;
        unmanagedVMsManager.unmanageVMInstance(notExistingId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void unmanageVMInstanceDestroyedInstanceTest() {
        when(virtualMachine.getState()).thenReturn(VirtualMachine.State.Destroyed);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void unmanageVMInstanceExpungedInstanceTest() {
        when(virtualMachine.getState()).thenReturn(VirtualMachine.State.Expunging);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = UnsupportedServiceException.class)
    public void unmanageVMInstanceExistingVMSnapshotsTest() {
        when(virtualMachine.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.None);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = UnsupportedServiceException.class)
    public void unmanageVMInstanceExistingVolumeSnapshotsTest() {
        when(virtualMachine.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.None);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = UnsupportedServiceException.class)
    public void unmanageVMInstanceExistingISOAttachedTest() {
        when(virtualMachine.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.None);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test
    public void testListRemoteInstancesTest() {
        ListVmsForImportCmd cmd = Mockito.mock(ListVmsForImportCmd.class);
        when(cmd.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM.toString());
        when(cmd.getUsername()).thenReturn("user");
        when(cmd.getPassword()).thenReturn("pass");
        ListResponse response = unmanagedVMsManager.listVmsForImport(cmd);
        Assert.assertEquals(1, response.getCount().intValue());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListRemoteInstancesTestNonKVM() {
        ListVmsForImportCmd cmd = Mockito.mock(ListVmsForImportCmd.class);
        unmanagedVMsManager.listVmsForImport(cmd);
    }
    @Test
    public void testImportFromExternalTest() throws InsufficientServerCapacityException {
        String vmname = "TestInstance";
        ImportVmCmd cmd = Mockito.mock(ImportVmCmd.class);
        when(cmd.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM.toString());
        when(cmd.getName()).thenReturn(vmname);
        when(cmd.getUsername()).thenReturn("user");
        when(cmd.getPassword()).thenReturn("pass");
        when(cmd.getImportSource()).thenReturn("external");
        when(cmd.getDomainId()).thenReturn(null);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(templateDao.findByName(anyString())).thenReturn(template);
        HostVO host = Mockito.mock(HostVO.class);
        when(userVmDao.getNextInSequence(Long.class, "id")).thenReturn(1L);
        DeployDestination mockDest = Mockito.mock(DeployDestination.class);
        when(deploymentPlanningManager.planDeployment(any(), any(), any(), any())).thenReturn(mockDest);
        DiskProfile diskProfile = Mockito.mock(DiskProfile.class);
        when(volumeManager.allocateRawVolume(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(diskProfile);
        Map<Volume, StoragePool> storage = new HashMap<>();
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        storage.put(volume, storagePool);
        when(mockDest.getStorageForDisks()).thenReturn(storage);
        when(mockDest.getHost()).thenReturn(host);
        when(volumeDao.findById(anyLong())).thenReturn(volume);
        CopyRemoteVolumeAnswer copyAnswer = Mockito.mock(CopyRemoteVolumeAnswer.class);
        when(copyAnswer.getResult()).thenReturn(true);
        when(agentManager.easySend(anyLong(), any(CopyRemoteVolumeCommand.class))).thenReturn(copyAnswer);
        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
            unmanagedVMsManager.importVm(cmd);
        }
    }

    private void baseBasicParametersCheckForImportInstance(String name, Long domainId, String accountName) {
        unmanagedVMsManager.basicParametersCheckForImportInstance(name, domainId, accountName);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testBasicParametersCheckForImportInstanceMissingName() {
        baseBasicParametersCheckForImportInstance(null, 1L, "test");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testBasicParametersCheckForImportInstanceMissingDomainAndAccount() {
        baseBasicParametersCheckForImportInstance("vm", 1L, "");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testBasicAccessChecksMissingClusterId() {
        unmanagedVMsManager.basicAccessChecks(null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testBasicAccessChecksNotAdminCaller() {
        CallContext.unregister();
        AccountVO account = new AccountVO("user", 1L, "", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        unmanagedVMsManager.basicAccessChecks(1L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testBasicAccessChecksUnsupportedHypervisorType() {
        ClusterVO clusterVO = new ClusterVO(1L, 1L, "Cluster");
        clusterVO.setHypervisorType(Hypervisor.HypervisorType.XenServer.toString());
        when(clusterDao.findById(anyLong())).thenReturn(clusterVO);
        unmanagedVMsManager.basicAccessChecks(1L);
    }

    @Test
    public void testGetTemplateForImportInstanceDefaultTemplate() {
        String defaultTemplateName = "DefaultTemplate";
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(template.getName()).thenReturn(defaultTemplateName);
        when(templateDao.findByName(anyString())).thenReturn(template);
        VMTemplateVO templateForImportInstance = unmanagedVMsManager.getTemplateForImportInstance(null, Hypervisor.HypervisorType.KVM);
        Assert.assertEquals(defaultTemplateName, templateForImportInstance.getName());
    }

    private enum VcenterParameter {
        EXISTING, EXTERNAL, BOTH, NONE, EXISTING_INVALID, AGENT_UNAVAILABLE, CONVERT_FAILURE
    }

    private void baseTestImportVmFromVmwareToKvm(VcenterParameter vcenterParameter, boolean selectConvertHost,
                                                 boolean selectTemporaryStorage) throws OperationTimedoutException, AgentUnavailableException {
        long clusterId = 1L;
        long zoneId = 1L;
        long podId = 1L;
        long existingDatacenterId = 1L;
        String vcenterHost = "192.168.1.2";
        String datacenter = "Datacenter";
        String username = "administrator@vsphere.local";
        String password = "password";
        String host = "192.168.1.10";
        String vmName = "TestInstanceFromVmware";
        instance.setName(vmName);
        String tmplFileName = "5b8d689a-e61a-4ac3-9b76-e121ff90fbd3";
        long newVmId = 2L;
        long networkId = 1L;
        when(vmDao.getNextInSequence(Long.class, "id")).thenReturn(newVmId);

        ClusterVO cluster = mock(ClusterVO.class);
        when(cluster.getId()).thenReturn(clusterId);
        when(cluster.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(cluster.getDataCenterId()).thenReturn(zoneId);
        when(clusterDao.findById(clusterId)).thenReturn(cluster);

        ImportVmCmd importVmCmd = Mockito.mock(ImportVmCmd.class);

        when(importVmCmd.getName()).thenReturn(vmName);
        when(importVmCmd.getClusterId()).thenReturn(clusterId);
        when(importVmCmd.getDomainId()).thenReturn(null);
        when(importVmCmd.getImportSource()).thenReturn(VmImportService.ImportSource.VMWARE.toString());
        when(importVmCmd.getHostIp()).thenReturn(host);
        when(importVmCmd.getNicNetworkList()).thenReturn(Map.of("NIC 1", networkId));
        when(importVmCmd.getConvertInstanceHostId()).thenReturn(null);
        when(importVmCmd.getConvertStoragePoolId()).thenReturn(null);

        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.L2);
        when(networkVO.getDataCenterId()).thenReturn(zoneId);
        when(networkDao.findById(networkId)).thenReturn(networkVO);

        HypervisorGuru vmwareGuru = mock(HypervisorGuru.class);
        when(hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.VMware)).thenReturn(vmwareGuru);
        when(vmwareGuru.getHypervisorVMOutOfBandAndCloneIfRequired(anyString(), anyString(), anyMap())).thenReturn(new Pair<>(instance, true));
        when(vmwareGuru.removeClonedHypervisorVMOutOfBand(anyString(), anyString(), anyMap())).thenReturn(true);
        when(vmwareGuru.createVMTemplateOutOfBand(anyString(), anyString(), anyMap(), any(DataStoreTO.class), anyInt())).thenReturn(tmplFileName);
        when(vmwareGuru.removeVMTemplateOutOfBand(any(DataStoreTO.class), anyString())).thenReturn(true);

        HostVO convertHost = mock(HostVO.class);
        long convertHostId = 1L;
        when(convertHost.getStatus()).thenReturn(Status.Up);
        when(convertHost.getResourceState()).thenReturn(ResourceState.Enabled);
        when(convertHost.getId()).thenReturn(convertHostId);
        when(convertHost.getName()).thenReturn("KVM-Convert-Host");
        when(convertHost.getType()).thenReturn(Host.Type.Routing);
        when(convertHost.getClusterId()).thenReturn(clusterId);
        if (selectConvertHost) {
            when(importVmCmd.getConvertInstanceHostId()).thenReturn(convertHostId);
            when(hostDao.findById(convertHostId)).thenReturn(convertHost);
        }

        DataStoreTO dataStoreTO = mock(DataStoreTO.class);
        DataStore dataStore = mock(DataStore.class);
        when(dataStore.getTO()).thenReturn(dataStoreTO);

        StoragePoolVO destPool = mock(StoragePoolVO.class);
        when(destPool.getUuid()).thenReturn(UUID.randomUUID().toString());
        when(destPool.getDataCenterId()).thenReturn(zoneId);
        when(destPool.getClusterId()).thenReturn(null);
        when(destPool.getPoolType()).thenReturn(Storage.StoragePoolType.NetworkFilesystem);
        StoragePoolVO zoneDestPool = mock(StoragePoolVO.class);
        if (selectTemporaryStorage) {
            long temporaryStoragePoolId = 1L;
            when(importVmCmd.getConvertStoragePoolId()).thenReturn(temporaryStoragePoolId);
            when(primaryDataStoreDao.findById(temporaryStoragePoolId)).thenReturn(destPool);
            when(dataStoreManager.getPrimaryDataStore(temporaryStoragePoolId)).thenReturn(dataStore);
        } else {
            ImageStoreVO imageStoreVO = mock(ImageStoreVO.class);
            when(imageStoreVO.getId()).thenReturn(1L);
            when(imageStoreDao.findOneByZoneAndProtocol(zoneId, "nfs")).thenReturn(imageStoreVO);
            when(dataStoreManager.getDataStore(1L, DataStoreRole.Image)).thenReturn(dataStore);
        }
        when(primaryDataStoreDao.listPoolByHostPath(Mockito.anyString(), Mockito.anyString())).thenReturn(List.of(destPool));
        when(primaryDataStoreDao.findClusterWideStoragePoolsByHypervisorAndPoolType(clusterId, Hypervisor.HypervisorType.KVM, Storage.StoragePoolType.NetworkFilesystem)).thenReturn(List.of(destPool));
        when(primaryDataStoreDao.findZoneWideStoragePoolsByHypervisorAndPoolType(zoneId, Hypervisor.HypervisorType.KVM, Storage.StoragePoolType.NetworkFilesystem)).thenReturn(List.of(zoneDestPool));

        if (VcenterParameter.EXISTING == vcenterParameter) {
            VmwareDatacenterVO datacenterVO = mock(VmwareDatacenterVO.class);
            when(datacenterVO.getVcenterHost()).thenReturn(vcenterHost);
            when(datacenterVO.getVmwareDatacenterName()).thenReturn(datacenter);
            when(datacenterVO.getUser()).thenReturn(username);
            when(datacenterVO.getPassword()).thenReturn(password);
            when(importVmCmd.getExistingVcenterId()).thenReturn(existingDatacenterId);
            when(vmwareDatacenterDao.findById(existingDatacenterId)).thenReturn(datacenterVO);
        } else if (VcenterParameter.EXTERNAL == vcenterParameter) {
            when(importVmCmd.getVcenter()).thenReturn(vcenterHost);
            when(importVmCmd.getDatacenterName()).thenReturn(datacenter);
            when(importVmCmd.getUsername()).thenReturn(username);
            when(importVmCmd.getPassword()).thenReturn(password);
        }

        if (VcenterParameter.BOTH == vcenterParameter) {
            when(importVmCmd.getExistingVcenterId()).thenReturn(existingDatacenterId);
            when(importVmCmd.getVcenter()).thenReturn(vcenterHost);
        } else if (VcenterParameter.NONE == vcenterParameter) {
            when(importVmCmd.getExistingVcenterId()).thenReturn(null);
            when(importVmCmd.getVcenter()).thenReturn(null);
        } else if (VcenterParameter.EXISTING_INVALID == vcenterParameter) {
            when(importVmCmd.getExistingVcenterId()).thenReturn(existingDatacenterId);
            when(vmwareDatacenterDao.findById(existingDatacenterId)).thenReturn(null);
        }

        CheckConvertInstanceAnswer checkConvertInstanceAnswer = mock(CheckConvertInstanceAnswer.class);
        when(checkConvertInstanceAnswer.getResult()).thenReturn(vcenterParameter != VcenterParameter.CONVERT_FAILURE);
        if (VcenterParameter.AGENT_UNAVAILABLE != vcenterParameter) {
            when(agentManager.send(Mockito.eq(convertHostId), Mockito.any(CheckConvertInstanceCommand.class))).thenReturn(checkConvertInstanceAnswer);
        }

        ConvertInstanceAnswer convertInstanceAnswer = mock(ConvertInstanceAnswer.class);
        when(convertInstanceAnswer.getResult()).thenReturn(vcenterParameter != VcenterParameter.CONVERT_FAILURE);
        when(convertInstanceAnswer.getConvertedInstance()).thenReturn(instance);
        if (VcenterParameter.AGENT_UNAVAILABLE != vcenterParameter) {
            when(agentManager.send(Mockito.eq(convertHostId), Mockito.any(ConvertInstanceCommand.class))).thenReturn(convertInstanceAnswer);
        }

        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
            unmanagedVMsManager.importVm(importVmCmd);
            verify(vmwareGuru).getHypervisorVMOutOfBandAndCloneIfRequired(Mockito.eq(host), Mockito.eq(vmName), anyMap());
            verify(vmwareGuru).createVMTemplateOutOfBand(Mockito.eq(host), Mockito.eq(vmName), anyMap(), any(DataStoreTO.class), anyInt());
            verify(vmwareGuru).removeClonedHypervisorVMOutOfBand(Mockito.eq(host), Mockito.eq(vmName), anyMap());
            verify(vmwareGuru).removeVMTemplateOutOfBand(any(DataStoreTO.class), anyString());
        }
    }

    @Test
    public void testImportFromLocalDisk() throws InsufficientServerCapacityException {
        testImportFromDisk("local");
    }

    @Test
    public void testImportFromsharedStorage() throws InsufficientServerCapacityException {
        testImportFromDisk("shared");
    }

    private void testImportFromDisk(String source) throws InsufficientServerCapacityException {
        String vmname = "testVm";
        ImportVmCmd cmd = Mockito.mock(ImportVmCmd.class);
        when(cmd.getHypervisor()).thenReturn(Hypervisor.HypervisorType.KVM.toString());
        when(cmd.getName()).thenReturn(vmname);
        when(cmd.getImportSource()).thenReturn(source);
        when(cmd.getDiskPath()).thenReturn("/var/lib/libvirt/images/test.qcow2");
        when(cmd.getDomainId()).thenReturn(null);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(templateDao.findByName(anyString())).thenReturn(template);
        HostVO host = Mockito.mock(HostVO.class);
        when(hostDao.findById(anyLong())).thenReturn(host);
        NetworkOffering netOffering = Mockito.mock(NetworkOffering.class);
        when(entityMgr.findById(NetworkOffering.class, 0L)).thenReturn(netOffering);
        when(userVmDao.getNextInSequence(Long.class, "id")).thenReturn(1L);
        DeployDestination mockDest = Mockito.mock(DeployDestination.class);
        when(deploymentPlanningManager.planDeployment(any(), any(), any(), any())).thenReturn(mockDest);
        DiskProfile diskProfile = Mockito.mock(DiskProfile.class);
        when(volumeManager.allocateRawVolume(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                        .thenReturn(diskProfile);
        Map<Volume, StoragePool> storage = new HashMap<>();
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
        storage.put(volume, storagePool);
        when(mockDest.getStorageForDisks()).thenReturn(storage);
        when(mockDest.getHost()).thenReturn(host);
        when(volumeDao.findById(anyLong())).thenReturn(volume);
        CheckVolumeAnswer answer = Mockito.mock(CheckVolumeAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(anyLong(), any(CheckVolumeCommand.class))).thenReturn(answer);
        List<StoragePoolVO> storagePools = new ArrayList<>();
        storagePools.add(storagePool);
        when(primaryDataStoreDao.findLocalStoragePoolsByHostAndTags(anyLong(), any())).thenReturn(storagePools);
        when(primaryDataStoreDao.findById(anyLong())).thenReturn(storagePool);
        when(volumeApiService.doesTargetStorageSupportDiskOffering(any(StoragePool.class), any())).thenReturn(true);
        StoragePoolHostVO storagePoolHost = Mockito.mock(StoragePoolHostVO.class);
        when(storagePoolHostDao.findByPoolHost(anyLong(), anyLong())).thenReturn(storagePoolHost);
        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
                unmanagedVMsManager.importVm(cmd);
        }
    }

    public void testImportVmFromVmwareToKvmExistingVcenter() throws OperationTimedoutException, AgentUnavailableException {
        baseTestImportVmFromVmwareToKvm(VcenterParameter.EXISTING, false, false);
    }

    @Test
    public void testImportVmFromVmwareToKvmExistingVcenterSetConvertHost() throws OperationTimedoutException, AgentUnavailableException {
        baseTestImportVmFromVmwareToKvm(VcenterParameter.EXISTING, true, false);
    }

    @Test
    public void testImportVmFromVmwareToKvmExistingVcenterSetConvertHostAndTemporaryStorage() throws OperationTimedoutException, AgentUnavailableException {
        baseTestImportVmFromVmwareToKvm(VcenterParameter.EXISTING, true, true);
    }

    @Test(expected = ServerApiException.class)
    public void testImportVmFromVmwareToKvmExistingVcenterExclusiveParameters() throws OperationTimedoutException, AgentUnavailableException {
        baseTestImportVmFromVmwareToKvm(VcenterParameter.BOTH, false, false);
    }

    @Test(expected = ServerApiException.class)
    public void testImportVmFromVmwareToKvmExistingVcenterMissingParameters() throws OperationTimedoutException, AgentUnavailableException {
        baseTestImportVmFromVmwareToKvm(VcenterParameter.NONE, false, false);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testImportVmFromVmwareToKvmExistingVcenterInvalid() throws OperationTimedoutException, AgentUnavailableException {
        baseTestImportVmFromVmwareToKvm(VcenterParameter.EXISTING_INVALID, false, false);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testImportVmFromVmwareToKvmExistingVcenterAgentUnavailable() throws OperationTimedoutException, AgentUnavailableException {
        baseTestImportVmFromVmwareToKvm(VcenterParameter.AGENT_UNAVAILABLE, false, false);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testImportVmFromVmwareToKvmExistingVcenterConvertFailure() throws OperationTimedoutException, AgentUnavailableException {
        baseTestImportVmFromVmwareToKvm(VcenterParameter.CONVERT_FAILURE, false, false);
    }

    private ClusterVO getClusterForTests() {
        ClusterVO cluster = mock(ClusterVO.class);
        when(cluster.getId()).thenReturn(1L);
        when(cluster.getDataCenterId()).thenReturn(1L);
        return cluster;
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSelectInstanceConversionTemporaryLocationInvalidStorage() {
        ClusterVO cluster = getClusterForTests();

        long poolId = 1L;
        when(primaryDataStoreDao.findById(poolId)).thenReturn(null);
        unmanagedVMsManager.selectInstanceConversionTemporaryLocation(cluster, poolId);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSelectInstanceConversionTemporaryLocationPoolInvalidScope() {
        ClusterVO cluster = getClusterForTests();
        long poolId = 1L;
        StoragePoolVO pool = mock(StoragePoolVO.class);
        Mockito.when(pool.getScope()).thenReturn(ScopeType.CLUSTER);
        Mockito.when(pool.getClusterId()).thenReturn(100L);
        when(primaryDataStoreDao.findById(poolId)).thenReturn(pool);
        unmanagedVMsManager.selectInstanceConversionTemporaryLocation(cluster, poolId);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSelectInstanceConversionTemporaryLocationLocalStoragePoolInvalid() {
        ClusterVO cluster = getClusterForTests();
        long poolId = 1L;
        StoragePoolVO pool = mock(StoragePoolVO.class);
        Mockito.when(pool.getScope()).thenReturn(ScopeType.HOST);
        when(primaryDataStoreDao.findById(poolId)).thenReturn(pool);
        unmanagedVMsManager.selectInstanceConversionTemporaryLocation(cluster, poolId);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSelectInstanceConversionTemporaryLocationStoragePoolInvalidType() {
        ClusterVO cluster = getClusterForTests();
        long poolId = 1L;
        StoragePoolVO pool = mock(StoragePoolVO.class);
        Mockito.when(pool.getScope()).thenReturn(ScopeType.CLUSTER);
        Mockito.when(pool.getClusterId()).thenReturn(1L);
        when(primaryDataStoreDao.findById(poolId)).thenReturn(pool);
        Mockito.when(pool.getPoolType()).thenReturn(Storage.StoragePoolType.RBD);
        unmanagedVMsManager.selectInstanceConversionTemporaryLocation(cluster, poolId);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testSelectInstanceConversionTemporaryLocationNoPoolAvailable() {
        ClusterVO cluster = getClusterForTests();
        Mockito.when(imageStoreDao.findOneByZoneAndProtocol(anyLong(), anyString())).thenReturn(null);
        unmanagedVMsManager.selectInstanceConversionTemporaryLocation(cluster, null);
    }

    @Test
    public void testCheckUnmanagedDiskLimits() {
        Account owner = Mockito.mock(Account.class);
        UnmanagedInstanceTO.Disk disk = Mockito.mock(UnmanagedInstanceTO.Disk.class);
        Mockito.when(disk.getDiskId()).thenReturn("disk1");
        Mockito.when(disk.getCapacity()).thenReturn(100L);
        ServiceOffering serviceOffering = Mockito.mock(ServiceOffering.class);
        Mockito.when(serviceOffering.getDiskOfferingId()).thenReturn(1L);
        UnmanagedInstanceTO.Disk dataDisk = Mockito.mock(UnmanagedInstanceTO.Disk.class);
        Mockito.when(dataDisk.getDiskId()).thenReturn("disk2");
        Mockito.when(dataDisk.getCapacity()).thenReturn(1000L);
        Map<String, Long> dataDiskMap = new HashMap<>();
        dataDiskMap.put("disk2", 2L);
        DiskOfferingVO offering1 = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOfferingDao.findById(1L)).thenReturn(offering1);
        String tag1 = "tag1";
        Mockito.when(resourceLimitService.getResourceLimitStorageTags(offering1)).thenReturn(List.of(tag1));
        DiskOfferingVO offering2 = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOfferingDao.findById(2L)).thenReturn(offering2);
        String tag2 = "tag2";
        Mockito.when(resourceLimitService.getResourceLimitStorageTags(offering2)).thenReturn(List.of(tag2));
        try {
            Mockito.doNothing().when(resourceLimitService).checkResourceLimit(any(), any(), any());
            Mockito.doNothing().when(resourceLimitService).checkResourceLimitWithTag(any(), any(), any(), any());
            unmanagedVMsManager.checkUnmanagedDiskLimits(owner, disk, serviceOffering, List.of(dataDisk), dataDiskMap);
            Mockito.verify(resourceLimitService, Mockito.times(1)).checkResourceLimit(owner, Resource.ResourceType.volume, 2);
            Mockito.verify(resourceLimitService, Mockito.times(1)).checkResourceLimit(owner, Resource.ResourceType.primary_storage, 1100L);
            Mockito.verify(resourceLimitService, Mockito.times(1)).checkResourceLimitWithTag(owner, Resource.ResourceType.volume, tag1,1);
            Mockito.verify(resourceLimitService, Mockito.times(1)).checkResourceLimitWithTag(owner, Resource.ResourceType.volume, tag2,1);
            Mockito.verify(resourceLimitService, Mockito.times(1)).checkResourceLimitWithTag(owner, Resource.ResourceType.primary_storage, tag1,100L);
            Mockito.verify(resourceLimitService, Mockito.times(1)).checkResourceLimitWithTag(owner, Resource.ResourceType.primary_storage, tag2,1000L);
        } catch (ResourceAllocationException e) {
            Assert.fail("Exception encountered: " + e.getMessage());
        }
    }
}
