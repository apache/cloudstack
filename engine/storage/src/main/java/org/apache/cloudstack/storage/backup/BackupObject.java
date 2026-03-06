//
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
//

package org.apache.cloudstack.storage.backup;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.component.ComponentContext;
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.NativeBackupJoinVO;
import org.apache.cloudstack.backup.dao.NativeBackupJoinDao;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.to.BackupDeltaTO;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class BackupObject implements DataObject {

    private long id;
    private String uuid;
    private Long zoneId;
    private Long size;
    private long physicalSize;
    private DataStore dataStore;
    private String imageStorePath;
    private Backup.Status status;
    private Backup.CompressionStatus compressionStatus;

    @Inject
    NativeBackupJoinDao nativeBackupJoinDao;
    @Inject
    DataStoreManager storeManager;

    public BackupObject() {

    }

    public static BackupObject getBackupObject(NativeBackupJoinVO nativeBackupJoinVO) {
        BackupObject backupObject = ComponentContext.inject(BackupObject.class);
        backupObject.configure(nativeBackupJoinVO);
        return backupObject;
    }

    private void configure(NativeBackupJoinVO nativeBackupJoin) {
        this.id = nativeBackupJoin.getId();
        this.uuid = nativeBackupJoin.getUuid();
        this.zoneId = nativeBackupJoin.getZoneId();
        this.size = nativeBackupJoin.getProtectedSize();
        this.physicalSize = nativeBackupJoin.getSize();
        this.imageStorePath = nativeBackupJoin.getImageStorePath();
        this.status = nativeBackupJoin.getStatus();
        this.compressionStatus = nativeBackupJoin.getCompressionStatus();
        this.dataStore = storeManager.getDataStore(nativeBackupJoin.getImageStoreId(), DataStoreRole.Image);
    }

    public List<List<BackupObject>> getChildren() {
        List<List<BackupObject>> children = new ArrayList<>();

        List<NativeBackupJoinVO> backups = nativeBackupJoinDao.listByParentId(id);
        while (CollectionUtils.isNotEmpty(backups)) {
            children.add(backups.stream().map(BackupObject::getBackupObject).collect(Collectors.toList()));
            backups = nativeBackupJoinDao.listByParentId(backups.get(0).getId());
        }

        return children;
    }

    public List<List<BackupObject>> getParents(long parentId) {
        LinkedList<List<BackupObject>> parents = new LinkedList<>();

        List<NativeBackupJoinVO> backups = nativeBackupJoinDao.listById(parentId);
        while (CollectionUtils.isNotEmpty(backups)) {
            parents.addFirst(backups.stream().map(BackupObject::getBackupObject).collect(Collectors.toList()));
            backups = nativeBackupJoinDao.listById(backups.get(0).getParentId());
        }

        return parents;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUri() {
        return "";
    }

    @Override
    public DataTO getTO() {
        DataTO to = dataStore.getDriver().getTO(this);
        if (to == null) {
            return new BackupDeltaTO(id, dataStore.getTO(), Hypervisor.HypervisorType.KVM, imageStorePath);
        }
        return to;
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public long getPhysicalSize() {
        return physicalSize;
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.BACKUP;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
    }

    @Override
    public void incRefCount() {
    }

    @Override
    public void decRefCount() {
    }

    @Override
    public Long getRefCount() {
        return 0L;
    }

    @Override
    public String getName() {
        return "";
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public String toString() {
        return uuid;
    }

    public Backup.CompressionStatus getCompressionStatus() {
        return compressionStatus;
    }

    public Backup.Status getStatus() {
        return status;
    }
}
