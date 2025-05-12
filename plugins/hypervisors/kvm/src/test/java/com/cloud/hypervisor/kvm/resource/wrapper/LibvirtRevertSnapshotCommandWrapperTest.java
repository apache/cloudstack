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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtRevertSnapshotCommandWrapperTest {

    @Spy
    LibvirtRevertSnapshotCommandWrapper libvirtRevertSnapshotCommandWrapperSpy;

    @Mock
    KVMStoragePool kvmStoragePoolPrimaryMock;

    @Mock
    KVMStoragePool kvmStoragePoolSecondaryMock;

    @Mock
    Path pathMock;

    @Mock
    SnapshotObjectTO snapshotObjectToPrimaryMock;

    @Mock
    SnapshotObjectTO snapshotObjectToSecondaryMock;

    @Mock
    Pair<String, SnapshotObjectTO> pairStringSnapshotObjectToMock;

    @Mock
    DataStoreTO dataStoreToMock;

    @Mock
    VolumeObjectTO volumeObjectToMock;

    @Mock
    LibvirtComputingResource resourceMock;

    @Test
    public void validateGetFullPathAccordingToStorage() {
        String snapshotPath = "snapshotPath";
        String storagePath = "storagePath";
        String expectedResult = String.format("%s%s%s", storagePath, File.separator, snapshotPath);

        Mockito.doReturn(storagePath).when(kvmStoragePoolPrimaryMock).getLocalPath();
        String result = libvirtRevertSnapshotCommandWrapperSpy.getFullPathAccordingToStorage(kvmStoragePoolPrimaryMock, snapshotPath);

        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void validateGetSnapshotPathExistsOnPrimaryStorage() {
        String snapshotPath = "test";
        Pair<String, SnapshotObjectTO> expectedResult = new Pair<>(snapshotPath, snapshotObjectToPrimaryMock);

        Mockito.doReturn(snapshotPath).when(snapshotObjectToPrimaryMock).getPath();

        try (MockedStatic<Files> ignored = Mockito.mockStatic(Files.class)) {
            Mockito.when(Files.exists(Mockito.any(Path.class))).thenReturn(true);

            Pair<String, SnapshotObjectTO> result = libvirtRevertSnapshotCommandWrapperSpy.getSnapshot(
                    snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock,
                    kvmStoragePoolPrimaryMock, kvmStoragePoolSecondaryMock);

            Assert.assertEquals(expectedResult.first(), result.first());
            Assert.assertEquals(expectedResult.second(), result.second());
        }
    }

    @Test
    public void validateGetSnapshotPathExistsOnSecondaryStorage() {
        String snapshotPath = "test";
        Pair<String, SnapshotObjectTO> expectedResult = new Pair<>(snapshotPath, snapshotObjectToSecondaryMock);

        try (MockedStatic<Files> ignored = Mockito.mockStatic(Files.class); MockedStatic<Paths> ignored2 = Mockito.mockStatic(Paths.class)) {

            Mockito.when(Paths.get(Mockito.any(), Mockito.any())).thenReturn(pathMock);
            Mockito.when(Files.exists(Mockito.any(Path.class), Mockito.any())).thenReturn(false);

            Mockito.doReturn(snapshotPath).when(snapshotObjectToSecondaryMock).getPath();
            Mockito.doReturn(snapshotPath).when(libvirtRevertSnapshotCommandWrapperSpy).getFullPathAccordingToStorage(
                    Mockito.any(), Mockito.any());

            Pair<String, SnapshotObjectTO> result = libvirtRevertSnapshotCommandWrapperSpy.getSnapshot(
                    snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock,
                    kvmStoragePoolPrimaryMock, kvmStoragePoolSecondaryMock);

            Assert.assertEquals(expectedResult.first(), result.first());
            Assert.assertEquals(expectedResult.second(), result.second());
        }
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateGetSnapshotPathDoesNotExistsOnSecondaryStorageThrows() {
        try (MockedStatic<Files> ignored = Mockito.mockStatic(Files.class); MockedStatic<Paths> ignored2 = Mockito.mockStatic(Paths.class)) {

            Mockito.when(Paths.get(Mockito.any(), Mockito.any())).thenReturn(pathMock);
            Mockito.when(Files.exists(Mockito.any(Path.class), Mockito.any())).thenReturn(false);

            Mockito.doReturn(null).when(snapshotObjectToSecondaryMock).getPath();

            libvirtRevertSnapshotCommandWrapperSpy.getSnapshot(snapshotObjectToPrimaryMock,
                    snapshotObjectToSecondaryMock,
                    kvmStoragePoolPrimaryMock, kvmStoragePoolSecondaryMock);
        }
    }

    @Test
    public void validateRevertVolumeToSnapshotReplaceSuccessfully() throws LibvirtException, QemuImgException {
        Mockito.doReturn(volumeObjectToMock).when(snapshotObjectToSecondaryMock).getVolume();
        Mockito.doReturn(pairStringSnapshotObjectToMock).when(libvirtRevertSnapshotCommandWrapperSpy).getSnapshot(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(libvirtRevertSnapshotCommandWrapperSpy).replaceVolumeWithSnapshot(Mockito.any(), Mockito.any());
        libvirtRevertSnapshotCommandWrapperSpy.revertVolumeToSnapshot(kvmStoragePoolSecondaryMock, snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock, kvmStoragePoolPrimaryMock, resourceMock
        );
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateRevertVolumeToSnapshotReplaceVolumeThrowsQemuImgException() throws LibvirtException, QemuImgException {
        Mockito.doReturn(volumeObjectToMock).when(snapshotObjectToSecondaryMock).getVolume();
        Mockito.doReturn(pairStringSnapshotObjectToMock).when(libvirtRevertSnapshotCommandWrapperSpy).getSnapshot(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doThrow(QemuImgException.class).when(libvirtRevertSnapshotCommandWrapperSpy).replaceVolumeWithSnapshot(Mockito.any(), Mockito.any());
        libvirtRevertSnapshotCommandWrapperSpy.revertVolumeToSnapshot(kvmStoragePoolSecondaryMock, snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock, kvmStoragePoolPrimaryMock, resourceMock
        );
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateRevertVolumeToSnapshotReplaceVolumeThrowsLibvirtException() throws LibvirtException, QemuImgException {
        Mockito.doReturn(volumeObjectToMock).when(snapshotObjectToSecondaryMock).getVolume();
        Mockito.doReturn(pairStringSnapshotObjectToMock).when(libvirtRevertSnapshotCommandWrapperSpy).getSnapshot(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doThrow(LibvirtException.class).when(libvirtRevertSnapshotCommandWrapperSpy).replaceVolumeWithSnapshot(Mockito.any(), Mockito.any());
        libvirtRevertSnapshotCommandWrapperSpy.revertVolumeToSnapshot(kvmStoragePoolSecondaryMock, snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock, kvmStoragePoolPrimaryMock, resourceMock
        );
    }
}
