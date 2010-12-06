/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.api.routing;

import java.util.List;

import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.network.rules.PortForwardingRule;

public class SetPortForwardingRulesCommand extends RoutingCommand {
    PortForwardingRuleTO[] rules;

    protected SetPortForwardingRulesCommand() {
    }
    
    public SetPortForwardingRulesCommand(List<? extends PortForwardingRule> pfRules) {
        rules = new PortForwardingRuleTO[pfRules.size()];
        int i = 0;
        for (PortForwardingRule rule : pfRules) {
            rules[i++] = new PortForwardingRuleTO(rule);
        }
    }
    
    public PortForwardingRuleTO[] getRules() {
        return rules;
    }
}
