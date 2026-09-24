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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.db.EntityManager;

@RunWith(Parameterized.class)
public class PrivateNetworkGuruTest {
    @Parameterized.Parameters(name = "{index}: {0}, {1} -> {2}")
    public static Collection<Object[]> networkTypes() {
        return Arrays.asList(new Object[][] {
                {BroadcastDomainType.Vlan, "vxlan://1005002", BroadcastDomainType.Vxlan},
                {BroadcastDomainType.Vlan, "vlan://123", BroadcastDomainType.Vlan},
                {BroadcastDomainType.Native, "vlan://untagged", BroadcastDomainType.Native},
                {BroadcastDomainType.Lswitch, "lswitch://private-network", BroadcastDomainType.Lswitch},
                {BroadcastDomainType.Vlan, null, BroadcastDomainType.Vlan}
        });
    }

    private final BroadcastDomainType suppliedType;
    private final URI broadcastUri;
    private final BroadcastDomainType expectedType;

    public PrivateNetworkGuruTest(BroadcastDomainType suppliedType, String broadcastUri, BroadcastDomainType expectedType) {
        this.suppliedType = suppliedType;
        this.broadcastUri = broadcastUri == null ? null : URI.create(broadcastUri);
        this.expectedType = expectedType;
    }

    @Test
    public void designPreservesNetworkIsolation() {
        PrivateNetworkGuru guru = new PrivateNetworkGuru();
        guru._entityMgr = mock(EntityManager.class);
        DeploymentPlan plan = mock(DeploymentPlan.class);
        DataCenter dc = mock(DataCenter.class);
        NetworkOffering offering = mock(NetworkOffering.class);
        when(plan.getDataCenterId()).thenReturn(1L);
        when(guru._entityMgr.findById(DataCenter.class, 1L)).thenReturn(dc);
        when(dc.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(offering.isSystemOnly()).thenReturn(true);

        NetworkVO suppliedNetwork = new NetworkVO();
        suppliedNetwork.setCidr("10.1.1.0/24");
        suppliedNetwork.setGateway("10.1.1.1");
        suppliedNetwork.setBroadcastDomainType(suppliedType);
        suppliedNetwork.setBroadcastUri(broadcastUri);

        Network network = guru.design(offering, plan, suppliedNetwork, "private-network", 1L, mock(Account.class));

        assertNotNull(network);
        assertEquals(expectedType, network.getBroadcastDomainType());
        assertEquals(broadcastUri, network.getBroadcastUri());
        assertEquals(broadcastUri == null ? Network.State.Allocated : Network.State.Setup, network.getState());
        assertEquals(suppliedNetwork.getCidr(), network.getCidr());
        assertEquals(suppliedNetwork.getGateway(), network.getGateway());
    }
}
