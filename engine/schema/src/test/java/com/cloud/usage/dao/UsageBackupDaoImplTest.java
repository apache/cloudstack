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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.usage.UsageBackupVO;
import com.cloud.utils.db.TransactionLegacy;

@RunWith(MockitoJUnitRunner.class)
public class UsageBackupDaoImplTest {

    private static final long ZONE_ID = 1L;
    private static final long ACCOUNT_ID = 2L;
    private static final long DOMAIN_ID = 3L;
    private static final long VM_ID = 4L;
    private static final long OFFERING_ID = 5L;

    @Mock
    private TransactionLegacy transactionMock;

    @Spy
    private UsageBackupDaoImpl usageBackupDao;

    private MockedStatic<TransactionLegacy> transactionLegacyMock;

    private final Date assigned = new Date(1_000_000L);
    private final Date eventDate = new Date(2_000_000L);

    @Before
    public void setUp() {
        transactionLegacyMock = Mockito.mockStatic(TransactionLegacy.class);
        transactionLegacyMock.when(() -> TransactionLegacy.open(TransactionLegacy.USAGE_DB)).thenReturn(transactionMock);
    }

    @After
    public void tearDown() {
        transactionLegacyMock.close();
    }

    private UsageBackupVO usage(long id, long size, long protectedSize, Date created) {
        return new UsageBackupVO(id, ZONE_ID, ACCOUNT_ID, DOMAIN_ID, VM_ID, OFFERING_ID, size, protectedSize, created, null);
    }

    private void mockActiveUsage(UsageBackupVO... rows) {
        List<UsageBackupVO> list = new ArrayList<>(List.of(rows));
        doReturn(list).when(usageBackupDao).listActiveUsage(VM_ID, OFFERING_ID);
    }

    @Test
    public void updateMetricsIgnoresUnchangedSize() {
        mockActiveUsage(usage(10L, 100L, 1000L, assigned));

        usageBackupDao.updateMetrics(VM_ID, OFFERING_ID, 100L, 1000L, eventDate);

        verify(usageBackupDao, never()).update(anyLong(), any(UsageBackupVO.class));
        verify(usageBackupDao, never()).persist(any(UsageBackupVO.class));
    }

    @Test
    public void updateMetricsClosesActiveRowAndOpensNewOneOnSizeChange() {
        UsageBackupVO active = usage(10L, 100L, 1000L, assigned);
        mockActiveUsage(active);
        doReturn(true).when(usageBackupDao).update(anyLong(), any(UsageBackupVO.class));
        doReturn(null).when(usageBackupDao).persist(any(UsageBackupVO.class));

        usageBackupDao.updateMetrics(VM_ID, OFFERING_ID, 9900L, 99000L, eventDate);

        Assert.assertEquals(eventDate, active.getRemoved());
        Assert.assertEquals(100L, active.getSize());
        verify(usageBackupDao).update(10L, active);

        ArgumentCaptor<UsageBackupVO> captor = ArgumentCaptor.forClass(UsageBackupVO.class);
        verify(usageBackupDao).persist(captor.capture());
        UsageBackupVO created = captor.getValue();
        Assert.assertEquals(9900L, created.getSize());
        Assert.assertEquals(99000L, created.getProtectedSize());
        Assert.assertEquals(eventDate, created.getCreated());
        Assert.assertNull(created.getRemoved());
        Assert.assertEquals(ZONE_ID, created.getZoneId());
        Assert.assertEquals(ACCOUNT_ID, created.getAccountId());
        Assert.assertEquals(DOMAIN_ID, created.getDomainId());
        Assert.assertEquals(VM_ID, created.getVmId());
        Assert.assertEquals(OFFERING_ID, created.getBackupOfferingId());
    }

    @Test
    public void updateMetricsClosesRowStartingAtEventDate() {
        UsageBackupVO active = usage(10L, 0L, 0L, eventDate);
        mockActiveUsage(active);
        doReturn(true).when(usageBackupDao).update(anyLong(), any(UsageBackupVO.class));
        doReturn(null).when(usageBackupDao).persist(any(UsageBackupVO.class));

        usageBackupDao.updateMetrics(VM_ID, OFFERING_ID, 100L, 1000L, eventDate);

        Assert.assertEquals(eventDate, active.getRemoved());
        verify(usageBackupDao).update(10L, active);
        ArgumentCaptor<UsageBackupVO> captor = ArgumentCaptor.forClass(UsageBackupVO.class);
        verify(usageBackupDao).persist(captor.capture());
        Assert.assertEquals(100L, captor.getValue().getSize());
        Assert.assertEquals(1000L, captor.getValue().getProtectedSize());
        Assert.assertEquals(eventDate, captor.getValue().getCreated());
    }

    @Test
    public void updateMetricsIgnoresVmWithoutActiveUsage() {
        mockActiveUsage();

        usageBackupDao.updateMetrics(VM_ID, OFFERING_ID, 100L, 1000L, eventDate);

        verify(usageBackupDao, never()).update(anyLong(), any(UsageBackupVO.class));
        verify(usageBackupDao, never()).persist(any(UsageBackupVO.class));
    }

    @Test
    public void updateMetricsMergesDuplicateActiveRows() {
        UsageBackupVO newer = usage(11L, 100L, 1000L, new Date(1_500_000L));
        UsageBackupVO older = usage(10L, 100L, 1000L, assigned);
        mockActiveUsage(newer, older);
        doReturn(true).when(usageBackupDao).update(anyLong(), any(UsageBackupVO.class));
        doReturn(null).when(usageBackupDao).persist(any(UsageBackupVO.class));

        usageBackupDao.updateMetrics(VM_ID, OFFERING_ID, 100L, 1000L, eventDate);

        Assert.assertEquals(eventDate, newer.getRemoved());
        Assert.assertEquals(eventDate, older.getRemoved());
        verify(usageBackupDao, times(2)).update(anyLong(), any(UsageBackupVO.class));
        verify(usageBackupDao, times(1)).persist(any(UsageBackupVO.class));
    }

    @Test
    public void updateMetricsTreatsNullSizesAsZero() {
        mockActiveUsage(usage(10L, 0L, 0L, assigned));

        usageBackupDao.updateMetrics(VM_ID, OFFERING_ID, null, null, eventDate);

        verify(usageBackupDao, never()).update(anyLong(), any(UsageBackupVO.class));
        verify(usageBackupDao, never()).persist(any(UsageBackupVO.class));
    }
}
