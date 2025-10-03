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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.deploy.DeploymentPlanner;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.component.Manager;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

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

    ConfigKey<Boolean> ConsoleProxySslEnabled = new ConfigKey<>(Boolean.class, "consoleproxy.sslEnabled",  ConfigKey.CATEGORY_ADVANCED, "false",
            "Enable SSL for console proxy", false, ConfigKey.Scope.Zone, null);

    ConfigKey<Boolean> NoVncConsoleDefault = new ConfigKey<>(Boolean.class, "novnc.console.default", ConfigKey.CATEGORY_ADVANCED, "true",
        "If true, noVNC console will be default console for virtual machines", false, ConfigKey.Scope.Zone, null);

    ConfigKey<Boolean> NoVncConsoleSourceIpCheckEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class, "novnc.console.sourceip.check.enabled", "false",
        "If true, The source IP to access novnc console must be same as the IP in request to management server for console URL. Needs to reconnect CPVM to management server when this changes (via restart CPVM, or management server, or cloud service in CPVM)", false);

    ConfigKey<Boolean> NoVncConsoleShowDot = new ConfigKey<>(Boolean.class, "novnc.console.show.dot", ConfigKey.CATEGORY_ADVANCED, "true",
            "If true, in noVNC console a dot cursor will be shown when the remote server provides no local cursor, or provides a fully-transparent (invisible) cursor.",
            true, ConfigKey.Scope.Zone, null);

    ConfigKey<String> ConsoleProxyServiceOffering = new ConfigKey<>(String.class, "consoleproxy.service.offering", "Console Proxy", null,
            "Uuid of the service offering used by console proxy; if NULL - system offering will be used", true, ConfigKey.Scope.Zone, null);

    ConfigKey<String> ConsoleProxyCapacityStandby = new ConfigKey<>(String.class, "consoleproxy.capacity.standby", "Console Proxy", String.valueOf(DEFAULT_STANDBY_CAPACITY),
            "The minimal number of console proxy viewer sessions that system is able to serve immediately(standby capacity)", false, ConfigKey.Scope.Zone, null);

    ConfigKey<String> ConsoleProxyCapacityScanInterval = new ConfigKey<>(String.class, "consoleproxy.capacityscan.interval", "Console Proxy", "30000",
            "The time interval(in millisecond) to scan whether or not system needs more console proxy to ensure minimal standby capacity", false, null);

    ConfigKey<Integer> ConsoleProxyCmdPort = new ConfigKey<>(Integer.class, "consoleproxy.cmd.port", "Console Proxy", String.valueOf(DEFAULT_PROXY_CMD_PORT),
            "Console proxy command port that is used to communicate with management server", false, ConfigKey.Scope.Zone, null);

    ConfigKey<Boolean> ConsoleProxyRestart = new ConfigKey<>(Boolean.class, "consoleproxy.restart", "Console Proxy", "true",
            "Console proxy restart flag, defaults to true", true, ConfigKey.Scope.Zone, null);

    ConfigKey<String> ConsoleProxyUrlDomain = new ConfigKey<>(String.class, "consoleproxy.url.domain", "Console Proxy", "",
            "Console proxy url domain - domainName,privateip", false, ConfigKey.Scope.Zone, null);

    ConfigKey<Integer> ConsoleProxySessionMax = new ConfigKey<>(Integer.class, "consoleproxy.session.max", "Console Proxy", String.valueOf(DEFAULT_PROXY_CAPACITY),
            "The max number of viewer sessions console proxy is configured to serve for", true, ConfigKey.Scope.Zone, null);

    ConfigKey<Integer> ConsoleProxySessionTimeout = new ConfigKey<>(Integer.class, "consoleproxy.session.timeout", "Console Proxy", String.valueOf(DEFAULT_PROXY_SESSION_TIMEOUT),
            "Timeout(in milliseconds) that console proxy tries to maintain a viewer session before it times out the session for no activity", true, ConfigKey.Scope.Zone, null);

    ConfigKey<Boolean> ConsoleProxyDisableRpFilter = new ConfigKey<>(Boolean.class, "consoleproxy.disable.rpfilter", "Console Proxy", "true",
            "disable rp_filter on console proxy VM public interface", true, ConfigKey.Scope.Zone, null);

    ConfigKey<Integer> ConsoleProxyLaunchMax = new ConfigKey<>(Integer.class, "consoleproxy.launch.max", "Console Proxy", "10",
            "maximum number of console proxy instances per zone can be launched", false, ConfigKey.Scope.Zone, null);

    String consoleProxyManagementStates = Arrays.stream(com.cloud.consoleproxy.ConsoleProxyManagementState.values()).map(Enum::name).collect(Collectors.joining(","));
    ConfigKey<String> ConsoleProxyServiceManagementState = new ConfigKey<String>(ConfigKey.CATEGORY_ADVANCED, String.class, "consoleproxy.management.state", com.cloud.consoleproxy.ConsoleProxyManagementState.Auto.toString(),
            "console proxy service management state", false, ConfigKey.Kind.Select, consoleProxyManagementStates);

    ConfigKey<String> ConsoleProxyManagementLastState = new ConfigKey<String>(ConfigKey.CATEGORY_ADVANCED, String.class, "consoleproxy.management.state.last", com.cloud.consoleproxy.ConsoleProxyManagementState.Auto.toString(),
            "last console proxy service management state", false, ConfigKey.Kind.Select, consoleProxyManagementStates);

    void setManagementState(ConsoleProxyManagementState state);

    ConsoleProxyManagementState getManagementState();

    void resumeLastManagementState();

    ConsoleProxyVO startProxy(long proxyVmId, boolean ignoreRestartSetting);

    void startProxyForHA(VirtualMachine vm, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlanner planner)
            throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException,
            OperationTimedoutException;

    boolean stopProxy(long proxyVmId);

    boolean rebootProxy(long proxyVmId);

    boolean destroyProxy(long proxyVmId);

    int getVncPort(Long dataCenterId);

}
