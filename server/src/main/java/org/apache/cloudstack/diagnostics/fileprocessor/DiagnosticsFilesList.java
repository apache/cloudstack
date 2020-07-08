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
package org.apache.cloudstack.diagnostics.fileprocessor;

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;

public interface DiagnosticsFilesList {

    /**
     * Global configs below are used to set the diagnostics
     * data types applicable for each system vm.
     * <p>
     * the names wrapped in square brackets are for data types that need to first execute a script
     * in the system vm and grab output for retrieval, e.g. the output from iptables-save is written to a file
     * which will then be retrieved.
     */
    ConfigKey<String> SystemVMDefaultSupportedFiles = new ConfigKey<>("Advanced", String.class,
            "diagnostics.data.systemvm.defaults", "iptables,ipaddr,iprule,iproute,/etc/cloudstack-release," +
            "/usr/local/cloud/systemvm/conf/agent.properties,/usr/local/cloud/systemvm/conf/consoleproxy.properties," +
            "/var/log/cloud.log,/var/log/patchsystemvm.log,/var/log/daemon.log",
            "List of supported diagnostics data file options for the CPVM and SSVM.", true);

    ConfigKey<String> RouterDefaultSupportedFiles = new ConfigKey<>("Advanced", String.class,
            "diagnostics.data.router.defaults", "iptables,ipaddr,iprule,iproute,/etc/cloudstack-release," +
            "/etc/dnsmasq.conf,/etc/dhcphosts.txt,/etc/dhcpopts.txt,/etc/dnsmasq.d/cloud.conf,/etc/dnsmasq-resolv.conf,/var/lib/misc/dnsmasq.leases,/var/log/dnsmasq.log," +
            "/etc/hosts,/etc/resolv.conf,/etc/haproxy/haproxy.cfg,/var/log/haproxy.log,/etc/ipsec.d/l2tp.conf,/var/log/cloud.log," +
            "/var/log/routerServiceMonitor.log,/var/log/daemon.log",
            "List of supported diagnostics data file options for the domain router.", true);

    List<String> generateFileList();
}
