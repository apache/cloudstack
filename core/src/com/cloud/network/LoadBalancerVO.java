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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.Table;

@Entity
@Table(name=("load_balancer"))
@SecondaryTable(name="account",
        pkJoinColumns={@PrimaryKeyJoinColumn(name="account_id", referencedColumnName="id")})
public class LoadBalancerVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="name")
    private String name;

    @Column(name="description")
    private String description;

    @Column(name="account_id")
    private long accountId;
    
    @Column(name="domain_id", table="account", insertable=false, updatable=false)
    private long domainId;
    
    @Column(name="account_name", table="account", insertable=false, updatable=false)
    private String accountName = null;

    @Column(name="ip_address")
    private String ipAddress;

    @Column(name="public_port")
    private String publicPort;

    @Column(name="private_port")
    private String privatePort;

    @Column(name="algorithm")
    private String algorithm;

    public LoadBalancerVO() { }

    public LoadBalancerVO(String name, String description, long accountId, String ipAddress, String publicPort, String privatePort, String algorithm) {
        this.name = name;
        this.description = description;
        this.accountId = accountId;
        this.ipAddress = ipAddress;
        this.publicPort = publicPort;
        this.privatePort = privatePort;
        this.algorithm = algorithm;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }
    public void setPrivatePort(String privatePort) {
        this.privatePort = privatePort;
    }

    public String getAlgorithm() {
        return algorithm;
    }
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
    
    public Long getDomainId() {
        return domainId;
    }
    
    public String getAccountName() {
        return accountName;
    }
}
