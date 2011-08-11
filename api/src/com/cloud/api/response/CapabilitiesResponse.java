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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class CapabilitiesResponse extends BaseResponse {
    @SerializedName("securitygroupsenabled") @Param(description="true if security groups support is enabled, false otherwise")
    private boolean securityGroupsEnabled;

    @SerializedName("cloudstackversion") @Param(description="version of the cloud stack")
    private String cloudStackVersion;
    
    @SerializedName("userpublictemplateenabled") @Param(description="true if user and domain admins can set templates to be shared, false otherwise")
    private boolean userPublicTemplateEnabled;
    
    @SerializedName("firewallRuleUiEnabled") @Param(description="true if the firewall rule UI is enabled")
    private boolean firewallRuleUiEnabled;
    
    
    @SerializedName("supportELB") @Param(description="true if region supports elastic load balancer on basic zones")
    private String supportELB;

    public boolean getSecurityGroupsEnabled() {
        return securityGroupsEnabled;
    }

    public void setSecurityGroupsEnabled(boolean securityGroupsEnabled) {
        this.securityGroupsEnabled = securityGroupsEnabled;
    }

    public String getCloudStackVersion() {
        return cloudStackVersion;
    }

    public void setCloudStackVersion(String cloudStackVersion) {
        this.cloudStackVersion = cloudStackVersion;
    }
    
    public boolean getUserPublicTemplateEnabled() {
        return securityGroupsEnabled;
    }

    public void setUserPublicTemplateEnabled(boolean userPublicTemplateEnabled) {
        this.userPublicTemplateEnabled = userPublicTemplateEnabled;
    }

    public void setSupportELB(String supportELB) {
        this.supportELB = supportELB;
    }

    public String getSupportELB() {
        return supportELB;
    }
    
    public void setFirewallRuleUiEnabled(boolean firewallRuleUiEnabled) {
    	this.firewallRuleUiEnabled = firewallRuleUiEnabled;
    }
    
    public boolean getFirewallRuleUiEnabled() {
    	return firewallRuleUiEnabled;
    }
}
