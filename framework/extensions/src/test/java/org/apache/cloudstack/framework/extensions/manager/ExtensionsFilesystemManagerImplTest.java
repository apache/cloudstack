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

package org.apache.cloudstack.framework.extensions.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.logging.log4j.Logger;
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

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionsFilesystemManagerImplTest {

    @Mock
    private Logger logger;

    @Spy
    @InjectMocks
    private ExtensionsFilesystemManagerImpl extensionsFilesystemManager;

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

        ReflectionTestUtils.setField(extensionsFilesystemManager, "extensionsDirectory", tempDir.getAbsolutePath());
        ReflectionTestUtils.setField(extensionsFilesystemManager, "extensionsDataDirectory", tempDataDir.getAbsolutePath());

        try (MockedStatic<PropertiesUtil> propertiesUtilMock = Mockito.mockStatic(PropertiesUtil.class)) {
            File mockPropsFile = mock(File.class);
            propertiesUtilMock.when(() -> PropertiesUtil.findConfigFile(anyString())).thenReturn(mockPropsFile);
        }
    }

    private void resetTestScript() {
        testScript.setExecutable(true);
        testScript.setReadable(true);
        testScript.setWritable(true);
    }

    @Test
    public void getExtensionRootPathReturnsCorrectPathForValidExtension() {
        Extension extension = mock(Extension.class);
        Mockito.when(extension.getName()).thenReturn("test-extension");
        String expectedPath = tempDir.getAbsolutePath() + File.separator + "test-extension";
        Path result = extensionsFilesystemManager.getExtensionRootPath(extension);
        assertEquals(expectedPath, result.toString());
    }


    @Test
    public void testGetExtensionPath() {
        String result = extensionsFilesystemManager.getExtensionPath("test-extension.sh");
        String expected = tempDir.getAbsolutePath() + File.separator + "test-extension.sh";
        assertEquals(expected, result);
    }

    @Test
    public void testGetExtensionCheckedPathValidFile() {
        String result = extensionsFilesystemManager.getExtensionCheckedPath("test-extension", "test-extension.sh");

        assertEquals(testScript.getAbsolutePath(), result);
    }

    @Test
    public void testGetExtensionCheckedPathFileNotExists() {
        String result = extensionsFilesystemManager.getExtensionCheckedPath("test-extension", "nonexistent.sh");

        assertNull(result);
    }

    @Test
    public void testGetExtensionCheckedPathNoExecutePermissions() {
        testScript.setExecutable(false);
        String result = extensionsFilesystemManager.getExtensionCheckedPath("test-extension", "test-extension.sh");
        assertNull(result);
        Mockito.verify(logger).error("{} is not executable", "Entry point [" + testScript.getAbsolutePath() + "] for extension: test-extension");
    }

    @Test
    public void testGetExtensionCheckedPathNoReadPermissions() {
        testScript.setWritable(false);
        testScript.setReadable(false);
        Assume.assumeFalse("Skipping test as file can not be marked unreadable", testScript.canRead());
        String result = extensionsFilesystemManager.getExtensionCheckedPath("test-extension", "test-extension.sh");
        assertNull(result);
        Mockito.verify(logger).error("{} is not readable", "Entry point [" + testScript.getAbsolutePath() + "] for extension: test-extension");
    }

    @Test
    public void testCheckExtensionsDirectoryValid() {
        boolean result = extensionsFilesystemManager.checkExtensionsDirectory();
        assertTrue(result);
    }

    @Test
    public void testCheckExtensionsDirectoryInvalid() {
        ReflectionTestUtils.setField(extensionsFilesystemManager, "extensionsDirectory", "/nonexistent/path");

        boolean result = extensionsFilesystemManager.checkExtensionsDirectory();
        assertFalse(result);
    }

    @Test
    public void testCreateOrCheckExtensionsDataDirectory() throws ConfigurationException {
        extensionsFilesystemManager.createOrCheckExtensionsDataDirectory();
        Mockito.verify(logger).info("Extensions data directory path: {}", tempDataDir.getAbsolutePath());
    }

    @Test(expected = ConfigurationException.class)
    public void testCreateOrCheckExtensionsDataDirectoryCreateThrowsExceptionFail() throws ConfigurationException {
        ReflectionTestUtils.setField(extensionsFilesystemManager, "extensionsDataDirectory", "/nonexistent/path");
        try(MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.createDirectories(any())).thenThrow(new IOException("fail"));
            extensionsFilesystemManager.createOrCheckExtensionsDataDirectory();
        }
    }

    @Test(expected = ConfigurationException.class)
    public void testCreateOrCheckExtensionsDataDirectoryNoCreateFail() throws ConfigurationException {
        ReflectionTestUtils.setField(extensionsFilesystemManager, "extensionsDataDirectory", "/nonexistent/path");
        try(MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.createDirectories(any())).thenReturn(mock(Path.class));
            extensionsFilesystemManager.createOrCheckExtensionsDataDirectory();
        }
    }

    @Test
    public void getChecksumMapForExtensionReturnsChecksumsForAllFiles() throws IOException {
        String extensionName = "test-extension";
        Path rootPath = tempDir.toPath();
        Path file1 = Files.createFile(rootPath.resolve("file1.txt"));
        Path file2 = Files.createFile(rootPath.resolve("file2.txt"));
        doReturn(rootPath).when(extensionsFilesystemManager).getExtensionRootPath(extensionName);
        doReturn(rootPath.toString()).when(extensionsFilesystemManager).getExtensionCheckedPath(extensionName, "");

        try (MockedStatic<DigestHelper> digestHelperMock = Mockito.mockStatic(DigestHelper.class)) {
            digestHelperMock.when(() -> DigestHelper.calculateChecksum(file1.toFile())).thenReturn("checksum1");
            digestHelperMock.when(() -> DigestHelper.calculateChecksum(file2.toFile())).thenReturn("checksum2");
            Map<String, String> result = extensionsFilesystemManager.getChecksumMapForExtension(extensionName, "");
            assertNotNull(result);
            for(Map.Entry<String, String> entry : result.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            assertTrue(result.size() > 2);
            assertEquals("checksum1", result.get("file1.txt"));
            assertEquals("checksum2", result.get("file2.txt"));
        }
    }

    @Test
    public void getChecksumMapForExtensionReturnsNullForBlankPath() {
        String extensionName = "test-extension";
        doReturn(null).when(extensionsFilesystemManager).getExtensionCheckedPath(extensionName, "");
        Map<String, String> result = extensionsFilesystemManager.getChecksumMapForExtension(extensionName, "");
        assertNull(result);
    }

    @Test
    public void getChecksumMapForExtensionHandlesIOExceptionDuringFileWalk() {
        String extensionName = "test-extension";
        Path rootPath = tempDir.toPath();
        doReturn(rootPath).when(extensionsFilesystemManager).getExtensionRootPath(extensionName);
        doReturn(rootPath.toString()).when(extensionsFilesystemManager).getExtensionCheckedPath(extensionName, "");
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.walk(rootPath)).thenThrow(new IOException("File walk error"));
            Map<String, String> result = extensionsFilesystemManager.getChecksumMapForExtension(extensionName, "");
            assertNull(result);
        }
    }

    @Test
    public void getChecksumMapForExtensionHandlesChecksumCalculationFailure() throws IOException {
        String extensionName = "test-extension";
        Path rootPath = tempDir.toPath();
        Path file1 = Files.createFile(rootPath.resolve("file1.txt"));
        doReturn(rootPath).when(extensionsFilesystemManager).getExtensionRootPath(extensionName);
        doReturn(rootPath.toString()).when(extensionsFilesystemManager).getExtensionCheckedPath(extensionName, "");
        try (MockedStatic<DigestHelper> digestHelperMock = Mockito.mockStatic(DigestHelper.class)) {
            digestHelperMock.when(() -> DigestHelper.calculateChecksum(file1.toFile())).thenThrow(new CloudRuntimeException("Checksum error"));
            Map<String, String> result = extensionsFilesystemManager.getChecksumMapForExtension(extensionName, "");
            assertNull(result);
        }
    }

    @Test
    public void getChecksumMapForExtensionReturnsEmptyMapWhenNoFilesExist() {
        String extensionName = "test-extension";
        Path rootPath = tempDir.toPath();
        doReturn(rootPath).when(extensionsFilesystemManager).getExtensionRootPath(extensionName);
        doReturn(rootPath.toString()).when(extensionsFilesystemManager).getExtensionCheckedPath(extensionName, "");
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.walk(rootPath)).thenReturn(Stream.empty());
            Map<String, String> result = extensionsFilesystemManager.getChecksumMapForExtension(extensionName, "");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void packFilesAsTgzReturnsTrue() {
        Path sourcePath = tempDir.toPath();
        Path archivePath = Path.of("test-archive.tgz");
        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class))).thenReturn(0);
            boolean result = extensionsFilesystemManager.packFilesAsTgz(sourcePath, archivePath);
            assertTrue(result);
        }
    }

    @Test
    public void packFilesAsTgzReturnsFalse() {
        Path sourcePath = tempDir.toPath();
        Path archivePath = Path.of("test-archive.tgz");
        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            scriptMock.when(() -> Script.executeCommandForExitValue(anyLong(), any(String[].class))).thenReturn(1);
            boolean result = extensionsFilesystemManager.packFilesAsTgz(sourcePath, archivePath);
            assertFalse(result);
        }
    }

    @Test
    public void packFilesAsTgzForValidInputs() {
        if ("tar".equals(Script.getExecutableAbsolutePath("tar"))) {
            System.out.println("Skipping test as tar command is not available");
            // tar command not found; skipping test
            return;
        }
        Path sourcePath = tempDir.toPath();
        Path archivePath = Paths.get(System.getProperty("java.io.tmpdir"), "testarchive-" + UUID.randomUUID() + ".tgz");
        try {
            boolean result = extensionsFilesystemManager.packFilesAsTgz(sourcePath, archivePath);
            assertTrue(result);
        } finally {
            try {
                Files.deleteIfExists(archivePath);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Test
    public void testPrepareExtensionPath() throws IOException {
        try (MockedStatic<Script> scriptMock = Mockito.mockStatic(Script.class)) {
            File mockScript = new File(tempDir, "extensionsFilesystemManager.sh");
            mockScript.createNewFile();
            scriptMock.when(() -> Script.findScript(anyString(), anyString())).thenReturn(mockScript.getAbsolutePath());

            extensionsFilesystemManager.prepareExtensionPath(
                    "test-extension", true, Extension.Type.Orchestrator, "test-extension-new.sh");

            File createdFile = new File(tempDir, "test-extension-new.sh");
            assertTrue(createdFile.exists());
        }
    }

    @Test
    public void testPrepareExtensionPathNotUserDefined() {
        extensionsFilesystemManager.prepareExtensionPath("test-extension", false, null, "test-extension-builtin.sh");
        File createdFile = new File(tempDir, "test-extension-builtin.sh");
        assertFalse(createdFile.exists());
    }

    @Test
    public void testPrepareExtensionPathNotBashScript() {
        extensionsFilesystemManager.prepareExtensionPath("test-extension", true, Extension.Type.Orchestrator, "test-extension.txt");

        File createdFile = new File(tempDir, "test-extension.txt");
        assertFalse(createdFile.exists());
    }

    @Test
    public void testPrepareExtensionPathFileAlreadyExists() {
        File existingFile = new File(tempDir, "test-extension.sh");

        extensionsFilesystemManager.prepareExtensionPath("test-extension", true, Extension.Type.Orchestrator, "test-extension.sh");

        assertTrue(existingFile.exists());
    }

    @Test
    public void testCleanupExtensionPath() throws IOException {
        String extensionDirName = Extension.getDirectoryName("test-extension");
        File extensionDir = new File(tempDir, extensionDirName);
        extensionDir.mkdirs();
        File testFile = new File(extensionDir, "test-file.txt");
        testFile.createNewFile();

        extensionsFilesystemManager.cleanupExtensionPath("test-extension", extensionDirName + "/test-file.txt");

        assertFalse(testFile.exists());
    }

    @Test
    public void testCleanupExtensionData() throws IOException {
        File extensionDataDir = new File(tempDataDir, "test-extension");
        extensionDataDir.mkdirs();
        File testFile = new File(extensionDataDir, "test-file.txt");
        testFile.createNewFile();

        extensionsFilesystemManager.cleanupExtensionData("test-extension", 1, true);

        assertFalse(extensionDataDir.exists());
    }

    @Test
    public void prepareExternalPayloadCreatesFileInNewDirectory() throws IOException {
        String extensionName = "test-extension";
        Map<String, Object> details = Map.of("key", "value");
        String result = extensionsFilesystemManager.prepareExternalPayload(extensionName, details);
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
        doNothing().when(extensionsFilesystemManager).scheduleExtensionPayloadDirectoryCleanup(extensionName);
        String result = extensionsFilesystemManager.prepareExternalPayload(extensionName, details);
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
        doNothing().when(extensionsFilesystemManager).scheduleExtensionPayloadDirectoryCleanup(extensionName);
        extensionsFilesystemManager.prepareExternalPayload(extensionName, details);
    }

    @Test(expected = IOException.class)
    public void prepareExternalPayloadThrowsExceptionWhenFileCannotBeWritten() throws IOException {
        String extensionName = "test-extension";
        Map<String, Object> details = Map.of("key", "value");
        Path existingDir = Paths.get(tempDataDir.getAbsolutePath(), extensionName);
        Files.createDirectories(existingDir);
        try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
            filesMock.when(() -> Files.writeString(any(Path.class), anyString(), eq(StandardOpenOption.CREATE_NEW))).thenThrow(IOException.class);
            extensionsFilesystemManager.prepareExternalPayload(extensionName, details);
        }
    }

    @Test
    public void getExtensionsStagingPathCreatesDirectoryIfNotExists() throws IOException {
        Path stagingPath = Paths.get(tempDir.getAbsolutePath(), ".staging");
        assertFalse(Files.exists(stagingPath));
        Path result = extensionsFilesystemManager.getExtensionsStagingPath();
        assertTrue(Files.exists(stagingPath));
        assertTrue(Files.isDirectory(stagingPath));
        assertEquals(stagingPath.toAbsolutePath(), result);
    }

    @Test
    public void getExtensionsStagingPathReturnsExistingDirectory() throws IOException {
        Path stagingPath = Paths.get(tempDir.getAbsolutePath(), ".staging");
        Files.createDirectories(stagingPath);
        Path result = extensionsFilesystemManager.getExtensionsStagingPath();
        assertTrue(Files.exists(stagingPath));
        assertTrue(Files.isDirectory(stagingPath));
        assertEquals(stagingPath.toAbsolutePath(), result);
    }

    @Test(expected = IOException.class)
    public void getExtensionsStagingPathThrowsExceptionWhenDirectoryCannotBeCreated() throws IOException {
        Path stagingPath = Paths.get(tempDir.getAbsolutePath(), ".staging");
        stagingPath.toFile().createNewFile(); // Create a file with the same name to block directory creation
        extensionsFilesystemManager.getExtensionsStagingPath();
    }

    @Test
    public void packExtensionFilesAsTgzReturnsTrue() {
        Extension extension = mock(Extension.class);
        Path sourcePath = tempDir.toPath();
        Path archivePath = Path.of("test-archive.tgz");
        doReturn(true).when(extensionsFilesystemManager).packFilesAsTgz(sourcePath, archivePath);
        boolean result = extensionsFilesystemManager.packExtensionFilesAsTgz(extension, sourcePath, archivePath);
        assertTrue(result);
    }

    @Test
    public void packExtensionFilesAsTgzReturnsFalse() {
        Extension extension = mock(Extension.class);
        Path sourcePath = tempDir.toPath();
        Path archivePath = Path.of("test-archive.tgz");
        doReturn(false).when(extensionsFilesystemManager).packFilesAsTgz(sourcePath, archivePath);
        boolean result = extensionsFilesystemManager.packExtensionFilesAsTgz(extension, sourcePath, archivePath);
        assertFalse(result);
    }
}
