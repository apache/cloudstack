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

import javax.naming.ConfigurationException;

import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.hypervisor.ovm3.objects.CloudStackPluginTest;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.resources.Ovm3HypervisorResource;
import com.cloud.hypervisor.ovm3.support.Ovm3SupportTest;

public class Ovm3VirtualRoutingSupportTest {
    ConnectionTest con = new ConnectionTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3SupportTest support = new Ovm3SupportTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    CloudStackPluginTest csp = new CloudStackPluginTest();
    XmlTestResultTest results = new XmlTestResultTest();

    @Test
    public void NetworkUsageCommandTest() throws ConfigurationException {
        hypervisor = support.prepare(configTest.getParams());
        NetworkUsageCommand nuc = new NetworkUsageCommand(csp.getDomrIp(), "something", "", false);
        Answer ra = hypervisor.executeRequest(nuc);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void NetworkUsageVpcCommandTest() throws ConfigurationException {
            hypervisor = support.prepare(configTest.getParams());
            NetworkUsageCommand nuc = new NetworkUsageCommand(csp.getDomrIp(), "something", "", true);
            Answer ra = hypervisor.executeRequest(nuc);
            results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void NetworkVpcGetCommandTest() throws ConfigurationException {
        NetworkVpcCommandTest("get");
    }
    @Test
    public void NetworkVpcCreateCommandTest() throws ConfigurationException {
        NetworkVpcCommandTest("create");
    }
    @Test
    public void NetworkVpcResetCommandTest() throws ConfigurationException {
        NetworkVpcCommandTest("reset");
    }
    @Test
    public void NetworkVpcVpnCommandTest() throws ConfigurationException {
        NetworkVpcCommandTest("vpn");
    }
    @Test
    public void NetworkVpcRemoveCommandTest() throws ConfigurationException {
        NetworkVpcCommandTest("remove");
    }
    public void NetworkVpcCommandTest(String cmd) throws ConfigurationException {
        hypervisor = support.prepare(configTest.getParams());
        NetworkUsageCommand nuc = new NetworkUsageCommand(csp.getDomrIp(), "something", cmd, true);
        Answer ra = hypervisor.executeRequest(nuc);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void CheckSshCommandTest() throws ConfigurationException {
        hypervisor = support.prepare(configTest.getParams());
        CheckSshCommand ssh = new CheckSshCommand("name", csp.getDomrIp(), 8899);
        Answer ra = hypervisor.executeRequest(ssh);
        results.basicBooleanTest(ra.getResult());
    }
}
