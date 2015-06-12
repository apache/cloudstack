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
import java.io.FileInputStream;
import java.io.InputStreamReader;
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

    private final String _ip;
    private final String _username;
    private final String _password;
    private String _cookie;

    private static final Logger s_logger = Logger.getLogger(CiscoVnmcConnectionImpl.class);

    private enum VnmcXml {
        LOGIN("login.xml", "mgmt-controller"),

        CREATE_TENANT("create-tenant.xml", "service-reg"),
        DELETE_TENANT("delete-tenant.xml", "service-reg"),
        CREATE_VDC("create-vdc.xml", "service-reg"),
        DELETE_VDC("delete-vdc.xml", "service-reg"),

        CREATE_EDGE_DEVICE_PROFILE("create-edge-device-profile.xml", "policy-mgr"),
        CREATE_EDGE_ROUTE_POLICY("create-edge-device-route-policy.xml", "policy-mgr"),
        CREATE_EDGE_ROUTE("create-edge-device-route.xml", "policy-mgr"),
        RESOLVE_EDGE_ROUTE_POLICY("associate-route-policy.xml", "policy-mgr"),

        CREATE_DHCP_POLICY("create-dhcp-policy.xml", "policy-mgr"),
        RESOLVE_EDGE_DHCP_POLICY("associate-dhcp-policy.xml", "policy-mgr"),
        RESOLVE_EDGE_DHCP_SERVER_POLICY("associate-dhcp-server.xml", "policy-mgr"),

        CREATE_EDGE_SECURITY_PROFILE("create-edge-security-profile.xml", "policy-mgr"),
        DELETE_EDGE_SECURITY_PROFILE("delete-edge-security-profile.xml", "policy-mgr"),

        CREATE_NAT_POLICY_SET("create-nat-policy-set.xml", "policy-mgr"),
        DELETE_NAT_POLICY_SET("delete-nat-policy-set.xml", "policy-mgr"),
        RESOLVE_NAT_POLICY_SET("associate-nat-policy-set.xml", "policy-mgr"),
        CREATE_NAT_POLICY("create-nat-policy.xml", "policy-mgr"),
        DELETE_NAT_POLICY("delete-nat-policy.xml", "policy-mgr"),
        LIST_NAT_POLICIES("list-nat-policies.xml", "policy-mgr"),
        CREATE_NAT_POLICY_REF("create-nat-policy-ref.xml", "policy-mgr"),
        CREATE_PORT_POOL("create-port-pool.xml", "policy-mgr"),
        CREATE_IP_POOL("create-ip-pool.xml", "policy-mgr"),

        CREATE_PF_RULE("create-pf-rule.xml", "policy-mgr"),
        CREATE_ACL_RULE_FOR_PF("create-acl-rule-for-pf.xml", "policy-mgr"),
        CREATE_DNAT_RULE("create-dnat-rule.xml", "policy-mgr"),
        CREATE_ACL_RULE_FOR_DNAT("create-acl-rule-for-dnat.xml", "policy-mgr"),
        CREATE_SOURCE_NAT_RULE("create-source-nat-rule.xml", "policy-mgr"),

        CREATE_ACL_POLICY_SET("create-acl-policy-set.xml", "policy-mgr"),
        DELETE_ACL_POLICY_SET("delete-acl-policy-set.xml", "policy-mgr"),
        RESOLVE_ACL_POLICY_SET("associate-acl-policy-set.xml", "policy-mgr"),
        CREATE_ACL_POLICY("create-acl-policy.xml", "policy-mgr"),
        DELETE_ACL_POLICY("delete-acl-policy.xml", "policy-mgr"),
        LIST_ACL_POLICIES("list-acl-policies.xml", "policy-mgr"),
        CREATE_ACL_POLICY_REF("create-acl-policy-ref.xml", "policy-mgr"),
        CREATE_INGRESS_ACL_RULE("create-ingress-acl-rule.xml", "policy-mgr"),
        CREATE_EGRESS_ACL_RULE("create-egress-acl-rule.xml", "policy-mgr"),
        CREATE_GENERIC_INGRESS_ACL_RULE("create-generic-ingress-acl-rule.xml", "policy-mgr"),
        CREATE_GENERIC_EGRESS_ACL_RULE("create-generic-egress-acl-rule.xml", "policy-mgr"),
        CREATE_GENERIC_EGRESS_ACL_NO_PROTOCOL_RULE("create-generic-egress-acl-no-protocol-rule.xml", "policy-mgr"),

        DELETE_RULE("delete-rule.xml", "policy-mgr"),

        LIST_CHILDREN("list-children.xml", "policy-mgr"),

        CREATE_EDGE_FIREWALL("create-edge-firewall.xml", "resource-mgr"),
        DELETE_EDGE_FIREWALL("delete-edge-firewall.xml", "resource-mgr"),

        LIST_UNASSOC_ASA1000V("list-unassigned-asa1000v.xml", "resource-mgr"),
        ASSIGN_ASA1000V("assoc-asa1000v.xml", "resource-mgr"),
        UNASSIGN_ASA1000V("disassoc-asa1000v.xml", "resource-mgr");

        private final String scriptsDir = "scripts/network/cisco";
        private String xml;
        private String service;

        private VnmcXml(String filename, String service) {
            xml = getXml(filename);
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

                InputStreamReader fr = new InputStreamReader(new FileInputStream(xmlFilePath),"UTF-8");
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
        _ip = hostIp;
        _username = userName;
        _password = password;

    }

    public boolean login() throws ExecutionException {
        String xml = VnmcXml.LOGIN.getXml();
        String service = VnmcXml.LOGIN.getService();
        xml = replaceXmlValue(xml, "username", _username);
        xml = replaceXmlValue(xml, "password", _password);
        String response = sendRequest(service, xml);
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
        return "EDSP-" + tenantName + "-Routes";
    }

    @Override
    public boolean createTenant(String tenantName) throws ExecutionException {
        String xml = VnmcXml.CREATE_TENANT.getXml();
        String service = VnmcXml.CREATE_TENANT.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "Tenant for account " + tenantName);
        xml = replaceXmlValue(xml, "name", tenantName);
        xml = replaceXmlValue(xml, "dn", getDnForTenant(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenant(String tenantName) throws ExecutionException {
        String xml = VnmcXml.DELETE_TENANT.getXml();
        String service = VnmcXml.DELETE_TENANT.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "name", tenantName);
        xml = replaceXmlValue(xml, "dn", getDnForTenant(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDC(String tenantName) throws ExecutionException {
        String xml = VnmcXml.CREATE_VDC.getXml();
        String service = VnmcXml.CREATE_VDC.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "VDC for Tenant " + tenantName);
        xml = replaceXmlValue(xml, "name", getNameForTenantVDC(tenantName));
        xml = replaceXmlValue(xml, "dn", getDnForTenantVDC(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenantVDC(String tenantName) throws ExecutionException {
        String xml = VnmcXml.DELETE_VDC.getXml();
        String service = VnmcXml.DELETE_VDC.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "name", getNameForTenantVDC(tenantName));
        xml = replaceXmlValue(xml, "dn", getDnForTenantVDC(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCEdgeDeviceProfile(String tenantName) throws ExecutionException {
        String xml = VnmcXml.CREATE_EDGE_DEVICE_PROFILE.getXml();
        String service = VnmcXml.CREATE_EDGE_DEVICE_PROFILE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "Edge Device Profile for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceServiceProfile(tenantName));
        xml = replaceXmlValue(xml, "dn", getDnForTenantVDCEdgeDeviceProfile(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCEdgeStaticRoutePolicy(String tenantName) throws ExecutionException {
        String xml = VnmcXml.CREATE_EDGE_ROUTE_POLICY.getXml();
        String service = VnmcXml.CREATE_EDGE_ROUTE_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceRoutePolicy(tenantName));
        xml = replaceXmlValue(xml, "routepolicydn", getDnForEdgeDeviceRoutingPolicy(tenantName));
        xml = replaceXmlValue(xml, "descr", "Routing Policy for Edge Device for Tenant " + tenantName);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCEdgeStaticRoute(String tenantName, String nextHopIp, String destination, String netmask) throws ExecutionException {
        String xml = VnmcXml.CREATE_EDGE_ROUTE.getXml();
        String service = VnmcXml.CREATE_EDGE_ROUTE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "routepolicydn", getDnForEdgeDeviceRoutingPolicy(tenantName));
        xml = replaceXmlValue(xml, "nexthop", nextHopIp);
        xml = replaceXmlValue(xml, "nexthopintf", getNameForEdgeOutsideIntf(tenantName));
        xml = replaceXmlValue(xml, "destination", destination);
        xml = replaceXmlValue(xml, "netmask", netmask);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean associateTenantVDCEdgeStaticRoutePolicy(String tenantName) throws ExecutionException {
        String xml = VnmcXml.RESOLVE_EDGE_ROUTE_POLICY.getXml();
        String service = VnmcXml.RESOLVE_EDGE_ROUTE_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceServiceProfile(tenantName));
        xml = replaceXmlValue(xml, "dn", getDnForTenantVDCEdgeDeviceProfile(tenantName));
        xml = replaceXmlValue(xml, "routepolicyname", getNameForEdgeDeviceRoutePolicy(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean associateTenantVDCEdgeDhcpPolicy(String tenantName, String intfName) throws ExecutionException {
        String xml = VnmcXml.RESOLVE_EDGE_DHCP_POLICY.getXml();
        String service = VnmcXml.RESOLVE_EDGE_DHCP_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "dhcpdn", getDnForDhcpPolicy(tenantName, intfName));
        xml = replaceXmlValue(xml, "insideintf", intfName);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCEdgeDhcpPolicy(String tenantName, String startIp, String endIp, String subnet, String nameServerIp, String domain)
        throws ExecutionException {
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

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean associateTenantVDCEdgeDhcpServerPolicy(String tenantName, String intfName) throws ExecutionException {
        String xml = VnmcXml.RESOLVE_EDGE_DHCP_SERVER_POLICY.getXml();
        String service = VnmcXml.RESOLVE_EDGE_DHCP_SERVER_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "dhcpdn", getDnForDhcpPolicy(tenantName, intfName));
        xml = replaceXmlValue(xml, "insideintf", intfName);
        xml = replaceXmlValue(xml, "dhcpserverpolicyname", getNameForDhcpServer(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCEdgeSecurityProfile(String tenantName) throws ExecutionException {
        String xml = VnmcXml.CREATE_EDGE_SECURITY_PROFILE.getXml();
        String service = VnmcXml.CREATE_EDGE_SECURITY_PROFILE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "Edge Security Profile for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "espdn", getDnForTenantVDCEdgeSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "egressref", "default-egress");
        xml = replaceXmlValue(xml, "ingressref", "default-ingress"); //FIXME: allows everything

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenantVDCEdgeSecurityProfile(String tenantName) throws ExecutionException {
        String xml = VnmcXml.DELETE_EDGE_SECURITY_PROFILE.getXml();
        String service = VnmcXml.DELETE_EDGE_SECURITY_PROFILE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "espdn", getDnForTenantVDCEdgeSecurityProfile(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private String getNameForSourceNatIpPool(String tenantName) {
        return "SNATIp-" + tenantName;
    }

    private String getDnForSourceNatPool(String tenantName) {
        return getDnForTenantVDC(tenantName) + "/objgrp-" + getNameForSourceNatIpPool(tenantName);
    }

    @Override
    public boolean createTenantVDCSourceNatIpPool(String tenantName, String identifier, String publicIp) throws ExecutionException {
        return createTenantVDCIpPool(getDnForSourceNatPool(tenantName), getNameForSourceNatIpPool(tenantName), "Source NAT ip pool for Tenant VDC " + tenantName,
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

    private String getNameForNatPolicySet(String tenantName) {
        return "NAT-PolicySet-" + tenantName;
    }

    private String getDnForNatPolicySet(String tenantName) {
        return getDnForTenantVDC(tenantName) + "/natpset-" + getNameForNatPolicySet(tenantName);
    }

    private String getDnForSourceNatPolicyRef(String tenantName) {
        return getDnForNatPolicySet(tenantName) + "/polref-" + getNameForSourceNatPolicy(tenantName);
    }

    @Override
    public boolean createTenantVDCSourceNatRule(String tenantName, String identifier, String startSourceIp, String endSourceIp) throws ExecutionException {

        String xml = VnmcXml.CREATE_SOURCE_NAT_RULE.getXml();
        String service = VnmcXml.CREATE_SOURCE_NAT_RULE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natruledn", getDnForSourceNatRule(tenantName));
        xml = replaceXmlValue(xml, "natrulename", getNameForSourceNatRule(tenantName));
        xml = replaceXmlValue(xml, "descr", "Source NAT rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "srcstartip", startSourceIp);
        xml = replaceXmlValue(xml, "srcendip", endSourceIp);
        xml = replaceXmlValue(xml, "ippoolname", getNameForSourceNatIpPool(tenantName));

        long order = 100;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCSourceNatPolicyRef(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicyRef(getDnForSourceNatPolicyRef(tenantName), getNameForSourceNatPolicy(tenantName), tenantName, true);
    }

    @Override
    public boolean createTenantVDCSourceNatPolicy(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicy(getDnForSourceNatPolicy(tenantName), getNameForSourceNatPolicy(tenantName));
    }

    @Override
    public boolean createTenantVDCNatPolicySet(String tenantName) throws ExecutionException {
        String xml = VnmcXml.CREATE_NAT_POLICY_SET.getXml();
        String service = VnmcXml.CREATE_NAT_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "NAT policy set for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "natpolicysetname", getNameForNatPolicySet(tenantName));
        xml = replaceXmlValue(xml, "natpolicysetdn", getDnForNatPolicySet(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenantVDCNatPolicySet(String tenantName) throws ExecutionException {
        String xml = VnmcXml.DELETE_NAT_POLICY_SET.getXml();
        String service = VnmcXml.DELETE_NAT_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natpolicysetname", getNameForNatPolicySet(tenantName));
        xml = replaceXmlValue(xml, "natpolicysetdn", getDnForNatPolicySet(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean associateNatPolicySet(String tenantName) throws ExecutionException {
        String xml = VnmcXml.RESOLVE_NAT_POLICY_SET.getXml();
        String service = VnmcXml.RESOLVE_NAT_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "Edge Security Profile for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "espdn", getDnForTenantVDCEdgeSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "natpolicysetname", getNameForNatPolicySet(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private String getNameForAclPolicySet(String tenantName, boolean ingress) {
        return (ingress ? "Ingress-" : "Egress-") + "ACL-PolicySet-" + tenantName;
    }

    private String getDnForAclPolicySet(String tenantName, boolean ingress) {
        return getDnForTenantVDC(tenantName) + "/pset-" + getNameForAclPolicySet(tenantName, ingress);
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

    @Override
    public boolean createTenantVDCAclPolicy(String tenantName, String identifier) throws ExecutionException {
        String xml = VnmcXml.CREATE_ACL_POLICY.getXml();
        String service = VnmcXml.CREATE_ACL_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclpolicyname", getNameForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicydn", getDnForAclPolicy(tenantName, identifier));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenantVDCAclPolicy(String tenantName, String identifier) throws ExecutionException {
        String xml = VnmcXml.DELETE_ACL_POLICY.getXml();
        String service = VnmcXml.DELETE_ACL_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclpolicyname", getNameForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicydn", getDnForAclPolicy(tenantName, identifier));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCAclPolicyRef(String tenantName, String identifier, boolean ingress) throws ExecutionException {
        String xml = VnmcXml.CREATE_ACL_POLICY_REF.getXml();
        String service = VnmcXml.CREATE_ACL_POLICY_REF.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclpolicyname", getNameForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicydn", getDnForAclPolicy(tenantName, identifier));
        xml = replaceXmlValue(xml, "aclpolicyrefdn", getDnForAclPolicyRef(tenantName, identifier, ingress));

        List<String> policies = listAclPolicies(tenantName);
        int order = 100;
        if (policies != null) {
            order += policies.size();
        }
        xml = replaceXmlValue(xml, "order", Integer.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCAclPolicySet(String tenantName, boolean ingress) throws ExecutionException {
        String xml = VnmcXml.CREATE_ACL_POLICY_SET.getXml();
        String service = VnmcXml.CREATE_ACL_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "ACL policy set for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "aclpolicysetname", getNameForAclPolicySet(tenantName, ingress));
        xml = replaceXmlValue(xml, "aclpolicysetdn", getDnForAclPolicySet(tenantName, ingress));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenantVDCAclPolicySet(String tenantName, boolean ingress) throws ExecutionException {
        String xml = VnmcXml.DELETE_ACL_POLICY_SET.getXml();
        String service = VnmcXml.DELETE_ACL_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclpolicysetname", getNameForAclPolicySet(tenantName, ingress));
        xml = replaceXmlValue(xml, "aclpolicysetdn", getDnForAclPolicySet(tenantName, ingress));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean associateAclPolicySet(String tenantName) throws ExecutionException {
        String xml = VnmcXml.RESOLVE_ACL_POLICY_SET.getXml();
        String service = VnmcXml.RESOLVE_ACL_POLICY_SET.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "descr", "Edge Security Profile for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "name", getNameForEdgeDeviceSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "espdn", getDnForTenantVDCEdgeSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "egresspolicysetname", getNameForAclPolicySet(tenantName, false));
        xml = replaceXmlValue(xml, "ingresspolicysetname", getNameForAclPolicySet(tenantName, true));
        xml = replaceXmlValue(xml, "natpolicysetname", getNameForNatPolicySet(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCIngressAclRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String sourceStartIp, String sourceEndIp,
        String destStartPort, String destEndPort) throws ExecutionException {
        String xml = VnmcXml.CREATE_INGRESS_ACL_RULE.getXml();
        String service = VnmcXml.CREATE_INGRESS_ACL_RULE.getService();

        String identifier = Long.toString(ruleId);
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclruledn", getDnForAclRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "aclrulename", getNameForAclRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "Ingress ACL rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "actiontype", "permit");
        xml = replaceXmlValue(xml, "protocolvalue", protocol);
        xml = replaceXmlValue(xml, "sourcestartip", sourceStartIp);
        xml = replaceXmlValue(xml, "sourceendip", sourceEndIp);
        xml = replaceXmlValue(xml, "deststartport", destStartPort);
        xml = replaceXmlValue(xml, "destendport", destEndPort);

        long order = 100 + ruleId;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCIngressAclRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String sourceStartIp, String sourceEndIp)
        throws ExecutionException {
        String xml = VnmcXml.CREATE_GENERIC_INGRESS_ACL_RULE.getXml();
        String service = VnmcXml.CREATE_GENERIC_INGRESS_ACL_RULE.getService();

        String identifier = Long.toString(ruleId);
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclruledn", getDnForAclRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "aclrulename", getNameForAclRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "Ingress ACL rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "actiontype", "permit");
        xml = replaceXmlValue(xml, "protocolvalue", protocol);
        xml = replaceXmlValue(xml, "sourcestartip", sourceStartIp);
        xml = replaceXmlValue(xml, "sourceendip", sourceEndIp);

        long order = 100 + ruleId;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCEgressAclRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String sourceStartIp, String sourceEndIp,
        String destStartPort, String destEndPort) throws ExecutionException {
        String xml = VnmcXml.CREATE_EGRESS_ACL_RULE.getXml();
        String service = VnmcXml.CREATE_EGRESS_ACL_RULE.getService();

        String identifier = Long.toString(ruleId);
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclruledn", getDnForAclRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "aclrulename", getNameForAclRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "Egress ACL rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "actiontype", "permit");
        xml = replaceXmlValue(xml, "protocolvalue", protocol);
        xml = replaceXmlValue(xml, "sourcestartip", sourceStartIp);
        xml = replaceXmlValue(xml, "sourceendip", sourceEndIp);
        xml = replaceXmlValue(xml, "deststartport", destStartPort);
        xml = replaceXmlValue(xml, "destendport", destEndPort);

        long order = 100 + ruleId;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCEgressAclRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String sourceStartIp, String sourceEndIp)
        throws ExecutionException {
        String xml = VnmcXml.CREATE_GENERIC_EGRESS_ACL_RULE.getXml();
        String service = VnmcXml.CREATE_GENERIC_EGRESS_ACL_RULE.getService();
        if (protocol.equalsIgnoreCase("all")) { // any protocol
            xml = VnmcXml.CREATE_GENERIC_EGRESS_ACL_NO_PROTOCOL_RULE.getXml();
            service = VnmcXml.CREATE_GENERIC_EGRESS_ACL_NO_PROTOCOL_RULE.getService();
        } else { // specific protocol
            xml = replaceXmlValue(xml, "protocolvalue", protocol);
        }

        String identifier = Long.toString(ruleId);
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclruledn", getDnForAclRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "aclrulename", getNameForAclRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "Egress ACL rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "actiontype", "permit");
        xml = replaceXmlValue(xml, "sourcestartip", sourceStartIp);
        xml = replaceXmlValue(xml, "sourceendip", sourceEndIp);

        long order = 100 + ruleId;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenantVDCAclRule(String tenantName, long ruleId, String policyIdentifier) throws ExecutionException {
        String identifier = Long.toString(ruleId);
        return deleteTenantVDCRule(getDnForAclRule(tenantName, identifier, policyIdentifier), getNameForAclRule(tenantName, identifier));
    }

    private String getNameForPFPortPool(String tenantName, String identifier) {
        return "PortPool-" + tenantName + "-" + identifier;
    }

    private String getDnForPFPortPool(String tenantName, String identifier) {
        return getDnForTenantVDC(tenantName) + "/objgrp-" + getNameForPFPortPool(tenantName, identifier);
    }

    private String getNameForPFIpPool(String tenantName, String identifier) {
        return "IpPool-" + tenantName + "-" + identifier;
    }

    private String getDnForPFIpPool(String tenantName, String identifier) {
        return getDnForTenantVDC(tenantName) + "/objgrp-" + getNameForPFIpPool(tenantName, identifier);
    }

    private boolean createTenantVDCPortPool(String poolDn, String name, String description, String startPort, String endPort) throws ExecutionException {
        String xml = VnmcXml.CREATE_PORT_POOL.getXml();
        String service = VnmcXml.CREATE_PORT_POOL.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "portpooldn", poolDn);
        xml = replaceXmlValue(xml, "portpoolname", name);
        xml = replaceXmlValue(xml, "descr", description);
        xml = replaceXmlValue(xml, "startport", startPort);
        xml = replaceXmlValue(xml, "endport", endPort);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private boolean createTenantVDCIpPool(String poolDn, String name, String description, String ipAddress) throws ExecutionException {
        String xml = VnmcXml.CREATE_IP_POOL.getXml();
        String service = VnmcXml.CREATE_IP_POOL.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "ippooldn", poolDn);
        xml = replaceXmlValue(xml, "ippoolname", name);
        xml = replaceXmlValue(xml, "descr", description);
        xml = replaceXmlValue(xml, "ipvalue", ipAddress);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private boolean createTenantVDCNatPolicyRef(String policyRefDn, String name, String tenantName, boolean isSourceNat) throws ExecutionException {
        String xml = VnmcXml.CREATE_NAT_POLICY_REF.getXml();
        String service = VnmcXml.CREATE_NAT_POLICY_REF.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natpolicyrefdn", policyRefDn);
        xml = replaceXmlValue(xml, "natpolicyname", name);

        // PF and static NAT policies need to come before source NAT, so leaving buffer
        // and creating source NAT with a high order value.
        // Initially tried setting MAX_INT as the order but VNMC complains about it
        int order = 10000; // TODO: For now value should be sufficient, if required may need to increase
        if (!isSourceNat) {
            List<String> policies = listNatPolicies(tenantName);
            order = 100; // order starts at 100
            if (policies != null) {
                order += policies.size();
            }
        }
        xml = replaceXmlValue(xml, "order", Integer.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private boolean createTenantVDCNatPolicy(String policyDn, String name) throws ExecutionException {
        String xml = VnmcXml.CREATE_NAT_POLICY.getXml();
        String service = VnmcXml.CREATE_NAT_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natpolicydn", policyDn);
        xml = replaceXmlValue(xml, "natpolicyname", name);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private boolean deleteTenantVDCNatPolicy(String policyDn, String name) throws ExecutionException {
        String xml = VnmcXml.DELETE_NAT_POLICY.getXml();
        String service = VnmcXml.DELETE_NAT_POLICY.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natpolicydn", policyDn);
        xml = replaceXmlValue(xml, "natpolicyname", name);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private boolean deleteTenantVDCRule(String ruledn, String ruleName) throws ExecutionException {
        String xml = VnmcXml.DELETE_RULE.getXml();
        String service = VnmcXml.DELETE_RULE.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "ruledn", ruledn);
        xml = replaceXmlValue(xml, "rulename", ruleName);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private List<String> listNatPolicies(String tenantName) throws ExecutionException {

        String xml = VnmcXml.LIST_NAT_POLICIES.getXml();
        String service = VnmcXml.LIST_NAT_POLICIES.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "vdcdn", getDnForTenantVDC(tenantName));

        String response = sendRequest(service, xml);

        List<String> result = new ArrayList<String>();
        Document xmlDoc = getDocument(response);
        xmlDoc.normalize();
        NodeList policyList = xmlDoc.getElementsByTagName("pair");
        for (int i = 0; i < policyList.getLength(); i++) {
            Node policyNode = policyList.item(i);
            result.add(policyNode.getAttributes().getNamedItem("key").getNodeValue());
        }

        return result;
    }

    private List<String> listAclPolicies(String tenantName) throws ExecutionException {

        String xml = VnmcXml.LIST_ACL_POLICIES.getXml();
        String service = VnmcXml.LIST_ACL_POLICIES.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "vdcdn", getDnForTenantVDC(tenantName));

        String response = sendRequest(service, xml);

        List<String> result = new ArrayList<String>();
        Document xmlDoc = getDocument(response);
        xmlDoc.normalize();
        NodeList policyList = xmlDoc.getElementsByTagName("pair");
        for (int i = 0; i < policyList.getLength(); i++) {
            Node policyNode = policyList.item(i);
            result.add(policyNode.getAttributes().getNamedItem("key").getNodeValue());
        }

        return result;
    }

    private List<String> listChildren(String dn) throws ExecutionException {

        String xml = VnmcXml.LIST_CHILDREN.getXml();
        String service = VnmcXml.LIST_CHILDREN.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "dn", dn);

        String response = sendRequest(service, xml);

        List<String> result = new ArrayList<String>();
        Document xmlDoc = getDocument(response);
        xmlDoc.normalize();
        NodeList policyList = xmlDoc.getElementsByTagName("policyRule");
        for (int i = 0; i < policyList.getLength(); i++) {
            Node policyNode = policyList.item(i);
            result.add(policyNode.getAttributes().getNamedItem("name").getNodeValue());
        }

        return result;
    }

    @Override
    public boolean createTenantVDCPFPortPool(String tenantName, String identifier, String startPort, String endPort) throws ExecutionException {
        return createTenantVDCPortPool(getDnForPFPortPool(tenantName, identifier), getNameForPFPortPool(tenantName, identifier), "PF port pool for " +
            getNameForPFPortPool(tenantName, identifier), startPort, endPort);
    }

    @Override
    public boolean createTenantVDCPFIpPool(String tenantName, String identifier, String ipAddress) throws ExecutionException {
        return createTenantVDCIpPool(getDnForPFIpPool(tenantName, identifier), getNameForPFIpPool(tenantName, identifier),
            "PF ip pool for " + getNameForPFIpPool(tenantName, identifier), ipAddress);
    }

    private String getNameForPFPolicy(String tenantName, String identifier) {
        return "PF-" + tenantName + "-" + identifier;
    }

    private String getDnForPFPolicy(String tenantName, String identifier) {
        return getDnForTenantVDC(tenantName) + "/natpol-" + getNameForPFPolicy(tenantName, identifier);
    }

    private String getDnForPFPolicyRef(String tenantName, String identifier) {
        return getDnForNatPolicySet(tenantName) + "/polref-" + getNameForPFPolicy(tenantName, identifier);
    }

    private String getNameForPFRule(String tenantName, String identifier) {
        return "Rule-" + tenantName + "-" + identifier;
    }

    private String getDnForPFRule(String tenantName, String identifier, String policyIdentifier) {
        return getDnForPFPolicy(tenantName, policyIdentifier) + "/rule-" + getNameForPFRule(tenantName, identifier);
    }

    @Override
    public boolean createTenantVDCPFRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String publicIp, String startPort, String endPort)
        throws ExecutionException {
        String xml = VnmcXml.CREATE_PF_RULE.getXml();
        String service = VnmcXml.CREATE_PF_RULE.getService();

        String identifier = Long.toString(ruleId);
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natruledn", getDnForPFRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "natrulename", getNameForPFRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "PF rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "ippoolname", getNameForPFIpPool(tenantName, identifier));
        xml = replaceXmlValue(xml, "portpoolname", getNameForPFPortPool(tenantName, identifier));
        xml = replaceXmlValue(xml, "ip", publicIp);
        xml = replaceXmlValue(xml, "startport", startPort);
        xml = replaceXmlValue(xml, "endport", endPort);
        xml = replaceXmlValue(xml, "protocolvalue", protocol);

        long order = 100 + ruleId;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenantVDCPFRule(String tenantName, long ruleId, String policyIdentifier) throws ExecutionException {
        String identifier = Long.toString(ruleId);
        return deleteTenantVDCRule(getDnForPFRule(tenantName, identifier, policyIdentifier), getNameForPFRule(tenantName, identifier));
    }

    @Override
    public boolean createTenantVDCAclRuleForPF(String tenantName, long ruleId, String policyIdentifier, String protocol, String ipAddress, String startPort,
        String endPort) throws ExecutionException {
        String xml = VnmcXml.CREATE_ACL_RULE_FOR_PF.getXml();
        String service = VnmcXml.CREATE_ACL_RULE_FOR_PF.getService();

        String identifier = Long.toString(ruleId);
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclruledn", getDnForAclRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "aclrulename", getNameForAclRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "ACL rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "actiontype", "permit");
        xml = replaceXmlValue(xml, "protocolvalue", protocol);
        xml = replaceXmlValue(xml, "ip", ipAddress);
        xml = replaceXmlValue(xml, "startport", startPort);
        xml = replaceXmlValue(xml, "endport", endPort);

        long order = 100 + ruleId;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCPFPolicyRef(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicyRef(getDnForPFPolicyRef(tenantName, identifier), getNameForPFPolicy(tenantName, identifier), tenantName, false);
    }

    @Override
    public boolean createTenantVDCPFPolicy(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicy(getDnForPFPolicy(tenantName, identifier), getNameForPFPolicy(tenantName, identifier));
    }

    @Override
    public boolean deleteTenantVDCPFPolicy(String tenantName, String identifier) throws ExecutionException {
        return deleteTenantVDCNatPolicy(getDnForPFPolicy(tenantName, identifier), getNameForPFPolicy(tenantName, identifier));
    }

    private String getNameForDNatIpPool(String tenantName, String identifier) {
        return "IpPool-" + tenantName + "-" + identifier + "n";
    }

    private String getDnForDNatIpPool(String tenantName, String identifier) {
        return getDnForTenantVDC(tenantName) + "/objgrp-" + getNameForDNatIpPool(tenantName, identifier);
    }

    @Override
    public boolean createTenantVDCDNatIpPool(String tenantName, String identifier, String ipAddress) throws ExecutionException {
        return createTenantVDCIpPool(getDnForDNatIpPool(tenantName, identifier), getNameForDNatIpPool(tenantName, identifier), "DNAT ip pool for " +
            getNameForDNatIpPool(tenantName, identifier), ipAddress);
    }

    private String getNameForDNatRule(String tenantName, String identifier) {
        return "Rule-" + tenantName + "-" + identifier;
    }

    private String getDnForDNatRule(String tenantName, String identifier, String policyIdentifier) {
        return getDnForDNatPolicy(tenantName, policyIdentifier) + "/rule-" + getNameForDNatRule(tenantName, identifier);
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

    @Override
    public boolean createTenantVDCDNatRule(String tenantName, long ruleId, String policyIdentifier, String publicIp) throws ExecutionException {
        String xml = VnmcXml.CREATE_DNAT_RULE.getXml();
        String service = VnmcXml.CREATE_DNAT_RULE.getService();

        String identifier = Long.toString(ruleId);
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "natruledn", getDnForDNatRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "natrulename", getNameForDNatRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "DNAT rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "ippoolname", getNameForDNatIpPool(tenantName, identifier));
        xml = replaceXmlValue(xml, "ip", publicIp);

        long order = 100 + ruleId;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteTenantVDCDNatRule(String tenantName, long ruleId, String policyIdentifier) throws ExecutionException {
        String identifier = Long.toString(ruleId);
        return deleteTenantVDCRule(getDnForDNatRule(tenantName, identifier, policyIdentifier), getNameForDNatRule(tenantName, identifier));
    }

    @Override
    public boolean createTenantVDCAclRuleForDNat(String tenantName, long ruleId, String policyIdentifier, String ipAddress) throws ExecutionException {
        String xml = VnmcXml.CREATE_ACL_RULE_FOR_DNAT.getXml();
        String service = VnmcXml.CREATE_ACL_RULE_FOR_DNAT.getService();

        String identifier = Long.toString(ruleId);
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "aclruledn", getDnForAclRule(tenantName, identifier, policyIdentifier));
        xml = replaceXmlValue(xml, "aclrulename", getNameForAclRule(tenantName, identifier));
        xml = replaceXmlValue(xml, "descr", "ACL rule for Tenant VDC " + tenantName);
        xml = replaceXmlValue(xml, "actiontype", "permit");
        xml = replaceXmlValue(xml, "ip", ipAddress);

        long order = 100 + ruleId;
        xml = replaceXmlValue(xml, "order", Long.toString(order));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean createTenantVDCDNatPolicyRef(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicyRef(getDnForDNatPolicyRef(tenantName, identifier), getNameForDNatPolicy(tenantName, identifier), tenantName, false);
    }

    @Override
    public boolean createTenantVDCDNatPolicy(String tenantName, String identifier) throws ExecutionException {
        return createTenantVDCNatPolicy(getDnForDNatPolicy(tenantName, identifier), getNameForDNatPolicy(tenantName, identifier));
    }

    @Override
    public boolean deleteTenantVDCDNatPolicy(String tenantName, String identifier) throws ExecutionException {
        return deleteTenantVDCNatPolicy(getDnForDNatPolicy(tenantName, identifier), getNameForDNatPolicy(tenantName, identifier));
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

    @Override
    public boolean createEdgeFirewall(String tenantName, String publicIp, String insideIp, String publicSubnet, String insideSubnet) throws ExecutionException {
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
        xml = replaceXmlValue(xml, "outsideintfsp", getDnForOutsideIntf(tenantName) + "/interface-service-profile");

        xml = replaceXmlValue(xml, "secprofileref", getNameForEdgeDeviceSecurityProfile(tenantName));
        xml = replaceXmlValue(xml, "deviceserviceprofile", getNameForEdgeDeviceServiceProfile(tenantName));

        xml = replaceXmlValue(xml, "insideip", insideIp);
        xml = replaceXmlValue(xml, "publicip", publicIp);
        xml = replaceXmlValue(xml, "insidesubnet", insideSubnet);
        xml = replaceXmlValue(xml, "outsidesubnet", publicSubnet);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean deleteEdgeFirewall(String tenantName) throws ExecutionException {
        String xml = VnmcXml.DELETE_EDGE_FIREWALL.getXml();
        String service = VnmcXml.DELETE_EDGE_FIREWALL.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "edgefwname", getNameForEdgeFirewall(tenantName));
        xml = replaceXmlValue(xml, "edgefwdn", getDnForEdgeFirewall(tenantName));

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public Map<String, String> listUnAssocAsa1000v() throws ExecutionException {
        String xml = VnmcXml.LIST_UNASSOC_ASA1000V.getXml();
        String service = VnmcXml.LIST_UNASSOC_ASA1000V.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);

        String response = sendRequest(service, xml);

        Map<String, String> result = new HashMap<String, String>();
        Document xmlDoc = getDocument(response);
        xmlDoc.normalize();
        NodeList fwList = xmlDoc.getElementsByTagName("fwInstance");
        for (int j = 0; j < fwList.getLength(); j++) {
            Node fwNode = fwList.item(j);
            result.put(fwNode.getAttributes().getNamedItem("mgmtIp").getNodeValue(), fwNode.getAttributes().getNamedItem("dn").getNodeValue());
        }

        return result;
    }

    @Override
    public boolean assignAsa1000v(String tenantName, String firewallDn) throws ExecutionException {
        String xml = VnmcXml.ASSIGN_ASA1000V.getXml();
        String service = VnmcXml.ASSIGN_ASA1000V.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "binddn", getDnForEdgeFirewall(tenantName) + "/binding");
        xml = replaceXmlValue(xml, "fwdn", firewallDn);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    @Override
    public boolean unassignAsa1000v(String tenantName, String firewallDn) throws ExecutionException {
        String xml = VnmcXml.UNASSIGN_ASA1000V.getXml();
        String service = VnmcXml.UNASSIGN_ASA1000V.getService();
        xml = replaceXmlValue(xml, "cookie", _cookie);
        xml = replaceXmlValue(xml, "binddn", getDnForEdgeFirewall(tenantName) + "/binding");
        xml = replaceXmlValue(xml, "fwdn", firewallDn);

        String response = sendRequest(service, xml);
        return verifySuccess(response);
    }

    private String sendRequest(String service, String xmlRequest) throws ExecutionException {
        HttpClient client = new HttpClient();
        String response = null;
        PostMethod method = new PostMethod("/xmlIM/" + service);
        method.setRequestBody(xmlRequest);

        try {
            org.apache.commons.httpclient.protocol.Protocol myhttps = new org.apache.commons.httpclient.protocol.Protocol("https", new EasySSLProtocolSocketFactory(), 443);
            client.getHostConfiguration().setHost(_ip, 443, myhttps);
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("Error code : " + statusCode);
            }
            response = method.getResponseBodyAsString();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new ExecutionException(e.getMessage());
        }
        System.out.println(response);
        return response;
    }

    private Map<String, String> checkResponse(String xmlResponse, String... keys) throws ExecutionException {
        Document xmlDoc = getDocument(xmlResponse);
        Map<String, String> result = new HashMap<String, String>();
        Node topElement = xmlDoc.getChildNodes().item(0);
        if (topElement != null) {
            for (String key : keys) {
                Node valueNode = topElement.getAttributes().getNamedItem(key);
                result.put(key, valueNode == null ? null : valueNode.getNodeValue());
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
