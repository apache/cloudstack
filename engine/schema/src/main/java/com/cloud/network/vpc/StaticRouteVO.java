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
import javax.persistence.Transient;

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
    Long vpcGatewayId;

    @Column(name = "next_hop")
    private String nextHop;

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

    @Transient
    boolean forVpn = false;

    /**
     * @param vpcGatewayId
     * @param cidr
     * @param vpcId
     * @param accountId TODO
     * @param domainId TODO
     */
    public StaticRouteVO(Long vpcGatewayId, String cidr, Long vpcId, long accountId, long domainId, String nextHop) {
        super();
        this.vpcGatewayId = vpcGatewayId;
        this.cidr = cidr;
        state = State.Staged;
        this.vpcId = vpcId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.nextHop = nextHop;
        uuid = UUID.randomUUID().toString();
    }

    public StaticRouteVO(String cidr, Long vpcId, long accountId, long domainId, String nextHop, State state, boolean forVpn) {
        super();
        this.cidr = cidr;
        this.state = state;
        this.vpcId = vpcId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.nextHop = nextHop;
        uuid = UUID.randomUUID().toString();
        this.forVpn = forVpn;
    }

    @Override
    public Long getVpcGatewayId() {
        return vpcGatewayId;
    }

    @Override
    public String getNextHop() {
        return nextHop;
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

    public boolean isForVpn() {
        return forVpn;
    }
}
