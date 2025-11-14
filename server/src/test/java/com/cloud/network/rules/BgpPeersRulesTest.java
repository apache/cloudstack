// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.network.rules;

import com.cloud.dc.DataCenter;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.router.VirtualRouter;

import org.apache.cloudstack.network.BgpPeer;
import org.apache.cloudstack.network.topology.NetworkTopologyVisitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BgpPeersRulesTest {

    private BgpPeersRules bgpPeersRules;
    private Network mockNetwork;
    private List<BgpPeer> mockBgpPeers;
    private NetworkTopologyVisitor mockVisitor;
    private VirtualRouter mockRouter;

    @Before
    public void setUp() {
        mockNetwork = mock(Network.class);
        BgpPeer peer1 = mock(BgpPeer.class);
        BgpPeer peer2 = mock(BgpPeer.class);
        mockBgpPeers = Arrays.asList(peer1, peer2);

        mockVisitor = mock(NetworkTopologyVisitor.class);
        mockRouter = mock(VirtualRouter.class);

        bgpPeersRules = new BgpPeersRules(mockBgpPeers, mockNetwork);
    }

    @Test
    public void testGetBgpPeers() {
        List<? extends BgpPeer> bgpPeers = bgpPeersRules.getBgpPeers();
        assertNotNull(bgpPeers);
        assertEquals(2, bgpPeers.size());
        assertTrue(bgpPeers.containsAll(mockBgpPeers));
    }

    @Test
    public void testAccept() throws ResourceUnavailableException {
        when(mockVisitor.visit(bgpPeersRules)).thenReturn(true);

        boolean result = bgpPeersRules.accept(mockVisitor, mockRouter);

        assertTrue(result);
        verify(mockVisitor, times(1)).visit(bgpPeersRules);
    }

    @Test
    public void testAcceptThrowsResourceUnavailableException() throws ResourceUnavailableException {
        when(mockVisitor.visit(bgpPeersRules)).thenThrow(new ResourceUnavailableException("Resource Unavailable", DataCenter.class, 1L));

        ResourceUnavailableException thrown = assertThrows(ResourceUnavailableException.class, () -> {
            bgpPeersRules.accept(mockVisitor, mockRouter);
        });

        assertEquals("Resource [DataCenter:1] is unreachable: Resource Unavailable", thrown.getMessage());
        assertEquals(DataCenter.class, thrown.getScope());
        assertEquals(1L, thrown.getResourceId());

        verify(mockVisitor, times(1)).visit(bgpPeersRules);
    }
}
