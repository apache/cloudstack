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

package com.cloud.dc;

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
@Table(name = "dc_ip6_guest_prefix")
public class DataCenterGuestIpv6PrefixVO implements DataCenterGuestIpv6Prefix {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    String uuid;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "prefix")
    private String prefix;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name= GenericDao.REMOVED_COLUMN)
    private Date removed;

    public DataCenterGuestIpv6PrefixVO(long dcId, String prefix) {
        this();
        this.dataCenterId = dcId;
        this.prefix = prefix;
        this.created = new Date();
    }

    protected DataCenterGuestIpv6PrefixVO() {
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

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Date getCreated() {
        return created;
    }


}
