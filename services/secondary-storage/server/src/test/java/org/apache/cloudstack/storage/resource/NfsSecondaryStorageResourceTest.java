/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.NfsTO;
import org.apache.cloudstack.storage.command.BackupDeleteAnswer;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.QuerySnapshotZoneCopyAnswer;
import org.apache.cloudstack.storage.command.QuerySnapshotZoneCopyCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.EncryptionUtil;
import com.cloud.utils.net.NetUtils;

@RunWith(MockitoJUnitRunner.class)
public class NfsSecondaryStorageResourceTest {

    @Spy
    private NfsSecondaryStorageResource resource;

    private static final String HOSTNAME = "hostname";

    private static final String UUID = "uuid";

    private static final String METADATA = "metadata";

    private static final String TIMEOUT = "timeout";

    private static final String PSK = "6HyGMx9Vat7rZw1pMZrM4OlD4FFwLUPznTsFqVFSOIvk0mAWMRCVZ6UCq42gZvhp";

    private static final String PROTOCOL = NetUtils.HTTP_PROTO;

    private static final String EXPECTED_SIGNATURE = "expectedSignature";

    private static final String COMPUTED_SIGNATURE = "computedSignature";

    @Mock
    private Logger loggerMock;

    @Mock
    private DeleteCommand deleteCommandMock;

    @Mock
    private BackupDeltaTO backupDeltaTOMock;

    @Mock
    private NfsTO nfsMock;

    @Test
    public void testSwiftWriteMetadataFile() throws Exception {
        String metaFileName = "test_metadata_file";
        try {
            String uniqueName = "test_unique_name";
            String filename = "test_filename";
            long size = 1024L;
            long virtualSize = 2048L;

            File metaFile = resource.swiftWriteMetadataFile(metaFileName, uniqueName, filename, size, virtualSize);

            assertTrue(metaFile.exists());
            Assert.assertEquals(metaFileName, metaFile.getName());

            String expectedContent = "uniquename=" + uniqueName + "\n" +
                    "filename=" + filename + "\n" +
                    "size=" + size + "\n" +
                    "virtualsize=" + virtualSize;

            String actualContent = new String(java.nio.file.Files.readAllBytes(metaFile.toPath()));
            Assert.assertEquals(expectedContent, actualContent);
        } finally {
            File metaFile = new File(metaFileName);
            metaFile.delete();
        }
    }

    @Test
    public void testCleanupStagingNfs() throws Exception{

        NfsSecondaryStorageResource spyResource = resource;
        spyResource.logger = loggerMock;
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(spyResource).execute(any(DeleteCommand.class));
        TemplateObjectTO mockTemplate = Mockito.mock(TemplateObjectTO.class);

        spyResource.cleanupStagingNfs(mockTemplate);

        verify(loggerMock, times(1)).debug("Failed to clean up staging area:", exception);

    }

    private void performGetSnapshotFilepathForDeleteTest(String expected, String path, String name) {
        Assert.assertEquals("Incorrect resultant Snapshot delete path", expected, resource.getSnapshotFilepathForDelete(path, name));
    }

    @Test
    public void testGetSnapshotFilepathForDelete() {
        performGetSnapshotFilepathForDeleteTest("/snapshots/2/10/somename",
                "/snapshots/2/10/somename",
                "somename");
        performGetSnapshotFilepathForDeleteTest("/snapshots/2/10/diffName/*diffname*",
                "/snapshots/2/10/diffName",
                "diffname");
        performGetSnapshotFilepathForDeleteTest("/snapshots/2/10/*somename*",
                "/snapshots/2/10",
                "somename");
    }

    @Test
    public void testExecuteQuerySnapshotZoneCopyCommand() {
        final String dir = "/snapshots/2/10/abc";
        final String fileName = "abc";
        DataStoreTO store = Mockito.mock(DataStoreTO.class);
        SnapshotObjectTO object = Mockito.mock(SnapshotObjectTO.class);
        Mockito.when(object.getDataStore()).thenReturn(store);
        Mockito.when(object.getPath()).thenReturn(dir + File.separator + fileName);
        QuerySnapshotZoneCopyCommand cmd = Mockito.mock(QuerySnapshotZoneCopyCommand.class);
        Mockito.when(cmd.getSnapshot()).thenReturn(object);
        Path p1 = Mockito.mock(Path.class);
        Mockito.when(p1.getFileName()).thenReturn(p1);
        Mockito.when(p1.toString()).thenReturn(fileName + ".vmdk");
        Path p2 = Mockito.mock(Path.class);
        Mockito.when(p2.getFileName()).thenReturn(p2);
        Mockito.when(p2.toString()).thenReturn(fileName + ".ovf");
        Stream<Path> paths = Stream.of(p1, p2);
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class)) {
            files.when(() -> Files.list(Mockito.any(Path.class))).thenReturn(paths);
            files.when(() -> Files.isDirectory(Mockito.any(Path.class))).thenReturn(false);
            QuerySnapshotZoneCopyAnswer answer = (QuerySnapshotZoneCopyAnswer)(resource.execute(cmd));
            List<String> result = answer.getFiles();
            Assert.assertEquals(2, result.size());
            Assert.assertEquals(dir + File.separator + fileName + ".vmdk", result.get(0));
            Assert.assertEquals(dir + File.separator + fileName + ".ovf", result.get(1));
        }
    }

    private void prepareForValidatePostUploadRequestSignatureTests(MockedStatic<EncryptionUtil> encryptionUtilMock) {
        doReturn(PROTOCOL).when(resource).getUploadProtocol();
        doReturn(PSK).when(resource).getPostUploadPSK();
        encryptionUtilMock.when(() -> EncryptionUtil.generateSignature(Mockito.anyString(), Mockito.anyString())).thenReturn(COMPUTED_SIGNATURE);
        String fullUrl = String.format("%s://%s/upload/%s", PROTOCOL, HOSTNAME, UUID);
        String data = String.format("%s%s%s", METADATA, fullUrl, TIMEOUT);
        encryptionUtilMock.when(() -> EncryptionUtil.generateSignature(data, PSK)).thenReturn(EXPECTED_SIGNATURE);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenProtocolDiffers() {
        try (MockedStatic<EncryptionUtil> encryptionUtilMock = Mockito.mockStatic(EncryptionUtil.class)) {
            prepareForValidatePostUploadRequestSignatureTests(encryptionUtilMock);
            doReturn(NetUtils.HTTPS_PROTO).when(resource).getUploadProtocol();

            resource.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, UUID, METADATA, TIMEOUT);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenHostnameDiffers() {
        try (MockedStatic<EncryptionUtil> encryptionUtilMock = Mockito.mockStatic(EncryptionUtil.class)) {
            prepareForValidatePostUploadRequestSignatureTests(encryptionUtilMock);

            resource.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, "test", UUID, METADATA, TIMEOUT);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenUuidDiffers() {
        try (MockedStatic<EncryptionUtil> encryptionUtilMock = Mockito.mockStatic(EncryptionUtil.class)) {
            prepareForValidatePostUploadRequestSignatureTests(encryptionUtilMock);

            resource.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, "test", METADATA, TIMEOUT);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenMetadataDiffers() {
        try (MockedStatic<EncryptionUtil> encryptionUtilMock = Mockito.mockStatic(EncryptionUtil.class)) {
            prepareForValidatePostUploadRequestSignatureTests(encryptionUtilMock);

            resource.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, UUID, "test", TIMEOUT);
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenTimeoutDiffers() {
        try (MockedStatic<EncryptionUtil> encryptionUtilMock = Mockito.mockStatic(EncryptionUtil.class)) {
            prepareForValidatePostUploadRequestSignatureTests(encryptionUtilMock);

            resource.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, UUID, METADATA, "test");
        }
    }

    @Test
    public void validatePostUploadRequestSignatureTestSuccessWhenDataIsTheSame() {
        try (MockedStatic<EncryptionUtil> encryptionUtilMock = Mockito.mockStatic(EncryptionUtil.class)) {
            prepareForValidatePostUploadRequestSignatureTests(encryptionUtilMock);

            resource.validatePostUploadRequestSignature(EXPECTED_SIGNATURE, HOSTNAME, UUID, METADATA, TIMEOUT);
        }
    }

    @Test
    public void getUploadProtocolTestReturnHttpsWhenUseHttpsToUploadIsTrue() {
        doReturn(true).when(resource).useHttpsToUpload();

        String result = resource.getUploadProtocol();

        Assert.assertEquals(NetUtils.HTTPS_PROTO, result);
    }

    @Test
    public void getUploadProtocolTestReturnHttpWhenUseHttpsToUploadIsFalse() {
        doReturn(false).when(resource).useHttpsToUpload();

        String result = resource.getUploadProtocol();

        Assert.assertEquals(NetUtils.HTTP_PROTO, result);
    }

    @Test
    public void configureStorageNetworkSetsStorageNetworkWhenParamsContainValues() {
        Map<String, Object> params = new HashMap<>();
       String ip = "192.168.1.10";
       String netmask = "255.255.255.0";
       String gateway = "192.168.1.1";
       params.put("storageip", ip);
       params.put("storagenetmask", netmask);
       params.put("storagegateway", gateway);
       resource.configureStorageNetwork(params);
       Assert.assertEquals(ip, ReflectionTestUtils.getField(resource, "_storageIp"));
       Assert.assertEquals(netmask, ReflectionTestUtils.getField(resource, "_storageNetmask"));
       Assert.assertEquals(gateway, ReflectionTestUtils.getField(resource, "_storageGateway"));
    }

    @Test
    public void configureStorageNetworkUsesManagementNetworkWhenStorageIpIsNullAndInSystemVM() {
        Map<String, Object> params = new HashMap<>();
        resource._inSystemVM = true;
        String ip = "10.0.0.10";
        String netmask = "255.255.255.0";
        String gateway = "10.0.0.1";
        ReflectionTestUtils.setField(resource, "_eth1ip", ip);
        ReflectionTestUtils.setField(resource, "_eth1mask", netmask);
        ReflectionTestUtils.setField(resource, "_localgw", gateway);
        resource.configureStorageNetwork(params);
        Assert.assertEquals(ip, ReflectionTestUtils.getField(resource, "_storageIp"));
        Assert.assertEquals(netmask, ReflectionTestUtils.getField(resource, "_storageNetmask"));
        Assert.assertEquals(gateway, ReflectionTestUtils.getField(resource, "_storageGateway"));
    }

    @Test
    public void configureStorageNetworkDoesNotSetStorageNetworkWhenNotInSystemVMAndStorageIpIsNull() {
        Map<String, Object> params = new HashMap<>();
        resource._inSystemVM = false;
        resource.configureStorageNetwork(params);
        assertNull(ReflectionTestUtils.getField(resource, "_storageIp"));
        assertNull(ReflectionTestUtils.getField(resource, "_storageNetmask"));
        assertNull(ReflectionTestUtils.getField(resource, "_storageGateway"));
    }


    @Test
    public void deleteBackupTestSuccess() {
        doReturn(backupDeltaTOMock).when(deleteCommandMock).getData();
        doReturn(nfsMock).when(backupDeltaTOMock).getDataStore();
        doReturn("asd").when(nfsMock).getUrl();

        doReturn("fds").when(resource).getRootDir(any(), any());
        doReturn("path/to/delta").when(backupDeltaTOMock).getPath();
        doReturn(null).when(resource).deleteLocalFile(any());
        doReturn(null).when(resource).deleteScreenshot(any(), any(), any());

        Answer answer = resource.deleteBackup(deleteCommandMock);

        assertTrue(answer.getResult());
    }

    @Test
    public void deleteBackupTestDeleteDeltaFails() {
        doReturn(backupDeltaTOMock).when(deleteCommandMock).getData();
        doReturn(nfsMock).when(backupDeltaTOMock).getDataStore();
        doReturn("asd").when(nfsMock).getUrl();

        doReturn("fds").when(resource).getRootDir(any(), any());
        doReturn("path/to/delta").when(backupDeltaTOMock).getPath();
        doReturn("error").when(resource).deleteLocalFile(any());

        Answer answer = resource.deleteBackup(deleteCommandMock);

        assertFalse(answer.getResult());
    }

    @Test
    public void deleteBackupTestScreenshotFailure() {
        doReturn(backupDeltaTOMock).when(deleteCommandMock).getData();
        doReturn(nfsMock).when(backupDeltaTOMock).getDataStore();
        doReturn("asd").when(nfsMock).getUrl();

        doReturn("fds").when(resource).getRootDir(any(), any());
        doReturn("path/to/delta").when(backupDeltaTOMock).getPath();
        doReturn(null).when(resource).deleteLocalFile(any());

        BackupDeleteAnswer failureAnswer = new BackupDeleteAnswer(deleteCommandMock, false, "fail");
        doReturn(failureAnswer).when(resource).deleteScreenshot(any(), any(), any());

        Answer answer = resource.deleteBackup(deleteCommandMock);

        assertFalse(answer.getResult());
    }

    @Test
    public void deleteBackupTestDirectoryDeletionFails() {
        doReturn(backupDeltaTOMock).when(deleteCommandMock).getData();
        doReturn(nfsMock).when(backupDeltaTOMock).getDataStore();
        doReturn("asd").when(nfsMock).getUrl();

        doReturn("fds").when(resource).getRootDir(any(), any());
        doReturn("path/to/delta").when(backupDeltaTOMock).getPath();
        doReturn(null).when(resource).deleteLocalFile(any());
        doReturn(null).when(resource).deleteScreenshot(any(), any(), any());
        doReturn(false).when(resource).deleteEmptyDirectory(any());

        Answer answer = resource.deleteBackup(deleteCommandMock);

        assertFalse(answer.getResult());
    }

    @Test
    public void deleteScreenshotTestNullPath() {
        BackupDeleteAnswer answer = resource.deleteScreenshot(deleteCommandMock, null, "/root/");

        assertNull(answer);
    }

    @Test
    public void deleteScreenshotTestSuccess() {
        doReturn(null).when(resource).deleteLocalFile(any());

        BackupDeleteAnswer answer = resource.deleteScreenshot(deleteCommandMock, "path/to/file", "/root/");

        assertNull(answer);
    }

    @Test
    public void deleteScreenshotTestFailure() {
        doReturn("error").when(resource).deleteLocalFile(any());
        doReturn(backupDeltaTOMock).when(deleteCommandMock).getData();

        BackupDeleteAnswer answer = resource.deleteScreenshot(deleteCommandMock, "path/to/file", "/root/");

        assertNotNull(answer);
        assertFalse(answer.getResult());
    }

    @Test
    public void deleteScreenshotTestPathStartsWithSeparator() {
        doReturn(null).when(resource).deleteLocalFile(any());

        resource.deleteScreenshot(deleteCommandMock, File.separator + "path/to/file", "/root/");

        verify(resource).deleteLocalFile("/root/" + "path/to/file");
    }
}
