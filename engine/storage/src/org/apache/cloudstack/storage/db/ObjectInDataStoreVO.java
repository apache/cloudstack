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
package org.apache.cloudstack.storage.db;

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
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.fsm.StateObject;

@Entity
@Table(name = "object_datastore_ref")
public class ObjectInDataStoreVO implements StateObject<ObjectInDataStoreStateMachine.State>, DataObjectInStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @Column(name = "datastore_uuid")
    private String dataStoreUuid;
    
    @Column(name = "datastore_role")
    @Enumerated(EnumType.STRING)
    private DataStoreRole dataStoreRole;

    @Column(name = "object_uuid")
    String objectUuid;
    
    @Column(name = "object_type")
    @Enumerated(EnumType.STRING)
    DataObjectType objectType;

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
    
    @Column (name="url")
    private String downloadUrl;
    
    @Column(name="format")
    private Storage.ImageFormat format;
    
    @Column(name="checksum")
    private String checksum;

    @Column(name = "error_str")
    String errorString;

    @Column(name = "job_id")
    String jobId;

    @Column(name = "install_path")
    String installPath;

    @Column(name = "size")
    Long size;
    
    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    ObjectInDataStoreStateMachine.State state;

    @Column(name="update_count", updatable = true, nullable=false)
    protected long updatedCount;
    
    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;
    
    public ObjectInDataStoreVO() {
        this.state = ObjectInDataStoreStateMachine.State.Allocated;
    }
    
    public long getId() {
        return this.id;
    }
    
    public String getDataStoreUuid() {
        return this.dataStoreUuid;
    }
    
    public void setDataStoreUuid(String uuid) {
        this.dataStoreUuid = uuid;
    }
    
    public DataStoreRole getDataStoreRole() {
        return this.dataStoreRole;
    }
    
    public void setDataStoreRole(DataStoreRole role) {
        this.dataStoreRole = role;
    }
    
    public String getObjectUuid() {
        return this.objectUuid;
    }
    
    public void setObjectUuid(String uuid) {
        this.objectUuid = uuid;
    }
    
    public DataObjectType getObjectType() {
        return this.objectType;
    }
    
    public void setObjectType(DataObjectType type) {
        this.objectType = type;
    }

    @Override
    public ObjectInDataStoreStateMachine.State getState() {
        return this.state;
    }
    
    public void setInstallPath(String path) {
        this.installPath = path;
    }
    
    public String getInstallPath() {
        return this.installPath;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public Long getSize() {
        return this.size;
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
    
    public void setUpdated(Date updated) {
        this.updated = updated;
    }
}
