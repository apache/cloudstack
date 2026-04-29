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

import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Column;


@Entity
@Table(name = "op_router_monitoring_services")
public class OpRouterMonitorServiceVO implements InternalIdentity {

    @Id
    @Column(name="vm_id")
    Long id;

    @Column(name="router_name")
    private String name;

    @Column(name="last_alert_timestamp")
    private String lastAlertTimestamp;


    public OpRouterMonitorServiceVO() {}

    public OpRouterMonitorServiceVO(long vmId, String name, String lastAlertTimestamp) {
        this.id = vmId;
        this.name = name;
        this.lastAlertTimestamp = lastAlertTimestamp;
    }


    @Override
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLastAlertTimestamp() {
        return lastAlertTimestamp;
    }

    public void setLastAlertTimestamp (String timestamp) {
        this.lastAlertTimestamp = timestamp;
    }

}
