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

package com.cloud.network.dao;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.cloud.network.rules.ApplicationLoadBalancerRule;
import com.cloud.utils.net.Ip;

/**
 * This VO represent Internal Load Balancer rule.
 * Instead of pointing to the public ip address id directly as External Load Balancer rule does, it refers to the ip address by its value/sourceNetworkid
 *
 */
@Entity
@Table(name=("load_balancing_rules"))
@DiscriminatorValue(value="LoadBalancing")
@PrimaryKeyJoinColumn(name="id")
public class ApplicationLoadBalancerRuleVO extends LoadBalancerVO implements ApplicationLoadBalancerRule{
    
    
    @Column(name="source_ip_address_network_id")
    String sourceIpNetworkUuid;
    
    @Id
    @Column(name="source_ip_address")
    @Enumerated(value=EnumType.STRING)
    private Ip sourceIpAddress = null;
    

    public ApplicationLoadBalancerRuleVO() {  
    }
    
    public ApplicationLoadBalancerRuleVO(String xId, String name, String description, int srcPort, int dstPort, String algorithm, long networkId,
            long accountId, long domainId, Ip sourceIp, String sourceIpNtwkUuid) {
        
        super(xId, name, description, srcPort, dstPort, algorithm, networkId, accountId, domainId);
        this.sourceIpAddress = sourceIp;
        this.sourceIpNetworkUuid = sourceIpNtwkUuid;
    }
    
    @Override
    public String getSourceIpNetworkUuid() {
        return sourceIpNetworkUuid;
    }

    @Override
    public Ip getSourceIpAddress() {
        return sourceIpAddress;
    }

    @Override
    public int getInstancePort() {
        return super.getDefaultPortStart();
    }
}
