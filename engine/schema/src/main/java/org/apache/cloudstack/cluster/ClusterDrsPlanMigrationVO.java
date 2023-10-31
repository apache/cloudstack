/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.cluster;

import org.apache.cloudstack.jobs.JobInfo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cluster_drs_plan_migration")
public class ClusterDrsPlanMigrationVO implements ClusterDrsPlanMigration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "plan_id", nullable = false)
    private long planId;

    @Column(name = "vm_id", nullable = false)
    private long vmId;

    @Column(name = "src_host_id", nullable = false)
    private long srcHostId;

    @Column(name = "dest_host_id", nullable = false)
    private long destHostId;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "status")
    private JobInfo.Status status;


    public ClusterDrsPlanMigrationVO(long planId, long vmId, long srcHostId, long destHostId) {
        this.planId = planId;
        this.vmId = vmId;
        this.srcHostId = srcHostId;
        this.destHostId = destHostId;
    }

    protected ClusterDrsPlanMigrationVO() {

    }

    public long getId() {
        return id;
    }

    public long getPlanId() {
        return planId;
    }

    public long getVmId() {
        return vmId;
    }

    public long getSrcHostId() {
        return srcHostId;
    }

    public long getDestHostId() {
        return destHostId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public JobInfo.Status getStatus() {
        return status;
    }

    public void setStatus(JobInfo.Status status) {
        this.status = status;
    }

}
