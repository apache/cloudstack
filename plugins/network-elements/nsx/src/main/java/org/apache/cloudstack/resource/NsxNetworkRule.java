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
package org.apache.cloudstack.resource;

import com.cloud.network.Network;

import java.util.List;

public class NsxNetworkRule {

    public enum NsxRuleAction {
        ALLOW, DROP
    }

    private long domainId;
    private long accountId;
    private long zoneId;
    private Long networkResourceId;
    private String networkResourceName;
    private boolean isVpcResource;
    private long vmId;
    private long ruleId;
    private String publicIp;
    private String vmIp;
    private String publicPort;
    private String privatePort;
    private String protocol;
    private String algorithm;
    private List<NsxLoadBalancerMember> memberList;
    private NsxRuleAction aclAction;
    private List<String> sourceCidrList;
    private List<String> destinationCidrList;
    private Integer icmpCode;

    private Integer icmpType;
    private String trafficType;
    private Network.Service service;

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    public Long getNetworkResourceId() {
        return networkResourceId;
    }

    public void setNetworkResourceId(Long networkResourceId) {
        this.networkResourceId = networkResourceId;
    }

    public String getNetworkResourceName() {
        return networkResourceName;
    }

    public void setNetworkResourceName(String networkResourceName) {
        this.networkResourceName = networkResourceName;
    }

    public boolean isVpcResource() {
        return isVpcResource;
    }

    public void setVpcResource(boolean vpcResource) {
        isVpcResource = vpcResource;
    }

    public long getVmId() {
        return vmId;
    }

    public void setVmId(long vmId) {
        this.vmId = vmId;
    }

    public long getRuleId() {
        return ruleId;
    }

    public void setRuleId(long ruleId) {
        this.ruleId = ruleId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getVmIp() {
        return vmIp;
    }

    public void setVmIp(String vmIp) {
        this.vmIp = vmIp;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public List<NsxLoadBalancerMember> getMemberList() {
        return memberList;
    }

    public void setMemberList(List<NsxLoadBalancerMember> memberList) {
        this.memberList = memberList;
    }

    public NsxRuleAction getAclAction() {
        return aclAction;
    }

    public void setAclAction(NsxRuleAction aclAction) {
        this.aclAction = aclAction;
    }

    public Network.Service getService() {
        return service;
    }

    public void setService(Network.Service service) {
        this.service = service;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public void setIcmpCode(Integer icmpCode) {
        this.icmpCode = icmpCode;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public void setIcmpType(Integer icmpType) {
        this.icmpType = icmpType;
    }

    public List<String> getSourceCidrList() {
        return sourceCidrList;
    }

    public void setSourceCidrList(List<String> sourceCidrList) {
        this.sourceCidrList = sourceCidrList;
    }

    public List<String> getDestinationCidrList() {
        return destinationCidrList;
    }

    public void setDestinationCidrList(List<String> destinationCidrList) {
        this.destinationCidrList = destinationCidrList;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public static final class Builder {
        private long domainId;
        private long accountId;
        private long zoneId;
        private Long networkResourceId;
        private String networkResourceName;
        private boolean isVpcResource;
        private long vmId;

        private long ruleId;
        private String publicIp;
        private String vmIp;
        private String publicPort;
        private String privatePort;
        private String protocol;
        private String algorithm;
        private List<NsxLoadBalancerMember> memberList;
        private NsxRuleAction aclAction;
        private List<String> sourceCidrList;
        private List<String> destinationidrList;
        private String trafficType;
        private Integer icmpType;
        private Integer icmpCode;
        private Network.Service service;

        public Builder() {
            // Default constructor
        }

        public Builder setDomainId(long domainId) {
            this.domainId = domainId;
            return this;
        }

        public Builder setAccountId(long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder setZoneId(long zoneId) {
            this.zoneId = zoneId;
            return this;
        }

        public Builder setNetworkResourceId(Long networkResourceId) {
            this.networkResourceId = networkResourceId;
            return this;
        }

        public Builder setNetworkResourceName(String networkResourceName) {
            this.networkResourceName = networkResourceName;
            return this;
        }

        public Builder setVpcResource(boolean isVpcResource) {
            this.isVpcResource = isVpcResource;
            return this;
        }


        public Builder setVmId(long vmId) {
            this.vmId = vmId;
            return this;
        }

        public Builder setRuleId(long ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder setPublicIp(String publicIp) {
            this.publicIp = publicIp;
            return this;
        }

        public Builder setVmIp(String vmIp) {
            this.vmIp = vmIp;
            return this;
        }

        public Builder setPublicPort(String publicPort) {
            this.publicPort = publicPort;
            return this;
        }

        public Builder setPrivatePort(String privatePort) {
            this.privatePort = privatePort;
            return this;
        }

        public Builder setProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder setMemberList(List<NsxLoadBalancerMember> memberList) {
            this.memberList = memberList;
            return this;
        }


        public Builder setAclAction(NsxRuleAction aclAction) {
            this.aclAction = aclAction;
            return this;
        }

        public Builder setTrafficType(String trafficType) {
            this.trafficType = trafficType;
            return this;
        }

        public Builder setIcmpType(Integer icmpType) {
            this.icmpType = icmpType;
            return this;
        }

        public Builder setIcmpCode(Integer icmpCode) {
            this.icmpCode = icmpCode;
            return this;
        }

        public Builder setSourceCidrList(List<String> sourceCidrList) {
            this.sourceCidrList = sourceCidrList;
            return this;
        }

        public Builder setDestinationCidrList(List<String> destinationCidrList) {
            this.destinationidrList = destinationCidrList;
            return this;
        }

        public Builder setService(Network.Service service) {
            this.service = service;
            return this;
        }

        public NsxNetworkRule build() {
            NsxNetworkRule rule = new NsxNetworkRule();
            rule.setDomainId(this.domainId);
            rule.setAccountId(this.accountId);
            rule.setZoneId(this.zoneId);
            rule.setNetworkResourceId(this.networkResourceId);
            rule.setNetworkResourceName(this.networkResourceName);
            rule.setVpcResource(this.isVpcResource);
            rule.setVmId(this.vmId);
            rule.setVmIp(this.vmIp);
            rule.setPublicIp(this.publicIp);
            rule.setPublicPort(this.publicPort);
            rule.setPrivatePort(this.privatePort);
            rule.setProtocol(this.protocol);
            rule.setRuleId(this.ruleId);
            rule.setAlgorithm(this.algorithm);
            rule.setMemberList(this.memberList);
            rule.setAclAction(this.aclAction);
            rule.setIcmpType(this.icmpType);
            rule.setIcmpCode(this.icmpCode);
            rule.setSourceCidrList(this.sourceCidrList);
            rule.setDestinationCidrList(this.destinationidrList);
            rule.setTrafficType(this.trafficType);
            rule.setService(service);
            return rule;
        }
    }
}
