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

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.google.gson.Gson;

import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.CreateIpAliasCommand;
import com.cloud.agent.api.routing.DeleteIpAliasCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
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
import com.cloud.network.HAProxyConfigurator;
import com.cloud.network.LoadBalancerConfigurator;
import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.net.NetUtils;

public class ConfigHelper {

    public static List<ConfigItem> generateCommandCfg(NetworkElementCommand cmd) {
        List<ConfigItem> cfg;
        if (cmd instanceof SetPortForwardingRulesVpcCommand) {
            cfg = generateConfig((SetPortForwardingRulesVpcCommand)cmd);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            cfg = generateConfig((SetPortForwardingRulesCommand)cmd);
        } else if (cmd instanceof SetStaticRouteCommand) {
            cfg = generateConfig((SetStaticRouteCommand)cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            cfg = generateConfig((SetStaticNatRulesCommand)cmd);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            cfg = generateConfig((LoadBalancerConfigCommand)cmd);
        } else if (cmd instanceof SavePasswordCommand) {
            cfg = generateConfig((SavePasswordCommand)cmd);
        } else if (cmd instanceof DhcpEntryCommand) {
            cfg = generateConfig((DhcpEntryCommand)cmd);
        } else if (cmd instanceof CreateIpAliasCommand) {
            cfg = generateConfig((CreateIpAliasCommand)cmd);
        } else if (cmd instanceof DnsMasqConfigCommand) {
            cfg = generateConfig((DnsMasqConfigCommand)cmd);
        } else if (cmd instanceof DeleteIpAliasCommand) {
            cfg = generateConfig((DeleteIpAliasCommand)cmd);
        } else if (cmd instanceof VmDataCommand) {
            cfg = generateConfig((VmDataCommand)cmd);
        } else if (cmd instanceof SetFirewallRulesCommand) {
            cfg = generateConfig((SetFirewallRulesCommand)cmd);
        } else if (cmd instanceof BumpUpPriorityCommand) {
            cfg = generateConfig((BumpUpPriorityCommand)cmd);
        } else if (cmd instanceof RemoteAccessVpnCfgCommand) {
            cfg = generateConfig((RemoteAccessVpnCfgCommand)cmd);
        } else if (cmd instanceof VpnUsersCfgCommand) {
            cfg = generateConfig((VpnUsersCfgCommand)cmd);
        } else if (cmd instanceof Site2SiteVpnCfgCommand) {
            cfg = generateConfig((Site2SiteVpnCfgCommand)cmd);
        } else if (cmd instanceof SetMonitorServiceCommand) {
            cfg = generateConfig((SetMonitorServiceCommand)cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            cfg = generateConfig((SetupGuestNetworkCommand)cmd);
        } else if (cmd instanceof SetNetworkACLCommand) {
            cfg = generateConfig((SetNetworkACLCommand)cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            cfg = generateConfig((SetSourceNatCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            cfg = generateConfig((IpAssocCommand)cmd);
        } else {
            return null;
        }
        return cfg;
    }

    private static List<ConfigItem> generateConfig(VpnUsersCfgCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        for (VpnUsersCfgCommand.UsernamePassword userpwd : cmd.getUserpwds()) {
            String args = "";
            if (!userpwd.isAdd()) {
                args += "-U ";
                args += userpwd.getUsername();
            } else {
                args += "-u ";
                args += userpwd.getUsernamePassword();
            }
            cfg.add(new ScriptConfigItem(VRScripts.VPN_L2TP, args));
        }
        return cfg;
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
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        for (PortForwardingRuleTO rule : cmd.getRules()) {
            StringBuilder args = new StringBuilder();
            args.append(rule.revoked() ? "-D" : "-A");
            args.append(" -P ").append(rule.getProtocol().toLowerCase());
            args.append(" -l ").append(rule.getSrcIp());
            args.append(" -p ").append(rule.getStringSrcPortRange());
            args.append(" -r ").append(rule.getDstIp());
            args.append(" -d ").append(rule.getStringDstPortRange());
            cfg.add(new ScriptConfigItem(VRScripts.FIREWALL_NAT, args.toString()));
        }

        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetStaticNatRulesCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        if (cmd.getVpcId() != null) {
            for (StaticNatRuleTO rule : cmd.getRules()) {
                String args = rule.revoked() ? " -D" : " -A";
                args += " -l " + rule.getSrcIp();
                args += " -r " + rule.getDstIp();

                cfg.add(new ScriptConfigItem(VRScripts.VPC_STATIC_NAT, args));
            }
        } else {
            for (StaticNatRuleTO rule : cmd.getRules()) {
                //1:1 NAT needs instanceip;publicip;domrip;op
                StringBuilder args = new StringBuilder();
                args.append(rule.revoked() ? " -D " : " -A ");
                args.append(" -l ").append(rule.getSrcIp());
                args.append(" -r ").append(rule.getDstIp());

                if (rule.getProtocol() != null) {
                    args.append(" -P ").append(rule.getProtocol().toLowerCase());
                }

                args.append(" -d ").append(rule.getStringSrcPortRange());
                args.append(" -G ");

                cfg.add(new ScriptConfigItem(VRScripts.FIREWALL_NAT, args.toString()));
            }
        }
        return cfg;
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
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        Map<String, List<String[]>> data = new HashMap<String, List<String[]>>();
        data.put(cmd.getVmIpAddress(), cmd.getVmData());

        String json = new Gson().toJson(data);
        String encoded;
        try {
            encoded = Base64.encodeBase64String(json.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unable retrieve UTF-8 encoded data from vmdata");
        }

        String args = "-d " + encoded;

        cfg.add(new ScriptConfigItem(VRScripts.VMDATA, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(SavePasswordCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        final String password = cmd.getPassword();
        final String vmIpAddress = cmd.getVmIpAddress();

        String args = "-v " + vmIpAddress;
        args += " -p " + password;

        cfg.add(new ScriptConfigItem(VRScripts.PASSWORD, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(DhcpEntryCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String args = " -m " + cmd.getVmMac();
        if (cmd.getVmIpAddress() != null) {
            args += " -4 " + cmd.getVmIpAddress();
        }
        args += " -h " + cmd.getVmName();

        if (cmd.getDefaultRouter() != null) {
            args += " -d " + cmd.getDefaultRouter();
        }

        if (cmd.getDefaultDns() != null) {
            args += " -n " + cmd.getDefaultDns();
        }

        if (cmd.getStaticRoutes() != null) {
            args += " -s " + cmd.getStaticRoutes();
        }

        if (cmd.getVmIp6Address() != null) {
            args += " -6 " + cmd.getVmIp6Address();
            args += " -u " + cmd.getDuid();
        }

        if (!cmd.isDefault()) {
            args += " -N";
        }
        cfg.add(new ScriptConfigItem(VRScripts.DHCP, args));

        return cfg;
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
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        NicTO nic = cmd.getNic();
        String routerGIP = cmd.getAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP);
        String gateway = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY);
        String cidr = Long.toString(NetUtils.getCidrSize(nic.getNetmask()));
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

        String dev = "eth" + nic.getDeviceId();
        String netmask = NetUtils.getSubNet(routerGIP, nic.getNetmask());
        String args = "";
        if (cmd.isAdd() == false) {
            //pass the argument to script to delete the network
            args += " -D";
        } else {
            // pass create option argument if the ip needs to be added to eth device
            args += " -C";
        }
        args += " -M " + nic.getMac();
        args += " -d " + dev;
        args += " -i " + routerGIP;
        args += " -g " + gateway;
        args += " -m " + cidr;
        args += " -n " + netmask;
        if (dns != null && !dns.isEmpty()) {
            args += " -s " + dns;
        }
        if (domainName != null && !domainName.isEmpty()) {
            args += " -e " + domainName;
        }

        cfg.add(new ScriptConfigItem(VRScripts.VPC_GUEST_NETWORK, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetNetworkACLCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String privateGw = cmd.getAccessDetail(NetworkElementCommand.VPC_PRIVATE_GATEWAY);

        String[][] rules = cmd.generateFwRules();
        String[] aclRules = rules[0];
        NicTO nic = cmd.getNic();
        String dev = "eth" + nic.getDeviceId();
        String netmask = Long.toString(NetUtils.getCidrSize(nic.getNetmask()));
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < aclRules.length; i++) {
            sb.append(aclRules[i]).append(',');
        }

        String rule = sb.toString();

        String args = " -d " + dev;
        args += " -M " + nic.getMac();
        if (privateGw != null) {
            args += " -a " + rule;

            cfg.add(new ScriptConfigItem(VRScripts.VPC_PRIVATEGW_ACL, args));
        } else {
            args += " -i " + nic.getIp();
            args += " -m " + netmask;
            args += " -a " + rule;
            cfg.add(new ScriptConfigItem(VRScripts.VPC_ACL, args));
        }

        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetSourceNatCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        IpAddressTO pubIP = cmd.getIpAddress();
        String dev = "eth" + pubIP.getNicDevId();
        String args = "-A";
        args += " -l ";
        args += pubIP.getPublicIp();
        args += " -c ";
        args += dev;

        cfg.add(new ScriptConfigItem(VRScripts.VPC_SOURCE_NAT, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetPortForwardingRulesVpcCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        for (PortForwardingRuleTO rule : cmd.getRules()) {
            String args = rule.revoked() ? "-D" : "-A";
            args += " -P " + rule.getProtocol().toLowerCase();
            args += " -l " + rule.getSrcIp();
            args += " -p " + rule.getStringSrcPortRange();
            args += " -r " + rule.getDstIp();
            args += " -d " + rule.getStringDstPortRange().replace(":", "-");

            cfg.add(new ScriptConfigItem(VRScripts.VPC_PORTFORWARDING, args));
        }

        return cfg;
    }

    private static List<ConfigItem> generateConfig(SetStaticRouteCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String[][] rules = cmd.generateSRouteRules();
        StringBuilder sb = new StringBuilder();
        String[] srRules = rules[0];

        for (int i = 0; i < srRules.length; i++) {
            sb.append(srRules[i]).append(',');
        }

        String args = " -a " + sb.toString();

        cfg.add(new ScriptConfigItem(VRScripts.VPC_STATIC_ROUTE, args));
        return cfg;
    }

    private static List<ConfigItem> generateConfig(IpAssocCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        ConfigItem c;

        //Gson gson = new Gson();
        //ConfigItem ipAssociationsFile = new FileConfigItem(VRScripts.CONFIG_PERSIST_LOCATION, VRScripts.IP_ASSOCIATION_CONFIG, gson.toJson(cmd.getIpAddresses()));
        //cfg.add(ipAssociationsFile);

        if (cmd instanceof IpAssocVpcCommand) {
            for (IpAddressTO ip : cmd.getIpAddresses()) {
                String args = "";
                String snatArgs = "";

                if (ip.isAdd()) {
                    args += " -A ";
                    snatArgs += " -A ";
                } else {
                    args += " -D ";
                    snatArgs += " -D ";
                }

                args += " -l ";
                args += ip.getPublicIp();
                String nicName = "eth" + ip.getNicDevId();
                args += " -c ";
                args += nicName;
                args += " -g ";
                args += ip.getVlanGateway();
                args += " -m ";
                args += Long.toString(NetUtils.getCidrSize(ip.getVlanNetmask()));
                args += " -n ";
                args += NetUtils.getSubNet(ip.getPublicIp(), ip.getVlanNetmask());

                c = new ScriptConfigItem(VRScripts.VPC_IPASSOC, args);
                c.setInfo(ip.getPublicIp() + " - vpc_ipassoc");
                cfg.add(c);

                if (ip.isSourceNat()) {
                    snatArgs += " -l " + ip.getPublicIp();
                    snatArgs += " -c " + nicName;

                    c = new ScriptConfigItem(VRScripts.VPC_PRIVATEGW, snatArgs);
                    c.setInfo(ip.getPublicIp() + " - vpc_privategateway");
                    cfg.add(c);
                }
            }
        } else {
            for (IpAddressTO ip : cmd.getIpAddresses()) {
                String args = "";
                if (ip.isAdd()) {
                    args += "-A";
                } else {
                    args += "-D";
                }
                String cidrSize = Long.toString(NetUtils.getCidrSize(ip.getVlanNetmask()));
                if (ip.isSourceNat()) {
                    args += " -s";
                }
                if (ip.isFirstIP()) {
                    args += " -f";
                }
                args += " -l ";
                args += ip.getPublicIp() + "/" + cidrSize;

                String publicNic = "eth" + ip.getNicDevId();
                args += " -c ";
                args += publicNic;

                args += " -g ";
                args += ip.getVlanGateway();

                if (ip.isNewNic()) {
                    args += " -n";
                }

                c = new ScriptConfigItem(VRScripts.IPASSOC, args);
                c.setInfo(ip.getPublicIp());
                cfg.add(c);
            }
        }
        return cfg;
    }

}
