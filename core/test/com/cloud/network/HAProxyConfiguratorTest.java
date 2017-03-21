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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import static com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.utils.Pair;

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
        assertTrue("keepalive disabled should result in 'mode http' in the resulting haproxy config", result.contains("mode http"));

        cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "4", true);
        result = genConfig(hpg, cmd);
        assertTrue("keepalive enabled should not result in 'mode http' in the resulting haproxy config", !result.contains("mode http"));
        // TODO
        // create lb command
        // setup tests for
        // maxconn (test for maxpipes as well)
        // httpmode
    }

    @Test
    public void testGenerateConfigurationLoadBalancerHealthCheckCommand() {
        List<LbHealthCheckPolicy> healthCheckPolicies = Arrays.asList(new LbHealthCheckPolicy("/", "health", 5, 5, 5, 5));
        LoadBalancerTO lb = new LoadBalancerTO("1", "10.2.0.1", 80, "http", "bla", false, false, false, null, null, healthCheckPolicies, null, null);
        LoadBalancerTO[] lba = new LoadBalancerTO[1];
        lba[0] = lb;
        HAProxyConfigurator hpg = new HAProxyConfigurator();
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "12", true);
        String result = genConfig(hpg, cmd);
        assertThat("keepalive enabled or http healthcheck should result in 'mode http' in the resulting haproxy config", result, containsString("mode http"));
        assertThat("healthcheck should result in 'option httpchk' in the resulting haproxy config", result, containsString("option httpchk /"));

        cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "4", true);
        healthCheckPolicies.set(0, new LbHealthCheckPolicy("/", "health", 5, 5, 5, 5, true));
        lba[0] = new LoadBalancerTO("1", "10.2.0.1", 80, "http", "bla", false, false, false, null, null, healthCheckPolicies, null, null);
        result = genConfig(hpg, cmd);
        assertThat("revoked healthcheck should not result in 'option httpchk' in the resulting haproxy config", result, not(containsString("option httpchk")));
        assertThat("keepalive enabled should not result in 'mode http' in the resulting haproxy config", result, not(containsString("mode http")));

        lba[0] = new LoadBalancerTO("1", "10.2.0.1", 80, "http", "bla", false, false, false, null, null);
        result = genConfig(hpg, cmd);
        assertThat("revoked healthcheck should not result in 'option httpchk' in the resulting haproxy config", result, not(containsString("option httpchk")));
        assertThat("keepalive enabled should not result in 'mode http' in the resulting haproxy config", result, not(containsString("mode http")));
    }

    @Test
    public void testGenerateConfigurationLoadBalancerStickinessCommand() {
        final List<LbDestination> dests = Arrays.asList(
            new LbDestination(443, 8443, "10.1.10.2", false),
            new LbDestination(443, 8443, "10.1.10.2", true)
        );
        final List<Pair<String, String>> stickinessParameters = Arrays.asList(
                new Pair<>("cookie-name", "SESSION_ID")
        );
        List<LbStickinessPolicy> stickinessPolicies = Arrays.asList(
                new LbStickinessPolicy(LbStickinessMethod.StickinessMethodType.AppCookieBased.getName(), stickinessParameters)
        );
        LoadBalancerTO lb = new LoadBalancerTO("1", "10.2.0.1", 80, "http", "bla", false, false, false, dests, stickinessPolicies);
        LoadBalancerTO[] lba = new LoadBalancerTO[1];
        lba[0] = lb;
        HAProxyConfigurator hpg = new HAProxyConfigurator();
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "12", true);
        String result = genConfig(hpg, cmd);
        assertThat("http cookie should result in 'mode http' in the resulting haproxy config", result, containsString("mode http"));
        assertThat("healthcheck enabled, but  result in 'option httpchk' in the resulting haproxy config", result, containsString("appsession SESSION_ID"));


        cmd = new LoadBalancerConfigCommand(lba, "10.0.0.1", "10.1.0.1", "10.1.1.1", null, 1L, "4", true);
        stickinessPolicies.set(0, new LbStickinessPolicy(LbStickinessMethod.StickinessMethodType.AppCookieBased.getName(), stickinessParameters, true));
        lba[0] = new LoadBalancerTO("1", "10.2.0.1", 80, "http", "bla", false, false, false, dests, stickinessPolicies);
        result = genConfig(hpg, cmd);
        assertThat("revoked healthcheck should not result in 'option httpchk' in the resulting haproxy config", result, not(containsString("appsession SESSION_ID")));
        assertThat("keepalive enabled should not result in 'mode http' in the resulting haproxy config", result, not(containsString("mode http")));

        lba[0] = new LoadBalancerTO("1", "10.2.0.1", 80, "http", "bla", false, false, false, dests, null);
        result = genConfig(hpg, cmd);
        assertThat("revoked healthcheck should not result in 'option httpchk' in the resulting haproxy config", result, not(containsString("appsession SESSION_ID")));
        assertThat("keepalive enabled should not result in 'mode http' in the resulting haproxy config", result, not(containsString("mode http")));
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

    private String genConfig(HAProxyConfigurator hpg, LoadBalancerConfigCommand cmd) {
        String[] sa = hpg.generateConfiguration(cmd);
        StringBuilder sb = new StringBuilder();
        for (String s : sa) {
            sb.append(s).append('\n');
        }
        return sb.toString();
    }

}
