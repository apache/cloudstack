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
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(PowerMockRunner.class)
public class LibvirtRevertSnapshotCommandWrapperTest {

    LibvirtRevertSnapshotCommandWrapper libvirtRevertSnapshotCommandWrapperSpy = Mockito.spy(LibvirtRevertSnapshotCommandWrapper.class);

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
    @PrepareForTest(LibvirtRevertSnapshotCommandWrapper.class)
    public void validateReplaceVolumeWithSnapshotReplaceFiles() throws IOException {
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.copy(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any(CopyOption.class))).thenReturn(pathMock);
        libvirtRevertSnapshotCommandWrapperSpy.replaceVolumeWithSnapshot("test", "test");
    }

    @Test (expected = IOException.class)
    @PrepareForTest(LibvirtRevertSnapshotCommandWrapper.class)
    public void validateReplaceVolumeWithSnapshotThrowsIOException() throws IOException {
        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.copy(Mockito.any(Path.class), Mockito.any(Path.class), Mockito.any(CopyOption.class))).thenThrow(IOException.class);
        libvirtRevertSnapshotCommandWrapperSpy.replaceVolumeWithSnapshot("test", "test");
    }

    @Test
    @PrepareForTest(LibvirtRevertSnapshotCommandWrapper.class)
    public void validateGetSnapshotPathExistsOnPrimaryStorage() {
        String snapshotPath = "test";
        Pair<String, SnapshotObjectTO> expectedResult = new Pair<>(snapshotPath, snapshotObjectToPrimaryMock);

        Mockito.doReturn(snapshotPath).when(snapshotObjectToPrimaryMock).getPath();

        PowerMockito.mockStatic(Files.class);
        PowerMockito.when(Files.exists(Mockito.any(Path.class), Mockito.any())).thenReturn(true);

        Pair<String, SnapshotObjectTO> result = libvirtRevertSnapshotCommandWrapperSpy.getSnapshot(snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock,
                kvmStoragePoolPrimaryMock, kvmStoragePoolSecondaryMock);

        Assert.assertEquals(expectedResult.first(), result.first());
        Assert.assertEquals(expectedResult.second(), result.second());
    }

    @Test
    @PrepareForTest(LibvirtRevertSnapshotCommandWrapper.class)
    public void validateGetSnapshotPathExistsOnSecondaryStorage() {
        String snapshotPath = "test";
        Pair<String, SnapshotObjectTO> expectedResult = new Pair<>(snapshotPath, snapshotObjectToSecondaryMock);

        PowerMockito.mockStatic(Files.class, Paths.class);
        PowerMockito.when(Paths.get(Mockito.any(), Mockito.any())).thenReturn(pathMock);
        PowerMockito.when(Files.exists(Mockito.any(Path.class), Mockito.any())).thenReturn(false);

        Mockito.doReturn(snapshotPath).when(snapshotObjectToSecondaryMock).getPath();
        Mockito.doReturn(snapshotPath).when(libvirtRevertSnapshotCommandWrapperSpy).getFullPathAccordingToStorage(Mockito.any(), Mockito.any());

        Pair<String, SnapshotObjectTO> result = libvirtRevertSnapshotCommandWrapperSpy.getSnapshot(snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock,
                kvmStoragePoolPrimaryMock, kvmStoragePoolSecondaryMock);

        Assert.assertEquals(expectedResult.first(), result.first());
        Assert.assertEquals(expectedResult.second(), result.second());
    }

    @Test (expected = CloudRuntimeException.class)
    @PrepareForTest(LibvirtRevertSnapshotCommandWrapper.class)
    public void validateGetSnapshotPathDoesNotExistsOnSecondaryStorageThrows() {
        PowerMockito.mockStatic(Files.class, Paths.class);
        PowerMockito.when(Paths.get(Mockito.any(), Mockito.any())).thenReturn(pathMock);
        PowerMockito.when(Files.exists(Mockito.any(Path.class), Mockito.any())).thenReturn(false);

        Mockito.doReturn(null).when(snapshotObjectToSecondaryMock).getPath();

        libvirtRevertSnapshotCommandWrapperSpy.getSnapshot(snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock,
                kvmStoragePoolPrimaryMock, kvmStoragePoolSecondaryMock);
    }

    @Test
    public void validateRevertVolumeToSnapshotReplaceSuccessfully() throws IOException {
        Mockito.doReturn(volumeObjectToMock).when(snapshotObjectToSecondaryMock).getVolume();
        Mockito.doReturn("").when(libvirtRevertSnapshotCommandWrapperSpy).getFullPathAccordingToStorage(Mockito.any(), Mockito.anyString());
        Mockito.doReturn(pairStringSnapshotObjectToMock).when(libvirtRevertSnapshotCommandWrapperSpy).getSnapshot(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doNothing().when(libvirtRevertSnapshotCommandWrapperSpy).replaceVolumeWithSnapshot(Mockito.any(), Mockito.any());
        libvirtRevertSnapshotCommandWrapperSpy.revertVolumeToSnapshot(snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock, dataStoreToMock, kvmStoragePoolPrimaryMock,
                kvmStoragePoolSecondaryMock);
    }

    @Test (expected = CloudRuntimeException.class)
    public void validateRevertVolumeToSnapshotReplaceVolumeThrowsIOException() throws IOException {
        Mockito.doReturn(volumeObjectToMock).when(snapshotObjectToSecondaryMock).getVolume();
        Mockito.doReturn("").when(libvirtRevertSnapshotCommandWrapperSpy).getFullPathAccordingToStorage(Mockito.any(), Mockito.anyString());
        Mockito.doReturn(pairStringSnapshotObjectToMock).when(libvirtRevertSnapshotCommandWrapperSpy).getSnapshot(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doThrow(IOException.class).when(libvirtRevertSnapshotCommandWrapperSpy).replaceVolumeWithSnapshot(Mockito.any(), Mockito.any());
        libvirtRevertSnapshotCommandWrapperSpy.revertVolumeToSnapshot(snapshotObjectToPrimaryMock, snapshotObjectToSecondaryMock, dataStoreToMock, kvmStoragePoolPrimaryMock,
                kvmStoragePoolSecondaryMock);
    }
}
