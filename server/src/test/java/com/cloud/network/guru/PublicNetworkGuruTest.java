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
package com.cloud.network.guru;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Ipv6Service;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PublicNetworkGuruTest {

    @InjectMocks
    protected PublicNetworkGuru guru = new PublicNetworkGuru();

    @Mock
    IpAddressManager ipAddrMgr;
    @Mock
    Ipv6Service ipv6Service;
    @Mock
    NetworkModel networkModel;

    @Mock
    DataCenter dc;
    @Mock
    Network network;
    @Mock
    VirtualMachineProfile vm;
    @Mock
    Account owner;

    private static final long MAC = 0x1e003c000102L;

    @Before
    public void setUp() {
        lenient().when(vm.getType()).thenReturn(VirtualMachine.Type.ConsoleProxy);
        lenient().when(vm.getOwner()).thenReturn(owner);
        lenient().when(networkModel.getNetworkIp4Dns(network, dc)).thenReturn(new Pair<>(null, null));
        lenient().when(networkModel.getNetworkIp6Dns(network, dc)).thenReturn(new Pair<>(null, null));
    }

    private PublicIp publicIp(String vlanTag, String ip6Cidr) {
        IPAddressVO addr = new IPAddressVO(new Ip("2.57.59.68"), 1L, 5L, 1L, false);
        VlanVO vlan = new VlanVO(VlanType.VirtualNetwork, vlanTag, "2.57.59.65", "255.255.255.192", 1L,
                "2.57.59.66-2.57.59.94", 200L, 200L, ip6Cidr == null ? null : "2a00:f10:402:2::1", ip6Cidr, null);
        return new PublicIp(addr, vlan, MAC);
    }

    private NicProfile allocateOn(PublicIp ip) throws Exception {
        when(ipAddrMgr.assignPublicIpAddress(anyLong(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(ip);
        NicProfile nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        guru.getIp(nic, dc, vm, network);
        return nic;
    }

    /**
     * Regression: setRoutedRangeIpv6() runs inside getIp()'s routed branch and needs the NIC's
     * MAC (EUI-64) — the MAC must be on the profile before that branch, not after it (NPE), and
     * the DualStack format it sets must not be clobbered back to Ip4 afterwards.
     */
    @Test
    public void getIpOnRoutedRangeComputesEui64Ipv6FromNicMac() throws Exception {
        PublicIp ip = publicIp("routed://534", "2a00:f10:402:2::/64");
        NicProfile nic = allocateOn(ip);

        assertEquals(ip.getMacAddress(), nic.getMacAddress());
        assertEquals("2.57.59.68", nic.getIPv4Address());
        assertEquals(NetUtils.IPV4_HOST_NETMASK, nic.getIPv4Netmask());
        assertEquals(NetUtils.getLinkLocalGateway(), nic.getIPv4Gateway());
        assertEquals("routed://534", nic.getBroadCastUri().toString());
        assertEquals(BroadcastDomainType.Routed, nic.getBroadcastType());
        assertEquals(NetUtils.EUI64Address("2a00:f10:402:2::/64", ip.getMacAddress()).toString(), nic.getIPv6Address());
        assertEquals(NetUtils.getIpv6LinkLocalGateway(), nic.getIPv6Gateway());
        assertEquals(AddressFormat.DualStack, nic.getFormat());
    }

    @Test
    public void getIpOnVlanRangeKeepsClassicShape() throws Exception {
        when(network.getBroadcastDomainType()).thenReturn(BroadcastDomainType.Vlan);
        PublicIp ip = publicIp("50", null);
        NicProfile nic = allocateOn(ip);

        assertEquals(ip.getMacAddress(), nic.getMacAddress());
        assertEquals("2.57.59.68", nic.getIPv4Address());
        assertEquals("255.255.255.192", nic.getIPv4Netmask());
        assertEquals("2.57.59.65", nic.getIPv4Gateway());
        assertEquals("vlan://50", nic.getBroadCastUri().toString());
        assertNull(nic.getIPv6Address());
        assertEquals(AddressFormat.Ip4, nic.getFormat());
    }
}
