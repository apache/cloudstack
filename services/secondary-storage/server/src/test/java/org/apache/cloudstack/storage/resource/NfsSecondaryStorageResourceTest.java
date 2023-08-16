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
import org.junit.Before;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.File;

import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class NfsSecondaryStorageResourceTest {

    @Spy
    private NfsSecondaryStorageResource resource;

    @Mock
    private Logger loggerMock;

    @Before
    public void setUp() {
        resource = new NfsSecondaryStorageResource();
    }

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
}
