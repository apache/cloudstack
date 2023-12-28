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

import com.cloud.dc.DataCenter;
import com.cloud.dc.VlanVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.net.Ip;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NetworkModelImplTest {
    final String[] ip4Dns1 = {"5.5.5.5", "6.6.6.6"};
    final String[] ip4Dns2 = {"7.7.7.7", "8.8.8.8"};
    final String[] ip6Dns1 = {"2001:4860:4860::5555", "2001:4860:4860::6666"};
    final String[] ip6Dns2 = {"2001:4860:4860::7777", "2001:4860:4860::8888"};

    @InjectMocks
    private NetworkModelImpl networkModel = new NetworkModelImpl();

    private NetworkOfferingDao networkOfferingDao;
    private NetworkServiceMapDao networkServiceMapDao;
    @Before
    public void setUp() {
        networkOfferingDao = Mockito.mock(NetworkOfferingDao.class);
        networkServiceMapDao = Mockito.mock(NetworkServiceMapDao.class);
        networkModel._networkOfferingDao = networkOfferingDao;
        networkModel._ntwkSrvcDao = networkServiceMapDao;
    }
    private void prepareMocks(boolean isIp6, Network network, DataCenter zone,
                              String dns1, String dns2, String dns3, String dns4) {
        if (isIp6) {
            Mockito.when(network.getIp6Dns1()).thenReturn(dns1);
            Mockito.when(zone.getIp6Dns1()).thenReturn(dns2);
            Mockito.when(network.getIp6Dns2()).thenReturn(dns3);
            Mockito.when(zone.getIp6Dns2()).thenReturn(dns4);
        } else {
            Mockito.when(network.getDns1()).thenReturn(dns1);
            Mockito.when(zone.getDns1()).thenReturn(dns2);
            Mockito.when(network.getDns2()).thenReturn(dns3);
            Mockito.when(zone.getDns2()).thenReturn(dns4);
        }
    }

    private void testDnsCases(boolean isIp6) {
        String[] dns1 = isIp6 ? ip6Dns1 : ip4Dns1;
        String[] dns2 = isIp6 ? ip6Dns2 : ip4Dns2;
        Network network = Mockito.mock(Network.class);
        DataCenter zone = Mockito.mock(DataCenter.class);
        // Both network and zone have valid dns
        prepareMocks(isIp6, network, zone, dns1[0], dns1[1], dns2[0], dns1[1]);
        Pair<String, String> result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertEquals(dns2[0], result.second());
        // Network has valid dns and zone don't
        prepareMocks(isIp6, network, zone, dns1[0], null, dns2[0], null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertEquals(dns2[0], result.second());
        // Zone has a valid dns and network don't
        prepareMocks(isIp6, network, zone, null, dns1[1],  null, dns2[1]);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[1], result.first());
        Assert.assertEquals(dns2[1], result.second());
        // Zone has a valid dns and network has only first dns
        prepareMocks(isIp6, network, zone, dns1[0], dns1[1],  null, dns2[1]);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertNull(result.second());
        // Both network and zone only have the first dns
        prepareMocks(isIp6, network, zone, dns1[0], dns1[1],  null, null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertEquals(dns1[0], result.first());
        Assert.assertNull(result.second());
        // Both network and zone dns are null
        prepareMocks(isIp6, network, zone, null, null,  null, null);
        result = isIp6 ? networkModel.getNetworkIp6Dns(network, zone) :
                networkModel.getNetworkIp4Dns(network, zone);
        Assert.assertNull(result.first());
        Assert.assertNull(result.second());
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
        networkOfferingVO.setForNsx(false);
        Network network = new NetworkVO();
        List<NetworkServiceMapVO> networkServiceMapVOs = new ArrayList<>();
        networkServiceMapVOs.add(new NetworkServiceMapVO(15L, Network.Service.Firewall, Network.Provider.VPCVirtualRouter));
        networkServiceMapVOs.add(new NetworkServiceMapVO(15L, Network.Service.SourceNat, Network.Provider.VPCVirtualRouter));
        NetworkElement element = new VpcVirtualRouterElement();

        ReflectionTestUtils.setField(networkModel, "networkElements", List.of(element));
        Mockito.when(networkOfferingDao.findById(ArgumentMatchers.anyLong())).thenReturn(networkOfferingVO);
        Mockito.when(networkServiceMapDao.getServicesInNetwork(ArgumentMatchers.anyLong())).thenReturn(networkServiceMapVOs);
        Map<PublicIpAddress, Set<Network.Service>> ipToServices = new HashMap<>();
        ipToServices.put(publicIpAddress1, services1);
        ipToServices.put(publicIpAddress2, services2);
        Map<Network.Provider, ArrayList<PublicIpAddress>> result = networkModel.getProviderToIpList(network, ipToServices);
        Assert.assertNotNull(result);
    }
}
