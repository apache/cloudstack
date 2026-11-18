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
package com.cloud.network.rules;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.utils.net.Ip;

@Entity
@Table(name = "port_forwarding_rules")
@DiscriminatorValue(value = "PortForwarding")
@PrimaryKeyJoinColumn(name = "id")
public class PortForwardingRuleVO extends FirewallRuleVO implements PortForwardingRule {

    @Enumerated(value = EnumType.STRING)
    @Column(name = "dest_ip_address")
    private Ip destinationIpAddress = null;

    @Column(name = "dest_port_start")
    private int destinationPortStart;

    @Column(name = "dest_port_end")
    private int destinationPortEnd;

    @Column(name = "instance_id")
    private long virtualMachineId;

    @Transient
    List<String> sourceCidrs;

    public PortForwardingRuleVO() {
    }

    public PortForwardingRuleVO(String xId, long srcIpId, int srcPortStart, int srcPortEnd, Ip dstIp, int dstPortStart, int dstPortEnd, String protocol, long networkId,
                                long accountId, long domainId, long instanceId, List<String> sourceCidrs) {
        super(xId, srcIpId, srcPortStart, srcPortEnd, protocol, networkId, accountId, domainId, Purpose.PortForwarding, sourceCidrs, null, null, null, null);
        this.destinationIpAddress = dstIp;
        this.virtualMachineId = instanceId;
        this.destinationPortStart = dstPortStart;
        this.destinationPortEnd = dstPortEnd;
        this.sourceCidrs = sourceCidrs;
    }

    public PortForwardingRuleVO(String xId, long srcIpId, int srcPortStart, int srcPortEnd, Ip dstIp, int dstPortStart, int dstPortEnd, String protocol, long networkId,
                                long accountId, long domainId, long instanceId) {
        this(xId, srcIpId, srcPortStart, srcPortEnd, dstIp, dstPortStart, dstPortEnd, protocol.toLowerCase(), networkId, accountId, domainId, instanceId, null);
    }

    public PortForwardingRuleVO(String xId, long srcIpId, int srcPort, Ip dstIp, int dstPort, String protocol, long networkId, long accountId,
                                long domainId, long instanceId) {
        this(xId, srcIpId, srcPort, srcPort, dstIp, dstPort, dstPort, protocol.toLowerCase(), networkId, accountId, domainId, instanceId, null);
    }

    @Override
    public Ip getDestinationIpAddress() {
        return destinationIpAddress;
    }

    @Override
    public void setDestinationIpAddress(Ip destinationIpAddress) {
        this.destinationIpAddress = destinationIpAddress;
    }

    @Override
    public int getDestinationPortStart() {
        return destinationPortStart;
    }

    public void setDestinationPortStart(int destinationPortStart) {
        this.destinationPortStart = destinationPortStart;
    }

    @Override
    public int getDestinationPortEnd() {
        return destinationPortEnd;
    }

    public void setDestinationPortEnd(int destinationPortEnd) {
        this.destinationPortEnd = destinationPortEnd;
    }

    @Override
    public long getVirtualMachineId() {
        return virtualMachineId;
    }

    public void setVirtualMachineId(long virtualMachineId) {
        this.virtualMachineId = virtualMachineId;
    }

    @Override
    public Long getRelated() {
        return null;
    }

    public void setSourceCidrList(List<String> sourceCidrs) {
        this.sourceCidrs = sourceCidrs;
    }

    @Override
    public List<String> getSourceCidrList() {
        return sourceCidrs;
    }

}
