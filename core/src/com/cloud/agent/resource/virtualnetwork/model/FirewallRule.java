//
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
//

package com.cloud.agent.resource.virtualnetwork.model;

import java.util.List;

public class FirewallRule {
    private long id;
    private String srcVlanTag;
    private String srcIp;
    private String protocol;
    private int[] srcPortRange;
    private boolean revoked;
    private boolean alreadyAdded;
    private List<String> sourceCidrList;
    private String purpose;
    private Integer icmpType;
    private Integer icmpCode;
    private String trafficType;
    private String guestCidr;
    private boolean defaultEgressPolicy;
    private String type;

    public FirewallRule() {
        // Empty constructor for (de)serialization
    }

    public FirewallRule(long id, String srcVlanTag, String srcIp, String protocol, int[] srcPortRange, boolean revoked, boolean alreadyAdded, List<String> sourceCidrList,
            String purpose, Integer icmpType, Integer icmpCode, String trafficType, String guestCidr, boolean defaultEgressPolicy) {
        this.id = id;
        this.srcVlanTag = srcVlanTag;
        this.srcIp = srcIp;
        this.protocol = protocol;
        this.srcPortRange = srcPortRange;
        this.revoked = revoked;
        this.alreadyAdded = alreadyAdded;
        this.sourceCidrList = sourceCidrList;
        this.purpose = purpose;
        this.icmpType = icmpType;
        this.icmpCode = icmpCode;
        this.trafficType = trafficType;
        this.guestCidr = guestCidr;
        this.defaultEgressPolicy = defaultEgressPolicy;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSrcVlanTag() {
        return srcVlanTag;
    }

    public void setSrcVlanTag(String srcVlanTag) {
        this.srcVlanTag = srcVlanTag;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public void setSrcIp(String srcIp) {
        this.srcIp = srcIp;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int[] getSrcPortRange() {
        return srcPortRange;
    }

    public void setSrcPortRange(int[] srcPortRange) {
        this.srcPortRange = srcPortRange;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public boolean isAlreadyAdded() {
        return alreadyAdded;
    }

    public void setAlreadyAdded(boolean alreadyAdded) {
        this.alreadyAdded = alreadyAdded;
    }

    public List<String> getSourceCidrList() {
        return sourceCidrList;
    }

    public void setSourceCidrList(List<String> sourceCidrList) {
        this.sourceCidrList = sourceCidrList;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
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

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public String getGuestCidr() {
        return guestCidr;
    }

    public void setGuestCidr(String guestCidr) {
        this.guestCidr = guestCidr;
    }

    public boolean isDefaultEgressPolicy() {
        return defaultEgressPolicy;
    }

    public void setDefaultEgressPolicy(boolean defaultEgressPolicy) {
        this.defaultEgressPolicy = defaultEgressPolicy;
    }

}
