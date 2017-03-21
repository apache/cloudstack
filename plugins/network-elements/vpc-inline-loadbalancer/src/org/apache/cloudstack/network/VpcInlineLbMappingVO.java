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
package org.apache.cloudstack.network;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "vpc_inline_load_balancer_map")
public class VpcInlineLbMappingVO implements InternalIdentity, Identity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="ip_address_id")
    private long publicIpId;

    @Column(name="vm_id")
    private long vmId;

    @Column(name="nic_id")
    private long nicId;

    @Column(name="nic_secondary_ip_id")
    private long nicSecondaryIpId;

    public VpcInlineLbMappingVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public VpcInlineLbMappingVO(long publicIpId, long vmId, long nicId, long nicSecondaryIpId) {
        this.publicIpId = publicIpId;
        this.vmId = vmId;
        this.nicId = nicId;
        this.nicSecondaryIpId = nicSecondaryIpId;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public long getPublicIpId() {
        return publicIpId;
    }

    public long getVmId() {
        return vmId;
    }

    public long getNicId() {
        return nicId;
    }

    public long getNicSecondaryIpId() {
        return nicSecondaryIpId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("uuid", uuid)
                .append("publicIpId", publicIpId)
                .append("vmId", vmId)
                .append("nicId", nicId)
                .append("nicSecondaryIpId", nicSecondaryIpId)
                .toString();
    }
}
