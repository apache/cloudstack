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
package org.apache.cloudstack.network;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class BgpPeerTOTest {

    private static Long peerId = 100L;
    private static String ip4Address = "ip4-address";
    private static String ip6Address = "ip6-address";
    private static Long peerAsNumber = 15000L;
    private static String peerPassword = "peer-password";
    private static Long networkId = 200L;
    private static Long networkAsNumber = 20000L;
    private static String guestIp4Cidr = "10.10.10.0/24";
    private static String guestIp6Cidr = "fd00:1111:2222:3333::1/64";

    @Test
    public void testBgpPeerTO1() {
        BgpPeerTO bgpPeerTO = new BgpPeerTO(networkId);

        Assert.assertEquals(networkId, bgpPeerTO.getNetworkId());
    }

    @Test
    public void testBgpPeerTO2() {
        Map<BgpPeer.Detail, String> details = new HashMap<>();
        details.put(BgpPeer.Detail.EBGP_MultiHop, "100");

        BgpPeerTO bgpPeerTO = new BgpPeerTO(peerId, ip4Address, ip6Address, peerAsNumber, peerPassword,
                networkId, networkAsNumber, guestIp4Cidr, guestIp6Cidr, details);

        Assert.assertEquals(peerId, bgpPeerTO.getPeerId());
        Assert.assertEquals(peerAsNumber, bgpPeerTO.getPeerAsNumber());
        Assert.assertEquals(ip4Address, bgpPeerTO.getIp4Address());
        Assert.assertEquals(ip6Address, bgpPeerTO.getIp6Address());
        Assert.assertEquals(peerPassword, bgpPeerTO.getPeerPassword());
        Assert.assertEquals(networkId, bgpPeerTO.getNetworkId());
        Assert.assertEquals(networkAsNumber, bgpPeerTO.getNetworkAsNumber());
        Assert.assertEquals(guestIp4Cidr, bgpPeerTO.getGuestIp4Cidr());
        Assert.assertEquals(guestIp6Cidr, bgpPeerTO.getGuestIp6Cidr());

        Assert.assertNotNull(bgpPeerTO.getDetails());
        details = bgpPeerTO.getDetails();
        Assert.assertEquals(1, details.size());
        Assert.assertEquals("100", details.get(BgpPeer.Detail.EBGP_MultiHop));
    }
}
