// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.qemu;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;

@Ignore
public class QemuImgFileTest {
    @Test
    public void testFileNameAtContructor() {
        String filename = "/tmp/test-image.qcow2";
        QemuImgFile file = new QemuImgFile(filename);
        assertEquals(file.getFileName(), filename);
    }

    @Test
    public void testFileNameAndSizeAtContructor() {
        long size = 1024;
        String filename = "/tmp/test-image.qcow2";
        QemuImgFile file = new QemuImgFile(filename, size);
        assertEquals(file.getFileName(), filename);
        assertEquals(file.getSize(), size);
    }

    @Test
    public void testFileNameAndSizeAndFormatAtContructor() {
        PhysicalDiskFormat format = PhysicalDiskFormat.RAW;
        long size = 1024;
        String filename = "/tmp/test-image.qcow2";
        QemuImgFile file = new QemuImgFile(filename, size, format);
        assertEquals(file.getFileName(), filename);
        assertEquals(file.getSize(), size);
        assertEquals(file.getFormat(), format);
    }

    @Test
    public void testFileNameAndFormatAtContructor() {
        PhysicalDiskFormat format = PhysicalDiskFormat.RAW;
        String filename = "/tmp/test-image.qcow2";
        QemuImgFile file = new QemuImgFile(filename, format);
        assertEquals(file.getFileName(), filename);
        assertEquals(file.getFormat(), format);
    }
}
