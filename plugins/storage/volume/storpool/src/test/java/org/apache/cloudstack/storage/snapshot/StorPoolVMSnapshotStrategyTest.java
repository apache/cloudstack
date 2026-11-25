/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.snapshot;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotVO;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.util.StorPoolUtil;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.storage.vmsnapshot.VMSnapshotHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class StorPoolVMSnapshotStrategyTest {

    private static final long VM_ID = 1L;
    private static final long STORPOOL_POOL_ID = 10L;
    private static final long OTHER_POOL_ID = 20L;

    @Mock
    private VMSnapshotHelper vmSnapshotHelper;
    @Mock
    private PrimaryDataStoreDao storagePool;
    @Mock
    private UserVmDao userVmDao;

    @InjectMocks
    private StorPoolVMSnapshotStrategy strategy;

    private VolumeObjectTO mockVolumeOnPool(long poolId, String providerName) {
        VolumeObjectTO volumeTO = Mockito.mock(VolumeObjectTO.class);
        Mockito.when(volumeTO.getPoolId()).thenReturn(poolId);
        StoragePoolVO pool = Mockito.mock(StoragePoolVO.class);
        Mockito.when(pool.getStorageProviderName()).thenReturn(providerName);
        Mockito.when(storagePool.findById(poolId)).thenReturn(pool);
        return volumeTO;
    }

    private VMSnapshot mockVmSnapshot(VMSnapshot.State state, VMSnapshot.Type type) {
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(VM_ID);
        Mockito.lenient().when(vmSnapshot.getState()).thenReturn(state);
        Mockito.lenient().when(vmSnapshot.getType()).thenReturn(type);
        return vmSnapshot;
    }

    @Test(expected = InvalidParameterValueException.class)
    public void canHandleAllocationWithMemoryThrowsWhenAllVolumesOnStorPool() {
        VolumeObjectTO volumeTO = mockVolumeOnPool(STORPOOL_POOL_ID, StorPoolUtil.SP_PROVIDER_NAME);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(VM_ID)).thenReturn(List.of(volumeTO));

        strategy.canHandle(VM_ID, STORPOOL_POOL_ID, true);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void canHandleAllocationWithMemoryThrowsWhenOneVolumeOnStorPool() {
        VolumeObjectTO otherVolumeTO = mockVolumeOnPool(OTHER_POOL_ID, "DefaultPrimary");
        VolumeObjectTO storPoolVolumeTO = mockVolumeOnPool(STORPOOL_POOL_ID, StorPoolUtil.SP_PROVIDER_NAME);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(VM_ID)).thenReturn(List.of(otherVolumeTO, storPoolVolumeTO));

        strategy.canHandle(VM_ID, OTHER_POOL_ID, true);
    }

    @Test
    public void canHandleAllocationWithMemoryReturnsCantHandleWhenNoVolumeOnStorPool() {
        VolumeObjectTO volumeTO = mockVolumeOnPool(OTHER_POOL_ID, "DefaultPrimary");
        Mockito.when(vmSnapshotHelper.getVolumeTOList(VM_ID)).thenReturn(List.of(volumeTO));

        Assert.assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(VM_ID, OTHER_POOL_ID, true));
    }

    @Test
    public void canHandleAllocationWithoutMemoryReturnsHighestWhenAllVolumesOnStorPool() {
        VolumeObjectTO volumeTO = mockVolumeOnPool(STORPOOL_POOL_ID, StorPoolUtil.SP_PROVIDER_NAME);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(VM_ID)).thenReturn(List.of(volumeTO));

        Assert.assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(VM_ID, STORPOOL_POOL_ID, false));
    }

    @Test
    public void canHandleAllocatedDiskAndMemorySnapshotReturnsCantHandle() {
        VMSnapshot vmSnapshot = Mockito.mock(VMSnapshot.class);
        Mockito.when(vmSnapshot.getState()).thenReturn(VMSnapshot.State.Allocated);
        Mockito.when(vmSnapshot.getType()).thenReturn(VMSnapshot.Type.DiskAndMemory);

        Assert.assertEquals(StrategyPriority.CANT_HANDLE, strategy.canHandle(vmSnapshot));
        Mockito.verifyNoInteractions(vmSnapshotHelper);
    }

    @Test
    public void canHandleAllocatedDiskSnapshotReturnsHighestWhenAllVolumesOnStorPool() {
        VolumeObjectTO volumeTO = mockVolumeOnPool(STORPOOL_POOL_ID, StorPoolUtil.SP_PROVIDER_NAME);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(VM_ID)).thenReturn(List.of(volumeTO));

        Assert.assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(mockVmSnapshot(VMSnapshot.State.Allocated, VMSnapshot.Type.Disk)));
    }

    @Test
    public void canHandleExistingDiskAndMemorySnapshotReturnsHighestWhenAllVolumesOnStorPool() {
        VolumeObjectTO volumeTO = mockVolumeOnPool(STORPOOL_POOL_ID, StorPoolUtil.SP_PROVIDER_NAME);
        Mockito.when(vmSnapshotHelper.getVolumeTOList(VM_ID)).thenReturn(List.of(volumeTO));

        Assert.assertEquals(StrategyPriority.HIGHEST, strategy.canHandle(mockVmSnapshot(VMSnapshot.State.Ready, VMSnapshot.Type.DiskAndMemory)));
    }

    private void assertRevertOfDiskAndMemorySnapshotIsBlocked(VirtualMachine.State vmState) {
        UserVmVO userVm = Mockito.mock(UserVmVO.class);
        Mockito.lenient().when(userVm.getState()).thenReturn(vmState);
        Mockito.when(userVmDao.findById(VM_ID)).thenReturn(userVm);
        VMSnapshotVO vmSnapshot = Mockito.mock(VMSnapshotVO.class);
        Mockito.when(vmSnapshot.getVmId()).thenReturn(VM_ID);
        Mockito.when(vmSnapshot.getType()).thenReturn(VMSnapshot.Type.DiskAndMemory);

        Assert.assertThrows(CloudRuntimeException.class, () -> strategy.revertVMSnapshot(vmSnapshot));
        Mockito.verifyNoInteractions(vmSnapshotHelper);
    }

    @Test
    public void revertDiskAndMemorySnapshotIsBlockedWhenVmIsRunning() {
        assertRevertOfDiskAndMemorySnapshotIsBlocked(VirtualMachine.State.Running);
    }

    @Test
    public void revertDiskAndMemorySnapshotIsBlockedWhenVmIsStopped() {
        assertRevertOfDiskAndMemorySnapshotIsBlocked(VirtualMachine.State.Stopped);
    }
}
