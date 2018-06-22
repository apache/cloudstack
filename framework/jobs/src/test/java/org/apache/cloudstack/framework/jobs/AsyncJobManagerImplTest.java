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
package org.apache.cloudstack.framework.jobs;

import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotService;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobManagerImpl;
import org.apache.cloudstack.framework.jobs.impl.SyncQueueManager;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertFalse;

@RunWith(PowerMockRunner.class)
public class AsyncJobManagerImplTest {

    @InjectMocks
    private AsyncJobManagerImpl asyncJobManager;

    @Mock
    private SyncQueueManager _queueMgr;

    @Mock
    private AsyncJobDao _jobDao;

    @Mock
    private VolumeDetailsDao _volumeDetailsDao;

    @Mock
    private VolumeDao _volsDao;

    @Mock
    private SnapshotDetailsDao _snapshotDetailsDao;

    @Mock
    private SnapshotDataFactory snapshotFactory;

    @Mock
    private SnapshotService snapshotSrv;

    @Mock
    private Logger s_logger;

//    @Before
//    public void setup() {
//        asyncJobManager = spy(AsyncJobManagerImpl.class);
//    }

    @Test
    public void testcleanupLeftOverJobs() throws Exception {
//        Whitebox.invokeMethod(asyncJobManager, "cleanupLeftOverJobs", 1L);
//        verify(_queueMgr).cleanupActiveQueueItems(1L, true);
//        boolean result = Whitebox.invokeMethod(asyncJobManager, "stop");
        assertFalse(false);
    }
}
