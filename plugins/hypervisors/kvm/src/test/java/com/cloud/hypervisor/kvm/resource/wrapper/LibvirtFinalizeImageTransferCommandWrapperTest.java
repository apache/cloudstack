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
package com.cloud.hypervisor.kvm.resource.wrapper;

import java.util.Map;

import org.apache.cloudstack.backup.FinalizeImageTransferCommand;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.ImageServerControlSocket;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtFinalizeImageTransferCommandWrapperTest {

    private LibvirtFinalizeImageTransferCommandWrapper wrapper;
    private FinalizeImageTransferCommand command;
    private LibvirtComputingResource resource;

    @Before
    public void setUp() {
        wrapper = new LibvirtFinalizeImageTransferCommandWrapper();
        command = Mockito.mock(FinalizeImageTransferCommand.class);
        resource = Mockito.mock(LibvirtComputingResource.class);
    }

    @Test
    public void testExecuteBlankTransferIdReturnsFailure() {
        Mockito.when(command.getTransferId()).thenReturn("");

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("transferId is empty.", answer.getDetails());
    }

    @Test
    public void testExecuteWithActiveTransfersReturnsSuccessWithoutStoppingServer() {
        Mockito.when(command.getTransferId()).thenReturn("tr-1");

        try (MockedStatic<ImageServerControlSocket> imageServerControlMock = Mockito.mockStatic(ImageServerControlSocket.class)) {
            imageServerControlMock.when(() -> ImageServerControlSocket.unregisterTransfer("tr-1")).thenReturn(2);

            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Image transfer finalized.", answer.getDetails());
        }
    }

    @Test
    public void testExecuteServerUnreachableReturnsSuccessAndForcesStop() {
        Mockito.when(command.getTransferId()).thenReturn("tr-2");

        try (MockedConstruction<Script> ignored = Mockito.mockConstruction(Script.class,
                (mock, context) -> Mockito.when(mock.execute()).thenReturn("inactive"));
             MockedStatic<ImageServerControlSocket> imageServerControlMock = Mockito.mockStatic(ImageServerControlSocket.class)) {
            imageServerControlMock.when(() -> ImageServerControlSocket.unregisterTransfer("tr-2")).thenReturn(-1);

            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Image transfer finalized (server unreachable, forced stop).", answer.getDetails());
        }
    }

    /**
     * Finalizes transfer tr-3, which uploaded a qcow2 image with the given qemu-img info to /dev/drbd1002, to be
     * converted into volume vol-3 of a pool whose new volumes read as zeros.
     */
    private Answer finalizeQcow2Upload(QemuImg qemu, Map<String, String> imageInfo) throws Exception {
        FinalizeImageTransferCommand cmd = new FinalizeImageTransferCommand("tr-3");
        cmd.setConvertInto("/dev/drbd1002", Storage.StoragePoolType.Linstor, "pool-3", "vol-3");
        cmd.setWait(600);
        KVMStoragePoolManager poolManager = Mockito.mock(KVMStoragePoolManager.class);
        KVMStoragePool pool = Mockito.mock(KVMStoragePool.class);
        KVMPhysicalDisk disk = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(resource.getStoragePoolMgr()).thenReturn(poolManager);
        Mockito.when(poolManager.getStoragePool(Storage.StoragePoolType.Linstor, "pool-3")).thenReturn(pool);
        Mockito.when(pool.getPhysicalDisk("vol-3")).thenReturn(disk);
        Mockito.lenient().when(disk.getPath()).thenReturn("/dev/drbd1001");
        Mockito.when(pool.isVolumeZeroInitialized("vol-3")).thenReturn(true);
        LibvirtFinalizeImageTransferCommandWrapper spyWrapper = Mockito.spy(wrapper);
        Mockito.doReturn(qemu).when(spyWrapper).createQemuImg(600000L, true);
        Mockito.when(qemu.info(Mockito.any(QemuImgFile.class))).thenReturn(imageInfo);

        try (MockedStatic<ImageServerControlSocket> imageServerControlMock = Mockito.mockStatic(ImageServerControlSocket.class)) {
            imageServerControlMock.when(() -> ImageServerControlSocket.unregisterTransfer("tr-3")).thenReturn(1);
            return spyWrapper.execute(cmd, resource);
        }
    }

    @Test
    public void testExecuteConvertsUploadedQcow2ImageIntoVolume() throws Exception {
        QemuImg qemu = Mockito.mock(QemuImg.class);

        Answer answer = finalizeQcow2Upload(qemu, Map.of(QemuImg.FILE_FORMAT, "qcow2"));

        Assert.assertTrue(answer.getResult());
        ArgumentCaptor<QemuImgFile> source = ArgumentCaptor.forClass(QemuImgFile.class);
        ArgumentCaptor<QemuImgFile> target = ArgumentCaptor.forClass(QemuImgFile.class);
        Mockito.verify(qemu).convert(source.capture(), target.capture(), Mockito.eq(true));
        Assert.assertEquals("/dev/drbd1002", source.getValue().getFileName());
        Assert.assertEquals(QemuImg.PhysicalDiskFormat.QCOW2, source.getValue().getFormat());
        Assert.assertEquals("/dev/drbd1001", target.getValue().getFileName());
        Assert.assertEquals(QemuImg.PhysicalDiskFormat.RAW, target.getValue().getFormat());
    }

    @Test
    public void testExecuteRejectsUploadedImageWithBackingFile() throws Exception {
        QemuImg qemu = Mockito.mock(QemuImg.class);

        Answer answer = finalizeQcow2Upload(qemu, Map.of(QemuImg.FILE_FORMAT, "qcow2", QemuImg.BACKING_FILE, "/etc/shadow"));

        Assert.assertFalse(answer.getResult());
        Mockito.verify(qemu, Mockito.never()).convert(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void testExecuteReportsFailedConversion() throws Exception {
        QemuImg qemu = Mockito.mock(QemuImg.class);
        Mockito.doThrow(new QemuImgException("No space left on device")).when(qemu)
                .convert(Mockito.any(), Mockito.any(), Mockito.anyBoolean());

        Answer answer = finalizeQcow2Upload(qemu, Map.of(QemuImg.FILE_FORMAT, "qcow2"));

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("No space left on device"));
    }
}
