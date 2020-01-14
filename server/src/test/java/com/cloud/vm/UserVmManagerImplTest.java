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

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.cloudstack.api.BaseCmd.HTTPMethod;
import org.apache.cloudstack.api.command.user.vm.UpdateVMCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.NetworkModel;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.uservm.UserVm;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;

@RunWith(PowerMockRunner.class)
public class UserVmManagerImplTest {

    @Spy
    @InjectMocks
    private UserVmManagerImpl userVmManagerImpl = new UserVmManagerImpl();

    @Mock
    private GuestOSDao guestOSDao;

    @Mock
    private UserVmDao userVmDao;

    @Mock
    private UpdateVMCmd updateVmCommand;

    @Mock
    private AccountManager accountManager;

    @Mock
    private UserVmDetailsDao userVmDetailVO;

    @Mock
    private UserVmVO userVmVoMock;

    @Mock
    private NetworkModel networkModel;

    @Mock
    private AccountVO callerAccount;

    @Mock
    private UserVO callerUser;

    private long vmId = 1l;

    @Before
    public void beforeTest() {
        Mockito.when(updateVmCommand.getId()).thenReturn(vmId);

        Mockito.when(userVmDao.findById(Mockito.eq(vmId))).thenReturn(userVmVoMock);

        Mockito.when(callerAccount.getType()).thenReturn(Account.ACCOUNT_TYPE_ADMIN);
        CallContext.register(callerUser, callerAccount);
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
        Mockito.when(guestOSDao.findById(Mockito.eq(1l))).thenReturn(Mockito.mock(GuestOSVO.class));

        userVmManagerImpl.validateGuestOsIdForUpdateVirtualMachineCommand(updateVmCommand);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateInputsAndPermissionForUpdateVirtualMachineCommandTestVmNotFound() {
        Mockito.when(userVmDao.findById(Mockito.eq(vmId))).thenReturn(null);

        userVmManagerImpl.validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);
    }

    @Test
    @PrepareForTest(CallContext.class)
    public void validateInputsAndPermissionForUpdateVirtualMachineCommandTest() {
        Mockito.doNothing().when(userVmManagerImpl).validateGuestOsIdForUpdateVirtualMachineCommand(updateVmCommand);

        Account accountMock = Mockito.mock(Account.class);
        CallContext callContextMock = Mockito.mock(CallContext.class);

        PowerMockito.mockStatic(CallContext.class);
        BDDMockito.given(CallContext.current()).willReturn(callContextMock);
        Mockito.when(callContextMock.getCallingAccount()).thenReturn(accountMock);

        Mockito.doNothing().when(accountManager).checkAccess(accountMock, null, true, userVmVoMock);
        userVmManagerImpl.validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);

        Mockito.verify(userVmManagerImpl).validateGuestOsIdForUpdateVirtualMachineCommand(updateVmCommand);
        Mockito.verify(accountManager).checkAccess(accountMock, null, true, userVmVoMock);
    }

    @Test
    public void updateVirtualMachineTestDisplayChanged() throws ResourceUnavailableException, InsufficientCapacityException {
        configureDoNothingForMethodsThatWeDoNotWantToTest();

        Mockito.when(userVmVoMock.isDisplay()).thenReturn(true);
        Mockito.doNothing().when(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);

        userVmManagerImpl.updateVirtualMachine(updateVmCommand);
        verifyMethodsThatAreAlwaysExecuted();

        Mockito.verify(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);
        Mockito.verify(userVmDetailVO, Mockito.times(0)).removeDetails(vmId);
    }

    @Test
    public void updateVirtualMachineTestCleanUpTrue() throws ResourceUnavailableException, InsufficientCapacityException {
        configureDoNothingForMethodsThatWeDoNotWantToTest();

        Mockito.when(updateVmCommand.isCleanupDetails()).thenReturn(true);

        Mockito.doNothing().when(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);
        Mockito.doNothing().when(userVmDetailVO).removeDetails(vmId);

        userVmManagerImpl.updateVirtualMachine(updateVmCommand);
        verifyMethodsThatAreAlwaysExecuted();
        Mockito.verify(userVmDetailVO).removeDetails(vmId);
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
        prepareAndExecuteMethodDealingWithDetails(false, false);
    }

    private void prepareAndExecuteMethodDealingWithDetails(boolean cleanUpDetails, boolean isDetailsEmpty) throws ResourceUnavailableException, InsufficientCapacityException {
        configureDoNothingForMethodsThatWeDoNotWantToTest();

        HashMap<String, String> details = new HashMap<>();
        if(!isDetailsEmpty) {
            details.put("", "");
        }
        Mockito.when(updateVmCommand.getDetails()).thenReturn(details);
        Mockito.when(updateVmCommand.isCleanupDetails()).thenReturn(cleanUpDetails);

        configureDoNothingForDetailsMethod();

        userVmManagerImpl.updateVirtualMachine(updateVmCommand);
        verifyMethodsThatAreAlwaysExecuted();

        Mockito.verify(userVmVoMock, Mockito.times(cleanUpDetails || isDetailsEmpty ? 0 : 1)).setDetails(details);
        Mockito.verify(userVmDetailVO, Mockito.times(cleanUpDetails ? 1: 0)).removeDetails(vmId);
        Mockito.verify(userVmDao, Mockito.times(cleanUpDetails || isDetailsEmpty ? 0 : 1)).saveDetails(userVmVoMock);
        Mockito.verify(userVmManagerImpl, Mockito.times(0)).updateDisplayVmFlag(false, vmId, userVmVoMock);
    }

    private void configureDoNothingForDetailsMethod() {
        Mockito.doNothing().when(userVmManagerImpl).updateDisplayVmFlag(false, vmId, userVmVoMock);
        Mockito.doNothing().when(userVmDetailVO).removeDetails(vmId);
        Mockito.doNothing().when(userVmDao).saveDetails(userVmVoMock);
    }

    @SuppressWarnings("unchecked")
    private void verifyMethodsThatAreAlwaysExecuted() throws ResourceUnavailableException, InsufficientCapacityException {
        Mockito.verify(userVmManagerImpl).validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);
        Mockito.verify(userVmManagerImpl).getSecurityGroupIdList(updateVmCommand);
        Mockito.verify(userVmManagerImpl).updateVirtualMachine(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyLong(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(HTTPMethod.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyListOf(Long.class),
                Mockito.anyMap());

    }

    @SuppressWarnings("unchecked")
    private void configureDoNothingForMethodsThatWeDoNotWantToTest() throws ResourceUnavailableException, InsufficientCapacityException {
        Mockito.doNothing().when(userVmManagerImpl).validateInputsAndPermissionForUpdateVirtualMachineCommand(updateVmCommand);
        Mockito.doReturn(new ArrayList<Long>()).when(userVmManagerImpl).getSecurityGroupIdList(updateVmCommand);
        Mockito.doReturn(Mockito.mock(UserVm.class)).when(userVmManagerImpl).updateVirtualMachine(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.anyLong(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.any(HTTPMethod.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyListOf(Long.class),
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
}
