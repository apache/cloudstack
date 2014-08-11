//
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
//

package com.cloud.agent.resource.virtualnetwork;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.CreateIpAliasCommand;
import com.cloud.agent.api.routing.DeleteIpAliasCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetMonitorServiceCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.to.DhcpTO;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.resource.virtualnetwork.model.AclRule;
import com.cloud.agent.resource.virtualnetwork.model.AllAclRule;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.ForwardingRule;
import com.cloud.agent.resource.virtualnetwork.model.ForwardingRules;
import com.cloud.agent.resource.virtualnetwork.model.GuestNetwork;
import com.cloud.agent.resource.virtualnetwork.model.IcmpAclRule;
import com.cloud.agent.resource.virtualnetwork.model.IpAddress;
import com.cloud.agent.resource.virtualnetwork.model.IpAssociation;
import com.cloud.agent.resource.virtualnetwork.model.NetworkACL;
import com.cloud.agent.resource.virtualnetwork.model.ProtocolAclRule;
import com.cloud.agent.resource.virtualnetwork.model.StaticNatRule;
import com.cloud.agent.resource.virtualnetwork.model.StaticNatRules;
import com.cloud.agent.resource.virtualnetwork.model.TcpAclRule;
import com.cloud.agent.resource.virtualnetwork.model.UdpAclRule;
import com.cloud.agent.resource.virtualnetwork.model.VmData;
import com.cloud.agent.resource.virtualnetwork.model.VmDhcpConfig;
import com.cloud.agent.resource.virtualnetwork.model.VmPassword;
import com.cloud.agent.resource.virtualnetwork.model.VpnUser;
import com.cloud.agent.resource.virtualnetwork.model.VpnUserList;
import com.cloud.network.HAProxyConfigurator;
import com.cloud.network.LoadBalancerConfigurator;
import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigHelper {
    private final static Gson gson;

    static {
        gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    public static List<ConfigItem> generateCommandCfg(NetworkElementCommand cmd) {
        List<ConfigItem> cfg;
        if (cmd instanceof SetPortForwardingRulesVpcCommand) {
            cfg = generateConfig((SetPortForwardingRulesVpcCommand)cmd);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            cfg = generateConfig((SetPortForwardingRulesCommand)cmd); // Migrated
        } else if (cmd instanceof SetStaticRouteCommand) {
            cfg = generateConfig((SetStaticRouteCommand)cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            cfg = generateConfig((SetStaticNatRulesCommand)cmd);  // Migrated
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            cfg = generateConfig((LoadBalancerConfigCommand)cmd);
        } else if (cmd instanceof SavePasswordCommand) {
            cfg = generateConfig((SavePasswordCommand)cmd);  // Migrated
        } else if (cmd instanceof DhcpEntryCommand) {
            cfg = generateConfig((DhcpEntryCommand)cmd);  // Migrated
        } else if (cmd instanceof CreateIpAliasCommand) {
            cfg = generateConfig((CreateIpAliasCommand)cmd);
        } else if (cmd instanceof DnsMasqConfigCommand) {
            cfg = generateConfig((DnsMasqConfigCommand)cmd);
        } else if (cmd instanceof DeleteIpAliasCommand) {
            cfg = generateConfig((DeleteIpAliasCommand)cmd);
        } else if (cmd instanceof VmDataCommand) {
            cfg = generateConfig((VmDataCommand)cmd); // Migrated
        } else if (cmd instanceof SetFirewallRulesCommand) {
            cfg = generateConfig((SetFirewallRulesCommand)cmd);
        } else if (cmd instanceof BumpUpPriorityCommand) {
            cfg = generateConfig((BumpUpPriorityCommand)cmd);
        } else if (cmd instanceof RemoteAccessVpnCfgCommand) {
            cfg = generateConfig((RemoteAccessVpnCfgCommand)cmd);
        } else if (cmd instanceof VpnUsersCfgCommand) {
            cfg = generateConfig((VpnUsersCfgCommand)cmd); // Migrated
        } else if (cmd instanceof Site2SiteVpnCfgCommand) {
            cfg = generateConfig((Site2SiteVpnCfgCommand)cmd);
        } else if (cmd instanceof SetMonitorServiceCommand) {
            cfg = generateConfig((SetMonitorServiceCommand)cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            cfg = generateConfig((SetupGuestNetworkCommand)cmd); // Migrated
        } else if (cmd instanceof SetNetworkACLCommand) {
            cfg = generateConfig((SetNetworkACLCommand)cmd); // Migrated
        } else if (cmd instanceof SetSourceNatCommand) {
            cfg = generateConfig((SetSourceNatCommand)cmd); // Migrated - ignored
        } else if (cmd instanceof IpAssocCommand) {
            cfg = generateConfig((IpAssocCommand)cmd); // Migrated
        } else {
            return null;
        }
        return cfg;
    }


    private static List<ConfigItem> generateConfig(VpnUsersCfgCommand cmd) {

        List<VpnUser> vpnUsers = new LinkedList<VpnUser>();
        for (VpnUsersCfgCommand.UsernamePassword userpwd : cmd.getUserpwds()) {
            vpnUsers.add(new VpnUser(userpwd.getUsername(), userpwd.getPassword(), userpwd.isAdd()));
        }

        VpnUserList vpnUserList = new VpnUserList(vpnUsers);
        return generateConfigItems(vpnUserList);
    }

    private static List<ConfigItem> generateConfig(RemoteAccessVpnCfgCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        String args = "";
        if (cmd.isCreate()) {
            args += "-r ";
            args += cmd.getIpRange();
            args += " -p ";
            args += cmd.getPresharedKey();
            args += " -s ";
            args += cmd.getVpnServerIp();
            args += " -l ";
            args += cmd.getLocalIp();
            args += " -c ";
        } else {
            args += "-d ";
            args += " -s ";
            args += cmd.getVpnServerIp();
        }
        args += " -C " + cmd.getLocalCidr();
        args += " -i " + cmd.getPublicInterface();
        cfg.add(new ScriptConfigItem(VRScripts.VPN_L2TP, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetFirewallRulesCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String egressDefault = cmd.getAccessDetail(NetworkElementCommand.FIREWALL_EGRESS_DEFAULT);

        FirewallRuleTO[] allrules = cmd.getRules();
        FirewallRule.TrafficType trafficType = allrules[0].getTrafficType();

        String[][] rules = cmd.generateFwRules();
        String args = " -F";

        if (trafficType == FirewallRule.TrafficType.Egress) {
            args += " -E";
            if (egressDefault.equals("true")) {
                args += " -P 1";
            } else if (egressDefault.equals("System")) {
                args += " -P 2";
            } else {
                args += " -P 0";
            }
        }

        StringBuilder sb = new StringBuilder();
        String[] fwRules = rules[0];
        if (fwRules.length > 0) {
            for (int i = 0; i < fwRules.length; i++) {
                sb.append(fwRules[i]).append(',');
            }
            args += " -a " + sb.toString();
        }

        if (trafficType == FirewallRule.TrafficType.Egress) {
            cfg.add(new ScriptConfigItem(VRScripts.FIREWALL_EGRESS, args));
        } else {
            cfg.add(new ScriptConfigItem(VRScripts.FIREWALL_INGRESS, args));
        }

        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetPortForwardingRulesCommand cmd) {
        List<ForwardingRule> rules = new ArrayList<ForwardingRule>();

        for (PortForwardingRuleTO rule : cmd.getRules()) {
            ForwardingRule fwdRule = new ForwardingRule(rule.revoked(), rule.getProtocol().toLowerCase(), rule.getSrcIp(), rule.getStringSrcPortRange(), rule.getDstIp(),
                    rule.getStringDstPortRange());
            rules.add(fwdRule);
        }

        ForwardingRules ruleSet = new ForwardingRules(rules.toArray(new ForwardingRule[rules.size()]));

        return generateConfigItems(ruleSet);
    }

    private static List<ConfigItem> generateConfig(SetStaticNatRulesCommand cmd) {

        LinkedList<StaticNatRule> rules = new LinkedList<>();
        for (StaticNatRuleTO rule : cmd.getRules()) {
            StaticNatRule staticNatRule = new StaticNatRule(rule.revoked(), rule.getProtocol(), rule.getSrcIp(), rule.getStringSrcPortRange(), rule.getDstIp());
            rules.add(staticNatRule);
        }
        StaticNatRules staticNatRules = new StaticNatRules(rules);

        return generateConfigItems(staticNatRules);
    }

    private static List<ConfigItem> generateConfig(LoadBalancerConfigCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        LoadBalancerConfigurator cfgtr = new HAProxyConfigurator();

        String[] config = cfgtr.generateConfiguration(cmd);
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < config.length; i++) {
            buff.append(config[i]);
            buff.append("\n");
        }
        String tmpCfgFilePath = "/etc/haproxy/";
        String tmpCfgFileName = "haproxy.cfg.new." + String.valueOf(System.currentTimeMillis());
        cfg.add(new FileConfigItem(tmpCfgFilePath, tmpCfgFileName, buff.toString()));

        String[][] rules = cfgtr.generateFwRules(cmd);

        String[] addRules = rules[LoadBalancerConfigurator.ADD];
        String[] removeRules = rules[LoadBalancerConfigurator.REMOVE];
        String[] statRules = rules[LoadBalancerConfigurator.STATS];

        String args = " -f " + tmpCfgFilePath + tmpCfgFileName;
        StringBuilder sb = new StringBuilder();
        if (addRules.length > 0) {
            for (int i = 0; i < addRules.length; i++) {
                sb.append(addRules[i]).append(',');
            }
            args += " -a " + sb.toString();
        }

        sb = new StringBuilder();
        if (removeRules.length > 0) {
            for (int i = 0; i < removeRules.length; i++) {
                sb.append(removeRules[i]).append(',');
            }

            args += " -d " + sb.toString();
        }

        sb = new StringBuilder();
        if (statRules.length > 0) {
            for (int i = 0; i < statRules.length; i++) {
                sb.append(statRules[i]).append(',');
            }

            args += " -s " + sb.toString();
        }

        if (cmd.getVpcId() == null) {
            args = " -i " + routerIp + args;
            cfg.add(new ScriptConfigItem(VRScripts.LB, args));
        } else {
            args = " -i " + cmd.getNic().getIp() + args;
            cfg.add(new ScriptConfigItem(VRScripts.VPC_LB, args));
        }

        return cfg;
    }

    private static List<ConfigItem> generateConfig(VmDataCommand cmd) {
        VmData vmData = new VmData(cmd.getVmIpAddress(), cmd.getVmData());

        return generateConfigItems(vmData);
    }

    private static List<ConfigItem> generateConfig(SavePasswordCommand cmd) {
        VmPassword vmPassword = new VmPassword(cmd.getVmIpAddress(), cmd.getPassword());

        return generateConfigItems(vmPassword);
    }

    private static List<ConfigItem> generateConfig(DhcpEntryCommand cmd) {
        VmDhcpConfig vmDhcpConfig = new VmDhcpConfig(cmd.getVmName(), cmd.getVmMac(), cmd.getVmIpAddress(), cmd.getVmIp6Address(), cmd.getDuid(), cmd.getDefaultDns(),
                cmd.getDefaultRouter(), cmd.getStaticRoutes(), cmd.isDefault());

        return generateConfigItems(vmDhcpConfig);
    }

    private static List<ConfigItem> generateConfig(CreateIpAliasCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        List<IpAliasTO> ipAliasTOs = cmd.getIpAliasList();
        StringBuilder args = new StringBuilder();
        for (IpAliasTO ipaliasto : ipAliasTOs) {
            args.append(ipaliasto.getAlias_count());
            args.append(':');
            args.append(ipaliasto.getRouterip());
            args.append(':');
            args.append(ipaliasto.getNetmask());
            args.append('-');
        }
        cfg.add(new ScriptConfigItem(VRScripts.IPALIAS_CREATE, args.toString()));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(DeleteIpAliasCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        StringBuffer buff = new StringBuffer();
        List<IpAliasTO> revokedIpAliasTOs = cmd.getDeleteIpAliasTos();
        for (IpAliasTO ipAliasTO : revokedIpAliasTOs) {
            buff.append(ipAliasTO.getAlias_count());
            buff.append(":");
            buff.append(ipAliasTO.getRouterip());
            buff.append(":");
            buff.append(ipAliasTO.getNetmask());
            buff.append("-");
        }
        //this is to ensure that thre is some argument passed to the deleteipAlias script  when there are no revoked rules.
        buff.append("- ");
        List<IpAliasTO> activeIpAliasTOs = cmd.getCreateIpAliasTos();
        for (IpAliasTO ipAliasTO : activeIpAliasTOs) {
            buff.append(ipAliasTO.getAlias_count());
            buff.append(":");
            buff.append(ipAliasTO.getRouterip());
            buff.append(":");
            buff.append(ipAliasTO.getNetmask());
            buff.append("-");
        }
        cfg.add(new ScriptConfigItem(VRScripts.IPALIAS_DELETE, buff.toString()));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(DnsMasqConfigCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        List<DhcpTO> dhcpTos = cmd.getIps();
        StringBuffer buff = new StringBuffer();
        for (DhcpTO dhcpTo : dhcpTos) {
            buff.append(dhcpTo.getRouterIp());
            buff.append(":");
            buff.append(dhcpTo.getGateway());
            buff.append(":");
            buff.append(dhcpTo.getNetmask());
            buff.append(":");
            buff.append(dhcpTo.getStartIpOfSubnet());
            buff.append("-");
        }
        cfg.add(new ScriptConfigItem(VRScripts.DNSMASQ_CONFIG, buff.toString()));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(BumpUpPriorityCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        cfg.add(new ScriptConfigItem(VRScripts.RVR_BUMPUP_PRI, null));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(Site2SiteVpnCfgCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String args = "";
        if (cmd.isCreate()) {
            args += "-A";
            args += " -l ";
            args += cmd.getLocalPublicIp();
            args += " -n ";
            args += cmd.getLocalGuestCidr();
            args += " -g ";
            args += cmd.getLocalPublicGateway();
            args += " -r ";
            args += cmd.getPeerGatewayIp();
            args += " -N ";
            args += cmd.getPeerGuestCidrList();
            args += " -e ";
            args += "\"" + cmd.getEspPolicy() + "\"";
            args += " -i ";
            args += "\"" + cmd.getIkePolicy() + "\"";
            args += " -t ";
            args += Long.toString(cmd.getIkeLifetime());
            args += " -T ";
            args += Long.toString(cmd.getEspLifetime());
            args += " -s ";
            args += "\"" + cmd.getIpsecPsk() + "\"";
            args += " -d ";
            if (cmd.getDpd()) {
                args += "1";
            } else {
                args += "0";
            }
            if (cmd.isPassive()) {
                args += " -p ";
            }
        } else {
            args += "-D";
            args += " -r ";
            args += cmd.getPeerGatewayIp();
            args += " -n ";
            args += cmd.getLocalGuestCidr();
            args += " -N ";
            args += cmd.getPeerGuestCidrList();
        }

        cfg.add(new ScriptConfigItem(VRScripts.S2SVPN_IPSEC, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetMonitorServiceCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String config = cmd.getConfiguration();
        String disableMonitoring = cmd.getAccessDetail(NetworkElementCommand.ROUTER_MONITORING_ENABLE);

        String args = " -c " + config;
        if (disableMonitoring != null) {
            args = args + " -d";
        }

        cfg.add(new ScriptConfigItem(VRScripts.MONITOR_SERVICE, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetupGuestNetworkCommand cmd) {
        NicTO nic = cmd.getNic();
        String routerGIP = cmd.getAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP);
        String gateway = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY);
        String cidr = Long.toString(NetUtils.getCidrSize(nic.getNetmask()));
        String netmask = nic.getNetmask();
        String domainName = cmd.getNetworkDomain();
        String dns = cmd.getDefaultDns1();

        if (dns == null || dns.isEmpty()) {
            dns = cmd.getDefaultDns2();
        } else {
            String dns2 = cmd.getDefaultDns2();
            if (dns2 != null && !dns2.isEmpty()) {
                dns += "," + dns2;
            }
        }

        GuestNetwork guestNetwork = new GuestNetwork(cmd.isAdd(), nic.getMac(), "eth" + nic.getDeviceId(), routerGIP, netmask, gateway,
                cidr, dns, domainName);

        return generateConfigItems(guestNetwork);
    }

    private static List<ConfigItem> generateConfig(SetNetworkACLCommand cmd) {
        String privateGw = cmd.getAccessDetail(NetworkElementCommand.VPC_PRIVATE_GATEWAY);

        String[][] rules = cmd.generateFwRules();
        String[] aclRules = rules[0];
        NicTO nic = cmd.getNic();
        String dev = "eth" + nic.getDeviceId();
        String netmask = Long.toString(NetUtils.getCidrSize(nic.getNetmask()));

        List<AclRule> ingressRules = new ArrayList<AclRule>();
        List<AclRule> egressRules = new ArrayList<AclRule>();

        for (int i = 0; i < aclRules.length; i++) {
            AclRule aclRule;
            String[] ruleParts = aclRules[i].split(":");
            switch (ruleParts[1].toLowerCase()) {
            case "icmp":
                aclRule = new IcmpAclRule(ruleParts[4], "ACCEPT".equals(ruleParts[5]), Integer.parseInt(ruleParts[2]), Integer.parseInt(ruleParts[3]));
                break;
            case "tcp":
                aclRule = new TcpAclRule(ruleParts[4], "ACCEPT".equals(ruleParts[5]), Integer.parseInt(ruleParts[2]), Integer.parseInt(ruleParts[3]));
                break;
            case "udp":
                aclRule = new UdpAclRule(ruleParts[4], "ACCEPT".equals(ruleParts[5]), Integer.parseInt(ruleParts[2]), Integer.parseInt(ruleParts[3]));
                break;
            case "all":
                aclRule = new AllAclRule(ruleParts[4], "ACCEPT".equals(ruleParts[5]));
                break;
            default:
                aclRule = new ProtocolAclRule(ruleParts[4], "ACCEPT".equals(ruleParts[5]), Integer.parseInt(ruleParts[1]));
            }
            if ("Ingress".equals(ruleParts[0])) {
                ingressRules.add(aclRule);
            } else {
                egressRules.add(aclRule);
            }
        }

        NetworkACL networkACL = new NetworkACL(dev, nic.getMac(), privateGw != null, nic.getIp(), netmask, ingressRules.toArray(new AclRule[ingressRules.size()]),
                egressRules.toArray(new AclRule[egressRules.size()]));

        return generateConfigItems(networkACL);
    }

    private static List<ConfigItem> generateConfig(SetSourceNatCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        /* FIXME This seems useless as we already pass this info with the ipassoc
         * IpAddressTO pubIP = cmd.getIpAddress();
         * String dev = "eth" + pubIP.getNicDevId();
         * String args = "-A";
         * args += " -l ";
         * args += pubIP.getPublicIp();
         * args += " -c ";
         * args += dev;
         * cfg.add(new ScriptConfigItem(VRScripts.VPC_SOURCE_NAT, args));
         */

        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetStaticRouteCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String[] rules = cmd.generateSRouteRules();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < rules.length; i++) {
            sb.append(rules[i]).append(',');
        }

        String args = " -a " + sb.toString();

        cfg.add(new ScriptConfigItem(VRScripts.VPC_STATIC_ROUTE, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(IpAssocCommand cmd) {
        new LinkedList<>();
        List<IpAddress> ips = new LinkedList<IpAddress>();

        for (IpAddressTO ip : cmd.getIpAddresses()) {
            IpAddress ipAddress = new IpAddress(ip.getPublicIp(), ip.isSourceNat(), ip.isAdd(), ip.isOneToOneNat(), ip.isFirstIP(), ip.getVlanGateway(), ip.getVlanNetmask(),
                    ip.getVifMacAddress(), ip.getNicDevId(), ip.isNewNic());
            ips.add(ipAddress);
        }

        IpAssociation ipAssociation = new IpAssociation(ips.toArray(new IpAddress[ips.size()]));

        return generateConfigItems(ipAssociation);
    }

    private static List<ConfigItem> generateConfigItems(ConfigBase configuration) {
        List<ConfigItem> cfg = new LinkedList<>();
        String destinationFile;

        switch (configuration.getType()) {
        case ConfigBase.FORWARDING_RULES:
            destinationFile = VRScripts.FORWARDING_RULES_CONFIG;
            break;
        case ConfigBase.GUEST_NETWORK:
            destinationFile = VRScripts.GUEST_NETWORK_CONFIG;
            break;
        case ConfigBase.IP_ASSOCIATION:
            destinationFile = VRScripts.IP_ASSOCIATION_CONFIG;
            break;
        case ConfigBase.NETWORK_ACL:
            destinationFile = VRScripts.NETWORK_ACL_CONFIG;
            break;
        case ConfigBase.STATICNAT_RULES:
            destinationFile = VRScripts.STATICNAT_RULES_CONFIG;
            break;
        case ConfigBase.VM_DHCP:
            destinationFile = VRScripts.VM_DHCP_CONFIG;
            break;
        case ConfigBase.VM_METADATA:
            destinationFile = VRScripts.VM_METADATA_CONFIG;
            break;
        case ConfigBase.VM_PASSWORD:
            destinationFile = VRScripts.VM_PASSWORD_CONFIG;
            break;
        case ConfigBase.VPN_USER_LIST:
            destinationFile = VRScripts.VPN_USER_LIST_CONFIG;
            break;
        default:
            throw new CloudRuntimeException("Unable to process the configuration for " + configuration.getType());
        }

        ConfigItem configFile = new FileConfigItem(VRScripts.CONFIG_PERSIST_LOCATION, destinationFile, gson.toJson(configuration));
        cfg.add(configFile);

        ConfigItem updateCommand = new ScriptConfigItem(VRScripts.UPDATE_CONFIG, destinationFile);
        cfg.add(updateCommand);

        return cfg;

    }
}
