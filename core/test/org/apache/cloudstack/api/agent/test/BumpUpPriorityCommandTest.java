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
package org.apache.cloudstack.api.agent.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;

public class BumpUpPriorityCommandTest {

    BumpUpPriorityCommand bupc = new BumpUpPriorityCommand();

    // test super class
    @Test
    public void testSuperGetAccessDetail() {
        String value;
        bupc.setAccessDetail(NetworkElementCommand.ACCOUNT_ID, "accountID");
        value = bupc.getAccessDetail(NetworkElementCommand.ACCOUNT_ID);
        assertTrue(value.equals("accountID"));

        bupc.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR,
                "GuestNetworkCIDR");
        value = bupc.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR);
        assertTrue(value.equals("GuestNetworkCIDR"));

        bupc.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY,
                "GuestNetworkGateway");
        value = bupc
                .getAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY);
        assertTrue(value.equals("GuestNetworkGateway"));

        bupc.setAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG,
                "GuestVlanTag");
        value = bupc.getAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG);
        assertTrue(value.equals("GuestVlanTag"));

        bupc.setAccessDetail(NetworkElementCommand.ROUTER_NAME, "RouterName");
        value = bupc.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        assertTrue(value.equals("RouterName"));

        bupc.setAccessDetail(NetworkElementCommand.ROUTER_IP, "RouterIP");
        value = bupc.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        assertTrue(value.equals("RouterIP"));

        bupc.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP,
                "RouterGuestIP");
        value = bupc.getAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP);
        assertTrue(value.equals("RouterGuestIP"));

        bupc.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE,
                "ZoneNetworkType");
        value = bupc.getAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE);
        assertTrue(value.equals("ZoneNetworkType"));

        bupc.setAccessDetail(NetworkElementCommand.GUEST_BRIDGE, "GuestBridge");
        value = bupc.getAccessDetail(NetworkElementCommand.GUEST_BRIDGE);
        assertTrue(value.equals("GuestBridge"));
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = bupc.executeInSequence();
        assertFalse(b);
    }
}
