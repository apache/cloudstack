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

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.utils.db.GenericDao;


@Entity
@Table(name = "s2s_vpn_connection")
public class Site2SiteVpnConnectionVO implements Site2SiteVpnConnection, InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "vpn_gateway_id")
    private long vpnGatewayId;

    @Column(name = "customer_gateway_id")
    private long customerGatewayId;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "passive")
    private boolean passive;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    public Site2SiteVpnConnectionVO() {
    }

    public Site2SiteVpnConnectionVO(long accountId, long domainId, long vpnGatewayId, long customerGatewayId, boolean passive) {
        uuid = UUID.randomUUID().toString();
        setVpnGatewayId(vpnGatewayId);
        setCustomerGatewayId(customerGatewayId);
        setState(State.Pending);
        this.accountId = accountId;
        this.domainId = domainId;
        this.passive = passive;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getVpnGatewayId() {
        return vpnGatewayId;
    }

    public void setVpnGatewayId(long vpnGatewayId) {
        this.vpnGatewayId = vpnGatewayId;
    }

    @Override
    public long getCustomerGatewayId() {
        return customerGatewayId;
    }

    public void setCustomerGatewayId(long customerGatewayId) {
        this.customerGatewayId = customerGatewayId;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public boolean isPassive() {
        return passive;
    }

    public void setPassive(boolean passive) {
        this.passive = passive;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setDisplay(boolean display) {
        this.display = display;
    }

    @Override
    public boolean isDisplay() {
        return display;
    }

    @Override
    public Class<?> getEntityType() {
        return Site2SiteVpnConnection.class;
    }
}
