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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.async.AsyncInstanceCreateStatus;
import com.google.gson.annotations.Expose;

@Entity
@Table(name=("network_ingress_rule"))
public class IngressRuleVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="network_group_id")
    private long networkGroupId;

    @Column(name="start_port")
    private int startPort;

    @Column(name="end_port")
    private int endPort;

    @Column(name="protocol")
    private String protocol;
    
    @Column(name="allowed_network_id", nullable=true)
    private Long allowedNetworkId = null;

    @Column(name="allowed_network_group")
    private String allowedNetworkGroup;
    
    @Column(name="allowed_net_grp_acct")
    private String allowedNetGrpAcct;
    
    @Column(name="allowed_ip_cidr", nullable=true)
    private String allowedSourceIpCidr = null;
    
    @Expose
    @Column(name="create_status", updatable = true, nullable=false)
    @Enumerated(value=EnumType.STRING)
    private AsyncInstanceCreateStatus createStatus;

    public IngressRuleVO() {}

    public IngressRuleVO(long networkGroupId, int fromPort, int toPort, String protocol, long allowedNetworkId, String allowedNetworkGroup, String allowedNetGrpAcct) {
        this.networkGroupId = networkGroupId;
        this.startPort = fromPort;
        this.endPort = toPort;
        this.protocol = protocol;
        this.allowedNetworkId  = allowedNetworkId;
        this.allowedNetworkGroup = allowedNetworkGroup;
        this.allowedNetGrpAcct = allowedNetGrpAcct;
    }
    
    public IngressRuleVO(long networkGroupId, int fromPort, int toPort, String protocol, String allowedIpCidr) {
        this.networkGroupId = networkGroupId;
        this.startPort = fromPort;
        this.endPort = toPort;
        this.protocol = protocol;
        this.allowedSourceIpCidr  = allowedIpCidr;
    }

    public Long getId() {
        return id;
    }

    public long getNetworkGroupId() {
        return networkGroupId;
    }

    public int getStartPort() {
        return startPort;
    }

    public int getEndPort() {
        return endPort;
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

	public Long getAllowedNetworkId() {
		return allowedNetworkId;
	}

	public String getAllowedNetworkGroup() {
	    return allowedNetworkGroup;
	}

    public String getAllowedNetGrpAcct() {
        return allowedNetGrpAcct;
    }

	public String getAllowedSourceIpCidr() {
		return allowedSourceIpCidr;
	}
}
