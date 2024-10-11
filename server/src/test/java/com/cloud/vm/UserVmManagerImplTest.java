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
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.offering.DiskOffering;
import org.apache.cloudstack.api.BaseCmd.HTTPMethod;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVnfApplianceCmd;
import org.apache.cloudstack.api.command.user.vm.ResetVMUserDataCmd;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.api.command.user.volume.ResizeVolumeCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
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
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.server.ManagementService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.ScopeType;
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
    private NetworkVO _networkMock;

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

    private static final long vmId = 1l;
    private static final long zoneId = 2L;
    private static final long accountId = 3L;
    private static final long serviceOfferingId = 10L;
    private static final long templateId = 11L;

    private static final long GiB_TO_BYTES = 1024 * 1024 * 1024;

    private Map<String, String> customParameters = new HashMap<>();

    String[] detailsConstants = {VmDetailConstants.MEMORY, VmDetailConstants.CPU_NUMBER, VmDetailConstants.CPU_SPEED};

    private DiskOfferingVO smallerDisdkOffering = prepareDiskOffering(5l * GiB_TO_BYTES, 1l, 1L, 2L);
    private DiskOfferingVO largerDisdkOffering = prepareDiskOffering(10l * GiB_TO_BYTES, 2l, 10L, 20L);

    @Before
    public void beforeTest() {

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
    public void updateVirtualMachineTestDisplayChanged() throws ResourceUnavailableException, InsufficientCapacityException {
        configureDoNothingForMethodsThatWeDoNotWantToTest();
        ServiceOffering offering = getSvcoffering(512);
        Mockito.when(_serviceOfferingDao.findById(Mockito.anyLong(), Mockito.anyLong())).thenReturn((ServiceOfferingVO) offering);
        Mockito.when(userVmVoMock.isDisplay()).thenReturn(true);
        Mockito.doNothing().when(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);
        Mockito.when(updateVmCommand.getUserdataId()).thenReturn(null);
        userVmManagerImpl.updateVirtualMachine(updateVmCommand);
        verifyMethodsThatAreAlwaysExecuted();

        Mockito.verify(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);
        Mockito.verify(userVmDetailsDao, Mockito.times(0)).removeDetail(anyLong(), anyString());
    }

    @Test
    public void updateVirtualMachineTestCleanUpTrue() throws ResourceUnavailableException, InsufficientCapacityException {
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
        Mockito.verify(userVmDetailsDao, Mockito.times(0)).removeDetail(vmId, "systemdetail");
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).updateDisplayVmFlag(false, vmId, userVmVoMock);
    }

    @Test
    public void updateVirtualMachineTestCleanUpTrueAndDetailEmpty() throws ResourceUnavailableException, InsufficientCapacityException {
        prepareAndExecuteMethodDealingWithDetails(true, true);
    }

    @Test
    public void updateVirtualMachineTestCleanUpTrueAndDetailsNotEmpty() throws ResourceUnavailableException, InsufficientCapacityException {
        prepareAndExecuteMethodDealingWithDetails(true, false);
    }

    @Test
    public void updateVirtualMachineTestCleanUpFalseAndDetailsNotEmpty() throws ResourceUnavailableException, InsufficientCapacityException {
        prepareAndExecuteMethodDealingWithDetails(false, true);
    }

    @Test
    public void updateVirtualMachineTestCleanUpFalseAndDetailsEmpty() throws ResourceUnavailableException, InsufficientCapacityException {
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(callerAccount);
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

    private void prepareAndExecuteMethodDealingWithDetails(boolean cleanUpDetails, boolean isDetailsEmpty) throws ResourceUnavailableException, InsufficientCapacityException {
        configureDoNothingForMethodsThatWeDoNotWantToTest();

        ServiceOffering offering = getSvcoffering(512);
        Mockito.when(_serviceOfferingDao.findById(Mockito.anyLong(), Mockito.anyLong())).thenReturn((ServiceOfferingVO) offering);
        Mockito.when(_serviceOfferingDao.findByIdIncludingRemoved(Mockito.anyLong(), Mockito.anyLong())).thenReturn((ServiceOfferingVO) offering);
        ServiceOfferingVO currentServiceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.lenient().when(currentServiceOffering.getCpu()).thenReturn(1);
        Mockito.lenient().when(currentServiceOffering.getRamSize()).thenReturn(512);

        List<NicVO> nics = new ArrayList<>();
        NicVO nic1 = mock(NicVO.class);
        NicVO nic2 = mock(NicVO.class);
        nics.add(nic1);
        nics.add(nic2);
        when(this.nicDao.listByVmId(Mockito.anyLong())).thenReturn(nics);
        when(_networkDao.findById(anyLong())).thenReturn(_networkMock);
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

        Mockito.verify(userVmVoMock, Mockito.times(cleanUpDetails || isDetailsEmpty ? 0 : 1)).setDetails(details);
        Mockito.verify(userVmDetailsDao, Mockito.times(cleanUpDetails ? 1 : 0)).removeDetail(vmId, "existingdetail");
        Mockito.verify(userVmDetailsDao, Mockito.times(0)).removeDetail(vmId, "systemdetail");
        Mockito.verify(userVmDao, Mockito.times(cleanUpDetails || isDetailsEmpty ? 0 : 1)).saveDetails(userVmVoMock);
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).updateDisplayVmFlag(false, vmId, userVmVoMock);
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
    private void configureDoNothingForMethodsThatWeDoNotWantToTest() throws ResourceUnavailableException, InsufficientCapacityException {
        Mockito.doNothing().when(userVmManagerImpl).validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);
        Mockito.doReturn(new ArrayList<Long>()).when(userVmManagerImpl).getSecurityGroupIdList(updateVmCommand);
        Mockito.lenient().doReturn(Mockito.mock(UserVm.class)).when(userVmManagerImpl).updateVirtualMachine(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyLong(),
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(HTTPMethod.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyList(),
                Mockito.anyMap());
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

        String returnedMacAddress = userVmManagerImpl.validateOrReplaceMacAddress(macAddress, 1l);

        Mockito.verify(networkModel, Mockito.times(times)).getNextAvailableMacAddressInNetwork(Mockito.anyLong());
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
        long offeringRootDiskSize = 10l * GiB_TO_BYTES;;

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
        Mockito.verify(userVmManagerImpl, Mockito.times(timesVerifyIfHypervisorSupports)).verifyIfHypervisorSupportsRootdiskSizeOverride(Mockito.any());
    }

    @Test
    public void verifyIfHypervisorSupportRootdiskSizeOverrideTest() {
        Hypervisor.HypervisorType[] hypervisorTypeArray = Hypervisor.HypervisorType.values();
        int exceptionCounter = 0;
        int expectedExceptionCounter = hypervisorTypeArray.length - 5;

        for(int i = 0; i < hypervisorTypeArray.length; i++) {
            if (UserVmManagerImpl.ROOT_DISK_SIZE_OVERRIDE_SUPPORTING_HYPERVISORS.contains(hypervisorTypeArray[i])) {
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
        Mockito.when(newRootDiskOffering.getName()).thenReturn("OfferingName");
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
                any(), any(), any(), any(), eq(true), any());

        UserVm result = userVmManagerImpl.createVirtualMachine(deployVMCmd);
        assertEquals(userVmVoMock, result);
        Mockito.verify(vnfTemplateManager).validateVnfApplianceNics(templateMock, null);
        Mockito.verify(userVmManagerImpl).createBasicSecurityGroupVirtualMachine(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), nullable(Boolean.class), any(), any(), any(),
                any(), any(), any(), any(), eq(true), any());
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
                any(), any(), any(), any(), eq(true), any());

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
        userVmManagerImpl.resourceLimitService = resourceLimitMgr;
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
            Mockito.verify(resourceLimitMgr, Mockito.times(1))
                    .checkResourceLimit(account, Resource.ResourceType.volume, 4);
            Mockito.verify(resourceLimitMgr, Mockito.times(1))
                    .checkResourceLimit(account, Resource.ResourceType.primary_storage, size);
            Mockito.verify(resourceLimitMgr, Mockito.times(1))
                    .checkResourceLimitWithTag(account, Resource.ResourceType.volume, "tag1", 2);
            Mockito.verify(resourceLimitMgr, Mockito.times(1))
                    .checkResourceLimitWithTag(account, Resource.ResourceType.volume, "tag2", 3);
            Mockito.verify(resourceLimitMgr, Mockito.times(1))
                    .checkResourceLimitWithTag(account, Resource.ResourceType.primary_storage, "tag1",
                            vol1.getSize() + vol5.getSize());
            Mockito.verify(resourceLimitMgr, Mockito.times(1))
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
                destinationHostVO, Mockito.times(1)
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
}
