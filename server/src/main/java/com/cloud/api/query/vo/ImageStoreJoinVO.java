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
package com.cloud.api.query.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.utils.db.GenericDao;

/**
 * Image Data Store DB view.
 *
 */
@Entity
@Table(name = "image_store_view")
public class ImageStoreJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "url", length = 2048)
    private String url;

    @Column(name = "protocol")
    private String protocol;

    @Column(name = "image_provider_name", nullable = false)
    private String providerName;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType scope;

    @Column(name = "role")
    @Enumerated(value = EnumType.STRING)
    private DataStoreRole role;

    @Column(name = "readonly")
    private boolean readonly = false;

    @Column(name = "data_center_id")
    private long zoneId;

    @Column(name = "data_center_uuid")
    private String zoneUuid;

    @Column(name = "data_center_name")
    private String zoneName;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public long getZoneId() {
        return zoneId;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getUrl() {
        return url;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getProviderName() {
        return providerName;
    }

    public ScopeType getScope() {
        return scope;
    }

    public DataStoreRole getRole() {
        return role;
    }

    public Date getRemoved() {
        return removed;
    }

    public boolean isReadonly() {
        return readonly;
    }
}
