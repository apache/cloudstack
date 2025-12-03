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
package com.cloud.storage.snapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.org.Grouping;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotZoneDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotManagerImplTest {
    @Mock
    AccountDao accountDao;
    @Mock
    SnapshotDao snapshotDao;
    @Mock
    AccountManager accountManager;
    @Mock
    SnapshotService snapshotService;
    @Mock
    SnapshotDataFactory snapshotFactory;
    @Mock
    ResourceLimitService resourceLimitService;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    SnapshotDataStoreDao snapshotStoreDao;
    @Mock
    DataStoreManager dataStoreManager;
    @Mock
    SnapshotZoneDao snapshotZoneDao;
    @Mock
    VolumeDao volumeDao;
    @InjectMocks
    SnapshotManagerImpl snapshotManager = new SnapshotManagerImpl();

    @Test
    public void testGetSnapshotZoneImageStoreValid() {
        final long snapshotId = 1L;
        final long zoneId = 1L;
        final long storeId = 1L;
        SnapshotDataStoreVO ref = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref.getDataStoreId()).thenReturn(storeId);
        Mockito.when(ref.getRole()).thenReturn(DataStoreRole.Image);
        List<SnapshotDataStoreVO> snapshotStoreList = List.of(Mockito.mock(SnapshotDataStoreVO.class), ref);
        Mockito.when(dataStoreManager.getStoreZoneId(storeId, DataStoreRole.Image)).thenReturn(zoneId);
        Mockito.when(dataStoreManager.getDataStore(storeId, DataStoreRole.Image)).thenReturn(Mockito.mock(DataStore.class));
        Mockito.when(snapshotStoreDao.listReadyBySnapshot(snapshotId, DataStoreRole.Image)).thenReturn(snapshotStoreList);
        DataStore store = snapshotManager.getSnapshotZoneImageStore(snapshotId, zoneId);
        Assert.assertNotNull(store);
    }

    @Test
    public void testGetSnapshotZoneImageStoreNull() {
        final long snapshotId = 1L;
        final long zoneId = 1L;
        final long storeId = 1L;
        SnapshotDataStoreVO ref = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref.getDataStoreId()).thenReturn(storeId);
        Mockito.when(ref.getRole()).thenReturn(DataStoreRole.Image);
        List<SnapshotDataStoreVO> snapshotStoreList = List.of(ref);
        Mockito.when(dataStoreManager.getStoreZoneId(storeId, DataStoreRole.Image)).thenReturn(100L);
        Mockito.when(snapshotStoreDao.listReadyBySnapshot(snapshotId, DataStoreRole.Image)).thenReturn(snapshotStoreList);
        DataStore store = snapshotManager.getSnapshotZoneImageStore(snapshotId, zoneId);
        Assert.assertNull(store);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetStoreRefsAndZonesForSnapshotDeleteException() {
        final long snapshotId = 1L;
        final long zoneId = 1L;
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(null);
        snapshotManager.getStoreRefsAndZonesForSnapshotDelete(snapshotId, zoneId);
    }

    @Test
    public void testGetStoreRefsAndZonesForSnapshotDeleteMultiZones() {
        final long snapshotId = 1L;
        SnapshotDataStoreVO ref = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref.getDataStoreId()).thenReturn(1L);
        Mockito.when(ref.getRole()).thenReturn(DataStoreRole.Image);
        SnapshotDataStoreVO ref1 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref1.getDataStoreId()).thenReturn(2L);
        Mockito.when(ref1.getRole()).thenReturn(DataStoreRole.Image);
        List<SnapshotDataStoreVO> snapshotStoreList = List.of(ref, ref1);
        Mockito.when(snapshotStoreDao.findBySnapshotId(snapshotId)).thenReturn(snapshotStoreList);
        Mockito.when(dataStoreManager.getStoreZoneId(1L, DataStoreRole.Image)).thenReturn(100L);
        Mockito.when(dataStoreManager.getStoreZoneId(2L, DataStoreRole.Image)).thenReturn(101L);
        Pair<List<SnapshotDataStoreVO>, List<Long>> pair = snapshotManager.getStoreRefsAndZonesForSnapshotDelete(snapshotId, null);
        Assert.assertNotNull(pair.first());
        Assert.assertNotNull(pair.second());
        Assert.assertEquals(snapshotStoreList.size(), pair.first().size());
        Assert.assertEquals(2, pair.second().size());
    }

    @Test
    public void testGetStoreRefsAndZonesForSnapshotDeleteSingle() {
        final long snapshotId = 1L;
        final long zoneId = 1L;
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(Mockito.mock(DataCenterVO.class));
        SnapshotDataStoreVO ref = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref.getDataStoreId()).thenReturn(1L);
        Mockito.when(ref.getRole()).thenReturn(DataStoreRole.Image);
        SnapshotDataStoreVO ref1 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref1.getDataStoreId()).thenReturn(2L);
        Mockito.when(ref1.getRole()).thenReturn(DataStoreRole.Primary);
        SnapshotDataStoreVO ref2 = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref2.getDataStoreId()).thenReturn(3L);
        Mockito.when(ref2.getRole()).thenReturn(DataStoreRole.Image);
        List<SnapshotDataStoreVO> snapshotStoreList = List.of(ref, ref1, ref2);
        Mockito.when(snapshotStoreDao.findBySnapshotId(snapshotId)).thenReturn(snapshotStoreList);
        Mockito.when(dataStoreManager.getStoreZoneId(1L, DataStoreRole.Image)).thenReturn(zoneId);
        Mockito.when(dataStoreManager.getStoreZoneId(2L, DataStoreRole.Primary)).thenReturn(zoneId);
        Mockito.when(dataStoreManager.getStoreZoneId(3L, DataStoreRole.Image)).thenReturn(2L);
        Pair<List<SnapshotDataStoreVO>, List<Long>> pair = snapshotManager.getStoreRefsAndZonesForSnapshotDelete(snapshotId, zoneId);
        Assert.assertNotNull(pair.first());
        Assert.assertNotNull(pair.second());
        Assert.assertEquals(snapshotStoreList.size() - 1, pair.first().size());
        Assert.assertEquals(1, pair.second().size());
    }
    @Test
    public void testValidatePolicyZonesNoZones() {
        snapshotManager.validatePolicyZones(null, Mockito.mock(VolumeVO.class), Mockito.mock(Account.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatePolicyZonesVolumeEdgeZone() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getDataCenterId()).thenReturn(1L);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getType()).thenReturn(DataCenter.Type.Edge);
        Mockito.when(dataCenterDao.findById(1L)).thenReturn(zone);
        snapshotManager.validatePolicyZones(List.of(1L), volumeVO, Mockito.mock(Account.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatePolicyZonesNullZone() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getDataCenterId()).thenReturn(1L);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getType()).thenReturn(DataCenter.Type.Core);
        Mockito.when(dataCenterDao.findById(1L)).thenReturn(zone);
        Mockito.when(dataCenterDao.findById(2L)).thenReturn(null);
        snapshotManager.validatePolicyZones(List.of(2L), volumeVO, Mockito.mock(Account.class));
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidatePolicyZonesDisabledZone() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getDataCenterId()).thenReturn(1L);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getType()).thenReturn(DataCenter.Type.Core);
        Mockito.when(dataCenterDao.findById(1L)).thenReturn(zone);
        DataCenterVO zone1 = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone1.getAllocationState()).thenReturn(Grouping.AllocationState.Disabled);
        Mockito.when(dataCenterDao.findById(2L)).thenReturn(zone1);
        Mockito.when(accountManager.isRootAdmin(Mockito.any())).thenReturn(false);
        snapshotManager.validatePolicyZones(List.of(2L), volumeVO, Mockito.mock(Account.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidatePolicyZonesEdgeZone() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getDataCenterId()).thenReturn(1L);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getType()).thenReturn(DataCenter.Type.Core);
        Mockito.when(dataCenterDao.findById(1L)).thenReturn(zone);
        DataCenterVO zone1 = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone1.getType()).thenReturn(DataCenter.Type.Edge);
        Mockito.when(zone1.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(dataCenterDao.findById(2L)).thenReturn(zone1);
        snapshotManager.validatePolicyZones(List.of(2L), volumeVO, Mockito.mock(Account.class));
    }

    @Test
    public void testValidatePolicyZonesValidZone() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getDataCenterId()).thenReturn(1L);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getType()).thenReturn(DataCenter.Type.Core);
        Mockito.when(dataCenterDao.findById(1L)).thenReturn(zone);
        DataCenterVO zone1 = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone1.getType()).thenReturn(DataCenter.Type.Core);
        Mockito.when(zone1.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(dataCenterDao.findById(2L)).thenReturn(zone1);
        snapshotManager.validatePolicyZones(List.of(2L), volumeVO, Mockito.mock(Account.class));
    }

    @Test
    public void testCopyNewSnapshotToZonesNoZones() {
        snapshotManager.copyNewSnapshotToZones(1L, 1L, new ArrayList<>());
    }

    @Test
    public void testCopyNewSnapshotToZones() {
        final long snapshotId = 1L;
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotVO.getId()).thenReturn(snapshotId);
        Mockito.when(snapshotVO.getAccountId()).thenReturn(1L);
        Mockito.when(snapshotDao.findById(snapshotId)).thenReturn(snapshotVO);
        final long zoneId = 1L;
        final long storeId = 1L;
        final long destZoneId = 2L;
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getId()).thenReturn(destZoneId);
        Mockito.when(dataCenterDao.findById(destZoneId)).thenReturn(zone);
        SnapshotDataStoreVO ref = Mockito.mock(SnapshotDataStoreVO.class);
        Mockito.when(ref.getDataStoreId()).thenReturn(storeId);
        Mockito.when(ref.getRole()).thenReturn(DataStoreRole.Image);
        List<SnapshotDataStoreVO> snapshotStoreList = List.of(Mockito.mock(SnapshotDataStoreVO.class), ref);
        Mockito.when(dataStoreManager.getStoreZoneId(storeId, DataStoreRole.Image)).thenReturn(zoneId);
        DataStore store = Mockito.mock(DataStore.class);
        Mockito.when(store.getId()).thenReturn(storeId);
        Mockito.when(dataStoreManager.getDataStore(storeId, DataStoreRole.Image)).thenReturn(store);
        Mockito.when(snapshotStoreDao.listReadyBySnapshot(snapshotId, DataStoreRole.Image)).thenReturn(snapshotStoreList);
        Mockito.when(snapshotFactory.getSnapshot(Mockito.anyLong(), Mockito.any())).thenReturn(Mockito.mock(SnapshotInfo.class));
        CreateCmdResult result = Mockito.mock(CreateCmdResult.class);
        Mockito.when(result.isFailed()).thenReturn(false);
        Mockito.when(result.getPath()).thenReturn("SOMEPATH");
        AsyncCallFuture<CreateCmdResult> future = Mockito.mock(AsyncCallFuture.class);
        Mockito.when(dataStoreManager.getImageStoresByScopeExcludingReadOnly(Mockito.any())).thenReturn(List.of(Mockito.mock(DataStore.class)));
        Mockito.when(dataStoreManager.getImageStoreWithFreeCapacity(Mockito.anyList())).thenReturn(Mockito.mock(DataStore.class));
        Mockito.when(snapshotStoreDao.findByStoreSnapshot(DataStoreRole.Image, 1L, 1L)).thenReturn(Mockito.mock(SnapshotDataStoreVO.class));
        AccountVO account = Mockito.mock(AccountVO.class);
        Mockito.when(account.getId()).thenReturn(1L);
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(account);
        SnapshotResult result1 = Mockito.mock(SnapshotResult.class);
        Mockito.when(result1.isFailed()).thenReturn(false);
        AsyncCallFuture<SnapshotResult> future1 = Mockito.mock(AsyncCallFuture.class);
        try {
            Mockito.doNothing().when(resourceLimitService).checkResourceLimit(Mockito.any(), Mockito.any(), Mockito.anyLong());
            Mockito.when(future.get()).thenReturn(result);
            Mockito.when(snapshotService.queryCopySnapshot(Mockito.any())).thenReturn(future);
            Mockito.when(future1.get()).thenReturn(result1);
            Mockito.when(snapshotService.copySnapshot(Mockito.any(SnapshotInfo.class), Mockito.anyString(), Mockito.any(DataStore.class))).thenReturn(future1);
        } catch (ResourceAllocationException | ResourceUnavailableException | ExecutionException | InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        List<Long> addedZone = new ArrayList<>();
        Mockito.doAnswer((Answer<Void>) invocation -> {
            Long zoneId1 = (Long) invocation.getArguments()[1];
            addedZone.add(zoneId1);
            return null;
        }).when(snapshotZoneDao).addSnapshotToZone(Mockito.anyLong(), Mockito.anyLong());
        try (MockedStatic<ActionEventUtils> utilities = Mockito.mockStatic(ActionEventUtils.class)) {
            utilities.when(() -> ActionEventUtils.onStartedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(),
                    Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong())).thenReturn(1L);
            snapshotManager.copyNewSnapshotToZones(snapshotId, 1L, List.of(2L));
            Assert.assertEquals(1, addedZone.size());
        }
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetCheckedSnapshotForCopyNoSnapshot() {
        snapshotManager.getCheckedSnapshotForCopy(1L, List.of(100L), null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetCheckedSnapshotForCopyNoSnapshotBackup() {
        final long snapshotId = 1L;
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotDao.findById(snapshotId)).thenReturn(snapshotVO);
        snapshotManager.getCheckedSnapshotForCopy(snapshotId, List.of(100L), null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetCheckedSnapshotForCopyNotOnSecondary() {
        final long snapshotId = 1L;
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotVO.getState()).thenReturn(Snapshot.State.BackedUp);
        Mockito.when(snapshotVO.getLocationType()).thenReturn(Snapshot.LocationType.PRIMARY);
        Mockito.when(snapshotDao.findById(snapshotId)).thenReturn(snapshotVO);
        snapshotManager.getCheckedSnapshotForCopy(snapshotId, List.of(100L), null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetCheckedSnapshotForCopyDestNotSpecified() {
        final long snapshotId = 1L;
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotVO.getState()).thenReturn(Snapshot.State.BackedUp);
        Mockito.when(snapshotDao.findById(snapshotId)).thenReturn(snapshotVO);
        snapshotManager.getCheckedSnapshotForCopy(snapshotId, new ArrayList<>(), null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetCheckedSnapshotForCopyDestContainsSource() {
        final long snapshotId = 1L;
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotVO.getState()).thenReturn(Snapshot.State.BackedUp);
        Mockito.when(snapshotVO.getVolumeId()).thenReturn(1L);
        Mockito.when(snapshotDao.findById(snapshotId)).thenReturn(snapshotVO);
        Mockito.when(volumeDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(VolumeVO.class));
        snapshotManager.getCheckedSnapshotForCopy(snapshotId, List.of(100L, 1L), 1L);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testGetCheckedSnapshotForCopyNullSourceZone() {
        final long snapshotId = 1L;
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotVO.getState()).thenReturn(Snapshot.State.BackedUp);
        Mockito.when(snapshotVO.getVolumeId()).thenReturn(1L);
        Mockito.when(snapshotDao.findById(snapshotId)).thenReturn(snapshotVO);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getDataCenterId()).thenReturn(1L);
        Mockito.when(volumeDao.findById(Mockito.anyLong())).thenReturn(volumeVO);
        snapshotManager.getCheckedSnapshotForCopy(snapshotId, List.of(100L, 101L), null);
    }

    @Test
    public void testGetCheckedSnapshotForCopyValid() {
        final long snapshotId = 1L;
        final Long zoneId = 1L;
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotVO.getState()).thenReturn(Snapshot.State.BackedUp);
        Mockito.when(snapshotVO.getVolumeId()).thenReturn(1L);
        Mockito.when(snapshotDao.findById(snapshotId)).thenReturn(snapshotVO);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(volumeDao.findById(Mockito.anyLong())).thenReturn(volumeVO);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(Mockito.mock(DataCenterVO.class));
        Pair<SnapshotVO, Long> result = snapshotManager.getCheckedSnapshotForCopy(snapshotId, List.of(100L, 101L), null);
        Assert.assertNotNull(result.first());
        Assert.assertEquals(zoneId, result.second());
    }

    @Test
    public void testGetCheckedSnapshotForCopyNullDest() {
        final long snapshotId = 1L;
        final Long zoneId = 1L;
        SnapshotVO snapshotVO = Mockito.mock(SnapshotVO.class);
        Mockito.when(snapshotVO.getState()).thenReturn(Snapshot.State.BackedUp);
        Mockito.when(snapshotVO.getVolumeId()).thenReturn(1L);
        Mockito.when(snapshotDao.findById(snapshotId)).thenReturn(snapshotVO);
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        Mockito.when(volumeVO.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(volumeDao.findById(Mockito.anyLong())).thenReturn(volumeVO);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(Mockito.mock(DataCenterVO.class));
        Pair<SnapshotVO, Long> result = snapshotManager.getCheckedSnapshotForCopy(snapshotId, List.of(100L, 101L), null);
        Assert.assertNotNull(result.first());
        Assert.assertEquals(zoneId, result.second());
    }

    @Test
    public void testGetCheckedDestinationZoneForSnapshotCopy() {
        long zoneId = 1L;
        DataCenterVO dataCenterVO = Mockito.mock(DataCenterVO.class);
        Mockito.when(dataCenterVO.getAllocationState()).thenReturn(Grouping.AllocationState.Enabled);
        Mockito.when(dataCenterVO.getType()).thenReturn(DataCenter.Type.Core);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(dataCenterVO);
        Assert.assertNotNull(snapshotManager.getCheckedDestinationZoneForSnapshotCopy(zoneId, false));
    }
}
