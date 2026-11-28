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
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
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
import com.cloud.vm.VirtualMachine;
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

    private KubernetesClusterVO mockClusterForScaleState(KubernetesCluster.State state, VirtualMachine.State controlVmState) {
        KubernetesClusterVO cluster = Mockito.mock(KubernetesClusterVO.class);
        Mockito.when(cluster.getState()).thenReturn(state);
        Mockito.lenient().when(cluster.getId()).thenReturn(1L);
        Mockito.lenient().when(cluster.getServiceOfferingId()).thenReturn(1L);
        Mockito.lenient().when(cluster.getWorkerNodeServiceOfferingId()).thenReturn(2L);
        Mockito.lenient().when(cluster.getNodeCount()).thenReturn(2L);
        Mockito.lenient().when(cluster.getTotalNodeCount()).thenReturn(2L);
        Mockito.lenient().when(cluster.getAutoscalingEnabled()).thenReturn(false);
        KubernetesClusterVmMapVO controlVmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.lenient().when(controlVmMap.getVmId()).thenReturn(100L);
        Mockito.lenient().when(controlVmMap.isControlNode()).thenReturn(true);
        KubernetesClusterVmMapVO workerVmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.lenient().when(workerVmMap.getVmId()).thenReturn(10L);
        Mockito.lenient().when(kubernetesClusterVmMapDao.listByClusterId(1L)).thenReturn(List.of(controlVmMap, workerVmMap));
        VMInstanceVO controlVm = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(controlVm.getState()).thenReturn(controlVmState);
        Mockito.lenient().when(vmInstanceDao.findById(100L)).thenReturn(controlVm);
        // The usual reason for Alert: a worker VM is stopped
        VMInstanceVO workerVm = Mockito.mock(VMInstanceVO.class);
        Mockito.lenient().when(workerVm.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.lenient().when(vmInstanceDao.findById(10L)).thenReturn(workerVm);
        return cluster;
    }

    private KubernetesClusterVO mockClusterForScaleState(KubernetesCluster.State state) {
        return mockClusterForScaleState(state, VirtualMachine.State.Running);
    }

    @Test
    public void testValidateKubernetesClusterStateForScaleAlertSize() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, null, null, null);
    }

    @Test
    public void testValidateKubernetesClusterStateForScaleAlertNodeIds() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, null, List.of(10L), null, null, null, null, null);
    }

    @Test
    public void testValidateKubernetesClusterStateForScaleAlertSizeWithCurrentSettings() {
        // The offerings and autoscaling setting the cluster already has may be sent along with the new size
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, 1L, Map.of(WORKER.name(), 2L), false, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertOfferingChange() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, 5L, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertNodeOfferingChange() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, Map.of(WORKER.name(), 5L), null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertAutoscaling() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, null, null, null, null, true, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateKubernetesClusterStateForScaleAlertNothingToDo() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, null, null, 1L, null, null, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateKubernetesClusterStateForScaleAlertSameSize() {
        // Would do nothing, then move the cluster from Alert to Running
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 2L, null, null, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertControlNodeDown() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert, VirtualMachine.State.Stopped);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertAutoscalingSizeChange() {
        // Autoscaling stays disabled, but minsize/maxsize differ: the worker treats it as an autoscaling change
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, false, 1L, 3L);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertExtraVms() {
        // For example a worker VM left by a failed scale-up
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        Mockito.when(cluster.getTotalNodeCount()).thenReturn(1L);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertAllVmsRunning() {
        // In Alert for another reason, such as a failed upgrade
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        VMInstanceVO workerVm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(workerVm.getState()).thenReturn(VirtualMachine.State.Running);
        Mockito.when(vmInstanceDao.findById(10L)).thenReturn(workerVm);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertNoVmDown() {
        // A VM on a disconnected host may still be running: the worker can't work around it
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        VMInstanceVO workerVm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(workerVm.getState()).thenReturn(VirtualMachine.State.Unknown);
        Mockito.when(vmInstanceDao.findById(10L)).thenReturn(workerVm);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertEtcdVmDown() {
        // etcd may have lost quorum, so new nodes can't join
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        Mockito.when(cluster.getTotalNodeCount()).thenReturn(3L);
        KubernetesClusterVmMapVO etcdVmMap = Mockito.mock(KubernetesClusterVmMapVO.class);
        Mockito.when(etcdVmMap.getVmId()).thenReturn(200L);
        Mockito.when(etcdVmMap.isEtcdNode()).thenReturn(true);
        List<KubernetesClusterVmMapVO> vmMaps = new ArrayList<>(kubernetesClusterVmMapDao.listByClusterId(1L));
        vmMaps.add(etcdVmMap);
        Mockito.when(kubernetesClusterVmMapDao.listByClusterId(1L)).thenReturn(vmMaps);
        VMInstanceVO etcdVm = Mockito.mock(VMInstanceVO.class);
        Mockito.when(etcdVm.getState()).thenReturn(VirtualMachine.State.Stopped);
        Mockito.when(vmInstanceDao.findById(200L)).thenReturn(etcdVm);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertOnlyExternalNodeDown() {
        // Scaling never acts on external nodes, so it can't work around one that is down
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        KubernetesClusterVmMapVO workerVmMap = kubernetesClusterVmMapDao.listByClusterId(1L).get(1);
        Mockito.when(workerVmMap.isExternalNode()).thenReturn(true);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleAlertRemoveControlNode() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Alert);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, null, List.of(100L), null, null, null, null, null);
    }

    @Test
    public void testValidateKubernetesClusterStateForScaleRunningOfferingChange() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Running);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, null, null, 5L, null, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testValidateKubernetesClusterStateForScaleUpgrading() {
        KubernetesClusterVO cluster = mockClusterForScaleState(KubernetesCluster.State.Upgrading);
        kubernetesClusterManager.validateKubernetesClusterStateForScale(cluster, 3L, null, null, null, null, null, null);
    }
}
