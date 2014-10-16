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

package com.cloud.agent.resource.virtualnetwork.facade;

import java.util.ArrayList;
import java.util.List;

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.ForwardingRule;
import com.cloud.agent.resource.virtualnetwork.model.ForwardingRules;

public class SetPortForwardingRulesConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final SetPortForwardingRulesCommand command = (SetPortForwardingRulesCommand) cmd;

        final List<ForwardingRule> rules = new ArrayList<ForwardingRule>();

        for (final PortForwardingRuleTO rule : command.getRules()) {
            final ForwardingRule fwdRule = new ForwardingRule(rule.revoked(), rule.getProtocol().toLowerCase(), rule.getSrcIp(), rule.getStringSrcPortRange(), rule.getDstIp(),
                    rule.getStringDstPortRange());
            rules.add(fwdRule);
        }

        final ForwardingRules ruleSet = new ForwardingRules(rules.toArray(new ForwardingRule[rules.size()]));

        return generateConfigItems(ruleSet);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.FORWARDING_RULES_CONFIG;

        return super.generateConfigItems(configuration);
    }
}