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
package com.cloud.network.router;

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;

import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.Manager;
import com.cloud.vm.DomainRouterVO;

/**
 * NetworkManager manages the network for the different end users.
 */
public interface VirtualNetworkApplianceManager extends Manager, VirtualNetworkApplianceService {

    static final String RouterTemplateXenCK = "router.template.xenserver";
    static final String RouterTemplateKvmCK = "router.template.kvm";
    static final String RouterTemplateVmwareCK = "router.template.vmware";
    static final String RouterTemplateHyperVCK = "router.template.hyperv";
    static final String RouterTemplateLxcCK = "router.template.lxc";
    static final String RouterTemplateOvm3CK = "router.template.ovm3";
    static final String SetServiceMonitorCK = "network.router.EnableServiceMonitoring";
    static final String RouterAlertsCheckIntervalCK = "router.alerts.check.interval";

    static final ConfigKey<String> RouterTemplateXen = new ConfigKey<String>(String.class, RouterTemplateXenCK, "Advanced", "SystemVM Template (XenServer)",
            "Name of the default router template on Xenserver.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateKvm = new ConfigKey<String>(String.class, RouterTemplateKvmCK, "Advanced", "SystemVM Template (KVM)",
            "Name of the default router template on KVM.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateVmware = new ConfigKey<String>(String.class, RouterTemplateVmwareCK, "Advanced", "SystemVM Template (vSphere)",
            "Name of the default router template on Vmware.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateHyperV = new ConfigKey<String>(String.class, RouterTemplateHyperVCK, "Advanced", "SystemVM Template (HyperV)",
            "Name of the default router template on Hyperv.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateLxc = new ConfigKey<String>(String.class, RouterTemplateLxcCK, "Advanced", "SystemVM Template (LXC)",
            "Name of the default router template on LXC.", true, ConfigKey.Scope.Zone, null);
    static final ConfigKey<String> RouterTemplateOvm3 = new ConfigKey<String>(String.class, RouterTemplateOvm3CK, "Advanced", "SystemVM Template (Ovm3)",
            "Name of the default router template on Ovm3.", true, ConfigKey.Scope.Zone, null);

    static final ConfigKey<String> SetServiceMonitor = new ConfigKey<String>(String.class, SetServiceMonitorCK, "Advanced", "true",
            "service monitoring in router enable/disable option, default true", true, ConfigKey.Scope.Zone, null);

    static final ConfigKey<Integer> RouterAlertsCheckInterval = new ConfigKey<Integer>(Integer.class, RouterAlertsCheckIntervalCK, "Advanced", "1800",
            "Interval (in seconds) to check for alerts in Virtual Router.", false, ConfigKey.Scope.Global, null);
    static final ConfigKey<Boolean> routerVersionCheckEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "router.version.check", "true",
            "If true, router minimum required version is checked before sending command", false);
    static final ConfigKey<Boolean> UseExternalDnsServers = new ConfigKey<Boolean>(Boolean.class, "use.external.dns", "Advanced", "false",
            "Bypass internal dns, use external dns1 and dns2", true, ConfigKey.Scope.Zone, null);

    public static final int DEFAULT_ROUTER_VM_RAMSIZE = 256;            // 256M
    public static final int DEFAULT_ROUTER_CPU_MHZ = 500;                // 500 MHz
    public static final boolean USE_POD_VLAN = false;
    public static final int DEFAULT_PRIORITY = 100;
    public static final int DEFAULT_DELTA = 2;

    /**
    /*
     * Send ssh public/private key pair to specified host
     * @param hostId
     * @param pubKey
     * @param prvKey
     *
     * NOT USED IN THE VIRTUAL NET APPLIANCE
     *
     */
    //boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey):

    boolean startRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;

    boolean deleteRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException;

    List<VirtualRouter> getRoutersForNetwork(long networkId);

    VirtualRouter stop(VirtualRouter router, boolean forced, User callingUser, Account callingAccount) throws ConcurrentOperationException, ResourceUnavailableException;

    String getDnsBasicZoneUpdate();

    boolean removeDhcpSupportForSubnet(Network network, List<DomainRouterVO> routers) throws ResourceUnavailableException;

    public boolean prepareAggregatedExecution(Network network, List<DomainRouterVO> routers) throws AgentUnavailableException, ResourceUnavailableException;

    public boolean completeAggregatedExecution(Network network, List<DomainRouterVO> routers) throws AgentUnavailableException, ResourceUnavailableException;
}
