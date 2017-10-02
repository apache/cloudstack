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

public interface ConsoleProxyManager extends Manager, ConsoleProxyService {

    public static final int DEFAULT_PROXY_CAPACITY = 50;
    public static final int DEFAULT_STANDBY_CAPACITY = 10;
    public static final int DEFAULT_PROXY_VM_RAMSIZE = 1024;            // 1G
    public static final int DEFAULT_PROXY_VM_CPUMHZ = 500;                // 500 MHz

    public static final int DEFAULT_PROXY_CMD_PORT = 8001;
    public static final int DEFAULT_PROXY_VNC_PORT = 0;
    public static final int DEFAULT_PROXY_URL_PORT = 80;
    public static final int DEFAULT_PROXY_SESSION_TIMEOUT = 300000;        // 5 minutes

    public static final String ALERT_SUBJECT = "proxy-alert";
    public static final String CERTIFICATE_NAME = "CPVMCertificate";

    public void setManagementState(ConsoleProxyManagementState state);

    public ConsoleProxyManagementState getManagementState();

    public void resumeLastManagementState();

    public ConsoleProxyVO startProxy(long proxyVmId, boolean ignoreRestartSetting);

    public boolean stopProxy(long proxyVmId);

    public boolean rebootProxy(long proxyVmId);

    public boolean destroyProxy(long proxyVmId);

}
