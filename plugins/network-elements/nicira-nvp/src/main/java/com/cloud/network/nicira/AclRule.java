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

package com.cloud.network.nicira;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

@SuppressWarnings("serial")
public class AclRule extends AccessRule {

    public static final String ETHERTYPE_ARP = "ARP";

    /**
     * @TODO Convert this String into Enum and check the JSON communication still works
     */
    protected String action;

    protected String sourceIpPrefix;

    protected String destinationIpPrefix;

    protected String sourceMacAddress;

    protected String destinationMacAddress;

    protected Integer sourcePortRangeMin;

    protected Integer destinationPortRangeMin;

    protected Integer sourcePortRangeMax;

    protected Integer destinationPortRangeMax;

    protected Integer icmpProtocolCode;

    protected Integer icmpProtocolType;

    protected int order;


    /**
     * Default constructor
     */
    public AclRule() {
    }

    /**
     * Fully parameterized constructor
     */
    public AclRule(String ethertype, int protocol, String action, String sourceMacAddress,
            String destinationMacAddress, String sourceIpPrefix, String destinationIpPrefix,
            Integer sourcePortRangeMin, Integer sourcePortRangeMax,
            Integer destinationPortRangeMin, Integer destinationPortRangeMax,
            int order, Integer icmpProtocolCode, Integer icmpProtocolType) {
        this.ethertype = ethertype;
        this.protocol = protocol;
        this.action = action;
        this.sourceMacAddress = sourceMacAddress;
        this.destinationMacAddress = destinationMacAddress;
        this.sourceIpPrefix = sourceIpPrefix;
        this.destinationIpPrefix = destinationIpPrefix;
        this.sourcePortRangeMin = sourcePortRangeMin;
        this.sourcePortRangeMax = sourcePortRangeMax;
        this.destinationPortRangeMin = destinationPortRangeMin;
        this.destinationPortRangeMax = destinationPortRangeMax;
        this.order = order;
        this.icmpProtocolCode = icmpProtocolCode;
        this.icmpProtocolType = icmpProtocolType;
    }


    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSourceIpPrefix() {
        return sourceIpPrefix;
    }

    public void setSourceIpPrefix(String sourceIpPrefix) {
        this.sourceIpPrefix = sourceIpPrefix;
    }

    public String getDestinationIpPrefix() {
        return destinationIpPrefix;
    }

    public void setDestinationIpPrefix(String destinationIpPrefix) {
        this.destinationIpPrefix = destinationIpPrefix;
    }

    public String getSourceMacAddress() {
        return sourceMacAddress;
    }

    public void setSourceMacAddress(String sourceMacAddress) {
        this.sourceMacAddress = sourceMacAddress;
    }

    public String getDestinationMacAddress() {
        return destinationMacAddress;
    }

    public void setDestinationMacAddress(String destinationMacAddress) {
        this.destinationMacAddress = destinationMacAddress;
    }

    public Integer getSourcePortRangeMin() {
        return sourcePortRangeMin;
    }

    public void setSourcePortRangeMin(Integer sourcePortRangeMin) {
        this.sourcePortRangeMin = sourcePortRangeMin;
    }

    public Integer getDestinationPortRangeMin() {
        return destinationPortRangeMin;
    }

    public void setDestinationPortRangeMin(Integer destinationPortRangeMin) {
        this.destinationPortRangeMin = destinationPortRangeMin;
    }

    public Integer getSourcePortRangeMax() {
        return sourcePortRangeMax;
    }

    public void setSourcePortRangeMax(Integer sourcePortRangeMax) {
        this.sourcePortRangeMax = sourcePortRangeMax;
    }

    public Integer getDestinationPortRangeMax() {
        return destinationPortRangeMax;
    }

    public void setDestinationPortRangeMax(Integer destinationPortRangeMax) {
        this.destinationPortRangeMax = destinationPortRangeMax;
    }

    public Integer getIcmpProtocolCode() {
        return icmpProtocolCode;
    }

    public void setIcmpProtocolCode(Integer icmpProtocolCode) {
        this.icmpProtocolCode = icmpProtocolCode;
    }

    public Integer getIcmpProtocolType() {
        return icmpProtocolType;
    }

    public void setIcmpProtocolType(Integer icmpProtocolType) {
        this.icmpProtocolType = icmpProtocolType;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31)
            .append(ethertype).append(protocol)
            .toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof AclRule)) {
            return false;
        }
        AclRule another = (AclRule) obj;
        return new EqualsBuilder()
                .append(ethertype, another.ethertype)
                .append(protocol, another.protocol)
                .isEquals();
    }
}
