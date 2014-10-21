// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.stack.models;

import com.google.gson.annotations.SerializedName;

public class CloudStackIngressRule {

    @SerializedName(ApiConstants.RULE_ID)
    private String ruleId;

    @SerializedName(ApiConstants.PROTOCOL)
    private String protocol;

    @SerializedName(ApiConstants.ICMP_TYPE)
    private Integer icmpType;

    @SerializedName(ApiConstants.ICMP_CODE)
    private Integer icmpCode;

    @SerializedName(ApiConstants.START_PORT)
    private Integer startPort;

    @SerializedName(ApiConstants.END_PORT)
    private Integer endPort;

    @SerializedName(ApiConstants.SECURITY_GROUP_NAME)
    private String securityGroupName;

    @SerializedName(ApiConstants.ACCOUNT)
    private String accountName;

    @SerializedName(ApiConstants.CIDR)
    private String cidr;

    public CloudStackIngressRule() {
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getProtocol() {
        return protocol;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getCidr() {
        return cidr;
    }
}
