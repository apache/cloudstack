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

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;

import com.cloud.utils.db.GenericDaoBase;

/**
 * Join table for storage hosts and templates
 *
 */
@Entity
@Table(name = "template_host_ref")
public class VMTemplateHostVO implements VMTemplateStorageResourceAssoc, DataObjectInStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "template_id")
    private long templateId;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    private Date created = null;

    @Column(name = "last_updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastUpdated = null;

    @Column(name = "download_pct")
    private int downloadPercent;

    @Column(name = "size")
    private long size;

    @Column(name = "physical_size")
    private long physicalSize;

    @Column(name = "download_state")
    @Enumerated(EnumType.STRING)
    private Status downloadState;

    @Column(name = "local_path")
    private String localDownloadPath;

    @Column(name = "error_str")
    private String errorString;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "install_path")
    private String installPath;

    @Column(name = "url", length = 2048)
    private String downloadUrl;

    @Column(name = "is_copy")
    private boolean isCopy = false;

    @Column(name = "destroyed")
    boolean destroyed = false;

    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updatedCount;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;

    @Override
    public String getInstallPath() {
        return installPath;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }

    @Override
    public long getTemplateId() {
        return templateId;
    }

    @Override
    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }

    @Override
    public int getDownloadPercent() {
        return downloadPercent;
    }

    @Override
    public void setDownloadPercent(int downloadPercent) {
        this.downloadPercent = downloadPercent;
    }

    @Override
    public void setDownloadState(Status downloadState) {
        this.downloadState = downloadState;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public void setLastUpdated(Date date) {
        lastUpdated = date;
    }

    @Override
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    @Override
    public Status getDownloadState() {
        return downloadState;
    }

    public VMTemplateHostVO(long hostId, long templateId) {
        super();
        this.hostId = hostId;
        this.templateId = templateId;
        this.state = ObjectInDataStoreStateMachine.State.Allocated;
    }

    public VMTemplateHostVO(long hostId, long templateId, Date lastUpdated, int downloadPercent, Status downloadState, String localDownloadPath, String errorString,
            String jobId, String installPath, String downloadUrl) {
        super();
        this.hostId = hostId;
        this.templateId = templateId;
        this.lastUpdated = lastUpdated;
        this.downloadPercent = downloadPercent;
        this.downloadState = downloadState;
        this.localDownloadPath = localDownloadPath;
        this.errorString = errorString;
        this.jobId = jobId;
        this.installPath = installPath;
        this.setDownloadUrl(downloadUrl);
    }

    protected VMTemplateHostVO() {

    }

    @Override
    public void setLocalDownloadPath(String localPath) {
        this.localDownloadPath = localPath;
    }

    @Override
    public String getLocalDownloadPath() {
        return localDownloadPath;
    }

    @Override
    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }

    @Override
    public String getErrorString() {
        return errorString;
    }

    @Override
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @Override
    public String getJobId() {
        return jobId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VMTemplateHostVO) {
            VMTemplateHostVO other = (VMTemplateHostVO)obj;
            return (this.templateId == other.getTemplateId() && this.hostId == other.getHostId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long tid = new Long(templateId);
        Long hid = new Long(hostId);
        return tid.hashCode() + hid.hashCode();
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return size;
    }

    public void setPhysicalSize(long physicalSize) {
        this.physicalSize = physicalSize;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean getDestroyed() {
        return destroyed;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setCopy(boolean isCopy) {
        this.isCopy = isCopy;
    }

    public boolean isCopy() {
        return isCopy;
    }

    @Override
    public long getTemplateSize() {
        return -1;
    }

    @Override
    public String toString() {
        return new StringBuilder("TmplHost[").append(id).append("-").append(templateId).append("-").append(hostId).append(installPath).append("]").toString();
    }

    @Override
    public ObjectInDataStoreStateMachine.State getState() {
        // TODO Auto-generated method stub
        return this.state;
    }

    public long getUpdatedCount() {
        return this.updatedCount;
    }

    public void incrUpdatedCount() {
        this.updatedCount++;
    }

    public void decrUpdatedCount() {
        this.updatedCount--;
    }

    public Date getUpdated() {
        return updated;
    }

    @Override
    public long getObjectId() {
        return this.getTemplateId();
    }

    @Override
    public long getDataStoreId() {
        return this.getHostId();
    }

    @Override
    public State getObjectInStoreState() {
        return this.state;
    }

}
