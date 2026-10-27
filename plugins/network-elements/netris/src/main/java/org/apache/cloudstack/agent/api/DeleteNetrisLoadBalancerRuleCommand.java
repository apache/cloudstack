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

public class DeleteNetrisLoadBalancerRuleCommand extends NetrisCommand {
    private Long lbId;
    private String ruleName;
    private String cidrList;

    public DeleteNetrisLoadBalancerRuleCommand(long zoneId, Long accountId, Long domainId, String name, Long id, boolean isVpc, Long lbId) {
        super(zoneId, accountId, domainId, name, id, isVpc);
        this.lbId = lbId;
    }

    public Long getLbId() {
        return lbId;
    }

    public void setLbId(Long lbId) {
        this.lbId = lbId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getCidrList() {
        return cidrList;
    }

    public void setCidrList(String cidrList) {
        this.cidrList = cidrList;
    }
}
