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

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.async.AsyncInstanceCreateStatus;
import com.google.gson.annotations.Expose;

@Entity
@Table(name = ("security_group_rule"))
public class SecurityGroupRuleVO implements SecurityRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "security_group_id")
    private long securityGroupId;

    @Column(name = "start_port")
    private int startPort;

    @Column(name = "end_port")
    private int endPort;

    @Column(name = "type")
    private String type;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "allowed_network_id", nullable = true)
    private Long allowedNetworkId = null;

    @Column(name = "allowed_ip_cidr", nullable = true)
    private String allowedSourceIpCidr = null;

    @Expose
    @Column(name = "create_status", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private AsyncInstanceCreateStatus createStatus;

    @Column(name = "uuid")
    private String uuid;
    
    public SecurityGroupRuleVO() {
    	this.uuid = UUID.randomUUID().toString();
    }

    public SecurityGroupRuleVO(SecurityRuleType type,long securityGroupId, int fromPort, int toPort, String protocol, long allowedNetworkId ) {
        this.securityGroupId = securityGroupId;
        this.startPort = fromPort;
        this.endPort = toPort;
        this.protocol = protocol;
        this.allowedNetworkId = allowedNetworkId;
    	this.uuid = UUID.randomUUID().toString();
        if (type == SecurityRuleType.IngressRule)
        {
        	this.type = SecurityRuleType.IngressRule.getType();
        }else{
        	this.type = SecurityRuleType.EgressRule.getType();
        }
    }

    public SecurityGroupRuleVO(SecurityRuleType type,long securityGroupId, int fromPort, int toPort, String protocol, String allowedIpCidr) {
        this.securityGroupId = securityGroupId;
        this.startPort = fromPort;
        this.endPort = toPort;
        this.protocol = protocol;
        this.allowedSourceIpCidr = allowedIpCidr;
    	this.uuid = UUID.randomUUID().toString();
        if (type == SecurityRuleType.IngressRule)
        {
            this.type = SecurityRuleType.IngressRule.getType();
        }else{
            this.type = SecurityRuleType.EgressRule.getType();
        }
    }

    @Override
    public long getId() {
        return id;
    }
    
    @Override
    public String getType() {
        return type;
    }

    @Override
    public long getSecurityGroupId() {
        return securityGroupId;
    }
    
    public SecurityRuleType getRuleType() {
    	if ("ingress".equalsIgnoreCase(this.type))
            return SecurityRuleType.IngressRule;
    	else
    		return SecurityRuleType.EgressRule;
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
    public AsyncInstanceCreateStatus getCreateStatus() {
        return createStatus;
    }

    public void setCreateStatus(AsyncInstanceCreateStatus createStatus) {
        this.createStatus = createStatus;
    }

    @Override
    public Long getAllowedNetworkId() {
        return allowedNetworkId;
    }

    @Override
    public String getAllowedSourceIpCidr() {
        return allowedSourceIpCidr;
    }
    
    @Override
    public String getUuid() {
    	return this.uuid;
    }
    
    public void setUuid(String uuid) {
    	this.uuid = uuid;
    }
}
