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
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PvlanSetupCommand;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.utils.component.ManagerBase;

public class MockNetworkManagerImpl extends ManagerBase implements MockNetworkManager {

    @Override
    public Answer SetStaticNatRules(SetStaticNatRulesCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer SetPortForwardingRules(SetPortForwardingRulesCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer SetFirewallRules(SetFirewallRulesCommand cmd) {
        return new Answer(cmd);
    }


    @Override
    public NetworkUsageAnswer getNetworkUsage(NetworkUsageCommand cmd) {
        return new NetworkUsageAnswer(cmd, null, 100L, 100L);
    }

    @Override
    public Answer IpAssoc(IpAssocCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer LoadBalancerConfig(LoadBalancerConfigCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer AddDhcpEntry(DhcpEntryCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer setupPVLAN(PvlanSetupCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer plugNic(PlugNicCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer unplugNic(UnPlugNicCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer ipAssoc(IpAssocVpcCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer setSourceNat(SetSourceNatCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer setNetworkAcl(SetNetworkACLCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer setVpcPortForwards(SetPortForwardingRulesVpcCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer setStaticRoute(SetStaticRouteCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer siteToSiteVpn(Site2SiteVpnCfgCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer checkSiteToSiteVpnConnection(CheckS2SVpnConnectionsCommand cmd) {
        return new Answer(cmd);
    }
}
