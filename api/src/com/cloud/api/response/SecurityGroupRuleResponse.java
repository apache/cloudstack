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
import com.cloud.utils.IdentityProxy;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class SecurityGroupRuleResponse extends BaseResponse {
    @SerializedName("ruleid") @Param(description="the id of the security group rule")
    private IdentityProxy ruleId = new IdentityProxy("security_group_rule");

    @SerializedName("protocol") @Param(description="the protocol of the security group rule")
    private String protocol;

    @SerializedName(ApiConstants.ICMP_TYPE) @Param(description="the type of the ICMP message response")
    private Integer icmpType;

    @SerializedName(ApiConstants.ICMP_CODE) @Param(description="the code for the ICMP message response")
    private Integer icmpCode;

    @SerializedName(ApiConstants.START_PORT) @Param(description="the starting IP of the security group rule")
    private Integer startPort;

    @SerializedName(ApiConstants.END_PORT) @Param(description="the ending IP of the security group rule ")
    private Integer endPort;

    @SerializedName(ApiConstants.SECURITY_GROUP_NAME) @Param(description="security group name")
    private String securityGroupName;

    @SerializedName(ApiConstants.ACCOUNT) @Param(description="account owning the security group rule")
    private String accountName;

    @SerializedName(ApiConstants.CIDR) @Param(description="the CIDR notation for the base IP address of the security group rule")
    private String cidr;

    public Long getRuleId() {
        return ruleId.getValue();
    }

    public void setRuleId(Long ruleId) {
        this.ruleId.setValue(ruleId);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public void setIcmpType(Integer icmpType) {
        this.icmpType = icmpType;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public void setIcmpCode(Integer icmpCode) {
        this.icmpCode = icmpCode;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public void setStartPort(Integer startPort) {
        this.startPort = startPort;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public void setEndPort(Integer endPort) {
        this.endPort = endPort;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public void setSecurityGroupName(String securityGroupName) {
        this.securityGroupName = securityGroupName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }
}
