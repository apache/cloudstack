//
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
//

package com.cloud.network.element;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.CloudException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.Vpc;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.vm.ReservationContext;
import com.google.common.collect.Lists;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import javax.naming.ConfigurationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.cloud.network.manager.NuageVspManager.NuageVspIsolatedNetworkDomainTemplateName;
import static com.cloud.network.manager.NuageVspManager.NuageVspSharedNetworkDomainTemplateName;
import static com.cloud.network.manager.NuageVspManager.NuageVspVpcDomainTemplateName;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NuageVspElementTest {

    private static final long NETWORK_ID = 42L;
    NuageVspElement element = new NuageVspElement();
    NetworkOrchestrationService networkManager = mock(NetworkOrchestrationService.class);
    NetworkModel networkModel = mock(NetworkModel.class);
    NetworkServiceMapDao ntwkSrvcDao = mock(NetworkServiceMapDao.class);
    AgentManager agentManager = mock(AgentManager.class);
    HostDao hostDao = mock(HostDao.class);
    NuageVspDao nuageVspDao = mock(NuageVspDao.class);
    DomainDao domainDao = mock(DomainDao.class);
    NetworkOfferingDao ntwkOfferingDao = mock(NetworkOfferingDao.class);
    NetworkOfferingServiceMapDao ntwkOfferingSrvcDao = mock(NetworkOfferingServiceMapDao.class);
    ConfigurationDao configDao = mock(ConfigurationDao.class);
    NuageVspManager nuageVspManager = mock(NuageVspManager.class);
    FirewallRulesDao firewallRulesDao = mock(FirewallRulesDao.class);
    IPAddressDao ipAddressDao = mock(IPAddressDao.class);
    PhysicalNetworkDao physNetDao = mock(PhysicalNetworkDao.class);

    org.mockito.stubbing.Answer<Object> genericAnswer = new org.mockito.stubbing.Answer<Object>() {
        public Object answer(InvocationOnMock invocation) {
            return null;
        }
    };

    @Before
    public void setUp() throws ConfigurationException {
        element._resourceMgr = mock(ResourceManager.class);
        element._ntwkSrvcDao = ntwkSrvcDao;
        element._networkModel = networkModel;
        element._agentMgr = agentManager;
        element._hostDao = hostDao;
        element._nuageVspDao = nuageVspDao;
        element._ntwkOfferingSrvcDao = ntwkOfferingSrvcDao;
        element._domainDao = domainDao;
        element._ntwkOfferingDao = ntwkOfferingDao;
        element._configDao = configDao;
        element._nuageVspManager = nuageVspManager;
        element._firewallRulesDao = firewallRulesDao;
        element._ipAddressDao = ipAddressDao;
        element._physicalNetworkDao = physNetDao;

        // Standard responses
        when(networkModel.isProviderForNetwork(Provider.NuageVsp, NETWORK_ID)).thenReturn(true);
        when(configDao.getValue(NuageVspIsolatedNetworkDomainTemplateName.key())).thenReturn("IsolatedDomainTemplate");
        when(configDao.getValue(NuageVspVpcDomainTemplateName.key())).thenReturn("VpcDomainTemplate");
        when(configDao.getValue(NuageVspSharedNetworkDomainTemplateName.key())).thenReturn("SharedDomainTemplate");

        element.configure("NuageVspTestElement", Collections.<String, Object> emptyMap());
    }

    @Test
    public void testCanHandle() {
        final Network net = mock(Network.class);
        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vsp);
        when(net.getId()).thenReturn(NETWORK_ID);

        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NuageVsp)).thenReturn(true);
        // Golden path
        assertTrue(element.canHandle(net, Service.Connectivity));

        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vlan);
        // Only broadcastdomaintype Vsp is supported
        assertFalse(element.canHandle(net, Service.Connectivity));

        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vsp);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NuageVsp)).thenReturn(false);
        // No NuageVsp provider in the network
        assertFalse(element.canHandle(net, Service.Connectivity));

        when(networkModel.isProviderForNetwork(Provider.NuageVsp, NETWORK_ID)).thenReturn(false);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NuageVsp)).thenReturn(true);
        // NusageVsp provider does not provide Connectivity for this network
        assertFalse(element.canHandle(net, Service.Connectivity));

        when(networkModel.isProviderForNetwork(Provider.NuageVsp, NETWORK_ID)).thenReturn(true);
        // Only service Connectivity is supported
        assertFalse(element.canHandle(net, Service.Dhcp));

    }

    @Test
    public void testImplement() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, URISyntaxException {
        final Network network = mock(Network.class);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vsp);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getVpcId()).thenReturn(null);
        when(network.getBroadcastUri()).thenReturn(new URI(""));
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDomainId()).thenReturn(NETWORK_ID);
        when(networkModel.isProviderForNetwork(Provider.NuageVsp, NETWORK_ID)).thenReturn(true);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NuageVsp)).thenReturn(true);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        DeployDestination deployDest = mock(DeployDestination.class);

        final DomainVO dom = mock(DomainVO.class);
        when(dom.getName()).thenReturn("domain");
        when(domainDao.findById(NETWORK_ID)).thenReturn(dom);
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        when(firewallRulesDao.listByNetworkPurposeTrafficType(NETWORK_ID, FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Ingress)).thenReturn(new ArrayList<FirewallRuleVO>());
        when(firewallRulesDao.listByNetworkPurposeTrafficType(NETWORK_ID, FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Egress)).thenReturn(new ArrayList<FirewallRuleVO>());
        when(ipAddressDao.listStaticNatPublicIps(NETWORK_ID)).thenReturn(new ArrayList<IPAddressVO>());
        when(nuageVspManager.getDnsDetails(network)).thenReturn(new ArrayList<String>());

        assertTrue(element.implement(network, offering, deployDest, context));
    }

    @Test
    public void testVerifyServiceCombination() {
        Set<Service> services = new HashSet<Service>();
        services.add(Service.Dhcp);
        services.add(Service.StaticNat);
        services.add(Service.SourceNat);
        services.add(Service.Connectivity);
        services.add(Service.Firewall);
        assertTrue(element.verifyServicesCombination(services));

        services = new HashSet<Service>();
        services.add(Service.Dhcp);
        services.add(Service.StaticNat);
        services.add(Service.Connectivity);
        services.add(Service.Firewall);
        assertFalse(element.verifyServicesCombination(services));
    }

    @Test
    public void testApplyStaticNats() throws CloudException {
        final Network network = mock(Network.class);
        when(network.getUuid()).thenReturn("aaaaaa");
        when(network.getVpcId()).thenReturn(null);
        when(network.getNetworkOfferingId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDomainId()).thenReturn(NETWORK_ID);

        final DomainVO domVo = mock(DomainVO.class);
        when(domainDao.findById(41l)).thenReturn(domVo);

        final NetworkOfferingVO ntwkoffer = mock(NetworkOfferingVO.class);
        when(ntwkoffer.getId()).thenReturn(NETWORK_ID);
        when(ntwkOfferingDao.findById(NETWORK_ID)).thenReturn(ntwkoffer);
        when(element.isL3Network(NETWORK_ID)).thenReturn(true);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        when(domainDao.findById(NETWORK_ID)).thenReturn(mock(DomainVO.class));
        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);
        assertTrue(element.applyStaticNats(network, new ArrayList<StaticNat>()));
    }

    @Test
    public void testApplyFWRules() throws Exception {
        final Network network = mock(Network.class);
        when(network.getUuid()).thenReturn("aaaaaa");
        when(network.getVpcId()).thenReturn(null);
        when(network.getNetworkOfferingId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDomainId()).thenReturn(NETWORK_ID);

        final NetworkOfferingVO ntwkoffer = mock(NetworkOfferingVO.class);
        when(ntwkoffer.getId()).thenReturn(NETWORK_ID);
        when(ntwkoffer.getEgressDefaultPolicy()).thenReturn(true);
        when(ntwkOfferingDao.findById(NETWORK_ID)).thenReturn(ntwkoffer);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        when(domainDao.findById(NETWORK_ID)).thenReturn(mock(DomainVO.class));

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);
        assertTrue(element.applyFWRules(network, new ArrayList<FirewallRule>()));
    }

    @Test
    public void testApplyNetworkACL() throws Exception {
        final Network network = mock(Network.class);
        when(network.getUuid()).thenReturn("aaaaaa");
        when(network.getVpcId()).thenReturn(null);
        when(network.getNetworkOfferingId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getDomainId()).thenReturn(NETWORK_ID);

        final NetworkOfferingVO ntwkoffer = mock(NetworkOfferingVO.class);
        when(ntwkoffer.getId()).thenReturn(NETWORK_ID);
        when(ntwkoffer.getEgressDefaultPolicy()).thenReturn(true);
        when(ntwkOfferingDao.findById(NETWORK_ID)).thenReturn(ntwkoffer);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        when(domainDao.findById(NETWORK_ID)).thenReturn(mock(DomainVO.class));
        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);
        assertTrue(element.applyNetworkACLs(network, new ArrayList<NetworkACLItem>()));
    }

    @Test
    public void testShutdownVpc() throws Exception {
        final Vpc vpc = mock(Vpc.class);
        when(vpc.getUuid()).thenReturn("aaaaaa");
        when(vpc.getState()).thenReturn(Vpc.State.Inactive);
        when(vpc.getDomainId()).thenReturn(NETWORK_ID);
        when(vpc.getZoneId()).thenReturn(NETWORK_ID);

        final DomainVO dom = mock(DomainVO.class);
        when(dom.getName()).thenReturn("domain");
        when(domainDao.findById(NETWORK_ID)).thenReturn(dom);
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        PhysicalNetworkVO physNet = mock(PhysicalNetworkVO.class);
        when(physNet.getIsolationMethods()).thenReturn(Lists.newArrayList(PhysicalNetwork.IsolationMethod.VSP.name()));
        when(physNet.getId()).thenReturn(NETWORK_ID);
        when(physNetDao.listByZone(NETWORK_ID)).thenReturn(Lists.newArrayList(physNet));

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);
        assertTrue(element.shutdownVpc(vpc, context));
    }
}