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
package com.cloud.network.security;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = ("tungsten_security_group_rule"))
public class TungstenSecurityGroupRuleVO implements TungstenSecurityGroupRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "security_group_id")
    private long securityGroupId;

    @Column(name = "rule_type")
    private String ruleType;

    @Column(name = "rule_target")
    private String ruleTarget;

    @Column(name = "ether_type")
    private String etherType;

    @Column(name = "default_rule")
    private boolean defaultRule;

    public TungstenSecurityGroupRuleVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public TungstenSecurityGroupRuleVO(final long zoneId, final long securityGroupId, final String ruleType,
        final String ruleTarget, final String etherType, final boolean defaultRule) {
        this.uuid = UUID.randomUUID().toString();
        this.zoneId = zoneId;
        this.securityGroupId = securityGroupId;
        this.ruleType = ruleType;
        this.ruleTarget = ruleTarget;
        this.etherType = etherType;
        this.defaultRule = defaultRule;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(final long zoneId) {
        this.zoneId = zoneId;
    }

    public long getSecurityGroupId() {
        return securityGroupId;
    }

    public void setSecurityGroupId(final long securityGroupId) {
        this.securityGroupId = securityGroupId;
    }

    public String getRuleType() {
        return ruleType;
    }

    public void setRuleType(final String ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleTarget() {
        return ruleTarget;
    }

    public void setRuleTarget(final String ruleTarget) {
        this.ruleTarget = ruleTarget;
    }

    public String getEtherType() {
        return etherType;
    }

    public void setEtherType(final String etherType) {
        this.etherType = etherType;
    }

    public boolean isDefaultRule() {
        return defaultRule;
    }

    public void setDefaultRule(final boolean defaultRule) {
        this.defaultRule = defaultRule;
    }
}
