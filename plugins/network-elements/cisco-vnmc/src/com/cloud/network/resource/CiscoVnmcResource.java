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
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.Element;
import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.PostMethod;
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
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.script.Script;

public class CiscoVnmcResource implements ServerResource {

    private String _name;
    private String _zoneId;
    private String _physicalNetworkId;
    private String _ip;
    private String _username;
    private String _password;
    private String _guid;
    private String _objectNameWordSep;
    private Integer _numRetries;
    private String _publicZone;
    private String _privateZone;
    private String _publicInterface;
    private String _privateInterface;
	private String _cookie;

    
    private String _primaryDnsAddress;

   
    private final Logger s_logger = Logger.getLogger(CiscoVnmcResource.class);

    private enum VnmcXml {
        LOGIN("login.xml", "mgmt-controller"),
        CREATE_TENANT("create-tenant.xml", "service-reg"),
        CREATE_VDC("create-vdc.xml", "service-reg"),
        CREATE_EDGE_DEVICE_PROFILE("create-edge-device-profile.xml", "policy-mgr"),
        CREATE_EDGE_ROUTE_POLICY("create-edge-device-route-policy.xml", "policy-mgr"),
        CREATE_EDGE_ROUTE("create-edge-device-route.xml", "policy-mgr"),
        RESOLVE_EDGE_ROUTE_POLICY("associate-route-policy.xml", "policy-mgr"),
        RESOLVE_EDGE_DHCP_POLICY("associate-dhcp-policy.xml", "policy-mgr"),
        CREATE_DHCP_POLICY("create-dhcp-policy.xml", "policy-mgr"),
        RESOLVE_EDGE_DHCP_SERVER_POLICY("associate-dhcp-server.xml", "policy-mgr"),
        CREATE_EDGE_SECURITY_PROFILE("create-edge-security-profile.xml", "policy-mgr"),
        CREATE_SOURCE_NAT_POOL("create-source-nat-pool.xml", "policy-mgr"),
        CREATE_SOURCE_NAT_POLICY("create-source-nat-policy.xml", "policy-mgr"),
        CREATE_NAT_POLICY_SET("create-nat-policy-set.xml", "policy-mgr"),
        RESOLVE_NAT_POLICY_SET("associate-nat-policy-set.xml", "policy-mgr"),
        CREATE_EDGE_FIREWALL("create-edge-firewall.xml", "resource-mgr"),
        LIST_UNASSOC_ASA1000V("list-unassigned-asa1000v.xml", "resource-mgr"),
        ASSIGN_ASA1000V("assoc-asa1000v.xml", "resource-mgr");

        private String scriptsDir = "scripts/network/cisco";
        private String xml;
        private String service;
        private final Logger s_logger = Logger.getLogger(CiscoVnmcResource.class);

        private VnmcXml(String filename, String service) {
            this.xml = getXml(filename);
            this.service = service;
        }

        public String getXml() {
            return xml;
        }

        private String getXml(String filename) {
            try {
                String xmlFilePath = Script.findScript(scriptsDir, filename);

                if (xmlFilePath == null) {
                    throw new Exception("Failed to find Cisco VNMC XML file: " + filename);
                }

                FileReader fr = new FileReader(xmlFilePath);
                BufferedReader br = new BufferedReader(fr);

                String xml = "";
                String line;
                while ((line = br.readLine()) != null) {
                    //xml += line.replaceAll("\n"," ");
                	xml += line;
                }

                return xml;
            } catch (Exception e) {
                s_logger.debug(e);
                return null;
            }
        }

		public String getService() {
			return service;
		}
    }	
    

    
    public CiscoVnmcResource(String ip, String username, String password) {
    	_ip = ip;
    	_username = username;
    	_password = password;
    }

    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand) cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand) cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand) cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            return execute((SetStaticNatRulesCommand) cmd);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            return execute((SetPortForwardingRulesCommand) cmd);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand) cmd);
        }  else {
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
                throw new ConfigurationException("Unable to find public interface.");
            }


            _privateInterface = (String) params.get("privateinterface");
            if (_privateInterface == null) {
                throw new ConfigurationException("Unable to find private interface.");
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

            _objectNameWordSep = "-";
       
            _primaryDnsAddress = "4.2.2.2";

            // Open a socket and login
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
        return new StartupCommand[]{cmd};
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
        return true;
    }

    public boolean login() throws ExecutionException {
        String xml = VnmcXml.LOGIN.getXml();
        String service = VnmcXml.LOGIN.getService();
        xml = replaceXmlValue(xml, "username", _username);
        xml = replaceXmlValue(xml, "password", _password);
        String response =  sendRequest(service, xml);
        Map<String, String> checked = checkResponse(response, "outCookie", "errorCode", "response");
        
        if (checked.get("errorCode") != null)
        	return false;
        _cookie = checked.get("outCookie");
        if (_cookie == null) {
        	return false;
        }
        return true;
    }
    
    private String getDnForTenant(String tenantName) {
    	return "org-root/org-" + tenantName;
    }
    
    private String getDnForTenantVDC(String tenantName) {
    	return getDnForTenant(tenantName) + "/org-VDC-" + tenantName;
    }
    
    private String getDnForTenantVDCEdgeDeviceProfile(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/edsp-" + getNameForEdgeDeviceServiceProfile(tenantName);
    }
    
    private String getDnForTenantVDCEdgeSecurityProfile(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/vnep-" + getNameForEdgeDeviceSecurityProfile(tenantName);
    }
    
    private String getDnForEdgeDeviceRoutingPolicy(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/routing-policy-" + getNameForEdgeDeviceRoutePolicy(tenantName);
    	//FIXME: any other construct is unreliable. why?
    }
    
    private String getDnForEdgeDeviceRoute(String tenantName, int id) {
    	return getDnForEdgeDeviceRoutingPolicy(tenantName) + "/sroute-" + id ;
    }
    
    private String getDnForDhcpPolicy(String tenantName, String intfName) {
    	return getDnForTenantVDCEdgeDeviceProfile(tenantName) + "/dhcp-" + intfName;
    }
    
    private String getNameForDhcpPolicy(String tenantName) {
    	return tenantName + "-Dhcp-Policy";
    }
    
    private String getNameForDhcpServer(String tenantName) {
    	return tenantName + "-Dhcp-Server";
    }
    
    private String getDnForDhcpServerPolicy(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/dhcp-server-" + getNameForDhcpPolicy(tenantName);
    }
    
    private String getNameForIpRange() {
    	return "iprange";
    }
    
    private String getDnForDhcpIpRange(String tenantName) {
    	return getDnForDhcpServerPolicy(tenantName) + "/ip-range-" + getNameForIpRange();
    }
    
    private String getNameForDNSService(String tenantName) {
    	return tenantName + "-DNS";
    }
   
    
    private String getDnForDnsService(String tenantName) {
    	return getDnForDhcpServerPolicy(tenantName) + "/dns-svc-" + getNameForDNSService(tenantName); 
    }
    
    private String getDnForDnsServer(String tenantName, String dnsip) {
    	return getDnForDnsService(tenantName) + "/dns-" + dnsip; 
    }
    
    private String getNameForTenantVDC(String tenantName) {
    	return "VDC-" + tenantName;
    }
    
    private String getNameForEdgeDeviceServiceProfile(String tenantName) {
    	return "EDSP-" + tenantName;
    }
    
    private String getNameForEdgeDeviceSecurityProfile(String tenantName) {
    	return "ESP-" + tenantName;
    }
    
    private String getNameForEdgeDeviceRoutePolicy(String tenantName) {
    	return "EDSP-" + tenantName + "-Routes";//FIXME: this has to match DN somehow?
    }
        
    public boolean createTenant(String tenantName) throws ExecutionException {
    	 String xml = VnmcXml.CREATE_TENANT.getXml();
         String service = VnmcXml.CREATE_TENANT.getService();
         xml = replaceXmlValue(xml, "cookie", _cookie);
         xml = replaceXmlValue(xml, "descr", "Tenant for account " + tenantName);
         xml = replaceXmlValue(xml, "name", tenantName);
         xml = replaceXmlValue(xml, "dn", getDnForTenant(tenantName));

         String response =  sendRequest(service, xml);
         return verifySuccess(response);
    }
    
    public boolean createTenantVDC(String tenantName) throws ExecutionException {
   	 String xml = VnmcXml.CREATE_VDC.getXml();
        String service = VnmcXml.CREATE_VDC.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "VDC for Tenant" + tenantName);
        xml = replaceXmlValue(xml, "name", getNameForTenantVDC(tenantName));
        xml = replaceXmlValue(xml, "dn", getDnForTenantVDC(tenantName));

        String response =  sendRequest(service, xml);
        
        return verifySuccess(response);
   }
    
    public boolean createTenantVDCEdgeDeviceProfile(String tenantName) throws ExecutionException {
      	 String xml = VnmcXml.CREATE_EDGE_DEVICE_PROFILE.getXml();
           String service = VnmcXml.CREATE_EDGE_DEVICE_PROFILE.getService();
           xml = replaceXmlValue(xml, "cookie", _cookie);
           xml = replaceXmlValue(xml, "descr", "Edge Device Profile for Tenant VDC" + tenantName);
           xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceServiceProfile(tenantName));
           xml = replaceXmlValue(xml, "dn", getDnForTenantVDCEdgeDeviceProfile(tenantName));

           String response =  sendRequest(service, xml);
           
           return verifySuccess(response);
    }

    public boolean createTenantVDCEdgeStaticRoutePolicy(String tenantName) throws ExecutionException {
     	 String xml = VnmcXml.CREATE_EDGE_ROUTE_POLICY.getXml();
          String service = VnmcXml.CREATE_EDGE_ROUTE_POLICY.getService();
          xml = replaceXmlValue(xml, "cookie", _cookie);
          xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceRoutePolicy(tenantName));//FIXME: this has to match DN somehow?
          xml = replaceXmlValue(xml, "routepolicydn", getDnForEdgeDeviceRoutingPolicy(tenantName));
          xml = replaceXmlValue(xml, "descr", "Routing Policy for Edge Device for Tenant " + tenantName);


          String response =  sendRequest(service, xml);
          return verifySuccess(response);
   }
    
    public boolean createTenantVDCEdgeStaticRoute(String tenantName, 
    		String nextHopIp, String outsideIntf,
    		String destination, String netmask) throws ExecutionException {
    	 String xml = VnmcXml.CREATE_EDGE_ROUTE.getXml();
         String service = VnmcXml.CREATE_EDGE_ROUTE.getService();
         xml = replaceXmlValue(xml, "cookie", _cookie);
         xml = replaceXmlValue(xml, "routedn", getDnForEdgeDeviceRoute(tenantName, 2));//TODO: why 2?
         xml = replaceXmlValue(xml, "id", "2"); // TODO:2?
         xml = replaceXmlValue(xml, "nexthop", nextHopIp);
         xml = replaceXmlValue(xml, "nexthopintf", outsideIntf);
         xml = replaceXmlValue(xml, "destination", destination);
         xml = replaceXmlValue(xml, "netmask", netmask);

         //TODO: this adds default route, make it more generic

         String response =  sendRequest(service, xml);
         return verifySuccess(response);
    }
    
    public boolean associateTenantVDCEdgeStaticRoutePolicy(String tenantName) throws ExecutionException {
    	 String xml = VnmcXml.RESOLVE_EDGE_ROUTE_POLICY.getXml();
         String service = VnmcXml.RESOLVE_EDGE_ROUTE_POLICY.getService();
         xml = replaceXmlValue(xml, "cookie", _cookie);
         xml = replaceXmlValue(xml, "profilename", getNameForEdgeDeviceServiceProfile(tenantName));
         xml = replaceXmlValue(xml, "profiledn", getDnForTenantVDC(tenantName) + "/edsp-" + getNameForEdgeDeviceServiceProfile(tenantName));
         xml = replaceXmlValue(xml, "routepolicyname", getNameForEdgeDeviceRoutePolicy(tenantName));

         String response =  sendRequest(service, xml);
         return verifySuccess(response);
    }
    
    public boolean associateTenantVDCEdgeDhcpPolicy(String tenantName, String intfName) throws ExecutionException {
   	 String xml = VnmcXml.RESOLVE_EDGE_DHCP_POLICY.getXml();
        String service = VnmcXml.RESOLVE_EDGE_DHCP_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "dhcpdn", getDnForDhcpPolicy(tenantName, intfName));
        xml = replaceXmlValue(xml, "insideintf", intfName);

        String response =  sendRequest(service, xml);
        
        return verifySuccess(response);
    }
    
    public boolean createTenantVDCEdgeDhcpPolicy(String tenantName, 
    		String startIp, String endIp, String subnet, String nameServerIp, String domain) throws ExecutionException {
    	String xml = VnmcXml.CREATE_DHCP_POLICY.getXml();
    	String service = VnmcXml.CREATE_DHCP_POLICY.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	xml = replaceXmlValue(xml, "dhcpserverdn", getDnForDhcpServerPolicy(tenantName));
    	xml = replaceXmlValue(xml, "dhcpserverdescr", "DHCP server for " + tenantName);
    	xml = replaceXmlValue(xml, "dhcpservername", getNameForDhcpPolicy(tenantName));
    	xml = replaceXmlValue(xml, "iprangedn", getDnForDhcpIpRange(tenantName));
    	xml = replaceXmlValue(xml, "startip", startIp);
    	xml = replaceXmlValue(xml, "endip", endIp);
    	xml = replaceXmlValue(xml, "subnet", subnet);
    	xml = replaceXmlValue(xml, "domain", domain);
    	xml = replaceXmlValue(xml, "dnsservicedn", getDnForDnsService(tenantName));
    	xml = replaceXmlValue(xml, "dnsservicename", getNameForDNSService(tenantName));
    	xml = replaceXmlValue(xml, "nameserverip", nameServerIp);
    	xml = replaceXmlValue(xml, "nameserverdn", getDnForDnsServer(tenantName, nameServerIp));

    	String response =  sendRequest(service, xml);
    	return verifySuccess(response);
    }
    
    public boolean associateTenantVDCEdgeDhcpServerPolicy(String tenantName, String intfName) throws ExecutionException {
      	 String xml = VnmcXml.RESOLVE_EDGE_DHCP_SERVER_POLICY.getXml();
           String service = VnmcXml.RESOLVE_EDGE_DHCP_SERVER_POLICY.getService();
           xml = replaceXmlValue(xml, "cookie", _cookie);
           xml = replaceXmlValue(xml, "dhcpdn", getDnForDhcpPolicy(tenantName, intfName));
           xml = replaceXmlValue(xml, "insideintf", intfName);
           xml = replaceXmlValue(xml, "dhcpserverpolicyname", getNameForDhcpServer(tenantName));

           String response =  sendRequest(service, xml);
           return verifySuccess(response);
    }
    
    public boolean createTenantVDCEdgeSecurityProfile(String tenantName) throws ExecutionException {
    	String xml = VnmcXml.CREATE_EDGE_SECURITY_PROFILE.getXml();
    	String service = VnmcXml.CREATE_EDGE_SECURITY_PROFILE.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	xml = replaceXmlValue(xml, "descr", "Edge Security Profile for Tenant VDC" + tenantName);
    	xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceSecurityProfile(tenantName));
    	xml = replaceXmlValue(xml, "espdn", getDnForTenantVDCEdgeSecurityProfile(tenantName));
    	xml = replaceXmlValue(xml, "egressref", "default-egress");
    	xml = replaceXmlValue(xml, "ingressref", "default-ingress"); //FIXME: allows everything

    	String response =  sendRequest(service, xml);

    	return verifySuccess(response);
   }
    
    private String getNameForSourceNatPool(String tenantName) {
    	return "Source-NAT-Pool-For-" + tenantName;
    }
    
    private String getDnForSourceNatPool(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/objgrp-" + getNameForSourceNatPool(tenantName);
    }
    
    private String getDnForSourceNatPoolExpr(String tenantName) {
    	return getDnForSourceNatPool(tenantName) + "/objgrp-expr-2";
    }
    
    private String getDnForSourceNatPublicIp(String tenantName) {
    	return getDnForSourceNatPoolExpr(tenantName) + "/nw-ip-2";
    }
    
    public boolean createTenantVDCSourceNATPool(String tenantName, String publicIp) throws ExecutionException {
    	String xml = VnmcXml.CREATE_SOURCE_NAT_POOL.getXml();
    	String service = VnmcXml.CREATE_SOURCE_NAT_POOL.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	xml = replaceXmlValue(xml, "descr", "Source NAT pool for Tenant VDC " + tenantName);
    	xml = replaceXmlValue(xml, "name", getNameForSourceNatPool(tenantName));
    	xml = replaceXmlValue(xml, "snatpooldn", getDnForSourceNatPool(tenantName));
    	xml = replaceXmlValue(xml, "snatpoolexprdn", getDnForSourceNatPoolExpr(tenantName));
    	xml = replaceXmlValue(xml, "publicipdn", getDnForSourceNatPublicIp(tenantName));
    	xml = replaceXmlValue(xml, "publicip", publicIp);

    	String response =  sendRequest(service, xml);

    	return verifySuccess(response);
    }
    
    
    private String getNameForSourceNatPolicy(String tenantName) {
       return "Source-NAT-For-" + tenantName;	
    }
    
    private String getDnForSourceNatPolicy(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/natpol-" + getNameForSourceNatPolicy(tenantName);
    }
    
    private String getNameForSourceNatRule(String tenantName) {
    	return "Source-NAT-Policy-Rule-" + tenantName;
    }
    
    private String getDnForSourceNatRule(String tenantName) {
    	return getDnForSourceNatPolicy(tenantName) + "/rule-" + getNameForSourceNatRule(tenantName);
    }
    
    private String getDnForSourceNatRuleAction(String tenantName) {
    	return getDnForSourceNatRule(tenantName) + "/nat-action";
    }
    
    private String getDnForSourceNatRuleRule(String tenantName) {
    	return getDnForSourceNatRule(tenantName) + "/rule-cond-2";
    }
    
    private String getDnForSourceNatRuleRange(String tenantName) {
    	return getDnForSourceNatRuleRule(tenantName) + "/nw-expr2";
    }
    
    private String getDnForSourceNatRuleRangeIp(String tenantName, int id) {
    	return getDnForSourceNatRuleRange(tenantName) + "/nw-ip-" + id;
    }
    
    private String getDnForSourceNatRuleRangeAttr(String tenantName) {
    	return getDnForSourceNatRuleRange(tenantName) + "/nw-attr-qual";
    }
    
    public boolean createTenantVDCSourceNATPolicy(String tenantName, 
    		String startSourceIp, String endSourceIp) throws ExecutionException {
    	
    	String xml = VnmcXml.CREATE_SOURCE_NAT_POLICY.getXml();
    	String service = VnmcXml.CREATE_SOURCE_NAT_POLICY.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	xml = replaceXmlValue(xml, "descr", "Source NAT Policy for Tenant VDC " + tenantName);
    	xml = replaceXmlValue(xml, "srcTranslatedIpPool", getNameForSourceNatPool(tenantName));
    	xml = replaceXmlValue(xml, "natrulename", getNameForSourceNatRule(tenantName));
    	xml = replaceXmlValue(xml, "natpolname", getNameForSourceNatPolicy(tenantName));
    	xml = replaceXmlValue(xml, "natruleruledescr", "Source NAT Policy for Tenant " + tenantName);
    	xml = replaceXmlValue(xml, "natpoldescr", "Source NAT Rule for Tenant " + tenantName);
    	xml = replaceXmlValue(xml, "natpoldn", getDnForSourceNatPolicy(tenantName));
    	xml = replaceXmlValue(xml, "natruledn", getDnForSourceNatRule(tenantName));
    	xml = replaceXmlValue(xml, "sourcestartip", startSourceIp);
    	xml = replaceXmlValue(xml, "sourceendip", endSourceIp);
    	xml = replaceXmlValue(xml, "sourcenatpoolname", getNameForSourceNatPool(tenantName));

    	
    	xml = replaceXmlValue(xml, "natactiondn", getDnForSourceNatRuleAction(tenantName));
    	xml = replaceXmlValue(xml, "natruleruledn", getDnForSourceNatRuleRule(tenantName));
    	xml = replaceXmlValue(xml, "natrangedn", getDnForSourceNatRuleRange(tenantName));
    	xml = replaceXmlValue(xml, "natipdn2", getDnForSourceNatRuleRangeIp(tenantName, 2));
    	xml = replaceXmlValue(xml, "natipdn3", getDnForSourceNatRuleRangeIp(tenantName, 3));

    	xml = replaceXmlValue(xml, "natsnatruleconddn", getDnForSourceNatRuleRangeAttr(tenantName));

    	String response =  sendRequest(service, xml);

    	return verifySuccess(response);
    }
    
    private String getNameForNatPolicySet(String tenantName) {
    	return "NAT-Policy-Set-" + tenantName;
    }

    private String getDnForNatPolicySet(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/natpset-" + getNameForNatPolicySet(tenantName) ;
    }
    
    private String getDnForNatPolicySetRef(String tenantName) {
    	return getDnForNatPolicySet(tenantName) + "/polref-" + getNameForSourceNatPolicy(tenantName) ;
    }
    
    public boolean createTenantVDCNatPolicySet(String tenantName) throws ExecutionException {
    	String xml = VnmcXml.CREATE_NAT_POLICY_SET.getXml();
    	String service = VnmcXml.CREATE_NAT_POLICY_SET.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	//xml = replaceXmlValue(xml, "descr", "Nat Policy Set for Tenant VDC " + tenantName);
    	xml = replaceXmlValue(xml, "natpolicyname", getNameForSourceNatPolicy(tenantName));
    	xml = replaceXmlValue(xml, "natpolicysetname", getNameForNatPolicySet(tenantName));
    	xml = replaceXmlValue(xml, "natpolicysetdn", getDnForNatPolicySet(tenantName));
    	xml = replaceXmlValue(xml, "natpolicyrefdn", getDnForNatPolicySetRef(tenantName));

    	String response =  sendRequest(service, xml);

    	return verifySuccess(response);
    }
    
    public boolean associateNatPolicySet(String tenantName) throws ExecutionException {
    	String xml = VnmcXml.RESOLVE_NAT_POLICY_SET.getXml();
    	String service = VnmcXml.RESOLVE_NAT_POLICY_SET.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	xml = replaceXmlValue(xml, "descr", "Edge Security Profile for Tenant VDC" + tenantName);
    	xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceSecurityProfile(tenantName));
    	xml = replaceXmlValue(xml, "espdn", getDnForTenantVDCEdgeSecurityProfile(tenantName));
    	xml = replaceXmlValue(xml, "egressref", "default-egress");
    	xml = replaceXmlValue(xml, "ingressref", "default-ingress");
    	xml = replaceXmlValue(xml, "natpolicysetname", getNameForNatPolicySet(tenantName));

    	String response =  sendRequest(service, xml);

    	return verifySuccess(response);
    }
    
    private String getNameForEdgeFirewall(String tenantName) {
    	return "ASA-1000v-" + tenantName;
    }
    
    private String getDnForEdgeFirewall(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/efw-" + getNameForEdgeFirewall(tenantName);
    }
    
    private String getNameForEdgeInsideIntf(String tenantName) {
    	return "Edge_Inside";
    }
    
    private String getNameForEdgeOutsideIntf(String tenantName) {
    	return "Edge_Outside";
    }
    
    private String getDnForOutsideIntf(String tenantName) {
    	return getDnForEdgeFirewall(tenantName) + "/interface-" + getNameForEdgeOutsideIntf(tenantName);
    }
    
    private String getDnForInsideIntf(String tenantName) {
    	return getDnForEdgeFirewall(tenantName) + "/interface-" + getNameForEdgeInsideIntf(tenantName);
    }
    
    public boolean createEdgeFirewall(String tenantName, String publicIp, String insideIp, 
    		String insideSubnet, String outsideSubnet) throws ExecutionException {
    	
    	String xml = VnmcXml.CREATE_EDGE_FIREWALL.getXml();
    	String service = VnmcXml.CREATE_EDGE_FIREWALL.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	xml = replaceXmlValue(xml, "edgefwdescr", "Edge Firewall for Tenant VDC " + tenantName);
    	xml = replaceXmlValue(xml, "edgefwname", getNameForEdgeFirewall(tenantName));
    	xml = replaceXmlValue(xml, "edgefwdn", getDnForEdgeFirewall(tenantName));
    	xml = replaceXmlValue(xml, "insideintfname", getNameForEdgeInsideIntf(tenantName));
    	xml = replaceXmlValue(xml, "outsideintfname", getNameForEdgeOutsideIntf(tenantName));

    	xml = replaceXmlValue(xml, "insideintfdn", getDnForInsideIntf(tenantName));
    	xml = replaceXmlValue(xml, "outsideintfdn", getDnForOutsideIntf(tenantName));

    	xml = replaceXmlValue(xml, "deviceserviceprofiledn", getDnForEdgeFirewall(tenantName) + "/device-service-profile");
    	xml = replaceXmlValue(xml, "outsideintfsp", getDnForOutsideIntf(tenantName)  + "/interface-service-profile");

    	xml = replaceXmlValue(xml, "secprofileref", getNameForEdgeDeviceSecurityProfile(tenantName));
    	xml = replaceXmlValue(xml, "deviceserviceprofile", getNameForEdgeDeviceServiceProfile(tenantName));


    	xml = replaceXmlValue(xml, "insideip", insideIp);
    	xml = replaceXmlValue(xml, "publicip", publicIp);
    	xml = replaceXmlValue(xml, "insidesubnet", insideSubnet);
    	xml = replaceXmlValue(xml, "outsidesubnet", outsideSubnet);
    	
    	String response =  sendRequest(service, xml);

    	return verifySuccess(response);

    }
    
    
    public List<String> listUnAssocAsa1000v() throws ExecutionException {
    	
    	String xml = VnmcXml.LIST_UNASSOC_ASA1000V.getXml();
    	String service = VnmcXml.LIST_UNASSOC_ASA1000V.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	
    	
    	String response =  sendRequest(service, xml);

    	List<String> result = new ArrayList<String>();
    	
    	Document xmlDoc = getDocument(response);
    	xmlDoc.normalize();
    	NodeList fwList = xmlDoc.getElementsByTagName("fwInstance");
    	for (int j=0; j < fwList.getLength(); j++) {
			Node fwNode = fwList.item(j);
			result.add (fwNode.getAttributes().getNamedItem("dn").getNodeValue());
			
		}
        
        return result;

    }
    
    public boolean assocAsa1000v(String tenantName, String firewallDn) throws ExecutionException {
    	
    	String xml = VnmcXml.ASSIGN_ASA1000V.getXml();
    	String service = VnmcXml.ASSIGN_ASA1000V.getService();
    	xml = replaceXmlValue(xml, "cookie", _cookie);
    	xml = replaceXmlValue(xml, "binddn", getDnForEdgeFirewall(tenantName) + "/binding");
    	xml = replaceXmlValue(xml, "fwdn", firewallDn);
    	
    	String response =  sendRequest(service, xml);

    	return verifySuccess(response);

    }
    

    private String sendRequest(String service, String xmlRequest) throws ExecutionException {
    	org.apache.commons.httpclient.protocol.Protocol myhttps = 
    			new org.apache.commons.httpclient.protocol.Protocol("https", new EasySSLProtocolSocketFactory(), 443);
    	HttpClient client = new HttpClient();
    	client.getHostConfiguration().setHost(_ip, 443, myhttps);
    	byte[] response = null;
    	PostMethod method = new PostMethod("/xmlIM/" + service);
    	
    	method.setRequestBody(xmlRequest);
    	
    	try{
    	    int statusCode = client.executeMethod(method);
    	             
    	    if (statusCode != HttpStatus.SC_OK) {
    	        throw new Exception("Error code : " + statusCode);
    	    }
    	    response = method.getResponseBody();
    	}catch(Exception e){
    	    System.out.println(e.getMessage());
    	    throw new ExecutionException(e.getMessage());
    	}
    	System.out.println(new String(response));
    	return new String(response);
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
     * Static NAT
     */

    private synchronized Answer execute(SetStaticNatRulesCommand cmd) {
    	refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }       

    private Answer execute(SetStaticNatRulesCommand cmd, int numRetries) {      
        
            return new Answer(cmd);
        
    }

    
    
   
    

    /*
     * Destination NAT
     */

    private synchronized Answer execute (SetPortForwardingRulesCommand cmd) {
    	refreshVnmcConnection();
        return execute(cmd, _numRetries);
    }

    private Answer execute(SetPortForwardingRulesCommand cmd, int numRetries) {     
        
            return new Answer(cmd);
        
    }

    /*
     * XML API commands
     */
    
    private Map<String, String> checkResponse(String xmlResponse, String... keys) throws ExecutionException {
        Document xmlDoc = getDocument(xmlResponse);
        Map<String, String> result = new HashMap<String, String>();
        Node topElement = xmlDoc.getChildNodes().item(0);
        if (topElement != null) {
        	for (String key: keys){
        		Node valueNode = topElement.getAttributes().getNamedItem(key);
        		result.put(key, valueNode==null?null:valueNode.getNodeValue());
        	}
        }
        return result;
    }

    private boolean verifySuccess(String xmlResponse) throws ExecutionException {                                                                                                                                                                                                   
    	Map<String, String> checked = checkResponse(xmlResponse, "errorCode", "errorDescr");

    	if (checked.get("errorCode") != null) {
    		String errorCode = checked.get("errorCode");
    		if (errorCode.equals("103")) {
    			//tenant already exists
    			return true;
    		}
    		String errorDescr = checked.get("errorDescr");
    		throw new ExecutionException(errorDescr);
    	}
    	return true;
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

    

    /*
     * Misc
     */    
    
    private Long getVlanTag(String vlan) throws ExecutionException {
    	Long publicVlanTag = null;
    	if (!vlan.equals("untagged")) {
    		try {
    			publicVlanTag = Long.parseLong(vlan);
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
            if (i != args.length -1) {
                objectName += _objectNameWordSep;
            }
        }

        return objectName;			
    }




    private Document getDocument(String xml) throws ExecutionException {
        StringReader xmlReader = new StringReader("<?xml version=\"1.0\"?> \n" + xml.trim());
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
    
}
