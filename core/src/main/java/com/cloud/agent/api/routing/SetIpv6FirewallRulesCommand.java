//
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
//

package com.cloud.agent.api.routing;

import java.util.List;

import com.cloud.agent.api.to.FirewallRuleTO;

/**
 *
 * AccessDetails allow different components to put in information about
 * how to access the components inside the command.
 */
public class SetIpv6FirewallRulesCommand extends NetworkElementCommand {
    FirewallRuleTO[] rules;
    String guestIp6Cidr;

    protected SetIpv6FirewallRulesCommand() {
    }

    public SetIpv6FirewallRulesCommand(List<FirewallRuleTO> rules, String guestIp6Cidr) {
        this.rules = rules.toArray(new FirewallRuleTO[rules.size()]);
        this.guestIp6Cidr = guestIp6Cidr;
    }

    public FirewallRuleTO[] getRules() {
        return rules;
    }

    public String getGuestIp6Cidr() {
        return guestIp6Cidr;
    }

    @Override
    public int getAnswersCount() {
        return rules.length;
    }
}
