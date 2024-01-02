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
package org.apache.cloudstack.agent.api;

import java.util.Objects;

public class CreateOrUpdateNsxTier1NatRuleCommand extends NsxCommand {

    private String tier1GatewayName;
    private String action;
    private String translatedIpAddress;
    private String natRuleId;

    public CreateOrUpdateNsxTier1NatRuleCommand(long domainId, long accountId, long zoneId,
                                                String tier1GatewayName, String action, String translatedIpAddress, String natRuleId) {
        super(domainId, accountId, zoneId);
        this.tier1GatewayName = tier1GatewayName;
        this.action = action;
        this.translatedIpAddress = translatedIpAddress;
        this.natRuleId = natRuleId;
    }

    public String getTier1GatewayName() {
        return tier1GatewayName;
    }

    public String getAction() {
        return action;
    }

    public String getTranslatedIpAddress() {
        return translatedIpAddress;
    }

    public String getNatRuleId() {
        return natRuleId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        CreateOrUpdateNsxTier1NatRuleCommand that = (CreateOrUpdateNsxTier1NatRuleCommand) o;
        return Objects.equals(tier1GatewayName, that.tier1GatewayName) && Objects.equals(action, that.action) && Objects.equals(translatedIpAddress, that.translatedIpAddress) && Objects.equals(natRuleId, that.natRuleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tier1GatewayName, action, translatedIpAddress, natRuleId);
    }
}
