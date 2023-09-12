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

package org.apache.cloudstack.network.topology;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.AdvancedVpnRules;
import com.cloud.network.rules.BasicVpnRules;
import com.cloud.network.rules.DhcpEntryRules;
import com.cloud.network.rules.DhcpPvlanRules;
import com.cloud.network.rules.DhcpSubNetRules;
import com.cloud.network.rules.FirewallRules;
import com.cloud.network.rules.IpAssociationRules;
import com.cloud.network.rules.LoadBalancingRules;
import com.cloud.network.rules.NetworkAclsRules;
import com.cloud.network.rules.NicPlugInOutRules;
import com.cloud.network.rules.PasswordToRouterRules;
import com.cloud.network.rules.PrivateGatewayRules;
import com.cloud.network.rules.SshKeyToRouterRules;
import com.cloud.network.rules.StaticNatRules;
import com.cloud.network.rules.StaticRoutesRules;
import com.cloud.network.rules.UserdataPwdRules;
import com.cloud.network.rules.UserdataToRouterRules;
import com.cloud.network.rules.VirtualNetworkApplianceFactory;
import com.cloud.network.rules.VpcIpAssociationRules;

public abstract class NetworkTopologyVisitor {

    public abstract VirtualNetworkApplianceFactory getVirtualNetworkApplianceFactory();

    public abstract boolean visit(StaticNatRules nat) throws ResourceUnavailableException;
    public abstract boolean visit(LoadBalancingRules loadbalancing) throws ResourceUnavailableException;
    public abstract boolean visit(FirewallRules firewall) throws ResourceUnavailableException;
    public abstract boolean visit(IpAssociationRules ipAddresses) throws ResourceUnavailableException;
    public abstract boolean visit(UserdataPwdRules userdata) throws ResourceUnavailableException;
    public abstract boolean visit(DhcpEntryRules dhcp) throws ResourceUnavailableException;
    public abstract boolean visit(SshKeyToRouterRules ssh) throws ResourceUnavailableException;
    public abstract boolean visit(PasswordToRouterRules pwd) throws ResourceUnavailableException;
    public abstract boolean visit(NetworkAclsRules acl) throws ResourceUnavailableException;
    public abstract boolean visit(VpcIpAssociationRules vpcIp) throws ResourceUnavailableException;
    public abstract boolean visit(UserdataToRouterRules userdata) throws ResourceUnavailableException;
    public abstract boolean visit(BasicVpnRules vpnRules) throws ResourceUnavailableException;
    public abstract boolean visit(AdvancedVpnRules vpnRules) throws ResourceUnavailableException;
    public abstract boolean visit(PrivateGatewayRules pvtGatewayRules) throws ResourceUnavailableException;
    public abstract boolean visit(DhcpPvlanRules dhcpRules) throws ResourceUnavailableException;
    public abstract boolean visit(DhcpSubNetRules dhcpRules) throws ResourceUnavailableException;
    public abstract boolean visit(NicPlugInOutRules nicPlugInOutRules) throws ResourceUnavailableException;
    public abstract boolean visit(StaticRoutesRules staticRoutesRules) throws ResourceUnavailableException;
}
