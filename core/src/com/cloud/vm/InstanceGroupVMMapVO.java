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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

@Entity
@Table(name=("instance_group_vm_map"))
@SecondaryTables({
@SecondaryTable(name="user_vm",
        pkJoinColumns={@PrimaryKeyJoinColumn(name="instance_id", referencedColumnName="id")}),      
@SecondaryTable(name="instance_group", 
		pkJoinColumns={@PrimaryKeyJoinColumn(name="group_id", referencedColumnName="id")})
		})
public class InstanceGroupVMMapVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Column(name="group_id")
    private long groupId;

    @Column(name="instance_id")
    private long instanceId;
    

    public InstanceGroupVMMapVO() { }

    public InstanceGroupVMMapVO(long groupId, long instanceId) {
        this.groupId = groupId;
        this.instanceId = instanceId;
    }

    public Long getId() {
        return id;
    }

    public long getGroupId() {
        return groupId;
    }

    public long getInstanceId() {
        return instanceId;
    }

}