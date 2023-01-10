/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.agent.manager;

import javax.inject.Inject;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.ReplugNicAnswer;
import com.cloud.agent.api.ReplugNicCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetFirewallRulesAnswer;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetIpv6FirewallRulesAnswer;
import com.cloud.agent.api.routing.SetIpv6FirewallRulesCommand;
import com.cloud.agent.api.routing.SetNetworkACLAnswer;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesAnswer;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatAnswer;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesAnswer;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteAnswer;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.simulator.MockVMVO;
import com.cloud.simulator.dao.MockVMDao;
import com.cloud.utils.component.ManagerBase;

public class MockNetworkManagerImpl extends ManagerBase implements MockNetworkManager {

    @Inject
    MockVMDao _mockVmDao;

    @Override
    public Answer SetStaticNatRules(SetStaticNatRulesCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer SetPortForwardingRules(SetPortForwardingRulesCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public SetFirewallRulesAnswer SetFirewallRules(SetFirewallRulesCommand cmd) {
        String[] results = new String[cmd.getRules().length];
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        if (routerIp == null) {
            return new SetFirewallRulesAnswer(cmd, false, results);
        }

        String[][] rules = cmd.generateFwRules();
        StringBuilder sb = new StringBuilder();
        String[] fwRules = rules[0];
        if (fwRules.length > 0) {
            for (int i = 0; i < fwRules.length; i++) {
                sb.append(fwRules[i]).append(',');
            }
        }
        return new SetFirewallRulesAnswer(cmd, true, results);
    }

    @Override
    public SetIpv6FirewallRulesAnswer SetIpv6FirewallRules(SetIpv6FirewallRulesCommand cmd) {
        String[] results = new String[cmd.getRules().length];
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        if (routerIp == null) {
            return new SetIpv6FirewallRulesAnswer(cmd, false, results);
        }
        return new SetIpv6FirewallRulesAnswer(cmd, true, results);
    }

    @Override
    public NetworkUsageAnswer getNetworkUsage(NetworkUsageCommand cmd) {
        return new NetworkUsageAnswer(cmd, null, 100L, 100L);
    }

    @Override
    public Answer IpAssoc(IpAssocCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer LoadBalancerConfig(LoadBalancerConfigCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer AddDhcpEntry(DhcpEntryCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer setupPVLAN(PvlanSetupCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public PlugNicAnswer plugNic(PlugNicCommand cmd) {
        String vmname = cmd.getVmName();
        if (_mockVmDao.findByVmName(vmname) != null) {
            logger.debug("Plugged NIC (dev=" + cmd.getNic().getDeviceId() + ", " + cmd.getNic().getIp() + ") into " + cmd.getVmName());
            return new PlugNicAnswer(cmd, true, "success");
        }
        logger.error("Plug NIC failed for (dev=" + cmd.getNic().getDeviceId() + ", " + cmd.getNic().getIp() + ") into " + cmd.getVmName());
        return new PlugNicAnswer(cmd, false, "failure");
    }

    @Override
    public UnPlugNicAnswer unplugNic(UnPlugNicCommand cmd) {
        String vmname = cmd.getVmName();
        if (_mockVmDao.findByVmName(vmname) != null) {
            logger.debug("Unplugged NIC (dev=" + cmd.getNic().getDeviceId() + ", " + cmd.getNic().getIp() + ") into " + cmd.getVmName());
            return new UnPlugNicAnswer(cmd, true, "success");
        }
        logger.error("Unplug NIC failed for (dev=" + cmd.getNic().getDeviceId() + ", " + cmd.getNic().getIp() + ") into " + cmd.getVmName());
        return new UnPlugNicAnswer(cmd, false, "failure");
    }

    @Override
    public ReplugNicAnswer replugNic(ReplugNicCommand cmd) {
        String vmname = cmd.getVmName();
        if (_mockVmDao.findByVmName(vmname) != null) {
            logger.debug("Replugged NIC (dev=" + cmd.getNic().getDeviceId() + ", " + cmd.getNic().getIp() + ") into " + cmd.getVmName());
            return new ReplugNicAnswer(cmd, true, "success");
        }
        logger.error("Replug NIC failed for (dev=" + cmd.getNic().getDeviceId() + ", " + cmd.getNic().getIp() + ") into " + cmd.getVmName());
        return new ReplugNicAnswer(cmd, false, "failure");
    }

    @Override
    public IpAssocAnswer ipAssoc(IpAssocVpcCommand cmd) {
        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        IpAddressTO[] ips = cmd.getIpAddresses();
        for (IpAddressTO ip : ips) {
            results[i++] = ip.getPublicIp() + " - success";
        }
        return new IpAssocAnswer(cmd, results);
    }

    @Override
    public SetSourceNatAnswer setSourceNat(SetSourceNatCommand cmd) {
        return new SetSourceNatAnswer(cmd, true, "success");
    }

    @Override
    public SetNetworkACLAnswer setNetworkAcl(SetNetworkACLCommand cmd) {
        String[] results = new String[cmd.getRules().length];
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);

        StringBuilder sb = new StringBuilder();
        sb.append(routerIp);
        sb.append(routerName);

        String[][] rules = cmd.generateFwRules();
        String[] aclRules = rules[0];

        for (int i = 0; i < aclRules.length; i++) {
            sb.append(aclRules[i]).append(',');
        }
        return new SetNetworkACLAnswer(cmd, true, results);
    }

    @Override
    public SetPortForwardingRulesAnswer setVpcPortForwards(SetPortForwardingRulesVpcCommand cmd) {
        String[] results = new String[cmd.getRules().length];
        StringBuilder sb = new StringBuilder();
        for (PortForwardingRuleTO rule : cmd.getRules()) {
            sb.append("src:");
            sb.append(rule.getStringSrcPortRange());
            sb.append("dst:");
            sb.append(rule.getStringDstPortRange());
        }
        return new SetPortForwardingRulesAnswer(cmd, results, true);
    }

    @Override
    public SetStaticRouteAnswer setStaticRoute(SetStaticRouteCommand cmd) {
        String[] results = new String[cmd.getStaticRoutes().length];
        String[] rules = cmd.generateSRouteRules();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rules.length; i++) {
            sb.append(rules[i]).append(',');
        }
        return new SetStaticRouteAnswer(cmd, true, results);
    }

    @Override
    public Answer setUpGuestNetwork(SetupGuestNetworkCommand cmd) {
        String domrName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        try {
            MockVMVO vms = _mockVmDao.findByVmName(domrName);
            if (vms == null) {
                return new Answer(cmd, false, "Can not find VM " + domrName);
            }
            return new Answer(cmd, true, "success");
        } catch (Exception e) {
            String msg = "Creating guest network failed due to " + e.toString();
            logger.warn(msg, e);
            return new Answer(cmd, false, msg);
        }
    }

    @Override
    public SetStaticNatRulesAnswer setVPCStaticNatRules(SetStaticNatRulesCommand cmd) {
        String[] results = new String[cmd.getRules().length];
        return new SetStaticNatRulesAnswer(cmd, results, true);
    }

    @Override
    public Answer siteToSiteVpn(Site2SiteVpnCfgCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer checkSiteToSiteVpnConnection(CheckS2SVpnConnectionsCommand cmd) {
        return new Answer(cmd);
    }
}
