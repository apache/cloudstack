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
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddress.State;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.dao.IPAddressDaoImpl;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.UserIpv6AddressDaoImpl;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.dao.NicSecondaryIpDaoImpl;
import com.cloud.vm.dao.NicSecondaryIpVO;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.mock;

public class Ipv6AddressManagerTest {

    @InjectMocks
    Ipv6AddressManagerImpl ip6Manager = Mockito.spy(new Ipv6AddressManagerImpl());

    @InjectMocks
    NicSecondaryIpDaoImpl nicSecondaryIpDao = Mockito.spy(new NicSecondaryIpDaoImpl());

    @InjectMocks
    UserIpv6AddressDaoImpl ipv6Dao = Mockito.spy(new UserIpv6AddressDaoImpl());

    @InjectMocks
    IpAddressManagerImpl ipAddressManager = Mockito.spy(new IpAddressManagerImpl());

    @InjectMocks
    NetworkModelImpl networkModel = Mockito.mock(NetworkModelImpl.class);// = Mockito.spy(new NetworkModelImpl());

    @InjectMocks
    IPAddressDaoImpl ipAddressDao = Mockito.spy(new IPAddressDaoImpl());

    private Network network = mockNetwork();

    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void isIp6TakenTestNoNull() {
        setIsIp6TakenTest(new UserIpv6AddressVO(), new NicSecondaryIpVO(0l, "ipaddr", 0l, 0l, 0l, 0l));
        boolean result = ip6Manager.isIp6Taken(network, "requestedIpv6");
        assertAndVerifyIsIp6Taken(true, result);
    }

    @Test
    public void isIp6TakenTestSecIpNull() {
        setIsIp6TakenTest(new UserIpv6AddressVO(), null);
        boolean result = ip6Manager.isIp6Taken(network, "requestedIpv6");
        assertAndVerifyIsIp6Taken(true, result);
    }

    @Test
    public void isIp6TakenTestUserIpv6AddressNull() {
        setIsIp6TakenTest(null, new NicSecondaryIpVO(0l, "ipaddr", 0l, 0l, 0l, 0l));
        boolean result = ip6Manager.isIp6Taken(network, "requestedIpv6");
        assertAndVerifyIsIp6Taken(true, result);
    }

    @Test
    public void isIp6TakenTestAllNull() {
        setIsIp6TakenTest(null, null);
        boolean result = ip6Manager.isIp6Taken(network, "requestedIpv6");
        assertAndVerifyIsIp6Taken(false, result);
    }

    private void assertAndVerifyIsIp6Taken(boolean expected, boolean result) {
        Assert.assertEquals(expected, result);
        Mockito.verify(ipv6Dao).findByNetworkIdAndIp(Mockito.anyLong(), Mockito.anyString());
        Mockito.verify(nicSecondaryIpDao).findByIp6AddressAndNetworkId(Mockito.anyString(), Mockito.anyLong());
    }

    private void setIsIp6TakenTest(UserIpv6AddressVO userIpv6, NicSecondaryIpVO nicSecondaryIp) {
        Mockito.doReturn(userIpv6).when(ipv6Dao).findByNetworkIdAndIp(Mockito.anyLong(), Mockito.anyString());
        Mockito.doReturn(nicSecondaryIp).when(nicSecondaryIpDao).findByIp6AddressAndNetworkId(Mockito.anyString(), Mockito.anyLong());
    }

    private Network mockNetwork() {
        Network network = mock(Network.class);
        Mockito.when(network.getId()).thenReturn(0l);
        Mockito.when(network.getIp6Cidr()).thenReturn("2001:db8::/32");
        return network;
    }

    @Test
    public void allocatePublicIp6ForGuestNicTestNoException() throws InsufficientAddressCapacityException {
        Account owner = Mockito.mock(Account.class);
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8::10", false, false);

        String returnedIp = ip6Manager.allocatePublicIp6ForGuestNic(network, 0l, owner, requestedIpv6);

        Mockito.verify(ip6Manager).checkIfCanAllocateIpv6Address(network, requestedIpv6);
        Assert.assertEquals(requestedIpv6, returnedIp);
    }

    @Test(expected = InsufficientAddressCapacityException.class)
    public void checkIfCanAllocateIpv6AddressTestIp6IsTaken() throws InsufficientAddressCapacityException {
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8::10", true, false);

        ip6Manager.checkIfCanAllocateIpv6Address(network, requestedIpv6);

        verifyCheckIfCanAllocateIpv6AddressTest(network, requestedIpv6, 1, 0);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIfCanAllocateIpv6AddressTestIpIsIpEqualsGatewayOrNetworkOfferingsEmpty() throws InsufficientAddressCapacityException {
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8::10", false, true);

        ip6Manager.checkIfCanAllocateIpv6Address(network, requestedIpv6);

        verifyCheckIfCanAllocateIpv6AddressTest(network, requestedIpv6, 1, 1);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void checkIfCanAllocateIpv6AddressTestIpINotInTheNetwork() throws InsufficientAddressCapacityException {
        String requestedIpv6 = "2002:db8::10";
        setCheckIfCanAllocateIpv6AddresscTest(requestedIpv6, false, false);

        ip6Manager.checkIfCanAllocateIpv6Address(network, requestedIpv6);

        verifyCheckIfCanAllocateIpv6AddressTest(network, requestedIpv6, 1, 1);
    }

    private void verifyCheckIfCanAllocateIpv6AddressTest(Network network, String requestedIpv6, int isIp6TakenTimes, int isIpEqualsGatewayTimes) {
        Mockito.verify(ip6Manager, Mockito.times(isIp6TakenTimes)).isIp6Taken(network, requestedIpv6);
        Mockito.verify(ipAddressManager, Mockito.times(isIpEqualsGatewayTimes)).isIpEqualsGatewayOrNetworkOfferingsEmpty(network, requestedIpv6);
    }

    private String setCheckIfCanAllocateIpv6AddresscTest(String requestedIpv6, boolean isIp6Taken, boolean isIpEqualsGatewayOrNetworkOfferingsEmpty) {
        Mockito.doReturn(isIp6Taken).when(ip6Manager).isIp6Taken(Mockito.eq(network), Mockito.anyString());
        Mockito.doReturn(isIpEqualsGatewayOrNetworkOfferingsEmpty).when(ipAddressManager).isIpEqualsGatewayOrNetworkOfferingsEmpty(network, requestedIpv6);
        NetUtils.isIp6InNetwork(requestedIpv6, network.getIp6Cidr());
        return requestedIpv6;
    }

    @Test
    public void acquireGuestIpv6AddressTest() throws InsufficientAddressCapacityException {
        setAcquireGuestIpv6AddressTest(true, State.Free);
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8::10", false, false);

        ip6Manager.acquireGuestIpv6Address(network, requestedIpv6);

        verifyAcquireGuestIpv6AddressTest();
    }

    private void verifyAcquireGuestIpv6AddressTest() {
        Mockito.verify(networkModel).areThereIPv6AddressAvailableInNetwork(Mockito.anyLong());
        Mockito.verify(networkModel).checkRequestedIpAddresses(Mockito.anyLong(), Mockito.any(IpAddresses.class));
        Mockito.verify(ipAddressDao).findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString());
    }

    @Test(expected = InsufficientAddressCapacityException.class)
    public void acquireGuestIpv6AddressTestUnavailableIp() throws InsufficientAddressCapacityException {
        setAcquireGuestIpv6AddressTest(false, State.Free);
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8::10", false, false);

        ip6Manager.acquireGuestIpv6Address(network, requestedIpv6);

        verifyAcquireGuestIpv6AddressTest();
    }

    @Test(expected = InsufficientAddressCapacityException.class)
    public void acquireGuestIpv6AddressTestStateAllocating() throws InsufficientAddressCapacityException {
        setAcquireGuestIpv6AddressTest(false, State.Allocating);
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8::10", false, false);

        ip6Manager.acquireGuestIpv6Address(network, requestedIpv6);

        verifyAcquireGuestIpv6AddressTest();
    }

    @Test(expected = InsufficientAddressCapacityException.class)
    public void acquireGuestIpv6AddressTestStateAllocated() throws InsufficientAddressCapacityException {
        setAcquireGuestIpv6AddressTest(false, State.Allocated);
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8::10", false, false);

        ip6Manager.acquireGuestIpv6Address(network, requestedIpv6);

        verifyAcquireGuestIpv6AddressTest();
    }

    @Test(expected = InsufficientAddressCapacityException.class)
    public void acquireGuestIpv6AddressTestStateReleasing() throws InsufficientAddressCapacityException {
        setAcquireGuestIpv6AddressTest(false, State.Releasing);
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8::10", false, false);

        ip6Manager.acquireGuestIpv6Address(network, requestedIpv6);

        verifyAcquireGuestIpv6AddressTest();
    }

    private void setAcquireGuestIpv6AddressTest(boolean isIPAvailable, State state) {
        mockNetwork();
        IPAddressVO ipVo = Mockito.mock(IPAddressVO.class);
        Mockito.doReturn(isIPAvailable).when(networkModel).areThereIPv6AddressAvailableInNetwork(Mockito.anyLong());
        Mockito.doReturn(ipVo).when(ipAddressDao).findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString());
        Mockito.when(ipVo.getState()).thenReturn(state);
    }

    @Test
    public void setNICIPv6AddressTest() throws InsufficientAddressCapacityException {
        NicProfile nic = new NicProfile();
        Network network = mock(Network.class);
        DataCenter dc = mock(DataCenter.class);

        nic.setMacAddress("1e:00:b1:00:0a:f6");

        Mockito.when(network.getIp6Cidr()).thenReturn("2001:db8:100::/64");
        Mockito.when(network.getIp6Gateway()).thenReturn("2001:db8:100::1");

        Mockito.when(networkModel.getNetworkIp6Dns(network, dc)).thenReturn(new Pair<>("2001:db8::53:1", "2001:db8::53:2"));

        String expected = "2001:db8:100:0:1c00:b1ff:fe00:af6";

        ip6Manager.setNicIp6Address(nic, dc, network);

        Assert.assertEquals(expected, nic.getIPv6Address());
    }

    @Test(expected = InsufficientAddressCapacityException.class)
    public void acquireGuestIpv6AddressEUI64Test() throws InsufficientAddressCapacityException {
        setAcquireGuestIpv6AddressTest(true, State.Free);
        String requestedIpv6 = setCheckIfCanAllocateIpv6AddresscTest("2001:db8:13f::1c00:4aff:fe00:fe", false, false);
        ip6Manager.acquireGuestIpv6Address(network, requestedIpv6);
    }
}
