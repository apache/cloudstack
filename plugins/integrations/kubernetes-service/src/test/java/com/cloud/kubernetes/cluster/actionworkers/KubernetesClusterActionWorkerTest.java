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

import java.util.UUID;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterDetailsVO;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterActionWorkerTest {

    @Mock
    KubernetesClusterDao kubernetesClusterDao;

    @Mock
    KubernetesClusterVmMapDao kubernetesClusterVmMapDao;

    @Mock
    KubernetesClusterDetailsDao kubernetesClusterDetailsDao;

    @Mock
    KubernetesSupportedVersionDao kubernetesSupportedVersionDao;

    @Mock
    KubernetesClusterManagerImpl kubernetesClusterManager;

    @Mock
    IPAddressDao ipAddressDao;

    KubernetesClusterActionWorker actionWorker = null;

    final static Long DEFAULT_ID = 1L;

    @Before
    public void setUp() throws Exception {
        kubernetesClusterManager.kubernetesClusterDao = kubernetesClusterDao;
        kubernetesClusterManager.kubernetesSupportedVersionDao = kubernetesSupportedVersionDao;
        kubernetesClusterManager.kubernetesClusterDetailsDao = kubernetesClusterDetailsDao;
        kubernetesClusterManager.kubernetesClusterVmMapDao = kubernetesClusterVmMapDao;
        KubernetesCluster kubernetesCluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(kubernetesCluster.getId()).thenReturn(DEFAULT_ID);
        actionWorker = new KubernetesClusterActionWorker(kubernetesCluster, kubernetesClusterManager);
        actionWorker.ipAddressDao = ipAddressDao;
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullDetail() {
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(Mockito.mock(Network.class));
        Assert.assertNull(result);
    }

    private String mockClusterPublicIpDetail(boolean isNull) {
        String uuid = isNull ? null : UUID.randomUUID().toString();
        KubernetesClusterDetailsVO detailsVO = new KubernetesClusterDetailsVO(DEFAULT_ID, ApiConstants.PUBLIC_IP_ID, uuid, false);
        Mockito.when(kubernetesClusterDetailsDao.findDetail(DEFAULT_ID, ApiConstants.PUBLIC_IP_ID)).thenReturn(detailsVO);
        return uuid;
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullDetailValue() {
        mockClusterPublicIpDetail(true);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(Mockito.mock(Network.class));
        Assert.assertNull(result);
    }

    private Network mockNetworkForGetVpcTierKubernetesPublicIpTest() {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getVpcId()).thenReturn(DEFAULT_ID);
        return network;
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpNullVpc() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNull(result);
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpDifferentVpc() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(address.getVpcId()).thenReturn(2L);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNull(result);
    }

    @Test
    public void testGetVpcTierKubernetesPublicIpValid() {
        String uuid = mockClusterPublicIpDetail(false);
        IPAddressVO address = Mockito.mock(IPAddressVO.class);
        Mockito.when(address.getVpcId()).thenReturn(DEFAULT_ID);
        Mockito.when(ipAddressDao.findByUuid(uuid)).thenReturn(address);
        IpAddress result = actionWorker.getVpcTierKubernetesPublicIp(mockNetworkForGetVpcTierKubernetesPublicIpTest());
        Assert.assertNotNull(result);
    }

    private KubernetesClusterVO mockClusterInState(KubernetesCluster.State state) {
        KubernetesClusterVO cluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(cluster.getState()).thenReturn(state);
        Mockito.when(kubernetesClusterDao.findById(DEFAULT_ID)).thenReturn(cluster);
        return cluster;
    }

    @Test
    public void testTransitToOperationStateMovesCluster() {
        KubernetesClusterVO cluster = mockClusterInState(KubernetesCluster.State.Running);
        Mockito.when(kubernetesClusterDao.updateState(KubernetesCluster.State.Running, KubernetesCluster.Event.UpgradeRequested,
                KubernetesCluster.State.Upgrading, cluster, null)).thenReturn(true);
        Assert.assertTrue(actionWorker.transitToOperationState(KubernetesCluster.Event.UpgradeRequested));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testTransitToOperationStateFailsWhenAnotherOperationMovedClusterFirst() {
        KubernetesClusterVO cluster = mockClusterInState(KubernetesCluster.State.Running);
        Mockito.when(kubernetesClusterDao.updateState(KubernetesCluster.State.Running, KubernetesCluster.Event.StopRequested,
                KubernetesCluster.State.Stopping, cluster, null)).thenReturn(false);
        // the compare-and-set misses even where the operation could go on without a transition: the other operation owns the state now
        actionWorker.transitToOperationState(KubernetesCluster.Event.StopRequested, KubernetesCluster.State.Running);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testTransitToOperationStateFailsWithoutTransitionInOtherStates() {
        mockClusterInState(KubernetesCluster.State.Scaling);
        actionWorker.transitToOperationState(KubernetesCluster.Event.DestroyRequested, KubernetesCluster.State.Created, KubernetesCluster.State.Destroying);
    }

    @Test
    public void testTransitToOperationStateGoesOnWithoutTransitionInGivenStates() {
        mockClusterInState(KubernetesCluster.State.Destroying);
        Assert.assertFalse(actionWorker.transitToOperationState(KubernetesCluster.Event.DestroyRequested, KubernetesCluster.State.Created, KubernetesCluster.State.Destroying));
        Mockito.verify(kubernetesClusterDao, Mockito.never()).updateState(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testStateTransitToFollowsTheStatesTheOperationMovesTheClusterTo() {
        KubernetesClusterVO cluster = mockClusterInState(KubernetesCluster.State.Running);
        Mockito.when(kubernetesClusterDao.updateState(Mockito.eq(KubernetesCluster.State.Running), Mockito.eq(KubernetesCluster.Event.UpgradeRequested),
                Mockito.eq(KubernetesCluster.State.Upgrading), Mockito.eq(cluster), Mockito.isNull())).thenAnswer(invocation -> {
                    Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Upgrading);
                    return true;
                });
        Mockito.when(kubernetesClusterDao.updateState(Mockito.eq(KubernetesCluster.State.Upgrading), Mockito.eq(KubernetesCluster.Event.OperationFailed),
                Mockito.eq(KubernetesCluster.State.Alert), Mockito.eq(cluster), Mockito.isNull())).thenAnswer(invocation -> {
                    Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Alert);
                    return true;
                });
        Mockito.when(kubernetesClusterDao.updateState(KubernetesCluster.State.Alert, KubernetesCluster.Event.OperationSucceeded,
                KubernetesCluster.State.Running, cluster, null)).thenReturn(true);

        actionWorker.transitToOperationState(KubernetesCluster.Event.UpgradeRequested);
        Assert.assertTrue(actionWorker.stateTransitTo(DEFAULT_ID, KubernetesCluster.Event.OperationFailed));
        // a step that failed and was retried successfully still completes the operation
        Assert.assertTrue(actionWorker.stateTransitTo(DEFAULT_ID, KubernetesCluster.Event.OperationSucceeded));
    }

    @Test
    public void testStateTransitToLeavesClusterAnotherOperationMovedOn() {
        KubernetesClusterVO cluster = mockClusterInState(KubernetesCluster.State.Running);
        Mockito.when(kubernetesClusterDao.updateState(Mockito.eq(KubernetesCluster.State.Running), Mockito.eq(KubernetesCluster.Event.UpgradeRequested),
                Mockito.eq(KubernetesCluster.State.Upgrading), Mockito.eq(cluster), Mockito.isNull())).thenAnswer(invocation -> {
                    Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Upgrading);
                    return true;
                });
        actionWorker.transitToOperationState(KubernetesCluster.Event.UpgradeRequested);
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Scaling);

        Assert.assertFalse(actionWorker.stateTransitTo(DEFAULT_ID, KubernetesCluster.Event.OperationFailed));
    }

    @Test
    public void testStateTransitToLeavesClusterTheOperationWentOnWithoutMoving() {
        mockClusterInState(KubernetesCluster.State.Error);
        Assert.assertFalse(actionWorker.transitToOperationState(KubernetesCluster.Event.StopRequested, KubernetesCluster.State.Error));
        Assert.assertFalse(actionWorker.stateTransitTo(DEFAULT_ID, KubernetesCluster.Event.OperationSucceeded));
        Mockito.verify(kubernetesClusterDao, Mockito.never()).updateState(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void testStartTakesClusterInAlert() {
        KubernetesClusterVO cluster = mockClusterInState(KubernetesCluster.State.Alert);
        Mockito.when(kubernetesClusterDao.updateState(KubernetesCluster.State.Alert, KubernetesCluster.Event.StartRequested,
                KubernetesCluster.State.Starting, cluster, null)).thenReturn(true);
        Assert.assertTrue(actionWorker.transitToOperationState(KubernetesCluster.Event.StartRequested));
    }

    @Test
    public void testStateTransitToKeepsTheStateAfterAnEventWithoutTransition() {
        KubernetesClusterVO cluster = mockClusterInState(KubernetesCluster.State.Running);
        Mockito.when(kubernetesClusterDao.updateState(Mockito.eq(KubernetesCluster.State.Running), Mockito.eq(KubernetesCluster.Event.ScaleUpRequested),
                Mockito.eq(KubernetesCluster.State.Scaling), Mockito.eq(cluster), Mockito.isNull())).thenAnswer(invocation -> {
                    Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Scaling);
                    return true;
                });
        Mockito.when(kubernetesClusterDao.updateState(KubernetesCluster.State.Scaling, KubernetesCluster.Event.OperationFailed,
                KubernetesCluster.State.Alert, cluster, null)).thenReturn(true);

        actionWorker.transitToOperationState(KubernetesCluster.Event.ScaleUpRequested);
        Assert.assertFalse(actionWorker.stateTransitTo(DEFAULT_ID, KubernetesCluster.Event.CreateFailed));
        Assert.assertTrue(actionWorker.stateTransitTo(DEFAULT_ID, KubernetesCluster.Event.OperationFailed));
    }
}
