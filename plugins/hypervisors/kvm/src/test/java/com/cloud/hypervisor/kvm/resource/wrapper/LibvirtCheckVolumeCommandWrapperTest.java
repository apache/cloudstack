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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVolumeAnswer;
import com.cloud.agent.api.CheckVolumeCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtCheckVolumeCommandWrapperTest {

    @Mock
    LibvirtComputingResource libvirtComputingResource;
    @Mock
    KVMStoragePoolManager storagePoolMgr;
    @Mock
    KVMStoragePool storagePool;
    @Mock
    StorageFilerTO storageFilerTO;
    @Mock
    KVMPhysicalDisk disk;

    @Spy
    LibvirtCheckVolumeCommandWrapper wrapper = new LibvirtCheckVolumeCommandWrapper();

    MockedConstruction<QemuImg> qemuImg;

    private final String poolUuid = "pool-uuid";
    private final String srcFile = "rbd-volume-uuid";
    private final long virtualSize = 21474836480L; // 20 GiB

    private Map<String, String> qemuInfo;

    @Before
    public void setUp() {
        qemuInfo = new HashMap<>();
        qemuInfo.put(QemuImg.FILE_FORMAT, "raw");
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
    }

    @After
    public void tearDown() {
        if (qemuImg != null) {
            qemuImg.close();
        }
    }

    private void mockRbdPool() {
        Mockito.when(storageFilerTO.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(storageFilerTO.getUuid()).thenReturn(poolUuid);
        Mockito.when(storagePoolMgr.getStoragePool(Storage.StoragePoolType.RBD, poolUuid)).thenReturn(storagePool);
        Mockito.when(storagePool.getType()).thenReturn(Storage.StoragePoolType.RBD);
        Mockito.when(storagePool.getPhysicalDisk(srcFile)).thenReturn(disk);
        Mockito.when(disk.getPath()).thenReturn(srcFile);
        Mockito.when(disk.getFormat()).thenReturn(QemuImg.PhysicalDiskFormat.RAW);
        // values consumed by KVMPhysicalDisk.RBDStringBuilder when building the RBD URI
        Mockito.when(storagePool.getSourceHost()).thenReturn("10.0.0.10");
        Mockito.when(storagePool.getSourcePort()).thenReturn(6789);
        Mockito.when(storagePool.getAuthUserName()).thenReturn(null);
        Mockito.when(storagePool.getDetails()).thenReturn(null);
    }

    private void mockLinstorPool() {
        Mockito.when(storageFilerTO.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(storageFilerTO.getUuid()).thenReturn(poolUuid);
        Mockito.when(storagePoolMgr.getStoragePool(Storage.StoragePoolType.Linstor, poolUuid)).thenReturn(storagePool);
        Mockito.when(storagePool.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(storagePool.getPhysicalDisk(srcFile)).thenReturn(disk);
        // Linstor resolves the resource to a local /dev/drbd block device path.
        Mockito.when(disk.getPath()).thenReturn("/dev/drbd/by-res/" + srcFile + "/0");
        Mockito.when(disk.getFormat()).thenReturn(QemuImg.PhysicalDiskFormat.RAW);
    }

    private CheckVolumeCommand buildCommand() {
        CheckVolumeCommand command = new CheckVolumeCommand();
        command.setSrcFile(srcFile);
        command.setStorageFilerTO(storageFilerTO);
        return command;
    }

    @Test
    public void testRbdVolumeReturnsSuccessWithVirtualSizeAndDetails() {
        mockRbdPool();
        Mockito.when(disk.getVirtualSize()).thenReturn(virtualSize);
        qemuImg = Mockito.mockConstruction(QemuImg.class, (mock, context) ->
                Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.anyBoolean())).thenReturn(qemuInfo));

        Answer answer = wrapper.execute(buildCommand(), libvirtComputingResource);

        Assert.assertTrue(answer instanceof CheckVolumeAnswer);
        Assert.assertTrue(answer.getResult());
        CheckVolumeAnswer checkVolumeAnswer = (CheckVolumeAnswer) answer;
        Assert.assertEquals(virtualSize, checkVolumeAnswer.getSize());
        Map<VolumeOnStorageTO.Detail, String> details = checkVolumeAnswer.getVolumeDetails();
        Assert.assertNotNull(details);
        Assert.assertEquals("raw", details.get(VolumeOnStorageTO.Detail.FILE_FORMAT));
        Assert.assertEquals("false", details.get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void testRbdVolumeInspectedThroughRbdUri() throws Exception {
        mockRbdPool();
        Mockito.when(disk.getVirtualSize()).thenReturn(virtualSize);
        qemuImg = Mockito.mockConstruction(QemuImg.class, (mock, context) ->
                Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.anyBoolean())).thenReturn(qemuInfo));

        wrapper.execute(buildCommand(), libvirtComputingResource);

        ArgumentCaptor<QemuImgFile> fileCaptor = ArgumentCaptor.forClass(QemuImgFile.class);
        Mockito.verify(qemuImg.constructed().get(0), Mockito.atLeastOnce())
                .info(fileCaptor.capture(), Mockito.anyBoolean());
        Assert.assertTrue("qemu-img should be pointed at the RBD URI",
                fileCaptor.getValue().getFileName().startsWith("rbd:" + srcFile));
    }

    @Test
    public void testLinstorVolumeReturnsSuccessInspectedViaDevicePath() throws Exception {
        mockLinstorPool();
        Mockito.when(disk.getVirtualSize()).thenReturn(virtualSize);
        qemuImg = Mockito.mockConstruction(QemuImg.class, (mock, context) ->
                Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.anyBoolean())).thenReturn(qemuInfo));

        Answer answer = wrapper.execute(buildCommand(), libvirtComputingResource);

        Assert.assertTrue(answer instanceof CheckVolumeAnswer);
        Assert.assertTrue("Linstor raw volume should validate (not rejected as non-qcow2)", answer.getResult());
        Assert.assertEquals(virtualSize, ((CheckVolumeAnswer) answer).getSize());
        // A Linstor volume is a local block device: qemu-img must inspect the
        // /dev/drbd device path directly, never build an rbd: URI.
        ArgumentCaptor<QemuImgFile> fileCaptor = ArgumentCaptor.forClass(QemuImgFile.class);
        Mockito.verify(qemuImg.constructed().get(0), Mockito.atLeastOnce())
                .info(fileCaptor.capture(), Mockito.anyBoolean());
        String fileName = fileCaptor.getValue().getFileName();
        Assert.assertTrue("qemu-img should point at the /dev/drbd device path, got " + fileName,
                fileName.startsWith("/dev/drbd/"));
        Assert.assertFalse("Linstor must not build an rbd: URI", fileName.startsWith("rbd:"));
    }

    @Test
    public void testLinstorVolumeInUseOnAnotherNodeIsReportedLocked() throws Exception {
        mockLinstorPool();
        Mockito.when(disk.getVirtualSize()).thenReturn(virtualSize);
        Mockito.when(disk.getName()).thenReturn(srcFile);
        // qemu-img info succeeds, so the host-local file lock is clear ...
        qemuImg = Mockito.mockConstruction(QemuImg.class, (mock, context) ->
                Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.anyBoolean())).thenReturn(qemuInfo));
        // ... but Linstor reports the DRBD resource in use on another node.
        Mockito.when(storagePool.getVolumeInUseNode(srcFile)).thenReturn("node-2");

        Answer answer = wrapper.execute(buildCommand(), libvirtComputingResource);

        Assert.assertTrue(answer instanceof CheckVolumeAnswer);
        Map<VolumeOnStorageTO.Detail, String> details = ((CheckVolumeAnswer) answer).getVolumeDetails();
        Assert.assertEquals("cluster-wide in-use must mark the volume locked so adoption refuses it",
                "true", details.get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void testRbdVolumeReportsLockedWhenInfoProbeFails() {
        mockRbdPool();
        Mockito.when(disk.getVirtualSize()).thenReturn(virtualSize);
        qemuImg = Mockito.mockConstruction(QemuImg.class, (mock, context) -> {
            // secure=true uses force-share and succeeds; secure=false is the lock probe
            Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.eq(true))).thenReturn(qemuInfo);
            Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.eq(false))).thenReturn(null);
        });

        Answer answer = wrapper.execute(buildCommand(), libvirtComputingResource);

        Assert.assertTrue(answer instanceof CheckVolumeAnswer);
        Assert.assertTrue(answer.getResult());
        Map<VolumeOnStorageTO.Detail, String> details = ((CheckVolumeAnswer) answer).getVolumeDetails();
        Assert.assertEquals("true", details.get(VolumeOnStorageTO.Detail.IS_LOCKED));
    }

    @Test
    public void testRbdVolumeWithoutInfoReturnsInvalid() {
        mockRbdPool();
        qemuImg = Mockito.mockConstruction(QemuImg.class, (mock, context) ->
                Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.anyBoolean())).thenReturn(null));

        Answer answer = wrapper.execute(buildCommand(), libvirtComputingResource);

        Assert.assertTrue(answer instanceof CheckVolumeAnswer);
        Assert.assertFalse(answer.getResult());
        CheckVolumeAnswer checkVolumeAnswer = (CheckVolumeAnswer) answer;
        Assert.assertEquals(0, checkVolumeAnswer.getSize());
        Assert.assertNull(checkVolumeAnswer.getVolumeDetails());
    }

    @Test
    public void testUnsupportedStoragePoolReturnsPlainAnswer() {
        Mockito.when(storageFilerTO.getType()).thenReturn(Storage.StoragePoolType.PowerFlex);
        Mockito.when(storageFilerTO.getUuid()).thenReturn(poolUuid);

        Answer answer = wrapper.execute(buildCommand(), libvirtComputingResource);

        Assert.assertFalse(answer instanceof CheckVolumeAnswer);
        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("Unsupported Storage Pool", answer.getDetails());
    }
}
