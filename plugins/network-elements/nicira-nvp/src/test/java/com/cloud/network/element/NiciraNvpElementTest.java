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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigureSharedNetworkUuidAnswer;
import com.cloud.agent.api.ConfigureSharedNetworkVlanIdAnswer;
import com.cloud.agent.api.CreateLogicalRouterAnswer;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.IpAddressManager;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.NiciraNvpRouterMappingVO;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NiciraNvpDao;
import com.cloud.network.dao.NiciraNvpRouterMappingDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.ReservationContext;

public class NiciraNvpElementTest {

    private static final long NETWORK_ID = 42L;
    private static final long NICIRA_NVP_HOST_ID = 9L;
    private static final String NETWORK_CIDR = "10.0.0.0/24";
    private static final String NETWORK_GATEWAY = "10.0.0.1";
    NiciraNvpElement element = new NiciraNvpElement();
    NetworkOrchestrationService networkManager = mock(NetworkOrchestrationService.class);
    NetworkModel networkModel = mock(NetworkModel.class);
    NetworkServiceMapDao ntwkSrvcDao = mock(NetworkServiceMapDao.class);
    AgentManager agentManager = mock(AgentManager.class);
    HostDao hostDao = mock(HostDao.class);
    NiciraNvpDao niciraNvpDao = mock(NiciraNvpDao.class);
    NiciraNvpRouterMappingDao niciraNvpRouterMappingDao = mock(NiciraNvpRouterMappingDao.class);
    VlanDao vlanDao = mock(VlanDao.class);
    IpAddressManager ipAddressManager = mock(IpAddressManager.class);

    @Before
    public void setUp() throws ConfigurationException {
        element.resourceMgr = mock(ResourceManager.class);
        element.networkManager = networkManager;
        element.ntwkSrvcDao = ntwkSrvcDao;
        element.networkModel = networkModel;
        element.agentMgr = agentManager;
        element.hostDao = hostDao;
        element.niciraNvpDao = niciraNvpDao;
        element.niciraNvpRouterMappingDao = niciraNvpRouterMappingDao;
        element.vlanDao = vlanDao;
        element.ipAddrMgr = ipAddressManager;

        // Standard responses
        when(networkModel.isProviderForNetwork(Provider.NiciraNvp, NETWORK_ID)).thenReturn(true);

        element.configure("NiciraNvpTestElement", Collections.<String, Object> emptyMap());
    }

    @Test
    public void canHandleTest() {
        final Network net = mock(Network.class);
        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Lswitch);
        when(net.getId()).thenReturn(NETWORK_ID);

        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NiciraNvp)).thenReturn(true);
        // Golden path
        assertTrue(element.canHandle(net, Service.Connectivity));

        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vlan);
        // Only broadcastdomaintype lswitch is supported
        assertFalse(element.canHandle(net, Service.Connectivity));

        when(net.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Lswitch);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NiciraNvp)).thenReturn(false);
        // No nvp provider in the network
        assertFalse(element.canHandle(net, Service.Connectivity));

        when(networkModel.isProviderForNetwork(Provider.NiciraNvp, NETWORK_ID)).thenReturn(false);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NiciraNvp)).thenReturn(true);
        // NVP provider does not provide Connectivity for this network
        assertFalse(element.canHandle(net, Service.Connectivity));

        when(networkModel.isProviderForNetwork(Provider.NiciraNvp, NETWORK_ID)).thenReturn(true);
        // Only service Connectivity is supported
        assertFalse(element.canHandle(net, Service.Dhcp));

    }

    @Test
    public void implementIsolatedNetworkTest() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException, URISyntaxException {
        final Network network = mock(Network.class);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Lswitch);
        when(network.getBroadcastUri()).thenReturn(new URI("lswitch:aaaaa"));
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getGuestType()).thenReturn(GuestType.Isolated);

        when(networkModel.isProviderForNetwork(Provider.NiciraNvp, NETWORK_ID)).thenReturn(true);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NiciraNvp)).thenReturn(true);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(niciraNvpDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);
        when(device.getHostId()).thenReturn(NICIRA_NVP_HOST_ID);

        HostVO niciraNvpHost = mock(HostVO.class);
        when(niciraNvpHost.getId()).thenReturn(NICIRA_NVP_HOST_ID);
        when(hostDao.findById(NICIRA_NVP_HOST_ID)).thenReturn(niciraNvpHost);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final DeployDestination dest = mock(DeployDestination.class);

        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        //ISOLATED NETWORK
        when(networkModel.isProviderSupportServiceInNetwork(NETWORK_ID, Service.SourceNat, Provider.NiciraNvp)).thenReturn(true);

        PublicIp sourceNatIp = mock(PublicIp.class);
        Ip ip = mock(Ip.class);
        when(ip.addr()).thenReturn("10.0.0.0");
        when(sourceNatIp.getAddress()).thenReturn(ip);
        when(sourceNatIp.getVlanNetmask()).thenReturn("255.255.255.0");
        when(sourceNatIp.getVlanTag()).thenReturn("111");

        when(ipAddressManager.assignSourceNatIpAddressToGuestNetwork(acc, network)).thenReturn(sourceNatIp);
        when(network.getGateway()).thenReturn(NETWORK_GATEWAY);
        when(network.getCidr()).thenReturn(NETWORK_CIDR);

        final CreateLogicalRouterAnswer answer = mock(CreateLogicalRouterAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NICIRA_NVP_HOST_ID), (Command)any())).thenReturn(answer);

        assertTrue(element.implement(network, offering, dest, context));
    }

    @Test
    public void applyIpTest() throws ResourceUnavailableException {
        final Network network = mock(Network.class);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Lswitch);
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);

        final List<PublicIpAddress> ipAddresses = new ArrayList<PublicIpAddress>();
        final PublicIpAddress pipReleased = mock(PublicIpAddress.class);
        final PublicIpAddress pipAllocated = mock(PublicIpAddress.class);
        final Ip ipReleased = new Ip("42.10.10.10");
        final Ip ipAllocated = new Ip("10.10.10.10");
        when(pipAllocated.getState()).thenReturn(IpAddress.State.Allocated);
        when(pipAllocated.getAddress()).thenReturn(ipAllocated);
        when(pipAllocated.getNetmask()).thenReturn("255.255.255.0");
        when(pipReleased.getState()).thenReturn(IpAddress.State.Releasing);
        when(pipReleased.getAddress()).thenReturn(ipReleased);
        when(pipReleased.getNetmask()).thenReturn("255.255.255.0");
        ipAddresses.add(pipAllocated);
        ipAddresses.add(pipReleased);

        final Set<Service> services = new HashSet<Service>();
        services.add(Service.SourceNat);
        services.add(Service.StaticNat);
        services.add(Service.PortForwarding);

        final List<NiciraNvpDeviceVO> deviceList = new ArrayList<NiciraNvpDeviceVO>();
        final NiciraNvpDeviceVO nndVO = mock(NiciraNvpDeviceVO.class);
        final NiciraNvpRouterMappingVO nnrmVO = mock(NiciraNvpRouterMappingVO.class);
        when(niciraNvpRouterMappingDao.findByNetworkId(NETWORK_ID)).thenReturn(nnrmVO);
        when(nnrmVO.getLogicalRouterUuid()).thenReturn("abcde");
        when(nndVO.getHostId()).thenReturn(NETWORK_ID);
        final HostVO hvo = mock(HostVO.class);
        when(hvo.getId()).thenReturn(NETWORK_ID);
        when(hvo.getDetail("l3gatewayserviceuuid")).thenReturn("abcde");
        when(hostDao.findById(NETWORK_ID)).thenReturn(hvo);
        deviceList.add(nndVO);
        when(niciraNvpDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(deviceList);

        final ConfigurePublicIpsOnLogicalRouterAnswer answer = mock(ConfigurePublicIpsOnLogicalRouterAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), any(ConfigurePublicIpsOnLogicalRouterCommand.class))).thenReturn(answer);

        assertTrue(element.applyIps(network, ipAddresses, services));

        verify(agentManager, atLeast(1)).easySend(eq(NETWORK_ID), argThat(new ArgumentMatcher<ConfigurePublicIpsOnLogicalRouterCommand>() {
            @Override
            public boolean matches(final ConfigurePublicIpsOnLogicalRouterCommand command) {
                return command.getPublicCidrs().size() == 1;
            }
        }));
    }

    @Test
    public void implementSharedNetworkUuidVlanIdTest() throws URISyntaxException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // SHARED NETWORKS CASE 1: LOGICAL ROUTER'S UUID AS VLAN ID
        final Network network = mock(Network.class);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Lswitch);
        when(network.getBroadcastUri()).thenReturn(new URI("lswitch:aaaaa"));
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getGuestType()).thenReturn(GuestType.Shared);

        when(networkModel.isProviderForNetwork(Provider.NiciraNvp, NETWORK_ID)).thenReturn(true);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NiciraNvp)).thenReturn(true);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(niciraNvpDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);
        when(device.getHostId()).thenReturn(NICIRA_NVP_HOST_ID);

        HostVO niciraNvpHost = mock(HostVO.class);
        when(niciraNvpHost.getId()).thenReturn(NICIRA_NVP_HOST_ID);
        when(hostDao.findById(NICIRA_NVP_HOST_ID)).thenReturn(niciraNvpHost);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Shared);

        final DeployDestination dest = mock(DeployDestination.class);

        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        //SHARED NETWORKS CASE 1
        when(niciraNvpRouterMappingDao.existsMappingForNetworkId(NETWORK_ID)).thenReturn(true);
        when(network.getCidr()).thenReturn(NETWORK_CIDR);
        when(network.getGateway()).thenReturn(NETWORK_GATEWAY);

        NiciraNvpRouterMappingVO mapping = mock(NiciraNvpRouterMappingVO.class);
        when(mapping.getLogicalRouterUuid()).thenReturn("xxxx-xxxx-xxxx");
        when(niciraNvpRouterMappingDao.findByNetworkId(NETWORK_ID)).thenReturn(mapping);

        final ConfigureSharedNetworkUuidAnswer answer = mock(ConfigureSharedNetworkUuidAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NICIRA_NVP_HOST_ID), (Command)any())).thenReturn(answer);

        assertTrue(element.implement(network, offering, dest, context));
    }

    @Test
    public void implementSharedNetworkNumericalVlanIdTest() throws URISyntaxException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // SHARED NETWORKS CASE 2: NUMERICAL VLAN ID
        final Network network = mock(Network.class);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Lswitch);
        when(network.getBroadcastUri()).thenReturn(new URI("lswitch:aaaaa"));
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getGuestType()).thenReturn(GuestType.Shared);

        when(networkModel.isProviderForNetwork(Provider.NiciraNvp, NETWORK_ID)).thenReturn(true);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NiciraNvp)).thenReturn(true);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(niciraNvpDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);
        when(device.getHostId()).thenReturn(NICIRA_NVP_HOST_ID);

        HostVO niciraNvpHost = mock(HostVO.class);
        when(niciraNvpHost.getId()).thenReturn(NICIRA_NVP_HOST_ID);
        when(hostDao.findById(NICIRA_NVP_HOST_ID)).thenReturn(niciraNvpHost);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Shared);

        final DeployDestination dest = mock(DeployDestination.class);

        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        //SHARED NETWORKS CASE 2
        when(niciraNvpRouterMappingDao.existsMappingForNetworkId(NETWORK_ID)).thenReturn(false);

        VlanVO vlanVO = mock(VlanVO.class);
        when(vlanVO.getVlanTag()).thenReturn("111");
        when(vlanDao.listVlansByNetworkId(NETWORK_ID)).thenReturn(Arrays.asList(new VlanVO[] {vlanVO}));

        when(niciraNvpHost.getDetail("l2gatewayserviceuuid")).thenReturn("bbbb-bbbb-bbbb");

        final ConfigureSharedNetworkVlanIdAnswer answer = mock(ConfigureSharedNetworkVlanIdAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NICIRA_NVP_HOST_ID), (Command)any())).thenReturn(answer);

        assertTrue(element.implement(network, offering, dest, context));
    }

    @Test(expected=CloudRuntimeException.class)
    public void implementSharedNetworkNumericalVlanIdWithoutL2GatewayService() throws URISyntaxException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException{
        final Network network = mock(Network.class);
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Lswitch);
        when(network.getBroadcastUri()).thenReturn(new URI("lswitch:aaaaa"));
        when(network.getId()).thenReturn(NETWORK_ID);
        when(network.getPhysicalNetworkId()).thenReturn(NETWORK_ID);
        when(network.getGuestType()).thenReturn(GuestType.Shared);

        when(networkModel.isProviderForNetwork(Provider.NiciraNvp, NETWORK_ID)).thenReturn(true);
        when(ntwkSrvcDao.canProviderSupportServiceInNetwork(NETWORK_ID, Service.Connectivity, Provider.NiciraNvp)).thenReturn(true);

        final NiciraNvpDeviceVO device = mock(NiciraNvpDeviceVO.class);
        when(niciraNvpDao.listByPhysicalNetwork(NETWORK_ID)).thenReturn(Arrays.asList(new NiciraNvpDeviceVO[] {device}));
        when(device.getId()).thenReturn(1L);
        when(device.getHostId()).thenReturn(NICIRA_NVP_HOST_ID);

        HostVO niciraNvpHost = mock(HostVO.class);
        when(niciraNvpHost.getId()).thenReturn(NICIRA_NVP_HOST_ID);
        when(hostDao.findById(NICIRA_NVP_HOST_ID)).thenReturn(niciraNvpHost);

        final NetworkOffering offering = mock(NetworkOffering.class);
        when(offering.getId()).thenReturn(NETWORK_ID);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Shared);

        final DeployDestination dest = mock(DeployDestination.class);

        final Domain dom = mock(Domain.class);
        when(dom.getName()).thenReturn("domain");
        final Account acc = mock(Account.class);
        when(acc.getAccountName()).thenReturn("accountname");
        final ReservationContext context = mock(ReservationContext.class);
        when(context.getDomain()).thenReturn(dom);
        when(context.getAccount()).thenReturn(acc);

        //SHARED NETWORKS CASE 2
        when(niciraNvpRouterMappingDao.existsMappingForNetworkId(NETWORK_ID)).thenReturn(false);

        VlanVO vlanVO = mock(VlanVO.class);
        when(vlanVO.getVlanTag()).thenReturn("111");
        when(vlanDao.listVlansByNetworkId(NETWORK_ID)).thenReturn(Arrays.asList(new VlanVO[] {vlanVO}));

        when(niciraNvpHost.getDetail("l2gatewayserviceuuid")).thenReturn(null);

        element.implement(network, offering, dest, context);
    }
}
