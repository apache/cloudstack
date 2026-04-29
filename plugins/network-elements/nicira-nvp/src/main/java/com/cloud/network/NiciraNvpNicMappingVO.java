//
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
//

package com.cloud.network;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "nicira_nvp_nic_map")
public class NiciraNvpNicMappingVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "logicalswitch")
    private String logicalSwitchUuid;

    @Column(name = "logicalswitchport")
    private String logicalSwitchPortUuid;

    @Column(name = "nic")
    private String nicUuid;

    public NiciraNvpNicMappingVO() {
    }

    public NiciraNvpNicMappingVO(final String logicalSwitchUuid, final String logicalSwitchPortUuid, final String nicUuid) {
        this.logicalSwitchUuid = logicalSwitchUuid;
        this.logicalSwitchPortUuid = logicalSwitchPortUuid;
        this.nicUuid = nicUuid;
    }

    public String getLogicalSwitchUuid() {
        return logicalSwitchUuid;
    }

    public void setLogicalSwitchUuid(final String logicalSwitchUuid) {
        this.logicalSwitchUuid = logicalSwitchUuid;
    }

    public String getLogicalSwitchPortUuid() {
        return logicalSwitchPortUuid;
    }

    public void setLogicalSwitchPortUuid(final String logicalSwitchPortUuid) {
        this.logicalSwitchPortUuid = logicalSwitchPortUuid;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public void setNicUuid(String nicUuid) {
        this.nicUuid = nicUuid;
    }

    @Override
    public long getId() {
        return id;
    }

}
