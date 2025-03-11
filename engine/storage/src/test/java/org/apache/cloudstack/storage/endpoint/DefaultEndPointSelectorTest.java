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
package org.apache.cloudstack.storage.endpoint;


import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.vm.VirtualMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEndPointSelectorTest {

    @Mock
    private VirtualMachine virtualMachineMock;

    @Mock
    private VolumeInfo volumeInfoMock;

    @Mock
    private SnapshotInfo snapshotInfoMock;

    @Mock
    private DataStore datastoreMock;

    @Spy
    private DefaultEndPointSelector defaultEndPointSelectorSpy;

    @Before
    public void setup() {
        Mockito.doReturn(volumeInfoMock).when(snapshotInfoMock).getBaseVolume();
    }

    @Test
    public void getEndPointForBitmapRemovalTestVolumeIsNotAttached() {
        Mockito.doReturn(false).when(volumeInfoMock).isAttachedVM();
        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).select(volumeInfoMock, false);

        defaultEndPointSelectorSpy.getEndPointForBitmapRemoval(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(volumeInfoMock, false);
    }

    @Test
    public void getEndPointForBitmapRemovalTestVolumeIsAttachedHostIdIsSet() {
        Mockito.doReturn(true).when(volumeInfoMock).isAttachedVM();
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        long hostId = 12L;
        Mockito.doReturn(hostId).when(virtualMachineMock).getHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).getEndPointFromHostId(hostId);

        defaultEndPointSelectorSpy.getEndPointForBitmapRemoval(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(hostId);
    }

    @Test
    public void getEndPointForBitmapRemovalTestVolumeIsAttachedLastHostIdIsSet() {
        Mockito.doReturn(true).when(volumeInfoMock).isAttachedVM();
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();

        Mockito.doReturn(null).when(virtualMachineMock).getHostId();
        long lastHostId = 13L;
        Mockito.doReturn(lastHostId).when(virtualMachineMock).getLastHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).getEndPointFromHostId(lastHostId);

        defaultEndPointSelectorSpy.getEndPointForBitmapRemoval(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(lastHostId);
    }

    @Test
    public void getEndPointForBitmapRemovalTestVolumeIsAttachedNoHostIsSet() {
        Mockito.doReturn(true).when(volumeInfoMock).isAttachedVM();
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();

        Mockito.doReturn(null).when(virtualMachineMock).getHostId();
        Mockito.doReturn(null).when(virtualMachineMock).getLastHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).select(volumeInfoMock, false);

        defaultEndPointSelectorSpy.getEndPointForBitmapRemoval(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(volumeInfoMock, false);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeIsNotAttachedToVMAndSnapshotOnPrimary() {
        Mockito.doReturn(null).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(DataStoreRole.Primary).when(datastoreMock).getRole();
        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).select(snapshotInfoMock, false);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(snapshotInfoMock, false);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeIsNotAttachedToVMAndSnapshotOnSecondary() {
        Mockito.doReturn(null).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(DataStoreRole.Image).when(datastoreMock).getRole();
        long zoneId = 1L;
        Mockito.doReturn(zoneId).when(snapshotInfoMock).getDataCenterId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).selectRandom(zoneId, Hypervisor.HypervisorType.KVM);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).selectRandom(zoneId, Hypervisor.HypervisorType.KVM);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeAttachedToRunningVm() {
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(VirtualMachine.State.Running).when(virtualMachineMock).getState();
        long hostId = 12L;
        Mockito.doReturn(hostId).when(virtualMachineMock).getHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).getEndPointFromHostId(hostId);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(hostId);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeAttachedToStoppedVmAndLastHostIdIsSet() {
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        long hostId = 13L;
        Mockito.doReturn(hostId).when(virtualMachineMock).getLastHostId();

        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).getEndPointFromHostId(hostId);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).getEndPointFromHostId(hostId);
    }

    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeAttachedToStoppedVmAndLastHostIdIsNotSetAndSnapshotIsOnSecondary() {
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(DataStoreRole.Image).when(datastoreMock).getRole();
        Mockito.doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        Mockito.doReturn(null).when(virtualMachineMock).getLastHostId();
        long zoneId = 1L;
        Mockito.doReturn(zoneId).when(snapshotInfoMock).getDataCenterId();
        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).selectRandom(zoneId, Hypervisor.HypervisorType.KVM);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).selectRandom(zoneId, Hypervisor.HypervisorType.KVM);
    }


    @Test
    public void getEndPointForSnapshotOperationsInKvmTestVolumeAttachedToStoppedVmAndLastHostIdIsNotSetAndSnapshotIsOnPrimary() {
        Mockito.doReturn(virtualMachineMock).when(volumeInfoMock).getAttachedVM();
        Mockito.doReturn(datastoreMock).when(snapshotInfoMock).getDataStore();
        Mockito.doReturn(DataStoreRole.Primary).when(datastoreMock).getRole();
        Mockito.doReturn(VirtualMachine.State.Stopped).when(virtualMachineMock).getState();
        Mockito.doReturn(null).when(virtualMachineMock).getLastHostId();
        Mockito.doReturn(null).when(defaultEndPointSelectorSpy).select(snapshotInfoMock, false);

        defaultEndPointSelectorSpy.getEndPointForSnapshotOperationsInKvm(snapshotInfoMock, false);

        Mockito.verify(defaultEndPointSelectorSpy, Mockito.times(1)).select(snapshotInfoMock, false);
    }

}
