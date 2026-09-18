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
package org.apache.cloudstack.utils.qemu;

import com.cloud.agent.api.to.FilesystemInfoTO;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.command.browser.ListDataStoreObjectsAnswer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GuestfishClientTest {

    @Spy
    GuestfishClient guestfishClientSpy;

    private static final String statsDetailsDir = "st_dev: 2049\nst_ino: 2\nst_mode: 16877\nst_nlink: 22\nst_uid: 0\nst_gid: 0\nst_rdev: 0\nst_size: 4096\n" +
            "st_blksize: 4096\nst_blocks: 8\nst_atime_sec: 1779800014\nst_atime_nsec: 952000000\nst_mtime_sec: 1779802005\nst_mtime_nsec: 508000000\nst_ctime_sec: 1779802005\n" +
            "st_ctime_nsec: 508000000\nst_spare1: 0\nst_spare2: 0\nst_spare3: 0\nst_spare4: 0\nst_spare5: 0\nst_spare6: 0";

    private static final String statsDetailsFile = "st_dev: 2049\nst_ino: 2\nst_mode: 32768\nst_nlink: 22\nst_uid: 0\nst_gid: 0\nst_rdev: 0\nst_size: 4096\n" +
            "st_blksize: 4096\nst_blocks: 8\nst_atime_sec: 1779800014\nst_atime_nsec: 952000000\nst_mtime_sec: 1779802005\nst_mtime_nsec: 508000000\nst_ctime_sec: 1779802005\n" +
            "st_ctime_nsec: 508000000\nst_spare1: 0\nst_spare2: 0\nst_spare3: 0\nst_spare4: 0\nst_spare5: 0\nst_spare6: 0";

    private static final String statsDetailsSymlink = "st_dev: 2049\nst_ino: 2\nst_mode: 40960\nst_nlink: 22\nst_uid: 0\nst_gid: 0\nst_rdev: 0\nst_size: 4096\n" +
            "st_blksize: 4096\nst_blocks: 8\nst_atime_sec: 1779800014\nst_atime_nsec: 952000000\nst_mtime_sec: 1779802005\nst_mtime_nsec: 508000000\nst_ctime_sec: 1779802005\n" +
            "st_ctime_nsec: 508000000\nst_spare1: 0\nst_spare2: 0\nst_spare3: 0\nst_spare4: 0\nst_spare5: 0\nst_spare6: 0";

    private static final String statsDetailsUnknownType = "st_dev: 2049\nst_ino: 2\nst_mode: 60\nst_nlink: 22\nst_uid: 0\nst_gid: 0\nst_rdev: 0\nst_size: 4096\n" +
            "st_blksize: 4096\nst_blocks: 8\nst_atime_sec: 1779800014\nst_atime_nsec: 952000000\nst_mtime_sec: 1779802005\nst_mtime_nsec: 508000000\nst_ctime_sec: 1779802005\n" +
            "st_ctime_nsec: 508000000\nst_spare1: 0\nst_spare2: 0\nst_spare3: 0\nst_spare4: 0\nst_spare5: 0\nst_spare6: 0";

    @Test
    public void parseLongTestNull() {
        long result = guestfishClientSpy.parseLong(null);
        assertEquals(0, result);
    }

    @Test
    public void parseLongTestEmpty() {
        long result = guestfishClientSpy.parseLong("");
        assertEquals(0, result);
    }

    @Test
    public void parseLongTestNaN() {
        long result = guestfishClientSpy.parseLong("acs");
        assertEquals(0, result);
    }

    @Test
    public void parseLongTestActualNumber() {
        long result = guestfishClientSpy.parseLong("123");
        assertEquals(123, result);
    }

    @Test
    public void parseKeyValueOutputTestEmptyString() {
        String stringToParse = "";
        Map<String, String> map = guestfishClientSpy.parseKeyValueOutput(stringToParse);
        assertEquals(Map.of(), map);
    }

    @Test
    public void parseKeyValueOutputTestUnexpectedString() {
        String stringToParse = "a=n\nc=d\n";
        Map<String, String> map = guestfishClientSpy.parseKeyValueOutput(stringToParse);
        assertEquals(Map.of(), map);
    }

    @Test
    public void parseKeyValueOutputTestWithExpectedInput() {
        Map<String, String> map = guestfishClientSpy.parseKeyValueOutput(statsDetailsDir);
        assertEquals(2049, Long.parseLong(map.get("st_dev")));
        assertEquals(1779800014, Long.parseLong(map.get("st_atime_sec")));
        assertEquals(4096, Long.parseLong(map.get("st_size")));
        assertEquals(0, Long.parseLong(map.get("st_spare1")));
        assertEquals(16877, Long.parseLong(map.get("st_mode")));
    }

    @Test
    public void getCanonicalPathTestFileDoesNotExist() {
        String input = "/bin";
        doReturn("/log").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.READLINK, input);
        doReturn("false").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.EXISTS, "/log");

        String result = guestfishClientSpy.getCanonicalPath(input);

        assertNull(result);
    }

    @Test
    public void getCanonicalPathTestNtfsStyleCanonicalPath() {
        String input = "/bin";
        doReturn("/sysroot/log").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.READLINK, input);
        doReturn("true").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.EXISTS, "/log");

        String result = guestfishClientSpy.getCanonicalPath(input);

        assertEquals("/log", result);
    }

    @Test
    public void getCanonicalPathTestRelativePathTwoDots() {
        String input = "/bin/test";
        doReturn("../log").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.READLINK, input);
        doReturn("true").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.EXISTS, "/log");

        String result = guestfishClientSpy.getCanonicalPath(input);

        assertEquals("/log", result);
    }

    @Test
    public void getCanonicalPathTestRelativePathSingleDot() {
        String input = "/bin/test";
        doReturn("./log").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.READLINK, input);
        doReturn("true").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.EXISTS, "/bin/log");;

        String result = guestfishClientSpy.getCanonicalPath(input);

        assertEquals("/bin/log", result);
    }

    @Test
    public void getCanonicalPathTestRelativePath() {
        String input = "/bin/test";
        doReturn("log").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.READLINK, input);
        doReturn("true").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.EXISTS, "/bin/log");

        String result = guestfishClientSpy.getCanonicalPath(input);

        assertEquals("/bin/log", result);
    }

    @Test
    public void addFilesToListsTestNormalFile() {
        List<String> names = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> canonicalPaths = new ArrayList<>();
        List<Boolean> isDirs = new ArrayList<>();
        List<Boolean> isSymlinks = new ArrayList<>();
        List<Long> sizes = new ArrayList<>();
        List<Long> modifiedList = new ArrayList<>();
        String directory = "/";
        String details = "[0] = {" + statsDetailsDir + "}";
        List<String> fileList = List.of("boot");

        guestfishClientSpy.addFilesToLists(directory, details, fileList, names, paths, canonicalPaths, isDirs, isSymlinks, sizes, modifiedList);

        assertEquals("boot", names.get(0));
        assertEquals("/boot", paths.get(0));
        assertEquals("/boot", canonicalPaths.get(0));
        assertEquals(true, isDirs.get(0));
        assertEquals(false, isSymlinks.get(0));
        assertEquals((Long)4096L, sizes.get(0));
        assertEquals((Long)1779800014000L, modifiedList.get(0));
    }

    @Test
    public void addFilesToListsTestSymlinkFile() {
        List<String> names = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> canonicalPaths = new ArrayList<>();
        List<Boolean> isDirs = new ArrayList<>();
        List<Boolean> isSymlinks = new ArrayList<>();
        List<Long> sizes = new ArrayList<>();
        List<Long> modifiedList = new ArrayList<>();
        String directory = "/";
        String details = "[0] = {" + statsDetailsSymlink + "}";
        List<String> fileList = List.of("boot");
        doReturn("/tst/path").when(guestfishClientSpy).getCanonicalPath("/boot");
        doReturn(statsDetailsDir).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, "/tst/path");

        guestfishClientSpy.addFilesToLists(directory, details, fileList, names, paths, canonicalPaths, isDirs, isSymlinks, sizes, modifiedList);

        assertEquals("boot", names.get(0));
        assertEquals("/boot", paths.get(0));
        assertEquals("/tst/path", canonicalPaths.get(0));
        assertEquals(true, isDirs.get(0));
        assertEquals(true, isSymlinks.get(0));
        assertEquals((Long)4096L, sizes.get(0));
        assertEquals((Long)1779800014000L, modifiedList.get(0));
    }

    @Test
    public void addFilesToListsTestSymlinkFileToAnotherSymlink() {
        List<String> names = new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<String> canonicalPaths = new ArrayList<>();
        List<Boolean> isDirs = new ArrayList<>();
        List<Boolean> isSymlinks = new ArrayList<>();
        List<Long> sizes = new ArrayList<>();
        List<Long> modifiedList = new ArrayList<>();
        String directory = "/";
        String details = "[0] = {" + statsDetailsSymlink + "}";
        List<String> fileList = List.of("boot");
        doReturn("/tst/path").when(guestfishClientSpy).getCanonicalPath("/boot");
        doReturn(statsDetailsSymlink).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, "/tst/path");

        guestfishClientSpy.addFilesToLists(directory, details, fileList, names, paths, canonicalPaths, isDirs, isSymlinks, sizes, modifiedList);

        assertEquals(List.of(), names);
        assertEquals(List.of(), paths);
        assertEquals(List.of(), canonicalPaths);
        assertEquals(List.of(), isDirs);
        assertEquals(List.of(), isSymlinks);
        assertEquals(List.of(), sizes);
        assertEquals(List.of(), modifiedList);
    }

    @Test
    public void formatFileListForLstatnslistTest() {
        List<String> fileList = List.of("libvirt", "super lib");

        String result = guestfishClientSpy.formatFileListForLstatnslist(fileList);

        assertEquals("'libvirt' 'super lib'", result);
    }

    @Test
    public void extractFileTestExtractDir() {
        String filesystem = "/dev/sda";
        String filePath = "/usr/share/batata";
        String destination = "/mnt/sec/kpodkpo.gz";
        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn(statsDetailsDir).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.TAR_OUT, filePath, destination, "compress:gzip");
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        boolean result = guestfishClientSpy.extractFile(filesystem, filePath, destination, false);

        assertTrue(result);
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.TAR_OUT, filePath, destination, "compress:gzip");
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);
    }

    @Test
    public void extractFileTestExtractSymlinkToFile() {
        String filesystem = "/dev/sda";
        String filePath = "/usr/share/batata";
        String canonicalFilePath = "/etc/frita";
        String destination = "/mnt/sec/kpodkpo.gz";
        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn(statsDetailsSymlink).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        doReturn(canonicalFilePath).when(guestfishClientSpy).getCanonicalPath(filePath);
        doReturn(statsDetailsFile).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, canonicalFilePath);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.COMPRESS_OUT, "gzip", canonicalFilePath, destination);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        boolean result = guestfishClientSpy.extractFile(filesystem, filePath, destination, true);

        assertFalse(result);
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        verify(guestfishClientSpy).getCanonicalPath(filePath);
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, canonicalFilePath);
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.COMPRESS_OUT, "gzip", canonicalFilePath, destination);
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);
    }

    @Test (expected = CloudRuntimeException.class)
    public void extractFileTestExtractUnknownFileType() {
        String filesystem = "/dev/sda";
        String filePath = "/usr/share/batata";
        String destination = "/mnt/sec/kpodkpo.gz";
        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn(statsDetailsUnknownType).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        guestfishClientSpy.extractFile(filesystem, filePath, destination, true);

        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        verify(guestfishClientSpy, never()).runGuestfishRemoteCommand(eq(GuestfishClient.COMPRESS_OUT), any(), any(), any());
        verify(guestfishClientSpy, never()).runGuestfishRemoteCommand(eq(GuestfishClient.TAR_OUT), any(), any(), any());
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);
    }

    @Test (expected = CloudRuntimeException.class)
    public void extractFileTestExtractSymlinkToNonExistentFile() {
        String filesystem = "/dev/sda";
        String filePath = "/usr/share/batata";
        String destination = "/mnt/sec/kpodkpo.gz";
        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn(statsDetailsSymlink).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        doReturn(null).when(guestfishClientSpy).getCanonicalPath(filePath);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        guestfishClientSpy.extractFile(filesystem, filePath, destination, true);

        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        verify(guestfishClientSpy).getCanonicalPath(filePath);
        verify(guestfishClientSpy, never()).runGuestfishRemoteCommand(eq(GuestfishClient.COMPRESS_OUT), any(), any(), any());
        verify(guestfishClientSpy, never()).runGuestfishRemoteCommand(eq(GuestfishClient.TAR_OUT), any(), any(), any());
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);
    }

    @Test (expected = CloudRuntimeException.class)
    public void extractFileTestExtractSymlinkToSymlink() {
        String filesystem = "/dev/sda";
        String filePath = "/usr/share/batata";
        String canonicalFilePath = "/etc/frita";
        String destination = "/mnt/sec/kpodkpo.gz";
        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn(statsDetailsSymlink).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        doReturn(canonicalFilePath).when(guestfishClientSpy).getCanonicalPath(filePath);
        doReturn(statsDetailsSymlink).when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, canonicalFilePath);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        guestfishClientSpy.extractFile(filesystem, filePath, destination, true);

        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, filePath);
        verify(guestfishClientSpy).getCanonicalPath(filePath);
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LSTATNS, canonicalFilePath);
        verify(guestfishClientSpy, never()).runGuestfishRemoteCommand(eq(GuestfishClient.COMPRESS_OUT), any(), any(), any());
        verify(guestfishClientSpy, never()).runGuestfishRemoteCommand(eq(GuestfishClient.TAR_OUT), any(), any(), any());
        verify(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);
    }

    @Test
    public void listFilesTestSymlinkToNonExistentFile() {
        String filesystem = "/dev/vdb";
        String directory = "/User Data/";
        Boolean isSymlink = null;

        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn("true").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.IS_SYMLINK, directory);
        doReturn(null).when(guestfishClientSpy).getCanonicalPath(directory.substring(0, directory.length() - 1));
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        ListDataStoreObjectsAnswer result = guestfishClientSpy.listFiles(filesystem, directory, isSymlink);

        assertEquals(0, result.getCount());
        assertFalse(result.isPathExists());
    }

    @Test
    public void listFilesTestSymlinkToSymlink() {
        String filesystem = "/dev/vdb";
        String directory = "/User Data/";
        String canonicalPath = "/Users/Default/AppData";
        Boolean isSymlink = null;

        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn("true").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.IS_SYMLINK, directory);
        doReturn(canonicalPath).when(guestfishClientSpy).getCanonicalPath(directory.substring(0, directory.length() - 1));
        doReturn("true").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.IS_SYMLINK, canonicalPath);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        ListDataStoreObjectsAnswer result = guestfishClientSpy.listFiles(filesystem, directory, isSymlink);

        assertEquals(0, result.getCount());
        assertFalse(result.isPathExists());
    }

    @Test
    public void listFilesTestSymlinkToEmptyDir() {
        String filesystem = "/dev/vdb";
        String directory = "/User Data/";
        String canonicalPath = "/Users/Default/AppData";
        Boolean isSymlink = null;

        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn("true").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.IS_SYMLINK, directory);
        doReturn(canonicalPath).when(guestfishClientSpy).getCanonicalPath(directory.substring(0, directory.length() - 1));
        doReturn("false").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.IS_SYMLINK, canonicalPath);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LS, canonicalPath);
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        ListDataStoreObjectsAnswer result = guestfishClientSpy.listFiles(filesystem, directory, isSymlink);

        assertEquals(0, result.getCount());
        assertTrue(result.isPathExists());
    }

    @Test
    public void listFilesTestDir() {
        String filesystem = "/dev/vdb";
        String directory = "/User Data/";
        Boolean isSymlink = false;

        doNothing().when(guestfishClientSpy).mount(filesystem);
        doReturn("a").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LS, directory);
        doReturn("[0] = " + statsDetailsFile).when(guestfishClientSpy).getDetails(eq(directory), eq(List.of("a")));
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.UMOUNT_ALL);

        ListDataStoreObjectsAnswer result = guestfishClientSpy.listFiles(filesystem, directory, isSymlink);

        assertEquals(1, result.getCount());
        assertEquals("a", result.getNames().get(0));
        assertTrue(result.isPathExists());
    }

    @Test
    public void listFilesystemsTestNoFilesystem() {
        doReturn("").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LIST_FILESYSTEMS);

        List<FilesystemInfoTO> result = guestfishClientSpy.listFilesystems();

        assertTrue(result.isEmpty());
    }

    @Test
    public void listFilesystemsTestSingleFilesystem() {
        doReturn("/dev/sda2: ext4").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LIST_FILESYSTEMS);
        doReturn("123").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.BLOCKDEV_GETSIZE_64, "/dev/sda2");

        List<FilesystemInfoTO> result = guestfishClientSpy.listFilesystems();

        FilesystemInfoTO filesystemInfoTO = result.get(0);
        assertEquals("/dev/sda2", filesystemInfoTO.getName());
        assertEquals(123, filesystemInfoTO.getSize());
        assertEquals("ext4", filesystemInfoTO.getFilesystem());
    }

    @Test
    public void listFilesystemsTestMultipleFilesystemWithUnknown() {
        doReturn("/dev/sda1: unknown\n" + "/dev/sda2: ext4\n" + "/dev/ubuntu-vg/ubuntu-lv: ext3").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.LIST_FILESYSTEMS);
        doReturn("123").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.BLOCKDEV_GETSIZE_64, "/dev/sda2");
        doReturn("444").when(guestfishClientSpy).runGuestfishRemoteCommand(GuestfishClient.BLOCKDEV_GETSIZE_64, "/dev/ubuntu-vg/ubuntu-lv");

        List<FilesystemInfoTO> result = guestfishClientSpy.listFilesystems();

        FilesystemInfoTO filesystemInfoTO = result.remove(0);
        assertEquals("/dev/sda2", filesystemInfoTO.getName());
        assertEquals(123, filesystemInfoTO.getSize());
        assertEquals("ext4", filesystemInfoTO.getFilesystem());

        FilesystemInfoTO filesystemInfoTO2 = result.remove(0);
        assertEquals("/dev/ubuntu-vg/ubuntu-lv", filesystemInfoTO2.getName());
        assertEquals(444, filesystemInfoTO2.getSize());
        assertEquals("ext3", filesystemInfoTO2.getFilesystem());

        assertTrue(result.isEmpty());
    }
}
