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
package com.cloud.agent.resource.virtualnetwork;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.CheckRouterAnswer;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.CheckS2SVpnConnectionsAnswer;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.AggregationControlCommand;
import com.cloud.agent.api.routing.AggregationControlCommand.Action;
import com.cloud.agent.api.routing.CreateIpAliasCommand;
import com.cloud.agent.api.routing.DeleteIpAliasCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.GroupAnswer;
import com.cloud.agent.api.routing.GetRouterAlertsCommand;
import com.cloud.agent.api.GetRouterAlertsAnswer;
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
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.net.NetUtils;
import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * VirtualNetworkResource controls and configures virtual networking
 *
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *  }
 **/
public class VirtualRoutingResource {
    protected class VRScripts {
        protected static final String S2SVPN_CHECK = "checkbatchs2svpn.sh";
        protected static final String S2SVPN_IPSEC = "ipsectunnel.sh";
        protected static final String DHCP = "edithosts.sh";
        protected static final String DNSMASQ_CONFIG = "dnsmasq.sh";
        protected static final String FIREWALL_EGRESS = "firewall_egress.sh";
        protected static final String FIREWALL_INGRESS = "firewall_ingress.sh";
        protected static final String FIREWALL_NAT = "firewall_nat.sh";
        protected static final String IPALIAS_CREATE = "createipAlias.sh";
        protected static final String IPALIAS_DELETE = "deleteipAlias.sh";
        protected static final String IPASSOC = "ipassoc.sh";
        protected static final String LB = "loadbalancer.sh";
        protected static final String MONITOR_SERVICE = "monitor_service.sh";
        protected static final String ROUTER_ALERTS = "getRouterAlerts.sh";
        protected static final String PASSWORD = "savepassword.sh";
        protected static final String RVR_CHECK = "checkrouter.sh";
        protected static final String RVR_BUMPUP_PRI = "bumpup_priority.sh";
        protected static final String VMDATA = "vmdata.py";
        protected static final String VERSION = "get_template_version.sh";
        protected static final String VPC_ACL = "vpc_acl.sh";
        protected static final String VPC_GUEST_NETWORK = "vpc_guestnw.sh";
        protected static final String VPC_IPASSOC = "vpc_ipassoc.sh";
        protected static final String VPC_LB = "vpc_loadbalancer.sh";
        protected static final String VPC_PRIVATEGW = "vpc_privateGateway.sh";
        protected static final String VPC_PRIVATEGW_ACL = "vpc_privategw_acl.sh";
        protected static final String VPC_PORTFORWARDING = "vpc_portforwarding.sh";
        protected static final String VPC_SOURCE_NAT = "vpc_snat.sh";
        protected static final String VPC_STATIC_NAT = "vpc_staticnat.sh";
        protected static final String VPC_STATIC_ROUTE = "vpc_staticroute.sh";
        protected static final String VPN_L2TP = "vpn_l2tp.sh";

        protected static final String VR_CFG = "vr_cfg.sh";
    }

    private static final Logger s_logger = Logger.getLogger(VirtualRoutingResource.class);
    private VirtualRouterDeployer _vrDeployer;
    private Map <String, Queue> _vrAggregateCommandsSet;
    protected Map<String, Lock> _vrLockMap = new HashMap<String, Lock>();

    private String _name;
    private int _sleep;
    private int _retry;
    private int _port;
    private int _eachTimeout;

    private String _cfgVersion = "1.0";

    public VirtualRoutingResource(VirtualRouterDeployer deployer) {
        this._vrDeployer = deployer;
    }

    public Answer executeRequest(final NetworkElementCommand cmd) {
        boolean aggregated = false;
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        Lock lock;
        if (_vrLockMap.containsKey(routerName)) {
            lock = _vrLockMap.get(routerName);
        } else {
            lock = new ReentrantLock();
            _vrLockMap.put(routerName, lock);
        }
        lock.lock();

        try {
            ExecutionResult rc = _vrDeployer.prepareCommand(cmd);
            if (!rc.isSuccess()) {
                s_logger.error("Failed to prepare VR command due to " + rc.getDetails());
                return new Answer(cmd, false, rc.getDetails());
            }

            assert cmd.getRouterAccessIp() != null : "Why there is no access IP for VR?";

            if (cmd.isQuery()) {
                return executeQueryCommand(cmd);
            }

            if (cmd instanceof AggregationControlCommand) {
                return execute((AggregationControlCommand)cmd);
            }

            if (_vrAggregateCommandsSet.containsKey(routerName)) {
                _vrAggregateCommandsSet.get(routerName).add(cmd);
                aggregated = true;
                // Clean up would be done after command has been executed
                //TODO: Deal with group answer as well
                return new Answer(cmd);
            }

            List<ConfigItem> cfg = generateCommandCfg(cmd);
            if (cfg == null) {
                return Answer.createUnsupportedCommandAnswer(cmd);
            }

            return applyConfig(cmd, cfg);
        } catch (final IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        } finally {
            lock.unlock();
            if (!aggregated) {
                ExecutionResult rc = _vrDeployer.cleanupCommand(cmd);
                if (!rc.isSuccess()) {
                    s_logger.error("Failed to cleanup VR command due to " + rc.getDetails());
                }
            }
        }
    }

    protected class ConfigItem {
        private String script;
        private String args;
        private String info;
        private String filePath;
        private String fileName;
        private String fileContents;
        private boolean isFile;

        public ConfigItem(String script, String args) {
            this.script = script;
            this.args = args;
            this.isFile = false;
        }

        public ConfigItem(String filePath, String fileName, String fileContents) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.fileContents = fileContents;
            this.isFile = true;
        }

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }

        public String getArgs() {
            return args;
        }

        public void setArgs(String args) {
            this.args = args;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileContents() {
            return fileContents;
        }

        public void setFileContents(String fileContents) {
            this.fileContents = fileContents;
        }

        public boolean isFile() {
            return isFile;
        }
    }

    private Answer executeQueryCommand(NetworkElementCommand cmd) {
        if (cmd instanceof CheckRouterCommand) {
            return execute((CheckRouterCommand)cmd);
        } else if (cmd instanceof GetDomRVersionCmd) {
            return execute((GetDomRVersionCmd)cmd);
        } else if (cmd instanceof CheckS2SVpnConnectionsCommand) {
            return execute((CheckS2SVpnConnectionsCommand) cmd);
        } else if (cmd instanceof GetRouterAlertsCommand) {
            return execute((GetRouterAlertsCommand)cmd);
        } else {
            s_logger.error("Unknown query command in VirtualRoutingResource!");
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private ExecutionResult applyConfigToVR(NetworkElementCommand cmd, ConfigItem c) {
        if (c.isFile()) {
            return _vrDeployer.createFileInVR(cmd.getRouterAccessIp(), c.getFilePath(), c.getFileName(), c.getFileContents());
        } else {
            return _vrDeployer.executeInVR(cmd.getRouterAccessIp(), c.getScript(), c.getArgs());
        }
    }

    private Answer applyConfig(NetworkElementCommand cmd, List<ConfigItem> cfg) {
        int answersCount = cmd.getAnswersCount();

        // Use the last answer as final answer
        if (answersCount == 1) {
            ExecutionResult result = new ExecutionResult(true, "No configure to be applied");
            for (ConfigItem c : cfg) {
                result = applyConfigToVR(cmd, c);
                if (!result.isSuccess()) {
                    break;
                }
            }
            return new Answer(cmd, result.isSuccess(), result.getDetails());
        }

        ExecutionResult[] results = new ExecutionResult[answersCount];
        String[] resultsString = new String[answersCount];
        boolean finalResult = true;
        int i = 0, j;
        for (ConfigItem c : cfg) {
            results[i] = applyConfigToVR(cmd, c);
            if (c.getInfo() != null) {
                if (results[i].isSuccess()) {
                    results[i].setDetails(c.getInfo() + " - success: " + results[i].getDetails());
                } else {
                    results[i].setDetails(c.getInfo() + " - failed: " + results[i].getDetails());
                }
            }
            i ++;
        }
        i = 0; j = 0;
        while (j < answersCount) {
            resultsString[j] = results[i].getDetails();
            if (!results[i].isSuccess()) {
                finalResult = false;
            }
            // Fill the resultsString with the last result of execution, mostly in 1:n
            if (i < cfg.size() - 1) {
                i ++;
            }
            j ++;
        }
        return new GroupAnswer(cmd, finalResult, answersCount, resultsString);
    }

    private List<ConfigItem> generateConfig(VpnUsersCfgCommand cmd) {
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
            cfg.add(new ConfigItem(VRScripts.VPN_L2TP, args));
        }
        return cfg;
    }

    private List<ConfigItem> generateConfig(RemoteAccessVpnCfgCommand cmd) {
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
        cfg.add(new ConfigItem(VRScripts.VPN_L2TP, args));
        return cfg;
    }

    private List<ConfigItem> generateConfig(SetFirewallRulesCommand cmd) {
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
            cfg.add(new ConfigItem(VRScripts.FIREWALL_EGRESS, args));
        } else {
            cfg.add(new ConfigItem(VRScripts.FIREWALL_INGRESS, args));
        }

        return cfg;
    }

    private List<ConfigItem> generateConfig(SetPortForwardingRulesCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        for (PortForwardingRuleTO rule : cmd.getRules()) {
            StringBuilder args = new StringBuilder();
            args.append(rule.revoked() ? "-D" : "-A");
            args.append(" -P ").append(rule.getProtocol().toLowerCase());
            args.append(" -l ").append(rule.getSrcIp());
            args.append(" -p ").append(rule.getStringSrcPortRange());
            args.append(" -r ").append(rule.getDstIp());
            args.append(" -d ").append(rule.getStringDstPortRange());
            cfg.add(new ConfigItem(VRScripts.FIREWALL_NAT, args.toString()));
        }

        return cfg;
    }

    private List<ConfigItem> generateConfig(SetStaticNatRulesCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        if (cmd.getVpcId() != null) {
            for (StaticNatRuleTO rule : cmd.getRules()) {
                String args = rule.revoked() ? " -D" : " -A";
                args += " -l " + rule.getSrcIp();
                args += " -r " + rule.getDstIp();

                cfg.add(new ConfigItem(VRScripts.VPC_STATIC_NAT, args));
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

                cfg.add(new ConfigItem(VRScripts.FIREWALL_NAT, args.toString()));
            }
        }
        return cfg;
    }

    private List<ConfigItem> generateConfig(LoadBalancerConfigCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        LoadBalancerConfigurator cfgtr = new HAProxyConfigurator();

        String[] config = cfgtr.generateConfiguration(cmd);
        String tmpCfgFileContents = "";
        for (int i = 0; i < config.length; i++) {
            tmpCfgFileContents += config[i];
            tmpCfgFileContents += "\n";
        }

        String tmpCfgFilePath = "/etc/haproxy/";
        String tmpCfgFileName = "haproxy.cfg.new." + String.valueOf(System.currentTimeMillis());
        cfg.add(new ConfigItem(tmpCfgFilePath, tmpCfgFileName, tmpCfgFileContents));

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
            cfg.add(new ConfigItem(VRScripts.LB, args));
        } else {
            args = " -i " + cmd.getNic().getIp() + args;
            cfg.add(new ConfigItem(VRScripts.VPC_LB, args));
        }

        return cfg;
    }

    private List<ConfigItem> generateConfig(VmDataCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        Map<String, List<String[]>> data = new HashMap<String, List<String[]>>();
        data.put(cmd.getVmIpAddress(), cmd.getVmData());

        String json = new Gson().toJson(data);
        s_logger.debug("JSON IS:" + json);

        json = Base64.encodeBase64String(json.getBytes());

        String args = "-d " + json;

        cfg.add(new ConfigItem(VRScripts.VMDATA, args));
        return cfg;
    }

    private List<ConfigItem> generateConfig(SavePasswordCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        final String password = cmd.getPassword();
        final String vmIpAddress = cmd.getVmIpAddress();

        String args = "-v " + vmIpAddress;
        args += " -p " + password;

        cfg.add(new ConfigItem(VRScripts.PASSWORD, args));
        return cfg;
    }

    private List<ConfigItem> generateConfig(DhcpEntryCommand cmd) {
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
        cfg.add(new ConfigItem(VRScripts.DHCP, args));

        return cfg;
    }

    private List<ConfigItem> generateConfig(CreateIpAliasCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        List<IpAliasTO> ipAliasTOs = cmd.getIpAliasList();
        String args = "";
        for (IpAliasTO ipaliasto : ipAliasTOs) {
            args = args + ipaliasto.getAlias_count() + ":" + ipaliasto.getRouterip() + ":" + ipaliasto.getNetmask() + "-";
        }

        cfg.add(new ConfigItem(VRScripts.IPALIAS_CREATE, args));
        return cfg;
    }

    private List<ConfigItem> generateConfig(DeleteIpAliasCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String args = "";
        List<IpAliasTO> revokedIpAliasTOs = cmd.getDeleteIpAliasTos();
        for (IpAliasTO ipAliasTO : revokedIpAliasTOs) {
            args = args + ipAliasTO.getAlias_count() + ":" + ipAliasTO.getRouterip() + ":" + ipAliasTO.getNetmask() + "-";
        }
        //this is to ensure that thre is some argument passed to the deleteipAlias script  when there are no revoked rules.
        args = args + "- ";
        List<IpAliasTO> activeIpAliasTOs = cmd.getCreateIpAliasTos();
        for (IpAliasTO ipAliasTO : activeIpAliasTOs) {
            args = args + ipAliasTO.getAlias_count() + ":" + ipAliasTO.getRouterip() + ":" + ipAliasTO.getNetmask() + "-";
        }

        cfg.add(new ConfigItem(VRScripts.IPALIAS_DELETE, args));
        return cfg;
    }

    private List<ConfigItem> generateConfig(DnsMasqConfigCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        List<DhcpTO> dhcpTos = cmd.getIps();
        String args = "";
        for (DhcpTO dhcpTo : dhcpTos) {
            args = args + dhcpTo.getRouterIp() + ":" + dhcpTo.getGateway() + ":" + dhcpTo.getNetmask() + ":" + dhcpTo.getStartIpOfSubnet() + "-";
        }

        cfg.add(new ConfigItem(VRScripts.DNSMASQ_CONFIG, args));
        return cfg;
    }

    private CheckS2SVpnConnectionsAnswer execute(CheckS2SVpnConnectionsCommand cmd) {
        String args = "";
        for (String ip : cmd.getVpnIps()) {
            args += ip + " ";
        }

        ExecutionResult result = _vrDeployer.executeInVR(cmd.getRouterAccessIp(), VRScripts.S2SVPN_CHECK, args);
        return new CheckS2SVpnConnectionsAnswer(cmd, result.isSuccess(), result.getDetails());
    }

    private GetRouterAlertsAnswer execute(GetRouterAlertsCommand cmd) {

        String routerIp = cmd.getAccessDetail(NetworkElementCommand.ROUTER_IP);
        String args = cmd.getPreviousAlertTimeStamp();

        ExecutionResult result = _vrDeployer.executeInVR(routerIp, VRScripts.ROUTER_ALERTS, args);
        String alerts[] = null;
        String lastAlertTimestamp = null;

        if (result.isSuccess()) {
            if (!result.getDetails().isEmpty() && !result.getDetails().trim().equals("No Alerts")) {
                alerts = result.getDetails().trim().split("\\\\n");
                String[] lastAlert = alerts[alerts.length - 1].split(",");
                lastAlertTimestamp = lastAlert[0];
            }
            return new GetRouterAlertsAnswer(cmd, alerts, lastAlertTimestamp);
        } else {
            return new GetRouterAlertsAnswer(cmd, result.getDetails());
        }
    }

    protected Answer execute(CheckRouterCommand cmd) {
        final ExecutionResult result = _vrDeployer.executeInVR(cmd.getRouterAccessIp(), VRScripts.RVR_CHECK, null);
        if (!result.isSuccess()) {
            return new CheckRouterAnswer(cmd, result.getDetails());
        }
        return new CheckRouterAnswer(cmd, result.getDetails(), true);
    }

    private List<ConfigItem> generateConfig(BumpUpPriorityCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        cfg.add(new ConfigItem(VRScripts.RVR_BUMPUP_PRI, null));
        return cfg;
    }

    protected Answer execute(GetDomRVersionCmd cmd) {
        final ExecutionResult result = _vrDeployer.executeInVR(cmd.getRouterAccessIp(), VRScripts.VERSION, null);
        if (!result.isSuccess()) {
            return new GetDomRVersionAnswer(cmd, "GetDomRVersionCmd failed");
        }
        String[] lines = result.getDetails().split("&");
        if (lines.length != 2) {
            return new GetDomRVersionAnswer(cmd, result.getDetails());
        }
        return new GetDomRVersionAnswer(cmd, result.getDetails(), lines[0], lines[1]);
    }

    private List<ConfigItem> generateConfig(Site2SiteVpnCfgCommand cmd) {
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

        cfg.add(new ConfigItem(VRScripts.S2SVPN_IPSEC, args));
        return cfg;
    }

    protected List<ConfigItem> generateConfig(SetMonitorServiceCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String config = cmd.getConfiguration();
        String disableMonitoring =  cmd.getAccessDetail(NetworkElementCommand.ROUTER_MONITORING_ENABLE);

        String args = " -c " + config;
        if (disableMonitoring != null) {
            args = args + " -d";
        }

        cfg.add(new ConfigItem(VRScripts.MONITOR_SERVICE, args));
        return cfg;
    }

    protected List<ConfigItem> generateConfig(SetupGuestNetworkCommand cmd) {
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
        if(cmd.isAdd() == false) {
            //pass the argument to script to delete the network
            args +=" -D ";
        } else {
            // pass create option argument if the ip needs to be added to eth device
            args +=" -C ";
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

        cfg.add(new ConfigItem(VRScripts.VPC_GUEST_NETWORK, args));
        return cfg;
    }

    protected List<ConfigItem> generateConfig(SetNetworkACLCommand cmd) {
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

            cfg.add(new ConfigItem(VRScripts.VPC_PRIVATEGW_ACL, args));
        } else {
            args += " -i " + nic.getIp();
            args += " -m " + netmask;
            args += " -a " + rule;
            cfg.add(new ConfigItem(VRScripts.VPC_ACL, args));
        }

        return cfg;
    }

    protected List<ConfigItem> generateConfig(SetSourceNatCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        IpAddressTO pubIP = cmd.getIpAddress();
        String dev = "eth" + pubIP.getNicDevId();
        String args = "-A";
        args += " -l ";
        args += pubIP.getPublicIp();
        args += " -c ";
        args += dev;

        cfg.add(new ConfigItem(VRScripts.VPC_SOURCE_NAT, args));
        return cfg;
    }

    protected List<ConfigItem> generateConfig(SetPortForwardingRulesVpcCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        for (PortForwardingRuleTO rule : cmd.getRules()) {
            String args = rule.revoked() ? "-D" : "-A";
            args += " -P " + rule.getProtocol().toLowerCase();
            args += " -l " + rule.getSrcIp();
            args += " -p " + rule.getStringSrcPortRange();
            args += " -r " + rule.getDstIp();
            args += " -d " + rule.getStringDstPortRange().replace(":", "-");

            cfg.add(new ConfigItem(VRScripts.VPC_PORTFORWARDING, args));
        }

        return cfg;
    }

    protected List<ConfigItem> generateConfig(SetStaticRouteCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();

        String[][] rules = cmd.generateSRouteRules();
        StringBuilder sb = new StringBuilder();
        String[] srRules = rules[0];

        for (int i = 0; i < srRules.length; i++) {
            sb.append(srRules[i]).append(',');
        }

        String args = " -a " + sb.toString();

        cfg.add(new ConfigItem(VRScripts.VPC_STATIC_ROUTE, args));
        return cfg;
    }

    protected List<ConfigItem> generateConfig(IpAssocCommand cmd) {
        LinkedList<ConfigItem> cfg = new LinkedList<>();
        ConfigItem c;

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

                c = new ConfigItem(VRScripts.VPC_IPASSOC, args);
                c.setInfo(ip.getPublicIp() + " - vpc_ipassoc");
                cfg.add(c);

                if (ip.isSourceNat()) {
                    snatArgs += " -l " + ip.getPublicIp();
                    snatArgs += " -c " + nicName;

                    c = new ConfigItem(VRScripts.VPC_PRIVATEGW, snatArgs);
                    c.setInfo(ip.getPublicIp() + " - vpc_privategateway");
                    cfg.add(c);
                }
            }
        } else {
            for (IpAddressTO ip: cmd.getIpAddresses()) {
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

                c = new ConfigItem(VRScripts.IPASSOC, args);
                c.setInfo(ip.getPublicIp());
                cfg.add(c);
            }
        }
        return cfg;
    }

    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        String value = (String)params.get("ssh.sleep");
        _sleep = NumbersUtil.parseInt(value, 10) * 1000;

        value = (String)params.get("ssh.retry");
        _retry = NumbersUtil.parseInt(value, 36);

        value = (String)params.get("ssh.port");
        _port = NumbersUtil.parseInt(value, 3922);

        value = (String)params.get("router.aggregation.command.each.timeout");
        _eachTimeout = NumbersUtil.parseInt(value, 3);

        if (_vrDeployer == null) {
            throw new ConfigurationException("Unable to find the resource for VirtualRouterDeployer!");
        }

        _vrAggregateCommandsSet = new HashMap<>();
        return true;
    }

    public String connect(final String ipAddress) {
        return connect(ipAddress, _port);
    }

    public String connect(final String ipAddress, final int port) {
        for (int i = 0; i <= _retry; i++) {
            SocketChannel sch = null;
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Trying to connect to " + ipAddress);
                }
                sch = SocketChannel.open();
                sch.configureBlocking(true);

                final InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
                sch.connect(addr);
                return null;
            } catch (final IOException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Could not connect to " + ipAddress);
                }
            } finally {
                if (sch != null) {
                    try {
                        sch.close();
                    } catch (final IOException e) {
                    }
                }
            }
            try {
                Thread.sleep(_sleep);
            } catch (final InterruptedException e) {
            }
        }

        s_logger.debug("Unable to logon to " + ipAddress);

        return "Unable to connect";
    }

    public boolean connect(final String ipAddress, int retry, int sleep) {
        for (int i = 0; i <= retry; i++) {
            SocketChannel sch = null;
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Trying to connect to " + ipAddress);
                }
                sch = SocketChannel.open();
                sch.configureBlocking(true);

                final InetSocketAddress addr = new InetSocketAddress(ipAddress, _port);
                sch.connect(addr);
                return true;
            } catch (final IOException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Could not connect to " + ipAddress);
                }
            } finally {
                if (sch != null) {
                    try {
                        sch.close();
                    } catch (final IOException e) {
                    }
                }
            }
            try {
                Thread.sleep(sleep);
            } catch (final InterruptedException e) {
            }
        }

        s_logger.debug("Unable to logon to " + ipAddress);

        return false;
    }

    private List<ConfigItem> generateCommandCfg(NetworkElementCommand cmd) {
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

    private Answer execute(AggregationControlCommand cmd) {
        Action action = cmd.getAction();
        String routerName = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        assert routerName != null;
        assert cmd.getRouterAccessIp() != null;

        if (action == Action.Start) {
            assert (!_vrAggregateCommandsSet.containsKey(routerName));

            Queue<NetworkElementCommand> queue = new LinkedBlockingQueue<>();
            _vrAggregateCommandsSet.put(routerName, queue);
            return new Answer(cmd);
        } else if (action == Action.Finish) {
            Queue<NetworkElementCommand> queue = _vrAggregateCommandsSet.get(routerName);
            int answerCounts = 0;
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("#Apache CloudStack Virtual Router Config File\n");
                sb.append("<version>\n" + _cfgVersion + "\n</version>\n");
                for (NetworkElementCommand command : queue) {
                    answerCounts += command.getAnswersCount();
                    List<ConfigItem> cfg = generateCommandCfg(command);
                    if (cfg == null) {
                        s_logger.warn("Unknown commands for VirtualRoutingResource, but continue: " + cmd.toString());
                        continue;
                    }

                    for (ConfigItem c : cfg) {
                        if (c.isFile()) {
                            sb.append("<file>\n");
                            sb.append(c.getFilePath() + c.getFileName() + "\n");
                            sb.append(c.getFileContents() + "\n");
                            sb.append("</file>\n");
                        } else {
                            sb.append("<script>\n");
                            sb.append("/opt/cloud/bin/" + c.getScript() + " " + c.getArgs() + "\n");
                            sb.append("</script>\n");
                        }
                    }
                }
                String cfgFilePath = "/var/cache/cloud/";
                String cfgFileName = "VR-"+ UUID.randomUUID().toString() + ".cfg";
                ExecutionResult result = _vrDeployer.createFileInVR(cmd.getRouterAccessIp(), cfgFilePath, cfgFileName, sb.toString());
                if (!result.isSuccess()) {
                    return new Answer(cmd, false, result.getDetails());
                }

                // 120s is the minimal timeout
                int timeout = answerCounts * _eachTimeout;
                if (timeout < 120) {
                    timeout = 120;
                }
                result = _vrDeployer.executeInVR(cmd.getRouterAccessIp(), VRScripts.VR_CFG, "-c " + cfgFilePath + cfgFileName, timeout);
                if (!result.isSuccess()) {
                    return new Answer(cmd, false, result.getDetails());
                }
                return new Answer(cmd);
            } finally {
                queue.clear();
                _vrAggregateCommandsSet.remove(routerName);
            }
        } else if (action == Action.Cleanup) {
            assert (_vrAggregateCommandsSet.containsKey(routerName));
            Queue<NetworkElementCommand> queue = _vrAggregateCommandsSet.get(routerName);
            if (queue != null) {
                queue.clear();
            }
            _vrAggregateCommandsSet.remove(routerName);

            return new Answer(cmd);
        }
        return new Answer(cmd, false, "Fail to recongize aggregation action " + action.toString());
    }
}
