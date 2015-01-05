/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.support;
import com.cloud.hypervisor.ovm3.objects.CloudStackPluginTest;
import com.cloud.hypervisor.ovm3.objects.ConnectionTest;
import com.cloud.hypervisor.ovm3.objects.LinuxTest;
import com.cloud.hypervisor.ovm3.objects.NetworkTest;
import com.cloud.hypervisor.ovm3.objects.XenTest;
import com.cloud.hypervisor.ovm3.objects.XmlTestResultTest;

public class Ovm3SupportTest {
    XmlTestResultTest results = new XmlTestResultTest();
    NetworkTest net = new NetworkTest();
    LinuxTest linux = new LinuxTest();
    XenTest xen = new XenTest();
    CloudStackPluginTest csp = new CloudStackPluginTest();

    public ConnectionTest prepConnectionResults() {
        ConnectionTest con = new ConnectionTest();
        con.setBogus(true);
        return configureResult(con);
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
        con.setMethodResponse("echo", results.simpleResponseWrapWrapper("put"));
        con.setMethodResponse("list_vms", xen.getMultipleVmsListXML());
        con.setMethodResponse("list_vm", xen.getSingleVmListXML());
        con.setMethodResponse("get_vm_config", xen.getSingleVmConfigXML());
        con.setMethodResponse("create_vm", results.getNil());
        con.setMethodResponse("start_vm", results.getNil());
        con.setMethodResponse("reboot_vm", results.getNil());
        con.setMethodResponse("stop_vm", results.getNil());
        con.setMethodResponse("copy_file", results.getNil());
        con.setMethodResponse("storage_plugin_destroy", results.getNil());
        con.setMethodResponse("check_domr_ssh",
                results.simpleResponseWrap("boolean", "1"));
        con.setMethodResponse("exec_domr", csp.getDomrExecXml());
        con.setMethodResponse("ovs_domr_upload_file",  results.simpleResponseWrap("boolean", "1"));
        return con;
    }
}
