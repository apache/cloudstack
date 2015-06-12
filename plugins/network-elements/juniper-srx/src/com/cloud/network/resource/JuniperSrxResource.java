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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
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
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand.UsernamePassword;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;

public class JuniperSrxResource implements ServerResource {

    private String _name;
    private String _zoneId;
    private String _ip;
    private String _username;
    private String _password;
    private String _guid;
    private String _objectNameWordSep;
    private BufferedWriter _toSrx;
    private BufferedReader _fromSrx;
    private BufferedWriter _UsagetoSrx;
    private BufferedReader _UsagefromSrx;
    private Integer _numRetries;
    private Integer _timeoutInSeconds;
    private String _publicZone;
    private String _privateZone;
    private String _publicZoneInputFilterName;
    private String _publicInterface;
    private String _usageInterface;
    private String _privateInterface;
    private String _ikeProposalName;
    private String _ipsecPolicyName;
    private String _primaryDnsAddress;
    private String _ikeGatewayHostname;
    private String _vpnObjectPrefix;
    private UsageFilter _usageFilterVlanInput;
    private UsageFilter _usageFilterVlanOutput;
    private UsageFilter _usageFilterIPInput;
    private UsageFilter _usageFilterIPOutput;
    private static final Logger s_logger = Logger.getLogger(JuniperSrxResource.class);

    private enum SrxXml {
        LOGIN("login.xml"),
        PRIVATE_INTERFACE_ADD("private-interface-add.xml"),
        PRIVATE_INTERFACE_WITH_FILTERS_ADD("private-interface-with-filters-add.xml"),
        PRIVATE_INTERFACE_GETONE("private-interface-getone.xml"),
        PROXY_ARP_ADD("proxy-arp-add.xml"),
        PROXY_ARP_GETONE("proxy-arp-getone.xml"),
        PROXY_ARP_GETALL("proxy-arp-getall.xml"),
        ZONE_INTERFACE_ADD("zone-interface-add.xml"),
        ZONE_INTERFACE_GETONE("zone-interface-getone.xml"),
        SRC_NAT_POOL_ADD("src-nat-pool-add.xml"),
        SRC_NAT_POOL_GETONE("src-nat-pool-getone.xml"),
        SRC_NAT_RULE_ADD("src-nat-rule-add.xml"),
        SRC_NAT_RULE_GETONE("src-nat-rule-getone.xml"),
        SRC_NAT_RULE_GETALL("src-nat-rule-getall.xml"),
        DEST_NAT_POOL_ADD("dest-nat-pool-add.xml"),
        DEST_NAT_POOL_GETONE("dest-nat-pool-getone.xml"),
        DEST_NAT_POOL_GETALL("dest-nat-pool-getall.xml"),
        DEST_NAT_RULE_ADD("dest-nat-rule-add.xml"),
        DEST_NAT_RULE_GETONE("dest-nat-rule-getone.xml"),
        DEST_NAT_RULE_GETALL("dest-nat-rule-getall.xml"),
        STATIC_NAT_RULE_ADD("static-nat-rule-add.xml"),
        STATIC_NAT_RULE_GETONE("static-nat-rule-getone.xml"),
        STATIC_NAT_RULE_GETALL("static-nat-rule-getall.xml"),
        ADDRESS_BOOK_ENTRY_ADD("address-book-entry-add.xml"),
        ADDRESS_BOOK_ENTRY_GETONE("address-book-entry-getone.xml"),
        ADDRESS_BOOK_ENTRY_GETALL("address-book-entry-getall.xml"),
        APPLICATION_ADD("application-add.xml"),
        APPLICATION_GETONE("application-getone.xml"),
        SECURITY_POLICY_ADD("security-policy-add.xml"),
        SECURITY_POLICY_GETONE("security-policy-getone.xml"),
        SECURITY_POLICY_GETALL("security-policy-getall.xml"),
        SECURITY_POLICY_GROUP("security-policy-group.xml"),
        GUEST_VLAN_FILTER_TERM_ADD("guest-vlan-filter-term-add.xml"),
        PUBLIC_IP_FILTER_TERM_ADD("public-ip-filter-term-add.xml"),
        FILTER_TERM_GETONE("filter-term-getone.xml"),
        FILTER_GETONE("filter-getone.xml"),
        FIREWALL_FILTER_BYTES_GETALL("firewall-filter-bytes-getall.xml"),
        IKE_POLICY_ADD("ike-policy-add.xml"),
        IKE_POLICY_GETONE("ike-policy-getone.xml"),
        IKE_POLICY_GETALL("ike-policy-getall.xml"),
        IKE_GATEWAY_ADD("ike-gateway-add.xml"),
        IKE_GATEWAY_GETONE("ike-gateway-getone.xml"),
        IKE_GATEWAY_GETALL("ike-gateway-getall.xml"),
        IPSEC_VPN_ADD("ipsec-vpn-add.xml"),
        IPSEC_VPN_GETONE("ipsec-vpn-getone.xml"),
        IPSEC_VPN_GETALL("ipsec-vpn-getall.xml"),
        DYNAMIC_VPN_CLIENT_ADD("dynamic-vpn-client-add.xml"),
        DYNAMIC_VPN_CLIENT_GETONE("dynamic-vpn-client-getone.xml"),
        DYNAMIC_VPN_CLIENT_GETALL("dynamic-vpn-client-getall.xml"),
        ADDRESS_POOL_ADD("address-pool-add.xml"),
        ADDRESS_POOL_GETONE("address-pool-getone.xml"),
        ADDRESS_POOL_GETALL("address-pool-getall.xml"),
        ACCESS_PROFILE_ADD("access-profile-add.xml"),
        ACCESS_PROFILE_GETONE("access-profile-getone.xml"),
        ACCESS_PROFILE_GETALL("access-profile-getall.xml"),
        FIREWALL_FILTER_TERM_ADD("firewall-filter-term-add.xml"),
        FIREWALL_FILTER_TERM_GETONE("firewall-filter-term-getone.xml"),
        TEMPLATE_ENTRY("template-entry.xml"),
        OPEN_CONFIGURATION("open-configuration.xml"),
        CLOSE_CONFIGURATION("close-configuration.xml"),
        COMMIT("commit.xml"),
        ROLLBACK("rollback.xml"),
        TEST("test.xml");

        private final String scriptsDir = "scripts/network/juniper";
        private final String xml;
        private static final Logger s_logger = Logger.getLogger(JuniperSrxResource.class);

        private SrxXml(String filename) {
            xml = getXml(filename);
        }

        public String getXml() {
            return xml;
        }

        private String getXml(String filename) {
            try {
                String xmlFilePath = Script.findScript(scriptsDir, filename);

                if (xmlFilePath == null) {
                    throw new Exception("Failed to find Juniper SRX XML file: " + filename);
                }

                try(InputStreamReader fr = new InputStreamReader(new FileInputStream(xmlFilePath),"UTF-8");
                BufferedReader br = new BufferedReader(fr);) {
                    String xml = "";
                    String line;
                    while ((line = br.readLine()) != null) {
                        xml += line.trim();
                    }
                    return xml;
                }catch (Exception e) {
                    s_logger.debug(e);
                    return null;
                }
            } catch (Exception e) {
                s_logger.debug(e);
                return null;
            }
        }
    }

    public class UsageFilter {
        private final String name;
        private final String counterIdentifier;
        private final String addressType;

        private UsageFilter(String name, String addressType, String counterIdentifier) {
            this.name = name;
            this.addressType = addressType;

            if (_usageInterface != null) {
                counterIdentifier = _usageInterface + counterIdentifier;
            }

            this.counterIdentifier = counterIdentifier;
        }

        public String getName() {
            return name;
        }

        public String getCounterIdentifier() {
            return counterIdentifier;
        }

        public String getAddressType() {
            return addressType;
        }
    }

    public class FirewallFilterTerm {
        private final String name;
        private final List<String> sourceCidrs;
        private final String destIp;
        private String portRange;
        private final String protocol;
        private String icmpType;
        private String icmpCode;
        private final String countName;

        private FirewallFilterTerm(String name, List<String> sourceCidrs, String destIp, String protocol, Integer startPort, Integer endPort, Integer icmpType,
                Integer icmpCode, String countName) {
            this.name = name;
            this.sourceCidrs = sourceCidrs;
            this.destIp = destIp;
            this.protocol = protocol;

            if (protocol.equals("tcp") || protocol.equals("udp")) {
                portRange = String.valueOf(startPort) + "-" + String.valueOf(endPort);
            } else if (protocol.equals("icmp")) {
                this.icmpType = String.valueOf(icmpType);
                this.icmpCode = String.valueOf(icmpCode);
            } else {
                assert protocol.equals("any");
            }
            this.countName = countName;

        }

        public String getName() {
            return name;
        }

        public List<String> getSourceCidrs() {
            return sourceCidrs;
        }

        public String getDestIp() {
            return destIp;
        }

        public String getPortRange() {
            return portRange;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getIcmpType() {
            return icmpType;
        }

        public String getIcmpCode() {
            return icmpCode;
        }

        public String getCountName() {
            return countName;
        }
    }

    private enum SrxCommand {
        LOGIN, OPEN_CONFIGURATION, CLOSE_CONFIGURATION, COMMIT, ROLLBACK, CHECK_IF_EXISTS, CHECK_IF_IN_USE, ADD, DELETE, GET_ALL;
    }

    private enum Protocol {
        tcp, udp, icmp, all, any;
    }

    private enum RuleMatchCondition {
        ALL, PUBLIC_PRIVATE_IPS, PRIVATE_SUBNET;
    }

    private enum GuestNetworkType {
        SOURCE_NAT, INTERFACE_NAT;
    }

    private enum SecurityPolicyType {
        STATIC_NAT("staticnat"), DESTINATION_NAT("destnat"), VPN("vpn"), SECURITYPOLICY_EGRESS("egress"), SECURITYPOLICY_EGRESS_DEFAULT("egress-default");

        private final String identifier;

        private SecurityPolicyType(String identifier) {
            this.identifier = identifier;
        }

        private String getIdentifier() {
            return identifier;
        }
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand)cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            return execute((SetStaticNatRulesCommand)cmd);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            return execute((SetPortForwardingRulesCommand)cmd);
        } else if (cmd instanceof SetFirewallRulesCommand) {
            return execute((SetFirewallRulesCommand)cmd);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand)cmd);
        } else if (cmd instanceof RemoteAccessVpnCfgCommand) {
            return execute((RemoteAccessVpnCfgCommand)cmd);
        } else if (cmd instanceof VpnUsersCfgCommand) {
            return execute((VpnUsersCfgCommand)cmd);
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

            _publicInterface = (String)params.get("publicinterface");
            if (_publicInterface == null) {
                throw new ConfigurationException("Unable to find public interface.");
            }

            _privateInterface = (String)params.get("privateinterface");
            if (_privateInterface == null) {
                throw new ConfigurationException("Unable to find private interface.");
            }

            _publicZone = (String)params.get("publiczone");
            if (_publicZone == null) {
                _publicZone = "untrust";
            }

            _privateZone = (String)params.get("privatezone");
            if (_privateZone == null) {
                _privateZone = "trust";
            }

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            _numRetries = NumbersUtil.parseInt((String)params.get("numretries"), 1);

            _timeoutInSeconds = NumbersUtil.parseInt((String)params.get("timeout"), 300);

            _objectNameWordSep = "-";

            _ikeProposalName = "cloud-ike-proposal";
            _ipsecPolicyName = "cloud-ipsec-policy";
            _ikeGatewayHostname = "cloud";
            _vpnObjectPrefix = "vpn-a";
            _primaryDnsAddress = "4.2.2.2";

            // Open a socket and login
            if (!refreshSrxConnection()) {
                throw new ConfigurationException("Unable to open a connection to the SRX.");
            }

            _publicZoneInputFilterName = _publicZone;

            _usageFilterVlanInput = new UsageFilter("vlan-input", null, "vlan-input");
            _usageFilterVlanOutput = new UsageFilter("vlan-output", null, "vlan-output");
            _usageFilterIPInput = new UsageFilter(_publicZone, "destination-address", "-i");
            _usageFilterIPOutput = new UsageFilter(_privateZone, "source-address", "-o");

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
        cmd.setVersion(JuniperSrxResource.class.getPackage().getImplementationVersion());
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
        return new PingCommand(Host.Type.ExternalFirewall, id);
    }

    @Override
    public void disconnected() {
        closeSocket();
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
        try {
            return getUsageAnswer(cmd);
        } catch (ExecutionException e) {
            return new ExternalNetworkResourceUsageAnswer(cmd, e);
        }
    }

    /*
     * Login
     */

    private boolean refreshSrxConnection() {
        if (!(closeSocket() && openSocket())) {
            return false;
        }

        try {
            return login();
        } catch (ExecutionException e) {
            s_logger.error("Failed to login due to " + e.getMessage());
            return false;
        }
    }

    private boolean login() throws ExecutionException {
        String xml = SrxXml.LOGIN.getXml();
        xml = replaceXmlValue(xml, "username", _username);
        xml = replaceXmlValue(xml, "password", _password);
        return sendRequestAndCheckResponse(SrxCommand.LOGIN, xml);
    }

    private boolean openSocket() {
        try {
            Socket s = new Socket(_ip, 3221);
            s.setKeepAlive(true);
            s.setSoTimeout(_timeoutInSeconds * 1000);
            _toSrx = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"UTF-8"));
            _fromSrx = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
            return true;
        } catch (IOException e) {
            s_logger.error(e);
            return false;
        }
    }

    private boolean closeSocket() {
        try {
            if (_toSrx != null) {
                _toSrx.close();
            }

            if (_fromSrx != null) {
                _fromSrx.close();
            }

            return true;
        } catch (IOException e) {
            s_logger.error(e);
            return false;
        }
    }

    /*
     * The usage data will be handled on it's own socket, so usage
     * commands will use the following methods...
     */
    private boolean usageLogin() throws ExecutionException {
        String xml = SrxXml.LOGIN.getXml();
        xml = replaceXmlValue(xml, "username", _username);
        xml = replaceXmlValue(xml, "password", _password);
        return sendUsageRequestAndCheckResponse(SrxCommand.LOGIN, xml);
    }

    private boolean openUsageSocket() throws ExecutionException {
        try {
            Socket s = new Socket(_ip, 3221);
            s.setKeepAlive(true);
            s.setSoTimeout(_timeoutInSeconds * 1000);
            _UsagetoSrx = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(),"UTF-8"));
            _UsagefromSrx = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
            return usageLogin();
        } catch (IOException e) {
            s_logger.error(e);
            return false;
        }
    }

    private boolean closeUsageSocket() {
        try {
            if (_UsagetoSrx != null) {
                _UsagetoSrx.close();
            }

            if (_UsagefromSrx != null) {
                _UsagefromSrx.close();
            }

            return true;
        } catch (IOException e) {
            s_logger.error(e);
            return false;
        }
    }

    /*
     * Commit/rollback
     */

    private void openConfiguration() throws ExecutionException {
        String xml = SrxXml.OPEN_CONFIGURATION.getXml();
        String successMsg = "Opened a private configuration.";
        String errorMsg = "Failed to open a private configuration.";

        if (!sendRequestAndCheckResponse(SrxCommand.OPEN_CONFIGURATION, xml)) {
            throw new ExecutionException(errorMsg);
        } else {
            s_logger.debug(successMsg);
        }
    }

    private void closeConfiguration() {
        String xml = SrxXml.CLOSE_CONFIGURATION.getXml();
        String successMsg = "Closed private configuration.";
        String errorMsg = "Failed to close private configuration.";

        try {
            if (!sendRequestAndCheckResponse(SrxCommand.CLOSE_CONFIGURATION, xml)) {
                s_logger.error(errorMsg);
            }
        } catch (ExecutionException e) {
            s_logger.error(errorMsg);
        }

        s_logger.debug(successMsg);
    }

    private void commitConfiguration() throws ExecutionException {
        String xml = SrxXml.COMMIT.getXml();
        String successMsg = "Committed to global configuration.";
        String errorMsg = "Failed to commit to global configuration.";

        if (!sendRequestAndCheckResponse(SrxCommand.COMMIT, xml)) {
            throw new ExecutionException(errorMsg);
        } else {
            s_logger.debug(successMsg);
            closeConfiguration();
        }
    }

    /*
     * Guest networks
     */

    private synchronized Answer execute(IpAssocCommand cmd) {
        refreshSrxConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(IpAssocCommand cmd, int numRetries) {
        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        try {
            IpAddressTO ip;
            if (cmd.getIpAddresses().length != 1) {
                throw new ExecutionException("Received an invalid number of guest IPs to associate.");
            } else {
                ip = cmd.getIpAddresses()[0];
            }

            String sourceNatIpAddress = null;
            GuestNetworkType type = GuestNetworkType.INTERFACE_NAT;

            if (ip.isSourceNat()) {
                type = GuestNetworkType.SOURCE_NAT;

                if (ip.getPublicIp() == null) {
                    throw new ExecutionException("Source NAT IP address must not be null.");
                } else {
                    sourceNatIpAddress = ip.getPublicIp();
                }
            }

            long guestVlanTag = Long.parseLong(cmd.getAccessDetail(NetworkElementCommand.GUEST_VLAN_TAG));
            String guestVlanGateway = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY);
            String cidr = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR);
            long cidrSize = NetUtils.cidrToLong(cidr)[1];
            String guestVlanSubnet = NetUtils.getCidrSubNet(guestVlanGateway, cidrSize);

            Long publicVlanTag = null;
            if (ip.getBroadcastUri() != null && !ip.getBroadcastUri().equals("untagged")) {
                try {
                    publicVlanTag = Long.parseLong(BroadcastDomainType.getValue(ip.getBroadcastUri()));
                } catch (Exception e) {
                    throw new ExecutionException("Could not parse public VLAN tag: " + ip.getBroadcastUri());
                }
            }

            openConfiguration();

            // Remove the guest network:
            // Remove source, static, and destination NAT rules
            // Remove VPN
            shutdownGuestNetwork(type, ip.getAccountId(), publicVlanTag, sourceNatIpAddress, guestVlanTag, guestVlanGateway, guestVlanSubnet, cidrSize);

            if (ip.isAdd()) {
                // Implement the guest network for this VLAN
                implementGuestNetwork(type, publicVlanTag, sourceNatIpAddress, guestVlanTag, guestVlanGateway, guestVlanSubnet, cidrSize);
            }

            commitConfiguration();
            results[i++] = ip.getPublicIp() + " - success";
        } catch (ExecutionException e) {
            s_logger.error(e);
            closeConfiguration();

            if (numRetries > 0 && refreshSrxConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying IPAssocCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                results[i++] = IpAssocAnswer.errorResult;
            }
        }

        return new IpAssocAnswer(cmd, results);
    }

    private void implementGuestNetwork(GuestNetworkType type, Long publicVlanTag, String publicIp, long privateVlanTag, String privateGateway, String privateSubnet,
        long privateCidrNumber) throws ExecutionException {
        privateGateway = privateGateway + "/" + privateCidrNumber;
        privateSubnet = privateSubnet + "/" + privateCidrNumber;

        managePrivateInterface(SrxCommand.ADD, !type.equals(GuestNetworkType.SOURCE_NAT), privateVlanTag, privateGateway);
        manageZoneInterface(SrxCommand.ADD, privateVlanTag);

        if (type.equals(GuestNetworkType.SOURCE_NAT)) {
            manageSourceNatPool(SrxCommand.ADD, publicIp);
            manageSourceNatRule(SrxCommand.ADD, publicIp, privateSubnet);
            manageProxyArp(SrxCommand.ADD, publicVlanTag, publicIp);
            manageUsageFilter(SrxCommand.ADD, _usageFilterIPOutput, privateSubnet, null, genIpFilterTermName(publicIp));
            manageUsageFilter(SrxCommand.ADD, _usageFilterIPInput, publicIp, null, genIpFilterTermName(publicIp));
        } else if (type.equals(GuestNetworkType.INTERFACE_NAT)) {
            manageUsageFilter(SrxCommand.ADD, _usageFilterVlanOutput, null, privateVlanTag, null);
            manageUsageFilter(SrxCommand.ADD, _usageFilterVlanInput, null, privateVlanTag, null);
        }

        String msg = "Implemented guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway;
        msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + publicIp : "";
        s_logger.debug(msg);
    }

    private void shutdownGuestNetwork(GuestNetworkType type, long accountId, Long publicVlanTag, String sourceNatIpAddress, long privateVlanTag, String privateGateway,
        String privateSubnet, long privateCidrSize) throws ExecutionException {
        // Remove static and destination NAT rules for the guest network
        removeStaticAndDestNatRulesInPrivateVlan(privateVlanTag, privateGateway, privateCidrSize);

        privateGateway = privateGateway + "/" + privateCidrSize;
        privateSubnet = privateSubnet + "/" + privateCidrSize;

        managePrivateInterface(SrxCommand.DELETE, false, privateVlanTag, privateGateway);
        manageZoneInterface(SrxCommand.DELETE, privateVlanTag);
        deleteVpnObjectsForAccount(accountId);

        if (type.equals(GuestNetworkType.SOURCE_NAT)) {
            manageSourceNatRule(SrxCommand.DELETE, sourceNatIpAddress, privateSubnet);
            manageSourceNatPool(SrxCommand.DELETE, sourceNatIpAddress);
            manageProxyArp(SrxCommand.DELETE, publicVlanTag, sourceNatIpAddress);
            manageUsageFilter(SrxCommand.DELETE, _usageFilterIPOutput, privateSubnet, null, genIpFilterTermName(sourceNatIpAddress));
            manageUsageFilter(SrxCommand.DELETE, _usageFilterIPInput, sourceNatIpAddress, null, genIpFilterTermName(sourceNatIpAddress));
        } else if (type.equals(GuestNetworkType.INTERFACE_NAT)) {
            manageUsageFilter(SrxCommand.DELETE, _usageFilterVlanOutput, null, privateVlanTag, null);
            manageUsageFilter(SrxCommand.DELETE, _usageFilterVlanInput, null, privateVlanTag, null);
        }

        String msg = "Shut down guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway;
        msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + sourceNatIpAddress : "";
        s_logger.debug(msg);
    }

    private Map<String, ArrayList<FirewallRuleTO>> getActiveFirewallEgressRules(FirewallRuleTO[] allRules) {
        Map<String, ArrayList<FirewallRuleTO>> activeRules = new HashMap<String, ArrayList<FirewallRuleTO>>();

        for (FirewallRuleTO rule : allRules) {
            String guestVlan;
            guestVlan = rule.getSrcVlanTag();

            ArrayList<FirewallRuleTO> activeRulesForNetwork = activeRules.get(guestVlan);

            if (activeRulesForNetwork == null) {
                activeRulesForNetwork = new ArrayList<FirewallRuleTO>();
            }

            if (!rule.revoked() || rule.isAlreadyAdded()) {
                activeRulesForNetwork.add(rule);
            }

            activeRules.put(guestVlan, activeRulesForNetwork);
        }

        return activeRules;
    }

    private List<String> extractCidrs(List<FirewallRuleTO> rules) throws ExecutionException {
        List<String> allCidrs = new ArrayList<String>();
        List<String> cidrs = new ArrayList<String>();

        for (FirewallRuleTO rule : rules) {
            cidrs = (rule.getSourceCidrList());
            for (String cidr : cidrs) {
                if (!allCidrs.contains(cidr)) {
                    allCidrs.add(cidr);
                }
            }
        }
        return allCidrs;
    }

    /* security policies */
    private synchronized Answer execute(SetFirewallRulesCommand cmd) {
        refreshSrxConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetFirewallRulesCommand cmd, int numRetries) {
        FirewallRuleTO[] rules = cmd.getRules();
        try {
            openConfiguration();
            if (rules[0].getTrafficType() == FirewallRule.TrafficType.Egress) {
                Map<String, ArrayList<FirewallRuleTO>> activeRules = getActiveFirewallEgressRules(rules);
                Set<String> guestVlans = activeRules.keySet();
                // List<String> cidrs = new ArrayList();
                boolean defaultEgressPolicy = rules[0].isDefaultEgressPolicy();
                FirewallRule.FirewallRuleType type = rules[0].getType();
                //getting
                String guestCidr = rules[0].getGuestCidr();
                List<String> cidrs = new ArrayList<String>();
                cidrs.add(guestCidr);

                List<Object[]> applications = new ArrayList<Object[]>();
                Object[] application = new Object[3];
                application[0] = Protocol.all;
                application[1] = NetUtils.PORT_RANGE_MIN;
                application[2] = NetUtils.PORT_RANGE_MAX;
                applications.add(application);

                for (String guestVlan : guestVlans) {
                    List<FirewallRuleTO> activeRulesForGuestNw = activeRules.get(guestVlan);

                    removeEgressSecurityPolicyAndApplications(SecurityPolicyType.SECURITYPOLICY_EGRESS, guestVlan, extractCidrs(activeRulesForGuestNw),
                        defaultEgressPolicy);
                    if (activeRulesForGuestNw.size() > 0 && type == FirewallRule.FirewallRuleType.User) {
                        addEgressSecurityPolicyAndApplications(SecurityPolicyType.SECURITYPOLICY_EGRESS, guestVlan, extractApplications(activeRulesForGuestNw),
                            extractCidrs(activeRulesForGuestNw), defaultEgressPolicy);

                        /* Adding default policy rules are required because the order of rules is important.
                         * Depending on the rules order the traffic accept/drop is performed
                         */
                        removeEgressSecurityPolicyAndApplications(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT, guestVlan, cidrs, defaultEgressPolicy);
                        addEgressSecurityPolicyAndApplications(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT, guestVlan, applications, cidrs, defaultEgressPolicy);
                    }

                    //remove required with out comparing default policy  because in upgrade network offering we may required to delete
                    // the previously added rule
                    removeEgressSecurityPolicyAndApplications(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT, guestVlan, cidrs, false);
                    if (defaultEgressPolicy == true && type == FirewallRule.FirewallRuleType.System) {
                        removeEgressSecurityPolicyAndApplications(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT, guestVlan, cidrs, defaultEgressPolicy);
                        if (activeRulesForGuestNw.size() > 0) {
                            //add default egress security policy
                            addEgressSecurityPolicyAndApplications(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT, guestVlan, applications, cidrs, defaultEgressPolicy);
                        }
                    }
                }
                commitConfiguration();
            } else {
                for (FirewallRuleTO rule : rules) {
                    int startPort = NetUtils.PORT_RANGE_MIN, endPort = NetUtils.PORT_RANGE_MAX;
                    if (rule.getSrcPortRange() != null) {
                        startPort = rule.getSrcPortRange()[0];
                        endPort = rule.getSrcPortRange()[1];
                    }

                    FirewallFilterTerm term =
                        new FirewallFilterTerm(genIpIdentifier(rule.getSrcIp()) + "-" + String.valueOf(rule.getId()), rule.getSourceCidrList(), rule.getSrcIp(),
                            rule.getProtocol(), startPort, endPort, rule.getIcmpType(), rule.getIcmpCode(), genIpIdentifier(rule.getSrcIp()) +
                                _usageFilterIPInput.getCounterIdentifier());
                    if (!rule.revoked()) {
                        manageProxyArp(SrxCommand.ADD, getVlanTag(rule.getSrcVlanTag()), rule.getSrcIp());
                        manageFirewallFilter(SrxCommand.ADD, term, _publicZoneInputFilterName);
                    } else {
                        manageFirewallFilter(SrxCommand.DELETE, term, _publicZoneInputFilterName);
                        manageProxyArp(SrxCommand.DELETE, getVlanTag(rule.getSrcVlanTag()), rule.getSrcIp());
                    }
                }
                commitConfiguration();
            }

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);
            closeConfiguration();

            if (numRetries > 0 && refreshSrxConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetFirewallRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    /*
     * Static NAT
     */

    private synchronized Answer execute(SetStaticNatRulesCommand cmd) {
        refreshSrxConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetStaticNatRulesCommand cmd, int numRetries) {
        StaticNatRuleTO[] allRules = cmd.getRules();
        Map<String, ArrayList<FirewallRuleTO>> activeRules = getActiveRules(allRules);
        Map<String, String> vlanTagMap = getVlanTagMap(allRules);

        try {
            openConfiguration();

            Set<String> ipPairs = activeRules.keySet();
            for (String ipPair : ipPairs) {
                String[] ipPairComponents = ipPair.split("-");
                String publicIp = ipPairComponents[0];
                String privateIp = ipPairComponents[1];

                List<FirewallRuleTO> activeRulesForIpPair = activeRules.get(ipPair);
                Long publicVlanTag = getVlanTag(vlanTagMap.get(publicIp));

                // Delete the existing static NAT rule for this IP pair
                removeStaticNatRule(publicVlanTag, publicIp, privateIp);

                if (activeRulesForIpPair.size() > 0) {
                    // If there are active FirewallRules for this IP pair, add the static NAT rule and open the specified port ranges
                    addStaticNatRule(publicVlanTag, publicIp, privateIp, activeRulesForIpPair);
                }
            }

            commitConfiguration();
            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);
            closeConfiguration();

            if (numRetries > 0 && refreshSrxConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetPortForwardingRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    private void addStaticNatRule(Long publicVlanTag, String publicIp, String privateIp, List<FirewallRuleTO> rules) throws ExecutionException {
        manageStaticNatRule(SrxCommand.ADD, publicIp, privateIp);
        manageAddressBookEntry(SrxCommand.ADD, _privateZone, privateIp, null);
        manageProxyArp(SrxCommand.ADD, publicVlanTag, publicIp);

        // Add a new security policy with the current set of applications
        addSecurityPolicyAndApplications(SecurityPolicyType.STATIC_NAT, privateIp, extractApplications(rules));

        s_logger.debug("Added static NAT rule for public IP " + publicIp + ", and private IP " + privateIp);
    }

    private void removeStaticNatRule(Long publicVlanTag, String publicIp, String privateIp) throws ExecutionException {
        manageStaticNatRule(SrxCommand.DELETE, publicIp, privateIp);

        // Remove any existing security policy and clean up applications
        removeSecurityPolicyAndApplications(SecurityPolicyType.STATIC_NAT, privateIp);

        manageAddressBookEntry(SrxCommand.DELETE, _privateZone, privateIp, null);
        manageProxyArp(SrxCommand.DELETE, publicVlanTag, publicIp);

        s_logger.debug("Removed static NAT rule for public IP " + publicIp + ", and private IP " + privateIp);
    }

    private void removeStaticNatRules(Long privateVlanTag, Map<String, Long> publicVlanTags, List<String[]> staticNatRules) throws ExecutionException {
        for (String[] staticNatRuleToRemove : staticNatRules) {
            String staticNatRulePublicIp = staticNatRuleToRemove[0];
            String staticNatRulePrivateIp = staticNatRuleToRemove[1];

            Long publicVlanTag = null;
            if (publicVlanTags.containsKey(staticNatRulePublicIp)) {
                publicVlanTag = publicVlanTags.get(staticNatRulePublicIp);
            }

            if (privateVlanTag != null) {
                s_logger.warn("Found a static NAT rule (" + staticNatRulePublicIp + " <-> " + staticNatRulePrivateIp + ") for guest VLAN with tag " + privateVlanTag +
                    " that is active when the guest network is being removed. Removing rule...");
            }

            removeStaticNatRule(publicVlanTag, staticNatRulePublicIp, staticNatRulePrivateIp);
        }
    }

    /*
     * VPN
     */

    private synchronized Answer execute(RemoteAccessVpnCfgCommand cmd) {
        refreshSrxConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(RemoteAccessVpnCfgCommand cmd, int numRetries) {
        long accountId = Long.parseLong(cmd.getAccessDetail(NetworkElementCommand.ACCOUNT_ID));
        String guestNetworkCidr = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR);
        String preSharedKey = cmd.getPresharedKey();
        String[] ipRange = cmd.getIpRange().split("-");

        try {
            openConfiguration();

            // Delete existing VPN objects for this account
            deleteVpnObjectsForAccount(accountId);

            if (cmd.isCreate()) {
                // Add IKE policy
                manageIkePolicy(SrxCommand.ADD, null, accountId, preSharedKey);

                // Add address pool
                manageAddressPool(SrxCommand.ADD, null, accountId, guestNetworkCidr, ipRange[0], ipRange[1], _primaryDnsAddress);
            }

            commitConfiguration();

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);
            closeConfiguration();

            if (numRetries > 0 && refreshSrxConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying RemoteAccessVpnCfgCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }

    }

    private void deleteVpnObjectsForAccount(long accountId) throws ExecutionException {
        // Delete all IKE policies
        for (String ikePolicyName : getVpnObjectNames(SrxXml.IKE_POLICY_GETALL, accountId)) {
            manageIkePolicy(SrxCommand.DELETE, ikePolicyName, null, null);
        }

        // Delete all address pools
        for (String addressPoolName : getVpnObjectNames(SrxXml.ADDRESS_POOL_GETALL, accountId)) {
            manageAddressPool(SrxCommand.DELETE, addressPoolName, null, null, null, null, null);
        }

        // Delete all IKE gateways
        for (String ikeGatewayName : getVpnObjectNames(SrxXml.IKE_GATEWAY_GETALL, accountId)) {
            manageIkeGateway(SrxCommand.DELETE, ikeGatewayName, null, null, null, null);
        }

        // Delete all IPsec VPNs
        for (String ipsecVpnName : getVpnObjectNames(SrxXml.IPSEC_VPN_GETALL, accountId)) {
            manageIpsecVpn(SrxCommand.DELETE, ipsecVpnName, null, null, null, null);
        }

        // Delete all dynamic VPN clients
        for (String dynamicVpnClientName : getVpnObjectNames(SrxXml.DYNAMIC_VPN_CLIENT_GETALL, accountId)) {
            manageDynamicVpnClient(SrxCommand.DELETE, dynamicVpnClientName, null, null, null, null);
        }

        // Delete all access profiles
        for (String accessProfileName : getVpnObjectNames(SrxXml.ACCESS_PROFILE_GETALL, accountId)) {
            manageAccessProfile(SrxCommand.DELETE, accessProfileName, null, null, null, null);
        }

        // Delete all security policies
        for (String securityPolicyName : getVpnObjectNames(SrxXml.SECURITY_POLICY_GETALL, accountId)) {
            manageSecurityPolicy(SecurityPolicyType.VPN, SrxCommand.DELETE, accountId, null, null, null, null, securityPolicyName, false);
        }

        // Delete all address book entries
        for (String addressBookEntryName : getVpnObjectNames(SrxXml.ADDRESS_BOOK_ENTRY_GETALL, accountId)) {
            manageAddressBookEntry(SrxCommand.DELETE, _privateZone, null, addressBookEntryName);
        }

    }

    public List<String> getVpnObjectNames(SrxXml xmlObj, long accountId) throws ExecutionException {
        List<String> vpnObjectNames = new ArrayList<String>();

        String xmlRequest = xmlObj.getXml();
        if (xmlObj.equals(SrxXml.SECURITY_POLICY_GETALL)) {
            xmlRequest = replaceXmlValue(xmlRequest, "from-zone", _publicZone);
            xmlRequest = replaceXmlValue(xmlRequest, "to-zone", _privateZone);
        } else if (xmlObj.equals(SrxXml.ADDRESS_BOOK_ENTRY_GETALL)) {
            xmlRequest = replaceXmlValue(xmlRequest, "zone", _privateZone);
        }

        String xmlResponse = sendRequest(xmlRequest);
        Document doc = getDocument(xmlResponse);
        NodeList vpnObjectNameNodes = doc.getElementsByTagName("name");
        for (int i = 0; i < vpnObjectNameNodes.getLength(); i++) {
            NodeList vpnObjectNameEntries = vpnObjectNameNodes.item(i).getChildNodes();
            for (int j = 0; j < vpnObjectNameEntries.getLength(); j++) {
                String vpnObjectName = vpnObjectNameEntries.item(j).getNodeValue();
                if (vpnObjectName.startsWith(genObjectName(_vpnObjectPrefix, String.valueOf(accountId)))) {
                    vpnObjectNames.add(vpnObjectName);
                }
            }
        }

        return vpnObjectNames;
    }

    private synchronized Answer execute(VpnUsersCfgCommand cmd) {
        refreshSrxConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(VpnUsersCfgCommand cmd, int numRetries) {
        long accountId = Long.parseLong(cmd.getAccessDetail(NetworkElementCommand.ACCOUNT_ID));
        String guestNetworkCidr = cmd.getAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR);
        String ikePolicyName = genIkePolicyName(accountId);
        UsernamePassword[] users = cmd.getUserpwds();

        try {
            openConfiguration();

            for (UsernamePassword user : users) {
                SrxCommand srxCmd = user.isAdd() ? SrxCommand.ADD : SrxCommand.DELETE;

                String ipsecVpnName = genIpsecVpnName(accountId, user.getUsername());

                // IKE gateway
                manageIkeGateway(srxCmd, null, accountId, ikePolicyName, _ikeGatewayHostname, user.getUsername());

                // IPSec VPN
                manageIpsecVpn(srxCmd, null, accountId, guestNetworkCidr, user.getUsername(), _ipsecPolicyName);

                // Dynamic VPN client
                manageDynamicVpnClient(srxCmd, null, accountId, guestNetworkCidr, ipsecVpnName, user.getUsername());

                // Access profile
                manageAccessProfile(srxCmd, null, accountId, user.getUsername(), user.getPassword(), genAddressPoolName(accountId));

                // Address book entry
                manageAddressBookEntry(srxCmd, _privateZone, guestNetworkCidr, ipsecVpnName);

                // Security policy
                manageSecurityPolicy(SecurityPolicyType.VPN, srxCmd, null, null, guestNetworkCidr, null, null, ipsecVpnName, false);
            }

            commitConfiguration();

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);
            closeConfiguration();

            if (numRetries > 0 && refreshSrxConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying RemoteAccessVpnCfgCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }

    }

    /*
     * Destination NAT
     */

    private synchronized Answer execute(SetPortForwardingRulesCommand cmd) {
        refreshSrxConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetPortForwardingRulesCommand cmd, int numRetries) {
        PortForwardingRuleTO[] allRules = cmd.getRules();
        Map<String, ArrayList<FirewallRuleTO>> activeRules = getActiveRules(allRules);

        try {
            openConfiguration();

            Set<String> ipPairs = activeRules.keySet();
            for (String ipPair : ipPairs) {
                String[] ipPairComponents = ipPair.split("-");
                String publicIp = ipPairComponents[0];
                String privateIp = ipPairComponents[1];

                List<FirewallRuleTO> activeRulesForIpPair = activeRules.get(ipPair);

                // Get a list of all destination NAT rules for the public/private IP address pair
                List<String[]> destNatRules = getDestNatRules(RuleMatchCondition.PUBLIC_PRIVATE_IPS, publicIp, privateIp, null, null);
                Map<String, Long> publicVlanTags = getPublicVlanTagsForNatRules(destNatRules);

                // Delete all of these rules, along with the destination NAT pools and security policies they use
                removeDestinationNatRules(null, publicVlanTags, destNatRules);

                // If there are active rules for the public/private IP address pair, add them back
                for (FirewallRuleTO rule : activeRulesForIpPair) {
                    Long publicVlanTag = getVlanTag(rule.getSrcVlanTag());
                    PortForwardingRuleTO portForwardingRule = (PortForwardingRuleTO)rule;
                    addDestinationNatRule(getProtocol(rule.getProtocol()), publicVlanTag, portForwardingRule.getSrcIp(), portForwardingRule.getDstIp(),
                        portForwardingRule.getSrcPortRange()[0], portForwardingRule.getSrcPortRange()[1], portForwardingRule.getDstPortRange()[0],
                        portForwardingRule.getDstPortRange()[1]);
                }
            }

            commitConfiguration();
            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);
            closeConfiguration();

            if (numRetries > 0 && refreshSrxConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetPortForwardingRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    private void addDestinationNatRule(Protocol protocol, Long publicVlanTag, String publicIp, String privateIp, int srcPortStart, int srcPortEnd, int destPortStart,
        int destPortEnd) throws ExecutionException {

        int offset = 0;
        for (int srcPort = srcPortStart; srcPort <= srcPortEnd; srcPort++) {
            int destPort = destPortStart + offset;
            manageDestinationNatPool(SrxCommand.ADD, privateIp, destPort);
            manageDestinationNatRule(SrxCommand.ADD, publicIp, privateIp, srcPort, destPort);
            offset += 1;
        }

        manageAddressBookEntry(SrxCommand.ADD, _privateZone, privateIp, null);

        List<Object[]> applications = new ArrayList<Object[]>();
        applications.add(new Object[] {protocol, destPortStart, destPortEnd});
        addSecurityPolicyAndApplications(SecurityPolicyType.DESTINATION_NAT, privateIp, applications);
        manageProxyArp(SrxCommand.ADD, publicVlanTag, publicIp);

        String srcPortRange = srcPortStart + "-" + srcPortEnd;
        String destPortRange = destPortStart + "-" + destPortEnd;
        s_logger.debug("Added destination NAT rule for protocol " + protocol + ", public IP " + publicIp + ", private IP " + privateIp + ", source port range " +
            srcPortRange + ", and dest port range " + destPortRange);
    }

    private void removeDestinationNatRule(Long publicVlanTag, String publicIp, String privateIp, int srcPort, int destPort) throws ExecutionException {
        manageDestinationNatRule(SrxCommand.DELETE, publicIp, privateIp, srcPort, destPort);
        manageDestinationNatPool(SrxCommand.DELETE, privateIp, destPort);
        manageProxyArp(SrxCommand.DELETE, publicVlanTag, publicIp);

        removeSecurityPolicyAndApplications(SecurityPolicyType.DESTINATION_NAT, privateIp);

        manageAddressBookEntry(SrxCommand.DELETE, _privateZone, privateIp, null);

        s_logger.debug("Removed destination NAT rule for public IP " + publicIp + ", private IP " + privateIp + ", source port " + srcPort + ", and dest port " +
            destPort);
    }

    private void removeDestinationNatRules(Long privateVlanTag, Map<String, Long> publicVlanTags, List<String[]> destNatRules) throws ExecutionException {
        for (String[] destNatRule : destNatRules) {
            String publicIp = destNatRule[0];
            String privateIp = destNatRule[1];
            int srcPort = Integer.parseInt(destNatRule[2]);
            int destPort = Integer.parseInt(destNatRule[3]);

            Long publicVlanTag = null;
            if (publicVlanTags.containsKey(publicIp)) {
                publicVlanTag = publicVlanTags.get(publicIp);
            }

            if (privateVlanTag != null) {
                s_logger.warn("Found a destination NAT rule (public IP: " + publicIp + ", private IP: " + privateIp + ", public port: " + srcPort + ", private port: " +
                    destPort + ") for guest VLAN with tag " + privateVlanTag + " that is active when the guest network is being removed. Removing rule...");
            }

            removeDestinationNatRule(publicVlanTag, publicIp, privateIp, srcPort, destPort);
        }
    }

    /*
     * General NAT utils
     */

    private List<String[]> getAllStaticAndDestNatRules() throws ExecutionException {
        List<String[]> staticAndDestNatRules = new ArrayList<String[]>();
        staticAndDestNatRules.addAll(getStaticNatRules(RuleMatchCondition.ALL, null, null));
        staticAndDestNatRules.addAll(getDestNatRules(RuleMatchCondition.ALL, null, null, null, null));
        return staticAndDestNatRules;
    }

    private void removeStaticAndDestNatRulesInPrivateVlan(long privateVlanTag, String privateGateway, long privateCidrSize) throws ExecutionException {
        List<String[]> staticNatRulesToRemove = getStaticNatRules(RuleMatchCondition.PRIVATE_SUBNET, privateGateway, privateCidrSize);
        List<String[]> destNatRulesToRemove = getDestNatRules(RuleMatchCondition.PRIVATE_SUBNET, null, null, privateGateway, privateCidrSize);

        List<String> publicIps = new ArrayList<String>();
        addPublicIpsToList(staticNatRulesToRemove, publicIps);
        addPublicIpsToList(destNatRulesToRemove, publicIps);

        Map<String, Long> publicVlanTags = getPublicVlanTagsForPublicIps(publicIps);

        removeStaticNatRules(privateVlanTag, publicVlanTags, staticNatRulesToRemove);
        removeDestinationNatRules(privateVlanTag, publicVlanTags, destNatRulesToRemove);
    }

    private Map<String, ArrayList<FirewallRuleTO>> getActiveRules(FirewallRuleTO[] allRules) {
        Map<String, ArrayList<FirewallRuleTO>> activeRules = new HashMap<String, ArrayList<FirewallRuleTO>>();

        for (FirewallRuleTO rule : allRules) {
            String ipPair;

            if (rule.getPurpose().equals(Purpose.StaticNat)) {
                StaticNatRuleTO staticNatRule = (StaticNatRuleTO)rule;
                ipPair = staticNatRule.getSrcIp() + "-" + staticNatRule.getDstIp();
            } else if (rule.getPurpose().equals(Purpose.PortForwarding)) {
                PortForwardingRuleTO portForwardingRule = (PortForwardingRuleTO)rule;
                ipPair = portForwardingRule.getSrcIp() + "-" + portForwardingRule.getDstIp();
            } else {
                continue;
            }

            ArrayList<FirewallRuleTO> activeRulesForIpPair = activeRules.get(ipPair);

            if (activeRulesForIpPair == null) {
                activeRulesForIpPair = new ArrayList<FirewallRuleTO>();
            }

            if (!rule.revoked() || rule.isAlreadyAdded()) {
                activeRulesForIpPair.add(rule);
            }

            activeRules.put(ipPair, activeRulesForIpPair);
        }

        return activeRules;
    }

    private Map<String, String> getVlanTagMap(FirewallRuleTO[] allRules) {
        Map<String, String> vlanTagMap = new HashMap<String, String>();

        for (FirewallRuleTO rule : allRules) {
            vlanTagMap.put(rule.getSrcIp(), rule.getSrcVlanTag());
        }

        return vlanTagMap;
    }

    /*
     * VPN
     */

    private String genIkePolicyName(long accountId) {
        return genObjectName(_vpnObjectPrefix, String.valueOf(accountId));
    }

    private boolean manageIkePolicy(SrxCommand command, String ikePolicyName, Long accountId, String preSharedKey) throws ExecutionException {
        if (ikePolicyName == null) {
            ikePolicyName = genIkePolicyName(accountId);
        }

        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.IKE_GATEWAY_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "policy-name", ikePolicyName);
                return sendRequestAndCheckResponse(command, xml, "name", ikePolicyName);

            case ADD:
                if (manageIkePolicy(SrxCommand.CHECK_IF_EXISTS, ikePolicyName, accountId, preSharedKey)) {
                    return true;
                }

                xml = SrxXml.IKE_POLICY_ADD.getXml();
                xml = replaceXmlValue(xml, "policy-name", ikePolicyName);
                xml = replaceXmlValue(xml, "proposal-name", _ikeProposalName);
                xml = replaceXmlValue(xml, "pre-shared-key", preSharedKey);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add IKE policy: " + ikePolicyName);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageIkePolicy(SrxCommand.CHECK_IF_EXISTS, ikePolicyName, accountId, preSharedKey)) {
                    return true;
                }

                xml = SrxXml.IKE_GATEWAY_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "policy-name", ikePolicyName);

                if (!sendRequestAndCheckResponse(command, xml, "name", ikePolicyName)) {
                    throw new ExecutionException("Failed to delete IKE policy: " + ikePolicyName);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }

    }

    private String genIkeGatewayName(long accountId, String username) {
        return genObjectName(_vpnObjectPrefix, String.valueOf(accountId), username);
    }

    private boolean manageIkeGateway(SrxCommand command, String ikeGatewayName, Long accountId, String ikePolicyName, String ikeGatewayHostname, String username)
        throws ExecutionException {
        if (ikeGatewayName == null) {
            ikeGatewayName = genIkeGatewayName(accountId, username);
        }

        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.IKE_GATEWAY_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "gateway-name", ikeGatewayName);
                return sendRequestAndCheckResponse(command, xml, "name", ikeGatewayName);

            case ADD:
                if (manageIkeGateway(SrxCommand.CHECK_IF_EXISTS, ikeGatewayName, accountId, ikePolicyName, ikeGatewayHostname, username)) {
                    return true;
                }

                xml = SrxXml.IKE_GATEWAY_ADD.getXml();
                xml = replaceXmlValue(xml, "gateway-name", ikeGatewayName);
                xml = replaceXmlValue(xml, "ike-policy-name", ikePolicyName);
                xml = replaceXmlValue(xml, "ike-gateway-hostname", ikeGatewayHostname);
                xml = replaceXmlValue(xml, "public-interface-name", _publicInterface);
                xml = replaceXmlValue(xml, "access-profile-name", genAccessProfileName(accountId, username));

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add IKE gateway: " + ikeGatewayName);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageIkeGateway(SrxCommand.CHECK_IF_EXISTS, ikeGatewayName, accountId, ikePolicyName, ikeGatewayHostname, username)) {
                    return true;
                }

                xml = SrxXml.IKE_GATEWAY_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "gateway-name", ikeGatewayName);

                if (!sendRequestAndCheckResponse(command, xml, "name", ikeGatewayName)) {
                    throw new ExecutionException("Failed to delete IKE gateway: " + ikeGatewayName);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    private String genIpsecVpnName(long accountId, String username) {
        return genObjectName(_vpnObjectPrefix, String.valueOf(accountId), username);
    }

    private boolean manageIpsecVpn(SrxCommand command, String ipsecVpnName, Long accountId, String guestNetworkCidr, String username, String ipsecPolicyName)
        throws ExecutionException {
        if (ipsecVpnName == null) {
            ipsecVpnName = genIpsecVpnName(accountId, username);
        }

        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.IPSEC_VPN_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "ipsec-vpn-name", ipsecVpnName);
                return sendRequestAndCheckResponse(command, xml, "name", ipsecVpnName);

            case ADD:
                if (manageIpsecVpn(SrxCommand.CHECK_IF_EXISTS, ipsecVpnName, accountId, guestNetworkCidr, username, ipsecPolicyName)) {
                    return true;
                }

                xml = SrxXml.IPSEC_VPN_ADD.getXml();
                xml = replaceXmlValue(xml, "ipsec-vpn-name", ipsecVpnName);
                xml = replaceXmlValue(xml, "ike-gateway", genIkeGatewayName(accountId, username));
                xml = replaceXmlValue(xml, "ipsec-policy-name", ipsecPolicyName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add IPSec VPN: " + ipsecVpnName);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageIpsecVpn(SrxCommand.CHECK_IF_EXISTS, ipsecVpnName, accountId, guestNetworkCidr, username, ipsecPolicyName)) {
                    return true;
                }

                xml = SrxXml.IPSEC_VPN_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "ipsec-vpn-name", ipsecVpnName);

                if (!sendRequestAndCheckResponse(command, xml, "name", ipsecVpnName)) {
                    throw new ExecutionException("Failed to delete IPSec VPN: " + ipsecVpnName);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    private String genDynamicVpnClientName(long accountId, String username) {
        return genObjectName(_vpnObjectPrefix, String.valueOf(accountId), username);
    }

    private boolean manageDynamicVpnClient(SrxCommand command, String clientName, Long accountId, String guestNetworkCidr, String ipsecVpnName, String username)
        throws ExecutionException {
        if (clientName == null) {
            clientName = genDynamicVpnClientName(accountId, username);
        }

        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.DYNAMIC_VPN_CLIENT_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "client-name", clientName);
                return sendRequestAndCheckResponse(command, xml, "name", clientName);

            case ADD:
                if (manageDynamicVpnClient(SrxCommand.CHECK_IF_EXISTS, clientName, accountId, guestNetworkCidr, ipsecVpnName, username)) {
                    return true;
                }

                xml = SrxXml.DYNAMIC_VPN_CLIENT_ADD.getXml();
                xml = replaceXmlValue(xml, "client-name", clientName);
                xml = replaceXmlValue(xml, "guest-network-cidr", guestNetworkCidr);
                xml = replaceXmlValue(xml, "ipsec-vpn-name", ipsecVpnName);
                xml = replaceXmlValue(xml, "username", username);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add dynamic VPN client: " + clientName);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageDynamicVpnClient(SrxCommand.CHECK_IF_EXISTS, clientName, accountId, guestNetworkCidr, ipsecVpnName, username)) {
                    return true;
                }

                xml = SrxXml.DYNAMIC_VPN_CLIENT_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "client-name", clientName);

                if (!sendRequestAndCheckResponse(command, xml, "name", clientName)) {
                    throw new ExecutionException("Failed to delete dynamic VPN client: " + clientName);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    private String genAddressPoolName(long accountId) {
        return genObjectName(_vpnObjectPrefix, String.valueOf(accountId));
    }

    private boolean manageAddressPool(SrxCommand command, String addressPoolName, Long accountId, String guestNetworkCidr, String lowAddress, String highAddress,
        String primaryDnsAddress) throws ExecutionException {
        if (addressPoolName == null) {
            addressPoolName = genAddressPoolName(accountId);
        }

        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.ADDRESS_POOL_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "address-pool-name", addressPoolName);
                return sendRequestAndCheckResponse(command, xml, "name", addressPoolName);

            case ADD:
                if (manageAddressPool(SrxCommand.CHECK_IF_EXISTS, addressPoolName, accountId, guestNetworkCidr, lowAddress, highAddress, primaryDnsAddress)) {
                    return true;
                }

                xml = SrxXml.ADDRESS_POOL_ADD.getXml();
                xml = replaceXmlValue(xml, "address-pool-name", addressPoolName);
                xml = replaceXmlValue(xml, "guest-network-cidr", guestNetworkCidr);
                xml = replaceXmlValue(xml, "address-range-name", "r-" + addressPoolName);
                xml = replaceXmlValue(xml, "low-address", lowAddress);
                xml = replaceXmlValue(xml, "high-address", highAddress);
                xml = replaceXmlValue(xml, "primary-dns-address", primaryDnsAddress);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add address pool: " + addressPoolName);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageAddressPool(SrxCommand.CHECK_IF_EXISTS, addressPoolName, accountId, guestNetworkCidr, lowAddress, highAddress, primaryDnsAddress)) {
                    return true;
                }

                xml = SrxXml.ADDRESS_POOL_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "address-pool-name", addressPoolName);

                if (!sendRequestAndCheckResponse(command, xml, "name", addressPoolName)) {
                    throw new ExecutionException("Failed to delete address pool: " + addressPoolName);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    private String genAccessProfileName(long accountId, String username) {
        return genObjectName(_vpnObjectPrefix, String.valueOf(accountId), username);
    }

    private boolean manageAccessProfile(SrxCommand command, String accessProfileName, Long accountId, String username, String password, String addressPoolName)
        throws ExecutionException {
        if (accessProfileName == null) {
            accessProfileName = genAccessProfileName(accountId, username);
        }

        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.ACCESS_PROFILE_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "access-profile-name", accessProfileName);
                return sendRequestAndCheckResponse(command, xml, "name", username);

            case ADD:
                if (manageAccessProfile(SrxCommand.CHECK_IF_EXISTS, accessProfileName, accountId, username, password, addressPoolName)) {
                    return true;
                }

                xml = SrxXml.ACCESS_PROFILE_ADD.getXml();
                xml = replaceXmlValue(xml, "access-profile-name", accessProfileName);
                xml = replaceXmlValue(xml, "username", username);
                xml = replaceXmlValue(xml, "password", password);
                xml = replaceXmlValue(xml, "address-pool-name", addressPoolName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add access profile: " + accessProfileName);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageAccessProfile(SrxCommand.CHECK_IF_EXISTS, accessProfileName, accountId, username, password, addressPoolName)) {
                    return true;
                }

                xml = SrxXml.ACCESS_PROFILE_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "access-profile-name", accessProfileName);

                if (!sendRequestAndCheckResponse(command, xml, "name", username)) {
                    throw new ExecutionException("Failed to delete access profile: " + accessProfileName);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Private interfaces
     */

    private boolean managePrivateInterface(SrxCommand command, boolean addFilters, long vlanTag, String privateInterfaceIp) throws ExecutionException {
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.PRIVATE_INTERFACE_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "private-interface-name", _privateInterface);
                xml = replaceXmlValue(xml, "vlan-id", String.valueOf(vlanTag));
                return sendRequestAndCheckResponse(command, xml, "name", String.valueOf(vlanTag));

            case ADD:
                if (managePrivateInterface(SrxCommand.CHECK_IF_EXISTS, false, vlanTag, privateInterfaceIp)) {
                    return true;
                }

                xml = addFilters ? SrxXml.PRIVATE_INTERFACE_WITH_FILTERS_ADD.getXml() : SrxXml.PRIVATE_INTERFACE_ADD.getXml();
                xml = replaceXmlValue(xml, "private-interface-name", _privateInterface);
                xml = replaceXmlValue(xml, "vlan-id", String.valueOf(vlanTag));
                xml = replaceXmlValue(xml, "private-interface-ip", privateInterfaceIp);

                if (addFilters) {
                    xml = replaceXmlValue(xml, "input-filter-name", _usageFilterVlanInput.getName() + "-" + vlanTag);
                    xml = replaceXmlValue(xml, "output-filter-name", _usageFilterVlanOutput.getName() + "-" + vlanTag);
                }

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add private interface for guest VLAN tag " + vlanTag);
                } else {
                    return true;
                }

            case DELETE:
                if (!managePrivateInterface(SrxCommand.CHECK_IF_EXISTS, false, vlanTag, privateInterfaceIp)) {
                    return true;
                }

                xml = SrxXml.PRIVATE_INTERFACE_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "private-interface-name", _privateInterface);
                xml = replaceXmlValue(xml, "vlan-id", String.valueOf(vlanTag));

                if (!sendRequestAndCheckResponse(command, xml, "name", String.valueOf(vlanTag))) {
                    throw new ExecutionException("Failed to delete private interface for guest VLAN tag " + vlanTag);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }

    }

    private Long getVlanTagFromInterfaceName(String interfaceName) throws ExecutionException {
        Long vlanTag = null;

        if (interfaceName.contains(".")) {
            try {
                String unitNum = interfaceName.split("\\.")[1];
                if (!unitNum.equals("0")) {
                    vlanTag = Long.parseLong(unitNum);
                }
            } catch (Exception e) {
                s_logger.error(e);
                throw new ExecutionException("Unable to parse VLAN tag from interface name: " + interfaceName);
            }
        }

        return vlanTag;
    }

    /*
     * Proxy ARP
     */

    private boolean manageProxyArp(SrxCommand command, Long publicVlanTag, String publicIp) throws ExecutionException {
        String publicInterface = genPublicInterface(publicVlanTag);
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.PROXY_ARP_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "public-interface-name", publicInterface);
                xml = replaceXmlValue(xml, "public-ip-address", publicIp);
                return sendRequestAndCheckResponse(command, xml, "name", publicIp + "/32");

            case CHECK_IF_IN_USE:
                // Check if any NAT rules are using this proxy ARP entry
                String poolName = genSourceNatPoolName(publicIp);

                String allStaticNatRules = sendRequest(SrxXml.STATIC_NAT_RULE_GETALL.getXml());
                String allDestNatRules = sendRequest(replaceXmlValue(SrxXml.DEST_NAT_RULE_GETALL.getXml(), "rule-set", _publicZone));
                String allSrcNatRules = sendRequest(SrxXml.SRC_NAT_RULE_GETALL.getXml());

                return (allStaticNatRules.contains(publicIp) || allDestNatRules.contains(publicIp) || allSrcNatRules.contains(poolName));

            case ADD:
                if (manageProxyArp(SrxCommand.CHECK_IF_EXISTS, publicVlanTag, publicIp)) {
                    return true;
                }

                xml = SrxXml.PROXY_ARP_ADD.getXml();
                xml = replaceXmlValue(xml, "public-interface-name", publicInterface);
                xml = replaceXmlValue(xml, "public-ip-address", publicIp);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add proxy ARP entry for public IP " + publicIp);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageProxyArp(SrxCommand.CHECK_IF_EXISTS, publicVlanTag, publicIp)) {
                    return true;
                }

                if (manageProxyArp(SrxCommand.CHECK_IF_IN_USE, publicVlanTag, publicIp)) {
                    return true;
                }

                xml = SrxXml.PROXY_ARP_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "public-interface-name", publicInterface);
                xml = replaceXmlValue(xml, "public-ip-address", publicIp);

                if (!sendRequestAndCheckResponse(command, xml, "name", publicIp)) {
                    throw new ExecutionException("Failed to delete proxy ARP entry for public IP " + publicIp);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;

        }

    }

    private Map<String, Long> getPublicVlanTagsForPublicIps(List<String> publicIps) throws ExecutionException {
        Map<String, Long> publicVlanTags = new HashMap<String, Long>();

        List<String> interfaceNames = new ArrayList<String>();

        String xmlRequest = SrxXml.PROXY_ARP_GETALL.getXml();
        xmlRequest = replaceXmlValue(xmlRequest, "interface-name", "");
        String xmlResponse = sendRequest(xmlRequest);

        Document doc = getDocument(xmlResponse);
        NodeList interfaces = doc.getElementsByTagName("interface");
        for (int i = 0; i < interfaces.getLength(); i++) {
            String interfaceName = null;
            NodeList interfaceEntries = interfaces.item(i).getChildNodes();
            for (int j = 0; j < interfaceEntries.getLength(); j++) {
                Node interfaceEntry = interfaceEntries.item(j);
                if (interfaceEntry.getNodeName().equals("name")) {
                    interfaceName = interfaceEntry.getFirstChild().getNodeValue();
                    break;
                }
            }

            if (interfaceName != null) {
                interfaceNames.add(interfaceName);
            }
        }

        if (interfaceNames.size() == 1) {
            populatePublicVlanTagsMap(xmlResponse, interfaceNames.get(0), publicIps, publicVlanTags);
        } else if (interfaceNames.size() > 1) {
            for (String interfaceName : interfaceNames) {
                xmlRequest = SrxXml.PROXY_ARP_GETALL.getXml();
                xmlRequest = replaceXmlValue(xmlRequest, "interface-name", interfaceName);
                xmlResponse = sendRequest(xmlRequest);
                populatePublicVlanTagsMap(xmlResponse, interfaceName, publicIps, publicVlanTags);
            }
        }

        return publicVlanTags;
    }

    private void populatePublicVlanTagsMap(String xmlResponse, String interfaceName, List<String> publicIps, Map<String, Long> publicVlanTags) throws ExecutionException {
        Long publicVlanTag = getVlanTagFromInterfaceName(interfaceName);
        if (publicVlanTag != null) {
            for (String publicIp : publicIps) {
                if (xmlResponse.contains(publicIp)) {
                    publicVlanTags.put(publicIp, publicVlanTag);
                }
            }
        }
    }

    private Map<String, Long> getPublicVlanTagsForNatRules(List<String[]> natRules) throws ExecutionException {
        List<String> publicIps = new ArrayList<String>();
        addPublicIpsToList(natRules, publicIps);
        return getPublicVlanTagsForPublicIps(publicIps);
    }

    private void addPublicIpsToList(List<String[]> natRules, List<String> publicIps) {
        for (String[] natRule : natRules) {
            if (!publicIps.contains(natRule[0])) {
                publicIps.add(natRule[0]);
            }
        }
    }

    private String genPublicInterface(Long vlanTag) {
        String publicInterface = _publicInterface;

        if (!publicInterface.contains(".")) {
            if (vlanTag == null) {
                publicInterface += ".0";
            } else {
                publicInterface += "." + vlanTag;
            }
        }

        return publicInterface;
    }

    /*
     * Zone interfaces
     */

    private String genZoneInterfaceName(long vlanTag) {
        return _privateInterface + "." + String.valueOf(vlanTag);
    }

    private boolean manageZoneInterface(SrxCommand command, long vlanTag) throws ExecutionException {
        String zoneInterfaceName = genZoneInterfaceName(vlanTag);
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.ZONE_INTERFACE_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "private-zone-name", _privateZone);
                xml = replaceXmlValue(xml, "zone-interface-name", zoneInterfaceName);
                return sendRequestAndCheckResponse(command, xml, "name", zoneInterfaceName);

            case ADD:
                if (manageZoneInterface(SrxCommand.CHECK_IF_EXISTS, vlanTag)) {
                    return true;
                }

                xml = SrxXml.ZONE_INTERFACE_ADD.getXml();
                xml = replaceXmlValue(xml, "private-zone-name", _privateZone);
                xml = replaceXmlValue(xml, "zone-interface-name", zoneInterfaceName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add interface " + zoneInterfaceName + " to zone " + _privateZone);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageZoneInterface(SrxCommand.CHECK_IF_EXISTS, vlanTag)) {
                    return true;
                }

                xml = SrxXml.ZONE_INTERFACE_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "private-zone-name", _privateZone);
                xml = replaceXmlValue(xml, "zone-interface-name", zoneInterfaceName);

                if (!sendRequestAndCheckResponse(command, xml, "name", zoneInterfaceName)) {
                    throw new ExecutionException("Failed to delete interface " + zoneInterfaceName + " from zone " + _privateZone);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Static NAT rules
     */

    private String genStaticNatRuleName(String publicIp, String privateIp) {
        return genObjectName(genIpIdentifier(publicIp), genIpIdentifier(privateIp));
    }

    private boolean manageStaticNatRule(SrxCommand command, String publicIp, String privateIp) throws ExecutionException {
        String ruleName = genStaticNatRuleName(publicIp, privateIp);
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.STATIC_NAT_RULE_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "rule-set", _publicZone);
                xml = replaceXmlValue(xml, "from-zone", _publicZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);
                return sendRequestAndCheckResponse(command, xml, "name", ruleName);

            case ADD:
                if (manageStaticNatRule(SrxCommand.CHECK_IF_EXISTS, publicIp, privateIp)) {
                    return true;
                }

                xml = SrxXml.STATIC_NAT_RULE_ADD.getXml();
                xml = replaceXmlValue(xml, "rule-set", _publicZone);
                xml = replaceXmlValue(xml, "from-zone", _publicZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);
                xml = replaceXmlValue(xml, "original-ip", publicIp);
                xml = replaceXmlValue(xml, "translated-ip", privateIp);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add static NAT rule from public IP " + publicIp + " to private IP " + privateIp);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageStaticNatRule(SrxCommand.CHECK_IF_EXISTS, publicIp, privateIp)) {
                    return true;
                }

                xml = SrxXml.STATIC_NAT_RULE_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "rule-set", _publicZone);
                xml = replaceXmlValue(xml, "from-zone", _publicZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);

                if (!sendRequestAndCheckResponse(command, xml, "name", ruleName)) {
                    throw new ExecutionException("Failed to delete static NAT rule from public IP " + publicIp + " to private IP " + privateIp);
                } else {
                    return true;
                }

            default:
                throw new ExecutionException("Unrecognized command.");

        }
    }

    private List<String[]> getStaticNatRules(RuleMatchCondition condition, String privateGateway, Long privateCidrSize) throws ExecutionException {
        List<String[]> staticNatRules = new ArrayList<String[]>();

        String xmlRequest = SrxXml.STATIC_NAT_RULE_GETALL.getXml();
        String xmlResponse = sendRequest(xmlRequest);
        Document doc = getDocument(xmlResponse);
        NodeList rules = doc.getElementsByTagName("rule");
        for (int i = 0; i < rules.getLength(); i++) {
            NodeList ruleEntries = rules.item(i).getChildNodes();
            for (int j = 0; j < ruleEntries.getLength(); j++) {
                Node ruleEntry = ruleEntries.item(j);
                if (ruleEntry.getNodeName().equals("name")) {
                    String name = ruleEntry.getFirstChild().getNodeValue();
                    String[] nameContents = name.split("-");

                    if (nameContents.length != 8) {
                        continue;
                    }

                    String rulePublicIp = nameContents[0] + "." + nameContents[1] + "." + nameContents[2] + "." + nameContents[3];
                    String rulePrivateIp = nameContents[4] + "." + nameContents[5] + "." + nameContents[6] + "." + nameContents[7];

                    boolean addToList = false;
                    if (condition.equals(RuleMatchCondition.ALL)) {
                        addToList = true;
                    } else if (condition.equals(RuleMatchCondition.PRIVATE_SUBNET)) {
                        assert (privateGateway != null && privateCidrSize != null);
                        addToList = NetUtils.sameSubnetCIDR(rulePrivateIp, privateGateway, privateCidrSize);
                    } else {
                        s_logger.error("Invalid rule match condition.");
                        assert false;
                    }

                    if (addToList) {
                        staticNatRules.add(new String[] {rulePublicIp, rulePrivateIp});
                    }
                }
            }
        }

        return staticNatRules;
    }

    /*
     * Destination NAT pools
     */

    private String genDestinationNatPoolName(String privateIp, long destPort) {
        return genObjectName(genIpIdentifier(privateIp), String.valueOf(destPort));
    }

    private boolean manageDestinationNatPool(SrxCommand command, String privateIp, long destPort) throws ExecutionException {
        String poolName = genDestinationNatPoolName(privateIp, destPort);
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.DEST_NAT_POOL_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "pool-name", poolName);
                return sendRequestAndCheckResponse(command, xml, "name", poolName);

            case CHECK_IF_IN_USE:
                // Check if any destination nat rules refer to this pool
                xml = SrxXml.DEST_NAT_RULE_GETALL.getXml();
                xml = replaceXmlValue(xml, "rule-set", _publicZone);
                return sendRequestAndCheckResponse(command, xml, "pool-name", poolName);

            case ADD:
                if (manageDestinationNatPool(SrxCommand.CHECK_IF_EXISTS, privateIp, destPort)) {
                    return true;
                }

                xml = SrxXml.DEST_NAT_POOL_ADD.getXml();
                xml = replaceXmlValue(xml, "pool-name", poolName);
                xml = replaceXmlValue(xml, "private-address", privateIp + "/32");
                xml = replaceXmlValue(xml, "dest-port", String.valueOf(destPort));

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add destination NAT pool for private IP " + privateIp + " and private port " + destPort);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageDestinationNatPool(SrxCommand.CHECK_IF_EXISTS, privateIp, destPort)) {
                    return true;
                }

                if (manageDestinationNatPool(SrxCommand.CHECK_IF_IN_USE, privateIp, destPort)) {
                    return true;
                }

                xml = SrxXml.DEST_NAT_POOL_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "pool-name", poolName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to delete destination NAT pool for private IP " + privateIp + " and private port " + destPort);
                } else {
                    return true;
                }

            default:
                throw new ExecutionException("Unrecognized command.");
        }
    }

    /*
     * Destination NAT rules
     */

    private String genDestinationNatRuleName(String publicIp, String privateIp, long srcPort, long destPort) {
        return "destnatrule-" +
            String.valueOf(genObjectName(publicIp, privateIp, String.valueOf(srcPort), String.valueOf(destPort)).hashCode()).replaceAll("[^a-zA-Z0-9]", "");
    }

    private boolean manageDestinationNatRule(SrxCommand command, String publicIp, String privateIp, long srcPort, long destPort) throws ExecutionException {
        String ruleName = genDestinationNatRuleName(publicIp, privateIp, srcPort, destPort);
        String poolName = genDestinationNatPoolName(privateIp, destPort);
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.DEST_NAT_RULE_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "rule-set", _publicZone);
                xml = replaceXmlValue(xml, "from-zone", _publicZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);
                return sendRequestAndCheckResponse(command, xml, "name", ruleName);

            case ADD:
                if (manageDestinationNatRule(SrxCommand.CHECK_IF_EXISTS, publicIp, privateIp, srcPort, destPort)) {
                    return true;
                }

                if (!manageDestinationNatPool(SrxCommand.CHECK_IF_EXISTS, privateIp, destPort)) {
                    throw new ExecutionException("The destination NAT pool corresponding to private IP: " + privateIp + " and destination port: " + destPort +
                        " does not exist.");
                }

                xml = SrxXml.DEST_NAT_RULE_ADD.getXml();
                xml = replaceXmlValue(xml, "rule-set", _publicZone);
                xml = replaceXmlValue(xml, "from-zone", _publicZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);
                xml = replaceXmlValue(xml, "public-address", publicIp);
                xml = replaceXmlValue(xml, "src-port", String.valueOf(srcPort));
                xml = replaceXmlValue(xml, "pool-name", poolName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add destination NAT rule from public IP " + publicIp + ", public port " + srcPort + ", private IP " +
                        privateIp + ", and private port " + destPort);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageDestinationNatRule(SrxCommand.CHECK_IF_EXISTS, publicIp, privateIp, srcPort, destPort)) {
                    return true;
                }

                xml = SrxXml.DEST_NAT_RULE_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "rule-set", _publicZone);
                xml = replaceXmlValue(xml, "from-zone", _publicZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to delete destination NAT rule from public IP " + publicIp + ", public port " + srcPort + ", private IP " +
                        privateIp + ", and private port " + destPort);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }

    }

    private List<String[]> getDestNatRules(RuleMatchCondition condition, String publicIp, String privateIp, String privateGateway, Long privateCidrSize)
        throws ExecutionException {
        List<String[]> destNatRules = new ArrayList<String[]>();

        String xmlRequest = SrxXml.DEST_NAT_RULE_GETALL.getXml();
        xmlRequest = replaceXmlValue(xmlRequest, "rule-set", _publicZone);
        String xmlResponse = sendRequest(xmlRequest);
        Document doc = getDocument(xmlResponse);
        NodeList rules = doc.getElementsByTagName("rule");
        for (int ruleIndex = 0; ruleIndex < rules.getLength(); ruleIndex++) {
            String rulePublicIp = null;
            String rulePrivateIp = null;
            String ruleSrcPort = null;
            String ruleDestPort = null;
            NodeList ruleEntries = rules.item(ruleIndex).getChildNodes();
            for (int ruleEntryIndex = 0; ruleEntryIndex < ruleEntries.getLength(); ruleEntryIndex++) {
                Node ruleEntry = ruleEntries.item(ruleEntryIndex);
                if (ruleEntry.getNodeName().equals("dest-nat-rule-match")) {
                    NodeList ruleMatchEntries = ruleEntry.getChildNodes();
                    for (int ruleMatchIndex = 0; ruleMatchIndex < ruleMatchEntries.getLength(); ruleMatchIndex++) {
                        Node ruleMatchEntry = ruleMatchEntries.item(ruleMatchIndex);
                        if (ruleMatchEntry.getNodeName().equals("destination-address")) {
                            NodeList destAddressEntries = ruleMatchEntry.getChildNodes();
                            for (int destAddressIndex = 0; destAddressIndex < destAddressEntries.getLength(); destAddressIndex++) {
                                Node destAddressEntry = destAddressEntries.item(destAddressIndex);
                                if (destAddressEntry.getNodeName().equals("dst-addr")) {
                                    rulePublicIp = destAddressEntry.getFirstChild().getNodeValue().split("/")[0];
                                }
                            }
                        } else if (ruleMatchEntry.getNodeName().equals("destination-port")) {
                            NodeList destPortEntries = ruleMatchEntry.getChildNodes();
                            for (int destPortIndex = 0; destPortIndex < destPortEntries.getLength(); destPortIndex++) {
                                Node destPortEntry = destPortEntries.item(destPortIndex);
                                if (destPortEntry.getNodeName().equals("dst-port")) {
                                    ruleSrcPort = destPortEntry.getFirstChild().getNodeValue();
                                }
                            }
                        }
                    }
                } else if (ruleEntry.getNodeName().equals("then")) {
                    NodeList ruleThenEntries = ruleEntry.getChildNodes();
                    for (int ruleThenIndex = 0; ruleThenIndex < ruleThenEntries.getLength(); ruleThenIndex++) {
                        Node ruleThenEntry = ruleThenEntries.item(ruleThenIndex);
                        if (ruleThenEntry.getNodeName().equals("destination-nat")) {
                            NodeList destNatEntries = ruleThenEntry.getChildNodes();
                            for (int destNatIndex = 0; destNatIndex < destNatEntries.getLength(); destNatIndex++) {
                                Node destNatEntry = destNatEntries.item(destNatIndex);
                                if (destNatEntry.getNodeName().equals("pool")) {
                                    NodeList poolEntries = destNatEntry.getChildNodes();
                                    for (int poolIndex = 0; poolIndex < poolEntries.getLength(); poolIndex++) {
                                        Node poolEntry = poolEntries.item(poolIndex);
                                        if (poolEntry.getNodeName().equals("pool-name")) {
                                            String[] poolName = poolEntry.getFirstChild().getNodeValue().split("-");
                                            if (poolName.length == 5) {
                                                rulePrivateIp = poolName[0] + "." + poolName[1] + "." + poolName[2] + "." + poolName[3];
                                                ruleDestPort = poolName[4];
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (rulePublicIp == null || rulePrivateIp == null || ruleSrcPort == null || ruleDestPort == null) {
                continue;
            }

            boolean addToList = false;
            if (condition.equals(RuleMatchCondition.ALL)) {
                addToList = true;
            } else if (condition.equals(RuleMatchCondition.PUBLIC_PRIVATE_IPS)) {
                assert (publicIp != null && privateIp != null);
                addToList = publicIp.equals(rulePublicIp) && privateIp.equals(rulePrivateIp);
            } else if (condition.equals(RuleMatchCondition.PRIVATE_SUBNET)) {
                assert (privateGateway != null && privateCidrSize != null);
                addToList = NetUtils.sameSubnetCIDR(rulePrivateIp, privateGateway, privateCidrSize);
            }

            if (addToList) {
                destNatRules.add(new String[] {rulePublicIp, rulePrivateIp, ruleSrcPort, ruleDestPort});
            }
        }

        return destNatRules;
    }

    /*
     * Source NAT pools
     */

    private String genSourceNatPoolName(String publicIp) {
        return genObjectName(genIpIdentifier(publicIp));
    }

    private boolean manageSourceNatPool(SrxCommand command, String publicIp) throws ExecutionException {
        String poolName = genSourceNatPoolName(publicIp);
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.SRC_NAT_POOL_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "pool-name", poolName);
                return sendRequestAndCheckResponse(command, xml, "name", poolName);

            case CHECK_IF_IN_USE:
                // Check if any source nat rules refer to this pool
                xml = SrxXml.SRC_NAT_RULE_GETALL.getXml();
                return sendRequestAndCheckResponse(command, xml, "pool-name", poolName);

            case ADD:
                if (manageSourceNatPool(SrxCommand.CHECK_IF_EXISTS, publicIp)) {
                    return true;
                }

                xml = SrxXml.SRC_NAT_POOL_ADD.getXml();
                xml = replaceXmlValue(xml, "pool-name", poolName);
                xml = replaceXmlValue(xml, "address", publicIp + "/32");

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add source NAT pool for public IP " + publicIp);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageSourceNatPool(SrxCommand.CHECK_IF_EXISTS, publicIp)) {
                    return true;
                }

                if (manageSourceNatPool(SrxCommand.CHECK_IF_IN_USE, publicIp)) {
                    return true;
                }

                xml = SrxXml.SRC_NAT_POOL_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "pool-name", poolName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to delete source NAT pool for public IP " + publicIp);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Source NAT rules
     */

    private String genSourceNatRuleName(String publicIp, String privateSubnet) {
        return genObjectName(genIpIdentifier(publicIp), genIpIdentifier(privateSubnet));
    }

    private boolean manageSourceNatRule(SrxCommand command, String publicIp, String privateSubnet) throws ExecutionException {
        String ruleName = genSourceNatRuleName(publicIp, privateSubnet);
        String poolName = genSourceNatPoolName(publicIp);
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.SRC_NAT_RULE_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "rule-set", _privateZone);
                xml = replaceXmlValue(xml, "from-zone", _privateZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);
                return sendRequestAndCheckResponse(command, xml, "name", ruleName);

            case ADD:
                if (manageSourceNatRule(SrxCommand.CHECK_IF_EXISTS, publicIp, privateSubnet)) {
                    return true;
                }

                if (!manageSourceNatPool(SrxCommand.CHECK_IF_EXISTS, publicIp)) {
                    throw new ExecutionException("The source NAT pool corresponding to " + publicIp + " does not exist.");
                }

                xml = SrxXml.SRC_NAT_RULE_ADD.getXml();
                xml = replaceXmlValue(xml, "rule-set", _privateZone);
                xml = replaceXmlValue(xml, "from-zone", _privateZone);
                xml = replaceXmlValue(xml, "to-zone", _publicZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);
                xml = replaceXmlValue(xml, "private-subnet", privateSubnet);
                xml = replaceXmlValue(xml, "pool-name", poolName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add source NAT rule for public IP " + publicIp + " and private subnet " + privateSubnet);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageSourceNatRule(SrxCommand.CHECK_IF_EXISTS, publicIp, privateSubnet)) {
                    return true;
                }

                xml = SrxXml.SRC_NAT_RULE_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "rule-set", _privateZone);
                xml = replaceXmlValue(xml, "from-zone", _privateZone);
                xml = replaceXmlValue(xml, "rule-name", ruleName);

                if (!sendRequestAndCheckResponse(command, xml, "name", ruleName)) {
                    throw new ExecutionException("Failed to delete source NAT rule for public IP " + publicIp + " and private subnet " + privateSubnet);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }

    }

    /*
     * Address book entries
     */

    private String genAddressBookEntryName(String ip) {
        if (ip == null) {
            return "any";
        } else {
            return genIpIdentifier(ip);
        }
    }

    private boolean manageAddressBookEntry(SrxCommand command, String zone, String ip, String entryName) throws ExecutionException {
        if (!zone.equals(_publicZone) && !zone.equals(_privateZone)) {
            throw new ExecutionException("Invalid zone.");
        }

        if (entryName == null) {
            entryName = genAddressBookEntryName(ip);
        }

        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.ADDRESS_BOOK_ENTRY_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "zone", zone);
                xml = replaceXmlValue(xml, "entry-name", entryName);
                return sendRequestAndCheckResponse(command, xml, "name", entryName);

            case CHECK_IF_IN_USE:
                // Check if any security policies refer to this address book entry
                xml = SrxXml.SECURITY_POLICY_GETALL.getXml();
                String fromZone = zone.equals(_publicZone) ? _privateZone : _publicZone;
                xml = replaceXmlValue(xml, "from-zone", fromZone);
                xml = replaceXmlValue(xml, "to-zone", zone);
                return sendRequestAndCheckResponse(command, xml, "destination-address", entryName);

            case ADD:
                if (manageAddressBookEntry(SrxCommand.CHECK_IF_EXISTS, zone, ip, entryName)) {
                    return true;
                }

                xml = SrxXml.ADDRESS_BOOK_ENTRY_ADD.getXml();
                xml = replaceXmlValue(xml, "zone", zone);
                xml = replaceXmlValue(xml, "entry-name", entryName);
                xml = replaceXmlValue(xml, "ip", ip);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add address book entry for IP " + ip + " in zone " + zone);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageAddressBookEntry(SrxCommand.CHECK_IF_EXISTS, zone, ip, entryName)) {
                    return true;
                }

                if (manageAddressBookEntry(SrxCommand.CHECK_IF_IN_USE, zone, ip, entryName)) {
                    return true;
                }

                xml = SrxXml.ADDRESS_BOOK_ENTRY_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "zone", zone);
                xml = replaceXmlValue(xml, "entry-name", entryName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to delete address book entry for IP " + ip + " in zone " + zone);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;

        }

    }

    /*
     * Applications
     */

    private String genApplicationName(SecurityPolicyType type, Protocol protocol, int startPort, int endPort) {
        if (protocol.equals(Protocol.any)) {
            return Protocol.any.toString();
        } else {
            if (type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS) || type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT)) {
                return genObjectName(type.getIdentifier(), protocol.toString(), String.valueOf(startPort), String.valueOf(endPort));
            } else {
                return genObjectName(protocol.toString(), String.valueOf(startPort), String.valueOf(endPort));
            }
        }
    }

    private Object[] parseApplicationName(SecurityPolicyType type, String applicationName) throws ExecutionException {
        String errorMsg = "Invalid application: " + applicationName;
        String[] applicationComponents = applicationName.split("-");

        Protocol protocol;
        Integer startPort;
        Integer endPort;
        int offset = 0;
        try {
            offset = (type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS) || type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT)) ? 1 : 0;
            protocol = getProtocol(applicationComponents[offset + 0]);
            startPort = Integer.parseInt(applicationComponents[offset + 1]);
            endPort = Integer.parseInt(applicationComponents[offset + 2]);
        } catch (Exception e) {
            throw new ExecutionException(errorMsg);
        }

        return new Object[] {protocol, startPort, endPort};
    }

    private boolean manageApplication(SecurityPolicyType type, SrxCommand command, Protocol protocol, int startPort, int endPort) throws ExecutionException {
        if (protocol.equals(Protocol.any)) {
            return true;
        }

        String applicationName = genApplicationName(type, protocol, startPort, endPort);
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.APPLICATION_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "name", applicationName);
                return sendRequestAndCheckResponse(command, xml, "name", applicationName);

            case ADD:
                if (manageApplication(type, SrxCommand.CHECK_IF_EXISTS, protocol, startPort, endPort)) {
                    return true;
                }
                String icmpOrDestPort;
                xml = SrxXml.APPLICATION_ADD.getXml();
                xml = replaceXmlValue(xml, "name", applicationName);
                xml = replaceXmlValue(xml, "protocol", protocol.toString());
                if (protocol.toString().equals(Protocol.icmp.toString())) {
                    icmpOrDestPort = "<icmp-type>" + startPort + "</icmp-type>";
                    icmpOrDestPort += "<icmp-code>" + endPort + "</icmp-code>";
                } else {
                    String destPort;

                    if (startPort == endPort) {
                        destPort = String.valueOf(startPort);
                    } else {
                        destPort = startPort + "-" + endPort;
                    }
                    icmpOrDestPort = "<destination-port>" + destPort + "</destination-port>";
                }

                xml = replaceXmlValue(xml, "dest-port-icmp", icmpOrDestPort);
                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add application " + applicationName);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageApplication(type, SrxCommand.CHECK_IF_EXISTS, protocol, startPort, endPort)) {
                    return true;
                }

                xml = SrxXml.APPLICATION_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "name", applicationName);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to delete application " + applicationName);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }

    }

    private List<String> getUnusedApplications(List<String> applications, String fromZone, String toZone) throws ExecutionException {
        List<String> unusedApplications = new ArrayList<String>();

        // Check if any of the applications are unused by existing security policies
        String xml = SrxXml.SECURITY_POLICY_GETALL.getXml();
        xml = replaceXmlValue(xml, "from-zone", fromZone);
        xml = replaceXmlValue(xml, "to-zone", toZone);
        String allPolicies = sendRequest(xml);

        for (String application : applications) {
            if (!application.equals(Protocol.any.toString()) && !allPolicies.contains(application)) {
                unusedApplications.add(application);
            }
        }

        return unusedApplications;
    }

    private List<String> getApplicationsForSecurityPolicy(SecurityPolicyType type, String privateIp, String fromZone, String toZone) throws ExecutionException {
        String policyName = genSecurityPolicyName(type, null, null, fromZone, toZone, privateIp);
        String xml = SrxXml.SECURITY_POLICY_GETONE.getXml();
        xml = setDelete(xml, false);
        xml = replaceXmlValue(xml, "from-zone", fromZone);
        xml = replaceXmlValue(xml, "to-zone", toZone);
        xml = replaceXmlValue(xml, "policy-name", policyName);
        String policy = sendRequest(xml);

        Document doc = getDocument(policy);

        List<String> policyApplications = new ArrayList<String>();
        NodeList applicationNodes = doc.getElementsByTagName("application");

        for (int i = 0; i < applicationNodes.getLength(); i++) {
            Node applicationNode = applicationNodes.item(i);
            policyApplications.add(applicationNode.getFirstChild().getNodeValue());
        }

        return policyApplications;
    }

    private List<Object[]> extractApplications(List<FirewallRuleTO> rules) throws ExecutionException {
        List<Object[]> applications = new ArrayList<Object[]>();

        for (FirewallRuleTO rule : rules) {
            Object[] application = new Object[3];
            application[0] = getProtocol(rule.getProtocol());
            if (application[0] == Protocol.icmp) {
                if (rule.getIcmpType() == -1) {
                    application[1] = 255;
                } else {
                    application[1] = rule.getIcmpType();
                }

                if (rule.getIcmpCode() == -1) {
                    application[2] = 255;
                } else {
                    application[2] = rule.getIcmpCode();
                }
            } else if (application[0] == Protocol.tcp || application[0] == Protocol.udp) {
                if (rule.getSrcPortRange() != null) {
                    application[1] = rule.getSrcPortRange()[0];
                    application[2] = rule.getSrcPortRange()[1];
                } else {
                    application[1] = 0;
                    application[2] = 65535;
                }
            } else if (application[0] == Protocol.all) {
                application[1] = 0;
                application[2] = 65535;
            }

            applications.add(application);
        }

        return applications;
    }

    /*
     * Security policies
     */

    private String genSecurityPolicyName(SecurityPolicyType type, Long accountId, String username, String fromZone, String toZone, String translatedIp) {
        if (type.equals(SecurityPolicyType.VPN)) {
            return genObjectName(_vpnObjectPrefix, String.valueOf(accountId), username);
        } else {
            return genObjectName(type.getIdentifier(), fromZone, toZone, genIpIdentifier(translatedIp));
        }
    }

    private boolean manageSecurityPolicy(SecurityPolicyType type, SrxCommand command, Long accountId, String username, String privateIp, List<String> applicationNames,
        List<String> cidrs, String ipsecVpnName, boolean defaultEgressAction) throws ExecutionException {
        String fromZone = _publicZone;
        String toZone = _privateZone;

        String securityPolicyName;
        String addressBookEntryName = null;

        if (type.equals(SecurityPolicyType.VPN) && ipsecVpnName != null) {
            securityPolicyName = ipsecVpnName;
            addressBookEntryName = ipsecVpnName;
        } else if (type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS) || type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT)) {
            fromZone = _privateZone;
            toZone = _publicZone;
            securityPolicyName = genSecurityPolicyName(type, accountId, username, fromZone, toZone, privateIp);
        } else {
            securityPolicyName = genSecurityPolicyName(type, accountId, username, fromZone, toZone, privateIp);
            addressBookEntryName = genAddressBookEntryName(privateIp);
        }

        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.SECURITY_POLICY_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "from-zone", fromZone);
                xml = replaceXmlValue(xml, "to-zone", toZone);
                xml = replaceXmlValue(xml, "policy-name", securityPolicyName);

                return sendRequestAndCheckResponse(command, xml, "name", securityPolicyName);

            case CHECK_IF_IN_USE:
                List<String[]> rulesToCheck = null;
                if (type.equals(SecurityPolicyType.STATIC_NAT)) {
                    // Check if any static NAT rules rely on this security policy
                    rulesToCheck = getStaticNatRules(RuleMatchCondition.ALL, null, null);
                } else if (type.equals(SecurityPolicyType.DESTINATION_NAT)) {
                    // Check if any destination NAT rules rely on this security policy
                    rulesToCheck = getDestNatRules(RuleMatchCondition.ALL, null, null, null, null);
                } else {
                    return false;
                }

                for (String[] rule : rulesToCheck) {
                    String rulePrivateIp = rule[1];
                    if (privateIp.equals(rulePrivateIp)) {
                        return true;
                    }
                }

                return false;

            case ADD:
                if (!(type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS) || type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT))) {
                    if (!manageAddressBookEntry(SrxCommand.CHECK_IF_EXISTS, toZone, privateIp, addressBookEntryName)) {
                        throw new ExecutionException("No address book entry for policy: " + securityPolicyName);
                    }
                }

                String srcAddrs = "";
                String dstAddrs = "";
                String action = "";
                xml = SrxXml.SECURITY_POLICY_ADD.getXml();
                xml = replaceXmlValue(xml, "policy-name", securityPolicyName);
                if (type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS) || type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT)) {
                    xml = replaceXmlValue(xml, "from-zone", _privateZone);
                    xml = replaceXmlValue(xml, "to-zone", _publicZone);
                    if (cidrs == null || cidrs.size() == 0) {
                        srcAddrs = "<source-address>any</source-address>";
                    } else {
                        for (String cidr : cidrs) {
                            srcAddrs += "<source-address>" + genAddressBookEntryName(cidr) + "</source-address>";
                        }
                    }
                    xml = replaceXmlValue(xml, "src-address", srcAddrs);
                    dstAddrs = "<destination-address>any</destination-address>";
                    xml = replaceXmlValue(xml, "dst-address", dstAddrs);
                    if (type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT)) {
                        if (defaultEgressAction == false) {
                            //for default policy is false add default deny rules
                            action = "<deny></deny>";
                        } else {
                            action = "<permit></permit>";
                        }
                    } else {
                        if (defaultEgressAction == true) {
                            //configure egress rules to deny the traffic when default egress is allow
                            action = "<deny></deny>";
                        } else {
                            action = "<permit></permit>";
                        }

                        xml = replaceXmlValue(xml, "action", action);
                    }
                } else {
                    xml = replaceXmlValue(xml, "from-zone", fromZone);
                    xml = replaceXmlValue(xml, "to-zone", toZone);
                    srcAddrs = "<source-address>any</source-address>";
                    xml = replaceXmlValue(xml, "src-address", srcAddrs);
                    dstAddrs = "<destination-address>" + addressBookEntryName + "</destination-address>";
                    xml = replaceXmlValue(xml, "dst-address", dstAddrs);
                }

                if (type.equals(SecurityPolicyType.VPN) && ipsecVpnName != null) {
                    xml = replaceXmlValue(xml, "tunnel", "<permit><tunnel><ipsec-vpn>" + ipsecVpnName + "</ipsec-vpn></tunnel></permit>");
                } else {
                    xml = replaceXmlValue(xml, "tunnel", "");
                    if (!(type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS_DEFAULT) || type.equals(SecurityPolicyType.SECURITYPOLICY_EGRESS))) {
                        action = "<permit></permit>";
                        xml = replaceXmlValue(xml, "action", action);
                    }
                }

                String applications;
                if (applicationNames == null || applicationNames.size() == 0) {
                    applications = "<application>any</application>";
                } else {
                    applications = "";
                    for (String applicationName : applicationNames) {
                        applications += "<application>" + applicationName + "</application>";
                    }
                }

                xml = replaceXmlValue(xml, "applications", applications);

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add security policy for privateIp " + privateIp + " and applications " + applicationNames);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageSecurityPolicy(type, SrxCommand.CHECK_IF_EXISTS, null, null, privateIp, applicationNames, cidrs, ipsecVpnName, defaultEgressAction)) {
                    return true;
                }

                if (manageSecurityPolicy(type, SrxCommand.CHECK_IF_IN_USE, null, null, privateIp, applicationNames, cidrs, ipsecVpnName, defaultEgressAction)) {
                    return true;
                }

                xml = SrxXml.SECURITY_POLICY_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "from-zone", fromZone);
                xml = replaceXmlValue(xml, "to-zone", toZone);
                xml = replaceXmlValue(xml, "policy-name", securityPolicyName);

                boolean success = sendRequestAndCheckResponse(command, xml);

                if (success) {
                    xml = SrxXml.SECURITY_POLICY_GETALL.getXml();
                    xml = replaceXmlValue(xml, "from-zone", fromZone);
                    xml = replaceXmlValue(xml, "to-zone", toZone);
                    String getAllResponseXml = sendRequest(xml);

                    if (getAllResponseXml == null) {
                        throw new ExecutionException("Deleted security policy, but failed to delete security policy group.");
                    }

                    if (!getAllResponseXml.contains(fromZone) || !getAllResponseXml.contains(toZone)) {
                        return true;
                    } else if (!getAllResponseXml.contains("match") && !getAllResponseXml.contains("then")) {
                        xml = SrxXml.SECURITY_POLICY_GROUP.getXml();
                        xml = replaceXmlValue(xml, "from-zone", fromZone);
                        xml = replaceXmlValue(xml, "to-zone", toZone);
                        xml = setDelete(xml, true);
                        if (!sendRequestAndCheckResponse(command, xml)) {
                            throw new ExecutionException("Deleted security policy, but failed to delete security policy group.");
                        } else {
                            return true;
                        }
                    } else {
                        return true;
                    }
                } else {
                    throw new ExecutionException("Failed to delete security policy for privateIp " + privateIp + " and applications " + applicationNames);
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;

        }
    }

    private boolean addSecurityPolicyAndApplications(SecurityPolicyType type, String privateIp, List<Object[]> applications) throws ExecutionException {
        // Add all necessary applications
        List<String> applicationNames = new ArrayList<String>();
        for (Object[] application : applications) {
            Protocol protocol = (Protocol)application[0];
            int startPort = application[1] != null ? ((Integer)application[1]) : -1;
            int endPort = application[2] != null ? ((Integer)application[2]) : -1;

            String applicationName = genApplicationName(type, protocol, startPort, endPort);
            if (!applicationNames.contains(applicationName)) {
                applicationNames.add(applicationName);
            }

            manageApplication(type, SrxCommand.ADD, protocol, startPort, endPort);
        }

        // Add a new security policy
        manageSecurityPolicy(type, SrxCommand.ADD, null, null, privateIp, applicationNames, null, null, false);

        return true;
    }

    private boolean removeSecurityPolicyAndApplications(SecurityPolicyType type, String privateIp) throws ExecutionException {
        if (!manageSecurityPolicy(type, SrxCommand.CHECK_IF_EXISTS, null, null, privateIp, null, null, null, false)) {
            return true;
        }

        if (manageSecurityPolicy(type, SrxCommand.CHECK_IF_IN_USE, null, null, privateIp, null, null, null, false)) {
            return true;
        }

        // Get a list of applications for this security policy
        List<String> applications = getApplicationsForSecurityPolicy(type, privateIp, _publicZone, _privateZone);

        // Remove the security policy
        manageSecurityPolicy(type, SrxCommand.DELETE, null, null, privateIp, null, null, null, false);

        // Remove any applications for the removed security policy that are no longer in use
        List<String> unusedApplications = getUnusedApplications(applications, _publicZone, _privateZone);
        for (String application : unusedApplications) {
            Object[] applicationComponents;

            try {
                applicationComponents = parseApplicationName(type, application);
            } catch (ExecutionException e) {
                s_logger.error("Found an invalid application: " + application + ". Not attempting to clean up.");
                continue;
            }

            Protocol protocol = (Protocol)applicationComponents[0];
            Integer startPort = (Integer)applicationComponents[1];
            Integer endPort = (Integer)applicationComponents[2];
            manageApplication(type, SrxCommand.DELETE, protocol, startPort, endPort);
        }

        return true;
    }

    private boolean removeEgressSecurityPolicyAndApplications(SecurityPolicyType type, String guestVlan, List<String> cidrs, boolean defaultEgressAction)
        throws ExecutionException {
        if (!manageSecurityPolicy(type, SrxCommand.CHECK_IF_EXISTS, null, null, guestVlan, null, cidrs, null, defaultEgressAction)) {
            return true;
        }
        // Get a list of applications for this security policy
        List<String> applications;
        applications = getApplicationsForSecurityPolicy(type, guestVlan, _privateZone, _publicZone);

        // Remove the security policy even if it is in use
        manageSecurityPolicy(type, SrxCommand.DELETE, null, null, guestVlan, null, cidrs, null, defaultEgressAction);

        // Remove any applications for the removed security policy that are no longer in use
        List<String> unusedApplications;
        unusedApplications = getUnusedApplications(applications, _privateZone, _publicZone);

        for (String application : unusedApplications) {
            Object[] applicationComponents;

            try {
                applicationComponents = parseApplicationName(type, application);
            } catch (ExecutionException e) {
                s_logger.error("Found an invalid application: " + application + ". Not attempting to clean up.");
                continue;
            }

            Protocol protocol = (Protocol)applicationComponents[0];
            Integer startPort = (Integer)applicationComponents[1];
            Integer endPort = (Integer)applicationComponents[2];
            manageApplication(type, SrxCommand.DELETE, protocol, startPort, endPort);
        }
        for (String cidr : cidrs) {
            manageAddressBookEntry(SrxCommand.DELETE, _publicZone, cidr, null);
        }

        return true;
    }

    private boolean addEgressSecurityPolicyAndApplications(SecurityPolicyType type, String guestVlan, List<Object[]> applications, List<String> cidrs,
        boolean defaultEgressAction) throws ExecutionException {
        // Add all necessary applications
        List<String> applicationNames = new ArrayList<String>();
        for (Object[] application : applications) {
            Protocol protocol = (Protocol)application[0];
            if (!protocol.equals(Protocol.all)) {
                int startPort = application[1] != null ? ((Integer)application[1]) : 0;
                int endPort = application[2] != null ? ((Integer)application[2]) : 65535;

                String applicationName = genApplicationName(type, protocol, startPort, endPort);
                if (!applicationNames.contains(applicationName)) {
                    applicationNames.add(applicationName);
                }
                manageApplication(type, SrxCommand.ADD, protocol, startPort, endPort);
            }
        }

        for (String cidr : cidrs) {
            manageAddressBookEntry(SrxCommand.ADD, _privateZone, cidr, null);
        }

        // Add a new security policy
        manageSecurityPolicy(type, SrxCommand.ADD, null, null, guestVlan, applicationNames, cidrs, null, defaultEgressAction);
        s_logger.debug("Added Egress firewall rule for guest network " + guestVlan);
        return true;
    }

    /*
     * Filter terms
     */

    private String genIpFilterTermName(String ipAddress) {
        return genIpIdentifier(ipAddress);
    }

    private boolean manageUsageFilter(SrxCommand command, UsageFilter filter, String ip, Long guestVlanTag, String filterTermName) throws ExecutionException {
        String filterName;
        String filterDescription;
        String xml;

        if (filter.equals(_usageFilterIPInput) || filter.equals(_usageFilterIPOutput)) {
            assert (ip != null && guestVlanTag == null);
            filterName = filter.getName();
            filterDescription = filter.toString() + ", public IP = " + ip;
            xml = SrxXml.PUBLIC_IP_FILTER_TERM_ADD.getXml();
        } else if (filter.equals(_usageFilterVlanInput) || filter.equals(_usageFilterVlanOutput)) {
            assert (ip == null && guestVlanTag != null);
            filterName = filter.getName() + "-" + guestVlanTag;
            filterDescription = filter.toString() + ", guest VLAN tag = " + guestVlanTag;
            filterTermName = filterName;
            xml = SrxXml.GUEST_VLAN_FILTER_TERM_ADD.getXml();
        } else {
            return false;
        }

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.FILTER_TERM_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "filter-name", filterName);
                xml = replaceXmlValue(xml, "term-name", filterTermName);
                return sendRequestAndCheckResponse(command, xml, "name", filterTermName);

            case ADD:
                if (manageUsageFilter(SrxCommand.CHECK_IF_EXISTS, filter, ip, guestVlanTag, filterTermName)) {
                    return true;
                }

                xml = replaceXmlValue(xml, "filter-name", filterName);
                xml = replaceXmlValue(xml, "term-name", filterTermName);

                if (filter.equals(_usageFilterIPInput) || filter.equals(_usageFilterIPOutput)) {
                    xml = replaceXmlValue(xml, "ip-address", ip);
                    xml = replaceXmlValue(xml, "address-type", filter.getAddressType());
                }

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add usage filter: " + filterDescription);
                } else {
                    return true;
                }

            case DELETE:
                if (!manageUsageFilter(SrxCommand.CHECK_IF_EXISTS, filter, ip, guestVlanTag, filterTermName)) {
                    return true;
                }

                boolean deleteFilter = filter.equals(_usageFilterVlanInput) || filter.equals(_usageFilterVlanOutput);
                xml = deleteFilter ? SrxXml.FILTER_GETONE.getXml() : SrxXml.FILTER_TERM_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "filter-name", filterName);
                xml = !deleteFilter ? replaceXmlValue(xml, "term-name", filterTermName) : xml;

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to delete usage filter: " + filterDescription);
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;

        }
    }

    private String genNameValueEntry(String name, String value) {
        String xml = SrxXml.TEMPLATE_ENTRY.getXml();
        xml = replaceXmlValue(xml, "name", name);
        xml = replaceXmlValue(xml, "value", value);
        return xml;
    }

    private String genMultipleEntries(String name, List<String> values) {
        String result = "";
        for (String value : values) {
            result = result + genNameValueEntry(name, value);
        }
        return result;
    }

    private String genPortRangeEntry(String protocol, String portRange) {
        String result = "";
        result = result + genNameValueEntry("protocol", protocol);
        result = result + genNameValueEntry("destination-port", portRange);
        return result;
    }

    private String genIcmpEntries(String icmpType, String icmpCode) {
        String result = "";
        result = result + genNameValueEntry("protocol", "icmp");
        if (icmpType.equals("-1")) {
            result = result + genNameValueEntry("icmp-type", "0-255");
        } else {
            result = result + genNameValueEntry("icmp-type", icmpType);
        }
        if (icmpCode.equals("-1")) {
            result = result + genNameValueEntry("icmp-code", "0-255");
        } else {
            result = result + genNameValueEntry("icmp-code", icmpCode);
        }
        return result;
    }

    private boolean manageFirewallFilter(SrxCommand command, FirewallFilterTerm term, String filterName) throws ExecutionException {
        String xml;

        switch (command) {

            case CHECK_IF_EXISTS:
                xml = SrxXml.FIREWALL_FILTER_TERM_GETONE.getXml();
                xml = setDelete(xml, false);
                xml = replaceXmlValue(xml, "filter-name", filterName);
                xml = replaceXmlValue(xml, "term-name", term.getName());
                return sendRequestAndCheckResponse(command, xml, "name", term.getName());

            case ADD:
                if (manageFirewallFilter(SrxCommand.CHECK_IF_EXISTS, term, filterName)) {
                    return true;
                }

                xml = SrxXml.FIREWALL_FILTER_TERM_ADD.getXml();

                xml = replaceXmlValue(xml, "filter-name", filterName);
                xml = replaceXmlValue(xml, "term-name", term.getName());
                xml = replaceXmlValue(xml, "source-address-entries", genMultipleEntries("source-address", term.getSourceCidrs()));
                xml = replaceXmlValue(xml, "dest-ip-address", term.getDestIp());

                String protocol = term.getProtocol();
                if (protocol.equals("tcp") || protocol.equals("udp")) {
                    xml = replaceXmlValue(xml, "protocol-options", genPortRangeEntry(protocol, term.getPortRange()));
                } else if (protocol.equals("icmp")) {
                    xml = replaceXmlValue(xml, "protocol-options", genIcmpEntries(term.getIcmpType(), term.getIcmpCode()));
                } else {
                    assert protocol.equals("any");
                    xml = replaceXmlValue(xml, "protocol-options", "");
                }
                xml = replaceXmlValue(xml, "count-name", term.getCountName());

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to add firewall filter: " + term.getName());
                } else {
                    return true;
                }

            case DELETE:
                if (!manageFirewallFilter(SrxCommand.CHECK_IF_EXISTS, term, filterName)) {
                    return true;
                }

                xml = SrxXml.FIREWALL_FILTER_TERM_GETONE.getXml();
                xml = setDelete(xml, true);
                xml = replaceXmlValue(xml, "filter-name", filterName);
                xml = replaceXmlValue(xml, "term-name", term.getName());

                if (!sendRequestAndCheckResponse(command, xml)) {
                    throw new ExecutionException("Failed to delete firewall filter: " + term.getName());
                } else {
                    return true;
                }

            default:
                s_logger.debug("Unrecognized command.");
                return false;

        }
    }

    /*
     * Usage
     */

    private ExternalNetworkResourceUsageAnswer getUsageAnswer(ExternalNetworkResourceUsageCommand cmd) throws ExecutionException {
        try {
            String socOpenException = "Failed to open a connection for Usage data.";
            String socCloseException = "Unable to close connection for Usage data.";
            if (!openUsageSocket()) {
                throw new ExecutionException(socOpenException);
            }

            ExternalNetworkResourceUsageAnswer answer = new ExternalNetworkResourceUsageAnswer(cmd);

            String xml = SrxXml.FIREWALL_FILTER_BYTES_GETALL.getXml();
            String rawUsageData = sendUsageRequest(xml);
            Document doc = getDocument(rawUsageData);

            NodeList counters = doc.getElementsByTagName("counter");
            for (int i = 0; i < counters.getLength(); i++) {
                Node n = counters.item(i);
                if (n.getNodeName().equals("counter")) {
                    NodeList counterInfoList = n.getChildNodes();
                    String counterName = null;
                    long byteCount = 0;

                    for (int j = 0; j < counterInfoList.getLength(); j++) {
                        Node counterInfo = counterInfoList.item(j);
                        if (counterInfo.getNodeName().equals("counter-name")) {
                            counterName = counterInfo.getFirstChild().getNodeValue();
                        } else if (counterInfo.getNodeName().equals("byte-count")) {
                            try {
                                byteCount = Long.parseLong(counterInfo.getFirstChild().getNodeValue());
                            } catch (Exception e) {
                                s_logger.debug(e);
                                byteCount = 0;
                            }
                        }
                    }

                    if (byteCount >= 0) {
                        updateUsageAnswer(answer, counterName, byteCount);
                    }
                }
            }
            if (!closeUsageSocket()) {
                throw new ExecutionException(socCloseException);
            }
            return answer;
        } catch (Exception e) {
            closeUsageSocket();
            throw new ExecutionException(e.getMessage());
        }

    }

    private void updateBytesMap(Map<String, long[]> bytesMap, UsageFilter filter, String usageAnswerKey, long additionalBytes) {
        long[] bytesSentAndReceived = bytesMap.get(usageAnswerKey);
        if (bytesSentAndReceived == null) {
            bytesSentAndReceived = new long[] {0, 0};
        }

        int index = 0;
        if (filter.equals(_usageFilterVlanOutput) || filter.equals(_usageFilterIPInput)) {
            index = 1;
        }

        bytesSentAndReceived[index] += additionalBytes;
        bytesMap.put(usageAnswerKey, bytesSentAndReceived);
    }

    private String getIpAddress(String counterName) {
        String[] counterNameArray = counterName.split("-");

        if (counterNameArray.length < 4) {
            return null;
        } else {
            return counterNameArray[0] + "." + counterNameArray[1] + "." + counterNameArray[2] + "." + counterNameArray[3];
        }
    }

    private String getGuestVlanTag(String counterName) {
        String[] counterNameArray = counterName.split("-");

        if (counterNameArray.length != 3) {
            return null;
        } else {
            return counterNameArray[2];
        }
    }

    private UsageFilter getUsageFilter(String counterName) {

        if (counterName.contains(_usageFilterVlanInput.getCounterIdentifier())) {
            return _usageFilterVlanInput;
        } else if (counterName.contains(_usageFilterVlanOutput.getCounterIdentifier())) {
            return _usageFilterVlanOutput;
        } else if (counterName.contains(_usageFilterIPInput.getCounterIdentifier())) {
            return _usageFilterIPInput;
        } else if (counterName.contains(_usageFilterIPOutput.getCounterIdentifier())) {
            return _usageFilterIPOutput;
        }

        return null;
    }

    private String getUsageAnswerKey(UsageFilter filter, String counterName) {
        if (filter.equals(_usageFilterVlanInput) || filter.equals(_usageFilterVlanOutput)) {
            return getGuestVlanTag(counterName);
        } else if (filter.equals(_usageFilterIPInput) || filter.equals(_usageFilterIPOutput)) {
            return getIpAddress(counterName);
        } else {
            return null;
        }
    }

    private Map<String, long[]> getBytesMap(ExternalNetworkResourceUsageAnswer answer, UsageFilter filter, String usageAnswerKey) {
        if (filter.equals(_usageFilterVlanInput) || filter.equals(_usageFilterVlanOutput)) {
            return answer.guestVlanBytes;
        } else if (filter.equals(_usageFilterIPInput) || filter.equals(_usageFilterIPOutput)) {
            return answer.ipBytes;
        } else {
            return null;
        }
    }

    private void updateUsageAnswer(ExternalNetworkResourceUsageAnswer answer, String counterName, long byteCount) {
        if (counterName == null || byteCount <= 0) {
            return;
        }

        UsageFilter filter = getUsageFilter(counterName);
        if (filter == null) {
            s_logger.debug("Failed to parse counter name in usage answer: " + counterName);
            return;
        }
        String usageAnswerKey = getUsageAnswerKey(filter, counterName);
        Map<String, long[]> bytesMap = getBytesMap(answer, filter, usageAnswerKey);
        updateBytesMap(bytesMap, filter, usageAnswerKey, byteCount);
    }

    /*
     * XML API commands
     */

    private String sendRequestPrim(BufferedWriter sendStream, BufferedReader recvStream, String xmlRequest) throws ExecutionException {
        if (!xmlRequest.contains("request-login")) {
            s_logger.debug("Sending request: " + xmlRequest);
        } else {
            s_logger.debug("Sending login request");
        }

        boolean timedOut = false;
        StringBuffer xmlResponseBuffer = new StringBuffer("");
        try {
            sendStream.write(xmlRequest);
            sendStream.flush();

            String line = "";
            while ((line = recvStream.readLine()) != null) {
                xmlResponseBuffer.append(line);
                if (line.contains("</rpc-reply>")) {
                    break;
                }
            }

        } catch (SocketTimeoutException e) {
            s_logger.debug(e);
            timedOut = true;
        } catch (IOException e) {
            s_logger.debug(e);
            return null;
        }

        String xmlResponse = xmlResponseBuffer.toString();
        String errorMsg = null;

        if (timedOut) {
            errorMsg = "Timed out on XML request: " + xmlRequest;
        } else if (xmlResponse.isEmpty()) {
            errorMsg = "Received an empty XML response.";
        } else if (xmlResponse.contains("Unexpected XML tag type")) {
            errorMsg = "Sent a command without being logged in.";
        } else if (!xmlResponse.contains("</rpc-reply>")) {
            errorMsg = "Didn't find the rpc-reply tag in the XML response.";
        }

        if (errorMsg == null) {
            return xmlResponse;
        } else {
            s_logger.error(errorMsg);
            throw new ExecutionException(errorMsg);
        }
    }

    private String sendRequest(String xmlRequest) throws ExecutionException {
        return sendRequestPrim(_toSrx, _fromSrx, xmlRequest);
    }

    private String sendUsageRequest(String xmlRequest) throws ExecutionException {
        return sendRequestPrim(_UsagetoSrx, _UsagefromSrx, xmlRequest);
    }

    private boolean checkResponse(String xmlResponse, boolean errorKeyAndValue, String key, String value) {
        if (xmlResponse == null) {
            s_logger.error("Failed to communicate with SRX!");
            return false;
        }

        if (!xmlResponse.contains("authentication-response")) {
            s_logger.debug("Checking response: " + xmlResponse);
        } else {
            s_logger.debug("Checking login response");
        }

        String textToSearchFor = key;
        if (value != null) {
            textToSearchFor = "<" + key + ">" + value + "</" + key + ">";
        }

        if ((errorKeyAndValue && !xmlResponse.contains(textToSearchFor)) || (!errorKeyAndValue && xmlResponse.contains(textToSearchFor))) {
            return true;
        }

        String responseMessage = extractXml(xmlResponse, "message");
        if (responseMessage != null) {
            s_logger.error("Request failed due to: " + responseMessage);
        } else {
            if (errorKeyAndValue) {
                s_logger.error("Found error (" + textToSearchFor + ") in response.");
            } else {
                s_logger.debug("Didn't find " + textToSearchFor + " in response.");
            }
        }

        return false;
    }

    private boolean sendRequestAndCheckResponse(SrxCommand command, String xmlRequest, String... keyAndValue) throws ExecutionException {
        boolean errorKeyAndValue = false;
        String key;
        String value;

        switch (command) {

            case LOGIN:
                key = "status";
                value = "success";
                break;

            case OPEN_CONFIGURATION:
            case CLOSE_CONFIGURATION:
                errorKeyAndValue = true;
                key = "error";
                value = null;
                break;

            case COMMIT:
                key = "commit-success";
                value = null;
                break;

            case CHECK_IF_EXISTS:
            case CHECK_IF_IN_USE:
                assert (keyAndValue != null && keyAndValue.length == 2) : "If the SrxCommand is " + command + ", both a key and value must be specified.";

                key = keyAndValue[0];
                value = keyAndValue[1];
                break;

            default:
                key = "load-success";
                value = null;
                break;

        }

        String xmlResponse = sendRequest(xmlRequest);
        return checkResponse(xmlResponse, errorKeyAndValue, key, value);
    }

    private boolean sendUsageRequestAndCheckResponse(SrxCommand command, String xmlRequest, String... keyAndValue) throws ExecutionException {
        boolean errorKeyAndValue = false;
        String key;
        String value;

        switch (command) {

            case LOGIN:
                key = "status";
                value = "success";
                break;

            case OPEN_CONFIGURATION:
            case CLOSE_CONFIGURATION:
                errorKeyAndValue = true;
                key = "error";
                value = null;
                break;

            case COMMIT:
                key = "commit-success";
                value = null;
                break;

            case CHECK_IF_EXISTS:
            case CHECK_IF_IN_USE:
                assert (keyAndValue != null && keyAndValue.length == 2) : "If the SrxCommand is " + command + ", both a key and value must be specified.";

                key = keyAndValue[0];
                value = keyAndValue[1];
                break;

            default:
                key = "load-success";
                value = null;
                break;

        }

        String xmlResponse = sendUsageRequest(xmlRequest);
        return checkResponse(xmlResponse, errorKeyAndValue, key, value);
    }

    /*
     * XML utils
     */

    private String replaceXmlTag(String xml, String oldTag, String newTag) {
        return xml.replaceAll(oldTag, newTag);
    }

    private String replaceXmlValue(String xml, String marker, String value) {
        marker = "\\s*%" + marker + "%\\s*";

        if (value == null) {
            value = "";
        }

        return xml.replaceAll(marker, value);
    }

    private String extractXml(String xml, String marker) {
        String startMarker = "<" + marker + ">";
        String endMarker = "</" + marker + ">";
        if (xml.contains(startMarker) && xml.contains(endMarker)) {
            return xml.substring(xml.indexOf(startMarker) + startMarker.length(), xml.indexOf(endMarker));
        } else {
            return null;
        }

    }

    private String setDelete(String xml, boolean delete) {
        if (delete) {
            String deleteMarker = " delete=\"delete\"";
            xml = replaceXmlTag(xml, "get-configuration", "load-configuration");
            xml = replaceXmlValue(xml, "delete", deleteMarker);
        } else {
            xml = replaceXmlTag(xml, "load-configuration", "get-configuration");
            xml = replaceXmlValue(xml, "delete", "");
        }

        return xml;
    }

    /*
     * Misc
     */

    private Long getVlanTag(String vlan) throws ExecutionException {
        Long publicVlanTag = null;
        if (!vlan.equals("untagged")) {
            try {
                // make sure this vlan is numeric
                publicVlanTag = Long.parseLong(BroadcastDomainType.getValue(vlan));
            } catch (Exception e) {
                throw new ExecutionException("Unable to parse VLAN tag: " + vlan);
            }
        }

        return publicVlanTag;
    }

    private String genObjectName(String... args) {
        String objectName = "";

        for (int i = 0; i < args.length; i++) {
            objectName += args[i];
            if (i != args.length - 1) {
                objectName += _objectNameWordSep;
            }
        }

        return objectName;
    }

    private String genIpIdentifier(String ip) {
        return ip.replace('.', '-').replace('/', '-');
    }

    private Protocol getProtocol(String protocolName) throws ExecutionException {
        protocolName = protocolName.toLowerCase();

        try {
            return Protocol.valueOf(protocolName);
        } catch (Exception e) {
            throw new ExecutionException("Invalid protocol: " + protocolName);
        }
    }

    private Document getDocument(String xml) throws ExecutionException {
        StringReader srcNatRuleReader = new StringReader(xml);
        InputSource srcNatRuleSource = new InputSource(srcNatRuleReader);
        Document doc = null;

        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(srcNatRuleSource);
        } catch (Exception e) {
            s_logger.error(e);
            throw new ExecutionException(e.getMessage());
        }

        if (doc == null) {
            throw new ExecutionException("Failed to parse xml " + xml);
        } else {
            return doc;
        }
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
