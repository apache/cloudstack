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
package com.cloud.consoleproxy;

import com.cloud.utils.component.Manager;
import com.cloud.vm.ConsoleProxyVO;

import org.apache.cloudstack.framework.config.ConfigKey;

public interface ConsoleProxyManager extends Manager, ConsoleProxyService {

    int DEFAULT_PROXY_CAPACITY = 50;
    int DEFAULT_STANDBY_CAPACITY = 10;
    int DEFAULT_PROXY_VM_RAMSIZE = 1024;            // 1G
    int DEFAULT_PROXY_VM_CPUMHZ = 500;                // 500 MHz

    int DEFAULT_PROXY_CMD_PORT = 8001;
    int DEFAULT_PROXY_VNC_PORT = 0;
    int DEFAULT_PROXY_URL_PORT = 80;
    int DEFAULT_PROXY_SESSION_TIMEOUT = 300000;        // 5 minutes

    String ALERT_SUBJECT = "proxy-alert";
    String CERTIFICATE_NAME = "CPVMCertificate";

    ConfigKey<Boolean> NoVncConsoleDefault = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class, "novnc.console.default", "true",
        "If true, noVNC console will be default console for virtual machines", true);

    ConfigKey<Boolean> NoVncConsoleSourceIpCheckEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class, "novnc.console.sourceip.check.enabled", "false",
        "If true, The source IP to access novnc console must be same as the IP in request to management server for console URL. Needs to reconnect CPVM to management server when this changes (via restart CPVM, or management server, or cloud service in CPVM)", false);

    void setManagementState(ConsoleProxyManagementState state);

    ConsoleProxyManagementState getManagementState();

    void resumeLastManagementState();

    ConsoleProxyVO startProxy(long proxyVmId, boolean ignoreRestartSetting);

    boolean stopProxy(long proxyVmId);

    boolean rebootProxy(long proxyVmId);

    boolean destroyProxy(long proxyVmId);

    int getVncPort();

}
