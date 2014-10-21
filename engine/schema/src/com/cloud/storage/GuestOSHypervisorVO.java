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
package com.cloud.storage;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "guest_os_hypervisor")
public class GuestOSHypervisorVO implements GuestOSHypervisor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "guest_os_id")
    long guestOsId;

    @Column(name = "guest_os_name")
    String guestOsName;

    @Column(name = "hypervisor_type")
    String hypervisorType;

    @Column(name = "hypervisor_version")
    String hypervisorVersion;

    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = "is_user_defined")
    private boolean isUserDefined;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    @Override
    public String getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public String getGuestOsName() {
        return guestOsName;
    }

    @Override
    public long getGuestOsId() {
        return guestOsId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public void setGuestOsId(long guestOsId) {
        this.guestOsId = guestOsId;
    }

    public void setGuestOsName(String guestOsName) {
        this.guestOsName = guestOsName;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public void setHypervisorVersion(String hypervisorVersion) {
        this.hypervisorVersion = hypervisorVersion;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    @Override
    public boolean getIsUserDefined() {
        return isUserDefined;
    }

    public void setIsUserDefined(boolean isUserDefined) {
        this.isUserDefined = isUserDefined;
    }
}
