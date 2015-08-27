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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.naming.ConfigurationException;
import org.junit.Test;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.hypervisor.ovm3.objects.CloudStackPluginTest;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.Ovm3ResourceException;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.resources.Ovm3HypervisorResource;
import com.cloud.hypervisor.ovm3.resources.Ovm3HypervisorResourceTest;
import com.cloud.hypervisor.ovm3.support.Ovm3SupportTest;
import com.cloud.network.Networks.TrafficType;
import com.cloud.vm.VirtualMachine;

public class Ovm3VmSupportTest {
    ConnectionTest con = new ConnectionTest();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();
    Ovm3SupportTest support = new Ovm3SupportTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    Ovm3HypervisorResourceTest hyperTest = new Ovm3HypervisorResourceTest();
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
    @Test
    public void GetVmStatsCommandTest() throws ConfigurationException {
        hypervisor = support.prepare(configTest.getParams());
        Ovm3Configuration configuration = new Ovm3Configuration(configTest.getParams());
        List<String> vms = new ArrayList<String>();
        vms.add(xen.getVmName());
        GetVmStatsCommand cmd = new GetVmStatsCommand(vms, configuration.getCsHostGuid(), hypervisor.getName());
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
        cmd = new GetVmStatsCommand(vms, configuration.getCsHostGuid(), hypervisor.getName());
        ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void PrepareForMigrationCommandTest() throws ConfigurationException, Ovm3ResourceException {
        hypervisor = support.prepare(configTest.getParams());
        PrepareForMigrationCommand cmd = new PrepareForMigrationCommand(hyperTest.createVm(xen.getVmName()));
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }
    @Test
    public void MigrateCommandTest() throws ConfigurationException, Ovm3ResourceException {
        Ovm3Configuration configuration = new Ovm3Configuration(configTest.getParams());
        hypervisor = support.prepare(configTest.getParams());
        MigrateCommand cmd = new MigrateCommand(xen.getVmName(), configuration.getAgentIp(), false, hyperTest.createVm(xen.getVmName()), false);
        Answer ra = hypervisor.executeRequest(cmd);
        results.basicBooleanTest(ra.getResult());
    }
}
