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
package com.cloud.api.response;

import com.cloud.api.ApiConstants;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class LoadBalancerResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the load balancer rule ID")
    private Long id;

    @SerializedName("name") @Param(description="the name of the load balancer")
    private String name;

    @SerializedName("description") @Param(description="the description of the load balancer")
    private String description;
    
    @SerializedName(ApiConstants.PUBLIC_IP_ID) @Param(description="the public ip address id")
    private Long publicIpId;

    @SerializedName(ApiConstants.PUBLIC_IP) @Param(description="the public ip address")
    private String publicIp;

    @SerializedName("publicport") @Param(description="the public port")
    private String publicPort;

    @SerializedName("privateport") @Param(description="the private port")
    private String privatePort;

    @SerializedName("algorithm") @Param(description="the load balancer algorithm (source, roundrobin, leastconn)")
    private String algorithm;

    @SerializedName("account") @Param(description="the account of the load balancer rule")
    private String accountName;

    @SerializedName("domainid") @Param(description="the domain ID of the load balancer rule")
    private Long domainId;

    @SerializedName("domain") @Param(description="the domain of the load balancer rule")
    private String domainName;
    
    @SerializedName("state") @Param(description="the state of the rule")
    private String state;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public void setPublicPort(String publicPort) {
        this.publicPort = publicPort;
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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getPublicIpId() {
        return publicIpId;
    }

    public void setPublicIpId(Long publicIpId) {
        this.publicIpId = publicIpId;
    }
    
}
