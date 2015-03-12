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

package com.cloud.hypervisor.ovm3.support;

import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.hypervisor.ovm3.objects.CloudStackPluginTest;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.LinuxTest;
import com.cloud.hypervisor.ovm3.objects.NetworkTest;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;
import com.cloud.hypervisor.ovm3.resources.Ovm3HypervisorResource;
import com.cloud.hypervisor.ovm3.resources.Ovm3VirtualRoutingResource;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3Configuration;
import com.cloud.hypervisor.ovm3.resources.helpers.Ovm3ConfigurationTest;

public class Ovm3SupportTest {
    ConnectionTest con = new ConnectionTest();
    XmlTestResultTest results = new XmlTestResultTest();
    NetworkTest net = new NetworkTest();
    LinuxTest linux = new LinuxTest();
    XenTest xen = new XenTest();
    CloudStackPluginTest csp = new CloudStackPluginTest();
    Ovm3HypervisorResource hypervisor = new Ovm3HypervisorResource();
    Ovm3VirtualRoutingResource virtualrouting = new Ovm3VirtualRoutingResource();
    Ovm3ConfigurationTest configTest = new Ovm3ConfigurationTest();

    public ConnectionTest prepConnectionResults() {
        ConnectionTest con = new ConnectionTest();
        con.setIp(con.getHostName());
        return configureResult(con);
    }

    public Ovm3HypervisorResource prepare(Map<String, Object> params) throws ConfigurationException {
        Ovm3Configuration config = new Ovm3Configuration(params);
        con = prepConnectionResults();
        hypervisor.setConnection(con);
        results.basicBooleanTest(hypervisor.configure(config.getAgentName(),
                configTest.getParams()));
        return hypervisor;
    }
    public ConnectionTest getConnection() {
        return con;
    }

    public ConnectionTest configureResult(ConnectionTest con) {
        con.setMethodResponse("check_dom0_ip",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("ovs_ip_config",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("ovs_local_config",
                results.simpleResponseWrap("string", "start"));
        con.setMethodResponse("ovs_control_interface",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("update_server_roles",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("discover_network",
                results.simpleResponseWrapWrapper(net.getDiscoverNetwork()));
        con.setMethodResponse("discover_hardware",
                results.simpleResponseWrapWrapper(linux.getDiscoverHw()));
        con.setMethodResponse("discover_server",
                results.simpleResponseWrapWrapper(linux.getDiscoverserver()));
        con.setMethodResponse("discover_mounted_file_systems",
                results.simpleResponseWrapWrapper(linux.getDiscoverFs()));
        con.setMethodResponse("get_vncport", results.simpleResponseWrapWrapper("5900"));
        con.setMethodResponse("echo", results.simpleResponseWrapWrapper("put"));
        con.setMethodResponse("list_vms", xen.getMultipleVmsListXML());
        con.setMethodResponse("list_vm", xen.getSingleVmListXML());
        con.setMethodResponse("get_vm_config", xen.getSingleVmConfigXML());
        con.setMethodResponse("create_vm", results.getNil());
        con.setMethodResponse("start_vm", results.getNil());
        con.setMethodResponse("reboot_vm", results.getNil());
        con.setMethodResponse("stop_vm", results.getNil());
        con.setMethodResponse("configure_vm", results.getNil());
        con.setMethodResponse("migrate_vm", results.getNil());
        con.setMethodResponse("copy_file", results.getNil());
        con.setMethodResponse("storage_plugin_destroy", results.getNil());
        con.setMethodResponse("ping",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("check_domr_ssh",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("check_domr_port",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("exec_domr", csp.getDomrExecXml());
        con.setMethodResponse("ovs_domr_upload_file",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("ovs_domU_stats", (csp.getDomuStatsXml()));
        con.setMethodResponse("check_dom0_status", (csp.getDom0StorageCheckXml()));
        con.setMethodResponse("check_dom0_storage_health", results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("check_dom0_port", results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("check_dom0_storage_health_check",  (csp.getDom0StorageCheckXml()));
        return con;
    }
}
