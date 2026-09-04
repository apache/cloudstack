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
package org.apache.cloudstack.storage.vmsnapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.alert.AlertManager;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotDetailsVO;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class ScaleIOVMSnapshotStrategyTest {

    @Mock
    VMSnapshotHelper vmSnapshotHelper;
    @Mock
    UserVmDao userVmDao;
    @Mock
    VMSnapshotDao vmSnapshotDao;
    @Mock
    VMSnapshotDetailsDao vmSnapshotDetailsDao;
    @Mock
    ConfigurationDao configurationDao;
    @Mock
    VolumeDao volumeDao;
    @Mock
    DiskOfferingDao diskOfferingDao;
    @Mock
    PrimaryDataStoreDao storagePoolDao;
    @Mock
    StoragePoolDetailsDao storagePoolDetailsDao;
    @Mock
    AlertManager alertManager;

    private ScaleIOVMSnapshotStrategy strategy;

    @Before
    public void setup() {
        strategy = new ScaleIOVMSnapshotStrategy();
        ReflectionTestUtils.setField(strategy, "vmSnapshotHelper", vmSnapshotHelper);
        ReflectionTestUtils.setField(strategy, "userVmDao", userVmDao);
        ReflectionTestUtils.setField(strategy, "vmSnapshotDao", vmSnapshotDao);
        ReflectionTestUtils.setField(strategy, "vmSnapshotDetailsDao", vmSnapshotDetailsDao);
        ReflectionTestUtils.setField(strategy, "configurationDao", configurationDao);
        ReflectionTestUtils.setField(strategy, "volumeDao", volumeDao);
        ReflectionTestUtils.setField(strategy, "diskOfferingDao", diskOfferingDao);
        ReflectionTestUtils.setField(strategy, "storagePoolDao", storagePoolDao);
        ReflectionTestUtils.setField(strategy, "storagePoolDetailsDao", storagePoolDetailsDao);
        ReflectionTestUtils.setField(strategy, "alertManager", alertManager);
    }

    // ------------------------------------------------------------------
    // configure(String, Map)
    // ------------------------------------------------------------------

    @Test
    public void configureReadsWaitValueFromConfigurationDao() throws Exception {
        when(configurationDao.getValue("vmsnapshot.create.wait")).thenReturn("120");

        boolean result = strategy.configure("ScaleIOVMSnapshotStrategy", null);

        assertTrue(result);
        assertEquals(120, (int) ReflectionTestUtils.getField(strategy, "_wait"));
    }

    @Test
    public void configureDefaultsWaitTo1800WhenConfigValueUnset() throws Exception {
        when(configurationDao.getValue("vmsnapshot.create.wait")).thenReturn(null);

        boolean result = strategy.configure("ScaleIOVMSnapshotStrategy", null);

        assertTrue(result);
        assertEquals(1800, (int) ReflectionTestUtils.getField(strategy, "_wait"));
    }

    // ------------------------------------------------------------------
    // canHandle(VMSnapshot)
    // ------------------------------------------------------------------

    @Test
    public void canHandleThrowsWhenNoVolumesFoundForVm() {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        when(vmSnapshot.getUuid()).thenReturn("vmsnapshot-uuid");
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(null);

        try {
            strategy.canHandle(vmSnapshot);
            fail("Expected CloudRuntimeException");
        } catch (CloudRuntimeException expected) {
            assertTrue(expected.getMessage().contains("vmsnapshot-uuid"));
        }
    }

    @Test
    public void canHandleReturnsCantHandleWhenNonAllocatedAndNoSnapshotGroupDetail() {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        when(vmSnapshot.getId()).thenReturn(10L);
        when(vmSnapshot.getState()).thenReturn(VMSnapshot.State.Ready);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.emptyList());
        when(vmSnapshotDetailsDao.findDetails(10L, "SnapshotGroupId")).thenReturn(Collections.emptyList());

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    public void canHandleReturnsHighestWhenNonAllocatedWithSnapshotGroupDetailAndPowerFlexVolumes() {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        when(vmSnapshot.getId()).thenReturn(10L);
        when(vmSnapshot.getState()).thenReturn(VMSnapshot.State.Ready);

        VolumeObjectTO volumeTO = mock(VolumeObjectTO.class);
        when(volumeTO.getPoolId()).thenReturn(5L);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.singletonList(volumeTO));
        when(vmSnapshotHelper.getStoragePoolType(5L)).thenReturn(Storage.StoragePoolType.PowerFlex);
        when(vmSnapshotDetailsDao.findDetails(10L, "SnapshotGroupId"))
                .thenReturn(Collections.singletonList(mock(VMSnapshotDetailsVO.class)));

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.HIGHEST, result);
    }

    @Test
    public void canHandleReturnsHighestForAllocatedSnapshotWithNoVolumes() {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        when(vmSnapshot.getState()).thenReturn(VMSnapshot.State.Allocated);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.emptyList());

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.HIGHEST, result);
        // Allocated state skips the SnapshotGroupId detail lookup entirely.
        verify(vmSnapshotDetailsDao, never()).findDetails(anyLong(), anyString());
    }

    @Test
    public void canHandleReturnsCantHandleWhenVolumeIsNotOnPowerFlexPool() {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        when(vmSnapshot.getState()).thenReturn(VMSnapshot.State.Allocated);

        VolumeObjectTO volumeTO = mock(VolumeObjectTO.class);
        when(volumeTO.getPoolId()).thenReturn(5L);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.singletonList(volumeTO));
        when(vmSnapshotHelper.getStoragePoolType(5L)).thenReturn(Storage.StoragePoolType.NetworkFilesystem);

        StrategyPriority result = strategy.canHandle(vmSnapshot);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    // ------------------------------------------------------------------
    // canHandle(Long vmId, Long rootPoolId, boolean snapshotMemory)
    // ------------------------------------------------------------------

    @Test
    public void canHandleWithMemorySnapshotAlwaysReturnsCantHandle() {
        StrategyPriority result = strategy.canHandle(1L, 5L, true);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
        verify(vmSnapshotHelper, never()).getVolumeTOList(any());
    }

    @Test
    public void canHandleByIdsReturnsCantHandleWhenVolumeListIsNull() {
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(null);

        StrategyPriority result = strategy.canHandle(1L, 5L, false);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    public void canHandleByIdsReturnsCantHandleWhenVolumeListIsEmpty() {
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.emptyList());

        StrategyPriority result = strategy.canHandle(1L, 5L, false);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    public void canHandleByIdsReturnsCantHandleWhenPoolTypeIsNotPowerFlex() {
        Long poolId = 5L;
        VolumeObjectTO volumeTO = mock(VolumeObjectTO.class);
        when(volumeTO.getPoolId()).thenReturn(poolId);
        // getFormat() is never reached: poolType != PowerFlex short-circuits the "||" chain first.
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.singletonList(volumeTO));
        when(vmSnapshotHelper.getStoragePoolType(poolId)).thenReturn(Storage.StoragePoolType.NetworkFilesystem);

        StrategyPriority result = strategy.canHandle(1L, poolId, false);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    public void canHandleByIdsReturnsCantHandleWhenVolumeFormatIsNotRaw() {
        Long poolId = 5L;
        VolumeObjectTO volumeTO = mock(VolumeObjectTO.class);
        when(volumeTO.getPoolId()).thenReturn(poolId);
        when(volumeTO.getFormat()).thenReturn(ImageFormat.QCOW2);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.singletonList(volumeTO));
        when(vmSnapshotHelper.getStoragePoolType(poolId)).thenReturn(Storage.StoragePoolType.PowerFlex);

        StrategyPriority result = strategy.canHandle(1L, poolId, false);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    @Test
    public void canHandleByIdsReturnsCantHandleWhenVolumePoolIdDoesNotMatchRootPoolId() {
        VolumeObjectTO volumeTO = mock(VolumeObjectTO.class);
        when(volumeTO.getPoolId()).thenReturn(5L);
        when(volumeTO.getFormat()).thenReturn(ImageFormat.RAW);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.singletonList(volumeTO));
        when(vmSnapshotHelper.getStoragePoolType(5L)).thenReturn(Storage.StoragePoolType.PowerFlex);

        StrategyPriority result = strategy.canHandle(1L, 6L, false);

        assertEquals(StrategyPriority.CANT_HANDLE, result);
    }

    /**
        // Use two distinct Long instances with the same value (outside the Long autobox
        // cache range) for the volume's pool id and the rootPoolId argument: the production
        // code compares them with equals(), so value equality is what matters, not identity.
    */
    @Test
    public void canHandleByIdsReturnsHighestWhenPoolTypeFormatAndRootPoolIdAllMatch() {
        Long poolId = Long.valueOf(500);
        Long rootPoolId = Long.valueOf(500);
        VolumeObjectTO volumeTO = mock(VolumeObjectTO.class);
        when(volumeTO.getPoolId()).thenReturn(poolId);
        when(volumeTO.getFormat()).thenReturn(ImageFormat.RAW);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.singletonList(volumeTO));
        when(vmSnapshotHelper.getStoragePoolType(poolId)).thenReturn(Storage.StoragePoolType.PowerFlex);

        StrategyPriority result = strategy.canHandle(1L, rootPoolId, false);

        assertEquals(StrategyPriority.HIGHEST, result);
    }

    // ------------------------------------------------------------------
    // updateOperationFailed(VMSnapshot)
    // ------------------------------------------------------------------

    @Test
    public void updateOperationFailedDelegatesToVmSnapshotHelper() throws NoTransitionException {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);

        strategy.updateOperationFailed(vmSnapshot);

        verify(vmSnapshotHelper).vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);
    }

    @Test
    public void updateOperationFailedRethrowsNoTransitionException() throws NoTransitionException {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        NoTransitionException noTransitionException = new NoTransitionException("cannot transition");
        when(vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed))
                .thenThrow(noTransitionException);

        try {
            strategy.updateOperationFailed(vmSnapshot);
            fail("Expected NoTransitionException");
        } catch (NoTransitionException expected) {
            assertEquals(noTransitionException, expected);
        }
    }

    // ------------------------------------------------------------------
    // deleteVMSnapshotFromDB(VMSnapshot, boolean unmanage)
    // ------------------------------------------------------------------

    @Test
    public void deleteVMSnapshotFromDBRemovesSnapshotAndSkipsUsageEventWhenNotUnmanaged() {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        when(vmSnapshot.getId()).thenReturn(10L);

        UserVmVO userVm = mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(1L);
        when(userVmDao.findById(1L)).thenReturn(userVm);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.emptyList());
        when(vmSnapshotDao.remove(10L)).thenReturn(true);

        try (MockedStatic<UsageEventUtils> usageEventUtils = mockStatic(UsageEventUtils.class)) {
            boolean result = strategy.deleteVMSnapshotFromDB(vmSnapshot, false);

            assertTrue(result);
            verify(vmSnapshotDao).remove(10L);
            usageEventUtils.verify(() -> UsageEventUtils.publishUsageEvent(
                    eq(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY), anyLong(), anyLong(), anyLong(), anyString(),
                    any(), any(), any(), any(), anyString(), anyString(), any()), never());
        }
    }

    @Test
    public void deleteVMSnapshotFromDBPublishesOffPrimaryUsageEventWhenUnmanaged() {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        when(vmSnapshot.getId()).thenReturn(10L);
        when(vmSnapshot.getAccountId()).thenReturn(2L);
        when(vmSnapshot.getName()).thenReturn("vm-snapshot-name");
        when(vmSnapshot.getUuid()).thenReturn("vmsnapshot-uuid");

        UserVmVO userVm = mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(1L);
        when(userVm.getDataCenterId()).thenReturn(3L);
        when(userVmDao.findById(1L)).thenReturn(userVm);
        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.emptyList());
        when(vmSnapshotDao.remove(10L)).thenReturn(true);

        try (MockedStatic<UsageEventUtils> usageEventUtils = mockStatic(UsageEventUtils.class)) {
            boolean result = strategy.deleteVMSnapshotFromDB(vmSnapshot, true);

            assertTrue(result);
            verify(vmSnapshotDao).remove(10L);
            usageEventUtils.verify(() -> UsageEventUtils.publishUsageEvent(
                    eq(EventTypes.EVENT_VM_SNAPSHOT_OFF_PRIMARY), anyLong(), anyLong(), anyLong(), anyString(),
                    any(), any(), any(), any(), anyString(), anyString(), any()), times(1));
        }
    }

    @Test
    public void deleteVMSnapshotFromDBThrowsWhenExpungeRequestedTransitionFails() throws NoTransitionException {
        VMSnapshot vmSnapshot = mock(VMSnapshot.class);
        when(vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.ExpungeRequested))
                .thenThrow(new NoTransitionException("cannot transition"));

        try {
            strategy.deleteVMSnapshotFromDB(vmSnapshot, false);
            fail("Expected CloudRuntimeException");
        } catch (CloudRuntimeException expected) {
            // expected: state transition failure is wrapped in a CloudRuntimeException
        }
        verify(vmSnapshotDao, never()).remove(anyLong());
    }

    // ------------------------------------------------------------------
    // takeVMSnapshot(VMSnapshot)
    //
    // The happy path of takeVMSnapshot() is not exercised here: past the state
    // transition and volume bookkeeping, it calls getScaleIOClient(storagePool),
    // which reaches the real ScaleIOGatewayClientConnectionPool.getInstance()
    // singleton. That singleton cannot be swapped out in a plain Mockito unit
    // test, and constructing a real gateway client requires a live PowerFlex
    // gateway. Instead, this test drives the method far enough to reach that
    // call (using an empty volume list so no volume/disk-offering mocking is
    // needed) and asserts on the failure path: the singleton predictably throws
    // (its Preconditions check rejects the mock storage pool's default id of 0),
    // which is caught by takeVMSnapshot's own catch block, converted into a
    // CloudRuntimeException, and re-thrown after the finally block sends the
    // alert whose subject/body this PR changed to include vmSnapshot.toString().
    // ------------------------------------------------------------------

    @Test
    public void takeVMSnapshotSendsAlertWithSnapshotDetailsWhenGatewayClientCannotBeCreated() throws Exception {
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        when(vmSnapshot.toString()).thenReturn("VMSnapshot {id=10, name=snap1}");

        UserVmVO userVm = mock(UserVmVO.class);
        when(userVm.getId()).thenReturn(1L);
        when(userVm.getDisplayName()).thenReturn("test-vm");
        when(userVm.getDataCenterId()).thenReturn(3L);
        when(userVm.getPodIdToDeployIn()).thenReturn(4L);
        when(userVmDao.findById(1L)).thenReturn(userVm);

        when(vmSnapshotHelper.getVolumeTOList(1L)).thenReturn(Collections.emptyList());

        StoragePoolVO storagePool = mock(StoragePoolVO.class);
        when(vmSnapshotHelper.getStoragePoolForVM(userVm)).thenReturn(storagePool);
        when(vmSnapshotDao.findCurrentSnapshotByVmId(1L)).thenReturn(null);

        try {
            strategy.takeVMSnapshot(vmSnapshot);
            fail("Expected CloudRuntimeException from the (unreachable in unit tests) ScaleIO gateway client");
        } catch (CloudRuntimeException expected) {
            // expected: getScaleIOClient() cannot succeed without a real PowerFlex gateway
        }

        verify(vmSnapshotHelper).vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.OperationFailed);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertManager).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_VM_SNAPSHOT), eq(3L), eq(4L),
                subjectCaptor.capture(), bodyCaptor.capture());

        assertTrue(subjectCaptor.getValue().contains("Take snapshot failed"));
        assertTrue(subjectCaptor.getValue().contains("test-vm"));
        assertTrue(bodyCaptor.getValue().contains("test-vm"));
        assertTrue(bodyCaptor.getValue().contains("VMSnapshot {id=10, name=snap1}"));
    }

    @Test
    public void takeVMSnapshotThrowsCloudRuntimeExceptionWhenCreateRequestedTransitionFails() throws Exception {
        VMSnapshotVO vmSnapshot = mock(VMSnapshotVO.class);
        when(vmSnapshot.getVmId()).thenReturn(1L);
        UserVmVO userVm = mock(UserVmVO.class);
        when(userVmDao.findById(1L)).thenReturn(userVm);
        when(vmSnapshotHelper.vmSnapshotStateTransitTo(vmSnapshot, VMSnapshot.Event.CreateRequested))
                .thenThrow(new NoTransitionException("cannot transition"));

        try {
            strategy.takeVMSnapshot(vmSnapshot);
            fail("Expected CloudRuntimeException");
        } catch (CloudRuntimeException expected) {
            // expected: NoTransitionException on CreateRequested is wrapped
        }
        // The CreateRequested failure is thrown before the try/finally block that
        // sends the failure alert, so no alert should have been raised here.
        verify(alertManager, never()).sendAlert(any(), anyLong(), any(), anyString(), anyString());
    }
}
