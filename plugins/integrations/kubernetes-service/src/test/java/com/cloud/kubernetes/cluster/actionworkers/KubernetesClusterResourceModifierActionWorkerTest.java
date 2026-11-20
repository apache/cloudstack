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

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterResourceModifierActionWorkerTest {
    @Mock
    private KubernetesClusterDao kubernetesClusterDaoMock;

    @Mock
    private KubernetesClusterDetailsDao kubernetesClusterDetailsDaoMock;

    @Mock
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDaoMock;

    @Mock
    private KubernetesSupportedVersionDao kubernetesSupportedVersionDaoMock;

    @Mock
    private KubernetesClusterManagerImpl kubernetesClusterManagerMock;

    @Mock
    private KubernetesCluster kubernetesClusterMock;

    @Mock
    private NetworkOfferingDao networkOfferingDaoMock;

    @Mock
    private NetworkOfferingVO networkOfferingMock;

    @Mock
    private Network networkMock;

    @Mock
    private IpAddress publicIpMock;

    private KubernetesClusterResourceModifierActionWorker kubernetesClusterResourceModifierActionWorker;

    @Before
    public void setUp() {
        kubernetesClusterManagerMock.kubernetesClusterDao = kubernetesClusterDaoMock;
        kubernetesClusterManagerMock.kubernetesSupportedVersionDao = kubernetesSupportedVersionDaoMock;
        kubernetesClusterManagerMock.kubernetesClusterDetailsDao = kubernetesClusterDetailsDaoMock;
        kubernetesClusterManagerMock.kubernetesClusterVmMapDao = kubernetesClusterVmMapDaoMock;

        kubernetesClusterResourceModifierActionWorker = new KubernetesClusterResourceModifierActionWorker(kubernetesClusterMock, kubernetesClusterManagerMock);
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestReturnOriginalPrefixWhenNamingAllRequirementsAreMet() {
        String originalPrefix = "k8s-cluster-01";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldOnlyContainLowerCaseCharacters() {
        String originalPrefix = "k8s-CLUSTER-01";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldBeTruncatedWhenRequired() {
        int maxPrefixLength = 43;

        String originalPrefix = "c".repeat(maxPrefixLength + 1);
        String expectedPrefix = "c".repeat(maxPrefixLength);

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        String normalizedPrefix = kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix();
        Assert.assertEquals(expectedPrefix, normalizedPrefix);
        Assert.assertEquals(maxPrefixLength, normalizedPrefix.length());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldBeTruncatedWhenRequiredAndWhenOriginalPrefixIsInvalid() {
        int maxPrefixLength = 43;

        String originalPrefix = "1!" + "c".repeat(maxPrefixLength);
        String expectedPrefix = "k8s-1" + "c".repeat(maxPrefixLength - 5);

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        String normalizedPrefix = kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix();
        Assert.assertEquals(expectedPrefix, normalizedPrefix);
        Assert.assertEquals(maxPrefixLength, normalizedPrefix.length());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldOnlyIncludeAlphanumericCharactersAndHyphen() {
        String originalPrefix = "Cluster!@#$%^&*()_+?.-01|<>";
        String expectedPrefix = "k8s-cluster-01";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldContainClusterUuidWhenAllCharactersAreInvalid() {
        String clusterUuid = "2699b547-cb56-4a59-a2c6-331cfb21d2e4";
        String originalPrefix = "!@#$%^&*()_+?.|<>";
        String expectedPrefix = "k8s-" + clusterUuid;

        Mockito.when(kubernetesClusterMock.getUuid()).thenReturn(clusterUuid);
        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    @Test
    public void getKubernetesClusterNodeNamePrefixTestNormalizedPrefixShouldNotStartWithADigit() {
        String originalPrefix = "1 cluster";
        String expectedPrefix = "k8s-1cluster";

        Mockito.when(kubernetesClusterMock.getName()).thenReturn(originalPrefix);
        Assert.assertEquals(expectedPrefix, kubernetesClusterResourceModifierActionWorker.getKubernetesClusterNodeNamePrefix());
    }

    private KubernetesClusterResourceModifierActionWorker mockWorkerForVpcTierRules(boolean vpcFirewall) throws Exception {
        KubernetesClusterResourceModifierActionWorker worker = Mockito.spy(kubernetesClusterResourceModifierActionWorker);
        worker.networkOfferingDao = networkOfferingDaoMock;
        Mockito.when(networkMock.getNetworkOfferingId()).thenReturn(1L);
        Mockito.when(networkOfferingDaoMock.findById(1L)).thenReturn(networkOfferingMock);
        Mockito.when(networkOfferingMock.isConserveMode()).thenReturn(true);
        Mockito.doNothing().when(worker).createVpcTierAclRules(networkMock);
        Mockito.doReturn(vpcFirewall).when(worker).isVpcFirewallEnabled(networkMock);
        Mockito.doNothing().when(worker).provisionLoadBalancerRule(Mockito.eq(publicIpMock), Mockito.eq(networkMock), Mockito.any(),
                Mockito.anyList(), Mockito.anyInt());
        Mockito.doReturn(new HashMap<>()).when(worker).getVmPortMap();
        Mockito.doNothing().when(worker).provisionSshPortForwardingRules(Mockito.eq(publicIpMock), Mockito.eq(networkMock), Mockito.any(),
                Mockito.anyList(), Mockito.anyMap());
        return worker;
    }

    @Test
    public void setupKubernetesClusterVpcTierRulesTestOpensPublicIpFirewallWhenVpcFirewallIsEnabled() throws Exception {
        List<Long> clusterVmIds = List.of(10L, 11L);
        KubernetesClusterResourceModifierActionWorker worker = mockWorkerForVpcTierRules(true);
        Mockito.doReturn(new HashMap<>()).when(worker).createFirewallRules(publicIpMock, clusterVmIds, true);

        worker.setupKubernetesClusterVpcTierRules(publicIpMock, networkMock, clusterVmIds);

        // Firewall rules on a VPC public IP must follow the LB/PF rules, which associate the IP with the VR
        InOrder inOrder = Mockito.inOrder(worker);
        inOrder.verify(worker).createVpcTierAclRules(networkMock);
        inOrder.verify(worker).provisionLoadBalancerRule(Mockito.eq(publicIpMock), Mockito.eq(networkMock), Mockito.any(),
                Mockito.anyList(), Mockito.anyInt());
        inOrder.verify(worker).provisionSshPortForwardingRules(Mockito.eq(publicIpMock), Mockito.eq(networkMock), Mockito.any(),
                Mockito.anyList(), Mockito.anyMap());
        inOrder.verify(worker).createFirewallRules(publicIpMock, clusterVmIds, true);
    }

    @Test
    public void setupKubernetesClusterVpcTierRulesTestKeepsAclOnlyWhenVpcHasNoFirewall() throws Exception {
        List<Long> clusterVmIds = List.of(10L, 11L);
        KubernetesClusterResourceModifierActionWorker worker = mockWorkerForVpcTierRules(false);

        worker.setupKubernetesClusterVpcTierRules(publicIpMock, networkMock, clusterVmIds);

        Mockito.verify(worker).createVpcTierAclRules(networkMock);
        Mockito.verify(worker, Mockito.never()).createFirewallRules(Mockito.any(), Mockito.anyList(), Mockito.anyBoolean());
    }
}
