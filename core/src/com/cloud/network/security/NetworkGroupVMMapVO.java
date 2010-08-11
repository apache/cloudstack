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

package com.cloud.network.security;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

import com.cloud.vm.State;

@Entity
@Table(name=("network_group_vm_map"))
@SecondaryTables({
@SecondaryTable(name="user_vm",
        pkJoinColumns={@PrimaryKeyJoinColumn(name="instance_id", referencedColumnName="id")}),
@SecondaryTable(name="vm_instance",
                pkJoinColumns={@PrimaryKeyJoinColumn(name="instance_id", referencedColumnName="id")}),
@SecondaryTable(name="network_group", 
		pkJoinColumns={@PrimaryKeyJoinColumn(name="network_group_id", referencedColumnName="id")})
		})
public class NetworkGroupVMMapVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="network_group_id")
    private long networkGroupId;

    @Column(name="instance_id")
    private long instanceId;
    
    @Column(name="guest_ip_address", table="user_vm", insertable=false, updatable=false)
    private String guestIpAddress;
    
    @Column(name="state", table="vm_instance", insertable=false, updatable=false)
    private State vmState;
    
    @Column(name="name", table="network_group", insertable=false, updatable=false)
    private String groupName;

    public NetworkGroupVMMapVO() { }

    public NetworkGroupVMMapVO(long networkGroupId, long instanceId) {
        this.networkGroupId = networkGroupId;
        this.instanceId = instanceId;
    }

    public Long getId() {
        return id;
    }

    public long getNetworkGroupId() {
        return networkGroupId;
    }

    public String getGuestIpAddress() {
        return guestIpAddress;
    }

    public long getInstanceId() {
        return instanceId;
    }

	public State getVmState() {
		return vmState;
	}

	public String getGroupName() {
		return groupName;
	}
}
