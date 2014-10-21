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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.utils.db.JoinType;

@Entity
@Table(name = ("security_group"))
@JoinType(type = "left")
@SecondaryTable(name = "security_group_rule", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "security_group_id")})
public class SecurityGroupRulesVO implements SecurityGroupRules {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "id", table = "security_group_rule", insertable = false, updatable = false)
    private Long ruleId;

    @Column(name = "uuid", table = "security_group_rule", insertable = false, updatable = false)
    private String ruleUuid;

    @Column(name = "start_port", table = "security_group_rule", insertable = false, updatable = false)
    private int startPort;

    @Column(name = "end_port", table = "security_group_rule", insertable = false, updatable = false)
    private int endPort;

    @Column(name = "protocol", table = "security_group_rule", insertable = false, updatable = false)
    private String protocol;

    @Column(name = "type", table = "security_group_rule", insertable = false, updatable = false)
    private String type;

    @Column(name = "allowed_network_id", table = "security_group_rule", insertable = false, updatable = false, nullable = true)
    private Long allowedNetworkId = null;

    @Column(name = "allowed_ip_cidr", table = "security_group_rule", insertable = false, updatable = false, nullable = true)
    private String allowedSourceIpCidr = null;

    public SecurityGroupRulesVO() {
    }

    public SecurityGroupRulesVO(long id) {
        this.id = id;
    }

    public SecurityGroupRulesVO(long id, String name, String description, Long domainId, Long accountId, Long ruleId, String ruleUuid, int startPort, int endPort,
            String protocol, Long allowedNetworkId, String allowedSourceIpCidr) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.domainId = domainId;
        this.accountId = accountId;
        this.ruleId = ruleId;
        this.ruleUuid = ruleUuid;
        this.startPort = startPort;
        this.endPort = endPort;
        this.protocol = protocol;
        this.allowedNetworkId = allowedNetworkId;
        this.allowedSourceIpCidr = allowedSourceIpCidr;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public Long getRuleId() {
        return ruleId;
    }

    @Override
    public String getRuleUuid() {
        return ruleUuid;
    }

    @Override
    public int getStartPort() {
        return startPort;
    }

    @Override
    public int getEndPort() {
        return endPort;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public SecurityRuleType getRuleType() {
        if ("ingress".equalsIgnoreCase(this.type)) {
            return SecurityRuleType.IngressRule;
        } else {
            return SecurityRuleType.EgressRule;
        }
    }

    @Override
    public Long getAllowedNetworkId() {
        return allowedNetworkId;
    }

    @Override
    public String getAllowedSourceIpCidr() {
        return allowedSourceIpCidr;
    }
}
