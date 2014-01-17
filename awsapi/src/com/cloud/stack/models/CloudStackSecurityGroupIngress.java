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

public class CloudStackSecurityGroupIngress {
    @SerializedName(ApiConstants.ACCOUNT)
    private String account;
    @SerializedName(ApiConstants.CIDR)
    private String cidr;
    @SerializedName(ApiConstants.END_PORT)
    private Integer endPort;
    @SerializedName(ApiConstants.ICMP_CODE)
    private Integer icmpcode;
    @SerializedName(ApiConstants.ICMP_TYPE)
    private Integer icmptype;
    @SerializedName(ApiConstants.PROTOCOL)
    private String protocol;
    @SerializedName(ApiConstants.RULE_ID)
    private String ruleId;
    @SerializedName(ApiConstants.SECURITY_GROUP_NAME)
    private String securityGroupName;
    @SerializedName(ApiConstants.START_PORT)
    private Integer startPort;

    /**
     *
     */
    public CloudStackSecurityGroupIngress() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the account
     */
    public String getAccount() {
        return account;
    }

    /**
     * @return the cidr
     */
    public String getCidr() {
        return cidr;
    }

    /**
     * @return the endPort
     */
    public Integer getEndPort() {
        return endPort;
    }

    /**
     * @return the icmpcode
     */
    public Integer getIcmpcode() {
        return icmpcode;
    }

    /**
     * @return the icmptype
     */
    public Integer getIcmptype() {
        return icmptype;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return the ruleId
     */
    public String getRuleId() {
        return ruleId;
    }

    /**
     * @return the securityGroupName
     */
    public String getSecurityGroupName() {
        return securityGroupName;
    }

    /**
     * @return the startPort
     */
    public Integer getStartPort() {
        return startPort;
    }
}
