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
package com.cloud.agent.api.routing;

import org.apache.cloudstack.network.BgpPeerTO;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class SetBgpPeersCommandTest {

    @Test
    public void testSetBgpPeersCommand1() {
        SetBgpPeersCommand command = new SetBgpPeersCommand();
        Assert.assertNull(command.getBpgPeers());
    }

    @Test
    public void testSetBgpPeersCommand2() {
        BgpPeerTO bgpPeerTO = Mockito.mock(BgpPeerTO.class);

        List<BgpPeerTO> bgpPeerTOs = new ArrayList<>();
        bgpPeerTOs.add(bgpPeerTO);

        SetBgpPeersCommand command = new SetBgpPeersCommand(bgpPeerTOs);
        Assert.assertNotNull(command.getBpgPeers());
        Assert.assertEquals(1, command.getBpgPeers().length);
        Assert.assertEquals(bgpPeerTO, command.getBpgPeers()[0]);
    }
}
