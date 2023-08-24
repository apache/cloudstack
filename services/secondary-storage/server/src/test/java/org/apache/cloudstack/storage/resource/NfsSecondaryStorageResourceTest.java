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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;

import org.apache.cloudstack.storage.command.DeleteCommand;
import org.apache.cloudstack.storage.to.TemplateObjectTO;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.test.TestAppender;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.xml.*", "org.xml.*"})
public class NfsSecondaryStorageResourceTest {

    private NfsSecondaryStorageResource resource;

    @Before
    public void setUp() {
        resource = new NfsSecondaryStorageResource();
    }

    @Test
    @PrepareForTest(NfsSecondaryStorageResource.class)
    public void testSwiftWriteMetadataFile() throws Exception {
        String filename = "testfile";
        try {
            String expected = "uniquename=test\nfilename=" + filename + "\nsize=100\nvirtualsize=1000";

            StringWriter stringWriter = new StringWriter();
            BufferedWriter bufferWriter = new BufferedWriter(stringWriter);
            PowerMockito.whenNew(BufferedWriter.class).withArguments(any(FileWriter.class)).thenReturn(bufferWriter);

            resource.swiftWriteMetadataFile(filename, "test", filename, 100, 1000);

            Assert.assertEquals(expected, stringWriter.toString());
        } finally {
            File remnance = new File(filename);
            remnance.delete();
        }
    }

    @Test
    public void testCleanupStagingNfs() throws Exception{

        NfsSecondaryStorageResource spyResource = spy(resource);
        RuntimeException exception = new RuntimeException();
        doThrow(exception).when(spyResource).execute(any(DeleteCommand.class));
        TemplateObjectTO mockTemplate = Mockito.mock(TemplateObjectTO.class);

        TestAppender.TestAppenderBuilder appenderBuilder = new TestAppender.TestAppenderBuilder();
        appenderBuilder.addExpectedPattern(Level.DEBUG, "Failed to clean up staging area:");
        TestAppender testLogAppender = appenderBuilder.build();
        TestAppender.safeAddAppender(NfsSecondaryStorageResource.s_logger, testLogAppender);

        spyResource.cleanupStagingNfs(mockTemplate);

        testLogAppender.assertMessagesLogged();

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
