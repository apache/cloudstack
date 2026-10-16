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
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
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

    private final String poolUuid = "pool-uuid";
    private final String volumePath = "1b0a8c3e-5f2d-4c67-9a10-3e5b7d9c2f41";
    private final long virtualSize = 1073741824L;

    LibvirtCheckVolumeCommandWrapper wrapper = new LibvirtCheckVolumeCommandWrapper();
    MockedConstruction<QemuImg> qemuImg;
    CheckVolumeCommand command;

    @Before
    public void setUp() {
        Mockito.when(storageFilerTO.getUuid()).thenReturn(poolUuid);
        Mockito.when(storageFilerTO.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        Mockito.when(storagePoolMgr.getStoragePool(Storage.StoragePoolType.Linstor, poolUuid)).thenReturn(storagePool);

        Map<String, String> info = new HashMap<>();
        info.put(QemuImg.FILE_FORMAT, "raw");
        qemuImg = Mockito.mockConstruction(QemuImg.class, (mock, context) ->
                Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.anyBoolean())).thenReturn(info));

        command = new CheckVolumeCommand();
        command.setSrcFile(volumePath);
        command.setStorageFilerTO(storageFilerTO);
    }

    @After
    public void tearDown() {
        qemuImg.close();
    }

    private void mockDisk() {
        Mockito.when(storagePool.getType()).thenReturn(Storage.StoragePoolType.Linstor);
        Mockito.when(storagePool.getPhysicalDisk(volumePath)).thenReturn(disk);
        Mockito.when(disk.getPath()).thenReturn("/dev/drbd/by-res/cs-" + volumePath + "/0");
        Mockito.when(disk.getFormat()).thenReturn(QemuImg.PhysicalDiskFormat.RAW);
        Mockito.when(disk.getVirtualSize()).thenReturn(virtualSize);
    }

    @Test
    public void testLinstorDiskInUseOnAnyHostIsReportedLocked() {
        Mockito.when(storagePool.isPhysicalDiskInUse(volumePath)).thenReturn(true);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer instanceof CheckVolumeAnswer);
        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("true", ((CheckVolumeAnswer) answer).getVolumeDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
        Mockito.verify(storagePool, Mockito.never()).connectPhysicalDisk(Mockito.eq(volumePath), Mockito.any());
        Mockito.verify(storagePool, Mockito.never()).disconnectPhysicalDisk(volumePath);
    }

    @Test
    public void testLinstorDiskConnectedForTheCheckIsDisconnectedAgain() {
        Mockito.when(storagePool.isPhysicalDiskInUse(volumePath)).thenReturn(false);
        Mockito.when(storagePool.connectPhysicalDisk(volumePath, null)).thenReturn(true);
        mockDisk();

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertTrue(answer instanceof CheckVolumeAnswer);
        Assert.assertTrue(answer.getResult());
        Assert.assertEquals(virtualSize, ((CheckVolumeAnswer) answer).getSize());
        Assert.assertEquals("raw", ((CheckVolumeAnswer) answer).getVolumeDetails().get(VolumeOnStorageTO.Detail.FILE_FORMAT));
        Assert.assertEquals("false", ((CheckVolumeAnswer) answer).getVolumeDetails().get(VolumeOnStorageTO.Detail.IS_LOCKED));
        Mockito.verify(storagePool).checkPhysicalDiskForImport(volumePath);
        Mockito.verify(storagePool).disconnectPhysicalDisk(volumePath);
    }

    @Test
    public void testLinstorDiskIsDisconnectedWhenTheCheckFails() {
        Mockito.when(storagePool.isPhysicalDiskInUse(volumePath)).thenReturn(false);
        Mockito.when(storagePool.connectPhysicalDisk(volumePath, null)).thenReturn(true);
        Mockito.when(storagePool.getPhysicalDisk(volumePath)).thenThrow(new CloudRuntimeException("device not found"));

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer.getResult());
        Mockito.verify(storagePool).disconnectPhysicalDisk(volumePath);
    }

    @Test
    public void testLinstorDiskOutsideThePoolFails() {
        Mockito.when(storagePool.isPhysicalDiskInUse(volumePath)).thenReturn(false);
        Mockito.doThrow(new CloudRuntimeException("not in resource group")).when(storagePool).checkPhysicalDiskForImport(volumePath);

        Answer answer = wrapper.execute(command, libvirtComputingResource);

        Assert.assertFalse(answer instanceof CheckVolumeAnswer);
        Assert.assertFalse(answer.getResult());
        Mockito.verify(storagePool, Mockito.never()).connectPhysicalDisk(Mockito.eq(volumePath), Mockito.any());
        Mockito.verify(storagePool, Mockito.never()).disconnectPhysicalDisk(volumePath);
    }
}
