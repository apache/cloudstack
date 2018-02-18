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
package org.apache.cloudstack.framework.jobs.impl;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "sync_queue_item")
public class SyncQueueItemVO implements SyncQueueItem, InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id = null;

    @Column(name = "queue_id")
    private Long queueId;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "content_id")
    private Long contentId;

    @Column(name = "queue_proc_msid")
    private Long lastProcessMsid;

    @Column(name = "queue_proc_number")
    private Long lastProcessNumber;

    @Column(name = "queue_proc_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastProcessTime;

    @Column(name = "created")
    private Date created;

    @Override
    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public Long getLastProcessMsid() {
        return lastProcessMsid;
    }

    public void setLastProcessMsid(Long lastProcessMsid) {
        this.lastProcessMsid = lastProcessMsid;
    }

    public Long getLastProcessNumber() {
        return lastProcessNumber;
    }

    public void setLastProcessNumber(Long lastProcessNumber) {
        this.lastProcessNumber = lastProcessNumber;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("SyncQueueItemVO {id:").append(getId()).append(", queueId: ").append(getQueueId());
        sb.append(", contentType: ").append(getContentType());
        sb.append(", contentId: ").append(getContentId());
        sb.append(", lastProcessMsid: ").append(getLastProcessMsid());
        sb.append(", lastprocessNumber: ").append(getLastProcessNumber());
        sb.append(", lastProcessTime: ").append(getLastProcessTime());
        sb.append(", created: ").append(getCreated());
        sb.append("}");
        return sb.toString();
    }

    public Date getLastProcessTime() {
        return lastProcessTime;
    }

    public void setLastProcessTime(Date lastProcessTime) {
        this.lastProcessTime = lastProcessTime;
    }
}
