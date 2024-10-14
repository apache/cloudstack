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

package com.cloud.api.query.vo;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.Volume;
import com.cloud.user.Account;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.util.HypervisorTypeConverter;

@Entity
@Table(name = "snapshot_view")
public class SnapshotJoinVO extends BaseViewWithTagInformationVO implements ControlledViewEntity {
    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(value = EnumType.STRING)
    private Snapshot.State status;

    @Column(name = "disk_offering_id")
    Long diskOfferingId;

    @Column(name = "snapshot_type")
    short snapshotType;

    @Column(name = "type_description")
    String typeDescription;

    @Column(name = "size")
    long size;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name = "location_type")
    @Enumerated(value = EnumType.STRING)
    private Snapshot.LocationType locationType;

    @Column(name = "hypervisor_type")
    @Convert(converter = HypervisorTypeConverter.class)
    Hypervisor.HypervisorType hypervisorType;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "account_uuid")
    private String accountUuid;

    @Column(name = "account_name")
    private String accountName = null;

    @Column(name = "account_type")
    @Enumerated(value = EnumType.ORDINAL)
    private Account.Type accountType;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_name")
    private String domainName = null;

    @Column(name = "domain_path")
    private String domainPath = null;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "project_uuid")
    private String projectUuid;

    @Column(name = "project_name")
    private String projectName;

    @Column(name = "data_center_id")
    private Long dataCenterId;

    @Column(name = "data_center_uuid")
    private String dataCenterUuid;

    @Column(name = "data_center_name")
    private String dataCenterName;

    @Column(name = "volume_id")
    private Long volumeId;

    @Column(name = "volume_uuid")
    private String volumeUuid;

    @Column(name = "volume_name")
    private String volumeName;

    @Column(name = "volume_type")
    @Enumerated(EnumType.STRING)
    Volume.Type volumeType = Volume.Type.UNKNOWN;

    @Column(name = "volume_state")
    @Enumerated(EnumType.STRING)
    Volume.State volumeState;

    @Column(name = "volume_size")
    Long volumeSize;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "store_uuid")
    private String storeUuid;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "store_role")
    @Enumerated(EnumType.STRING)
    private DataStoreRole storeRole;

    @Column(name = "store_state")
    @Enumerated(EnumType.STRING)
    private ObjectInDataStoreStateMachine.State storeState;

    @Column(name = "download_state")
    @Enumerated(EnumType.STRING)
    private VMTemplateStorageResourceAssoc.Status downloadState;

    @Column(name = "download_pct")
    private int downloadPercent;

    @Column(name = "error_str")
    private String errorString;

    @Column(name = "store_size")
    private long storeSize;

    @Column(name = "created_on_store")
    private Date createdOnStore = null;

    @Column(name = "snapshot_store_pair")
    private String snapshotStorePair;

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    public Snapshot.State getStatus() {
        return status;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public short getSnapshotType() {
        return snapshotType;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public long getSize() {
        return size;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }

    public Snapshot.LocationType getLocationType() {
        return locationType;
    }

    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public String getAccountName() {
        return accountName;
    }

    @Override
    public Account.Type getAccountType() {
        return accountType;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getDomainUuid() {
        return domainUuid;
    }

    @Override
    public String getDomainName() {
        return domainName;
    }

    @Override
    public String getDomainPath() {
        return domainPath;
    }

    public long getProjectId() {
        return projectId;
    }

    @Override
    public String getProjectUuid() {
        return projectUuid;
    }

    @Override
    public String getProjectName() {
        return projectName;
    }

    public Long getDataCenterId() {
        return dataCenterId;
    }

    public String getDataCenterUuid() {
        return dataCenterUuid;
    }

    public String getDataCenterName() {
        return dataCenterName;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public Volume.Type getVolumeType() {
        return volumeType;
    }

    public Volume.State getVolumeState() {
        return volumeState;
    }

    public Long getVolumeSize() {
        return volumeSize;
    }

    public Long getStoreId() {
        return storeId;
    }

    public String getStoreUuid() {
        return storeUuid;
    }

    public String getStoreName() {
        return storeName;
    }

    public DataStoreRole getStoreRole() {
        return storeRole;
    }

    public ObjectInDataStoreStateMachine.State getStoreState() {
        return storeState;
    }

    public VMTemplateStorageResourceAssoc.Status getDownloadState() {
        return downloadState;
    }

    public int getDownloadPercent() {
        return downloadPercent;
    }

    public String getErrorString() {
        return errorString;
    }

    public long getStoreSize() {
        return storeSize;
    }

    public Date getCreatedOnStore() {
        return createdOnStore;
    }

    public String getSnapshotStorePair() {
        return snapshotStorePair;
    }

    @Override
    public Class<?> getEntityType() {
        return Snapshot.class;
    }
}
