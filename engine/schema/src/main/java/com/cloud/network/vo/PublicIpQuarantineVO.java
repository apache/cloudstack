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
package com.cloud.network.vo;

import com.cloud.network.PublicIpQuarantine;
import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "quarantined_ips")
public class PublicIpQuarantineVO implements PublicIpQuarantine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "uuid", nullable = false)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "public_ip_address_id", nullable = false)
    private Long publicIpAddressId;

    @Column(name = "previous_owner_id", nullable = false)
    private Long previousOwnerId;

    @Column(name = GenericDao.CREATED_COLUMN, nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    @Column(name = "end_date", nullable = false)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date endDate;

    @Column(name = "removal_reason")
    private String removalReason = null;

    @Column(name = "remover_account_id")
    private Long removerAccountId = null;

    public PublicIpQuarantineVO() {
    }

    public PublicIpQuarantineVO(Long publicIpAddressId, Long previousOwnerId, Date created, Date endDate) {
        this.publicIpAddressId = publicIpAddressId;
        this.previousOwnerId = previousOwnerId;
        this.created = created;
        this.endDate = endDate;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Long getPublicIpAddressId() {
        return publicIpAddressId;
    }

    @Override
    public Long getPreviousOwnerId() {
        return previousOwnerId;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public String getRemovalReason() {
        return removalReason;
    }

    @Override
    public Long getRemoverAccountId() {
        return this.removerAccountId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setRemovalReason(String removalReason) {
        this.removalReason = removalReason;
    }

    public void setRemoverAccountId(Long removerAccountId) {
        this.removerAccountId = removerAccountId;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
