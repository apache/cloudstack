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
package com.cloud.api.response;

import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.serializer.Param;

public class SecurityGroupRuleResultObject {
    @Param(name = "id")
    private Long id;

    @Param(name = "startport")
    private int startPort;

    @Param(name = "endport")
    private int endPort;

    @Param(name = "protocol")
    private String protocol;

    @Param(name = "securitygroup")
    private String allowedSecurityGroup = null;

    @Param(name = "account")
    private String allowedSecGroupAcct = null;

    @Param(name = "cidr")
    private String allowedSourceIpCidr = null;

    private SecurityRuleType type;

    public SecurityGroupRuleResultObject() {
    }

    public SecurityGroupRuleResultObject(Long id, int startPort, int endPort, String protocol, String allowedSecurityGroup, String allowedSecGroupAcct,
            String allowedSourceIpCidr) {
        this.id = id;
        this.startPort = startPort;
        this.endPort = endPort;
        this.protocol = protocol;
        this.allowedSecurityGroup = allowedSecurityGroup;
        this.allowedSecGroupAcct = allowedSecGroupAcct;
        this.allowedSourceIpCidr = allowedSourceIpCidr;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getStartPort() {
        return startPort;
    }

    public void setRuleType(SecurityRuleType type) {
        this.type = type;
    }

    public SecurityRuleType getRuleType() {
        return type;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAllowedSecurityGroup() {
        return allowedSecurityGroup;
    }

    public void setAllowedSecurityGroup(String allowedSecurityGroup) {
        this.allowedSecurityGroup = allowedSecurityGroup;
    }

    public String getAllowedSecGroupAcct() {
        return allowedSecGroupAcct;
    }

    public void setAllowedSecGroupAcct(String allowedSecGroupAcct) {
        this.allowedSecGroupAcct = allowedSecGroupAcct;
    }

    public String getAllowedSourceIpCidr() {
        return allowedSourceIpCidr;
    }

    public void setAllowedSourceIpCidr(String allowedSourceIpCidr) {
        this.allowedSourceIpCidr = allowedSourceIpCidr;
    }
}
