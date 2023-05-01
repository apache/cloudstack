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

package com.cloud.network;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;

import java.util.List;
import java.util.ArrayList;

/**
 * @author dhoogland
 *
 */
public class HAProxyConfiguratorTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link com.cloud.network.HAProxyConfigurator#generateConfiguration(com.cloud.agent.api.routing.LoadBalancerConfigCommand)}.
     */
    @Test
    public void testGenerateConfigurationLoadBalancerConfigCommand() {
        LoadBalancerTO lb = new LoadBalancerTO("1", "10.2.0.1", 80, "http", "bla", false, false, false, null);
        LoadBalancerTO[] lba = new LoadBalancerTO[1];
        lba[0] = lb;
        HAProxyConfigurator hpg = new HAProxyConfigurator();
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "12", false);
        String result = genConfig(hpg, cmd);
        assertTrue("keepalive disabled should result in 'option httpclose' in the resulting haproxy config", result.contains("option httpclose"));

        cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "4", true);
        result = genConfig(hpg, cmd);
        assertTrue("keepalive enabled should not result in 'option httpclose' in the resulting haproxy config", result.contains("no option httpclose"));
        // TODO
        // create lb command
        // setup tests for
        // maxconn (test for maxpipes as well)
        // httpmode
    }

    /**
     * Test method for {@link com.cloud.network.HAProxyConfigurator#generateConfiguration(com.cloud.agent.api.routing.LoadBalancerConfigCommand)}.
     */
    @Test
    public void testGenerateConfigurationLoadBalancerProxyProtocolConfigCommand() {
        final List<LbDestination> dests = new ArrayList<>();
        dests.add(new LbDestination(443, 8443, "10.1.10.2", false));
        dests.add(new LbDestination(443, 8443, "10.1.10.2", true));
        LoadBalancerTO lb = new LoadBalancerTO("1", "10.2.0.1", 443, "tcp", "http", false, false, false, dests);
        lb.setLbProtocol("tcp-proxy");
        LoadBalancerTO[] lba = new LoadBalancerTO[1];
        lba[0] = lb;
        HAProxyConfigurator hpg = new HAProxyConfigurator();
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "12", false);
        String result = genConfig(hpg, cmd);
        assertTrue("'send-proxy' should result if protocol is 'tcp-proxy'", result.contains("send-proxy"));
    }

    @Test
    public void generateConfigurationTestWithCidrList() {
        LoadBalancerTO lb = new LoadBalancerTO("1", "10.2.0.1", 22, "tcp", "roundrobin", false, false, false, null, null);
        lb.setCidrList("1.1.1.1 2.2.2.2/24");
        LoadBalancerTO[] lba = new LoadBalancerTO[1];
        lba[0] = lb;
        HAProxyConfigurator hpg = new HAProxyConfigurator();
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "12", false);
        String result = genConfig(hpg, cmd);
        Assert.assertTrue(result.contains("acl network_allowed src 1.1.1.1 2.2.2.2/24 \n\ttcp-request connection reject if !network_allowed"));
    }

    private String genConfig(HAProxyConfigurator hpg, LoadBalancerConfigCommand cmd) {
        String[] sa = hpg.generateConfiguration(cmd);
        StringBuilder sb = new StringBuilder();
        for (String s : sa) {
            sb.append(s).append('\n');
        }
        return sb.toString();
    }

}
