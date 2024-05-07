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

import org.apache.logging.log4j.Logger;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.EncryptionUtil;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.command.QuerySnapshotZoneCopyAnswer;
import org.apache.cloudstack.storage.command.QuerySnapshotZoneCopyCommand;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.DataStoreTO;

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

    @Test
    public void testSwiftWriteMetadataFile() throws Exception {
        String metaFileName = "test_metadata_file";
        try {
            String uniqueName = "test_unique_name";
            String filename = "test_filename";
            long size = 1024L;
            long virtualSize = 2048L;

            File metaFile = resource.swiftWriteMetadataFile(metaFileName, uniqueName, filename, size, virtualSize);

            Assert.assertTrue(metaFile.exists());
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

        NfsSecondaryStorageResource spyResource = spy(resource);
        spyResource.logger = loggerMock;
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(spyResource).execute(any(DeleteCommand.class));
        TemplateObjectTO mockTemplate = Mockito.mock(TemplateObjectTO.class);

        spyResource.cleanupStagingNfs(mockTemplate);

        Mockito.verify(loggerMock, times(1)).debug("Failed to clean up staging area:", exception);

    }

    private void performGetSnapshotFilepathForDeleteTest(String expected, String path, String name) {
        Assert.assertEquals("Incorrect resultant snapshot delete path", expected, resource.getSnapshotFilepathForDelete(path, name));
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
        Mockito.doReturn(PROTOCOL).when(resource).getUploadProtocol();
        Mockito.doReturn(PSK).when(resource).getPostUploadPSK();
        encryptionUtilMock.when(() -> EncryptionUtil.generateSignature(Mockito.anyString(), Mockito.anyString())).thenReturn(COMPUTED_SIGNATURE);
        String fullUrl = String.format("%s://%s/upload/%s", PROTOCOL, HOSTNAME, UUID);
        String data = String.format("%s%s%s", METADATA, fullUrl, TIMEOUT);
        encryptionUtilMock.when(() -> EncryptionUtil.generateSignature(data, PSK)).thenReturn(EXPECTED_SIGNATURE);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validatePostUploadRequestSignatureTestThrowExceptionWhenProtocolDiffers() {
        try (MockedStatic<EncryptionUtil> encryptionUtilMock = Mockito.mockStatic(EncryptionUtil.class)) {
            prepareForValidatePostUploadRequestSignatureTests(encryptionUtilMock);
            Mockito.doReturn(NetUtils.HTTPS_PROTO).when(resource).getUploadProtocol();

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
        Mockito.doReturn(true).when(resource).useHttpsToUpload();

        String result = resource.getUploadProtocol();

        Assert.assertEquals(NetUtils.HTTPS_PROTO, result);
    }

    @Test
    public void getUploadProtocolTestReturnHttpWhenUseHttpsToUploadIsFalse() {
        Mockito.doReturn(false).when(resource).useHttpsToUpload();

        String result = resource.getUploadProtocol();

        Assert.assertEquals(NetUtils.HTTP_PROTO, result);
    }
}
