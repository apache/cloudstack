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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;

import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.framework.extensions.command.DownloadAndSyncExtensionFilesCommand;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.utils.filesystem.ArchiveUtil;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.cloudstack.utils.security.HMACSignUtil;
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.cluster.ClusterManager;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.FileUtil;
import com.cloud.utils.HttpUtils;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionsShareManagerImplTest {

    @Mock
    ExtensionsFilesystemManager extensionsFilesystemManager;

    @Mock
    ClusterManager clusterManager;

    @Spy
    @InjectMocks
    ExtensionsShareManagerImpl extensionsShareManager = new ExtensionsShareManagerImpl();

    @Test
    public void getExtensionsSharePathReturnsCorrectPath() {
        String baseDir = "/cloudstack";
        String expectedPath = baseDir + File.separator + ExtensionsShareManagerImpl.EXTENSIONS_SHARE_SUBDIR;
        try (MockedStatic<ServerPropertiesUtil> serverPropertiesUtilMock = mockStatic(ServerPropertiesUtil.class)) {
            serverPropertiesUtilMock.when(ServerPropertiesUtil::getShareBaseDirectory).thenReturn(baseDir);
            Path result = extensionsShareManager.getExtensionsSharePath();
            assertEquals(expectedPath, result.toString());
        }
    }

    @Test
    public void getExtensionsSharePathHandlesEmptyBaseDirectory() {
        try (MockedStatic<ServerPropertiesUtil> serverPropertiesUtilMock = mockStatic(ServerPropertiesUtil.class)) {
            serverPropertiesUtilMock.when(ServerPropertiesUtil::getShareBaseDirectory).thenReturn("");
            Path result = extensionsShareManager.getExtensionsSharePath();
            assertNotNull(result);
            assertEquals(File.separator + ExtensionsShareManagerImpl.EXTENSIONS_SHARE_SUBDIR, result.toString());
        }
    }

    @Test
    public void getManagementServerBaseUrlHandlesHttpPort() {
        int port = 8181;
        String ip = "192.168.1.1";
        try (MockedStatic<ServerPropertiesUtil> serverPropertiesUtilMock = mockStatic(ServerPropertiesUtil.class)) {
            serverPropertiesUtilMock.when(() -> ServerPropertiesUtil.getProperty("https.enable", "false")).thenReturn("false");
            serverPropertiesUtilMock.when(() -> ServerPropertiesUtil.getProperty("http.port", "8080")).thenReturn(String.valueOf(port));
            ManagementServerHost managementHost = mock(ManagementServerHost.class);
            when(managementHost.getServiceIP()).thenReturn(ip);
            assertEquals(String.format("http://%s:%d", ip, port), extensionsShareManager.getManagementServerBaseUrl(managementHost));
        }
    }

    @Test
    public void getManagementServerBaseUrlHandlesHttpsPort() {
        int port = 8433;
        String ip = "192.168.1.1";
        try (MockedStatic<ServerPropertiesUtil> serverPropertiesUtilMock = mockStatic(ServerPropertiesUtil.class)) {
            serverPropertiesUtilMock.when(() -> ServerPropertiesUtil.getProperty("https.enable", "false")).thenReturn("true");
            serverPropertiesUtilMock.when(() -> ServerPropertiesUtil.getProperty("https.port", "8443")).thenReturn(String.valueOf(port));
            ManagementServerHost managementHost = mock(ManagementServerHost.class);
            when(managementHost.getServiceIP()).thenReturn(ip);
            assertEquals(String.format("https://%s:%d", ip, port), extensionsShareManager.getManagementServerBaseUrl(managementHost));
        }
    }

    @Test
    public void getResultFromAnswersStringParsesValidJsonAndReturnsSuccess() {
        Answer[] answers = new Answer[]{new Answer(mock(DownloadAndSyncExtensionFilesCommand.class), true, "Operation successful")};
        String json = GsonHelper.getGson().toJson(answers);
        Extension extension = mock(Extension.class);
        ManagementServerHost msHost = mock(ManagementServerHost.class);

        Pair<Boolean, String> result = extensionsShareManager.getResultFromAnswersString(json, extension, msHost, "operation");

        assertTrue(result.first());
        assertEquals("Operation successful", result.second());
    }

    @Test
    public void getResultFromAnswersStringHandlesEmptyJsonArray() {
        String answersStr = "[]";
        Extension extension = mock(Extension.class);
        ManagementServerHost msHost = mock(ManagementServerHost.class);

        Pair<Boolean, String> result = extensionsShareManager.getResultFromAnswersString(answersStr, extension, msHost, "operation");

        assertFalse(result.first());
        assertEquals("Unknown error", result.second());
    }

    @Test
    public void getResultFromAnswersStringHandlesNullAnswer() {
        String answersStr = "null";
        Extension extension = mock(Extension.class);
        ManagementServerHost msHost = mock(ManagementServerHost.class);

        Pair<Boolean, String> result = extensionsShareManager.getResultFromAnswersString(answersStr, extension, msHost, "operation");

        assertFalse(result.first());
        assertEquals("Unknown error", result.second());
    }

    @Test
    public void getResultFromAnswersStringHandlesJsonParsingError() {
        String answersStr = "invalid-json";
        Extension extension = mock(Extension.class);
        ManagementServerHost msHost = mock(ManagementServerHost.class);

        Pair<Boolean, String> result = extensionsShareManager.getResultFromAnswersString(answersStr, extension, msHost, "operation");

        assertFalse(result.first());
        assertNotNull(result.second());
    }

    @Test
    public void getResultFromAnswersStringHandlesFailedOperation() {
        Answer[] answers = new Answer[]{new Answer(mock(DownloadAndSyncExtensionFilesCommand.class), false, "Operation failed")};
        String json = GsonHelper.getGson().toJson(answers);
        Extension extension = mock(Extension.class);
        ManagementServerHost msHost = mock(ManagementServerHost.class);

        Pair<Boolean, String> result = extensionsShareManager.getResultFromAnswersString(json, extension, msHost, "operation");

        assertFalse(result.first());
        assertEquals("Operation failed", result.second());
    }

    @Test
    public void createArchiveCreatesCompleteArchiveForSyncSuccessfully() throws IOException {
        Extension extension = mock(Extension.class);
        String name = "testExtension";
        String path = "extensions" + File.separator + name;
        when(extension.getName()).thenReturn(name);
        when(extension.getRelativePath()).thenReturn(path);
        Path extensionRootPath = Path.of(path);
        when(extensionsFilesystemManager.getExtensionCheckedPath(name, path)).thenReturn(path);
        when(extensionsFilesystemManager.getExtensionRootPath(extension)).thenReturn(extensionRootPath);
        try (MockedStatic<DigestHelper> ignored = mockStatic(DigestHelper.class);
             MockedStatic<Files> ignored1 = mockStatic(Files.class)) {
            doReturn(true).when(extensionsShareManager).packArchiveForSync(eq(extension), eq(extensionRootPath),
                    Mockito.anyList(), Mockito.any());
            String sharePath = "/share/extensions";
            doReturn(Path.of(sharePath)).when(extensionsShareManager).getExtensionsSharePath();
            when(DigestHelper.calculateChecksum(Mockito.any())).thenReturn("checksum123");
            when(Files.size(Mockito.any())).thenReturn(100L);
            ExtensionsShareManagerImpl.ArchiveInfo archiveInfo = extensionsShareManager.createArchiveForSync(extension, List.of());
            assertNotNull(archiveInfo);
            System.out.println(archiveInfo.getPath().toString());
            assertTrue(archiveInfo.getPath().toString().matches(sharePath + File.separator + Extension.getDirectoryName(name) + "-\\d+\\.tgz"));
            assertEquals("checksum123", archiveInfo.getChecksum());
            assertEquals(100L, archiveInfo.getSize());
            assertEquals(DownloadAndSyncExtensionFilesCommand.SyncType.Complete, archiveInfo.getSyncType());
        }
    }

    @Test
    public void createArchiveCreatesPartialArchiveForSyncSuccessfully() throws IOException {
        Extension extension = mock(Extension.class);
        String name = "testExtension";
        String path = "extensions" + File.separator + name;
        when(extension.getName()).thenReturn(name);
        when(extension.getRelativePath()).thenReturn(path);
        Path extensionRootPath = Path.of(path);
        when(extensionsFilesystemManager.getExtensionCheckedPath(name, path)).thenReturn(path);
        when(extensionsFilesystemManager.getExtensionRootPath(extension)).thenReturn(extensionRootPath);
        try (MockedStatic<DigestHelper> ignored = mockStatic(DigestHelper.class);
             MockedStatic<Files> ignored1 = mockStatic(Files.class)) {
            when(Files.exists(any())).thenReturn(true);
            doReturn(true).when(extensionsShareManager).packArchiveForSync(eq(extension), eq(extensionRootPath),
                    Mockito.anyList(), Mockito.any());
            String sharePath = "/share/extensions";
            doReturn(Path.of(sharePath)).when(extensionsShareManager).getExtensionsSharePath();
            when(DigestHelper.calculateChecksum(Mockito.any())).thenReturn("checksum123");
            when(Files.size(Mockito.any())).thenReturn(200L);
            ExtensionsShareManagerImpl.ArchiveInfo archiveInfo = extensionsShareManager.createArchiveForSync(extension, List.of("file1.sh", "file2.sh"));
            assertNotNull(archiveInfo);
            assertTrue(archiveInfo.getPath().toString().matches(sharePath + File.separator + "partial-" + Extension.getDirectoryName(name) + "-\\d+\\.tgz"));
            assertEquals("checksum123", archiveInfo.getChecksum());
            assertEquals(200L, archiveInfo.getSize());
            assertEquals(DownloadAndSyncExtensionFilesCommand.SyncType.Partial, archiveInfo.getSyncType());
        }
    }

    @Test(expected = IOException.class)
    public void createArchiveForSyncThrowsExceptionForInvalidExtensionPath() throws IOException{
        Extension extension = mock(Extension.class);
        String name = "testExtension";
        String path = "extensions" + File.separator + name;
        when(extension.getName()).thenReturn(name);
        when(extension.getRelativePath()).thenReturn(path);
        when(extensionsFilesystemManager.getExtensionCheckedPath(name, path)).thenReturn(null);
        extensionsShareManager.createArchiveForSync(extension, Collections.emptyList());
    }

    @Test
    public void createArchiveForSyncThrowsExceptionForInvalidFilePath() {
        Extension extension = mock(Extension.class);
        String name = "testExtension";
        String path = "extensions" + File.separator + name;
        when(extension.getName()).thenReturn(name);
        when(extension.getRelativePath()).thenReturn(path);
        Path extensionRootPath = Path.of(path);
        when(extensionsFilesystemManager.getExtensionCheckedPath(name, path)).thenReturn(path);
        when(extensionsFilesystemManager.getExtensionRootPath(extension)).thenReturn(extensionRootPath);
        assertThrows(SecurityException.class, () -> extensionsShareManager.createArchiveForSync(extension, List.of("../invalid.txt")));
    }

    @Test
    public void createArchiveForSyncThrowsExceptionForMissingFile() {
        Extension extension = mock(Extension.class);
        String name = "testExtension";
        String path = "extensions" + File.separator + name;
        when(extension.getName()).thenReturn(name);
        when(extension.getRelativePath()).thenReturn(path);
        Path extensionRootPath = Path.of(path);
        when(extensionsFilesystemManager.getExtensionCheckedPath(name, path)).thenReturn(path);
        when(extensionsFilesystemManager.getExtensionRootPath(extension)).thenReturn(extensionRootPath);
        assertThrows(NoSuchFileException.class, () -> extensionsShareManager.createArchiveForSync(extension, List.of("missing.txt")));
    }

    @Test
    public void createArchiveForSyncThrowsIOExceptionWhenPackingFails() throws IOException {
        Extension extension = mock(Extension.class);
        String name = "testExtension";
        String path = "extensions" + File.separator + name;
        when(extension.getName()).thenReturn(name);
        when(extension.getRelativePath()).thenReturn(path);
        Path extensionRootPath = Path.of(path);
        when(extensionsFilesystemManager.getExtensionCheckedPath(name, path)).thenReturn(path);
        when(extensionsFilesystemManager.getExtensionRootPath(extension)).thenReturn(extensionRootPath);
        doReturn(false).when(extensionsShareManager).packArchiveForSync(eq(extension), eq(extensionRootPath),
                Mockito.anyList(), Mockito.any());

        assertThrows(IOException.class, () -> extensionsShareManager.createArchiveForSync(extension, List.of()));
    }

    @Test
    public void generateSignedArchiveUrlReturnsValidUrlWithSignature() throws Exception {
        ManagementServerHost managementServer = mock(ManagementServerHost.class);
        Path archivePath = Path.of("/share/extensions/test-archive.tgz");
        String baseUrl = "http://abc";
        doReturn(baseUrl).when(extensionsShareManager).getManagementServerBaseUrl(managementServer);
        try (MockedStatic<ServerPropertiesUtil> serverPropertiesUtilMock = mockStatic(ServerPropertiesUtil.class);
             MockedStatic<HMACSignUtil> hmacSignUtilMock = mockStatic(HMACSignUtil.class)) {
            serverPropertiesUtilMock.when(() -> ServerPropertiesUtil.getShareSecret()).thenReturn("secretKey");
            hmacSignUtilMock.when(() -> HMACSignUtil.generateSignature(anyString(), anyString())).thenReturn("signature123");
            String result = extensionsShareManager.generateSignedArchiveUrl(managementServer, archivePath);
            assertTrue(result.startsWith(baseUrl + archivePath));
            assertTrue(result.contains("exp="));
            assertTrue(result.contains("sig=signature123"));
        }
    }

    @Test
    public void generateSignedArchiveUrlReturnsUrlWithoutSignatureWhenSecretKeyIsBlank() throws Exception {
        ManagementServerHost managementServer = mock(ManagementServerHost.class);
        Path archivePath = Path.of("/share/extensions/test-archive.tgz");
        String baseUrl = "http://abc";
        doReturn(baseUrl).when(extensionsShareManager).getManagementServerBaseUrl(managementServer);
        String result = extensionsShareManager.generateSignedArchiveUrl(managementServer, archivePath);
        assertTrue(result.startsWith(baseUrl + archivePath));
        assertTrue(result.contains("exp="));
        assertFalse(result.contains("sig="));
    }

    @Test
    public void buildCommandCreatesCommandWithCorrectParameters() {
        long msId = 12345L;
        Extension extension = mock(Extension.class);
        when(extension.getId()).thenReturn(100L);
        ExtensionsShareManagerImpl.ArchiveInfo archiveInfo = mock(ExtensionsShareManagerImpl.ArchiveInfo.class);
        when(archiveInfo.getSize()).thenReturn(1024L);
        when(archiveInfo.getChecksum()).thenReturn("checksum123");
        when(archiveInfo.getSyncType()).thenReturn(DownloadAndSyncExtensionFilesCommand.SyncType.Complete);
        String signedUrl = "http://example.com/archive.tgz";

        DownloadAndSyncExtensionFilesCommand command = extensionsShareManager.buildCommand(msId, extension, archiveInfo, signedUrl);

        assertNotNull(command);
        assertEquals(msId, command.getMsId());
        assertEquals(100L, command.getExtensionId());
        assertEquals(signedUrl, command.getDownloadUrl());
        assertEquals(1024L, command.getSize());
        assertEquals("checksum123", command.getChecksum());
        assertEquals(DownloadAndSyncExtensionFilesCommand.SyncType.Complete, command.getSyncType());
    }

    @Test
    public void buildCommandHandlesNullArchiveInfo() {
        long msId = 12345L;
        Extension extension = mock(Extension.class);
        String signedUrl = "http://example.com/archive.tgz";

        assertThrows(NullPointerException.class, () -> extensionsShareManager.buildCommand(msId, extension, null, signedUrl));
    }

    @Test
    public void packArchiveForSyncCreatesArchiveForSingleDirectory() throws IOException {
        Path extensionRootPath = Files.createTempDirectory("extensionRoot");
        Path archivePath = Files.createTempFile("archive", ".tgz");
        Extension extension = mock(Extension.class);
        try (MockedStatic<ArchiveUtil> mockedStatic = mockStatic(ArchiveUtil.class)) {
            mockedStatic.when(() -> ArchiveUtil.packPath(ArchiveUtil.ArchiveFormat.TGZ, extensionRootPath, archivePath, 60))
                    .thenReturn(true);
            boolean result = extensionsShareManager.packArchiveForSync(extension, extensionRootPath,
                    List.of(extensionRootPath), archivePath);
            assertTrue(result);
            assertFalse(Files.exists(archivePath));
        } finally {
            FileUtil.deleteRecursively(extensionRootPath);
            Files.deleteIfExists(archivePath);
        }
    }

    @Test
    public void packArchiveForSyncCreatesArchiveForSingleDirectoryReturnsFalse() throws IOException {
        Path extensionRootPath = Files.createTempDirectory("extensionRoot");
        Path archivePath = Files.createTempFile("archive", ".tgz");
        Extension extension = mock(Extension.class);
        try (MockedStatic<ArchiveUtil> mockedStatic = mockStatic(ArchiveUtil.class)) {
            mockedStatic.when(() -> ArchiveUtil.packPath(ArchiveUtil.ArchiveFormat.TGZ, extensionRootPath, archivePath, 60))
                    .thenReturn(false);
            boolean result = extensionsShareManager.packArchiveForSync(extension, extensionRootPath,
                    List.of(extensionRootPath), archivePath);
            assertFalse(result);
            assertFalse(Files.exists(archivePath));
        } finally {
            FileUtil.deleteRecursively(extensionRootPath);
            Files.deleteIfExists(archivePath);
        }
    }

    @Test
    public void packArchiveForSyncCreatesArchiveForMultipleFiles() throws IOException {
        Path extensionRootPath = Files.createTempDirectory("extensionRoot");
        Path archivePath = Files.createTempFile("archive", ".tgz");
        Path file1 = Files.createFile(extensionRootPath.resolve("file1.txt"));
        Path file2 = Files.createFile(extensionRootPath.resolve("file2.txt"));
        Extension extension = mock(Extension.class);
        try (MockedStatic<ArchiveUtil> mockedStatic = mockStatic(ArchiveUtil.class)) {
            mockedStatic.when(() -> ArchiveUtil.packPath(eq(ArchiveUtil.ArchiveFormat.TGZ), any(Path.class), eq(archivePath), eq(60)))
                    .thenReturn(true);
            boolean result = extensionsShareManager.packArchiveForSync(extension, extensionRootPath, List.of(file1, file2),
                    archivePath);
            assertTrue(result);
            assertFalse(Files.exists(archivePath));
        } finally {
            FileUtil.deleteRecursively(extensionRootPath);
            Files.deleteIfExists(archivePath);
            Files.deleteIfExists(file1);
            Files.deleteIfExists(file2);
        }
    }

    @Test
    public void downloadToSuccessfullyDownloadsFile() throws IOException {
        Path dest = Files.createTempFile("download", ".tmp");
        try (MockedStatic<HttpUtils> httpUtilsMock = mockStatic(HttpUtils.class)) {
            httpUtilsMock.when(() -> HttpUtils.downloadFileWithProgress(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                    .thenReturn(true);
            long size = extensionsShareManager.downloadTo("http://example.com/file", dest);
            assertTrue(Files.exists(dest));
            assertEquals(Files.size(dest), size);
        } finally {
            Files.deleteIfExists(dest);
        }
    }

    @Test
    public void downloadToThrowsExceptionWhenDownloadFails() throws IOException {
        Path dest = Files.createTempFile("download", ".tmp");
        try (MockedStatic<HttpUtils> httpUtilsMock = mockStatic(HttpUtils.class)) {
            httpUtilsMock.when(() -> HttpUtils.downloadFileWithProgress(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                    .thenReturn(false);

            assertThrows(IOException.class, () -> extensionsShareManager.downloadTo("http://example.com/file", dest));
        } finally {
            Files.deleteIfExists(dest);
        }
    }

    @Test
    public void downloadToThrowsExceptionWhenFileNotFound() throws IOException {
        Path dest = Files.createTempFile("download", ".tmp");
        try (MockedStatic<HttpUtils> httpUtilsMock = mockStatic(HttpUtils.class)) {
            httpUtilsMock.when(() -> HttpUtils.downloadFileWithProgress(Mockito.anyString(), Mockito.anyString(), Mockito.any()))
                    .thenReturn(true);
            Files.deleteIfExists(dest);
            assertThrows(IOException.class, () -> extensionsShareManager.downloadTo("http://example.com/file", dest));
        }
    }

    @Test
    public void atomicReplaceDirReplacesTargetDirectorySuccessfully() throws IOException {
        Path source = Files.createTempDirectory("sourceDir");
        Path target = Files.createTempDirectory("targetDir");
        try {
            Files.createFile(source.resolve("file.txt"));
            Files.createFile(target.resolve("oldFile.txt"));
            ExtensionsShareManagerImpl.atomicReplaceDir(source, target);
            assertTrue(Files.exists(target.resolve("file.txt")));
            assertFalse(Files.exists(target.resolve("oldFile.txt")));
        } finally {
            FileUtil.deleteRecursively(source);
            FileUtil.deleteRecursively(target);
        }
    }

    @Test
    public void atomicReplaceDirCreatesTargetDirectoryWhenMissing() throws IOException {
        Path source = Files.createTempDirectory("sourceDir");
        Path target = source.getParent().resolve("nonexistentTarget");
        try {
            Files.createFile(source.resolve("file.txt"));
            ExtensionsShareManagerImpl.atomicReplaceDir(source, target);
            assertTrue(Files.exists(target.resolve("file.txt")));
        } finally {
            FileUtil.deleteRecursively(source);
            FileUtil.deleteRecursively(target);
        }
    }

    @Test
    public void atomicReplaceDirRestoresBackupOnFailure() throws IOException {
        Path source = Files.createTempDirectory("sourceDir");
        Path target = Files.createTempDirectory("targetDir");
        Path backup = target.getParent().resolve(target.getFileName().toString() + ".bak-" + System.currentTimeMillis());
        Files.createFile(source.resolve("file.txt"));
        Files.createFile(target.resolve("oldFile.txt"));
        try (MockedStatic<Files> filesMock = mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            filesMock.when(() -> Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)).thenThrow(IOException.class);
            assertThrows(IOException.class, () -> ExtensionsShareManagerImpl.atomicReplaceDir(source, target));
            assertTrue(Files.exists(source.resolve("file.txt")));
            assertTrue(Files.exists(target.resolve("oldFile.txt")));
        } finally {
            FileUtil.deleteRecursively(source);
            FileUtil.deleteRecursively(target);
            FileUtil.deleteRecursively(backup);
        }
    }

    @Test
    public void atomicReplaceDirHandlesNonAtomicBackupMove() throws IOException {
        Path source = Files.createTempDirectory("sourceDir");
        Path target = Files.createTempDirectory("targetDir");
        Path backup = target.getParent().resolve(target.getFileName().toString() + ".bak-" + System.currentTimeMillis());
        try (MockedStatic<Files> filesMock = mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            Files.createFile(source.resolve("file.txt"));
            Files.createFile(target.resolve("oldFile.txt"));
            filesMock.when(() -> Files.move(target, backup, StandardCopyOption.ATOMIC_MOVE))
                    .thenThrow(IOException.class);
            ExtensionsShareManagerImpl.atomicReplaceDir(source, target);
            assertTrue(Files.exists(target.resolve("file.txt")));
            assertFalse(Files.exists(target.resolve("oldFile.txt")));
        } finally {
            FileUtil.deleteRecursively(source);
            FileUtil.deleteRecursively(target);
            FileUtil.deleteRecursively(backup);
        }
    }

    @Test
    public void overlayIntoOverlaysFilesSuccessfully() throws IOException {
        Path source = Files.createTempDirectory("sourceDir");
        Path target = Files.createTempDirectory("targetDir");
        try {
            Files.createDirectories(source.resolve("subdir"));
            Files.createFile(source.resolve("subdir/file1.txt"));
            Files.createFile(source.resolve("file2.txt"));
            ExtensionsShareManagerImpl.overlayInto(source, target);
            assertTrue(Files.exists(target.resolve("subdir/file1.txt")));
            assertTrue(Files.exists(target.resolve("file2.txt")));
        } finally {
            FileUtil.deleteRecursively(source);
            FileUtil.deleteRecursively(target);
        }
    }

    @Test
    public void overlayIntoHandlesEmptySourceDirectory() throws IOException {
        Path source = Files.createTempDirectory("sourceDir");
        Path target = Files.createTempDirectory("targetDir");
        try {
            ExtensionsShareManagerImpl.overlayInto(source, target);
            assertTrue(Files.exists(target));
            assertEquals(0, Files.list(target).count());
        } finally {
            FileUtil.deleteRecursively(source);
            FileUtil.deleteRecursively(target);
        }
    }

    @Test
    public void overlayIntoReplacesExistingFilesInTarget() throws IOException {
        Path source = Files.createTempDirectory("sourceDir");
        Path target = Files.createTempDirectory("targetDir");
        try {
            Files.createFile(source.resolve("file.txt"));
            Files.createFile(target.resolve("file.txt"));
            ExtensionsShareManagerImpl.overlayInto(source, target);
            assertTrue(Files.exists(target.resolve("file.txt")));
            assertEquals(Files.size(source.resolve("file.txt")), Files.size(target.resolve("file.txt")));
        } finally {
            FileUtil.deleteRecursively(source);
            FileUtil.deleteRecursively(target);
        }
    }

    @Test
    public void applyExtensionSyncExtractsAndReplacesDirectoryForCompleteSync() throws IOException {
        Extension extension = mock(Extension.class);
        when(extension.getName()).thenReturn("testExtension");
        Path tmpArchive = Files.createTempFile("archive", ".tgz");
        Path extensionRootPath = Files.createTempDirectory("extensionRoot");
        Path stagingDir = Files.createTempDirectory("stagingDir");
        when(extensionsFilesystemManager.getExtensionsStagingPath()).thenReturn(stagingDir);
        try (MockedStatic<ArchiveUtil> mockedStatic = mockStatic(ArchiveUtil.class);
             MockedStatic<ExtensionsShareManagerImpl> extensionsShareManagerMockedStatic =
                     mockStatic(ExtensionsShareManagerImpl.class)) {
            mockedStatic.when(() -> ArchiveUtil.extractToPath(eq(ArchiveUtil.ArchiveFormat.TGZ), eq(tmpArchive), any(Path.class), eq(60)))
                    .thenReturn(true);
            extensionsShareManager.applyExtensionSync(extension, DownloadAndSyncExtensionFilesCommand.SyncType.Complete, tmpArchive, extensionRootPath);
            extensionsShareManagerMockedStatic.verify(() -> ExtensionsShareManagerImpl.atomicReplaceDir(
                    any(Path.class), eq(extensionRootPath)));
        } finally {
            FileUtil.deleteRecursively(tmpArchive);
            FileUtil.deleteRecursively(extensionRootPath);
            FileUtil.deleteRecursively(stagingDir);
        }
    }

    @Test
    public void applyExtensionSyncExtractsAndOverlaysFilesForPartialSync() throws IOException {
        Extension extension = mock(Extension.class);
        when(extension.getName()).thenReturn("testExtension");
        Path tmpArchive = Files.createTempFile("archive", ".tgz");
        Path extensionRootPath = Files.createTempDirectory("extensionRoot");
        Path stagingDir = Files.createTempDirectory("stagingDir");
        when(extensionsFilesystemManager.getExtensionsStagingPath()).thenReturn(stagingDir);
        try (MockedStatic<ArchiveUtil> mockedStatic = mockStatic(ArchiveUtil.class);
            MockedStatic<ExtensionsShareManagerImpl> extensionsShareManagerMockedStatic =
                    mockStatic(ExtensionsShareManagerImpl.class)) {
            mockedStatic.when(() -> ArchiveUtil.extractToPath(eq(ArchiveUtil.ArchiveFormat.TGZ), eq(tmpArchive), any(Path.class), eq(60)))
                    .thenReturn(true);
            extensionsShareManager.applyExtensionSync(extension, DownloadAndSyncExtensionFilesCommand.SyncType.Partial,
                    tmpArchive, extensionRootPath);
            extensionsShareManagerMockedStatic.verify(() -> ExtensionsShareManagerImpl.overlayInto(
                    any(Path.class), eq(extensionRootPath)));
            assertEquals(0, Files.list(stagingDir).count());
        } finally {
            FileUtil.deleteRecursively(tmpArchive);
            FileUtil.deleteRecursively(extensionRootPath);
            FileUtil.deleteRecursively(stagingDir);
        }
    }

    @Test
    public void applyExtensionSyncThrowsIOExceptionWhenExtractionFails() throws IOException {
        Extension extension = mock(Extension.class);
        when(extension.getName()).thenReturn("testExtension");
        Path tmpArchive = Files.createTempFile("archive", ".tgz");
        Path stagingDir = Files.createTempDirectory("stagingDir");
        when(extensionsFilesystemManager.getExtensionsStagingPath()).thenReturn(stagingDir);
        try (MockedStatic<ArchiveUtil> mockedStatic = mockStatic(ArchiveUtil.class)) {
            mockedStatic.when(() -> ArchiveUtil.extractToPath(eq(ArchiveUtil.ArchiveFormat.TGZ), eq(tmpArchive), any(Path.class), eq(60)))
                    .thenReturn(false);
            assertThrows(IOException.class, () -> extensionsShareManager.applyExtensionSync(
                    extension, DownloadAndSyncExtensionFilesCommand.SyncType.Complete, tmpArchive, stagingDir));
        } finally {
            FileUtil.deleteRecursively(tmpArchive);
            FileUtil.deleteRecursively(stagingDir);
        }
    }

    @Test
    public void cleanupExtensionsShareFilesDeletesExpiredArchives() throws IOException {
        long cutoffDiff = 1000000L;
        Path sharePath = Files.createTempDirectory("sharePath");
        Path expiredFile = Files.createFile(sharePath.resolve("expired.tgz"));
        Files.setLastModifiedTime(expiredFile, FileTime.fromMillis(System.currentTimeMillis() - 2 * cutoffDiff));
        Path validFile = Files.createFile(sharePath.resolve("valid.tgz"));
        Files.setLastModifiedTime(validFile, FileTime.fromMillis(System.currentTimeMillis() + (cutoffDiff / 2)));
        try {
            doReturn(sharePath).when(extensionsShareManager).getExtensionsSharePath();
            extensionsShareManager.cleanupExtensionsShareFiles(System.currentTimeMillis() - cutoffDiff);
            assertFalse(Files.exists(expiredFile));
            assertTrue(Files.exists(validFile));
        } finally {
            FileUtil.deleteRecursively(sharePath);
        }
    }

    @Test
    public void cleanupExtensionsShareFilesHandlesNonTgzFilesGracefully() throws IOException {
        Path sharePath = Files.createTempDirectory("sharePath");
        Path nonTgzFile = Files.createFile(sharePath.resolve("file.txt"));
        try {
            doReturn(sharePath).when(extensionsShareManager).getExtensionsSharePath();
            extensionsShareManager.cleanupExtensionsShareFiles(System.currentTimeMillis());
            assertTrue(Files.exists(nonTgzFile));
        } finally {
            FileUtil.deleteRecursively(sharePath);
        }
    }

    @Test
    public void cleanupExtensionsShareFilesSkipsNonExistentSharePath() throws IOException {
        Path nonExistentPath = Path.of("/nonexistent/sharePath");
        doReturn(nonExistentPath).when(extensionsShareManager).getExtensionsSharePath();
        extensionsShareManager.cleanupExtensionsShareFiles(System.currentTimeMillis());
        // No exception should be thrown
    }

    @Test
    public void syncExtensionReturnsSuccessWhenAllTargetsSucceed() throws Exception {
        Extension extension = mock(Extension.class);
        ManagementServerHost sourceManagementServer = mock(ManagementServerHost.class);
        ManagementServerHost targetManagementServer1 = mock(ManagementServerHost.class);
        when(targetManagementServer1.getUuid()).thenReturn("uuid123");
        when(targetManagementServer1.getMsid()).thenReturn(12345L);
        ManagementServerHost targetManagementServer2 = mock(ManagementServerHost.class);
        when(targetManagementServer2.getUuid()).thenReturn("uuid234");
        when(targetManagementServer2.getMsid()).thenReturn(23456L);
        List<ManagementServerHost> targetManagementServers = List.of(targetManagementServer1,
                targetManagementServer2);
        List<String> files = List.of("file1.txt", "file2.txt");

        ExtensionsShareManagerImpl.ArchiveInfo archiveInfo = mock(ExtensionsShareManagerImpl.ArchiveInfo.class);
        doReturn(archiveInfo).when(extensionsShareManager).createArchiveForSync(extension, files);
        when(archiveInfo.getPath()).thenReturn(Path.of("/path/to/archive.tgz"));
        when(extensionsShareManager.generateSignedArchiveUrl(sourceManagementServer, archiveInfo.getPath()))
                .thenReturn("http://example.com/archive.tgz");
        when(clusterManager.execute(anyString(), anyLong(), anyString(), eq(true)))
                .thenReturn("Something");
        doReturn(new Pair<>(true, "success")).when(extensionsShareManager)
                .getResultFromAnswersString(anyString(), eq(extension), any(ManagementServerHost.class), anyString());

        Pair<Boolean, String> result =
                extensionsShareManager.syncExtension(extension, sourceManagementServer, targetManagementServers, files);

        assertTrue(result.first());
        assertEquals("", result.second());
    }

    @Test
    public void syncExtensionReturnsFailureWhenArchiveCreationFails() throws Exception {
        Extension extension = mock(Extension.class);
        ManagementServerHost sourceManagementServer = mock(ManagementServerHost.class);
        List<ManagementServerHost> targetManagementServers = List.of();
        List<String> files = List.of("file1.txt");
        doThrow(new IOException("Archive creation failed")).when(extensionsShareManager)
                .createArchiveForSync(extension, files);
        Pair<Boolean, String> result = extensionsShareManager.syncExtension(extension, sourceManagementServer,
                targetManagementServers, files);
        assertFalse(result.first());
        assertEquals("Failed to create archive", result.second());
    }

    @Test
    public void syncExtensionReturnsFailureWhenSignedUrlGenerationFails() throws Exception {
        Extension extension = mock(Extension.class);
        ManagementServerHost sourceManagementServer = mock(ManagementServerHost.class);
        List<ManagementServerHost> targetManagementServers = List.of();
        List<String> files = List.of("file1.txt");
        ExtensionsShareManagerImpl.ArchiveInfo archiveInfo = mock(ExtensionsShareManagerImpl.ArchiveInfo.class);
        doReturn(archiveInfo).when(extensionsShareManager).createArchiveForSync(extension, files);
        when(archiveInfo.getPath()).thenReturn(Path.of("/path/to/archive.tgz"));
        when(extensionsShareManager.generateSignedArchiveUrl(sourceManagementServer, archiveInfo.getPath()))
                .thenThrow(new NoSuchAlgorithmException("HMAC error"));
        Pair<Boolean, String> result = extensionsShareManager.syncExtension(extension, sourceManagementServer,
                targetManagementServers, files);
        assertFalse(result.first());
        assertEquals("Failed to generate signed URL", result.second());
    }

    @Test
    public void syncExtensionReturnsFailureWhenTargetSyncFails() throws Exception {
        Extension extension = mock(Extension.class);
        ManagementServerHost sourceManagementServer = mock(ManagementServerHost.class);
        ManagementServerHost targetManagementServer = mock(ManagementServerHost.class);
        when(targetManagementServer.getUuid()).thenReturn("uuid123");
        when(targetManagementServer.getMsid()).thenReturn(12345L);
        when(targetManagementServer.getName()).thenReturn("targetServer");
        List<ManagementServerHost> targetManagementServers = List.of(targetManagementServer);
        List<String> files = List.of("file1.txt");
        ExtensionsShareManagerImpl.ArchiveInfo archiveInfo = mock(ExtensionsShareManagerImpl.ArchiveInfo.class);
        doReturn(archiveInfo).when(extensionsShareManager).createArchiveForSync(extension, files);
        when(archiveInfo.getPath()).thenReturn(Path.of("/path/to/archive.tgz"));
        when(extensionsShareManager.generateSignedArchiveUrl(sourceManagementServer, archiveInfo.getPath()))
                .thenReturn("http://example.com/archive.tgz");
        when(clusterManager.execute(anyString(), anyLong(), anyString(), eq(true)))
                .thenReturn("error");
        doReturn(new Pair<>(false, "error")).when(extensionsShareManager)
                .getResultFromAnswersString(anyString(), eq(extension), any(ManagementServerHost.class), anyString());
        Pair<Boolean, String> result = extensionsShareManager.syncExtension(extension, sourceManagementServer,
                targetManagementServers, files);
        assertFalse(result.first());
        assertEquals("Sync failed on management server: targetServer", result.second());
    }
}
