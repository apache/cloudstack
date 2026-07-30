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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.vm.NicProfile;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DirectRoutedNetworkGuruTest {

    @InjectMocks
    protected DirectRoutedNetworkGuru guru = new DirectRoutedNetworkGuru();

    @Mock
    DataCenterDao dcDao;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;

    @Mock
    NetworkOffering offering;
    @Mock
    DataCenterVO dc;
    @Mock
    PhysicalNetworkVO physicalNetwork;
    @Mock
    DeploymentPlan plan;
    @Mock
    Account owner;

    @Before
    public void setUp() {
        lenient().when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        lenient().when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        lenient().when(offering.getGuestType()).thenReturn(GuestType.L3);
        lenient().when(plan.getDataCenterId()).thenReturn(1L);
        lenient().when(plan.getPhysicalNetworkId()).thenReturn(1L);
        lenient().when(dcDao.findById(1L)).thenReturn(dc);
        lenient().when(physicalNetworkDao.findById(1L)).thenReturn(physicalNetwork);
    }

    @Test
    public void canHandleAcceptsL3InAdvancedZone() {
        assertTrue(guru.canHandle(offering, dc, physicalNetwork));
    }

    @Test
    public void canHandleRejectsBasicZone() {
        when(dc.getNetworkType()).thenReturn(NetworkType.Basic);
        assertFalse(guru.canHandle(offering, dc, physicalNetwork));
    }

    @Test
    public void canHandleRejectsOtherGuestTypes() {
        for (GuestType type : new GuestType[] {GuestType.Shared, GuestType.Isolated, GuestType.L2}) {
            when(offering.getGuestType()).thenReturn(type);
            assertFalse("guru must not claim guest type " + type, guru.canHandle(offering, dc, physicalNetwork));
        }
    }

    @Test
    public void designProducesNativeStaticNetwork() {
        Network network = guru.design(offering, plan, null, "test", null, owner);
        assertNotNull(network);
        NetworkVO config = (NetworkVO)network;
        assertEquals(BroadcastDomainType.Native, config.getBroadcastDomainType());
        assertEquals(Mode.Static, config.getMode());
        assertNull(config.getBroadcastUri());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void designRejectsVpc() {
        guru.design(offering, plan, null, "test", 42L, owner);
    }

    @Test
    public void designReturnsNullForOtherGuestTypes() {
        when(offering.getGuestType()).thenReturn(GuestType.Shared);
        assertNull(guru.design(offering, plan, null, "test", null, owner));
    }

    @Test
    public void applyDirectRoutedAddressingForcesHostRouteForm() {
        NicProfile nic = new NicProfile();
        nic.setIPv4Address("203.0.113.55");
        nic.setIPv4Netmask("255.255.255.0");
        nic.setIPv4Gateway("203.0.113.1");
        nic.setIPv6Address("2001:db8:1::55");
        nic.setIPv6Cidr("2001:db8:1::/64");
        nic.setIPv6Gateway("2001:db8:1::1");

        guru.applyDirectRoutedAddressing(nic);

        assertEquals("255.255.255.255", nic.getIPv4Netmask());
        assertEquals("169.254.0.1", nic.getIPv4Gateway());
        assertEquals("2001:db8:1::55/128", nic.getIPv6Cidr());
        assertEquals("fe80::1", nic.getIPv6Gateway());
    }

    @Test
    public void applyDirectRoutedAddressingLeavesAbsentFamiliesAlone() {
        NicProfile nic = new NicProfile();
        nic.setIPv4Address("203.0.113.55");
        nic.setIPv4Netmask("255.255.255.0");
        nic.setIPv4Gateway("203.0.113.1");

        guru.applyDirectRoutedAddressing(nic);

        assertEquals("255.255.255.255", nic.getIPv4Netmask());
        assertEquals("169.254.0.1", nic.getIPv4Gateway());
        assertNull(nic.getIPv6Cidr());
        assertNull(nic.getIPv6Gateway());
    }

    @Test
    public void applyDirectRoutedAddressingIsNullSafe() {
        guru.applyDirectRoutedAddressing(null);
    }
}
