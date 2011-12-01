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

@Entity
@Table(name = ("security_group"))
@SecondaryTable(name = "security_group_rule", join = "left", pkJoinColumns = { @PrimaryKeyJoinColumn(name = "id", referencedColumnName = "security_group_id") })
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

    public SecurityGroupRulesVO(long id, String name, String description, Long domainId, Long accountId, Long ruleId, int startPort, int endPort, String protocol, Long allowedNetworkId,
            String allowedSourceIpCidr) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.domainId = domainId;
        this.accountId = accountId;
        this.ruleId = ruleId;
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
