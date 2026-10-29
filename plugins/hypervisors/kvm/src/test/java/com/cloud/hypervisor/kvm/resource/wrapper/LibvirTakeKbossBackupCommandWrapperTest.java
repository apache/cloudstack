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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.backup.TakeKbossBackupAnswer;
import org.apache.cloudstack.backup.TakeKbossBackupCommand;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;
import org.apache.cloudstack.storage.to.KbossTO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.LibvirtException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.BackupException;

@RunWith(MockitoJUnitRunner.class)
public class LibvirTakeKbossBackupCommandWrapperTest {

    @Mock
    private TakeKbossBackupCommand takeKbossBackupCommandMock;

    @Mock
    private LibvirtComputingResource libvirtComputingResourceMock;

    @Mock
    private KVMStoragePoolManager kvmStoragePoolManagerMock;

    @Mock
    private KVMStoragePool kvmStoragePool1;

    @Mock
    private KVMStoragePool kvmStoragePool2;

    @Mock
    private KVMStoragePool kvmStoragePool3;

    @Mock
    private KbossTO kbossTO1;

    @Mock
    private KbossTO kbossTO2;

    @Mock
    private VolumeObjectTO volumeObjectToMock1;

    @Mock
    private VolumeObjectTO volumeObjectToMock2;

    @Mock
    private DeltaMergeTreeTO deltaMergeTreeToMock;

    @Mock
    private BackupDeltaTO backupDeltaTOMock;

    @Mock
    private PrimaryDataStoreTO primaryDataStoreToMock;

    @Spy
    @InjectMocks
    private LibvirtTakeKbossBackupCommandWrapper libvirtTakeKbossBackupCommandWrapperSpy;

    private String volUuid1 = "uuid1";

    private String volUuid2 = "uuid2";

    private String deltaPath1 = "deltapath1";

    private String deltaPath2 = "deltapath2";

    private String secondaryUrl = "nfs://1.1.1.2:/mnt";

    private String secondaryUrl2 = "nfs://2.2.2.2:/mnt2";

    @Test
    public void executeTestBackupException() {
        doReturn(List.of()).when(takeKbossBackupCommandMock).getKbossTOs();
        doThrow(new BackupException("tst", false)).when(libvirtComputingResourceMock).createDiskOnlyVMSnapshotOfStoppedVm(any(), any());

        TakeKbossBackupAnswer answer = (TakeKbossBackupAnswer)libvirtTakeKbossBackupCommandWrapperSpy.execute(takeKbossBackupCommandMock, libvirtComputingResourceMock);

        assertFalse(answer.getResult());
        assertFalse(answer.isVmConsistent());
    }

    @Test
    public void executeTestSuccessStoppedVm() {
        doReturn(List.of()).when(takeKbossBackupCommandMock).getKbossTOs();
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).backupVolumes(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).cleanupVm(any(), any(), any(), any(), anyBoolean(), any());

        TakeKbossBackupAnswer answer = (TakeKbossBackupAnswer)libvirtTakeKbossBackupCommandWrapperSpy.execute(takeKbossBackupCommandMock, libvirtComputingResourceMock);

        verify(libvirtComputingResourceMock).createDiskOnlyVMSnapshotOfStoppedVm(any(), any());
        verify(libvirtTakeKbossBackupCommandWrapperSpy).backupVolumes(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        verify(libvirtTakeKbossBackupCommandWrapperSpy).cleanupVm(any(), any(), any(), any(), anyBoolean(), any());
        assertTrue(answer.getResult());
    }

    @Test
    public void executeTestSuccessRunningVm() {
        doReturn(List.of()).when(takeKbossBackupCommandMock).getKbossTOs();
        doReturn(true).when(takeKbossBackupCommandMock).isRunningVM();
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).backupVolumes(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).cleanupVm(any(), any(), any(), any(), anyBoolean(), any());

        TakeKbossBackupAnswer answer = (TakeKbossBackupAnswer)libvirtTakeKbossBackupCommandWrapperSpy.execute(takeKbossBackupCommandMock, libvirtComputingResourceMock);

        verify(libvirtComputingResourceMock).createDiskOnlyVmSnapshotForRunningVm(any(), any(), any(), anyBoolean());
        verify(libvirtTakeKbossBackupCommandWrapperSpy).backupVolumes(any(), any(), any(), any(), any(), any(), anyBoolean(), any());
        verify(libvirtTakeKbossBackupCommandWrapperSpy).cleanupVm(any(), any(), any(), any(), anyBoolean(), any());
        assertTrue(answer.getResult());
    }

    @Test (expected = BackupException.class)
    public void backupVolumesTestRecoverIfExceptionThrown() {
        List<Pair<VolumeObjectTO, String>> volumeTosAndNewPaths = new ArrayList<>();
        Map<String, Pair<String, Long>> mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize = new HashMap<>();
        String secondaryUrl = "nfs://1.1.1.2:/mnt";

        doReturn(secondaryUrl).when(takeKbossBackupCommandMock).getImageStoreUrl();
        doThrow(new RuntimeException("odasij")).when(kbossTO1).getVolumeObjectTO();

        libvirtTakeKbossBackupCommandWrapperSpy.backupVolumes(takeKbossBackupCommandMock, libvirtComputingResourceMock, kvmStoragePoolManagerMock, List.of(kbossTO1),
                volumeTosAndNewPaths, "tst", false, mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize);


        verify(libvirtTakeKbossBackupCommandWrapperSpy).recoverPreviousVmStateAndDeletePartialBackup(libvirtComputingResourceMock, volumeTosAndNewPaths, "tst", false,
                mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize, kvmStoragePoolManagerMock, secondaryUrl);
    }

    @Test
    public void backupVolumesTestHappyPath() {
        setupKbossTos();
        List<Pair<VolumeObjectTO, String>> volumeTosAndNewPaths = new ArrayList<>();
        Map<String, Pair<String, Long>> mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize = new HashMap<>();

        doReturn(100).when(takeKbossBackupCommandMock).getWait();
        doReturn(secondaryUrl).when(takeKbossBackupCommandMock).getImageStoreUrl();
        Pair<String, Long> pair1 = new Pair<>("p1", 10L);
        doReturn(pair1).when(libvirtTakeKbossBackupCommandWrapperSpy).copyBackupDeltaToSecondary(eq(kvmStoragePoolManagerMock), eq(kbossTO1), anyList(),
                        eq(secondaryUrl), anyInt());
        Pair<String, Long> pair2 = new Pair<>("p2", 13L);
        doReturn(pair2).when(libvirtTakeKbossBackupCommandWrapperSpy).copyBackupDeltaToSecondary(eq(kvmStoragePoolManagerMock), eq(kbossTO2), anyList(),
                eq(secondaryUrl), anyInt());

        libvirtTakeKbossBackupCommandWrapperSpy.backupVolumes(takeKbossBackupCommandMock, libvirtComputingResourceMock, kvmStoragePoolManagerMock, List.of(kbossTO1, kbossTO2),
                volumeTosAndNewPaths, "tst", false, mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize);

        verify(libvirtTakeKbossBackupCommandWrapperSpy, never()).recoverPreviousVmStateAndDeletePartialBackup(libvirtComputingResourceMock, volumeTosAndNewPaths, "tst", false,
                mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize, kvmStoragePoolManagerMock, secondaryUrl);
        assertEquals(pair1, mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize.get(volUuid1));
        assertEquals(pair2, mapVolumeUuidToDeltaPathOnSecondaryAndDeltaSize.get(volUuid2));
    }

    @Test
    public void cleanupVmTestEndOfChain() {
        setupKbossTos();
        Map<String, String> mapVolumeUUidToNewVolumePath = new HashMap<>();
        String path1 = "path1";
        String path2 = "path2";
        String path3 = "path3";
        String vmName = "ttt";
        doReturn(path1).when(volumeObjectToMock1).getPath();
        doReturn(path2).when(volumeObjectToMock2).getPath();
        doReturn(deltaPath1).when(kbossTO1).getDeltaPathOnPrimary();
        doReturn(deltaPath2).when(kbossTO2).getDeltaPathOnPrimary();
        doReturn(deltaMergeTreeToMock).when(kbossTO1).getDeltaMergeTreeTO();
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).mergeBackupDelta(any(), any(), any(), any(), anyBoolean(), any(), anyBoolean());
        doReturn(true).when(takeKbossBackupCommandMock).isEndChain();
        doReturn(volumeObjectToMock1).when(deltaMergeTreeToMock).getChild();
        doReturn(backupDeltaTOMock).when(deltaMergeTreeToMock).getParent();
        doReturn(path3).when(backupDeltaTOMock).getPath();
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).endChainForVolume(libvirtComputingResourceMock, volumeObjectToMock1, vmName, true, volUuid1, path3);

        libvirtTakeKbossBackupCommandWrapperSpy.cleanupVm(takeKbossBackupCommandMock, libvirtComputingResourceMock, List.of(kbossTO1, kbossTO2), vmName, true,
                mapVolumeUUidToNewVolumePath);

        verify(volumeObjectToMock1).setPath(deltaPath1);
        verify(volumeObjectToMock2).setPath(deltaPath2);
        verify(libvirtTakeKbossBackupCommandWrapperSpy).mergeBackupDelta(eq(libvirtComputingResourceMock), eq(deltaMergeTreeToMock), eq(volumeObjectToMock1), eq(vmName), eq(true),
                eq(volUuid1), anyBoolean());
        verify(libvirtTakeKbossBackupCommandWrapperSpy).endChainForVolume(libvirtComputingResourceMock, volumeObjectToMock1, vmName, true, volUuid1, path3);
        assertEquals(path3, mapVolumeUUidToNewVolumePath.get(volUuid1));
        assertEquals(path2, mapVolumeUUidToNewVolumePath.get(volUuid2));
    }

    @Test
    public void cleanupVmTestNotEndOfChainAndNotIsolated() {
        setupKbossTos();
        Map<String, String> mapVolumeUUidToNewVolumePath = new HashMap<>();
        String vmName = "ttt";
        doReturn(deltaPath1).when(kbossTO1).getDeltaPathOnPrimary();
        doReturn(deltaPath2).when(kbossTO2).getDeltaPathOnPrimary();

        libvirtTakeKbossBackupCommandWrapperSpy.cleanupVm(takeKbossBackupCommandMock, libvirtComputingResourceMock, List.of(kbossTO1, kbossTO2), vmName, true,
                mapVolumeUUidToNewVolumePath);

        verify(volumeObjectToMock1).setPath(deltaPath1);
        verify(volumeObjectToMock2).setPath(deltaPath2);
        assertEquals(deltaPath1, mapVolumeUUidToNewVolumePath.get(volUuid1));
        assertEquals(deltaPath2, mapVolumeUUidToNewVolumePath.get(volUuid2));
    }

    @Test
    public void copyBackupDeltaToSecondaryTest() throws LibvirtException, QemuImgException {
        String parentPath = "parentPath";
        String volumePath = "volPath";
        String parentBackupFullPath = "parentBackupFullPath";
        String backupDeltaFullPathOnPrimary1 = "backupDeltaFullPathOnPrimary1";
        String backupDeltaFullPathOnSecondary1 = "backupDeltaFullPathOnSecondary1";
        String randomPath1 = "random";
        String backupDeltaFullPathOnPrimary2 = "backupDeltaFullPathOnPrimary2";

        doReturn(volumeObjectToMock1).when(kbossTO1).getVolumeObjectTO();
        doReturn(volumePath).when(volumeObjectToMock1).getPath();
        doReturn(volUuid1).when(volumeObjectToMock1).getUuid();
        doReturn(parentPath).when(kbossTO1).getPathBackupParentOnSecondary();
        doReturn(new ArrayList<>(List.of(deltaPath2))).when(kbossTO1).getVmSnapshotDeltaPaths();
        doReturn(deltaPath1).when(kbossTO1).getDeltaPathOnSecondary();
        doReturn(kvmStoragePool1).when(kvmStoragePoolManagerMock).getStoragePoolByURI(secondaryUrl);
        doReturn(kvmStoragePool2).when(kvmStoragePoolManagerMock).getStoragePoolByURI(secondaryUrl2);
        doReturn(primaryDataStoreToMock).when(volumeObjectToMock1).getDataStore();
        doReturn(kvmStoragePool3).when(kvmStoragePoolManagerMock).getStoragePool(any(), any());

        doReturn(parentBackupFullPath).when(kvmStoragePool2).getLocalPathFor(parentPath);
        doReturn(backupDeltaFullPathOnPrimary1).when(kvmStoragePool3).getLocalPathFor(deltaPath2);
        doReturn(backupDeltaFullPathOnSecondary1).when(kvmStoragePool1).getLocalPathFor(deltaPath1);
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).convertDeltaToSecondary(backupDeltaFullPathOnPrimary1, backupDeltaFullPathOnSecondary1, parentBackupFullPath, volUuid1, 100000);

        doReturn(randomPath1).when(libvirtTakeKbossBackupCommandWrapperSpy).getRelativePathOnSecondaryForBackup(anyLong(), anyLong(), any());
        doReturn("random2").when(kvmStoragePool1).getLocalPathFor(randomPath1);
        doReturn(backupDeltaFullPathOnPrimary2).when(kvmStoragePool3).getLocalPathFor(volumePath);
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).convertDeltaToSecondary(eq(backupDeltaFullPathOnPrimary2), eq("random2"), eq(backupDeltaFullPathOnSecondary1),
                any(), anyInt());

        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).commitTopDeltaOnBaseBackupOnSecondaryIfNeeded(randomPath1, deltaPath1, kvmStoragePool1,
                backupDeltaFullPathOnSecondary1, 100000);
        doNothing().when(libvirtTakeKbossBackupCommandWrapperSpy).removeTemporaryDeltas(any(), anyBoolean());

        try(MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.size(any())).thenReturn(1000L);
            Pair<String, Long> result = libvirtTakeKbossBackupCommandWrapperSpy.copyBackupDeltaToSecondary(kvmStoragePoolManagerMock, kbossTO1, List.of(secondaryUrl2),
                    secondaryUrl, 100000);

            assertEquals(deltaPath1, result.first());
            assertEquals(Long.valueOf(1000L), result.second());
        }

        verify(libvirtTakeKbossBackupCommandWrapperSpy).convertDeltaToSecondary(backupDeltaFullPathOnPrimary1, backupDeltaFullPathOnSecondary1, parentBackupFullPath, volUuid1, 100000);
        verify(libvirtTakeKbossBackupCommandWrapperSpy).convertDeltaToSecondary(eq(backupDeltaFullPathOnPrimary2), eq("random2"), eq(backupDeltaFullPathOnSecondary1),
                any(), anyInt());
        verify(libvirtTakeKbossBackupCommandWrapperSpy).commitTopDeltaOnBaseBackupOnSecondaryIfNeeded(randomPath1, deltaPath1, kvmStoragePool1,
                backupDeltaFullPathOnSecondary1, 100000);
        verify(libvirtTakeKbossBackupCommandWrapperSpy).removeTemporaryDeltas(any(), anyBoolean());
    }

    @Test
    public void removeTemporaryDeltasTestResultTrue() {
        ArrayList<String> input = new ArrayList<>(List.of("a", "b"));

        try(MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            libvirtTakeKbossBackupCommandWrapperSpy.removeTemporaryDeltas(input, true);

            filesMockedStatic.verify(() -> Files.deleteIfExists(any()), Mockito.times(1));
        }
    }

    @Test
    public void removeTemporaryDeltasTestResultFalse() {
        ArrayList<String> input = new ArrayList<>(List.of("a", "b"));

        try(MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            libvirtTakeKbossBackupCommandWrapperSpy.removeTemporaryDeltas(input, false);

            filesMockedStatic.verify(() -> Files.deleteIfExists(any()), Mockito.times(2));
        }
    }

    @Test
    public void removeTemporaryDeltasTestExceptionIsIgnored() {
        ArrayList<String> input = new ArrayList<>(List.of("a", "b"));

        try(MockedStatic<Files> filesMockedStatic = Mockito.mockStatic(Files.class)) {
            filesMockedStatic.when(() -> Files.deleteIfExists(any())).thenThrow(new IOException("das"));
            libvirtTakeKbossBackupCommandWrapperSpy.removeTemporaryDeltas(input, false);

            filesMockedStatic.verify(() -> Files.deleteIfExists(any()), Mockito.times(2));
        }
    }

    @Test (expected = BackupException.class)
    public void mergeBackupDeltaTestThrowsException() throws LibvirtException, QemuImgException {
        doThrow(new QemuImgException("a")).when(libvirtComputingResourceMock).mergeDeltaForRunningVm(any(), any(), any());

        libvirtTakeKbossBackupCommandWrapperSpy.mergeBackupDelta(libvirtComputingResourceMock, deltaMergeTreeToMock, volumeObjectToMock1, "ttt", true, volUuid1, false);
    }

    @Test
    public void mergeBackupDeltaTestRunningVm() throws LibvirtException, QemuImgException {
        libvirtTakeKbossBackupCommandWrapperSpy.mergeBackupDelta(libvirtComputingResourceMock, deltaMergeTreeToMock, volumeObjectToMock1, "ttt", true, volUuid1, false);

        verify(libvirtComputingResourceMock).mergeDeltaForRunningVm(deltaMergeTreeToMock, "ttt", volumeObjectToMock1);
    }

    @Test
    public void mergeBackupDeltaTestStoppedVm() throws LibvirtException, QemuImgException, IOException {
        libvirtTakeKbossBackupCommandWrapperSpy.mergeBackupDelta(libvirtComputingResourceMock, deltaMergeTreeToMock, volumeObjectToMock1, "ttt", false, volUuid1, false);

        verify(libvirtComputingResourceMock).mergeDeltaForStoppedVm(deltaMergeTreeToMock);
    }

    @Test
    public void mergeBackupDeltaTestStoppedVmCountNewestDeltaAsGrandChild() throws LibvirtException, QemuImgException, IOException {
        libvirtTakeKbossBackupCommandWrapperSpy.mergeBackupDelta(libvirtComputingResourceMock, deltaMergeTreeToMock, volumeObjectToMock1, "ttt", false, volUuid1, true);

        verify(deltaMergeTreeToMock).addGrandChild(volumeObjectToMock1);
        verify(libvirtComputingResourceMock).mergeDeltaForStoppedVm(deltaMergeTreeToMock);
    }

    private void setupKbossTos() {
        doReturn(volumeObjectToMock1).when(kbossTO1).getVolumeObjectTO();
        doReturn(volUuid1).when(volumeObjectToMock1).getUuid();
        doReturn(volumeObjectToMock2).when(kbossTO2).getVolumeObjectTO();
        doReturn(volUuid2).when(volumeObjectToMock2).getUuid();
    }
}
