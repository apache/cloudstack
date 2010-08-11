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

package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.async.AsyncInstanceCreateStatus;
import com.google.gson.annotations.Expose;

@Entity
@Table(name=("network_rule_config"))
public class NetworkRuleConfigVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="security_group_id")
    private long securityGroupId;

    @Column(name="public_port")
    private String publicPort;

    @Column(name="private_port")
    private String privatePort;

    @Column(name="protocol")
    private String protocol;
    
    @Expose
    @Column(name="create_status", updatable = true, nullable=false)
    @Enumerated(value=EnumType.STRING)
    private AsyncInstanceCreateStatus createStatus;

    public NetworkRuleConfigVO() {}

    public NetworkRuleConfigVO(long securityGroupId, String publicPort, String privatePort, String protocol) {
        this.securityGroupId = securityGroupId;
        this.publicPort = publicPort;
        this.privatePort = privatePort;
        this.protocol = protocol;
    }

    public Long getId() {
        return id;
    }

    public long getSecurityGroupId() {
        return securityGroupId;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getProtocol() {
        return protocol;
    }
    
    public AsyncInstanceCreateStatus getCreateStatus() {
    	return createStatus;
    }
    
    public void setCreateStatus(AsyncInstanceCreateStatus createStatus) {
    	this.createStatus = createStatus;
    }
}
