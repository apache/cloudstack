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
 * Join table for storage pools and templates
 *
 */
@Entity
@Table(name = "template_spool_ref")
public class VMTemplateStoragePoolVO implements VMTemplateStorageResourceAssoc, DataObjectInStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "pool_id")
    private long poolId;

    @Column(name = "template_id")
    long templateId;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    Date created = null;

    @Column(name = "last_updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date lastUpdated = null;

    @Column(name = "download_pct")
    int downloadPercent;

    @Column(name = "download_state")
    @Enumerated(EnumType.STRING)
    Status downloadState;

    @Column(name = "local_path")
    String localDownloadPath;

    @Column(name = "error_str")
    String errorString;

    @Column(name = "job_id")
    String jobId;

    @Column(name = "install_path")
    String installPath;

    @Column(name = "template_size")
    long templateSize;

    @Column(name = "marked_for_gc")
    boolean markedForGC;

    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updatedCount;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;

    @Column(name = "deployment_option")
    private String deploymentOption;

    @Override
    public String getInstallPath() {
        return installPath;
    }

    @Override
    public long getTemplateSize() {
        return templateSize;
    }

    public long getPoolId() {
        return poolId;
    }

    public void setpoolId(long poolId) {
        this.poolId = poolId;
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

    public VMTemplateStoragePoolVO(long poolId, long templateId, String configuration) {
        super();
        this.poolId = poolId;
        this.templateId = templateId;
        this.downloadState = Status.NOT_DOWNLOADED;
        this.state = ObjectInDataStoreStateMachine.State.Allocated;
        this.markedForGC = false;
        this.deploymentOption = configuration;
    }

    public VMTemplateStoragePoolVO(long poolId, long templateId, Date lastUpdated, int downloadPercent, Status downloadState, String localDownloadPath,
            String errorString, String jobId, String installPath, long templateSize, String configuration) {
        super();
        this.poolId = poolId;
        this.templateId = templateId;
        this.lastUpdated = lastUpdated;
        this.downloadPercent = downloadPercent;
        this.downloadState = downloadState;
        this.localDownloadPath = localDownloadPath;
        this.errorString = errorString;
        this.jobId = jobId;
        this.installPath = installPath;
        this.templateSize = templateSize;
        this.deploymentOption = configuration;
    }

    protected VMTemplateStoragePoolVO() {

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

    public void setTemplateSize(long templateSize) {
        this.templateSize = templateSize;
    }

    public boolean getMarkedForGC() {
        return markedForGC;
    }

    public void setMarkedForGC(boolean markedForGC) {
        this.markedForGC = markedForGC;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof VMTemplateStoragePoolVO) {
            VMTemplateStoragePoolVO other = (VMTemplateStoragePoolVO)obj;
            return (this.templateId == other.getTemplateId() && this.poolId == other.getPoolId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long tid = new Long(templateId);
        Long hid = new Long(poolId);
        return tid.hashCode() + hid.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("TmplPool[").append(id).append("-").append(templateId).append("-").append(poolId).append("-").append(installPath).append("]").toString();
    }

    @Override
    public State getState() {
        return this.state;
    }

    //TODO: this should be revisited post-4.2 to completely use state transition machine
    public void setState(ObjectInDataStoreStateMachine.State state) {
        this.state = state;
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
        return this.getPoolId();
    }

    @Override
    public State getObjectInStoreState() {
        return this.state;
    }

    public String getDeploymentOption() {
        return deploymentOption;
    }

    public void setDeploymentOption(String deploymentOption) {
        this.deploymentOption = deploymentOption;
    }
}
