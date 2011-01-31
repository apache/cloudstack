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

package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.utils.net.NetUtils;

@Entity
@Table(name=("load_balancing_rules"))
@DiscriminatorValue(value="LoadBalancing")
@PrimaryKeyJoinColumn(name="id")
public class LoadBalancerVO extends FirewallRuleVO implements LoadBalancer {
    
    @Column(name="name")
    private String name;

    @Column(name="description")
    private String description;

    @Column(name="algorithm")
    private String algorithm;

    @Column(name="default_port_start")
    private int defaultPortStart;
    
    @Column(name="default_port_end")
    private int defaultPortEnd;
    
    public LoadBalancerVO() { 
    }

    public LoadBalancerVO(String xId, String name, String description, long srcIpId, int srcPort, int dstPort, String algorithm, long networkId, long accountId, long domainId) {
        super(xId, srcIpId, srcPort, NetUtils.TCP_PROTO, networkId, accountId, domainId, Purpose.LoadBalancing, false);
        this.name = name;
        this.description = description;
        this.algorithm = algorithm;
        this.defaultPortStart = dstPort;
        this.defaultPortEnd = dstPort;
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }
    
    @Override
    public int getDefaultPortStart() { 
        return defaultPortStart;
    }

    @Override
    public int getDefaultPortEnd() {
        return defaultPortEnd;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setDescription(String description) {
        this.description = description;
    }  
}
