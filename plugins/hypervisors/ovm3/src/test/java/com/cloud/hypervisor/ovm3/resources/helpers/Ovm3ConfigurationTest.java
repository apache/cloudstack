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

package com.cloud.hypervisor.ovm3.resources.helpers;

import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Test;

import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;

public class Ovm3ConfigurationTest {
    XmlTestResultTest results = new XmlTestResultTest();
    private Ovm3Configuration ovm3config;
    private static HashMap<String, Object> params;
    static {
        params = new HashMap<String, Object>();
        params.put("agentusername", "oracle");
        params.put("xenserver.heartbeat.interval", "60");
        params.put("public.network.device", "xenbr0");
        params.put("private.network.device", "xenbr0");
        params.put("agentpassword", "unknown");
        params.put("secondary.storage.vm", "false");
        params.put("Hypervisor.Version", "4.1.3OVM");
        params.put("Host.OS", "Oracle VM Server");
        params.put("ipaddress", "192.168.1.64");
        params.put("ovm3pool", "true");
        params.put("password", "unknown");
        params.put("username", "root");
        params.put("pool", "a9c1219d-817d-4242-b23e-2607801c79d5");
        params.put("isprimary", "false");
        params.put("storage.network.device", "xenbr0");
        params.put("Host.OS.Version", "5.7");
        params.put("xenserver.nics.max", "7");
        params.put("agentVersion", "3.2.1-183");
        params.put("router.aggregation.command.each.timeout", "3");
        params.put("pod", "1");
        params.put("max.template.iso.size", "50");
        params.put("host", "ovm-1");
        params.put("com.cloud.network.Networks.RouterPrivateIpStrategy",
                "DcGlobal");
        params.put("agentport", "8899");
        params.put("Host.OS.Kernel.Version", "2.6.39-300.22.2.el5uek");
        params.put("migratewait", "3600");
        params.put("storage.network.device1", "xenbr0");
        params.put("ovm3cluster", "false");
        params.put("ip", "192.168.1.64");
        params.put("guid", "19e5f1e7-22f4-3b6d-8d41-c82f89c65295");
        params.put("ovm3vip", "192.168.1.230");
        params.put("hasprimary", "true");
        params.put("guest.network.device", "xenbr0");
        params.put("cluster", "1");
        params.put("xenserver.heartbeat.timeout", "120");
        params.put("ovm3.heartbeat.timeout", "120");
        params.put("ovm3.heartbeat.interval", "1");
        params.put("zone", "1");
        params.put("istest", true);
    }

    @Test
    public void testConfigLoad() throws ConfigurationException {
        params.put("pod", "1");
        ovm3config = new Ovm3Configuration(params);
        results.basicStringTest(ovm3config.getAgentHostname(), "ovm-1");
    }

    @Test(expected = ConfigurationException.class)
    public void testFailedParams() throws ConfigurationException {
        HashMap<String, Object> par = new HashMap<String,Object>(params);
        par.put("pod", null);
        ovm3config = new Ovm3Configuration(par);
    }
    @Test
    public void testValidatePool() throws ConfigurationException {
        HashMap<String, Object> par = new HashMap<String,Object>(params);
        par.put("cluster", "1");
        par.put("ovm3vip", "this is not an IP!");
        ovm3config = new Ovm3Configuration(par);
        results.basicBooleanTest(ovm3config.getAgentInOvm3Pool(), false);
        results.basicBooleanTest(ovm3config.getAgentInOvm3Cluster(), false);
        results.basicStringTest(ovm3config.getOvm3PoolVip(), "");
    }
    @Test
    public void testAgentPort() throws ConfigurationException {
        HashMap<String, Object> par = new HashMap<String,Object>(params);
        String altPort="6333";
        par.put("agentport", altPort);
        ovm3config = new Ovm3Configuration(par);
        results.basicIntTest(Integer.parseInt(altPort), ovm3config.getAgentOvsAgentPort());
    }
    public Map<String, Object> getParams() {
        return params;
    }
}
