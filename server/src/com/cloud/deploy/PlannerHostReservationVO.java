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
package com.cloud.deploy;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "op_host_planner_reservation")
public class PlannerHostReservationVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="host_id")
    private Long hostId;

    @Column(name="data_center_id")
    private Long dataCenterId;

    @Column(name="pod_id")
    private Long podId;

    @Column(name="cluster_id")
    private Long clusterId;

    @Column(name = "resource_type")
    private short resourceType;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "deployment_planner")
    private String deploymentPlanner;

    public PlannerHostReservationVO() {
    }

    public PlannerHostReservationVO(Long hostId, Long dataCenterId, Long podId, Long clusterId, short resourceType,
            String planner) {
        this.hostId = hostId;
        this.dataCenterId = dataCenterId;
        this.podId = podId;
        this.clusterId = clusterId;
        this.resourceType = resourceType;
        this.deploymentPlanner = planner;
    }

    @Override
    public long getId() {
        return id;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public Long getDataCenterId() {
        return dataCenterId;
    }
    public void setDataCenterId(Long dataCenterId) {
        this.dataCenterId = dataCenterId;
    }

    public Long getPodId() {
        return podId;
    }
    public void setPodId(long podId) {
        this.podId = new Long(podId);
    }

    public Long getClusterId() {
        return clusterId;
    }
    public void setClusterId(long clusterId) {
        this.clusterId = new Long(clusterId);
    }

    public short getResourceType() {
        return resourceType;
    }

    public void setResourceType(short resourceType) {
        this.resourceType = resourceType;
    }

    public String getDeploymentPlanner() {
        return deploymentPlanner;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

}
