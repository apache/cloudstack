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
package com.cloud.bridge.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 * Holds the relation
 *  Id,
 *  Name,
 *  OwnerCanonicalId,
 *  SHost,
 *  CreateTime,
 *  VersioningStatus
 * For ORM see "com/cloud/bridge/model/SHost.hbm.xml"
 */

@Entity
@Table(name = "sbucket")
public class SBucketVO implements SBucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "Name")
    private String name;

    @Column(name = "OwnerCanonicalID")
    private String ownerCanonicalId;

    @Column(name = "SHostID")
    private long shostID;

    @Column(name = "CreateTime")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date createTime;

    @Column(name = "VersioningStatus")
    private int versioningStatus;

    @Transient
    private SHostVO shost;

    @Transient
    private Set<SObjectVO> objectsInBucket = new HashSet<SObjectVO>();

    public SBucketVO() {
        versioningStatus = VERSIONING_NULL;
        this.createTime = new Date();
    }

    public SBucketVO(String bucketName, Date currentGMTTime, String canonicalUserId, SHostVO first) {
        this.versioningStatus = VERSIONING_NULL;
        this.name = bucketName;
        this.createTime = new Date();
        this.ownerCanonicalId = canonicalUserId;
        this.shost = first;
        this.shostID = shost.getId();
    }

    public Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerCanonicalId() {
        return ownerCanonicalId;
    }

    public void setOwnerCanonicalId(String ownerCanonicalId) {
        this.ownerCanonicalId = ownerCanonicalId;
    }

    public long getShostID() {
        return shostID;
    }

    public void setShostID(long shostID) {
        this.shostID = shostID;
    }

    public SHostVO getShost() {
        return shost;
    }

    public void setShost(SHostVO shost) {
        this.shost = shost;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public int getVersioningStatus() {
        return versioningStatus;
    }

    public void setVersioningStatus(int versioningStatus) {
        this.versioningStatus = versioningStatus;
    }

    public Set<SObjectVO> getObjectsInBucket() {
        return objectsInBucket;
    }

    public void setObjectsInBucket(Set<SObjectVO> objectsInBucket) {
        this.objectsInBucket = objectsInBucket;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof SBucketVO))
            return false;

        return getName().equals(((SBucketVO)other).getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
