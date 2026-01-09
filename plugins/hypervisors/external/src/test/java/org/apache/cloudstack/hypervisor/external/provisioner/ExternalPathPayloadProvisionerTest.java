//
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
//
package org.apache.cloudstack.hypervisor.external.provisioner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsFilesystemManager;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.GetExternalConsoleAnswer;
import com.cloud.agent.api.GetExternalConsoleCommand;
import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.PrepareExternalProvisioningAnswer;
import com.cloud.agent.api.PrepareExternalProvisioningCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.serializer.GsonHelper;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.Pair;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.UserVmDao;

@RunWith(MockitoJUnitRunner.class)
public class ExternalPathPayloadProvisionerTest {

    @Spy
    @InjectMocks
    private ExternalPathPayloadProvisioner provisioner;

    @Mock
    private UserVmDao userVmDao;

    @Mock
    private HostDao hostDao;

    @Mock
    private HypervisorGuruManager hypervisorGuruManager;

    @Mock
    private HypervisorGuru hypervisorGuru;

    @Mock
    private Logger logger;

    @Mock
    private ExtensionsManager extensionsManager;

    @Mock
    private ExtensionsFilesystemManager extensionsFilesystemManager;

    @Test
    public void testLoadAccessDetails() {
        Map<String, Map<String, String>> externalDetails = new HashMap<>();
        externalDetails.put(ApiConstants.EXTENSION, Map.of("key1", "value1"));

        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(vmTO.getName()).thenReturn("test-vm");

        Map<String, Object> result = provisioner.loadAccessDetails(externalDetails, vmTO);

        assertNotNull(result);
        assertEquals(externalDetails, result.get(ApiConstants.EXTERNAL_DETAILS));
        assertEquals("test-uuid", result.get(ApiConstants.VIRTUAL_MACHINE_ID));
        assertEquals("test-vm", result.get(ApiConstants.VIRTUAL_MACHINE_NAME));
        assertEquals(vmTO, result.get("cloudstack.vm.details"));
    }

    @Test
    public void testLoadAccessDetailsWithNullExternalDetails() {
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(vmTO.getName()).thenReturn("test-vm");

        Map<String, Object> result = provisioner.loadAccessDetails(null, vmTO);

        assertNotNull(result);
        assertNull(result.get(ApiConstants.EXTERNAL_DETAILS));
        assertEquals("test-uuid", result.get(ApiConstants.VIRTUAL_MACHINE_ID));
        assertEquals("test-vm", result.get(ApiConstants.VIRTUAL_MACHINE_NAME));
    }

    @Test
    public void testLoadAccessDetails_WithCaller() {
        Map<String, Map<String, String>> externalDetails = new HashMap<>();
        externalDetails.put(ApiConstants.EXTENSION, Map.of("key1", "value1"));
        externalDetails.put(ApiConstants.CALLER, Map.of("key2", "value2"));
        Map<String, Object> result = provisioner.loadAccessDetails(externalDetails, null);

        assertNotNull(result);
        assertNotNull(result.get(ApiConstants.EXTERNAL_DETAILS));
        assertNotNull(((Map<String, String>) result.get(ApiConstants.EXTERNAL_DETAILS)).get(ApiConstants.EXTENSION));
        assertNotNull(result.get(ApiConstants.CALLER));
        assertNull(result.get(VmDetailConstants.CLOUDSTACK_VM_DETAILS));
    }

    private String setupCheckedExtensionPath() {
        String path = "test-extension.sh";
        when(extensionsFilesystemManager.getExtensionCheckedPath(anyString(), anyString())).thenReturn(path);
        return path;
    }

    @Test
    public void testPrepareExternalProvisioning() {
        String path = setupCheckedExtensionPath();
        PrepareExternalProvisioningCommand cmd = mock(PrepareExternalProvisioningCommand.class);
        VirtualMachineTO vmTO = new VirtualMachineTO(1, "test-vm", VirtualMachine.Type.User, 1, 1000, 256,
                512, VirtualMachineTemplate.BootloaderType.HVM, "OS", false, false, "Pass");
        vmTO.setUuid("test-uuid");

        when(cmd.getVirtualMachineTO()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "{\"nics\":[{\"uuid\":\"test-net-uuid\",\"mac\":\"00:00:00:01:02:03\"}]}")).when(provisioner)
                .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        PrepareExternalProvisioningAnswer answer = provisioner.prepareExternalProvisioning(
                "host-name", "test-extension", path, cmd);

        assertTrue(answer.getResult());
        assertEquals("test-net-uuid", answer.getVirtualMachineTO().getNics()[0].getNetworkUuid());
        assertEquals("00:00:00:01:02:03", answer.getVirtualMachineTO().getNics()[0].getMac());
    }

    @Test
    public void testPrepareExternalProvisioning_ExtensionNotConfigured() {
        PrepareExternalProvisioningCommand cmd = mock(PrepareExternalProvisioningCommand.class);

        String extensionName = "test-extension";
        String hostName = "host-name";
        PrepareExternalProvisioningAnswer answer = provisioner.prepareExternalProvisioning(
            hostName, extensionName, "nonexistent.sh", cmd);

        assertFalse(answer.getResult());
        assertNotNull(answer);
        assertEquals(String.format("Extension: %s not configured for host: %s", extensionName, hostName), answer.getDetails());
    }

    @Test
    public void testStartInstance() {
        String path = setupCheckedExtensionPath();
        StartCommand cmd = mock(StartCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "{\"status\": \"success\", \"message\": \"Instance started\"}")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StartAnswer answer = provisioner.startInstance("host-name", "test-extension", path, cmd);

        assertTrue(answer.getResult());
        verify(logger).debug("Starting VM test-uuid on the external system");
    }

    @Test
    public void testStartInstanceDeploy() {
        String path = setupCheckedExtensionPath();
        StartCommand cmd = mock(StartCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        Map<String, String> details = new HashMap<>();
        details.put("deployvm", "true");
        when(vmTO.getDetails()).thenReturn(details);
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "{\"status\": \"success\", \"message\": \"Instance started\"}")).when(provisioner)
                .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StartAnswer answer = provisioner.startInstance("host-name", "test-extension", path, cmd);

        assertTrue(answer.getResult());
        verify(logger).debug("Deploying VM test-uuid on the external system");
    }

    @Test
    public void testStartInstanceError() {
        String path = setupCheckedExtensionPath();
        StartCommand cmd = mock(StartCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(false, "{\"error\": \"Instance failed to start\"}")).when(provisioner)
                .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StartAnswer answer = provisioner.startInstance("host-name", "test-extension", path, cmd);

        assertFalse(answer.getResult());
        assertEquals("{\"error\": \"Instance failed to start\"}", answer.getDetails());
        verify(logger).debug("Starting VM test-uuid on the external system failed: {\"error\": \"Instance failed to start\"}");
    }

    @Test
    public void testStopInstance() {
        String path = setupCheckedExtensionPath();
        StopCommand cmd = mock(StopCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "success")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StopAnswer answer = provisioner.stopInstance("host-name", "test-extension", path, cmd);

        assertTrue(answer.getResult());
    }

    @Test
    public void testRebootInstance() {
        String path = setupCheckedExtensionPath();
        RebootCommand cmd = mock(RebootCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "success")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        RebootAnswer answer = provisioner.rebootInstance("host-name", "test-extension", path, cmd);

        assertTrue(answer.getResult());
    }

    @Test
    public void testExpungeInstance() {
        String path = setupCheckedExtensionPath();
        StopCommand cmd = mock(StopCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "success")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StopAnswer answer = provisioner.expungeInstance("host-name", "test-extension", path, cmd);

        assertTrue(answer.getResult());
    }

    @Test
    public void testGetHostVmStateReport() {
        String path = setupCheckedExtensionPath();
        HostVO host = mock(HostVO.class);
        when(hostDao.findById(anyLong())).thenReturn(host);

        UserVmVO vm = mock(UserVmVO.class);
        when(vm.getUuid()).thenReturn("test-uuid");
        when(vm.getInstanceName()).thenReturn("test-instance");

        List<UserVmVO> vms = new ArrayList<>();
        vms.add(vm);
        when(userVmDao.listByHostId(anyLong())).thenReturn(vms);
        when(userVmDao.listByLastHostId(anyLong())).thenReturn(new ArrayList<>());

        when(hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.External)).thenReturn(hypervisorGuru);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(hypervisorGuru.implement(any(VirtualMachineProfile.class))).thenReturn(vmTO);

        doReturn(new Pair<>(true, "PowerOn")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        Map<String, HostVmStateReportEntry> result = provisioner.getHostVmStateReport(1L, "test-extension", path);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("test-instance"));
    }

    @Test
    public void testGetHostVmStateReportHostNotFound() {
        String path = setupCheckedExtensionPath();
        Map<String, HostVmStateReportEntry> result = provisioner.getHostVmStateReport(1L, "test-extension", path);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(logger).error("Host with ID: {} not found", 1L);
    }

    @Test
    public void testRunCustomAction() {
        String path = setupCheckedExtensionPath();
        RunCustomActionCommand cmd = mock(RunCustomActionCommand.class);
        when(cmd.getActionName()).thenReturn("test-action");
        when(cmd.getParameters()).thenReturn(new HashMap<>());
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "success")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        RunCustomActionAnswer answer = provisioner.runCustomAction("host-name", "test-extension", path, cmd);

        assertTrue(answer.getResult());
        verify(logger).debug("Executing custom action '{}' in the external system", "test-action");
    }

    @Test
    public void testExecuteExternalCommand() throws IOException {
        when(extensionsFilesystemManager.prepareExternalPayload(anyString(), anyMap())).thenAnswer(
                invocation -> {
                    String extensionName = invocation.getArgument(0, String.class);
                    Map<String, String> details = invocation.getArgument(1, Map.class);
                    File tempFile = File.createTempFile(extensionName, ".json");
                    String json = GsonHelper.getGson().toJson(details);
                    Files.writeString(tempFile.toPath(), json);
                    tempFile.deleteOnExit();
                    return tempFile.getAbsolutePath();
                }
        );
        String scriptContent = "#!/bin/bash\nif grep -q '{\"test\":\"value\"}' \"$2\"; then\necho 'success'\nexit 0\nelse\necho 'fail'\nexit 1\nfi";
        File testScript = File.createTempFile("test-extension", ".sh");
        Files.writeString(testScript.toPath(), scriptContent);
        testScript.setExecutable(true);
        testScript.deleteOnExit();

        Map<String, Object> accessDetails = new HashMap<>();
        accessDetails.put("test", "value");

        Pair<Boolean, String> result = provisioner.executeExternalCommand(
            "test-extension", "test-action", accessDetails, 30, "Test error", testScript.getAbsolutePath());

        assertTrue(result.first());
        assertTrue(result.second().contains("success"));
    }

    @Test
    public void testExecuteExternalCommandFileNotExecutable() {
        Map<String, Object> accessDetails = new HashMap<>();

        Pair<Boolean, String> result = provisioner.executeExternalCommand(
            "test-extension", "test-action", accessDetails, 30, "Test error", "/nonexistent/path");

        assertFalse(result.first());
        assertEquals("File is not executable", result.second());
    }

    @Test
    public void deleteExtensionPayloadFile_DeletesFile_WhenActionIsTrivial() {
        String extensionName = "test-extension";
        String action = "status"; // in TRIVIAL_ACTIONS
        String payloadFileName = "/tmp/test-payload.json";
        provisioner.deleteExtensionPayloadFile(extensionName, action, payloadFileName);
        verify(extensionsFilesystemManager).deleteExtensionPayload(extensionName, payloadFileName);
    }

    @Test
    public void deleteExtensionPayloadFile_DoesNothing_WhenActionIsNotTrivial() {
        String extensionName = "test-extension";
        String action = "start";
        String payloadFileName = "/tmp/test-payload.json";
        provisioner.deleteExtensionPayloadFile(extensionName, action, payloadFileName);
        verify(logger).trace(
                "Skipping deletion of payload file: {} for extension: {}, action: {}",
                payloadFileName, extensionName, action);
    }

    @Test
    public void getPowerStateFromStringReturnsPowerOnForValidInput() {
        VirtualMachine.PowerState result = provisioner.getPowerStateFromString("PowerOn");
        assertEquals(VirtualMachine.PowerState.PowerOn, result);
    }

    @Test
    public void getPowerStateFromStringReturnsPowerOffForValidInput() {
        VirtualMachine.PowerState result = provisioner.getPowerStateFromString("PowerOff");
        assertEquals(VirtualMachine.PowerState.PowerOff, result);
    }

    @Test
    public void getPowerStateFromStringReturnsPowerUnknownForInvalidInput() {
        VirtualMachine.PowerState result = provisioner.getPowerStateFromString("InvalidState");
        assertEquals(VirtualMachine.PowerState.PowerUnknown, result);
    }

    @Test
    public void getPowerStateFromStringReturnsPowerUnknownForBlankInput() {
        VirtualMachine.PowerState result = provisioner.getPowerStateFromString("");
        assertEquals(VirtualMachine.PowerState.PowerUnknown, result);
    }

    @Test
    public void parsePowerStateFromResponseReturnsPowerOnForValidJson() {
        String response = "{\"power_state\":\"PowerOn\"}";
        UserVmVO vm = mock(UserVmVO.class);
        VirtualMachine.PowerState result = provisioner.parsePowerStateFromResponse(vm, response);
        assertEquals(VirtualMachine.PowerState.PowerOn, result);
    }

    @Test
    public void parsePowerStateFromResponseReturnsPowerOffForValidJson() {
        String response = "{\"power_state\":\"PowerOff\"}";
        UserVmVO vm = mock(UserVmVO.class);
        VirtualMachine.PowerState result = provisioner.parsePowerStateFromResponse(vm, response);
        assertEquals(VirtualMachine.PowerState.PowerOff, result);
    }

    @Test
    public void parsePowerStateFromResponseReturnsPowerUnknownForInvalidJson() {
        String response = "{\"invalid_key\":\"value\"}";
        UserVmVO vm = mock(UserVmVO.class);
        VirtualMachine.PowerState result = provisioner.parsePowerStateFromResponse(vm, response);
        assertEquals(VirtualMachine.PowerState.PowerUnknown, result);
    }

    @Test
    public void parsePowerStateFromResponseReturnsPowerUnknownForMalformedJson() {
        String response = "{power_state:PowerOn";
        UserVmVO vm = mock(UserVmVO.class);
        VirtualMachine.PowerState result = provisioner.parsePowerStateFromResponse(vm, response);
        assertEquals(VirtualMachine.PowerState.PowerUnknown, result);
    }

    @Test
    public void parsePowerStateFromResponseReturnsPowerUnknownForBlankResponse() {
        String response = "";
        UserVmVO vm = mock(UserVmVO.class);
        VirtualMachine.PowerState result = provisioner.parsePowerStateFromResponse(vm, response);
        assertEquals(VirtualMachine.PowerState.PowerUnknown, result);
    }

    @Test
    public void parsePowerStateFromResponseReturnsPowerStateForPlainTextResponse() {
        String response = "PowerOn";
        UserVmVO vm = mock(UserVmVO.class);
        VirtualMachine.PowerState result = provisioner.parsePowerStateFromResponse(vm, response);
        assertEquals(VirtualMachine.PowerState.PowerOn, result);
    }

    @Test
    public void getVirtualMachineTOReturnsNullWhenVmIsNull() {
        VirtualMachineTO result = provisioner.getVirtualMachineTO(null);
        assertNull(result);
    }

    @Test
    public void getVirtualMachineTOReturnsValidTOWhenVmIsNotNull() {
        VirtualMachine vm = mock(VirtualMachine.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.External)).thenReturn(hypervisorGuru);
        when(hypervisorGuru.implement(any(VirtualMachineProfile.class))).thenReturn(vmTO);
        VirtualMachineTO result = provisioner.getVirtualMachineTO(vm);
        assertNotNull(result);
        assertEquals(vmTO, result);
        verify(hypervisorGuruManager).getGuru(Hypervisor.HypervisorType.External);
        verify(hypervisorGuru).implement(any(VirtualMachineProfile.class));
    }

    @Test
    public void getInstanceConsoleReturnsAnswerWhenConsoleDetailsAreValid() {
        String path = setupCheckedExtensionPath();
        GetExternalConsoleCommand cmd = mock(GetExternalConsoleCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(vmTO.getUuid()).thenReturn("test-uuid");

        Map<String, Object> accessDetails = new HashMap<>();
        when(provisioner.loadAccessDetails(any(), eq(vmTO))).thenReturn(accessDetails);

        String validOutput = "{\"console\":{\"host\":\"127.0.0.1\",\"port\":5900,\"password\":\"pass\",\"protocol\":\"vnc\"}}";
        doReturn(new Pair<>(true, validOutput)).when(provisioner)
                .getInstanceConsoleOnExternalSystem(anyString(), anyString(), anyString(), anyMap(), anyInt());

        GetExternalConsoleAnswer result = provisioner.getInstanceConsole("host-name", "test-extension", path, cmd);

        assertNotNull(result);
        assertEquals("127.0.0.1", result.getHost());
        Integer port = 5900;
        assertEquals(port, result.getPort());
        assertEquals("pass", result.getPassword());
        assertEquals("vnc", result.getProtocol());
    }

    @Test
    public void getInstanceConsoleReturnsErrorWhenExtensionNotConfigured() {
        GetExternalConsoleCommand cmd = mock(GetExternalConsoleCommand.class);
        when(extensionsFilesystemManager.getExtensionCheckedPath(anyString(), anyString())).thenReturn(null);

        String extensionName = "test-extension";
        String hostName = "host-name";
        GetExternalConsoleAnswer result = provisioner.getInstanceConsole(hostName,
                extensionName, "test-extension.sh", cmd);

        assertNotNull(result);
        assertEquals(String.format("Extension: %s not configured for host: %s", extensionName, hostName), result.getDetails());
    }

    @Test
    public void getInstanceConsoleReturnsErrorWhenExternalSystemFails() {
        String path = setupCheckedExtensionPath();
        GetExternalConsoleCommand cmd = mock(GetExternalConsoleCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(vmTO.getUuid()).thenReturn("test-uuid");

        doReturn(new Pair<>(false, "External system error")).when(provisioner)
                .getInstanceConsoleOnExternalSystem(anyString(), anyString(), anyString(), anyMap(), anyInt());

        GetExternalConsoleAnswer result = provisioner.getInstanceConsole("host-name", "test-extension", path, cmd);

        assertNotNull(result);
        assertEquals("External system error", result.getDetails());
    }

    @Test
    public void getInstanceConsoleReturnsErrorWhenConsoleObjectIsMissing() {
        String path = setupCheckedExtensionPath();
        GetExternalConsoleCommand cmd = mock(GetExternalConsoleCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(vmTO.getUuid()).thenReturn("test-uuid");

        String invalidOutput = "{\"invalid_key\":\"value\"}";
        doReturn(new Pair<>(true, invalidOutput)).when(provisioner)
                .getInstanceConsoleOnExternalSystem(anyString(), anyString(), anyString(), anyMap(), anyInt());

        GetExternalConsoleAnswer result = provisioner.getInstanceConsole("host-name", "test-extension", path, cmd);

        assertNotNull(result);
        assertEquals("Missing console object in output", result.getDetails());
    }

    @Test
    public void getInstanceConsoleReturnsErrorWhenRequiredFieldsAreMissing() {
        String path = setupCheckedExtensionPath();
        GetExternalConsoleCommand cmd = mock(GetExternalConsoleCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(vmTO.getUuid()).thenReturn("test-uuid");

        String incompleteOutput = "{\"console\":{\"host\":\"127.0.0.1\"}}";
        doReturn(new Pair<>(true, incompleteOutput)).when(provisioner)
                .getInstanceConsoleOnExternalSystem(anyString(), anyString(), anyString(), anyMap(), anyInt());

        GetExternalConsoleAnswer result = provisioner.getInstanceConsole("host-name", "test-extension", path, cmd);

        assertNotNull(result);
        assertEquals("Missing required fields in output", result.getDetails());
    }

    @Test
    public void getInstanceConsoleReturnsErrorWhenOutputParsingFails() {
        String path = setupCheckedExtensionPath();
        GetExternalConsoleCommand cmd = mock(GetExternalConsoleCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(vmTO.getUuid()).thenReturn("test-uuid");

        String malformedOutput = "{console:invalid}";
        doReturn(new Pair<>(true, malformedOutput)).when(provisioner)
                .getInstanceConsoleOnExternalSystem(anyString(), anyString(), anyString(), anyMap(), anyInt());

        GetExternalConsoleAnswer result = provisioner.getInstanceConsole("host-name", "test-extension", path, cmd);

        assertNotNull(result);
        assertEquals("Failed to parse output", result.getDetails());
    }

    @Test
    public void getInstanceConsoleOnExternalSystemReturnsSuccessWhenCommandExecutesSuccessfully() {
        String extensionName = "test-extension";
        String filename = "test-script.sh";
        String vmUUID = "test-vm-uuid";
        Map<String, Object> accessDetails = new HashMap<>();
        int wait = 30;

        doReturn(new Pair<>(true, "Console details")).when(provisioner)
            .executeExternalCommand(eq(extensionName), eq("getconsole"), eq(accessDetails), eq(wait), anyString(), eq(filename));

        Pair<Boolean, String> result = provisioner.getInstanceConsoleOnExternalSystem(extensionName, filename, vmUUID, accessDetails, wait);

        assertTrue(result.first());
        assertEquals("Console details", result.second());
    }

    @Test
    public void getInstanceConsoleOnExternalSystemReturnsFailureWhenCommandFails() {
        String extensionName = "test-extension";
        String filename = "test-script.sh";
        String vmUUID = "test-vm-uuid";
        Map<String, Object> accessDetails = new HashMap<>();
        int wait = 30;

        doReturn(new Pair<>(false, "Failed to get console")).when(provisioner)
            .executeExternalCommand(eq(extensionName), eq("getconsole"), eq(accessDetails), eq(wait), anyString(), eq(filename));

        Pair<Boolean, String> result = provisioner.getInstanceConsoleOnExternalSystem(extensionName, filename, vmUUID, accessDetails, wait);

        assertFalse(result.first());
        assertEquals("Failed to get console", result.second());
    }

    @Test
    public void getInstanceConsoleOnExternalSystemHandlesNullResponseGracefully() {
        String extensionName = "test-extension";
        String filename = "test-script.sh";
        String vmUUID = "test-vm-uuid";
        Map<String, Object> accessDetails = new HashMap<>();
        int wait = 30;

        doReturn(null).when(provisioner)
            .executeExternalCommand(eq(extensionName), eq("getconsole"), eq(accessDetails), eq(wait), anyString(), eq(filename));

        Pair<Boolean, String> result = provisioner.getInstanceConsoleOnExternalSystem(extensionName, filename, vmUUID, accessDetails, wait);

        assertNull(result);
    }

    @Test
    public void getSanitizedJsonStringForLogReturnsNullWhenInputIsNull() {
        String result = provisioner.getSanitizedJsonStringForLog(null);
        assertNull(result);
    }

    @Test
    public void getSanitizedJsonStringForLogReturnsEmptyWhenInputIsEmpty() {
        String result = provisioner.getSanitizedJsonStringForLog("");
        assertEquals("", result);
    }

    @Test
    public void getSanitizedJsonStringForLogReturnsSameStringWhenNoPasswordField() {
        String json = "{\"key\":\"value\"}";
        String result = provisioner.getSanitizedJsonStringForLog(json);
        assertEquals(json, result);
    }

    @Test
    public void getSanitizedJsonStringForLogMasksPasswordField() {
        String json = "{\"password\":\"secret\"}";
        String result = provisioner.getSanitizedJsonStringForLog(json);
        assertEquals("{\"password\":\"****\"}", result);
    }

    @Test
    public void getSanitizedJsonStringForLogHandlesMultiplePasswordFields() {
        String json = "{\"password\":\"secret\",\"nested\":{\"password\":\"anotherSecret\"}}";
        String result = provisioner.getSanitizedJsonStringForLog(json);
        assertEquals("{\"password\":\"****\",\"nested\":{\"password\":\"****\"}}", result);
    }

    @Test
    public void getSanitizedJsonStringForLogHandlesMalformedJsonGracefully() {
        String json = "{password:\"secret\"";
        String result = provisioner.getSanitizedJsonStringForLog(json);
        assertEquals("{password:\"secret\"", result);
    }

    @Test
    public void getExtensionConfigureErrorReturnsMessageWhenHostNameIsNotBlank() {
        String result = provisioner.getExtensionConfigureError("test-extension", "test-host");
        assertEquals("Extension: test-extension not configured for host: test-host", result);
    }

    @Test
    public void getExtensionConfigureErrorReturnsMessageWhenHostNameIsBlank() {
        String result = provisioner.getExtensionConfigureError("test-extension", "");
        assertEquals("Extension: test-extension not configured", result);
    }

    @Test
    public void getExtensionConfigureErrorReturnsMessageWhenHostNameIsNull() {
        String result = provisioner.getExtensionConfigureError("test-extension", null);
        assertEquals("Extension: test-extension not configured", result);
    }
}
