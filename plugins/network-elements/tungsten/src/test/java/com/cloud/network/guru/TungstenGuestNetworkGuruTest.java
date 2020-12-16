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
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import org.apache.cloudstack.network.tungsten.service.TungstenGuestNetworkGuru;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TungstenGuestNetworkGuruTest {

    @Mock
    NetworkOfferingServiceMapDao ntwkOfferingSrvcDao;
    @Mock
    DataCenterVO dc;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    NetworkOffering offering;
    @Mock
    DataCenterDao dcDao;
    @Mock
    DeploymentPlan plan;
    @Mock
    PhysicalNetworkVO physicalNetwork;
    @Mock
    NetworkModel networkModel;

    TungstenGuestNetworkGuru guru;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        guru = new TungstenGuestNetworkGuru();
        guru._ntwkOfferingSrvcDao = ntwkOfferingSrvcDao;
        guru._physicalNetworkDao = physicalNetworkDao;
        guru._dcDao = dcDao;
        guru._networkModel = networkModel;

        when(dc.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        when(dc.getGuestNetworkCidr()).thenReturn("10.1.1.1/24");
        when(dc.getId()).thenReturn(1l);
        when(dcDao.findById((Long)any())).thenReturn(dc);

        when(physicalNetwork.getId()).thenReturn(1l);
        when(physicalNetwork.getIsolationMethods()).thenReturn(Arrays.asList("TF"));
        when(physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetwork);

        when(offering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(offering.getId()).thenReturn(1l);

        when(ntwkOfferingSrvcDao.isProviderForNetworkOffering(offering.getId(), Network.Provider.Tungsten)).thenReturn(true);

        when(plan.getDataCenterId()).thenReturn(1l);
        when(plan.getPhysicalNetworkId()).thenReturn(1l);
    }

    @Test
    public void testIsMyIsolationMethod() {
        assertTrue(guru.isMyIsolationMethod(physicalNetwork));
    }

    @Test
    public void testIsolationMethods() {
        PhysicalNetwork.IsolationMethod[] expected = new PhysicalNetwork.IsolationMethod[] { new PhysicalNetwork.IsolationMethod("TF") };
        assertEquals(expected, guru.getIsolationMethods());
    }

    @Test
    public void testTrafficTypes() {
        assertTrue(guru.isMyTrafficType(Networks.TrafficType.Guest));
    }

    @Test
    public void testCanHandle() {
        assertTrue(guru.canHandle(offering, dc.getNetworkType(), physicalNetwork));
    }

    @Test
    public void testCanDesign() {
        List<Network.Service> networkOfferingServiceList = new ArrayList<>(Arrays.asList(Network.Service.Connectivity, Network.Service.Dns,
                Network.Service.Dhcp, Network.Service.SourceNat, Network.Service.StaticNat, Network.Service.UserData));
        when(networkModel.listNetworkOfferingServices(anyLong())).thenReturn(networkOfferingServiceList);

        final Network network = mock(Network.class);
        final Account account = mock(Account.class);

        final Network designedNetwork = guru.design(offering, plan, network, account);
        assertTrue(designedNetwork != null);
        assertTrue(designedNetwork.getBroadcastDomainType() == Networks.BroadcastDomainType.Tungsten);
    }
}
