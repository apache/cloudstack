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
import javax.persistence.Table;

@Entity
@Table(name = "user_vm_clone_setting")
public class UserVmCloneSettingVO {

    @Column(name = "vm_id")
    private Long vmId;

    @Column(name = "clone_type")
    private String cloneType;

    public UserVmCloneSettingVO() {

    }

    public UserVmCloneSettingVO(long id, String cloneType) {
        this.vmId = id;
        this.cloneType = cloneType;
    }

    public long getVmId() {
        return this.vmId;
    }

    public String getCloneType() {
        return this.cloneType;
    }
}
