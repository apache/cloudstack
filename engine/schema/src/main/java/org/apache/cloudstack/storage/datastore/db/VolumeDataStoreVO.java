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

import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.fsm.StateObject;

/**
 * Join table for image_data_store and volumes
 *
 */
@Entity
@Table(name = "volume_store_ref")
public class VolumeDataStoreVO implements StateObject<ObjectInDataStoreStateMachine.State>, DataObjectInStore {
    private static final Logger s_logger = Logger.getLogger(VolumeDataStoreVO.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "store_id")
    private long dataStoreId;

    @Column(name = "volume_id")
    private long volumeId;

    @Column(name = "zone_id")
    private long zoneId;

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

    @Column(name = "checksum")
    private String checksum;

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

    public long getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(long volumeId) {
        this.volumeId = volumeId;
    }

    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
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

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public VolumeDataStoreVO(long hostId, long volumeId) {
        super();
        dataStoreId = hostId;
        this.volumeId = volumeId;
        state = ObjectInDataStoreStateMachine.State.Allocated;
        refCnt = 0L;
    }

    public VolumeDataStoreVO(long hostId, long volumeId, Date lastUpdated, int downloadPercent, Status downloadState, String localDownloadPath, String errorString,
            String jobId, String installPath, String downloadUrl, String checksum) {
        // super();
        dataStoreId = hostId;
        this.volumeId = volumeId;
        // this.zoneId = zoneId;
        this.lastUpdated = lastUpdated;
        this.downloadPercent = downloadPercent;
        this.downloadState = downloadState;
        this.localDownloadPath = localDownloadPath;
        this.errorString = errorString;
        this.jobId = jobId;
        this.installPath = installPath;
        setDownloadUrl(downloadUrl);
        this.checksum = checksum;
        refCnt = 0L;
    }

    public VolumeDataStoreVO() {
        refCnt = 0L;
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
        if (obj instanceof VolumeDataStoreVO) {
            VolumeDataStoreVO other = (VolumeDataStoreVO)obj;
            return (volumeId == other.getVolumeId() && dataStoreId == other.getDataStoreId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        Long tid = new Long(volumeId);
        Long hid = new Long(dataStoreId);
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

    public long getVolumeSize() {
        return -1;
    }

    @Override
    public String toString() {
        return new StringBuilder("VolumeDataStore[").append(id).append("-").append(volumeId).append("-").append(dataStoreId).append(installPath).append("]").toString();
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
    public ObjectInDataStoreStateMachine.State getState() {
        // TODO Auto-generated method stub
        return state;
    }

    public void setState(ObjectInDataStoreStateMachine.State state) {
        this.state = state;
    }

    @Override
    public long getObjectId() {
        return getVolumeId();
    }

    @Override
    public State getObjectInStoreState() {
        return state;
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
        else {
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
