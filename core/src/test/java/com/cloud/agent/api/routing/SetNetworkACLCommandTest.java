//
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
//

package com.cloud.agent.api.routing;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.cloud.agent.api.to.NetworkACLTO;
import com.cloud.network.vpc.NetworkACLItem.TrafficType;
import com.google.common.collect.Lists;

public class SetNetworkACLCommandTest {

    @Test
    public void testNetworkAclRuleOrdering(){

        //given
        List<NetworkACLTO> aclList = Lists.newArrayList();

        aclList.add(new NetworkACLTO(3, null, null, null, null, false, false, null, null, null, null, false, 3));
        aclList.add(new NetworkACLTO(1, null, null, null, null, false, false, null, null, null, null, false, 1));
        aclList.add(new NetworkACLTO(2, null, null, null, null, false, false, null, null, null, null, false, 2));

        SetNetworkACLCommand cmd = new SetNetworkACLCommand(aclList, null);

        //when
        cmd.orderNetworkAclRulesByRuleNumber(aclList);

        //then
        for(int i=0; i< aclList.size();i++){
            assertEquals(aclList.get(i).getNumber(), i+1);
        }
    }

    private String generateRule(String protocol, Integer portStart, Integer portEnd, Integer icmpType, Integer icmpCode) {
        NetworkACLTO rule = new NetworkACLTO(1, null, protocol, portStart, portEnd, false, false, Lists.newArrayList("10.0.0.0/24"),
                icmpType, icmpCode, TrafficType.Ingress, true, 1);
        return new SetNetworkACLCommand(Lists.newArrayList(rule), null).generateFwRules()[0][0];
    }

    @Test
    public void testTcpAndUdpByProtocolNumberKeepTheirPorts() {
        assertEquals("Ingress;tcp;22;23;10.0.0.0/24;ACCEPT;", generateRule("6", 22, 23, null, null));
        assertEquals("Ingress;udp;53;53;10.0.0.0/24;ACCEPT;", generateRule("17", 53, 53, null, null));
    }

    @Test
    public void testTcpAndUdpByProtocolNumberWithoutPortsStayWholeProtocol() {
        // a bare number matches the whole protocol on every VR path
        assertEquals("Ingress;6;0;0;10.0.0.0/24;ACCEPT;", generateRule("6", null, null, null, null));
        assertEquals("Ingress;17;0;0;10.0.0.0/24;ACCEPT;", generateRule("17", null, null, null, null));
    }

    @Test
    public void testProtocolNumberInAnotherFormIsReadAsANumber() {
        assertEquals("Ingress;tcp;22;22;10.0.0.0/24;ACCEPT;", generateRule("006", 22, 22, null, null));
        assertEquals("Ingress;icmp;8;0;10.0.0.0/24;ACCEPT;", generateRule(" 01 ", null, null, 8, 0));
        assertEquals("Ingress;47;0;0;10.0.0.0/24;ACCEPT;", generateRule("047", null, null, null, null));
    }

    @Test
    public void testIcmpByProtocolNumberKeepsItsTypeAndCode() {
        assertEquals("Ingress;icmp;8;0;10.0.0.0/24;ACCEPT;", generateRule("1", null, null, 8, 0));
        // a rule updated to protocol 1 may have no ICMP type or code: any
        assertEquals("Ingress;icmp;-1;-1;10.0.0.0/24;ACCEPT;", generateRule("1", null, null, null, null));
    }

    @Test
    public void testIcmpIsMatchedWhateverItsCase() {
        assertEquals("Ingress;icmp;8;0;10.0.0.0/24;ACCEPT;", generateRule("ICMP", null, null, 8, 0));
    }

    @Test
    public void testOtherProtocolNumberIsPassedThrough() {
        assertEquals("Ingress;47;0;0;10.0.0.0/24;ACCEPT;", generateRule("47", null, null, null, null));
        assertEquals("Ingress;all;0;0;10.0.0.0/24;ACCEPT;", generateRule("all", null, null, null, null));
    }
}
