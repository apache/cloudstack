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

import java.util.List;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NetworkGroupResponse extends BaseResponse {
    @SerializedName("id") @Param(description="the ID of the network group")
    private Long id;

    @SerializedName("name") @Param(description="the name of the network group")
    private String name;

    @SerializedName("description") @Param(description="the description of the network group")
    private String description;

    @SerializedName("account") @Param(description="the account owning the network group")
    private String accountName;

    @SerializedName("domainid") @Param(description="the domain ID of the network group")
    private Long domainId;

    @SerializedName("domain") @Param(description="the domain name of the network group")
    private String domainName;

    @SerializedName("ingressrule")  @Param(description="the list of ingress rules associated with the network group")
    private List<IngressRuleResponse> ingressRules;

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

    public List<IngressRuleResponse> getIngressRules() {
        return ingressRules;
    }

    public void setIngressRules(List<IngressRuleResponse> ingressRules) {
        this.ingressRules = ingressRules;
    }
}
