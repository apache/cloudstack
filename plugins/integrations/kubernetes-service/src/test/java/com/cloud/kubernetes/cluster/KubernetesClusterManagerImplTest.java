/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.kubernetes.cluster;

import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.cpu.CPU;
import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterActionWorker;
import com.cloud.kubernetes.cluster.actionworkers.KubernetesClusterUpgradeWorker;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDetailsDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesSupportedVersionVO;
import com.cloud.kubernetes.version.dao.KubernetesSupportedVersionDao;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.AddVirtualMachinesToKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.RemoveVirtualMachinesFromKubernetesClusterCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.collections.MapUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.CONTROL;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.DEFAULT;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.ETCD;
import static com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType.WORKER;

@RunWith(MockitoJUnitRunner.class)
public class KubernetesClusterManagerImplTest {

    @Mock
    FirewallRulesDao firewallRulesDao;

    @Mock
    VMTemplateDao templateDao;

    @Mock
    TemplateJoinDao templateJoinDao;

    @Mock
    KubernetesClusterDao kubernetesClusterDao;

    @Mock
    KubernetesClusterVmMapDao kubernetesClusterVmMapDao;

    @Mock
    KubernetesClusterDetailsDao kubernetesClusterDetailsDao;

    @Mock
    KubernetesSupportedVersionDao kubernetesSupportedVersionDao;

    @Mock
    VMInstanceDao vmInstanceDao;

    @Mock
    private AccountManager accountManager;

    @Mock
    private ServiceOfferingDao serviceOfferingDao;

    @Spy
    @InjectMocks
    KubernetesClusterManagerImpl kubernetesClusterManager;

    @Test
    public void testValidateVpcTierAllocated() {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getState()).thenReturn(Network.State.Allocated);
        kubernetesClusterManager.validateVpcTier(network);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVpcTierDefaultDenyRule() {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getState()).thenReturn(Network.State.Implemented);
        Mockito.when(network.getNetworkACLId()).thenReturn(NetworkACL.DEFAULT_DENY);
        kubernetesClusterManager.validateVpcTier(network);
    }

    @Test
    public void testValidateVpcTierValid() {
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getState()).thenReturn(Network.State.Implemented);
        Mockito.when(network.getNetworkACLId()).thenReturn(NetworkACL.DEFAULT_ALLOW);
        kubernetesClusterManager.validateVpcTier(network);
    }

    @Test
    public void validateIsolatedNetworkIpRulesNoRules() {
        long ipId = 1L;
        FirewallRule.Purpose purpose = FirewallRule.Purpose.Firewall;
        Network network = Mockito.mock(Network.class);
        Mockito.when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(ipId, purpose, NetUtils.TCP_PROTO)).thenReturn(new ArrayList<>());
        kubernetesClusterManager.validateIsolatedNetworkIpRules(ipId, FirewallRule.Purpose.Firewall, network, 3);
    }

    private FirewallRuleVO createRule(int startPort, int endPort) {
        FirewallRuleVO rule = new FirewallRuleVO(null, null, startPort, endPort, "tcp", 1, 1, 1, FirewallRule.Purpose.Firewall, List.of("0.0.0.0/0"), null, null, null, FirewallRule.TrafficType.Ingress);
        return rule;
    }

    @Test
    public void validateIsolatedNetworkIpRulesNoConflictingRules() {
        long ipId = 1L;
        FirewallRule.Purpose purpose = FirewallRule.Purpose.Firewall;
        Network network = Mockito.mock(Network.class);
        Mockito.when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(ipId, purpose, NetUtils.TCP_PROTO)).thenReturn(List.of(createRule(80, 80), createRule(443, 443)));
        kubernetesClusterManager.validateIsolatedNetworkIpRules(ipId, FirewallRule.Purpose.Firewall, network, 3);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateIsolatedNetworkIpRulesApiConflictingRules() {
        long ipId = 1L;
        FirewallRule.Purpose purpose = FirewallRule.Purpose.Firewall;
        Network network = Mockito.mock(Network.class);
        Mockito.when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(ipId, purpose, NetUtils.TCP_PROTO)).thenReturn(List.of(createRule(6440, 6445), createRule(443, 443)));
        kubernetesClusterManager.validateIsolatedNetworkIpRules(ipId, FirewallRule.Purpose.Firewall, network, 3);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateIsolatedNetworkIpRulesSshConflictingRules() {
        long ipId = 1L;
        FirewallRule.Purpose purpose = FirewallRule.Purpose.Firewall;
        Network network = Mockito.mock(Network.class);
        Mockito.when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(ipId, purpose, NetUtils.TCP_PROTO)).thenReturn(List.of(createRule(2200, KubernetesClusterActionWorker.CLUSTER_NODES_DEFAULT_START_SSH_PORT), createRule(443, 443)));
        kubernetesClusterManager.validateIsolatedNetworkIpRules(ipId, FirewallRule.Purpose.Firewall, network, 3);
    }

    @Test
    public void validateIsolatedNetworkIpRulesNearConflictingRules() {
        long ipId = 1L;
        FirewallRule.Purpose purpose = FirewallRule.Purpose.Firewall;
        Network network = Mockito.mock(Network.class);
        Mockito.when(firewallRulesDao.listByIpPurposeProtocolAndNotRevoked(ipId, purpose, NetUtils.TCP_PROTO)).thenReturn(List.of(createRule(2220, 2221), createRule(2225, 2227), createRule(6440, 6442), createRule(6444, 6446)));
        kubernetesClusterManager.validateIsolatedNetworkIpRules(ipId, FirewallRule.Purpose.Firewall, network, 3);
    }

    @Test
    public void testValidateKubernetesClusterScaleSizeNullNewSizeNoError() {
        kubernetesClusterManager.validateKubernetesClusterScaleSize(Mockito.mock(KubernetesClusterVO.class), null, 100, Mockito.mock(DataCenter.class));
    }

    @Test
    public void testValidateKubernetesClusterScaleSizeSameNewSizeNoError() {
        Long size = 2L;
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getNodeCount()).thenReturn(size);
        kubernetesClusterManager.validateKubernetesClusterScaleSize(clusterVO, size, 100, Mockito.mock(DataCenter.class));
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterScaleSizeStoppedCluster() {
        Long size = 2L;
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getNodeCount()).thenReturn(size);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Stopped);
        kubernetesClusterManager.validateKubernetesClusterScaleSize(clusterVO, 3L, 100, Mockito.mock(DataCenter.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateKubernetesClusterScaleSizeZeroNewSize() {
        Long size = 2L;
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(clusterVO.getNodeCount()).thenReturn(size);
        kubernetesClusterManager.validateKubernetesClusterScaleSize(clusterVO, 0L, 100, Mockito.mock(DataCenter.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateKubernetesClusterScaleSizeOverMaxSize() {
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(clusterVO.getControlNodeCount()).thenReturn(1L);
        kubernetesClusterManager.validateKubernetesClusterScaleSize(clusterVO, 4L, 4, Mockito.mock(DataCenter.class));
    }

    @Test
    public void testValidateKubernetesClusterScaleSizeDownscaleNoError() {
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(clusterVO.getControlNodeCount()).thenReturn(1L);
        Mockito.when(clusterVO.getNodeCount()).thenReturn(4L);
        kubernetesClusterManager.validateKubernetesClusterScaleSize(clusterVO, 2L, 10, Mockito.mock(DataCenter.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateKubernetesClusterScaleSizeUpscaleDeletedTemplate() {
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(clusterVO.getControlNodeCount()).thenReturn(1L);
        Mockito.when(clusterVO.getNodeCount()).thenReturn(2L);
        Mockito.when(templateDao.findById(Mockito.anyLong())).thenReturn(null);
        kubernetesClusterManager.validateKubernetesClusterScaleSize(clusterVO, 4L, 10, Mockito.mock(DataCenter.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateKubernetesClusterScaleSizeUpscaleNotInZoneTemplate() {
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(clusterVO.getControlNodeCount()).thenReturn(1L);
        Mockito.when(clusterVO.getNodeCount()).thenReturn(2L);
        Mockito.when(templateDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(VMTemplateVO.class));
        Mockito.when(templateJoinDao.newTemplateView(Mockito.any(VMTemplateVO.class), Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(null);
        kubernetesClusterManager.validateKubernetesClusterScaleSize(clusterVO, 4L, 10, Mockito.mock(DataCenter.class));
    }

    @Test
    public void testValidateKubernetesClusterScaleSizeUpscaleNoError() {
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getState()).thenReturn(KubernetesCluster.State.Running);
        Mockito.when(clusterVO.getControlNodeCount()).thenReturn(1L);
        Mockito.when(clusterVO.getNodeCount()).thenReturn(2L);
        Mockito.when(templateDao.findById(Mockito.anyLong())).thenReturn(Mockito.mock(VMTemplateVO.class));
        Mockito.when(templateJoinDao.newTemplateView(Mockito.any(VMTemplateVO.class), Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(List.of(Mockito.mock(TemplateJoinVO.class)));
        kubernetesClusterManager.validateKubernetesClusterScaleSize(clusterVO, 4L, 10, Mockito.mock(DataCenter.class));

    }

    @Before
    public void setUp() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        overrideDefaultConfigValue(KubernetesClusterService.KubernetesServiceEnabled, "_defaultValue", "true");
        Mockito.doNothing().when(accountManager).checkAccess(
                Mockito.any(Account.class), Mockito.any(), Mockito.anyBoolean(), Mockito.any());
    }

    @After
    public void tearDown() throws Exception {
        CallContext.unregister();
    }

    private void overrideDefaultConfigValue(final ConfigKey configKey, final String name, final Object o) throws IllegalAccessException, NoSuchFieldException {
        Field f = ConfigKey.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(configKey, o);
    }

    @Test
    public void addVmsToCluster() {
        KubernetesClusterVO cluster = Mockito.mock(KubernetesClusterVO.class);
        VMInstanceVO vm = Mockito.mock(VMInstanceVO.class);
        AddVirtualMachinesToKubernetesClusterCmd cmd = Mockito.mock(AddVirtualMachinesToKubernetesClusterCmd.class);
        List<Long> vmIds = Arrays.asList(1L, 2L, 3L);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getVmIds()).thenReturn(vmIds);
        Mockito.when(cmd.getActualCommandName()).thenReturn(BaseCmd.getCommandNameByClass(RemoveVirtualMachinesFromKubernetesClusterCmd.class));
        Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.ExternalManaged);
        Mockito.when(vmInstanceDao.findById(Mockito.anyLong())).thenReturn(vm);
        Mockito.when(kubernetesClusterDao.findById(Mockito.anyLong())).thenReturn(cluster);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterIdAndVmIdsIn(1L, vmIds)).thenReturn(Collections.emptyList());
        Assert.assertTrue(kubernetesClusterManager.addVmsToCluster(cmd));
    }

    @Test
    public void removeVmsFromCluster() {
        KubernetesClusterVO cluster = Mockito.mock(KubernetesClusterVO.class);
        RemoveVirtualMachinesFromKubernetesClusterCmd cmd = Mockito.mock(RemoveVirtualMachinesFromKubernetesClusterCmd.class);
        List<Long> vmIds = Arrays.asList(1L, 2L, 3L);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getVmIds()).thenReturn(vmIds);
        Mockito.when(cmd.getActualCommandName()).thenReturn(BaseCmd.getCommandNameByClass(RemoveVirtualMachinesFromKubernetesClusterCmd.class));
        Mockito.when(cluster.getClusterType()).thenReturn(KubernetesCluster.ClusterType.ExternalManaged);
        Mockito.when(kubernetesClusterDao.findById(Mockito.anyLong())).thenReturn(cluster);
        Assert.assertTrue(kubernetesClusterManager.removeVmsFromCluster(cmd).size() > 0);
    }

    @Test
    public void testValidateServiceOfferingNodeType() {
        Map<String, Long> map = new HashMap<>();
        map.put(WORKER.name(), 1L);
        map.put(CONTROL.name(), 2L);
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(1L)).thenReturn(serviceOffering);
        Mockito.when(serviceOffering.isDynamic()).thenReturn(false);
        Mockito.when(serviceOffering.getCpu()).thenReturn(2);
        Mockito.when(serviceOffering.getRamSize()).thenReturn(2048);
        KubernetesSupportedVersion version = Mockito.mock(KubernetesSupportedVersion.class);
        Mockito.when(version.getMinimumCpu()).thenReturn(2);
        Mockito.when(version.getMinimumRamSize()).thenReturn(2048);
        kubernetesClusterManager.validateServiceOfferingForNode(map, 1L, WORKER.name(), null, version);
        Mockito.verify(kubernetesClusterManager).validateServiceOffering(serviceOffering, version);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateServiceOfferingNodeTypeInvalidOffering() {
        Map<String, Long> map = new HashMap<>();
        map.put(WORKER.name(), 1L);
        map.put(CONTROL.name(), 2L);
        ServiceOfferingVO serviceOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(1L)).thenReturn(serviceOffering);
        Mockito.when(serviceOffering.isDynamic()).thenReturn(true);
        kubernetesClusterManager.validateServiceOfferingForNode(map, 1L, WORKER.name(), null, null);
    }

    @Test
    public void testClusterCapacity() {
        long workerOfferingId = 1L;
        long controlOfferingId = 2L;
        long workerCount = 2L;
        long controlCount = 2L;

        int workerOfferingCpus = 4;
        int workerOfferingMemory = 4096;
        int controlOfferingCpus = 2;
        int controlOfferingMemory = 2048;

        Map<String, Long> map = Map.of(WORKER.name(), workerOfferingId, CONTROL.name(), controlOfferingId);
        Map<String, Long> nodeCount = Map.of(WORKER.name(), workerCount, CONTROL.name(), controlCount);

        ServiceOfferingVO workerOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(workerOfferingId)).thenReturn(workerOffering);
        ServiceOfferingVO controlOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(controlOfferingId)).thenReturn(controlOffering);
        Mockito.when(workerOffering.getCpu()).thenReturn(workerOfferingCpus);
        Mockito.when(workerOffering.getRamSize()).thenReturn(workerOfferingMemory);
        Mockito.when(controlOffering.getCpu()).thenReturn(controlOfferingCpus);
        Mockito.when(controlOffering.getRamSize()).thenReturn(controlOfferingMemory);

        Pair<Long, Long> pair = kubernetesClusterManager.calculateClusterCapacity(map, nodeCount, 1L);
        Long expectedCpu = (workerOfferingCpus * workerCount) + (controlOfferingCpus * controlCount);
        Long expectedMemory = (workerOfferingMemory * workerCount) + (controlOfferingMemory * controlCount);
        Assert.assertEquals(expectedCpu, pair.first());
        Assert.assertEquals(expectedMemory, pair.second());
    }

    @Test
    public void testIsAnyNodeOfferingEmptyNullMap() {
        Assert.assertTrue(kubernetesClusterManager.isAnyNodeOfferingEmpty(null));
    }

    @Test
    public void testIsAnyNodeOfferingEmptyNullValue() {
        Map<String, Long> map = new HashMap<>();
        map.put(WORKER.name(), 1L);
        map.put(CONTROL.name(), null);
        map.put(ETCD.name(), 2L);
        Assert.assertTrue(kubernetesClusterManager.isAnyNodeOfferingEmpty(map));
    }

    @Test
    public void testIsAnyNodeOfferingEmpty() {
        Map<String, Long> map = new HashMap<>();
        map.put(WORKER.name(), 1L);
        map.put(CONTROL.name(), 2L);
        Assert.assertFalse(kubernetesClusterManager.isAnyNodeOfferingEmpty(map));
    }

    @Test
    public void testCreateNodeTypeToServiceOfferingMapNullMap() {
        KubernetesClusterVO clusterVO = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(clusterVO.getServiceOfferingId()).thenReturn(1L);
        ServiceOfferingVO offering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(1L)).thenReturn(offering);
        Map<String, ServiceOffering> mapping = kubernetesClusterManager.createNodeTypeToServiceOfferingMap(new HashMap<>(), null, clusterVO);
        Assert.assertFalse(MapUtils.isEmpty(mapping));
        Assert.assertTrue(mapping.containsKey(DEFAULT.name()));
        Assert.assertEquals(offering, mapping.get(DEFAULT.name()));
    }

    @Test
    public void testCreateNodeTypeToServiceOfferingMap() {
        Map<String, Long> idsMap = new HashMap<>();
        long workerOfferingId = 1L;
        long controlOfferingId = 2L;
        idsMap.put(WORKER.name(), workerOfferingId);
        idsMap.put(CONTROL.name(), controlOfferingId);

        ServiceOfferingVO workerOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(workerOfferingId)).thenReturn(workerOffering);
        ServiceOfferingVO controlOffering = Mockito.mock(ServiceOfferingVO.class);
        Mockito.when(serviceOfferingDao.findById(controlOfferingId)).thenReturn(controlOffering);

        Map<String, ServiceOffering> mapping = kubernetesClusterManager.createNodeTypeToServiceOfferingMap(idsMap, null, null);
        Assert.assertEquals(2, mapping.size());
        Assert.assertTrue(mapping.containsKey(WORKER.name()) && mapping.containsKey(CONTROL.name()));
        Assert.assertEquals(workerOffering, mapping.get(WORKER.name()));
        Assert.assertEquals(controlOffering, mapping.get(CONTROL.name()));
    }

    @Test
    public void testGetCksClusterPreferredArchDifferentArchsPreferCKSIsoArch() {
        String systemVMArch = "x86_64";
        VMTemplateVO cksIso = Mockito.mock(VMTemplateVO.class);
        Mockito.when(cksIso.getArch()).thenReturn(CPU.CPUArch.arm64);
        String cksClusterPreferredArch = kubernetesClusterManager.getCksClusterPreferredArch(systemVMArch, cksIso);
        Assert.assertEquals(CPU.CPUArch.arm64.getType(), cksClusterPreferredArch);
    }

    @Test
    public void testGetCksClusterPreferredArchSameArch() {
        String systemVMArch = "x86_64";
        VMTemplateVO cksIso = Mockito.mock(VMTemplateVO.class);
        Mockito.when(cksIso.getArch()).thenReturn(CPU.CPUArch.amd64);
        String cksClusterPreferredArch = kubernetesClusterManager.getCksClusterPreferredArch(systemVMArch, cksIso);
        Assert.assertEquals(CPU.CPUArch.amd64.getType(), cksClusterPreferredArch);
    }

    private KubernetesCluster mockClusterForUpgradeRetry(String... nodeVersions) {
        KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getKubernetesVersionId()).thenReturn(10L);
        List<KubernetesClusterVmMapVO> vmMaps = new ArrayList<>();
        for (String nodeVersion : nodeVersions) {
            KubernetesClusterVmMapVO vmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
            Mockito.when(vmMap.getNodeVersion()).thenReturn(nodeVersion);
            vmMaps.add(vmMap);
        }
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(1L)).thenReturn(vmMaps);
        mockClusterVersion("1.28.9");
        return cluster;
    }

    private void mockClusterVersion(String semanticVersion) {
        KubernetesSupportedVersionVO clusterVersion = Mockito.mock(KubernetesSupportedVersionVO.class);
        Mockito.when(clusterVersion.getSemanticVersion()).thenReturn(semanticVersion);
        Mockito.when(kubernetesSupportedVersionDao.findById(10L)).thenReturn(clusterVersion);
    }

    private KubernetesSupportedVersion mockUpgradeVersion(long id) {
        KubernetesSupportedVersion version = Mockito.mock(KubernetesSupportedVersion.class);
        Mockito.when(version.getId()).thenReturn(id);
        Mockito.lenient().when(version.getSemanticVersion()).thenReturn("1.29.2");
        return version;
    }

    @Test
    public void testIsRetryOfFailedUpgradeWithNodeAtTargetVersion() {
        KubernetesCluster cluster = mockClusterForUpgradeRetry("v1.29.2", "1.28.9", "1.28.9");
        Assert.assertTrue(kubernetesClusterManager.isRetryOfFailedUpgrade(cluster, mockUpgradeVersion(11L)));
    }

    @Test
    public void testIsRetryOfFailedUpgradeWithNoNodeAtTargetVersion() {
        KubernetesCluster cluster = mockClusterForUpgradeRetry("1.28.9", "v1.28.9");
        Assert.assertFalse(kubernetesClusterManager.isRetryOfFailedUpgrade(cluster, mockUpgradeVersion(11L)));
    }

    @Test
    public void testIsRetryOfFailedUpgradeIgnoresManuallyUpgradedNodes() {
        KubernetesCluster cluster = mockClusterForUpgradeRetry("1.28.9");
        KubernetesClusterVmMapVO manualNode = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(manualNode.isManualUpgrade()).thenReturn(true);
        Mockito.lenient().when(manualNode.getNodeVersion()).thenReturn("v1.29.2");
        List<KubernetesClusterVmMapVO> vmMaps = new ArrayList<>(kubernetesClusterVmMapDao.listByClusterId(1L));
        vmMaps.add(manualNode);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(1L)).thenReturn(vmMaps);
        Assert.assertFalse(kubernetesClusterManager.isRetryOfFailedUpgrade(cluster, mockUpgradeVersion(11L)));
    }

    @Test
    public void testIsRetryOfFailedUpgradeToCurrentVersion() {
        KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(cluster.getKubernetesVersionId()).thenReturn(10L);
        KubernetesSupportedVersion version = Mockito.mock(KubernetesSupportedVersion.class);
        Mockito.when(version.getId()).thenReturn(10L);
        Assert.assertFalse(kubernetesClusterManager.isRetryOfFailedUpgrade(cluster, version));
        Mockito.verifyNoInteractions(kubernetesClusterVmMapDao);
    }

    @Test
    public void testIsRetryOfFailedUpgradeUsesRecordedUpgradeVersion() {
        KubernetesCluster cluster = Mockito.mock(KubernetesCluster.class);
        Mockito.when(cluster.getId()).thenReturn(1L);
        Mockito.when(cluster.getKubernetesVersionId()).thenReturn(10L);
        Mockito.when(kubernetesClusterDetailsDao.findDetail(1L, KubernetesClusterUpgradeWorker.UPGRADE_VERSION_DETAIL))
                .thenReturn(new KubernetesClusterDetailsVO(1L, KubernetesClusterUpgradeWorker.UPGRADE_VERSION_DETAIL, "11", false));
        mockClusterVersion("1.28.9");
        KubernetesSupportedVersion sameVersion = Mockito.mock(KubernetesSupportedVersion.class);
        Mockito.when(sameVersion.getId()).thenReturn(11L);
        Mockito.when(sameVersion.getSemanticVersion()).thenReturn("1.29.2");
        KubernetesSupportedVersion otherVersion = Mockito.mock(KubernetesSupportedVersion.class);
        Mockito.when(otherVersion.getId()).thenReturn(12L);
        Mockito.when(otherVersion.getSemanticVersion()).thenReturn("1.29.3");
        // No node has to be at the version, for example when the first control node failed
        KubernetesSupportedVersionVO failedVersion = Mockito.mock(KubernetesSupportedVersionVO.class);
        Mockito.when(failedVersion.getSemanticVersion()).thenReturn("1.29.2");
        Mockito.when(kubernetesSupportedVersionDao.findByIdIncludingRemoved(11L)).thenReturn(failedVersion);
        KubernetesSupportedVersion equivalentVersion = Mockito.mock(KubernetesSupportedVersion.class);
        Mockito.when(equivalentVersion.getId()).thenReturn(13L);
        Mockito.when(equivalentVersion.getSemanticVersion()).thenReturn("1.29.2");
        Assert.assertTrue(kubernetesClusterManager.isRetryOfFailedUpgrade(cluster, sameVersion));
        Assert.assertTrue(kubernetesClusterManager.isRetryOfFailedUpgrade(cluster, equivalentVersion));
        Assert.assertFalse(kubernetesClusterManager.isRetryOfFailedUpgrade(cluster, otherVersion));
        Mockito.verifyNoInteractions(kubernetesClusterVmMapDao);
    }

    @Test
    public void testIsRetryOfFailedUpgradeToSameSemanticVersion() {
        // Another version row with the cluster's own semantic version, with every node recorded at it
        KubernetesCluster cluster = mockClusterForUpgradeRetry("1.29.2", "1.29.2");
        mockClusterVersion("1.29.2");
        Assert.assertFalse(kubernetesClusterManager.isRetryOfFailedUpgrade(cluster, mockUpgradeVersion(11L)));
    }
}
