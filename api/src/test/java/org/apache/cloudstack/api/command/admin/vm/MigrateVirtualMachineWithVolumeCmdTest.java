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
package org.apache.cloudstack.api.command.admin.vm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SystemVmResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.host.Host;
import com.cloud.resource.ResourceService;
import com.cloud.uservm.UserVm;
import com.cloud.utils.db.UUIDManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VirtualMachine;

@RunWith(MockitoJUnitRunner.class)
public class MigrateVirtualMachineWithVolumeCmdTest {
    @Mock
    UserVmService userVmServiceMock;

    @Mock
    UUIDManager uuidManagerMock;

    @Mock
    ResourceService resourceServiceMock;

    @Mock
    ResponseGenerator responseGeneratorMock;

    @Mock
    VirtualMachine virtualMachineMock;

    @Mock
    Host hostMock;

    @Spy
    @InjectMocks
    MigrateVirtualMachineWithVolumeCmd cmdSpy;

    private Long hostId = 1L;
    private Long virtualMachineId = 1L;
    private String virtualMachineName = "VM-name";
    private  Map<String, String> migrateVolumeTo = null;
    private SystemVmResponse systemVmResponse = new SystemVmResponse();
    private UserVmResponse userVmResponse = new UserVmResponse();

    @Before
    public void setUp() throws Exception {
        ReflectionTestUtils.setField(cmdSpy, "virtualMachineId", virtualMachineId);
        migrateVolumeTo = new HashMap<>();
        migrateVolumeTo.put("volume", "abc");
        migrateVolumeTo.put("pool", "xyz");
    }

    @Test
    public void executeTestRequiredArgsNullThrowsInvalidParameterValueException() {
        ReflectionTestUtils.setField(cmdSpy, "hostId", null);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", null);
        ReflectionTestUtils.setField(cmdSpy, "autoSelect", null);

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(InvalidParameterValueException.class, e.getClass());
            String expected = String.format("Either %s or %s must be passed or %s must be true for migrating the VM.", ApiConstants.HOST_ID, ApiConstants.MIGRATE_TO, ApiConstants.AUTO_SELECT);
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestVMIsStoppedAndHostIdIsNotNullThrowsInvalidParameterValueException() {
        ReflectionTestUtils.setField(cmdSpy, "hostId", hostId);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(virtualMachineMock.toString()).thenReturn(String.format("VM [uuid: %s, name: %s]", virtualMachineId, virtualMachineName));

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(InvalidParameterValueException.class, e.getClass());
            String expected = String.format("%s is not in the Running state to migrate it to the new host.", virtualMachineMock);
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestVMIsRunningHostIdIsNullAndAutoSelectIsFalseThrowsInvalidParameterValueException() {
        ReflectionTestUtils.setField(cmdSpy, "hostId", null);
        ReflectionTestUtils.setField(cmdSpy, "autoSelect", false);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(virtualMachineMock.toString()).thenReturn(String.format("VM [uuid: %s, name: %s]", virtualMachineId, virtualMachineName));

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(InvalidParameterValueException.class, e.getClass());
            String expected = String.format("%s is not in the Stopped state to migrate, use the %s or %s parameter to migrate it to a new host.", virtualMachineMock,
                    ApiConstants.HOST_ID, ApiConstants.AUTO_SELECT);
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestHostIdIsNullThrowsInvalidParameterValueException() {
        ReflectionTestUtils.setField(cmdSpy, "virtualMachineId", virtualMachineId);
        ReflectionTestUtils.setField(cmdSpy, "hostId", hostId);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);
        ReflectionTestUtils.setField(cmdSpy, "autoSelect", false);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(resourceServiceMock.getHost(Mockito.anyLong())).thenReturn(null);

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(InvalidParameterValueException.class, e.getClass());
            String expected = "Unable to find the specified host to migrate the VM.";
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    private Map getMockedMigrateVolumeToApiCmdParam() {
        Map<String, String> migrateVolumeTo = new HashMap<>();
        migrateVolumeTo.put("volume", "abc");
        migrateVolumeTo.put("pool", "xyz");
        return Map.of("", migrateVolumeTo);
    }

    @Test
    public void executeTestHostIsNotNullMigratedVMIsNullThrowsServerApiException() throws ManagementServerException, ResourceUnavailableException, VirtualMachineMigrationException {
        ReflectionTestUtils.setField(cmdSpy, "hostId", hostId);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", getMockedMigrateVolumeToApiCmdParam());

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(resourceServiceMock.getHost(hostId)).thenReturn(hostMock);
        Mockito.when(userVmServiceMock.migrateVirtualMachineWithVolume(Mockito.anyLong(), Mockito.any(), Mockito.anyMap())).thenReturn(null);

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(ServerApiException.class, e.getClass());
            String expected = "Failed to migrate vm";
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestHostIsNullMigratedVMIsNullThrowsServerApiException() {
        ReflectionTestUtils.setField(cmdSpy, "hostId", null);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", getMockedMigrateVolumeToApiCmdParam());

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(userVmServiceMock.vmStorageMigration(Mockito.anyLong(), Mockito.anyMap())).thenReturn(null);

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(ServerApiException.class, e.getClass());
            String expected = "Failed to migrate vm";
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestSystemVMMigratedWithSuccess() {
        ReflectionTestUtils.setField(cmdSpy, "hostId", null);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", getMockedMigrateVolumeToApiCmdParam());

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(userVmServiceMock.vmStorageMigration(Mockito.anyLong(), Mockito.anyMap())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);
        Mockito.when(responseGeneratorMock.createSystemVmResponse(virtualMachineMock)).thenReturn(systemVmResponse);

        cmdSpy.execute();

        Mockito.verify(responseGeneratorMock, Mockito.times(1)).createSystemVmResponse(virtualMachineMock);
    }

    @Test
    public void executeTestUserVMMigratedWithSuccess() {
        UserVm userVmMock = Mockito.mock(UserVm.class);
        ReflectionTestUtils.setField(cmdSpy, "hostId", null);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", getMockedMigrateVolumeToApiCmdParam());

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(userVmMock);
        Mockito.when(userVmMock.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(userVmServiceMock.vmStorageMigration(Mockito.anyLong(), Mockito.anyMap())).thenReturn(userVmMock);
        Mockito.when(userVmMock.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(responseGeneratorMock.createUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVmMock)).thenReturn(List.of(userVmResponse));

        cmdSpy.execute();

        Mockito.verify(responseGeneratorMock, Mockito.times(1)).createUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVmMock);
    }
}
