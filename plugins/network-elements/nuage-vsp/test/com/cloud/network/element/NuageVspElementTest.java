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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.cloud.tags.dao.ResourceTagDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;

import com.cloud.NuageTest;
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
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.NuageVspDeviceVO;
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
import com.cloud.util.NuageVspEntityBuilder;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.dao.DomainRouterDao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NuageVspElementTest extends NuageTest {

    @InjectMocks
    private NuageVspElement _nuageVspElement = new NuageVspElement();

    @Mock private NetworkServiceMapDao _networkServiceMapDao;
    @Mock private AgentManager _agentManager;
    @Mock private HostDao _hostDao;
    @Mock private NuageVspDao _nuageVspDao;
    @Mock private DomainDao _domainDao;
    @Mock private NetworkOfferingDao _networkOfferingDao;
    @Mock private NetworkOfferingServiceMapDao _networkOfferingServiceMapDao;
    @Mock private NuageVspManager _nuageVspManager;
    @Mock private FirewallRulesDao _firewallRulesDao;
    @Mock private IPAddressDao _ipAddressDao;
    @Mock private PhysicalNetworkDao _physicalNetworkDao;
    @Mock private NuageVspEntityBuilder _nuageVspEntityBuilder;
    @Mock private VpcDetailsDao _vpcDetailsDao;
    @Mock private DomainRouterDao _domainRouterDao;
    @Mock private ResourceManager _resourceManager;
    @Mock private ResourceTagDao _resourceTagDao;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        _nuageVspElement._nuageVspEntityBuilder = _nuageVspEntityBuilder;
        _nuageVspElement._vpcDetailsDao = _vpcDetailsDao;
        _nuageVspElement._routerDao = _domainRouterDao;

        when(_networkServiceMapDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NuageVsp)).thenReturn(true);
        when(_networkServiceMapDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.SourceNat, Provider.NuageVsp)).thenReturn(true);

        _nuageVspElement.configure("NuageVspTestElement", Collections.<String, Object>emptyMap());
    }

    @Test
    public void testCanHandle() {
        final Network net = mock(Network.class);
        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vsp);
        when(net.getId()).thenReturn(NETWORK_ID);
        when(net.getNetworkOfferingId()).thenReturn(NETWORK_ID);

        final NetworkOfferingVO ntwkoffer = mock(NetworkOfferingVO.class);
        when(ntwkoffer.getId()).thenReturn(NETWORK_ID);
        when(ntwkoffer.getIsPersistent()).thenReturn(true);
        when(_networkOfferingDao.findById(NETWORK_ID)).thenReturn(ntwkoffer);

        // Golden path
        assertTrue(_nuageVspElement.canHandle(net, Service.Connectivity));

        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vlan);
        // Only broadcastdomaintype Vsp is supported
        assertFalse(_nuageVspElement.canHandle(net, Service.Connectivity));

        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vsp);
        when(_networkServiceMapDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NuageVsp)).thenReturn(false);
        // No NuageVsp provider in the network
        assertFalse(_nuageVspElement.canHandle(net, Service.Connectivity));

        when(_networkModel.isProviderForNetwork(Provider.NuageVsp, NETWORK_ID)).thenReturn(false);
        when(_networkServiceMapDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NuageVsp)).thenReturn(true);
        // NusageVsp provider does not provide Connectivity for this network
        assertFalse(_nuageVspElement.canHandle(net, Service.Connectivity));

        when(_networkModel.isProviderForNetwork(Provider.NuageVsp, NETWORK_ID)).thenReturn(true);
        // Only service Connectivity is supported
        assertFalse(_nuageVspElement.canHandle(net, Service.Dhcp));

        // Can't handle network offerings with specify vlan = true
        when(ntwkoffer.getSpecifyVlan()).thenReturn(true);
        assertFalse(_nuageVspElement.canHandle(net, Service.Connectivity));
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
        when(network.getDataCenterId()).thenReturn(NETWORK_ID);
        when(_networkModel.isProviderForNetwork(Provider.NuageVsp, NETWORK_ID)).thenReturn(true);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        DeployDestination deployDest = mock(DeployDestination.class);

        final DomainVO dom = mock(DomainVO.class);
        when(dom.getName()).thenReturn("domain");
        when(_domainDao.findById(NETWORK_ID)).thenReturn(dom);
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[]{nuageVspDevice}));
        when(_hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(_nuageVspManager.getNuageVspHost(NETWORK_ID)).thenReturn(host);

        when(_firewallRulesDao.listByNetworkPurposeTrafficType(NETWORK_ID, FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Ingress)).thenReturn(new ArrayList<FirewallRuleVO>());
        when(_firewallRulesDao.listByNetworkPurposeTrafficType(NETWORK_ID, FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Egress)).thenReturn(new ArrayList<FirewallRuleVO>());
        when(_ipAddressDao.listStaticNatPublicIps(NETWORK_ID)).thenReturn(new ArrayList<IPAddressVO>());
        when(_nuageVspManager.getDnsDetails(network.getDataCenterId())).thenReturn(new ArrayList<String>());

        assertTrue(_nuageVspElement.implement(network, offering, deployDest, context));
    }

    @Test
    public void testVerifyServiceCombination() {

        Set<Service> services = Sets.newHashSet(
            Service.Dhcp,
            Service.StaticNat,
            Service.SourceNat,
            Service.Connectivity,
            Service.Firewall);
        assertTrue(_nuageVspElement.verifyServicesCombination(services));

        services = Sets.newHashSet(
                Service.Dhcp,
                Service.StaticNat,
                Service.Connectivity,
                Service.Firewall);
        assertTrue(_nuageVspElement.verifyServicesCombination(services));
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
        when(_domainDao.findById(41l)).thenReturn(domVo);

        final NetworkOfferingVO ntwkoffer = mock(NetworkOfferingVO.class);
        when(ntwkoffer.getId()).thenReturn(NETWORK_ID);
        when(_networkOfferingDao.findById(NETWORK_ID)).thenReturn(ntwkoffer);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[]{nuageVspDevice}));
        when(_hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(_nuageVspManager.getNuageVspHost(NETWORK_ID)).thenReturn(host);

        when(_domainDao.findById(NETWORK_ID)).thenReturn(mock(DomainVO.class));
        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(_agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);
        assertTrue(_nuageVspElement.applyStaticNats(network, new ArrayList<StaticNat>()));
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
        when(_networkOfferingDao.findById(NETWORK_ID)).thenReturn(ntwkoffer);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[]{nuageVspDevice}));
        when(_hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(_nuageVspManager.getNuageVspHost(NETWORK_ID)).thenReturn(host);

        when(_domainDao.findById(NETWORK_ID)).thenReturn(mock(DomainVO.class));

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(_agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);
        assertTrue(_nuageVspElement.applyFWRules(network, new ArrayList<FirewallRule>()));
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
        when(_networkOfferingDao.findById(NETWORK_ID)).thenReturn(ntwkoffer);

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NuageVspDeviceVO[]{nuageVspDevice}));
        when(_hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(_nuageVspManager.getNuageVspHost(NETWORK_ID)).thenReturn(host);

        when(_domainDao.findById(NETWORK_ID)).thenReturn(mock(DomainVO.class));
        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(_agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);
        assertTrue(_nuageVspElement.applyNetworkACLs(network, new ArrayList<NetworkACLItem>()));
    }

    @Test
    public void testShutdownVpc() throws Exception {
        final Vpc vpc = mock(Vpc.class);
        when(vpc.getUuid()).thenReturn("aaaaaa");
        when(vpc.getState()).thenReturn(Vpc.State.Inactive);
        when(vpc.getDomainId()).thenReturn(NETWORK_ID);
        when(vpc.getZoneId()).thenReturn(NETWORK_ID);
        when(vpc.getId()).thenReturn(NETWORK_ID);

        final DomainVO dom = mock(DomainVO.class);
        when(dom.getName()).thenReturn("domain");
        when(_domainDao.findById(NETWORK_ID)).thenReturn(dom);
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        PhysicalNetworkVO physNet = mock(PhysicalNetworkVO.class);
        when(physNet.getIsolationMethods()).thenReturn(Lists.newArrayList("VSP"));
        when(physNet.getId()).thenReturn(NETWORK_ID);
        when(_physicalNetworkDao.listByZone(NETWORK_ID)).thenReturn(Lists.newArrayList(physNet));

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(_nuageVspDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Lists.newArrayList(nuageVspDevice));
        when(_hostDao.findById(NETWORK_ID)).thenReturn(host);
        when(_nuageVspManager.getNuageVspHost(NETWORK_ID)).thenReturn(host);

        DomainRouterVO domainRouter = mock(DomainRouterVO.class);
        when(domainRouter.getUuid()).thenReturn("aaaaaa");
        when(_domainRouterDao.listByVpcId(NETWORK_ID)).thenReturn(Lists.newArrayList(domainRouter));

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(_agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);
        assertTrue(_nuageVspElement.shutdownVpc(vpc, context));
    }
}