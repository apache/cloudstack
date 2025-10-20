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

package org.apache.cloudstack.api.command.admin.network.bgp;

import com.cloud.event.EventTypes;
import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.network.BgpPeer;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseDedicatedBgpPeerCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    @Test
    public void testReleaseDedicatedBgpPeerCmd() {
        Long id = 1L;

        ReleaseDedicatedBgpPeerCmd cmd = new ReleaseDedicatedBgpPeerCmd();
        ReflectionTestUtils.setField(cmd, "id", id);
        ReflectionTestUtils.setField(cmd,"routedIpv4Manager", routedIpv4Manager);

        Assert.assertEquals(id, cmd.getId());
        Assert.assertEquals(1L, cmd.getEntityOwnerId());
        Assert.assertEquals(EventTypes.EVENT_BGP_PEER_RELEASE, cmd.getEventType());
        Assert.assertEquals(String.format("Releasing a dedicated Bgp Peer %s", id), cmd.getEventDescription());

        BgpPeer bgpPeer = Mockito.mock(BgpPeer.class);
        Mockito.when(routedIpv4Manager.releaseDedicatedBgpPeer(cmd)).thenReturn(bgpPeer);

        BgpPeerResponse response = Mockito.mock(BgpPeerResponse.class);
        Mockito.when(routedIpv4Manager.createBgpPeerResponse(bgpPeer)).thenReturn(response);

        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertEquals(response, cmd.getResponseObject());
    }
}
