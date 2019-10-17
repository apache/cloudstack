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
package com.cloud.network.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AssociateAsaWithLogicalEdgeFirewallCommand;
import com.cloud.agent.api.CleanupLogicalEdgeFirewallCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ConfigureNexusVsmForAsaCommand;
import com.cloud.agent.api.CreateLogicalEdgeFirewallCommand;
import com.cloud.agent.api.ExternalNetworkResourceUsageAnswer;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalFirewallCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.network.cisco.CiscoVnmcConnectionImpl;
import com.cloud.network.rules.FirewallRule.TrafficType;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.cisco.n1kv.vsm.NetconfHelper;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.OperationType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;

public class CiscoVnmcResource implements ServerResource {

    private String _name;
    private String _zoneId;
    private String _physicalNetworkId;
    private String _ip;
    private String _username;
    private String _password;
    private String _guid;
    private Integer _numRetries = 1;

    private CiscoVnmcConnectionImpl _connection;

    public void setConnection(CiscoVnmcConnectionImpl connection) {
        _connection = connection;
    }

    private static final Logger s_logger = Logger.getLogger(CiscoVnmcResource.class);

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand)cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return execute((SetSourceNatCommand)cmd);
        } else if (cmd instanceof SetFirewallRulesCommand) {
            return execute((SetFirewallRulesCommand)cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            return execute((SetStaticNatRulesCommand)cmd);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            return execute((SetPortForwardingRulesCommand)cmd);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand)cmd);
        } else if (cmd instanceof CreateLogicalEdgeFirewallCommand) {
            return execute((CreateLogicalEdgeFirewallCommand)cmd);
        } else if (cmd instanceof CleanupLogicalEdgeFirewallCommand) {
            return execute((CleanupLogicalEdgeFirewallCommand)cmd);
        } else if (cmd instanceof ConfigureNexusVsmForAsaCommand) {
            return execute((ConfigureNexusVsmForAsaCommand)cmd);
        } else if (cmd instanceof AssociateAsaWithLogicalEdgeFirewallCommand) {
            return execute((AssociateAsaWithLogicalEdgeFirewallCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            _name = (String)params.get("name");
            if (_name == null) {
                throw new ConfigurationException("Unable to find name");
            }

            _zoneId = (String)params.get("zoneId");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone");
            }

            _physicalNetworkId = (String)params.get("physicalNetworkId");
            if (_physicalNetworkId == null) {
                throw new ConfigurationException("Unable to find physical network id in the configuration parameters");
            }

            _ip = (String)params.get("ip");
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP");
            }

            _username = (String)params.get("username");
            if (_username == null) {
                throw new ConfigurationException("Unable to find username");
            }

            _password = (String)params.get("password");
            if (_password == null) {
                throw new ConfigurationException("Unable to find password");
            }

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            _numRetries = NumbersUtil.parseInt((String)params.get("numretries"), 1);

            NumbersUtil.parseInt((String)params.get("timeout"), 300);

            // Open a socket and login
            _connection = new CiscoVnmcConnectionImpl(_ip, _username, _password);
            if (!refreshVnmcConnection()) {
                throw new ConfigurationException("Unable to connect to VNMC, check if ip/username/password is valid.");
            }

            return true;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }

    }

    @Override
    public StartupCommand[] initialize() {
        StartupExternalFirewallCommand cmd = new StartupExternalFirewallCommand();
        cmd.setName(_name);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion(CiscoVnmcResource.class.getPackage().getImplementationVersion());
        cmd.setGuid(_guid);
        return new StartupCommand[] {cmd};
    }

    @Override
    public Host.Type getType() {
        return Host.Type.ExternalFirewall;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        if (!refreshVnmcConnection()) {
            return null;
        }
        return new PingCommand(Host.Type.ExternalFirewall, id);
    }

    @Override
    public void disconnected() {
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        return;
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private ExternalNetworkResourceUsageAnswer execute(ExternalNetworkResourceUsageCommand cmd) {
        return new ExternalNetworkResourceUsageAnswer(cmd);
    }

    /*
     * Login
     */
    private boolean refreshVnmcConnection() {
        boolean ret = false;
        try {
            ret = _connection.login();
        } catch (ExecutionException ex) {
            s_logger.error("Login to Vnmc failed", ex);
        }
        return ret;
    }

    private synchronized Answer execute(IpAssocCommand cmd) {
        refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(IpAssocCommand cmd, int numRetries) {
        String[] results = new String[cmd.getIpAddresses().length];
        return new IpAssocAnswer(cmd, results);
    }

    private String[] getIpRangeFromCidr(String cidr) {
        String[] result = new String[2];
        String[] cidrData = cidr.split("\\/");
        assert (cidrData.length == 2) : "Something is wrong with source cidr " + cidr;
        long size = Long.parseLong(cidrData[1]);
        result[0] = cidrData[0];
        result[1] = cidrData[0];
        if (size < 32) {
            result[0] = NetUtils.getIpRangeStartIpFromCidr(cidrData[0], size);
            result[1] = NetUtils.getIpRangeEndIpFromCidr(cidrData[0], size);
        }
        return result;
    }

    /*
     * Source NAT
     */
    private synchronized Answer execute(SetSourceNatCommand cmd) {
        refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetSourceNatCommand cmd, int numRetries) {
        String vlanId = cmd.getContextParam(NetworkElementCommand.GUEST_VLAN_TAG);
        String tenant = "vlan-" + vlanId;
        String policyIdentifier = cmd.getIpAddress().getPublicIp().replace('.', '-');
        try {
            if (!_connection.createTenantVDCNatPolicySet(tenant)) {
                throw new ExecutionException("Failed to create NAT policy set in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCSourceNatPolicy(tenant, policyIdentifier)) {
                throw new ExecutionException("Failed to create source NAT policy in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCSourceNatPolicyRef(tenant, policyIdentifier)) {
                throw new ExecutionException("Failed to associate source NAT policy with NAT policy set in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCSourceNatIpPool(tenant, policyIdentifier, cmd.getIpAddress().getPublicIp())) {
                throw new ExecutionException("Failed to create source NAT ip pool in VNMC for guest network with vlan " + vlanId);
            }

            String[] ipRange = getIpRangeFromCidr(cmd.getContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR));
            if (!_connection.createTenantVDCSourceNatRule(tenant, policyIdentifier, ipRange[0], ipRange[1])) {
                throw new ExecutionException("Failed to create source NAT rule in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.associateNatPolicySet(tenant)) {
                throw new ExecutionException("Failed to associate source NAT policy set with edge security profile in VNMC for guest network with vlan " + vlanId);
            }
        } catch (ExecutionException e) {
            String msg = "SetSourceNatCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    /*
     * Firewall rule
     */
    private synchronized Answer execute(SetFirewallRulesCommand cmd) {
        refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetFirewallRulesCommand cmd, int numRetries) {
        String vlanId = cmd.getContextParam(NetworkElementCommand.GUEST_VLAN_TAG);
        String tenant = "vlan-" + vlanId;

        FirewallRuleTO[] rules = cmd.getRules();
        Map<String, List<FirewallRuleTO>> publicIpRulesMap = new HashMap<String, List<FirewallRuleTO>>();
        for (FirewallRuleTO rule : rules) {
            String publicIp = rule.getSrcIp();
            if (!publicIpRulesMap.containsKey(publicIp)) {
                List<FirewallRuleTO> publicIpRulesList = new ArrayList<FirewallRuleTO>();
                publicIpRulesMap.put(publicIp, publicIpRulesList);
            }
            publicIpRulesMap.get(publicIp).add(rule);
        }

        try {
            if (!_connection.createTenantVDCAclPolicySet(tenant, true)) {
                throw new ExecutionException("Failed to create ACL ingress policy set in VNMC for guest network with vlan " + vlanId);
            }
            if (!_connection.createTenantVDCAclPolicySet(tenant, false)) {
                throw new ExecutionException("Failed to create ACL egress policy set in VNMC for guest network with vlan " + vlanId);
            }

            for (String publicIp : publicIpRulesMap.keySet()) {
                String policyIdentifier = publicIp.replace('.', '-');

                if (!_connection.createTenantVDCAclPolicy(tenant, policyIdentifier)) {
                    throw new ExecutionException("Failed to create ACL policy in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCAclPolicyRef(tenant, policyIdentifier, true)) {
                    throw new ExecutionException("Failed to associate ACL policy with ACL ingress policy set in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCAclPolicyRef(tenant, policyIdentifier, false)) {
                    throw new ExecutionException("Failed to associate ACL policy with ACL egress policy set in VNMC for guest network with vlan " + vlanId);
                }

                for (FirewallRuleTO rule : publicIpRulesMap.get(publicIp)) {
                    if (rule.revoked()) {
                        if (!_connection.deleteTenantVDCAclRule(tenant, rule.getId(), policyIdentifier)) {
                            throw new ExecutionException("Failed to delete ACL rule in VNMC for guest network with vlan " + vlanId);
                        }
                    } else {
                        String[] externalIpRange = getIpRangeFromCidr(rule.getSourceCidrList().get(0));
                        if (rule.getTrafficType() == TrafficType.Ingress) {
                            if (!rule.getProtocol().equalsIgnoreCase("icmp") && rule.getSrcPortRange() != null) {
                                if (!_connection.createTenantVDCIngressAclRule(tenant, rule.getId(), policyIdentifier, rule.getProtocol().toUpperCase(),
                                    externalIpRange[0], externalIpRange[1], Integer.toString(rule.getSrcPortRange()[0]), Integer.toString(rule.getSrcPortRange()[1]))) {
                                    throw new ExecutionException("Failed to create ACL ingress rule in VNMC for guest network with vlan " + vlanId);
                                }
                            } else {
                                if (!_connection.createTenantVDCIngressAclRule(tenant, rule.getId(), policyIdentifier, rule.getProtocol().toUpperCase(),
                                    externalIpRange[0], externalIpRange[1])) {
                                    throw new ExecutionException("Failed to create ACL ingress rule in VNMC for guest network with vlan " + vlanId);
                                }
                            }
                        } else {
                            if ((rule.getProtocol().equalsIgnoreCase("tcp") || rule.getProtocol().equalsIgnoreCase("udp")) && rule.getSrcPortRange() != null) {
                                if (!_connection.createTenantVDCEgressAclRule(tenant, rule.getId(), policyIdentifier, rule.getProtocol().toUpperCase(),
                                    externalIpRange[0], externalIpRange[1], Integer.toString(rule.getSrcPortRange()[0]), Integer.toString(rule.getSrcPortRange()[1]))) {
                                    throw new ExecutionException("Failed to create ACL egress rule in VNMC for guest network with vlan " + vlanId);
                                }
                            } else {
                                if (!_connection.createTenantVDCEgressAclRule(tenant, rule.getId(), policyIdentifier, rule.getProtocol().toUpperCase(),
                                    externalIpRange[0], externalIpRange[1])) {
                                    throw new ExecutionException("Failed to create ACL egress rule in VNMC for guest network with vlan " + vlanId);
                                }
                            }
                        }
                    }
                }
            }

            if (!_connection.associateAclPolicySet(tenant)) {
                throw new ExecutionException("Failed to associate ACL policy set with edge security profile in VNMC for guest network with vlan " + vlanId);
            }
        } catch (ExecutionException e) {
            String msg = "SetFirewallRulesCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    /*
     * Static NAT
     */
    private synchronized Answer execute(SetStaticNatRulesCommand cmd) {
        refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetStaticNatRulesCommand cmd, int numRetries) {
        String vlanId = cmd.getContextParam(NetworkElementCommand.GUEST_VLAN_TAG);
        String tenant = "vlan-" + vlanId;

        StaticNatRuleTO[] rules = cmd.getRules();
        Map<String, List<StaticNatRuleTO>> publicIpRulesMap = new HashMap<String, List<StaticNatRuleTO>>();
        for (StaticNatRuleTO rule : rules) {
            String publicIp = rule.getSrcIp();
            if (!publicIpRulesMap.containsKey(publicIp)) {
                List<StaticNatRuleTO> publicIpRulesList = new ArrayList<StaticNatRuleTO>();
                publicIpRulesMap.put(publicIp, publicIpRulesList);
            }
            publicIpRulesMap.get(publicIp).add(rule);
        }

        try {
            if (!_connection.createTenantVDCNatPolicySet(tenant)) {
                throw new ExecutionException("Failed to create NAT policy set in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCAclPolicySet(tenant, true)) {
                throw new ExecutionException("Failed to create ACL ingress policy set in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCAclPolicySet(tenant, false)) {
                throw new ExecutionException("Failed to create ACL egress policy set in VNMC for guest network with vlan " + vlanId);
            }

            for (String publicIp : publicIpRulesMap.keySet()) {
                String policyIdentifier = publicIp.replace('.', '-');

                if (!_connection.createTenantVDCDNatPolicy(tenant, policyIdentifier)) {
                    throw new ExecutionException("Failed to create DNAT policy in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCDNatPolicyRef(tenant, policyIdentifier)) {
                    throw new ExecutionException("Failed to associate DNAT policy with NAT policy set in VNMC for guest network with vlan " + vlanId);
                }

                if (!_connection.createTenantVDCAclPolicy(tenant, policyIdentifier)) {
                    throw new ExecutionException("Failed to create ACL policy in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCAclPolicyRef(tenant, policyIdentifier, true)) {
                    throw new ExecutionException("Failed to associate ACL policy with ACL ingress policy set in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCAclPolicyRef(tenant, policyIdentifier, false)) {
                    throw new ExecutionException("Failed to associate ACL policy with ACL egress policy set in VNMC for guest network with vlan " + vlanId);
                }

                for (StaticNatRuleTO rule : publicIpRulesMap.get(publicIp)) {
                    if (rule.revoked()) {
                        if (!_connection.deleteTenantVDCDNatRule(tenant, rule.getId(), policyIdentifier)) {
                            throw new ExecutionException("Failed to delete DNAT rule in VNMC for guest network with vlan " + vlanId);
                        }
                    } else {
                        if (!_connection.createTenantVDCDNatIpPool(tenant, Long.toString(rule.getId()), rule.getDstIp())) {
                            throw new ExecutionException("Failed to create DNAT ip pool in VNMC for guest network with vlan " + vlanId);
                        }

                        if (!_connection.createTenantVDCDNatRule(tenant, rule.getId(), policyIdentifier, rule.getSrcIp())) {
                            throw new ExecutionException("Failed to create DNAT rule in VNMC for guest network with vlan " + vlanId);
                        }
                    }
                }
            }

            if (!_connection.associateAclPolicySet(tenant)) {
                throw new ExecutionException("Failed to associate source NAT policy set with edge security profile in VNMC for guest network with vlan " + vlanId);
            }
        } catch (ExecutionException e) {
            String msg = "SetStaticNatRulesCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    /*
     * PF
     */
    private synchronized Answer execute(SetPortForwardingRulesCommand cmd) {
        refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetPortForwardingRulesCommand cmd, int numRetries) {
        String vlanId = cmd.getContextParam(NetworkElementCommand.GUEST_VLAN_TAG);
        String tenant = "vlan-" + vlanId;

        PortForwardingRuleTO[] rules = cmd.getRules();
        Map<String, List<PortForwardingRuleTO>> publicIpRulesMap = new HashMap<String, List<PortForwardingRuleTO>>();
        for (PortForwardingRuleTO rule : rules) {
            String publicIp = rule.getSrcIp();
            if (!publicIpRulesMap.containsKey(publicIp)) {
                List<PortForwardingRuleTO> publicIpRulesList = new ArrayList<PortForwardingRuleTO>();
                publicIpRulesMap.put(publicIp, publicIpRulesList);
            }
            publicIpRulesMap.get(publicIp).add(rule);
        }

        try {
            if (!_connection.createTenantVDCNatPolicySet(tenant)) {
                throw new ExecutionException("Failed to create NAT policy set in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCAclPolicySet(tenant, true)) {
                throw new ExecutionException("Failed to create ACL ingress policy set in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCAclPolicySet(tenant, false)) {
                throw new ExecutionException("Failed to create ACL egress policy set in VNMC for guest network with vlan " + vlanId);
            }

            for (String publicIp : publicIpRulesMap.keySet()) {
                String policyIdentifier = publicIp.replace('.', '-');

                if (!_connection.createTenantVDCPFPolicy(tenant, policyIdentifier)) {
                    throw new ExecutionException("Failed to create PF policy in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCPFPolicyRef(tenant, policyIdentifier)) {
                    throw new ExecutionException("Failed to associate PF policy with NAT policy set in VNMC for guest network with vlan " + vlanId);
                }

                if (!_connection.createTenantVDCAclPolicy(tenant, policyIdentifier)) {
                    throw new ExecutionException("Failed to create ACL policy in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCAclPolicyRef(tenant, policyIdentifier, true)) {
                    throw new ExecutionException("Failed to associate ACL policy with ACL ingress policy set in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCAclPolicyRef(tenant, policyIdentifier, false)) {
                    throw new ExecutionException("Failed to associate ACL policy with ACL egress policy set in VNMC for guest network with vlan " + vlanId);
                }

                for (PortForwardingRuleTO rule : publicIpRulesMap.get(publicIp)) {
                    if (rule.revoked()) {
                        if (!_connection.deleteTenantVDCPFRule(tenant, rule.getId(), policyIdentifier)) {
                            throw new ExecutionException("Failed to delete PF rule in VNMC for guest network with vlan " + vlanId);
                        }
                    } else {
                        if (!_connection.createTenantVDCPFIpPool(tenant, Long.toString(rule.getId()), rule.getDstIp())) {
                            throw new ExecutionException("Failed to create PF ip pool in VNMC for guest network with vlan " + vlanId);
                        }
                        if (!_connection.createTenantVDCPFPortPool(tenant, Long.toString(rule.getId()), Integer.toString(rule.getDstPortRange()[0]),
                            Integer.toString(rule.getDstPortRange()[1]))) {
                            throw new ExecutionException("Failed to create PF port pool in VNMC for guest network with vlan " + vlanId);
                        }

                        if (!_connection.createTenantVDCPFRule(tenant, rule.getId(), policyIdentifier, rule.getProtocol().toUpperCase(), rule.getSrcIp(),
                            Integer.toString(rule.getSrcPortRange()[0]), Integer.toString(rule.getSrcPortRange()[1]))) {
                            throw new ExecutionException("Failed to create PF rule in VNMC for guest network with vlan " + vlanId);
                        }
                    }
                }
            }

            if (!_connection.associateAclPolicySet(tenant)) {
                throw new ExecutionException("Failed to associate source NAT policy set with edge security profile in VNMC for guest network with vlan " + vlanId);
            }
        } catch (ExecutionException e) {
            String msg = "SetPortForwardingRulesCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    /*
     * Logical edge firewall
     */
    private synchronized Answer execute(CreateLogicalEdgeFirewallCommand cmd) {
        refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private void createEdgeDeviceProfile(String tenant, List<String> gateways, Long vlanId) throws ExecutionException {
        // create edge device profile
        if (!_connection.createTenantVDCEdgeDeviceProfile(tenant))
            throw new ExecutionException("Failed to create tenant edge device profile in VNMC for guest network with vlan " + vlanId);

        // create edge static route policy
        if (!_connection.createTenantVDCEdgeStaticRoutePolicy(tenant))
            throw new ExecutionException("Failed to create tenant edge static route policy in VNMC for guest network with vlan " + vlanId);

        // create edge static route for all gateways
        for (String gateway : gateways) {
            if (!_connection.createTenantVDCEdgeStaticRoute(tenant, gateway, "0.0.0.0", "0.0.0.0"))
                throw new ExecutionException("Failed to create tenant edge static route in VNMC for guest network with vlan " + vlanId);
        }

        // associate edge
        if (!_connection.associateTenantVDCEdgeStaticRoutePolicy(tenant))
            throw new ExecutionException("Failed to associate edge static route policy with edge device profile in VNMC for guest network with vlan " + vlanId);
    }

    private Answer execute(CreateLogicalEdgeFirewallCommand cmd, int numRetries) {
        String tenant = "vlan-" + cmd.getVlanId();
        try {
            // create tenant
            if (!_connection.createTenant(tenant))
                throw new ExecutionException("Failed to create tenant in VNMC for guest network with vlan " + cmd.getVlanId());

            // create tenant VDC
            if (!_connection.createTenantVDC(tenant))
                throw new ExecutionException("Failed to create tenant VDC in VNMC for guest network with vlan " + cmd.getVlanId());

            // create edge security profile
            if (!_connection.createTenantVDCEdgeSecurityProfile(tenant))
                throw new ExecutionException("Failed to create tenant edge security profile in VNMC for guest network with vlan " + cmd.getVlanId());

            // create edge device profile and associated route
            createEdgeDeviceProfile(tenant, cmd.getPublicGateways(), cmd.getVlanId());

            // create logical edge firewall
            if (!_connection.createEdgeFirewall(tenant, cmd.getPublicIp(), cmd.getInternalIp(), cmd.getPublicSubnet(), cmd.getInternalSubnet()))
                throw new ExecutionException("Failed to create edge firewall in VNMC for guest network with vlan " + cmd.getVlanId());
        } catch (ExecutionException e) {
            String msg = "CreateLogicalEdgeFirewallCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    /*
     * Create vservice node and update inside port profile for ASA appliance in VSM
     */
    private synchronized Answer execute(ConfigureNexusVsmForAsaCommand cmd) {
        return execute(cmd, _numRetries);
    }

    private Answer execute(ConfigureNexusVsmForAsaCommand cmd, int numRetries) {
        String vlanId = Long.toString(cmd.getVlanId());
        NetconfHelper helper = null;
        List<Pair<OperationType, String>> params = new ArrayList<Pair<OperationType, String>>();
        params.add(new Pair<OperationType, String>(OperationType.addvlanid, vlanId));
        try {
            helper = new NetconfHelper(cmd.getVsmIp(), cmd.getVsmUsername(), cmd.getVsmPassword());
            s_logger.debug("Connected to Cisco VSM " + cmd.getVsmIp());
            helper.addVServiceNode(vlanId, cmd.getIpAddress());
            s_logger.debug("Created vservice node for ASA appliance in Cisco VSM for vlan " + vlanId);
            helper.updatePortProfile(cmd.getAsaInPortProfile(), SwitchPortMode.access, params);
            s_logger.debug("Updated inside port profile for ASA appliance in Cisco VSM with new vlan " + vlanId);
        } catch (CloudRuntimeException e) {
            String msg = "ConfigureVSMForASACommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        } finally {
            if( helper != null) {
                helper.disconnect();
            }
        }
        return new Answer(cmd, true, "Success");
    }

    /*
     * Associates ASA 1000v with logical edge firewall in VNMC
     */
    private synchronized Answer execute(AssociateAsaWithLogicalEdgeFirewallCommand cmd) {
        return execute(cmd, _numRetries);
    }

    private Answer execute(AssociateAsaWithLogicalEdgeFirewallCommand cmd, int numRetries) {
        String tenant = "vlan-" + cmd.getVlanId();
        try {
            Map<String, String> availableAsaAppliances = _connection.listUnAssocAsa1000v();
            if (availableAsaAppliances.isEmpty()) {
                throw new ExecutionException("No ASA 1000v available to associate with logical edge firewall for guest vlan " + cmd.getVlanId());
            }

            String asaInstanceDn = availableAsaAppliances.get(cmd.getAsaMgmtIp());
            if (asaInstanceDn == null) {
                throw new ExecutionException("Requested ASA 1000v (" + cmd.getAsaMgmtIp() + ") is not available");
            }

            if (!_connection.assignAsa1000v(tenant, asaInstanceDn)) {
                throw new ExecutionException("Failed to associate ASA 1000v (" + cmd.getAsaMgmtIp() + ") with logical edge firewall for guest vlan " + cmd.getVlanId());
            }
        } catch (ExecutionException e) {
            String msg = "AssociateAsaWithLogicalEdgeFirewallCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    /*
     * Cleanup
     */
    private synchronized Answer execute(CleanupLogicalEdgeFirewallCommand cmd) {
        refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(CleanupLogicalEdgeFirewallCommand cmd, int numRetries) {
        String tenant = "vlan-" + cmd.getVlanId();
        try {
            _connection.deleteTenant(tenant);
        } catch (ExecutionException e) {
            String msg = "CleanupLogicalEdgeFirewallCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub
    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub
    }

}
