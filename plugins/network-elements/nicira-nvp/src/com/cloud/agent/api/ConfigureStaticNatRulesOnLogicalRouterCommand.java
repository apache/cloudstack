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

package com.cloud.agent.api;

import java.util.List;

import com.cloud.agent.api.to.StaticNatRuleTO;

/**
 *
 */
public class ConfigureStaticNatRulesOnLogicalRouterCommand extends Command {

    private String logicalRouterUuid;
    private List<StaticNatRuleTO> rules;

    public ConfigureStaticNatRulesOnLogicalRouterCommand(final String logicalRouterUuid, final List<StaticNatRuleTO> rules) {
        super();
        this.logicalRouterUuid = logicalRouterUuid;
        this.rules = rules;

    }

    public String getLogicalRouterUuid() {
        return logicalRouterUuid;
    }

    public void setLogicalRouterUuid(final String logicalRouterUuid) {
        this.logicalRouterUuid = logicalRouterUuid;
    }

    public List<StaticNatRuleTO> getRules() {
        return rules;
    }

    public void setRules(final List<StaticNatRuleTO> rules) {
        this.rules = rules;
    }

    /* (non-Javadoc)
     * @see com.cloud.agent.api.Command#executeInSequence()
     */
    @Override
    public boolean executeInSequence() {
        return false;
    }

}
