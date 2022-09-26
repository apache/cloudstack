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
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
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
    MigrateVirtualMachineWithVolumeCmd cmdSpy = new MigrateVirtualMachineWithVolumeCmd();

    private Long hostId = 1L;
    private Long virtualMachineUuid = 1L;
    private String virtualMachineName = "VM-name";
    private Map<String, String> migrateVolumeTo = Map.of("key","value");
    private SystemVmResponse systemVmResponse = new SystemVmResponse();
    private UserVmResponse userVmResponse = new UserVmResponse();

    @Before
    public void setup() {
        Mockito.when(cmdSpy.getVirtualMachineId()).thenReturn(virtualMachineUuid);
        Mockito.when(cmdSpy.getHostId()).thenReturn(hostId);
        Mockito.when(cmdSpy.getVolumeToPool()).thenReturn(migrateVolumeTo);
    }

    @Test
    public void executeTestHostIdIsNullAndMigrateVolumeToIsNullThrowsInvalidParameterValueException(){
        ReflectionTestUtils.setField(cmdSpy, "hostId", null);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", null);

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(InvalidParameterValueException.class, e.getClass());
            String expected = String.format("Either %s or %s must be passed for migrating the VM.", ApiConstants.HOST_ID, ApiConstants.MIGRATE_TO);
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestVMIsStoppedAndHostIdIsNotNullThrowsInvalidParameterValueException(){
        ReflectionTestUtils.setField(cmdSpy, "hostId", hostId);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(virtualMachineMock.toString()).thenReturn(String.format("VM [uuid: %s, name: %s]", virtualMachineUuid, virtualMachineName));

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(InvalidParameterValueException.class, e.getClass());
            String expected = String.format("%s is not in the Running state to migrate it to the new host.", virtualMachineMock);
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestVMIsRunningAndHostIdIsNullThrowsInvalidParameterValueException(){
        ReflectionTestUtils.setField(cmdSpy, "hostId", null);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(virtualMachineMock.toString()).thenReturn(String.format("VM [uuid: %s, name: %s]", virtualMachineUuid, virtualMachineName));

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(InvalidParameterValueException.class, e.getClass());
            String expected = String.format("%s is not in the Stopped state to migrate, use the %s parameter to migrate it to a new host.", virtualMachineMock,
                    ApiConstants.HOST_ID);
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestHostIdIsNullThrowsInvalidParameterValueException(){
        ReflectionTestUtils.setField(cmdSpy, "hostId", hostId);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(resourceServiceMock.getHost(Mockito.anyLong())).thenReturn(null);
        Mockito.when(uuidManagerMock.getUuid(Host.class, virtualMachineUuid)).thenReturn(virtualMachineUuid.toString());

        try {
            cmdSpy.execute();
        } catch (Exception e) {
            Assert.assertEquals(InvalidParameterValueException.class, e.getClass());
            String expected = "Unable to find the specified host to migrate the VM.";
            Assert.assertEquals(expected , e.getMessage());
        }
    }

    @Test
    public void executeTestHostIsNotNullMigratedVMIsNullThrowsServerApiException() throws ManagementServerException, ResourceUnavailableException, VirtualMachineMigrationException {
        ReflectionTestUtils.setField(cmdSpy, "hostId", hostId);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(resourceServiceMock.getHost(Mockito.anyLong())).thenReturn(hostMock);
        Mockito.when(userVmServiceMock.migrateVirtualMachineWithVolume(virtualMachineUuid, hostMock, migrateVolumeTo)).thenReturn(null);

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
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(userVmServiceMock.vmStorageMigration(virtualMachineUuid, migrateVolumeTo)).thenReturn(null);

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
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(userVmServiceMock.vmStorageMigration(virtualMachineUuid, migrateVolumeTo)).thenReturn(virtualMachineMock);
        Mockito.when(virtualMachineMock.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);
        Mockito.when(responseGeneratorMock.createSystemVmResponse(virtualMachineMock)).thenReturn(systemVmResponse);

        cmdSpy.execute();

        Mockito.verify(responseGeneratorMock, Mockito.times(1)).createSystemVmResponse(virtualMachineMock);
    }

    @Test
    public void executeTestUserVMMigratedWithSuccess() {
        UserVm userVmMock = Mockito.mock(UserVm.class);
        ReflectionTestUtils.setField(cmdSpy, "hostId", null);
        ReflectionTestUtils.setField(cmdSpy, "migrateVolumeTo", migrateVolumeTo);

        Mockito.when(userVmServiceMock.getVm(Mockito.anyLong())).thenReturn(userVmMock);
        Mockito.when(userVmMock.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(userVmServiceMock.vmStorageMigration(virtualMachineUuid, migrateVolumeTo)).thenReturn(userVmMock);
        Mockito.when(userVmMock.getType()).thenReturn(VirtualMachine.Type.User);
        Mockito.when(responseGeneratorMock.createUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVmMock)).thenReturn(List.of(userVmResponse));

        cmdSpy.execute();

        Mockito.verify(responseGeneratorMock, Mockito.times(1)).createUserVmResponse(ResponseObject.ResponseView.Full, "virtualmachine", userVmMock);
    }
}
