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
package com.cloud.usage;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "usage_network_offering")
public class UsageNetworkOfferingVO {

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "vm_instance_id")
    private long vmInstanceId;

    @Column(name = "network_offering_id")
    private Long networkOfferingId;

    @Column(name = "is_default")
    private boolean isDefault = false;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    @Column(name = "deleted")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date deleted = null;

    @Column(name = "nic_id")
    private Long nicId;

    protected UsageNetworkOfferingVO() {
    }

    public UsageNetworkOfferingVO(long zoneId, long accountId, long domainId, long vmInstanceId, long networkOfferingId, long nicId, boolean isDefault, Date created,
            Date deleted) {
        this.zoneId = zoneId;
        this.accountId = accountId;
        this.domainId = domainId;
        this.vmInstanceId = vmInstanceId;
        this.networkOfferingId = networkOfferingId;
        this.isDefault = isDefault;
        this.created = created;
        this.deleted = deleted;
        this.nicId = nicId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public long getAccountId() {
        return accountId;
    }

    public long getDomainId() {
        return domainId;
    }

    public long getVmInstanceId() {
        return vmInstanceId;
    }

    public Long getNetworkOfferingId() {
        return networkOfferingId;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Date getCreated() {
        return created;
    }

    public Date getDeleted() {
        return deleted;
    }

    public void setDeleted(Date deleted) {
        this.deleted = deleted;
    }

    public Long getNicId() {
        return nicId;
    }

    public void setNicId(Long nicId) {
        this.nicId = nicId;
    }
}
