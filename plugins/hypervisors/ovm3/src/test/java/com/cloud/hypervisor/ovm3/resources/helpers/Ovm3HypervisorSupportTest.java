package com.cloud.hypervisor.ovm3.resources.helpers;

import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.naming.ConfigurationException;

import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.LinuxTest;
import com.cloud.hypervisor.ovm3.objects.NetworkTest;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3HypervisorSupport;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.State;

public class Ovm3HypervisorSupportTest {
    ConnectionTest con = new ConnectionTest();
    XmlTestResultTest results = new XmlTestResultTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    XenTest xenTest = new XenTest();
    String vmName = xenTest.getVmName();
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

    @Test
    public void ReportedVmStatesTest() throws ConfigurationException,
            Ovm3ResourceException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con.setResult(xenTest.getVmListXML());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        hypervisor.vmStateMapClear();

        State vmState = hypervisor.getVmState(vmName);
        results.basicStringTest(vmState.toString(), State.Running.toString());
        hypervisor.setVmStateStarting(vmName);
        results.basicStringTest(hypervisor.getVmState(vmName)
                .toString(), State.Starting.toString());
        hypervisor.setVmState(vmName, State.Running);
        results.basicStringTest(hypervisor.getVmState(vmName)
                .toString(), State.Running.toString());
        hypervisor.revmoveVmState(vmName);
        assertNull(hypervisor.getVmState(vmName));
    }

    @Test
    public void HypervisorVmStateTest() throws ConfigurationException,
            Ovm3ResourceException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con.setResult(xenTest.getVmListXML());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        // hypervisor.vmStateMapClear();
        /* paused is used for migrate!!! */
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
    public void CombinedVmStateTest() throws ConfigurationException, Ovm3ResourceException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con.setResult(xenTest.getVmListXML());
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
     * @param hypervisor
     * @param original
     * @param replace
     * @param state
     * @throws Ovm3ResourceException
     */
    public void setHypervisorVmState(Ovm3HypervisorSupport hypervisor, String original,
            String replace, State state) throws Ovm3ResourceException {
        String x = xenTest.getVmListXML().replaceAll(original, replace);
        con.setResult(x);
        hypervisor.syncState();
        results.basicStringTest(hypervisor.getVmState(vmName)
                .toString(), state.toString());
    }

    @Test
    public void getSystemVMKeyFileTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        hypervisor.getSystemVMKeyFile(config.getAgentSshKeyFileName());
    }

    @Test
    public void checkHealthTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        con.setResult(results.simpleResponseWrapWrapper("put"));
        CheckHealthCommand cmd = new CheckHealthCommand();
        hypervisor.execute(cmd);
    }

    @Test
    public void masterCheckTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        con.setResult(results.simpleResponseWrapWrapper("true"));
        /* euh ? */
        // System.out.println(hypervisor.masterCheck());
    }

    @Test
    public void GetHostStatsCommandTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        /* fake */
        GetHostStatsCommand cmd = new GetHostStatsCommand(config.getCsGuid(),
                config.getAgentName(), 1L);
        con.setResult(this.dom0stats);
        hypervisor.execute((GetHostStatsCommand) cmd);
    }

    @Test
    public void GetHostStatsCommandFailTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        /* fake */
        GetHostStatsCommand cmd = new GetHostStatsCommand(config.getCsGuid(),
                config.getAgentName(), 1L);
        con.setResult(null);
        Answer x = hypervisor.execute((GetHostStatsCommand) cmd);
        results.basicBooleanTest(x.getResult(), false);
    }

    @Test
    public void CheckVirtualMachineCommandTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
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
        con.setResult(results.simpleResponseWrapWrapper(linuxTest.getDiscoverHw()));
        con.addResult(results.simpleResponseWrapWrapper(linuxTest.getDiscoverserver()));
        con.addResult(results.simpleResponseWrapWrapper(networkTest.getDiscoverNetwork()));
        hypervisor.fillHostInfo(srCmd);
    }
    
    @Test(expected = CloudRuntimeException.class)
    public void setupServerTest() throws ConfigurationException, IOException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        ConnectionTest con = new ConnectionTest();
        con.setIp(config.getAgentIp());
        Ovm3HypervisorSupport hypervisor = new Ovm3HypervisorSupport(con,
                config);
        hypervisor.setupServer(config.getAgentSshKeyFileName());
    }
}
