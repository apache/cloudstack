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
package com.cloud.network.element;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.network.OvsProvider;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "ovs_providers")
public class OvsProviderVO implements OvsProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "nsp_id")
    private long nspId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "enabled")
    private boolean enabled;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    public OvsProviderVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public OvsProviderVO(long nspId) {
        this.nspId = nspId;
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public long getNspId() {
        return nspId;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setNspId(long nspId) {
        this.nspId = nspId;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
