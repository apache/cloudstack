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
package org.apache.cloudstack.agent.api;

public class CreateOrUpdateNetrisNatCommand extends NetrisCommand {
    private String vpcName;
    private Long vpcId;
    private String vpcCidr;
    private String natRuleName;
    private String natIp;
    private String natRuleType;

    // Parameters for DNAT
    private String protocol;
    private String sourceAddress;
    private String sourcePort;
    private String destinationAddress;
    private String destinationPort;
    private String state;

    // Parameters for SNAT (Static NAT)
    private String vmIp;

    public CreateOrUpdateNetrisNatCommand(long zoneId, Long accountId, Long domainId, String vpcName, Long vpcId, String vNetName, Long networkId, boolean isVpc, String vpcCidr) {
        super(zoneId, accountId, domainId, vNetName, networkId, isVpc);
        this.vpcName = vpcName;
        this.vpcId = vpcId;
        this.vpcCidr = vpcCidr;
    }

    public String getVpcName() {
        return vpcName;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public String getNatIp() {
        return natIp;
    }

    public void setNatRuleName(String natRuleName) {
        this.natRuleName = natRuleName;
    }

    public String getNatRuleName() {
        return natRuleName;
    }

    public String getVpcCidr() {
        return vpcCidr;
    }

    public void setNatIp(String natIp) {
        this.natIp = natIp;
    }

    public String getVmIp() {
        return vmIp;
    }

    public void setVmIp(String vmIp) {
        this.vmIp = vmIp;
    }

    public String getNatRuleType() {
        return natRuleType;
    }

    public void setNatRuleType(String natRuleType) {
        this.natRuleType = natRuleType;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public void setVpcId(Long vpcId) {
        this.vpcId = vpcId;
    }

    public void setVpcCidr(String vpcCidr) {
        this.vpcCidr = vpcCidr;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public String getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(String sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public String getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(String destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
