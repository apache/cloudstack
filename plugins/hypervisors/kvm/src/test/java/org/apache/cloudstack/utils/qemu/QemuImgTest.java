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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.commons.collections.MapUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class QemuImgTest {

    @BeforeClass
    public static void setUp() {
        Assume.assumeTrue("qemu-img not found", Script.runSimpleBashScript("command -v qemu-img") != null);
        boolean libVirtAvailable = false;
        try {
            Connect conn = new Connect("qemu:///system", false);
            conn.getVersion();
            libVirtAvailable = true;
        } catch (LibvirtException ignored) {}
        Assume.assumeTrue("libvirt not available", libVirtAvailable);
    }

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

        long size = 10 * 1024 * 1024L;
        QemuImgFile file = new QemuImgFile(filename, size, PhysicalDiskFormat.QCOW2);
        String preallocation = "metadata";
        Map<String, String> options = new HashMap<String, String>();

        options.put("preallocation", preallocation);

        QemuImg qemu = new QemuImg(0);
        qemu.create(file, options);

        String allocatedSize = Script.runSimpleBashScript(String.format("ls -alhs %s | awk '{print $1}'", filename));
        String declaredSize  = Script.runSimpleBashScript(String.format("ls -alhs %s | awk '{print $6}'", filename));

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
            qemu.resize(file, endSize, null);
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
            qemu.resize(file, increment, true, null);
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

    // This test is failing and needs changes in QemuImg.resize to support shrinking images with delta sizes.
    // Earlier whole test suite was ignored, now only this test is ignored to allow other tests to run.
    @Ignore
    @Test
    public void testCreateAndResizeDeltaNegative() throws QemuImgException, LibvirtException {
        String filename = "/tmp/" + UUID.randomUUID() + ".qcow2";

        long startSize = 81920;
        long increment = -40960;
        QemuImgFile file = new QemuImgFile(filename, startSize, PhysicalDiskFormat.RAW);

        try {
            QemuImg qemu = new QemuImg(0);
            qemu.create(file);
            qemu.resize(file, increment, true, null);
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
            qemu.resize(file, endSize, null);
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
        qemu.resize(file, 0, null);

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
            qemu.checkAndRepair(file, new QemuImageOptions(Collections.emptyMap()), Collections.emptyList(), null);
        } catch (QemuImgException e) {
            fail(e.getMessage());
        }

        File f = new File(filename);
        f.delete();
    }

    @Test
    public void addScriptOptionsFromMapAddsValidOptions() throws LibvirtException, QemuImgException {
        Script script = Mockito.mock(Script.class);
        Map<String, String> options = new HashMap<>();
        options.put("key1", "value1");
        options.put("key2", "value2");

        QemuImg qemu = new QemuImg(0);
        qemu.addScriptOptionsFromMap(options, script);

        Mockito.verify(script, Mockito.times(1)).add("-o");
        Mockito.verify(script, Mockito.times(1)).add("key1=value1,key2=value2");
    }

    @Test
    public void addScriptOptionsFromMapHandlesEmptyOptions() throws LibvirtException, QemuImgException {
        Script script = Mockito.mock(Script.class);
        Map<String, String> options = new HashMap<>();

        QemuImg qemu = new QemuImg(0);
        qemu.addScriptOptionsFromMap(options, script);

        Mockito.verify(script, Mockito.never()).add(Mockito.anyString());
    }

    @Test
    public void addScriptOptionsFromMapHandlesNullOptions() throws LibvirtException, QemuImgException {
        Script script = Mockito.mock(Script.class);

        QemuImg qemu = new QemuImg(0);
        qemu.addScriptOptionsFromMap(null, script);

        Mockito.verify(script, Mockito.never()).add(Mockito.anyString());
    }

    @Test
    public void addScriptOptionsFromMapHandlesTrailingComma() throws LibvirtException, QemuImgException {
        Script script = Mockito.mock(Script.class);
        Map<String, String> options = new HashMap<>();
        options.put("key1", "value1");

        QemuImg qemu = new QemuImg(0);
        qemu.addScriptOptionsFromMap(options, script);

        Mockito.verify(script, Mockito.times(1)).add("-o");
        Mockito.verify(script, Mockito.times(1)).add("key1=value1");
    }

    @Test
    public void getResizeOptionsFromConvertOptionsReturnsNullForEmptyOptions() throws LibvirtException, QemuImgException {
        QemuImg qemuImg = new QemuImg(0);
        Map<String, String> options = new HashMap<>();

        Map<String, String> result = qemuImg.getResizeOptionsFromConvertOptions(options);

        Assert.assertNull(result);
    }

    @Test
    public void getResizeOptionsFromConvertOptionsReturnsNullForNullOptions() throws LibvirtException, QemuImgException {
        QemuImg qemuImg = new QemuImg(0);

        Map<String, String> result = qemuImg.getResizeOptionsFromConvertOptions(null);

        Assert.assertNull(result);
    }

    @Test
    public void getResizeOptionsFromConvertOptionsReturnsPreallocationOption() throws LibvirtException, QemuImgException {
        QemuImg qemuImg = new QemuImg(0);
        Map<String, String> options = new HashMap<>();
        options.put(QemuImg.PREALLOCATION, "metadata");

        Map<String, String> result = qemuImg.getResizeOptionsFromConvertOptions(options);

        Assert.assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("metadata", result.get(QemuImg.PREALLOCATION));
    }

    @Test
    public void getResizeOptionsFromConvertOptionsIgnoresUnrelatedOptions() throws LibvirtException, QemuImgException {
        QemuImg qemuImg = new QemuImg(0);
        Map<String, String> options = new HashMap<>();
        options.put("unrelatedKey", "unrelatedValue");

        Map<String, String> result = qemuImg.getResizeOptionsFromConvertOptions(options);

        Assert.assertTrue(MapUtils.isEmpty(result));
    }

    @Test
    public void getResizeOptionsFromConvertOptionsHandlesMixedOptions() throws LibvirtException, QemuImgException {
        QemuImg qemuImg = new QemuImg(0);
        Map<String, String> options = new HashMap<>();
        options.put(QemuImg.PREALLOCATION, "full");
        options.put("unrelatedKey", "unrelatedValue");

        Map<String, String> result = qemuImg.getResizeOptionsFromConvertOptions(options);

        Assert.assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("full", result.get(QemuImg.PREALLOCATION));
    }

    @Test
    public void addScriptResizeOptionsFromMapAddsPreallocationOption() throws LibvirtException, QemuImgException {
        Script script = Mockito.mock(Script.class);
        Map<String, String> options = new HashMap<>();
        options.put(QemuImg.PREALLOCATION, "metadata");

        QemuImg qemuImg = new QemuImg(0);
        qemuImg.addScriptResizeOptionsFromMap(options, script);

        Mockito.verify(script, Mockito.times(1)).add("--preallocation=metadata");
        Mockito.verify(script, Mockito.never()).add("-o");
        assertTrue(options.isEmpty());
    }

    @Test
    public void addScriptResizeOptionsFromMapHandlesEmptyOptions() throws LibvirtException, QemuImgException {
        Script script = Mockito.mock(Script.class);
        Map<String, String> options = new HashMap<>();

        QemuImg qemuImg = new QemuImg(0);
        qemuImg.addScriptResizeOptionsFromMap(options, script);

        Mockito.verify(script, Mockito.never()).add(Mockito.anyString());
    }

    @Test
    public void addScriptResizeOptionsFromMapHandlesNullOptions() throws LibvirtException, QemuImgException {
        Script script = Mockito.mock(Script.class);

        QemuImg qemuImg = new QemuImg(0);
        qemuImg.addScriptResizeOptionsFromMap(null, script);

        Mockito.verify(script, Mockito.never()).add(Mockito.anyString());
    }

    @Test
    public void addScriptResizeOptionsFromMapHandlesMixedOptions() throws LibvirtException, QemuImgException {
        Script script = Mockito.mock(Script.class);
        Map<String, String> options = new HashMap<>();
        options.put(QemuImg.PREALLOCATION, "full");
        options.put("key", "value");

        QemuImg qemuImg = new QemuImg(0);
        qemuImg.addScriptResizeOptionsFromMap(options, script);

        Mockito.verify(script, Mockito.times(1)).add("--preallocation=full");
        Mockito.verify(script, Mockito.times(1)).add("-o");
        Mockito.verify(script, Mockito.times(1)).add("key=value");
        assertFalse(options.containsKey(QemuImg.PREALLOCATION));
    }
}
