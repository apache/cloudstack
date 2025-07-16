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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.storage.StorageManager;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.BaseCmd.HTTPMethod;
import org.apache.cloudstack.api.command.admin.vm.AssignVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVnfApplianceCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMUserDataCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.template.VnfTemplateManager;
import org.apache.cloudstack.userdata.UserDataManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.query.dao.ServiceOfferingJoinDao;
import com.cloud.api.query.vo.ServiceOfferingJoinVO;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.server.ManagementService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserData;
import com.cloud.user.UserDataVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserDataDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionProxyObject;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

import org.apache.cloudstack.vm.lease.VMLeaseManager;

import org.mockito.MockedStatic;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;

@RunWith(MockitoJUnitRunner.class)
public class UserVmManagerImplTest {

    @Spy
    @InjectMocks
    private UserVmManagerImpl userVmManagerImpl = new UserVmManagerImpl();

    @Mock
    private ServiceOfferingDao _serviceOfferingDao;

    @Mock
    private DiskOfferingDao diskOfferingDao;

    @Mock
    private DataCenterDao _dcDao;

    @Mock
    private DataCenterVO _dcMock;

    @Mock
    protected NicDao nicDao;

    @Mock
    private NetworkDao _networkDao;

    @Mock
    private NetworkOrchestrationService _networkMgr;

    @Mock
    private NetworkVO networkMock;

    @Mock
    private GuestOSDao guestOSDao;

    @Mock
    private UserVmDao userVmDao;

    @Mock
    private UpdateVMCmd updateVmCommand;

    @Mock
    private AccountManager accountManager;

    @Mock
    private AccountService accountService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private UserVmDetailsDao userVmDetailsDao;

    @Mock
    private UserVmVO userVmVoMock;

    @Mock
    private NetworkModel networkModel;

    @Mock
    private Account accountMock;

    @Mock
    private AccountVO callerAccount;

    @Mock
    private UserVO callerUser;

    @Mock
    private VMTemplateDao templateDao;

    @Mock
    private AccountDao accountDao;

    @Mock
    private UserDao userDao;

    @Mock
    ResourceLimitService resourceLimitMgr;

    @Mock
    VolumeApiService volumeApiService;

    @Mock
    UserDataDao userDataDao;

    @Mock
    PrimaryDataStoreDao primaryDataStoreDao;

    @Mock
    VirtualMachineManager virtualMachineManager;

    @Mock
    DeploymentPlanningManager planningManager;

    @Mock
    HostDao hostDao;

    @Mock
    private VolumeVO volumeVOMock;

    @Mock
    private VolumeDao volumeDaoMock;

    @Mock
    private SnapshotDao snapshotDaoMock;

    @Mock
    private VMSnapshotDao vmSnapshotDaoMock;

    @Mock
    AccountVO account;

    @Mock
    VMTemplateVO vmTemplateVoMock;

    @Mock
    ManagementService managementServiceMock;

    @Mock
    AssignVMCmd assignVmCmdMock;

    @Mock
    PortForwardingRulesDao portForwardingRulesDaoMock;

    @Mock
    List<PortForwardingRule> portForwardingRulesListMock;

    @Mock
    FirewallRulesDao firewallRulesDaoMock;

    @Mock
    List<FirewallRuleVO> firewallRuleVoListMock;

    @Mock
    LoadBalancerVMMapDao loadBalancerVmMapDaoMock;

    @Mock
    List<LoadBalancerVMMapVO> loadBalancerVmMapVoListMock;

    @Mock
    IPAddressDao ipAddressDaoMock;

    @Mock
    IPAddressVO ipAddressVoMock;

    @Mock
    VirtualMachineTemplate virtualMachineTemplateMock;

    @Mock
    VirtualMachineProfileImpl virtualMachineProfileMock;

    @Mock
    List<NetworkVO> networkVoListMock;

    @Mock
    SecurityGroupManager securityGroupManagerMock;

    @Mock
    NetworkOfferingDao networkOfferingDaoMock;

    @Mock
    NetworkOfferingVO networkOfferingVoMock;

    @Mock
    List<NetworkOfferingVO> networkOfferingVoListMock;

    @Mock
    PhysicalNetworkDao physicalNetworkDaoMock;

    @Mock
    SecurityGroupVO securityGroupVoMock;

    @Mock
    DomainDao domainDaoMock;

    @Mock
    DomainVO domainVoMock;

    @Mock
    SnapshotVO snapshotVoMock;

    @Mock
    ServiceOfferingVO serviceOfferingVoMock;

    @Mock
    private ServiceOfferingVO serviceOffering;

    @Mock
    UserDataManager userDataManager;

    @Mock
    VirtualMachineProfile virtualMachineProfile;

    @Mock
    VirtualMachineTemplate templateMock;

    @Mock
    VnfTemplateManager vnfTemplateManager;

    @Mock
    ServiceOfferingJoinDao serviceOfferingJoinDao;

    @Mock
    private VMInstanceVO vmInstanceMock;

    @Mock
    StorageManager storageManager;

    @Mock
    private VolumeDataFactory volumeDataFactory;

    @Mock
    private VolumeInfo volumeInfo;

    @Mock
    private SnapshotVO snapshotMock;

    @Mock
    private PrimaryDataStore primaryDataStore;

    @Mock
    private Scope scopeMock;

    private static final long vmId = 1l;
    private static final long zoneId = 2L;
    private static final long accountId = 3L;
    private static final long serviceOfferingId = 10L;
    private static final long templateId = 11L;
    private static final long volumeId = 1L;
    private static final long snashotId = 1L;

    private static final long GiB_TO_BYTES = 1024 * 1024 * 1024;

    private Map<String, String> customParameters = new HashMap<>();

    String[] detailsConstants = {VmDetailConstants.MEMORY, VmDetailConstants.CPU_NUMBER, VmDetailConstants.CPU_SPEED};

    private DiskOfferingVO smallerDisdkOffering = prepareDiskOffering(5l * GiB_TO_BYTES, 1l, 1L, 2L);
    private DiskOfferingVO largerDisdkOffering = prepareDiskOffering(10l * GiB_TO_BYTES, 2l, 10L, 20L);
    Class<InvalidParameterValueException> expectedInvalidParameterValueException = InvalidParameterValueException.class;
    Class<CloudRuntimeException> expectedCloudRuntimeException = CloudRuntimeException.class;

    @Before
    public void beforeTest() {
        userVmManagerImpl.resourceLimitService = resourceLimitMgr;

        Mockito.when(updateVmCommand.getId()).thenReturn(vmId);

        when(_dcDao.findById(anyLong())).thenReturn(_dcMock);

        Mockito.when(userVmDao.findById(vmId)).thenReturn(userVmVoMock);

        Mockito.when(callerAccount.getType()).thenReturn(Account.Type.ADMIN);
        CallContext.register(callerUser, callerAccount);

        customParameters.put(VmDetailConstants.ROOT_DISK_SIZE, "123");
        customParameters.put(VmDetailConstants.MEMORY, "2048");
        customParameters.put(VmDetailConstants.CPU_NUMBER, "4");
        customParameters.put(VmDetailConstants.CPU_SPEED, "1000");

        lenient().doNothing().when(resourceLimitMgr).incrementResourceCount(anyLong(), any(Resource.ResourceType.class));
        lenient().doNothing().when(resourceLimitMgr).decrementResourceCount(anyLong(), any(Resource.ResourceType.class), anyLong());

        Mockito.when(virtualMachineProfile.getId()).thenReturn(vmId);
    }

    @After
    public void afterTest() {
        CallContext.unregister();
    }

    @Test
    public void validateGuestOsIdForUpdateVirtualMachineCommandTestOsTypeNull() {
        Mockito.when(updateVmCommand.getOsTypeId()).thenReturn(null);
        userVmManagerImpl.validateGuestOsIdForUpdateVirtualMachineCommand(updateVmCommand);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateGuestOsIdForUpdateVirtualMachineCommandTestOsTypeNotFound() {
        Mockito.when(updateVmCommand.getOsTypeId()).thenReturn(1l);

        userVmManagerImpl.validateGuestOsIdForUpdateVirtualMachineCommand(updateVmCommand);
    }

    @Test
    public void validateGuestOsIdForUpdateVirtualMachineCommandTestOsTypeFound() {
        Mockito.when(updateVmCommand.getOsTypeId()).thenReturn(1l);
        Mockito.when(guestOSDao.findById(1l)).thenReturn(Mockito.mock(GuestOSVO.class));

        userVmManagerImpl.validateGuestOsIdForUpdateVirtualMachineCommand(updateVmCommand);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateInputsAndPermissionForUpdateVirtualMachineCommandTestVmNotFound() {
        Mockito.when(userVmDao.findById(vmId)).thenReturn(null);

        userVmManagerImpl.validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);
    }

    private ServiceOfferingVO getSvcoffering(int ramSize) {
        String name = "name";
        String displayText = "displayText";
        int cpu = 1;
        int speed = 128;

        boolean ha = false;
        boolean useLocalStorage = false;

        ServiceOfferingVO serviceOffering = new ServiceOfferingVO(name, cpu, ramSize, speed, null, null, ha, displayText, false, null,
                false);
        serviceOffering.setDiskOfferingId(1l);
        return serviceOffering;
    }

    @Test
    public void validateInputsAndPermissionForUpdateVirtualMachineCommandTest() {
        Mockito.doNothing().when(userVmManagerImpl).validateGuestOsIdForUpdateVirtualMachineCommand(updateVmCommand);

        CallContext callContextMock = Mockito.mock(CallContext.class);

        Mockito.lenient().doReturn(accountMock).when(callContextMock).getCallingAccount();

        ServiceOffering offering = getSvcoffering(512);
        Mockito.lenient().when(_serviceOfferingDao.findById(Mockito.anyLong(), Mockito.anyLong())).thenReturn((ServiceOfferingVO) offering);
        Mockito.lenient().doNothing().when(accountManager).checkAccess(accountMock, null, true, userVmVoMock);
        userVmManagerImpl.validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);

        Mockito.verify(userVmManagerImpl).validateGuestOsIdForUpdateVirtualMachineCommand(updateVmCommand);
        Mockito.verify(accountManager).checkAccess(callerAccount, null, true, userVmVoMock);
    }

    @Test
    public void updateVirtualMachineTestDisplayChanged() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        configureDoNothingForMethodsThatWeDoNotWantToTest();
        ServiceOffering offering = getSvcoffering(512);
        Mockito.when(_serviceOfferingDao.findById(Mockito.anyLong(), Mockito.anyLong())).thenReturn((ServiceOfferingVO) offering);
        Mockito.when(userVmVoMock.isDisplay()).thenReturn(true);
        Mockito.doNothing().when(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);
        Mockito.when(updateVmCommand.getUserdataId()).thenReturn(null);
        userVmManagerImpl.updateVirtualMachine(updateVmCommand);
        verifyMethodsThatAreAlwaysExecuted();

        Mockito.verify(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);
        Mockito.verify(userVmDetailsDao, times(0)).removeDetail(anyLong(), anyString());
    }

    @Test
    public void updateVirtualMachineTestCleanUpTrue() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        configureDoNothingForMethodsThatWeDoNotWantToTest();
        ServiceOffering offering = getSvcoffering(512);
        Mockito.when(_serviceOfferingDao.findById(Mockito.anyLong(), Mockito.anyLong())).thenReturn((ServiceOfferingVO) offering);
        Mockito.when(updateVmCommand.isCleanupDetails()).thenReturn(true);
        Mockito.lenient().doNothing().when(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);

        Mockito.when(updateVmCommand.getUserdataId()).thenReturn(null);

        prepareExistingDetails(vmId, "userdetail");

        userVmManagerImpl.updateVirtualMachine(updateVmCommand);
        verifyMethodsThatAreAlwaysExecuted();
        Mockito.verify(userVmDetailsDao).removeDetail(vmId, "userdetail");
        Mockito.verify(userVmDetailsDao, times(0)).removeDetail(vmId, "systemdetail");
        Mockito.verify(userVmManagerImpl, times(0)).updateDisplayVmFlag(false, vmId, userVmVoMock);
    }

    @Test
    public void updateVirtualMachineTestCleanUpTrueAndDetailEmpty() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        prepareAndExecuteMethodDealingWithDetails(true, true);
    }

    @Test
    public void updateVirtualMachineTestCleanUpTrueAndDetailsNotEmpty() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        prepareAndExecuteMethodDealingWithDetails(true, false);
    }

    @Test
    public void updateVirtualMachineTestCleanUpFalseAndDetailsNotEmpty() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        prepareAndExecuteMethodDealingWithDetails(false, true);
    }

    @Test
    public void updateVirtualMachineTestCleanUpFalseAndDetailsEmpty() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        Mockito.doNothing().when(userVmManagerImpl).verifyVmLimits(Mockito.any(), Mockito.anyMap());
        prepareAndExecuteMethodDealingWithDetails(false, false);
    }

    private List<UserVmDetailVO> prepareExistingDetails(Long vmId, String... existingDetailKeys) {
        List<UserVmDetailVO> existingDetails = new ArrayList<>();
        for (String detail : existingDetailKeys) {
            existingDetails.add(new UserVmDetailVO(vmId, detail, "foo", true));
        }
        existingDetails.add(new UserVmDetailVO(vmId, "systemdetail", "bar", false));
        Mockito.when(userVmDetailsDao.listDetails(vmId)).thenReturn(existingDetails);
        return existingDetails;
    }

    private void prepareAndExecuteMethodDealingWithDetails(boolean cleanUpDetails, boolean isDetailsEmpty) throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        configureDoNothingForMethodsThatWeDoNotWantToTest();

        ServiceOffering offering = getSvcoffering(512);
        Mockito.when(_serviceOfferingDao.findById(Mockito.anyLong(), Mockito.anyLong())).thenReturn((ServiceOfferingVO) offering);
        ServiceOfferingVO currentServiceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.lenient().when(currentServiceOffering.getCpu()).thenReturn(1);
        Mockito.lenient().when(currentServiceOffering.getRamSize()).thenReturn(512);

        List<NicVO> nics = new ArrayList<>();
        NicVO nic1 = mock(NicVO.class);
        NicVO nic2 = mock(NicVO.class);
        nics.add(nic1);
        nics.add(nic2);
        when(this.nicDao.listByVmId(Mockito.anyLong())).thenReturn(nics);
        when(_networkDao.findById(anyLong())).thenReturn(networkMock);
        lenient().doNothing().when(_networkMgr).saveExtraDhcpOptions(anyString(), anyLong(), anyMap());
        HashMap<String, String> details = new HashMap<>();
        if(!isDetailsEmpty) {
            details.put("newdetail", "foo");
        }
        prepareExistingDetails(vmId, "existingdetail");
        Mockito.when(updateVmCommand.getUserdataId()).thenReturn(null);
        Mockito.when(updateVmCommand.getDetails()).thenReturn(details);
        Mockito.when(updateVmCommand.isCleanupDetails()).thenReturn(cleanUpDetails);
        configureDoNothingForDetailsMethod();

        userVmManagerImpl.updateVirtualMachine(updateVmCommand);
        verifyMethodsThatAreAlwaysExecuted();

        Mockito.verify(userVmVoMock, times(cleanUpDetails || isDetailsEmpty ? 0 : 1)).setDetails(details);
        Mockito.verify(userVmDetailsDao, times(cleanUpDetails ? 1 : 0)).removeDetail(vmId, "existingdetail");
        Mockito.verify(userVmDetailsDao, times(0)).removeDetail(vmId, "systemdetail");
        Mockito.verify(userVmDao, times(cleanUpDetails || isDetailsEmpty ? 0 : 1)).saveDetails(userVmVoMock);
        Mockito.verify(userVmManagerImpl, times(0)).updateDisplayVmFlag(false, vmId, userVmVoMock);
    }

    private void configureDoNothingForDetailsMethod() {
        Mockito.lenient().doNothing().when(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);
        Mockito.doNothing().when(userVmDetailsDao).removeDetail(anyLong(), anyString());
        Mockito.doNothing().when(userVmDao).saveDetails(userVmVoMock);
    }

    @SuppressWarnings("unchecked")
    private void verifyMethodsThatAreAlwaysExecuted() throws ResourceUnavailableException, InsufficientCapacityException {
        Mockito.verify(userVmManagerImpl).validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);
        Mockito.verify(userVmManagerImpl).getSecurityGroupIdList(updateVmCommand);

        Mockito.verify(userVmManagerImpl).updateVirtualMachine(nullable(Long.class), nullable(String.class), nullable(String.class), nullable(Boolean.class),
                nullable(Boolean.class), nullable(Boolean.class), nullable(Long.class),
                nullable(String.class), nullable(Long.class), nullable(String.class), nullable(Boolean.class), nullable(HTTPMethod.class), nullable(String.class), nullable(String.class), nullable(String.class), nullable(List.class),
                nullable(Map.class));

    }

    @SuppressWarnings("unchecked")
    private void configureDoNothingForMethodsThatWeDoNotWantToTest() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        Mockito.doNothing().when(userVmManagerImpl).validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);
        Mockito.doReturn(new ArrayList<Long>()).when(userVmManagerImpl).getSecurityGroupIdList(updateVmCommand);

        Mockito.lenient().doReturn(Mockito.mock(UserVm.class)).when(userVmManagerImpl).updateVirtualMachine(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(HTTPMethod.class), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyList(), Mockito.any());

        Mockito.doNothing().when(userVmManagerImpl).validateIfVmSupportsMigration(Mockito.any(), Mockito.anyLong());
        Mockito.doNothing().when(userVmManagerImpl).validateOldAndNewAccounts(Mockito.nullable(Account.class), Mockito.nullable(Account.class), Mockito.anyLong(), Mockito.nullable(String.class), Mockito.nullable(Long.class));
        Mockito.doNothing().when(userVmManagerImpl).validateIfVmHasNoRules(Mockito.any(), Mockito.anyLong());
        Mockito.doNothing().when(userVmManagerImpl).removeInstanceFromInstanceGroup(Mockito.anyLong());
        Mockito.doNothing().when(userVmManagerImpl).verifyResourceLimitsForAccountAndStorage(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.any());
        Mockito.doNothing().when(userVmManagerImpl).validateIfNewOwnerHasAccessToTemplate(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doNothing().when(userVmManagerImpl).updateVmOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(userVmManagerImpl).updateVolumesOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(userVmManagerImpl).updateVmNetwork(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doNothing().when(userVmManagerImpl).resourceCountIncrement(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void validateOrReplaceMacAddressTestMacAddressValid() throws InsufficientAddressCapacityException {
        configureValidateOrReplaceMacAddressTest(0, "01:23:45:67:89:ab", "01:23:45:67:89:ab");
    }

    @Test
    public void validateOrReplaceMacAddressTestMacAddressNull() throws InsufficientAddressCapacityException {
        configureValidateOrReplaceMacAddressTest(1, null, "01:23:45:67:89:ab");
    }

    @Test
    public void validateOrReplaceMacAddressTestMacAddressBlank() throws InsufficientAddressCapacityException {
        configureValidateOrReplaceMacAddressTest(1, " ", "01:23:45:67:89:ab");
    }

    @Test
    public void validateOrReplaceMacAddressTestMacAddressEmpty() throws InsufficientAddressCapacityException {
        configureValidateOrReplaceMacAddressTest(1, "", "01:23:45:67:89:ab");
    }

    @Test
    public void validateOrReplaceMacAddressTestMacAddressNotValidOption1() throws InsufficientAddressCapacityException {
        configureValidateOrReplaceMacAddressTest(1, "abcdef:gh:ij:kl", "01:23:45:67:89:ab");
    }

    @Test
    public void validateOrReplaceMacAddressTestMacAddressNotValidOption2() throws InsufficientAddressCapacityException {
        configureValidateOrReplaceMacAddressTest(1, "01:23:45:67:89:", "01:23:45:67:89:ab");
    }

    @Test
    public void validateOrReplaceMacAddressTestMacAddressNotValidOption3() throws InsufficientAddressCapacityException {
        configureValidateOrReplaceMacAddressTest(1, "01:23:45:67:89:az", "01:23:45:67:89:ab");
    }

    @Test
    public void validateOrReplaceMacAddressTestMacAddressNotValidOption4() throws InsufficientAddressCapacityException {
        configureValidateOrReplaceMacAddressTest(1, "@1:23:45:67:89:ab", "01:23:45:67:89:ab");
    }

    private void configureValidateOrReplaceMacAddressTest(int times, String macAddress, String expectedMacAddress) throws InsufficientAddressCapacityException {
        Mockito.when(networkModel.getNextAvailableMacAddressInNetwork(Mockito.anyLong())).thenReturn(expectedMacAddress);

        String returnedMacAddress = userVmManagerImpl.validateOrReplaceMacAddress(macAddress, networkMock);

        Mockito.verify(networkModel, times(times)).getNextAvailableMacAddressInNetwork(Mockito.anyLong());
        assertEquals(expectedMacAddress, returnedMacAddress);
    }

    @Test
    public void testValidatekeyValuePair() throws Exception {
        assertTrue(userVmManagerImpl.isValidKeyValuePair("is-a-template=true\nHVM-boot-policy=\nPV-bootloader=pygrub\nPV-args=hvc0"));
        assertTrue(userVmManagerImpl.isValidKeyValuePair("is-a-template=true HVM-boot-policy= PV-bootloader=pygrub PV-args=hvc0"));
        assertTrue(userVmManagerImpl.isValidKeyValuePair("nvp.vm-uuid=34b3d5ea-1c25-4bb0-9250-8dc3388bfa9b"));
        assertFalse(userVmManagerImpl.isValidKeyValuePair("key"));
        //key-1=value1, param:key-2=value2, my.config.v0=False"
        assertTrue(userVmManagerImpl.isValidKeyValuePair("key-1=value1"));
        assertTrue(userVmManagerImpl.isValidKeyValuePair("param:key-2=value2"));
        assertTrue(userVmManagerImpl.isValidKeyValuePair("my.config.v0=False"));
    }

    @Test
    public void configureCustomRootDiskSizeTest() {
        String vmDetailsRootDiskSize = "123";
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(VmDetailConstants.ROOT_DISK_SIZE, vmDetailsRootDiskSize);
        long expectedRootDiskSize = 123l * GiB_TO_BYTES;
        long offeringRootDiskSize = 0l;
        prepareAndRunConfigureCustomRootDiskSizeTest(customParameters, expectedRootDiskSize, 1, offeringRootDiskSize);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void configureCustomRootDiskSizeTestExpectExceptionZero() {
        String vmDetailsRootDiskSize = "0";
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(VmDetailConstants.ROOT_DISK_SIZE, vmDetailsRootDiskSize);
        long expectedRootDiskSize = 0l;
        long offeringRootDiskSize = 0l;
        prepareAndRunConfigureCustomRootDiskSizeTest(customParameters, expectedRootDiskSize, 1, offeringRootDiskSize);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void configureCustomRootDiskSizeTestExpectExceptionNegativeNum() {
        String vmDetailsRootDiskSize = "-123";
        Map<String, String> customParameters = new HashMap<>();
        customParameters.put(VmDetailConstants.ROOT_DISK_SIZE, vmDetailsRootDiskSize);
        long expectedRootDiskSize = -123l * GiB_TO_BYTES;
        long offeringRootDiskSize = 0l;
        prepareAndRunConfigureCustomRootDiskSizeTest(customParameters, expectedRootDiskSize, 1, offeringRootDiskSize);
    }

    @Test
    public void configureCustomRootDiskSizeTestEmptyParameters() {
        Map<String, String> customParameters = new HashMap<>();
        long expectedRootDiskSize = 99l * GiB_TO_BYTES;
        long offeringRootDiskSize = 0l;
        prepareAndRunConfigureCustomRootDiskSizeTest(customParameters, expectedRootDiskSize, 1, offeringRootDiskSize);
    }

    @Test
    public void configureCustomRootDiskSizeTestEmptyParametersAndOfferingRootSize() {
        Map<String, String> customParameters = new HashMap<>();
        long expectedRootDiskSize = 10l * GiB_TO_BYTES;
        long offeringRootDiskSize = 10l * GiB_TO_BYTES;

        prepareAndRunConfigureCustomRootDiskSizeTest(customParameters, expectedRootDiskSize, 1, offeringRootDiskSize);
    }

    private void prepareAndRunConfigureCustomRootDiskSizeTest(Map<String, String> customParameters, long expectedRootDiskSize, int timesVerifyIfHypervisorSupports, Long offeringRootDiskSize) {
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.getId()).thenReturn(1l);
        Mockito.when(template.getSize()).thenReturn(99L * GiB_TO_BYTES);
        Mockito.when(templateDao.findById(Mockito.anyLong())).thenReturn(template);

        DiskOfferingVO diskfferingVo = Mockito.mock(DiskOfferingVO.class);

        Mockito.when(diskfferingVo.getDiskSize()).thenReturn(offeringRootDiskSize);

        Mockito.when(volumeApiService.validateVolumeSizeInBytes(Mockito.anyLong())).thenReturn(true);
        long rootDiskSize = userVmManagerImpl.configureCustomRootDiskSize(customParameters, template, Hypervisor.HypervisorType.KVM, diskfferingVo);

        Assert.assertEquals(expectedRootDiskSize, rootDiskSize);
        Mockito.verify(userVmManagerImpl, times(timesVerifyIfHypervisorSupports)).verifyIfHypervisorSupportsRootdiskSizeOverride(Mockito.any());
    }

    @Test
    public void verifyIfHypervisorSupportRootdiskSizeOverrideTest() {
        Hypervisor.HypervisorType[] hypervisorTypeArray = Hypervisor.HypervisorType.values();
        int exceptionCounter = 0;
        int expectedExceptionCounter = hypervisorTypeArray.length - 5;

        for(int i = 0; i < hypervisorTypeArray.length; i++) {
            if (hypervisorTypeArray[i].isFunctionalitySupported(Hypervisor.HypervisorType.Functionality.RootDiskSizeOverride)) {
                userVmManagerImpl.verifyIfHypervisorSupportsRootdiskSizeOverride(hypervisorTypeArray[i]);
            } else {
                try {
                    userVmManagerImpl.verifyIfHypervisorSupportsRootdiskSizeOverride(hypervisorTypeArray[i]);
                } catch (InvalidParameterValueException e) {
                    exceptionCounter ++;
                }
            }
        }

        Assert.assertEquals(expectedExceptionCounter, exceptionCounter);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void prepareResizeVolumeCmdTestRootVolumeNull() {
        DiskOfferingVO newRootDiskOffering = Mockito.mock(DiskOfferingVO.class);
        DiskOfferingVO currentRootDiskOffering = Mockito.mock(DiskOfferingVO.class);
        userVmManagerImpl.prepareResizeVolumeCmd(null, currentRootDiskOffering, newRootDiskOffering);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void prepareResizeVolumeCmdTestCurrentRootDiskOffering() {
        DiskOfferingVO newRootDiskOffering = Mockito.mock(DiskOfferingVO.class);
        VolumeVO rootVolumeOfVm = Mockito.mock(VolumeVO.class);
        userVmManagerImpl.prepareResizeVolumeCmd(rootVolumeOfVm, null, newRootDiskOffering);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void prepareResizeVolumeCmdTestNewRootDiskOffering() {
        VolumeVO rootVolumeOfVm = Mockito.mock(VolumeVO.class);
        DiskOfferingVO currentRootDiskOffering = Mockito.mock(DiskOfferingVO.class);
        userVmManagerImpl.prepareResizeVolumeCmd(rootVolumeOfVm, currentRootDiskOffering, null);
    }

    @Test
    public void prepareResizeVolumeCmdTestNewOfferingLarger() {
        prepareAndRunResizeVolumeTest(2L, 10L, 20L, smallerDisdkOffering, largerDisdkOffering);
    }

    @Test
    public void prepareResizeVolumeCmdTestSameOfferingSize() {
        prepareAndRunResizeVolumeTest(null, 1L, 2L, smallerDisdkOffering, smallerDisdkOffering);
    }

    @Test
    public void prepareResizeVolumeCmdTestOfferingRootSizeZero() {
        DiskOfferingVO rootSizeZero = prepareDiskOffering(0l, 3l, 100L, 200L);
        prepareAndRunResizeVolumeTest(null, 100L, 200L, smallerDisdkOffering, rootSizeZero);
    }

    @Test (expected = InvalidParameterValueException.class)
    public void prepareResizeVolumeCmdTestNewOfferingSmaller() {
        prepareAndRunResizeVolumeTest(2L, 10L, 20L, largerDisdkOffering, smallerDisdkOffering);
    }

    private void prepareAndRunResizeVolumeTest(Long expectedOfferingId, long expectedMinIops, long expectedMaxIops, DiskOfferingVO currentRootDiskOffering, DiskOfferingVO newRootDiskOffering) {
        long rootVolumeId = 1l;
        VolumeVO rootVolumeOfVm = Mockito.mock(VolumeVO.class);
        Mockito.when(rootVolumeOfVm.getId()).thenReturn(rootVolumeId);

        ResizeVolumeCmd resizeVolumeCmd = userVmManagerImpl.prepareResizeVolumeCmd(rootVolumeOfVm, currentRootDiskOffering, newRootDiskOffering);

        Assert.assertEquals(rootVolumeId, resizeVolumeCmd.getId().longValue());
        Assert.assertEquals(expectedOfferingId, resizeVolumeCmd.getNewDiskOfferingId());
        Assert.assertEquals(expectedMinIops, resizeVolumeCmd.getMinIops().longValue());
        Assert.assertEquals(expectedMaxIops, resizeVolumeCmd.getMaxIops().longValue());
    }

    private DiskOfferingVO prepareDiskOffering(long rootSize, long diskOfferingId, long offeringMinIops, long offeringMaxIops) {
        DiskOfferingVO newRootDiskOffering = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(newRootDiskOffering.getDiskSize()).thenReturn(rootSize);
        Mockito.when(newRootDiskOffering.getId()).thenReturn(diskOfferingId);
        Mockito.when(newRootDiskOffering.getMinIops()).thenReturn(offeringMinIops);
        Mockito.when(newRootDiskOffering.getMaxIops()).thenReturn(offeringMaxIops);
        return newRootDiskOffering;
    }

    @Test (expected = CloudRuntimeException.class)
    public void testUserDataDenyOverride() {
        Long userDataId = 1L;

        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        when(template.getUserDataId()).thenReturn(2L);
        when(template.getUserDataOverridePolicy()).thenReturn(UserData.UserDataOverridePolicy.DENYOVERRIDE);

        userVmManagerImpl.finalizeUserData(null, userDataId, template);
    }

    @Test
    public void testUserDataAllowOverride() {
        String templateUserData = "testTemplateUserdata";
        Long userDataId = 1L;

        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        when(template.getUserDataId()).thenReturn(2L);
        when(template.getUserDataOverridePolicy()).thenReturn(UserData.UserDataOverridePolicy.ALLOWOVERRIDE);

        UserDataVO apiUserDataVO = Mockito.mock(UserDataVO.class);
        doReturn(apiUserDataVO).when(userDataDao).findById(userDataId);
        when(apiUserDataVO.getUserData()).thenReturn(templateUserData);

        String finalUserdata = userVmManagerImpl.finalizeUserData(null, userDataId, template);

        Assert.assertEquals(finalUserdata, templateUserData);
    }

    @Test
    public void testUserDataWithoutTemplate() {
        String userData = "testUserdata";
        Long userDataId = 1L;

        UserDataVO apiUserDataVO = Mockito.mock(UserDataVO.class);
        doReturn(apiUserDataVO).when(userDataDao).findById(userDataId);
        when(apiUserDataVO.getUserData()).thenReturn(userData);

        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        when(template.getUserDataId()).thenReturn(null);

        String finalUserdata = userVmManagerImpl.finalizeUserData(null, userDataId, template);

        Assert.assertEquals(finalUserdata, userData);
    }

    @Test
    public void testUserDataAllowOverrideWithoutAPIuserdata() {
        String templateUserData = "testTemplateUserdata";

        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        when(template.getUserDataId()).thenReturn(2L);
        when(template.getUserDataOverridePolicy()).thenReturn(UserData.UserDataOverridePolicy.ALLOWOVERRIDE);
        UserDataVO templateUserDataVO = Mockito.mock(UserDataVO.class);
        doReturn(templateUserDataVO).when(userDataDao).findById(2L);
        when(templateUserDataVO.getUserData()).thenReturn(templateUserData);

        String finalUserdata = userVmManagerImpl.finalizeUserData(null, null, template);

        Assert.assertEquals(finalUserdata, templateUserData);
    }

    @Test
    public void testUserDataAllowOverrideWithUserdataText() {
        String userData = "testUserdata";
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        when(template.getUserDataId()).thenReturn(null);

        String finalUserdata = userVmManagerImpl.finalizeUserData(userData, null, template);

        Assert.assertEquals(finalUserdata, userData);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testResetVMUserDataVMStateNotStopped() {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        Mockito.lenient().doReturn(accountMock).when(callContextMock).getCallingAccount();

        ResetVMUserDataCmd cmd = Mockito.mock(ResetVMUserDataCmd.class);
        when(cmd.getId()).thenReturn(1L);
        when(userVmDao.findById(1L)).thenReturn(userVmVoMock);

        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(userVmVoMock.getTemplateId()).thenReturn(2L);
        when(templateDao.findByIdIncludingRemoved(2L)).thenReturn(template);


        when(userVmVoMock.getState()).thenReturn(VirtualMachine.State.Running);

        try {
            userVmManagerImpl.resetVMUserData(cmd);
        } catch (ResourceUnavailableException e) {
            throw new RuntimeException(e);
        } catch (InsufficientCapacityException e) {
            throw new RuntimeException(e);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testResetVMUserDataDontAcceptBothUserdataAndUserdataId() {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        Mockito.lenient().doReturn(accountMock).when(callContextMock).getCallingAccount();

        ResetVMUserDataCmd cmd = Mockito.mock(ResetVMUserDataCmd.class);
        when(cmd.getId()).thenReturn(1L);
        when(userVmDao.findById(1L)).thenReturn(userVmVoMock);

        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(userVmVoMock.getTemplateId()).thenReturn(2L);
        when(templateDao.findByIdIncludingRemoved(2L)).thenReturn(template);


        when(userVmVoMock.getState()).thenReturn(VirtualMachine.State.Stopped);

        when(cmd.getUserData()).thenReturn("testUserdata");
        when(cmd.getUserdataId()).thenReturn(1L);

        try {
            userVmManagerImpl.resetVMUserData(cmd);
        } catch (ResourceUnavailableException e) {
            throw new RuntimeException(e);
        } catch (InsufficientCapacityException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testResetVMUserDataSuccessResetWithUserdata() {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        Mockito.lenient().doReturn(accountMock).when(callContextMock).getCallingAccount();

        UserVmVO userVmVO = new UserVmVO();
        userVmVO.setTemplateId(2L);
        userVmVO.setState(VirtualMachine.State.Stopped);
        userVmVO.setUserDataId(100L);
        userVmVO.setUserData("RandomUserdata");

        ResetVMUserDataCmd cmd = Mockito.mock(ResetVMUserDataCmd.class);
        when(cmd.getId()).thenReturn(1L);
        when(userVmDao.findById(1L)).thenReturn(userVmVO);

        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(templateDao.findByIdIncludingRemoved(2L)).thenReturn(template);
        when(template.getUserDataId()).thenReturn(null);

        String testUserData = "testUserdata";
        when(cmd.getUserData()).thenReturn(testUserData);
        when(cmd.getUserdataId()).thenReturn(null);
        when(cmd.getHttpMethod()).thenReturn(HTTPMethod.GET);

        when(userDataManager.validateUserData(testUserData, HTTPMethod.GET)).thenReturn(testUserData);

        try {
            doNothing().when(userVmManagerImpl).updateUserData(userVmVO);
            userVmManagerImpl.resetVMUserData(cmd);
        } catch (ResourceUnavailableException e) {
            throw new RuntimeException(e);
        } catch (InsufficientCapacityException e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals("testUserdata", userVmVO.getUserData());
        Assert.assertEquals(null, userVmVO.getUserDataId());
    }

    @Test
    public void testResetVMUserDataSuccessResetWithUserdataId() {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        Mockito.lenient().doReturn(accountMock).when(callContextMock).getCallingAccount();

        UserVmVO userVmVO = new UserVmVO();
        userVmVO.setTemplateId(2L);
        userVmVO.setState(VirtualMachine.State.Stopped);
        userVmVO.setUserDataId(100L);
        userVmVO.setUserData("RandomUserdata");

        ResetVMUserDataCmd cmd = Mockito.mock(ResetVMUserDataCmd.class);
        when(cmd.getId()).thenReturn(1L);
        when(userVmDao.findById(1L)).thenReturn(userVmVO);

        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        when(templateDao.findByIdIncludingRemoved(2L)).thenReturn(template);
        when(template.getUserDataId()).thenReturn(null);

        String testUserData = "testUserdata";
        when(cmd.getUserdataId()).thenReturn(1L);
        UserDataVO apiUserDataVO = Mockito.mock(UserDataVO.class);
        when(userDataDao.findById(1L)).thenReturn(apiUserDataVO);
        when(apiUserDataVO.getUserData()).thenReturn(testUserData);
        when(cmd.getHttpMethod()).thenReturn(HTTPMethod.GET);

        when(userDataManager.validateUserData(testUserData, HTTPMethod.GET)).thenReturn(testUserData);

        try {
            doNothing().when(userVmManagerImpl).updateUserData(userVmVO);
            userVmManagerImpl.resetVMUserData(cmd);
        } catch (ResourceUnavailableException e) {
            throw new RuntimeException(e);
        } catch (InsufficientCapacityException e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals("testUserdata", userVmVO.getUserData());
        Assert.assertEquals(1L, (long)userVmVO.getUserDataId());
    }

    @Test
    public void recoverRootVolumeTestDestroyState() {
        Mockito.doReturn(Volume.State.Destroy).when(volumeVOMock).getState();

        userVmManagerImpl.recoverRootVolume(volumeVOMock, vmId);

        Mockito.verify(volumeApiService).recoverVolume(volumeVOMock.getId());
        Mockito.verify(volumeDaoMock).attachVolume(volumeVOMock.getId(), vmId, UserVmManagerImpl.ROOT_DEVICE_ID);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createVirtualMachineWithInactiveServiceOffering() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        DeployVMCmd deployVMCmd = new DeployVMCmd();
        ReflectionTestUtils.setField(deployVMCmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(deployVMCmd, "serviceOfferingId", serviceOfferingId);
        deployVMCmd._accountService = accountService;

        when(accountService.finalyzeAccountId(nullable(String.class), nullable(Long.class), nullable(Long.class), eq(true))).thenReturn(accountId);
        when(accountService.getActiveAccountById(accountId)).thenReturn(account);
        when(entityManager.findById(DataCenter.class, zoneId)).thenReturn(_dcMock);
        when(entityManager.findById(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOffering);
        when(serviceOffering.getState()).thenReturn(ServiceOffering.State.Inactive);

        userVmManagerImpl.createVirtualMachine(deployVMCmd);
    }

    @Test
    public void createVirtualMachine() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        DeployVMCmd deployVMCmd = new DeployVMCmd();
        ReflectionTestUtils.setField(deployVMCmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(deployVMCmd, "templateId", templateId);
        ReflectionTestUtils.setField(deployVMCmd, "serviceOfferingId", serviceOfferingId);
        deployVMCmd._accountService = accountService;

        when(accountService.finalyzeAccountId(nullable(String.class), nullable(Long.class), nullable(Long.class), eq(true))).thenReturn(accountId);
        when(accountService.getActiveAccountById(accountId)).thenReturn(account);
        when(entityManager.findById(DataCenter.class, zoneId)).thenReturn(_dcMock);
        when(entityManager.findById(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOffering);
        when(serviceOffering.getState()).thenReturn(ServiceOffering.State.Active);

        when(entityManager.findById(VirtualMachineTemplate.class, templateId)).thenReturn(templateMock);
        when(templateMock.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        when(templateMock.isDeployAsIs()).thenReturn(false);
        when(templateMock.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(templateMock.getUserDataId()).thenReturn(null);
        Mockito.doNothing().when(vnfTemplateManager).validateVnfApplianceNics(any(), nullable(List.class));

        ServiceOfferingJoinVO svcOfferingMock = Mockito.mock(ServiceOfferingJoinVO.class);
        when(serviceOfferingJoinDao.findById(anyLong())).thenReturn(svcOfferingMock);
        when(_dcMock.isLocalStorageEnabled()).thenReturn(true);
        when(_dcMock.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);
        Mockito.doReturn(userVmVoMock).when(userVmManagerImpl).createBasicSecurityGroupVirtualMachine(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), nullable(Boolean.class), any(), any(), any(),
                any(), any(), any(), any(), eq(true), any(), any(), any());

        UserVm result = userVmManagerImpl.createVirtualMachine(deployVMCmd);
        assertEquals(userVmVoMock, result);
        Mockito.verify(vnfTemplateManager).validateVnfApplianceNics(templateMock, null);
        Mockito.verify(userVmManagerImpl).createBasicSecurityGroupVirtualMachine(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), nullable(Boolean.class), any(), any(), any(),
                any(), any(), any(), any(), eq(true), any(), any(), any());
    }

    private List<VolumeVO> mockVolumesForIsAnyVmVolumeUsingLocalStorageTest(int localVolumes, int nonLocalVolumes) {
        List<VolumeVO> volumes = new ArrayList<>();
        for (int i=0; i< localVolumes + nonLocalVolumes; ++i) {
            VolumeVO vol = Mockito.mock(VolumeVO.class);
            long index = i + 1;
            Mockito.when(vol.getDiskOfferingId()).thenReturn(index);
            Mockito.when(vol.getPoolId()).thenReturn(index);
            DiskOfferingVO diskOffering = Mockito.mock(DiskOfferingVO.class);
            Mockito.when(diskOfferingDao.findById(index)).thenReturn(diskOffering);
            StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
            Mockito.when(primaryDataStoreDao.findById(index)).thenReturn(storagePool);
            if (i < localVolumes) {
                if ((localVolumes + nonLocalVolumes) % 2 == 0) {
                    Mockito.when(diskOffering.isUseLocalStorage()).thenReturn(true);
                } else {

                    Mockito.when(diskOffering.isUseLocalStorage()).thenReturn(false);
                    Mockito.when(storagePool.isLocal()).thenReturn(true);
                }
            } else {
                Mockito.when(diskOffering.isUseLocalStorage()).thenReturn(false);
                Mockito.when(storagePool.isLocal()).thenReturn(false);
            }
            volumes.add(vol);
        }
        return volumes;
    }

    @Test
    public void testIsAnyVmVolumeUsingLocalStorage() {
        try {
            Assert.assertTrue(userVmManagerImpl.isAnyVmVolumeUsingLocalStorage(mockVolumesForIsAnyVmVolumeUsingLocalStorageTest(1, 0)));
            Assert.assertTrue(userVmManagerImpl.isAnyVmVolumeUsingLocalStorage(mockVolumesForIsAnyVmVolumeUsingLocalStorageTest(2, 0)));
            Assert.assertTrue(userVmManagerImpl.isAnyVmVolumeUsingLocalStorage(mockVolumesForIsAnyVmVolumeUsingLocalStorageTest(1, 1)));
            Assert.assertFalse(userVmManagerImpl.isAnyVmVolumeUsingLocalStorage(mockVolumesForIsAnyVmVolumeUsingLocalStorageTest(0, 2)));
            Assert.assertFalse(userVmManagerImpl.isAnyVmVolumeUsingLocalStorage(mockVolumesForIsAnyVmVolumeUsingLocalStorageTest(0, 0)));
        }catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    private List<VolumeVO> mockVolumesForIsAllVmVolumesOnZoneWideStore(int nullPoolIdVolumes, int nullPoolVolumes, int zoneVolumes, int nonZoneVolumes) {
        List<VolumeVO> volumes = new ArrayList<>();
        for (int i=0; i< nullPoolIdVolumes + nullPoolVolumes + zoneVolumes + nonZoneVolumes; ++i) {
            VolumeVO vol = Mockito.mock(VolumeVO.class);
            volumes.add(vol);
            if (i < nullPoolIdVolumes) {
                Mockito.when(vol.getPoolId()).thenReturn(null);
                continue;
            }
            long index = i + 1;
            Mockito.when(vol.getPoolId()).thenReturn(index);
            if (i < nullPoolVolumes) {
                Mockito.when(primaryDataStoreDao.findById(index)).thenReturn(null);
                continue;
            }
            StoragePoolVO storagePool = Mockito.mock(StoragePoolVO.class);
            Mockito.when(primaryDataStoreDao.findById(index)).thenReturn(storagePool);
            if (i < zoneVolumes) {
                Mockito.when(storagePool.getScope()).thenReturn(ScopeType.ZONE);
            } else {
                Mockito.when(storagePool.getScope()).thenReturn(ScopeType.CLUSTER);
            }
        }
        return volumes;
    }

    @Test
    public void testIsAllVmVolumesOnZoneWideStoreCombinations() {
        Assert.assertTrue(userVmManagerImpl.isAllVmVolumesOnZoneWideStore(mockVolumesForIsAllVmVolumesOnZoneWideStore(0, 0, 1, 0)));
        Assert.assertTrue(userVmManagerImpl.isAllVmVolumesOnZoneWideStore(mockVolumesForIsAllVmVolumesOnZoneWideStore(0, 0, 2, 0)));
        Assert.assertFalse(userVmManagerImpl.isAllVmVolumesOnZoneWideStore(mockVolumesForIsAllVmVolumesOnZoneWideStore(0, 0, 1, 1)));
        Assert.assertFalse(userVmManagerImpl.isAllVmVolumesOnZoneWideStore(mockVolumesForIsAllVmVolumesOnZoneWideStore(0, 0, 0, 0)));
        Assert.assertFalse(userVmManagerImpl.isAllVmVolumesOnZoneWideStore(mockVolumesForIsAllVmVolumesOnZoneWideStore(1, 0, 1, 1)));
        Assert.assertFalse(userVmManagerImpl.isAllVmVolumesOnZoneWideStore(mockVolumesForIsAllVmVolumesOnZoneWideStore(0, 1, 1, 1)));
    }

    private Pair<VMInstanceVO, Host> mockObjectsForChooseVmMigrationDestinationUsingVolumePoolMapTest(boolean nullPlan, Host destinationHost) {
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getServiceOfferingId()).thenReturn(1L);
        Host host = Mockito.mock(Host.class);
        Mockito.when(host.getId()).thenReturn(1L);
        Mockito.when(hostDao.findById(1L)).thenReturn(Mockito.mock(HostVO.class));
        Mockito.when(virtualMachineManager.getMigrationDeployment(Mockito.any(VirtualMachine.class),
                        Mockito.any(Host.class), Mockito.nullable(Long.class),
                        Mockito.any(DeploymentPlanner.ExcludeList.class)))
                .thenReturn(Mockito.mock(DataCenterDeployment.class));
        if (!nullPlan) {
            try {
                DeployDestination destination = Mockito.mock(DeployDestination.class);
                Mockito.when(destination.getHost()).thenReturn(destinationHost);
                Mockito.when(planningManager.planDeployment(Mockito.any(VirtualMachineProfile.class),
                                Mockito.any(DataCenterDeployment.class), Mockito.any(DeploymentPlanner.ExcludeList.class),
                                Mockito.nullable(DeploymentPlanner.class)))
                        .thenReturn(destination);
            } catch (InsufficientServerCapacityException e) {
                fail("Failed to mock DeployDestination");
            }
        }
        return new Pair<>(vm, host);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testChooseVmMigrationDestinationUsingVolumePoolMapNullDestination() {
        Pair<VMInstanceVO, Host> pair = mockObjectsForChooseVmMigrationDestinationUsingVolumePoolMapTest(true, null);
        userVmManagerImpl.chooseVmMigrationDestinationUsingVolumePoolMap(pair.first(), pair.second(), null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testChooseVmMigrationDestinationUsingVolumePoolMapNullHost() {
        Pair<VMInstanceVO, Host> pair = mockObjectsForChooseVmMigrationDestinationUsingVolumePoolMapTest(false, null);
        userVmManagerImpl.chooseVmMigrationDestinationUsingVolumePoolMap(pair.first(), pair.second(), null);
    }

    @Test
    public void testChooseVmMigrationDestinationUsingVolumePoolMapValid() {
        Host destinationHost = Mockito.mock(Host.class);
        Pair<VMInstanceVO, Host> pair = mockObjectsForChooseVmMigrationDestinationUsingVolumePoolMapTest(false, destinationHost);
        Assert.assertEquals(destinationHost, userVmManagerImpl.chooseVmMigrationDestinationUsingVolumePoolMap(pair.first(), pair.second(), null));
    }

    @Test
    public void testUpdateVncPasswordIfItHasChanged() {
        String vncPassword = "12345678";
        userVmManagerImpl.updateVncPasswordIfItHasChanged(vncPassword, vncPassword, virtualMachineProfile);
        Mockito.verify(userVmDao, Mockito.never()).update(vmId, userVmVoMock);
    }

    @Test
    public void testUpdateVncPasswordIfItHasChangedNewPassword() {
        String vncPassword = "12345678";
        String newPassword = "87654321";
        Mockito.when(userVmVoMock.getId()).thenReturn(vmId);
        userVmManagerImpl.updateVncPasswordIfItHasChanged(vncPassword, newPassword, virtualMachineProfile);
        Mockito.verify(userVmDao).findById(vmId);
        Mockito.verify(userVmDao).update(vmId, userVmVoMock);
    }

    @Test
    public void testGetSecurityGroupIdList() {
        DeployVnfApplianceCmd cmd = Mockito.mock(DeployVnfApplianceCmd.class);
        Mockito.doReturn(new ArrayList<Long>()).when(userVmManagerImpl).getSecurityGroupIdList(cmd);
        SecurityGroupVO securityGroupVO = Mockito.mock(SecurityGroupVO.class);
        long securityGroupId = 100L;
        when(securityGroupVO.getId()).thenReturn(securityGroupId);
        Mockito.doReturn(securityGroupVO).when(vnfTemplateManager).createSecurityGroupForVnfAppliance(any(), any(), any(), any(DeployVnfApplianceCmd.class));

        List<Long> securityGroupIds = userVmManagerImpl.getSecurityGroupIdList(cmd, null, null, null);

        Assert.assertEquals(1, securityGroupIds.size());
        Assert.assertEquals(securityGroupId, securityGroupIds.get(0).longValue());

        Mockito.verify(userVmManagerImpl).getSecurityGroupIdList(cmd);
        Mockito.verify(vnfTemplateManager).createSecurityGroupForVnfAppliance(any(), any(), any(), any(DeployVnfApplianceCmd.class));
    }

    @Test
    public void getCurrentVmPasswordOrDefineNewPasswordTestTemplateIsNotPasswordEnabledReturnPreDefinedString() {
        String expected = "saved_password";

        Mockito.doReturn(false).when(vmTemplateVoMock).isEnablePassword();

        String result = userVmManagerImpl.getCurrentVmPasswordOrDefineNewPassword("", userVmVoMock, vmTemplateVoMock);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getCurrentVmPasswordOrDefineNewPasswordTestVmHasPasswordReturnCurrentPassword() {
        String expected = "current_password";

        Mockito.doReturn(true).when(vmTemplateVoMock).isEnablePassword();
        Mockito.doReturn(expected).when(userVmVoMock).getDetail("password");

        String result = userVmManagerImpl.getCurrentVmPasswordOrDefineNewPassword("", userVmVoMock, vmTemplateVoMock);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getCurrentVmPasswordOrDefineNewPasswordTestUserDefinedPasswordReturnNewPasswordAndSetVmPassword() {
        String expected = "new_password";

        Mockito.doReturn(true).when(vmTemplateVoMock).isEnablePassword();
        Mockito.doReturn(null).when(userVmVoMock).getDetail("password");
        Mockito.doCallRealMethod().when(userVmVoMock).setPassword(Mockito.any());
        Mockito.doCallRealMethod().when(userVmVoMock).getPassword();

        String result = userVmManagerImpl.getCurrentVmPasswordOrDefineNewPassword(expected, userVmVoMock, vmTemplateVoMock);

        Assert.assertEquals(expected, result);
        Assert.assertEquals(expected, userVmVoMock.getPassword());
    }

    @Test
    public void getCurrentVmPasswordOrDefineNewPasswordTestUserDefinedPasswordReturnRandomPasswordAndSetVmPassword() {
        String expected = "random_password";

        Mockito.doReturn(true).when(vmTemplateVoMock).isEnablePassword();
        Mockito.doReturn(null).when(userVmVoMock).getDetail("password");
        Mockito.doReturn(expected).when(managementServiceMock).generateRandomPassword();
        Mockito.doCallRealMethod().when(userVmVoMock).setPassword(Mockito.any());
        Mockito.doCallRealMethod().when(userVmVoMock).getPassword();

        String result = userVmManagerImpl.getCurrentVmPasswordOrDefineNewPassword("", userVmVoMock, vmTemplateVoMock);

        Assert.assertEquals(expected, result);
        Assert.assertEquals(expected, userVmVoMock.getPassword());
    }

    @Test
    public void testSetVmRequiredFieldsForImportNotImport() {
        userVmManagerImpl.setVmRequiredFieldsForImport(false, userVmVoMock, _dcMock,
                Hypervisor.HypervisorType.VMware, Mockito.mock(HostVO.class), Mockito.mock(HostVO.class), VirtualMachine.PowerState.PowerOn);
        Mockito.verify(userVmVoMock, never()).setDataCenterId(anyLong());
    }


    @Test
    public void createVirtualMachineWithCloudRuntimeException() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        DeployVMCmd deployVMCmd = new DeployVMCmd();
        ReflectionTestUtils.setField(deployVMCmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(deployVMCmd, "templateId", templateId);
        ReflectionTestUtils.setField(deployVMCmd, "serviceOfferingId", serviceOfferingId);
        deployVMCmd._accountService = accountService;

        when(accountService.finalyzeAccountId(nullable(String.class), nullable(Long.class), nullable(Long.class), eq(true))).thenReturn(accountId);
        when(accountService.getActiveAccountById(accountId)).thenReturn(account);
        when(entityManager.findById(DataCenter.class, zoneId)).thenReturn(_dcMock);
        when(entityManager.findById(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOffering);
        when(serviceOffering.getState()).thenReturn(ServiceOffering.State.Active);

        when(entityManager.findById(VirtualMachineTemplate.class, templateId)).thenReturn(templateMock);
        when(templateMock.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        when(templateMock.isDeployAsIs()).thenReturn(false);
        when(templateMock.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(templateMock.getUserDataId()).thenReturn(null);
        Mockito.doNothing().when(vnfTemplateManager).validateVnfApplianceNics(any(), nullable(List.class));

        ServiceOfferingJoinVO svcOfferingMock = Mockito.mock(ServiceOfferingJoinVO.class);
        when(serviceOfferingJoinDao.findById(anyLong())).thenReturn(svcOfferingMock);
        when(_dcMock.isLocalStorageEnabled()).thenReturn(true);
        when(_dcMock.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);
        String vmId = "testId";
        CloudRuntimeException cre = new CloudRuntimeException("Error and CloudRuntimeException is thrown");
        cre.addProxyObject(vmId, "vmId");

        Mockito.doThrow(cre).when(userVmManagerImpl).createBasicSecurityGroupVirtualMachine(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), nullable(Boolean.class), any(), any(), any(),
                any(), any(), any(), any(), eq(true), any(), any(), any());

        CloudRuntimeException creThrown = assertThrows(CloudRuntimeException.class, () -> userVmManagerImpl.createVirtualMachine(deployVMCmd));
        ArrayList<ExceptionProxyObject> proxyIdList = creThrown.getIdProxyList();
        assertNotNull(proxyIdList != null );
        assertTrue(proxyIdList.stream().anyMatch( p -> p.getUuid().equals(vmId)));
    }

    @Test
    public void testSetVmRequiredFieldsForImportFromLastHost() {
        HostVO lastHost = Mockito.mock(HostVO.class);
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(_dcMock.getId()).thenReturn(1L);
        Mockito.when(host.getId()).thenReturn(1L);
        Mockito.when(lastHost.getId()).thenReturn(2L);
        userVmManagerImpl.setVmRequiredFieldsForImport(true, userVmVoMock, _dcMock,
                Hypervisor.HypervisorType.VMware, host, lastHost, VirtualMachine.PowerState.PowerOn);
        Mockito.verify(userVmVoMock).setLastHostId(2L);
        Mockito.verify(userVmVoMock).setState(VirtualMachine.State.Running);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRestoreVMNoVM() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        Mockito.lenient().doReturn(accountMock).when(callContextMock).getCallingAccount();

        RestoreVMCmd cmd = Mockito.mock(RestoreVMCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getTemplateId()).thenReturn(2L);
        when(userVmDao.findById(vmId)).thenReturn(null);

        userVmManagerImpl.restoreVM(cmd);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testRestoreVMWithVolumeSnapshots() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        Mockito.lenient().doReturn(accountMock).when(callContextMock).getCallingAccount();
        Mockito.lenient().doNothing().when(accountManager).checkAccess(accountMock, null, true, userVmVoMock);

        RestoreVMCmd cmd = Mockito.mock(RestoreVMCmd.class);
        when(cmd.getVmId()).thenReturn(vmId);
        when(cmd.getTemplateId()).thenReturn(2L);
        when(userVmDao.findById(vmId)).thenReturn(userVmVoMock);

        userVmManagerImpl.restoreVM(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRestoreVirtualMachineNoOwner() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        long userId = 1l;
        long accountId = 2l;
        long newTemplateId = 2l;
        when(accountMock.getId()).thenReturn(userId);
        when(userVmDao.findById(vmId)).thenReturn(userVmVoMock);
        when(userVmVoMock.getAccountId()).thenReturn(accountId);
        when(accountDao.findById(accountId)).thenReturn(null);

        userVmManagerImpl.restoreVirtualMachine(accountMock, vmId, newTemplateId, null, false, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testRestoreVirtualMachineOwnerDisabled() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        long userId = 1l;
        long accountId = 2l;
        long newTemplateId = 2l;
        when(accountMock.getId()).thenReturn(userId);
        when(userVmDao.findById(vmId)).thenReturn(userVmVoMock);
        when(userVmVoMock.getAccountId()).thenReturn(accountId);
        when(accountDao.findById(accountId)).thenReturn(callerAccount);
        when(callerAccount.getState()).thenReturn(Account.State.DISABLED);

        userVmManagerImpl.restoreVirtualMachine(accountMock, vmId, newTemplateId, null, false, null);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testRestoreVirtualMachineNotInRightState() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        long userId = 1l;
        long accountId = 2l;
        long newTemplateId = 2l;
        when(accountMock.getId()).thenReturn(userId);
        when(userVmDao.findById(vmId)).thenReturn(userVmVoMock);
        when(userVmVoMock.getAccountId()).thenReturn(accountId);
        when(userVmVoMock.getUuid()).thenReturn("a967643d-7633-4ab4-ac26-9c0b63f50cc1");
        when(accountDao.findById(accountId)).thenReturn(callerAccount);
        when(userVmVoMock.getState()).thenReturn(VirtualMachine.State.Starting);

        userVmManagerImpl.restoreVirtualMachine(accountMock, vmId, newTemplateId, null, false, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRestoreVirtualMachineNoRootVolume() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        long userId = 1l;
        long accountId = 2l;
        long currentTemplateId = 1l;
        long newTemplateId = 2l;
        when(accountMock.getId()).thenReturn(userId);
        when(userVmDao.findById(vmId)).thenReturn(userVmVoMock);
        when(userVmVoMock.getAccountId()).thenReturn(accountId);
        when(userVmVoMock.getUuid()).thenReturn("a967643d-7633-4ab4-ac26-9c0b63f50cc1");
        when(accountDao.findById(accountId)).thenReturn(callerAccount);
        when(userVmVoMock.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmVoMock.getTemplateId()).thenReturn(currentTemplateId);

        VMTemplateVO currentTemplate = Mockito.mock(VMTemplateVO.class);
        when(templateDao.findById(currentTemplateId)).thenReturn(currentTemplate);
        when(volumeDaoMock.findByInstanceAndType(vmId, Volume.Type.ROOT)).thenReturn(new ArrayList<VolumeVO>());

        userVmManagerImpl.restoreVirtualMachine(accountMock, vmId, newTemplateId, null, false, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRestoreVirtualMachineMoreThanOneRootVolume() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        long userId = 1l;
        long accountId = 2l;
        long currentTemplateId = 1l;
        long newTemplateId = 2l;
        when(accountMock.getId()).thenReturn(userId);
        when(userVmDao.findById(vmId)).thenReturn(userVmVoMock);
        when(userVmVoMock.getAccountId()).thenReturn(accountId);
        when(userVmVoMock.getUuid()).thenReturn("a967643d-7633-4ab4-ac26-9c0b63f50cc1");
        when(accountDao.findById(accountId)).thenReturn(callerAccount);
        when(userVmVoMock.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmVoMock.getTemplateId()).thenReturn(currentTemplateId);

        VMTemplateVO currentTemplate = Mockito.mock(VMTemplateVO.class);
        when(currentTemplate.isDeployAsIs()).thenReturn(false);
        when(templateDao.findById(currentTemplateId)).thenReturn(currentTemplate);
        List<VolumeVO> volumes = new ArrayList<>();
        VolumeVO rootVolume1 = Mockito.mock(VolumeVO.class);
        volumes.add(rootVolume1);
        VolumeVO rootVolume2 = Mockito.mock(VolumeVO.class);
        volumes.add(rootVolume2);
        when(volumeDaoMock.findByInstanceAndType(vmId, Volume.Type.ROOT)).thenReturn(volumes);

        userVmManagerImpl.restoreVirtualMachine(accountMock, vmId, newTemplateId, null, false, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testRestoreVirtualMachineWithVMSnapshots() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        long userId = 1l;
        long accountId = 2l;
        long currentTemplateId = 1l;
        long newTemplateId = 2l;
        when(accountMock.getId()).thenReturn(userId);
        when(userVmDao.findById(vmId)).thenReturn(userVmVoMock);
        when(userVmVoMock.getAccountId()).thenReturn(accountId);
        when(accountDao.findById(accountId)).thenReturn(callerAccount);
        when(userVmVoMock.getState()).thenReturn(VirtualMachine.State.Running);
        when(userVmVoMock.getTemplateId()).thenReturn(currentTemplateId);

        VMTemplateVO currentTemplate = Mockito.mock(VMTemplateVO.class);
        when(templateDao.findById(currentTemplateId)).thenReturn(currentTemplate);
        List<VolumeVO> volumes = new ArrayList<>();
        VolumeVO rootVolumeOfVm = Mockito.mock(VolumeVO.class);
        volumes.add(rootVolumeOfVm);
        when(volumeDaoMock.findByInstanceAndType(vmId, Volume.Type.ROOT)).thenReturn(volumes);
        List<VMSnapshotVO> vmSnapshots = new ArrayList<>();
        VMSnapshotVO vmSnapshot = Mockito.mock(VMSnapshotVO.class);
        vmSnapshots.add(vmSnapshot);
        when(vmSnapshotDaoMock.findByVm(vmId)).thenReturn(vmSnapshots);

        userVmManagerImpl.restoreVirtualMachine(accountMock, vmId, newTemplateId, null, false, null);
    }

    @Test
    public void addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecifiedTestDetailsConstantIsNotNullDoNothing() {
        int currentValue = 123;

        for (String detailsConstant : detailsConstants) {
            userVmManagerImpl.addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecified(null, customParameters, detailsConstant, currentValue);
        }

        Assert.assertEquals(customParameters.get(VmDetailConstants.MEMORY), "2048");
        Assert.assertEquals(customParameters.get(VmDetailConstants.CPU_NUMBER), "4");
        Assert.assertEquals(customParameters.get(VmDetailConstants.CPU_SPEED), "1000");
    }

    @Test
    public void addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecifiedTestNewValueIsNotNullDoNothing() {
        Map<String, String> details = new HashMap<>();
        int currentValue = 123;

        for (String detailsConstant : detailsConstants) {
            userVmManagerImpl.addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecified(321, details, detailsConstant, currentValue);
        }

        Assert.assertNull(details.get(VmDetailConstants.MEMORY));
        Assert.assertNull(details.get(VmDetailConstants.CPU_NUMBER));
        Assert.assertNull(details.get(VmDetailConstants.CPU_SPEED));
    }

    @Test
    public void addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecifiedTestBothValuesAreNullKeepCurrentValue() {
        Map<String, String> details = new HashMap<>();
        int currentValue = 123;

        for (String detailsConstant : detailsConstants) {
            userVmManagerImpl.addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecified(null, details, detailsConstant, currentValue);
        }

        Assert.assertEquals(details.get(VmDetailConstants.MEMORY), String.valueOf(currentValue));
        Assert.assertEquals(details.get(VmDetailConstants.CPU_NUMBER), String.valueOf(currentValue));
        Assert.assertEquals(details.get(VmDetailConstants.CPU_SPEED),String.valueOf(currentValue));
    }

    @Test
    public void addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecifiedTestNeitherValueIsNullDoNothing() {
        int currentValue = 123;

        for (String detailsConstant : detailsConstants) {
            userVmManagerImpl.addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecified(321, customParameters, detailsConstant, currentValue);
        }

        Assert.assertEquals(customParameters.get(VmDetailConstants.MEMORY), "2048");
        Assert.assertEquals(customParameters.get(VmDetailConstants.CPU_NUMBER), "4");
        Assert.assertEquals(customParameters.get(VmDetailConstants.CPU_SPEED),"1000");
    }

    @Test
    public void updateInstanceDetailsMapWithCurrentValuesForAbsentDetailsTestAllConstantsAreUpdated() {
        Mockito.doReturn(serviceOffering).when(_serviceOfferingDao).findById(Mockito.anyLong());
        Mockito.doReturn(1L).when(vmInstanceMock).getId();
        Mockito.doReturn(1L).when(vmInstanceMock).getServiceOfferingId();
        Mockito.doReturn(serviceOffering).when(_serviceOfferingDao).findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong());
        userVmManagerImpl.updateInstanceDetailsMapWithCurrentValuesForAbsentDetails(null, vmInstanceMock, 0l);

        Mockito.verify(userVmManagerImpl).addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecified(Mockito.any(), Mockito.any(), Mockito.eq(VmDetailConstants.CPU_SPEED), Mockito.any());
        Mockito.verify(userVmManagerImpl).addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecified(Mockito.any(), Mockito.any(), Mockito.eq(VmDetailConstants.MEMORY), Mockito.any());
        Mockito.verify(userVmManagerImpl).addCurrentDetailValueToInstanceDetailsMapIfNewValueWasNotSpecified(Mockito.any(), Mockito.any(), Mockito.eq(VmDetailConstants.CPU_NUMBER), Mockito.any());
    }

    @Test
    public void testCheckVolumesLimits() {
        long diskOffId1 = 1L;
        DiskOfferingVO diskOfferingVO1 = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOfferingDao.findById(diskOffId1)).thenReturn(diskOfferingVO1);
        Mockito.when(resourceLimitMgr.getResourceLimitStorageTags(diskOfferingVO1)).thenReturn(List.of("tag1", "tag2"));
        long diskOffId2 = 2L;
        DiskOfferingVO diskOfferingVO2 = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOfferingDao.findById(diskOffId2)).thenReturn(diskOfferingVO2);
        Mockito.when(resourceLimitMgr.getResourceLimitStorageTags(diskOfferingVO2)).thenReturn(List.of("tag2"));
        long diskOffId3 = 3L;
        DiskOfferingVO diskOfferingVO3 = Mockito.mock(DiskOfferingVO.class);
        Mockito.when(diskOfferingDao.findById(diskOffId3)).thenReturn(diskOfferingVO3);
        Mockito.when(resourceLimitMgr.getResourceLimitStorageTags(diskOfferingVO3)).thenReturn(new ArrayList<>());

        VolumeVO vol1 = Mockito.mock(VolumeVO.class);
        Mockito.when(vol1.getDiskOfferingId()).thenReturn(diskOffId1);
        Mockito.when(vol1.getSize()).thenReturn(10L);
        Mockito.when(vol1.isDisplay()).thenReturn(true);
        VolumeVO undisplayedVolume = Mockito.mock(VolumeVO.class); // shouldn't be considered for limits
        Mockito.when(undisplayedVolume.isDisplay()).thenReturn(false);
        VolumeVO vol3 = Mockito.mock(VolumeVO.class);
        Mockito.when(vol3.getDiskOfferingId()).thenReturn(diskOffId2);
        Mockito.when(vol3.getSize()).thenReturn(30L);
        Mockito.when(vol3.isDisplay()).thenReturn(true);
        VolumeVO vol4 = Mockito.mock(VolumeVO.class);
        Mockito.when(vol4.getDiskOfferingId()).thenReturn(diskOffId3);
        Mockito.when(vol4.getSize()).thenReturn(40L);
        Mockito.when(vol4.isDisplay()).thenReturn(true);
        VolumeVO vol5 = Mockito.mock(VolumeVO.class);
        Mockito.when(vol5.getDiskOfferingId()).thenReturn(diskOffId1);
        Mockito.when(vol5.getSize()).thenReturn(50L);
        Mockito.when(vol5.isDisplay()).thenReturn(true);

        List<VolumeVO> volumes = List.of(vol1, undisplayedVolume, vol3, vol4, vol5);
        Long size = volumes.stream().filter(VolumeVO::isDisplay).mapToLong(VolumeVO::getSize).sum();
        try {
            userVmManagerImpl.checkVolumesLimits(account, volumes);
            Mockito.verify(resourceLimitMgr, times(1))
                    .checkResourceLimit(account, Resource.ResourceType.volume, 4);
            Mockito.verify(resourceLimitMgr, times(1))
                    .checkResourceLimit(account, Resource.ResourceType.primary_storage, size);
            Mockito.verify(resourceLimitMgr, times(1))
                    .checkResourceLimitWithTag(account, Resource.ResourceType.volume, "tag1", 2);
            Mockito.verify(resourceLimitMgr, times(1))
                    .checkResourceLimitWithTag(account, Resource.ResourceType.volume, "tag2", 3);
            Mockito.verify(resourceLimitMgr, times(1))
                    .checkResourceLimitWithTag(account, Resource.ResourceType.primary_storage, "tag1",
                            vol1.getSize() + vol5.getSize());
            Mockito.verify(resourceLimitMgr, times(1))
                    .checkResourceLimitWithTag(account, Resource.ResourceType.primary_storage, "tag2",
                            vol1.getSize() + vol3.getSize() + vol5.getSize());
        } catch (ResourceAllocationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testValidateStrictHostTagCheckPass() {
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        HostVO destinationHostVO = Mockito.mock(HostVO.class);

        Mockito.when(_serviceOfferingDao.findByIdIncludingRemoved(1L)).thenReturn(serviceOffering);
        Mockito.when(templateDao.findByIdIncludingRemoved(2L)).thenReturn(template);

        Mockito.when(vm.getServiceOfferingId()).thenReturn(1L);
        Mockito.when(vm.getTemplateId()).thenReturn(2L);

        Mockito.when(destinationHostVO.checkHostServiceOfferingAndTemplateTags(Mockito.any(ServiceOffering.class), Mockito.any(VirtualMachineTemplate.class), Mockito.anySet())).thenReturn(true);

        userVmManagerImpl.validateStrictHostTagCheck(vm, destinationHostVO);

        Mockito.verify(
                destinationHostVO, times(1)
        ).checkHostServiceOfferingAndTemplateTags(Mockito.any(ServiceOffering.class), Mockito.any(VirtualMachineTemplate.class), Mockito.anySet());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateStrictHostTagCheckFail() {
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);

        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        HostVO destinationHostVO = Mockito.mock(HostVO.class);

        Mockito.when(_serviceOfferingDao.findByIdIncludingRemoved(1L)).thenReturn(serviceOffering);
        Mockito.when(templateDao.findByIdIncludingRemoved(2L)).thenReturn(template);

        Mockito.when(vm.getServiceOfferingId()).thenReturn(1L);
        Mockito.when(vm.getTemplateId()).thenReturn(2L);

        Mockito.when(destinationHostVO.checkHostServiceOfferingAndTemplateTags(Mockito.any(ServiceOffering.class), Mockito.any(VirtualMachineTemplate.class), Mockito.anySet())).thenReturn(false);
        userVmManagerImpl.validateStrictHostTagCheck(vm, destinationHostVO);
    }

    public void testGetRootVolumeSizeForVmRestore() {
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.getSize()).thenReturn(10L * GiB_TO_BYTES);
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        Mockito.when(userVm.getId()).thenReturn(1L);
        DiskOffering diskOffering = Mockito.mock(DiskOffering.class);
        Mockito.when(diskOffering.isCustomized()).thenReturn(false);
        Mockito.when(diskOffering.getDiskSize()).thenReturn(8L * GiB_TO_BYTES);
        Map<String, String> details = new HashMap<>();
        details.put(VmDetailConstants.ROOT_DISK_SIZE, "16");
        UserVmDetailVO vmRootDiskSizeDetail = Mockito.mock(UserVmDetailVO.class);
        Mockito.when(vmRootDiskSizeDetail.getValue()).thenReturn("20");
        Mockito.when(userVmDetailsDao.findDetail(1L, VmDetailConstants.ROOT_DISK_SIZE)).thenReturn(vmRootDiskSizeDetail);
        Long actualSize = userVmManagerImpl.getRootVolumeSizeForVmRestore(null, template, userVm, diskOffering, details, false);
        Assert.assertEquals(16 * GiB_TO_BYTES, actualSize.longValue());
    }

    @Test
    public void testGetRootVolumeSizeForVmRestoreNullDiskOfferingAndEmptyDetails() {
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.getSize()).thenReturn(10L * GiB_TO_BYTES);
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        Mockito.when(userVm.getId()).thenReturn(1L);
        DiskOffering diskOffering = null;
        Map<String, String> details = new HashMap<>();
        UserVmDetailVO vmRootDiskSizeDetail = Mockito.mock(UserVmDetailVO.class);
        Mockito.when(vmRootDiskSizeDetail.getValue()).thenReturn("20");
        Mockito.when(userVmDetailsDao.findDetail(1L, VmDetailConstants.ROOT_DISK_SIZE)).thenReturn(vmRootDiskSizeDetail);
        Long actualSize = userVmManagerImpl.getRootVolumeSizeForVmRestore(null, template, userVm, diskOffering, details, false);
        Assert.assertEquals(20 * GiB_TO_BYTES, actualSize.longValue());
    }

    @Test
    public void checkExpungeVMPermissionTestAccountIsNotAdminConfigFalseThrowsPermissionDeniedException () {
        Mockito.doReturn(false).when(accountManager).isAdmin(Mockito.anyLong());
        Mockito.doReturn(false).when(userVmManagerImpl).getConfigAllowUserExpungeRecoverVm(Mockito.anyLong());

        Assert.assertThrows(PermissionDeniedException.class, () -> userVmManagerImpl.checkExpungeVmPermission(accountMock));
    }
    @Test
    public void checkExpungeVmPermissionTestAccountIsNotAdminConfigTrueNoApiAccessThrowsPermissionDeniedException () {
        Mockito.doReturn(false).when(accountManager).isAdmin(Mockito.anyLong());
        Mockito.doReturn(true).when(userVmManagerImpl).getConfigAllowUserExpungeRecoverVm(Mockito.anyLong());
        Mockito.doThrow(PermissionDeniedException.class).when(accountManager).checkApiAccess(accountMock, "expungeVirtualMachine");

        Assert.assertThrows(PermissionDeniedException.class, () -> userVmManagerImpl.checkExpungeVmPermission(accountMock));
    }
    @Test
    public void checkExpungeVmPermissionTestAccountIsNotAdminConfigTrueHasApiAccessReturnNothing () {
        Mockito.doReturn(false).when(accountManager).isAdmin(Mockito.anyLong());
        Mockito.doReturn(true).when(userVmManagerImpl).getConfigAllowUserExpungeRecoverVm(Mockito.anyLong());

        userVmManagerImpl.checkExpungeVmPermission(accountMock);
    }
    @Test
    public void checkExpungeVmPermissionTestAccountIsAdminNoApiAccessThrowsPermissionDeniedException () {
        Mockito.doReturn(true).when(accountManager).isAdmin(Mockito.anyLong());
        Mockito.doThrow(PermissionDeniedException.class).when(accountManager).checkApiAccess(accountMock, "expungeVirtualMachine");

        Assert.assertThrows(PermissionDeniedException.class, () -> userVmManagerImpl.checkExpungeVmPermission(accountMock));
    }
    @Test
    public void checkExpungeVmPermissionTestAccountIsAdminHasApiAccessReturnNothing () {
        Mockito.doReturn(true).when(accountManager).isAdmin(Mockito.anyLong());

        userVmManagerImpl.checkExpungeVmPermission(accountMock);
    }

    @Test
    public void validateIfVmSupportsMigrationTestVmIsNullThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("There is no VM by ID [%s].", 1l);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfVmSupportsMigration(null, 1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfVmSupportsMigrationTestVmIsRunningThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Unable to move VM [%s] in [%s] state.", userVmVoMock, VirtualMachine.State.Running);
        Mockito.doReturn(VirtualMachine.State.Running).when(userVmVoMock).getState();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfVmSupportsMigration(userVmVoMock, 1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfVmSupportsMigrationTestVmIsSharedFileSystemInstanceThrowsInvalidParameterValueException() {
        Mockito.doReturn(UserVmManager.SHAREDFSVM).when(userVmVoMock).getUserVmType();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfVmSupportsMigration(userVmVoMock, 1l);
        });

        Assert.assertEquals("Migration is not supported for Shared FileSystem Instances.", assertThrows.getMessage());
    }

    @Test
    public void validateIfVmSupportsMigrationTestVmIsNotRunningDoesNotThrowInvalidParameterValueException() {
        userVmManagerImpl.validateIfVmSupportsMigration(userVmVoMock, 1l);
    }

    @Test
    public void validateOldAndNewAccountsTestBothAreValidDoNothing() {
        Account newAccount = Mockito.mock(Account.class);
        Mockito.doReturn(1l).when(newAccount).getAccountId();

        userVmManagerImpl.validateOldAndNewAccounts(accountMock, newAccount, 1L, "", 1L);
    }

    @Test
    public void validateOldAndNewAccountsTestOldAccountIsNullThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Invalid old account [%s] for VM in domain [%s].", userVmVoMock.getAccountId(), assignVmCmdMock.getDomainId());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateOldAndNewAccounts(null, accountMock, userVmVoMock.getAccountId(), "", assignVmCmdMock.getDomainId());
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateOldAndNewAccountsTestNewAccountIsNullThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Invalid new account [%s] for VM in domain [%s].", assignVmCmdMock.getAccountName(), assignVmCmdMock.getDomainId());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateOldAndNewAccounts(accountMock, null, 1l, assignVmCmdMock.getAccountName(), assignVmCmdMock.getDomainId());
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateOldAndNewAccountsTestNewAccountStateIsDisabledThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("The new account owner [%s] is disabled.", accountMock.toString());

        Mockito.doReturn(Account.State.DISABLED).when(accountMock).getState();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateOldAndNewAccounts(accountMock, accountMock, 1l, "", 1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateOldAndNewAccountsTestOldAccountIsTheSameAsNewAccountThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("The new account [%s] is the same as the old account.", accountMock.toString());

        Mockito.doReturn(Account.State.ENABLED).when(accountMock).getState();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateOldAndNewAccounts(accountMock, accountMock, 1l, "", 1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateOldAndNewAccountsTestOldAccountIsNotTheSameAsNewAccountDoesNotThrowInvalidParameterValueException() {
        AccountVO oldAccount = new AccountVO();
        Mockito.doReturn(1l).when(accountMock).getAccountId();

        userVmManagerImpl.validateOldAndNewAccounts(oldAccount, accountMock, 1l, "", 1l);
    }

    @Test
    public void checkCallerAccessToAccountsTestCallsCheckAccessToOldAccountAndNewAccount() {
        AccountVO oldAccount = new AccountVO();

        userVmManagerImpl.checkCallerAccessToAccounts(callerAccount, oldAccount, accountMock);

        Mockito.verify(accountManager).checkAccess(callerAccount, null, true, oldAccount);
        Mockito.verify(accountManager).checkAccess(callerAccount, null, true, accountMock);
    }

    @Test
    public void validateIfVmHasNoRulesTestPortForwardingRulesExistThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Remove any Port Forwarding rules for VM [%s] before assigning it to another user.", userVmVoMock);

        Mockito.doReturn(portForwardingRulesListMock).when(portForwardingRulesDaoMock).listByVm(Mockito.anyLong());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfVmHasNoRules(userVmVoMock, 1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfVmHasNoRulesTestStaticNatRulesExistThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Remove the StaticNat rules for VM [%s] before assigning it to another user.", userVmVoMock);

        Mockito.doReturn(firewallRuleVoListMock).when(firewallRulesDaoMock).listStaticNatByVmId(Mockito.anyLong());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfVmHasNoRules(userVmVoMock, 1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfVmHasNoRulesTestLoadBalancingRulesExistThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Remove the Load Balancing rules for VM [%s] before assigning it to another user.", userVmVoMock);

        Mockito.doReturn(loadBalancerVmMapVoListMock).when(loadBalancerVmMapDaoMock).listByInstanceId(Mockito.anyLong());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfVmHasNoRules(userVmVoMock, 1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfVmHasNoRulesTestOneToOneNatRulesExistThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Remove the One to One Nat rule for VM [%s] for IP [%s].", userVmVoMock, ipAddressVoMock.toString());

        LinkedList<IPAddressVO> ipAddressVoList = new LinkedList<IPAddressVO>();

        Mockito.doReturn(ipAddressVoList).when(ipAddressDaoMock).findAllByAssociatedVmId(Mockito.anyLong());
        ipAddressVoList.add(ipAddressVoMock);
        Mockito.doReturn(true).when(ipAddressVoMock).isOneToOneNat();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfVmHasNoRules(userVmVoMock, 1l);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfVmHasNoRulesTestOneToOneNatRulesDoNotExistDoesNotThrowInvalidParameterValueException() {
        userVmManagerImpl.validateIfVmHasNoRules(userVmVoMock, 1l);
    }

    @Test
    public void verifyResourceLimitsForAccountAndStorageTestCountOnlyRunningVmsInResourceLimitationIsTrueDoesNotCallVmResourceLimitCheck() throws ResourceAllocationException {
        LinkedList<VolumeVO> volumeVoList = new LinkedList<VolumeVO>();
        Mockito.doReturn(true).when(userVmManagerImpl).countOnlyRunningVmsInResourceLimitation();

        userVmManagerImpl.verifyResourceLimitsForAccountAndStorage(accountMock, userVmVoMock, serviceOfferingVoMock, volumeVoList, virtualMachineTemplateMock);

        Mockito.verify(resourceLimitMgr, Mockito.never()).checkVmResourceLimit(Mockito.any(), Mockito.anyBoolean(), Mockito.any(), Mockito.any());
        Mockito.verify(resourceLimitMgr).checkResourceLimit(accountMock, Resource.ResourceType.volume, 0l);
        Mockito.verify(resourceLimitMgr).checkResourceLimit(accountMock, Resource.ResourceType.primary_storage, 0l);
    }

    @Test
    public void verifyResourceLimitsForAccountAndStorageTestCountOnlyRunningVmsInResourceLimitationIsFalseCallsVmResourceLimitCheck() throws ResourceAllocationException {
        LinkedList<VolumeVO> volumeVoList = new LinkedList<VolumeVO>();
        Mockito.doReturn(false).when(userVmManagerImpl).countOnlyRunningVmsInResourceLimitation();

        userVmManagerImpl.verifyResourceLimitsForAccountAndStorage(accountMock, userVmVoMock, serviceOfferingVoMock, volumeVoList, virtualMachineTemplateMock);

        Mockito.verify(resourceLimitMgr).checkVmResourceLimit(Mockito.any(), Mockito.anyBoolean(), Mockito.any(), Mockito.any());
        Mockito.verify(resourceLimitMgr).checkResourceLimit(accountMock, Resource.ResourceType.volume, 0l);
        Mockito.verify(resourceLimitMgr).checkResourceLimit(accountMock, Resource.ResourceType.primary_storage, 0l);
    }

    @Test
    public void validateIfNewOwnerHasAccessToTemplateTestTemplateIsNullThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Template for VM [%s] cannot be found.", userVmVoMock.getUuid());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfNewOwnerHasAccessToTemplate(userVmVoMock, accountMock, null);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfNewOwnerHasAccessToTemplateTestCallCheckAccessWhenTemplateIsNotPublic() {
        userVmManagerImpl.validateIfNewOwnerHasAccessToTemplate(userVmVoMock, accountMock, virtualMachineTemplateMock);

        Mockito.verify(accountManager).checkAccess(accountMock, SecurityChecker.AccessType.UseEntry, true, virtualMachineTemplateMock);
    }

    @Test
    public void updateVmOwnerTestCallsSetAccountIdSetDomainIdAndPersist() {
        userVmManagerImpl.updateVmOwner(accountMock, userVmVoMock, 1l, 1l);

        Mockito.verify(userVmVoMock).setAccountId(Mockito.anyLong());
        Mockito.verify(userVmVoMock).setDomainId(Mockito.anyLong());
        Mockito.verify(userVmDao).persist(userVmVoMock);
    }

    @Test
    public void updateVmNetworkTestCallsUpdateBasicTypeNetworkForVmIfBasicTypeZone() throws InsufficientCapacityException, ResourceAllocationException {
        Mockito.doReturn(_dcMock).when(_dcDao).findById(Mockito.anyLong());
        Mockito.doReturn(DataCenter.NetworkType.Basic).when(_dcMock).getNetworkType();
        Mockito.doNothing().when(userVmManagerImpl).updateBasicTypeNetworkForVm(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());

        userVmManagerImpl.updateVmNetwork(assignVmCmdMock, callerAccount, userVmVoMock, accountMock, virtualMachineTemplateMock);

        Mockito.verify(userVmManagerImpl).updateBasicTypeNetworkForVm(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any());
    }

    @Test
    public void updateVmNetworkTestCallsUpdateAdvancedTypeNetworkForVmIfNotBasicTypeZone() throws InsufficientCapacityException, ResourceAllocationException {
        Mockito.doReturn(_dcMock).when(_dcDao).findById(Mockito.anyLong());
        Mockito.doReturn(DataCenter.NetworkType.Advanced).when(_dcMock).getNetworkType();
        Mockito.doNothing().when(userVmManagerImpl).updateAdvancedTypeNetworkForVm(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any());

        userVmManagerImpl.updateVmNetwork(assignVmCmdMock, callerAccount, userVmVoMock, accountMock, virtualMachineTemplateMock);

        Mockito.verify(userVmManagerImpl).updateAdvancedTypeNetworkForVm(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void cleanupOfOldOwnerNicsForNetworkTestCallsCleanupNicsAndRemoveNics() {
        userVmManagerImpl.cleanupOfOldOwnerNicsForNetwork(virtualMachineProfileMock);

        Mockito.verify(_networkMgr).cleanupNics(virtualMachineProfileMock);
        Mockito.verify(_networkMgr).removeNics(virtualMachineProfileMock);
    }

    @Test
    public void addDefaultNetworkToNetworkListTestDefaultNetworkIsNullThrowsInvalidParameterValueException() {
        String expectedMessage = "Unable to find a default network to start a VM.";

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.addDefaultNetworkToNetworkList(networkVoListMock, null);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void addDefaultNetworkToNetworkListTestDefaultNetworkIsNotNullAddNetworkToNetworkList() {
        userVmManagerImpl.addDefaultNetworkToNetworkList(networkVoListMock, networkMock);

        Mockito.verify(networkVoListMock).add(Mockito.any());
    }

    @Test
    public void allocateNetworksForVmTestCallsNetworkManagerAllocate() throws InsufficientCapacityException {
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>();

        Mockito.doReturn(userVmVoMock).when(virtualMachineManager).findById(Mockito.anyLong());

        userVmManagerImpl.allocateNetworksForVm(userVmVoMock, networks);

        Mockito.verify(_networkMgr).allocate(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void addSecurityGroupsToVmTestIsVmWareAndSecurityGroupIdListIsNotNullThrowsInvalidParameterValueException() {
        String expectedMessage = "Security group feature is not supported for VMWare hypervisor.";
        LinkedList<Long> securityGroupIdList = new LinkedList<Long>();

        Mockito.doReturn(Hypervisor.HypervisorType.VMware).when(virtualMachineTemplateMock).getHypervisorType();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.addSecurityGroupsToVm(accountMock, userVmVoMock, virtualMachineTemplateMock, securityGroupIdList, networkMock);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void addSecurityGroupsToVmTestIsNotVmWareDefaultNetworkIsNullAndNetworkModelCanAddDefaultSecurityGroupCallsAddDefaultSecurityGroupToSecurityGroupIdList() {
        LinkedList<Long> securityGroupIdList = new LinkedList<Long>();

        Mockito.doReturn(Hypervisor.HypervisorType.KVM).when(virtualMachineTemplateMock).getHypervisorType();
        Mockito.doReturn(true).when(networkModel).canAddDefaultSecurityGroup();
        Mockito.doReturn(securityGroupVoMock).when(securityGroupManagerMock).getDefaultSecurityGroup(Mockito.anyLong());

        userVmManagerImpl.addSecurityGroupsToVm(accountMock, userVmVoMock, virtualMachineTemplateMock, securityGroupIdList, null);

        Mockito.verify(userVmManagerImpl).addDefaultSecurityGroupToSecurityGroupIdList(accountMock, securityGroupIdList);
        Mockito.verify(securityGroupManagerMock).addInstanceToGroups(Mockito.any(), Mockito.any());
    }

    @Test
    public void addNetworksToNetworkIdListTestCallsKeepOldSharedNetworkForVmAndAddAdditionalNetworksToVm() {
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        userVmManagerImpl.addNetworksToNetworkIdList(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics,
                requestedIPv6ForNics);

        Mockito.verify(userVmManagerImpl).keepOldSharedNetworkForVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);
        Mockito.verify(userVmManagerImpl).addAdditionalNetworksToVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);
    }

    @Test
    public void getOfferingWithRequiredAvailabilityForNetworkCreationTestRequiredOfferingsListHasNoOfferingsThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Unable to find network offering with availability [%s] to automatically create the network as a part of VM creation.",
                NetworkOffering.Availability.Required);
        LinkedList<NetworkOfferingVO> requiredOfferings = new LinkedList<>();

        Mockito.doReturn(requiredOfferings).when(networkOfferingDaoMock).listByAvailability(NetworkOffering.Availability.Required, false);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.getOfferingWithRequiredAvailabilityForNetworkCreation();
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void getOfferingWithRequiredAvailabilityForNetworkCreationTestFirstOfferingIsNotEnabledThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Required network offering ID [%s] is not in [%s] state.", 1l, NetworkOffering.State.Enabled);

        Mockito.doReturn(networkOfferingVoListMock).when(networkOfferingDaoMock).listByAvailability(NetworkOffering.Availability.Required, false);
        Mockito.doReturn(networkOfferingVoMock).when(networkOfferingVoListMock).get(0);

        Mockito.doReturn(NetworkOffering.State.Disabled).when(networkOfferingVoMock).getState();

        Mockito.doReturn(1l).when(networkOfferingVoMock).getId();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.getOfferingWithRequiredAvailabilityForNetworkCreation();
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test(expected = CloudRuntimeException.class)
    public void selectApplicableNetworkToCreateVmTestVirtualNetworkIsEmptyThrowsException() throws InsufficientCapacityException,
            ResourceAllocationException {

        HashSet<NetworkVO> applicableNetworks = new HashSet<>();
        LinkedList<? extends Network> virtualNetworks = new LinkedList<>();

        Mockito.doReturn(virtualNetworks).when(networkModel).listNetworksForAccount(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());

        userVmManagerImpl.selectApplicableNetworkToCreateVm(accountMock, _dcMock, applicableNetworks);
    }

    @Test
    public void selectApplicableNetworkToCreateVmTestVirtualNetworkHasMultipleNetworksThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("More than one default isolated network has been found for account [%s]; please specify networkIDs.", accountMock.toString());
        HashSet<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
        LinkedList<NetworkVO> virtualNetworks = new LinkedList<NetworkVO>();

        Mockito.doReturn(virtualNetworks).when(networkModel).listNetworksForAccount(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());

        virtualNetworks.add(networkMock);
        virtualNetworks.add(networkMock);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.selectApplicableNetworkToCreateVm(accountMock, _dcMock, applicableNetworks);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void selectApplicableNetworkToCreateVmTestVirtualNetworkHasOneNetworkCallsNetworkDaoFindById() throws InsufficientCapacityException, ResourceAllocationException {
        HashSet<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();

        Mockito.doReturn(networkVoListMock).when(networkModel).listNetworksForAccount(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());

        Mockito.doReturn(false).when(networkVoListMock).isEmpty();
        Mockito.doReturn(1).when(networkVoListMock).size();
        Mockito.doReturn(networkMock).when(networkVoListMock).get(0);

        userVmManagerImpl.selectApplicableNetworkToCreateVm(accountMock, _dcMock, applicableNetworks);

        Mockito.verify(_networkDao).findById(Mockito.anyLong());
    }

    @Test
    public void addDefaultSecurityGroupToSecurityGroupIdListTestDefaultGroupIsNullCallsCreateSecurityGroup() {
        String expected = "";
        LinkedList<Long> securityGroupIdList = Mockito.spy(new LinkedList<Long>());

        Mockito.doReturn(null).when(securityGroupManagerMock).getDefaultSecurityGroup(Mockito.anyLong());
        Mockito.doReturn(securityGroupVoMock).when(securityGroupManagerMock).createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME,
                SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION, 1l, 1l, expected);

        Mockito.doReturn(1l).when(accountMock).getDomainId();
        Mockito.doReturn(1l).when(accountMock).getId();
        Mockito.doReturn(expected).when(accountMock).getAccountName();
        Mockito.doReturn(1l).when(securityGroupVoMock).getId();

        userVmManagerImpl.addDefaultSecurityGroupToSecurityGroupIdList(accountMock, securityGroupIdList);

        Mockito.verify(securityGroupManagerMock).createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION, 1l, 1l, expected);
        Mockito.verify(securityGroupIdList).add(1l);
    }

    @Test
    public void addDefaultSecurityGroupToSecurityGroupIdListTestDefaultGroupIsPresentDoesNotCallAddIdToSecurityGroupIdList() {
        LinkedList<Long> securityGroupIdList = Mockito.spy(new LinkedList<Long>());

        securityGroupIdList.addFirst(1l);
        Mockito.doReturn(securityGroupVoMock).when(securityGroupManagerMock).getDefaultSecurityGroup(Mockito.anyLong());
        Mockito.doReturn(1l).when(securityGroupVoMock).getId();

        userVmManagerImpl.addDefaultSecurityGroupToSecurityGroupIdList(accountMock, securityGroupIdList);

        Mockito.verify(securityGroupIdList, Mockito.never()).add(Mockito.anyLong());
    }

    @Test
    public void addDefaultSecurityGroupToSecurityGroupIdListTestDefaultGroupIsNotPresentCallsAddIdToSecurityGroupIdList() {
        LinkedList<Long> securityGroupIdList = Mockito.spy(new LinkedList<Long>());

        Mockito.doReturn(securityGroupVoMock).when(securityGroupManagerMock).getDefaultSecurityGroup(Mockito.anyLong());
        Mockito.doReturn(1l).when(securityGroupVoMock).getId();

        userVmManagerImpl.addDefaultSecurityGroupToSecurityGroupIdList(accountMock, securityGroupIdList);

        Mockito.verify(securityGroupIdList).add(1l);
    }

    @Test
    public void keepOldSharedNetworkForVmTestNetworkIdListIsNotNullOrEmptyDoesNotCallFindDefaultNicForVm() {
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        networkIdList.add(1l);

        userVmManagerImpl.keepOldSharedNetworkForVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(nicDao, Mockito.never()).findDefaultNicForVM(Mockito.anyLong());
    }

    @Test
    public void keepOldSharedNetworkForVmTestNetworkIdListIsNullCallsFindDefaultNicForVm() {
        HashSet<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        userVmManagerImpl.keepOldSharedNetworkForVm(userVmVoMock, accountMock, null, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(nicDao).findDefaultNicForVM(Mockito.anyLong());
    }

    @Test
    public void keepOldSharedNetworkForVmTestNetworkIdListIsEmptyCallsFindDefaultNicForVm() {
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        userVmManagerImpl.keepOldSharedNetworkForVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(nicDao).findDefaultNicForVM(Mockito.anyLong());
    }

    @Test
    public void keepOldSharedNetworkForVmTestDefaultNicOldIsNullDoesNotCallNetworkDaoFindById() {
        HashSet<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        Mockito.doReturn(null).when(nicDao).findDefaultNicForVM(Mockito.anyLong());

        userVmManagerImpl.keepOldSharedNetworkForVm(userVmVoMock, accountMock, null, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(_networkDao, Mockito.never()).findById(Mockito.anyLong());
    }

    @Test
    public void keepOldSharedNetworkForVmTestDefaultNicOldIsNotNullCallsNetworkDaoFindById() {
        HashSet<NetworkVO> applicableNetworks = new HashSet<NetworkVO>();
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        Mockito.doReturn(new NicVO()).when(nicDao).findDefaultNicForVM(Mockito.anyLong());

        userVmManagerImpl.keepOldSharedNetworkForVm(userVmVoMock, accountMock, null, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(_networkDao).findById(Mockito.anyLong());
    }

    @Test
    public void keepOldSharedNetworkForVmTestAccountCanNotUseNetworkDoesNotAddNetworkToApplicableNetworks() {
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        Mockito.doReturn(new NicVO()).when(nicDao).findDefaultNicForVM(Mockito.anyLong());
        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(false).when(userVmManagerImpl).canAccountUseNetwork(accountMock, networkMock);

        userVmManagerImpl.keepOldSharedNetworkForVm(userVmVoMock, accountMock, null, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(applicableNetworks, Mockito.never()).add(Mockito.any());
    }

    @Test
    public void keepOldSharedNetworkForVmTestAccountCanUseNetworkAddsNetworkToApplicableNetworks() {
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        Mockito.doReturn(new NicVO()).when(nicDao).findDefaultNicForVM(Mockito.anyLong());
        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(true).when(userVmManagerImpl).canAccountUseNetwork(accountMock, networkMock);

        userVmManagerImpl.keepOldSharedNetworkForVm(userVmVoMock, accountMock, null, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(applicableNetworks).add(Mockito.any());
    }

    @Test
    public void addAdditionalNetworksToVmTestNetworkIdListIsNullDoesNotCallCheckNetworkPermissions() {
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        userVmManagerImpl.addAdditionalNetworksToVm(userVmVoMock, accountMock, null, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(networkModel, Mockito.never()).checkNetworkPermissions(Mockito.any(), Mockito.any());
    }

    @Test
    public void addAdditionalNetworksToVmTestNetworkIdListIsEmptyDoesNotCallCheckNetworkPermissions() {
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        userVmManagerImpl.addAdditionalNetworksToVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(networkModel, Mockito.never()).checkNetworkPermissions(Mockito.any(), Mockito.any());
    }

    @Test
    public void addAdditionalNetworksToVmTestNetworkIsNullThrowsInvalidParameterValueException() {
        String expectedMessage = "Unable to find specified Network ID.";
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        networkIdList.add(1l);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.addAdditionalNetworksToVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void addAdditionalNetworksToVmTestNetworkOfferingIsSystemOnlyThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Specified network [%s] is system only and cannot be used for VM deployment.", networkMock);
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        networkIdList.add(1l);

        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(networkOfferingVoMock).when(entityManager).findById(Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(true).when(networkOfferingVoMock).isSystemOnly();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.addAdditionalNetworksToVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void addAdditionalNetworksToVmTestNetworkIsNotSharedGuestTypeDoesNotCallNicDaoFindByNtwkIdAndInstanceId() {
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        networkIdList.add(1l);

        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(networkOfferingVoMock).when(entityManager).findById(Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(false).when(networkOfferingVoMock).isSystemOnly();
        Mockito.doReturn(Network.GuestType.L2).when(networkMock).getGuestType();

        userVmManagerImpl.addAdditionalNetworksToVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(nicDao, Mockito.never()).findByNtwkIdAndInstanceId(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void addAdditionalNetworksToVmTestNetworkIsNotDomainAclTypeDoesNotCallNicDaoFindByNtwkIdAndInstanceId() {
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        networkIdList.add(1l);

        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(networkOfferingVoMock).when(entityManager).findById(Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(false).when(networkOfferingVoMock).isSystemOnly();
        Mockito.doReturn(Network.GuestType.Shared).when(networkMock).getGuestType();
        Mockito.doReturn(ControlledEntity.ACLType.Account).when(networkMock).getAclType();

        userVmManagerImpl.addAdditionalNetworksToVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(nicDao, Mockito.never()).findByNtwkIdAndInstanceId(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void addAdditionalNetworksToVmTestOldNicIsNullDoesNotPutIpv4InRequestIpv4ForNics() {
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = Mockito.spy(new HashMap<Long, String>());
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        networkIdList.add(1l);

        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(networkOfferingVoMock).when(entityManager).findById(Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(false).when(networkOfferingVoMock).isSystemOnly();
        Mockito.doReturn(Network.GuestType.Shared).when(networkMock).getGuestType();
        Mockito.doReturn(ControlledEntity.ACLType.Domain).when(networkMock).getAclType();
        Mockito.doReturn(null).when(nicDao).findByNtwkIdAndInstanceId(Mockito.anyLong(), Mockito.anyLong());

        userVmManagerImpl.addAdditionalNetworksToVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(nicDao).findByNtwkIdAndInstanceId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(requestedIPv4ForNics, Mockito.never()).put(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void addAdditionalNetworksToVmTestOldNicIsNotNullPutsIpv4InRequestIpv4ForNics() {
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        HashSet<NetworkVO> applicableNetworks = Mockito.spy(new HashSet<NetworkVO>());
        HashMap<Long, String> requestedIPv4ForNics = Mockito.spy(new HashMap<Long, String>());
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();

        networkIdList.add(1l);

        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(networkOfferingVoMock).when(entityManager).findById(Mockito.any(), Mockito.anyLong());
        Mockito.doReturn(false).when(networkOfferingVoMock).isSystemOnly();
        Mockito.doReturn(Network.GuestType.Shared).when(networkMock).getGuestType();
        Mockito.doReturn(ControlledEntity.ACLType.Domain).when(networkMock).getAclType();
        Mockito.doReturn(new NicVO()).when(nicDao).findByNtwkIdAndInstanceId(Mockito.anyLong(), Mockito.anyLong());

        userVmManagerImpl.addAdditionalNetworksToVm(userVmVoMock, accountMock, networkIdList, applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics);

        Mockito.verify(nicDao).findByNtwkIdAndInstanceId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(requestedIPv4ForNics).put(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void createApplicableNetworkToCreateVmTestPhysicalNetworkIsNullThrowsInvalidParameterValueException() {
        Mockito.doReturn(networkOfferingVoMock).when(userVmManagerImpl).getOfferingWithRequiredAvailabilityForNetworkCreation();

        String expectedMessage = String.format("Unable to find physical network with ID [%s] and tag [%s].", 0l, null);
        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.createApplicableNetworkToCreateVm(accountMock, _dcMock);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void createApplicableNetworkToCreateVmTestFirstNetworkOfferingIsPersistentCallsImplementNetwork() throws InsufficientCapacityException, ResourceAllocationException {
        PhysicalNetworkVO physicalNetworkVo = new PhysicalNetworkVO();

        Mockito.doReturn(physicalNetworkVo).when(physicalNetworkDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(true).when(networkOfferingVoMock).isPersistent();
        Mockito.doReturn(networkOfferingVoMock).when(userVmManagerImpl).getOfferingWithRequiredAvailabilityForNetworkCreation();
        Mockito.doReturn(networkMock).when(userVmManagerImpl).implementNetwork(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(networkMock).when(_networkMgr).createGuestNetwork(Mockito.anyLong(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        userVmManagerImpl.createApplicableNetworkToCreateVm(accountMock, _dcMock);

        Mockito.verify(userVmManagerImpl).implementNetwork(callerAccount, _dcMock, networkMock);
    }

    @Test
    public void createApplicableNetworkToCreateVmTestFirstNetworkOfferingIsNotPersistentDoesNotCallImplementNetwork() throws InsufficientCapacityException,
            ResourceAllocationException {

        PhysicalNetworkVO physicalNetworkVo = new PhysicalNetworkVO();

        Mockito.doReturn(physicalNetworkVo).when(physicalNetworkDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(networkMock).when(_networkMgr).createGuestNetwork(Mockito.anyLong(), Mockito.anyString(),
                Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(networkOfferingVoMock).when(userVmManagerImpl).getOfferingWithRequiredAvailabilityForNetworkCreation();
        Mockito.doReturn(1l).when(networkMock).getId();
        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());

        userVmManagerImpl.createApplicableNetworkToCreateVm(accountMock, _dcMock);

        Mockito.verify(userVmManagerImpl, Mockito.never()).implementNetwork(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void canAccountUseNetworkTestNetworkIsNullReturnFalse() {
        boolean canAccountUseNetwork = userVmManagerImpl.canAccountUseNetwork(accountMock, null);

        Assert.assertFalse(canAccountUseNetwork);
    }

    @Test
    public void canAccountUseNetworkTestNetworkAclTypeIsNotDomainReturnFalse() {
        Mockito.doReturn(ControlledEntity.ACLType.Account).when(networkMock).getAclType();

        boolean canAccountUseNetwork = userVmManagerImpl.canAccountUseNetwork(accountMock, networkMock);

        Assert.assertFalse(canAccountUseNetwork);
    }

    @Test
    public void canAccountUseNetworkTestNetworkGuestTypeIsNotSharedOrL2ReturnFalse() {
        Mockito.doReturn(ControlledEntity.ACLType.Domain).when(networkMock).getAclType();
        Mockito.doReturn(Network.GuestType.Isolated).when(networkMock).getGuestType();

        boolean canAccountUseNetwork = userVmManagerImpl.canAccountUseNetwork(accountMock, networkMock);

        Assert.assertFalse(canAccountUseNetwork);
    }

    @Test
    public void canAccountUseNetworkTestNetworkGuestTypeIsSharedReturnTrue() {
        Mockito.doReturn(ControlledEntity.ACLType.Domain).when(networkMock).getAclType();
        Mockito.doReturn(Network.GuestType.Shared).when(networkMock).getGuestType();

        boolean canAccountUseNetwork = userVmManagerImpl.canAccountUseNetwork(accountMock, networkMock);

        Mockito.verify(networkModel).checkNetworkPermissions(accountMock, networkMock);
        Assert.assertTrue(canAccountUseNetwork);
    }

    @Test
    public void canAccountUseNetworkTestNetworkGuestTypeIsL2ReturnTrue() {
        Mockito.doReturn(ControlledEntity.ACLType.Domain).when(networkMock).getAclType();
        Mockito.doReturn(Network.GuestType.L2).when(networkMock).getGuestType();

        boolean canAccountUseNetwork = userVmManagerImpl.canAccountUseNetwork(accountMock, networkMock);

        Mockito.verify(networkModel).checkNetworkPermissions(accountMock, networkMock);
        Assert.assertTrue(canAccountUseNetwork);
    }

    @Test
    public void canAccountUseNetworkTestPermissionDeniedExceptionThrownReturnFalse() {
        Mockito.doReturn(ControlledEntity.ACLType.Domain).when(networkMock).getAclType();
        Mockito.doReturn(Network.GuestType.L2).when(networkMock).getGuestType();

        Mockito.doThrow(PermissionDeniedException.class).when(networkModel).checkNetworkPermissions(accountMock, networkMock);

        boolean canAccountUseNetwork = userVmManagerImpl.canAccountUseNetwork(accountMock, networkMock);

        Assert.assertFalse(canAccountUseNetwork);
    }

    @Test
    public void implementNetworkTestImplementedNetworkIsNullReturnCurrentNewNetwork() throws ResourceUnavailableException, InsufficientCapacityException {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        NetworkVO currentNetwork = Mockito.mock(NetworkVO.class);

        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(CallContext.current()).thenReturn(callContextMock);

            Mockito.doReturn(1l).when(callContextMock).getCallingUserId();

            Mockito.doReturn(callerUser).when(userDao).findById(Mockito.anyLong());
            Mockito.doReturn(null).when(_networkMgr).implementNetwork(Mockito.anyLong(), Mockito.any(), Mockito.any());

            Network newNetwork = userVmManagerImpl.implementNetwork(accountMock, _dcMock, currentNetwork);

            Assert.assertEquals(newNetwork, currentNetwork);
        }
    }

    @Test
    public void implementNetworkTestImplementedNetworkFirstIsNullReturnCurrentNewNetwork() throws ResourceUnavailableException, InsufficientCapacityException {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        NetworkVO currentNetwork = Mockito.mock(NetworkVO.class);

        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(CallContext.current()).thenReturn(callContextMock);

            Mockito.doReturn(1l).when(callContextMock).getCallingUserId();

            Pair<? extends NetworkGuru, ? extends Network> implementedNetwork = Mockito.mock(Pair.class);

            Mockito.doReturn(callerUser).when(userDao).findById(Mockito.anyLong());
            Mockito.doReturn(null).when(implementedNetwork).first();
            Mockito.doReturn(implementedNetwork).when(_networkMgr).implementNetwork(Mockito.anyLong(), Mockito.any(), Mockito.any());

            Network newNetwork = userVmManagerImpl.implementNetwork(accountMock, _dcMock, currentNetwork);

            Assert.assertEquals(newNetwork, currentNetwork);
        }
    }

    @Test
    public void implementNetworkTestImplementedNetworkSecondIsNullReturnCurrentNewNetwork() throws ResourceUnavailableException, InsufficientCapacityException {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        NetworkVO currentNetwork = Mockito.mock(NetworkVO.class);

        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(CallContext.current()).thenReturn(callContextMock);

            Mockito.doReturn(1l).when(callContextMock).getCallingUserId();

            Pair<? extends NetworkGuru, ? extends Network> implementedNetwork = Mockito.mock(Pair.class);

            Mockito.doReturn(callerUser).when(userDao).findById(Mockito.anyLong());
            Mockito.doReturn(networkMock).when(implementedNetwork).first();
            Mockito.doReturn(null).when(implementedNetwork).second();
            Mockito.doReturn(implementedNetwork).when(_networkMgr).implementNetwork(Mockito.anyLong(), Mockito.any(), Mockito.any());

            Network newNetwork = userVmManagerImpl.implementNetwork(accountMock, _dcMock, currentNetwork);

            Assert.assertEquals(newNetwork, currentNetwork);
        }
    }

    @Test
    public void implementNetworkTestImplementedNetworkSecondIsNotNullReturnImplementedNetworkSecond() throws ResourceUnavailableException, InsufficientCapacityException {
        CallContext callContextMock = Mockito.mock(CallContext.class);
        NetworkVO currentNetwork = Mockito.mock(NetworkVO.class);

        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(CallContext.current()).thenReturn(callContextMock);

            Mockito.doReturn(1l).when(callContextMock).getCallingUserId();

            Pair<? extends NetworkGuru, ? extends Network> implementedNetwork = Mockito.mock(Pair.class);

            Mockito.doReturn(callerUser).when(userDao).findById(Mockito.anyLong());
            Mockito.doReturn(networkMock).when(implementedNetwork).first();
            Mockito.doReturn(networkMock).when(implementedNetwork).second();
            Mockito.doReturn(implementedNetwork).when(_networkMgr).implementNetwork(Mockito.anyLong(), Mockito.any(), Mockito.any());

            Network newNetwork = userVmManagerImpl.implementNetwork(accountMock, _dcMock, currentNetwork);

            Assert.assertEquals(newNetwork, networkMock);
        }
    }

    @Test
    public void implementNetworkTestImplementedNetworkCatchException() throws ResourceUnavailableException, InsufficientCapacityException {
        String expectedMessage = String.format("Failed to implement network [%s] elements and resources as a part of network provision.", networkMock);

        CallContext callContextMock = Mockito.mock(CallContext.class);

        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            Mockito.when(CallContext.current()).thenReturn(callContextMock);

            Mockito.doReturn(1l).when(callContextMock).getCallingUserId();

            Pair<? extends NetworkGuru, ? extends Network> implementedNetwork = Mockito.mock(Pair.class);

            Mockito.doReturn(callerUser).when(userDao).findById(Mockito.anyLong());
            Mockito.doThrow(InvalidParameterValueException.class).when(_networkMgr).implementNetwork(Mockito.anyLong(), Mockito.any(), Mockito.any());

            CloudRuntimeException assertThrows = Assert.assertThrows(expectedCloudRuntimeException, () -> {
                userVmManagerImpl.implementNetwork(accountMock, _dcMock, networkMock);
            });

            Assert.assertEquals(expectedMessage, assertThrows.getMessage());
        }
    }

    @Test
    public void updateBasicTypeNetworkForVmTestNetworkIdListIsNotEmptyThrowsInvalidParameterValueException() {
        String expectedMessage = "Cannot move VM with Network IDs; this is a basic zone VM.";
        LinkedList<Long> networkIdList = new LinkedList<Long>();
        LinkedList<Long> securityGroupIdList = new LinkedList<Long>();

        networkIdList.add(1l);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.updateBasicTypeNetworkForVm(userVmVoMock, accountMock, virtualMachineTemplateMock, virtualMachineProfileMock, _dcMock, networkIdList,
                    securityGroupIdList);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void updateBasicTypeNetworkForVmTestNetworkIdListIsNullCallsCleanupOfOldOwnerNicsForNetworkAddDefaultNetworkToNetworkListAllocateNetworksForVmAndAddSecurityGroupsToVm()
            throws InsufficientCapacityException {

        LinkedList<Long> securityGroupIdList = Mockito.mock(LinkedList.class);

        Mockito.doReturn(networkMock).when(networkModel).getExclusiveGuestNetwork(Mockito.anyLong());

        userVmManagerImpl.updateBasicTypeNetworkForVm(userVmVoMock, accountMock, virtualMachineTemplateMock, virtualMachineProfileMock, _dcMock, null,
                securityGroupIdList);

        Mockito.verify(userVmManagerImpl).cleanupOfOldOwnerNicsForNetwork(virtualMachineProfileMock);
        Mockito.verify(userVmManagerImpl).addDefaultNetworkToNetworkList(Mockito.anyList(), Mockito.any());
        Mockito.verify(userVmManagerImpl).allocateNetworksForVm(Mockito.any(), Mockito.any());
        Mockito.verify(userVmManagerImpl).addSecurityGroupsToVm(accountMock, userVmVoMock,virtualMachineTemplateMock, securityGroupIdList, networkMock);
    }

    @Test
    public void updateBasicTypeNetworkForVmTestNetworkIdListIsEmptyCallsCleanupOfOldOwnerNicsForNetworkAddDefaultNetworkToNetworkListAllocateNetworksForVmAndAddSecurityGroupsToVm()
            throws InsufficientCapacityException {

        LinkedList<Long> securityGroupIdList = Mockito.mock(LinkedList.class);
        LinkedList<Long> networkIdList = new LinkedList<Long>();

        Mockito.doReturn(networkMock).when(networkModel).getExclusiveGuestNetwork(Mockito.anyLong());

        userVmManagerImpl.updateBasicTypeNetworkForVm(userVmVoMock, accountMock, virtualMachineTemplateMock, virtualMachineProfileMock, _dcMock, networkIdList,
                securityGroupIdList);

        Mockito.verify(userVmManagerImpl).cleanupOfOldOwnerNicsForNetwork(virtualMachineProfileMock);
        Mockito.verify(userVmManagerImpl).addDefaultNetworkToNetworkList(Mockito.anyList(), Mockito.any());
        Mockito.verify(userVmManagerImpl).allocateNetworksForVm(Mockito.any(), Mockito.any());
        Mockito.verify(userVmManagerImpl).addSecurityGroupsToVm(accountMock, userVmVoMock,virtualMachineTemplateMock, securityGroupIdList, networkMock);
    }

    @Test
    public void updateAdvancedTypeNetworkForVmTestSecurityGroupIsEnabledApplicableNetworksIsEmptyThrowsInvalidParameterValueException() {
        String expectedMessage = "No network is specified, please specify one when you move the VM. For now, please add a network to VM on NICs tab.";
        LinkedList<Long> securityGroupIdList = Mockito.mock(LinkedList.class);
        LinkedList<Long> networkIdList = new LinkedList<Long>();

        Mockito.doReturn(true).when(networkModel).checkSecurityGroupSupportForNetwork(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.updateAdvancedTypeNetworkForVm(callerAccount, userVmVoMock, accountMock, virtualMachineTemplateMock, virtualMachineProfileMock,
                    _dcMock, networkIdList, securityGroupIdList);
        });

        Mockito.verify(securityGroupManagerMock).removeInstanceFromGroups(Mockito.any());
        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void updateAdvancedTypeNetworkForVmTestSecurityGroupIsEnabledApplicableNetworksIsNotEmptyCallsAllocateNetworksForVm() throws InsufficientCapacityException,
            ResourceAllocationException {

        LinkedList<Long> securityGroupIdList = Mockito.mock(LinkedList.class);
        LinkedList<Long> networkIdList = new LinkedList<Long>();

        Mockito.doReturn(new NicVO()).when(nicDao).findDefaultNicForVM(Mockito.anyLong());
        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(true).when(userVmManagerImpl).canAccountUseNetwork(accountMock, networkMock);

        Mockito.doReturn(true).when(networkModel).checkSecurityGroupSupportForNetwork(accountMock, _dcMock, networkIdList, securityGroupIdList);

        userVmManagerImpl.updateAdvancedTypeNetworkForVm(callerAccount, userVmVoMock, accountMock, virtualMachineTemplateMock, virtualMachineProfileMock, _dcMock,
                networkIdList, securityGroupIdList);

        Mockito.verify(securityGroupManagerMock).removeInstanceFromGroups(Mockito.any());
        Mockito.verify(userVmManagerImpl).allocateNetworksForVm(Mockito.any(), Mockito.any());
        Mockito.verify(userVmManagerImpl).addSecurityGroupsToVm(accountMock, userVmVoMock, virtualMachineTemplateMock, securityGroupIdList, networkMock);
    }

    @Test
    public void updateAdvancedTypeNetworkForVmTestSecurityGroupIsNotEnabledSecurityGroupIdListIsNotEmptyThrowsInvalidParameterValueException() {
        String expectedMessage = "Cannot move VM with security groups; security group feature is not enabled in this zone.";
        LinkedList<Long> securityGroupIdList = Mockito.mock(LinkedList.class);
        LinkedList<Long> networkIdList = new LinkedList<Long>();

        securityGroupIdList.add(1l);

        Mockito.doReturn(false).when(networkModel).checkSecurityGroupSupportForNetwork(accountMock, _dcMock, networkIdList, securityGroupIdList);

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.updateAdvancedTypeNetworkForVm(callerAccount, userVmVoMock, accountMock, virtualMachineTemplateMock, virtualMachineProfileMock,
                    _dcMock, networkIdList, securityGroupIdList);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void updateAdvancedTypeNetworkForVmTestSecurityGroupIsNotEnabledApplicableNetworksIsEmptyCallsSelectApplicableNetworkToCreateVm() throws InsufficientCapacityException,
            ResourceAllocationException {

        LinkedList<Long> securityGroupIdList = Mockito.mock(LinkedList.class);
        LinkedList<Long> networkIdList = new LinkedList<Long>();

        Mockito.doReturn(networkMock).when(userVmManagerImpl).addNicsToApplicableNetworksAndReturnDefaultNetwork(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any());
        Mockito.doNothing().when(userVmManagerImpl).selectApplicableNetworkToCreateVm(Mockito.any(), Mockito.any(), Mockito.any());

        Mockito.doReturn(false).when(networkModel).checkSecurityGroupSupportForNetwork(accountMock, _dcMock, networkIdList, securityGroupIdList);
        Mockito.doReturn(true).when(securityGroupIdList).isEmpty();

        userVmManagerImpl.updateAdvancedTypeNetworkForVm(callerAccount, userVmVoMock, accountMock, virtualMachineTemplateMock, virtualMachineProfileMock, _dcMock,
                networkIdList, securityGroupIdList);

        Mockito.verify(userVmManagerImpl).addNetworksToNetworkIdList(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap());
        Mockito.verify(userVmManagerImpl).cleanupOfOldOwnerNicsForNetwork(Mockito.any());
        Mockito.verify(userVmManagerImpl).selectApplicableNetworkToCreateVm(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(userVmManagerImpl).addNicsToApplicableNetworksAndReturnDefaultNetwork(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any());
        Mockito.verify(userVmManagerImpl).allocateNetworksForVm(Mockito.any(), Mockito.any());
    }

    @Test
    public void updateAdvancedTypeNetworkForVmTestSecurityGroupIsNotEnabledApplicableNetworksIsNotEmptyDoesNotCallSelectApplicableNetworkToCreateVm()
            throws InsufficientCapacityException, ResourceAllocationException {

        LinkedList<Long> securityGroupIdList = Mockito.mock(LinkedList.class);
        LinkedList<Long> networkIdList = new LinkedList<Long>();

        Mockito.doReturn(false).when(networkModel).checkSecurityGroupSupportForNetwork(accountMock, _dcMock, networkIdList, securityGroupIdList);
        Mockito.doReturn(true).when(securityGroupIdList).isEmpty();

        Mockito.doReturn(new NicVO()).when(nicDao).findDefaultNicForVM(Mockito.anyLong());
        Mockito.doReturn(networkMock).when(_networkDao).findById(Mockito.anyLong());
        Mockito.doReturn(true).when(userVmManagerImpl).canAccountUseNetwork(accountMock, networkMock);

        userVmManagerImpl.updateAdvancedTypeNetworkForVm(callerAccount, userVmVoMock, accountMock, virtualMachineTemplateMock, virtualMachineProfileMock, _dcMock,
                networkIdList, securityGroupIdList);

        Mockito.verify(userVmManagerImpl).addNetworksToNetworkIdList(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap(), Mockito.anyMap());
        Mockito.verify(userVmManagerImpl).cleanupOfOldOwnerNicsForNetwork(Mockito.any());
        Mockito.verify(userVmManagerImpl, Mockito.never()).selectApplicableNetworkToCreateVm(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.verify(userVmManagerImpl).addNicsToApplicableNetworksAndReturnDefaultNetwork(Mockito.any(), Mockito.anyMap(), Mockito.anyMap(), Mockito.any());
        Mockito.verify(userVmManagerImpl).allocateNetworksForVm(Mockito.any(), Mockito.any());
    }

    @Test
    public void addNicsToApplicableNetworksAndReturnDefaultNetworkTestApplicableNetworkIsEmptyReturnNull() {
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();
        LinkedHashSet<NetworkVO> applicableNetworks = new LinkedHashSet<NetworkVO>();
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>();

        NetworkVO defaultNetwork = userVmManagerImpl.addNicsToApplicableNetworksAndReturnDefaultNetwork(applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics, networks);

        Assert.assertNull(defaultNetwork);
    }

    @Test
    public void addNicsToApplicableNetworksAndReturnDefaultNetworkTestApplicableNetworkIsNotEmptyReturnFirstElement() {
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();
        LinkedHashSet<NetworkVO> applicableNetworks = new LinkedHashSet<NetworkVO>();
        LinkedHashMap<Network, List<? extends NicProfile>> networks = Mockito.spy(LinkedHashMap.class);

        applicableNetworks.add(networkMock);

        NetworkVO defaultNetwork = userVmManagerImpl.addNicsToApplicableNetworksAndReturnDefaultNetwork(applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics, networks);

        Mockito.verify(networks).put(Mockito.any(), Mockito.any());
        Assert.assertEquals(defaultNetwork, networkMock);
    }

    @Test
    public void addNicsToApplicableNetworksAndReturnDefaultNetworkTestApplicableNetworkIsNotEmptyPutTwoNetworksInNetworksMapAndReturnFirst() {
        HashMap<Long, String> requestedIPv4ForNics = new HashMap<Long, String>();
        HashMap<Long, String> requestedIPv6ForNics = new HashMap<Long, String>();
        LinkedHashSet<NetworkVO> applicableNetworks = new LinkedHashSet<NetworkVO>();
        LinkedHashMap<Network, List<? extends NicProfile>> networks = Mockito.spy(LinkedHashMap.class);

        NetworkVO networkVoMock2 = Mockito.mock(NetworkVO.class);
        applicableNetworks.add(networkMock);
        applicableNetworks.add(networkVoMock2);

        NetworkVO defaultNetwork = userVmManagerImpl.addNicsToApplicableNetworksAndReturnDefaultNetwork(applicableNetworks, requestedIPv4ForNics, requestedIPv6ForNics, networks);

        Mockito.verify(networks, times(2)).put(Mockito.any(), Mockito.any());
        Assert.assertEquals(defaultNetwork, networkMock);
    }

    @Test
    public void validateIfVolumesHaveNoSnapshotsTestVolumeHasSnapshotsThrowsInvalidParameterException() {
        String expectedMessage = String.format("Snapshots exist for volume [%s]. Detach volume or remove snapshots for the volume before assigning VM to another user.",
                volumeVOMock.getName());

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();
        volumes.add(volumeVOMock);
        LinkedList<SnapshotVO> snapshots = new LinkedList<SnapshotVO>();
        snapshots.add(snapshotVoMock);

        Mockito.doReturn(snapshots).when(snapshotDaoMock).listByStatusNotIn(Mockito.anyLong(), Mockito.any(), Mockito.any());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.validateIfVolumesHaveNoSnapshots(volumes);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void validateIfVolumesHaveNoSnapshotsTestVolumeHasNoSnapshotsDoesNotThrowInvalidParameterException() {
        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();
        volumes.add(volumeVOMock);
        LinkedList<SnapshotVO> snapshots = new LinkedList<SnapshotVO>();

        Mockito.doReturn(snapshots).when(snapshotDaoMock).listByStatusNotIn(Mockito.anyLong(), Mockito.any(), Mockito.any());

        userVmManagerImpl.validateIfVolumesHaveNoSnapshots(volumes);
    }

    @Test
    public void moveVmToUserTestCallerIsNotRootAdminAndDomainAdminThrowsInvalidParameterValueException() {
        String expectedMessage = String.format("Only root or domain admins are allowed to assign VMs. Caller [%s] is of type [%s].", callerAccount, callerAccount.getType());

        Mockito.doReturn(false).when(accountManager).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(false).when(accountManager).isDomainAdmin(Mockito.anyLong());

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.moveVmToUser(assignVmCmdMock);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void moveVmToUserTestValidateVmExistsAndIsNotRunningThrowsInvalidParameterValueException() {
        Mockito.doReturn(true).when(accountManager).isRootAdmin(Mockito.anyLong());

        Mockito.doThrow(InvalidParameterValueException.class).when(userVmManagerImpl).validateIfVmSupportsMigration(Mockito.any(), Mockito.anyLong());

        Assert.assertThrows(InvalidParameterValueException.class, () -> userVmManagerImpl.moveVmToUser(assignVmCmdMock));
    }

    @Test
    public void moveVmToUserTestValidateAccountsAndCallerAccessToThemThrowsInvalidParameterValueException() {
        Mockito.doReturn(true).when(accountManager).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(userVmVoMock).when(userVmDao).findById(Mockito.anyLong());

        Assert.assertThrows(InvalidParameterValueException.class, () -> userVmManagerImpl.moveVmToUser(assignVmCmdMock));
    }

    @Test
    public void moveVmToUserTestProjectIdProvidedAndDomainIdIsNullThrowsInvalidParameterValueException() throws ResourceUnavailableException, InsufficientCapacityException,
            ResourceAllocationException {

        String expectedMessage = "Please provide a valid domain ID; cannot assign VM to a project if domain ID is NULL.";

        Mockito.doReturn(true).when(accountManager).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(userVmVoMock).when(userVmDao).findById(Mockito.anyLong());
        Mockito.doReturn(1l).when(assignVmCmdMock).getProjectId();
        Mockito.doReturn(null).when(assignVmCmdMock).getDomainId();

        configureDoNothingForMethodsThatWeDoNotWantToTest();

        InvalidParameterValueException assertThrows = Assert.assertThrows(expectedInvalidParameterValueException, () -> {
            userVmManagerImpl.moveVmToUser(assignVmCmdMock);
        });

        Assert.assertEquals(expectedMessage, assertThrows.getMessage());
    }

    @Test
    public void moveVmToUserTestValidateIfVmHasNoRulesThrowsInvalidParameterValueException() throws ResourceUnavailableException, InsufficientCapacityException,
            ResourceAllocationException {

        Mockito.doReturn(true).when(accountManager).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(userVmVoMock).when(userVmDao).findById(Mockito.anyLong());
        Mockito.doReturn(null).when(assignVmCmdMock).getProjectId();

        configureDoNothingForMethodsThatWeDoNotWantToTest();

        Mockito.doThrow(InvalidParameterValueException.class).when(userVmManagerImpl).validateIfVmHasNoRules(Mockito.any(), Mockito.anyLong());

        Assert.assertThrows(InvalidParameterValueException.class, () -> userVmManagerImpl.moveVmToUser(assignVmCmdMock));
    }

    @Test
    public void moveVmToUserTestSnapshotsForVolumeExistThrowsInvalidParameterValueException() throws ResourceUnavailableException, InsufficientCapacityException,
            ResourceAllocationException {

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();
        volumes.add(volumeVOMock);

        Mockito.doReturn(true).when(accountManager).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(userVmVoMock).when(userVmDao).findById(Mockito.anyLong());
        Mockito.doReturn(null).when(assignVmCmdMock).getProjectId();
        Mockito.doReturn(volumes).when(volumeDaoMock).findByInstance(Mockito.anyLong());

        configureDoNothingForMethodsThatWeDoNotWantToTest();

        Mockito.doThrow(InvalidParameterValueException.class).when(userVmManagerImpl).validateIfVolumesHaveNoSnapshots(Mockito.any());

        Assert.assertThrows(InvalidParameterValueException.class, () -> userVmManagerImpl.moveVmToUser(assignVmCmdMock));
    }

    @Test
    public void moveVmToUserTestVerifyResourceLimitsForAccountAndStorageThrowsResourceAllocationException() throws ResourceUnavailableException, InsufficientCapacityException,
            ResourceAllocationException {

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();

        Mockito.doReturn(true).when(accountManager).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(userVmVoMock).when(userVmDao).findById(Mockito.anyLong());
        Mockito.doReturn(null).when(assignVmCmdMock).getProjectId();
        Mockito.doReturn(volumes).when(volumeDaoMock).findByInstance(Mockito.anyLong());

        configureDoNothingForMethodsThatWeDoNotWantToTest();

        Mockito.doThrow(ResourceAllocationException.class).when(userVmManagerImpl).verifyResourceLimitsForAccountAndStorage(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any());

        Assert.assertThrows(ResourceAllocationException.class, () -> userVmManagerImpl.moveVmToUser(assignVmCmdMock));
    }

    @Test
    public void moveVmToUserTestVerifyValidateIfNewOwnerHasAccessToTemplateThrowsInvalidParameterValueException() throws ResourceUnavailableException,
            InsufficientCapacityException, ResourceAllocationException {

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();

        Mockito.doReturn(true).when(accountManager).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(userVmVoMock).when(userVmDao).findById(Mockito.anyLong());
        Mockito.doReturn(null).when(assignVmCmdMock).getProjectId();
        Mockito.doReturn(volumes).when(volumeDaoMock).findByInstance(Mockito.anyLong());

        configureDoNothingForMethodsThatWeDoNotWantToTest();

        Mockito.doThrow(InvalidParameterValueException.class).when(userVmManagerImpl).validateIfNewOwnerHasAccessToTemplate(Mockito.any(), Mockito.any(), Mockito.any());

        Assert.assertThrows(InvalidParameterValueException.class, () -> userVmManagerImpl.moveVmToUser(assignVmCmdMock));
    }

    @Test
    public void moveVmToUserTestAccountManagerCheckAccessThrowsPermissionDeniedException() throws ResourceUnavailableException, InsufficientCapacityException,
            ResourceAllocationException {

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();

        Mockito.doReturn(true).when(accountManager).isRootAdmin(Mockito.anyLong());
        Mockito.doReturn(userVmVoMock).when(userVmDao).findById(Mockito.anyLong());
        Mockito.doReturn(null).when(assignVmCmdMock).getProjectId();
        Mockito.doReturn(volumes).when(volumeDaoMock).findByInstance(Mockito.anyLong());
        Mockito.doReturn(accountMock).when(accountManager).finalizeOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(domainVoMock).when(domainDaoMock).findById(Mockito.anyLong());

        configureDoNothingForMethodsThatWeDoNotWantToTest();

        Mockito.doThrow(PermissionDeniedException.class).when(accountManager).checkAccess(Mockito.any(Account.class), Mockito.any());

        Assert.assertThrows(PermissionDeniedException.class, () -> userVmManagerImpl.moveVmToUser(assignVmCmdMock));
    }

    @Test
    public void executeStepsToChangeOwnershipOfVmTestUpdateVmNetworkThrowsInsufficientCapacityException() throws ResourceUnavailableException, InsufficientCapacityException,
            ResourceAllocationException {

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();

        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
            Mockito.doReturn(Hypervisor.HypervisorType.KVM).when(userVmVoMock).getHypervisorType();

            configureDoNothingForMethodsThatWeDoNotWantToTest();

            Mockito.doThrow(InsufficientAddressCapacityException.class).when(userVmManagerImpl).updateVmNetwork(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                    Mockito.any());

            Assert.assertThrows(CloudRuntimeException.class, () -> userVmManagerImpl.executeStepsToChangeOwnershipOfVm(assignVmCmdMock, callerAccount, accountMock, accountMock,
                    userVmVoMock, serviceOfferingVoMock, volumes, virtualMachineTemplateMock, 1l));

            Mockito.verify(userVmManagerImpl).resourceCountDecrement(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(userVmManagerImpl).updateVmOwner(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong());
            Mockito.verify(userVmManagerImpl).updateVolumesOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());
        }
    }

    @Test
    public void executeStepsToChangeOwnershipOfVmTestUpdateVmNetworkThrowsResourceAllocationException() throws ResourceUnavailableException, InsufficientCapacityException,
            ResourceAllocationException {

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();

        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
            Mockito.doReturn(Hypervisor.HypervisorType.KVM).when(userVmVoMock).getHypervisorType();

            configureDoNothingForMethodsThatWeDoNotWantToTest();

            Mockito.doThrow(ResourceAllocationException.class).when(userVmManagerImpl).updateVmNetwork(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),
                    Mockito.any());

            Assert.assertThrows(CloudRuntimeException.class, () -> userVmManagerImpl.executeStepsToChangeOwnershipOfVm(assignVmCmdMock, callerAccount, accountMock, accountMock,
                    userVmVoMock, serviceOfferingVoMock, volumes, virtualMachineTemplateMock, 1l));

            Mockito.verify(userVmManagerImpl).resourceCountDecrement(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(userVmManagerImpl).updateVmOwner(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong());
            Mockito.verify(userVmManagerImpl).updateVolumesOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());
        }
    }

    @Test
    public void executeStepsToChangeOwnershipOfVmTestResourceCountRunningVmsOnlyEnabledIsFalseCallsResourceCountIncrement() throws ResourceUnavailableException,
            InsufficientCapacityException, ResourceAllocationException {

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();


        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
            Mockito.doReturn(Hypervisor.HypervisorType.KVM).when(userVmVoMock).getHypervisorType();
            Mockito.doReturn(false).when(userVmManagerImpl).isResourceCountRunningVmsOnlyEnabled();

            configureDoNothingForMethodsThatWeDoNotWantToTest();

            userVmManagerImpl.executeStepsToChangeOwnershipOfVm(assignVmCmdMock, callerAccount, accountMock, accountMock, userVmVoMock, serviceOfferingVoMock, volumes,
                    virtualMachineTemplateMock, 1L);

            Mockito.verify(userVmManagerImpl).resourceCountDecrement(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(userVmManagerImpl).updateVmOwner(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong());
            Mockito.verify(userVmManagerImpl).updateVolumesOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());
            Mockito.verify(userVmManagerImpl).updateVmNetwork(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(userVmManagerImpl).resourceCountIncrement(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());
        }
    }

    @Test
    public void executeStepsToChangeOwnershipOfVmTestResourceCountRunningVmsOnlyEnabledIsTrueDoesNotCallResourceCountIncrement() throws ResourceUnavailableException,
            InsufficientCapacityException, ResourceAllocationException {

        LinkedList<VolumeVO> volumes = new LinkedList<VolumeVO>();

        try (MockedStatic<UsageEventUtils> ignored = Mockito.mockStatic(UsageEventUtils.class)) {
            Mockito.doReturn(Hypervisor.HypervisorType.KVM).when(userVmVoMock).getHypervisorType();
            Mockito.doReturn(true).when(userVmManagerImpl).isResourceCountRunningVmsOnlyEnabled();

            configureDoNothingForMethodsThatWeDoNotWantToTest();

            userVmManagerImpl.executeStepsToChangeOwnershipOfVm(assignVmCmdMock, callerAccount, accountMock, accountMock, userVmVoMock, serviceOfferingVoMock, volumes,
                    virtualMachineTemplateMock, 1l);

            Mockito.verify(userVmManagerImpl).resourceCountDecrement(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(userVmManagerImpl).updateVmOwner(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong());
            Mockito.verify(userVmManagerImpl).updateVolumesOwner(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyLong());
            Mockito.verify(userVmManagerImpl).updateVmNetwork(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
            Mockito.verify(userVmManagerImpl, Mockito.never()).resourceCountIncrement(Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any());
        }
    }

    @Test
    public void validateStorageAccessGroupsOnHostsMatchingSAGsNoException() {
        Host srcHost = Mockito.mock(Host.class);
        Host destHost = Mockito.mock(Host.class);

        Mockito.when(srcHost.getId()).thenReturn(1L);
        Mockito.when(destHost.getId()).thenReturn(2L);
        when(storageManager.getStorageAccessGroups(null, null, null, srcHost.getId())).thenReturn(new String[]{"sag1", "sag2"});
        when(storageManager.getStorageAccessGroups(null, null, null, destHost.getId())).thenReturn(new String[]{"sag1", "sag2", "sag3"});

        userVmManagerImpl.validateStorageAccessGroupsOnHosts(srcHost, destHost);

        Mockito.verify(storageManager, times(1)).getStorageAccessGroups(null, null, null, srcHost.getId());
        Mockito.verify(storageManager, times(1)).getStorageAccessGroups(null, null, null, destHost.getId());
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateSAGsOnHostsNonMatchingSAGsThrowsException() {
        Host srcHost = Mockito.mock(Host.class);
        Host destHost = Mockito.mock(Host.class);

        Mockito.when(srcHost.getId()).thenReturn(1L);
        Mockito.when(destHost.getId()).thenReturn(2L);
        when(storageManager.getStorageAccessGroups(null, null, null, srcHost.getId())).thenReturn(new String[]{"sag1", "sag2"});
        when(storageManager.getStorageAccessGroups(null, null, null, destHost.getId())).thenReturn(new String[]{"sag1", "sag3"});

        userVmManagerImpl.validateStorageAccessGroupsOnHosts(srcHost, destHost);
    }

    @Test
    public void validateEmptyStorageAccessGroupOnHosts() {
        Host srcHost = Mockito.mock(Host.class);
        Host destHost = Mockito.mock(Host.class);

        Mockito.when(srcHost.getId()).thenReturn(1L);
        Mockito.when(destHost.getId()).thenReturn(2L);
        when(storageManager.getStorageAccessGroups(null, null, null, srcHost.getId())).thenReturn(new String[]{});
        when(storageManager.getStorageAccessGroups(null, null, null, destHost.getId())).thenReturn(new String[]{});

        userVmManagerImpl.validateStorageAccessGroupsOnHosts(srcHost, destHost);

        Mockito.verify(storageManager, times(1)).getStorageAccessGroups(null, null, null, srcHost.getId());
        Mockito.verify(storageManager, times(1)).getStorageAccessGroups(null, null, null, destHost.getId());
    }

    @Test
    public void validateSAGsOnHostsNullStorageAccessGroups() {
        Host srcHost = Mockito.mock(Host.class);
        Host destHost = Mockito.mock(Host.class);

        Mockito.when(srcHost.getId()).thenReturn(1L);
        Mockito.when(destHost.getId()).thenReturn(2L);
        when(storageManager.getStorageAccessGroups(null, null, null, srcHost.getId())).thenReturn(null);
        when(storageManager.getStorageAccessGroups(null, null, null, destHost.getId())).thenReturn(null);

        userVmManagerImpl.validateStorageAccessGroupsOnHosts(srcHost, destHost);

        Mockito.verify(storageManager, times(1)).getStorageAccessGroups(null, null, null, srcHost.getId());
        Mockito.verify(storageManager, times(1)).getStorageAccessGroups(null, null, null, destHost.getId());
    }

    @Test(expected = CloudRuntimeException.class)
    public void validateSAGsOnDestHostNullStorageAccessGroups() {
        Host srcHost = Mockito.mock(Host.class);
        Host destHost = Mockito.mock(Host.class);

        Mockito.when(srcHost.getId()).thenReturn(1L);
        Mockito.when(destHost.getId()).thenReturn(2L);
        when(storageManager.getStorageAccessGroups(null, null, null, srcHost.getId())).thenReturn(new String[]{"sag1", "sag2"});
        when(storageManager.getStorageAccessGroups(null, null, null, destHost.getId())).thenReturn(null);

        userVmManagerImpl.validateStorageAccessGroupsOnHosts(srcHost, destHost);
    }

    @Test
    public void validateNullStorageAccessGroupsOnSrcHost() {

        Host srcHost = Mockito.mock(Host.class);
        Host destHost = Mockito.mock(Host.class);

        Mockito.when(srcHost.getId()).thenReturn(1L);
        Mockito.when(destHost.getId()).thenReturn(2L);
        when(storageManager.getStorageAccessGroups(null, null, null, srcHost.getId())).thenReturn(null);
        when(storageManager.getStorageAccessGroups(null, null, null, destHost.getId())).thenReturn(new String[]{"sag1", "sag2"});

        userVmManagerImpl.validateStorageAccessGroupsOnHosts(srcHost, destHost);

        Mockito.verify(storageManager, times(1)).getStorageAccessGroups(null, null, null, srcHost.getId());
        Mockito.verify(storageManager, times(1)).getStorageAccessGroups(null, null, null, destHost.getId());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateLeasePropertiesInvalidDuration() {
        userVmManagerImpl.validateLeaseProperties(-2, VMLeaseManager.ExpiryAction.STOP);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateLeasePropertiesNullActionValue() {
        userVmManagerImpl.validateLeaseProperties(20, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateLeasePropertiesNullDurationValue() {
        userVmManagerImpl.validateLeaseProperties(null, VMLeaseManager.ExpiryAction.STOP);
    }

    @Test
    public void testValidateLeasePropertiesMinusOneDuration() {
        userVmManagerImpl.validateLeaseProperties(-1, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateLeasePropertiesZeroDayDuration() {
        userVmManagerImpl.validateLeaseProperties(0, VMLeaseManager.ExpiryAction.STOP);
    }

    @Test
    public void testValidateLeasePropertiesValidValues() {
        userVmManagerImpl.validateLeaseProperties(20, VMLeaseManager.ExpiryAction.STOP);
    }

    @Test
    public void testValidateLeasePropertiesBothNUll() {
        userVmManagerImpl.validateLeaseProperties(null, null);
    }

    @Test
    public void testAddLeaseDetailsForInstance() {
        UserVm userVm = mock(UserVm.class);
        when(userVm.getId()).thenReturn(vmId);
        when(userVm.getUuid()).thenReturn(UUID.randomUUID().toString());
        userVmManagerImpl.addLeaseDetailsForInstance(userVm, 10, VMLeaseManager.ExpiryAction.STOP);
        verify(userVmDetailsDao).addDetail(eq(vmId), eq(VmDetailConstants.INSTANCE_LEASE_EXPIRY_ACTION), eq(VMLeaseManager.ExpiryAction.STOP.name()), anyBoolean());
        verify(userVmDetailsDao).addDetail(eq(vmId), eq(VmDetailConstants.INSTANCE_LEASE_EXPIRY_DATE), eq(getLeaseExpiryDate(10L)), anyBoolean());
    }

    @Test
    public void testAddNullDurationLeaseDetailsForInstance() {
        UserVm userVm = mock(UserVm.class);
        userVmManagerImpl.addLeaseDetailsForInstance(userVm, null, VMLeaseManager.ExpiryAction.STOP);
        Mockito.verify(userVmDetailsDao, Mockito.times(0)).removeDetail(vmId, VmDetailConstants.INSTANCE_LEASE_EXPIRY_ACTION);
        Mockito.verify(userVmDetailsDao, Mockito.times(0)).removeDetail(vmId, VmDetailConstants.INSTANCE_LEASE_EXPIRY_DATE);
    }

    @Test
    public void testApplyLeaseOnCreateInstanceFeatureEnabled() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        ServiceOfferingJoinVO svcOfferingMock = Mockito.mock(ServiceOfferingJoinVO.class);
        userVmManagerImpl.applyLeaseOnCreateInstance(userVm, 10, VMLeaseManager.ExpiryAction.DESTROY, svcOfferingMock);
        Mockito.verify(userVmManagerImpl, Mockito.times(1)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test
    public void testApplyLeaseOnCreateInstanceNegativeLease() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        userVmManagerImpl.applyLeaseOnCreateInstance(userVm, -1, VMLeaseManager.ExpiryAction.DESTROY, null);
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test
    public void testApplyLeaseOnCreateInstanceFromSvcOfferingWithoutLease() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        ServiceOfferingJoinVO svcOfferingMock = Mockito.mock(ServiceOfferingJoinVO.class);
        userVmManagerImpl.applyLeaseOnCreateInstance(userVm, null, VMLeaseManager.ExpiryAction.DESTROY, svcOfferingMock);
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test
    public void testApplyLeaseOnCreateInstanceFromSvcOfferingWithLease() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        ServiceOfferingJoinVO svcOfferingMock = Mockito.mock(ServiceOfferingJoinVO.class);
        when(svcOfferingMock.getLeaseDuration()).thenReturn(10);
        userVmManagerImpl.applyLeaseOnCreateInstance(userVm, null, VMLeaseManager.ExpiryAction.DESTROY, svcOfferingMock);
        Mockito.verify(userVmManagerImpl, Mockito.times(1)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test
    public void testApplyLeaseOnCreateInstanceNullExpiryAction() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        ServiceOfferingJoinVO svcOfferingMock = Mockito.mock(ServiceOfferingJoinVO.class);
        userVmManagerImpl.applyLeaseOnCreateInstance(userVm, 10, null, svcOfferingMock);
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testApplyLeaseOnUpdateInstanceForNoLease() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(vmId);
        when(userVmDetailsDao.listDetailsKeyPairs(anyLong(), anyList())).thenReturn(getLeaseDetails(5, VMLeaseManager.LeaseActionExecution.DISABLED.name()));
        userVmManagerImpl.applyLeaseOnUpdateInstance(userVm, 10, VMLeaseManager.ExpiryAction.STOP);
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test
    public void testApplyLeaseOnUpdateInstanceForLease() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(vmId);
        when(userVmDetailsDao.listDetailsKeyPairs(anyLong(), anyList())).thenReturn(getLeaseDetails(5, VMLeaseManager.LeaseActionExecution.PENDING.name()));
        userVmManagerImpl.applyLeaseOnUpdateInstance(userVm, 10, VMLeaseManager.ExpiryAction.STOP);
        Mockito.verify(userVmManagerImpl, Mockito.times(1)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testApplyLeaseOnUpdateInstanceForDisabledLeaseInstance() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(vmId);
        when(userVmDetailsDao.listDetailsKeyPairs(anyLong(), anyList())).thenReturn(getLeaseDetails(5, VMLeaseManager.LeaseActionExecution.DISABLED.name()));
        userVmManagerImpl.applyLeaseOnUpdateInstance(userVm, 10, VMLeaseManager.ExpiryAction.STOP);
        Mockito.verify(userVmManagerImpl, Mockito.times(1)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testApplyLeaseOnUpdateInstanceForLeaseExpired() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        when(userVmDetailsDao.listDetailsKeyPairs(anyLong(), anyList())).thenReturn(getLeaseDetails(-2, VMLeaseManager.LeaseActionExecution.PENDING.name()));
        userVmManagerImpl.applyLeaseOnUpdateInstance(userVm, 10, VMLeaseManager.ExpiryAction.STOP);
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).addLeaseDetailsForInstance(any(), any(), any());
    }

    @Test
    public void testApplyLeaseOnUpdateInstanceToRemoveLease() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(vmId);;
        when(userVmDetailsDao.listDetailsKeyPairs(anyLong(), anyList())).thenReturn(getLeaseDetails(2, VMLeaseManager.LeaseActionExecution.PENDING.name()));
        try (MockedStatic<ActionEventUtils> ignored = Mockito.mockStatic(ActionEventUtils.class)) {
            Mockito.when(ActionEventUtils.onActionEvent(Mockito.anyLong(), Mockito.anyLong(),
                    Mockito.anyLong(),
                    Mockito.anyString(), Mockito.anyString(),
                    Mockito.anyLong(), Mockito.anyString())).thenReturn(1L);
            userVmManagerImpl.applyLeaseOnUpdateInstance(userVm, -1, VMLeaseManager.ExpiryAction.STOP);
        }
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).addLeaseDetailsForInstance(any(), any(), any());
        Mockito.verify(userVmDetailsDao, Mockito.times(1)).
                addDetail(vmId, VmDetailConstants.INSTANCE_LEASE_EXECUTION, VMLeaseManager.LeaseActionExecution.DISABLED.name(), false);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testApplyLeaseOnUpdateInstanceToRemoveLeaseForExpired() {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(vmId);
        when(userVmDetailsDao.listDetailsKeyPairs(anyLong(), anyList())).thenReturn(getLeaseDetails(-2, VMLeaseManager.LeaseActionExecution.PENDING.name()));
        userVmManagerImpl.applyLeaseOnUpdateInstance(userVm, -1, VMLeaseManager.ExpiryAction.STOP);
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).addLeaseDetailsForInstance(any(), any(), any());
        Mockito.verify(userVmDetailsDao, Mockito.times(0)).removeDetail(vmId, VmDetailConstants.INSTANCE_LEASE_EXPIRY_ACTION);
        Mockito.verify(userVmDetailsDao, Mockito.times(0)).removeDetail(vmId, VmDetailConstants.INSTANCE_LEASE_EXPIRY_DATE);
    }

    String getLeaseExpiryDate(long leaseDuration) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime leaseExpiryDateTime = now.plusDays(leaseDuration);
        Date leaseExpiryDate = Date.from(leaseExpiryDateTime.atZone(ZoneOffset.UTC).toInstant());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(leaseExpiryDate);
    }

    Map<String, String> getLeaseDetails(int leaseDuration, String leaseExecution) {

        Map<String, String> leaseDetails = new HashMap<>();
        leaseDetails.put(VmDetailConstants.INSTANCE_LEASE_EXPIRY_DATE, getLeaseExpiryDate(leaseDuration));
        leaseDetails.put(VmDetailConstants.INSTANCE_LEASE_EXECUTION, leaseExecution);
        return leaseDetails;
    }

    @Test
    public void createVirtualMachineWithExistingVolume() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        DeployVMCmd deployVMCmd = new DeployVMCmd();
        ReflectionTestUtils.setField(deployVMCmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(deployVMCmd, "serviceOfferingId", serviceOfferingId);
        ReflectionTestUtils.setField(deployVMCmd, "volumeId", volumeId);
        deployVMCmd._accountService = accountService;

        when(accountService.finalyzeAccountId(nullable(String.class), nullable(Long.class), nullable(Long.class), eq(true))).thenReturn(accountId);
        when(accountService.getActiveAccountById(accountId)).thenReturn(account);
        when(entityManager.findById(DataCenter.class, zoneId)).thenReturn(_dcMock);
        when(entityManager.findById(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOffering);
        when(entityManager.findById(DiskOffering.class, serviceOffering.getId())).thenReturn(smallerDisdkOffering);
        when(entityManager.findByIdIncludingRemoved(VirtualMachineTemplate.class, templateId)).thenReturn(templateMock);
        when(volumeDataFactory.getVolume(volumeId)).thenReturn(volumeInfo);
        when(volumeInfo.getTemplateId()).thenReturn(templateId);
        when(volumeInfo.getInstanceId()).thenReturn(null);
        when(volumeInfo.getDataStore()).thenReturn(primaryDataStore);
        when(primaryDataStore.getScope()).thenReturn(scopeMock);
        when(primaryDataStore.getScope().getScopeType()).thenReturn(ScopeType.ZONE);
        when(templateMock.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        when(templateMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateMock.isDeployAsIs()).thenReturn(false);
        when(templateMock.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(templateMock.getUserDataId()).thenReturn(null);
        Mockito.doNothing().when(vnfTemplateManager).validateVnfApplianceNics(any(), nullable(List.class));
        when(_dcMock.isLocalStorageEnabled()).thenReturn(false);
        when(_dcMock.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);
        Mockito.doReturn(userVmVoMock).when(userVmManagerImpl).createBasicSecurityGroupVirtualMachine(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), nullable(Boolean.class), any(), any(), any(),
                any(), any(), any(), any(), eq(true), any(), any(), any());


        userVmManagerImpl.createVirtualMachine(deployVMCmd);
    }

    @Test
    public void createVirtualMachineWithExistingSnapshot() throws ResourceUnavailableException, InsufficientCapacityException, ResourceAllocationException {
        DeployVMCmd deployVMCmd = new DeployVMCmd();
        ReflectionTestUtils.setField(deployVMCmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(deployVMCmd, "serviceOfferingId", serviceOfferingId);
        ReflectionTestUtils.setField(deployVMCmd, "snapshotId", snashotId);
        deployVMCmd._accountService = accountService;

        when(accountService.finalyzeAccountId(nullable(String.class), nullable(Long.class), nullable(Long.class), eq(true))).thenReturn(accountId);
        when(accountService.getActiveAccountById(accountId)).thenReturn(account);
        when(entityManager.findById(DataCenter.class, zoneId)).thenReturn(_dcMock);
        when(entityManager.findById(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOffering);
        when(entityManager.findById(DiskOffering.class, serviceOffering.getId())).thenReturn(smallerDisdkOffering);
        when(snapshotDaoMock.findById(snashotId)).thenReturn(snapshotMock);
        when(entityManager.findByIdIncludingRemoved(VirtualMachineTemplate.class, templateId)).thenReturn(templateMock);
        when(volumeDataFactory.getVolume(volumeId)).thenReturn(volumeInfo);
        when(snapshotMock.getVolumeId()).thenReturn(volumeId);
        when(volumeInfo.getTemplateId()).thenReturn(templateId);
        when(volumeInfo.getInstanceId()).thenReturn(null);
        when(volumeInfo.getDataStore()).thenReturn(primaryDataStore);
        when(primaryDataStore.getScope()).thenReturn(scopeMock);
        when(primaryDataStore.getScope().getScopeType()).thenReturn(ScopeType.ZONE);
        when(templateMock.getTemplateType()).thenReturn(Storage.TemplateType.VNF);
        when(templateMock.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(templateMock.isDeployAsIs()).thenReturn(false);
        when(templateMock.getFormat()).thenReturn(Storage.ImageFormat.QCOW2);
        when(templateMock.getUserDataId()).thenReturn(null);
        Mockito.doNothing().when(vnfTemplateManager).validateVnfApplianceNics(any(), nullable(List.class));
        when(_dcMock.isLocalStorageEnabled()).thenReturn(false);
        when(_dcMock.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);
        Mockito.doReturn(userVmVoMock).when(userVmManagerImpl).createBasicSecurityGroupVirtualMachine(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), nullable(Boolean.class), any(), any(), any(),
                any(), any(), any(), any(), eq(true), any(), any(), any());


        userVmManagerImpl.createVirtualMachine(deployVMCmd);
    }
}
