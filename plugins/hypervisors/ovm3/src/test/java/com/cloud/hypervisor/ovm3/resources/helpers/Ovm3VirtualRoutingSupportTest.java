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
