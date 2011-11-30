/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
import com.cloud.api.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LBStickinessResponse extends BaseResponse {
    @SerializedName("lbruleid")
    @Param(description = "the LB rule ID")
    private IdentityProxy lbRuleId = new IdentityProxy("firewall_rules");

    @SerializedName("name")
    @Param(description = "the name of the Stickiness policy")
    private String name;

    @SerializedName("description")
    @Param(description = "the description of the Stickiness policy")
    private String description;;

    @SerializedName("account")
    @Param(description = "the account of the Stickiness policy")
    private String accountName;
    
    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "the domain ID of the Stickiness policy")
    private IdentityProxy domainId = new IdentityProxy("domain");

    @SerializedName("domain")
    @Param(description = "the domain of the Stickiness policy")
    private String domainName;

    @SerializedName("state")
    @Param(description = "the state of the policy")
    private String state;
    
    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the id of the zone the Stickiness policy belongs to")
    private IdentityProxy zoneId = new IdentityProxy("data_center");

    @SerializedName("stickinesspolicy")
    @Param(description = "the list of stickinesspolicies", responseObject = LBStickinessPolicyResponse.class)
    private List<LBStickinessPolicyResponse> stickinessPolicies;

    public void setlbRuleId(Long lbRuleId) {
        this.lbRuleId.setValue(lbRuleId);
    }

    public void setRules(List<LBStickinessPolicyResponse> policies) {
        this.stickinessPolicies = policies;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<LBStickinessPolicyResponse> getStickinessPolicies() {
        return stickinessPolicies;
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

    public void setDomainId(Long domainId) {
        this.domainId.setValue(domainId);
    }
    
    public void setZoneId(Long zoneId) {
        this.zoneId.setValue(zoneId);
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

    public LBStickinessResponse() {

    }

}
