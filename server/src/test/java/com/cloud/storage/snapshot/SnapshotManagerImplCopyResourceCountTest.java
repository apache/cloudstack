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

import static org.mockito.Mockito.when;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotResult;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.framework.async.AsyncCallFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.dao.SnapshotZoneDao;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SnapshotManagerImplCopyResourceCountTest {

    @Mock
    DataStoreManager dataStoreMgr;
    @Mock
    SnapshotService snapshotSrv;
    @Mock
    SnapshotZoneDao snapshotZoneDao;
    @Mock
    ResourceLimitService _resourceLimitMgr;

    @InjectMocks
    SnapshotManagerImpl snapshotManager = new SnapshotManagerImpl();

    @Test
    @SuppressWarnings("unchecked")
    public void copySnapshotOnPoolChargesPrimaryStorageToTheSnapshotOwnerAccount() throws Exception {
        long ownerAccountId = 42L;
        long snapshotSize = 1024L;
        Long poolId = 5L;

        SnapshotInfo snapshot = Mockito.mock(SnapshotInfo.class);
        SnapshotInfo snapshotOnStore = Mockito.mock(SnapshotInfo.class);
        SnapshotStrategy strategy = Mockito.mock(SnapshotStrategy.class);
        DataStore store = Mockito.mock(DataStore.class);
        AsyncCallFuture<SnapshotResult> future = Mockito.mock(AsyncCallFuture.class);
        SnapshotResult result = Mockito.mock(SnapshotResult.class);

        when(snapshot.getId()).thenReturn(100L);
        when(snapshot.getSize()).thenReturn(snapshotSize);
        when(snapshot.getAccountId()).thenReturn(ownerAccountId);
        when(dataStoreMgr.getDataStore(poolId, DataStoreRole.Primary)).thenReturn(store);
        when(store.create(snapshot)).thenReturn(snapshotOnStore);
        when(snapshotOnStore.getDataCenterId()).thenReturn(2L);
        when(snapshotSrv.copySnapshot(snapshot, snapshotOnStore, strategy)).thenReturn(future);
        when(future.get()).thenReturn(result);
        when(result.isFailed()).thenReturn(false);

        try (MockedStatic<CallContext> ctxMock = Mockito.mockStatic(CallContext.class)) {
            CallContext ctx = Mockito.mock(CallContext.class);
            ctxMock.when(CallContext::current).thenReturn(ctx);
            // Use SYSTEM ids so the usage-event branch is skipped; the point of the
            // test is which account the primary_storage count is charged to.
            when(ctx.getCallingUserId()).thenReturn(Account.ACCOUNT_ID_SYSTEM);
            when(ctx.getCallingAccountId()).thenReturn(Account.ACCOUNT_ID_SYSTEM);

            snapshotManager.copySnapshotOnPool(snapshot, strategy, poolId);
        }

        // Must charge the snapshot OWNER's account, not the calling user id.
        // Before the fix this was called with CallContext.getCallingUserId().
        Mockito.verify(_resourceLimitMgr).incrementResourceCount(
                Mockito.eq(ownerAccountId), Mockito.eq(ResourceType.primary_storage), Mockito.eq(snapshotSize));
    }
}
