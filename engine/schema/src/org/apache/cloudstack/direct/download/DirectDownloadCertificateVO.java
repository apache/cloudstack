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
package org.apache.cloudstack.direct.download;

import com.cloud.hypervisor.Hypervisor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "direct_download_certificate")
public class DirectDownloadCertificateVO implements DirectDownloadCertificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "alias")
    private String alias;

    @Column(name = "certificate", length = 65535)
    private String certificate;

    @Column(name = "hypervisor_type")
    private Hypervisor.HypervisorType hypervisorType;

    @Column(name = "zone_id")
    private Long zoneId;

    public DirectDownloadCertificateVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public void setHypervisorType(Hypervisor.HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public DirectDownloadCertificateVO(String alias, String certificate,
                                       Hypervisor.HypervisorType hypervisorType, Long zoneId) {
        this();
        this.alias = alias;
        this.certificate = certificate;
        this.hypervisorType = hypervisorType;
        this.zoneId = zoneId;
    }

    @Override
    public String getCertificate() {
        return certificate;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

}
