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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;
import javax.persistence.SecondaryTables;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "instance_group_vm_map")
@SecondaryTables({@SecondaryTable(name = "user_vm", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "instance_id", referencedColumnName = "id")}),
    @SecondaryTable(name = "instance_group", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "group_id", referencedColumnName = "id")})})
public class InstanceGroupVMMapVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "group_id")
    private long groupId;

    @Column(name = "instance_id")
    private long instanceId;

    public InstanceGroupVMMapVO() {
    }

    public InstanceGroupVMMapVO(long groupId, long instanceId) {
        this.groupId = groupId;
        this.instanceId = instanceId;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getGroupId() {
        return groupId;
    }

    public long getInstanceId() {
        return instanceId;
    }

}
