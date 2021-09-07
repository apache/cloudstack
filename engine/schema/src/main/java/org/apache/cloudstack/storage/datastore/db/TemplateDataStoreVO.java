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
package org.apache.cloudstack.storage.datastore.db;

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

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.fsm.StateObject;

/**
 * Join table for image_data_store and templates
 *
 */
@Entity
@Table(name = "template_store_ref")
public class TemplateDataStoreVO implements StateObject<ObjectInDataStoreStateMachine.State>, DataObjectInStore {
    private static final Logger s_logger = Logger.getLogger(TemplateDataStoreVO.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "store_id")
    private Long dataStoreId; // this can be null for baremetal templates

    @Column(name = "template_id")
    private long templateId;

    @Column(name = "store_role")
    @Enumerated(EnumType.STRING)
    private DataStoreRole dataStoreRole;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    private Date created = null;

    @Column(name = "last_updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date lastUpdated = null;

    @Column(name = "download_pct")
    private int downloadPercent;

    @Column(name = "size")
    private Long size;

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

    @Column(name = "download_url", length = 2048)
    private String extractUrl;

    @Column(name = "download_url_created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date extractUrlCreated = null;

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

    @Column(name = "ref_cnt")
    Long refCnt = 0L;

    public TemplateDataStoreVO(Long hostId, long templateId) {
        super();
        dataStoreId = hostId;
        this.templateId = templateId;
        state = ObjectInDataStoreStateMachine.State.Allocated;
        refCnt = 0L;
    }

    public TemplateDataStoreVO(Long hostId, long templateId, Date lastUpdated, int downloadPercent, Status downloadState, String localDownloadPath, String errorString,
            String jobId, String installPath, String downloadUrl) {
        super();
        dataStoreId = hostId;
        this.templateId = templateId;
        this.lastUpdated = lastUpdated;
        this.downloadPercent = downloadPercent;
        this.downloadState = downloadState;
        this.localDownloadPath = localDownloadPath;
        this.errorString = errorString;
        this.jobId = jobId;
        refCnt = 0L;
        this.installPath = installPath;
        setDownloadUrl(downloadUrl);
        switch (downloadState) {
            case DOWNLOADED:
                state = ObjectInDataStoreStateMachine.State.Ready;
                break;
            case CREATING:
            case DOWNLOAD_IN_PROGRESS:
            case UPLOAD_IN_PROGRESS:
                state = ObjectInDataStoreStateMachine.State.Creating2;
                break;
            case DOWNLOAD_ERROR:
            case UPLOAD_ERROR:
                state = ObjectInDataStoreStateMachine.State.Failed;
                break;
            case ABANDONED:
                state = ObjectInDataStoreStateMachine.State.Destroyed;
                break;
            default:
                state = ObjectInDataStoreStateMachine.State.Allocated;
                break;
        }
    }

    public TemplateDataStoreVO() {
        refCnt = 0L;
    }

    @Override
    public String getInstallPath() {
        return installPath;
    }

    @Override
    public long getDataStoreId() {
        return dataStoreId;
    }

    public void setDataStoreId(long storeId) {
        dataStoreId = storeId;
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

    @Override
    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public Status getDownloadState() {
        return downloadState;
    }

    public void setLocalDownloadPath(String localPath) {
        localDownloadPath = localPath;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TemplateDataStoreVO) {
            TemplateDataStoreVO other = (TemplateDataStoreVO)obj;
            return (templateId == other.getTemplateId() && dataStoreId == other.getDataStoreId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long tid = new Long(templateId);
        Long hid = new Long(dataStoreId);
        return tid.hashCode() + hid.hashCode();
    }

    public void setSize(Long size) {
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

    public long getTemplateSize() {
        return -1;
    }

    @Override
    public String toString() {
        return new StringBuilder("TmplDataStore[").append(id).append("-").append(templateId).append("-").append(dataStoreId).append(installPath).append("]").toString();
    }

    @Override
    public ObjectInDataStoreStateMachine.State getState() {
        // TODO Auto-generated method stub
        return state;
    }

    public void setState(ObjectInDataStoreStateMachine.State state) {
        this.state = state;
    }

    public long getUpdatedCount() {
        return updatedCount;
    }

    public void incrUpdatedCount() {
        updatedCount++;
    }

    public void decrUpdatedCount() {
        updatedCount--;
    }

    public Date getUpdated() {
        return updated;
    }

    @Override
    public long getObjectId() {
        return getTemplateId();
    }

    @Override
    public State getObjectInStoreState() {
        return state;
    }

    public DataStoreRole getDataStoreRole() {
        return dataStoreRole;
    }

    public void setDataStoreRole(DataStoreRole dataStoreRole) {
        this.dataStoreRole = dataStoreRole;
    }

    public Long getRefCnt() {
        return refCnt;
    }

    public void setRefCnt(Long refCnt) {
        this.refCnt = refCnt;
    }

    public void incrRefCnt() {
        refCnt++;
    }

    public void decrRefCnt() {
        if (refCnt > 0) {
            refCnt--;
        }
        else{
            s_logger.warn("We should not try to decrement a zero reference count even though our code has guarded");
        }
    }

    public String getExtractUrl() {
        return extractUrl;
    }

    public void setExtractUrl(String extractUrl) {
        this.extractUrl = extractUrl;
    }

    public Date getExtractUrlCreated() {
        return extractUrlCreated;
    }

    public void setExtractUrlCreated(Date extractUrlCreated) {
        this.extractUrlCreated = extractUrlCreated;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
