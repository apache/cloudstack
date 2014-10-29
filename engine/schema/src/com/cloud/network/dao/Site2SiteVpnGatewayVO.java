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
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.utils.db.GenericDao;


@Entity
@Table(name = ("s2s_vpn_gateway"))
public class Site2SiteVpnGatewayVO implements Site2SiteVpnGateway {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "addr_id")
    private long addrId;

    @Column(name = "vpc_id")
    private long vpcId;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    public Site2SiteVpnGatewayVO() {
    }

    public Site2SiteVpnGatewayVO(long accountId, long domainId, long addrId, long vpcId) {
        uuid = UUID.randomUUID().toString();
        setAddrId(addrId);
        setVpcId(vpcId);
        this.accountId = accountId;
        this.domainId = domainId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getVpcId() {
        return vpcId;
    }

    public void setVpcId(long vpcId) {
        this.vpcId = vpcId;
    }

    @Override
    public long getAddrId() {
        return addrId;
    }

    public void setAddrId(long addrId) {
        this.addrId = addrId;
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
        return Site2SiteVpnGateway.class;
    }
}
