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

import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotPolicyDao;
import com.cloud.storage.dao.SnapshotScheduleDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class SnapshotSchedulerImplTest {

    @Spy
    @InjectMocks
    SnapshotSchedulerImpl snapshotSchedulerImplSpy = new SnapshotSchedulerImpl();

    @Mock
    SnapshotPolicyDao snapshotPolicyDaoMock;

    @Mock
    SnapshotPolicyVO snapshotPolicyVoMock;

    @Mock
    SnapshotScheduleDao snapshotScheduleDaoMock;

    @Mock
    AccountDao accountDaoMock;

    @Mock
    VolumeDao volumeDaoMock;

    @Mock
    VolumeVO volumeVoMock;

    @Mock
    AccountVO accountVoMock;

    @Test
    public void scheduleNextSnapshotJobTestParameterIsNullReturnNull() {
        SnapshotScheduleVO snapshotScheduleVO = null;

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);

        Assert.assertNull(result);
    }

    @Test
    public void scheduleNextSnapshotJobTestIsManualPolicyIdReturnNull() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        snapshotScheduleVO.setPolicyId(Snapshot.MANUAL_POLICY_ID);

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);

        Assert.assertNull(result);
    }

    @Test
    public void scheduleNextSnapshotJobTestPolicyIsNotNullDoNotCallExpunge() {
        Date expected = new Date();
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        snapshotScheduleVO.setPolicyId(1l);

        Mockito.doReturn(snapshotPolicyVoMock).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(expected).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(Mockito.any(SnapshotPolicyVO.class));

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);
        Assert.assertEquals(expected, result);

        Mockito.verify(snapshotScheduleDaoMock, Mockito.never()).expunge(Mockito.anyLong());
    }

    @Test
    public void scheduleNextSnapshotJobTestPolicyIsNullCallExpunge() {
        Date expected = new Date();
        SnapshotPolicyVO snapshotPolicyVO = null;
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        snapshotScheduleVO.setPolicyId(1l);

        Mockito.doReturn(snapshotPolicyVO).when(snapshotPolicyDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(true).when(snapshotScheduleDaoMock).expunge(Mockito.anyLong());
        Mockito.doReturn(expected).when(snapshotSchedulerImplSpy).scheduleNextSnapshotJob(snapshotPolicyVO);

        Date result = snapshotSchedulerImplSpy.scheduleNextSnapshotJob(snapshotScheduleVO);
        Assert.assertEquals(expected, result);

        Mockito.verify(snapshotScheduleDaoMock).expunge(Mockito.anyLong());
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountIsNullReturnTrue() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(null).when(accountDaoMock).findById(Mockito.anyLong());

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountStateIsDisabledReturnTrue() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(Account.State.DISABLED).when(accountVoMock).getState();

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);
    }

    @Test
    public void isAccountRemovedOrDisabledTestVolumeAccountStateIsNotNullNorDisabledReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(accountVoMock).when(accountDaoMock).findById(Mockito.anyLong());
        Mockito.doReturn(Account.State.ENABLED).when(accountVoMock).getState();

        boolean result = snapshotSchedulerImplSpy.isAccountRemovedOrDisabled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestVolumeIsRemovedReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(new Date()).when(volumeVoMock).getRemoved();

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestVolumeIsNotAttachedToStoragePoolReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(null).when(volumeVoMock).getPoolId();

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestAccountIsRemovedOrDisabledReturnFalse() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(1l).when(volumeVoMock).getPoolId();
        Mockito.doReturn(true).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertFalse(result);
    }

    @Test
    public void canSnapshotBeScheduledTestSnapshotPolicyIsRemovedCallRemove() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();

        Mockito.doReturn(1l).when(volumeVoMock).getPoolId();
        Mockito.doReturn(false).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());
        Mockito.doReturn(null).when(snapshotPolicyDaoMock).findById(Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);

        Mockito.verify(snapshotScheduleDaoMock).remove(Mockito.anyLong());
    }

    @Test
    public void canSnapshotBeScheduledTestSnapshotPolicyIsNotRemovedDoNotCallRemove() {
        SnapshotScheduleVO snapshotScheduleVO = new SnapshotScheduleVO();
        SnapshotPolicyVO snapshotPolicyVO = new SnapshotPolicyVO();

        Mockito.doReturn(1l).when(volumeVoMock).getPoolId();
        Mockito.doReturn(false).when(snapshotSchedulerImplSpy).isAccountRemovedOrDisabled(Mockito.any(), Mockito.any());
        Mockito.doReturn(snapshotPolicyVO).when(snapshotPolicyDaoMock).findById(Mockito.any());

        boolean result = snapshotSchedulerImplSpy.canSnapshotBeScheduled(snapshotScheduleVO, volumeVoMock);

        Assert.assertTrue(result);

        Mockito.verify(snapshotScheduleDaoMock, Mockito.never()).remove(Mockito.anyLong());
    }
}
