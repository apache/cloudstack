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

package org.apache.cloudstack.region.gslb;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.region.ha.GlobalLoadBalancerRule;

@Entity
@Table(name = ("global_load_balancing_rules"))
public class GlobalLoadBalancerRuleVO implements GlobalLoadBalancerRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "name")
    private String name;

    @Column(name = "description", length = 4096)
    private String description;

    @Column(name = "algorithm")
    private String algorithm;

    @Column(name = "persistence")
    private String persistence;

    @Column(name = "gslb_domain_name")
    private String gslbDomain;

    @Column(name = "service_type")
    private String serviceType;

    @Column(name = "region_id")
    private int region;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id", updatable = false)
    long domainId;

    @Column(name = "uuid")
    String uuid;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "state")
    GlobalLoadBalancerRule.State state;

    public GlobalLoadBalancerRuleVO() {
        uuid = UUID.randomUUID().toString();
    }

    public GlobalLoadBalancerRuleVO(String name, String description, String gslbDomain, String algorithm, String persistence, String serviceType, int regionId,
            long accountId, long domainId, State state) {
        this.name = name;
        this.description = description;
        region = regionId;
        this.algorithm = algorithm;
        this.gslbDomain = gslbDomain;
        this.persistence = persistence;
        this.accountId = accountId;
        this.domainId = domainId;
        this.serviceType = serviceType;
        uuid = UUID.randomUUID().toString();
        this.state = state;
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
    public String getPersistence() {
        return persistence;
    }

    @Override
    public int getRegion() {
        return region;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setPersistence(String persistence) {
        this.persistence = persistence;
    }

    @Override
    public String getGslbDomain() {
        return gslbDomain;
    }

    public void setGslbDomain(String gslbDomain) {
        this.gslbDomain = gslbDomain;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public void setState(GlobalLoadBalancerRule.State state) {
        this.state = state;
    }

    @Override
    public GlobalLoadBalancerRule.State getState() {
        return state;
    }

    @Override
    public Class<?> getEntityType() {
        return GlobalLoadBalancerRule.class;
    }
}
