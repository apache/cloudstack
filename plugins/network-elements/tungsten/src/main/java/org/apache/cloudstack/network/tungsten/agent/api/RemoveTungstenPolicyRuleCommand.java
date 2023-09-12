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
package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.Objects;

public class RemoveTungstenPolicyRuleCommand extends TungstenCommand {
    private final String policyUuid;
    private final String ruleUuid;

    public RemoveTungstenPolicyRuleCommand(final String policyUuid, final String ruleUuid) {
        this.policyUuid = policyUuid;
        this.ruleUuid = ruleUuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public String getRuleUuid() {
        return ruleUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RemoveTungstenPolicyRuleCommand that = (RemoveTungstenPolicyRuleCommand) o;
        return Objects.equals(policyUuid, that.policyUuid) && Objects.equals(ruleUuid, that.ruleUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), policyUuid, ruleUuid);
    }
}
