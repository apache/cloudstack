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

import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "cluster_drs_plan")
public class ClusterDrsPlanVO implements ClusterDrsPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Column(name = "uuid", nullable = false)
    String uuid;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = "cluster_id")
    private long clusterId;

    @Column(name = "event_id")
    private long eventId;

    @Column(name = "type")
    private Type type;

    @Column(name = "status")
    private Status status;

    public ClusterDrsPlanVO(long clusterId, long eventId, Type type, Status status) {
        uuid = UUID.randomUUID().toString();
        this.clusterId = clusterId;
        this.eventId = eventId;
        this.type = type;
        this.status = status;
    }

    protected ClusterDrsPlanVO() {
        uuid = UUID.randomUUID().toString();
    }

    public long getId() {
        return id;
    }

    public long getEventId() {
        return eventId;
    }

    public long getClusterId() {
        return clusterId;
    }

    public Type getType() {
        return type;
    }

    public Date getCreated() {
        return created;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public String getUuid() {
        return uuid;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
