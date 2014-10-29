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
package com.cloud.network;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import com.cloud.network.rules.HealthCheckPolicy;

@Entity
@Table(name = ("load_balancer_healthcheck_policies"))
@PrimaryKeyJoinColumn(name = "load_balancer_id", referencedColumnName = "id")
public class LBHealthCheckPolicyVO implements HealthCheckPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "load_balancer_id")
    private long loadBalancerId;

    @Column(name = "pingpath")
    private String pingPath;

    @Column(name = "description")
    private String description;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "response_time")
    private int responseTime;

    @Column(name = "healthcheck_interval")
    private int healthcheckInterval;

    @Column(name = "healthcheck_thresshold")
    private int healthcheckThresshold;

    @Column(name = "unhealth_thresshold")
    private int unhealthThresshold;

    @Column(name = "revoke")
    private boolean revoke = false;

    @Column(name = "display", updatable = true, nullable = false)
    protected boolean display = true;

    protected LBHealthCheckPolicyVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public LBHealthCheckPolicyVO(long loadBalancerId, String pingPath, String description, int responseTime, int healthcheckInterval, int healthcheckThresshold,
            int unhealthThresshold) {
        this.loadBalancerId = loadBalancerId;

        if (pingPath == null || pingPath.isEmpty())
            this.pingPath = "/";
        else
            this.pingPath = pingPath;

        if (responseTime == 0)
            this.responseTime = 2;
        else
            this.responseTime = responseTime;

        if (healthcheckInterval == 0)
            this.healthcheckInterval = 5;
        else
            this.healthcheckInterval = healthcheckInterval;

        if (healthcheckThresshold == 0)
            this.healthcheckThresshold = 2;
        else
            this.healthcheckThresshold = healthcheckThresshold;

        if (unhealthThresshold == 0)
            this.unhealthThresshold = 1;
        else
            this.unhealthThresshold = unhealthThresshold;
        this.uuid = UUID.randomUUID().toString();

    }

    @Override
    public int getResponseTime() {
        return responseTime;
    }

    @Override
    public int getHealthcheckInterval() {
        return healthcheckInterval;
    }

    @Override
    public int getHealthcheckThresshold() {
        return healthcheckThresshold;
    }

    @Override
    public int getUnhealthThresshold() {
        return unhealthThresshold;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getLoadBalancerId() {
        return loadBalancerId;
    }

    @Override
    public String getpingpath() {
        return pingPath;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isRevoke() {
        return revoke;
    }

    public void setRevoke(boolean revoke) {
        this.revoke = revoke;
    }

    @Override
    public String getUuid() {
        return this.uuid;
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
}
