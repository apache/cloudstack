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

import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;


@Entity
@Table(name = ("s2s_customer_gateway"))
public class Site2SiteCustomerGatewayVO implements Site2SiteCustomerGateway {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "gateway_ip")
    private String gatewayIp;

    @Column(name = "guest_cidr_list")
    private String guestCidrList;

    @Encrypt
    @Column(name = "ipsec_psk")
    private String ipsecPsk;

    @Column(name = "ike_policy")
    private String ikePolicy;

    @Column(name = "esp_policy")
    private String espPolicy;

    @Column(name = "ike_lifetime")
    private long ikeLifetime;

    @Column(name = "esp_lifetime")
    private long espLifetime;

    @Column(name = "dpd")
    private boolean dpd;

    @Column(name = "force_encap")
    private boolean encap;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public Site2SiteCustomerGatewayVO() {
    }

    public Site2SiteCustomerGatewayVO(String name, long accountId, long domainId, String gatewayIp, String guestCidrList, String ipsecPsk, String ikePolicy,
            String espPolicy, long ikeLifetime, long espLifetime, boolean dpd, boolean encap) {
        this.name = name;
        this.gatewayIp = gatewayIp;
        this.guestCidrList = guestCidrList;
        this.ipsecPsk = ipsecPsk;
        this.ikePolicy = ikePolicy;
        this.espPolicy = espPolicy;
        this.ikeLifetime = ikeLifetime;
        this.espLifetime = espLifetime;
        this.dpd = dpd;
        this.encap = encap;
        uuid = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.domainId = domainId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getGatewayIp() {
        return gatewayIp;
    }

    public void setGatewayIp(String gatewayIp) {
        this.gatewayIp = gatewayIp;
    }

    @Override
    public String getGuestCidrList() {
        return guestCidrList;
    }

    public void setGuestCidrList(String guestCidrList) {
        this.guestCidrList = guestCidrList;
    }

    @Override
    public String getIpsecPsk() {
        return ipsecPsk;
    }

    public void setIpsecPsk(String ipsecPsk) {
        this.ipsecPsk = ipsecPsk;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public Long getIkeLifetime() {
        return ikeLifetime;
    }

    public void setIkeLifetime(long ikeLifetime) {
        this.ikeLifetime = ikeLifetime;
    }

    @Override
    public Long getEspLifetime() {
        return espLifetime;
    }

    public void setEspLifetime(long espLifetime) {
        this.espLifetime = espLifetime;
    }

    @Override
    public String getIkePolicy() {
        return ikePolicy;
    }

    public void setIkePolicy(String ikePolicy) {
        this.ikePolicy = ikePolicy;
    }

    @Override
    public String getEspPolicy() {
        return espPolicy;
    }

    public void setEspPolicy(String espPolicy) {
        this.espPolicy = espPolicy;
    }

    @Override
    public Boolean getDpd() {
        return dpd;
    }

    public void setDpd(boolean dpd) {
        this.dpd = dpd;
    }

    @Override
    public Boolean getEncap() {
        return encap;
    }

    public void setEncap(boolean encap) {
        this.encap = encap;
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
    public Class<?> getEntityType() {
        return Site2SiteCustomerGateway.class;
    }
}
