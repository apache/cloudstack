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
package org.apache.cloudstack.storage.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.storage.DeleteEntityDownloadURLCommand;
import com.cloud.utils.FileUtil;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class UploadManagerImplTest {

    @Spy
    @InjectMocks
    UploadManagerImpl uploadManager;

    MockedStatic<FileUtil> fileUtilMock;

    @Before
    public void setup() {
        fileUtilMock = mockStatic(FileUtil.class, Mockito.CALLS_REAL_METHODS);
        fileUtilMock.when(() -> FileUtil.deleteRecursively(any(Path.class))).thenReturn(true);
    }

    @After
    public void tearDown() {
        fileUtilMock.close();
    }

    @Test
    public void doesNotDeleteWhenLinkPathIsEmpty() {
        String emptyLinkPath = "";
        uploadManager.deleteEntitySymlinkRootDirectoryIfNeeded(mock(DeleteEntityDownloadURLCommand.class), emptyLinkPath);
        fileUtilMock.verify(() -> FileUtil.deleteRecursively(any(Path.class)), never());
    }

    @Test
    public void doesNotDeleteWhenRootDirIsNotUuid() {
        String invalidLinkPath = "invalidRootDir/file";
        uploadManager.deleteEntitySymlinkRootDirectoryIfNeeded(mock(DeleteEntityDownloadURLCommand.class), invalidLinkPath);
        fileUtilMock.verify(() -> FileUtil.deleteRecursively(any(Path.class)), never());
    }

    @Test
    public void deletesSymlinkRootDirectoryWhenValidUuid() {
        String validLinkPath = "123e4567-e89b-12d3-a456-426614174000/file";
        uploadManager.deleteEntitySymlinkRootDirectoryIfNeeded(mock(DeleteEntityDownloadURLCommand.class), validLinkPath);
        fileUtilMock.verify(() -> FileUtil.deleteRecursively(any(Path.class)), times(1));
    }

    @Test
    public void deletesSymlinkRootDirectoryWhenNoFile() {
        String validLinkPath = "123e4567-e89b-12d3-a456-426614174000";
        uploadManager.deleteEntitySymlinkRootDirectoryIfNeeded(mock(DeleteEntityDownloadURLCommand.class), validLinkPath);
        fileUtilMock.verify(() -> FileUtil.deleteRecursively(any(Path.class)), times(1));
    }

    @Test
    public void baseExtractDirectoryIsNotReadyWhenMissing() {
        assertFalse(uploadManager.isBaseExtractDirectoryReady(Path.of("/nonexistent/cloudstack/userdata/")));
    }

    @Test
    public void baseExtractDirectoryIsNotReadyWhenNotOwnedByApacheUser() throws Exception {
        Path dir = Files.createTempDirectory("userdata");
        try {
            assertFalse(uploadManager.isBaseExtractDirectoryReady(dir));
        } finally {
            Files.delete(dir);
        }
    }

    @Test
    public void ensureBaseExtractDirectorySkipsInstallWhenReady() {
        doReturn(true).when(uploadManager).isBaseExtractDirectoryReady(any(Path.class));
        try (MockedConstruction<Script> scripts = mockConstruction(Script.class)) {
            assertNull(uploadManager.ensureBaseExtractDirectory());
            Assert.assertTrue(scripts.constructed().isEmpty());
        }
    }

    @Test
    public void ensureBaseExtractDirectoryInstallsWhenNotReady() {
        doReturn(false).when(uploadManager).isBaseExtractDirectoryReady(any(Path.class));
        try (MockedConstruction<Script> scripts = mockConstruction(Script.class,
                (script, context) -> Mockito.when(script.execute()).thenReturn("Permission denied"))) {
            Assert.assertEquals("Permission denied", uploadManager.ensureBaseExtractDirectory());
            Assert.assertEquals(1, scripts.constructed().size());
            Mockito.verify(scripts.constructed().get(0)).add("-d", "-o", UploadManagerImpl.APACHE_USER, "-g", UploadManagerImpl.APACHE_USER,
                    "-m", "755", UploadManagerImpl.BASE_EXTRACT_PATH);
        }
    }
}
