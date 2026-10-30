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
package com.cloud.storage;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.utils.Pair;

public class StorageManagerTest {

    private static final long ACCOUNT_ID = 2L;
    private static final long CLUSTER_ID = 3L;
    private static final String ACCOUNT_POOL_UUID = "account-pool-uuid";
    private static final String CLUSTER_POOL_UUID = "cluster-pool-uuid";
    private static final String GLOBAL_POOL_UUID = "global-pool-uuid";

    private ConfigDepotImpl configDepot;
    private Object previousDepot;

    @Before
    public void setUp() {
        previousDepot = ReflectionTestUtils.getField(ConfigKey.class, "s_depot");
        configDepot = Mockito.mock(ConfigDepotImpl.class);
        ConfigKey.init(configDepot);
    }

    @After
    public void tearDown() {
        ConfigKey.init((ConfigDepotImpl) previousDepot);
    }

    private void mockPreferredStoragePool(ConfigKey.Scope scope, Long id, String uuid) {
        Mockito.when(configDepot.getConfigStringValue(StorageManager.PreferredStoragePool.key(), scope, id)).thenReturn(uuid);
    }

    @Test
    public void testGetPreferredStoragePoolUuidNotSetAtAnyLevel() {
        Assert.assertNull(StorageManager.getPreferredStoragePoolUuid(ACCOUNT_ID, CLUSTER_ID));
    }

    @Test
    public void testGetPreferredStoragePoolUuidSetAtAccountLevel() {
        mockPreferredStoragePool(ConfigKey.Scope.Account, ACCOUNT_ID, ACCOUNT_POOL_UUID);
        mockPreferredStoragePool(ConfigKey.Scope.Cluster, CLUSTER_ID, CLUSTER_POOL_UUID);
        mockPreferredStoragePool(ConfigKey.Scope.Global, null, GLOBAL_POOL_UUID);

        Pair<ConfigKey.Scope, String> result = StorageManager.getPreferredStoragePoolUuid(ACCOUNT_ID, CLUSTER_ID);

        Assert.assertEquals(ConfigKey.Scope.Account, result.first());
        Assert.assertEquals(ACCOUNT_POOL_UUID, result.second());
    }

    @Test
    public void testGetPreferredStoragePoolUuidSetAtClusterLevel() {
        mockPreferredStoragePool(ConfigKey.Scope.Cluster, CLUSTER_ID, CLUSTER_POOL_UUID);
        mockPreferredStoragePool(ConfigKey.Scope.Global, null, GLOBAL_POOL_UUID);

        Pair<ConfigKey.Scope, String> result = StorageManager.getPreferredStoragePoolUuid(ACCOUNT_ID, CLUSTER_ID);

        Assert.assertEquals(ConfigKey.Scope.Cluster, result.first());
        Assert.assertEquals(CLUSTER_POOL_UUID, result.second());
    }

    @Test
    public void testGetPreferredStoragePoolUuidEmptyAccountValueFallsBackToCluster() {
        mockPreferredStoragePool(ConfigKey.Scope.Account, ACCOUNT_ID, "");
        mockPreferredStoragePool(ConfigKey.Scope.Cluster, CLUSTER_ID, CLUSTER_POOL_UUID);

        Pair<ConfigKey.Scope, String> result = StorageManager.getPreferredStoragePoolUuid(ACCOUNT_ID, CLUSTER_ID);

        Assert.assertEquals(ConfigKey.Scope.Cluster, result.first());
        Assert.assertEquals(CLUSTER_POOL_UUID, result.second());
    }

    @Test
    public void testGetPreferredStoragePoolUuidSetAtGlobalLevel() {
        mockPreferredStoragePool(ConfigKey.Scope.Global, null, GLOBAL_POOL_UUID);

        Pair<ConfigKey.Scope, String> result = StorageManager.getPreferredStoragePoolUuid(ACCOUNT_ID, CLUSTER_ID);

        Assert.assertEquals(ConfigKey.Scope.Global, result.first());
        Assert.assertEquals(GLOBAL_POOL_UUID, result.second());
    }

    @Test
    public void testGetPreferredStoragePoolUuidNullIdsUseGlobalLevel() {
        mockPreferredStoragePool(ConfigKey.Scope.Global, null, GLOBAL_POOL_UUID);

        Pair<ConfigKey.Scope, String> result = StorageManager.getPreferredStoragePoolUuid(null, null);

        Assert.assertEquals(ConfigKey.Scope.Global, result.first());
        Assert.assertEquals(GLOBAL_POOL_UUID, result.second());
        Mockito.verify(configDepot, Mockito.never()).getConfigStringValue(Mockito.anyString(), Mockito.eq(ConfigKey.Scope.Account), Mockito.any());
        Mockito.verify(configDepot, Mockito.never()).getConfigStringValue(Mockito.anyString(), Mockito.eq(ConfigKey.Scope.Cluster), Mockito.any());
    }
}
