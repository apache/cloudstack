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
package org.apache.cloudstack.framework.jobs.impl;

import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AsyncJobManagerImplExecuteQueueItemTest {

    @Mock
    AsyncJobDao _jobDao;
    @Mock
    SyncQueueManager _queueMgr;

    @Spy
    @InjectMocks
    AsyncJobManagerImpl asyncJobManager = new AsyncJobManagerImpl();

    @Test
    public void executeQueueItemDoesNotScheduleWhenTheJobUpdateFailsAndItemIsReturned() {
        long contentId = 10L;
        long itemId = 20L;
        long jobId = 1L;

        SyncQueueItemVO item = Mockito.mock(SyncQueueItemVO.class);
        Mockito.when(item.getContentId()).thenReturn(contentId);
        Mockito.when(item.getId()).thenReturn(itemId);

        AsyncJobVO job = Mockito.mock(AsyncJobVO.class);
        Mockito.when(job.getId()).thenReturn(jobId);
        Mockito.when(_jobDao.findById(contentId)).thenReturn(job);

        // Simulate the DB deadlock the catch block was written to survive.
        Mockito.doThrow(new CloudRuntimeException("simulated DB deadlock"))
                .when(_jobDao).update(Mockito.anyLong(), Mockito.any(AsyncJobVO.class));

        // Stub the executor path so we can assert whether it is reached (and avoid the real submit).
        Mockito.doNothing().when(asyncJobManager).scheduleExecution(Mockito.any(AsyncJobVO.class));

        asyncJobManager.executeQueueItem(item, false);

        // The queue item was returned for a later retry; the job must NOT also be scheduled now, or it
        // would run twice (once here and once when the heartbeat re-dequeues the returned item).
        Mockito.verify(_queueMgr).returnItem(itemId);
        Mockito.verify(asyncJobManager, Mockito.never()).scheduleExecution(Mockito.any(AsyncJobVO.class));
    }
}
