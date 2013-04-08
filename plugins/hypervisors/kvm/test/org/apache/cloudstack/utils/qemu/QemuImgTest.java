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

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.io.File;

@Ignore
public class QemuImgTest {

    @Test
    public void testCreateAndInfo() throws QemuImgException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        /* 10TB virtual_size */
        long size = 10995116277760l;
        QemuImgFile file = new QemuImgFile(filename, size, PhysicalDiskFormat.QCOW2);

        QemuImg qemu = new QemuImg();
        qemu.create(file);
        Map<String, String> info = qemu.info(file);

        if (info == null) {
            fail("We didn't get any information back from qemu-img");
        }

        Long infoSize = Long.parseLong(info.get(new String("virtual_size")));
        assertEquals(Long.valueOf(size), Long.valueOf(infoSize));

        String infoPath = info.get(new String("image"));
        assertEquals(filename, infoPath);

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateAndInfoWithOptions() throws QemuImgException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        /* 10TB virtual_size */
        long size = 10995116277760l;
        QemuImgFile file = new QemuImgFile(filename, size, PhysicalDiskFormat.QCOW2);
        String clusterSize = "131072";
        Map<String, String> options = new HashMap<String, String>();

        options.put("cluster_size", clusterSize);

        QemuImg qemu = new QemuImg();
        qemu.create(file, options);
        Map<String, String> info = qemu.info(file);

        Long infoSize = Long.parseLong(info.get(new String("virtual_size")));
        assertEquals(Long.valueOf(size), Long.valueOf(infoSize));

        String infoPath = info.get(new String("image"));
        assertEquals(filename, infoPath);

        String infoClusterSize = info.get(new String("cluster_size"));
        assertEquals(clusterSize, infoClusterSize);

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateAndResize() throws QemuImgException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 20480;
        long endSize = 40960;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.QCOW2);

        try {
            QemuImg qemu = new QemuImg();
            qemu.create(file);
            qemu.resize(file, endSize);
            Map<String, String> info = qemu.info(file);

            if (info == null) {
                fail("We didn't get any information back from qemu-img");
            }

            Long infoSize = Long.parseLong(info.get(new String("virtual_size")));
            assertEquals(Long.valueOf(endSize), Long.valueOf(infoSize));
        } catch (QemuImgException e) {
            fail(e.getMessage());
        }

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateAndResizeDeltaPositive() throws QemuImgException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 20480;
        long increment = 20480;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.RAW);

        try {
            QemuImg qemu = new QemuImg();
            qemu.create(file);
            qemu.resize(file, increment, true);
            Map<String, String> info = qemu.info(file);

            if (info == null) {
                fail("We didn't get any information back from qemu-img");
            }

            Long infoSize = Long.parseLong(info.get(new String("virtual_size")));
            assertEquals(Long.valueOf(startSize + increment), Long.valueOf(infoSize));
        } catch (QemuImgException e) {
            fail(e.getMessage());
        }

        File f = new File(filename);
        f.delete();
    }

    @Test
    public void testCreateAndResizeDeltaNegative() throws QemuImgException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 81920;
        long increment = -40960;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.RAW);

        try {
            QemuImg qemu = new QemuImg();
            qemu.create(file);
            qemu.resize(file, increment, true);
            Map<String, String> info = qemu.info(file);

            if (info == null) {
                fail("We didn't get any information back from qemu-img");
            }

            Long infoSize = Long.parseLong(info.get(new String("virtual_size")));
            assertEquals(Long.valueOf(startSize + increment), Long.valueOf(infoSize));
        } catch (QemuImgException e) {
            fail(e.getMessage());
        }

        File f = new File(filename);
        f.delete();
    }

    @Test(expected = QemuImgException.class)
    public void testCreateAndResizeFail() throws QemuImgException  {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 20480;

        /* Negative new size, expect failure */
        long endSize = -1;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.QCOW2);

        QemuImg qemu = new QemuImg();
        try {
            qemu.create(file);
            qemu.resize(file, endSize);
        } finally {
            File f = new File(filename);
            f.delete();
        }
    }

    @Test(expected = QemuImgException.class)
    public void testCreateAndResizeZero() throws QemuImgException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 20480;
        QemuImgFile file = new QemuImgFile(filename, 20480, PhysicalDiskFormat.QCOW2);

        QemuImg qemu = new QemuImg();
        qemu.create(file);
        qemu.resize(file, 0);

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateWithBackingFile() throws QemuImgException {
        String firstFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";
        String secondFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";

        QemuImgFile firstFile = new QemuImgFile(firstFileName, 20480, PhysicalDiskFormat.QCOW2);
        QemuImgFile secondFile = new QemuImgFile(secondFileName, PhysicalDiskFormat.QCOW2);

        QemuImg qemu = new QemuImg();
        qemu.create(firstFile);
        qemu.create(secondFile, firstFile);

        Map<String, String> info = qemu.info(secondFile);
        if (info == null) {
            fail("We didn't get any information back from qemu-img");
        }

        String backingFile = info.get(new String("backing_file"));
        if (backingFile == null) {
            fail("The second file does not have a property backing_file! Create failed?");
        }
    }

    @Test
    public void testConvertBasic() throws QemuImgException {
        long srcSize = 20480;
        String srcFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";
        String destFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";

        QemuImgFile srcFile = new QemuImgFile(srcFileName, srcSize);
        QemuImgFile destFile = new QemuImgFile(destFileName);

        QemuImg qemu = new QemuImg();
        qemu.create(srcFile);
        qemu.convert(srcFile, destFile);
        Map<String, String> info = qemu.info(destFile);
        if (info == null) {
            fail("We didn't get any information back from qemu-img");
        }

        File sf = new File(srcFileName);
        sf.delete();

        File df = new File(destFileName);
        df.delete();

    }

    @Test
    public void testConvertAdvanced() throws QemuImgException {
        long srcSize = 4019200;
        String srcFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";
        String destFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";
        PhysicalDiskFormat srcFormat = PhysicalDiskFormat.RAW;
        PhysicalDiskFormat destFormat = PhysicalDiskFormat.QCOW2;

        QemuImgFile srcFile = new QemuImgFile(srcFileName, srcSize, srcFormat);
        QemuImgFile destFile = new QemuImgFile(destFileName, destFormat);

        QemuImg qemu = new QemuImg();
        qemu.create(srcFile);
        qemu.convert(srcFile, destFile);

        Map<String, String> info = qemu.info(destFile);

        PhysicalDiskFormat infoFormat = PhysicalDiskFormat.valueOf(info.get(new String("file_format")).toUpperCase());
        assertEquals(destFormat, infoFormat);

        Long infoSize = Long.parseLong(info.get(new String("virtual_size")));
        assertEquals(Long.valueOf(srcSize), Long.valueOf(infoSize));

        File sf = new File(srcFileName);
        sf.delete();

        File df = new File(destFileName);
        df.delete();

    }
}