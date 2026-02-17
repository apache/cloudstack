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

package org.apache.cloudstack.kms;

import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "kms_hsm_profiles")
public class HSMProfileVO implements HSMProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "domain_id")
    private Long domainId;

    @Column(name = "zone_id")
    private Long zoneId;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = "system")
    private boolean system;

    @Column(name = "created")
    private Date created;

    @Column(name = "removed")
    private Date removed;

    public HSMProfileVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
        this.system = false;
    }

    public HSMProfileVO(String name, String protocol, Long accountId, Long domainId, Long zoneId, String vendorName) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.protocol = protocol;
        this.accountId = accountId;
        this.domainId = domainId;
        this.zoneId = zoneId;
        this.vendorName = vendorName;
        this.enabled = true;
        this.system = false;
        this.created = new Date();
    }

    @Override
    public String toString() {
        return String.format("HSMProfileVO %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                        this, "id", "uuid", "name", "protocol", "system", "enabled"));
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public long getAccountId() {
        return accountId == null ? -1 : accountId;
    }

    @Override
    public long getDomainId() {
        return domainId == null ? -1 : domainId;
    }

    @Override
    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public Class<?> getEntityType() {
        return HSMProfile.class;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }
}
