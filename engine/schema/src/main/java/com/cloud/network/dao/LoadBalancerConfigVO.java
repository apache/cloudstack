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

import com.cloud.utils.db.GenericDao;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.network.lb.LoadBalancerConfig;
import org.apache.cloudstack.network.lb.LoadBalancerConfigKey;

@Entity
@Table(name = "load_balancer_config")
public class LoadBalancerConfigVO implements InternalIdentity, LoadBalancerConfig {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "scope")
    private Scope scope;

    @Column(name = "network_id")
    private Long networkId;

    @Column(name = "vpc_id")
    private Long vpcId;

    @Column(name = "load_balancer_id")
    private Long loadBalancerId;

    @Column(name = "name")
    private String name;

    @Column(name = "value")
    private String value;

    @Transient
    private String defaultValue;

    @Transient
    private String displayText;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public LoadBalancerConfigVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public LoadBalancerConfigVO(Scope scope, Long networkId, Long vpcId, Long loadBalancerId, String name, String value) {
        this.scope = scope;
        this.networkId = networkId;
        this.vpcId = vpcId;
        this.loadBalancerId = loadBalancerId;
        this.name = name;
        this.value = value;
        this.uuid = UUID.randomUUID().toString();
    }

    public LoadBalancerConfigVO(Scope scope, Long networkId, Long vpcId, Long loadBalancerId, LoadBalancerConfigKey config, String value) {
        this.scope = scope;
        this.networkId = networkId;
        this.vpcId = vpcId;
        this.loadBalancerId = loadBalancerId;
        this.name = config.key();
        this.defaultValue = config.defaultValue();
        this.displayText = config.displayText();
        if (value != null) {
            this.value = value;
            this.uuid = UUID.randomUUID().toString();
        }
    }

    // Getters
    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public Long getNetworkId() {
        return networkId;
    }

    @Override
    public Long getVpcId() {
        return vpcId;
    }

    @Override
    public Long getLoadBalancerId() {
        return loadBalancerId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }
}
