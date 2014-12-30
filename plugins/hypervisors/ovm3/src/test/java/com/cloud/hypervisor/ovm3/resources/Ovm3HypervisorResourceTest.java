package com.cloud.hypervisor.ovm3.resources;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import javax.naming.ConfigurationException;

import org.junit.Test;

import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.hypervisor.ovm3.objects.CloudStackPluginTest;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.LinuxTest;
import com.cloud.hypervisor.ovm3.objects.NetworkTest;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3ConfigurationTest;
import com.cloud.utils.exception.CloudRuntimeException;

public class Ovm3HypervisorResourceTest {
    ConnectionTest con;
    XmlTestResultTest results = new XmlTestResultTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    NetworkTest net = new NetworkTest();
    LinuxTest linux = new LinuxTest();
    CloudStackPluginTest csp = new CloudStackPluginTest();
    XenTest xen = new XenTest();
    String getCurrentStatus = "put";
    String vmName = "i-2-3-VM";
    
    private ConnectionTest prepConnectionResults(List<String> l) {
        ConnectionTest con = new ConnectionTest();
        con.setBogus(true);
        con.setResult(l);
        return con;
    }
    
    private ArrayList<String> configureResult() {
        ArrayList<String> res = new ArrayList<String>();
        res.add(results.simpleResponseWrap("boolean", "1"));
        res.add(results.simpleResponseWrapWrapper(net
                .getDiscoverNetwork()));
        res.add(results.simpleResponseWrap("boolean", "1"));
        res.add(results.simpleResponseWrap("boolean", "1"));
        res.add(results.simpleResponseWrapWrapper(linux.getDiscoverHw()));
        res.add(results.simpleResponseWrapWrapper(linux
                .getDiscoverserver()));
        res.add(results.simpleResponseWrap("boolean", "1"));
        return res;
    }

    @Test
    public void configureTest() throws ConfigurationException {
        /* the order needs to reflect what it is... */
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = prepConnectionResults(configureResult());
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
    }

    @Test(expected = ConfigurationException.class)
    public void configureFailBaseConnectionTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        ConnectionTest con = new ConnectionTest();
        con.setBogus(false);
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
    }

    @Test(expected = CloudRuntimeException.class)
    public void configureFailNetTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        String netdef = net.getDiscoverNetwork();
        netdef.replaceAll(config.getAgentControlNetworkName(), "thisisnotit0");
        ArrayList<String> res = new ArrayList<String>();
        res.add(results.simpleResponseWrapWrapper(netdef));
        con = prepConnectionResults(res);
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
    }

    @Test
    public void startCommandTest() {
    }

    @Test
    public void getCurrentStatusAndConfigureTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = prepConnectionResults(configureResult());        
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        assertNotNull(getCurrentStatusTest(getCurrentStatus));
        assertNotNull(getCurrentStatusTest(getCurrentStatus));
        assertNotNull(getCurrentStatusTest(getCurrentStatus));
    }

    private PingCommand getCurrentStatusTest(String put) {
        con.setResult(results.simpleResponseWrapWrapper(put));
        con.addResult(xen.getVmListXML());
        hypervisor.setConnection(con);
        return hypervisor.getCurrentStatus(1L);
    }

    @Test
    public void getCurrentStatusFailTest() throws ConfigurationException {
        con = prepConnectionResults(configureResult());
        assertNull(getCurrentStatusTest("fail"));
    }
    @Test
    public void getCurrentStatusExceptionTest() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = new ConnectionTest();
        hypervisor.setConnection(con);
        assertNull(hypervisor.getCurrentStatus(1L));
    }

    @Test
    public void initializeTest() throws Exception {
        ArrayList<String> res = new ArrayList<String>();
        // res.add();
        con = prepConnectionResults(res);
        hypervisor.setConnection(con);
    }
    
    private Ovm3HypervisorResource vmActionPreparation() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = prepConnectionResults(configureResult());
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        ArrayList<String> res = new ArrayList<String>();
        con = prepConnectionResults(res);
        hypervisor.setConnection(con);
        return hypervisor;
    }

    private Boolean rebootVm(String name) throws ConfigurationException {
        hypervisor = vmActionPreparation();
        con.addResult(xen.getVmListXML());
        con.addResult(xen.getVmListXML().replace(vmName, vmName + "-hide"));
        con.addResult(xen.getVmListXML());
        RebootCommand cmd = new RebootCommand(name);
        RebootAnswer ra = hypervisor.execute(cmd);
        return ra.getResult();
    }

    @Test
    public void rebootCommandTest() throws ConfigurationException {
        results.basicBooleanTest(rebootVm(vmName));
    }
 
    @Test
    public void rebootCommandFailt() throws ConfigurationException {
        results.basicBooleanTest(rebootVm("bogus"), false);
    }
    
    public void stopVm() throws ConfigurationException {
        hypervisor = vmActionPreparation();
        con.addResult(xen.getVmListXML());
        con.addResult(xen.getVmListXML());
        con.addResult(xen.getVmListXML().replace(vmName, vmName + "-hide"));
        con.addResult(results.simpleResponseWrap("boolean", "1"));
    }
    @Test
    public void stopVmTest() throws ConfigurationException {
        stopVm();
        con.addResult(xen.getVmListXML().replace(vmName, vmName + "-hide"));
        StopCommand cmd = new StopCommand(vmName, true, true);
        StopAnswer ra = hypervisor.execute(cmd);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void stopVmTestFail() throws ConfigurationException {
        stopVm();
        con.addResult(xen.getVmListXML());
        con.addResult(xen.getVmListXML().replace(vmName, vmName));
        StopCommand cmd = new StopCommand(vmName, true, true);
        StopAnswer ra = hypervisor.execute(cmd);
        results.basicBooleanTest(ra.getResult(), false);
    }
    @Test
    public void stopVmTreatAsStoppedTestl() throws ConfigurationException {
        hypervisor = vmActionPreparation();
        con.addResult(xen.getVmListXML().replace(vmName, vmName + "-hide"));
        StopCommand cmd = new StopCommand(vmName, true, true);
        StopAnswer ra = hypervisor.execute(cmd);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void stopVmException() throws ConfigurationException {
        hypervisor = vmActionPreparation();
        // Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        // con = new ConnectionTest(config.getAgentIp(), config.getAgentOvsAgentUser(), config.getAgentOvsAgentPassword());
        hypervisor.setConnection(con);
        StopCommand cmd = new StopCommand(vmName, true, true);
        StopAnswer ra = hypervisor.execute(cmd);
        results.basicBooleanTest(ra.getResult(), false);
    }

    @Test
    public void startVm() throws ConfigurationException {
        hypervisor = vmActionPreparation();
        //  public StartCommand(VirtualMachineTO vm, Host host, boolean executeInSequence) {
        // ./api/src/com/cloud/agent/api/to/VirtualMachineTO.java
        // StartCommand cmd = new StartCommand();
    }
    @Test
    public void startResource() {
        results.basicBooleanTest(hypervisor.start());
    }
    @Test 
    public void stopResource() {
        results.basicBooleanTest(hypervisor.stop());
    }
}