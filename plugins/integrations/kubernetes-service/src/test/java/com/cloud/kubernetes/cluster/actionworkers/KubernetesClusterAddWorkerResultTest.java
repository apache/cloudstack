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
package com.cloud.kubernetes.cluster.actionworkers;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventVO;
import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterEventTypes;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterAddWorkerResultTest {

    @Mock
    KubernetesClusterManagerImpl kubernetesClusterManager;

    @Mock
    KubernetesCluster kubernetesCluster;

    @Mock
    UserVmDao userVmDao;

    KubernetesClusterAddWorker addWorker;

    @Before
    public void setUp() {
        Mockito.lenient().when(kubernetesCluster.getName()).thenReturn("k8s");
        Mockito.lenient().when(kubernetesCluster.getUuid()).thenReturn("uuid-k8s");
        addWorker = new KubernetesClusterAddWorker(kubernetesCluster, kubernetesClusterManager);
        addWorker.userVmDao = userVmDao;
        mockVm(1L, "node-a", "uuid-a");
        mockVm(2L, "node-b", "uuid-b");
    }

    private void mockVm(long id, String name, String uuid) {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.lenient().when(vm.getName()).thenReturn(name);
        Mockito.lenient().when(vm.getUuid()).thenReturn(uuid);
        Mockito.lenient().when(userVmDao.findByIdIncludingRemoved(id)).thenReturn(vm);
    }

    @Test
    public void testNodesNotAddedMessageWhenNoNodeAdded() {
        String msg = addWorker.getNodesNotAddedMessage(1, List.of(1L));
        Assert.assertEquals("Failed to add 1 of 1 nodes to Kubernetes cluster k8s (uuid-k8s). Not added: node-a (uuid-a). Added: none", msg);
    }

    @Test
    public void testNodesNotAddedMessageWhenSomeNodesAdded() {
        addWorker.finalNodeIds.add(2L);
        String msg = addWorker.getNodesNotAddedMessage(2, List.of(1L));
        Assert.assertEquals("Failed to add 1 of 2 nodes to Kubernetes cluster k8s (uuid-k8s). Not added: node-a (uuid-a). Added: node-b (uuid-b)", msg);
    }

    @Test
    public void testNodesNotAddedMessageWithMissingVm() {
        String msg = addWorker.getNodesNotAddedMessage(1, List.of(3L));
        Assert.assertEquals("Failed to add 1 of 1 nodes to Kubernetes cluster k8s (uuid-k8s). Not added: 3. Added: none", msg);
    }

    @Test
    public void testRecordNodesAdditionFailureRecordsErrorEvent() {
        Mockito.when(kubernetesCluster.getId()).thenReturn(10L);
        User user = Mockito.mock(User.class);
        Mockito.when(user.getId()).thenReturn(5L);
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getId()).thenReturn(6L);
        CallContext.register(user, account);
        try (MockedStatic<ActionEventUtils> events = Mockito.mockStatic(ActionEventUtils.class)) {
            addWorker.recordNodesAdditionFailure("boom");
            addWorker.recordNodesAdditionFailure("x".repeat(2000));
            events.verify(() -> ActionEventUtils.onCompletedActionEvent(ArgumentMatchers.eq(5L), ArgumentMatchers.eq(6L),
                    ArgumentMatchers.eq(EventVO.LEVEL_ERROR), ArgumentMatchers.eq(KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODES_ADD),
                    ArgumentMatchers.eq("boom"), ArgumentMatchers.eq(10L), ArgumentMatchers.eq(ApiCommandResourceType.KubernetesCluster.toString()), ArgumentMatchers.eq(0L)));
            events.verify(() -> ActionEventUtils.onCompletedActionEvent(ArgumentMatchers.eq(5L), ArgumentMatchers.eq(6L),
                    ArgumentMatchers.eq(EventVO.LEVEL_ERROR), ArgumentMatchers.eq(KubernetesClusterEventTypes.EVENT_KUBERNETES_CLUSTER_NODES_ADD),
                    ArgumentMatchers.argThat((String d) -> d.length() == 1024 && d.endsWith("...")), ArgumentMatchers.eq(10L),
                    ArgumentMatchers.eq(ApiCommandResourceType.KubernetesCluster.toString()), ArgumentMatchers.eq(0L)));
        } finally {
            CallContext.unregister();
        }
    }

    private KubernetesClusterAddWorker spyForFailure() {
        KubernetesClusterAddWorker worker = Mockito.spy(addWorker);
        Mockito.doNothing().when(worker).recordNodesAdditionFailure(Mockito.anyString());
        Mockito.doNothing().when(worker).detachCksIsoFromNodesAddedToCluster(Mockito.anyList(), Mockito.anyLong(), Mockito.anyBoolean());
        return worker;
    }

    @Test
    public void testFailNodesAdditionWaitsForReadyNodesBeforeDetachOnPartialAdd() {
        KubernetesClusterAddWorker worker = spyForFailure();
        worker.finalNodeIds.add(2L);
        Mockito.doReturn(true).when(worker).waitForReadyNodes();
        CloudRuntimeException e = worker.failNodesAddition(List.of(1L, 2L), List.of(1L), false);
        String msg = "Failed to add 1 of 2 nodes to Kubernetes cluster k8s (uuid-k8s). Not added: node-a (uuid-a). Added: node-b (uuid-b)";
        Assert.assertEquals(msg, e.getMessage());
        InOrder inOrder = Mockito.inOrder(worker);
        inOrder.verify(worker).detachCksIsoFromNodesAddedToCluster(List.of(1L), 0L, false);
        inOrder.verify(worker).waitForReadyNodes();
        inOrder.verify(worker).detachCksIsoFromNodesAddedToCluster(List.of(2L), 0L, false);
        inOrder.verify(worker).recordNodesAdditionFailure(msg);
    }

    @Test
    public void testFailNodesAdditionKeepsVirtualRouterIsoUntilReadyOnPartialAdd() {
        KubernetesClusterAddWorker worker = spyForFailure();
        worker.finalNodeIds.add(2L);
        Mockito.doReturn(false).when(worker).waitForReadyNodes();
        CloudRuntimeException e = worker.failNodesAddition(List.of(1L, 2L), List.of(1L), true);
        Assert.assertTrue(e.getMessage().endsWith("Added: node-b (uuid-b). Not every node of the cluster was Ready before the add-node timeout, so the added nodes may not have joined"));
        InOrder inOrder = Mockito.inOrder(worker);
        inOrder.verify(worker).waitForReadyNodes();
        inOrder.verify(worker).detachCksIsoFromNodesAddedToCluster(List.of(2L), 0L, true);
        inOrder.verify(worker).recordNodesAdditionFailure(e.getMessage());
        Mockito.verify(worker, Mockito.times(1)).detachCksIsoFromNodesAddedToCluster(Mockito.anyList(), Mockito.anyLong(), Mockito.anyBoolean());
    }

    @Test
    public void testFailNodesAdditionDetachesWhenReadyCheckThrows() {
        KubernetesClusterAddWorker worker = spyForFailure();
        worker.finalNodeIds.add(2L);
        Mockito.doThrow(new CloudRuntimeException("no ssh")).when(worker).waitForReadyNodes();
        CloudRuntimeException e = worker.failNodesAddition(List.of(1L, 2L), List.of(1L), false);
        Assert.assertTrue(e.getMessage().endsWith(". Could not check whether the added nodes joined: no ssh"));
        Mockito.verify(worker).detachCksIsoFromNodesAddedToCluster(List.of(2L), 0L, false);
    }

    @Test
    public void testFailNodesAdditionDetachesAtOnceWhenNoNodeAdded() {
        KubernetesClusterAddWorker worker = spyForFailure();
        CloudRuntimeException e = worker.failNodesAddition(List.of(1L), List.of(1L), false);
        Assert.assertEquals("Failed to add 1 of 1 nodes to Kubernetes cluster k8s (uuid-k8s). Not added: node-a (uuid-a). Added: none", e.getMessage());
        Mockito.verify(worker, Mockito.never()).waitForReadyNodes();
        Mockito.verify(worker).detachCksIsoFromNodesAddedToCluster(List.of(1L), 0L, false);
    }

    @Test
    public void testFailNodesAdditionDetachesVirtualRouterIsoWhenNoNodeAdded() {
        KubernetesClusterAddWorker worker = spyForFailure();
        worker.failNodesAddition(List.of(1L), List.of(1L), true);
        Mockito.verify(worker, Mockito.never()).waitForReadyNodes();
        Mockito.verify(worker).detachCksIsoFromNodesAddedToCluster(Mockito.anyList(), Mockito.eq(0L), Mockito.eq(true));
    }

    @Test
    public void testFailNodesAdditionKeepsMessageWhenDetachFails() {
        KubernetesClusterAddWorker worker = spyForFailure();
        Mockito.doThrow(new CloudRuntimeException("detach")).when(worker).detachCksIsoFromNodesAddedToCluster(Mockito.anyList(), Mockito.anyLong(), Mockito.anyBoolean());
        CloudRuntimeException e = worker.failNodesAddition(List.of(1L), List.of(1L), false);
        Assert.assertTrue(e.getMessage().startsWith("Failed to add 1 of 1 nodes"));
        Mockito.verify(worker).recordNodesAdditionFailure(e.getMessage());
    }
}
