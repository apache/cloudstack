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

import com.cloud.utils.Pair;
import org.apache.cloudstack.backup.RestoreKbossBackupAnswer;
import org.apache.cloudstack.backup.RestoreKbossBackupCommand;
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
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.storage.KVMStoragePool;
import com.cloud.hypervisor.kvm.storage.KVMStoragePoolManager;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtRestoreKbossBackupCommandWrapperTest {

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

    @Mock
    private RestoreKbossBackupCommand cmdMock;

    @Spy
    @InjectMocks
    private LibvirtRestoreKbossBackupCommandWrapper libvirtRestoreKbossBackupCommandWrapperSpy;

    @Test
    public void executeTestLibvirtException() throws LibvirtException, QemuImgException {
        doReturn(primaryDataStoreToMock).when(backupDeltaTOMock).getDataStore();
        doReturn(Set.of(new Pair<>(backupDeltaTOMock, volumeObjectToMock1))).when(cmdMock).getBackupAndVolumePairs();
        doReturn(null).when(libvirtRestoreKbossBackupCommandWrapperSpy).mountSecondaryStorages(any(), any(), any(), any());
        doThrow(new QemuImgException("asd")).when(libvirtRestoreKbossBackupCommandWrapperSpy).restoreVolumes(any(), any(), any(), anyBoolean(), anyInt());

        RestoreKbossBackupAnswer answer = (RestoreKbossBackupAnswer)libvirtRestoreKbossBackupCommandWrapperSpy.execute(cmdMock, libvirtComputingResourceMock);
        assertFalse(answer.getResult());

    }
}
