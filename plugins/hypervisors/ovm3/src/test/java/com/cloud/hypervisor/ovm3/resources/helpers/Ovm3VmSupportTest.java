package com.cloud.hypervisor.ovm3.resources.helpers;

import java.net.URI;
import java.net.URISyntaxException;

import javax.naming.ConfigurationException;

import org.junit.Test;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.ovm3.objects.CloudStackPluginTest;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.resources.Ovm3HypervisorResource;
import com.cloud.hypervisor.ovm3.support.Ovm3SupportTest;
import com.cloud.network.Networks.TrafficType;
import com.cloud.vm.VirtualMachine;

public class Ovm3VmSupportTest {
    ConnectionTest con = new ConnectionTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3SupportTest support = new Ovm3SupportTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    CloudStackPluginTest csp = new CloudStackPluginTest();
    XenTest xen = new XenTest();
    XmlTestResultTest results = new XmlTestResultTest();

    private NicTO prepNic(String mac, Integer vlan, TrafficType type) throws URISyntaxException {
        return prepNic(mac, vlan, type, 0);
    }
    private NicTO prepNic(String mac, Integer vlan, TrafficType type, Integer id) throws URISyntaxException {
        URI iso = new URI("vlan://" + vlan.toString());
        NicTO nic = new NicTO();
        nic.setType(type);
        /* Isolation is not what it seems.... */
        /* nic.setIsolationuri(iso); */
        nic.setBroadcastUri(iso);
        nic.setMac(mac);
        nic.setDeviceId(id);
        return nic;
    }
    @Test
    public void PlugNicTest() throws ConfigurationException, URISyntaxException {
        hypervisor = support.prepare(configTest.getParams());
        NicTO nic = prepNic(xen.getVmNicMac(), 200, TrafficType.Guest);
        PlugNicCommand plug = new PlugNicCommand(nic,xen.getVmName(), VirtualMachine.Type.User);
        Answer ra = hypervisor.executeRequest(plug);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void PlugNicBreakTest() throws ConfigurationException, URISyntaxException {
        hypervisor = support.prepare(configTest.getParams());
        NicTO nic = prepNic(xen.getVmNicMac(), 240, TrafficType.Guest);
        PlugNicCommand plug = new PlugNicCommand(nic,xen.getVmName(), VirtualMachine.Type.User);
        Answer ra = hypervisor.executeRequest(plug);
        results.basicBooleanTest(ra.getResult(), false);
    }
    @Test
    public void unPlugNicTest() throws ConfigurationException, URISyntaxException {
        hypervisor = support.prepare(configTest.getParams());
        NicTO nic = prepNic(xen.getVmNicMac(), 200, TrafficType.Guest);
        UnPlugNicCommand plug = new UnPlugNicCommand(nic, xen.getVmName());
        Answer ra = hypervisor.executeRequest(plug);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void unPlugNicBreakTest() throws ConfigurationException, URISyntaxException {
        hypervisor = support.prepare(configTest.getParams());
        NicTO nic = prepNic(xen.getVmNicMac(), 240, TrafficType.Guest);
        UnPlugNicCommand plug = new UnPlugNicCommand(nic, xen.getVmName());
        Answer ra = hypervisor.executeRequest(plug);
        results.basicBooleanTest(ra.getResult(), false);
    }
}
