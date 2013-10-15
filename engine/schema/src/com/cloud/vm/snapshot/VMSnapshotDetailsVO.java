/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.vm.snapshot;

import org.apache.cloudstack.api.InternalIdentity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

@Entity
@Table(name = "vm_snapshot_details")
public class VMSnapshotDetailsVO implements InternalIdentity {
    @Id
    @TableGenerator(name = "vm_snapshot_details_seq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "vm_snapshot_details_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    private long id;

    @Column(name = "vm_snapshot_id")
    Long vmSnapshotId;

    @Column(name = "name")
    String name;

    @Column(name = "value")
    String value;

    public VMSnapshotDetailsVO() {

    }

    public VMSnapshotDetailsVO(Long vmSnapshotId, String name, String value) {
        this.vmSnapshotId = vmSnapshotId;
        this.name = name;
        this.value = value;
    }

    public Long getVmSnapshotId() {
        return this.vmSnapshotId;
    }

    public void setVmSnapshotId(Long vmSnapshotId) {
        this.vmSnapshotId = vmSnapshotId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public long getId() {
       return id;
    }
}
