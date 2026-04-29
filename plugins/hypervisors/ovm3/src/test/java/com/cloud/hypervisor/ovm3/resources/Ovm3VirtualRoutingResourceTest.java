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

package com.cloud.hypervisor.ovm3.resources;

import java.util.ArrayList;
import java.util.List;

import javax.naming.ConfigurationException;

import org.junit.Test;

import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.hypervisor.ovm3.objects.CloudStackPluginTest;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.LinuxTest;
import com.cloud.hypervisor.ovm3.objects.NetworkTest;
import com.cloud.hypervisor.ovm3.objects.OvmObject;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3ConfigurationTest;
import com.cloud.hypervisor.ovm3.support.Ovm3SupportTest;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.ExecutionResult;

public class Ovm3VirtualRoutingResourceTest {
    ConnectionTest con;
    OvmObject ovmObject = new OvmObject();
    XmlTestResultTest results = new XmlTestResultTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    Ovm3VirtualRoutingResource virtualrouting = new Ovm3VirtualRoutingResource();
    Ovm3SupportTest support = new Ovm3SupportTest();
    XenTest xen = new XenTest();
    NetworkTest net = new NetworkTest();
    LinuxTest linux = new LinuxTest();
    CloudStackPluginTest csp = new CloudStackPluginTest();
    String dom0Ip = csp.getDom0Ip();
    String domrIp = csp.getDomrIp();
    String routerip = "64.1.1.10";
    /* cheat */
    String cmd = "ls";
    String args = "";

    @Test
    public void executeInVRTest() {
        con = support.prepConnectionResults();
        cmd = "/opt/cloud/bin/" + cmd;
        virtualrouting.setConnection(con);
        ExecutionResult result = virtualrouting.executeInVR(domrIp, cmd, args);
        results.basicBooleanTest(result.isSuccess());
    }

    @Test
    public void executeInVRFailTest() {
        ConnectionTest con = new ConnectionTest();
        virtualrouting.setConnection(con);
        ExecutionResult result = virtualrouting.executeInVR(domrIp, cmd, args);
        results.basicBooleanTest(result.isSuccess(), false);
    }

    @Test
    public void createFileInVRTest() {
        con = support.prepConnectionResults();
        virtualrouting.setConnection(con);
        ExecutionResult result = virtualrouting.createFileInVR(domrIp, "/tmp",
                "test", "1 2 3");
        results.basicBooleanTest(result.isSuccess());
    }

    @Test
    public void createFileInVRFailTest() {
        ConnectionTest con = new ConnectionTest();
        virtualrouting.setConnection(con);
        ExecutionResult result = virtualrouting.createFileInVR(domrIp, "/tmp",
                "test", "1 2 3");
        results.basicBooleanTest(result.isSuccess(), false);
    }

    private ConnectionTest prepare() throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(configTest.getParams());
        con = support.prepConnectionResults();
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        virtualrouting.setConnection(con);
        return con;
    }

    @Test
    public void prepareVpcCommandFailTest() throws ConfigurationException {
        prepare();
        IpAssocVpcCommand vpc = generateIpAssocVpcCommand(xen.getVmNicMac()
                .replace("0", "A"));
        results.basicBooleanTest(hypervisor.executeRequest(vpc).getResult(),
                false);
    }

    @Test
    public void prepareVpcCommandFailHeavierTest()
            throws ConfigurationException {
        prepare();
        con.removeMethodResponse("list_vms");
        IpAssocVpcCommand vpc = generateIpAssocVpcCommand(xen.getVmNicMac()
                .replace("0", "F"));
        results.basicBooleanTest(hypervisor.executeRequest(vpc).getResult(),
                false);
    }

    @Test
    public void prepareCommandTest() throws ConfigurationException {
        prepare();
        IpAssocCommand rvm = generateIpAssocCommand(xen.getVmNicMac());
        results.basicBooleanTest(hypervisor.executeRequest(rvm).getResult());
    }

    /**
     * Test is not broken, but code broke somewhere look for the mac that doesn'
    "nics": [
          {
            "deviceId": 2,
            "networkRateMbps": 200,
            "defaultNic": true,
            "pxeDisable": true,
            "nicUuid": "80bd3c5b-a0f0-4de5-894a-999c83210d7c",
            "uuid": "eec7acd1-2845-4e46-9226-bd211bb97f28",
            "ip": "192.168.1.154",
            "netmask": "255.255.255.0",
            "gateway": "192.168.1.1",
            "mac": "06:83:0c:00:00:1e",
            "dns1": "192.168.1.60",
            "dns2": "192.168.1.1",
            "broadcastType": "Vlan",
            "type": "Public",
            "broadcastUri": "vlan://0",
            "isolationUri": "vlan://0",
            "isSecurityGroupEnabled": false
          },
    {
    "com.cloud.agent.api.routing.IpAssocCommand": {
      "ipAddresses": [
        {
          "accountId": 2,
          "publicIp": "192.168.1.154",
          "sourceNat": true,
          "add": true,
          "oneToOneNat": false,
          "firstIP": true,
          "broadcastUri": "vlan://0",
          "vlanGateway": "192.168.1.1",
          "vlanNetmask": "255.255.255.0",
          "vifMacAddress": "06:74:f6:00:00:1e",
          "networkRate": 200,
          "trafficType": "Public",
          "newNic": false
        }
     * @throws ConfigurationException
     */
    @Test
    public void prepareCommandFailTest() throws ConfigurationException {
        prepare();
        IpAssocCommand rvm = generateIpAssocCommand(xen.getVmNicMac().replace(
                "0", "F"));
        boolean res = hypervisor.executeRequest(rvm).getResult();
        // this should be false...
        results.basicBooleanTest(res,
                true);
    }

    @Test
    public void prepareCommandFailHeavierTest() throws ConfigurationException {
        prepare();
        con.removeMethodResponse("list_vms");
        IpAssocCommand rvm = generateIpAssocCommand(xen.getVmNicMac().replace(
                "0", "F"));
        results.basicBooleanTest(hypervisor.executeRequest(rvm).getResult(),
                false);
    }

    @Test
    public void prepareVpcCommandTest() throws ConfigurationException {
        prepare();
        IpAssocVpcCommand vpc = generateIpAssocVpcCommand(xen.getVmNicMac());
        results.basicBooleanTest(hypervisor.executeRequest(vpc).getResult());
    }

    private IpAddressTO[] getIp(String mac) {
        String br[] = xen.getVmNicBridge().split("[.]");
        List<IpAddressTO> ips = new ArrayList<IpAddressTO>();
        IpAddressTO ip = new IpAddressTO(1, routerip, true, true, true, "vlan://"
                + br[1], "64.1.1.1", "255.255.255.0", mac, 1000, false);
        ip.setTrafficType(TrafficType.Public);
        ips.add(ip);
        IpAddressTO[] ipArray = ips.toArray(new IpAddressTO[ips.size()]);
        return ipArray;
    }

    private IpAssocVpcCommand generateIpAssocVpcCommand(String mac) {
        IpAssocVpcCommand cmd = new IpAssocVpcCommand(getIp(mac));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, xen.getVmName());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerip);
        // assertEquals(6, cmd.getAnswersCount()); // AnswersCount is clearly
        // wrong as it doesn't know enough to tell
        return cmd;
    }

    private IpAssocCommand generateIpAssocCommand(String mac) {
        IpAssocCommand cmd = new IpAssocCommand(getIp(mac));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, xen.getVmName());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerip);
        // assertEquals(6, cmd.getAnswersCount()); // AnswersCount is clearly
        // wrong as it doesn't know enough to tell
        return cmd;
    }
}
