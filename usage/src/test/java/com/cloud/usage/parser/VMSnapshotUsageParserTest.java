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
package com.cloud.usage.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.usage.UsageTypes;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.usage.UsageVMSnapshotVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageVMSnapshotDao;
import com.cloud.user.AccountVO;

@RunWith(MockitoJUnitRunner.class)
public class VMSnapshotUsageParserTest {

    private static final long HOUR = 60L * 60L * 1000L;
    private static final long ACCOUNT_ID = 2L;
    private static final long DOMAIN_ID = 1L;
    private static final long ZONE_ID = 1L;
    private static final long VM_ID = 10L;
    private static final long VOLUME_ID = 100L;

    @Mock
    private UsageVMSnapshotDao usageVMSnapshotDao;

    @Mock
    private UsageDao usageDao;

    @Mock
    private AccountVO account;

    @InjectMocks
    private VMSnapshotUsageParser parser = new VMSnapshotUsageParser();

    private final Date startDate = new Date(1_700_000_000_000L);
    private final Date endDate = new Date(startDate.getTime() + 24 * HOUR - 1);

    @Before
    public void setUp() {
        parser.usageDao = usageDao;
        Mockito.when(account.getId()).thenReturn(ACCOUNT_ID);
        Mockito.when(account.getDomainId()).thenReturn(DOMAIN_ID);
    }

    private UsageVMSnapshotVO row(long vmSnapshotId, long size, Date created, Date processed) {
        UsageVMSnapshotVO vo = new UsageVMSnapshotVO(VOLUME_ID, ZONE_ID, ACCOUNT_ID, DOMAIN_ID, VM_ID, null, size, created, processed);
        vo.setVmSnapshotId(vmSnapshotId);
        return vo;
    }

    private Map<Long, UsageVO> parseAndCapture(List<UsageVMSnapshotVO> rows, int expectedRecords) {
        Mockito.when(usageVMSnapshotDao.getUsageRecords(ACCOUNT_ID, DOMAIN_ID, startDate, endDate)).thenReturn(rows);

        Assert.assertTrue(parser.parse(account, startDate, endDate));

        ArgumentCaptor<UsageVO> captor = ArgumentCaptor.forClass(UsageVO.class);
        Mockito.verify(usageDao, Mockito.times(expectedRecords)).persist(captor.capture());
        Map<Long, UsageVO> bySnapshot = new HashMap<>();
        for (UsageVO usage : captor.getAllValues()) {
            Assert.assertEquals(UsageTypes.VM_SNAPSHOT, usage.getUsageType());
            Assert.assertNull("Snapshot billed more than once in the same period", bySnapshot.put(usage.getUsageId(), usage));
        }
        // The parser must never consume ("process") rows; that column is reserved for the delete event.
        Mockito.verify(usageVMSnapshotDao, Mockito.never()).update(Mockito.any(UsageVMSnapshotVO.class));
        return bySnapshot;
    }

    private static double hours(UsageVO usage) {
        return usage.getRawUsage();
    }

    @Test
    public void twoSnapshotsCreatedInSamePeriodAreBilledIndependently() {
        UsageVMSnapshotVO snap1 = row(1L, 1000L, new Date(startDate.getTime() + 2 * HOUR), null);
        UsageVMSnapshotVO snap2 = row(2L, 2000L, new Date(startDate.getTime() + 5 * HOUR), null);

        Map<Long, UsageVO> usages = parseAndCapture(Arrays.asList(snap1, snap2), 2);

        UsageVO u1 = usages.get(1L);
        Assert.assertEquals(1000L, u1.getSize().longValue());
        Assert.assertEquals(snap1.getCreated(), u1.getStartDate());
        Assert.assertEquals(endDate, u1.getEndDate());
        Assert.assertEquals(22.0, hours(u1), 0.001);

        UsageVO u2 = usages.get(2L);
        Assert.assertEquals(2000L, u2.getSize().longValue());
        Assert.assertEquals(snap2.getCreated(), u2.getStartDate());
        Assert.assertEquals(19.0, hours(u2), 0.001);
    }

    @Test
    public void olderSnapshotKeepsAccruingAfterNewerOneIsCreatedInLaterPeriod() {
        UsageVMSnapshotVO snap1 = row(1L, 1000L, new Date(startDate.getTime() - 30 * HOUR), null);
        UsageVMSnapshotVO snap2 = row(2L, 2000L, new Date(startDate.getTime() + 6 * HOUR), null);

        Map<Long, UsageVO> usages = parseAndCapture(Arrays.asList(snap1, snap2), 2);

        Assert.assertEquals(startDate, usages.get(1L).getStartDate());
        Assert.assertEquals(24.0, hours(usages.get(1L)), 0.001);
        Assert.assertEquals(18.0, hours(usages.get(2L)), 0.001);
    }

    @Test
    public void deletedSnapshotIsBilledUntilDeletion() {
        Date deleted = new Date(startDate.getTime() + 4 * HOUR);
        UsageVMSnapshotVO snap1 = row(1L, 1000L, new Date(startDate.getTime() - 10 * HOUR), deleted);
        UsageVMSnapshotVO snap2 = row(2L, 2000L, new Date(startDate.getTime() - 5 * HOUR), null);

        Map<Long, UsageVO> usages = parseAndCapture(Arrays.asList(snap1, snap2), 2);

        Assert.assertEquals(deleted, usages.get(1L).getEndDate());
        Assert.assertEquals(4.0, hours(usages.get(1L)), 0.001);
        Assert.assertEquals(24.0, hours(usages.get(2L)), 0.001);
    }

    @Test
    public void snapshotCreatedAndDeletedInSamePeriod() {
        Date created = new Date(startDate.getTime() + 3 * HOUR);
        Date deleted = new Date(startDate.getTime() + 8 * HOUR);

        Map<Long, UsageVO> usages = parseAndCapture(Collections.singletonList(row(1L, 1000L, created, deleted)), 1);

        Assert.assertEquals(5.0, hours(usages.get(1L)), 0.001);
    }

    @Test
    public void snapshotDeletedBeforePeriodIsNotBilled() {
        UsageVMSnapshotVO snap = row(1L, 1000L, new Date(startDate.getTime() - 10 * HOUR), new Date(startDate.getTime() - 2 * HOUR));

        parseAndCapture(Collections.singletonList(snap), 0);
    }

    @Test
    public void noRecords() {
        Mockito.when(usageVMSnapshotDao.getUsageRecords(ACCOUNT_ID, DOMAIN_ID, startDate, endDate)).thenReturn(Collections.emptyList());

        Assert.assertTrue(parser.parse(account, startDate, endDate));

        Mockito.verify(usageDao, Mockito.never()).persist(Mockito.any(UsageVO.class));
    }
}
