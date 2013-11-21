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
package com.cloud.vm.dao;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.NicSecondaryIp;

@Entity
@Table(name = "nic_secondary_ips")
public class NicSecondaryIpVO implements NicSecondaryIp {

    public NicSecondaryIpVO(Long nicId, String ipaddr, Long vmId, Long accountId, Long domainId, Long networkId) {
        this.nicId = nicId;
        this.vmId = vmId;
        this.ip4Address = ipaddr;
        this.accountId = accountId;
        this.domainId = domainId;
        this.networkId = networkId;
    }

    protected NicSecondaryIpVO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "nicId")
    long nicId;

    @Column(name = "domain_id", updatable = false)
    long domainId;

    @Column(name = "account_id", updatable = false)
    private Long accountId;

    @Column(name = "ip4_address")
    String ip4Address;

    @Column(name = "ip6_address")
    String ip6Address;

    @Column(name = "network_id")
    long networkId;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    @Column(name = "vmId")
    Long vmId;

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public long getNicId() {
        return nicId;
    }

    public void setNicId(long nicId) {
        this.nicId = nicId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getIp4Address() {
        return ip4Address;
    }

    public void setIp4Address(String ip4Address) {
        this.ip4Address = ip4Address;
    }

    public String getIp6Address() {
        return ip6Address;
    }

    public void setIp6Address(String ip6Address) {
        this.ip6Address = ip6Address;
    }

    @Override
    public long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(long networkId) {
        this.networkId = networkId;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public long getVmId() {
        return vmId;
    }

    public void setVmId(Long vmId) {
        this.vmId = vmId;
    }
}
