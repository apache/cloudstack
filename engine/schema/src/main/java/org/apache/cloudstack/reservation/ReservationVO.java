//
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
//
package org.apache.cloudstack.reservation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.user.ResourceReservation;

import com.cloud.configuration.Resource;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import java.util.Date;

@Entity
@Table(name = "resource_reservation")
public class ReservationVO implements ResourceReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "resource_type", nullable = false)
    Resource.ResourceType resourceType;

    @Column(name = "tag")
    String tag;

    @Column(name = "resource_id")
    Long resourceId;

    @Column(name = "amount")
    long amount;

    @Column(name = "mgmt_server_id")
    Long managementServerId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    protected ReservationVO() {
    }

    public ReservationVO(Long accountId, Long domainId, Resource.ResourceType resourceType, String tag, Long delta) {
        if (delta == null) {
            throw new CloudRuntimeException("resource reservations can not be made for null resources");
        }
        this.accountId = accountId;
        this.domainId = domainId;
        this.resourceType = resourceType;
        this.tag = tag;
        this.amount = delta;
        this.managementServerId = ManagementServerNode.getManagementServerId();
    }

    public ReservationVO(Long accountId, Long domainId, Resource.ResourceType resourceType, Long delta) {
        this(accountId, domainId, resourceType, null, delta);
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public Long getAccountId() {
        return accountId;
    }

    @Override
    public Long getDomainId() {
        return domainId;
    }

    @Override
    public Resource.ResourceType getResourceType() {
        return resourceType;
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public Long getReservedAmount() {
        return amount;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Long getManagementServerId() {
        return managementServerId;
    }
}
