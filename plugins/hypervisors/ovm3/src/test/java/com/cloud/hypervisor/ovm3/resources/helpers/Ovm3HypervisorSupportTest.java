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

import static org.junit.Assert.assertNull;

import javax.naming.ConfigurationException;

import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.LinuxTest;
import com.cloud.hypervisor.ovm3.objects.NetworkTest;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.resources.Ovm3HypervisorResource;
import com.cloud.hypervisor.ovm3.resources.Ovm3StorageProcessor;
import com.cloud.hypervisor.ovm3.resources.Ovm3VirtualRoutingResource;
import com.cloud.hypervisor.ovm3.support.Ovm3SupportTest;
import com.cloud.vm.VirtualMachine.State;

public class Ovm3HypervisorSupportTest {
    ConnectionTest con = new ConnectionTest();
    XmlTestResultTest results = new XmlTestResultTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    Ovm3VirtualRoutingResource virtualrouting = new Ovm3VirtualRoutingResource();
    Ovm3SupportTest support = new Ovm3SupportTest();
    Ovm3StorageProcessor storage;
    Ovm3StoragePool pool;
    XenTest xen = new XenTest();
    String vmName = xen.getVmName();
    String unknown = "------";
    String running = "r-----";
    String blocked = "-b----";
    String paused = "--p---";
    String shutdown = "---s--";
    String crashed = "----c-";
    String dying = "-----d";

    /* we only want this for the xml results */
    String dom0stats = results.simpleResponseWrapWrapper("<struct>"
            + "<member>" + "<name>rx</name>"
            + "<value><string>25069761</string></value>" + "</member>"
            + "<member>" + "<name>total</name>"
            + "<value><string>4293918720</string></value>" + "</member>"
            + "<member>" + "<name>tx</name>"
            + "<value><string>37932556</string></value>" + "</member>"
            + "<member>" + "<name>cpu</name>"
            + "<value><string>2.4</string></value>" + "</member>" + "<member>"
            + "<name>free</name>"
            + "<value><string>1177550848</string></value>" + "</member>"
            + "</struct>");

    private ConnectionTest prepare() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = support.prepConnectionResults();
        pool = new Ovm3StoragePool(con, config);
        storage = new Ovm3StorageProcessor(con, config, pool);
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        virtualrouting.setConnection(con);
        return con;
    }
    @Test
    public void ReportedVmStatesTest() throws ConfigurationException,
            Ovm3ResourceException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con.setResult(xen.getMultipleVmsListXML());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        hypervisor.vmStateMapClear();

        State vmState = hypervisor.getVmState(vmName);
        results.basicStringTest(vmState.toString(), State.Running.toString());
        hypervisor.setVmStateStarting(vmName);
        results.basicStringTest(hypervisor.getVmState(vmName).toString(),
                State.Starting.toString());
        hypervisor.setVmState(vmName, State.Running);
        results.basicStringTest(hypervisor.getVmState(vmName).toString(),
                State.Running.toString());
        hypervisor.revmoveVmState(vmName);
        assertNull(hypervisor.getVmState(vmName));
    }

    @Test
    public void HypervisorVmStateTest() throws ConfigurationException,
            Ovm3ResourceException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        setHypervisorVmState(hypervisor, blocked, unknown, State.Unknown);
        setHypervisorVmState(hypervisor, blocked, running, State.Running);
        setHypervisorVmState(hypervisor, blocked, blocked, State.Running);
        setHypervisorVmState(hypervisor, blocked, paused, State.Running);
        /* TODO: ehm wtf ? */
        setHypervisorVmState(hypervisor, blocked, shutdown, State.Running);
        setHypervisorVmState(hypervisor, blocked, crashed, State.Error);
        setHypervisorVmState(hypervisor, blocked, dying, State.Stopping);
    }

    @Test
    public void CombinedVmStateTest() throws ConfigurationException,
            Ovm3ResourceException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con.setResult(xen.getMultipleVmsListXML());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        hypervisor.vmStateMapClear();
        /* test starting */
        hypervisor.setVmState(vmName, State.Starting);
        // System.out.println(hypervisor.getVmState(vmName));
        hypervisor.syncState();
        // System.out.println(hypervisor.getVmState(vmName));

        // setHypervisorVmState(hypervisor, blocked, paused, State.Stopped);

        hypervisor.setVmState(vmName, State.Stopping);
        hypervisor.setVmState(vmName, State.Migrating);
        // setHypervisorVmState(hypervisor, blocked, running, State.Running);
        hypervisor.setVmState(vmName, State.Stopped);

        // setHypervisorVmState(hypervisor, blocked, running, State.Migrating);

    }

    /**
     * Sets the state, original, of the fake VM to replace.
     *
     * @param hypervisor
     * @param original
     * @param replace
     * @param state
     * @throws Ovm3ResourceException
     */
    public void setHypervisorVmState(Ovm3HypervisorSupport hypervisor,
            String original, String replace, State state)
            throws Ovm3ResourceException {
        String x = xen.getMultipleVmsListXML().replaceAll(original, replace);
        con.setResult(x);
        hypervisor.syncState();
        results.basicStringTest(hypervisor.getVmState(vmName).toString(),
                state.toString());
    }

    @Test
    public void getSystemVMKeyFileTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        hypervisor.getSystemVMKeyFile(config.getAgentSshKeyFileName());
    }
    @Test
    public void getSystemVMKeyFileMissingTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        hypervisor.getSystemVMKeyFile("missing");
    }

    @Test
    public void checkHealthTest() throws ConfigurationException {
        con = prepare();
        CheckHealthCommand cmd = new CheckHealthCommand();
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }

    @Test
    public void primaryCheckTest() throws ConfigurationException {
        con = prepare();
        // System.out.println(hypervisor.primaryCheck());
    }

    @Test
    public void GetHostStatsCommandTest() throws ConfigurationException {
        con = prepare();
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        GetHostStatsCommand cmd = new GetHostStatsCommand(config.getCsHostGuid(),
                config.getAgentName(), 1L);
        con.setResult(this.dom0stats);
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }

    @Test
    public void GetHostStatsCommandFailTest() throws ConfigurationException {
        con = prepare();
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        GetHostStatsCommand cmd = new GetHostStatsCommand(config.getCsHostGuid(),
                config.getAgentName(), 1L);
        con.setNull();
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult(), false);
    }

    @Test
    public void CheckVirtualMachineCommandTest() throws ConfigurationException {
        con = prepare();
        CheckVirtualMachineCommand cmd = new CheckVirtualMachineCommand(xen.getVmName());
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void MaintainCommandTest() throws ConfigurationException {
        con = prepare();
        MaintainCommand cmd = new MaintainCommand();
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void GetVncPortCommandTest() throws ConfigurationException {
        con = prepare();
        GetVncPortCommand cmd = new GetVncPortCommand(0, xen.getVmName());
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }
    /* We can't fence yet... */
    @Test
    public void FenceCommandTest() throws ConfigurationException {
        con = prepare();
        FenceCommand cmd = new FenceCommand();
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult(), false);
    }

    @Test
    public void fillHostinfoTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        ConnectionTest con = new ConnectionTest();
        con.setIp(config.getAgentIp());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        LinuxTest linuxTest = new LinuxTest();
        NetworkTest networkTest = new NetworkTest();
        StartupRoutingCommand srCmd = new StartupRoutingCommand();
        con.setResult(results.simpleResponseWrapWrapper(linuxTest
                .getDiscoverHw()));
        con.addResult(results.simpleResponseWrapWrapper(linuxTest
                .getDiscoverserver()));
        con.addResult(results.simpleResponseWrapWrapper(networkTest
                .getDiscoverNetwork()));
        hypervisor.fillHostInfo(srCmd);
    }

    /* @Test(expected = CloudRuntimeException.class)
    public void setupServerTest() throws ConfigurationException, IOException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        ConnectionTest con = new ConnectionTest();
        con.setIp("127.0.0.1");
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        hypervisor.setupServer(config.getAgentSshKeyFileName());
    } */
}
