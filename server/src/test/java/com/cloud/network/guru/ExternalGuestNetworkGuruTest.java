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

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.vm.NicProfile;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class ExternalGuestNetworkGuruTest {
    @Mock
    NetworkModel networkModel;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;

    @InjectMocks
    protected ExternalGuestNetworkGuru guru = new ExternalGuestNetworkGuru();

    final String[] ip4Dns = {"5.5.5.5", "6.6.6.6"};
    final String[] ip6Dns = {"2001:4860:4860::5555", "2001:4860:4860::6666"};

    @Test
    public void testDesignDns() {
        Mockito.when(networkModel.areServicesSupportedByNetworkOffering(Mockito.anyLong(), Mockito.any())).thenReturn(false);
        Mockito.when(networkModel.networkIsConfiguredForExternalNetworking(Mockito.anyLong(), Mockito.anyLong())).thenReturn(true);
        NetworkOffering networkOffering = Mockito.mock(NetworkOffering.class);
        Mockito.when(networkOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        Mockito.when(networkOffering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Mockito.when(zone.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        Mockito.when(dataCenterDao.findById(Mockito.anyLong())).thenReturn(zone);
        PhysicalNetworkVO physicalNetwork = Mockito.mock(PhysicalNetworkVO.class);
        Mockito.when(physicalNetworkDao.findById(Mockito.anyLong())).thenReturn(physicalNetwork);
        DeploymentPlan plan = Mockito.mock(DeploymentPlan.class);
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getDns1()).thenReturn(ip4Dns[0]);
        Mockito.when(network.getDns2()).thenReturn(ip4Dns[1]);
        Mockito.when(network.getIp6Dns1()).thenReturn(ip6Dns[0]);
        Mockito.when(network.getIp6Dns2()).thenReturn(ip6Dns[1]);
        Account owner = Mockito.mock(Account.class);
        Network config = guru.design(networkOffering, plan, network, owner);
        assertNotNull(config);
        assertEquals(ip4Dns[0], config.getDns1());
        assertEquals(ip4Dns[1], config.getDns2());
        assertEquals(ip6Dns[0], config.getIp6Dns1());
        assertEquals(ip6Dns[1], config.getIp6Dns2());
    }

    @Test
    public void testUpdateNicProfile() {
        NicProfile nicProfile = new NicProfile();
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Network network = Mockito.mock(Network.class);
        Mockito.when(dataCenterDao.findById(Mockito.anyLong())).thenReturn(zone);
        Mockito.when(networkModel.getNetworkIp4Dns(network, zone)).thenReturn(new Pair<>(ip4Dns[0], ip4Dns[1]));
        Mockito.when(networkModel.getNetworkIp6Dns(network, zone)).thenReturn(new Pair<>(ip6Dns[0], ip6Dns[1]));
        guru.updateNicProfile(nicProfile, network);
        assertNotNull(nicProfile);
        assertEquals(ip4Dns[0], nicProfile.getIPv4Dns1());
        assertEquals(ip4Dns[1], nicProfile.getIPv4Dns2());
        assertEquals(ip6Dns[0], nicProfile.getIPv6Dns1());
        assertEquals(ip6Dns[1], nicProfile.getIPv6Dns2());
    }

    @Test
    public void testUpdateNetworkProfile() {
        DataCenterVO zone = Mockito.mock(DataCenterVO.class);
        Network network = Mockito.mock(Network.class);
        NetworkProfile profile = new NetworkProfile(network);
        Mockito.when(dataCenterDao.findById(Mockito.anyLong())).thenReturn(zone);
        Mockito.when(networkModel.getNetwork(Mockito.anyLong())).thenReturn(network);
        Mockito.when(networkModel.getNetworkIp4Dns(network, zone)).thenReturn(new Pair<>(ip4Dns[0], null));
        Mockito.when(networkModel.getNetworkIp6Dns(network, zone)).thenReturn(new Pair<>(ip6Dns[0], null));
        guru.updateNetworkProfile(profile);
        assertNotNull(profile);
        assertEquals(ip4Dns[0], profile.getDns1());
        assertNull(profile.getDns2());
        assertEquals(ip6Dns[0], profile.getIp6Dns1());
        assertNull(profile.getIp6Dns2());
    }
}
