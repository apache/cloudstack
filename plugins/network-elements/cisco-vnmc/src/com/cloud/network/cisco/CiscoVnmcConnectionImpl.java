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
package com.cloud.network.cisco;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.script.Script;

public class CiscoVnmcConnectionImpl implements CiscoVnmcConnection {

    private String _ip;
    private String _username;
    private String _password;
    private String _cookie;

    private final Logger s_logger = Logger.getLogger(CiscoVnmcConnectionImpl.class);

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

        CREATE_NAT_POLICY_SET("create-nat-policy-set.xml", "policy-mgr"),
        RESOLVE_NAT_POLICY_SET("associate-nat-policy-set.xml", "policy-mgr"),
        CREATE_NAT_POLICY("create-nat-policy.xml", "policy-mgr"),
        DELETE_NAT_POLICY("delete-nat-policy.xml", "policy-mgr"),
        LIST_NAT_POLICIES("list-nat-policies.xml", "policy-mgr"),
        CREATE_NAT_POLICY_REF("create-nat-policy-ref.xml", "policy-mgr"),
        CREATE_PORT_POOL("create-port-pool.xml", "policy-mgr"),
        CREATE_IP_POOL("create-ip-pool.xml", "policy-mgr"),

        CREATE_DNAT_RULE("create-dnat-rule.xml", "policy-mgr"),
        CREATE_SOURCE_NAT_RULE("create-source-nat-rule.xml", "policy-mgr"),

        CREATE_ACL_POLICY_SET("create-acl-policy-set.xml", "policy-mgr"),
        RESOLVE_ACL_POLICY_SET("associate-acl-policy-set.xml", "policy-mgr"),
        CREATE_ACL_POLICY("create-acl-policy.xml", "policy-mgr"),
        DELETE_ACL_POLICY("delete-acl-policy.xml", "policy-mgr"),
        CREATE_ACL_POLICY_REF("create-acl-policy-ref.xml", "policy-mgr"),
        CREATE_INGRESS_ACL_RULE("create-ingress-acl-rule.xml", "policy-mgr"),
        DELETE_ACL_RULE("delete-acl-rule.xml", "policy-mgr"),

        CREATE_EDGE_FIREWALL("create-edge-firewall.xml", "resource-mgr"),
        LIST_UNASSOC_ASA1000V("list-unassigned-asa1000v.xml", "resource-mgr"),
        ASSIGN_ASA1000V("assoc-asa1000v.xml", "resource-mgr");

        private String scriptsDir = "scripts/network/cisco";
        private String xml;
        private String service;
        private final Logger s_logger = Logger.getLogger(CiscoVnmcConnectionImpl.class);


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

    public CiscoVnmcConnectionImpl(String hostIp, String userName, String password) {
        this._ip = hostIp;
        this._username = userName;
        this._password = password;
        
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenant(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDC(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCEdgeDeviceProfile(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCEdgeStaticRoutePolicy(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCEdgeStaticRoute(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#associateTenantVDCEdgeStaticRoutePolicy(java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#associateTenantVDCEdgeDhcpPolicy(java.lang.String, java.lang.String)
     */
    @Override
    public boolean associateTenantVDCEdgeDhcpPolicy(String tenantName, String intfName) throws ExecutionException {
        String xml = VnmcXml.RESOLVE_EDGE_DHCP_POLICY.getXml();
        String service = VnmcXml.RESOLVE_EDGE_DHCP_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "dhcpdn", getDnForDhcpPolicy(tenantName, intfName));
        xml = replaceXmlValue(xml, "insideintf", intfName);

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCEdgeDhcpPolicy(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#associateTenantVDCEdgeDhcpServerPolicy(java.lang.String, java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCEdgeSecurityProfile(java.lang.String)
     */
    @Override
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

    private String getNameForSourceNatIpPool(String tenantName) {
        return "SNAT-Ip-" + tenantName;
    }

    private String getDnForSourceNatPool(String tenantName) {
        return getDnForTenantVDC(tenantName) + "/objgrp-" + getNameForSourceNatIpPool(tenantName);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCSourceNatIpPool(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean createTenantVDCSourceNatIpPool(String tenantName, String identifier,
            String publicIp) throws ExecutionException {
        return createTenantVDCIpPool(getDnForSourceNatPool(tenantName),
                getNameForSourceNatIpPool(tenantName),
                "Source NAT ip pool for Tenant VDC " + tenantName,
                publicIp);
    }

    private String getNameForSourceNatPolicy(String tenantName) {
       return "SNAT-Policy-" + tenantName;
    }

    private String getDnForSourceNatPolicy(String tenantName) {
        return getDnForTenantVDC(tenantName) + "/natpol-" + getNameForSourceNatPolicy(tenantName);
    }

    private String getNameForSourceNatRule(String tenantName) {
        return "SNAT-Rule-" + tenantName;
    }

    private String getDnForSourceNatRule(String tenantName) {
        return getDnForSourceNatPolicy(tenantName) + "/rule-" + getNameForSourceNatRule(tenantName);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCSourceNatRule(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean createTenantVDCSourceNatRule(String tenantName, String identifier,
            String startSourceIp, String endSourceIp) throws ExecutionException {

        String xml = VnmcXml.CREATE_SOURCE_NAT_RULE.getXml();
        String service = VnmcXml.CREATE_SOURCE_NAT_RULE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natruledn", getDnForSourceNatRule(tenantName));
        xml = replaceXmlValue(xml, "natrulename", getNameForSourceNatRule(tenantName));
        xml = replaceXmlValue(xml, "descr", "Source NAT rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "srcstartip", startSourceIp);
        xml = replaceXmlValue(xml, "srcendip", endSourceIp);
        xml = replaceXmlValue(xml, "ippoolname", getNameForSourceNatIpPool(tenantName));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    private String getNameForNatPolicySet(String tenantName) {
        return "NAT-PolicySet-" + tenantName;
    }

    private String getDnForNatPolicySet(String tenantName) {
        return getDnForTenantVDC(tenantName) + "/natpset-" + getNameForNatPolicySet(tenantName) ;
    }

    private String getDnForSourceNatPolicyRef(String tenantName) {
        return getDnForNatPolicySet(tenantName) + "/polref-" + getNameForSourceNatPolicy(tenantName) ;
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCSourceNatPolicyRef(java.lang.String, java.lang.String)
     */
    @Override
    public boolean createTenantVDCSourceNatPolicyRef(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicyRef(
                getDnForSourceNatPolicyRef(tenantName),
                getNameForSourceNatPolicy(tenantName),
                tenantName);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCSourceNatPolicy(java.lang.String, java.lang.String)
     */
    @Override
    public boolean createTenantVDCSourceNatPolicy(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicy(
                getDnForSourceNatPolicy(tenantName),
                getNameForSourceNatPolicy(tenantName));
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCNatPolicySet(java.lang.String)
     */
    @Override
    public boolean createTenantVDCNatPolicySet(String tenantName) throws ExecutionException {
        String xml = VnmcXml.CREATE_NAT_POLICY_SET.getXml();
        String service = VnmcXml.CREATE_NAT_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "NAT policy set for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "natpolicysetname", getNameForNatPolicySet(tenantName));
        xml = replaceXmlValue(xml, "natpolicysetdn", getDnForNatPolicySet(tenantName));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#associateNatPolicySet(java.lang.String)
     */
    @Override
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

    private String getNameForAclPolicySet(String tenantName, boolean ingress) {
        return (ingress ? "Ingress-" : "Egress-") + "ACL-PolicySet-" + tenantName;
    }

    private String getDnForAclPolicySet(String tenantName, boolean ingress) {
        return getDnForTenantVDC(tenantName) + "/pset-" + getNameForAclPolicySet(tenantName, ingress) ;
    }

    private String getNameForAclPolicy(String tenantName, String identifier) {
        return "ACL-" + tenantName + "-" + identifier;
    }

    private String getDnForAclPolicy(String tenantName, String identifier) {
        return getDnForTenantVDC(tenantName) + "/pol-" + getNameForAclPolicy(tenantName, identifier);
    }

    private String getDnForAclPolicyRef(String tenantName, String identifier, boolean ingress) {
        return getDnForAclPolicySet(tenantName, ingress) + "/polref-" + getNameForAclPolicy(tenantName, identifier);
    }

    private String getNameForAclRule(String tenantName, String identifier) {
        return "Rule-" + tenantName + "-" + identifier;
    }

    private String getDnForAclRule(String tenantName, String identifier, String policyIdentifier) {
        return getDnForAclPolicy(tenantName, policyIdentifier) + "/rule-" + getNameForAclRule(tenantName, identifier);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCAclPolicy(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public boolean createTenantVDCAclPolicy(String tenantName, String identifier, boolean ingress) throws ExecutionException {
        String xml = VnmcXml.CREATE_ACL_POLICY.getXml();
        String service = VnmcXml.CREATE_ACL_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclpolicyname", getNameForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicydn", getDnForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicyrefdn", getDnForAclPolicyRef(tenantName, identifier, ingress));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#deleteTenantVDCAclPolicy(java.lang.String, java.lang.String)
     */
    @Override
    public boolean deleteTenantVDCAclPolicy(String tenantName, String identifier) throws ExecutionException {
        String xml = VnmcXml.DELETE_ACL_POLICY.getXml();
        String service = VnmcXml.DELETE_ACL_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclpolicyname", getNameForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicydn", getDnForAclPolicy(tenantName, identifier));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCAclPolicyRef(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public boolean createTenantVDCAclPolicyRef(String tenantName, String identifier, boolean ingress) throws ExecutionException {
        String xml = VnmcXml.CREATE_ACL_POLICY_REF.getXml();
        String service = VnmcXml.CREATE_ACL_POLICY_REF.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclpolicyname", getNameForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicydn", getDnForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicyrefdn", getDnForAclPolicyRef(tenantName, identifier, ingress));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCAclPolicySet(java.lang.String, boolean)
     */
    @Override
    public boolean createTenantVDCAclPolicySet(String tenantName, boolean ingress) throws ExecutionException {
        String xml = VnmcXml.CREATE_ACL_POLICY_SET.getXml();
        String service = VnmcXml.CREATE_ACL_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        //xml = replaceXmlValue(xml, "descr", "ACL policy set for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "aclpolicysetname", getNameForAclPolicySet(tenantName, ingress));
        xml = replaceXmlValue(xml, "aclpolicysetdn", getDnForAclPolicySet(tenantName, ingress));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#associateAclPolicySet(java.lang.String)
     */
    @Override
    public boolean associateAclPolicySet(String tenantName) throws ExecutionException {
        String xml = VnmcXml.RESOLVE_ACL_POLICY_SET.getXml();
        String service = VnmcXml.RESOLVE_ACL_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "Edge Security Profile for Tenant VDC" + tenantName);
        xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "espdn", getDnForTenantVDCEdgeSecurityProfile(tenantName));
        //xml = replaceXmlValue(xml, "egresspolicysetname", getNameForAclPolicySet(tenantName, false));
        xml = replaceXmlValue(xml, "egresspolicysetname", "default-egress"); //FIXME
        xml = replaceXmlValue(xml, "ingresspolicysetname", getNameForAclPolicySet(tenantName, true));
        xml = replaceXmlValue(xml, "natpolicysetname", getNameForNatPolicySet(tenantName));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createIngressAclRule(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean createIngressAclRule(String tenantName,
            String identifier, String policyIdentifier,
            String protocol, String sourceStartIp, String sourceEndIp,
            String destStartPort, String destEndPort, String destIp) throws ExecutionException {
        String xml = VnmcXml.CREATE_INGRESS_ACL_RULE.getXml();
        String service = VnmcXml.CREATE_INGRESS_ACL_RULE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        //xml = replaceXmlValue(xml, "descr", "Ingress ACL policy for Tenant VDC" + tenantName);
        xml = replaceXmlValue(xml, "aclruledn", getDnForAclRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "aclrulename", getNameForAclRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "actiontype", "permit");
        xml = replaceXmlValue(xml, "protocolvalue", protocol);
        xml = replaceXmlValue(xml, "sourcestartip", sourceStartIp);
        xml = replaceXmlValue(xml, "sourceendip", sourceEndIp);
        xml = replaceXmlValue(xml, "deststartport", destStartPort);
        xml = replaceXmlValue(xml, "destendport", destEndPort);
        xml = replaceXmlValue(xml, "destip", destIp);

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#deleteAclRule(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean deleteAclRule(String tenantName, String identifier, String policyIdentifier) throws ExecutionException {
        String xml = VnmcXml.DELETE_ACL_RULE.getXml();
        String service = VnmcXml.DELETE_ACL_RULE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclruledn", getDnForAclRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "aclrulename", getNameForAclRule(tenantName, identifier));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    private String getNameForDNatPortPool(String tenantName, String identifier) {
        return "Port-" + tenantName + "-" + identifier;
    }

    private String getDnForDNatPortPool(String tenantName, String identifier) {
        return getDnForTenantVDC(tenantName) + "/objgrp-" + getNameForDNatPortPool(tenantName, identifier);
    }

    private String getNameForDNatIpPool(String tenantName, String identifier) {
        return "Ip-" + tenantName + "-" + identifier;
    }

    private String getDnForDNatIpPool(String tenantName, String identifier) {
        return getDnForTenantVDC(tenantName) + "/objgrp-" + getNameForDNatIpPool(tenantName, identifier);
    }

    private boolean createTenantVDCPortPool(String poolDn, String name,
            String description, String startPort, String endPort) throws ExecutionException {
        String xml = VnmcXml.CREATE_PORT_POOL.getXml();
        String service = VnmcXml.CREATE_PORT_POOL.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "portpooldn", poolDn);
        xml = replaceXmlValue(xml, "portpoolname", name);
        xml = replaceXmlValue(xml, "descr", description);
        xml = replaceXmlValue(xml, "startport", startPort);
        xml = replaceXmlValue(xml, "endport", endPort);

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    private boolean createTenantVDCIpPool(String poolDn, String name,
            String description, String ipAddress) throws ExecutionException {
        String xml = VnmcXml.CREATE_IP_POOL.getXml();
        String service = VnmcXml.CREATE_IP_POOL.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "ippooldn", poolDn);
        xml = replaceXmlValue(xml, "ippoolname", name);
        xml = replaceXmlValue(xml, "descr", description);
        xml = replaceXmlValue(xml, "ipvalue", ipAddress);

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    private boolean createTenantVDCNatPolicyRef(String policyRefDn, String name, String tenantName) throws ExecutionException {
        String xml = VnmcXml.CREATE_NAT_POLICY_REF.getXml();
        String service = VnmcXml.CREATE_NAT_POLICY_REF.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natpolicyrefdn", policyRefDn);
        xml = replaceXmlValue(xml, "natpolicyname", name);

        List<String> policies = listNatPolicies(tenantName);
        int order = 100;
        if (policies != null) {
            order += policies.size();
        }
        xml = replaceXmlValue(xml, "order", Integer.toString(order));

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    private boolean createTenantVDCNatPolicy(String policyDn, String name) throws ExecutionException {
        String xml = VnmcXml.CREATE_NAT_POLICY.getXml();
        String service = VnmcXml.CREATE_NAT_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natpolicydn", policyDn);
        xml = replaceXmlValue(xml, "natpolicyname", name);

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    private boolean deleteTenantVDCNatPolicy(String policyDn, String name) throws ExecutionException {
        String xml = VnmcXml.DELETE_NAT_POLICY.getXml();
        String service = VnmcXml.DELETE_NAT_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natpolicydn", policyDn);
        xml = replaceXmlValue(xml, "natpolicyname", name);

        String response =  sendRequest(service, xml);

        return verifySuccess(response);
    }

    private List<String> listNatPolicies(String tenantName) throws ExecutionException {

        String xml = VnmcXml.LIST_NAT_POLICIES.getXml();
        String service = VnmcXml.LIST_NAT_POLICIES.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "vdcdn", getDnForTenantVDC(tenantName));

        String response =  sendRequest(service, xml);

        List<String> result = new ArrayList<String>();
        Document xmlDoc = getDocument(response);
        xmlDoc.normalize();
        NodeList policyList = xmlDoc.getElementsByTagName("pair");
        for (int i=0; i < policyList.getLength(); i++) {
            Node policyNode = policyList.item(i);
            result.add(policyNode.getAttributes().getNamedItem("key").getNodeValue());
        }

        return result;

    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCDNatPortPool(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean createTenantVDCDNatPortPool(String tenantName, String identifier,
            String startPort, String endPort) throws ExecutionException {
        return createTenantVDCPortPool(
                getDnForDNatPortPool(tenantName, identifier), getNameForDNatPortPool(tenantName, identifier),
                "DNAT port pool for " + getNameForDNatPortPool(tenantName, identifier), startPort, endPort);
    }
    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCDNatIpPool(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public boolean createTenantVDCDNatIpPool(String tenantName, String identifier,
            String ipAddress) throws ExecutionException {
        return createTenantVDCIpPool(
                getDnForDNatIpPool(tenantName, identifier), getNameForDNatIpPool(tenantName, identifier),
                "DNAT ip pool for " + getNameForDNatIpPool(tenantName, identifier), ipAddress);
    }

    private String getNameForDNatPolicy(String tenantName, String identifier) {
        return "DNAT-" + tenantName + "-" + identifier;
    }

    private String getDnForDNatPolicy(String tenantName, String identifier) {
        return getDnForTenantVDC(tenantName) + "/natpol-" + getNameForDNatPolicy(tenantName, identifier);
    }

    private String getDnForDNatPolicyRef(String tenantName, String identifier) {
        return getDnForNatPolicySet(tenantName) + "/polref-" + getNameForDNatPolicy(tenantName, identifier);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCDNatPolicyRef(java.lang.String, java.lang.String)
     */
    public boolean createTenantVDCDNatPolicyRef(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicyRef(
                getDnForDNatPolicyRef(tenantName, identifier),
                getNameForDNatPolicy(tenantName, identifier),
                tenantName);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCDNatPolicy(java.lang.String, java.lang.String)
     */
    public boolean createTenantVDCDNatPolicy(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicy(
                getDnForDNatPolicy(tenantName, identifier),
                getNameForDNatPolicy(tenantName, identifier));
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#deleteTenantVDCDNatPolicy(java.lang.String, java.lang.String)
     */
    public boolean deleteTenantVDCDNatPolicy(String tenantName, String identifier) throws ExecutionException {
        return deleteTenantVDCNatPolicy(
                getDnForDNatPolicy(tenantName, identifier),
                getNameForDNatPolicy(tenantName, identifier));
    }

    private String getNameForDNatRule(String tenantName, String identifier) {
        return "Rule-" + tenantName + "-" + identifier;
    }

    private String getDnForDNatRule(String tenantName, String identifier, String policyIdentifier) {
        return getDnForDNatPolicy(tenantName, policyIdentifier) + "/rule-" + getNameForDNatRule(tenantName, identifier);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createTenantVDCDNatRule(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean createTenantVDCDNatRule(String tenantName,
            String identifier, String policyIdentifier,
            String protocol, String sourceIp,
            String startSourcePort, String endSourcePort) throws ExecutionException {
        String xml = VnmcXml.CREATE_DNAT_RULE.getXml();
        String service = VnmcXml.CREATE_DNAT_RULE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natruledn", getDnForDNatRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "natrulename", getNameForDNatRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "DNAT rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "ippoolname", getNameForDNatIpPool(tenantName, policyIdentifier + "-" + identifier));
        xml = replaceXmlValue(xml, "portpoolname", getNameForDNatPortPool(tenantName, policyIdentifier + "-" + identifier));
        xml = replaceXmlValue(xml, "srcip", sourceIp);
        xml = replaceXmlValue(xml, "srcportstart", startSourcePort);
        xml = replaceXmlValue(xml, "srcportend", endSourcePort);
        xml = replaceXmlValue(xml, "protocolvalue", protocol);

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
        return "Edge_Inside"; //TODO: make this configurable
    }

    private String getNameForEdgeOutsideIntf(String tenantName) {
        return "Edge_Outside"; //TODO: make this configurable
    }

    private String getDnForOutsideIntf(String tenantName) {
        return getDnForEdgeFirewall(tenantName) + "/interface-" + getNameForEdgeOutsideIntf(tenantName);
    }

    private String getDnForInsideIntf(String tenantName) {
        return getDnForEdgeFirewall(tenantName) + "/interface-" + getNameForEdgeInsideIntf(tenantName);
    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#createEdgeFirewall(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
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

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#listUnAssocAsa1000v()
     */
    @Override
    public Map<String, String> listUnAssocAsa1000v() throws ExecutionException {

        String xml = VnmcXml.LIST_UNASSOC_ASA1000V.getXml();
        String service = VnmcXml.LIST_UNASSOC_ASA1000V.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);

        String response =  sendRequest(service, xml);

        Map<String, String> result = new HashMap<String, String>();
        Document xmlDoc = getDocument(response);
        xmlDoc.normalize();
        NodeList fwList = xmlDoc.getElementsByTagName("fwInstance");
        for (int j=0; j < fwList.getLength(); j++) {
            Node fwNode = fwList.item(j);
            result.put(fwNode.getAttributes().getNamedItem("mgmtIp").getNodeValue(),
                    fwNode.getAttributes().getNamedItem("dn").getNodeValue());
        }

        return result;

    }

    /* (non-Javadoc)
     * @see com.cloud.network.resource.CiscoVnmcConnection#assocAsa1000v(java.lang.String, java.lang.String)
     */
    @Override
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

}
