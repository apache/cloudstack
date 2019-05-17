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

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.Network.GuestType;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class DirectNetworkGuruTest {

    protected DirectNetworkGuru guru = new DirectNetworkGuru();

    @Mock
    PhysicalNetworkVO physicalNetwork;

    @Mock
    DataCenterVO dc;

    @Mock
    NetworkOffering offering;

    @Mock
    NetworkOfferingServiceMapDao ntwkOfferingSrvcDao;

    @Mock
    DataCenterDao dcDao;

    @Mock
    PhysicalNetworkDao physicalNetworkDao;

    @Mock
    Network network;

    @Mock
    NetworkModel networkModel;

    @Mock
    DeploymentPlan plan;

    @Mock
    Account owner;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        guru._ntwkOfferingSrvcDao = ntwkOfferingSrvcDao;
        guru._dcDao = dcDao;
        guru._physicalNetworkDao = physicalNetworkDao;
        guru._networkModel = networkModel;

        when(physicalNetwork.getId()).thenReturn(1l);
        when(physicalNetwork.getIsolationMethods()).thenReturn(Arrays.asList("VXLAN", "VLAN"));

        when(dc.getNetworkType()).thenReturn(NetworkType.Advanced);
        when(dc.getId()).thenReturn(1l);
        when(offering.getGuestType()).thenReturn(GuestType.Shared);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getId()).thenReturn(42l);
        when(ntwkOfferingSrvcDao.isProviderForNetworkOffering(offering.getId(), Network.Provider.NiciraNvp)).thenReturn(false);
    }

    @Test
    public void testIsMyIsolationMethod() {
        assertTrue(guru.isMyIsolationMethod(physicalNetwork));
    }

    @Test
    public void testIsolationMethods() {
        IsolationMethod[] expected = new IsolationMethod[] { new IsolationMethod("VLAN"), new IsolationMethod("VXLAN") };
        assertEquals(expected, guru.getIsolationMethods());
    }

    @Test
    public void testTrafficTypes() {
        assertTrue(guru.isMyTrafficType(TrafficType.Guest));
    }

    @Test
    public void testCanHandle() {
        assertTrue(guru.canHandle(offering, dc, physicalNetwork));
    }

    @Test
    public void testCanDesign() {
        when(dcDao.findById(dc.getId())).thenReturn(dc);
        when(plan.getDataCenterId()).thenReturn(1l);
        when(plan.getPhysicalNetworkId()).thenReturn(1l);
        when(physicalNetworkDao.findById(physicalNetwork.getId())).thenReturn(physicalNetwork);
        when(offering.isRedundantRouter()).thenReturn(false);

        when(networkModel.areServicesSupportedByNetworkOffering(offering.getId(), Network.Service.SecurityGroup)).thenReturn(true);

        assertNotNull(guru.design(offering, plan, network, owner));
    }
}
