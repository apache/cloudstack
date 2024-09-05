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
package com.cloud.storage;

import com.cloud.utils.db.GenericDao;
import com.google.gson.annotations.Expose;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "bucket")
public class BucketVO implements Bucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "account_id")
    long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "object_store_id")
    long objectStoreId;

    @Expose
    @Column(name = "name")
    String name;

    @Expose
    @Column(name = "state", updatable = true, nullable = false)
    @Enumerated(value = EnumType.STRING)
    private State state;

    @Column(name = "size")
    Long size;

    @Column(name = "quota")
    Integer quota;

    @Column(name = "versioning")
    boolean versioning;

    @Column(name = "encryption")
    boolean encryption;

    @Column(name = "object_lock")
    boolean objectLock;

    @Column(name = "policy")
    String policy;

    @Column(name = "bucket_url")
    String bucketURL;

    @Column(name = "access_key")
    String accessKey;

    @Column(name = "secret_key")
    String secretKey;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = "uuid")
    String uuid;

    public BucketVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public BucketVO(String name) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.state = State.Allocated;
    }

    public BucketVO(long accountId, long domainId, long objectStoreId, String name, Integer quota, boolean versioning,
                    boolean encryption, boolean objectLock, String policy) {
        this.accountId = accountId;
        this.domainId = domainId;
        this.objectStoreId = objectStoreId;
        this.name = name;
        this.state = State.Allocated;
        this.uuid = UUID.randomUUID().toString();
        this.quota = quota;
        this.versioning = versioning;
        this.encryption = encryption;
        this.objectLock = objectLock;
        this.policy = policy;
        this.size = 0L;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public long getObjectStoreId() {
        return objectStoreId;
    }

    @Override
    public String getName() {
        return name;
    }

    public Long getSize() {
        return size;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getQuota() {
        return quota;
    }

    public void setQuota(Integer quota) {
        this.quota = quota;
    }

    public boolean isVersioning() {
        return versioning;
    }

    public void setVersioning(boolean versioning) {
        this.versioning = versioning;
    }

    public boolean isEncryption() {
        return encryption;
    }

    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

    public boolean isObjectLock() {
        return objectLock;
    }

    public void setObjectLock(boolean objectLock) {
        this.objectLock = objectLock;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public String getBucketURL() {
        return bucketURL;
    }
    public void setBucketURL(String bucketURL) {
        this.bucketURL = bucketURL;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public Class<?> getEntityType() {
        return Bucket.class;
    }

    @Override
    public String toString() {
        return String.format("Bucket %s", new ToStringBuilder(this, ToStringStyle.JSON_STYLE).append("uuid", getUuid()).append("name", getName())
                .append("ObjectStoreId", getObjectStoreId()).toString());
    }
}
