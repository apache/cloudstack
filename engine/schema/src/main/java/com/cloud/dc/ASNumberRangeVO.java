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

import com.cloud.bgp.ASNumberRange;
import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

@Entity
@Table(name = "as_number_range")
public class ASNumberRangeVO implements ASNumberRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "data_center_id")
    private long dataCenterId;

    @Column(name = "start_as_number")
    private long startASNumber;

    @Column(name = "end_as_number")
    private long endASNumber;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    public ASNumberRangeVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = GregorianCalendar.getInstance().getTime();
    }

    public ASNumberRangeVO(long dataCenterId, long startASNumber, long endASNumber) {
        this();
        this.dataCenterId = dataCenterId;
        this.startASNumber = startASNumber;
        this.endASNumber = endASNumber;
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
    public long getDataCenterId() {
        return dataCenterId;
    }

    @Override
    public long getStartASNumber() {
        return startASNumber;
    }

    @Override
    public long getEndASNumber() {
        return endASNumber;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }
}
