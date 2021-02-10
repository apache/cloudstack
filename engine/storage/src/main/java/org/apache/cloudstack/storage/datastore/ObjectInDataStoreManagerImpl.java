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
package org.apache.cloudstack.storage.datastore;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.db.ObjectInDataStoreDao;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.S3TO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.template.TemplateConstants;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class ObjectInDataStoreManagerImpl implements ObjectInDataStoreManager {
    private static final Logger s_logger = Logger.getLogger(ObjectInDataStoreManagerImpl.class);
    @Inject
    TemplateDataFactory imageFactory;
    @Inject
    DataStoreManager storeMgr;
    @Inject
    VolumeDataFactory volumeFactory;
    @Inject
    TemplateDataStoreDao templateDataStoreDao;
    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;
    @Inject
    VolumeDataStoreDao volumeDataStoreDao;
    @Inject
    VMTemplatePoolDao templatePoolDao;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    ObjectInDataStoreDao objInStoreDao;
    @Inject
    VMTemplateDao templateDao;
    @Inject
    SnapshotDao snapshotDao;
    @Inject
    VolumeDao volumeDao;
    protected StateMachine2<State, Event, DataObjectInStore> stateMachines;

    public ObjectInDataStoreManagerImpl() {
        stateMachines = new StateMachine2<State, Event, DataObjectInStore>();
        stateMachines.addTransition(State.Allocated, Event.CreateOnlyRequested, State.Creating);
        stateMachines.addTransition(State.Allocated, Event.DestroyRequested, State.Destroying);
        stateMachines.addTransition(State.Allocated, Event.OperationFailed, State.Failed);
        stateMachines.addTransition(State.Allocated, Event.OperationSuccessed, State.Ready);
        stateMachines.addTransition(State.Creating, Event.OperationFailed, State.Allocated);
        stateMachines.addTransition(State.Creating, Event.OperationSuccessed, State.Ready);
        stateMachines.addTransition(State.Ready, Event.CopyingRequested, State.Copying);
        stateMachines.addTransition(State.Copying, Event.OperationSuccessed, State.Ready);
        stateMachines.addTransition(State.Copying, Event.OperationFailed, State.Ready);
        stateMachines.addTransition(State.Ready, Event.DestroyRequested, State.Destroying);
        stateMachines.addTransition(State.Destroying, Event.DestroyRequested, State.Destroying);
        stateMachines.addTransition(State.Destroying, Event.OperationSuccessed, State.Destroyed);
        stateMachines.addTransition(State.Destroying, Event.OperationFailed, State.Destroying);
        stateMachines.addTransition(State.Failed, Event.DestroyRequested, State.Destroying);
        // TODO: further investigate why an extra event is sent when it is
        // alreay Ready for DownloadListener
        stateMachines.addTransition(State.Ready, Event.OperationSuccessed, State.Ready);
        // State transitions for data object migration
        stateMachines.addTransition(State.Ready, Event.MigrateDataRequested, State.Migrating);
        stateMachines.addTransition(State.Ready, Event.CopyRequested, State.Copying);
        stateMachines.addTransition(State.Allocated, Event.MigrateDataRequested, State.Migrating);
        stateMachines.addTransition(State.Migrating, Event.MigrationFailed, State.Failed);
        stateMachines.addTransition(State.Migrating, Event.MigrationSucceeded, State.Destroyed);
        stateMachines.addTransition(State.Migrating, Event.OperationSuccessed, State.Ready);
        stateMachines.addTransition(State.Migrating, Event.OperationFailed, State.Ready);
    }

    @Override
    public DataObject create(DataObject obj, DataStore dataStore) {
        if (dataStore.getRole() == DataStoreRole.Primary) {
            if (obj.getType() == DataObjectType.TEMPLATE) {
                VMTemplateStoragePoolVO vo = new VMTemplateStoragePoolVO(dataStore.getId(), obj.getId(), null);
                vo = templatePoolDao.persist(vo);
            } else if (obj.getType() == DataObjectType.SNAPSHOT) {
                SnapshotInfo snapshotInfo = (SnapshotInfo)obj;
                SnapshotDataStoreVO ss = new SnapshotDataStoreVO();
                ss.setSnapshotId(obj.getId());
                ss.setDataStoreId(dataStore.getId());
                ss.setRole(dataStore.getRole());
                ss.setVolumeId(snapshotInfo.getVolumeId());
                ss.setSize(snapshotInfo.getSize()); // this is the virtual size of snapshot in primary storage.
                ss.setPhysicalSize(snapshotInfo.getSize()); // this physical size will get updated with actual size once the snapshot backup is done.
                SnapshotDataStoreVO snapshotDataStoreVO = snapshotDataStoreDao.findParent(dataStore.getRole(), dataStore.getId(), snapshotInfo.getVolumeId());
                if (snapshotDataStoreVO != null) {
                    //Double check the snapshot is removed or not
                    SnapshotVO parentSnap = snapshotDao.findById(snapshotDataStoreVO.getSnapshotId());
                    if (parentSnap != null) {
                        ss.setParentSnapshotId(snapshotDataStoreVO.getSnapshotId());
                    } else {
                        s_logger.debug("find inconsistent db for snapshot " + snapshotDataStoreVO.getSnapshotId());
                    }
                }
                ss.setState(ObjectInDataStoreStateMachine.State.Allocated);
                ss = snapshotDataStoreDao.persist(ss);
            }
        } else {
            // Image store
            switch (obj.getType()) {
                case TEMPLATE:
                    TemplateDataStoreVO ts = new TemplateDataStoreVO();
                    ts.setTemplateId(obj.getId());
                    ts.setDataStoreId(dataStore.getId());
                    ts.setDataStoreRole(dataStore.getRole());
                    String installPath =
                        TemplateConstants.DEFAULT_TMPLT_ROOT_DIR + "/" + TemplateConstants.DEFAULT_TMPLT_FIRST_LEVEL_DIR +
                            templateDao.findById(obj.getId()).getAccountId() + "/" + obj.getId();
                    if (dataStore.getTO() instanceof S3TO) {
                        TemplateInfo tmpl = (TemplateInfo)obj;
                        installPath += "/" + tmpl.getUniqueName(); // for S3, we
                        // append
                        // template name
                        // in the path
                        // for template
                        // sync since we
                        // don't have
                        // template.properties
                        // there
                    }

                    ts.setInstallPath(installPath);
                    ts.setState(ObjectInDataStoreStateMachine.State.Allocated);
                    ts = templateDataStoreDao.persist(ts);
                    break;
                case SNAPSHOT:
                    SnapshotInfo snapshot = (SnapshotInfo)obj;
                    SnapshotDataStoreVO ss = new SnapshotDataStoreVO();
                    ss.setSnapshotId(obj.getId());
                    ss.setDataStoreId(dataStore.getId());
                    ss.setRole(dataStore.getRole());
                    ss.setSize(snapshot.getSize());
                    ss.setVolumeId(snapshot.getVolumeId());
                    SnapshotDataStoreVO snapshotDataStoreVO = snapshotDataStoreDao.findParent(dataStore.getRole(), dataStore.getId(), snapshot.getVolumeId());
                    if (snapshotDataStoreVO != null) {
                        ss.setParentSnapshotId(snapshotDataStoreVO.getSnapshotId());
                    }
                    ss.setInstallPath(TemplateConstants.DEFAULT_SNAPSHOT_ROOT_DIR + "/" + snapshotDao.findById(obj.getId()).getAccountId() + "/" + snapshot.getVolumeId());
                    ss.setState(ObjectInDataStoreStateMachine.State.Allocated);
                    ss = snapshotDataStoreDao.persist(ss);
                    break;
                case VOLUME:
                    VolumeDataStoreVO vs = new VolumeDataStoreVO();
                    vs.setVolumeId(obj.getId());
                    vs.setDataStoreId(dataStore.getId());
                    vs.setInstallPath(TemplateConstants.DEFAULT_VOLUME_ROOT_DIR + "/" + volumeDao.findById(obj.getId()).getAccountId() + "/" + obj.getId());
                    vs.setState(ObjectInDataStoreStateMachine.State.Allocated);
                    vs = volumeDataStoreDao.persist(vs);
                    break;
            }
        }

        return this.get(obj, dataStore, null);
    }

    @Override
    public boolean delete(DataObject dataObj) {
        long objId = dataObj.getId();
        DataStore dataStore = dataObj.getDataStore();
        if (dataStore.getRole() == DataStoreRole.Primary) {
            if (dataObj.getType() == DataObjectType.TEMPLATE) {
                VMTemplateStoragePoolVO destTmpltPool = templatePoolDao.findByPoolTemplate(dataStore.getId(), objId, null);
                if (destTmpltPool != null) {
                    return templatePoolDao.remove(destTmpltPool.getId());
                } else {
                    s_logger.warn("Template " + objId + " is not found on storage pool " + dataStore.getId() + ", so no need to delete");
                    return true;
                }
            }
        } else {
            // Image store
            switch (dataObj.getType()) {
                case TEMPLATE:
                    TemplateDataStoreVO destTmpltStore = templateDataStoreDao.findByStoreTemplate(dataStore.getId(), objId);
                    if (destTmpltStore != null) {
                        return templateDataStoreDao.remove(destTmpltStore.getId());
                    } else {
                        s_logger.warn("Template " + objId + " is not found on image store " + dataStore.getId() + ", so no need to delete");
                        return true;
                    }
                case SNAPSHOT:
                    SnapshotDataStoreVO destSnapshotStore = snapshotDataStoreDao.findByStoreSnapshot(dataStore.getRole(), dataStore.getId(), objId);
                    if (destSnapshotStore != null) {
                        return snapshotDataStoreDao.remove(destSnapshotStore.getId());
                    } else {
                        s_logger.warn("Snapshot " + objId + " is not found on image store " + dataStore.getId() + ", so no need to delete");
                        return true;
                    }
                case VOLUME:
                    VolumeDataStoreVO destVolumeStore = volumeDataStoreDao.findByStoreVolume(dataStore.getId(), objId);
                    if (destVolumeStore != null) {
                        return volumeDataStoreDao.remove(destVolumeStore.getId());
                    } else {
                        s_logger.warn("Volume " + objId + " is not found on image store " + dataStore.getId() + ", so no need to delete");
                        return true;
                    }
            }
        }

        s_logger.warn("Unsupported data object (" + dataObj.getType() + ", " + dataObj.getDataStore() + ")");
        return false;
    }

    @Override
    public boolean deleteIfNotReady(DataObject dataObj) {
        long objId = dataObj.getId();
        DataStore dataStore = dataObj.getDataStore();
        if (dataStore.getRole() == DataStoreRole.Primary) {
            if (dataObj.getType() == DataObjectType.TEMPLATE) {
                VMTemplateStoragePoolVO destTmpltPool = templatePoolDao.findByPoolTemplate(dataStore.getId(), objId, null);
                if (destTmpltPool != null && destTmpltPool.getState() != ObjectInDataStoreStateMachine.State.Ready) {
                    return templatePoolDao.remove(destTmpltPool.getId());
                } else {
                    s_logger.warn("Template " + objId + " is not found on storage pool " + dataStore.getId() + ", so no need to delete");
                    return true;
                }
            } else if (dataObj.getType() == DataObjectType.SNAPSHOT) {
                SnapshotDataStoreVO destSnapshotStore = snapshotDataStoreDao.findByStoreSnapshot(dataStore.getRole(), dataStore.getId(), objId);
                if (destSnapshotStore != null && destSnapshotStore.getState() != ObjectInDataStoreStateMachine.State.Ready) {
                    snapshotDataStoreDao.remove(destSnapshotStore.getId());
                }
                return true;
            }
        } else {
            // Image store
            switch (dataObj.getType()) {
                case TEMPLATE:
                    return true;
                case SNAPSHOT:
                    SnapshotDataStoreVO destSnapshotStore = snapshotDataStoreDao.findByStoreSnapshot(dataStore.getRole(), dataStore.getId(), objId);
                    if (destSnapshotStore != null && destSnapshotStore.getState() != ObjectInDataStoreStateMachine.State.Ready) {
                        return snapshotDataStoreDao.remove(destSnapshotStore.getId());
                    } else {
                        s_logger.warn("Snapshot " + objId + " is not found on image store " + dataStore.getId() + ", so no need to delete");
                        return true;
                    }
                case VOLUME:
                    VolumeDataStoreVO destVolumeStore = volumeDataStoreDao.findByStoreVolume(dataStore.getId(), objId);
                    if (destVolumeStore != null && destVolumeStore.getState() != ObjectInDataStoreStateMachine.State.Ready) {
                        return volumeDataStoreDao.remove(destVolumeStore.getId());
                    } else {
                        s_logger.warn("Volume " + objId + " is not found on image store " + dataStore.getId() + ", so no need to delete");
                        return true;
                    }
            }
        }

        s_logger.warn("Unsupported data object (" + dataObj.getType() + ", " + dataObj.getDataStore() + "), no need to delete from object in store ref table");
        return false;
    }

    @Override
    public boolean update(DataObject data, Event event) throws NoTransitionException, ConcurrentOperationException {
        DataObjectInStore obj = this.findObject(data, data.getDataStore());
        if (obj == null) {
            throw new CloudRuntimeException("can't find mapping in ObjectInDataStore table for: " + data);
        }

        boolean result = true;
        if (data.getDataStore().getRole() == DataStoreRole.Image || data.getDataStore().getRole() == DataStoreRole.ImageCache) {
            switch (data.getType()) {
                case TEMPLATE:
                    result = this.stateMachines.transitTo(obj, event, null, templateDataStoreDao);
                    break;
                case SNAPSHOT:
                    result = this.stateMachines.transitTo(obj, event, null, snapshotDataStoreDao);
                    break;
                case VOLUME:
                    result = this.stateMachines.transitTo(obj, event, null, volumeDataStoreDao);
                    break;
            }
        } else if (data.getType() == DataObjectType.TEMPLATE && data.getDataStore().getRole() == DataStoreRole.Primary) {

            result = this.stateMachines.transitTo(obj, event, null, templatePoolDao);

        } else if (data.getType() == DataObjectType.SNAPSHOT && data.getDataStore().getRole() == DataStoreRole.Primary) {
            result = this.stateMachines.transitTo(obj, event, null, snapshotDataStoreDao);
        } else {
            throw new CloudRuntimeException("Invalid data or store type: " + data.getType() + " " + data.getDataStore().getRole());
        }

        if (!result) {
            throw new ConcurrentOperationException("Multiple threads are trying to update data object state, racing condition");
        }
        return true;
    }

    @Override
    public DataObject get(DataObject dataObj, DataStore store, String configuration) {
        if (dataObj.getType() == DataObjectType.TEMPLATE) {
            return imageFactory.getTemplate(dataObj, store, configuration);
        } else if (dataObj.getType() == DataObjectType.VOLUME) {
            return volumeFactory.getVolume(dataObj, store);
        } else if (dataObj.getType() == DataObjectType.SNAPSHOT) {
            return snapshotFactory.getSnapshot(dataObj, store);
        }

        throw new CloudRuntimeException("unknown type");
    }

    @Override
    public DataObjectInStore findObject(DataObject obj, DataStore store) {
        String deployAsIsConfiguration = null;
        if (obj.getType() == DataObjectType.TEMPLATE) {
            deployAsIsConfiguration = ((TemplateInfo) obj).getDeployAsIsConfiguration();
        }
        return findObject(obj.getId(), obj.getType(), store.getId(), store.getRole(), deployAsIsConfiguration);
    }

    @Override
    public DataObjectInStore findObject(long objId, DataObjectType type, long dataStoreId, DataStoreRole role, String deployAsIsConfiguration) {
        DataObjectInStore vo = null;
        if (role == DataStoreRole.Image || role == DataStoreRole.ImageCache) {
            switch (type) {
                case TEMPLATE:
                    vo = templateDataStoreDao.findByStoreTemplate(dataStoreId, objId);
                    break;
                case SNAPSHOT:
                    vo = snapshotDataStoreDao.findByStoreSnapshot(role, dataStoreId, objId);
                    break;
                case VOLUME:
                    vo = volumeDataStoreDao.findByStoreVolume(dataStoreId, objId);
                    break;
            }
        } else if (type == DataObjectType.TEMPLATE && role == DataStoreRole.Primary) {
            vo = templatePoolDao.findByPoolTemplate(dataStoreId, objId, deployAsIsConfiguration);
        } else if (type == DataObjectType.SNAPSHOT && role == DataStoreRole.Primary) {
            vo = snapshotDataStoreDao.findByStoreSnapshot(role, dataStoreId, objId);
        } else {
            s_logger.debug("Invalid data or store type: " + type + " " + role);
            throw new CloudRuntimeException("Invalid data or store type: " + type + " " + role);
        }

        return vo;

    }

    @Override
    public DataStore findStore(long objId, DataObjectType type, DataStoreRole role) {
        DataStore store = null;
        if (role == DataStoreRole.Image) {
            DataObjectInStore vo = null;
            switch (type) {
                case TEMPLATE:
                    vo = templateDataStoreDao.findByTemplate(objId, role);
                    break;
                case SNAPSHOT:
                    vo = snapshotDataStoreDao.findBySnapshot(objId, role);
                    break;
                case VOLUME:
                    vo = volumeDataStoreDao.findByVolume(objId);
                    break;
            }
            if (vo != null) {
                store = this.storeMgr.getDataStore(vo.getDataStoreId(), role);
            }
        }
        return store;
    }

}
