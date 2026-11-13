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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.utils.rbd.RbdEncryption;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.StoragePool;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.ceph.rbd.RbdException;
import com.ceph.rbd.RbdImage;
import com.ceph.rbd.jna.RbdSnapInfo;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.hypervisor.kvm.resource.LibvirtStoragePoolDef;
import com.cloud.storage.Storage;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

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

    private static final String BASE_SNAP = "cloudstack-base-snap";
    private static final String LUKS_SNAP = "cloudstack-base-snap-luks2";
    private static final long TEMPLATE_SIZE = 4L << 30;
    private static final long TIMEOUT = 10800000L;

    private RbdImage templateImageWithSnapshots(String... names) throws RbdException {
        RbdImage image = Mockito.mock(RbdImage.class);
        List<RbdSnapInfo> snaps = new ArrayList<>();
        for (String name : names) {
            RbdSnapInfo info = new RbdSnapInfo();
            info.name = name;
            snaps.add(info);
        }
        Mockito.when(image.snapList()).thenReturn(snaps);
        return image;
    }

    private KVMPhysicalDisk rbdTemplate() {
        KVMPhysicalDisk template = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.lenient().when(template.getName()).thenReturn("tmpl");
        Mockito.lenient().when(template.getVirtualSize()).thenReturn(TEMPLATE_SIZE);
        return template;
    }

    private KVMStoragePool rbdPool() {
        KVMStoragePool pool = Mockito.mock(KVMStoragePool.class);
        Mockito.lenient().when(pool.getSourceHost()).thenReturn("1.2.3.4");
        Mockito.lenient().when(pool.getSourcePort()).thenReturn(6789);
        Mockito.lenient().when(pool.getAuthUserName()).thenReturn("cloudstack");
        Mockito.lenient().when(pool.getAuthSecret()).thenReturn("secret");
        Mockito.lenient().when(pool.getSourceDir()).thenReturn("cloudstack");
        return pool;
    }

    private LibvirtStorageAdaptor adaptorWith(RbdEncryption rbdEncryption) {
        LibvirtStorageAdaptor adaptor = Mockito.spy(new LibvirtStorageAdaptor(null));
        Mockito.doReturn(rbdEncryption).when(adaptor).createRbdEncryption();
        return adaptor;
    }

    @Test
    public void prepareLuksTemplateSnapshotCreatesBaseSnapshotWhenFirstDeployIsEncrypted() throws Exception {
        RbdImage image = templateImageWithSnapshots();
        RbdEncryption rbdEncryption = Mockito.mock(RbdEncryption.class);

        adaptorWith(rbdEncryption).prepareLuksTemplateSnapshot(image, rbdTemplate(), rbdPool(), LUKS_SNAP, TIMEOUT);

        // the base snapshot must exist, and still hold the template's original size and data, before the rewrite reads it
        InOrder order = Mockito.inOrder(image, rbdEncryption);
        order.verify(image).snapCreate(BASE_SNAP);
        order.verify(image).snapProtect(BASE_SNAP);
        order.verify(image).resize(TEMPLATE_SIZE + (16L << 20));
        order.verify(rbdEncryption).copyTemplateDense("1.2.3.4", 6789, "cloudstack", "secret", "cloudstack", "tmpl", BASE_SNAP, TIMEOUT);
        order.verify(image).snapCreate(LUKS_SNAP);
        order.verify(image).snapProtect(LUKS_SNAP);
    }

    @Test
    public void prepareLuksTemplateSnapshotUpgradesOldLuksSnapshotUsingExistingBase() throws Exception {
        RbdImage image = templateImageWithSnapshots(BASE_SNAP, "cloudstack-base-snap-luks");
        RbdEncryption rbdEncryption = Mockito.mock(RbdEncryption.class);

        adaptorWith(rbdEncryption).prepareLuksTemplateSnapshot(image, rbdTemplate(), rbdPool(), LUKS_SNAP, TIMEOUT);

        Mockito.verify(image, never()).snapCreate(BASE_SNAP);
        Mockito.verify(rbdEncryption).copyTemplateDense("1.2.3.4", 6789, "cloudstack", "secret", "cloudstack", "tmpl", BASE_SNAP, TIMEOUT);
        Mockito.verify(image).snapCreate(LUKS_SNAP);
    }

    @Test
    public void prepareLuksTemplateSnapshotDoesNothingWhenAlreadyPrepared() throws Exception {
        RbdImage image = templateImageWithSnapshots(BASE_SNAP, LUKS_SNAP);
        RbdEncryption rbdEncryption = Mockito.mock(RbdEncryption.class);

        adaptorWith(rbdEncryption).prepareLuksTemplateSnapshot(image, rbdTemplate(), rbdPool(), LUKS_SNAP, TIMEOUT);

        Mockito.verify(image, never()).resize(anyLong());
        Mockito.verify(image, never()).snapCreate(anyString());
        Mockito.verifyNoInteractions(rbdEncryption);
    }

    @Test
    public void prepareLuksTemplateSnapshotTakesNoSnapshotWhenRewriteFailsAndRetryStartsOver() throws Exception {
        RbdImage image = templateImageWithSnapshots();
        RbdEncryption rbdEncryption = Mockito.mock(RbdEncryption.class);
        Mockito.doThrow(new CloudRuntimeException("qemu-img failed")).doNothing().when(rbdEncryption)
                .copyTemplateDense(anyString(), Mockito.anyInt(), anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
        LibvirtStorageAdaptor adaptor = adaptorWith(rbdEncryption);

        Assert.assertThrows(CloudRuntimeException.class,
                () -> adaptor.prepareLuksTemplateSnapshot(image, rbdTemplate(), rbdPool(), LUKS_SNAP, TIMEOUT));
        // a snapshot of a half-rewritten template would be cloned from forever
        Mockito.verify(image, never()).snapCreate(LUKS_SNAP);

        // next deploy: the base snapshot is there now, the rewrite runs again and the snapshot is taken
        RbdSnapInfo baseSnapshot = new RbdSnapInfo();
        baseSnapshot.name = BASE_SNAP;
        Mockito.when(image.snapList()).thenReturn(List.of(baseSnapshot));
        adaptor.prepareLuksTemplateSnapshot(image, rbdTemplate(), rbdPool(), LUKS_SNAP, TIMEOUT);
        Mockito.verify(image, Mockito.times(1)).snapCreate(BASE_SNAP);
        Mockito.verify(rbdEncryption, Mockito.times(2)).copyTemplateDense("1.2.3.4", 6789, "cloudstack", "secret", "cloudstack", "tmpl", BASE_SNAP, TIMEOUT);
        Mockito.verify(image).snapCreate(LUKS_SNAP);
    }

    @Test
    public void prepareLuksTemplateSnapshotAcceptsAlreadyProtectedDuplicateSnapshots() throws Exception {
        RbdImage image = templateImageWithSnapshots();
        RbdEncryption rbdEncryption = Mockito.mock(RbdEncryption.class);
        Mockito.doThrow(new RbdException("File exists", -17)).when(image).snapCreate(anyString());
        Mockito.when(image.snapIsProtected(anyString())).thenReturn(true);

        adaptorWith(rbdEncryption).prepareLuksTemplateSnapshot(image, rbdTemplate(), rbdPool(), LUKS_SNAP, TIMEOUT);

        Mockito.verify(rbdEncryption).copyTemplateDense("1.2.3.4", 6789, "cloudstack", "secret", "cloudstack", "tmpl", BASE_SNAP, TIMEOUT);
    }

    @Test
    public void prepareLuksTemplateSnapshotFailsWhenSnapshotCannotBeTaken() throws Exception {
        RbdImage image = templateImageWithSnapshots(BASE_SNAP);
        RbdEncryption rbdEncryption = Mockito.mock(RbdEncryption.class);
        Mockito.doThrow(new RbdException("No space left", -28)).when(image).snapCreate(LUKS_SNAP);
        Mockito.when(image.snapIsProtected(LUKS_SNAP)).thenReturn(false);
        LibvirtStorageAdaptor adaptor = adaptorWith(rbdEncryption);

        Assert.assertThrows(RbdException.class,
                () -> adaptor.prepareLuksTemplateSnapshot(image, rbdTemplate(), rbdPool(), LUKS_SNAP, TIMEOUT));
    }
}
