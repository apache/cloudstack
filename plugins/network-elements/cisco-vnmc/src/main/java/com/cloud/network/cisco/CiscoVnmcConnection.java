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

import java.util.Map;

import com.cloud.utils.exception.ExecutionException;

public interface CiscoVnmcConnection {

    public boolean createTenant(String tenantName) throws ExecutionException;

    public boolean deleteTenant(String tenantName) throws ExecutionException;

    public boolean createTenantVDC(String tenantName) throws ExecutionException;

    public boolean deleteTenantVDC(String tenantName) throws ExecutionException;

    public boolean createTenantVDCEdgeDeviceProfile(String tenantName) throws ExecutionException;

    public boolean createTenantVDCEdgeStaticRoutePolicy(String tenantName) throws ExecutionException;

    public boolean createTenantVDCEdgeStaticRoute(String tenantName, String nextHopIp, String destination, String netmask) throws ExecutionException;

    public boolean associateTenantVDCEdgeStaticRoutePolicy(String tenantName) throws ExecutionException;

    public boolean associateTenantVDCEdgeDhcpPolicy(String tenantName, String intfName) throws ExecutionException;

    public boolean createTenantVDCEdgeDhcpPolicy(String tenantName, String startIp, String endIp, String subnet, String nameServerIp, String domain)
        throws ExecutionException;

    public boolean associateTenantVDCEdgeDhcpServerPolicy(String tenantName, String intfName) throws ExecutionException;

    public boolean createTenantVDCEdgeSecurityProfile(String tenantName) throws ExecutionException;

    public boolean deleteTenantVDCEdgeSecurityProfile(String tenantName) throws ExecutionException;

    public boolean createTenantVDCSourceNatIpPool(String tenantName, String identifier, String publicIp) throws ExecutionException;

    public boolean createTenantVDCSourceNatRule(String tenantName, String identifier, String startSourceIp, String endSourceIp) throws ExecutionException;

    public boolean createTenantVDCSourceNatPolicy(String tenantName, String identifier) throws ExecutionException;

    public boolean createTenantVDCSourceNatPolicyRef(String tenantName, String identifier) throws ExecutionException;

    public boolean createTenantVDCDNatIpPool(String tenantName, String identifier, String ipAddress) throws ExecutionException;

    public boolean createTenantVDCDNatRule(String tenantName, long ruleId, String policyIdentifier, String publicIp) throws ExecutionException;

    public boolean deleteTenantVDCDNatRule(String tenantName, long ruleId, String policyIdentifier) throws ExecutionException;

    public boolean createTenantVDCAclRuleForDNat(String tenantName, long ruleId, String policyIdentifier, String ipAddress) throws ExecutionException;

    public boolean createTenantVDCDNatPolicy(String tenantName, String identifier) throws ExecutionException;

    public boolean deleteTenantVDCDNatPolicy(String tenantName, String identifier) throws ExecutionException;

    public boolean createTenantVDCDNatPolicyRef(String tenantName, String identifier) throws ExecutionException;

    public boolean createTenantVDCPFPortPool(String tenantName, String identifier, String startPort, String endPort) throws ExecutionException;

    public boolean createTenantVDCPFIpPool(String tenantName, String identifier, String ipAddress) throws ExecutionException;

    public boolean createTenantVDCPFRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String publicIp, String startPort, String endPort)
        throws ExecutionException;

    public boolean deleteTenantVDCPFRule(String tenantName, long ruleId, String policyIdentifier) throws ExecutionException;

    public boolean createTenantVDCAclRuleForPF(String tenantName, long ruleId, String policyIdentifier, String protocol, String ipAddress, String startPort,
        String endPort) throws ExecutionException;

    public boolean createTenantVDCPFPolicy(String tenantName, String identifier) throws ExecutionException;

    public boolean deleteTenantVDCPFPolicy(String tenantName, String identifier) throws ExecutionException;

    public boolean createTenantVDCPFPolicyRef(String tenantName, String identifier) throws ExecutionException;

    public boolean createTenantVDCNatPolicySet(String tenantName) throws ExecutionException;

    public boolean deleteTenantVDCNatPolicySet(String tenantName) throws ExecutionException;

    public boolean associateNatPolicySet(String tenantName) throws ExecutionException;

    public boolean createTenantVDCIngressAclRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String sourceStartIp, String sourceEndIp,
        String destStartPort, String destEndPort) throws ExecutionException;

    public boolean createTenantVDCIngressAclRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String sourceStartIp, String sourceEndIp)
        throws ExecutionException;

    public boolean createTenantVDCEgressAclRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String sourceStartIp, String sourceEndIp,
        String destStartPort, String destEndPort) throws ExecutionException;

    public boolean createTenantVDCEgressAclRule(String tenantName, long ruleId, String policyIdentifier, String protocol, String sourceStartIp, String sourceEndIp)
        throws ExecutionException;

    public boolean deleteTenantVDCAclRule(String tenantName, long ruleId, String policyIdentifier) throws ExecutionException;

    public boolean createTenantVDCAclPolicy(String tenantName, String identifier) throws ExecutionException;

    public boolean createTenantVDCAclPolicyRef(String tenantName, String identifier, boolean ingress) throws ExecutionException;

    public boolean deleteTenantVDCAclPolicy(String tenantName, String identifier) throws ExecutionException;

    public boolean createTenantVDCAclPolicySet(String tenantName, boolean ingress) throws ExecutionException;

    public boolean deleteTenantVDCAclPolicySet(String tenantName, boolean ingress) throws ExecutionException;

    public boolean associateAclPolicySet(String tenantName) throws ExecutionException;

    public boolean createEdgeFirewall(String tenantName, String publicIp, String insideIp, String publicSubnet, String insideSubnet) throws ExecutionException;

    public boolean deleteEdgeFirewall(String tenantName) throws ExecutionException;

    public Map<String, String> listUnAssocAsa1000v() throws ExecutionException;

    public boolean assignAsa1000v(String tenantName, String firewallDn) throws ExecutionException;

    public boolean unassignAsa1000v(String tenantName, String firewallDn) throws ExecutionException;
}
