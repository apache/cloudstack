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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

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
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        String guestIp = "10.10.10.10";
        String guestMac = "aa:aa:aa:aa:aa:aa";
        String vmName = "vm";
        Long vmId = 1L;
        String signature = "sig";
        Long seqNum = 0L;
        String proto = "abc";
        int startPort = 1;
        int endPort = 2;
        String[] allowedCidrs = new String[] {"1.2.3.4/5","6.7.8.9/0"};
        IpPortAndProto[] ingressRuleSet = new IpPortAndProto[]{new IpPortAndProto(proto, startPort, endPort, allowedCidrs)};
        IpPortAndProto[] egressRuleSet = new IpPortAndProto[]{new IpPortAndProto(proto, startPort, endPort, allowedCidrs)};
        List<String> secIps = new Vector<String>();
        securityGroupRulesCmd = new SecurityGroupRulesCmd(guestIp, guestMac, vmName, vmId, signature, seqNum, ingressRuleSet, egressRuleSet, secIps);
    }

    /**
     * Test method for {@link com.cloud.agent.api.SecurityGroupRulesCmd#stringifyRules()}.
     */
    @Test
    public void testStringifyRules() throws Exception {
        String a = securityGroupRulesCmd.stringifyRules();
// do verification on a
        assertTrue(a.contains(SecurityGroupRulesCmd.EGRESS_RULE));
    }

    /**
     * Test method for {@link com.cloud.agent.api.SecurityGroupRulesCmd#stringifyCompressedRules()}.
     */
    @Test
    public void testStringifyCompressedRules() throws Exception {
        String a = securityGroupRulesCmd.stringifyCompressedRules();
// do verification on a
        assertTrue(a.contains(SecurityGroupRulesCmd.EGRESS_RULE));
    }

    /**
     * Test method for {@link com.cloud.agent.api.SecurityGroupRulesCmd#compressStringifiedRules()}.
     */
    @Test
    public void testCompressStringifiedRules() throws Exception {
        String compressed = "eJzztEpMSrYytDKyMtQz0jPWM9E31THTM9ez0LPUN9Dxc40IUXAlrAQAPdoP3Q==";
        String a = securityGroupRulesCmd.compressStringifiedRules();
        assertTrue(compressed.equals(a));
    }

}
