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
package org.apache.cloudstack.network.tungsten.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.agent.AgentManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVMMapVO;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.TungstenGuestNetworkIpAddressDao;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.uservm.UserVm;
import com.cloud.utils.EncryptionUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenPortForwardingCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AssignTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkLoadbalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ReleaseTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerHealthMonitorCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerMemberCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerPoolCommand;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorDao;
import org.apache.cloudstack.network.tungsten.dao.TungstenFabricLBHealthMonitorVO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TungstenElementTest {
    @Mock
    TungstenFabricUtils tungstenFabricUtils;
    @Mock
    NetworkModel networkModel;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    LoadBalancerVMMapDao lbVmMapDao;
    @Mock
    TungstenGuestNetworkIpAddressDao tungstenGuestNetworkIpAddressDao;
    @Mock
    IpAddressManager ipAddressMgr;
    @Mock
    ConfigurationDao configDao;
    @Mock
    LoadBalancerDao lbDao;
    @Mock
    AccountManager accountMgr;
    @Mock
    HostDao hostDao;
    @Mock
    MessageBus messageBus;
    @Mock
    PhysicalNetworkTrafficTypeDao physicalNetworkTrafficTypeDao;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    TungstenProviderDao tungstenProviderDao;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    VlanDao vlanDao;
    @Mock
    NetworkServiceMapDao networkServiceMapDao;
    @Mock
    HostPodDao hostPodDao;
    @Mock
    NetworkDetailsDao networkDetailsDao;
    @Mock
    AgentManager agentManager;
    @Mock
    NetworkDao networkDao;
    @Mock
    TungstenService tungstenService;
    @Mock
    TungstenFabricLBHealthMonitorDao tungstenFabricLBHealthMonitorDao;
    @Mock
    LoadBalancerCertMapDao loadBalancerCertMapDao;

    TungstenElement tungstenElement;

    MockedStatic<ApiDBUtils> apiDBUtilsMocked;

    MockedStatic<EncryptionUtil> encryptionUtilMocked;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        tungstenElement = new TungstenElement();
        tungstenElement.tungstenFabricUtils = tungstenFabricUtils;
        tungstenElement.networkModel = networkModel;
        tungstenElement.ipAddressDao = ipAddressDao;
        tungstenElement.vmInstanceDao = vmInstanceDao;
        tungstenElement.lbVmMapDao = lbVmMapDao;
        tungstenElement.tungstenGuestNetworkIpAddressDao = tungstenGuestNetworkIpAddressDao;
        tungstenElement.ipAddressMgr = ipAddressMgr;
        tungstenElement.configDao = configDao;
        tungstenElement.lbDao = lbDao;
        tungstenElement.accountMgr = accountMgr;
        tungstenElement.hostDao = hostDao;
        tungstenElement.messageBus = messageBus;
        tungstenElement.physicalNetworkDao = physicalNetworkDao;
        tungstenElement.tungstenService = tungstenService;
        tungstenElement.tungstenProviderDao = tungstenProviderDao;
        tungstenElement.dataCenterDao = dataCenterDao;
        tungstenElement.vlanDao = vlanDao;
        tungstenElement.networkServiceMapDao = networkServiceMapDao;
        tungstenElement.podDao = hostPodDao;
        tungstenElement.agentMgr = agentManager;
        tungstenElement.networkDetailsDao = networkDetailsDao;
        tungstenElement.networkDao = networkDao;
        tungstenElement.physicalNetworkTrafficTypeDao = physicalNetworkTrafficTypeDao;
        tungstenElement.tungstenFabricLBHealthMonitorDao = tungstenFabricLBHealthMonitorDao;
        tungstenElement.loadBalancerCertMapDao = loadBalancerCertMapDao;

        apiDBUtilsMocked = Mockito.mockStatic(ApiDBUtils.class);
        encryptionUtilMocked = Mockito.mockStatic(EncryptionUtil.class);

        when(tungstenService.getTungstenProjectFqn(any())).thenReturn("default-domain:default-project");
    }

    @After
    public void tearDown() throws Exception {
        apiDBUtilsMocked.close();
        encryptionUtilMocked.close();
        closeable.close();
    }

    @Test
    public void canHandleSuccessTest() {
        Network network = mock(Network.class);
        Network.Service service = mock(Network.Service.class);

        when(networkModel.isProviderForNetwork(any(), anyLong())).thenReturn(true);

        assertTrue(tungstenElement.canHandle(network, service));
    }

    @Test
    public void canHandleFailTest() {
        Network network = mock(Network.class);
        Network.Service service = mock(Network.Service.class);

        when(networkModel.isProviderForNetwork(any(), anyLong())).thenReturn(false);

        assertFalse(tungstenElement.canHandle(network, service));
    }

    @Test
    public void applyStaticNatsAddRuleSuccessTest() {
        Network network = mock(Network.class);
        StaticNatImpl staticNat = mock(StaticNatImpl.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        TungstenAnswer assignFloatingIpAnswer = MockTungstenAnswerFactory.get(true);
        Nic nic = mock(Nic.class);
        Network publicNetwork = mock(Network.class);
        List<StaticNatImpl> staticNatList = List.of(staticNat);

        when(staticNat.isForRevoke()).thenReturn(false);
        when(ipAddressDao.findByIdIncludingRemoved(anyLong())).thenReturn(ipAddressVO);
        when(vmInstanceDao.findByIdIncludingRemoved(anyLong())).thenReturn(vmInstanceVO);
        when(networkModel.getNicInNetworkIncludingRemoved(anyLong(), anyLong())).thenReturn(nic);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(AssignTungstenFloatingIpCommand.class), anyLong())).thenReturn(assignFloatingIpAnswer);

        assertTrue(tungstenElement.applyStaticNats(network, staticNatList));
    }

    @Test
    public void applyStaticNatsAddRuleFailTest() {
        Network network = mock(Network.class);
        StaticNatImpl staticNat = mock(StaticNatImpl.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        TungstenAnswer assignFloatingIpAnswer = MockTungstenAnswerFactory.get(false);
        Nic nic = mock(Nic.class);
        Network publicNetwork = mock(Network.class);
        List<StaticNatImpl> staticNatList = List.of(staticNat);

        when(staticNat.isForRevoke()).thenReturn(false);
        when(ipAddressDao.findByIdIncludingRemoved(anyLong())).thenReturn(ipAddressVO);
        when(vmInstanceDao.findByIdIncludingRemoved(anyLong())).thenReturn(vmInstanceVO);
        when(networkModel.getNicInNetworkIncludingRemoved(anyLong(), anyLong())).thenReturn(nic);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(AssignTungstenFloatingIpCommand.class), anyLong())).thenReturn(assignFloatingIpAnswer);

        assertFalse(tungstenElement.applyStaticNats(network, staticNatList));
    }

    @Test
    public void applyStaticNatsRevokeRuleSuccessTest() {
        Network network = mock(Network.class);
        StaticNatImpl staticNat = mock(StaticNatImpl.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        TungstenAnswer releaseFloatingIpAnswer = MockTungstenAnswerFactory.get(true);
        Nic nic = mock(Nic.class);
        Network publicNetwork = mock(Network.class);
        List<StaticNatImpl> staticNatList = List.of(staticNat);

        when(staticNat.isForRevoke()).thenReturn(true);
        when(ipAddressDao.findByIdIncludingRemoved(anyLong())).thenReturn(ipAddressVO);
        when(vmInstanceDao.findByIdIncludingRemoved(anyLong())).thenReturn(vmInstanceVO);
        when(networkModel.getNicInNetworkIncludingRemoved(anyLong(), anyLong())).thenReturn(nic);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(ReleaseTungstenFloatingIpCommand.class), anyLong())).thenReturn(releaseFloatingIpAnswer);

        assertTrue(tungstenElement.applyStaticNats(network, staticNatList));
    }

    @Test
    public void applyStaticNatsRevokeRuleFailTest() {
        Network network = mock(Network.class);
        StaticNatImpl staticNat = mock(StaticNatImpl.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        TungstenAnswer releaseFloatingIpAnswer = MockTungstenAnswerFactory.get(false);
        Nic nic = mock(Nic.class);
        Network publicNetwork = mock(Network.class);
        List<StaticNatImpl> staticNatList = List.of(staticNat);

        when(staticNat.isForRevoke()).thenReturn(true);
        when(ipAddressDao.findByIdIncludingRemoved(anyLong())).thenReturn(ipAddressVO);
        when(vmInstanceDao.findByIdIncludingRemoved(anyLong())).thenReturn(vmInstanceVO);
        when(networkModel.getNicInNetworkIncludingRemoved(anyLong(), anyLong())).thenReturn(nic);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(ReleaseTungstenFloatingIpCommand.class), anyLong())).thenReturn(releaseFloatingIpAnswer);

        assertFalse(tungstenElement.applyStaticNats(network, staticNatList));
    }

    @Test
    public void applyLBRulesAddRuleSuccessTest() {
        User caller = mock(User.class);
        Network network = mock(Network.class);
        Network publicNetwork = mock(Network.class);
        Ip ip = mock(Ip.class);
        LoadBalancingRule loadBalancingRule1 = mock(LoadBalancingRule.class);
        LoadBalancerVMMapVO loadBalancerVMMapVO = mock(LoadBalancerVMMapVO.class);
        LoadBalancerVO loadBalancerVO = mock(LoadBalancerVO.class);
        LoadBalancingRule.LbStickinessPolicy lbStickinessPolicy = mock(LoadBalancingRule.LbStickinessPolicy.class);
        List<LoadBalancingRule> loadBalancingRuleList1 = List.of(loadBalancingRule1);
        List<LoadBalancerVMMapVO> loadBalancerVMMapVOList = List.of(loadBalancerVMMapVO);
        List<LoadBalancingRule.LbStickinessPolicy> lbStickinessPolicyList = List.of(lbStickinessPolicy);
        List<LoadBalancerVO> loadBalancerVOList = List.of(loadBalancerVO);
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO = mock(TungstenFabricLBHealthMonitorVO.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = mock(TungstenGuestNetworkIpAddressVO.class);
        TungstenAnswer createTungstenNetworkLoadbalancerAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenLoadBalancerPoolAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenLoadBalancerMemberAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenLoadBalancerListenerAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenHealthMonitorAnswer = MockTungstenAnswerFactory.get(true);
        LoadBalancingRule.LbSslCert lbSslCert = mock(LoadBalancingRule.LbSslCert.class);
        when(lbStickinessPolicy.getMethodName()).thenReturn("AppCookie");
        List<Pair<String, String>> pairList = List.of(new Pair<>("cookieName", "cookieValue"));

        when(lbStickinessPolicy.getParams()).thenReturn(pairList);
        when(loadBalancingRule1.getId()).thenReturn(1L);
        when(loadBalancingRule1.getState()).thenReturn(FirewallRule.State.Add);
        when(loadBalancingRule1.getAlgorithm()).thenReturn("roundrobin");
        when(loadBalancingRule1.getSourcePortStart()).thenReturn(443);
        when(loadBalancingRule1.getDefaultPortStart()).thenReturn(443);
        when(loadBalancingRule1.getStickinessPolicies()).thenReturn(lbStickinessPolicyList);
        when(loadBalancingRule1.getSourceIp()).thenReturn(ip);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddressVO);
        when(ipAddressVO.getAddress()).thenReturn(ip);
        when(lbVmMapDao.listByLoadBalancerId(anyLong(), anyBoolean())).thenReturn(loadBalancerVMMapVOList);
        when(tungstenGuestNetworkIpAddressVO.getGuestIpAddress()).thenReturn(ip);
        when(ip.addr()).thenReturn("10.10.10.10");
        when(tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(anyLong(), anyString())).thenReturn(tungstenGuestNetworkIpAddressVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkLoadbalancerCommand.class), anyLong())).thenReturn(createTungstenNetworkLoadbalancerAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenLoadBalancerPoolCommand.class), anyLong())).thenReturn(updateTungstenLoadBalancerPoolAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenLoadBalancerMemberCommand.class), anyLong())).thenReturn(updateTungstenLoadBalancerMemberAnswer);
        when(configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key())).thenReturn("enabled");
        when(tungstenService.updateLoadBalancer(any(), any())).thenReturn(true);
        when(EncryptionUtil.generateSignature(anyString(), anyString())).thenReturn("generatedString");
        when(tungstenFabricLBHealthMonitorDao.findByLbId(anyLong())).thenReturn(tungstenFabricLBHealthMonitorVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenLoadBalancerHealthMonitorCommand.class), anyLong())).thenReturn(updateTungstenHealthMonitorAnswer);

        assertTrue(tungstenElement.applyLBRules(network, loadBalancingRuleList1));
    }

    @Test
    public void applyLBRulesAddRuleFailTest() {
        Network network = mock(Network.class);
        Network publicNetwork = mock(Network.class);
        Ip ip = mock(Ip.class);
        LoadBalancingRule loadBalancingRule1 = mock(LoadBalancingRule.class);
        LoadBalancerVMMapVO loadBalancerVMMapVO = mock(LoadBalancerVMMapVO.class);
        LoadBalancerVO loadBalancerVO = mock(LoadBalancerVO.class);
        LoadBalancingRule.LbStickinessPolicy lbStickinessPolicy = mock(LoadBalancingRule.LbStickinessPolicy.class);
        List<LoadBalancingRule> loadBalancingRuleList1 = List.of(loadBalancingRule1);
        List<LoadBalancerVMMapVO> loadBalancerVMMapVOList = List.of(loadBalancerVMMapVO);
        List<LoadBalancingRule.LbStickinessPolicy> lbStickinessPolicyList = List.of(lbStickinessPolicy);
        List<LoadBalancerVO> loadBalancerVOList = List.of(loadBalancerVO);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = mock(TungstenGuestNetworkIpAddressVO.class);
        TungstenAnswer createTungstenNetworkLoadbalancerAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenLoadBalancerPoolAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenLoadBalancerMemberAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenHealthMonitorAnswer = MockTungstenAnswerFactory.get(true);
        List<Pair<String, String>> pairList = List.of(new Pair<>("cookieName", "cookieValue"));
        TungstenFabricLBHealthMonitorVO tungstenFabricLBHealthMonitorVO = mock(TungstenFabricLBHealthMonitorVO.class);

        when(lbStickinessPolicy.getMethodName()).thenReturn("AppCookie");
        when(lbStickinessPolicy.getParams()).thenReturn(pairList);
        when(loadBalancingRule1.getId()).thenReturn(1L);
        when(loadBalancingRule1.getState()).thenReturn(FirewallRule.State.Add);
        when(loadBalancingRule1.getAlgorithm()).thenReturn("roundrobin");
        when(loadBalancingRule1.getSourcePortStart()).thenReturn(80);
        when(loadBalancingRule1.getDefaultPortStart()).thenReturn(443);
        when(loadBalancingRule1.getStickinessPolicies()).thenReturn(lbStickinessPolicyList);
        when(loadBalancingRule1.getSourceIp()).thenReturn(ip);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddressVO);
        when(ipAddressVO.getAddress()).thenReturn(ip);
        when(lbVmMapDao.listByLoadBalancerId(anyLong(), anyBoolean())).thenReturn(loadBalancerVMMapVOList);
        when(ip.addr()).thenReturn("10.10.10.10");
        when(ipAddressMgr.acquireGuestIpAddress(any(), any())).thenReturn("192.168.100.100");
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkLoadbalancerCommand.class), anyLong())).thenReturn(createTungstenNetworkLoadbalancerAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenLoadBalancerPoolCommand.class), anyLong())).thenReturn(updateTungstenLoadBalancerPoolAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenLoadBalancerMemberCommand.class), anyLong())).thenReturn(updateTungstenLoadBalancerMemberAnswer);
        when(configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key())).thenReturn("disabled");
        when(tungstenFabricLBHealthMonitorDao.findByLbId(anyLong())).thenReturn(tungstenFabricLBHealthMonitorVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenLoadBalancerHealthMonitorCommand.class), anyLong())).thenReturn(updateTungstenHealthMonitorAnswer);

        assertFalse(tungstenElement.applyLBRules(network, loadBalancingRuleList1));
    }

    @Test
    public void applyLBRulesRevokeRuleFailTest() {
        Network network = mock(Network.class);
        Network publicNetwork = mock(Network.class);
        Ip ip1 = mock(Ip.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        LoadBalancingRule loadBalancingRule1 = mock(LoadBalancingRule.class);
        LoadBalancerVO loadBalancerVO1 = mock(LoadBalancerVO.class);
        List<LoadBalancingRule> loadBalancingRuleList1 = List.of(loadBalancingRule1);
        List<LoadBalancerVO> loadBalancerVOList1 = List.of(loadBalancerVO1);
        TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = mock(TungstenGuestNetworkIpAddressVO.class);
        TungstenAnswer deleteTungstenLoadBalancerListenerAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer deleteTungstenLoadBalancerCommand = MockTungstenAnswerFactory.get(true);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(loadBalancingRule1.getSourceIp()).thenReturn(ip1);
        when(loadBalancingRule1.getState()).thenReturn(FirewallRule.State.Revoke);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddressVO);
        when(ipAddressVO.getAddress()).thenReturn(ip1);
        when(ip1.addr()).thenReturn("10.10.10.10");
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenLoadBalancerCommand.class), anyLong())).thenReturn(deleteTungstenLoadBalancerCommand);
        when(lbDao.listByIpAddress(anyLong())).thenReturn(loadBalancerVOList1);
        when(tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(anyLong(),anyString())).thenReturn(tungstenGuestNetworkIpAddressVO);
        when(tungstenGuestNetworkIpAddressDao.remove(anyLong())).thenReturn(false);

        assertFalse(tungstenElement.applyLBRules(network, loadBalancingRuleList1));
    }

    @Test
    public void applyLBRulesRevokeRuleSuccessTest() {
        Network network = mock(Network.class);
        Network publicNetwork = mock(Network.class);
        Ip ip = mock(Ip.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        LoadBalancingRule loadBalancingRule = mock(LoadBalancingRule.class);
        LoadBalancerVO loadBalancerVO1 = mock(LoadBalancerVO.class);
        LoadBalancerVO loadBalancerVO2 = mock(LoadBalancerVO.class);
        TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = mock(TungstenGuestNetworkIpAddressVO.class);
        TungstenAnswer deleteTungstenLoadBalancerListenerAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer deleteTungstenLoadBalancerCommand = MockTungstenAnswerFactory.get(true);
        List<LoadBalancingRule> loadBalancingRuleList = List.of(loadBalancingRule);
        List<LoadBalancerVO> loadBalancerVOList = Arrays.asList(loadBalancerVO1, loadBalancerVO2);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(loadBalancingRule.getSourceIp()).thenReturn(ip);
        when(loadBalancingRule.getState()).thenReturn(FirewallRule.State.Revoke);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddressVO);
        when(ip.addr()).thenReturn("10.10.10.10");
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenLoadBalancerListenerCommand.class), anyLong())).thenReturn(deleteTungstenLoadBalancerListenerAnswer);
        when(tungstenService.updateLoadBalancer(any(), any())).thenReturn(true);
        when(lbDao.listByIpAddress(anyLong())).thenReturn(loadBalancerVOList);

        assertTrue(tungstenElement.applyLBRules(network, loadBalancingRuleList));
    }

    @Test
    public void applyPFRulesAddRuleSuccessTest() {
        Network network = mock(Network.class);
        Network publicNetwork = mock(Network.class);
        PortForwardingRule portForwardingRule = mock(PortForwardingRule.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        UserVm userVm = mock(UserVm.class);
        Nic nic = mock(Nic.class);
        TungstenAnswer applyTungstenPortForwardingAnswer = MockTungstenAnswerFactory.get(true);
        List<PortForwardingRule> portForwardingRuleList = List.of(portForwardingRule);

        when(portForwardingRule.getState()).thenReturn(FirewallRule.State.Add);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(ApiDBUtils.findIpAddressById(anyLong())).thenReturn(ipAddressVO);
        when(ApiDBUtils.findUserVmById(anyLong())).thenReturn(userVm);
        when(networkModel.getNicInNetwork(anyLong(), anyLong())).thenReturn(nic);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenPortForwardingCommand.class), anyLong())).thenReturn(applyTungstenPortForwardingAnswer);

        assertTrue(tungstenElement.applyPFRules(network, portForwardingRuleList));
    }

    @Test
    public void applyPFRulesAddRuleFailTest() {
        Network network = mock(Network.class);
        Network publicNetwork = mock(Network.class);
        PortForwardingRule portForwardingRule = mock(PortForwardingRule.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        UserVm userVm = mock(UserVm.class);
        Nic nic = mock(Nic.class);
        TungstenAnswer applyTungstenPortForwardingAnswer = MockTungstenAnswerFactory.get(false);
        List<PortForwardingRule> portForwardingRuleList = List.of(portForwardingRule);

        when(portForwardingRule.getState()).thenReturn(FirewallRule.State.Add);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(ApiDBUtils.findIpAddressById(anyLong())).thenReturn(ipAddressVO);
        when(ApiDBUtils.findUserVmById(anyLong())).thenReturn(userVm);
        when(networkModel.getNicInNetwork(anyLong(), anyLong())).thenReturn(nic);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenPortForwardingCommand.class), anyLong())).thenReturn(applyTungstenPortForwardingAnswer);

        assertFalse(tungstenElement.applyPFRules(network, portForwardingRuleList));
    }

    @Test
    public void applyPFRulesRevokeRuleSuccessTest() {
        Network network = mock(Network.class);
        Network publicNetwork = mock(Network.class);
        PortForwardingRule portForwardingRule = mock(PortForwardingRule.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        UserVm userVm = mock(UserVm.class);
        Nic nic = mock(Nic.class);
        TungstenAnswer applyTungstenPortForwardingAnswer = MockTungstenAnswerFactory.get(true);
        List<PortForwardingRule> portForwardingRuleList = List.of(portForwardingRule);

        when(portForwardingRule.getState()).thenReturn(FirewallRule.State.Revoke);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(ApiDBUtils.findIpAddressById(anyLong())).thenReturn(ipAddressVO);
        when(ApiDBUtils.findUserVmById(anyLong())).thenReturn(userVm);
        when(networkModel.getNicInNetwork(anyLong(), anyLong())).thenReturn(nic);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenPortForwardingCommand.class), anyLong())).thenReturn(applyTungstenPortForwardingAnswer);

        assertTrue(tungstenElement.applyPFRules(network, portForwardingRuleList));
    }

    @Test
    public void applyPFRulesRevokeRuleFailTest() {
        Network network = mock(Network.class);
        Network publicNetwork = mock(Network.class);
        PortForwardingRule portForwardingRule = mock(PortForwardingRule.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        UserVm userVm = mock(UserVm.class);
        Nic nic = mock(Nic.class);
        TungstenAnswer applyTungstenPortForwardingAnswer = MockTungstenAnswerFactory.get(false);
        List<PortForwardingRule> portForwardingRuleList = List.of(portForwardingRule);

        when(portForwardingRule.getState()).thenReturn(FirewallRule.State.Revoke);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), any())).thenReturn(publicNetwork);
        when(ApiDBUtils.findIpAddressById(anyLong())).thenReturn(ipAddressVO);
        when(ApiDBUtils.findUserVmById(anyLong())).thenReturn(userVm);
        when(networkModel.getNicInNetwork(anyLong(), anyLong())).thenReturn(nic);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenPortForwardingCommand.class), anyLong())).thenReturn(applyTungstenPortForwardingAnswer);

        assertFalse(tungstenElement.applyPFRules(network, portForwardingRuleList));
    }

    @Test
    public void preparePublicNetworkTest() throws ConcurrentOperationException {
        Network network = mock(Network.class);
        NicProfile nicProfile = new NicProfile();
        VirtualMachineProfile virtualMachineProfile = mock(VirtualMachineProfile.class);
        DeployDestination deployDestination = mock(DeployDestination.class);
        ReservationContext reservationContext = mock(ReservationContext.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        HostVO host = mock(HostVO.class);
        TungstenAnswer createTungstenVMAnswer = MockTungstenAnswerFactory.get(true);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        TungstenAnswer createTungstenNetworkPolicyAnswer = MockTungstenAnswerFactory.get(true);

        nicProfile.setIPv4Address("192.168.100.100");
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Public);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vmInstanceVO);
        when(hostDao.findById(anyLong())).thenReturn(host);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenVirtualMachineCommand.class), anyLong())).thenReturn(createTungstenVMAnswer);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddressVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(createTungstenNetworkPolicyAnswer);
        when(virtualMachineProfile.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);

        assertTrue(tungstenElement.prepare(network, nicProfile, virtualMachineProfile, deployDestination, reservationContext));
        assertEquals(Nic.ReservationStrategy.Create, nicProfile.getReservationStrategy());
        assertEquals(Networks.BroadcastDomainType.TUNGSTEN, nicProfile.getBroadcastType());
        assertEquals(Networks.BroadcastDomainType.TUNGSTEN.toUri("tf"), nicProfile.getBroadCastUri());
        assertEquals(TungstenUtils.DEFAULT_VHOST_INTERFACE, nicProfile.getName());
    }

    @Test
    public void prepareManagementNetworkTest() throws ConcurrentOperationException {
        Network network = mock(Network.class);
        NicProfile nicProfile = new NicProfile();
        VirtualMachineProfile virtualMachineProfile = mock(VirtualMachineProfile.class);
        DeployDestination deployDestination = mock(DeployDestination.class);
        ReservationContext reservationContext = mock(ReservationContext.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        HostVO host = mock(HostVO.class);
        TungstenAnswer createTungstenVMAnswer = MockTungstenAnswerFactory.get(true);

        nicProfile.setIPv4Address("192.168.100.100");
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Management);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vmInstanceVO);
        when(hostDao.findById(anyLong())).thenReturn(host);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenVirtualMachineCommand.class), anyLong())).thenReturn(createTungstenVMAnswer);
        when(virtualMachineProfile.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);

        assertTrue(tungstenElement.prepare(network, nicProfile, virtualMachineProfile, deployDestination, reservationContext));
        assertEquals(Nic.ReservationStrategy.Create, nicProfile.getReservationStrategy());
        assertEquals(Networks.BroadcastDomainType.TUNGSTEN, nicProfile.getBroadcastType());
        assertEquals(Networks.BroadcastDomainType.TUNGSTEN.toUri("tf"), nicProfile.getBroadCastUri());
        assertEquals(TungstenUtils.DEFAULT_VHOST_INTERFACE, nicProfile.getName());
    }

    @Test(expected = CloudRuntimeException.class)
    public void prepareWithExceptionTest() throws ConcurrentOperationException {
        Network network = mock(Network.class);
        NicProfile nicProfile = new NicProfile();
        VirtualMachineProfile virtualMachineProfile = mock(VirtualMachineProfile.class);
        DeployDestination deployDestination = mock(DeployDestination.class);
        ReservationContext reservationContext = mock(ReservationContext.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        HostVO host = mock(HostVO.class);
        TungstenAnswer createTungstenVMAnswer = mock(TungstenAnswer.class);

        nicProfile.setIPv4Address("192.168.100.100");
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Management);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vmInstanceVO);
        when(hostDao.findById(anyLong())).thenReturn(host);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenVirtualMachineCommand.class), anyLong())).thenReturn(createTungstenVMAnswer);
        when(virtualMachineProfile.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);

        tungstenElement.prepare(network, nicProfile, virtualMachineProfile, deployDestination, reservationContext);
    }

    @Test
    public void releasePublicNetworkTest() throws ConcurrentOperationException, ResourceUnavailableException {
        Network network = mock(Network.class);
        NicProfile nicProfile = mock(NicProfile.class);
        VirtualMachineProfile virtualMachineProfile = mock(VirtualMachineProfile.class);
        ReservationContext reservationContext = mock(ReservationContext.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        HostVO host = mock(HostVO.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        TungstenAnswer deleteTungstenVRouterPortAnswer = mock(TungstenAnswer.class);
        TungstenAnswer deleteVmiAnswer = mock(TungstenAnswer.class);
        TungstenAnswer deleteVmAnswer = mock(TungstenAnswer.class);
        TungstenAnswer deleteTungstenNetworkPolicyAnswer = mock(TungstenAnswer.class);

        when(nicProfile.getIPv4Address()).thenReturn("192.168.100.100");
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Public);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vmInstanceVO);
        when(hostDao.findById(anyLong())).thenReturn(host);
        when(virtualMachineProfile.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddressVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVRouterPortCommand.class), anyLong())).thenReturn(deleteTungstenVRouterPortAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVmInterfaceCommand.class), anyLong())).thenReturn(deleteVmiAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVmCommand.class), anyLong())).thenReturn(deleteVmAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(deleteTungstenNetworkPolicyAnswer);

        assertTrue(tungstenElement.release(network, nicProfile, virtualMachineProfile, reservationContext));
    }

    @Test
    public void releaseManagementNetworkTest() throws ConcurrentOperationException, ResourceUnavailableException {
        Network network = mock(Network.class);
        NicProfile nicProfile = mock(NicProfile.class);
        VirtualMachineProfile virtualMachineProfile = mock(VirtualMachineProfile.class);
        ReservationContext reservationContext = mock(ReservationContext.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        HostVO host = mock(HostVO.class);
        TungstenAnswer deleteTungstenVRouterPortAnswer = mock(TungstenAnswer.class);
        TungstenAnswer deleteVmiAnswer = mock(TungstenAnswer.class);
        TungstenAnswer deleteVmAnswer = mock(TungstenAnswer.class);

        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Management);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vmInstanceVO);
        when(hostDao.findById(anyLong())).thenReturn(host);
        when(virtualMachineProfile.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVRouterPortCommand.class), anyLong())).thenReturn(deleteTungstenVRouterPortAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVmInterfaceCommand.class), anyLong())).thenReturn(deleteVmiAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVmCommand.class), anyLong())).thenReturn(deleteVmAnswer);

        assertTrue(tungstenElement.release(network, nicProfile, virtualMachineProfile, reservationContext));
    }

    @Test(expected = CloudRuntimeException.class)
    public void releaseWithExceptionTest() throws ConcurrentOperationException, ResourceUnavailableException {
        Network network = mock(Network.class);
        NicProfile nicProfile = mock(NicProfile.class);
        VirtualMachineProfile virtualMachineProfile = mock(VirtualMachineProfile.class);
        ReservationContext reservationContext = mock(ReservationContext.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        HostVO host = mock(HostVO.class);
        TungstenAnswer deleteTungstenVRouterPortAnswer = mock(TungstenAnswer.class);
        TungstenAnswer deleteVmiAnswer = mock(TungstenAnswer.class);

        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Management);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vmInstanceVO);
        when(hostDao.findById(anyLong())).thenReturn(host);
        when(virtualMachineProfile.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVRouterPortCommand.class), anyLong())).thenReturn(deleteTungstenVRouterPortAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVmInterfaceCommand.class), anyLong())).thenReturn(deleteVmiAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenVmCommand.class), anyLong())).thenThrow(IllegalArgumentException.class);

        tungstenElement.release(network, nicProfile, virtualMachineProfile, reservationContext);
    }

    @Test
    public void destroyTest() throws ConcurrentOperationException, ResourceUnavailableException {
        IPAddressVO ipAddressVO1 = mock(IPAddressVO.class);
        IPAddressVO ipAddressVO2 = mock(IPAddressVO.class);
        Network network = mock(Network.class);
        ReservationContext reservationContext = mock(ReservationContext.class);
        List<IPAddressVO> ipAddressVOList = Arrays.asList(ipAddressVO1, ipAddressVO2);
        TungstenAnswer tungstenDeleteFIPAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer tungstenDeleteNPAnswer = MockTungstenAnswerFactory.get(true);

        when(ipAddressDao.listByAssociatedNetwork(anyLong(), anyBoolean())).thenReturn(ipAddressVOList);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenFloatingIpCommand.class), anyLong())).thenReturn(tungstenDeleteFIPAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(tungstenDeleteNPAnswer);

        assertTrue(tungstenElement.destroy(network, reservationContext));
    }

    @Test
    public void isReadyTest() {
        PhysicalNetworkServiceProvider physicalNetworkServiceProvider = mock(PhysicalNetworkServiceProvider.class);
        PhysicalNetworkTrafficTypeVO physicalNetworkTrafficTypeVO = mock(PhysicalNetworkTrafficTypeVO.class);

        when(physicalNetworkTrafficTypeDao.findBy(anyLong(), eq(Networks.TrafficType.Management))).thenReturn(physicalNetworkTrafficTypeVO);

        assertTrue(tungstenElement.isReady(physicalNetworkServiceProvider));
    }

    @Test
    public void shutdownProviderInstancesTest() throws ConcurrentOperationException {
        PhysicalNetworkServiceProvider physicalNetworkServiceProvider = mock(PhysicalNetworkServiceProvider.class);
        ReservationContext reservationContext = mock(ReservationContext.class);
        PhysicalNetworkVO physicalNetworkVO = mock(PhysicalNetworkVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);
        Network publicNetwork = mock(Network.class);
        VlanVO vlanVO1 = mock(VlanVO.class);
        VlanVO vlanVO2 = mock(VlanVO.class);
        List<VlanVO> vlanVOList = Arrays.asList(vlanVO1, vlanVO2);
        HostPodVO hostPodVO1 = mock(HostPodVO.class);
        HostPodVO hostPodVO2 = mock(HostPodVO.class);
        List<HostPodVO> hostPodVOList = Arrays.asList(hostPodVO1, hostPodVO2);

        when(physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetworkVO);
        when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProviderVO);
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(vlanDao.listVlansByNetworkIdIncludingRemoved(anyLong())).thenReturn(vlanVOList);
        when(hostPodDao.listByDataCenterId(anyLong())).thenReturn(hostPodVOList);

        assertTrue(tungstenElement.shutdownProviderInstances(physicalNetworkServiceProvider, reservationContext));
        verify(networkServiceMapDao, times(1)).deleteByNetworkId(anyLong());
        verify(tungstenService, times(2)).removePublicNetworkSubnet(any(VlanVO.class));
        verify(tungstenService, times(1)).deletePublicNetwork(anyLong());
        verify(tungstenService, times(2)).removeManagementNetworkSubnet(any(HostPodVO.class));
        verify(tungstenService, times(1)).deleteManagementNetwork(anyLong());
    }

    //@Test
    //public void processConnectWithoutSecurityGroupTest() throws ConnectionException {
    //    Host host = mock(Host.class);
    //    StartupCommand startupCommand = mock(StartupCommand.class);
    //    TungstenProviderVO tungstenProvider = mock(TungstenProviderVO.class);
    //    DataCenterVO dataCenterVO = mock(DataCenterVO.class);
    //    VlanVO vlanVO1 = mock(VlanVO.class);
    //    VlanVO vlanVO2 = mock(VlanVO.class);
    //    List<VlanVO> vlanList = Arrays.asList(vlanVO1, vlanVO2);
    //    Network publicNetwork = mock(Network.class);
    //    NetworkDetailVO networkDetail = mock(NetworkDetailVO.class);
//
    //    when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
    //    when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProvider);
    //    when(host.getPublicIpAddress()).thenReturn("192.168.100.100");
    //    when(tungstenProvider.getGateway()).thenReturn("192.168.100.100");
    //    when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
    //    when(vlanDao.listByZone(anyLong())).thenReturn(vlanList);
    //    when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
    //    when(networkDetailsDao.findDetail(anyLong(), anyString())).thenReturn(networkDetail);
    //    when(vlanVO1.getVlanGateway()).thenReturn("192.168.100.1");
    //    when(vlanVO1.getVlanNetmask()).thenReturn("255.255.255.0");
    //    when(vlanVO2.getVlanGateway()).thenReturn("192.168.101.1");
    //    when(vlanVO2.getVlanNetmask()).thenReturn("255.255.255.0");
    //    when(dataCenterVO.isSecurityGroupEnabled()).thenReturn(false);
//
    //    tungstenElement.processConnect(host, startupCommand, true);
    //    verify(agentManager, times(1)).easySend(anyLong(), any(SetupTungstenVRouterCommand.class));
    //}

    //@Test
    //public void processConnectWithSecurityGroupTest() throws ConnectionException {
    //    Host host = mock(Host.class);
    //    StartupCommand startupCommand = mock(StartupCommand.class);
    //    TungstenProviderVO tungstenProvider = mock(TungstenProviderVO.class);
    //    DataCenterVO dataCenterVO = mock(DataCenterVO.class);
    //    NetworkVO network = mock(NetworkVO.class);
    //    NetworkDetailVO networkDetail = mock(NetworkDetailVO.class);
    //    Network publicNetwork = mock(Network.class);
//
    //    when(host.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
    //    when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProvider);
    //    when(host.getPublicIpAddress()).thenReturn("192.168.100.100");
    //    when(tungstenProvider.getGateway()).thenReturn("192.168.100.100");
    //    when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
    //    when(networkDao.listByZoneSecurityGroup(anyLong())).thenReturn(Arrays.asList(network));
    //    when(networkDetailsDao.findDetail(anyLong(), anyString())).thenReturn(networkDetail);
    //    when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
    //    when(dataCenterVO.isSecurityGroupEnabled()).thenReturn(true);
//
    //    tungstenElement.processConnect(host, startupCommand, true);
    //    verify(agentManager, times(1)).easySend(anyLong(), any(SetupTungstenVRouterCommand.class));
    //}

    @Test
    public void processHostAboutToBeRemovedWithSecurityGroupTest() {
        HostVO hostVO = mock(HostVO.class);
        TungstenProviderVO tungstenProvider = mock(TungstenProviderVO.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);
        NetworkVO network = mock(NetworkVO.class);

        when(hostVO.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProvider);
        when(hostVO.getPublicIpAddress()).thenReturn("192.168.100.100");
        when(tungstenProvider.getGateway()).thenReturn("192.168.100.100");
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(hostDao.findById(anyLong())).thenReturn(hostVO);
        when(dataCenterVO.isSecurityGroupEnabled()).thenReturn(true);
        when(networkDao.listByZoneSecurityGroup(anyLong())).thenReturn(List.of(network));

        tungstenElement.processHostAboutToBeRemoved(1L);
        verify(agentManager, times(1)).easySend(anyLong(), any(SetupTungstenVRouterCommand.class));
    }

    @Test
    public void processHostAboutToBeRemovedWithoutSecurityGroupTest() {
        HostVO hostVO = mock(HostVO.class);
        TungstenProviderVO tungstenProvider = mock(TungstenProviderVO.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(hostVO.getHypervisorType()).thenReturn(Hypervisor.HypervisorType.KVM);
        when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProvider);
        when(hostVO.getPublicIpAddress()).thenReturn("192.168.100.100");
        when(tungstenProvider.getGateway()).thenReturn("192.168.100.100");
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(hostDao.findById(anyLong())).thenReturn(hostVO);
        when(dataCenterVO.isSecurityGroupEnabled()).thenReturn(false);

        tungstenElement.processHostAboutToBeRemoved(1L);
        verify(agentManager, times(1)).easySend(anyLong(), any(SetupTungstenVRouterCommand.class));
    }

    @Test
    public void applyFWRulesWithAddEgressRuleTest() throws ResourceUnavailableException {
        Network network = mock(Network.class);
        FirewallRuleVO firewallRuleVO = mock(FirewallRuleVO.class);
        Network publicNetwork = mock(Network.class);
        TungstenAnswer createNetworkPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer applyNetworkPolicyAnswer = MockTungstenAnswerFactory.get(true);

        when(firewallRuleVO.getState()).thenReturn(FirewallRule.State.Add);
        when(firewallRuleVO.getSourceCidrList()).thenReturn(List.of("192.168.100.0/24"));
        when(firewallRuleVO.getProtocol()).thenReturn(NetUtils.ALL_PROTO);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(firewallRuleVO.getPurpose()).thenReturn(FirewallRule.Purpose.Firewall);
        when(firewallRuleVO.getTrafficType()).thenReturn(FirewallRule.TrafficType.Egress);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(createNetworkPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(applyNetworkPolicyAnswer);

        assertTrue(tungstenElement.applyFWRules(network, List.of(firewallRuleVO)));
    }

    @Test
    public void applyFWRulesWithAddIngressRuleTest() throws ResourceUnavailableException {
        Network network = mock(Network.class);
        FirewallRuleVO firewallRuleVO = mock(FirewallRuleVO.class);
        Network publicNetwork = mock(Network.class);
        TungstenAnswer createNetworkPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer applyNetworkPolicyAnswer = MockTungstenAnswerFactory.get(true);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        Ip ip = mock(Ip.class);

        when(firewallRuleVO.getState()).thenReturn(FirewallRule.State.Add);
        when(firewallRuleVO.getSourceCidrList()).thenReturn(List.of("192.168.100.0/24"));
        when(firewallRuleVO.getProtocol()).thenReturn(NetUtils.ALL_PROTO);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(ipAddressDao.findById(anyLong())).thenReturn(ipAddressVO);
        when(ipAddressVO.getAddress()).thenReturn(ip);
        when(firewallRuleVO.getPurpose()).thenReturn(FirewallRule.Purpose.Firewall);
        when(firewallRuleVO.getTrafficType()).thenReturn(FirewallRule.TrafficType.Ingress);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(createNetworkPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(applyNetworkPolicyAnswer);

        assertTrue(tungstenElement.applyFWRules(network, List.of(firewallRuleVO)));
    }

    @Test
    public void applyFWRulesWithRevokeRuleTest() throws ResourceUnavailableException {
        Network network = mock(Network.class);
        FirewallRuleVO firewallRuleVO = mock(FirewallRuleVO.class);
        Network publicNetwork = mock(Network.class);
        TungstenAnswer deleteNetworkPolicyAnswer = mock(TungstenAnswer.class);

        when(firewallRuleVO.getState()).thenReturn(FirewallRule.State.Revoke);
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(firewallRuleVO.getPurpose()).thenReturn(FirewallRule.Purpose.Firewall);
        when(firewallRuleVO.getTrafficType()).thenReturn(FirewallRule.TrafficType.Ingress);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(deleteNetworkPolicyAnswer);

        assertTrue(tungstenElement.applyFWRules(network, List.of(firewallRuleVO)));
    }

    @Test
    public void postStateTransitionEventWithVmStartedTest() {
        StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition = mock(StateMachine2.Transition.class);
        VMInstanceVO vo = mock(VMInstanceVO.class);
        Object opaque = mock(Object.class);

        when(transition.getCurrentState()).thenReturn(VirtualMachine.State.Starting);
        when(transition.getToState()).thenReturn(VirtualMachine.State.Running);
        when(tungstenService.addTungstenVmSecurityGroup(any(VMInstanceVO.class))).thenReturn(true);

        assertTrue(tungstenElement.postStateTransitionEvent(transition, vo, true, opaque));
    }

    @Test
    public void postStateTransitionEventWithVmStopedTest() {
        StateMachine2.Transition<VirtualMachine.State, VirtualMachine.Event> transition = mock(StateMachine2.Transition.class);
        VMInstanceVO vo = mock(VMInstanceVO.class);
        Object opaque = mock(Object.class);

        when(transition.getCurrentState()).thenReturn(VirtualMachine.State.Stopping);
        when(transition.getToState()).thenReturn(VirtualMachine.State.Stopped);
        when(tungstenService.removeTungstenVmSecurityGroup(any(VMInstanceVO.class))).thenReturn(true);

        assertTrue(tungstenElement.postStateTransitionEvent(transition, vo, true, opaque));
    }

    @Test
    public void prepareMigrationTest() {
        NicProfile nic = mock(NicProfile.class);
        Network network = mock(Network.class);
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        DeployDestination dest = mock(DeployDestination.class);
        ReservationContext context = mock(ReservationContext.class);
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        HostVO hostVO = mock(HostVO.class);
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);

        when(vm.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);
        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Public);
        when(vmInstanceDao.findById(anyLong())).thenReturn(vmInstanceVO);
        when(hostDao.findById(anyLong())).thenReturn(hostVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenVirtualMachineCommand.class), anyLong())).thenReturn(tungstenAnswer);

        assertTrue(tungstenElement.prepareMigration(nic, network, vm, dest, context));
    }

    @Test
    public void rollbackMigration() {
        NicProfile nic = mock(NicProfile.class);
        Network network = mock(Network.class);
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        ReservationContext src = mock(ReservationContext.class);
        ReservationContext dest = mock(ReservationContext.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        HostVO hostVO = mock(HostVO.class);

        when(vm.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);
        when(vm.getVirtualMachine()).thenReturn(virtualMachine);
        when(hostDao.findById(anyLong())).thenReturn(hostVO);

        tungstenElement.rollbackMigration(nic, network, vm, src, dest);
        verify(tungstenFabricUtils, times(1)).sendTungstenCommand(any(DeleteTungstenVRouterPortCommand.class), anyLong());
    }

    @Test
    public void commitMigration() {
        NicProfile nic = mock(NicProfile.class);
        Network network = mock(Network.class);
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        ReservationContext src = mock(ReservationContext.class);
        ReservationContext dest = mock(ReservationContext.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        HostVO hostVO = mock(HostVO.class);

        when(vm.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);
        when(vm.getVirtualMachine()).thenReturn(virtualMachine);
        when(hostDao.findById(anyLong())).thenReturn(hostVO);

        tungstenElement.commitMigration(nic, network, vm, src, dest);
        verify(tungstenFabricUtils, times(1)).sendTungstenCommand(any(DeleteTungstenVRouterPortCommand.class), anyLong());
    }
}
