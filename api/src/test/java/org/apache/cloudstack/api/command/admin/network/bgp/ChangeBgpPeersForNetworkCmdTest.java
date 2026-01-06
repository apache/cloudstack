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

import com.cloud.network.Network;
import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.network.RoutedIpv4Manager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ChangeBgpPeersForNetworkCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    ResponseGenerator _responseGenerator = Mockito.spy(ResponseGenerator.class);

    @Test
    public void testChangeBgpPeersForNetworkCmd() {
        Long networkId = 10L;
        List<Long> bgpPeerIds = Arrays.asList(20L, 21L);

        ChangeBgpPeersForNetworkCmd cmd = new ChangeBgpPeersForNetworkCmd();
        ReflectionTestUtils.setField(cmd, "networkId", networkId);
        ReflectionTestUtils.setField(cmd, "bgpPeerIds", bgpPeerIds);
        ReflectionTestUtils.setField(cmd,"routedIpv4Manager", routedIpv4Manager);
        ReflectionTestUtils.setField(cmd,"_responseGenerator", _responseGenerator);

        Assert.assertEquals(networkId, cmd.getNetworkId());
        Assert.assertEquals(bgpPeerIds, cmd.getBgpPeerIds());
        Assert.assertEquals(1L, cmd.getEntityOwnerId());
        Assert.assertEquals(EventTypes.EVENT_NETWORK_BGP_PEER_UPDATE, cmd.getEventType());
        Assert.assertEquals(String.format("Changing Bgp Peers for network %s", networkId), cmd.getEventDescription());

        Network network = Mockito.mock(Network.class);
        Mockito.when(routedIpv4Manager.changeBgpPeersForNetwork(cmd)).thenReturn(network);

        NetworkResponse response = Mockito.mock(NetworkResponse.class);
        Mockito.when(_responseGenerator.createNetworkResponse(ResponseObject.ResponseView.Full, network)).thenReturn(response);

        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertEquals(response, cmd.getResponseObject());
    }
}
