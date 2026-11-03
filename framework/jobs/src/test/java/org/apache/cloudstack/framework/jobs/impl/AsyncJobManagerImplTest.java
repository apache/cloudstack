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
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.jobs.dao.AsyncJobDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.List;

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
    AsyncJobDao asyncJobDao;

    private static final long NETWORK_ID = 42L;
    private static final long ACCOUNT_ID = 2L;
    // job.expire.minutes defaults to one day; these networks were created two days ago
    private static final Date OLD_NETWORK_CREATED = new Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000L);

    private NetworkVO mockImplementingNetworkListed(Date created) {
        NetworkVO listed = Mockito.mock(NetworkVO.class);
        when(listed.getCreated()).thenReturn(created);
        SearchCriteria<NetworkVO> sc = Mockito.mock(SearchCriteria.class);
        when(networkDao.createSearchCriteria()).thenReturn(sc);
        when(networkDao.search(sc, null)).thenReturn(List.of(listed));
        return listed;
    }

    private NetworkVO mockNetworkReadUnderLock(Network.State state) {
        NetworkVO locked = Mockito.mock(NetworkVO.class);
        when(locked.getState()).thenReturn(state);
        when(networkDao.acquireInLockTable(NETWORK_ID, 0)).thenReturn(locked);
        return locked;
    }

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

    @Test
    public void testCleanupOrphanedNetworksSkipsNetworkWhoseLockIsHeld() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        // a VM start implementing the network holds its lock, so a try without waiting fails
        when(networkDao.acquireInLockTable(NETWORK_ID, 0)).thenReturn(null);

        asyncJobManager.cleanupOrphanedNetworks();

        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.any(), Mockito.any());
        Mockito.verify(networkDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
        Mockito.verify(networkDao, Mockito.never()).releaseFromLockTable(Mockito.anyLong());
        Mockito.verifyNoInteractions(asyncJobDao);
    }

    @Test
    public void testCleanupOrphanedNetworksShutsDownOrphanedNetworkUnderLock() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        NetworkVO locked = mockNetworkReadUnderLock(Network.State.Implementing);
        when(locked.getId()).thenReturn(NETWORK_ID);
        when(locked.getAccountId()).thenReturn(ACCOUNT_ID);
        when(locked.getCreated()).thenReturn(OLD_NETWORK_CREATED);
        when(asyncJobDao.findInstancePendingAsyncJobs("Network", ACCOUNT_ID)).thenReturn(Collections.emptyList());

        asyncJobManager.cleanupOrphanedNetworks();

        InOrder inOrder = Mockito.inOrder(networkDao, networkOrchestrationService);
        inOrder.verify(networkDao).acquireInLockTable(NETWORK_ID, 0);
        inOrder.verify(networkOrchestrationService).stateTransitTo(locked, Network.Event.OperationFailed);
        inOrder.verify(networkDao).releaseFromLockTable(NETWORK_ID);
        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.eq(listed), Mockito.any());
    }

    @Test
    public void testCleanupOrphanedNetworksSkipsNetworkImplementedBeforeItWasLocked() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        // the implementation finished between the search and the lock
        mockNetworkReadUnderLock(Network.State.Implemented);

        asyncJobManager.cleanupOrphanedNetworks();

        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.any(), Mockito.any());
        Mockito.verify(networkDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
        Mockito.verify(networkDao).releaseFromLockTable(NETWORK_ID);
    }

    @Test
    public void testCleanupOrphanedNetworksSkipsNetworkWithPendingNetworkJob() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        NetworkVO locked = mockNetworkReadUnderLock(Network.State.Implementing);
        when(locked.getId()).thenReturn(NETWORK_ID);
        when(locked.getAccountId()).thenReturn(ACCOUNT_ID);
        AsyncJobVO job = new AsyncJobVO();
        job.setInstanceId(NETWORK_ID);
        when(asyncJobDao.findInstancePendingAsyncJobs("Network", ACCOUNT_ID)).thenReturn(List.of(job));

        asyncJobManager.cleanupOrphanedNetworks();

        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.any(), Mockito.any());
        Mockito.verify(networkDao).releaseFromLockTable(NETWORK_ID);
    }

    @Test
    public void testCleanupOrphanedNetworksSkipsYoungNetwork() throws NoTransitionException {
        mockImplementingNetworkListed(new Date());

        asyncJobManager.cleanupOrphanedNetworks();

        Mockito.verify(networkDao, Mockito.never()).acquireInLockTable(Mockito.anyLong(), Mockito.anyInt());
        Mockito.verify(networkOrchestrationService, Mockito.never()).stateTransitTo(Mockito.any(), Mockito.any());
        Mockito.verifyNoInteractions(asyncJobDao);
    }

    @Test
    public void testCleanupOrphanedNetworksReleasesLockWhenForcingShutdown() throws NoTransitionException {
        NetworkVO listed = mockImplementingNetworkListed(OLD_NETWORK_CREATED);
        when(listed.getId()).thenReturn(NETWORK_ID);
        NetworkVO locked = mockNetworkReadUnderLock(Network.State.Implementing);
        when(locked.getId()).thenReturn(NETWORK_ID);
        when(locked.getAccountId()).thenReturn(ACCOUNT_ID);
        when(locked.getCreated()).thenReturn(OLD_NETWORK_CREATED);
        when(asyncJobDao.findInstancePendingAsyncJobs("Network", ACCOUNT_ID)).thenReturn(Collections.emptyList());
        when(networkOrchestrationService.stateTransitTo(locked, Network.Event.OperationFailed)).thenThrow(new NoTransitionException("no transition"));

        asyncJobManager.cleanupOrphanedNetworks();

        InOrder inOrder = Mockito.inOrder(locked, networkDao);
        inOrder.verify(locked).setState(Network.State.Shutdown);
        inOrder.verify(networkDao).update(NETWORK_ID, locked);
        inOrder.verify(networkDao).releaseFromLockTable(NETWORK_ID);
    }
}
