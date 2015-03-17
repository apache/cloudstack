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

import java.util.LinkedList;
import java.util.List;

import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.resource.virtualnetwork.ConfigItem;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.model.ConfigBase;
import com.cloud.agent.resource.virtualnetwork.model.StaticNatRule;
import com.cloud.agent.resource.virtualnetwork.model.StaticNatRules;

public class SetStaticNatRulesConfigItem extends AbstractConfigItemFacade {

    @Override
    public List<ConfigItem> generateConfig(final NetworkElementCommand cmd) {
        final SetStaticNatRulesCommand command = (SetStaticNatRulesCommand) cmd;

        final LinkedList<StaticNatRule> rules = new LinkedList<>();
        for (final StaticNatRuleTO rule : command.getRules()) {
            final StaticNatRule staticNatRule = new StaticNatRule(rule.revoked(), rule.getProtocol(), rule.getSrcIp(), rule.getStringSrcPortRange(), rule.getDstIp());
            rules.add(staticNatRule);
        }
        final StaticNatRules staticNatRules = new StaticNatRules(rules);

        return generateConfigItems(staticNatRules);
    }

    @Override
    protected List<ConfigItem> generateConfigItems(final ConfigBase configuration) {
        destinationFile = VRScripts.STATICNAT_RULES_CONFIG;

        return super.generateConfigItems(configuration);
    }
}