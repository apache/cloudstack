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

package org.apache.cloudstack.network.contrail.model;

import com.cloud.network.Network;
import com.cloud.network.dao.NetworkVO;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockNetworkVO {

    private NetworkVO network;

    MockNetworkVO(Network.State state) {
        network = mock(NetworkVO.class);
        when(network.getName()).thenReturn("testnetwork");
        when(network.getState()).thenReturn(state);
        when(network.getGateway()).thenReturn("10.1.1.1");
        when(network.getCidr()).thenReturn("10.1.1.0/24");
        when(network.getPhysicalNetworkId()).thenReturn(42L);
        when(network.getDomainId()).thenReturn(10L);
        when(network.getAccountId()).thenReturn(42L);
    }

    public NetworkVO getNetwork() {
        return network;
    }

    public static NetworkVO getNetwork(Network.State state) {
        MockNetworkVO mockNetwork = new MockNetworkVO(state);
        return mockNetwork.getNetwork();
    }

}
