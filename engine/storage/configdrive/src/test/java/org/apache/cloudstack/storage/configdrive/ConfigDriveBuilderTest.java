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

package org.apache.cloudstack.storage.configdrive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

public class ConfigDriveBuilderTest {

    @Test
    public void testConfigDriveIsoPath() throws IOException {
        Assert.assertEquals(ConfigDrive.createConfigDrivePath("i-x-y"), "configdrive/i-x-y/configdrive.iso");
    }

    @Test
    public void testConfigDriveBuild() throws IOException {
        List<String[]> actualVmData = Arrays.asList(
                new String[]{"userdata", "user_data", "c29tZSB1c2VyIGRhdGE="},
                new String[]{"metadata", "service-offering", "offering"},
                new String[]{"metadata", "availability-zone", "zone1"},
                new String[]{"metadata", "local-hostname", "hostname"},
                new String[]{"metadata", "local-ipv4", "192.168.111.111"},
                new String[]{"metadata", "public-hostname", "7.7.7.7"},
                new String[]{"metadata", "public-ipv4", "7.7.7.7"},
                new String[]{"metadata", "vm-id", "uuid"},
                new String[]{"metadata", "instance-id", "i-x-y"},
                new String[]{"metadata", "public-keys", "ssh-rsa some-key"},
                new String[]{"metadata", "cloud-identifier", String.format("CloudStack-{%s}", "uuid")},
                new String[]{"password", "vm_password", "password123"}
        );

        final Path tempDir = Files.createTempDirectory(ConfigDrive.CONFIGDRIVEDIR);
        final String isoData = ConfigDriveBuilder.buildConfigDrive(actualVmData, "i-x-y.iso", "config-2");
        final File isoFile = ConfigDriveBuilder.base64StringToFile(isoData, tempDir.toAbsolutePath().toString(), ConfigDrive.CONFIGDRIVEFILENAME);

        Assert.assertTrue(isoFile.exists());
        Assert.assertTrue(isoFile.isFile());
        Assert.assertTrue(isoFile.length() > 0L);

        FileUtils.deleteDirectory(tempDir.toFile());
    }
}