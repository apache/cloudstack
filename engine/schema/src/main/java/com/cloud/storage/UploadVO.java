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

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GenericDaoBase;

@Entity
@Table(name = "upload")
public class UploadVO implements Upload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "host_id")
    private long dataStoreId;

    @Column(name = "type_id")
    private long typeId;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    private Date created = null;

    @Column(name = "last_updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastUpdated = null;

    @Column(name = "upload_pct")
    private int uploadPercent;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name = "mode")
    @Enumerated(EnumType.STRING)
    private Mode mode = Mode.FTP_UPLOAD;

    @Column(name = "upload_state")
    @Enumerated(EnumType.STRING)
    private Status uploadState;

    @Column(name = "error_str")
    private String errorString;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "url", length = 2048)
    private String uploadUrl;

    @Column(name = "install_path")
    private String installPath;

    @Override
    public long getDataStoreId() {
        return dataStoreId;
    }

    public void setDataStoreId(long hostId) {
        this.dataStoreId = hostId;
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
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date date) {
        lastUpdated = date;
    }

    public UploadVO(long hostId, long templateId) {
        super();
        this.dataStoreId = hostId;
        this.typeId = templateId;
        this.uuid = UUID.randomUUID().toString();
    }

    public UploadVO(long hostId, long typeId, Date lastUpdated, Status uploadState, Type type, String uploadUrl, Mode mode) {
        super();
        this.dataStoreId = hostId;
        this.typeId = typeId;
        this.lastUpdated = lastUpdated;
        this.uploadState = uploadState;
        this.mode = mode;
        this.type = type;
        this.uploadUrl = uploadUrl;
        this.uuid = UUID.randomUUID().toString();
    }

    public UploadVO(long hostId, long typeId, Date lastUpdated, Status uploadState, int uploadPercent, Type type, Mode mode) {
        super();
        this.dataStoreId = hostId;
        this.typeId = typeId;
        this.lastUpdated = lastUpdated;
        this.uploadState = uploadState;
        this.uploadPercent = uploadPercent;
        this.type = type;
        this.mode = mode;
        this.uuid = UUID.randomUUID().toString();

    }

    protected UploadVO() {
    }

    public UploadVO(Long uploadId) {
        this.id = uploadId;
    }

    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }

    @Override
    public String getErrorString() {
        return errorString;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String getJobId() {
        return jobId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UploadVO) {
            UploadVO other = (UploadVO)obj;
            return (this.typeId == other.getTypeId() && this.dataStoreId == other.getDataStoreId() && this.type == other.getType());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return NumbersUtil.hash(id);
    }

    @Override
    public int getUploadPercent() {
        return uploadPercent;
    }

    public void setUploadPercent(int uploadPercent) {
        this.uploadPercent = uploadPercent;
    }

    @Override
    public Status getUploadState() {
        return uploadState;
    }

    public void setUploadState(Status uploadState) {
        this.uploadState = uploadState;
    }

    @Override
    public long getTypeId() {
        return typeId;
    }

    public void setTypeId(long typeId) {
        this.typeId = typeId;
    }

    @Override
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public String getInstallPath() {
        return installPath;
    }

    @Override
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }
}
