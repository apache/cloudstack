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

package com.cloud.usage.dao;

import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.TransactionLegacy;
import java.util.Date;
import java.util.TimeZone;

import com.cloud.usage.UsageStorageVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TransactionLegacy.class)
@PowerMockIgnore("javax.management.*")
public class UsageStorageDaoImplTest {

    @Mock
    private PreparedStatement preparedStatementMock;

    @Mock
    private TransactionLegacy transactionMock;

    @Mock
    private  UsageStorageVO userStorageVOMock;

    private final UsageStorageDaoImpl usageDao = new UsageStorageDaoImpl();

    @Test
    public void testUpdate() throws Exception {


        long id = 21, zoneId = 31, accountId = 41;
        int storageType = 1;
        String UPDATE_DELETED = "UPDATE usage_storage SET deleted = ? WHERE account_id = ? AND entity_id = ? AND storage_type = ? AND zone_id = ? and deleted IS NULL";
        Date deleted = new Date();

        PowerMockito.mockStatic(TransactionLegacy.class);
        Mockito.when(TransactionLegacy.open(TransactionLegacy.USAGE_DB)).thenReturn(transactionMock);

        when(transactionMock.prepareStatement(contains(UPDATE_DELETED))).thenReturn(preparedStatementMock);
        when(userStorageVOMock.getAccountId()).thenReturn(accountId);
        when(userStorageVOMock.getEntityId()).thenReturn(id);
        when(userStorageVOMock.getStorageType()).thenReturn(storageType);
        when(userStorageVOMock.getZoneId()).thenReturn(zoneId);
        when(userStorageVOMock.getDeleted()).thenReturn(deleted);



        usageDao.update(userStorageVOMock);

        verify(transactionMock, times(1)).prepareStatement(UPDATE_DELETED);
        verify(preparedStatementMock, times(1)).setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), deleted));
        verify(preparedStatementMock, times(1)).setLong(2, accountId);
        verify(preparedStatementMock, times(1)).setLong(3, id);
        verify(preparedStatementMock, times(1)).setInt(4, storageType);
        verify(preparedStatementMock, times(1)).setLong(5, zoneId);
        verify(preparedStatementMock, times(1)).executeUpdate();
    }
}
