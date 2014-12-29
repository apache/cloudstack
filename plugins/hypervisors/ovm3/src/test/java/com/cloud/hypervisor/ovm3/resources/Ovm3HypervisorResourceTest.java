package com.cloud.hypervisor.ovm3.resources;

import static org.junit.Assert.assertNull;

import javax.naming.ConfigurationException;

import org.junit.Test;

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
    XmlTestResultTest results = new XmlTestResultTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    NetworkTest net = new NetworkTest();
    LinuxTest linux = new LinuxTest();
    CloudStackPluginTest csp = new CloudStackPluginTest();
    XenTest xen = new XenTest();

    @Test
    public void configureTest() throws ConfigurationException {
        ConnectionTest con = new ConnectionTest();
        con.setBogus(true);
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        /* prepare connection answers */
        /* */
        con.setResult(results.simpleResponseWrap("boolean", "1"));
        /* network discovery */
        con.addResult(results.simpleResponseWrapWrapper(net
                .getDiscoverNetwork()));
        /* configure networking */
        con.addResult(results.simpleResponseWrap("boolean", "1"));
        con.addResult(results.simpleResponseWrap("boolean", "1"));
        /* discover hardware */
        con.addResult(results.simpleResponseWrapWrapper(linux.getDiscoverHw()));
        con.addResult(results.simpleResponseWrapWrapper(linux
                .getDiscoverserver()));
        /* */
        con.addResult(results.simpleResponseWrap("boolean", "1"));

        /* */
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
    }

    @Test(expected = ConfigurationException.class)
    public void configureFailBaseConnectionTest() throws ConfigurationException {
        ConnectionTest con = new ConnectionTest();
        con.setBogus(false);
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
    }

    @Test(expected = CloudRuntimeException.class)
    public void configureFailNetTest() throws ConfigurationException {
        ConnectionTest con = new ConnectionTest();
        con.setBogus(true);
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        /* prepare connection answers */
        // con.setIp(config.getAgentIp());
        // con.setResult(results.simpleResponseWrap("boolean", "0"));
        /* network discovery */
        String netdef = net.getDiscoverNetwork();
        netdef.replaceAll(config.getAgentControlNetworkName(), "thisisnotit0");
        con.setResult(results.simpleResponseWrapWrapper(netdef));
        /* configure networking */
        // con.addResult(results.simpleResponseWrap("boolean", "1"));
        // con.addResult(results.simpleResponseWrap("boolean", "1"));
        /* discover hardware */
        // con.addResult(results.simpleResponseWrapWrapper(linux.getDiscoverHw()));
        // con.addResult(results.simpleResponseWrapWrapper(linux.getDiscoverserver()));
        /* */
        // con.addResult(results.simpleResponseWrap("boolean", "1"));

        /* */
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
    }

    @Test
    public void startCommandTest() {
    }

    @Test
    public void PingCommandAndConfigureTest() throws ConfigurationException {
        ConnectionTest con = new ConnectionTest();
        con.setBogus(true);
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());

        con.setResult(results.simpleResponseWrap("boolean", "1"));
        /* network discovery */
        con.addResult(results.simpleResponseWrapWrapper(net
                .getDiscoverNetwork()));
        /* configure networking */
        con.addResult(results.simpleResponseWrap("boolean", "1"));
        con.addResult(results.simpleResponseWrap("boolean", "1"));
        /* discover hardware */
        con.addResult(results.simpleResponseWrapWrapper(linux.getDiscoverHw()));
        con.addResult(results.simpleResponseWrapWrapper(linux
                .getDiscoverserver()));
        /* */
        con.addResult(results.simpleResponseWrap("boolean", "1"));

        /* */
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        con.setResult(results.simpleResponseWrapWrapper("put"));
        con.addResult(xen.getVmListXML());
        hypervisor.setConnection(con);
        hypervisor.getCurrentStatus(1L);
        /* double up for the win */
        con.setResult(results.simpleResponseWrapWrapper("put"));
        con.addResult(xen.getVmListXML());
        hypervisor.getCurrentStatus(1L);
    }

    @Test
    public void PingCommandFailTest() throws ConfigurationException {
        ConnectionTest con = new ConnectionTest();
        con.setBogus(true);
        // Ovm3Configuration config = new
        // Ovm3Configuration(configTest.getParams());
        con.setResult(results.simpleResponseWrapWrapper("fail"));
        hypervisor.setConnection(con);
        assertNull(hypervisor.getCurrentStatus(1L));
    }
}