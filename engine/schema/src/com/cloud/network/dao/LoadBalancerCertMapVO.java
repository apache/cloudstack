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
package com.cloud.network.dao;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "load_balancer_cert_map")
public class LoadBalancerCertMapVO implements InternalIdentity {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "load_balancer_id")
    private Long lbId;

    @Column(name = "certificate_id")
    private Long certId;

    @Column(name = "revoke")
    private boolean revoke = false;

    public LoadBalancerCertMapVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public LoadBalancerCertMapVO(Long lbId, Long certId, boolean revoke) {

        this.lbId = lbId;
        this.certId = certId;
        this.revoke = revoke;
        this.uuid = UUID.randomUUID().toString();
    }

    // Getters
    @Override
    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public Long getLbId() {
        return lbId;
    }

    public Long getCertId() {
        return certId;
    }

    public boolean isRevoke() {
        return revoke;
    }

    //Setters
    public void setLbId(Long lbId) {
        this.lbId = lbId;
    }

    public void setCertId(Long certId) {
        this.certId = certId;
    }

    public void setRevoke(boolean revoke) {
        this.revoke = revoke;
    }
}
