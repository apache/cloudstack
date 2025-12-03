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

package org.apache.cloudstack.datacenter;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "dc_ip4_guest_subnets")
public class DataCenterIpv4GuestSubnetVO implements DataCenterIpv4GuestSubnet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "subnet")
    private String subnet;

    @Column(name = "domain_id")
    Long domainId;

    @Column(name = "account_id")
    Long accountId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name= GenericDao.REMOVED_COLUMN)
    private Date removed;

    public DataCenterIpv4GuestSubnetVO(long dcId, String subnet) {
        this();
        this.dataCenterId = dcId;
        this.subnet = subnet;
        this.created = new Date();
    }

    protected DataCenterIpv4GuestSubnetVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public Long getDataCenterId() {
        return dataCenterId;
    }

    public void setDataCenterId(long dcId) {
        this.dataCenterId = dcId;
    }

    public String getSubnet() {
        return subnet;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    @Override
    public Date getCreated() {
        return created;
    }


}
