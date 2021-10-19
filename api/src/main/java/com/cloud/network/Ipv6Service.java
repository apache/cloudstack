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
package com.cloud.network;

import com.cloud.dc.DataCenter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.utils.Pair;
import com.cloud.vm.NicProfile;
import org.apache.cloudstack.api.command.admin.ipv6.CreateIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.DedicateIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.DeleteIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.ListIpv6RangesCmd;
import org.apache.cloudstack.api.command.admin.ipv6.ReleaseIpv6RangeCmd;
import org.apache.cloudstack.api.command.admin.ipv6.UpdateIpv6RangeCmd;
import org.apache.cloudstack.api.command.user.ipv6.CreateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.ListIpv6FirewallRulesCmd;
import org.apache.cloudstack.api.command.user.ipv6.UpdateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.response.Ipv6RangeResponse;
import org.apache.cloudstack.framework.config.ConfigKey;

import java.util.List;

public interface Ipv6Service {

    public static final String IPV6_CIDR_SUFFIX = "/64";

    public static final ConfigKey<String> routerIpv6Gateway = new ConfigKey<String>("Advanced", String.class, "router.ipv6.prefix", "",
            "The gateway of Router Ipv6 address (CIDR prefix length is /64). For example 2001:10:10:10::1", true, ConfigKey.Scope.Account);

    Ipv6Address createIpv6Range(CreateIpv6RangeCmd cmd);

    Ipv6Address updateIpv6Range(UpdateIpv6RangeCmd cmd);

    boolean deleteIpv6Range(DeleteIpv6RangeCmd cmd);

    Ipv6Address dedicateIpv6Range(DedicateIpv6RangeCmd cmd);

    boolean releaseIpv6Range(ReleaseIpv6RangeCmd cmd);

    Ipv6Address takeIpv6Range(long zoneId, boolean isRouterIpv6Null);

    Pair<List<? extends Ipv6Address>, Integer> searchForIpv6Range(ListIpv6RangesCmd cmd);

    Ipv6RangeResponse createIpv6RangeResponse(Ipv6Address address);

    Ipv6Address.InternetProtocol getNetworkOfferingInternetProtocol(Long offeringId);

    Ipv6Address.IPv6Routing getNetworkOfferingIpv6Routing(Long offeringId);

    boolean isIpv6Supported(Long offeringId);

    Boolean isIpv6FirewallEnabled(Long offeringId);

    void updateNicIpv6(NicProfile nic, DataCenter dc, Network network);

    FirewallRule updateIpv6FirewallRule(UpdateIpv6FirewallRuleCmd updateIpv6FirewallRuleCmd);

    Pair<List<? extends FirewallRule>,Integer> listIpv6FirewallRules(ListIpv6FirewallRulesCmd listIpv6FirewallRulesCmd);

    boolean revokeIpv6FirewallRule(Long id);

    FirewallRule createIpv6FirewallRule(CreateIpv6FirewallRuleCmd createIpv6FirewallRuleCmd);

    FirewallRule getIpv6FirewallRule(Long entityId);

    boolean applyIpv6FirewallRule(long id);
}
