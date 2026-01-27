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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtSetupDirectDownloadCertificateCommandWrapperTest {

    @Spy
    LibvirtSetupDirectDownloadCertificateCommandWrapper wrapper = new LibvirtSetupDirectDownloadCertificateCommandWrapper();

    private String getTempFilepath() {
        return String.format("%s/%s.txt", System.getProperty("java.io.tmpdir"), UUID.randomUUID());
    }

    private void runTestCleanupTemporaryFileForRandomFileNames(String fileWithCommand, String filePath) {
        wrapper.cleanupTemporaryFile(fileWithCommand);
        File f = new File(filePath);
        if(f.exists() && !f.isDirectory()) {
            Assert.fail(String.format("Command injection working for fileWithCommand: %s", fileWithCommand));
        }
    }

    @Test
    public void testCleanupTemporaryFileForRandomFileNames() {
        List<String> commandVariants = List.of(
                "';touch %s'",
                ";touch %s",
                "&& touch %s",
                "|| touch %s",
                "%s");
        for (String cmd : commandVariants) {
            String filePath = getTempFilepath();
            String arg = String.format(cmd, filePath);
            runTestCleanupTemporaryFileForRandomFileNames(arg, filePath);
        }
    }

    private String createTempFile() {
        String filePath = getTempFilepath();
        Path path = Paths.get(getTempFilepath());
        try {
            if (Files.notExists(path)) {
                Files.createFile(path);
            }
        } catch (IOException e) {
            Assert.fail(String.format("Error while creating file: %s due to %s", filePath, e.getMessage()));
        }
        return filePath;
    }

    @Test
    public void testCleanupTemporaryFileValid() {
        String filePath = createTempFile();
        wrapper.cleanupTemporaryFile(filePath);
        File f = new File(filePath);
        if(f.exists() && !f.isDirectory()) {
            Assert.fail(String.format("Command injection working for fileWithCommand: %s", filePath));
        }
    }
}
