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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
// for prettyFormat()
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
// http client handling
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
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
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.utils.HttpClientWrapper;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;

public class PaloAltoResource implements ServerResource {

    private String _name;
    private String _zoneId;
    private String _ip;
    private String _username;
    private String _password;
    private String _guid;
    private String _key;
    private Integer _numRetries;
    private Integer _timeoutInSeconds;
    private String _publicZone;
    private String _privateZone;
    private String _publicInterface;
    private String _privateInterface;
    private String _publicInterfaceType;
    private String _privateInterfaceType;
    private String _virtualRouter;
    private String _threatProfile;
    private String _logProfile;
    private String _pingManagementProfile;
    private static final Logger s_logger = Logger.getLogger(PaloAltoResource.class);

    private static String s_apiUri = "/api";
    private static HttpClient s_httpclient;

    protected enum PaloAltoMethod {
        GET, POST;
    }

    private enum PaloAltoPrimative {
        CHECK_IF_EXISTS, ADD, DELETE;
    }

    private enum InterfaceType {
        AGGREGATE("aggregate-ethernet"), ETHERNET("ethernet");

        private final String type;

        private InterfaceType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return type;
        }
    }

    private enum Protocol {
        TCP("tcp"), UDP("udp"), ICMP("icmp"), ALL("all");

        private final String protocol;

        private Protocol(String protocol) {
            this.protocol = protocol;
        }

        @Override
        public String toString() {
            return protocol;
        }
    }

    private enum GuestNetworkType {
        SOURCE_NAT, INTERFACE_NAT;
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

            _publicZone = (String)params.get("publicnetwork");
            if (_publicZone == null) {
                throw new ConfigurationException("Unable to find public zone");
            }

            _privateZone = (String)params.get("privatenetwork");
            if (_privateZone == null) {
                throw new ConfigurationException("Unable to find private zone");
            }

            _virtualRouter = (String)params.get("pavr");
            if (_virtualRouter == null) {
                throw new ConfigurationException("Unable to find virtual router");
            }

            _threatProfile = (String)params.get("patp");
            _logProfile = (String)params.get("palp");

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            _numRetries = NumbersUtil.parseInt((String)params.get("numretries"), 1);
            _timeoutInSeconds = NumbersUtil.parseInt((String)params.get("timeout"), 300);

            // Open a socket and login
            if (!refreshPaloAltoConnection()) {
                throw new ConfigurationException("Unable to open a connection to the Palo Alto.");
            }

            // check that the threat profile exists if one was specified
            if (_threatProfile != null) {
                try {
                    boolean has_profile = getThreatProfile(_threatProfile);
                    if (!has_profile) {
                        throw new ConfigurationException("The specified threat profile group does not exist.");
                    }
                } catch (ExecutionException e) {
                    throw new ConfigurationException(e.getMessage());
                }
            }

            // check that the log profile exists if one was specified
            if (_logProfile != null) {
                try {
                    boolean has_profile = getLogProfile(_logProfile);
                    if (!has_profile) {
                        throw new ConfigurationException("The specified log profile does not exist.");
                    }
                } catch (ExecutionException e) {
                    throw new ConfigurationException(e.getMessage());
                }
            }

            // get public interface type
            try {
                _publicInterfaceType = getInterfaceType(_publicInterface);
                if (_publicInterfaceType.equals("")) {
                    throw new ConfigurationException("The specified public interface is not configured on the Palo Alto.");
                }
            } catch (ExecutionException e) {
                throw new ConfigurationException(e.getMessage());
            }

            // get private interface type
            try {
                _privateInterfaceType = getInterfaceType(_privateInterface);
                if (_privateInterfaceType.equals("")) {
                    throw new ConfigurationException("The specified private interface is not configured on the Palo Alto.");
                }
            } catch (ExecutionException e) {
                throw new ConfigurationException(e.getMessage());
            }

            _pingManagementProfile = "Ping";
            try {
                ArrayList<IPaloAltoCommand> cmdList = new ArrayList<IPaloAltoCommand>();
                managePingProfile(cmdList, PaloAltoPrimative.ADD);
                boolean status = requestWithCommit(cmdList);
            } catch (ExecutionException e) {
                throw new ConfigurationException(e.getMessage());
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
        cmd.setVersion(PaloAltoResource.class.getPackage().getImplementationVersion());
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
        // nothing for now...
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        return;
    }

    /*
     * Login
     */
    private void openHttpConnection() {
        s_httpclient = new DefaultHttpClient();

        // Allows you to connect via SSL using unverified certs
        s_httpclient = HttpClientWrapper.wrapClient(s_httpclient);
    }

    private boolean refreshPaloAltoConnection() {
        if (s_httpclient == null) {
            openHttpConnection();
        }

        try {
            return login(_username, _password);
        } catch (ExecutionException e) {
            s_logger.error("Failed to login due to " + e.getMessage());
            return false;
        }
    }

    private boolean login(String username, String password) throws ExecutionException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("type", "keygen");
        params.put("user", username);
        params.put("password", password);

        String keygenBody;
        try {
            keygenBody = request(PaloAltoMethod.GET, params);
        } catch (ExecutionException e) {
            return false;
        }
        Document keygen_doc = getDocument(keygenBody);
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            XPathExpression expr = xpath.compile("/response[@status='success']/result/key/text()");
            _key = (String)expr.evaluate(keygen_doc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new ExecutionException(e.getCause().getMessage());
        }
        if (_key != null) {
            return true;
        }
        return false;
    }

    // ENTRY POINTS...

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
     * Guest networks
     */

    private synchronized Answer execute(IpAssocCommand cmd) {
        refreshPaloAltoConnection();
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
            if (ip.getBroadcastUri() != null) {
                String parsedVlanTag = parsePublicVlanTag(ip.getBroadcastUri());
                if (!parsedVlanTag.equals("untagged")) {
                    try {
                        publicVlanTag = Long.parseLong(parsedVlanTag);
                    } catch (Exception e) {
                        throw new ExecutionException("Could not parse public VLAN tag: " + parsedVlanTag);
                    }
                }
            }

            ArrayList<IPaloAltoCommand> commandList = new ArrayList<IPaloAltoCommand>();

            if (ip.isAdd()) {
                // Implement the guest network for this VLAN
                implementGuestNetwork(commandList, type, publicVlanTag, sourceNatIpAddress, guestVlanTag, guestVlanGateway, guestVlanSubnet, cidrSize);
            } else {
                // Remove the guest network:
                shutdownGuestNetwork(commandList, type, publicVlanTag, sourceNatIpAddress, guestVlanTag, guestVlanGateway, guestVlanSubnet, cidrSize);
            }

            boolean status = requestWithCommit(commandList);

            results[i++] = ip.getPublicIp() + " - success";
        } catch (ExecutionException e) {
            s_logger.error(e);

            if (numRetries > 0 && refreshPaloAltoConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying IPAssocCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                results[i++] = IpAssocAnswer.errorResult;
            }
        }

        return new IpAssocAnswer(cmd, results);
    }

    private void implementGuestNetwork(ArrayList<IPaloAltoCommand> cmdList, GuestNetworkType type, Long publicVlanTag, String publicIp, long privateVlanTag,
        String privateGateway, String privateSubnet, long privateCidrNumber) throws ExecutionException {
        privateSubnet = privateSubnet + "/" + privateCidrNumber;

        managePrivateInterface(cmdList, PaloAltoPrimative.ADD, privateVlanTag, privateGateway + "/" + privateCidrNumber);

        if (type.equals(GuestNetworkType.SOURCE_NAT)) {
            managePublicInterface(cmdList, PaloAltoPrimative.ADD, publicVlanTag, publicIp + "/32", privateVlanTag);
            manageSrcNatRule(cmdList, PaloAltoPrimative.ADD, type, publicVlanTag, publicIp + "/32", privateVlanTag, privateGateway + "/" + privateCidrNumber);
            manageNetworkIsolation(cmdList, PaloAltoPrimative.ADD, privateVlanTag, privateSubnet, privateGateway);
        }

        String msg =
            "Implemented guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway + "/" + privateCidrNumber;
        msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + publicIp : "";
        s_logger.debug(msg);
    }

    private void shutdownGuestNetwork(ArrayList<IPaloAltoCommand> cmdList, GuestNetworkType type, Long publicVlanTag, String sourceNatIpAddress, long privateVlanTag,
        String privateGateway, String privateSubnet, long privateCidrSize) throws ExecutionException {
        privateSubnet = privateSubnet + "/" + privateCidrSize;

        // remove any orphaned egress rules if they exist...
        removeOrphanedFirewallRules(cmdList, privateVlanTag);

        if (type.equals(GuestNetworkType.SOURCE_NAT)) {
            manageNetworkIsolation(cmdList, PaloAltoPrimative.DELETE, privateVlanTag, privateSubnet, privateGateway);
            manageSrcNatRule(cmdList, PaloAltoPrimative.DELETE, type, publicVlanTag, sourceNatIpAddress + "/32", privateVlanTag, privateGateway + "/" + privateCidrSize);
            managePublicInterface(cmdList, PaloAltoPrimative.DELETE, publicVlanTag, sourceNatIpAddress + "/32", privateVlanTag);
        }

        managePrivateInterface(cmdList, PaloAltoPrimative.DELETE, privateVlanTag, privateGateway + "/" + privateCidrSize);

        String msg = "Shut down guest network with type " + type + ". Guest VLAN tag: " + privateVlanTag + ", guest gateway: " + privateGateway + "/" + privateCidrSize;
        msg += type.equals(GuestNetworkType.SOURCE_NAT) ? ", source NAT IP: " + sourceNatIpAddress : "";
        s_logger.debug(msg);
    }

    /*
     * Firewall rule entry point
     */
    private synchronized Answer execute(SetFirewallRulesCommand cmd) {
        refreshPaloAltoConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetFirewallRulesCommand cmd, int numRetries) {
        FirewallRuleTO[] rules = cmd.getRules();
        try {
            ArrayList<IPaloAltoCommand> commandList = new ArrayList<IPaloAltoCommand>();

            for (FirewallRuleTO rule : rules) {
                if (!rule.revoked()) {
                    manageFirewallRule(commandList, PaloAltoPrimative.ADD, rule);
                } else {
                    manageFirewallRule(commandList, PaloAltoPrimative.DELETE, rule);
                }
            }

            boolean status = requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

            if (numRetries > 0 && refreshPaloAltoConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetFirewallRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    /*
     * Static NAT rule entry point
     */

    private synchronized Answer execute(SetStaticNatRulesCommand cmd) {
        refreshPaloAltoConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetStaticNatRulesCommand cmd, int numRetries) {
        StaticNatRuleTO[] rules = cmd.getRules();

        try {
            ArrayList<IPaloAltoCommand> commandList = new ArrayList<IPaloAltoCommand>();

            for (StaticNatRuleTO rule : rules) {
                if (!rule.revoked()) {
                    manageStcNatRule(commandList, PaloAltoPrimative.ADD, rule);
                } else {
                    manageStcNatRule(commandList, PaloAltoPrimative.DELETE, rule);
                }
            }

            boolean status = requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

            if (numRetries > 0 && refreshPaloAltoConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetStaticNatRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    /*
     * Destination NAT (Port Forwarding) entry point
     */
    private synchronized Answer execute(SetPortForwardingRulesCommand cmd) {
        refreshPaloAltoConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetPortForwardingRulesCommand cmd, int numRetries) {
        PortForwardingRuleTO[] rules = cmd.getRules();

        try {
            ArrayList<IPaloAltoCommand> commandList = new ArrayList<IPaloAltoCommand>();

            for (PortForwardingRuleTO rule : rules) {
                if (!rule.revoked()) {
                    manageDstNatRule(commandList, PaloAltoPrimative.ADD, rule);
                } else {
                    manageDstNatRule(commandList, PaloAltoPrimative.DELETE, rule);
                }
            }

            boolean status = requestWithCommit(commandList);

            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error(e);

            if (numRetries > 0 && refreshPaloAltoConnection()) {
                int numRetriesRemaining = numRetries - 1;
                s_logger.debug("Retrying SetPortForwardingRulesCommand. Number of retries remaining: " + numRetriesRemaining);
                return execute(cmd, numRetriesRemaining);
            } else {
                return new Answer(cmd, e);
            }
        }
    }

    // IMPLEMENTATIONS...

    /*
     * Private interface implementation
     */

    private String genPrivateInterfaceName(long vlanTag) {
        return _privateInterface + "." + Long.toString(vlanTag);
    }

    public boolean managePrivateInterface(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim, long privateVlanTag, String privateGateway)
        throws ExecutionException {
        String interfaceName = genPrivateInterfaceName(privateVlanTag);

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/network/interface/" + _privateInterfaceType + "/entry[@name='" + _privateInterface +
                    "']/layer3/units/entry[@name='" + interfaceName + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Private sub-interface exists: " + interfaceName + ", " + result);
                return result;

            case ADD:
                if (managePrivateInterface(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, privateVlanTag, privateGateway)) {
                    return true;
                }

                // add cmds
                // add sub-interface
                Map<String, String> a_sub_params = new HashMap<String, String>();
                a_sub_params.put("type", "config");
                a_sub_params.put("action", "set");
                a_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _privateInterfaceType + "/entry[@name='" + _privateInterface +
                    "']/layer3/units/entry[@name='" + interfaceName + "']");
                a_sub_params.put("element", "<tag>" + privateVlanTag + "</tag><ip><entry name='" + privateGateway + "'/></ip><interface-management-profile>" +
                    _pingManagementProfile + "</interface-management-profile>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_sub_params));

                // add sub-interface to VR...
                Map<String, String> a_vr_params = new HashMap<String, String>();
                a_vr_params.put("type", "config");
                a_vr_params.put("action", "set");
                a_vr_params.put("xpath", "/config/devices/entry/network/virtual-router/entry[@name='" + _virtualRouter + "']/interface");
                a_vr_params.put("element", "<member>" + interfaceName + "</member>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_vr_params));

                // add sub-interface to vsys...
                Map<String, String> a_vsys_params = new HashMap<String, String>();
                a_vsys_params.put("type", "config");
                a_vsys_params.put("action", "set");
                a_vsys_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/import/network/interface");
                a_vsys_params.put("element", "<member>" + interfaceName + "</member>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_vsys_params));

                // add sub-interface to zone...
                Map<String, String> a_zone_params = new HashMap<String, String>();
                a_zone_params.put("type", "config");
                a_zone_params.put("action", "set");
                a_zone_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/zone/entry[@name='" + _privateZone + "']/network/layer3");
                a_zone_params.put("element", "<member>" + interfaceName + "</member>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_zone_params));

                return true;

            case DELETE:
                if (!managePrivateInterface(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, privateVlanTag, privateGateway)) {
                    return true;
                }

                // add cmds to the list
                // delete sub-interface from zone...
                Map<String, String> d_zone_params = new HashMap<String, String>();
                d_zone_params.put("type", "config");
                d_zone_params.put("action", "delete");
                d_zone_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/zone/entry[@name='" + _privateZone + "']/network/layer3/member[text()='" +
                    interfaceName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_zone_params));

                // delete sub-interface from vsys...
                Map<String, String> d_vsys_params = new HashMap<String, String>();
                d_vsys_params.put("type", "config");
                d_vsys_params.put("action", "delete");
                d_vsys_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/import/network/interface/member[text()='" + interfaceName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_vsys_params));

                // delete sub-interface from VR...
                Map<String, String> d_vr_params = new HashMap<String, String>();
                d_vr_params.put("type", "config");
                d_vr_params.put("action", "delete");
                d_vr_params.put("xpath", "/config/devices/entry/network/virtual-router/entry[@name='" + _virtualRouter + "']/interface/member[text()='" + interfaceName +
                    "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_vr_params));

                // delete sub-interface...
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "config");
                d_sub_params.put("action", "delete");
                d_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _privateInterfaceType + "/entry[@name='" + _privateInterface +
                    "']/layer3/units/entry[@name='" + interfaceName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_sub_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Public Interface implementation
     */

    private String genPublicInterfaceName(Long id) {
        return _publicInterface + "." + Long.toString(id);
    }

    public boolean managePublicInterface(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim, Long publicVlanTag, String publicIp, long privateVlanTag)
        throws ExecutionException {
        String interfaceName;
        if (publicVlanTag == null) {
            interfaceName = genPublicInterfaceName(new Long("9999"));
        } else {
            interfaceName = genPublicInterfaceName(publicVlanTag);
        }

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + interfaceName + "']/ip/entry[@name='" + publicIp + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Public sub-interface & IP exists: " + interfaceName + " : " + publicIp + ", " + result);
                return result;

            case ADD:
                if (managePublicInterface(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, publicVlanTag, publicIp, privateVlanTag)) {
                    return true;
                }

                // add IP to the sub-interface
                Map<String, String> a_sub_params = new HashMap<String, String>();
                a_sub_params.put("type", "config");
                a_sub_params.put("action", "set");
                a_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + interfaceName + "']/ip");
                a_sub_params.put("element", "<entry name='" + publicIp + "'/>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_sub_params));

                // add sub-interface to VR (does nothing if already done)...
                Map<String, String> a_vr_params = new HashMap<String, String>();
                a_vr_params.put("type", "config");
                a_vr_params.put("action", "set");
                a_vr_params.put("xpath", "/config/devices/entry/network/virtual-router/entry[@name='" + _virtualRouter + "']/interface");
                a_vr_params.put("element", "<member>" + interfaceName + "</member>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_vr_params));

                // add sub-interface to vsys (does nothing if already done)...
                Map<String, String> a_vsys_params = new HashMap<String, String>();
                a_vsys_params.put("type", "config");
                a_vsys_params.put("action", "set");
                a_vsys_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/import/network/interface");
                a_vsys_params.put("element", "<member>" + interfaceName + "</member>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_vsys_params));

                // add sub-interface to zone (does nothing if already done)...
                Map<String, String> a_zone_params = new HashMap<String, String>();
                a_zone_params.put("type", "config");
                a_zone_params.put("action", "set");
                a_zone_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/zone/entry[@name='" + _publicZone + "']/network/layer3");
                a_zone_params.put("element", "<member>" + interfaceName + "</member>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_zone_params));

                return true;

            case DELETE:
                if (!managePublicInterface(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, publicVlanTag, publicIp, privateVlanTag)) {
                    return true;
                }

                // delete IP from sub-interface...
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "config");
                d_sub_params.put("action", "delete");
                d_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + interfaceName + "']/ip/entry[@name='" + publicIp + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_sub_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Source NAT rule implementation
     */

    private String genSrcNatRuleName(Long privateVlanTag) {
        return "src_nat." + Long.toString(privateVlanTag);
    }

    public boolean manageSrcNatRule(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim, GuestNetworkType type, Long publicVlanTag, String publicIp,
        long privateVlanTag, String privateGateway) throws ExecutionException {
        String publicInterfaceName;
        if (publicVlanTag == null) {
            publicInterfaceName = genPublicInterfaceName(new Long("9999"));
        } else {
            publicInterfaceName = genPublicInterfaceName(publicVlanTag);
        }
        String srcNatName = genSrcNatRuleName(privateVlanTag);

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + srcNatName + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Source NAT exists: " + srcNatName + ", " + result);
                return result;

            case ADD:
                if (manageSrcNatRule(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, type, publicVlanTag, publicIp, privateVlanTag, privateGateway)) {
                    return true;
                }

                String xml = "";
                xml += "<from><member>" + _privateZone + "</member></from>";
                xml += "<to><member>" + _publicZone + "</member></to>";
                xml += "<source><member>" + privateGateway + "</member></source>";
                xml += "<destination><member>any</member></destination>";
                xml += "<service>any</service>";
                xml += "<nat-type>ipv4</nat-type>";
                xml += "<to-interface>" + publicInterfaceName + "</to-interface>";
                xml += "<source-translation><dynamic-ip-and-port><interface-address>";
                xml += "<ip>" + publicIp + "</ip>";
                xml += "<interface>" + publicInterfaceName + "</interface>";
                xml += "</interface-address></dynamic-ip-and-port></source-translation>";

                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + srcNatName + "']");
                a_params.put("element", xml);
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, a_params));

                return true;

            case DELETE:
                if (!manageSrcNatRule(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, type, publicVlanTag, publicIp, privateVlanTag, privateGateway)) {
                    return true;
                }

                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + srcNatName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, d_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Destination NAT rules (Port Forwarding) implementation
     */
    private String genDstNatRuleName(String publicIp, long id) {
        return "dst_nat." + genIpIdentifier(publicIp) + "_" + Long.toString(id);
    }

    public boolean manageDstNatRule(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim, PortForwardingRuleTO rule) throws ExecutionException {
        String publicIp = rule.getSrcIp();
        String dstNatName = genDstNatRuleName(publicIp, rule.getId());

        String publicInterfaceName;
        String publicVlanTag;
        if (rule.getSrcVlanTag() == null) {
            publicInterfaceName = genPublicInterfaceName(new Long("9999"));
        } else {
            publicVlanTag = parsePublicVlanTag(rule.getSrcVlanTag());
            if (publicVlanTag.equals("untagged")) {
                publicInterfaceName = genPublicInterfaceName(new Long("9999"));
            } else {
                publicInterfaceName = genPublicInterfaceName(new Long(publicVlanTag));
            }
        }

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + dstNatName + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Destination NAT exists: " + dstNatName + ", " + result);
                return result;

            case ADD:
                if (manageDstNatRule(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                // build source service xml
                String srcService;
                String protocol = rule.getProtocol();
                int[] srcPortRange = rule.getSrcPortRange();
                if (srcPortRange != null) {
                    String portRange;
                    if (srcPortRange.length == 1 || srcPortRange[0] == srcPortRange[1]) {
                        portRange = String.valueOf(srcPortRange[0]);
                    } else {
                        portRange = String.valueOf(srcPortRange[0]) + "-" + String.valueOf(srcPortRange[1]);
                    }
                    manageService(cmdList, PaloAltoPrimative.ADD, protocol, portRange, null);
                    srcService = genServiceName(protocol, portRange, null);
                } else {
                    // no equivalent config in PA, so allow all traffic...
                    srcService = "any";
                }

                // build destination port xml (single port limit in PA)
                String dstPortXML = "";
                int[] dstPortRange = rule.getDstPortRange();
                if (dstPortRange != null) {
                    dstPortXML = "<translated-port>" + dstPortRange[0] + "</translated-port>";
                }

                // add public IP to the sub-interface
                Map<String, String> a_sub_params = new HashMap<String, String>();
                a_sub_params.put("type", "config");
                a_sub_params.put("action", "set");
                a_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + publicInterfaceName + "']/ip");
                a_sub_params.put("element", "<entry name='" + publicIp + "/32'/>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_sub_params));

                // add the destination nat rule for the public IP
                String xml = "";
                xml += "<from><member>" + _publicZone + "</member></from>";
                xml += "<to><member>" + _publicZone + "</member></to>";
                xml += "<source><member>any</member></source>";
                xml += "<destination><member>" + publicIp + "</member></destination>";
                xml += "<service>" + srcService + "</service>";
                xml += "<nat-type>ipv4</nat-type>";
                xml += "<to-interface>" + publicInterfaceName + "</to-interface>";
                xml += "<destination-translation><translated-address>" + rule.getDstIp() + "</translated-address>" + dstPortXML + "</destination-translation>";

                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + dstNatName + "']");
                a_params.put("element", xml);
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, a_params));

                return true;

            case DELETE:
                if (!manageDstNatRule(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                // determine if we need to delete the ip from the interface as well...
                Map<String, String> c_params = new HashMap<String, String>();
                c_params.put("type", "config");
                c_params.put("action", "get");
                c_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[destination/member[text()='" + publicIp + "']]");
                String c_response = request(PaloAltoMethod.GET, c_params);

                String count = "";
                NodeList response_body;
                Document doc = getDocument(c_response);
                XPath xpath = XPathFactory.newInstance().newXPath();
                try {
                    XPathExpression expr = xpath.compile("/response[@status='success']/result");
                    response_body = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    throw new ExecutionException(e.getCause().getMessage());
                }
                if (response_body.getLength() > 0 && response_body.item(0).getAttributes().getLength() > 0) {
                    count = response_body.item(0).getAttributes().getNamedItem("count").getTextContent();
                }

                // delete the dst nat rule
                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + dstNatName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, d_params));

                if (!count.equals("") && Integer.parseInt(count) == 1) { // this dst nat rule is the last, so remove the ip...
                    // delete IP from sub-interface...
                    Map<String, String> d_sub_params = new HashMap<String, String>();
                    d_sub_params.put("type", "config");
                    d_sub_params.put("action", "delete");
                    d_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                        "']/layer3/units/entry[@name='" + publicInterfaceName + "']/ip/entry[@name='" + publicIp + "/32']");
                    cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_sub_params));
                }

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Static NAT rule implementation
     */
    private String genStcNatRuleName(String publicIp, long id) {
        return "stc_nat." + genIpIdentifier(publicIp) + "_" + Long.toString(id);
    }

    public boolean manageStcNatRule(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim, StaticNatRuleTO rule) throws ExecutionException {
        String publicIp = rule.getSrcIp();
        String stcNatName = genStcNatRuleName(publicIp, rule.getId());

        String publicInterfaceName;
        String publicVlanTag;
        if (rule.getSrcVlanTag() == null) {
            publicInterfaceName = genPublicInterfaceName(new Long("9999"));
        } else {
            publicVlanTag = parsePublicVlanTag(rule.getSrcVlanTag());
            if (publicVlanTag.equals("untagged")) {
                publicInterfaceName = genPublicInterfaceName(new Long("9999"));
            } else {
                publicInterfaceName = genPublicInterfaceName(new Long(publicVlanTag));
            }
        }

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + stcNatName + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Static NAT exists: " + stcNatName + ", " + result);
                return result;

            case ADD:
                if (manageStcNatRule(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                // add public IP to the sub-interface
                Map<String, String> a_sub_params = new HashMap<String, String>();
                a_sub_params.put("type", "config");
                a_sub_params.put("action", "set");
                a_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + publicInterfaceName + "']/ip");
                a_sub_params.put("element", "<entry name='" + publicIp + "/32'/>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_sub_params));

                // add the static nat rule for the public IP
                String xml = "";
                xml += "<from><member>" + _publicZone + "</member></from>";
                xml += "<to><member>" + _publicZone + "</member></to>";
                xml += "<source><member>any</member></source>";
                xml += "<destination><member>" + publicIp + "</member></destination>";
                xml += "<service>any</service>";
                xml += "<nat-type>ipv4</nat-type>";
                xml += "<to-interface>" + publicInterfaceName + "</to-interface>";
                xml += "<destination-translation><translated-address>" + rule.getDstIp() + "</translated-address></destination-translation>";

                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + stcNatName + "']");
                a_params.put("element", xml);
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, a_params));

                return true;

            case DELETE:
                if (!manageStcNatRule(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                // delete the static nat rule
                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/nat/rules/entry[@name='" + stcNatName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, d_params));

                // delete IP from sub-interface...
                Map<String, String> d_sub_params = new HashMap<String, String>();
                d_sub_params.put("type", "config");
                d_sub_params.put("action", "delete");
                d_sub_params.put("xpath", "/config/devices/entry/network/interface/" + _publicInterfaceType + "/entry[@name='" + _publicInterface +
                    "']/layer3/units/entry[@name='" + publicInterfaceName + "']/ip/entry[@name='" + publicIp + "/32']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_sub_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    /*
     * Firewall rule implementation
     */
    private String genFirewallRuleName(long id) { // ingress
        return "policy_" + Long.toString(id);
    }

    private String genFirewallRuleName(long id, String vlan) { // egress
        return "policy_" + Long.toString(id) + "_" + vlan;
    }

    public boolean manageFirewallRule(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim, FirewallRuleTO rule) throws ExecutionException {
        String ruleName;
        if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
            ruleName = genFirewallRuleName(rule.getId(), rule.getSrcVlanTag());
        } else {
            ruleName = genFirewallRuleName(rule.getId());
        }

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Firewall policy exists: " + ruleName + ", " + result);
                return result;

            case ADD:
                if (manageFirewallRule(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                String srcZone;
                String dstZone;
                String dstAddressXML;
                String appXML;
                String serviceXML;

                String protocol = rule.getProtocol();
                String action = "allow";

                // Only ICMP will use an Application, so others will be any.
                if (protocol.equals(Protocol.ICMP.toString())) {
                    appXML = "<member>icmp</member><member>ping</member><member>traceroute</member>"; // use the default icmp applications...
                } else {
                    appXML = "<member>any</member>";
                }

                // Only TCP and UDP will use a Service, others will use any.
                if (protocol.equals(Protocol.TCP.toString()) || protocol.equals(Protocol.UDP.toString())) {
                    String portRange;
                    if (rule.getSrcPortRange() != null) {
                        int startPort = rule.getSrcPortRange()[0];
                        int endPort = rule.getSrcPortRange()[1];
                        if (startPort == endPort) {
                            portRange = String.valueOf(startPort);
                        } else {
                            portRange = String.valueOf(startPort) + "-" + String.valueOf(endPort);
                        }
                        manageService(cmdList, PaloAltoPrimative.ADD, protocol, portRange, null);
                        serviceXML = "<member>" + genServiceName(protocol, portRange, null) + "</member>";
                    } else {
                        // no equivalent config in PA, so allow all traffic...
                        serviceXML = "<member>any</member>";
                    }
                } else {
                    serviceXML = "<member>any</member>";
                }

                // handle different types of fire wall rules (egress | ingress)
                if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) { // Egress Rule
                    srcZone = _privateZone;
                    dstZone = _publicZone;
                    dstAddressXML = "<member>any</member>";

                    // defaults to 'allow', the deny rules are as follows
                    if (rule.getType() == FirewallRule.FirewallRuleType.System) {
                        if (!rule.isDefaultEgressPolicy()) { // default of deny && system rule, so deny
                            action = "deny";
                        }
                    } else {
                        if (rule.isDefaultEgressPolicy()) { // default is allow && user rule, so deny
                            action = "deny";
                        }
                    }
                } else { // Ingress Rule
                    srcZone = _publicZone;
                    dstZone = _privateZone;
                    dstAddressXML = "<member>" + rule.getSrcIp() + "</member>";
                }

                // build the source cidr xml
                String srcCidrXML = "";
                List<String> ruleSrcCidrList = rule.getSourceCidrList();
                if (ruleSrcCidrList.size() > 0) { // a cidr was entered, modify as needed...
                    for (int i = 0; i < ruleSrcCidrList.size(); i++) {
                        if (ruleSrcCidrList.get(i).trim().equals("0.0.0.0/0")) { // allow any
                            if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                                srcCidrXML += "<member>" + getPrivateSubnet(rule.getSrcVlanTag()) + "</member>";
                            } else {
                                srcCidrXML += "<member>any</member>";
                            }
                        } else {
                            srcCidrXML += "<member>" + ruleSrcCidrList.get(i).trim() + "</member>";
                        }
                    }
                } else { // no cidr was entered, so allow ALL according to firewall rule type
                    if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                        srcCidrXML = "<member>" + getPrivateSubnet(rule.getSrcVlanTag()) + "</member>";
                    } else {
                        srcCidrXML = "<member>any</member>";
                    }
                }

                // build new rule xml
                String xml = "";
                xml += "<from><member>" + srcZone + "</member></from>";
                xml += "<to><member>" + dstZone + "</member></to>";
                xml += "<source>" + srcCidrXML + "</source>";
                xml += "<destination>" + dstAddressXML + "</destination>";
                xml += "<application>" + appXML + "</application>";
                xml += "<service>" + serviceXML + "</service>";
                xml += "<action>" + action + "</action>";
                xml += "<negate-source>no</negate-source>";
                xml += "<negate-destination>no</negate-destination>";
                if (_threatProfile != null && action.equals("allow")) { // add the threat profile if it exists
                    xml += "<profile-setting><group><member>" + _threatProfile + "</member></group></profile-setting>";
                }
                if (_logProfile != null && action.equals("allow")) { // add the log profile if it exists
                    xml += "<log-setting>" + _logProfile + "</log-setting>";
                }

                boolean has_default = false;
                String defaultEgressRule = "";
                if (rule.getTrafficType() == FirewallRule.TrafficType.Egress) {
                    // check if a default egress rule exists because it always has to be after the other rules.
                    Map<String, String> e_params = new HashMap<String, String>();
                    e_params.put("type", "config");
                    e_params.put("action", "get");
                    e_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_0_" + rule.getSrcVlanTag() + "']");
                    String e_response = request(PaloAltoMethod.GET, e_params);
                    has_default = (validResponse(e_response) && responseNotEmpty(e_response));

                    // there is an existing default rule, so we need to remove it and add it back after the new rule is added.
                    if (has_default) {
                        s_logger.debug("Moving the default egress rule after the new rule: " + ruleName);
                        NodeList response_body;
                        Document doc = getDocument(e_response);
                        XPath xpath = XPathFactory.newInstance().newXPath();
                        try {
                            XPathExpression expr = xpath.compile("/response[@status='success']/result/entry/node()");
                            response_body = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                        } catch (XPathExpressionException e) {
                            throw new ExecutionException(e.getCause().getMessage());
                        }
                        for (int i = 0; i < response_body.getLength(); i++) {
                            Node n = response_body.item(i);
                            defaultEgressRule += nodeToString(n);
                        }
                        Map<String, String> dd_params = new HashMap<String, String>();
                        dd_params.put("type", "config");
                        dd_params.put("action", "delete");
                        dd_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_0_" + rule.getSrcVlanTag() +
                            "']");
                        cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, dd_params));
                    }
                }

                // add the new rule...
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                a_params.put("element", xml);
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, a_params));

                // add back the default rule
                if (rule.getTrafficType() == FirewallRule.TrafficType.Egress && has_default) {
                    Map<String, String> da_params = new HashMap<String, String>();
                    da_params.put("type", "config");
                    da_params.put("action", "set");
                    da_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='policy_0_" + rule.getSrcVlanTag() + "']");
                    da_params.put("element", defaultEgressRule);
                    cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, da_params));
                    s_logger.debug("Completed move of the default egress rule after rule: " + ruleName);
                }

                return true;

            case DELETE:
                if (!manageFirewallRule(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, rule)) {
                    return true;
                }

                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, d_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    // remove orphaned rules if they exist...
    public void removeOrphanedFirewallRules(ArrayList<IPaloAltoCommand> cmdList, long vlan) throws ExecutionException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("type", "config");
        params.put("action", "get");
        params.put("xpath",
            "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[contains(@name, 'policy') and contains(@name, '" + Long.toString(vlan) + "')]");
        String response = request(PaloAltoMethod.GET, params);
        boolean has_orphans = (validResponse(response) && responseNotEmpty(response));

        if (has_orphans) {
            Map<String, String> d_params = new HashMap<String, String>();
            d_params.put("type", "config");
            d_params.put("action", "delete");
            d_params.put("xpath",
                "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[contains(@name, 'policy') and contains(@name, '" + Long.toString(vlan) +
                    "')]");
            cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, d_params));
        }
    }

    /*
     * Usage
     */

    /*
     * Helper config functions
     */

    // ensure guest network isolation
    private String genNetworkIsolationName(long privateVlanTag) {
        return "isolate_" + Long.toString(privateVlanTag);
    }

    public boolean manageNetworkIsolation(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim, long privateVlanTag, String privateSubnet, String privateGateway)
        throws ExecutionException {
        String ruleName = genNetworkIsolationName(privateVlanTag);

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Firewall policy exists: " + ruleName + ", " + result);
                return result;

            case ADD:
                if (manageNetworkIsolation(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, privateVlanTag, privateSubnet, privateGateway)) {
                    return true;
                }

                String xml = "";
                xml += "<from><member>" + _privateZone + "</member></from>";
                xml += "<to><member>" + _privateZone + "</member></to>";
                xml += "<source><member>" + privateSubnet + "</member></source>";
                xml += "<destination><member>" + privateGateway + "</member></destination>";
                xml += "<application><member>any</member></application>";
                xml += "<service><member>any</member></service>";
                xml += "<action>deny</action>";
                xml += "<negate-source>no</negate-source>";
                xml += "<negate-destination>yes</negate-destination>";

                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                a_params.put("element", xml);
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, a_params));

                return true;

            case DELETE:
                if (!manageNetworkIsolation(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, privateVlanTag, privateSubnet, privateGateway)) {
                    return true;
                }

                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/rulebase/security/rules/entry[@name='" + ruleName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.POST, d_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    // make the interfaces pingable for basic network troubleshooting
    public boolean managePingProfile(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim) throws ExecutionException {
        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/network/profiles/interface-management-profile/entry[@name='" + _pingManagementProfile + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Management profile exists: " + _pingManagementProfile + ", " + result);
                return result;

            case ADD:
                if (managePingProfile(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS)) {
                    return true;
                }

                // add ping profile...
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/network/profiles/interface-management-profile/entry[@name='" + _pingManagementProfile + "']");
                a_params.put("element", "<ping>yes</ping>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_params));

                return true;

            case DELETE:
                if (!managePingProfile(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS)) {
                    return true;
                }

                // delete ping profile...
                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/network/profiles/interface-management-profile/entry[@name='" + _pingManagementProfile + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    private String genServiceName(String protocol, String dstPorts, String srcPorts) {
        String name;
        if (srcPorts == null) {
            name = "cs_" + protocol.toLowerCase() + "_" + dstPorts.replace(',', '.');
        } else {
            name = "cs_" + protocol.toLowerCase() + "_" + dstPorts.replace(',', '.') + "_" + srcPorts.replace(',', '.');
        }
        return name;
    }

    public boolean manageService(ArrayList<IPaloAltoCommand> cmdList, PaloAltoPrimative prim, String protocol, String dstPorts, String srcPorts)
        throws ExecutionException {
        String serviceName = genServiceName(protocol, dstPorts, srcPorts);

        switch (prim) {

            case CHECK_IF_EXISTS:
                // check if one exists already
                Map<String, String> params = new HashMap<String, String>();
                params.put("type", "config");
                params.put("action", "get");
                params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/service/entry[@name='" + serviceName + "']");
                String response = request(PaloAltoMethod.GET, params);
                boolean result = (validResponse(response) && responseNotEmpty(response));
                s_logger.debug("Service exists: " + serviceName + ", " + result);
                return result;

            case ADD:
                if (manageService(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, protocol, dstPorts, srcPorts)) {
                    return true;
                }

                String dstPortXML = "<port>" + dstPorts + "</port>";
                String srcPortXML = "";
                if (srcPorts != null) {
                    srcPortXML = "<source-port>" + srcPorts + "</source-port>";
                }

                // add ping profile...
                Map<String, String> a_params = new HashMap<String, String>();
                a_params.put("type", "config");
                a_params.put("action", "set");
                a_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/service/entry[@name='" + serviceName + "']");
                a_params.put("element", "<protocol><" + protocol.toLowerCase() + ">" + dstPortXML + srcPortXML + "</" + protocol.toLowerCase() + "></protocol>");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, a_params));

                return true;

            case DELETE:
                if (!manageService(cmdList, PaloAltoPrimative.CHECK_IF_EXISTS, protocol, dstPorts, srcPorts)) {
                    return true;
                }

                // delete ping profile...
                Map<String, String> d_params = new HashMap<String, String>();
                d_params.put("type", "config");
                d_params.put("action", "delete");
                d_params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/service/entry[@name='" + serviceName + "']");
                cmdList.add(new DefaultPaloAltoCommand(PaloAltoMethod.GET, d_params));

                return true;

            default:
                s_logger.debug("Unrecognized command.");
                return false;
        }
    }

    private String getPrivateSubnet(String vlan) throws ExecutionException {
        String _interfaceName = genPrivateInterfaceName(Long.parseLong(vlan));
        Map<String, String> params = new HashMap<String, String>();
        params.put("type", "config");
        params.put("action", "get");
        params.put("xpath", "/config/devices/entry/network/interface/" + _privateInterfaceType + "/entry[@name='" + _privateInterface + "']/layer3/units/entry[@name='" +
            _interfaceName + "']/ip/entry");
        String response = request(PaloAltoMethod.GET, params);
        if (validResponse(response) && responseNotEmpty(response)) {
            NodeList response_body;
            Document doc = getDocument(response);
            XPath xpath = XPathFactory.newInstance().newXPath();
            try {
                XPathExpression expr = xpath.compile("/response[@status='success']/result/entry");
                response_body = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                throw new ExecutionException(e.getCause().getMessage());
            }
            if (response_body.getLength() > 0) {
                return response_body.item(0).getAttributes().getNamedItem("name").getTextContent();
            }
        }
        return null;
    }

    /*
     * XML API commands
     */

    /* Function to make calls to the Palo Alto API. */
    /* All API calls will end up going through this function. */
    protected String request(PaloAltoMethod method, Map<String, String> params) throws ExecutionException {
        if (method != PaloAltoMethod.GET && method != PaloAltoMethod.POST) {
            throw new ExecutionException("Invalid http method used to access the Palo Alto API.");
        }

        String responseBody = "";
        String debug_msg = "Palo Alto Request\n";

        // a GET method...
        if (method == PaloAltoMethod.GET) {
            String queryString = "?";
            for (String key : params.keySet()) {
                if (!queryString.equals("?")) {
                    queryString = queryString + "&";
                }
                try {
                    queryString = queryString + key + "=" + URLEncoder.encode(params.get(key), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new ExecutionException(e.getMessage());
                }
            }
            if (_key != null) {
                queryString = queryString + "&key=" + _key;
            }

            try {
                debug_msg = debug_msg + "GET request: https://" + _ip + s_apiUri + URLDecoder.decode(queryString, "UTF-8") + "\n";
            } catch (UnsupportedEncodingException e) {
                debug_msg = debug_msg + "GET request: https://" + _ip + s_apiUri + queryString + "\n";
            }

            HttpGet get_request = new HttpGet("https://" + _ip + s_apiUri + queryString);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            try {
                responseBody = s_httpclient.execute(get_request, responseHandler);
            } catch (IOException e) {
                throw new ExecutionException(e.getMessage());
            }
        }

        // a POST method...
        if (method == PaloAltoMethod.POST) {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            for (String key : params.keySet()) {
                nvps.add(new BasicNameValuePair(key, params.get(key)));
            }
            if (_key != null) {
                nvps.add(new BasicNameValuePair("key", _key));
            }

            debug_msg = debug_msg + "POST request: https://" + _ip + s_apiUri + "\n";
            for (NameValuePair nvp : nvps) {
                debug_msg = debug_msg + "param: " + nvp.getName() + ", " + nvp.getValue() + "\n";
            }

            HttpPost post_request = new HttpPost("https://" + _ip + s_apiUri);
            try {
                post_request.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            } catch (UnsupportedEncodingException e) {
                throw new ExecutionException(e.getMessage());
            }
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            try {
                responseBody = s_httpclient.execute(post_request, responseHandler);
            } catch (IOException e) {
                throw new ExecutionException(e.getMessage());
            }
        }

        debug_msg = debug_msg + prettyFormat(responseBody);
        debug_msg = debug_msg + "\n" + responseBody.replace("\"", "\\\"") + "\n\n"; // test cases
        //s_logger.debug(debug_msg); // this can be commented if we don't want to show each request in the log.

        return responseBody;
    }

    /* Used for requests that require polling to get a result (eg: commit) */
    private String requestWithPolling(PaloAltoMethod method, Map<String, String> params) throws ExecutionException {
        String job_id;
        String job_response = request(method, params);
        Document doc = getDocument(job_response);
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            XPathExpression expr = xpath.compile("/response[@status='success']/result/job/text()");
            job_id = (String)expr.evaluate(doc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new ExecutionException(e.getCause().getMessage());
        }
        if (job_id.length() > 0) {
            boolean finished = false;
            Map<String, String> job_params = new HashMap<String, String>();
            job_params.put("type", "op");
            job_params.put("cmd", "<show><jobs><id>" + job_id + "</id></jobs></show>");

            while (!finished) {
                String job_status;
                String response = request(PaloAltoMethod.GET, job_params);
                Document job_doc = getDocument(response);
                XPath job_xpath = XPathFactory.newInstance().newXPath();
                try {
                    XPathExpression expr = job_xpath.compile("/response[@status='success']/result/job/status/text()");
                    job_status = (String)expr.evaluate(job_doc, XPathConstants.STRING);
                } catch (XPathExpressionException e) {
                    throw new ExecutionException(e.getCause().getMessage());
                }
                if (job_status.equals("FIN")) {
                    finished = true;
                    String job_result;
                    try {
                        XPathExpression expr = job_xpath.compile("/response[@status='success']/result/job/result/text()");
                        job_result = (String)expr.evaluate(job_doc, XPathConstants.STRING);
                    } catch (XPathExpressionException e) {
                        throw new ExecutionException(e.getCause().getMessage());
                    }
                    if (!job_result.equals("OK")) {
                        NodeList job_details;
                        try {
                            XPathExpression expr = job_xpath.compile("/response[@status='success']/result/job/details/line");
                            job_details = (NodeList)expr.evaluate(job_doc, XPathConstants.NODESET);
                        } catch (XPathExpressionException e) {
                            throw new ExecutionException(e.getCause().getMessage());
                        }
                        String error = "";
                        for (int i = 0; i < job_details.getLength(); i++) {
                            error = error + job_details.item(i).getTextContent() + "\n";
                        }
                        throw new ExecutionException(error);
                    }
                    return response;
                } else {
                    try {
                        Thread.sleep(2000); // poll periodically for the status of the async job...
                    } catch (InterruptedException e) { /* do nothing */
                    }
                }
            }
        } else {
            return job_response;
        }
        return null;
    }

    /* Runs a sequence of commands and attempts to commit at the end. */
    /* Uses the Command pattern to enable overriding of the response handling if needed. */
    private synchronized boolean requestWithCommit(ArrayList<IPaloAltoCommand> commandList) throws ExecutionException {
        boolean result = true;

        if (commandList.size() > 0) {
            // CHECK IF THERE IS PENDING CHANGES THAT HAVE NOT BEEN COMMITTED...
            String pending_changes;
            Map<String, String> check_params = new HashMap<String, String>();
            check_params.put("type", "op");
            check_params.put("cmd", "<check><pending-changes></pending-changes></check>");
            String check_response = request(PaloAltoMethod.GET, check_params);
            Document check_doc = getDocument(check_response);
            XPath check_xpath = XPathFactory.newInstance().newXPath();
            try {
                XPathExpression expr = check_xpath.compile("/response[@status='success']/result/text()");
                pending_changes = (String)expr.evaluate(check_doc, XPathConstants.STRING);
            } catch (XPathExpressionException e) {
                throw new ExecutionException(e.getCause().getMessage());
            }
            if (pending_changes.equals("yes")) {
                throw new ExecutionException("The Palo Alto has uncommited changes, so no changes can be made.  Try again later or contact your administrator.");
            } else {
                // ADD A CONFIG LOCK TO CAPTURE THE PALO ALTO RESOURCE
                String add_lock_status;
                Map<String, String> add_lock_params = new HashMap<String, String>();
                add_lock_params.put("type", "op");
                add_lock_params.put("cmd", "<request><config-lock><add></add></config-lock></request>");
                String add_lock_response = request(PaloAltoMethod.GET, add_lock_params);
                Document add_lock_doc = getDocument(add_lock_response);
                XPath add_lock_xpath = XPathFactory.newInstance().newXPath();
                try {
                    XPathExpression expr = add_lock_xpath.compile("/response[@status='success']/result/text()");
                    add_lock_status = (String)expr.evaluate(add_lock_doc, XPathConstants.STRING);
                } catch (XPathExpressionException e) {
                    throw new ExecutionException(e.getCause().getMessage());
                }
                if (add_lock_status.length() == 0) {
                    throw new ExecutionException("The Palo Alto is locked, no changes can be made at this time.");
                }

                try {
                    // RUN THE SEQUENCE OF COMMANDS
                    for (IPaloAltoCommand command : commandList) {
                        result = (result && command.execute()); // run commands and modify result boolean
                    }

                    // COMMIT THE CHANGES (ALSO REMOVES CONFIG LOCK)
                    String commit_job_id;
                    Map<String, String> commit_params = new HashMap<String, String>();
                    commit_params.put("type", "commit");
                    commit_params.put("cmd", "<commit></commit>");
                    String commit_response = requestWithPolling(PaloAltoMethod.GET, commit_params);
                    Document commit_doc = getDocument(commit_response);
                    XPath commit_xpath = XPathFactory.newInstance().newXPath();
                    try {
                        XPathExpression expr = commit_xpath.compile("/response[@status='success']/result/job/id/text()");
                        commit_job_id = (String)expr.evaluate(commit_doc, XPathConstants.STRING);
                    } catch (XPathExpressionException e) {
                        throw new ExecutionException(e.getCause().getMessage());
                    }
                    if (commit_job_id.length() == 0) { // no commit was done, so release the lock...
                        // REMOVE THE CONFIG LOCK TO RELEASE THE PALO ALTO RESOURCE
                        String remove_lock_status;
                        Map<String, String> remove_lock_params = new HashMap<String, String>();
                        remove_lock_params.put("type", "op");
                        remove_lock_params.put("cmd", "<request><config-lock><remove></remove></config-lock></request>");
                        String remove_lock_response = request(PaloAltoMethod.GET, remove_lock_params);
                        Document remove_lock_doc = getDocument(remove_lock_response);
                        XPath remove_lock_xpath = XPathFactory.newInstance().newXPath();
                        try {
                            XPathExpression expr = remove_lock_xpath.compile("/response[@status='success']/result/text()");
                            remove_lock_status = (String)expr.evaluate(remove_lock_doc, XPathConstants.STRING);
                        } catch (XPathExpressionException e) {
                            throw new ExecutionException(e.getCause().getMessage());
                        }
                        if (remove_lock_status.length() == 0) {
                            throw new ExecutionException("Could not release the Palo Alto device.  Please notify an administrator!");
                        }
                    }

                } catch (ExecutionException ex) {
                    // REVERT TO RUNNING
                    String revert_job_id;
                    Map<String, String> revert_params = new HashMap<String, String>();
                    revert_params.put("type", "op");
                    revert_params.put("cmd", "<load><config><from>running-config.xml</from></config></load>");
                    requestWithPolling(PaloAltoMethod.GET, revert_params);

                    // REMOVE THE CONFIG LOCK TO RELEASE THE PALO ALTO RESOURCE
                    String remove_lock_status;
                    Map<String, String> remove_lock_params = new HashMap<String, String>();
                    remove_lock_params.put("type", "op");
                    remove_lock_params.put("cmd", "<request><config-lock><remove></remove></config-lock></request>");
                    String remove_lock_response = request(PaloAltoMethod.GET, remove_lock_params);
                    Document remove_lock_doc = getDocument(remove_lock_response);
                    XPath remove_lock_xpath = XPathFactory.newInstance().newXPath();
                    try {
                        XPathExpression expr = remove_lock_xpath.compile("/response[@status='success']/result/text()");
                        remove_lock_status = (String)expr.evaluate(remove_lock_doc, XPathConstants.STRING);
                    } catch (XPathExpressionException e) {
                        throw new ExecutionException(e.getCause().getMessage());
                    }
                    if (remove_lock_status.length() == 0) {
                        throw new ExecutionException("Could not release the Palo Alto device.  Please notify an administrator!");
                    }

                    throw ex; // Bubble up the reason we reverted...
                }

                return result;
            }
        } else {
            return true; // nothing to do
        }
    }

    /* A default response handler to validate that the request was successful. */
    public boolean validResponse(String response) throws ExecutionException {
        NodeList response_body;
        Document doc = getDocument(response);
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            XPathExpression expr = xpath.compile("/response[@status='success']");
            response_body = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new ExecutionException(e.getCause().getMessage());
        }

        if (response_body.getLength() > 0) {
            return true;
        } else {
            NodeList error_details;
            try {
                XPathExpression expr = xpath.compile("/response/msg/line/line");
                error_details = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                throw new ExecutionException(e.getCause().getMessage());
            }
            if (error_details.getLength() == 0) {
                try {
                    XPathExpression expr = xpath.compile("/response/msg/line");
                    error_details = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                } catch (XPathExpressionException e) {
                    throw new ExecutionException(e.getCause().getMessage());
                }

                if (error_details.getLength() == 0) {
                    try {
                        XPathExpression expr = xpath.compile("/response/result/msg");
                        error_details = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
                    } catch (XPathExpressionException e) {
                        throw new ExecutionException(e.getCause().getMessage());
                    }
                }
            }
            String error = "";
            for (int i = 0; i < error_details.getLength(); i++) {
                error = error + error_details.item(i).getTextContent() + "\n";
            }
            throw new ExecutionException(error);
        }
    }

    /* Validate that the response is not empty. */
    public boolean responseNotEmpty(String response) throws ExecutionException {
        NodeList response_body;
        Document doc = getDocument(response);
        XPath xpath = XPathFactory.newInstance().newXPath();
        try {
            XPathExpression expr = xpath.compile("/response[@status='success']");
            response_body = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new ExecutionException(e.getCause().getMessage());
        }

        if (response_body.getLength() > 0 &&
            (!response_body.item(0).getTextContent().equals("") || (response_body.item(0).hasChildNodes() && response_body.item(0).getFirstChild().hasChildNodes()))) {
            return true;
        } else {
            return false;
        }
    }

    /* Get the type of interface from the PA device. */
    private String getInterfaceType(String interfaceName) throws ExecutionException {
        String[] types = {InterfaceType.ETHERNET.toString(), InterfaceType.AGGREGATE.toString()};
        for (String type : types) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("type", "config");
            params.put("action", "get");
            params.put("xpath", "/config/devices/entry/network/interface/" + type + "/entry[@name='" + interfaceName + "']");
            String ethernet_response = request(PaloAltoMethod.GET, params);
            if (validResponse(ethernet_response) && responseNotEmpty(ethernet_response)) {
                return type;
            }
        }
        return "";
    }

    /* Get the threat profile from the server if it exists. */
    private boolean getThreatProfile(String profile) throws ExecutionException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("type", "config");
        params.put("action", "get");
        params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/profile-group/entry[@name='" + profile + "']");
        String response = request(PaloAltoMethod.GET, params);
        return (validResponse(response) && responseNotEmpty(response));
    }

    /* Get the log profile from the server if it exists. */
    private boolean getLogProfile(String profile) throws ExecutionException {
        Map<String, String> params = new HashMap<String, String>();
        params.put("type", "config");
        params.put("action", "get");
        params.put("xpath", "/config/devices/entry/vsys/entry[@name='vsys1']/log-settings/profiles/entry[@name='" + profile + "']");
        String response = request(PaloAltoMethod.GET, params);
        return (validResponse(response) && responseNotEmpty(response));
    }

    /* Command Interface */
    public interface IPaloAltoCommand {
        public boolean execute() throws ExecutionException;
    }

    /* Command Abstract */
    private abstract class AbstractPaloAltoCommand implements IPaloAltoCommand {
        PaloAltoMethod method;
        Map<String, String> params;

        public AbstractPaloAltoCommand() {
        }

        public AbstractPaloAltoCommand(PaloAltoMethod method, Map<String, String> params) {
            this.method = method;
            this.params = params;
        }

        @Override
        public boolean execute() throws ExecutionException {
            String response = request(method, params);
            return validResponse(response);
        }
    }

    /* Implement the default functionality */
    private class DefaultPaloAltoCommand extends AbstractPaloAltoCommand {
        public DefaultPaloAltoCommand(PaloAltoMethod method, Map<String, String> params) {
            super(method, params);
        }
    }

    /*
     * Misc
     */

    private String genIpIdentifier(String ip) {
        return ip.replace('.', '-').replace('/', '-');
    }

    private String parsePublicVlanTag(String uri) {
        return uri.replace("vlan://", "");
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
        StringReader xmlReader = new StringReader(xml);
        InputSource xmlSource = new InputSource(xmlReader);
        Document doc = null;

        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlSource);
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

    // return an xml node as a string
    private String nodeToString(Node node) throws ExecutionException {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (Throwable t) {
            throw new ExecutionException("XML convert error when modifying PA config: " + t.getMessage());
        }
        return sw.toString();
    }

    // pretty printing of xml strings
    private String prettyFormat(String input) {
        int indent = 4;
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Throwable e) {
            try {
                Source xmlInput = new StreamSource(new StringReader(input));
                StringWriter stringWriter = new StringWriter();
                StreamResult xmlOutput = new StreamResult(stringWriter);
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indent));
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(xmlInput, xmlOutput);
                return xmlOutput.getWriter().toString();
            } catch (Throwable t) {
                return input;
            }
        }
    }

    //@Override
    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub
    }

    //@Override
    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub
    }

    //@Override
    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    //@Override
    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    //@Override
    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub
    }
}
