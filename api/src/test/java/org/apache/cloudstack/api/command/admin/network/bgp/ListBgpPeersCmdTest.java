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

import org.apache.cloudstack.api.response.BgpPeerResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.network.BgpPeer;
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
public class ListBgpPeersCmdTest {

    RoutedIpv4Manager routedIpv4Manager = Mockito.spy(RoutedIpv4Manager.class);

    @Test
    public void testIsDedicated() {
        ListBgpPeersCmd cmd = new ListBgpPeersCmd();

        Assert.assertNull(cmd.getDedicated());

        ReflectionTestUtils.setField(cmd, "isDedicated", Boolean.TRUE);
        Assert.assertTrue(cmd.getDedicated());

        ReflectionTestUtils.setField(cmd, "isDedicated", Boolean.FALSE);
        Assert.assertFalse(cmd.getDedicated());
    }

    @Test
    public void testListBgpPeersCmd() {
        Long id = 1L;
        Long zoneId = 2L;
        Long peerAsNumber = 15000L;
        String accountName = "user";
        Long projectId = 10L;
        Long domainId = 11L;

        ListBgpPeersCmd cmd = new ListBgpPeersCmd();
        ReflectionTestUtils.setField(cmd, "id", id);
        ReflectionTestUtils.setField(cmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(cmd, "asNumber", peerAsNumber);
        ReflectionTestUtils.setField(cmd, "accountName", accountName);
        ReflectionTestUtils.setField(cmd,"projectId", projectId);
        ReflectionTestUtils.setField(cmd,"domainId", domainId);
        ReflectionTestUtils.setField(cmd,"routedIpv4Manager", routedIpv4Manager);

        Assert.assertEquals(id, cmd.getId());
        Assert.assertEquals(zoneId, cmd.getZoneId());
        Assert.assertEquals(peerAsNumber, cmd.getAsNumber());
        Assert.assertEquals(accountName, cmd.getAccountName());
        Assert.assertEquals(projectId, cmd.getProjectId());
        Assert.assertEquals(domainId, cmd.getDomainId());

        Assert.assertEquals(0L, cmd.getEntityOwnerId());

        BgpPeer bgpPeer = Mockito.mock(BgpPeer.class);
        List<BgpPeer> bgpPeers = Arrays.asList(bgpPeer);
        Mockito.when(routedIpv4Manager.listBgpPeers(cmd)).thenReturn(bgpPeers);

        BgpPeerResponse response = Mockito.mock(BgpPeerResponse.class);
        Mockito.when(routedIpv4Manager.createBgpPeerResponse(bgpPeer)).thenReturn(response);

        try {
            cmd.execute();
        } catch (Exception ignored) {
        }

        Assert.assertTrue(cmd.getResponseObject() instanceof ListResponse);
        ListResponse listResponse = (ListResponse) cmd.getResponseObject();
        Assert.assertEquals(1, (int) listResponse.getCount());
        Assert.assertEquals(response, listResponse.getResponses().get(0));
    }
}
