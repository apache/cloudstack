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
import com.cloud.agent.api.GetVolumesOnStorageAnswer;
import com.cloud.agent.api.GetVolumesOnStorageCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMPhysicalDisk;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.storage.Storage;
import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtGetVolumesOnStorageCommandWrapperTest {

    @Mock
    LibvirtComputingResource libvirtComputingResource;

    @Mock
    KVMStoragePoolManager storagePoolMgr;
    @Mock
    KVMStoragePool storagePool;

    @Mock
    StorageFilerTO pool;
    @Mock
    Map<String, String> qemuImgInfo;


    private final Storage.StoragePoolType poolType = Storage.StoragePoolType.NetworkFilesystem;
    private final String poolUuid = "pool-uuid";
    private final String volumePath = "volume-path";

    private final String backingFilePath = "backing file path";
    private final String backingFileFormat = "QCOW2";
    private final String clusterSize = "4096";
    private final String fileFormat = "QCOW2";
    private final String encrypted = "yes";
    private final String diskNamePrefix = "disk-";

    @Spy
    LibvirtGetVolumesOnStorageCommandWrapper libvirtGetVolumesOnStorageCommandWrapper = new LibvirtGetVolumesOnStorageCommandWrapper();

    MockedConstruction<QemuImg> qemuImg;
    MockedConstruction<VolumeOnStorageTO> volumeOnStorageTOMock;

    @Before
    public void setUp() {
        Mockito.when(pool.getUuid()).thenReturn(poolUuid);
        Mockito.when(pool.getType()).thenReturn(poolType);
        Mockito.when(libvirtComputingResource.getStoragePoolMgr()).thenReturn(storagePoolMgr);
        Mockito.when(storagePoolMgr.getStoragePool(poolType, poolUuid, true)).thenReturn(storagePool);

        qemuImg = Mockito.mockConstruction(QemuImg.class, (mock, context) -> {
            Mockito.when(mock.info(Mockito.any(QemuImgFile.class), Mockito.eq(true))).thenReturn(qemuImgInfo);
        });
        volumeOnStorageTOMock = Mockito.mockConstruction(VolumeOnStorageTO.class);
    }

    @After
    public void tearDown() {
        qemuImg.close();
        volumeOnStorageTOMock.close();
    }

    @Test
    public void testLibvirtGetVolumesOnStorageCommandWrapperForAllVolumes() {
        GetVolumesOnStorageCommand command = new GetVolumesOnStorageCommand(pool, null, diskNamePrefix);
        List<KVMPhysicalDisk> physicalDisks = new ArrayList<>();
        int numberDisks = 3;
        for (int i = 0; i < numberDisks; i++) {
            KVMPhysicalDisk disk = Mockito.mock(KVMPhysicalDisk.class);
            Mockito.when(disk.getName()).thenReturn(diskNamePrefix + (numberDisks - i));
            Mockito.when(disk.getFormat()).thenReturn(QemuImg.PhysicalDiskFormat.QCOW2);
            Mockito.when(disk.getQemuEncryptFormat()).thenReturn(QemuObject.EncryptFormat.LUKS);
            physicalDisks.add(disk);
        }
        Mockito.when(storagePool.listPhysicalDisks()).thenReturn(physicalDisks);

        Answer answer = libvirtGetVolumesOnStorageCommandWrapper.execute(command, libvirtComputingResource);
        Assert.assertTrue(answer instanceof GetVolumesOnStorageAnswer);
        Assert.assertTrue(answer.getResult());
        List<VolumeOnStorageTO> volumes = ((GetVolumesOnStorageAnswer) answer).getVolumes();
        Assert.assertEquals(numberDisks, volumes.size());
        volumeOnStorageTOMock.constructed().forEach(s -> Mockito.verify(s, times(1)).setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS.toString()));
    }

    @Test
    public void testLibvirtGetVolumesOnStorageCommandWrapperForVolume() {
        KVMPhysicalDisk disk = Mockito.mock(KVMPhysicalDisk.class);
        Mockito.when(disk.getPath()).thenReturn(volumePath);
        Mockito.when(disk.getFormat()).thenReturn(QemuImg.PhysicalDiskFormat.QCOW2);
        Mockito.when(disk.getQemuEncryptFormat()).thenReturn(QemuObject.EncryptFormat.LUKS);
        Mockito.when(storagePool.getPhysicalDisk(volumePath)).thenReturn(disk);
        Mockito.when(storagePool.getType()).thenReturn(poolType);

        Mockito.when(qemuImgInfo.get(QemuImg.BACKING_FILE)).thenReturn(backingFilePath);
        Mockito.when(qemuImgInfo.get(QemuImg.BACKING_FILE_FORMAT)).thenReturn(backingFileFormat);
        Mockito.when(qemuImgInfo.get(QemuImg.CLUSTER_SIZE)).thenReturn(clusterSize);
        Mockito.when(qemuImgInfo.get(QemuImg.FILE_FORMAT)).thenReturn(fileFormat);
        Mockito.when(qemuImgInfo.get(QemuImg.ENCRYPTED)).thenReturn(encrypted);

        GetVolumesOnStorageCommand command = new GetVolumesOnStorageCommand(pool, volumePath, null);
        Answer answer = libvirtGetVolumesOnStorageCommandWrapper.execute(command, libvirtComputingResource);
        Assert.assertTrue(answer instanceof GetVolumesOnStorageAnswer);
        Assert.assertTrue(answer.getResult());
        List<VolumeOnStorageTO> volumes = ((GetVolumesOnStorageAnswer) answer).getVolumes();
        Assert.assertEquals(1, volumes.size());

        VolumeOnStorageTO volumeOnStorageTO = volumeOnStorageTOMock.constructed().get(0);
        Mockito.verify(volumeOnStorageTO).setQemuEncryptFormat(QemuObject.EncryptFormat.LUKS.toString());
        Mockito.verify(volumeOnStorageTO).addDetail(VolumeOnStorageTO.Detail.BACKING_FILE, backingFilePath);
        Mockito.verify(volumeOnStorageTO).addDetail(VolumeOnStorageTO.Detail.BACKING_FILE_FORMAT, backingFileFormat);
        Mockito.verify(volumeOnStorageTO).addDetail(VolumeOnStorageTO.Detail.CLUSTER_SIZE, clusterSize);
        Mockito.verify(volumeOnStorageTO).addDetail(VolumeOnStorageTO.Detail.FILE_FORMAT, fileFormat);
        Mockito.verify(volumeOnStorageTO).addDetail(VolumeOnStorageTO.Detail.IS_ENCRYPTED, "true");
        Mockito.verify(volumeOnStorageTO).addDetail(VolumeOnStorageTO.Detail.IS_LOCKED, "false");
    }
}
