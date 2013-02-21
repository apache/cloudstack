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

import com.cloud.region.ha.GlobalLoadBalancer;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name=("global_load_balancing_rules"))
public class GlobalLoadBalancerVO implements GlobalLoadBalancer {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;

    @Column(name="name")
    private String name;

    @Column(name="description", length=4096)
    private String description;

    @Column(name="algorithm")
    private String algorithm;

    @Column(name="persistence")
    private String persistence;

    @Column(name="gslb_domain_name")
    private String gslbDomain;

    @Column(name="region_id")
    private int region;

    @Column(name="account_id")
    long accountId;

    @Column(name="uuid")
    String uuid;

    public GlobalLoadBalancerVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public GlobalLoadBalancerVO(String name, String description, String gslbDomain, String algorithm,
                                String persistence, int regionId, long accountId) {
        this.name =name;
        this.description = description;
        this.region = regionId;
        this.algorithm = algorithm;
        this.gslbDomain = gslbDomain;
        this.persistence = persistence;
        this.accountId = accountId;
        this.uuid = UUID.randomUUID().toString();
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
        this.region =region;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
