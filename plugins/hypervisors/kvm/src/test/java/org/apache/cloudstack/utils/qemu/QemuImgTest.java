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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import com.cloud.utils.script.Script;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.libvirt.LibvirtException;

@Ignore
public class QemuImgTest {

    @Test
    public void testCreateAndInfo() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        /* 10TB virtual_size */
        long size = 10995116277760l;
        QemuImgFile file = new QemuImgFile(filename, size, PhysicalDiskFormat.QCOW2);

        QemuImg qemu = new QemuImg(0);
        qemu.create(file);
        Map<String, String> info = qemu.info(file);

        if (info == null) {
            fail("We didn't get any information back from qemu-img");
        }

        Long infoSize = Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
        assertEquals(Long.valueOf(size), Long.valueOf(infoSize));

        String infoPath = info.get(QemuImg.IMAGE);
        assertEquals(filename, infoPath);

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateAndInfoWithOptions() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        /* 10TB virtual_size */
        long size = 10995116277760l;
        QemuImgFile file = new QemuImgFile(filename, size, PhysicalDiskFormat.QCOW2);
        String clusterSize = "131072";
        Map<String, String> options = new HashMap<String, String>();

        options.put("cluster_size", clusterSize);

        QemuImg qemu = new QemuImg(0);
        qemu.create(file, options);
        Map<String, String> info = qemu.info(file);

        Long infoSize = Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
        assertEquals(Long.valueOf(size), Long.valueOf(infoSize));

        String infoPath = info.get(QemuImg.IMAGE);
        assertEquals(filename, infoPath);

        String infoClusterSize = info.get(QemuImg.CLUSTER_SIZE);
        assertEquals(clusterSize, infoClusterSize);

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateWithSecretObject() throws QemuImgException, LibvirtException {
        Path testFile = Paths.get("/tmp/", UUID.randomUUID().toString()).normalize().toAbsolutePath();
        long size = 1<<30; // 1 Gi

        Map<QemuObject.ObjectParameter, String> objectParams = new HashMap<>();
        objectParams.put(QemuObject.ObjectParameter.ID, "sec0");
        objectParams.put(QemuObject.ObjectParameter.DATA, UUID.randomUUID().toString());

        Map<String, String> options = new HashMap<String, String>();

        options.put(QemuImg.ENCRYPT_FORMAT, "luks");
        options.put(QemuImg.ENCRYPT_KEY_SECRET, "sec0");

        List<QemuObject> qObjects = new ArrayList<>();
        qObjects.add(new QemuObject(QemuObject.ObjectType.SECRET, objectParams));

        QemuImgFile file = new QemuImgFile(testFile.toString(), size, PhysicalDiskFormat.QCOW2);
        QemuImg qemu = new QemuImg(0);
        qemu.create(file, null, options, qObjects);

        Map<String, String> info = qemu.info(file);
        assertEquals("yes", info.get("encrypted"));

        assertTrue(testFile.toFile().delete());
    }

    @Test
    public void testCreateSparseVolume() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        /* 10TB virtual_size */
        long size = 10995116277760l;
        QemuImgFile file = new QemuImgFile(filename, size, PhysicalDiskFormat.QCOW2);
        String preallocation = "metadata";
        Map<String, String> options = new HashMap<String, String>();

        options.put("preallocation", preallocation);

        QemuImg qemu = new QemuImg(0);
        qemu.create(file, options);

        String allocatedSize = Script.runSimpleBashScript(String.format("ls -alhs %s | awk '{print $1}'", file));
        String declaredSize  = Script.runSimpleBashScript(String.format("ls -alhs %s | awk '{print $6}'", file));

        assertFalse(allocatedSize.equals(declaredSize));

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateAndResize() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 20480;
        long endSize = 40960;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.QCOW2);

        try {
            QemuImg qemu = new QemuImg(0);
            qemu.create(file);
            qemu.resize(file, endSize);
            Map<String, String> info = qemu.info(file);

            if (info == null) {
                fail("We didn't get any information back from qemu-img");
            }

            Long infoSize = Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
            assertEquals(Long.valueOf(endSize), Long.valueOf(infoSize));
        } catch (QemuImgException e) {
            fail(e.getMessage());
        }

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateAndResizeDeltaPositive() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 20480;
        long increment = 20480;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.RAW);

        try {
            QemuImg qemu = new QemuImg(0);
            qemu.create(file);
            qemu.resize(file, increment, true);
            Map<String, String> info = qemu.info(file);

            if (info == null) {
                fail("We didn't get any information back from qemu-img");
            }

            Long infoSize = Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
            assertEquals(Long.valueOf(startSize + increment), Long.valueOf(infoSize));
        } catch (QemuImgException e) {
            fail(e.getMessage());
        }

        File f = new File(filename);
        f.delete();
    }

    @Test
    public void testCreateAndResizeDeltaNegative() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 81920;
        long increment = -40960;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.RAW);

        try {
            QemuImg qemu = new QemuImg(0);
            qemu.create(file);
            qemu.resize(file, increment, true);
            Map<String, String> info = qemu.info(file);

            if (info == null) {
                fail("We didn't get any information back from qemu-img");
            }

            Long infoSize = Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
            assertEquals(Long.valueOf(startSize + increment), Long.valueOf(infoSize));
        } catch (QemuImgException e) {
            fail(e.getMessage());
        }

        File f = new File(filename);
        f.delete();
    }

    @Test(expected = QemuImgException.class)
    public void testCreateAndResizeFail() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 20480;

        /* Negative new size, expect failure */
        long endSize = -1;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.QCOW2);

        QemuImg qemu = new QemuImg(0);
        try {
            qemu.create(file);
            qemu.resize(file, endSize);
        } finally {
            File f = new File(filename);
            f.delete();
        }
    }

    @Test(expected = QemuImgException.class)
    public void testCreateAndResizeZero() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 20480;
        QemuImgFile file = new QemuImgFile(filename, 20480, PhysicalDiskFormat.QCOW2);

        QemuImg qemu = new QemuImg(0);
        qemu.create(file);
        qemu.resize(file, 0);

        File f = new File(filename);
        f.delete();

    }

    @Test
    public void testCreateWithBackingFile() throws QemuImgException, LibvirtException {
        String firstFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";
        String secondFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";

        QemuImgFile firstFile = new QemuImgFile(firstFileName, 20480, PhysicalDiskFormat.QCOW2);
        QemuImgFile secondFile = new QemuImgFile(secondFileName, PhysicalDiskFormat.QCOW2);

        QemuImg qemu = new QemuImg(0);
        qemu.create(firstFile);
        qemu.create(secondFile, firstFile);

        Map<String, String> info = qemu.info(secondFile);
        if (info == null) {
            fail("We didn't get any information back from qemu-img");
        }

        String backingFile = info.get(QemuImg.BACKING_FILE);
        if (backingFile == null) {
            fail("The second file does not have a property backing_file! Create failed?");
        }
    }

    @Test
    public void testConvertBasic() throws QemuImgException, LibvirtException {
        long srcSize = 20480;
        String srcFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";
        String destFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";

        QemuImgFile srcFile = new QemuImgFile(srcFileName, srcSize);
        QemuImgFile destFile = new QemuImgFile(destFileName);

        QemuImg qemu = new QemuImg(0);
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
    public void testConvertAdvanced() throws QemuImgException, LibvirtException {
        long srcSize = 4019200;
        String srcFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";
        String destFileName = "/tmp/" + UUID.randomUUID() + ".qcow2";
        PhysicalDiskFormat srcFormat = PhysicalDiskFormat.RAW;
        PhysicalDiskFormat destFormat = PhysicalDiskFormat.QCOW2;

        QemuImgFile srcFile = new QemuImgFile(srcFileName, srcSize, srcFormat);
        QemuImgFile destFile = new QemuImgFile(destFileName, destFormat);

        QemuImg qemu = new QemuImg(0);
        qemu.create(srcFile);
        qemu.convert(srcFile, destFile);

        Map<String, String> info = qemu.info(destFile);

        PhysicalDiskFormat infoFormat = PhysicalDiskFormat.valueOf(info.get(QemuImg.FILE_FORMAT).toUpperCase());
        assertEquals(destFormat, infoFormat);

        Long infoSize = Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
        assertEquals(Long.valueOf(srcSize), Long.valueOf(infoSize));

        File sf = new File(srcFileName);
        sf.delete();

        File df = new File(destFileName);
        df.delete();

    }

    @Test
    public void testHelpSupportsImageFormat() throws QemuImgException, LibvirtException {
        String partialHelp = "Parameters to dd subcommand:\n" +
                "  'bs=BYTES' read and write up to BYTES bytes at a time (default: 512)\n" +
                "  'count=N' copy only N input blocks\n" +
                "  'if=FILE' read from FILE\n" +
                "  'of=FILE' write to FILE\n" +
                "  'skip=N' skip N bs-sized blocks at the start of input\n" +
                "\n" +
                "Supported formats: cloop copy-on-read file ftp ftps host_cdrom host_device https iser luks nbd nvme parallels qcow qcow2 qed quorum raw rbd ssh throttle vdi vhdx vmdk vpc vvfat\n" +
                "\n" +
                "See <https://qemu.org/contribute/report-a-bug> for how to report bugs.\n" +
                "More information on the QEMU project at <https://qemu.org>.";
        Assert.assertTrue("should support luks", QemuImg.helpSupportsImageFormat(partialHelp, PhysicalDiskFormat.LUKS));
        Assert.assertTrue("should support qcow2", QemuImg.helpSupportsImageFormat(partialHelp, PhysicalDiskFormat.QCOW2));
        Assert.assertFalse("should not support http", QemuImg.helpSupportsImageFormat(partialHelp, PhysicalDiskFormat.SHEEPDOG));
    }

    @Test
    public void testCheckAndRepair() throws LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        QemuImgFile file = new QemuImgFile(filename);

        try {
            QemuImg qemu = new QemuImg(0);
            qemu.checkAndRepair(file, null, null, null);
        } catch (QemuImgException e) {
            fail(e.getMessage());
        }

        File f = new File(filename);
        f.delete();
    }
}
