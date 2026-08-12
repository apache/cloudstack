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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.cloudstack.extension.ExtensionHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;

public class NetworkExtensionGuestNetworkGuruTest {

    private final NetworkExtensionGuestNetworkGuru guru = new NetworkExtensionGuestNetworkGuru();

    @Mock
    private NetworkOffering offering;
    @Mock
    private PhysicalNetworkVO physicalNetwork;
    @Mock
    private NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Mock
    private ExtensionHelper extensionHelper;

    private AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        guru.networkOfferingServiceMapDao = networkOfferingServiceMapDao;
        guru.extensionHelper = extensionHelper;

        when(offering.getId()).thenReturn(42L);
        when(offering.getTrafficType()).thenReturn(TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(GuestType.Isolated);
        when(offering.isSystemOnly()).thenReturn(false);
        when(physicalNetwork.getId()).thenReturn(7L);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void canHandleWhenOfferingUsesStaticNetworkExtensionProvider() {
        String provider = Network.Provider.NetworkExtension.getName();
        when(networkOfferingServiceMapDao.getDistinctProviders(offering.getId())).thenReturn(List.of(provider));
        when(extensionHelper.isNetworkExtensionProvider(provider)).thenReturn(true);
        when(extensionHelper.usesNetworkExtensionIsolation(provider)).thenReturn(true);

        assertTrue(guru.canHandle(offering, NetworkType.Advanced, physicalNetwork));
    }

    @Test
    public void canHandleWhenOfferingUsesDynamicExtensionProvider() {
        when(networkOfferingServiceMapDao.getDistinctProviders(offering.getId())).thenReturn(List.of("my-ext"));
        when(extensionHelper.isNetworkExtensionProvider("my-ext")).thenReturn(true);
        when(extensionHelper.usesNetworkExtensionIsolation("my-ext")).thenReturn(true);

        assertTrue(guru.canHandle(offering, NetworkType.Advanced, physicalNetwork));
    }

    @Test
    public void cannotHandleWhenIsolationMethodDetailIsNotExtension() {
        when(networkOfferingServiceMapDao.getDistinctProviders(offering.getId())).thenReturn(List.of("my-ext"));
        when(extensionHelper.isNetworkExtensionProvider("my-ext")).thenReturn(true);
        when(extensionHelper.usesNetworkExtensionIsolation("my-ext")).thenReturn(false);

        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physicalNetwork));
    }

    @Test
    public void cannotHandleWhenNoExtensionProvidersArePresent() {
        when(networkOfferingServiceMapDao.getDistinctProviders(offering.getId()))
                .thenReturn(List.of(Network.Provider.VirtualRouter.getName()));
        when(extensionHelper.isNetworkExtensionProvider(Network.Provider.VirtualRouter.getName())).thenReturn(false);

        assertFalse(guru.canHandle(offering, NetworkType.Advanced, physicalNetwork));
    }

}
