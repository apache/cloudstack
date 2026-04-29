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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.network.RouterHealthCheckResult;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.utils.StringUtils;

@Entity
@Table(name = "router_health_check")
public class RouterHealthCheckResultVO implements RouterHealthCheckResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private long id;

    @Column(name = "router_id", updatable = false, nullable = false)
    private long routerId;

    @Column(name = "check_name", updatable = false, nullable = false)
    private String checkName;

    @Column(name = "check_type", updatable = false, nullable = false)
    private String checkType;

    @Column(name = "check_result")
    private VirtualNetworkApplianceService.RouterHealthStatus checkResult;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", updatable = true, nullable = true)
    private Date lastUpdateTime;

    @Column(name = "check_details", updatable = true, nullable = true)
    private byte[] checkDetails;

    protected RouterHealthCheckResultVO() {
    }

    public RouterHealthCheckResultVO(long routerId, String checkName, String checkType) {
        this.routerId = routerId;
        this.checkName = checkName;
        this.checkType = checkType;
    }

    public long getId() {
        return id;
    }

    @Override
    public long getRouterId() {
        return routerId;
    }

    @Override
    public String getCheckName() {
        return checkName;
    }

    @Override
    public String getCheckType() {
        return checkType;
    }

    @Override
    public VirtualNetworkApplianceService.RouterHealthStatus getCheckResult() {
        return checkResult;
    }

    @Override
    public Date getLastUpdateTime() {
        return lastUpdateTime;
    }

    @Override
    public String getParsedCheckDetails() {
        return checkDetails != null ? new String(checkDetails, StringUtils.getPreferredCharset()) : "";
    }

    public byte[] getCheckDetails() {
        return checkDetails;
    }

    public void setCheckResult(VirtualNetworkApplianceService.RouterHealthStatus checkResult) {
        this.checkResult = checkResult;
    }

    public void setLastUpdateTime(Date lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public void setCheckDetails(byte[] checkDetails) {
        this.checkDetails = checkDetails;
    }

    @Override
    public String toString() {
        return super.toString() +
                "- check type: " + checkType +
                ",check name: " + checkName +
                ", check result: " + checkResult +
                ", check last update: " + lastUpdateTime +
                ", details: " + getParsedCheckDetails();
    }
}
