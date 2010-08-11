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

package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name=("security_group_vm_map"))
public class SecurityGroupVMMapVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="security_group_id")
    private long securityGroupId;

    @Column(name="ip_address")
    private String ipAddress;

    @Column(name="instance_id")
    private long instanceId;

    public SecurityGroupVMMapVO() { }

    public SecurityGroupVMMapVO(long securityGroupId, String ipAddress, long instanceId) {
        this.securityGroupId = securityGroupId;
        this.ipAddress = ipAddress;
        this.instanceId = instanceId;
    }

    public Long getId() {
        return id;
    }

    public long getSecurityGroupId() {
        return securityGroupId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getInstanceId() {
        return instanceId;
    }
}
