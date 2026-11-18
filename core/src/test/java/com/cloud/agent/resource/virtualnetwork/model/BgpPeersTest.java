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
package com.cloud.agent.resource.virtualnetwork.model;

import org.apache.cloudstack.network.BgpPeerTO;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class BgpPeersTest {

    @Test
    public void testBgpPeers() {
        BgpPeerTO bgpPeerTO = Mockito.mock(BgpPeerTO.class);
        List<BgpPeerTO> bgpPeerTOs = new ArrayList<>();
        bgpPeerTOs.add(bgpPeerTO);

        BgpPeers bgpPeers = new BgpPeers(bgpPeerTOs);
        Assert.assertEquals(ConfigBase.BGP_PEERS, bgpPeers.getType());
        Assert.assertNotNull(bgpPeers.getPeers());
        Assert.assertEquals(1, bgpPeers.getPeers().size());
        Assert.assertEquals(bgpPeerTO, bgpPeers.getPeers().get(0));
    }

    @Test
    public void testBgpPeers2() {
        BgpPeers bgpPeers = new BgpPeers();
        Assert.assertEquals(ConfigBase.BGP_PEERS, bgpPeers.getType());

        BgpPeerTO bgpPeerTO = Mockito.mock(BgpPeerTO.class);
        List<BgpPeerTO> bgpPeerTOs = new ArrayList<>();
        bgpPeerTOs.add(bgpPeerTO);
        bgpPeers.setPeers(bgpPeerTOs);

        Assert.assertNotNull(bgpPeers.getPeers());
        Assert.assertEquals(1, bgpPeers.getPeers().size());
        Assert.assertEquals(bgpPeerTO, bgpPeers.getPeers().get(0));
    }
}
