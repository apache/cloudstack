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
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.utils.net.NetUtils;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

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
    private IpAddress publicIpMock;

    @Mock
    private FirewallRulesDao firewallRulesDaoMock;

    @Mock
    private PortForwardingRulesDao portForwardingRulesDaoMock;

    @Mock
    private FirewallService firewallServiceMock;

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

    private static final long NETWORK_ID = 10L;

    private void mockSshFirewallRules(FirewallRuleVO... rules) {
        kubernetesClusterResourceModifierActionWorker.firewallRulesDao = firewallRulesDaoMock;
        kubernetesClusterResourceModifierActionWorker.portForwardingRulesDao = portForwardingRulesDaoMock;
        kubernetesClusterResourceModifierActionWorker.firewallService = firewallServiceMock;
        Mockito.when(publicIpMock.getId()).thenReturn(1L);
        Mockito.when(firewallRulesDaoMock.listByIpPurposeProtocolAndNotRevoked(1L, FirewallRule.Purpose.Firewall, NetUtils.TCP_PROTO)).thenReturn(Arrays.asList(rules));
    }

    private FirewallRuleVO mockSshForwardedFirewallRule(int port) {
        FirewallRuleVO rule = Mockito.mock(FirewallRuleVO.class);
        Mockito.when(rule.getSourcePortStart()).thenReturn(port);
        Mockito.when(rule.getSourcePortEnd()).thenReturn(port);
        PortForwardingRuleVO pfRule = Mockito.mock(PortForwardingRuleVO.class);
        Mockito.when(pfRule.getDestinationPortStart()).thenReturn(KubernetesClusterActionWorker.DEFAULT_SSH_PORT);
        Mockito.when(portForwardingRulesDaoMock.findByNetworkAndPorts(NETWORK_ID, port, port)).thenReturn(pfRule);
        return rule;
    }

    @Test
    public void removeSshFirewallRuleTestPrefersNodesRuleOverEtcdRuleListedFirst() {
        FirewallRuleVO etcdRule = mockSshForwardedFirewallRule(50000);
        FirewallRuleVO nodesRule = Mockito.mock(FirewallRuleVO.class);
        Mockito.when(nodesRule.getSourcePortStart()).thenReturn(KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT);
        Mockito.when(nodesRule.getId()).thenReturn(11L);
        mockSshFirewallRules(etcdRule, nodesRule);

        Assert.assertSame(nodesRule, kubernetesClusterResourceModifierActionWorker.removeSshFirewallRule(publicIpMock, NETWORK_ID));
        Mockito.verify(firewallServiceMock).revokeIngressFwRule(11L, true);
        Mockito.verifyNoMoreInteractions(firewallServiceMock);
    }

    @Test
    public void removeSshFirewallRuleTestFallsBackToSshForwardedRule() {
        FirewallRuleVO externalNodeRule = mockSshForwardedFirewallRule(2225);
        Mockito.when(externalNodeRule.getId()).thenReturn(12L);
        mockSshFirewallRules(externalNodeRule);

        Assert.assertSame(externalNodeRule, kubernetesClusterResourceModifierActionWorker.removeSshFirewallRule(publicIpMock, NETWORK_ID));
        Mockito.verify(firewallServiceMock).revokeIngressFwRule(12L, true);
    }

    @Test
    public void removeSshFirewallRuleTestReturnsNullWhenNoSshRule() {
        FirewallRuleVO apiRule = Mockito.mock(FirewallRuleVO.class);
        Mockito.when(apiRule.getSourcePortStart()).thenReturn(KubernetesClusterActionWorker.CLUSTER_API_PORT);
        Mockito.when(apiRule.getSourcePortEnd()).thenReturn(KubernetesClusterActionWorker.CLUSTER_API_PORT);
        mockSshFirewallRules(apiRule);

        Assert.assertNull(kubernetesClusterResourceModifierActionWorker.removeSshFirewallRule(publicIpMock, NETWORK_ID));
        Mockito.verifyNoMoreInteractions(firewallServiceMock);
    }
}
