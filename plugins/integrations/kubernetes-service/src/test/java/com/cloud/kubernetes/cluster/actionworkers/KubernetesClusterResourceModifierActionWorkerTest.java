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

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesClusterManagerImpl;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
    private IPAddressDao ipAddressDaoMock;

    @Mock
    private Account accountMock;

    private TestKubernetesClusterResourceModifierActionWorker kubernetesClusterResourceModifierActionWorker;

    private static class TestKubernetesClusterResourceModifierActionWorker extends KubernetesClusterResourceModifierActionWorker {
        private int provisionAclRuleCalls;
        private int removeAclRuleCalls;

        TestKubernetesClusterResourceModifierActionWorker(KubernetesCluster kubernetesCluster, KubernetesClusterManagerImpl clusterManager) {
            super(kubernetesCluster, clusterManager);
        }

        @Override
        protected void provisionVpcTierAllowPortACLRule(Network network, int startPort, int endPort) {
            provisionAclRuleCalls++;
        }

        @Override
        protected void removeVpcTierAllowPortACLRule(Network network, int startPort, int endPort) {
            removeAclRuleCalls++;
        }
    }

    private static class TestKubernetesClusterStartWorker extends KubernetesClusterStartWorker {
        private int provisionAclRuleCalls;
        private int provisionPortForwardingRuleCalls;

        TestKubernetesClusterStartWorker(KubernetesCluster kubernetesCluster, KubernetesClusterManagerImpl clusterManager) {
            super(kubernetesCluster, clusterManager);
        }

        @Override
        protected void provisionVpcTierAllowPortACLRule(Network network, int startPort, int endPort) {
            provisionAclRuleCalls++;
        }

        @Override
        protected void provisionPublicIpPortForwardingRule(IpAddress publicIp, Network network, Account account,
                long vmId, int sourcePort, int destPort) {
            provisionPortForwardingRuleCalls++;
        }
    }

    @Before
    public void setUp() {
        kubernetesClusterManagerMock.kubernetesClusterDao = kubernetesClusterDaoMock;
        kubernetesClusterManagerMock.kubernetesSupportedVersionDao = kubernetesSupportedVersionDaoMock;
        kubernetesClusterManagerMock.kubernetesClusterDetailsDao = kubernetesClusterDetailsDaoMock;
        kubernetesClusterManagerMock.kubernetesClusterVmMapDao = kubernetesClusterVmMapDaoMock;

        kubernetesClusterResourceModifierActionWorker = new TestKubernetesClusterResourceModifierActionWorker(kubernetesClusterMock, kubernetesClusterManagerMock);
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

    @Test
    public void createVpcTierAclRulesWithoutAclProvisionsRules() throws Exception {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getNetworkACLId()).thenReturn(null);

        try (MockedStatic<CallContext> ignored = Mockito.mockStatic(CallContext.class)) {
            kubernetesClusterResourceModifierActionWorker.createVpcTierAclRules(network);
        }

        Assert.assertEquals(2, kubernetesClusterResourceModifierActionWorker.provisionAclRuleCalls);
    }

    @Test
    public void createVpcTierAclRulesWithDefaultAllowDoesNotProvisionRules() throws Exception {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getNetworkACLId()).thenReturn(NetworkACL.DEFAULT_ALLOW);

        kubernetesClusterResourceModifierActionWorker.createVpcTierAclRules(network);

        Assert.assertEquals(0, kubernetesClusterResourceModifierActionWorker.provisionAclRuleCalls);
    }

    @Test
    public void removeVpcTierAclRulesWithoutAclDoesNotRemoveRules() throws Exception {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getNetworkACLId()).thenReturn(null);

        kubernetesClusterResourceModifierActionWorker.removeVpcTierAclRules(network);

        Assert.assertEquals(0, kubernetesClusterResourceModifierActionWorker.removeAclRuleCalls);
    }

    @Test
    public void removeVpcTierAclRulesWithCustomAclRemovesRules() throws Exception {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getNetworkACLId()).thenReturn(3L);

        kubernetesClusterResourceModifierActionWorker.removeVpcTierAclRules(network);

        Assert.assertEquals(2, kubernetesClusterResourceModifierActionWorker.removeAclRuleCalls);
    }

    @Test
    public void setupKubernetesEtcdNetworkRulesWithoutAclProvisionsAclRule() throws Exception {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getVpcId()).thenReturn(1L);
        Mockito.when(network.getNetworkACLId()).thenReturn(null);
        UserVm etcdVm = Mockito.mock(UserVm.class);
        Mockito.when(etcdVm.getId()).thenReturn(1L);
        IPAddressVO publicIp = Mockito.mock(IPAddressVO.class);
        Mockito.when(ipAddressDaoMock.findByIpAndDcId(Mockito.anyLong(), Mockito.anyString())).thenReturn(publicIp);

        TestKubernetesClusterStartWorker worker = new TestKubernetesClusterStartWorker(kubernetesClusterMock, kubernetesClusterManagerMock);
        worker.ipAddressDao = ipAddressDaoMock;
        worker.owner = accountMock;
        worker.publicIpAddress = "192.0.2.1";

        worker.setupKubernetesEtcdNetworkRules(List.of(etcdVm), network);

        Assert.assertEquals(1, worker.provisionAclRuleCalls);
        Assert.assertEquals(1, worker.provisionPortForwardingRuleCalls);
    }
}
