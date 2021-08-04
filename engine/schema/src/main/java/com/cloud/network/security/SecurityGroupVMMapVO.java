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

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.vm.VirtualMachine.State;

@Entity
@Table(name = "security_group_vm_map")
@SecondaryTables({@SecondaryTable(name = "nics", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "instance_id", referencedColumnName = "instance_id")}),
    @SecondaryTable(name = "vm_instance", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "instance_id", referencedColumnName = "id")}),
    @SecondaryTable(name = "security_group", pkJoinColumns = {@PrimaryKeyJoinColumn(name = "security_group_id", referencedColumnName = "id")})})
public class SecurityGroupVMMapVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "security_group_id")
    private long securityGroupId;

    @Column(name = "instance_id")
    private long instanceId;

    @Column(name = "ip4_address", table = "nics", insertable = false, updatable = false)
    private String guestIpAddress;

    @Column(name = "state", table = "vm_instance", insertable = false, updatable = false)
    private State vmState;

    @Column(name = "name", table = "security_group", insertable = false, updatable = false)
    private String groupName;

    public SecurityGroupVMMapVO() {
    }

    public SecurityGroupVMMapVO(long securityGroupId, long instanceId) {
        this.securityGroupId = securityGroupId;
        this.instanceId = instanceId;
    }

    @Override
    public long getId() {
        return id;
    }

    public long getSecurityGroupId() {
        return securityGroupId;
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
