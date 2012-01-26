/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
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

    public SecondaryStorageVmVO(long id, long serviceOfferingId, String name, long templateId, HypervisorType hypervisorType, long guestOSId, long dataCenterId, long domainId, long accountId,
            Role role, boolean haEnabled) {
        super(id, serviceOfferingId, null, name, Type.SecondaryStorageVm, templateId, hypervisorType, guestOSId, domainId, accountId, haEnabled);
        this.role = role;
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
