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

package com.cloud.agent.api;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Vector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.SecurityGroupRulesCmd.IpPortAndProto;

/**
 * @author daan
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityGroupRulesCmdTest {
    private SecurityGroupRulesCmd securityGroupRulesCmd;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        final String guestIp = "10.10.10.10";
        final String guestIp6 = "2001:db8::cad:40ff:fefd:75c4";
        final String guestMac = "aa:aa:aa:aa:aa:aa";
        final String vmName = "vm";
        final Long vmId = 1L;
        final String signature = "sig";
        final Long seqNum = 0L;
        final String proto = "abc";
        final int startPort = 1;
        final int endPort = 2;
        final String[] allowedCidrs = new String[] {"1.2.3.4/5","6.7.8.9/0"};
        final IpPortAndProto[] ingressRuleSet = new IpPortAndProto[]{new IpPortAndProto(proto, startPort, endPort, allowedCidrs)};
        final IpPortAndProto[] egressRuleSet = new IpPortAndProto[]{new IpPortAndProto(proto, startPort, endPort, allowedCidrs)};
        final List<String> secIps = new Vector<String>();
        securityGroupRulesCmd = new SecurityGroupRulesCmd(guestIp, guestIp6, guestMac, vmName, vmId, signature, seqNum, ingressRuleSet, egressRuleSet, secIps);
    }

    /**
     * Test method for {@link com.cloud.agent.api.SecurityGroupRulesCmd#stringifyRules()}.
     */
    @Test
    public void testStringifyRules() throws Exception {
        final String a = securityGroupRulesCmd.stringifyRules();
// do verification on a
        assertTrue(a.contains(SecurityGroupRulesCmd.EGRESS_RULE));
    }

    /**
     * Test method for {@link com.cloud.agent.api.SecurityGroupRulesCmd#stringifyCompressedRules()}.
     */
    @Test
    public void testStringifyCompressedRules() throws Exception {
        final String a = securityGroupRulesCmd.stringifyCompressedRules();
// do verification on a
        assertTrue(a.contains(SecurityGroupRulesCmd.EGRESS_RULE));
    }

    /**
     * Test method for {@link com.cloud.agent.api.SecurityGroupRulesCmd#compressStringifiedRules()}.
     */
    @Test
    public void testCompressStringifiedRules() throws Exception {
        final String compressed = "eJzztEpMSrY2tDayNtQz0jPWM9E31THTM9ez0LPUN9Dxc40IUXAlrAQAPusP4w==";
        final String a = securityGroupRulesCmd.compressStringifiedRules();
        assertTrue(compressed.equals(a));
    }

}
