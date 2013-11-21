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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

@Entity
@Table(name = "sobject_item")
public class SObjectItemVO {
    private static final long serialVersionUID = -7351173256185687851L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "SObjectID")
    private long sObjectID;

    @Column(name = "Version")
    private String version;

    @Column(name = "MD5")
    private String md5;

    @Column(name = "StoredPath")
    private String storedPath;

    @Column(name = "StoredSize")
    private long storedSize;

    @Column(name = "CreateTime")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date createTime;

    @Column(name = "LastModifiedTime")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastModifiedTime;

    @Column(name = "LastAccessTime")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastAccessTime;

    @Transient
    private SObjectVO theObject;

    public SObjectItemVO() {
    }

    public Long getId() {
        return id;
    }

    private void setId(Long id) {
        this.id = id;
    }

    public SObjectVO getTheObject() {
        return theObject;
    }

    public void setTheObject(SObjectVO theObject) {
        this.theObject = theObject;
    }

    public long getsObjectID() {
        return sObjectID;
    }

    public void setsObjectID(long sObjectID) {
        this.sObjectID = sObjectID;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getStoredPath() {
        return storedPath;
    }

    public void setStoredPath(String storedPath) {
        this.storedPath = storedPath;   // TODO - storedpath holds integer, called from S3Engine.allocObjectItem
    }

    public long getStoredSize() {
        return storedSize;
    }

    public void setStoredSize(long storedSize) {
        this.storedSize = storedSize;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    public void setLastModifiedTime(Date lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    public Date getLastAccessTime() {
        return lastAccessTime;
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof SObjectItemVO))
            return false;

        if (version != null) {
            if (!version.equals(((SObjectItemVO)other).getVersion()))
                return false;
        } else {
            if (((SObjectItemVO)other).getVersion() != null)
                return false;
        }

        if (theObject.getId() != null) {
            if (!theObject.getId().equals(((SObjectItemVO)other).getTheObject()))
                return false;
        } else {
            if (((SObjectItemVO)other).getTheObject() != null)
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        if (version != null)
            hashCode = hashCode * 17 + version.hashCode();

        if (theObject != null)
            hashCode = hashCode * 17 + theObject.hashCode();

        return hashCode;
    }
}
