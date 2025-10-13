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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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

import com.cloud.utils.FileUtil;
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

        try (MockedStatic<PropertiesUtil> propertiesUtilMock = mockStatic(PropertiesUtil.class)) {
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
        verify(logger).error("{} is not executable", "Entry point [" + testScript.getAbsolutePath() + "] for extension: test-extension");
    }

    @Test
    public void testGetExtensionCheckedPathNoReadPermissions() {
        testScript.setWritable(false);
        testScript.setReadable(false);
        Assume.assumeFalse("Skipping test as file can not be marked unreadable", testScript.canRead());
        String result = extensionsFilesystemManager.getExtensionCheckedPath("test-extension", "test-extension.sh");
        assertNull(result);
        verify(logger).error("{} is not readable", "Entry point [" + testScript.getAbsolutePath() + "] for extension: test-extension");
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
        verify(logger).info("Extensions data directory path: {}", tempDataDir.getAbsolutePath());
    }

    @Test(expected = ConfigurationException.class)
    public void testCreateOrCheckExtensionsDataDirectoryCreateThrowsExceptionFail() throws ConfigurationException {
        ReflectionTestUtils.setField(extensionsFilesystemManager, "extensionsDataDirectory", "/nonexistent/path");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.createDirectories(any())).thenThrow(new IOException("fail"));
            extensionsFilesystemManager.createOrCheckExtensionsDataDirectory();
        }
    }

    @Test(expected = ConfigurationException.class)
    public void testCreateOrCheckExtensionsDataDirectoryNoCreateFail() throws ConfigurationException {
        ReflectionTestUtils.setField(extensionsFilesystemManager, "extensionsDataDirectory", "/nonexistent/path");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.createDirectories(any())).thenReturn(mock(Path.class));
            extensionsFilesystemManager.createOrCheckExtensionsDataDirectory();
        }
    }

    @Test
    public void schedulesCleanupTaskSuccessfully() {
        String extensionName = "test-extension";
        ExecutorService payloadCleanupExecutor = mock(ExecutorService.class);
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupExecutor", payloadCleanupExecutor);
        ScheduledExecutorService payloadCleanupScheduler = mock(ScheduledExecutorService.class);
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupScheduler", payloadCleanupScheduler);
        extensionsFilesystemManager.scheduleExtensionPayloadDirectoryCleanup(extensionName);

        verify(payloadCleanupExecutor).submit(any(Runnable.class));
        verify(payloadCleanupScheduler).schedule(any(Runnable.class), eq(3L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void handlesRejectedExecutionExceptionGracefully() {
        String extensionName = "test-extension";
        ExecutorService payloadCleanupExecutor = mock(ExecutorService.class);
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupExecutor", payloadCleanupExecutor);
        doThrow(new RejectedExecutionException("Task rejected"))
                .when(payloadCleanupExecutor).submit(any(Runnable.class));

        extensionsFilesystemManager.scheduleExtensionPayloadDirectoryCleanup(extensionName);

        verify(logger).warn("Payload cleanup task for extension: {} was rejected due to: {}", extensionName, "Task rejected");
    }

    @Test
    public void cancelsTaskIfNotCompletedWithinTimeout() {
        String extensionName = "test-extension";
        Future<?> mockFuture = mock(Future.class);
        ExecutorService payloadCleanupExecutor = mock(ExecutorService.class);
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupExecutor", payloadCleanupExecutor);
        ScheduledExecutorService payloadCleanupScheduler = mock(ScheduledExecutorService.class);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return mock(ScheduledFuture.class);
        }).when(payloadCleanupScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupScheduler", payloadCleanupScheduler);
        doReturn(mockFuture).when(payloadCleanupExecutor).submit(any(Runnable.class));
        Mockito.when(mockFuture.isDone()).thenReturn(false);

        extensionsFilesystemManager.scheduleExtensionPayloadDirectoryCleanup(extensionName);

        verify(mockFuture).cancel(true);
        verify(logger).trace("Cancelled cleaning up payload directory for extension: {} as it running for more than 3 seconds", extensionName);
    }

    @Test
    public void logsExceptionDuringCleanupTask() {
        String extensionName = "test-extension";
        doThrow(new RuntimeException("Cleanup error"))
                .when(extensionsFilesystemManager).cleanupExtensionData(extensionName, 1, false);
        ExecutorService payloadCleanupExecutor = mock(ExecutorService.class);
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupExecutor", payloadCleanupExecutor);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return mock(Future.class);
        }).when(payloadCleanupExecutor).submit(any(Runnable.class));
        ScheduledExecutorService payloadCleanupScheduler = mock(ScheduledExecutorService.class);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return mock(ScheduledFuture.class);
        }).when(payloadCleanupScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupScheduler", payloadCleanupScheduler);
        extensionsFilesystemManager.scheduleExtensionPayloadDirectoryCleanup(extensionName);

        verify(logger).warn("Exception during payload cleanup for extension: {} due to {}", extensionName, "Cleanup error");
    }

    @Test
    public void logsExceptionWhenCancellingTaskFails() {
        String extensionName = "test-extension";
        Future<?> mockFuture = mock(Future.class);
        ExecutorService payloadCleanupExecutor = mock(ExecutorService.class);
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupExecutor", payloadCleanupExecutor);
        ScheduledExecutorService payloadCleanupScheduler = mock(ScheduledExecutorService.class);
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return mock(ScheduledFuture.class);
        }).when(payloadCleanupScheduler).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        ReflectionTestUtils.setField(extensionsFilesystemManager, "payloadCleanupScheduler", payloadCleanupScheduler);
        doReturn(mockFuture).when(payloadCleanupExecutor).submit(any(Runnable.class));
        doThrow(new RuntimeException("Cancel error")).when(mockFuture).cancel(true);

        extensionsFilesystemManager.scheduleExtensionPayloadDirectoryCleanup(extensionName);

        verify(logger).warn("Failed to cancel payload cleanup task for extension: {} due to {}", extensionName, "Cancel error");
    }

    @Test
    public void getChecksumMapForExtensionReturnsChecksumsForAllFiles() throws IOException {
        String extensionName = "test-extension";
        Path rootPath = tempDir.toPath();
        Path file1 = Files.createFile(rootPath.resolve("file1.txt"));
        Path file2 = Files.createFile(rootPath.resolve("file2.txt"));
        doReturn(rootPath).when(extensionsFilesystemManager).getExtensionRootPath(extensionName);
        doReturn(rootPath.toString()).when(extensionsFilesystemManager).getExtensionCheckedPath(extensionName, "");

        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class)) {
            digestHelperMock.when(() -> DigestHelper.calculateChecksum(file1.toFile())).thenReturn("checksum1");
            digestHelperMock.when(() -> DigestHelper.calculateChecksum(file2.toFile())).thenReturn("checksum2");
            Map<String, String> result = extensionsFilesystemManager.getChecksumMapForExtension(extensionName, "");
            assertNotNull(result);
            for (Map.Entry<String, String> entry : result.entrySet()) {
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
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
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
        try (MockedStatic<DigestHelper> digestHelperMock = mockStatic(DigestHelper.class)) {
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
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.walk(rootPath)).thenReturn(Stream.empty());
            Map<String, String> result = extensionsFilesystemManager.getChecksumMapForExtension(extensionName, "");
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testPrepareExtensionPath() throws IOException {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
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
    public void cleansUpFileWhenPathIsValid() throws IOException {
        String extensionName = "test-extension";
        String extensionRelativePath = "test-file.txt";
        Path rootPath = Paths.get(tempDir.getAbsolutePath());
        Path filePath = rootPath.resolve(extensionRelativePath);
        Files.createFile(filePath);

        extensionsFilesystemManager.cleanupExtensionPath(extensionName, extensionRelativePath);

        assertFalse(Files.exists(filePath));
    }

    @Test
    public void doesNothingWhenPathDoesNotExist() {
        String extensionName = "test-extension";
        String extensionRelativePath = "nonexistent-file.txt";

        extensionsFilesystemManager.cleanupExtensionPath(extensionName, extensionRelativePath);

        Mockito.verifyNoInteractions(logger);
    }

    @Test(expected = CloudRuntimeException.class)
    public void throwsExceptionWhenPathIsNotFileOrDirectory() throws IOException {
        String extensionName = "test-extension";
        String extensionRelativePath = "invalid-path";
        Path rootPath = Paths.get(tempDir.getAbsolutePath());
        Path invalidPath = rootPath.resolve(extensionRelativePath);
        Files.createSymbolicLink(invalidPath, rootPath);

        extensionsFilesystemManager.cleanupExtensionPath(extensionName, extensionRelativePath);
    }

    @Test(expected = CloudRuntimeException.class)
    public void throwsExceptionWhenDeletionFails() throws IOException {
        String extensionName = "test-extension";
        String extensionRelativePath = "undeletable-file.txt";
        Path rootPath = Paths.get(tempDir.getAbsolutePath());
        Path filePath = rootPath.resolve(extensionRelativePath);
        Files.createFile(filePath);
        try (MockedStatic<FileUtil> fileUtilMock = mockStatic(FileUtil.class)) {
            fileUtilMock.when(() -> FileUtil.deleteRecursively(filePath)).thenReturn(false);

            extensionsFilesystemManager.cleanupExtensionPath(extensionName, extensionRelativePath);
        }
    }

    @Test
    public void cleansUpDirectoryWhenPathIsValid() throws IOException {
        String extensionName = "test-extension";
        String extensionRelativePath = "test-dir";
        Path rootPath = Paths.get(tempDir.getAbsolutePath());
        Path dirPath = rootPath.resolve(extensionRelativePath);
        Files.createDirectories(dirPath);

        extensionsFilesystemManager.cleanupExtensionPath(extensionName, extensionRelativePath);

        assertFalse(Files.exists(dirPath));
    }

    @Test
    public void cleansUpEntireDirectoryWhenCleanupDirectoryIsTrue() throws IOException {
        String extensionName = "test-extension";
        Path extensionDataDir = Paths.get(tempDataDir.getAbsolutePath(), extensionName);
        Files.createDirectories(extensionDataDir);
        Files.createFile(extensionDataDir.resolve("file1.txt"));
        Files.createFile(extensionDataDir.resolve("file2.txt"));

        extensionsFilesystemManager.cleanupExtensionData(extensionName, 1, true);

        assertFalse(Files.exists(extensionDataDir));
    }

    @Test
    public void cleansUpOldFilesWhenOlderThanDaysIsSpecified() throws IOException {
        String extensionName = "test-extension";
        Path extensionDataDir = Paths.get(tempDataDir.getAbsolutePath(), extensionName);
        Files.createDirectories(extensionDataDir);
        Path oldFile = extensionDataDir.resolve("old-file.txt");
        Path newFile = extensionDataDir.resolve("new-file.txt");
        Files.createFile(oldFile);
        Files.createFile(newFile);
        Files.setLastModifiedTime(oldFile, FileTime.fromMillis(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)));
        Files.setLastModifiedTime(newFile, FileTime.fromMillis(System.currentTimeMillis()));

        extensionsFilesystemManager.cleanupExtensionData(extensionName, 1, false);

        assertFalse(Files.exists(oldFile));
        assertTrue(Files.exists(newFile));
    }

    @Test
    public void doesNothingWhenDirectoryDoesNotExist() {
        String extensionName = "nonexistent-extension";

        extensionsFilesystemManager.cleanupExtensionData(extensionName, 1, true);

        Mockito.verifyNoInteractions(logger);
    }

    @Test
    public void handlesIOExceptionDuringFileWalkGracefully() throws IOException {
        String extensionName = "test-extension";
        Path extensionDataDir = Paths.get(tempDataDir.getAbsolutePath(), extensionName);
        Files.createDirectories(extensionDataDir);
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.walk(extensionDataDir)).thenThrow(new IOException("File walk error"));

            extensionsFilesystemManager.cleanupExtensionData(extensionName, 1, false);
        }

        assertTrue(Files.exists(extensionDataDir));
    }

    @Test
    public void skipsFilesNotOlderThanSpecifiedDays() throws IOException {
        String extensionName = "test-extension";
        Path extensionDataDir = Paths.get(tempDataDir.getAbsolutePath(), extensionName);
        Files.createDirectories(extensionDataDir);
        Path recentFile = extensionDataDir.resolve("recent-file.txt");
        Files.createFile(recentFile);
        Files.setLastModifiedTime(recentFile, FileTime.fromMillis(System.currentTimeMillis()));

        extensionsFilesystemManager.cleanupExtensionData(extensionName, 1, false);

        assertTrue(Files.exists(recentFile));
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
        try (MockedStatic<Files> filesMock = mockStatic(Files.class)) {
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
    public void doesNothingWhenFileListIsEmpty() {
        Extension extension = mock(Extension.class);
        extensionsFilesystemManager.validateExtensionFiles(extension, null);
        extensionsFilesystemManager.validateExtensionFiles(extension, List.of());
    }

    @Test(expected = CloudRuntimeException.class)
    public void throwsExceptionWhenExtensionDirectoryDoesNotExist() {
        Extension extension = mock(Extension.class);
        doReturn(Paths.get("/nonexistent/path")).when(extensionsFilesystemManager).getExtensionRootPath(extension);

        extensionsFilesystemManager.validateExtensionFiles(extension, List.of("file1.txt"));
    }

    @Test(expected = CloudRuntimeException.class)
    public void throwsExceptionWhenFileDoesNotExist() {
        Extension extension = mock(Extension.class);
        Path rootPath = tempDir.toPath();
        doReturn(rootPath).when(extensionsFilesystemManager).getExtensionRootPath(extension);

        extensionsFilesystemManager.validateExtensionFiles(extension, List.of("nonexistent-file.txt"));
    }

    @Test
    public void validatesFilesWhenAllExist() throws IOException {
        Extension extension = mock(Extension.class);
        Path rootPath = tempDir.toPath();
        Path file1 = Files.createFile(rootPath.resolve("file1.txt"));
        Path file2 = Files.createFile(rootPath.resolve("file2.txt"));
        doReturn(rootPath).when(extensionsFilesystemManager).getExtensionRootPath(extension);

        extensionsFilesystemManager.validateExtensionFiles(extension, List.of("file1.txt", "file2.txt"));

        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));
    }

    @Test(expected = CloudRuntimeException.class)
    public void throwsExceptionWhenRelativeFilePathIsInvalid() {
        Extension extension = mock(Extension.class);
        Path rootPath = tempDir.toPath();
        doReturn(rootPath).when(extensionsFilesystemManager).getExtensionRootPath(extension);

        extensionsFilesystemManager.validateExtensionFiles(extension, List.of("../invalid-path/file.txt"));
    }

    @Test
    public void deleteExtensionPayloadVerifyFileUtils() {
        try (MockedStatic<FileUtil> mockedStatic = mockStatic(FileUtil.class)) {
            String payloadFilePath = "payload.json";
            extensionsFilesystemManager.deleteExtensionPayload("test-extension", payloadFilePath);
            mockedStatic.verify(() -> FileUtil.deletePath(payloadFilePath), Mockito.times(1));
        }
    }
}
