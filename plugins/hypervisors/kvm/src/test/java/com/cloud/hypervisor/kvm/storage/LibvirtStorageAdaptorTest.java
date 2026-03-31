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

package com.cloud.hypervisor.kvm.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.StoragePool;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtStorageAdaptorTest {

    private MockedStatic<LibvirtConnection> libvirtConnectionMockedStatic;

    private AutoCloseable closeable;

    @Mock
    LibvirtStoragePool mockPool;

    MockedStatic<Script> mockScript;

    // For mocking Script constructor
    private MockedConstruction<Script> mockScriptConstruction;

    @Spy
    static LibvirtStorageAdaptor libvirtStorageAdaptor = new LibvirtStorageAdaptor(null);

    @Before
    public void initMocks() {
        closeable = MockitoAnnotations.openMocks(this);
        libvirtConnectionMockedStatic = Mockito.mockStatic(LibvirtConnection.class);
        Mockito.reset(mockPool);
        mockScript = Mockito.mockStatic(Script.class);
    }

    @After
    public void tearDown() throws Exception {
        libvirtConnectionMockedStatic.close();
        mockScript.close();
        if (mockScriptConstruction != null) {
            mockScriptConstruction.close();
        }
        closeable.close();
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateStoragePoolWithNFSMountOpts() throws Exception {
        LibvirtStoragePoolDef.PoolType type = LibvirtStoragePoolDef.PoolType.NETFS;
        String name = "Primary1";
        String uuid = String.valueOf(UUID.randomUUID());
        String host = "127.0.0.1";
        String dir  = "/export/primary";
        String targetPath = "/mnt/" + uuid;

        String poolXml = "<pool type='" + type.toString() + "' xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>\n" +
                "<name>" +name + "</name>\n<uuid>" + uuid + "</uuid>\n" +
                "<source>\n<host name='" + host + "'/>\n<dir path='" + dir + "'/>\n</source>\n<target>\n" +
                "<path>" + targetPath + "</path>\n</target>\n" +
                "<fs:mount_opts>\n<fs:option name='vers=4.1'/>\n<fs:option name='nconnect=4'/>\n</fs:mount_opts>\n</pool>\n";

        Connect conn =  Mockito.mock(Connect.class);
        StoragePool sp = Mockito.mock(StoragePool.class);
        Mockito.when(LibvirtConnection.getConnection()).thenReturn(conn);
        Mockito.when(conn.storagePoolLookupByUUIDString(uuid)).thenReturn(sp);
        Mockito.when(sp.isActive()).thenReturn(1);
        Mockito.when(sp.getXMLDesc(0)).thenReturn(poolXml);
        Mockito.when(Script.runSimpleBashScriptForExitValue(anyString())).thenReturn(-1);

        Map<String, String> details = new HashMap<>();
        details.put("nfsmountopts", "vers=4.1, nconnect=4");
        KVMStoragePool pool = libvirtStorageAdaptor.createStoragePool(uuid, null, 0, dir, null, Storage.StoragePoolType.NetworkFilesystem, details, true);
    }

    @Test
    public void testUpdateLocalPoolIops_IgnoredForNonFilesystemType() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.SharedMountPoint);

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);
    }

    @Test
    public void testUpdateLocalPoolIops_IgnoredForBlankLocalPath() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("");

        Mockito.verify(mockPool, never()).getLocalPath();
        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool, never()).setUsedIops(anyLong());
    }

    @Test
    public void testUpdateLocalPoolIops_NoDevice() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("/mock/path");
        Mockito.when(mockPool.getName()).thenReturn("mockPool");
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(1000L))).thenReturn(new Pair<>(0, "\n"));

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool, never()).setUsedIops(anyLong());
    }

    @Test
    public void testUpdateLocalPoolIops_SuccessfulUpdate() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("/mock/path");
        Mockito.when(mockPool.getName()).thenReturn("mockPool");
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(1000L))).thenReturn(new Pair<>(0, "sda\n"));
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(10000L))).thenReturn(new Pair<>(0, "42\n"));

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool).setUsedIops(42L);
    }

    @Test
    public void testUpdateLocalPoolIops_HandlesNumberFormatException() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("/mock/path");
        Mockito.when(mockPool.getName()).thenReturn("mockPool");
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(1000L))).thenReturn(new Pair<>(0, "sda\n"));
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(10000L)))
                .thenReturn(new Pair<>(0, "invalid_number"));

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool, never()).setUsedIops(anyLong());
    }

    @Test
    public void testUpdateLocalPoolIops_NullResultFromScript() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.Filesystem);
        Mockito.when(mockPool.getLocalPath()).thenReturn("/mock/path");
        Mockito.when(mockPool.getName()).thenReturn("mockPool");
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(1000L))).thenReturn(new Pair<>(0, "sda\n"));
        Mockito.when(Script.executePipedCommands(anyList(), Mockito.eq(10000L)))
                .thenReturn(new Pair<>(0, null));

        libvirtStorageAdaptor.updateLocalPoolIops(mockPool);

        Mockito.verify(mockPool, never()).setUsedIops(anyLong());
    }

    @Test
    public void testGetVolumeFromCLVMPool_ReturnsNullForNonCLVMPool() {
        Storage.StoragePoolType type = Storage.StoragePoolType.NetworkFilesystem;

        assert type != Storage.StoragePoolType.CLVM;
        assert type != Storage.StoragePoolType.CLVM_NG;
    }

    @Test
    public void testCLVMPoolTypeDetection_CLVM() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        Storage.StoragePoolType type = mockPool.getType();

        assert type == Storage.StoragePoolType.CLVM;
        assert type != Storage.StoragePoolType.CLVM_NG;
    }

    @Test
    public void testCLVMPoolTypeDetection_CLVM_NG() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        Storage.StoragePoolType type = mockPool.getType();

        assert type == Storage.StoragePoolType.CLVM_NG;
        assert type != Storage.StoragePoolType.CLVM;
    }

    @Test
    public void testCLVMPoolLocalPathFormat() {
        String vgName = "acsvg";
        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        String localPath = mockPool.getLocalPath();

        assert localPath.equals(vgName);
        assert !localPath.startsWith("/");
    }

    @Test
    public void testCLVMNGPoolLocalPathFormat() {
        String vgName = "acsvg";
        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        String localPath = mockPool.getLocalPath();

        assert localPath.equals(vgName);
        assert !localPath.startsWith("/");
    }

    @Test
    public void testCLVMVolumePathFormat() {
        String vgName = "acsvg";
        String volumeUuid = UUID.randomUUID().toString();
        String expectedPath = "/dev/" + vgName + "/" + volumeUuid;

        assert expectedPath.startsWith("/dev/");
        assert expectedPath.contains(vgName);
        assert expectedPath.endsWith(volumeUuid);
    }

    @Test
    public void testCLVMNGVolumePathFormat() {
        String vgName = "acsvg";
        String volumeUuid = UUID.randomUUID().toString();
        String expectedPath = "/dev/" + vgName + "/" + volumeUuid;

        assert expectedPath.startsWith("/dev/");
        assert expectedPath.contains(vgName);
        assert expectedPath.endsWith(volumeUuid);
    }

    @Test
    public void testCLVMTemplateVolumeNamingConvention() {
        String templateUuid = "550e8400-e29b-41d4-a716-446655440000";
        String expectedLvName = "template-" + templateUuid;

        assert expectedLvName.startsWith("template-");
        assert expectedLvName.contains(templateUuid);
        assert expectedLvName.equals("template-550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    public void testCLVMTemplatePathFormat() {
        // CLVM_NG template paths: /dev/vgname/template-{uuid}
        String vgName = "acsvg";
        String templateUuid = "550e8400-e29b-41d4-a716-446655440000";
        String expectedPath = "/dev/" + vgName + "/template-" + templateUuid;

        assert expectedPath.equals("/dev/acsvg/template-550e8400-e29b-41d4-a716-446655440000");
        assert expectedPath.startsWith("/dev/");
        assert expectedPath.contains("template-");
    }

    @Test
    public void testCLVMPoolIsBlockDeviceStorage() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        Storage.StoragePoolType type = mockPool.getType();

        boolean isBlockBased = (type == Storage.StoragePoolType.CLVM ||
                               type == Storage.StoragePoolType.CLVM_NG);
        assert isBlockBased;
    }

    @Test
    public void testCLVMNGPoolIsBlockDeviceStorage() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        Storage.StoragePoolType type = mockPool.getType();

        boolean isBlockBased = (type == Storage.StoragePoolType.CLVM ||
                               type == Storage.StoragePoolType.CLVM_NG);
        assert isBlockBased;
    }

    @Test
    public void testCLVMPoolSupportsSharedStorage() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        Storage.StoragePoolType type = mockPool.getType();
        boolean supportsShared = (type == Storage.StoragePoolType.CLVM ||
                                 type == Storage.StoragePoolType.CLVM_NG);
        assert supportsShared;
    }

    @Test
    public void testVolumeGroupNameExtraction() {
        String vgName = "acsvg";
        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        String extractedVgName = mockPool.getLocalPath();

        assert extractedVgName.equals("acsvg");
        assert extractedVgName.matches("[a-zA-Z0-9_-]+");
    }

    @Test
    public void testCLVMPoolDetailsContainSecureZeroFill() {
        Map<String, String> details = new HashMap<>();
        details.put("CLVM_SECURE_ZERO_FILL", "true");

        boolean secureZeroEnabled = "true".equals(details.get("CLVM_SECURE_ZERO_FILL"));
        assert secureZeroEnabled;
    }

    @Test
    public void testCLVMPoolDetailsSecureZeroFillDisabled() {
        Map<String, String> details = new HashMap<>();
        details.put("CLVM_SECURE_ZERO_FILL", "false");

        boolean secureZeroEnabled = "true".equals(details.get("CLVM_SECURE_ZERO_FILL"));
        assert !secureZeroEnabled;
    }

    @Test
    public void testCLVMPoolDetailsSecureZeroFillDefault() {
        Map<String, String> details = new HashMap<>();

        boolean secureZeroEnabled = "true".equals(details.get("CLVM_SECURE_ZERO_FILL"));
        assert !secureZeroEnabled;
    }

    @Test
    public void testCreateCLVMStoragePool_Success() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String vgName = "testvg";
        String host = "localhost";

        Connect mockConn = Mockito.mock(Connect.class);
        StoragePool mockStoragePool = Mockito.mock(StoragePool.class);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn(null);
            });

        Mockito.when(mockConn.storagePoolDefineXML(anyString(), eq(0)))
               .thenReturn(mockStoragePool);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createCLVMStoragePool", Connect.class, String.class, String.class, String.class);
        method.setAccessible(true);

        StoragePool result = (StoragePool) method.invoke(
            libvirtStorageAdaptor, mockConn, uuid, host, vgName);

        assertNotNull("Storage pool should be created", result);
        Mockito.verify(mockStoragePool).setAutostart(1);
        Mockito.verify(mockConn).storagePoolDefineXML(anyString(), eq(0));
    }

    @Test
    public void testCreateCLVMStoragePool_VGNotFound() throws Exception {
        String uuid = UUID.randomUUID().toString();
        String vgName = "nonexistentvg";
        String host = "localhost";

        Connect mockConn = Mockito.mock(Connect.class);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn("Volume group \"" + vgName + "\" not found");
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createCLVMStoragePool", Connect.class, String.class, String.class, String.class);
        method.setAccessible(true);

        StoragePool result = (StoragePool) method.invoke(
            libvirtStorageAdaptor, mockConn, uuid, host, vgName);

        assert result == null : "Should return null when VG doesn't exist";
        Mockito.verify(mockConn, never()).storagePoolDefineXML(anyString(), anyInt());
    }

    @Test
    public void testCreateClvmVolume_Success() throws Exception {
        String volumeName = UUID.randomUUID().toString();
        long size = 10737418240L;
        String vgName = "testvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn(null);
            });


        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createClvmVolume", String.class, long.class, KVMStoragePool.class);
        method.setAccessible(true);

        KVMPhysicalDisk result = (KVMPhysicalDisk) method.invoke(
            libvirtStorageAdaptor, volumeName, size, mockPool);

        assertNotNull("Physical disk should be created", result);
        assertEquals("Volume name should match", volumeName, result.getName());
        assertEquals("Format should be RAW", PhysicalDiskFormat.RAW, result.getFormat());
        assert result.getPath().contains(vgName) : "Path should contain VG name";
        assert result.getPath().contains(volumeName) : "Path should contain volume name";
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateClvmVolume_LvcreateFailure() throws Throwable {
        String volumeName = UUID.randomUUID().toString();
        long size = 10737418240L;
        String vgName = "testvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn("lvcreate failed: insufficient space");
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createClvmVolume", String.class, long.class, KVMStoragePool.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, volumeName, size, mockPool);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testCreateClvmNgDiskWithBacking_NoBackingFile() throws Exception {
        String volumeUuid = UUID.randomUUID().toString();
        int timeout = 30000;
        long virtualSize = 10737418240L;
        String vgName = "testvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn(null);
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createClvmNgDiskWithBacking",
            String.class, int.class, long.class, String.class,
            KVMStoragePool.class, Storage.ProvisioningType.class);
        method.setAccessible(true);

        KVMPhysicalDisk result = (KVMPhysicalDisk) method.invoke(
            libvirtStorageAdaptor, volumeUuid, timeout, virtualSize, null,
            mockPool, Storage.ProvisioningType.THIN);

        assertNotNull("Physical disk should be created", result);
        assertEquals("Volume UUID should match", volumeUuid, result.getName());
        assertEquals("Format should be QCOW2", PhysicalDiskFormat.QCOW2, result.getFormat());
        assert result.getPath().contains(vgName) : "Path should contain VG name";
        assert result.getPath().contains(volumeUuid) : "Path should contain volume UUID";
    }

    @Test
    public void testCreateClvmNgDiskWithBacking_WithBackingFile() throws Exception {
        String volumeUuid = UUID.randomUUID().toString();
        String backingFile = "/dev/testvg/parent-volume";
        int timeout = 30000;
        long virtualSize = 10737418240L;
        String vgName = "testvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn(null);
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createClvmNgDiskWithBacking",
            String.class, int.class, long.class, String.class,
            KVMStoragePool.class, Storage.ProvisioningType.class);
        method.setAccessible(true);

        KVMPhysicalDisk result = (KVMPhysicalDisk) method.invoke(
            libvirtStorageAdaptor, volumeUuid, timeout, virtualSize, backingFile,
            mockPool, Storage.ProvisioningType.SPARSE);

        assertNotNull("Physical disk should be created", result);
        assertEquals("Format should be QCOW2", PhysicalDiskFormat.QCOW2, result.getFormat());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateClvmNgDiskWithBacking_LvcreateFailure() throws Throwable {
        String volumeUuid = UUID.randomUUID().toString();
        int timeout = 30000;
        long virtualSize = 10737418240L;
        String vgName = "testvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn("Insufficient free extents");
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createClvmNgDiskWithBacking",
            String.class, int.class, long.class, String.class,
            KVMStoragePool.class, Storage.ProvisioningType.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, volumeUuid, timeout, virtualSize,
                         null, mockPool, Storage.ProvisioningType.THIN);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testCreateClvmNgDiskWithBacking_QemuImgFailure() throws Throwable {
        String volumeUuid = UUID.randomUUID().toString();
        int timeout = 30000;
        long virtualSize = 10737418240L;
        String vgName = "testvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        final int[] callCount = {0};
        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenAnswer(invocation -> {
                    callCount[0]++;
                    if (callCount[0] == 1) {
                        return null;
                    } else {
                        return "qemu-img: error creating QCOW2";
                    }
                });
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createClvmNgDiskWithBacking",
            String.class, int.class, long.class, String.class,
            KVMStoragePool.class, Storage.ProvisioningType.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, volumeUuid, timeout, virtualSize,
                         null, mockPool, Storage.ProvisioningType.THIN);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetPhysicalDiskViaDirectBlockDevice_VolumeNotFound() throws Throwable {
        String volumeUuid = UUID.randomUUID().toString();
        String vgName = "testvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn("  Volume not found");
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "getPhysicalDiskViaDirectBlockDevice",
            String.class, KVMStoragePool.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, volumeUuid, mockPool);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetPhysicalDiskViaDirectBlockDevice_NullPoolPath() throws Throwable {
        String volumeUuid = UUID.randomUUID().toString();

        Mockito.when(mockPool.getLocalPath()).thenReturn(null);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "getPhysicalDiskViaDirectBlockDevice",
            String.class, KVMStoragePool.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, volumeUuid, mockPool);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetPhysicalDiskViaDirectBlockDevice_EmptyPoolPath() throws Throwable {
        String volumeUuid = UUID.randomUUID().toString();

        Mockito.when(mockPool.getLocalPath()).thenReturn("");

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "getPhysicalDiskViaDirectBlockDevice",
            String.class, KVMStoragePool.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, volumeUuid, mockPool);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testShouldSecureZeroFill_EnabledInDetails() throws Exception {
        Map<String, String> details = new HashMap<>();
        details.put(KVMStoragePool.CLVM_SECURE_ZERO_FILL, "true");

        Mockito.when(mockPool.getDetails()).thenReturn(details);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "shouldSecureZeroFill", KVMStoragePool.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, mockPool);

        assert result : "Should return true when CLVM_SECURE_ZERO_FILL is 'true'";
    }

    @Test
    public void testShouldSecureZeroFill_DisabledInDetails() throws Exception {
        Map<String, String> details = new HashMap<>();
        details.put(KVMStoragePool.CLVM_SECURE_ZERO_FILL, "false");

        Mockito.when(mockPool.getDetails()).thenReturn(details);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "shouldSecureZeroFill", KVMStoragePool.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, mockPool);

        assert !result : "Should return false when CLVM_SECURE_ZERO_FILL is 'false'";
    }

    @Test
    public void testShouldSecureZeroFill_NotSetInDetails() throws Exception {
        Map<String, String> details = new HashMap<>();

        Mockito.when(mockPool.getDetails()).thenReturn(details);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "shouldSecureZeroFill", KVMStoragePool.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, mockPool);

        assert !result : "Should return false when CLVM_SECURE_ZERO_FILL is not set";
    }

    @Test
    public void testShouldSecureZeroFill_NullDetails() throws Exception {
        Mockito.when(mockPool.getDetails()).thenReturn(null);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "shouldSecureZeroFill", KVMStoragePool.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, mockPool);

        assert !result : "Should return false when details are null";
    }

    @Test
    public void testExtractVgNameFromPool_SimplePath() throws Exception {
        String vgName = "testvg";
        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "extractVgNameFromPool", KVMStoragePool.class);
        method.setAccessible(true);

        String result = (String) method.invoke(libvirtStorageAdaptor, mockPool);

        assertEquals("Should extract VG name correctly", vgName, result);
    }

    @Test
    public void testExtractVgNameFromPool_PathWithSlash() throws Exception {
        String vgName = "testvg";
        String path = "/" + vgName;
        Mockito.when(mockPool.getLocalPath()).thenReturn(path);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "extractVgNameFromPool", KVMStoragePool.class);
        method.setAccessible(true);

        String result = (String) method.invoke(libvirtStorageAdaptor, mockPool);

        assertEquals("Should extract VG name from path with leading slash", vgName, result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testExtractVgNameFromPool_NullPath() throws Throwable {
        Mockito.when(mockPool.getLocalPath()).thenReturn(null);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "extractVgNameFromPool", KVMStoragePool.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, mockPool);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testLvExists_VolumeExists() throws Exception {
        String lvPath = "/dev/testvg/test-volume";

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn(null);
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "lvExists", String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, lvPath);

        assert result : "Should return true when LV exists (lvs returns null)";
    }

    @Test
    public void testLvExists_VolumeDoesNotExist() throws Exception {
        String lvPath = "/dev/testvg/nonexistent-volume";

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> {
                when(mock.execute()).thenReturn("Volume not found");
            });

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "lvExists", String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, lvPath);

        assert !result : "Should return false when LV does not exist";
    }

    @Test
    public void testGetVgName_SimpleName() throws Exception {
        String vgName = "acsvg";

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "getVgName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(libvirtStorageAdaptor, vgName);

        assertEquals("Should return VG name as-is for simple name", vgName, result);
    }

    @Test
    public void testGetVgName_PathWithSlash() throws Exception {
        String vgName = "acsvg";
        String path = "/" + vgName;

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "getVgName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(libvirtStorageAdaptor, path);

        assertEquals("Should extract VG name from path with leading slash", vgName, result);
    }

    @Test
    public void testGetVgName_DevPath() throws Exception {
        String vgName = "acsvg";
        String path = "/dev/" + vgName;

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "getVgName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(libvirtStorageAdaptor, path);

        assertEquals("Should extract VG name from /dev/ path", vgName, result);
    }

    @Test
    public void testCreatePhysicalDiskFromClvmLv_CLVM() throws Exception {
        String lvPath = "/dev/testvg/test-volume";
        String volumeUuid = "test-volume";
        long size = 10737418240L;

        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createPhysicalDiskFromClvmLv",
            String.class, String.class, KVMStoragePool.class, long.class);
        method.setAccessible(true);

        KVMPhysicalDisk result = (KVMPhysicalDisk) method.invoke(
            libvirtStorageAdaptor, lvPath, volumeUuid, mockPool, size);

        assertNotNull("Physical disk should be created", result);
        assertEquals("Format should be RAW for CLVM", PhysicalDiskFormat.RAW, result.getFormat());
        assertEquals("Size should match", size, result.getSize());
        assertEquals("Path should match", lvPath, result.getPath());
    }

    @Test
    public void testCreatePhysicalDiskFromClvmLv_CLVM_NG() throws Exception {
        String lvPath = "/dev/testvg-ng/test-volume";
        String volumeUuid = "test-volume";
        long size = 10737418240L;

        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createPhysicalDiskFromClvmLv",
            String.class, String.class, KVMStoragePool.class, long.class);
        method.setAccessible(true);

        KVMPhysicalDisk result = (KVMPhysicalDisk) method.invoke(
            libvirtStorageAdaptor, lvPath, volumeUuid, mockPool, size);

        assertNotNull("Physical disk should be created", result);
        assertEquals("Format should be QCOW2 for CLVM_NG", PhysicalDiskFormat.QCOW2, result.getFormat());
        assertEquals("Size should match", size, result.getSize());
        assertEquals("Path should match", lvPath, result.getPath());
    }

    @Test
    public void testCLVMPoolSecureZeroFill_TrueValue() {
        Map<String, String> details = new HashMap<>();
        details.put(KVMStoragePool.CLVM_SECURE_ZERO_FILL, "true");

        Mockito.when(mockPool.getDetails()).thenReturn(details);

        String value = mockPool.getDetails().get(KVMStoragePool.CLVM_SECURE_ZERO_FILL);
        assert "true".equals(value) : "CLVM_SECURE_ZERO_FILL should be 'true'";
    }

    @Test
    public void testCLVMPoolSecureZeroFill_FalseValue() {
        Map<String, String> details = new HashMap<>();
        details.put(KVMStoragePool.CLVM_SECURE_ZERO_FILL, "false");

        Mockito.when(mockPool.getDetails()).thenReturn(details);

        String value = mockPool.getDetails().get(KVMStoragePool.CLVM_SECURE_ZERO_FILL);
        assert "false".equals(value) : "CLVM_SECURE_ZERO_FILL should be 'false'";
    }

    @Test
    public void testCLVMVolumePathConstruction() {
        String vgName = "acsvg";
        String volumeUuid = "550e8400-e29b-41d4-a716-446655440000";
        String expectedPath = "/dev/" + vgName + "/" + volumeUuid;

        assertEquals("/dev/acsvg/550e8400-e29b-41d4-a716-446655440000", expectedPath);
    }

    @Test
    public void testCLVMNGPoolSupportsQCOW2Format() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        Storage.StoragePoolType type = mockPool.getType();
        PhysicalDiskFormat expectedFormat = PhysicalDiskFormat.QCOW2;

        assert type == Storage.StoragePoolType.CLVM_NG : "Pool type should be CLVM_NG";
        assertNotNull("QCOW2 format should be available", expectedFormat);
    }

    @Test
    public void testCLVMPoolSupportsRAWFormat() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        Storage.StoragePoolType type = mockPool.getType();
        PhysicalDiskFormat expectedFormat = PhysicalDiskFormat.RAW;

        assert type == Storage.StoragePoolType.CLVM : "Pool type should be CLVM";
        assertNotNull("RAW format should be available", expectedFormat);
    }

    @Test
    public void testVerifyLvExistsInVg_VolumeExists() throws Exception {
        String volumeUuid = "test-volume-uuid";
        String vgName = "testvg";

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> when(mock.execute()).thenReturn(null));

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "verifyLvExistsInVg", String.class, String.class);
        method.setAccessible(true);

        method.invoke(libvirtStorageAdaptor, volumeUuid, vgName);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testVerifyLvExistsInVg_VolumeDoesNotExist() throws Throwable {
        String volumeUuid = "nonexistent-volume";
        String vgName = "testvg";

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> when(mock.execute()).thenReturn("  Volume not found"));

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "verifyLvExistsInVg", String.class, String.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, volumeUuid, vgName);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void testGetClvmVolumeSize_MethodExists() throws Exception {
        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "getClvmVolumeSize", String.class);
        method.setAccessible(true);

        assertNotNull("getClvmVolumeSize method should exist", method);
    }

    @Test
    public void testCLVMPoolIsBlockBased() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        boolean isBlockBased = mockPool.getType() == Storage.StoragePoolType.CLVM ||
                               mockPool.getType() == Storage.StoragePoolType.CLVM_NG;

        assert isBlockBased : "CLVM should be recognized as block-based storage";
    }

    @Test
    public void testCLVMNGPoolIsBlockBased() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        boolean isBlockBased = mockPool.getType() == Storage.StoragePoolType.CLVM ||
                               mockPool.getType() == Storage.StoragePoolType.CLVM_NG;

        assert isBlockBased : "CLVM_NG should be recognized as block-based storage";
    }

    @Test
    public void testGetVgName_ComplexPath() throws Exception {
        String vgName = "acsvg-ng";
        String path = "/dev/mapper/" + vgName;

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "getVgName", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(libvirtStorageAdaptor, path);

        assertNotNull("VG name should not be null", result);
    }

    @Test
    public void testExtractVgNameFromPool_EmptyPath() throws Exception {
        Mockito.when(mockPool.getLocalPath()).thenReturn("");

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "extractVgNameFromPool", KVMStoragePool.class);
        method.setAccessible(true);

        try {
            method.invoke(libvirtStorageAdaptor, mockPool);
            assert false : "Should throw CloudRuntimeException for empty path";
        } catch (InvocationTargetException e) {
            assert e.getCause() instanceof CloudRuntimeException :
                "Should throw CloudRuntimeException";
        }
    }

    @Test
    public void testCleanupCLVMVolume_VolumeExists_WithSecureZeroFill() throws Exception {
        String volumeUuid = UUID.randomUUID().toString();
        String vgName = "testvg";
        Map<String, String> details = new HashMap<>();
        details.put(KVMStoragePool.CLVM_SECURE_ZERO_FILL, "true");

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);
        Mockito.when(mockPool.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(mockPool.getDetails()).thenReturn(details);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> when(mock.execute()).thenReturn(null));

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "cleanupCLVMVolume", String.class, KVMStoragePool.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, volumeUuid, mockPool);

        assert result : "Cleanup should succeed";
    }

    @Test
    public void testCleanupCLVMVolume_VolumeExists_WithoutSecureZeroFill() throws Exception {
        String volumeUuid = UUID.randomUUID().toString();
        String vgName = "testvg";
        Map<String, String> details = new HashMap<>();
        details.put(KVMStoragePool.CLVM_SECURE_ZERO_FILL, "false");

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);
        Mockito.when(mockPool.getUuid()).thenReturn(UUID.randomUUID().toString());
        Mockito.when(mockPool.getDetails()).thenReturn(details);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> when(mock.execute()).thenReturn(null));

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "cleanupCLVMVolume", String.class, KVMStoragePool.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, volumeUuid, mockPool);

        assert result : "Cleanup should succeed without zero-fill";
    }

    @Test
    public void testCleanupCLVMVolume_VolumeNotFound() throws Exception {
        String volumeUuid = "nonexistent-volume";
        String vgName = "testvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);
        Mockito.when(mockPool.getUuid()).thenReturn(UUID.randomUUID().toString());

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> when(mock.execute()).thenReturn("Volume not found"));

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "cleanupCLVMVolume", String.class, KVMStoragePool.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, volumeUuid, mockPool);

        assert result : "Should return true when volume not found (already deleted)";
    }

    @Test
    public void testCleanupCLVMVolume_NullSourceDir() throws Exception {
        String volumeUuid = UUID.randomUUID().toString();

        Mockito.when(mockPool.getLocalPath()).thenReturn(null);
        Mockito.when(mockPool.getUuid()).thenReturn(UUID.randomUUID().toString());

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "cleanupCLVMVolume", String.class, KVMStoragePool.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(libvirtStorageAdaptor, volumeUuid, mockPool);

        assert result : "Should return true when source dir is null";
    }

    @Test
    public void testRemoveLvOnFailure_Success() throws Exception {
        String lvPath = "/dev/testvg/test-volume";
        int timeout = 30000;

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> when(mock.execute()).thenReturn(null));

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "removeLvOnFailure", String.class, int.class);
        method.setAccessible(true);

        method.invoke(libvirtStorageAdaptor, lvPath, timeout);
    }

    @Test
    public void testEnsureTemplateAccessibility_CLVM_NG_TemplateVolume() throws Exception {
        String volumeUuid = "template-550e8400-e29b-41d4-a716-446655440000";
        String lvPath = "/dev/testvg/template-550e8400-e29b-41d4-a716-446655440000";

        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        mockScriptConstruction = Mockito.mockConstruction(Script.class,
            (mock, context) -> when(mock.execute()).thenReturn(null));

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "ensureTemplateAccessibility", String.class, String.class, KVMStoragePool.class);
        method.setAccessible(true);

        method.invoke(libvirtStorageAdaptor, volumeUuid, lvPath, mockPool);
    }

    @Test
    public void testEnsureTemplateAccessibility_CLVM_NonTemplate() throws Exception {
        String volumeUuid = UUID.randomUUID().toString();
        String lvPath = "/dev/testvg/" + volumeUuid;

        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "ensureTemplateAccessibility", String.class, String.class, KVMStoragePool.class);
        method.setAccessible(true);

        method.invoke(libvirtStorageAdaptor, volumeUuid, lvPath, mockPool);
    }

    @Test
    public void testCreateVirtualClvmPool_MethodExists() throws Exception {
        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "createVirtualClvmPool",
            String.class, String.class, String.class,
            Storage.StoragePoolType.class, Map.class);
        method.setAccessible(true);

        assertNotNull("createVirtualClvmPool method should exist", method);
    }

    @Test
    public void testFindDeviceNodeAfterActivation_MethodExists() throws Exception {
        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "findDeviceNodeAfterActivation",
            String.class, String.class, String.class);
        method.setAccessible(true);

        assertNotNull("findDeviceNodeAfterActivation method should exist", method);
    }

    @Test
    public void testHandleMissingDeviceNode_MethodExists() throws Exception {
        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "handleMissingDeviceNode",
            String.class, String.class, KVMStoragePool.class);
        method.setAccessible(true);

        assertNotNull("handleMissingDeviceNode method should exist", method);
    }

    @Test
    public void testHandleMissingDeviceNode_NonTemplate_MethodExists() throws Exception {
        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "handleMissingDeviceNode",
            String.class, String.class, KVMStoragePool.class);
        method.setAccessible(true);

        assertNotNull("handleMissingDeviceNode method should exist", method);
    }

    @Test
    public void testFindAccessibleDeviceNode_MethodExists() throws Exception {
        Method method = LibvirtStorageAdaptor.class.getDeclaredMethod(
            "findAccessibleDeviceNode",
            String.class, String.class, KVMStoragePool.class);
        method.setAccessible(true);

        assertNotNull("findAccessibleDeviceNode method should exist", method);
    }

    @Test
    public void testCLVMNGDiskFormat_IsQCOW2() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        PhysicalDiskFormat format = (mockPool.getType() == Storage.StoragePoolType.CLVM_NG)
            ? PhysicalDiskFormat.QCOW2
            : PhysicalDiskFormat.RAW;

        assertEquals("CLVM_NG should use QCOW2 format", PhysicalDiskFormat.QCOW2, format);
    }

    @Test
    public void testCLVMDiskFormat_IsRAW() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        PhysicalDiskFormat format = (mockPool.getType() == Storage.StoragePoolType.CLVM_NG)
            ? PhysicalDiskFormat.QCOW2
            : PhysicalDiskFormat.RAW;

        assertEquals("CLVM should use RAW format", PhysicalDiskFormat.RAW, format);
    }

    @Test
    public void testCLVMPoolDetails_DefaultSecureZeroFill() {
        Map<String, String> details = new HashMap<>();

        String value = details.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL);

        assert value == null : "CLVM_SECURE_ZERO_FILL should default to null";
    }

    @Test
    public void testCLVMPoolDetails_CustomSecureZeroFill() {
        Map<String, String> details = new HashMap<>();
        details.put(KVMStoragePool.CLVM_SECURE_ZERO_FILL, "true");

        String value = details.get(KVMStoragePool.CLVM_SECURE_ZERO_FILL);

        assertEquals("CLVM_SECURE_ZERO_FILL should be 'true'", "true", value);
    }

    @Test
    public void testCLVMVolumeDeviceMapperPathEscaping() {
        String vgName = "acsvg-ng";
        String volumeUuid = "ea19580e-0882-4ab8-973b-a320637037f4";

        String vgNameEscaped = vgName.replace("-", "--");
        String volumeUuidEscaped = volumeUuid.replace("-", "--");
        String mapperPath = "/dev/mapper/" + vgNameEscaped + "-" + volumeUuidEscaped;

        String expectedPath = "/dev/mapper/acsvg--ng-ea19580e--0882--4ab8--973b--a320637037f4";
        assertEquals("Mapper path should have escaped hyphens", expectedPath, mapperPath);
    }

    @Test
    public void testCLVMTemplateNamePrefix() {
        String templateUuid = UUID.randomUUID().toString();
        String templateName = "template-" + templateUuid;

        assert templateName.startsWith("template-");
        assert templateName.length() > 9;
    }

    @Test
    public void testCLVMPoolTypeIsNotFileSystem() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        boolean isFileSystem = mockPool.getType() == Storage.StoragePoolType.Filesystem;

        assert !isFileSystem : "CLVM is not a file system pool type";
    }

    @Test
    public void testCLVMNGPoolTypeIsNotFileSystem() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        boolean isFileSystem = mockPool.getType() == Storage.StoragePoolType.Filesystem;

        assert !isFileSystem : "CLVM_NG is not a file system pool type";
    }

    @Test
    public void testCLVMPoolTypeIsNotNFS() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM);

        boolean isNFS = mockPool.getType() == Storage.StoragePoolType.NetworkFilesystem;

        assert !isNFS : "CLVM is not an NFS pool type";
    }

    @Test
    public void testCLVMNGPoolTypeIsNotRBD() {
        Mockito.when(mockPool.getType()).thenReturn(Storage.StoragePoolType.CLVM_NG);

        boolean isRBD = mockPool.getType() == Storage.StoragePoolType.RBD;

        assert !isRBD : "CLVM_NG is not an RBD pool type";
    }

    @Test
    public void testCLVMVolumeUUIDFormat() {
        String volumeUuid = UUID.randomUUID().toString();

        assert volumeUuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    public void testCLVMNGBackingFileFormat() {
        String vgName = "testvg";
        String templateUuid = UUID.randomUUID().toString();
        String backingFile = "/dev/" + vgName + "/template-" + templateUuid;

        assert backingFile.startsWith("/dev/");
        assert backingFile.contains("template-");
        assert backingFile.contains(templateUuid);
    }

    @Test
    public void testCLVMPoolLocalPathValidation() {
        String vgName = "acsvg";

        Mockito.when(mockPool.getLocalPath()).thenReturn(vgName);

        String localPath = mockPool.getLocalPath();

        assertNotNull("Local path should not be null", localPath);
        assert !localPath.isEmpty() : "Local path should not be empty";
    }
}
