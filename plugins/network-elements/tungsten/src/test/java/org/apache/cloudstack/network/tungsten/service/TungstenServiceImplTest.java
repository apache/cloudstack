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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks;
import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerCertMapDao;
import com.cloud.network.dao.LoadBalancerCertMapVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.SslCertVO;
import com.cloud.network.dao.TungstenGuestNetworkIpAddressDao;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupRuleVO;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.network.security.SecurityRule;
import com.cloud.network.security.TungstenSecurityGroupRuleVO;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.security.dao.SecurityGroupRuleDao;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.network.security.dao.TungstenSecurityGroupRuleDao;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.ActionListType;
import net.juniper.tungsten.api.types.AddressGroup;
import net.juniper.tungsten.api.types.AddressType;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.FirewallPolicy;
import net.juniper.tungsten.api.types.FirewallRuleEndpointType;
import net.juniper.tungsten.api.types.FirewallRuleMatchTagsType;
import net.juniper.tungsten.api.types.FirewallSequence;
import net.juniper.tungsten.api.types.FirewallServiceGroupType;
import net.juniper.tungsten.api.types.FirewallServiceType;
import net.juniper.tungsten.api.types.Loadbalancer;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.PolicyEntriesType;
import net.juniper.tungsten.api.types.PolicyRuleType;
import net.juniper.tungsten.api.types.PortType;
import net.juniper.tungsten.api.types.ServiceGroup;
import net.juniper.tungsten.api.types.SubnetListType;
import net.juniper.tungsten.api.types.SubnetType;
import net.juniper.tungsten.api.types.Tag;
import net.juniper.tungsten.api.types.TagType;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageSubscriber;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkGatewayToLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecondaryIpAddressCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenVmToSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDefaultProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNetworkDnsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNicCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkGatewayFromLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecondaryIpAddressCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetupTungstenVRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateLoadBalancerServiceInstanceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenDefaultSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerSslCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadbalancerStatsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenVrouterConfigCommand;
import org.apache.cloudstack.network.tungsten.model.TungstenLogicalRouter;
import org.apache.cloudstack.network.tungsten.model.TungstenNetworkPolicy;
import org.apache.cloudstack.network.tungsten.model.TungstenRule;
import org.apache.cloudstack.network.tungsten.model.TungstenTag;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TungstenServiceImplTest {
    @Mock
    MessageBus messageBus;
    @Mock
    ProjectDao projectDao;
    @Mock
    AccountDao accountDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    ConfigurationDao configDao;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    EntityManager entityMgr;
    @Mock
    NetworkModel networkModel;
    @Mock
    DomainDao domainDao;
    @Mock
    LoadBalancerCertMapDao lbCertMapDao;
    @Mock
    FirewallRulesDao fwRulesDao;
    @Mock
    TungstenGuestNetworkIpAddressDao tungstenGuestNetworkIpAddressDao;
    @Mock
    TungstenProviderDao tungstenProviderDao;
    @Mock
    TungstenFabricUtils tungstenFabricUtils;
    @Mock
    AgentManager agentMgr;
    @Mock
    HostDao hostDao;
    @Mock
    NetworkDetailsDao networkDetailsDao;
    @Mock
    SecurityGroupDao securityGroupDao;
    @Mock
    NicDao nicDao;
    @Mock
    TungstenSecurityGroupRuleDao tungstenSecurityGroupRuleDao;
    @Mock
    SecurityGroupVMMapDao securityGroupVMMapDao;
    @Mock
    SecurityGroupRuleDao securityGroupRuleDao;
    @Mock
    SecurityGroupManager securityGroupManager;
    @Mock
    NicSecondaryIpDao nicSecIpDao;
    @Mock
    DataCenterIpAddressDao dataCenterIpAddressDao;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    IpAddressManager ipAddressManager;

    TungstenServiceImpl tungstenService;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        tungstenService = new TungstenServiceImpl();
        tungstenService.projectDao = projectDao;
        tungstenService.tungstenProviderDao = tungstenProviderDao;
        tungstenService.networkModel = networkModel;
        tungstenService.ipAddressDao = ipAddressDao;
        tungstenService.tungstenFabricUtils = tungstenFabricUtils;
        tungstenService.domainDao = domainDao;
        tungstenService.accountDao = accountDao;
        tungstenService.dataCenterIpAddressDao = dataCenterIpAddressDao;
        tungstenService.networkDetailsDao = networkDetailsDao;
        tungstenService.agentMgr = agentMgr;
        tungstenService.hostDao = hostDao;
        tungstenService.configDao = configDao;
        tungstenService.fwRulesDao = fwRulesDao;
        tungstenService.lbCertMapDao = lbCertMapDao;
        tungstenService.entityMgr = entityMgr;
        tungstenService.tungstenGuestNetworkIpAddressDao = tungstenGuestNetworkIpAddressDao;
        tungstenService.securityGroupDao = securityGroupDao;
        tungstenService.tungstenSecurityGroupRuleDao = tungstenSecurityGroupRuleDao;
        tungstenService.securityGroupVMMapDao = securityGroupVMMapDao;
        tungstenService.nicDao = nicDao;
        tungstenService.nicSecIpDao = nicSecIpDao;
        tungstenService.securityGroupRuleDao = securityGroupRuleDao;
        tungstenService.securityGroupManager = securityGroupManager;
        tungstenService.networkDao = networkDao;
        tungstenService.dataCenterDao = dataCenterDao;
        tungstenService.ipAddressManager = ipAddressManager;
        tungstenService.messageBus = messageBus;
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void createTungstenFloatingIpTest() throws Exception {
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        NetworkVO networkVO = mock(NetworkVO.class);
        TungstenAnswer createTungstenFloatingIpAnswer = MockTungstenAnswerFactory.get(true);
        Ip ip = mock(Ip.class);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(networkVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenFloatingIpCommand.class), anyLong())).thenReturn(createTungstenFloatingIpAnswer);
        when(ipAddressVO.getAddress()).thenReturn(ip);

        assertTrue(ReflectionTestUtils.invokeMethod(tungstenService, "createTungstenFloatingIp", 1L, ipAddressVO));
    }

    @Test
    public void deleteTungstenFloatingIpWithIpAddressTest() throws Exception {
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        NetworkVO networkVO = mock(NetworkVO.class);
        TungstenAnswer deleteTungstenFloatingIpAnswer = MockTungstenAnswerFactory.get(true);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(networkVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenFloatingIpCommand.class), anyLong())).thenReturn(deleteTungstenFloatingIpAnswer);

        assertTrue(ReflectionTestUtils.invokeMethod(tungstenService, "deleteTungstenFloatingIp", 1L, ipAddressVO));
    }

    @Test
    public void deleteTungstenDomainTest() throws Exception {
        DomainVO domainVO = mock(DomainVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer deleteTungstenDomainAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenDomainCommand.class), anyLong())).thenReturn(deleteTungstenDomainAnswer);

        assertTrue(ReflectionTestUtils.invokeMethod(tungstenService, "deleteTungstenDomain", domainVO));
    }

    @Test
    public void deleteTungstenProjectTest() throws Exception {
        ProjectVO projectVO = mock(ProjectVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer deleteTungstenProjectAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenProjectCommand.class), anyLong())).thenReturn(deleteTungstenProjectAnswer);

        assertTrue(ReflectionTestUtils.invokeMethod(tungstenService, "deleteTungstenProject", projectVO));
    }

    @Test
    public void addTungstenDefaultNetworkPolicyTest() {
        TungstenRule tungstenRule = mock(TungstenRule.class);
        TungstenAnswer createTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer applyTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);

        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(createTungstenPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(applyTungstenPolicyAnswer);
        when(createTungstenPolicyAnswer.getApiObjectBase()).thenReturn(networkPolicy);

        assertTrue(tungstenService.addTungstenDefaultNetworkPolicy(1L, "default-domain:default-project", "policyName", "7279ed91-314e-45be-81b4-b10395fd2ae3"
                , List.of(tungstenRule), 1, 1));
    }

    @Test
    public void createManagementNetworkTest() {
        Network managementNetwork = mock(Network.class);
        VirtualNetwork managementVirtualNetwork = mock(VirtualNetwork.class);
        VirtualNetwork fabricVirtualNetwork = mock(VirtualNetwork.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        TungstenAnswer createTungstenNetworkAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenDefaultSecurityGroupAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenGlobalVrouterConfigAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer getTungstenFabricNetworkAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer createTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer applyTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(),
                eq(Networks.TrafficType.Management))).thenReturn(managementNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkCommand.class), anyLong())).thenReturn(createTungstenNetworkAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenDefaultSecurityGroupCommand.class), anyLong())).thenReturn(updateTungstenDefaultSecurityGroupAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenVrouterConfigCommand.class), anyLong())).thenReturn(updateTungstenGlobalVrouterConfigAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenFabricNetworkCommand.class), anyLong())).thenReturn(getTungstenFabricNetworkAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(createTungstenPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(applyTungstenPolicyAnswer);

        when(createTungstenNetworkAnswer.getApiObjectBase()).thenReturn(managementVirtualNetwork);
        when(createTungstenPolicyAnswer.getApiObjectBase()).thenReturn(networkPolicy);
        when(getTungstenFabricNetworkAnswer.getApiObjectBase()).thenReturn(fabricVirtualNetwork);

        assertTrue(tungstenService.createManagementNetwork(1L));
    }

    @Test
    public void addManagementNetworkSubnetTest() {
        HostPodVO hostPodVO = mock(HostPodVO.class);
        Network managementNetwork = mock(Network.class);
        TungstenAnswer addTungstenNetworkSubnetAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer getTungstenNetworkDnsAnswer = MockTungstenAnswerFactory.get(true);
        DataCenterIpAddressVO dataCenterIpAddressVO = mock(DataCenterIpAddressVO.class);

        when(hostPodVO.getDescription()).thenReturn("192.168.100.100-192.168.100.200");
        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(),
                eq(Networks.TrafficType.Management))).thenReturn(managementNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenNetworkSubnetCommand.class), anyLong())).thenReturn(addTungstenNetworkSubnetAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenNetworkDnsCommand.class), anyLong())).thenReturn(getTungstenNetworkDnsAnswer);
        when(getTungstenNetworkDnsAnswer.getDetails()).thenReturn("192.168.100.150");
        when(managementNetwork.getCidr()).thenReturn("192.168.100.0/24");
        when(managementNetwork.getTrafficType()).thenReturn(Networks.TrafficType.Management);
        when(dataCenterIpAddressDao.listByPodIdDcIdIpAddress(anyLong(), anyLong(), anyString())).thenReturn(List.of(dataCenterIpAddressVO));
        when(dataCenterIpAddressDao.mark(anyLong(), anyLong(), anyString())).thenReturn(true);

        assertTrue(tungstenService.addManagementNetworkSubnet(hostPodVO));
    }

    @Test
    public void deleteManagementNetworkTest() {
        Network managementNetwork = mock(Network.class);
        TungstenAnswer deleteTungstenManagementPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer getTungstenFabricNetworkAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer deleteTungstenFabricPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer deleteTungstenNetworkAnswer = MockTungstenAnswerFactory.get(true);
        VirtualNetwork fabricVirtualNetwork = mock(VirtualNetwork.class);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(),
                eq(Networks.TrafficType.Management))).thenReturn(managementNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenFabricNetworkCommand.class), anyLong())).thenReturn(getTungstenFabricNetworkAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(deleteTungstenFabricPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenNetworkCommand.class), anyLong())).thenReturn(deleteTungstenNetworkAnswer);
        when(getTungstenFabricNetworkAnswer.getApiObjectBase()).thenReturn(fabricVirtualNetwork);

        assertTrue(tungstenService.deleteManagementNetwork(1L));
    }

    @Test
    public void removeManagementNetworkSubnetTest() {
        HostPodVO hostPodVO = mock(HostPodVO.class);
        Network managementNetwork = mock(Network.class);
        TungstenAnswer removeTungstenNetworkSubnetAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer getTungstenNetworkDnsAnswer = MockTungstenAnswerFactory.get(true);
        DataCenterIpAddressVO dataCenterIpAddressVO = mock(DataCenterIpAddressVO.class);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(),
                eq(Networks.TrafficType.Management))).thenReturn(managementNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenNetworkSubnetCommand.class), anyLong())).thenReturn(removeTungstenNetworkSubnetAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenNetworkDnsCommand.class), anyLong())).thenReturn(getTungstenNetworkDnsAnswer);
        when(getTungstenNetworkDnsAnswer.getDetails()).thenReturn("192.168.100.150");
        when(managementNetwork.getTrafficType()).thenReturn(Networks.TrafficType.Management);
        when(dataCenterIpAddressDao.listByPodIdDcIdIpAddress(anyLong(), anyLong(), anyString())).thenReturn(List.of(dataCenterIpAddressVO));

        assertTrue(tungstenService.removeManagementNetworkSubnet(hostPodVO));
        verify(dataCenterIpAddressDao, times(1)).releasePodIpAddress(anyLong());
    }

    @Test
    public void createPublicNetworkTest() {
        Network publicNetwork = mock(Network.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        NetworkDetailVO networkDetailVO = mock(NetworkDetailVO.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        TungstenAnswer createPublicNetworkAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer createFloatingIpPoolAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer createTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer applyTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkCommand.class), anyLong())).thenReturn(createPublicNetworkAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenFloatingIpPoolCommand.class), anyLong())).thenReturn(createFloatingIpPoolAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(createTungstenPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(applyTungstenPolicyAnswer);
        when(createPublicNetworkAnswer.getApiObjectBase()).thenReturn(virtualNetwork);
        when(createTungstenPolicyAnswer.getApiObjectBase()).thenReturn(networkPolicy);
        when(networkDetailsDao.persist(any(NetworkDetailVO.class))).thenReturn(networkDetailVO);
        when(virtualNetwork.getQualifiedName()).thenReturn(Arrays.asList("default-domain", "default-project", "publicNetwork"));

        assertTrue(tungstenService.createPublicNetwork(1L));
    }

    @Test
    public void addPublicNetworkSubnetTest() {
        VlanVO vlanVO = mock(VlanVO.class);
        Network publicNetwork = mock(Network.class);
        TungstenAnswer addTungstenNetworkSubnetAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer createTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer applyTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        TungstenAnswer getTungstenNetworkDnsAnswer = MockTungstenAnswerFactory.get(true);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenNetworkSubnetCommand.class), anyLong())).thenReturn(addTungstenNetworkSubnetAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(createTungstenPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(applyTungstenPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenNetworkDnsCommand.class), anyLong())).thenReturn(getTungstenNetworkDnsAnswer);
        when(getTungstenNetworkDnsAnswer.getDetails()).thenReturn("192.168.100.150");
        when(vlanVO.getIpRange()).thenReturn("192.168.100.100-192.168.100.200");
        when(vlanVO.getVlanGateway()).thenReturn("192.168.100.1");
        when(vlanVO.getVlanNetmask()).thenReturn("255.255.255.0");
        when(publicNetwork.getCidr()).thenReturn("192.168.100.0/24");
        when(publicNetwork.getTrafficType()).thenReturn(Networks.TrafficType.Public);
        when(createTungstenPolicyAnswer.getApiObjectBase()).thenReturn(networkPolicy);
        when(ipAddressDao.mark(anyLong(), any(Ip.class))).thenReturn(true);

        assertTrue(tungstenService.addPublicNetworkSubnet(vlanVO));
    }

    @Test
    public void deletePublicNetworkTest() {
        Network publicNetwork = mock(Network.class);
        TungstenAnswer deleteTungstenNetworkPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer deleteTungstenFloatingIpPoolAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer deleteTungstenNetworkAnswer = MockTungstenAnswerFactory.get(true);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(deleteTungstenNetworkPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenFloatingIpPoolCommand.class), anyLong())).thenReturn(deleteTungstenFloatingIpPoolAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenNetworkCommand.class), anyLong())).thenReturn(deleteTungstenNetworkAnswer);

        assertTrue(tungstenService.deletePublicNetwork(1L));
    }

    @Test
    public void removePublicNetworkSubnetTest() {
        VlanVO vlanVO = mock(VlanVO.class);
        Network publicNetwork = mock(Network.class);
        TungstenAnswer deleteTungstenNetworkPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer removeTungstenNetworkSubnetAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer getTungstenNetworkDnsAnswer = MockTungstenAnswerFactory.get(true);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(deleteTungstenNetworkPolicyAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenNetworkSubnetCommand.class), anyLong())).thenReturn(removeTungstenNetworkSubnetAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenNetworkDnsCommand.class), anyLong())).thenReturn(getTungstenNetworkDnsAnswer);
        when(getTungstenNetworkDnsAnswer.getDetails()).thenReturn("192.168.100.150");
        when(publicNetwork.getTrafficType()).thenReturn(Networks.TrafficType.Public);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddressVO);

        assertTrue(tungstenService.removePublicNetworkSubnet(vlanVO));
        verify(ipAddressDao, times(1)).unassignIpAddress(anyLong());
    }

    @Test
    public void allocateDnsIpAddressTest() {
        NetworkVO networkVO = mock(NetworkVO.class);
        TungstenAnswer getTungstenNetworkDnsAnswer = MockTungstenAnswerFactory.get(true);
        TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = mock(TungstenGuestNetworkIpAddressVO.class);

        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenNetworkDnsCommand.class), anyLong())).thenReturn(getTungstenNetworkDnsAnswer);
        when(networkVO.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(tungstenGuestNetworkIpAddressDao.persist(any(TungstenGuestNetworkIpAddressVO.class))).thenReturn(tungstenGuestNetworkIpAddressVO);
        when(networkVO.getCidr()).thenReturn("192.168.100.0/24");
        when(getTungstenNetworkDnsAnswer.getDetails()).thenReturn("192.168.100.100");

        assertTrue(tungstenService.allocateDnsIpAddress(networkVO, null, "test"));
    }

    @Test
    public void deallocateDnsIpAddressTest() {
        NetworkVO networkVO = mock(NetworkVO.class);
        TungstenAnswer getTungstenNetworkDnsAnswer = MockTungstenAnswerFactory.get(true);
        TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = mock(TungstenGuestNetworkIpAddressVO.class);

        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenNetworkDnsCommand.class), anyLong())).thenReturn(getTungstenNetworkDnsAnswer);
        when(networkVO.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(networkVO.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(tungstenGuestNetworkIpAddressDao.findByNetworkAndGuestIpAddress(anyLong(), anyString())).thenReturn(tungstenGuestNetworkIpAddressVO);
        when(getTungstenNetworkDnsAnswer.getDetails()).thenReturn("192.168.100.100");

        tungstenService.deallocateDnsIpAddress(networkVO, null, "test");
        verify(tungstenGuestNetworkIpAddressDao, times(1)).expunge(anyLong());
    }

    @Test
    public void subscribeTungstenEventTest() {
        tungstenService.subscribeTungstenEvent();

        verify(messageBus, times(1)).subscribe(eq(IpAddressManager.MESSAGE_ASSIGN_IPADDR_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(IpAddressManager.MESSAGE_RELEASE_IPADDR_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(TungstenService.MESSAGE_APPLY_NETWORK_POLICY_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(ConfigurationManager.MESSAGE_CREATE_VLAN_IP_RANGE_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(ConfigurationManager.MESSAGE_DELETE_VLAN_IP_RANGE_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(ConfigurationManager.MESSAGE_CREATE_POD_IP_RANGE_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(ConfigurationManager.MESSAGE_DELETE_POD_IP_RANGE_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(DomainManager.MESSAGE_CREATE_TUNGSTEN_DOMAIN_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(DomainManager.MESSAGE_DELETE_TUNGSTEN_DOMAIN_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(ProjectManager.MESSAGE_CREATE_TUNGSTEN_PROJECT_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(ProjectManager.MESSAGE_DELETE_TUNGSTEN_PROJECT_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(TungstenService.MESSAGE_SYNC_TUNGSTEN_DB_WITH_DOMAINS_AND_PROJECTS_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(SecurityGroupService.MESSAGE_CREATE_TUNGSTEN_SECURITY_GROUP_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(SecurityGroupService.MESSAGE_DELETE_TUNGSTEN_SECURITY_GROUP_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(SecurityGroupService.MESSAGE_ADD_SECURITY_GROUP_RULE_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(SecurityGroupService.MESSAGE_REMOVE_SECURITY_GROUP_RULE_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(NetworkService.MESSAGE_ASSIGN_NIC_SECONDARY_IP_EVENT), any(MessageSubscriber.class));
        verify(messageBus, times(1)).subscribe(eq(NetworkService.MESSAGE_RELEASE_NIC_SECONDARY_IP_EVENT), any(MessageSubscriber.class));
    }

    @Test
    public void syncTungstenDbWithCloudstackProjectsAndDomainsTest() {
        DomainVO domainVO = mock(DomainVO.class);
        ProjectVO projectVO = mock(ProjectVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer createTungstenDomainAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer createTungstenProjectAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer createTungstenDefaultProjectAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateTungstenDefaultSecurityGroupAnswer = MockTungstenAnswerFactory.get(true);

        when(domainDao.listAll()).thenReturn(List.of(domainVO));
        when(projectDao.listAll()).thenReturn(List.of(projectVO));
        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenDomainCommand.class), anyLong())).thenReturn(createTungstenDomainAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenProjectCommand.class), anyLong())).thenReturn(createTungstenProjectAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateTungstenDefaultSecurityGroupCommand.class), anyLong())).thenReturn(updateTungstenDefaultSecurityGroupAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenDefaultProjectCommand.class), anyLong())).thenReturn(createTungstenDefaultProjectAnswer);
        when(domainDao.findById(anyLong())).thenReturn(domainVO);

        assertTrue(tungstenService.syncTungstenDbWithCloudstackProjectsAndDomains());
    }

    @Test
    public void updateLoadBalancerTest() {
        Network network = mock(Network.class);
        LoadBalancingRule loadBalancingRule = mock(LoadBalancingRule.class);
        Network publicNetwork = mock(Network.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        HostVO hostVO = mock(HostVO.class);
        TungstenAnswer getTungstenLoadBalancerAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer updateLoadBalancerServiceInstanceAnswer = MockTungstenAnswerFactory.get(true);
        Answer updateTungstenLoadbalancerStatsAnswer = mock(Answer.class);
        Answer updateTungstenLoadbalancerSslAnswer = mock(Answer.class);
        FirewallRuleVO firewallRuleVO = mock(FirewallRuleVO.class);
        LoadBalancerCertMapVO loadBalancerCertMapVO = mock(LoadBalancerCertMapVO.class);
        SslCertVO sslCertVO = mock(SslCertVO.class);
        TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = mock(TungstenGuestNetworkIpAddressVO.class);
        Ip ip = mock(Ip.class);
        AccountVO accountVO = mock(AccountVO.class);
        Loadbalancer loadbalancer = mock(Loadbalancer.class);

        when(networkModel.getSystemNetworkByZoneAndTrafficType(anyLong(), eq(Networks.TrafficType.Public))).thenReturn(publicNetwork);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddressVO);
        when(hostDao.listAllHostsByZoneAndHypervisorType(anyLong(), eq(Hypervisor.HypervisorType.KVM))).thenReturn(List.of(hostVO));
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenLoadBalancerCommand.class), anyLong())).thenReturn(getTungstenLoadBalancerAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(UpdateLoadBalancerServiceInstanceCommand.class), anyLong())).thenReturn(updateLoadBalancerServiceInstanceAnswer);
        when(agentMgr.easySend(anyLong(), any(UpdateTungstenLoadbalancerStatsCommand.class))).thenReturn(updateTungstenLoadbalancerStatsAnswer);
        when(agentMgr.easySend(anyLong(), any(UpdateTungstenLoadbalancerSslCommand.class))).thenReturn(updateTungstenLoadbalancerSslAnswer);
        when(updateTungstenLoadbalancerStatsAnswer.getResult()).thenReturn(true);
        when(updateTungstenLoadbalancerSslAnswer.getResult()).thenReturn(true);
        when(configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key())).thenReturn("enabled");
        when(fwRulesDao.listByIpAndPurposeAndNotRevoked(anyLong(), eq(FirewallRule.Purpose.LoadBalancing))).thenReturn(List.of(firewallRuleVO));
        when(lbCertMapDao.findByLbRuleId(anyLong())).thenReturn(loadBalancerCertMapVO);
        when(entityMgr.findById(eq(SslCertVO.class), anyLong())).thenReturn(sslCertVO);
        when(tungstenGuestNetworkIpAddressDao.findByNetworkIdAndPublicIp(anyLong(), anyString())).thenReturn(tungstenGuestNetworkIpAddressVO);
        when(loadBalancingRule.getSourceIp()).thenReturn(ip);
        when(accountDao.findById(anyLong())).thenReturn(accountVO);
        when(ip.addr()).thenReturn("192.168.100.100");
        when(getTungstenLoadBalancerAnswer.getApiObjectBase()).thenReturn(loadbalancer);
        when(ipAddressVO.getAddress()).thenReturn(ip);
        when(tungstenGuestNetworkIpAddressVO.getGuestIpAddress()).thenReturn(ip);

        assertTrue(tungstenService.updateLoadBalancer(network, loadBalancingRule));
    }

    @Test
    public void createTungstenSecurityGroupTest() {
        SecurityGroup securityGroup = mock(SecurityGroup.class);
        ProjectVO projectVO = mock(ProjectVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        DomainVO domainVO = mock(DomainVO.class);
        TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = mock(TungstenSecurityGroupRuleVO.class);
        TungstenAnswer createTungstenSecurityGroupAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer addTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);

        when(projectDao.findByProjectAccountId(anyLong())).thenReturn(projectVO);
        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(domainDao.findById(anyLong())).thenReturn(domainVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenSecurityGroupCommand.class), anyLong())).thenReturn(createTungstenSecurityGroupAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(addTungstenSecurityGroupRuleAnswer);

        try (MockedStatic<Transaction> transactionMocked = Mockito.mockStatic(Transaction.class)) {
            transactionMocked.when(() -> Transaction.execute(any(TransactionCallback.class))).thenReturn(List.of(tungstenSecurityGroupRuleVO));
            assertTrue(tungstenService.createTungstenSecurityGroup(securityGroup));
        }
    }

    @Test
    public void deleteTungstenSecurityGroupTest() {
        SecurityGroup securityGroup = mock(SecurityGroup.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer deleteTungstenSecurityGroupAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenSecurityGroupCommand.class), anyLong())).thenReturn(deleteTungstenSecurityGroupAnswer);

        assertTrue(tungstenService.deleteTungstenSecurityGroup(securityGroup));
    }

    @Test
    public void addTungstenSecurityGroupEgressRuleTest() {
        SecurityRule securityRule = mock(SecurityRule.class);
        SecurityGroupVO securityGroupVO = mock(SecurityGroupVO.class);
        TungstenAnswer getTungstenSecurityGroupAnswer = mock(TungstenAnswer.class);
        TungstenAnswer removeTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer addTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        net.juniper.tungsten.api.types.SecurityGroup securityGroup = mock(net.juniper.tungsten.api.types.SecurityGroup.class);
        TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = mock(TungstenSecurityGroupRuleVO.class);
        NicVO nicVO = mock(NicVO.class);

        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(securityGroupDao.findById(anyLong())).thenReturn(securityGroupVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenSecurityGroupCommand.class), anyLong())).thenReturn(getTungstenSecurityGroupAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(removeTungstenSecurityGroupRuleAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(addTungstenSecurityGroupRuleAnswer);
        when(getTungstenSecurityGroupAnswer.getApiObjectBase()).thenReturn(securityGroup);
        when(securityRule.getRuleType()).thenReturn(SecurityRule.SecurityRuleType.EgressRule);
        when(tungstenSecurityGroupRuleDao.findDefaultSecurityRule(anyLong(), anyString(), anyString())).thenReturn(tungstenSecurityGroupRuleVO);
        when(securityRule.getProtocol()).thenReturn(NetUtils.ALL_PROTO);
        when(securityRule.getEndPort()).thenReturn(NetUtils.PORT_RANGE_MIN);
        when(securityGroupVMMapDao.listVmIdsBySecurityGroup(anyLong())).thenReturn(List.of(1L));
        when(nicDao.findDefaultNicForVM(anyLong())).thenReturn(nicVO);
        when(nicVO.getIPv4Address()).thenReturn("192.168.100.100");
        when(nicVO.getIPv6Address()).thenReturn("fd00::1");
        when(nicVO.getSecondaryIp()).thenReturn(true);
        when(nicSecIpDao.getSecondaryIpAddressesForNic(anyLong())).thenReturn(List.of("192.168.100.200"));
        when(tungstenSecurityGroupRuleDao.persist(any(TungstenSecurityGroupRuleVO.class))).thenReturn(tungstenSecurityGroupRuleVO);
        when(securityRule.getAllowedNetworkId()).thenReturn(1L);

        assertTrue(tungstenService.addTungstenSecurityGroupRule(List.of(securityRule)));
    }

    @Test
    public void addTungstenSecurityGroupIngressRuleTest() {
        SecurityRule securityRule = mock(SecurityRule.class);
        SecurityGroupVO securityGroupVO = mock(SecurityGroupVO.class);
        TungstenAnswer getTungstenSecurityGroupAnswer = mock(TungstenAnswer.class);
        TungstenAnswer addTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        net.juniper.tungsten.api.types.SecurityGroup securityGroup = mock(net.juniper.tungsten.api.types.SecurityGroup.class);

        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(securityGroupDao.findById(anyLong())).thenReturn(securityGroupVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenSecurityGroupCommand.class), anyLong())).thenReturn(getTungstenSecurityGroupAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(addTungstenSecurityGroupRuleAnswer);
        when(getTungstenSecurityGroupAnswer.getApiObjectBase()).thenReturn(securityGroup);
        when(securityRule.getRuleType()).thenReturn(SecurityRule.SecurityRuleType.IngressRule);
        when(securityRule.getProtocol()).thenReturn(NetUtils.ALL_PROTO);
        when(securityRule.getEndPort()).thenReturn(NetUtils.PORT_RANGE_MIN);
        when(securityRule.getAllowedNetworkId()).thenReturn(null);

        assertTrue(tungstenService.addTungstenSecurityGroupRule(List.of(securityRule)));
    }

    @Test
    public void removeTungstenSecurityGroupEgressRuleTest() {
        SecurityRule securityRule = mock(SecurityRule.class);
        SecurityGroupVO securityGroupVO = mock(SecurityGroupVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = mock(TungstenSecurityGroupRuleVO.class);
        TungstenAnswer addTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer removeTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);
        NicVO nicVO = mock(NicVO.class);

        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(securityGroupDao.findById(anyLong())).thenReturn(securityGroupVO);
        when(securityRule.getRuleType()).thenReturn(SecurityRule.SecurityRuleType.EgressRule);
        when(securityRule.getType()).thenReturn("egress");
        when(tungstenSecurityGroupRuleDao.persist(any(TungstenSecurityGroupRuleVO.class))).thenReturn(tungstenSecurityGroupRuleVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(addTungstenSecurityGroupRuleAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(removeTungstenSecurityGroupRuleAnswer);
        when(securityRule.getAllowedNetworkId()).thenReturn(1L);
        when(securityGroupVMMapDao.listVmIdsBySecurityGroup(anyLong())).thenReturn(List.of(1L));
        when(nicDao.findDefaultNicForVM(anyLong())).thenReturn(nicVO);
        when(tungstenSecurityGroupRuleDao.expunge(anyLong())).thenReturn(true);
        when(nicVO.getIPv4Address()).thenReturn("192.168.100.100");
        when(nicVO.getIPv6Address()).thenReturn("fd00::1");
        when(nicVO.getSecondaryIp()).thenReturn(true);
        when(nicSecIpDao.getSecondaryIpAddressesForNic(anyLong())).thenReturn(List.of("192.168.100.200"));
        when(tungstenSecurityGroupRuleDao.findBySecurityGroupAndRuleTypeAndRuleTarget(anyLong(), anyString(), anyString())).thenReturn(tungstenSecurityGroupRuleVO);

        assertTrue(tungstenService.removeTungstenSecurityGroupRule(securityRule));
    }

    @Test
    public void removeTungstenSecurityGroupIngressRuleTest() {
        SecurityRule securityRule = mock(SecurityRule.class);
        SecurityGroupVO securityGroupVO = mock(SecurityGroupVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer removeTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(securityGroupDao.findById(anyLong())).thenReturn(securityGroupVO);
        when(securityRule.getRuleType()).thenReturn(SecurityRule.SecurityRuleType.IngressRule);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(removeTungstenSecurityGroupRuleAnswer);
        when(securityRule.getAllowedNetworkId()).thenReturn(null);

        assertTrue(tungstenService.removeTungstenSecurityGroupRule(securityRule));
    }

    @Test
    public void addTungstenNicSecondaryIpAddressTest() {
        NicSecondaryIp nicSecondaryIp = mock(NicSecondaryIp.class);
        Network network = mock(Network.class);
        DataCenter dataCenter = mock(DataCenter.class);
        Nic nic = mock(Nic.class);
        SecurityGroupVO securityGroupVO = mock(SecurityGroupVO.class);
        SecurityGroupRuleVO securityGroupRuleVO = mock(SecurityGroupRuleVO.class);
        TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = mock(TungstenSecurityGroupRuleVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer addTungstenSecondaryIpAddressAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer addTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);

        when(entityMgr.findById(eq(NicSecondaryIp.class), anyLong())).thenReturn(nicSecondaryIp);
        when(entityMgr.findById(eq(Network.class), anyLong())).thenReturn(network);
        when(entityMgr.findById(eq(DataCenter.class), anyLong())).thenReturn(dataCenter);
        when(entityMgr.findById(eq(Nic.class), anyLong())).thenReturn(nic);
        when(nicSecondaryIp.getIp4Address()).thenReturn("192.168.100.100");
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenSecondaryIpAddressCommand.class), anyLong())).thenReturn(addTungstenSecondaryIpAddressAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(addTungstenSecurityGroupRuleAnswer);
        when(dataCenter.isSecurityGroupEnabled()).thenReturn(true);
        when(network.getGuestType()).thenReturn(Network.GuestType.Shared);
        when(securityGroupManager.getSecurityGroupsForVm(anyLong())).thenReturn(List.of(securityGroupVO));
        when(securityGroupRuleDao.listByAllowedSecurityGroupId(anyLong())).thenReturn(List.of(securityGroupRuleVO));
        when(tungstenSecurityGroupRuleDao.persist(any(TungstenSecurityGroupRuleVO.class))).thenReturn(tungstenSecurityGroupRuleVO);
        when(securityGroupRuleVO.getProtocol()).thenReturn(NetUtils.ALL_PROTO);
        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));

        assertTrue(tungstenService.addTungstenNicSecondaryIpAddress(1L));
    }

    @Test
    public void removeTungstenNicSecondaryIpAddressTest() {
        NicSecondaryIpVO nicSecondaryIpVO = mock(NicSecondaryIpVO.class);
        Network network = mock(Network.class);
        DataCenter dataCenter = mock(DataCenter.class);
        TungstenAnswer removeTungstenSecondaryIpAddressAnswer = MockTungstenAnswerFactory.get(true);
        TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = mock(TungstenSecurityGroupRuleVO.class);
        SecurityGroupVO securityGroupVO = mock(SecurityGroupVO.class);
        TungstenAnswer removeTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);

        when(entityMgr.findById(eq(Network.class), anyLong())).thenReturn(network);
        when(entityMgr.findById(eq(DataCenter.class), anyLong())).thenReturn(dataCenter);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenSecondaryIpAddressCommand.class), anyLong())).thenReturn(removeTungstenSecondaryIpAddressAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(removeTungstenSecurityGroupRuleAnswer);

        when(dataCenter.isSecurityGroupEnabled()).thenReturn(true);
        when(network.getGuestType()).thenReturn(Network.GuestType.Shared);
        when(tungstenSecurityGroupRuleDao.listByRuleTarget(anyString())).thenReturn(List.of(tungstenSecurityGroupRuleVO));
        when(securityGroupDao.findById(anyLong())).thenReturn(securityGroupVO);
        when(tungstenSecurityGroupRuleDao.expunge(anyLong())).thenReturn(true);
        when(nicSecondaryIpVO.getIp4Address()).thenReturn("192.168.100.100");

        assertTrue(tungstenService.removeTungstenNicSecondaryIpAddress(nicSecondaryIpVO));
    }

    @Test
    public void createTungstenPolicyTest() {
        TungstenAnswer createTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenNetworkPolicy tungstenNetworkPolicy = mock(TungstenNetworkPolicy.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenPolicyCommand.class), anyLong())).thenReturn(createTungstenPolicyAnswer);
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(createTungstenPolicyAnswer.getTungstenModel()).thenReturn(tungstenNetworkPolicy);
        when(tungstenNetworkPolicy.getNetworkPolicy()).thenReturn(networkPolicy);

        assertNotNull(tungstenService.createTungstenPolicy(1L, "test"));
    }

    @Test
    public void addTungstenPolicyRuleTest() throws Exception {
        TungstenAnswer addTungstenPolicyRuleAnswer = MockTungstenAnswerFactory.get(true);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        PolicyEntriesType policyEntriesType = mock(PolicyEntriesType.class);
        PolicyRuleType policyRuleType = mock(PolicyRuleType.class);
        ActionListType actionListType = mock(ActionListType.class);
        AddressType addressType = mock(AddressType.class);
        SubnetType subnetType = mock(SubnetType.class);
        PortType portType = mock(PortType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenPolicyRuleCommand.class), anyLong())).thenReturn(addTungstenPolicyRuleAnswer);
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(addTungstenPolicyRuleAnswer.getApiObjectBase()).thenReturn(networkPolicy);
        when(networkPolicy.getEntries()).thenReturn(policyEntriesType);
        when(policyEntriesType.getPolicyRule()).thenReturn(List.of(policyRuleType));

        when(policyRuleType.getRuleUuid()).thenReturn("8b4637b6-5629-46de-8fb2-d0b0502bfa85");
        when(policyRuleType.getActionList()).thenReturn(actionListType);
        when(actionListType.getSimpleAction()).thenReturn("pass");
        when(policyRuleType.getSrcAddresses()).thenReturn(List.of(addressType));
        when(addressType.getSubnet()).thenReturn(subnetType);
        when(policyRuleType.getSrcPorts()).thenReturn(List.of(portType));
        when(policyRuleType.getDstAddresses()).thenReturn(List.of(addressType));
        when(policyRuleType.getDstPorts()).thenReturn(List.of(portType));

        try (MockedConstruction<AddTungstenPolicyRuleCommand> ignored =
                     Mockito.mockConstruction(AddTungstenPolicyRuleCommand.class, (mock, context) -> {
                         when(mock.getUuid()).thenReturn("8b4637b6-5629-46de-8fb2-d0b0502bfa85");
                     })) {

            assertNotNull(tungstenService.addTungstenPolicyRule(1L, "948f421c-edde-4518-a391-09299cc25dc2", "pass",
                    "<>", "tcp", "network1", "192.168.100.100", 32, 80, 80,
                    "network2", "192.168.200.200", 32, 80, 80));
        }
    }

    @Test
    public void listTungstenPolicyTest() {
        NetworkVO networkVO = mock(NetworkVO.class);
        TungstenAnswer listTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenNetworkPolicy tungstenNetworkPolicy = mock(TungstenNetworkPolicy.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(networkDao.findById(anyLong())).thenReturn(networkVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenPolicyCommand.class), anyLong())).thenReturn(listTungstenPolicyAnswer);
        when(listTungstenPolicyAnswer.getTungstenModelList()).thenReturn(List.of(tungstenNetworkPolicy));
        when(tungstenNetworkPolicy.getNetworkPolicy()).thenReturn(networkPolicy);
        when(tungstenNetworkPolicy.getVirtualNetworkList()).thenReturn(
                List.of(virtualNetwork));

        assertNotNull(tungstenService.listTungstenPolicy(1L, 2L, 3L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void listTungstenNetworkTest() {
        TungstenAnswer listTungstenNetworkAnswer = MockTungstenAnswerFactory.get(true);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenNetworkCommand.class), anyLong())).thenReturn(listTungstenNetworkAnswer);
        doReturn(List.of(virtualNetwork)).when(listTungstenNetworkAnswer).getApiObjectBaseList();
        when((virtualNetwork.getName())).thenReturn("guestNetwork1");

        assertNotNull(tungstenService.listTungstenNetwork(1L, "948f421c-edde-4518-a391-09299cc25dc2", false));
    }

    @Test
    public void listTungstenNicTest() {
        TungstenAnswer listTungstenNicAnswer = MockTungstenAnswerFactory.get(true);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenNicCommand.class), anyLong())).thenReturn(listTungstenNicAnswer);
        doReturn(List.of(virtualMachineInterface)).when(listTungstenNicAnswer).getApiObjectBaseList();

        assertNotNull(tungstenService.listTungstenNic(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void listTungstenVmTest() {
        TungstenAnswer listTungstenVmAnswer = MockTungstenAnswerFactory.get(true);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenVmCommand.class), anyLong())).thenReturn(listTungstenVmAnswer);
        doReturn(List.of(virtualMachine)).when(listTungstenVmAnswer).getApiObjectBaseList();

        assertNotNull(tungstenService.listTungstenVm(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void deleteTungstenPolicyTest() {
        TungstenAnswer deleteTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenPolicyCommand.class), anyLong())).thenReturn(deleteTungstenPolicyAnswer);

        assertTrue(tungstenService.deleteTungstenPolicy(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void listTungstenPolicyRuleWithRuleUuidTest() {
        TungstenAnswer listTungstenPolicyRuleAnswer = MockTungstenAnswerFactory.get(true);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        PolicyEntriesType policyEntriesType = mock(PolicyEntriesType.class);
        PolicyRuleType policyRuleType = mock(PolicyRuleType.class);
        ActionListType actionListType = mock(ActionListType.class);
        AddressType addressType = mock(AddressType.class);
        SubnetType subnetType = mock(SubnetType.class);
        PortType portType = mock(PortType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenPolicyRuleCommand.class), anyLong())).thenReturn(listTungstenPolicyRuleAnswer);
        doReturn(networkPolicy).when(listTungstenPolicyRuleAnswer).getApiObjectBase();
        when(networkPolicy.getEntries()).thenReturn(policyEntriesType);
        when(policyEntriesType.getPolicyRule()).thenReturn(List.of(policyRuleType));
        when(policyRuleType.getRuleUuid()).thenReturn("8b4637b6-5629-46de-8fb2-d0b0502bfa85");
        when(policyRuleType.getActionList()).thenReturn(actionListType);
        when(actionListType.getSimpleAction()).thenReturn("pass");
        when(policyRuleType.getSrcAddresses()).thenReturn(List.of(addressType));
        when(addressType.getSubnet()).thenReturn(subnetType);
        when(policyRuleType.getSrcPorts()).thenReturn(List.of(portType));
        when(policyRuleType.getDstAddresses()).thenReturn(List.of(addressType));
        when(policyRuleType.getDstPorts()).thenReturn(List.of(portType));

        assertNotNull(tungstenService.listTungstenPolicyRule(1L, "948f421c-edde-4518-a391-09299cc25dc2", "8b4637b6-5629-46de-8fb2-d0b0502bfa85"));
    }

    @Test
    public void listTungstenPolicyRuleWithAllRuleTest() {
        TungstenAnswer listTungstenPolicyRuleAnswer = MockTungstenAnswerFactory.get(true);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        PolicyEntriesType policyEntriesType = mock(PolicyEntriesType.class);
        PolicyRuleType policyRuleType = mock(PolicyRuleType.class);
        ActionListType actionListType = mock(ActionListType.class);
        AddressType addressType = mock(AddressType.class);
        SubnetType subnetType = mock(SubnetType.class);
        PortType portType = mock(PortType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenPolicyRuleCommand.class), anyLong())).thenReturn(listTungstenPolicyRuleAnswer);
        doReturn(networkPolicy).when(listTungstenPolicyRuleAnswer).getApiObjectBase();
        when(networkPolicy.getEntries()).thenReturn(policyEntriesType);
        when(policyEntriesType.getPolicyRule()).thenReturn(List.of(policyRuleType));
        when(policyRuleType.getRuleUuid()).thenReturn("8b4637b6-5629-46de-8fb2-d0b0502bfa85");
        when(policyRuleType.getActionList()).thenReturn(actionListType);
        when(actionListType.getSimpleAction()).thenReturn("pass");
        when(policyRuleType.getSrcAddresses()).thenReturn(List.of(addressType));
        when(addressType.getSubnet()).thenReturn(subnetType);
        when(policyRuleType.getSrcPorts()).thenReturn(List.of(portType));
        when(policyRuleType.getDstAddresses()).thenReturn(List.of(addressType));
        when(policyRuleType.getDstPorts()).thenReturn(List.of(portType));

        assertNotNull(tungstenService.listTungstenPolicyRule(1L, "948f421c-edde-4518-a391-09299cc25dc2", null));
    }

    @Test
    public void removeTungstenPolicyRuleTest() {
        TungstenAnswer removeTungstenPolicyRuleAnswer = MockTungstenAnswerFactory.get(true);
        TungstenNetworkPolicy tungstenNetworkPolicy = mock(TungstenNetworkPolicy.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenPolicyRuleCommand.class), anyLong())).thenReturn(removeTungstenPolicyRuleAnswer);
        when(removeTungstenPolicyRuleAnswer.getTungstenModel()).thenReturn(tungstenNetworkPolicy);
        when(tungstenNetworkPolicy.getNetworkPolicy()).thenReturn(networkPolicy);
        when(tungstenNetworkPolicy.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));

        assertNotNull(tungstenService.removeTungstenPolicyRule(1L, "948f421c-edde-4518-a391-09299cc25dc2", "8b4637b6-5629-46de-8fb2-d0b0502bfa85"));
    }

    @Test
    public void createTungstenTagTest() {
        TungstenAnswer createTungstenTagAnswer = MockTungstenAnswerFactory.get(true);
        TungstenTag tungstenTag = mock(TungstenTag.class);
        Tag tag = mock(Tag.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenTagCommand.class), anyLong())).thenReturn(createTungstenTagAnswer);
        when(createTungstenTagAnswer.getTungstenModel()).thenReturn(tungstenTag);
        when(tungstenTag.getTag()).thenReturn(tag);
        doReturn(List.of(virtualNetwork)).when(tungstenTag).getVirtualNetworkList();
        doReturn(List.of(virtualMachine)).when(tungstenTag).getVirtualMachineList();
        doReturn(List.of(virtualMachineInterface)).when(tungstenTag).getVirtualMachineInterfaceList();
        doReturn(List.of(networkPolicy)).when(tungstenTag).getNetworkPolicyList();

        assertNotNull(tungstenService.createTungstenTag(1L, "testTag", "testTagType"));
    }

    @Test
    public void createTungstenTagTypeTest() {
        TungstenAnswer createTungstenTagTypeAnswer = MockTungstenAnswerFactory.get(true);
        TagType tagtype = mock(TagType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenTagTypeCommand.class), anyLong())).thenReturn(createTungstenTagTypeAnswer);
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(createTungstenTagTypeAnswer.getApiObjectBase()).thenReturn(tagtype);

        assertNotNull(tungstenService.createTungstenTagType(1L, "testTagType"));
    }

    @Test
    public void listTungstenTagsTest() {
        TungstenAnswer listTungstenTagAnswer = MockTungstenAnswerFactory.get(true);
        TungstenTag tungstenTag = mock(TungstenTag.class);
        Tag tag = mock(Tag.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        ApplicationPolicySet applicationPolicySet = mock(ApplicationPolicySet.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenTagCommand.class), anyLong())).thenReturn(listTungstenTagAnswer);
        when(listTungstenTagAnswer.getTungstenModelList()).thenReturn(List.of(tungstenTag));
        when(tungstenTag.getTag()).thenReturn(tag);
        doReturn(List.of(virtualNetwork)).when(tungstenTag).getVirtualNetworkList();
        doReturn(List.of(virtualMachine)).when(tungstenTag).getVirtualMachineList();
        doReturn(List.of(virtualMachineInterface)).when(tungstenTag).getVirtualMachineInterfaceList();
        doReturn(List.of(networkPolicy)).when(tungstenTag).getNetworkPolicyList();

        assertNotNull(tungstenService.listTungstenTags(1L, "948f421c-edde-4518-a391-09299cc25dc2"
                , "8b4637b6-5629-46de-8fb2-d0b0502bfa85", "8d097a79-a38d-4db4-8a41-16f15d9c5afa", "a329662e-1805-4a89-9b05-2b818ea35978",
                "d5e3f5c5-97ed-41b6-9b6f-7f696b9eddeb", "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652"));
    }

    @Test
    public void listTungstenTagTypesTest() {
        TungstenAnswer listTungstenTagTypeAnswer = MockTungstenAnswerFactory.get(true);
        TagType tagtype = mock(TagType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenTagTypeCommand.class), anyLong())).thenReturn(listTungstenTagTypeAnswer);
        doReturn(List.of(tagtype)).when(listTungstenTagTypeAnswer).getApiObjectBaseList();

        assertNotNull(tungstenService.listTungstenTagTypes(1L, "testTagType"));
    }

    @Test
    public void deleteTungstenTagTest() {
        TungstenAnswer deleteTungstenTagAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenTagCommand.class), anyLong())).thenReturn(deleteTungstenTagAnswer);

        assertTrue(tungstenService.deleteTungstenTag(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void deleteTungstenTagTypeTest() {
        TungstenAnswer deleteTungstenTagTypeAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenTagTypeCommand.class), anyLong())).thenReturn(deleteTungstenTagTypeAnswer);

        assertTrue(tungstenService.deleteTungstenTagType(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void applyTungstenPolicyTest() {
        TungstenAnswer applyTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenNetworkPolicy tungstenNetworkPolicy = mock(TungstenNetworkPolicy.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenNetworkPolicyCommand.class), anyLong())).thenReturn(applyTungstenPolicyAnswer);
        when(applyTungstenPolicyAnswer.getTungstenModel()).thenReturn(tungstenNetworkPolicy);
        when(tungstenNetworkPolicy.getNetworkPolicy()).thenReturn(networkPolicy);
        when(tungstenNetworkPolicy.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));

        assertNotNull(tungstenService.applyTungstenPolicy(1L, "948f421c-edde-4518-a391-09299cc25dc2", "8b4637b6-5629-46de-8fb2-d0b0502bfa85", 1, 1));
    }

    @Test
    public void applyTungstenTagTest() {
        TungstenAnswer applyTungstenTagAnswer = MockTungstenAnswerFactory.get(true);
        TungstenTag tungstenTag = mock(TungstenTag.class);
        Tag tag = mock(Tag.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ApplyTungstenTagCommand.class), anyLong())).thenReturn(applyTungstenTagAnswer);
        when(applyTungstenTagAnswer.getTungstenModel()).thenReturn(tungstenTag);
        when(tungstenTag.getTag()).thenReturn(tag);
        when(tungstenTag.getNetworkPolicyList()).thenReturn(List.of(networkPolicy));
        when(tungstenTag.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));
        when(tungstenTag.getVirtualMachineList()).thenReturn(List.of(virtualMachine));
        when(tungstenTag.getVirtualMachineInterfaceList()).thenReturn(List.of(virtualMachineInterface));

        assertNotNull(tungstenService.applyTungstenTag(1L, List.of("948f421c-edde-4518-a391-09299cc25dc2"), List.of("8b4637b6-5629-46de-8fb2-d0b0502bfa85")
                , List.of("8d097a79-a38d-4db4-8a41-16f15d9c5afa"), "a329662e-1805-4a89-9b05-2b818ea35978", "d5e3f5c5-97ed-41b6-9b6f-7f696b9eddeb"
                , "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652"));
    }

    @Test
    public void removeTungstenPolicyTest() {
        TungstenAnswer removeTungstenPolicyAnswer = MockTungstenAnswerFactory.get(true);
        TungstenNetworkPolicy tungstenNetworkPolicy = mock(TungstenNetworkPolicy.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenPolicyCommand.class), anyLong())).thenReturn(removeTungstenPolicyAnswer);
        when(removeTungstenPolicyAnswer.getTungstenModel()).thenReturn(tungstenNetworkPolicy);
        when(tungstenNetworkPolicy.getNetworkPolicy()).thenReturn(networkPolicy);
        when(tungstenNetworkPolicy.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));

        assertNotNull(tungstenService.removeTungstenPolicy(1L, "948f421c-edde-4518-a391-09299cc25dc2", "8b4637b6-5629-46de-8fb2-d0b0502bfa85"));
    }

    @Test
    public void removeTungstenTagTest() {
        TungstenAnswer removeTungstenTagAnswer = MockTungstenAnswerFactory.get(true);
        TungstenTag tungstenTag = mock(TungstenTag.class);
        Tag tag = mock(Tag.class);
        NetworkPolicy networkPolicy = mock(NetworkPolicy.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        VirtualMachine virtualMachine = mock(VirtualMachine.class);
        VirtualMachineInterface virtualMachineInterface = mock(VirtualMachineInterface.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenTagCommand.class), anyLong())).thenReturn(removeTungstenTagAnswer);
        when(removeTungstenTagAnswer.getTungstenModel()).thenReturn(tungstenTag);
        when(tungstenTag.getTag()).thenReturn(tag);
        when(tungstenTag.getNetworkPolicyList()).thenReturn(List.of(networkPolicy));
        when(tungstenTag.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));
        when(tungstenTag.getVirtualMachineList()).thenReturn(List.of(virtualMachine));
        when(tungstenTag.getVirtualMachineInterfaceList()).thenReturn(List.of(virtualMachineInterface));

        assertNotNull(tungstenService.removeTungstenTag(1L, List.of("948f421c-edde-4518-a391-09299cc25dc2"),
                List.of("8b4637b6-5629-46de-8fb2-d0b0502bfa85"),
                List.of("8d097a79-a38d-4db4-8a41-16f15d9c5afa"), "a329662e-1805-4a89-9b05-2b818ea35978", null,
                "d5e3f5c5-97ed-41b6-9b6f-7f696b9eddeb"));
    }

    @Test
    public void createTungstenAddressGroupTest() {
        TungstenAnswer createTungstenAddressGroupAnswer = MockTungstenAnswerFactory.get(true);
        AddressGroup addressGroup = mock(AddressGroup.class);
        SubnetListType subnetListType = mock(SubnetListType.class);
        SubnetType subnetType = mock(SubnetType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenAddressGroupCommand.class), anyLong())).thenReturn(createTungstenAddressGroupAnswer);
        when(createTungstenAddressGroupAnswer.getApiObjectBase()).thenReturn(addressGroup);
        when(addressGroup.getPrefix()).thenReturn(subnetListType);
        when(subnetListType.getSubnet()).thenReturn(List.of(subnetType));

        assertNotNull(tungstenService.createTungstenAddressGroup(1L, "test", "192.168.100.0", 24));
    }

    @Test
    public void createTungstenServiceGroupTest() {
        TungstenAnswer createTungstenServiceGroupAnswer = MockTungstenAnswerFactory.get(true);
        ServiceGroup serviceGroup = mock(ServiceGroup.class);
        FirewallServiceGroupType firewallServiceGroupType = mock(FirewallServiceGroupType.class);
        FirewallServiceType firewallServiceType = mock(FirewallServiceType.class);
        PortType portType = mock(PortType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenServiceGroupCommand.class), anyLong())).thenReturn(createTungstenServiceGroupAnswer);
        when(createTungstenServiceGroupAnswer.getApiObjectBase()).thenReturn(serviceGroup);
        when(serviceGroup.getFirewallServiceList()).thenReturn(firewallServiceGroupType);
        when(firewallServiceGroupType.getFirewallService()).thenReturn(List.of(firewallServiceType));
        when(firewallServiceType.getDstPorts()).thenReturn(portType);

        assertNotNull(tungstenService.createTungstenServiceGroup(1L, "test", "tcp", 80, 80));
    }

    @Test
    public void createTungstenFirewallRuleTest() {
        TungstenAnswer createTungstenFirewallRuleAnswer = MockTungstenAnswerFactory.get(true);
        net.juniper.tungsten.api.types.FirewallRule firewallRule = mock(net.juniper.tungsten.api.types.FirewallRule.class);
        ActionListType actionListType = mock(ActionListType.class);
        ObjectReference<ApiPropertyBase> serviceGroup = mock(ObjectReference.class);
        FirewallRuleEndpointType firewallRuleEndpointType1 = mock(FirewallRuleEndpointType.class);
        FirewallRuleEndpointType firewallRuleEndpointType2 = mock(FirewallRuleEndpointType.class);
        FirewallRuleMatchTagsType firewallRuleMatchTagsType = mock(FirewallRuleMatchTagsType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenFirewallRuleCommand.class), anyLong())).thenReturn(createTungstenFirewallRuleAnswer);
        when(createTungstenFirewallRuleAnswer.getApiObjectBase()).thenReturn(firewallRule);
        when(firewallRule.getActionList()).thenReturn(actionListType);
        when(actionListType.getSimpleAction()).thenReturn("pass");
        when(firewallRule.getServiceGroup()).thenReturn(List.of(serviceGroup));
        when(serviceGroup.getReferredName()).thenReturn(List.of("service"));
        when(firewallRule.getEndpoint1()).thenReturn(firewallRuleEndpointType1);
        when(firewallRule.getEndpoint2()).thenReturn(firewallRuleEndpointType2);
        when(firewallRuleEndpointType1.getTags()).thenReturn(List.of("tag"));
        when(firewallRuleEndpointType2.getTags()).thenReturn(null);
        when(firewallRuleEndpointType2.getAddressGroup()).thenReturn("address:group");
        when(firewallRule.getMatchTags()).thenReturn(firewallRuleMatchTagsType);

        assertNotNull(tungstenService.createTungstenFirewallRule(1L, "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652", "test", "pass", "948f421c-edde-4518-a391-09299cc25dc2",
                "8b4637b6-5629-46de-8fb2-d0b0502bfa85", "8d097a79-a38d-4db4-8a41-16f15d9c5afa", null, "<>", "a329662e-1805-4a89-9b05-2b818ea35978"
                , "d5e3f5c5-97ed-41b6-9b6f-7f696b9eddeb", null, "df8e4490-2a40-4d63-a6f3-1f829ffe4fc6", 1));
    }

    @Test
    public void createTungstenFirewallPolicyTest() {
        TungstenAnswer createTungstenFirewallPolicyAnswer = MockTungstenAnswerFactory.get(true);
        FirewallPolicy firewallPolicy = mock(FirewallPolicy.class);
        ObjectReference<FirewallSequence> firewallSequenceObjectReference = mock(ObjectReference.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenFirewallPolicyCommand.class), anyLong())).thenReturn(createTungstenFirewallPolicyAnswer);
        when(createTungstenFirewallPolicyAnswer.getApiObjectBase()).thenReturn(firewallPolicy);
        when(firewallPolicy.getFirewallRule()).thenReturn(List.of(firewallSequenceObjectReference));
        when(firewallSequenceObjectReference.getReferredName()).thenReturn(List.of("firewallrule"));

        assertNotNull(tungstenService.createTungstenFirewallPolicy(1L, "f5ba12c8-d4c5-4c20-a57d-67a9b6fca652","test", 1));
    }

    @Test
    public void createTungstenApplicationPolicySetTest() {
        TungstenAnswer createTungstenApplicationPolicySetAnswer = MockTungstenAnswerFactory.get(true);
        ApplicationPolicySet applicationPolicySet = mock(ApplicationPolicySet.class);
        ObjectReference<ApiPropertyBase> objectReference = mock(ObjectReference.class);
        ObjectReference<FirewallSequence> firewallSequenceObjectReference = mock(ObjectReference.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenApplicationPolicySetCommand.class), anyLong())).thenReturn(createTungstenApplicationPolicySetAnswer);
        when(createTungstenApplicationPolicySetAnswer.getApiObjectBase()).thenReturn(applicationPolicySet);
        when(applicationPolicySet.getTag()).thenReturn(List.of(objectReference));
        when(objectReference.getReferredName()).thenReturn(List.of("tag"));
        when(applicationPolicySet.getFirewallPolicy()).thenReturn(List.of(firewallSequenceObjectReference));
        when(firewallSequenceObjectReference.getReferredName()).thenReturn(List.of("firewallrule"));

        assertNotNull(tungstenService.createTungstenApplicationPolicySet(1L, "test"));
    }

    @Test
    public void listTungstenApplicationPolicySetTest() {
        TungstenAnswer listTungstenApplicationPolicySetAnswer = MockTungstenAnswerFactory.get(true);
        ApplicationPolicySet applicationPolicySet = mock(ApplicationPolicySet.class);
        ObjectReference<ApiPropertyBase> objectReference = mock(ObjectReference.class);
        ObjectReference<FirewallSequence> firewallSequenceObjectReference = mock(ObjectReference.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenApplicationPolicySetCommand.class), anyLong())).thenReturn(listTungstenApplicationPolicySetAnswer);
        doReturn(List.of(applicationPolicySet)).when(listTungstenApplicationPolicySetAnswer).getApiObjectBaseList();
        when(applicationPolicySet.getTag()).thenReturn(List.of(objectReference));
        when(objectReference.getReferredName()).thenReturn(List.of("tag"));
        when(applicationPolicySet.getFirewallPolicy()).thenReturn(List.of(firewallSequenceObjectReference));
        when(firewallSequenceObjectReference.getReferredName()).thenReturn(List.of("firewallrule"));

        assertNotNull(tungstenService.listTungstenApplicationPolicySet(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void listTungstenFirewallPolicyTest() {
        TungstenAnswer listTungstenFirewallPolicyAnswer = MockTungstenAnswerFactory.get(true);
        FirewallPolicy firewallPolicy = mock(FirewallPolicy.class);
        ObjectReference<FirewallSequence> firewallSequenceObjectReference = mock(ObjectReference.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenFirewallPolicyCommand.class), anyLong())).thenReturn(listTungstenFirewallPolicyAnswer);
        doReturn(List.of(firewallPolicy)).when(listTungstenFirewallPolicyAnswer).getApiObjectBaseList();
        when(firewallPolicy.getFirewallRule()).thenReturn(List.of(firewallSequenceObjectReference));
        when(firewallSequenceObjectReference.getReferredName()).thenReturn(List.of("firewallrule"));

        assertNotNull(tungstenService.listTungstenFirewallPolicy(1L, "948f421c-edde-4518-a391-09299cc25dc2", "8b4637b6-5629-46de-8fb2-d0b0502bfa85"));
    }

    @Test
    public void listTungstenFirewallRuleTest() {
        TungstenAnswer listTungstenFirewallRuleAnswer = MockTungstenAnswerFactory.get(true);
        net.juniper.tungsten.api.types.FirewallRule firewallRule = mock(net.juniper.tungsten.api.types.FirewallRule.class);
        ActionListType actionListType = mock(ActionListType.class);
        ObjectReference<ApiPropertyBase> serviceGroup = mock(ObjectReference.class);
        FirewallRuleEndpointType firewallRuleEndpointType1 = mock(FirewallRuleEndpointType.class);
        FirewallRuleEndpointType firewallRuleEndpointType2 = mock(FirewallRuleEndpointType.class);
        FirewallRuleMatchTagsType firewallRuleMatchTagsType = mock(FirewallRuleMatchTagsType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenFirewallRuleCommand.class), anyLong())).thenReturn(listTungstenFirewallRuleAnswer);
        doReturn(List.of(firewallRule)).when(listTungstenFirewallRuleAnswer).getApiObjectBaseList();
        when(firewallRule.getActionList()).thenReturn(actionListType);
        when(actionListType.getSimpleAction()).thenReturn("pass");
        when(firewallRule.getServiceGroup()).thenReturn(List.of(serviceGroup));
        when(serviceGroup.getReferredName()).thenReturn(List.of("service"));
        when(firewallRule.getEndpoint1()).thenReturn(firewallRuleEndpointType1);
        when(firewallRule.getEndpoint2()).thenReturn(firewallRuleEndpointType2);
        when(firewallRuleEndpointType1.getTags()).thenReturn(List.of("tag"));
        when(firewallRuleEndpointType2.getTags()).thenReturn(null);
        when(firewallRuleEndpointType2.getAddressGroup()).thenReturn("address:group");
        when(firewallRule.getMatchTags()).thenReturn(firewallRuleMatchTagsType);

        assertNotNull(tungstenService.listTungstenFirewallRule(1L, "948f421c-edde-4518-a391-09299cc25dc2",
                "8b4637b6-5629-46de-8fb2-d0b0502bfa85"));
    }

    @Test
    public void listTungstenAddressGroupTest() {
        TungstenAnswer listTungstenAddressGroupAnswer = MockTungstenAnswerFactory.get(true);
        AddressGroup addressGroup = mock(AddressGroup.class);
        SubnetListType subnetListType = mock(SubnetListType.class);
        SubnetType subnetType = mock(SubnetType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenAddressGroupCommand.class), anyLong())).thenReturn(listTungstenAddressGroupAnswer);
        doReturn(List.of(addressGroup)).when(listTungstenAddressGroupAnswer).getApiObjectBaseList();
        when(addressGroup.getPrefix()).thenReturn(subnetListType);
        when(subnetListType.getSubnet()).thenReturn(
                List.of(subnetType));

        assertNotNull(tungstenService.listTungstenAddressGroup(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void listTungstenServiceGroupTest() {
        TungstenAnswer listTungstenServiceGroupAnswer = MockTungstenAnswerFactory.get(true);
        ServiceGroup serviceGroup = mock(ServiceGroup.class);
        FirewallServiceGroupType firewallServiceGroupType = mock(FirewallServiceGroupType.class);
        FirewallServiceType firewallServiceType = mock(FirewallServiceType.class);
        PortType portType = mock(PortType.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenServiceGroupCommand.class), anyLong())).thenReturn(listTungstenServiceGroupAnswer);
        doReturn(List.of(serviceGroup)).when(listTungstenServiceGroupAnswer).getApiObjectBaseList();
        when(serviceGroup.getFirewallServiceList()).thenReturn(firewallServiceGroupType);
        when(firewallServiceGroupType.getFirewallService()).thenReturn(List.of(firewallServiceType));
        when(firewallServiceType.getDstPorts()).thenReturn(portType);

        assertNotNull(tungstenService.listTungstenServiceGroup(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void deleteTungstenApplicationPolicySetTest() {
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenApplicationPolicySetCommand.class), anyLong())).thenReturn(tungstenAnswer);

        assertTrue(tungstenService.deleteTungstenApplicationPolicySet(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void deleteTungstenFirewallPolicyTest() {
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenFirewallPolicyCommand.class), anyLong())).thenReturn(tungstenAnswer);

        assertTrue(tungstenService.deleteTungstenFirewallPolicy(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void deleteTungstenFirewallRuleTest() {
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenFirewallRuleCommand.class), anyLong())).thenReturn(tungstenAnswer);

        assertTrue(tungstenService.deleteTungstenFirewallRule(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void deleteTungstenServiceGroupTest() {
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenServiceGroupCommand.class), anyLong())).thenReturn(tungstenAnswer);

        assertTrue(tungstenService.deleteTungstenServiceGroup(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void deleteTungstenAddressGroupTest() {
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenAddressGroupCommand.class), anyLong())).thenReturn(tungstenAnswer);

        assertTrue(tungstenService.deleteTungstenAddressGroup(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void createSharedNetworkTest() {
        Network network = mock(Network.class);
        Vlan vlan = mock(Vlan.class);
        AccountVO accountVO = mock(AccountVO.class);
        TungstenAnswer createTungstenSharedNetworkAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer addNetworkSubnetAnswer = MockTungstenAnswerFactory.get(true);
        TungstenAnswer getTungstenNetworkDnsAnswer = MockTungstenAnswerFactory.get(true);
        NetworkDetailVO networkDetailVO = mock(NetworkDetailVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        HostVO hostVO = mock(HostVO.class);
        Answer answer = mock(Answer.class);
        IPAddressVO ipAddressVO = mock(IPAddressVO.class);
        ApiObjectBase apiObjectBase = mock(ApiObjectBase.class);

        when(accountDao.findById(anyLong())).thenReturn(accountVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenNetworkCommand.class), anyLong())).thenReturn(createTungstenSharedNetworkAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenNetworkSubnetCommand.class), anyLong())).thenReturn(addNetworkSubnetAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenNetworkDnsCommand.class), anyLong())).thenReturn(getTungstenNetworkDnsAnswer);
        when(network.getMode()).thenReturn(Networks.Mode.Dhcp);
        when(network.getCidr()).thenReturn("192.168.100.0/24");
        when(vlan.getIpRange()).thenReturn("192.168.100.100-192.168.100.200");
        when(vlan.getVlanGateway()).thenReturn("192.168.100.1");
        when(vlan.getIp6Gateway()).thenReturn("fd00::1");
        when(networkDetailsDao.persist(any(NetworkDetailVO.class))).thenReturn(networkDetailVO);
        when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProviderVO);
        when(hostDao.findByPublicIp(anyString())).thenReturn(hostVO);
        when(agentMgr.easySend(anyLong(), any(SetupTungstenVRouterCommand.class))).thenReturn(answer);
        when(answer.getResult()).thenReturn(true);
        when(vlan.getIp6Cidr()).thenReturn("fd00::1/64");
        when(vlan.getIp6Range()).thenReturn("fd00::100-fd00::200");
        when(getTungstenNetworkDnsAnswer.getDetails()).thenReturn("192.168.1.150");
        when(createTungstenSharedNetworkAnswer.getApiObjectBase()).thenReturn(apiObjectBase);
        when(apiObjectBase.getQualifiedName()).thenReturn(List.of("network"));
        when(tungstenProviderVO.getGateway()).thenReturn("192.168.100.1");

        assertTrue(tungstenService.createSharedNetwork(network, vlan));
    }

    @Test
    public void addTungstenVmSecurityGroupTest() {
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);
        SecurityGroupVO securityGroupVO = mock(SecurityGroupVO.class);
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer getTungstenSecurityGroupAnswer = mock(TungstenAnswer.class);
        TungstenAnswer addTungstenVmToSecurityGroupAnswer = MockTungstenAnswerFactory.get(true);
        net.juniper.tungsten.api.types.SecurityGroup securityGroup = mock(net.juniper.tungsten.api.types.SecurityGroup.class);
        NicVO nicVO = mock(NicVO.class);
        SecurityGroupRuleVO securityGroupRuleVO = mock(SecurityGroupRuleVO.class);
        TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = mock(TungstenSecurityGroupRuleVO.class);
        TungstenAnswer addTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(dataCenterVO.isSecurityGroupEnabled()).thenReturn(true);
        when(securityGroupManager.getSecurityGroupsForVm(anyLong())).thenReturn(List.of(securityGroupVO));
        when(tungstenProviderDao.findAll()).thenReturn(List.of(tungstenProviderVO));
        when(tungstenFabricUtils.sendTungstenCommand(any(GetTungstenSecurityGroupCommand.class), anyLong())).thenReturn(getTungstenSecurityGroupAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenVmToSecurityGroupCommand.class), anyLong())).thenReturn(addTungstenVmToSecurityGroupAnswer);
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenSecurityGroupRuleCommand.class), anyLong())).thenReturn(addTungstenSecurityGroupRuleAnswer);
        when(getTungstenSecurityGroupAnswer.getApiObjectBase()).thenReturn(securityGroup);
        when(nicDao.findDefaultNicForVM(anyLong())).thenReturn(nicVO);
        when(nicVO.getBroadcastUri()).thenReturn(Networks.BroadcastDomainType.TUNGSTEN.toUri("tf"));
        when(securityGroupRuleDao.listByAllowedSecurityGroupId(anyLong())).thenReturn(List.of(securityGroupRuleVO));
        when(nicVO.getIPv4Address()).thenReturn("192.168.100.100");
        when(nicVO.getIPv6Address()).thenReturn("fd00::1");
        when(nicVO.getSecondaryIp()).thenReturn(true);
        when(nicSecIpDao.getSecondaryIpAddressesForNic(anyLong())).thenReturn(List.of("192.168.100.200"));
        when(securityGroupRuleVO.getProtocol()).thenReturn(NetUtils.ALL_PROTO);
        when(tungstenSecurityGroupRuleDao.persist(any(TungstenSecurityGroupRuleVO.class))).thenReturn(tungstenSecurityGroupRuleVO);

        assertTrue(tungstenService.addTungstenVmSecurityGroup(vmInstanceVO));
    }

    @Test
    public void removeTungstenVmSecurityGroupTest() {
        VMInstanceVO vmInstanceVO = mock(VMInstanceVO.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);
        SecurityGroupVO securityGroupVO = mock(SecurityGroupVO.class);
        TungstenAnswer removeTungstenVmFromSecurityGroupAnswer = MockTungstenAnswerFactory.get(true);
        NicVO nicVO = mock(NicVO.class);
        SecurityGroupRuleVO securityGroupRuleVO = mock(SecurityGroupRuleVO.class);
        TungstenAnswer removeTungstenSecurityGroupRuleAnswer = MockTungstenAnswerFactory.get(true);
        TungstenSecurityGroupRuleVO tungstenSecurityGroupRuleVO = mock(TungstenSecurityGroupRuleVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(dataCenterVO.isSecurityGroupEnabled()).thenReturn(true);

        assertTrue(tungstenService.removeTungstenVmSecurityGroup(vmInstanceVO));
    }

    @Test
    public void createRoutingLogicalRouterTest() {
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);
        TungstenLogicalRouter tungstenLogicalRouter = mock(TungstenLogicalRouter.class);
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(CreateTungstenRoutingLogicalRouterCommand.class), anyLong())).thenReturn(tungstenAnswer);
        when(tungstenAnswer.getTungstenModel()).thenReturn(tungstenLogicalRouter);
        when(tungstenLogicalRouter.getLogicalRouter()).thenReturn(logicalRouter);
        when(tungstenLogicalRouter.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));

        assertNotNull(tungstenService.createRoutingLogicalRouter(1L, "default-domain:default-project", "testLR"));
    }

    @Test
    public void addNetworkGatewayToLogicalRouterTest() {
        NetworkVO networkVO = mock(NetworkVO.class);
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);
        TungstenLogicalRouter tungstenLogicalRouter = mock(TungstenLogicalRouter.class);
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(networkDao.findByUuid(anyString())).thenReturn(networkVO);
        when(ipAddressManager.acquireLastGuestIpAddress(any(Network.class))).thenReturn("192.168.100.100");
        when(tungstenFabricUtils.sendTungstenCommand(any(AddTungstenNetworkGatewayToLogicalRouterCommand.class), anyLong())).thenReturn(tungstenAnswer);
        when(tungstenAnswer.getTungstenModel()).thenReturn(tungstenLogicalRouter);
        when(tungstenLogicalRouter.getLogicalRouter()).thenReturn(logicalRouter);
        when(tungstenLogicalRouter.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));

        assertNotNull(tungstenService.addNetworkGatewayToLogicalRouter(1L, "948f421c-edde-4518-a391-09299cc25dc2", "8b4637b6-5629-46de-8fb2-d0b0502bfa85"));
    }

    @Test
    public void listRoutingLogicalRouterTest() {
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);
        TungstenLogicalRouter tungstenLogicalRouter = mock(TungstenLogicalRouter.class);
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(ListTungstenRoutingLogicalRouterCommand.class), anyLong())).thenReturn(tungstenAnswer);
        when(tungstenAnswer.getTungstenModelList()).thenReturn(List.of(tungstenLogicalRouter));
        when(tungstenLogicalRouter.getLogicalRouter()).thenReturn(logicalRouter);
        when(tungstenLogicalRouter.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));

        assertNotNull(tungstenService.listRoutingLogicalRouter(1L, null, "948f421c-edde-4518-a391-09299cc25dc2"));
    }

    @Test
    public void removeNetworkGatewayFromLogicalRouterTest() {
        NetworkVO networkVO = mock(NetworkVO.class);
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);
        TungstenGuestNetworkIpAddressVO tungstenGuestNetworkIpAddressVO = mock(TungstenGuestNetworkIpAddressVO.class);
        TungstenLogicalRouter tungstenLogicalRouter = mock(TungstenLogicalRouter.class);
        LogicalRouter logicalRouter = mock(LogicalRouter.class);
        VirtualNetwork virtualNetwork = mock(VirtualNetwork.class);
        DataCenterVO dataCenterVO = mock(DataCenterVO.class);

        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(networkDao.findByUuid(anyString())).thenReturn(networkVO);
        when(tungstenGuestNetworkIpAddressDao.findByNetworkAndLogicalRouter(anyLong(), anyString())).thenReturn(tungstenGuestNetworkIpAddressVO);
        when(tungstenFabricUtils.sendTungstenCommand(any(RemoveTungstenNetworkGatewayFromLogicalRouterCommand.class), anyLong())).thenReturn(tungstenAnswer);
        when(tungstenAnswer.getTungstenModel()).thenReturn(tungstenLogicalRouter);
        when(tungstenLogicalRouter.getLogicalRouter()).thenReturn(logicalRouter);
        when(tungstenLogicalRouter.getVirtualNetworkList()).thenReturn(List.of(virtualNetwork));

        assertNotNull(tungstenService.removeNetworkGatewayFromLogicalRouter(1L, "948f421c-edde-4518-a391-09299cc25dc2", "8b4637b6-5629-46de-8fb2-d0b0502bfa85"));
    }

    @Test
    public void deleteLogicalRouterTest() {
        TungstenAnswer tungstenAnswer = MockTungstenAnswerFactory.get(true);

        when(tungstenFabricUtils.sendTungstenCommand(any(DeleteTungstenRoutingLogicalRouterCommand.class), anyLong())).thenReturn(tungstenAnswer);

        assertTrue(tungstenService.deleteLogicalRouter(1L, "948f421c-edde-4518-a391-09299cc25dc2"));
    }
}
