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

import com.cloud.network.Network;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.storage.Volume;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.apache.cloudstack.framework.jobs.dao.SyncQueueItemDao;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AsyncJobManagerImplTest {
    @Spy
    @InjectMocks
    AsyncJobManagerImpl asyncJobManager;
    @Mock
    VolumeDataFactory volFactory;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    VirtualMachineManager virtualMachineManager;
    @Mock
    NetworkDao networkDao;
    @Mock
    NetworkOrchestrationService networkOrchestrationService;

    @Mock
    AsyncJobDao jobDao;

    @Mock
    SyncQueueItemDao queueItemDao;

    @Mock
    SyncQueueManager queueMgr;

    @Mock
    AsyncJobMonitor jobMonitor;

    @Test
    public void testCleanupVolumeResource() {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.Volume.toString());
        job.setInstanceId(1L);
        VolumeInfo volumeInfo = Mockito.mock(VolumeInfo.class);
        when(volFactory.getVolume(Mockito.anyLong())).thenReturn(volumeInfo);
        when(volumeInfo.getState()).thenReturn(Volume.State.Attaching);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(volumeInfo, Mockito.times(1)).stateTransit(Volume.Event.OperationFailed);
    }

    @Test
    public void testCleanupVmResource() throws NoTransitionException {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.VirtualMachine.toString());
        job.setInstanceId(1L);
        VMInstanceVO vmInstanceVO = Mockito.mock(VMInstanceVO.class);
        when(vmInstanceDao.findById(Mockito.anyLong())).thenReturn(vmInstanceVO);
        when(vmInstanceVO.getState()).thenReturn(VirtualMachine.State.Starting);
        when(vmInstanceVO.getHostId()).thenReturn(1L);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(virtualMachineManager, Mockito.times(1)).stateTransitTo(vmInstanceVO, VirtualMachine.Event.OperationFailed, 1L);
    }

    @Test
    public void testCleanupNetworkResource() throws NoTransitionException {
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceType(ApiCommandResourceType.Network.toString());
        job.setInstanceId(1L);
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        when(networkDao.findById(Mockito.anyLong())).thenReturn(networkVO);
        when(networkVO.getState()).thenReturn(Network.State.Implementing);
        asyncJobManager.cleanupResources(job);
        Mockito.verify(networkOrchestrationService, Mockito.times(1)).stateTransitTo(networkVO,
                Network.Event.OperationFailed);
    }

    private AsyncJobVO childJob(long id, JobInfo.Status status) {
        AsyncJobVO child = new AsyncJobVO();
        child.setId(id);
        child.setStatus(status);
        child.setRelated("10");
        return child;
    }

    private SyncQueueItemVO queueItem(long id, Long lastProcessMsid) {
        SyncQueueItemVO item = new SyncQueueItemVO();
        item.setId(id);
        item.setLastProcessMsid(lastProcessMsid);
        return item;
    }

    @Test
    public void testCancelQueuedChildJobsCompletesAndPurgesWaitingChild() {
        when(jobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(childJob(11L, JobInfo.Status.IN_PROGRESS)));
        when(queueItemDao.getQueueItemIdByContentIdAndType(11L, SyncQueueItem.AsyncJobContentType)).thenReturn(5L);
        when(queueItemDao.findById(5L)).thenReturn(queueItem(5L, null));
        Mockito.doNothing().when(asyncJobManager).completeAsyncJob(Mockito.eq(11L), Mockito.eq(JobInfo.Status.CANCELLED), Mockito.eq(0), Mockito.anyString());

        asyncJobManager.cancelQueuedChildJobs(10L, "user request");

        Mockito.verify(asyncJobManager).completeAsyncJob(Mockito.eq(11L), Mockito.eq(JobInfo.Status.CANCELLED), Mockito.eq(0), Mockito.contains("user request"));
        Mockito.verify(jobMonitor).unregisterByJobId(11L);
        Mockito.verify(queueMgr).purgeItem(5L);
    }

    @Test
    public void testCancelQueuedChildJobsLeavesDequeuedChildToTheInFlightPath() {
        when(jobDao.listChildJobs(10L)).thenReturn(Collections.singletonList(childJob(11L, JobInfo.Status.IN_PROGRESS)));
        when(queueItemDao.getQueueItemIdByContentIdAndType(11L, SyncQueueItem.AsyncJobContentType)).thenReturn(5L);
        when(queueItemDao.findById(5L)).thenReturn(queueItem(5L, 1L));

        asyncJobManager.cancelQueuedChildJobs(10L, "user request");

        Mockito.verify(asyncJobManager, Mockito.never()).completeAsyncJob(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(jobMonitor, Mockito.never()).unregisterByJobId(Mockito.anyLong());
        Mockito.verify(queueMgr, Mockito.never()).purgeItem(Mockito.anyLong());
    }

    @Test
    public void testCancelQueuedChildJobsSkipsFinishedAndUnqueuedChildren() {
        when(jobDao.listChildJobs(10L)).thenReturn(Arrays.asList(childJob(11L, JobInfo.Status.SUCCEEDED), childJob(12L, JobInfo.Status.IN_PROGRESS)));
        when(queueItemDao.getQueueItemIdByContentIdAndType(12L, SyncQueueItem.AsyncJobContentType)).thenReturn(null);

        asyncJobManager.cancelQueuedChildJobs(10L, "user request");

        Mockito.verify(queueItemDao, Mockito.never()).getQueueItemIdByContentIdAndType(Mockito.eq(11L), Mockito.anyString());
        Mockito.verify(asyncJobManager, Mockito.never()).completeAsyncJob(Mockito.anyLong(), Mockito.any(), Mockito.anyInt(), Mockito.anyString());
        Mockito.verify(queueMgr, Mockito.never()).purgeItem(Mockito.anyLong());
    }
}
