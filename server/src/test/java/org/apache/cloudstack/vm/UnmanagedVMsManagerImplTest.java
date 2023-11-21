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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVolumeAnswer;
import com.cloud.agent.api.CheckVolumeCommand;
import com.cloud.agent.api.CopyRemoteVolumeAnswer;
import com.cloud.agent.api.CopyRemoteVolumeCommand;
import com.cloud.agent.api.GetRemoteVmsAnswer;
import com.cloud.agent.api.GetRemoteVmsCommand;
import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.configuration.Resource;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cloud.exception.UnsupportedServiceException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
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
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class UnmanagedVMsManagerImplTest {

    @InjectMocks
    private UnmanagedVMsManager unmanagedVMsManager = new UnmanagedVMsManagerImpl();

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
    private VMInstanceVO virtualMachine;
    @Mock
    private NicVO nicVO;
    @Mock
    EntityManager entityMgr;
    @Mock
    DeploymentPlanningManager deploymentPlanningManager;

    private static final long virtualMachineId = 1L;

    private AutoCloseable closeable;

    @Before
    public void setUp() throws Exception {
        closeable = MockitoAnnotations.openMocks(this);

        AccountVO account = new AccountVO("admin", 1L, "", Account.Type.ADMIN, "uuid");
        UserVO user = new UserVO(1, "adminuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        UnmanagedInstanceTO instance = new UnmanagedInstanceTO();
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

        clusterVO = new ClusterVO(1L, 1L, "Cluster");
        when(clusterDao.findById(anyLong())).thenReturn(clusterVO);
        when(configurationDao.getValue(anyString())).thenReturn(null);
        doNothing().when(resourceLimitService).checkResourceLimit(any(Account.class), any(Resource.ResourceType.class), anyLong());
        List<HostVO> hosts = new ArrayList<>();
        HostVO hostVO = Mockito.mock(HostVO.class);
        when(hostVO.isInMaintenanceStates()).thenReturn(false);
        hosts.add(hostVO);
        when(hostVO.checkHostServiceOfferingTags(any())).thenReturn(true);
        when(resourceManager.listHostsInClusterByStatus(anyLong(), any(Status.class))).thenReturn(hosts);
        when(resourceManager.listAllUpAndEnabledHostsInOneZoneByHypervisor(any(Hypervisor.HypervisorType.class), Mockito.anyLong())).thenReturn(hosts);
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
        when(agentManager.easySend(Mockito.anyLong(), any(GetRemoteVmsCommand.class))).thenReturn(remoteVmListAnswer);
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
        DiskOfferingVO diskOfferingVO = Mockito.mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(anyLong())).thenReturn(diskOfferingVO);
        userVm = Mockito.mock(UserVmVO.class);
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
        when(primaryDataStoreDao.listPoolByHostPath(anyString(), anyString())).thenReturn(pools);
        when(userVmManager.importVM(nullable(DataCenter.class), nullable(Host.class), nullable(VirtualMachineTemplate.class), nullable(String.class), nullable(String.class),
                nullable(Account.class), nullable(String.class), nullable(Account.class), nullable(Boolean.class), nullable(String.class),
                nullable(Long.class), nullable(Long.class), nullable(ServiceOffering.class), nullable(String.class),
                nullable(String.class), nullable(Hypervisor.HypervisorType.class), nullable(Map.class), nullable(VirtualMachine.PowerState.class), nullable(LinkedHashMap.class))).thenReturn(userVm);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.L2);
        when(networkVO.getDataCenterId()).thenReturn(1L);
        when(networkDao.findById(anyLong())).thenReturn(networkVO);
        List<NetworkVO> networks = new ArrayList<>();
        networks.add(networkVO);
        when(networkDao.listByZone(anyLong())).thenReturn(networks);
        doNothing().when(networkModel).checkNetworkPermissions(any(Account.class), any(Network.class));
        NicProfile profile = Mockito.mock(NicProfile.class);
        Integer deviceId = 100;
        Pair<NicProfile, Integer> pair = new Pair<NicProfile, Integer>(profile, deviceId);
        when(networkOrchestrationService.importNic(nullable(String.class), nullable(Integer.class), nullable(Network.class), nullable(Boolean.class), nullable(VirtualMachine.class), nullable(Network.IpAddresses.class), Mockito.anyBoolean())).thenReturn(pair);
        when(volumeDao.findByInstance(anyLong())).thenReturn(volumes);
        List<UserVmResponse> userVmResponses = new ArrayList<>();
        UserVmResponse userVmResponse = new UserVmResponse();
        userVmResponse.setInstanceName(instance.getName());
        userVmResponses.add(userVmResponse);
        when(responseGenerator.createUserVmResponse(any(ResponseObject.ResponseView.class), anyString(), any(UserVm.class))).thenReturn(userVmResponses);
        when(vmDao.findById(virtualMachineId)).thenReturn(virtualMachine);
        when(virtualMachine.getState()).thenReturn(VirtualMachine.State.Running);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        CallContext.unregister();
    }

    @Test
    public void listUnmanagedInstancesTest() {
        testListUnmanagedInstancesTest(Hypervisor.HypervisorType.VMware);
        testListUnmanagedInstancesTest(Hypervisor.HypervisorType.KVM);
    }

    private void testListUnmanagedInstancesTest(Hypervisor.HypervisorType hypervisorType) {
        clusterVO.setHypervisorType(hypervisorType.toString());
        when(virtualMachine.getHypervisorType()).thenReturn(hypervisorType);
        when(userVm.getHypervisorType()).thenReturn(hypervisorType);
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
        testListUnmanagedInstancesInvalidCallerTest(Hypervisor.HypervisorType.VMware);
        testListUnmanagedInstancesInvalidCallerTest(Hypervisor.HypervisorType.KVM);
    }

    private void testListUnmanagedInstancesInvalidCallerTest(Hypervisor.HypervisorType hypervisorType) {
        CallContext.unregister();
        clusterVO.setHypervisorType(hypervisorType.toString());
        AccountVO account = new AccountVO("user", 1L, "", Account.Type.NORMAL, "uuid");
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);
        ListUnmanagedInstancesCmd cmd = Mockito.mock(ListUnmanagedInstancesCmd.class);
        unmanagedVMsManager.listUnmanagedInstances(cmd);
    }

    @Test
    public void importUnmanagedInstanceTest() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        testImportUnmanagedInstanceTest(Hypervisor.HypervisorType.VMware);
        testImportUnmanagedInstanceTest(Hypervisor.HypervisorType.KVM);
    }

    private void testImportUnmanagedInstanceTest(Hypervisor.HypervisorType hypervisorType) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        clusterVO.setHypervisorType(hypervisorType.toString());
        when(virtualMachine.getHypervisorType()).thenReturn(hypervisorType);
        when(userVm.getHypervisorType()).thenReturn(hypervisorType);
        ImportUnmanagedInstanceCmd importUnmanageInstanceCmd = Mockito.mock(ImportUnmanagedInstanceCmd.class);
        when(importUnmanageInstanceCmd.getName()).thenReturn("TestInstance");
        when(importUnmanageInstanceCmd.getDomainId()).thenReturn(null);
        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class);
             MockedStatic<ActionEventUtils> ignored2 = Mockito.mockStatic(ActionEventUtils.class)) {
            unmanagedVMsManager.importUnmanagedInstance(importUnmanageInstanceCmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void importUnmanagedInstanceInvalidHostnameTest() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        testImportUnmanagedInstanceInvalidHostnameTest(Hypervisor.HypervisorType.VMware);
        testImportUnmanagedInstanceInvalidHostnameTest(Hypervisor.HypervisorType.KVM);
    }

    private void testImportUnmanagedInstanceInvalidHostnameTest(Hypervisor.HypervisorType hypervisorType) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        clusterVO.setHypervisorType(hypervisorType.toString());
        ImportUnmanagedInstanceCmd importUnmanageInstanceCmd = Mockito.mock(ImportUnmanagedInstanceCmd.class);
        when(importUnmanageInstanceCmd.getName()).thenReturn("TestInstance");
        when(importUnmanageInstanceCmd.getName()).thenReturn("some name");
        unmanagedVMsManager.importUnmanagedInstance(importUnmanageInstanceCmd);
    }

    @Test(expected = ServerApiException.class)
    public void importUnmanagedInstanceMissingInstanceTest() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        testImportUnmanagedInstanceMissingInstanceTest(Hypervisor.HypervisorType.VMware);
        testImportUnmanagedInstanceMissingInstanceTest(Hypervisor.HypervisorType.KVM);
    }

    private void testImportUnmanagedInstanceMissingInstanceTest(Hypervisor.HypervisorType hypervisorType) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        clusterVO.setHypervisorType(hypervisorType.toString());
        ImportUnmanagedInstanceCmd importUnmanageInstanceCmd = Mockito.mock(ImportUnmanagedInstanceCmd.class);
        when(importUnmanageInstanceCmd.getName()).thenReturn("SomeInstance");
        when(importUnmanageInstanceCmd.getDomainId()).thenReturn(null);
        try (MockedStatic<ActionEventUtils> ignored2 = Mockito.mockStatic(ActionEventUtils.class)) {
            unmanagedVMsManager.importUnmanagedInstance(importUnmanageInstanceCmd);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void unmanageVMInstanceMissingInstanceTest() {
        testUnmanageVMInstanceMissingInstanceTest(Hypervisor.HypervisorType.VMware);
        testUnmanageVMInstanceMissingInstanceTest(Hypervisor.HypervisorType.KVM);

    }

    private void testUnmanageVMInstanceMissingInstanceTest(Hypervisor.HypervisorType hypervisorType) {
        clusterVO.setHypervisorType(hypervisorType.toString());
        long notExistingId = 10L;
        unmanagedVMsManager.unmanageVMInstance(notExistingId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void unmanageVMInstanceDestroyedInstanceTest() {
        testUnmanageVMInstanceDestroyedInstanceTest(Hypervisor.HypervisorType.VMware);
        testUnmanageVMInstanceDestroyedInstanceTest(Hypervisor.HypervisorType.KVM);
    }

    private void testUnmanageVMInstanceDestroyedInstanceTest(Hypervisor.HypervisorType hypervisorType){
        clusterVO.setHypervisorType(hypervisorType.toString());
        when(virtualMachine.getState()).thenReturn(VirtualMachine.State.Destroyed);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void unmanageVMInstanceExpungedInstanceTest() {
        testUnmanageVMInstanceExpungedInstanceTest(Hypervisor.HypervisorType.VMware);
        testUnmanageVMInstanceExpungedInstanceTest(Hypervisor.HypervisorType.KVM);
    }

    private void testUnmanageVMInstanceExpungedInstanceTest(Hypervisor.HypervisorType hypervisorType){
        clusterVO.setHypervisorType(hypervisorType.toString());
        when(virtualMachine.getState()).thenReturn(VirtualMachine.State.Expunging);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = UnsupportedServiceException.class)
    public void unmanageVMInstanceExistingVMSnapshotsTest() {
        testUnmanageVMInstanceExistingVMSnapshotsTest(Hypervisor.HypervisorType.VMware);
        testUnmanageVMInstanceExistingVMSnapshotsTest(Hypervisor.HypervisorType.KVM);
    }

    private void testUnmanageVMInstanceExistingVMSnapshotsTest(Hypervisor.HypervisorType hypervisorType) {
        clusterVO.setHypervisorType(hypervisorType.toString());
        when(virtualMachine.getHypervisorType()).thenReturn(hypervisorType);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = UnsupportedServiceException.class)
    public void unmanageVMInstanceExistingVolumeSnapshotsTest() {
        testUnmanageVMInstanceExistingVolumeSnapshotsTest(Hypervisor.HypervisorType.VMware);
        testUnmanageVMInstanceExistingVolumeSnapshotsTest(Hypervisor.HypervisorType.KVM);
    }

    private void testUnmanageVMInstanceExistingVolumeSnapshotsTest(Hypervisor.HypervisorType hypervisorType){
        clusterVO.setHypervisorType(hypervisorType.toString());
        when(virtualMachine.getHypervisorType()).thenReturn(hypervisorType);
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = UnsupportedServiceException.class)
    public void unmanageVMInstanceExistingISOAttachedTest() {
        testUnmanageVMInstanceExistingISOAttachedTest(Hypervisor.HypervisorType.VMware);
        testUnmanageVMInstanceExistingISOAttachedTest(Hypervisor.HypervisorType.KVM);
    }

    private void testUnmanageVMInstanceExistingISOAttachedTest(Hypervisor.HypervisorType hypervisorType) {
        clusterVO.setHypervisorType(hypervisorType.toString());
        when(virtualMachine.getHypervisorType()).thenReturn(hypervisorType);
        UserVmVO userVmVO = mock(UserVmVO.class);
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
        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
                unmanagedVMsManager.importVm(cmd);
        }
    }
}
