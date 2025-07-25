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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.manager.ExtensionsManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

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
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.FileUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.script.Script;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

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
    private VMInstanceDao vmInstanceDao;

    @Mock
    private HypervisorGuruManager hypervisorGuruManager;

    @Mock
    private HypervisorGuru hypervisorGuru;

    @Mock
    private Logger logger;

    @Mock
    private ExtensionsManager extensionsManager;

    private File tempDir;
    private File tempDataDir;
    private Properties testProperties;
    private File testScript;

    @Before
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("extensions-test").toFile();
        tempDataDir = Files.createTempDirectory("extensions-data-test").toFile();

        testScript = new File(tempDir, "test-extension.sh");
        testScript.createNewFile();
        resetTestScript();

        testProperties = new Properties();
        testProperties.setProperty("extensions.deployment.mode", "developer");

        ReflectionTestUtils.setField(provisioner, "extensionsDirectory", tempDir.getAbsolutePath());
        ReflectionTestUtils.setField(provisioner, "extensionsDataDirectory", tempDataDir.getAbsolutePath());

        try (MockedStatic<PropertiesUtil> propertiesUtilMock = Mockito.mockStatic(PropertiesUtil.class)) {
            File mockPropsFile = mock(File.class);
            propertiesUtilMock.when(() -> PropertiesUtil.findConfigFile(anyString())).thenReturn(mockPropsFile);
        }
    }

    @After
    public void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            deleteDirectory(tempDir);
        }
        if (tempDataDir != null && tempDataDir.exists()) {
            deleteDirectory(tempDataDir);
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }

    private void resetTestScript() {
        testScript.setExecutable(true);
        testScript.setReadable(true);
        testScript.setWritable(true);
    }

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
    public void testGetExtensionCheckedPathValidFile() {
        String result = provisioner.getExtensionCheckedPath("test-extension", "test-extension.sh");

        assertEquals(testScript.getAbsolutePath(), result);
    }

    @Test
    public void testGetExtensionCheckedPathFileNotExists() {
        String result = provisioner.getExtensionCheckedPath("test-extension", "nonexistent.sh");

        assertNull(result);
    }

    @Test
    public void testGetExtensionCheckedPathNoExecutePermissions() {
        testScript.setExecutable(false);
        String result = provisioner.getExtensionCheckedPath("test-extension", "test-extension.sh");
        assertNull(result);
        Mockito.verify(logger).error("{} is not executable", "Entry point [" + testScript.getAbsolutePath() + "] for extension: test-extension");
    }

    @Test
    public void testGetExtensionCheckedPathNoReadPermissions() {
        testScript.setWritable(false);
        testScript.setReadable(false);
        Assume.assumeFalse("Skipping test as file can not be marked unreadable", testScript.canRead());
        String result = provisioner.getExtensionCheckedPath("test-extension", "test-extension.sh");
        assertNull(result);
        Mockito.verify(logger).error("{} is not readable", "Entry point [" + testScript.getAbsolutePath() + "] for extension: test-extension");
    }

    @Test
    public void testCheckExtensionsDirectoryValid() {
        boolean result = provisioner.checkExtensionsDirectory();
        assertTrue(result);
    }

    @Test
    public void testCheckExtensionsDirectoryInvalid() {
        ReflectionTestUtils.setField(provisioner, "extensionsDirectory", "/nonexistent/path");

        boolean result = provisioner.checkExtensionsDirectory();
        assertFalse(result);
    }

    @Test
    public void testCreateOrCheckExtensionsDataDirectory() throws ConfigurationException {
        provisioner.createOrCheckExtensionsDataDirectory();
        Mockito.verify(logger).info("Extensions data directory path: {}", tempDataDir.getAbsolutePath());
    }

    @Test(expected = ConfigurationException.class)
    public void testCreateOrCheckExtensionsDataDirectoryCreateThrowsExceptionFail() throws ConfigurationException {
        ReflectionTestUtils.setField(provisioner, "extensionsDataDirectory", "/nonexistent/path");
        try(MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.createDirectories(any())).thenThrow(new IOException("fail"));
            provisioner.createOrCheckExtensionsDataDirectory();
        }
    }

    @Test(expected = ConfigurationException.class)
    public void testCreateOrCheckExtensionsDataDirectoryNoCreateFail() throws ConfigurationException {
        ReflectionTestUtils.setField(provisioner, "extensionsDataDirectory", "/nonexistent/path");
        try(MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.createDirectories(any())).thenReturn(mock(Path.class));
            provisioner.createOrCheckExtensionsDataDirectory();
        }
    }

    @Test
    public void testGetExtensionPath() {
        String result = provisioner.getExtensionPath("test-extension.sh");
        String expected = tempDir.getAbsolutePath() + File.separator + "test-extension.sh";
        assertEquals(expected, result);
    }

    @Test
    public void testGetChecksumForExtensionPath() {
        String result = provisioner.getChecksumForExtensionPath("test-extension", "test-extension.sh");

        assertNotNull(result);
    }

    @Test
    public void testGetChecksumForExtensionPath_InvalidFile() {
        String result = provisioner.getChecksumForExtensionPath("test-extension", "nonexistent.sh");

        assertNull(result);
    }

    @Test
    public void testPrepareExternalProvisioning() {
        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            scriptMock.when(() -> Script.findScript(anyString(), anyString())).thenReturn(testScript.getAbsolutePath());

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
                "host-guid", "test-extension", "test-extension.sh", cmd);

            assertTrue(answer.getResult());
            assertEquals("test-net-uuid", answer.getVirtualMachineTO().getNics()[0].getNetworkUuid());
            assertEquals("00:00:00:01:02:03", answer.getVirtualMachineTO().getNics()[0].getMac());
        }
    }

    @Test
    public void testPrepareExternalProvisioning_ExtensionNotConfigured() {
        PrepareExternalProvisioningCommand cmd = mock(PrepareExternalProvisioningCommand.class);

        PrepareExternalProvisioningAnswer answer = provisioner.prepareExternalProvisioning(
            "host-guid", "test-extension", "nonexistent.sh", cmd);

        assertFalse(answer.getResult());
        assertEquals("Extension not configured", answer.getDetails());
    }

    @Test
    public void testStartInstance() {
        StartCommand cmd = mock(StartCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "{\"status\": \"success\", \"message\": \"Instance started\"}")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StartAnswer answer = provisioner.startInstance("host-guid", "test-extension", "test-extension.sh", cmd);

        assertTrue(answer.getResult());
        Mockito.verify(logger).debug("Starting VM test-uuid on the external system");
    }

    @Test
    public void testStartInstanceDeploy() {
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

        StartAnswer answer = provisioner.startInstance("host-guid", "test-extension", "test-extension.sh", cmd);

        assertTrue(answer.getResult());
        Mockito.verify(logger).debug("Deploying VM test-uuid on the external system");
    }

    @Test
    public void testStartInstanceError() {
        StartCommand cmd = mock(StartCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(false, "{\"error\": \"Instance failed to start\"}")).when(provisioner)
                .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StartAnswer answer = provisioner.startInstance("host-guid", "test-extension", "test-extension.sh", cmd);

        assertFalse(answer.getResult());
        assertEquals("{\"error\": \"Instance failed to start\"}", answer.getDetails());
        Mockito.verify(logger).debug("Starting VM test-uuid on the external system failed: {\"error\": \"Instance failed to start\"}");
    }

    @Test
    public void testStopInstance() {
        StopCommand cmd = mock(StopCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "success")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StopAnswer answer = provisioner.stopInstance("host-guid", "test-extension", "test-extension.sh", cmd);

        assertTrue(answer.getResult());
    }

    @Test
    public void testRebootInstance() {
        RebootCommand cmd = mock(RebootCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "success")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        RebootAnswer answer = provisioner.rebootInstance("host-guid", "test-extension", "test-extension.sh", cmd);

        assertTrue(answer.getResult());
    }

    @Test
    public void testExpungeInstance() {
        StopCommand cmd = mock(StopCommand.class);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(vmTO.getUuid()).thenReturn("test-uuid");
        when(cmd.getVirtualMachine()).thenReturn(vmTO);
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);

        doReturn(new Pair<>(true, "success")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        StopAnswer answer = provisioner.expungeInstance("host-guid", "test-extension", "test-extension.sh", cmd);

        assertTrue(answer.getResult());
    }

    @Test
    public void testGetHostVmStateReport() {
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

        Map<String, HostVmStateReportEntry> result = provisioner.getHostVmStateReport(1L, "test-extension", "test-extension.sh");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("test-instance"));
    }

    @Test
    public void testGetHostVmStateReportHostNotFound() {
        Map<String, HostVmStateReportEntry> result = provisioner.getHostVmStateReport(1L, "test-extension", "test-extension.sh");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        Mockito.verify(logger).error("Host with ID: {} not found", 1L);
    }

    @Test
    public void testRunCustomAction() {
        RunCustomActionCommand cmd = mock(RunCustomActionCommand.class);
        when(cmd.getActionName()).thenReturn("test-action");
        when(cmd.getParameters()).thenReturn(new HashMap<>());
        when(cmd.getExternalDetails()).thenReturn(new HashMap<>());
        when(cmd.getWait()).thenReturn(30);
        when(cmd.getVmId()).thenReturn(1L);

        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vm);

        when(hypervisorGuruManager.getGuru(Hypervisor.HypervisorType.External)).thenReturn(hypervisorGuru);
        VirtualMachineTO vmTO = mock(VirtualMachineTO.class);
        when(hypervisorGuru.implement(any(VirtualMachineProfile.class))).thenReturn(vmTO);

        doReturn(new Pair<>(true, "success")).when(provisioner)
            .executeExternalCommand(anyString(), anyString(), anyMap(), anyInt(), anyString(), anyString());

        RunCustomActionAnswer answer = provisioner.runCustomAction("host-guid", "test-extension", "test-extension.sh", cmd);

        assertTrue(answer.getResult());
        Mockito.verify(logger).debug("Executing custom action '{}' in the external system", "test-action");
    }

    @Test
    public void testPrepareExtensionPath() throws IOException {
        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            File mockScript = new File(tempDir, "provisioner.sh");
            mockScript.createNewFile();
            scriptMock.when(() -> Script.findScript(anyString(), anyString())).thenReturn(mockScript.getAbsolutePath());

            provisioner.prepareExtensionPath("test-extension", true, "test-extension-new.sh");

            File createdFile = new File(tempDir, "test-extension-new.sh");
            assertTrue(createdFile.exists());
        }
    }

    @Test
    public void testPrepareExtensionPathNotUserDefined() {
        provisioner.prepareExtensionPath("test-extension", false, "test-extension-builtin.sh");
        File createdFile = new File(tempDir, "test-extension-builtin.sh");
        assertFalse(createdFile.exists());
    }

    @Test
    public void testPrepareExtensionPathNotBashScript() {
        provisioner.prepareExtensionPath("test-extension", true, "test-extension.txt");

        File createdFile = new File(tempDir, "test-extension.txt");
        assertFalse(createdFile.exists());
    }

    @Test
    public void testPrepareExtensionPathFileAlreadyExists() {
        File existingFile = new File(tempDir, "test-extension.sh");

        provisioner.prepareExtensionPath("test-extension", true, "test-extension.sh");

        assertTrue(existingFile.exists());
    }

    @Test
    public void testCleanupExtensionPath() throws IOException {
        String extensionDirName = Extension.getDirectoryName("test-extension");
        File extensionDir = new File(tempDir, extensionDirName);
        extensionDir.mkdirs();
        File testFile = new File(extensionDir, "test-file.txt");
        testFile.createNewFile();

        provisioner.cleanupExtensionPath("test-extension", extensionDirName + "/test-file.txt");

        assertFalse(testFile.exists());
    }

    @Test
    public void testCleanupExtensionData() throws IOException {
        File extensionDataDir = new File(tempDataDir, "test-extension");
        extensionDataDir.mkdirs();
        File testFile = new File(extensionDataDir, "test-file.txt");
        testFile.createNewFile();

        provisioner.cleanupExtensionData("test-extension", 1, true);

        assertFalse(extensionDataDir.exists());
    }

    @Test
    public void testExecuteExternalCommand() throws IOException {
        String scriptContent = "#!/bin/bash\nif grep -q '{\"test\":\"value\"}' \"$2\"; then\necho 'success'\nexit 0\nelse\necho 'fail'\nexit 1\nfi";
        Files.write(testScript.toPath(), scriptContent.getBytes());

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

        try (MockedStatic<FileUtil> fileUtilMock = Mockito.mockStatic(FileUtil.class)) {
            provisioner.deleteExtensionPayloadFile(extensionName, action, payloadFileName);

            fileUtilMock.verify(() -> FileUtil.deletePath(payloadFileName));
            Mockito.verify(logger).trace(
                    "Deleting payload file: {} for extension: {}, action: {}, file: {}",
                    payloadFileName, extensionName, action
            );
        }
    }

    @Test
    public void deleteExtensionPayloadFile_DoesNothing_WhenActionIsNotTrivial() {
        String extensionName = "test-extension";
        String action = "start";
        String payloadFileName = "/tmp/test-payload.json";

        try (MockedStatic<FileUtil> fileUtilMock = Mockito.mockStatic(FileUtil.class)) {
            provisioner.deleteExtensionPayloadFile(extensionName, action, payloadFileName);

            fileUtilMock.verifyNoInteractions();
            Mockito.verify(logger, Mockito.never()).trace(anyString(), any(), any(), any(), any());
        }
    }

    @Test
    public void prepareExternalPayloadCreatesFileInNewDirectory() throws IOException {
        String extensionName = "test-extension";
        Map<String, Object> details = Map.of("key", "value");
        String result = provisioner.prepareExternalPayload(extensionName, details);
        assertNotNull(result);
        Path payloadFile = Paths.get(result);
        assertTrue(Files.exists(payloadFile));
        assertTrue(Files.isRegularFile(payloadFile));
        assertEquals("{\"key\":\"value\"}", Files.readString(payloadFile));
    }

    @Test
    public void prepareExternalPayloadCreatesFileInExistingDirectory() throws IOException {
        String extensionName = "test-extension";
        Map<String, Object> details = Map.of("key", "value");
        Path existingDir = Paths.get(tempDataDir.getAbsolutePath(), extensionName);
        Files.createDirectories(existingDir);
        doNothing().when(provisioner).scheduleExtensionPayloadDirectoryCleanup(extensionName);
        String result = provisioner.prepareExternalPayload(extensionName, details);
        assertNotNull(result);
        Path payloadFile = Paths.get(result);
        assertTrue(Files.exists(payloadFile));
        assertTrue(Files.isRegularFile(payloadFile));
        assertEquals("{\"key\":\"value\"}", Files.readString(payloadFile));
    }

    @Test
    public void prepareExternalPayloadSchedulesCleanupForExistingDirectory() throws IOException {
        String extensionName = "test-extension";
        Map<String, Object> details = Map.of("key", "value");
        Path existingDir = Paths.get(tempDataDir.getAbsolutePath(), extensionName);
        Files.createDirectories(existingDir);
        doNothing().when(provisioner).scheduleExtensionPayloadDirectoryCleanup(extensionName);
        provisioner.prepareExternalPayload(extensionName, details);
    }

    @Test(expected = IOException.class)
    public void prepareExternalPayloadThrowsExceptionWhenFileCannotBeWritten() throws IOException {
        String extensionName = "test-extension";
        Map<String, Object> details = Map.of("key", "value");
        Path existingDir = Paths.get(tempDataDir.getAbsolutePath(), extensionName);
        Files.createDirectories(existingDir);
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.writeString(any(Path.class), anyString(), eq(StandardOpenOption.CREATE_NEW))).thenThrow(IOException.class);
            provisioner.prepareExternalPayload(extensionName, details);
        }
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
}
