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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cloud.user.Account;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.Service;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.AccountVO;
import com.cloud.utils.net.Ip;

@RunWith(MockitoJUnitRunner.class)
public class IpAddressManagerTest {

    @Mock
    IPAddressDao ipAddressDao;

    @Mock
    NetworkDao networkDao;

    @Mock
    NetworkOfferingDao networkOfferingDao;

    @Spy
    @InjectMocks
    IpAddressManagerImpl ipAddressManager;

    @InjectMocks
    NetworkModelImpl networkModel = Mockito.spy(new NetworkModelImpl());

    IPAddressVO ipAddressVO;

    AccountVO account;

    @Before
    public void setup() throws ResourceUnavailableException {

        ipAddressVO = new IPAddressVO(new Ip("192.0.0.1"), 1L, 1L, 1L,false);
        ipAddressVO.setAllocatedToAccountId(1L);

        account = new AccountVO("admin", 1L, null, Account.Type.ADMIN, 1L, "c65a73d5-ebbd-11e7-8f45-107b44277808");
        account.setId(1L);

        NetworkOfferingVO networkOfferingVO = Mockito.mock(NetworkOfferingVO.class);
        networkOfferingVO.setSharedSourceNat(false);

        Mockito.when(networkOfferingDao.findById(Mockito.anyLong())).thenReturn(networkOfferingVO);
    }

    @Test
    public void testGetStaticNatSourceIps() {
        String publicIpAddress = "192.168.1.3";
        IPAddressVO vo = mock(IPAddressVO.class);
        lenient().when(vo.getAddress()).thenReturn(new Ip(publicIpAddress));
        lenient().when(vo.getId()).thenReturn(1l);

        when(ipAddressDao.findById(anyLong())).thenReturn(vo);
        StaticNat snat = new StaticNatImpl(1, 1, 1, 1, publicIpAddress, false);

        List<IPAddressVO> ips = ipAddressManager.getStaticNatSourceIps(Collections.singletonList(snat));
        Assert.assertNotNull(ips);
        Assert.assertEquals(1, ips.size());

        IPAddressVO returnedVO = ips.get(0);
        Assert.assertEquals(vo, returnedVO);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestRequestedIpEqualsIp6Gateway() {
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", null, new ArrayList<Service>());

        boolean result = ipAddressManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "ip6Gateway");

        Mockito.verify(networkModel, Mockito.times(0)).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertTrue(result);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestRequestedIpEqualsGateway() {
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", null, new ArrayList<Service>());

        boolean result = ipAddressManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "gateway");

        Mockito.verify(networkModel, Mockito.times(0)).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertTrue(result);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestExpectFalseServicesNotEmpty() {
        List<Service> services = new ArrayList<Service>();
        Service serviceGateway = new Service("Gateway");
        services.add(serviceGateway);
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", null, services);

        boolean result = ipAddressManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "requestedIp");

        Mockito.verify(networkModel).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertFalse(result);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestExpectFalseServicesCidrNotNull() {
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", "cidr", new ArrayList<Service>());

        boolean result = ipAddressManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "requestedIp");

        Mockito.verify(networkModel).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertFalse(result);
    }

    @Test
    public void assertSourceNatImplementedNetwork() {

        NetworkVO networkImplemented = Mockito.mock(NetworkVO.class);
        lenient().when(networkImplemented.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        lenient().when(networkImplemented.getNetworkOfferingId()).thenReturn(8L);
        lenient().when(networkImplemented.getState()).thenReturn(Network.State.Implemented);
        when(networkImplemented.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(networkImplemented.getVpcId()).thenReturn(null);
        when(networkImplemented.getId()).thenReturn(1L);

        Mockito.lenient().when(networkDao.findById(1L)).thenReturn(networkImplemented);
        doReturn(null).when(ipAddressManager).getExistingSourceNatInNetwork(1L, 1L);

        boolean isSourceNat = ipAddressManager.isSourceNatAvailableForNetwork(account, ipAddressVO, networkImplemented);

        assertTrue("Source NAT should be true", isSourceNat);
    }

    @Test
    public void assertSourceNatAllocatedNetwork() {

        NetworkVO networkAllocated = Mockito.mock(NetworkVO.class);
        lenient().when(networkAllocated.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(networkAllocated.getNetworkOfferingId()).thenReturn(8L);
        lenient().when(networkAllocated.getState()).thenReturn(Network.State.Allocated);
        when(networkAllocated.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(networkAllocated.getVpcId()).thenReturn(null);
        when(networkAllocated.getId()).thenReturn(2L);

        Mockito.lenient().when(networkDao.findById(2L)).thenReturn(networkAllocated);
        doReturn(null).when(ipAddressManager).getExistingSourceNatInNetwork(1L, 2L);

        assertTrue(ipAddressManager.isSourceNatAvailableForNetwork(account, ipAddressVO, networkAllocated));
    }

    @Test
    public void assertExistingSourceNatAllocatedNetwork() {

        NetworkVO networkNat = Mockito.mock(NetworkVO.class);
        lenient().when(networkNat.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(networkNat.getNetworkOfferingId()).thenReturn(8L);
        lenient().when(networkNat.getState()).thenReturn(Network.State.Implemented);
        lenient().when(networkNat.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(networkNat.getId()).thenReturn(3L);
        lenient().when(networkNat.getVpcId()).thenReturn(null);
        when(networkNat.getId()).thenReturn(3L);

        IPAddressVO sourceNat = new IPAddressVO(new Ip("192.0.0.2"), 1L, 1L, 1L,true);

        Mockito.lenient().when(networkDao.findById(3L)).thenReturn(networkNat);
        doReturn(sourceNat).when(ipAddressManager).getExistingSourceNatInNetwork(1L, 3L);

        boolean isSourceNat = ipAddressManager.isSourceNatAvailableForNetwork(account, ipAddressVO, networkNat);

        assertFalse("Source NAT should be false", isSourceNat);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestNetworkOfferingsEmptyAndCidrNull() {
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", null, new ArrayList<Service>());

        boolean result = ipAddressManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "requestedIp");

        Mockito.verify(networkModel).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertTrue(result);
    }

    private Network setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(long networkOfferingId, String gateway, String ip6Gateway, String cidr, List<Service> services) {
        Network network = mock(Network.class);
        Mockito.when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        Mockito.when(network.getGateway()).thenReturn(gateway);
        Mockito.when(network.getIp6Gateway()).thenReturn(ip6Gateway);
        Mockito.when(network.getCidr()).thenReturn(cidr);
        Mockito.doReturn(services).when(networkModel).listNetworkOfferingServices(Mockito.anyLong());
        return network;
    }

}
