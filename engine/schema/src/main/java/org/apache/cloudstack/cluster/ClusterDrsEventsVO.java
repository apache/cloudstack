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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "cluster_drs_events")
public class ClusterDrsEventsVO implements ClusterDrsEvents {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "event_id")
    private long eventId;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "execution_date")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date executionDate;

    @Column(name = "type")
    private Type type;

    @Column(name = "result")
    private Result result;

    public ClusterDrsEventsVO(long clusterId, long eventId, Date executionDate, Long jobId, Type type, Result result) {
        this.clusterId = clusterId;
        this.eventId = eventId;
        this.executionDate = executionDate;
        this.jobId = jobId;
        this.type = type;
        this.result = result;
    }

    protected ClusterDrsEventsVO() {
    }

    public long getClusterId() {
        return clusterId;
    }

    public Date getExecutionDate() {
        return executionDate;
    }

    public Type getType() {
        return type;
    }
}
