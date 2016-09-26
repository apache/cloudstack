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
package org.apache.cloudstack.storage.datastore.db;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ImageStore;
import com.cloud.storage.ScopeType;
import com.cloud.utils.UriUtils;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "image_store")
public class ImageStoreVO implements ImageStore {
    @Id
    @TableGenerator(name = "image_store_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "image_store_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "uuid", nullable = false)
    private String uuid;

    @Column(name = "protocol", nullable = false)
    private String protocol;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "image_provider_name", nullable = false)
    private String providerName;

    @Column(name = "data_center_id")
    private Long dcId;

    @Column(name = "scope")
    @Enumerated(value = EnumType.STRING)
    private ScopeType scope;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name = "role")
    @Enumerated(value = EnumType.STRING)
    private DataStoreRole role;

    @Column(name = "parent")
    private String parent;

    @Column(name = "total_size")
    private Long totalSize;

    @Column(name = "used_bytes")
    private Long usedBytes;

    public DataStoreRole getRole() {
        return role;
    }

    public void setRole(DataStoreRole role) {
        this.role = role;
    }

    @Override
    public long getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getProviderName() {
        return this.providerName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProviderName(String provider) {
        this.providerName = provider;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getProtocol() {
        return this.protocol;
    }

    public void setDataCenterId(Long dcId) {
        this.dcId = dcId;
    }

    @Override
    public Long getDataCenterId() {
        return this.dcId;
    }

    public ScopeType getScope() {
        return this.scope;
    }

    public void setScope(ScopeType scope) {
        this.scope = scope;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public String getUrl() {
        String updatedUrl = url;
        if ("cifs".equalsIgnoreCase(this.protocol)) {
            updatedUrl = UriUtils.getUpdateUri(updatedUrl, false);
        }
        return updatedUrl;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getRemoved() {
        return removed;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Long getUsedBytes() {
        return usedBytes;
    }

    public void setUsedBytes(Long usedBytes) {
        this.usedBytes = usedBytes;
    }
}
