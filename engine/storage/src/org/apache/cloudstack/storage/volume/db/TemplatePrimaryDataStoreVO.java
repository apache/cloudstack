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
package org.apache.cloudstack.storage.volume.db;

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

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.fsm.StateObject;

@Entity
@Table(name = "template_spool_ref")
public class TemplatePrimaryDataStoreVO implements StateObject<ObjectInDataStoreStateMachine.State> {
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

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;

    @Column(name = "update_count", updatable = true, nullable = false)
    protected long updatedCount;

    public long getUpdatedCount() {
        return this.updatedCount;
    }

    public void incrUpdatedCount() {
        this.updatedCount++;
    }

    public void decrUpdatedCount() {
        this.updatedCount--;
    }

    public String getInstallPath() {
        return installPath;
    }

    public long getTemplateSize() {
        return templateSize;
    }

    public long getPoolId() {
        return poolId;
    }

    public void setpoolId(long poolId) {
        this.poolId = poolId;
    }

    public long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(long templateId) {
        this.templateId = templateId;
    }

    public int getDownloadPercent() {
        return downloadPercent;
    }

    public void setDownloadPercent(int downloadPercent) {
        this.downloadPercent = downloadPercent;
    }

    public void setDownloadState(Status downloadState) {
        this.downloadState = downloadState;
    }

    public long getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date date) {
        lastUpdated = date;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public Status getDownloadState() {
        return downloadState;
    }

    public TemplatePrimaryDataStoreVO(long poolId, long templateId) {
        super();
        this.poolId = poolId;
        this.templateId = templateId;
        this.downloadState = Status.NOT_DOWNLOADED;
        this.state = ObjectInDataStoreStateMachine.State.Allocated;
        this.markedForGC = false;
    }

    public TemplatePrimaryDataStoreVO(long poolId, long templateId, Date lastUpdated, int downloadPercent, Status downloadState, String localDownloadPath,
            String errorString, String jobId, String installPath, long templateSize) {
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
    }

    protected TemplatePrimaryDataStoreVO() {

    }

    public void setLocalDownloadPath(String localPath) {
        this.localDownloadPath = localPath;
    }

    public String getLocalDownloadPath() {
        return localDownloadPath;
    }

    public void setErrorString(String errorString) {
        this.errorString = errorString;
    }

    public String getErrorString() {
        return errorString;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

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
        if (obj instanceof TemplatePrimaryDataStoreVO) {
            TemplatePrimaryDataStoreVO other = (TemplatePrimaryDataStoreVO)obj;
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
        return new StringBuilder("TmplPool[").append(id)
            .append("-")
            .append(templateId)
            .append("-")
            .append("poolId")
            .append("-")
            .append(installPath)
            .append("]")
            .toString();
    }

    @Override
    public ObjectInDataStoreStateMachine.State getState() {
        return this.state;
    }

}