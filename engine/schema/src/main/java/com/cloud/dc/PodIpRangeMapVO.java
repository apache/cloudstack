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

import com.cloud.utils.db.GenericDao;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.GenerationType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "pod_ip_range_map")
public class PodIpRangeMapVO implements PodIpRangeMap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "pod_id")
    private long podId;

    @Column(name = "start_ip")
    private String startIp;

    @Column(name = "end_ip")
    private String endIp;

    @Column(name = "vlan_id")
    private Long vlanId;

    @Column(name = "forsystemvms")
    private boolean forSystemVms;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "uuid")
    private String uuid;

    public PodIpRangeMapVO(long podId, String startIp, String endIp, Long vlanId, boolean forSystemVms) {
        this.podId = podId;
        this.startIp = startIp;
        this.endIp = endIp;
        this.vlanId = vlanId;
        this.forSystemVms = forSystemVms;
        this.uuid = UUID.randomUUID().toString();
    }

    protected PodIpRangeMapVO() { this.uuid = UUID.randomUUID().toString(); }

    @Override
    public long getPodId() {
        return podId;
    }

    @Override
    public String getStartIp() {
        return startIp;
    }

    @Override
    public String getEndIp() {
        return endIp;
    }

    @Override
    public boolean getForSystemVms() {
        return forSystemVms;
    }

    @Override
    public Long getVlanId() { return vlanId; }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() { return id; }
}
