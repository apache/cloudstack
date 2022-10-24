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
package org.apache.cloudstack.utils.qemu;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class QemuImageOptionsTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        String imagePath = "/path/to/file";
        String secretName = "secretname";
        return Arrays.asList(new Object[][] {
            { null, imagePath, null, new String[]{ imagePath } },
            { QemuImg.PhysicalDiskFormat.QCOW2, imagePath, null, new String[]{"--image-opts",String.format("driver=qcow2,file.filename=%s", imagePath)} },
            { QemuImg.PhysicalDiskFormat.RAW, imagePath, secretName, new String[]{ imagePath } },
            { QemuImg.PhysicalDiskFormat.QCOW2, imagePath, secretName, new String[]{"--image-opts", String.format("driver=qcow2,encrypt.key-secret=%s,file.filename=%s", secretName, imagePath)} },
            { QemuImg.PhysicalDiskFormat.LUKS, imagePath, secretName, new String[]{"--image-opts", String.format("driver=luks,file.filename=%s,key-secret=%s", imagePath, secretName)} }
        });
    }

    public QemuImageOptionsTest(QemuImg.PhysicalDiskFormat format, String filePath, String secretName, String[] expected) {
        this.format = format;
        this.filePath = filePath;
        this.secretName = secretName;
        this.expected = expected;
    }

    private final QemuImg.PhysicalDiskFormat format;
    private final String filePath;
    private final String secretName;
    private final String[] expected;

    @Test
    public void qemuImageOptionsFileNameTest() {
        QemuImageOptions options = new QemuImageOptions(format, filePath, secretName);
        Assert.assertEquals(expected, options.toCommandFlag());
    }
}
