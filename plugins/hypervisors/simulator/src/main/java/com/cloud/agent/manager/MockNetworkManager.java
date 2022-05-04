/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.agent.manager;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.ReplugNicAnswer;
import com.cloud.agent.api.ReplugNicCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetIpv6FirewallRulesCommand;
import com.cloud.agent.api.routing.SetNetworkACLAnswer;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesAnswer;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatAnswer;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesAnswer;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteAnswer;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.utils.component.Manager;

public interface MockNetworkManager extends Manager {

    Answer SetStaticNatRules(SetStaticNatRulesCommand cmd);

    Answer SetPortForwardingRules(SetPortForwardingRulesCommand cmd);

    Answer SetFirewallRules(SetFirewallRulesCommand cmd);

    Answer SetIpv6FirewallRules(SetIpv6FirewallRulesCommand cmd);

    Answer getNetworkUsage(NetworkUsageCommand cmd);

    Answer IpAssoc(IpAssocCommand cmd);

    Answer LoadBalancerConfig(LoadBalancerConfigCommand cmd);

    Answer AddDhcpEntry(DhcpEntryCommand cmd);

    Answer setupPVLAN(PvlanSetupCommand cmd);

    PlugNicAnswer plugNic(PlugNicCommand cmd);

    UnPlugNicAnswer unplugNic(UnPlugNicCommand cmd);

    ReplugNicAnswer replugNic(ReplugNicCommand cmd);

    IpAssocAnswer ipAssoc(IpAssocVpcCommand cmd);

    SetSourceNatAnswer setSourceNat(SetSourceNatCommand cmd);

    SetNetworkACLAnswer setNetworkAcl(SetNetworkACLCommand cmd);

    SetPortForwardingRulesAnswer setVpcPortForwards(SetPortForwardingRulesVpcCommand cmd);

    Answer setUpGuestNetwork(SetupGuestNetworkCommand cmd);

    SetStaticNatRulesAnswer setVPCStaticNatRules(SetStaticNatRulesCommand cmd);

    SetStaticRouteAnswer setStaticRoute(SetStaticRouteCommand cmd);

    Answer siteToSiteVpn(Site2SiteVpnCfgCommand cmd);

    Answer checkSiteToSiteVpnConnection(CheckS2SVpnConnectionsCommand cmd);
}
