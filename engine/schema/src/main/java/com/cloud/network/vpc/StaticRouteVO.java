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
package com.cloud.network.vpc;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "static_routes")
public class StaticRouteVO implements StaticRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "vpc_gateway_id", updatable = false)
    long vpcGatewayId;

    @Column(name = "cidr")
    private String cidr;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "state")
    State state;

    @Column(name = "vpc_id")
    private Long vpcId;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    protected StaticRouteVO() {
        uuid = UUID.randomUUID().toString();
    }

    /**
     * @param vpcGatewayId
     * @param cidr
     * @param vpcId
     * @param accountId TODO
     * @param domainId TODO
     */
    public StaticRouteVO(long vpcGatewayId, String cidr, Long vpcId, long accountId, long domainId) {
        super();
        this.vpcGatewayId = vpcGatewayId;
        this.cidr = cidr;
        state = State.Staged;
        this.vpcId = vpcId;
        this.accountId = accountId;
        this.domainId = domainId;
        uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getVpcGatewayId() {
        return vpcGatewayId;
    }

    @Override
    public String getCidr() {
        return cidr;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public Long getVpcId() {
        return vpcId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder("StaticRoute[");
        buf.append(uuid).append("|").append(cidr).append("|").append(vpcGatewayId).append("]");
        return buf.toString();
    }

    @Override
    public Class<?> getEntityType() {
        return StaticRoute.class;
    }

    @Override
    public String getName() {
        return null;
    }
}
