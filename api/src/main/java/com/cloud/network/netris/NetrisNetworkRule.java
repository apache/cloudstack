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
package com.cloud.network.netris;

import com.cloud.network.SDNProviderNetworkRule;


import java.util.List;

public class NetrisNetworkRule {
    public enum NetrisRuleAction {
        PERMIT, DENY
    }

    private SDNProviderNetworkRule baseRule;
    private NetrisRuleAction aclAction;
    private List<NetrisLbBackend> lbBackends;
    private String lbRuleName;
    private String lbCidrList;
    private String reason;

    public NetrisNetworkRule(Builder builder) {
        this.baseRule = builder.baseRule;
        this.aclAction = builder.aclAction;
        this.lbBackends = builder.lbBackends;
        this.reason = builder.reason;
        this.lbCidrList = builder.lbCidrList;
        this.lbRuleName = builder.lbRuleName;
    }

    public NetrisRuleAction getAclAction() {
        return aclAction;
    }

    public List<NetrisLbBackend> getLbBackends() {
        return lbBackends;
    }

    public String getReason() {
        return reason;
    }

    public String getLbCidrList() {return lbCidrList; }

    public String getLbRuleName() { return lbRuleName; }

    public SDNProviderNetworkRule getBaseRule() {
        return baseRule;
    }

    // Builder class extending the parent builder
    public static class Builder {
        private SDNProviderNetworkRule baseRule;
        private NetrisRuleAction aclAction;
        private List<NetrisLbBackend> lbBackends;
        private String reason;
        private String lbCidrList;
        private String lbRuleName;

        public Builder baseRule(SDNProviderNetworkRule baseRule) {
            this.baseRule = baseRule;
            return this;
        }

        public Builder aclAction(NetrisRuleAction aclAction) {
            this.aclAction = aclAction;
            return this;
        }

        public Builder lbBackends(List<NetrisLbBackend> lbBackends) {
            this.lbBackends = lbBackends;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder lbCidrList(String lbCidrList) {
            this.lbCidrList = lbCidrList;
            return this;
        }

        public Builder lbRuleName(String lbRuleName) {
            this.lbRuleName = lbRuleName;
            return this;
        }

        public NetrisNetworkRule build() {
            return new NetrisNetworkRule(this);
        }
    }
}
