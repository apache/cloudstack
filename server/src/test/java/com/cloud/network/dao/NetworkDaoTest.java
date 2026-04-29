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
package com.cloud.network.dao;

import com.cloud.network.Network;
import junit.framework.TestCase;
import org.junit.Assert;
import org.mockito.Spy;

public class NetworkDaoTest extends TestCase {

    @Spy
    private NetworkDaoImpl dao = new NetworkDaoImpl();

    private static final Integer existingPrimaryVlan = 900;
    private static final Integer existingSecondaryVlan = 901;

    private static final Integer requestedVlan = 902;

    public void testNetworkOverlappingExactPair() {
        Assert.assertTrue(dao.isNetworkOverlappingRequestedPvlan(existingPrimaryVlan, existingSecondaryVlan, Network.PVlanType.Isolated,
                existingPrimaryVlan, existingSecondaryVlan, Network.PVlanType.Isolated));
        Assert.assertTrue(dao.isNetworkOverlappingRequestedPvlan(existingPrimaryVlan, existingSecondaryVlan, Network.PVlanType.Isolated,
                existingPrimaryVlan, existingSecondaryVlan, Network.PVlanType.Community));
    }

    public void testNetworkOverlappingPromiscuous() {
        Assert.assertTrue(dao.isNetworkOverlappingRequestedPvlan(existingPrimaryVlan, existingPrimaryVlan, Network.PVlanType.Promiscuous,
                existingPrimaryVlan, existingPrimaryVlan, Network.PVlanType.Promiscuous));
    }

    public void testNetworkOverlappingIsolated() {
        Assert.assertTrue(dao.isNetworkOverlappingRequestedPvlan(existingPrimaryVlan, existingSecondaryVlan, Network.PVlanType.Isolated,
                existingPrimaryVlan, requestedVlan, Network.PVlanType.Isolated));
    }

    public void testNetworkOverlappingMultipleCommunityAllowed() {
        Assert.assertFalse(dao.isNetworkOverlappingRequestedPvlan(existingPrimaryVlan, existingSecondaryVlan, Network.PVlanType.Community,
                existingPrimaryVlan, requestedVlan, Network.PVlanType.Community));
    }

    public void testNetworkOverlappingVlanPvlanTrue() {
        Assert.assertTrue(dao.isNetworkOverlappingRequestedPvlan(existingPrimaryVlan, existingSecondaryVlan, existingPrimaryVlan));
        Assert.assertTrue(dao.isNetworkOverlappingRequestedPvlan(existingPrimaryVlan, existingSecondaryVlan, existingSecondaryVlan));
    }

    public void testNetworkOverlappingVlanPvlanFalse() {
        Assert.assertFalse(dao.isNetworkOverlappingRequestedPvlan(existingPrimaryVlan, existingSecondaryVlan, requestedVlan));
    }

}
