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
package com.cloud.network;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.Ip;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.extension.ExtensionHelper;
import org.apache.cloudstack.framework.extensions.network.NetworkExtensionElement;

@RunWith(MockitoJUnitRunner.class)
public class NetworkModelImplTest {
    final String[] ip4Dns1 = {"5.5.5.5", "6.6.6.6", "9.9.9.9"};
    final String[] ip4Dns2 = {"7.7.7.7", "8.8.8.8", "10.10.10.10"};
    final String[] ip6Dns1 = {"2001:4860:4860::5555", "2001:4860:4860::6666", "2001:4860:4860::9999"};
    final String[] ip6Dns2 = {"2001:4860:4860::7777", "2001:4860:4860::8888", "2001:4860:4860::AAAA"};

    @Mock
    private VpcDao vpcDao;
    @Mock
    private NetworkDao _networksDao;
    @Inject
    private NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Mock
    private ExtensionHelper extensionHelper;

    @Mock
    private NetworkExtensionElement networkExtensionElement;

    @Mock
    private PhysicalNetworkDao physicalNetworkDao;

    @Mock
    private PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;

    @Mock
    private VMInstanceDao vmInstanceDao;
    @Mock
    private NicDao nicDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private ConfigurationManager configMgr;
    @Mock
    private EntityManager entityMgr;

    @Spy
    @InjectMocks
    private NetworkModelImpl networkModel = new NetworkModelImpl();

    private NetworkOfferingDao networkOfferingDao;
    private NetworkServiceMapDao networkServiceMapDao;

    @Before
    public void setUp() {
        networkOfferingDao = mock(NetworkOfferingDao.class);
        networkServiceMapDao = mock(NetworkServiceMapDao.class);
        networkOfferingServiceMapDao = mock(NetworkOfferingServiceMapDao.class);
        networkModel._networkOfferingDao = networkOfferingDao;
        networkModel._ntwkSrvcDao = networkServiceMapDao;
        networkModel._ntwkOfferingSrvcDao = networkOfferingServiceMapDao;
        networkModel.extensionHelper = extensionHelper;
        networkModel.networkExtensionElement = networkExtensionElement;
        networkModel._physicalNetworkDao = physicalNetworkDao;
        networkModel._pNSPDao = physicalNetworkServiceProviderDao;
        networkModel._vmDao = vmInstanceDao;
        networkModel._nicDao = nicDao;
        networkModel._serviceOfferingDao = serviceOfferingDao;
        networkModel._configMgr = configMgr;
        networkModel._entityMgr = entityMgr;
        Mockito.lenient().when(extensionHelper.isNetworkExtensionProvider(Mockito.anyString())).thenReturn(false);
    }

    private void prepareMocks(boolean isIp6, Network network, DataCenter zone, VpcVO vpc,
                              String networkDns1, String zoneDns1, String networkDns2, String zoneDns2,
                              String vpcDns1, String vpcDns2) {
        if (isIp6) {
            when(network.getIp6Dns1()).thenReturn(networkDns1);
            when(zone.getIp6Dns1()).thenReturn(zoneDns1);
            when(network.getIp6Dns2()).thenReturn(networkDns2);
            when(zone.getIp6Dns2()).thenReturn(zoneDns2);
            when(vpc.getIp6Dns1()).thenReturn(vpcDns1);
            when(vpc.getIp6Dns2()).thenReturn(vpcDns2);
        } else {
            when(network.getDns1()).thenReturn(networkDns1);
            when(zone.getDns1()).thenReturn(zoneDns1);
            when(network.getDns2()).thenReturn(networkDns2);
            when(zone.getDns2()).thenReturn(zoneDns2);
            when(vpc.getIp4Dns1()).thenReturn(vpcDns1);
            when(vpc.getIp4Dns2()).thenReturn(vpcDns2);
        }
    }

    private void testDnsCases(boolean isIp6) {
        String[] dns1 = isIp6 ? ip6Dns1 : ip4Dns1;
        String[] dns2 = isIp6 ? ip6Dns2 : ip4Dns2;
        Network network = mock(Network.class);
        DataCenter zone = mock(DataCenter.class);
        VpcVO vpc = mock(VpcVO.class);
        when(network.getVpcId()).thenReturn(1L);
        Mockito.doReturn(vpc).when(vpcDao).findById(ArgumentMatchers.anyLong());
        // network, vpc and zone have valid dns
        prepareMocks(isIp6, network, zone, vpc, dns1[0], dns1[1], dns2[0], dns2[1], dns1[2], dns2[2]);
        Pair<String, String> result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertEquals(dns2[0], result.second());
        // Network has valid dns and vpc/zone don't
        prepareMocks(isIp6, network, zone, vpc, dns1[0], null, dns2[0], null, null, null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertEquals(dns2[0], result.second());
        // Vpc has valid dns and network/zone don't
        prepareMocks(isIp6, network, zone, vpc, null, null, null, null, dns1[2], dns2[2]);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[2], result.first());
        Assert.assertEquals(dns2[2], result.second());
        // Zone has a valid dns and network/vpc don't
        prepareMocks(isIp6, network, zone, vpc, null, dns1[1], null, dns2[1], null, null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[1], result.first());
        Assert.assertEquals(dns2[1], result.second());
        // Zone/vpc has a valid dns and network has only first dns
        prepareMocks(isIp6, network, zone, vpc, dns1[0], dns1[1], null, dns2[1], dns1[2], dns2[2]);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        assertNull(result.second());
        // network don't have a valid dns, vpc has only first dns, Zone has a valid dns
        prepareMocks(isIp6, network, zone, vpc, null, dns1[1], null, dns2[1], dns1[2], null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[2], result.first());
        assertNull(result.second());
        // network/vpc/zone only have the first dns
        prepareMocks(isIp6, network, zone, vpc, dns1[0], dns1[1], null, null, dns1[2], null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        assertNull(result.second());
        // network/vpc and zone dns are null
        prepareMocks(isIp6, network, zone, vpc, null, null, null, null, null, null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        assertNull(result.first());
        assertNull(result.second());
    }

    @Test
    public void testGetNetworkIp4Dns() {
        testDnsCases(false);
    }

    @Test
    public void testGetNetworkIp6Dns() {
        testDnsCases(true);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testVerifyIp4DnsPairDns1NullFailure() {
        networkModel.verifyIp4DnsPair(null, ip4Dns1[1]);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testVerifyIp4DnsPairInvalidDns1Failure() {
        networkModel.verifyIp4DnsPair("invalid", ip4Dns1[1]);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testVerifyIp4DnsPairInvalidDns2Failure() {
        networkModel.verifyIp4DnsPair(ip4Dns1[0], "invalid");
    }

    @Test
    public void testVerifyIp4DnsPairValid() {
        networkModel.verifyIp4DnsPair(ip4Dns1[0], ip4Dns1[1]);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testVerifyIp6DnsPairDns1NullFailure() {
        networkModel.verifyIp6DnsPair(null, ip6Dns1[1]);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testVerifyIp6DnsPairInvalidDns1Failure() {
        networkModel.verifyIp6DnsPair("invalid", ip6Dns1[1]);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testVerifyIp6DnsPairInvalidDns2Failure() {
        networkModel.verifyIp6DnsPair(ip6Dns1[0], "invalid");
    }

    @Test
    public void testVerifyIp6DnsPairValid() {
        networkModel.verifyIp6DnsPair(ip6Dns1[0], ip6Dns1[1]);
    }

    @Test
    public void testGetProviderToIpList() {
        Set<Network.Service> services1 = new HashSet<>(List.of(Network.Service.Firewall));
        Set<Network.Service> services2 = new HashSet<>(List.of(Network.Service.SourceNat));
        Ip ip1 = new Ip("10.10.10.10");
        Ip ip2 = new Ip("10.10.10.10");
        IPAddressVO ipAddressVO1 = new IPAddressVO(ip1, 1L, 0x0ac00000L, 2L, true);
        IPAddressVO ipAddressVO2 = new IPAddressVO(ip2, 1L, 0x0ac00000L, 2L, true);
        VlanVO vlanVO = new VlanVO();
        vlanVO.setNetworkId(15L);
        PublicIpAddress publicIpAddress1 = new PublicIp(ipAddressVO1, vlanVO, 0x0ac00000L);
        PublicIpAddress publicIpAddress2 = new PublicIp(ipAddressVO2, vlanVO, 0x0ac00000L);
        NetworkOfferingVO networkOfferingVO = new NetworkOfferingVO();
        networkOfferingVO.setForVpc(true);
        Network network = new NetworkVO();
        List<NetworkServiceMapVO> networkServiceMapVOs = new ArrayList<>();
        networkServiceMapVOs.add(new NetworkServiceMapVO(15L, Network.Service.Firewall.getName(), Network.Provider.VPCVirtualRouter.getName()));
        networkServiceMapVOs.add(new NetworkServiceMapVO(15L, Network.Service.SourceNat.getName(), Network.Provider.VPCVirtualRouter.getName()));
        NetworkElement element = new VpcVirtualRouterElement();

        ReflectionTestUtils.setField(networkModel, "networkElements", List.of(element));
        when(networkOfferingDao.findById(ArgumentMatchers.anyLong())).thenReturn(networkOfferingVO);
        when(networkServiceMapDao.getServicesInNetwork(ArgumentMatchers.anyLong())).thenReturn(networkServiceMapVOs);
        Map<PublicIpAddress, Set<Network.Service>> ipToServices = new HashMap<>();
        ipToServices.put(publicIpAddress1, services1);
        ipToServices.put(publicIpAddress2, services2);
        Mockito.when(networkOfferingServiceMapDao.isProviderForNetworkOffering(networkOfferingVO.getId(), Network.Provider.Nsx)).thenReturn(false);
        Map<Network.Provider, ArrayList<PublicIpAddress>> result = networkModel.getProviderToIpList(network, ipToServices);
        assertNotNull(result);
    }

    @Test
    public void getNicProfile_validInputs_returnsNicProfile() {
        VirtualMachine vm = mock(VirtualMachine.class);
        Nic nic = mock(Nic.class);
        NetworkVO network = mock(NetworkVO.class);
        when(network.getId()).thenReturn(1L);
        when(nic.getNetworkId()).thenReturn(1L);
        when(vm.getId()).thenReturn(10L);
        when(_networksDao.findById(1L)).thenReturn(network);
        doReturn(100).when(networkModel).getNetworkRate(1L, 10L);
        doReturn("cloud").when(networkModel).getNetworkTag(any(), any());
        doReturn(false).when(networkModel).isSecurityGroupSupportedInNetwork(any());
        NicProfile result = networkModel.getNicProfile(vm, nic, mock(DataCenterVO.class));

        assertNotNull(result);
    }

    // -----------------------------------------------------------------------
    // Tests for getElementImplementingProvider with extension provider
    // -----------------------------------------------------------------------

    @Test
    public void getElementImplementingProviderReturnsExtensionElementForExtensionProvider() {
        String providerName = "my-ext-provider";
        // Provider is not in the static map, so element would be null
        ReflectionTestUtils.setField(networkModel, "networkElements", new ArrayList<>());
        when(extensionHelper.isNetworkExtensionProvider(providerName)).thenReturn(true);
        NetworkExtensionElement mockElement = mock(NetworkExtensionElement.class);
        when(networkExtensionElement.withProviderName(providerName)).thenReturn(mockElement);

        NetworkElement result = networkModel.getElementImplementingProvider(providerName);
        // When the element is a NetworkExtensionElement (which is a NetworkElement), result should not be null
        assertNotNull(result);
    }

    @Test
    public void getElementImplementingProviderReturnsNullForUnknownNonExtensionProvider() {
        String providerName = "unknown-provider";
        ReflectionTestUtils.setField(networkModel, "networkElements", new ArrayList<>());
        when(extensionHelper.isNetworkExtensionProvider(providerName)).thenReturn(false);

        NetworkElement result = networkModel.getElementImplementingProvider(providerName);
        assertNull(result);
    }

    // -----------------------------------------------------------------------
    // Tests for resolveProvider
    // -----------------------------------------------------------------------

    @Test
    public void resolveProviderReturnsKnownProvider() {
        Network.Provider result = networkModel.resolveProvider(Network.Provider.VirtualRouter.getName());
        assertNotNull(result);
        assertEquals(Network.Provider.VirtualRouter, result);
    }

    @Test
    public void resolveProviderReturnsTransientProviderForExtensionProvider() {
        String extensionName = "my-ext-network-provider";
        when(extensionHelper.isNetworkExtensionProvider(extensionName)).thenReturn(true);

        Network.Provider result = networkModel.resolveProvider(extensionName);
        assertNotNull(result);
        assertEquals(extensionName, result.getName());
    }

    @Test
    public void resolveProviderReturnsNullForUnknownNonExtensionProvider() {
        String providerName = "totally-unknown";
        when(extensionHelper.isNetworkExtensionProvider(providerName)).thenReturn(false);

        Network.Provider result = networkModel.resolveProvider(providerName);
        assertNull(result);
    }

    // -----------------------------------------------------------------------
    // Tests for canElementEnableIndividualServicesByName
    // -----------------------------------------------------------------------

    @Test
    public void canElementEnableIndividualServicesByNameReturnsFalseForNullProvider() {
        assertFalse(networkModel.canElementEnableIndividualServicesByName(null));
    }

    @Test
    public void canElementEnableIndividualServicesByNameReturnsFalseForUnknownProvider() {
        when(extensionHelper.isNetworkExtensionProvider("unknown")).thenReturn(false);
        assertFalse(networkModel.canElementEnableIndividualServicesByName("unknown"));
    }

    // -----------------------------------------------------------------------
    // Tests for getExternalProviderCapabilities
    // -----------------------------------------------------------------------

    @Test
    public void getExternalProviderCapabilitiesCallsExtensionHelper() {
        Map<Network.Service, Map<Network.Capability, String>> caps = new HashMap<>();
        when(extensionHelper.getNetworkCapabilitiesForProvider(10L, "my-ext")).thenReturn(caps);

        Map<Network.Service, Map<Network.Capability, String>> result =
                networkModel.getExternalProviderCapabilities(10L, "my-ext");
        assertEquals(caps, result);
    }

    // -----------------------------------------------------------------------
    // Tests for isServiceProvidedByNsp (via listSupportedNetworkServiceProviders)
    // -----------------------------------------------------------------------

    @Test
    public void listSupportedNetworkServiceProvidersIncludesExtensionBackedProvidersWhenServiceIsSupported() {
        PhysicalNetworkServiceProviderVO nsp = mock(PhysicalNetworkServiceProviderVO.class);
        when(nsp.getProviderName()).thenReturn("my-ext");
        when(nsp.isFirewallServiceProvided()).thenReturn(true);
        when(physicalNetworkServiceProviderDao.listAll()).thenReturn(List.of(nsp));

        Extension extension = mock(Extension.class);
        when(extension.getName()).thenReturn("my-ext");
        when(extensionHelper.listExtensionsByType(Extension.Type.NetworkOrchestrator)).thenReturn(List.of(extension));

        when(extensionHelper.isNetworkExtensionProvider("my-ext")).thenReturn(true);

        // networkElements is empty so no standard providers found
        ReflectionTestUtils.setField(networkModel, "networkElements", new ArrayList<>());

        NetworkExtensionElement mockElement = mock(NetworkExtensionElement.class);
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<>();
        capabilities.put(Network.Service.Firewall, new HashMap<>());
        when(networkExtensionElement.withProviderName("my-ext")).thenReturn(mockElement);
        when(mockElement.getCapabilities()).thenReturn(capabilities);

        List<? extends Network.Provider> result =
                networkModel.listSupportedNetworkServiceProviders(Network.Service.Firewall.getName());

        boolean found = result.stream().anyMatch(p -> "my-ext".equalsIgnoreCase(p.getName()));
        assertTrue("Extension-backed provider should be included", found);

        Mockito.verify(physicalNetworkServiceProviderDao, Mockito.times(1)).listAll();
        Mockito.verify(physicalNetworkServiceProviderDao, Mockito.never()).listBy(Mockito.anyLong());
    }

    @Test
    public void listSupportedNetworkServiceProvidersExcludesExtensionBackedProvidersWhenServiceIsNotSupported() {
        PhysicalNetworkServiceProviderVO nsp = mock(PhysicalNetworkServiceProviderVO.class);
        when(nsp.getProviderName()).thenReturn("my-ext");
        when(nsp.isFirewallServiceProvided()).thenReturn(true);
        when(physicalNetworkServiceProviderDao.listAll()).thenReturn(List.of(nsp));

        Extension extension = mock(Extension.class);
        when(extension.getName()).thenReturn("my-ext");
        when(extensionHelper.listExtensionsByType(Extension.Type.NetworkOrchestrator)).thenReturn(List.of(extension));

        when(extensionHelper.isNetworkExtensionProvider("my-ext")).thenReturn(true);

        // networkElements is empty so no standard providers found
        ReflectionTestUtils.setField(networkModel, "networkElements", new ArrayList<>());

        NetworkExtensionElement mockElement = mock(NetworkExtensionElement.class);
        Map<Network.Service, Map<Network.Capability, String>> capabilities = new HashMap<>();
        when(networkExtensionElement.withProviderName("my-ext")).thenReturn(mockElement);
        when(mockElement.getCapabilities()).thenReturn(capabilities);

        List<? extends Network.Provider> result =
                networkModel.listSupportedNetworkServiceProviders(Network.Service.Firewall.getName());

        boolean found = result.stream().anyMatch(p -> "my-ext".equalsIgnoreCase(p.getName()));
        assertFalse("Extension-backed provider should NOT be included", found);

        Mockito.verify(physicalNetworkServiceProviderDao, Mockito.times(1)).listAll();
        Mockito.verify(physicalNetworkServiceProviderDao, Mockito.never()).listBy(Mockito.anyLong());
    }

    // -----------------------------------------------------------------------
    // Tests for getNetworkRate
    // -----------------------------------------------------------------------

    private NetworkVO mockNetwork(long networkId, long networkOfferingId, long dataCenterId, TrafficType trafficType) {
        NetworkVO network = mock(NetworkVO.class);
        when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        when(network.getDataCenterId()).thenReturn(dataCenterId);
        when(network.getTrafficType()).thenReturn(trafficType);
        when(_networksDao.findById(networkId)).thenReturn(network);
        return network;
    }

    private void mockNetworkOffering(long networkOfferingId) {
        NetworkOffering ntwkOff = mock(NetworkOffering.class);
        when(ntwkOff.getId()).thenReturn(networkOfferingId);
        when(entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(ntwkOff);
    }

    @Test
    public void getNetworkRate_userVmWithNic_returnsServiceOfferingRate() {
        long networkId = 1L, vmId = 10L, serviceOfferingId = 20L, dataCenterId = 2L;
        mockNetwork(networkId, 30L, dataCenterId, TrafficType.Guest);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        when(vm.getServiceOfferingId()).thenReturn(serviceOfferingId);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(nicDao.findByNtwkIdAndInstanceId(networkId, vmId)).thenReturn(mock(NicVO.class));
        when(configMgr.getServiceOfferingNetworkRate(serviceOfferingId, dataCenterId)).thenReturn(100);

        assertEquals(Integer.valueOf(100), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_userVmWithoutNic_fallsBackToNetworkOffering() {
        long networkId = 1L, vmId = 10L, networkOfferingId = 30L, dataCenterId = 2L;
        mockNetwork(networkId, networkOfferingId, dataCenterId, TrafficType.Guest);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.User);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(nicDao.findByNtwkIdAndInstanceId(networkId, vmId)).thenReturn(null);
        mockNetworkOffering(networkOfferingId);
        when(configMgr.getNetworkOfferingNetworkRate(networkOfferingId, dataCenterId)).thenReturn(50);

        assertEquals(Integer.valueOf(50), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_routerGuestWithSystemOfferingRate_returnsSystemOfferingRate() {
        long networkId = 1L, vmId = 11L, serviceOfferingId = 21L, dataCenterId = 2L;
        mockNetwork(networkId, 31L, dataCenterId, TrafficType.Guest);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vm.getServiceOfferingId()).thenReturn(serviceOfferingId);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(nicDao.findByNtwkIdAndInstanceId(networkId, vmId)).thenReturn(mock(NicVO.class));
        ServiceOfferingVO routerOffering = mock(ServiceOfferingVO.class);
        when(routerOffering.getRateMbps()).thenReturn(75);
        when(serviceOfferingDao.findById(serviceOfferingId)).thenReturn(routerOffering);

        assertEquals(Integer.valueOf(75), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_routerGuestWithZeroSystemOfferingRate_returnsUnlimited() {
        long networkId = 1L, vmId = 11L, serviceOfferingId = 21L, dataCenterId = 2L;
        mockNetwork(networkId, 31L, dataCenterId, TrafficType.Guest);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vm.getServiceOfferingId()).thenReturn(serviceOfferingId);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(nicDao.findByNtwkIdAndInstanceId(networkId, vmId)).thenReturn(mock(NicVO.class));
        ServiceOfferingVO routerOffering = mock(ServiceOfferingVO.class);
        when(routerOffering.getRateMbps()).thenReturn(0);
        when(serviceOfferingDao.findById(serviceOfferingId)).thenReturn(routerOffering);

        assertEquals(Integer.valueOf(-1), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_routerGuestWithoutSystemOfferingRate_fallsBackToNetworkOfferingRate() {
        long networkId = 1L, vmId = 11L, serviceOfferingId = 21L, networkOfferingId = 31L, dataCenterId = 2L;
        mockNetwork(networkId, networkOfferingId, dataCenterId, TrafficType.Guest);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vm.getServiceOfferingId()).thenReturn(serviceOfferingId);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(nicDao.findByNtwkIdAndInstanceId(networkId, vmId)).thenReturn(mock(NicVO.class));
        when(serviceOfferingDao.findById(serviceOfferingId)).thenReturn(null);
        when(configMgr.getNetworkOfferingNetworkRate(networkOfferingId, dataCenterId)).thenReturn(60);

        assertEquals(Integer.valueOf(60), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_routerGuestWithoutNic_fallsBackToNetworkOfferingViaEntityManager() {
        long networkId = 1L, vmId = 11L, networkOfferingId = 31L, dataCenterId = 2L;
        mockNetwork(networkId, networkOfferingId, dataCenterId, TrafficType.Guest);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(nicDao.findByNtwkIdAndInstanceId(networkId, vmId)).thenReturn(null);
        mockNetworkOffering(networkOfferingId);
        when(configMgr.getNetworkOfferingNetworkRate(networkOfferingId, dataCenterId)).thenReturn(40);

        assertEquals(Integer.valueOf(40), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_routerPublicWithGuestSibling_returnsGuestNetworkOfferingRate() {
        long networkId = 1L, vmId = 12L, dataCenterId = 2L, guestNetworkId = 5L, guestOfferingId = 55L;
        mockNetwork(networkId, 99L, dataCenterId, TrafficType.Public);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        NicVO guestNic = mock(NicVO.class);
        when(guestNic.getNetworkId()).thenReturn(guestNetworkId);
        when(nicDao.listByVmId(vmId)).thenReturn(List.of(guestNic));
        NetworkVO guestNetwork = mock(NetworkVO.class);
        when(guestNetwork.getTrafficType()).thenReturn(TrafficType.Guest);
        when(guestNetwork.getNetworkOfferingId()).thenReturn(guestOfferingId);
        when(_networksDao.findById(guestNetworkId)).thenReturn(guestNetwork);
        when(configMgr.getNetworkOfferingNetworkRate(guestOfferingId, dataCenterId)).thenReturn(80);

        assertEquals(Integer.valueOf(80), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_routerPublicWithoutGuestSibling_fallsBackToNetworkOfferingViaEntityManager() {
        long networkId = 1L, vmId = 12L, networkOfferingId = 99L, dataCenterId = 2L;
        mockNetwork(networkId, networkOfferingId, dataCenterId, TrafficType.Public);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);
        when(nicDao.listByVmId(vmId)).thenReturn(Collections.emptyList());
        mockNetworkOffering(networkOfferingId);
        when(configMgr.getNetworkOfferingNetworkRate(networkOfferingId, dataCenterId)).thenReturn(33);

        assertEquals(Integer.valueOf(33), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_consoleProxy_returnsUnlimitedWithoutOfferingLookup() {
        long networkId = 1L, vmId = 13L, dataCenterId = 2L;
        mockNetwork(networkId, 40L, dataCenterId, TrafficType.Guest);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);

        assertEquals(Integer.valueOf(-1), networkModel.getNetworkRate(networkId, vmId));
        Mockito.verifyNoInteractions(configMgr);
    }

    @Test
    public void getNetworkRate_secondaryStorageVm_returnsUnlimitedWithoutOfferingLookup() {
        long networkId = 1L, vmId = 14L, dataCenterId = 2L;
        mockNetwork(networkId, 40L, dataCenterId, TrafficType.Guest);
        VMInstanceVO vm = mock(VMInstanceVO.class);
        when(vm.getType()).thenReturn(VirtualMachine.Type.SecondaryStorageVm);
        when(vmInstanceDao.findById(vmId)).thenReturn(vm);

        assertEquals(Integer.valueOf(-1), networkModel.getNetworkRate(networkId, vmId));
        Mockito.verifyNoInteractions(configMgr);
    }

    @Test
    public void getNetworkRate_noVm_usesNetworkOfferingRate() {
        long networkId = 1L, networkOfferingId = 30L, dataCenterId = 2L;
        mockNetwork(networkId, networkOfferingId, dataCenterId, TrafficType.Guest);
        mockNetworkOffering(networkOfferingId);
        when(configMgr.getNetworkOfferingNetworkRate(networkOfferingId, dataCenterId)).thenReturn(90);

        assertEquals(Integer.valueOf(90), networkModel.getNetworkRate(networkId, null));
    }

    @Test
    public void getNetworkRate_vmNotFound_fallsBackToNetworkOfferingRate() {
        long networkId = 1L, vmId = 15L, networkOfferingId = 30L, dataCenterId = 2L;
        mockNetwork(networkId, networkOfferingId, dataCenterId, TrafficType.Guest);
        when(vmInstanceDao.findById(vmId)).thenReturn(null);
        mockNetworkOffering(networkOfferingId);
        when(configMgr.getNetworkOfferingNetworkRate(networkOfferingId, dataCenterId)).thenReturn(77);

        assertEquals(Integer.valueOf(77), networkModel.getNetworkRate(networkId, vmId));
    }

    @Test
    public void getNetworkRate_noNetworkOffering_usesGlobalThrottlingRate() {
        long networkId = 1L, networkOfferingId = 30L, dataCenterId = 2L;
        mockNetwork(networkId, networkOfferingId, dataCenterId, TrafficType.Guest);
        when(entityMgr.findById(NetworkOffering.class, networkOfferingId)).thenReturn(null);

        Integer expected = NetworkOrchestrationService.NetworkThrottlingRate.valueIn(dataCenterId);
        assertEquals(expected, networkModel.getNetworkRate(networkId, null));
    }
}
