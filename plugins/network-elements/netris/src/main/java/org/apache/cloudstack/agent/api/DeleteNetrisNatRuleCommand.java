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

public class DeleteNetrisNatRuleCommand extends NetrisCommand {

    private String vpcName;
    private Long vpcId;
    private String natRuleType;
    private String natRuleName;
    private String natIp;

    public DeleteNetrisNatRuleCommand(long zoneId, Long accountId, Long domainId, String vpcName, Long vpcId, String vNetName, Long networkId, boolean isVpc) {
        super(zoneId, accountId, domainId, vNetName, networkId, isVpc);
        this.vpcName = vpcName;
        this.vpcId = vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }

    public String getNatRuleType() {
        return natRuleType;
    }

    public void setNatRuleType(String natRuleType) {
        this.natRuleType = natRuleType;
    }

    public String getNatRuleName() {
        return natRuleName;
    }

    public void setNatRuleName(String natRuleName) {
        this.natRuleName = natRuleName;
    }

    public String getNatIp() {
        return natIp;
    }

    public void setNatIp(String natIp) {
        this.natIp = natIp;
    }
}
