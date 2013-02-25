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
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.cisco.n1kv.vsm.NetconfHelper;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.OperationType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;

public class CiscoVnmcResource implements ServerResource{

    private String _name;
    private String _zoneId;
    private String _physicalNetworkId;
    private String _ip;
    private String _username;
    private String _password;
    private String _guid;
    private Integer _numRetries;
    private String _publicZone;
    private String _privateZone;
    private String _publicInterface;
    private String _privateInterface;

    CiscoVnmcConnectionImpl _connection;

    private final Logger s_logger = Logger.getLogger(CiscoVnmcResource.class);

    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand) cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand) cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand) cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return execute((SetSourceNatCommand) cmd);
        } else if (cmd instanceof SetFirewallRulesCommand) {
            return execute((SetFirewallRulesCommand) cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            return execute((SetStaticNatRulesCommand) cmd);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            return execute((SetPortForwardingRulesCommand) cmd);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand) cmd);
        } else if (cmd instanceof CreateLogicalEdgeFirewallCommand) {
            return execute((CreateLogicalEdgeFirewallCommand)cmd);
        } else if (cmd instanceof ConfigureNexusVsmForAsaCommand) {
            return execute((ConfigureNexusVsmForAsaCommand)cmd);
        } else if (cmd instanceof AssociateAsaWithLogicalEdgeFirewallCommand) {
            return execute((AssociateAsaWithLogicalEdgeFirewallCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            _name = (String) params.get("name");
            if (_name == null) {
                throw new ConfigurationException("Unable to find name");
            }

            _zoneId = (String) params.get("zoneId");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone");
            }

            _physicalNetworkId = (String) params.get("physicalNetworkId");
            if (_physicalNetworkId == null) {
                throw new ConfigurationException("Unable to find physical network id in the configuration parameters");
            }

            _ip = (String) params.get("ip");
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP");
            }

            _username = (String) params.get("username");
            if (_username == null) {
                throw new ConfigurationException("Unable to find username");
            }

            _password = (String) params.get("password");
            if (_password == null) {
                throw new ConfigurationException("Unable to find password");
            }            

            _publicInterface = (String) params.get("publicinterface");
            if (_publicInterface == null) {
                //throw new ConfigurationException("Unable to find public interface.");
            }

            _privateInterface = (String) params.get("privateinterface");
            if (_privateInterface == null) {
                //throw new ConfigurationException("Unable to find private interface.");
            }

            _publicZone = (String) params.get("publiczone");
            if (_publicZone == null) {
                _publicZone = "untrust";
            }

            _privateZone = (String) params.get("privatezone");
            if (_privateZone == null) {
                _privateZone = "trust";
            }

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            _numRetries = NumbersUtil.parseInt((String) params.get("numretries"), 1);

            NumbersUtil.parseInt((String) params.get("timeout"), 300);

            // Open a socket and login
            _connection = new CiscoVnmcConnectionImpl(_ip, _username, _password);
            if (!refreshVnmcConnection()) {
                throw new ConfigurationException("Unable to open a connection to the VNMC.");
            }

            return true;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }

    }

    public StartupCommand[] initialize() {   
        StartupExternalFirewallCommand cmd = new StartupExternalFirewallCommand();
        cmd.setName(_name);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion("");
        cmd.setGuid(_guid);
        return new StartupCommand[] { cmd };
    }

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
        return new PingCommand(Host.Type.ExternalFirewall, id);
    }

    @Override
    public void disconnected() {
    }

    public IAgentControl getAgentControl() {
        return null;
    }

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
                throw new Exception("Failed to create NAT policy set in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCSourceNatPolicy(tenant, policyIdentifier)) {
                throw new Exception("Failed to create source NAT policy in VNMC for guest network with vlan " + vlanId);
            }
            if (!_connection.createTenantVDCSourceNatPolicyRef(tenant, policyIdentifier)) {
                throw new Exception("Failed to associate source NAT policy with NAT policy set in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.createTenantVDCSourceNatIpPool(tenant, policyIdentifier, cmd.getIpAddress().getPublicIp())) {
                throw new Exception("Failed to create source NAT ip pool in VNMC for guest network with vlan " + vlanId);
            }

            String cidr = cmd.getContextParam(NetworkElementCommand.GUEST_NETWORK_CIDR);
            String[] result = cidr.split("\\/");
            assert (result.length == 2) : "Something is wrong with guest cidr " + cidr;
            long size = Long.valueOf(result[1]);
            String startIp = NetUtils.getIpRangeStartIpFromCidr(result[0], size);
            String endIp = NetUtils.getIpRangeEndIpFromCidr(result[0], size);
            if (!_connection.createTenantVDCSourceNatRule(tenant, policyIdentifier, startIp, endIp)) {
                throw new Exception("Failed to create source NAT rule in VNMC for guest network with vlan " + vlanId);
            }

            if (!_connection.associateNatPolicySet(tenant)) {
                throw new Exception("Failed to associate source NAT policy set with edge security profile in VNMC for guest network with vlan " + vlanId);
            }
        } catch (Throwable e) {
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
                throw new Exception("Failed to create ACL ingress policy set in VNMC for guest network with vlan " + vlanId);
            }
            // TODO for egress

            for (String publicIp : publicIpRulesMap.keySet()) {
                String policyIdentifier = publicIp.replace('.', '-');

                if (!_connection.deleteTenantVDCAclPolicy(tenant, policyIdentifier)) {
                    throw new Exception("Failed to delete ACL ingress policy in VNMC for guest network with vlan " + vlanId);
                }
                // TODO for egress

                if (!_connection.createTenantVDCAclPolicy(tenant, policyIdentifier, true)) {
                    throw new Exception("Failed to create ACL ingress policy in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCAclPolicyRef(tenant, policyIdentifier, true)) {
                    throw new Exception("Failed to associate ACL ingress policy with ACL ingress policy set in VNMC for guest network with vlan " + vlanId);
                }
                // TODO for egress

                for (FirewallRuleTO rule : publicIpRulesMap.get(publicIp)) {
                    if (rule.revoked()) {
                        //_connection.deleteAclRule(tenant, Long.toString(rule.getId()), publicIp);
                    } else {
                        String cidr = rule.getSourceCidrList().get(0);
                        String[] result = cidr.split("\\/");
                        assert (result.length == 2) : "Something is wrong with source cidr " + cidr;
                        long size = Long.valueOf(result[1]);
                        String externalStartIp = NetUtils.getIpRangeStartIpFromCidr(result[0], size);
                        String externalEndIp = NetUtils.getIpRangeEndIpFromCidr(result[0], size);

                        if (!_connection.createIngressAclRule(tenant,
                                Long.toString(rule.getId()), policyIdentifier,
                                rule.getProtocol().toUpperCase(), externalStartIp, externalEndIp,
                                Integer.toString(rule.getSrcPortRange()[0]), Integer.toString(rule.getSrcPortRange()[1]), publicIp)) {
                            throw new Exception("Failed to create ACL ingress rule in VNMC for guest network with vlan " + vlanId);
                        }
                    }
                    // TODO for egress
                }
            }

            if (!_connection.associateAclPolicySet(tenant)) {
                throw new Exception("Failed to associate ACL policy set with edge security profile in VNMC for guest network with vlan " + vlanId);
            }
        } catch (Throwable e) {
            String msg = "SetFirewallRulesCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd);
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
                throw new Exception("Failed to create NAT policy set in VNMC for guest network with vlan " + vlanId);
            }

            for (String publicIp : publicIpRulesMap.keySet()) {
                String policyIdentifier = publicIp.replace('.', '-');

                if (!_connection.deleteTenantVDCDNatPolicy(tenant, policyIdentifier)) {
                    throw new Exception("Failed to delete ACL ingress policy in VNMC for guest network with vlan " + vlanId);
                }

                if (!_connection.createTenantVDCDNatPolicy(tenant, policyIdentifier)) {
                    throw new Exception("Failed to create DNAT policy in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCDNatPolicyRef(tenant, policyIdentifier)) {
                    throw new Exception("Failed to associate DNAT policy with NAT policy set in VNMC for guest network with vlan " + vlanId);
                }

                for (StaticNatRuleTO rule : publicIpRulesMap.get(publicIp)) {
                    if (rule.revoked()) {
                        //_connection.deleteDNatRule(tenant, Long.toString(rule.getId()), publicIp);
                    } else {
                        if (!_connection.createTenantVDCDNatIpPool(tenant, policyIdentifier + "-" + rule.getId(), rule.getDstIp())) {
                            throw new Exception("Failed to create DNAT ip pool in VNMC for guest network with vlan " + vlanId);
                        }

                        if (!_connection.createTenantVDCDNatRule(tenant,
                                Long.toString(rule.getId()), policyIdentifier, rule.getSrcIp())) {
                            throw new Exception("Failed to create DNAT rule in VNMC for guest network with vlan " + vlanId);
                        }
                    }
                }
            }

            if (!_connection.associateNatPolicySet(tenant)) {
                throw new Exception("Failed to associate source NAT policy set with edge security profile in VNMC for guest network with vlan " + vlanId);
            }
        } catch (Throwable e) {
            String msg = "SetSourceNatCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

    /*
     * Destination NAT
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
                throw new Exception("Failed to create NAT policy set in VNMC for guest network with vlan " + vlanId);
            }

            for (String publicIp : publicIpRulesMap.keySet()) {
                String policyIdentifier = publicIp.replace('.', '-');

                if (!_connection.deleteTenantVDCPFPolicy(tenant, policyIdentifier)) {
                    throw new Exception("Failed to delete ACL ingress policy in VNMC for guest network with vlan " + vlanId);
                }

                if (!_connection.createTenantVDCPFPolicy(tenant, policyIdentifier)) {
                    throw new Exception("Failed to create PF policy in VNMC for guest network with vlan " + vlanId);
                }
                if (!_connection.createTenantVDCPFPolicyRef(tenant, policyIdentifier)) {
                    throw new Exception("Failed to associate PF policy with NAT policy set in VNMC for guest network with vlan " + vlanId);
                }

                for (PortForwardingRuleTO rule : publicIpRulesMap.get(publicIp)) {
                    if (rule.revoked()) {
                        //_connection.deletePFRule(tenant, Long.toString(rule.getId()), publicIp);
                    } else {
                        if (!_connection.createTenantVDCPFIpPool(tenant, policyIdentifier + "-" + rule.getId(), rule.getDstIp())) {
                            throw new Exception("Failed to create PF ip pool in VNMC for guest network with vlan " + vlanId);
                        }

                        if (!_connection.createTenantVDCPFPortPool(tenant, policyIdentifier + "-" + rule.getId(),
                                Integer.toString(rule.getDstPortRange()[0]), Integer.toString(rule.getDstPortRange()[1]))) {
                            throw new Exception("Failed to create PF port pool in VNMC for guest network with vlan " + vlanId);
                        }

                        if (!_connection.createTenantVDCPFRule(tenant,
                                Long.toString(rule.getId()), policyIdentifier,
                                rule.getProtocol().toUpperCase(), rule.getSrcIp(),
                                Integer.toString(rule.getSrcPortRange()[0]), Integer.toString(rule.getSrcPortRange()[1]))) {
                            throw new Exception("Failed to create PF rule in VNMC for guest network with vlan " + vlanId);
                        }
                    }
                }
            }

            if (!_connection.associateNatPolicySet(tenant)) {
                throw new Exception("Failed to associate source NAT policy set with edge security profile in VNMC for guest network with vlan " + vlanId);
            }
        } catch (Throwable e) {
            String msg = "SetSourceNatCommand failed due to " + e.getMessage();
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

    private Answer execute(CreateLogicalEdgeFirewallCommand cmd, int numRetries) {
        String tenant = "vlan-" + cmd.getVlanId();
        try {
            // create tenant
            if (!_connection.createTenant(tenant))
                throw new Exception("Failed to create tenant in VNMC for guest network with vlan " + cmd.getVlanId());

            // create tenant VDC
            if (!_connection.createTenantVDC(tenant))
                throw new Exception("Failed to create tenant VDC in VNMC for guest network with vlan " + cmd.getVlanId());

            // create edge security profile
            if (!_connection.createTenantVDCEdgeSecurityProfile(tenant))
                throw new Exception("Failed to create tenant edge security profile in VNMC for guest network with vlan " + cmd.getVlanId());

            // create logical edge firewall
            if (!_connection.createEdgeFirewall(tenant, cmd.getPublicIp(), cmd.getInternalIp(), cmd.getPublicSubnet(), cmd.getInternalSubnet()))
                throw new Exception("Failed to create edge firewall in VNMC for guest network with vlan " + cmd.getVlanId());
        } catch (Throwable e) {
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
        } catch (Throwable e) {
            String msg = "ConfigureVSMForASACommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        } finally {
            helper.disconnect();
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
                throw new Exception("No ASA 1000v available to associate with logical edge firewall for guest vlan " + cmd.getVlanId());
            }

            String asaInstanceDn = availableAsaAppliances.get(cmd.getAsaMgmtIp());
            if (asaInstanceDn == null) {
                throw new Exception("Requested ASA 1000v (" + cmd.getAsaMgmtIp() + ") is not available");
            }

            if (!_connection.assocAsa1000v(tenant, asaInstanceDn)) {
                throw new Exception("Failed to associate ASA 1000v (" + cmd.getAsaMgmtIp() + ") with logical edge firewall for guest vlan " + cmd.getVlanId());
            }
        } catch (Throwable e) {
            String msg = "AssociateAsaWithLogicalEdgeFirewallCommand failed due to " + e.getMessage();
            s_logger.error(msg, e);
            return new Answer(cmd, false, msg);
        }

        return new Answer(cmd, true, "Success");
    }

}
