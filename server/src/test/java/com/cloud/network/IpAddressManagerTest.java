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

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.cloud.network.Network.Service;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.utils.net.Ip;

public class IpAddressManagerTest {

    @Mock
    IPAddressDao _ipAddrDao;

    @InjectMocks
    IpAddressManagerImpl _ipManager;

    @InjectMocks
    NetworkModelImpl networkModel = Mockito.spy(new NetworkModelImpl());

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetStaticNatSourceIps() {
        String publicIpAddress = "192.168.1.3";
        IPAddressVO vo = mock(IPAddressVO.class);
        when(vo.getAddress()).thenReturn(new Ip(publicIpAddress));
        when(vo.getId()).thenReturn(1l);

        when(_ipAddrDao.findById(anyLong())).thenReturn(vo);
        StaticNat snat = new StaticNatImpl(1, 1, 1, 1, publicIpAddress, false);

        List<IPAddressVO> ips = _ipManager.getStaticNatSourceIps(Collections.singletonList(snat));
        Assert.assertNotNull(ips);
        Assert.assertEquals(1, ips.size());

        IPAddressVO returnedVO = ips.get(0);
        Assert.assertEquals(vo, returnedVO);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestRequestedIpEqualsIp6Gateway() {
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", null, new ArrayList<Service>());

        boolean result = _ipManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "ip6Gateway");

        Mockito.verify(networkModel, Mockito.times(0)).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertTrue(result);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestRequestedIpEqualsGateway() {
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", null, new ArrayList<Service>());

        boolean result = _ipManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "gateway");

        Mockito.verify(networkModel, Mockito.times(0)).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertTrue(result);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestExpectFalseServicesNotEmpty() {
        List<Service> services = new ArrayList<Service>();
        Service serviceGateway = new Service("Gateway");
        services.add(serviceGateway);
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", null, services);

        boolean result = _ipManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "requestedIp");

        Mockito.verify(networkModel).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertFalse(result);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestExpectFalseServicesCidrNotNull() {
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", "cidr", new ArrayList<Service>());

        boolean result = _ipManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "requestedIp");

        Mockito.verify(networkModel).listNetworkOfferingServices(Mockito.anyLong());
        Assert.assertFalse(result);
    }

    @Test
    public void isIpEqualsGatewayOrNetworkOfferingsEmptyTestNetworkOfferingsEmptyAndCidrNull() {
        Network network = setTestIsIpEqualsGatewayOrNetworkOfferingsEmpty(0l, "gateway", "ip6Gateway", null, new ArrayList<Service>());

        boolean result = _ipManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, "requestedIp");

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