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
package com.cloud.vm;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.hypervisor.Hypervisor.HypervisorType;

/**
 * SecondaryStorageVmVO domain object
 */

@Entity
@Table(name = "secondary_storage_vm")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue(value = "SecondaryStorageVm")
public class SecondaryStorageVmVO extends VMInstanceVO implements SecondaryStorageVm {

    @Column(name = "public_ip_address", nullable = false)
    private String publicIpAddress;

    @Column(name = "public_mac_address", nullable = false)
    private String publicMacAddress;

    @Column(name = "public_netmask", nullable = false)
    private String publicNetmask;

    @Column(name = "guid", nullable = false)
    private String guid;

    @Column(name = "nfs_share", nullable = false)
    private String nfsShare;

    @Column(name = "role", nullable = false)
    @Enumerated(value = EnumType.STRING)
    private Role role;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_update", updatable = true, nullable = true)
    private Date lastUpdateTime;

    public SecondaryStorageVmVO(long id, long serviceOfferingId, String name, long templateId, HypervisorType hypervisorType, long guestOSId, long dataCenterId,
                                long domainId, long accountId, long userId, Role role, boolean haEnabled) {
        super(id, serviceOfferingId, name, name, Type.SecondaryStorageVm, templateId, hypervisorType, guestOSId, domainId, accountId, userId, haEnabled);
        this.role = role;
        this.dataCenterId = dataCenterId;
    }

    protected SecondaryStorageVmVO() {
        super();
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    public void setLastUpdateTime(Date time) {
        this.lastUpdateTime = time;
    }

    @Override
    public String getPublicIpAddress() {
        return this.publicIpAddress;
    }

    @Override
    public String getPublicNetmask() {
        return this.publicNetmask;
    }

    @Override
    public String getPublicMacAddress() {
        return this.publicMacAddress;
    }

    @Override
    public Date getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getGuid() {
        return guid;
    }

    public void setNfsShare(String nfsShare) {
        this.nfsShare = nfsShare;
    }

    public String getNfsShare() {
        return nfsShare;
    }

    public Role getRole() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
