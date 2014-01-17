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
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.utils.net.NetUtils;

/**
 * This VO represent Public Load Balancer
 * It references source ip address by its Id.
 * To get the VO for Internal Load Balancer rule, please refer to LoadBalancerRuleVO
 *
 */
@Entity
@Table(name = ("load_balancing_rules"))
@DiscriminatorValue(value = "LoadBalancing")
@PrimaryKeyJoinColumn(name = "id")
public class LoadBalancerVO extends FirewallRuleVO implements LoadBalancer {

    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "algorithm")
    private String algorithm;

    @Column(name = "default_port_start")
    private int defaultPortStart;

    @Column(name = "default_port_end")
    private int defaultPortEnd;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "scheme")
    Scheme scheme = Scheme.Public;

    @Column(name = "lb_protocol")
    String lbProtocol;

    public LoadBalancerVO() {
    }

    public LoadBalancerVO(String xId, String name, String description, long srcIpId, int srcPort, int dstPort, String algorithm, long networkId, long accountId,
            long domainId, String lbProtocol) {
        super(xId, srcIpId, srcPort, NetUtils.TCP_PROTO, networkId, accountId, domainId, Purpose.LoadBalancing, null, null, null, null);
        this.name = name;
        this.description = description;
        this.algorithm = algorithm;
        this.defaultPortStart = dstPort;
        this.defaultPortEnd = dstPort;
        this.scheme = Scheme.Public;
        this.lbProtocol = lbProtocol;
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

    @Override
    public String getLbProtocol() {
        return lbProtocol;
    }

    public void setLbProtocol(String lbProtocol) {
        this.lbProtocol = lbProtocol;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Scheme getScheme() {
        return scheme;
    }
}
