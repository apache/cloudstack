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
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.storage.DiskOfferingVO.Type;
import com.cloud.utils.db.GenericDao;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name="disk_offering_view")
public class DiskOfferingJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @Column(name="id", updatable=false, nullable = false)
    private long id;

    @Column(name="uuid")
    private String uuid;

    @Column(name="name")
    private String name;

    @Column(name="display_text")
    private String displayText;

    @Column(name="disk_size")
    long diskSize;

    @Column(name="tags", length=4096)
    String tags;

    @Column(name="use_local_storage")
    private boolean useLocalStorage;

    @Column(name="system_use")
    private boolean systemUse;

    @Column(name="customized")
    private boolean customized;

    @Column(name="sort_key")
    int sortKey;

    @Column(name="type")
    Type type;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name="domain_id")
    private long domainId;

    @Column(name="domain_uuid")
    private String domainUuid;

    @Column(name="domain_name")
    private String domainName = null;

    @Column(name="domain_path")
    private String domainPath = null;


    public DiskOfferingJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isUseLocalStorage() {
        return useLocalStorage;
    }

    public void setUseLocalStorage(boolean useLocalStorage) {
        this.useLocalStorage = useLocalStorage;
    }

    public boolean isSystemUse() {
        return systemUse;
    }

    public void setSystemUse(boolean systemUse) {
        this.systemUse = systemUse;
    }

    public boolean isCustomized() {
        return customized;
    }

    public void setCustomized(boolean customized) {
        this.customized = customized;
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

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }



}
