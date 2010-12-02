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

package com.cloud.network.rules;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.cloud.utils.net.Ip;

@Entity
@Table(name=("port_forwarding_rules"))
@DiscriminatorValue(value="PortForwarding")
@PrimaryKeyJoinColumn(name="id")
public class PortForwardingRuleVO extends FirewallRuleVO implements PortForwardingRule {

    @Enumerated(value=EnumType.STRING)
    @Column(name="dest_ip_address")
    private Ip destinationIpAddress = null;

    @Column(name="dest_port_start")
    private int destinationPortStart;
    
    @Column(name="dest_port_end")
    private int destinationPortEnd;

    public PortForwardingRuleVO() {
    }

    public PortForwardingRuleVO(String xId, Ip srcIp, int srcPortStart, int srcPortEnd, Ip dstIp, int dstPortStart, int dstPortEnd, String protocol, long networkId, long accountId, long domainId) {
        super(xId, srcIp, srcPortStart, srcPortEnd, protocol, networkId, accountId, domainId, Purpose.PortForwarding);
        this.destinationIpAddress = dstIp;
        this.destinationPortStart = dstPortStart;
        this.destinationPortEnd = dstPortEnd;
    }
    
    public PortForwardingRuleVO(String xId, Ip srcIp, int srcPort, Ip dstIp, int dstPort, String protocol, long networkId, long accountId, long domainId) {
        this(xId, srcIp, srcPort, srcPort, dstIp, dstPort, dstPort, protocol, networkId, accountId, domainId);
    }

    @Override
    public Ip getDestinationIpAddress() {
        return destinationIpAddress;
    }

    @Override
    public int getDestinationPortStart() {
        return destinationPortStart;
    }
    
    @Override
    public int getDestinationPortEnd() {
        return destinationPortEnd;
    }
}

