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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
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
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
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
        CREATE_EDGE_ROUTE("create-edge-device-route.xml", "policy-mgr");


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
                    xml += line.replaceAll("\n"," ");
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
    	return getDnForTenantVDC(tenantName) + "/edsp-" + tenantName + "-Edge-Device-Profile";
    }
    
    private String getDnForEdgeDeviceRoutingPolicy(String tenantName) {
    	return getDnForTenantVDC(tenantName) + "/routing-policy-EDSP-" + tenantName + "-Routes";
    	//FIXME: any other construct is unreliable. why?
    }
    
    private String getDnForEdgeDeviceRoute(String tenantName, int id) {
    	return getDnForEdgeDeviceRoutingPolicy(tenantName) + "/sroute-" + id ;
    }
    
    
    public boolean createTenant(String tenantName) throws ExecutionException {
    	 String xml = VnmcXml.CREATE_TENANT.getXml();
         String service = VnmcXml.CREATE_TENANT.getService();
         xml = replaceXmlValue(xml, "cookie", _cookie);
         xml = replaceXmlValue(xml, "descr", "Tenant for account " + tenantName);
         xml = replaceXmlValue(xml, "name", tenantName);
         xml = replaceXmlValue(xml, "dn", getDnForTenant(tenantName));

         String response =  sendRequest(service, xml);
         Map<String, String> checked = checkResponse(response, "errorCode", "response");
         
         if (checked.get("errorCode") != null) {
        	 String errorCode = checked.get("errorCode");
        	 if (errorCode.equals("103")) {
        		 //tenant already exists
        		 return true;
        	 }
        	 return false;
         }
         return true;
    }
    
    public boolean createTenantVDC(String tenantName) throws ExecutionException {
   	 String xml = VnmcXml.CREATE_VDC.getXml();
        String service = VnmcXml.CREATE_VDC.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "VDC for Tenant" + tenantName);
        xml = replaceXmlValue(xml, "name", "VDC-" + tenantName);
        xml = replaceXmlValue(xml, "dn", getDnForTenantVDC(tenantName));

        String response =  sendRequest(service, xml);
        Map<String, String> checked = checkResponse(response, "errorCode", "response");
        
        if (checked.get("errorCode") != null) {
       	 String errorCode = checked.get("errorCode");
       	 if (errorCode.equals("103")) {
       		 //tenant already exists
       		 return true;
       	 }
       	 return false;
        }
        return true;
   }
    
    public boolean createTenantVDCEdgeDeviceProfile(String tenantName) throws ExecutionException {
      	 String xml = VnmcXml.CREATE_EDGE_DEVICE_PROFILE.getXml();
           String service = VnmcXml.CREATE_EDGE_DEVICE_PROFILE.getService();
           xml = replaceXmlValue(xml, "cookie", _cookie);
           xml = replaceXmlValue(xml, "descr", "Edge Device Profile for Tenant VDC" + tenantName);
           xml = replaceXmlValue(xml, "name", "EDSP-" + tenantName);
           xml = replaceXmlValue(xml, "dn", getDnForTenantVDCEdgeDeviceProfile(tenantName));

           String response =  sendRequest(service, xml);
           Map<String, String> checked = checkResponse(response, "errorCode", "response");
           
           if (checked.get("errorCode") != null) {
          	 String errorCode = checked.get("errorCode");
          	 if (errorCode.equals("103")) {
          		 //tenant already exists
          		 return true;
          	 }
          	 return false;
           }
           return true;
    }

    public boolean createTenantVDCEdgeStaticRoutePolicy(String tenantName) throws ExecutionException {
     	 String xml = VnmcXml.CREATE_EDGE_ROUTE_POLICY.getXml();
          String service = VnmcXml.CREATE_EDGE_ROUTE_POLICY.getService();
          xml = replaceXmlValue(xml, "cookie", _cookie);
          xml = replaceXmlValue(xml, "name", "EDSP-" + tenantName + "-Routes");//FIXME: this has to match DN somehow?
          xml = replaceXmlValue(xml, "routepolicydn", getDnForEdgeDeviceRoutingPolicy(tenantName));
          xml = replaceXmlValue(xml, "descr", "Routing Policy for Edge Device for Tenant " + tenantName);


          String response =  sendRequest(service, xml);
          Map<String, String> checked = checkResponse(response, "errorCode", "response");
          
          if (checked.get("errorCode") != null) {
         	 String errorCode = checked.get("errorCode");
         	 if (errorCode.equals("103")) {
         		 //already exists
         		 return true;
         	 }
         	 return false;
          }
          return true;
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
         Map<String, String> checked = checkResponse(response, "errorCode", "response");
         
         if (checked.get("errorCode") != null) {
        	 String errorCode = checked.get("errorCode");
        	 if (errorCode.equals("103")) {
        		 //tenant already exists
        		 return true;
        	 }
        	 return false;
         }
         return true;
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
