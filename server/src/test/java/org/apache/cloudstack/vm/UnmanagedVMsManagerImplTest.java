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
import com.cloud.agent.api.GetUnmanagedInstancesAnswer;
import com.cloud.agent.api.GetUnmanagedInstancesCommand;
import com.cloud.configuration.Resource;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
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
import org.apache.cloudstack.api.command.admin.vm.ListUnmanagedInstancesCmd;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnmanagedVMsManagerImplTest {

    @InjectMocks
    private UnmanagedVMsManager unmanagedVMsManager = new UnmanagedVMsManagerImpl();

    @Mock
    private UserVmManager userVmManager;
    @Mock
    private ClusterDao clusterDao;
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

        ClusterVO clusterVO = new ClusterVO(1L, 1L, "Cluster");
        clusterVO.setHypervisorType(Hypervisor.HypervisorType.VMware.toString());
        when(clusterDao.findById(Mockito.anyLong())).thenReturn(clusterVO);
        when(configurationDao.getValue(Mockito.anyString())).thenReturn(null);
        doNothing().when(resourceLimitService).checkResourceLimit(Mockito.any(Account.class), Mockito.any(Resource.ResourceType.class), Mockito.anyLong());
        List<HostVO> hosts = new ArrayList<>();
        HostVO hostVO = Mockito.mock(HostVO.class);
        when(hostVO.isInMaintenanceStates()).thenReturn(false);
        hosts.add(hostVO);
        when(hostVO.checkHostServiceOfferingTags(Mockito.any())).thenReturn(true);
        when(resourceManager.listHostsInClusterByStatus(Mockito.anyLong(), Mockito.any(Status.class))).thenReturn(hosts);
        List<VMTemplateStoragePoolVO> templates = new ArrayList<>();
        when(templatePoolDao.listAll()).thenReturn(templates);
        List<VolumeVO> volumes = new ArrayList<>();
        when(volumeDao.findIncludingRemovedByZone(Mockito.anyLong())).thenReturn(volumes);
        GetUnmanagedInstancesCommand cmd = Mockito.mock(GetUnmanagedInstancesCommand.class);
        HashMap<String, UnmanagedInstanceTO> map = new HashMap<>();
        map.put(instance.getName(), instance);
        Answer answer = new GetUnmanagedInstancesAnswer(cmd, "", map);
        when(agentManager.easySend(Mockito.anyLong(), Mockito.any(GetUnmanagedInstancesCommand.class))).thenReturn(answer);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        when(zone.getId()).thenReturn(1L);
        when(dataCenterDao.findById(Mockito.anyLong())).thenReturn(zone);
        when(accountService.getActiveAccountById(Mockito.anyLong())).thenReturn(Mockito.mock(Account.class));
        List<UserVO> users = new ArrayList<>();
        users.add(Mockito.mock(UserVO.class));
        when(userDao.listByAccount(Mockito.anyLong())).thenReturn(users);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(template.getName()).thenReturn("Template");
        when(templateDao.findById(Mockito.anyLong())).thenReturn(template);
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        when(serviceOffering.getId()).thenReturn(1L);
        when(serviceOffering.isDynamic()).thenReturn(false);
        when(serviceOffering.getCpu()).thenReturn(instance.getCpuCores());
        when(serviceOffering.getRamSize()).thenReturn(instance.getMemory());
        when(serviceOffering.getSpeed()).thenReturn(instance.getCpuSpeed());
        when(serviceOfferingDao.findById(Mockito.anyLong())).thenReturn(serviceOffering);
        DiskOfferingVO diskOfferingVO = Mockito.mock(DiskOfferingVO.class);
        when(diskOfferingDao.findById(Mockito.anyLong())).thenReturn(diskOfferingVO);
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
                nullable(String.class), nullable(Hypervisor.HypervisorType.class), nullable(Map.class), nullable(VirtualMachine.PowerState.class))).thenReturn(userVm);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.L2);
        when(networkVO.getBroadcastUri()).thenReturn(URI.create(String.format("vlan://%d", instanceNic.getVlan())));
        when(networkVO.getDataCenterId()).thenReturn(1L);
        when(networkDao.findById(Mockito.anyLong())).thenReturn(networkVO);
        List<NetworkVO> networks = new ArrayList<>();
        networks.add(networkVO);
        when(networkDao.listByZone(Mockito.anyLong())).thenReturn(networks);
        doNothing().when(networkModel).checkNetworkPermissions(Mockito.any(Account.class), Mockito.any(Network.class));
        NicProfile profile = Mockito.mock(NicProfile.class);
        Integer deviceId = 100;
        Pair<NicProfile, Integer> pair = new Pair<NicProfile, Integer>(profile, deviceId);
        when(networkOrchestrationService.importNic(nullable(String.class), nullable(Integer.class), nullable(Network.class), nullable(Boolean.class), nullable(VirtualMachine.class), nullable(Network.IpAddresses.class), Mockito.anyBoolean())).thenReturn(pair);
        when(volumeDao.findByInstance(Mockito.anyLong())).thenReturn(volumes);
        List<UserVmResponse> userVmResponses = new ArrayList<>();
        UserVmResponse userVmResponse = new UserVmResponse();
        userVmResponse.setInstanceName(instance.getName());
        userVmResponses.add(userVmResponse);
        when(responseGenerator.createUserVmResponse(Mockito.any(ResponseObject.ResponseView.class), Mockito.anyString(), Mockito.any(UserVm.class))).thenReturn(userVmResponses);

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
        ListUnmanagedInstancesCmd cmd = Mockito.mock(ListUnmanagedInstancesCmd.class);
        unmanagedVMsManager.listUnmanagedInstances(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void listUnmanagedInstancesInvalidHypervisorTest() {
        ListUnmanagedInstancesCmd cmd = Mockito.mock(ListUnmanagedInstancesCmd.class);
        ClusterVO cluster = new ClusterVO(1, 1, "Cluster");
        cluster.setHypervisorType(Hypervisor.HypervisorType.KVM.toString());
        when(clusterDao.findById(Mockito.anyLong())).thenReturn(cluster);
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
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = UnsupportedServiceException.class)
    public void unmanageVMInstanceExistingVolumeSnapshotsTest() {
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }

    @Test(expected = UnsupportedServiceException.class)
    public void unmanageVMInstanceExistingISOAttachedTest() {
        unmanagedVMsManager.unmanageVMInstance(virtualMachineId);
    }
}
