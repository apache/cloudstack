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

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.KubernetesClusterVO;
import com.cloud.kubernetes.cluster.KubernetesClusterVmMapVO;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.cluster.utils.NodeAccess;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

public class KubernetesClusterScaleWorkerOfferingTest {
    private final KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
    private final KubernetesClusterManagerImpl manager = Mockito.mock(KubernetesClusterManagerImpl.class);
    private final KubernetesClusterVmMapDao vmMapDao = Mockito.mock(KubernetesClusterVmMapDao.class);
    private final KubernetesClusterNodeOfferingResizer resizer = Mockito.mock(KubernetesClusterNodeOfferingResizer.class);
    private final PortForwardingRulesDao portForwardingRulesDao = Mockito.mock(PortForwardingRulesDao.class);
    private final IPAddressDao ipAddressDao = Mockito.mock(IPAddressDao.class);
    private final Network network = Mockito.mock(Network.class);
    private final ServiceOfferingVO target = Mockito.mock(ServiceOfferingVO.class);
    private final KubernetesClusterVmMapVO firstNode = Mockito.mock(KubernetesClusterVmMapVO.class);
    private final KubernetesClusterVmMapVO secondNode = Mockito.mock(KubernetesClusterVmMapVO.class);
    private final UserVm vm = Mockito.mock(UserVm.class);
    private KubernetesClusterScaleWorker worker;

    @Before
    public void setUp() {
        Mockito.when(cluster.getId()).thenReturn(100L);
        Mockito.when(cluster.getName()).thenReturn("test-cluster");
        Mockito.when(cluster.getUuid()).thenReturn("cluster-uuid");
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(cluster.getNodeCount()).thenReturn(2L);
        Mockito.when(cluster.getTotalNodeCount()).thenReturn(2L);
        worker = Mockito.spy(new KubernetesClusterScaleWorker(cluster, new HashMap<>(), null, null, null, null, null, manager));
        Mockito.when(cluster.getState()).thenReturn(KubernetesCluster.State.Scaling);
        worker.kubernetesClusterVmMapDao = vmMapDao;
        worker.portForwardingRulesDao = portForwardingRulesDao;
        worker.ipAddressDao = ipAddressDao;
        ReflectionTestUtils.setField(worker, "network", network);
        ReflectionTestUtils.setField(worker, "scaleTimeoutTime", System.currentTimeMillis() + 60000L);
        Mockito.doReturn(resizer).when(worker).createNodeOfferingResizer();
        Mockito.doReturn(true).when(worker).stateTransitTo(anyLong(), any());
        Mockito.doNothing().when(worker).deleteTemplateLaunchPermission();
        Mockito.doReturn(Mockito.mock(KubernetesClusterVO.class)).when(worker).updateKubernetesClusterEntry(
                any(), any(), any(), any(), any(), any(), any(), eq(WORKER), anyBoolean(), anyBoolean());
        Mockito.doReturn(new Pair<>("control", 2222)).when(worker).getKubernetesClusterServerIpSshPort(null);
        Mockito.doReturn("cloud").when(worker).getControlNodeLoginUser();
        Mockito.doReturn(null).when(worker).getManagementServerSshPublicKeyFile();
        List<KubernetesClusterVmMapVO> nodes = List.of(firstNode, secondNode);
        Mockito.when(vmMapDao.listByClusterId(100L)).thenReturn(nodes);
        Mockito.when(vmMapDao.listByClusterIdAndVmType(100L, WORKER)).thenReturn(nodes);
        Mockito.when(target.getId()).thenReturn(20L);
        Mockito.when(target.getCpu()).thenReturn(4);
        Mockito.when(target.getRamSize()).thenReturn(4096);
        Mockito.when(network.getId()).thenReturn(200L);
        Mockito.when(vm.getId()).thenReturn(1L);
        Mockito.when(vm.getUuid()).thenReturn("vm-uuid");
        Mockito.when(vm.getPrivateIpAddress()).thenReturn("10.1.1.11");
    }

    @Test
    public void testNodesAreResizedOneAfterAnotherBeforeTheClusterIsUpdated() {
        scale();

        InOrder order = Mockito.inOrder(resizer, worker);
        order.verify(resizer).resize(eq(firstNode), eq(WORKER), eq(target), anyLong());
        order.verify(resizer).resize(eq(secondNode), eq(WORKER), eq(target), anyLong());
        order.verify(worker).updateKubernetesClusterEntry(eq(8L), eq(8192L), any(), eq(20L), any(), any(), any(), eq(WORKER), eq(true), eq(false));
        Mockito.verify(worker, Mockito.never()).stateTransitTo(100L, KubernetesCluster.Event.OperationFailed);
    }

    @Test
    public void testNodeFailureMarksTheScaleFailedAndStopsBeforeTheNextNode() {
        CloudRuntimeException failure = new CloudRuntimeException("unable to scale WORKER node VM worker-1");
        Mockito.doThrow(failure).when(resizer).resize(eq(firstNode), eq(WORKER), eq(target), anyLong());

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, this::scale);

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("unable to scale WORKER node VM worker-1"));
        Mockito.verify(worker).stateTransitTo(100L, KubernetesCluster.Event.OperationFailed);
        Mockito.verify(resizer, Mockito.never()).resize(eq(secondNode), any(), any(), anyLong());
        assertClusterNotUpdated();
    }

    @Test
    public void testUnexpectedErrorFromANodeAlsoMarksTheScaleFailed() {
        NullPointerException failure = new NullPointerException();
        Mockito.doThrow(failure).when(resizer).resize(eq(firstNode), eq(WORKER), eq(target), anyLong());

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, this::scale);

        Assert.assertSame(failure, error.getCause());
        Assert.assertTrue(error.getMessage(), error.getMessage().contains("NullPointerException"));
        Mockito.verify(worker).stateTransitTo(100L, KubernetesCluster.Event.OperationFailed);
        assertClusterNotUpdated();
    }

    @Test
    public void testExpiredTimeoutStopsBeforeTheNextNode() {
        Mockito.doAnswer(invocation -> {
            ReflectionTestUtils.setField(worker, "scaleTimeoutTime", System.currentTimeMillis() - 1000L);
            return null;
        }).when(resizer).resize(eq(firstNode), eq(WORKER), eq(target), anyLong());

        CloudRuntimeException error = Assert.assertThrows(CloudRuntimeException.class, this::scale);

        Assert.assertTrue(error.getMessage(), error.getMessage().contains("timed out after 1 of 2 WORKER VMs were scaled"));
        Mockito.verify(resizer, Mockito.never()).resize(eq(secondNode), any(), any(), anyLong());
        Mockito.verify(worker).stateTransitTo(100L, KubernetesCluster.Event.OperationFailed);
        assertClusterNotUpdated();
    }

    @Test
    public void testTimeoutReachedWhileTheLastNodeFinishesIsNotAFailure() {
        Mockito.doAnswer(invocation -> {
            ReflectionTestUtils.setField(worker, "scaleTimeoutTime", System.currentTimeMillis() - 1000L);
            return null;
        }).when(resizer).resize(eq(secondNode), eq(WORKER), eq(target), anyLong());

        scale();

        Mockito.verify(worker, Mockito.never()).stateTransitTo(100L, KubernetesCluster.Event.OperationFailed);
        Mockito.verify(worker).updateKubernetesClusterEntry(any(), any(), any(), eq(20L), any(), any(), any(), eq(WORKER), eq(true), eq(false));
    }

    @Test
    public void testCreatedClusterUpdatesMetadataWithoutTouchingVms() {
        ReflectionTestUtils.setField(worker, "originalState", KubernetesCluster.State.Created);

        scale();

        Mockito.verifyNoInteractions(resizer);
        Mockito.verify(worker).updateKubernetesClusterEntry(eq(8L), eq(8192L), any(), eq(20L), any(), any(), any(), eq(WORKER), eq(true), eq(false));
    }

    @Test
    public void testDirectAccessUsesThePrivateAddressOfTheNode() {
        Mockito.when(manager.isDirectAccess(network)).thenReturn(true);

        NodeAccess access = worker.resolveNodeAccess(vm);

        Assert.assertEquals("control", access.getControlAddress());
        Assert.assertEquals(2222, access.getControlPort());
        Assert.assertEquals("10.1.1.11", access.getNodeAddress());
        Assert.assertEquals(22, access.getNodePort());
        Mockito.verifyNoInteractions(portForwardingRulesDao);
    }

    @Test
    public void testNodeAccessUsesThePublicIpAndPortOfTheSshRule() {
        PortForwardingRuleVO udpRule = sshRule("udp", FirewallRule.State.Active, 4L, 2222);
        PortForwardingRuleVO revokedRule = sshRule("tcp", FirewallRule.State.Revoke, 4L, 2222);
        PortForwardingRuleVO natRule = sshRule("tcp", FirewallRule.State.Active, 5L, 2223);
        Mockito.when(portForwardingRulesDao.listByVm(1L)).thenReturn(List.of(udpRule, revokedRule, natRule));
        IPAddressVO natIp = Mockito.mock(IPAddressVO.class);
        Mockito.when(natIp.getAddress()).thenReturn(new Ip("203.0.113.5"));
        Mockito.when(ipAddressDao.findById(5L)).thenReturn(natIp);

        NodeAccess access = worker.resolveNodeAccess(vm);

        Assert.assertEquals("control", access.getControlAddress());
        Assert.assertEquals("203.0.113.5", access.getNodeAddress());
        Assert.assertEquals(2223, access.getNodePort());
        // On Netris VPC tiers the cluster's public IP is the load balancer IP, not the NAT IP of the SSH rules.
        Mockito.verify(worker, Mockito.never()).getVpcTierKubernetesPublicIp(any());
        Mockito.verify(worker, Mockito.never()).getNetworkSourceNatIp(any());
    }

    @Test
    public void testMissingSshRuleFails() {
        PortForwardingRuleVO revokedRule = sshRule("tcp", FirewallRule.State.Revoke, 5L, 2223);
        Mockito.when(portForwardingRulesDao.listByVm(1L)).thenReturn(List.of(revokedRule));

        Assert.assertThrows(CloudRuntimeException.class, () -> worker.resolveNodeAccess(vm));
    }

    @Test
    public void testMissingIpOfTheSshRuleFails() {
        PortForwardingRuleVO ruleWithoutIp = sshRule("tcp", FirewallRule.State.Active, 5L, 2223);
        Mockito.when(portForwardingRulesDao.listByVm(1L)).thenReturn(List.of(ruleWithoutIp));

        Assert.assertThrows(CloudRuntimeException.class, () -> worker.resolveNodeAccess(vm));
    }

    @Test
    public void testMissingControlPlaneAddressFails() {
        Mockito.doReturn(new Pair<>(null, 2222)).when(worker).getKubernetesClusterServerIpSshPort(null);

        Assert.assertThrows(CloudRuntimeException.class, () -> worker.resolveNodeAccess(vm));
    }

    private void scale() {
        worker.scaleKubernetesClusterOffering(WORKER, target, true, false);
    }

    private void assertClusterNotUpdated() {
        Mockito.verify(worker, Mockito.never()).updateKubernetesClusterEntry(
                any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
    }

    private PortForwardingRuleVO sshRule(String protocol, FirewallRule.State state, long sourceIpId, int sourcePort) {
        PortForwardingRuleVO rule = Mockito.mock(PortForwardingRuleVO.class);
        Mockito.when(rule.getDestinationPortStart()).thenReturn(22);
        Mockito.when(rule.getProtocol()).thenReturn(protocol);
        Mockito.when(rule.getNetworkId()).thenReturn(200L);
        Mockito.when(rule.getState()).thenReturn(state);
        Mockito.when(rule.getSourceIpAddressId()).thenReturn(sourceIpId);
        Mockito.when(rule.getSourcePortStart()).thenReturn(sourcePort);
        return rule;
    }
}
