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
package org.apache.cloudstack.network.tungsten.agent.api;

public class AddTungstenSecurityGroupRuleCommand extends TungstenCommand {
    private final String tungstenSecurityGroupUuid;
    private final String tungstenGroupRuleUuid;
    private final String securityGroupRuleType;
    private final int startPort;
    private final int endPort;
    private final String cidr;
    private final String ipPrefix;
    private final int ipPrefixLen;
    private final String protocol;

    public AddTungstenSecurityGroupRuleCommand(String tungstenSecurityGroupUuid, String tungstenGroupRuleUuid,
                                               String securityGroupRuleType, int startPort, int endPort,
                                               String cidr, String ipPrefix, int ipPrefixLen, String protocol) {
        this.tungstenSecurityGroupUuid = tungstenSecurityGroupUuid;
        this.tungstenGroupRuleUuid = tungstenGroupRuleUuid;
        this.securityGroupRuleType = securityGroupRuleType;
        this.startPort = startPort;
        this.endPort = endPort;
        this.cidr = cidr;
        this.ipPrefix = ipPrefix;
        this.ipPrefixLen = ipPrefixLen;
        this.protocol = protocol;
    }

    public String getTungstenSecurityGroupUuid() {
        return tungstenSecurityGroupUuid;
    }

    public String getTungstenGroupRuleUuid() {
        return tungstenGroupRuleUuid;
    }

    public String getSecurityGroupRuleType() {
        return securityGroupRuleType;
    }

    public int getStartPort() {
        return startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public String getCidr() {
        return cidr;
    }

    public String getIpPrefix() {
        return ipPrefix;
    }

    public int getIpPrefixLen() {
        return ipPrefixLen;
    }

    public String getProtocol() {
        return protocol;
    }
}
