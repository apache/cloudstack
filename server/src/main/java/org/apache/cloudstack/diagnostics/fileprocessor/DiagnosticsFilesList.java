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
    ConfigKey<String> SsvmDefaultSupportedFiles = new ConfigKey<>("Advanced", String.class,
            "diagnostics.data.ssvm.defaults", "[IPTABLES], [IFCONFIG], [ROUTE], /usr/local/cloud/systemvm/conf/agent.properties," +
            " /usr/local/cloud/systemvm/conf/consoleproxy.properties, /var/log/cloud.log",
            "List of supported diagnostics data file options for the ssvm", true);

    ConfigKey<String> VrDefaultSupportedFiles = new ConfigKey<>("Advanced", String.class,
            "diagnostics.data.vr.defaults", "[IPTABLES], [IFCONFIG], [ROUTE], " +
            "/etc/dnsmasq.conf, /etc/resolv.conf, /etc/haproxy.conf, /etc/hosts.conf, /etcdnsmaq-resolv.conf, /var/log/cloud.log, " +
            "/var/log/routerServiceMonitor.log, /var/log/dnsmasq.log",
            "List of supported diagnostics data file options for the VR", true);

    ConfigKey<String> CpvmDefaultSupportedFiles = new ConfigKey<>("Advanced", String.class,
            "diagnostics.data.cpvm.defaults", "[IPTABLES], [IFCONFIG], [ROUTE], /usr/local/cloud/systemvm/conf/agent.properties, " +
            "/usr/local/cloud/systemvm/conf/consoleproxy.properties, /var/log/cloud.log",
            "List of supported diagnostics data file options for the cpvm", true);

    List<String> generateFileList();
}
