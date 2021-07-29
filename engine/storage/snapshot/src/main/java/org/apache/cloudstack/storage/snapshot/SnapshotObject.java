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
package org.apache.cloudstack.storage.snapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

public class SnapshotObject implements SnapshotInfo {
    private static final Logger s_logger = Logger.getLogger(SnapshotObject.class);
    private SnapshotVO snapshot;
    private DataStore store;
    private Object payload;
    private Boolean fullBackup;
    @Inject
    protected SnapshotDao snapshotDao;
    @Inject
    protected VolumeDao volumeDao;
    @Inject
    protected VolumeDataFactory volFactory;
    @Inject
    protected SnapshotStateMachineManager stateMachineMgr;
    @Inject
    SnapshotDataFactory snapshotFactory;
    @Inject
    ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    SnapshotDataStoreDao snapshotStoreDao;
    @Inject
    StorageStrategyFactory storageStrategyFactory;
    private String installPath; // temporarily set installPath before passing to resource for entries with empty installPath for object store migration case

    public SnapshotObject() {

    }

    protected void configure(SnapshotVO snapshot, DataStore store) {
        this.snapshot = snapshot;
        this.store = store;
    }

    public static SnapshotObject getSnapshotObject(SnapshotVO snapshot, DataStore store) {
        SnapshotObject snapObj = ComponentContext.inject(SnapshotObject.class);
        snapObj.configure(snapshot, store);
        return snapObj;
    }

    public DataStore getStore() {
        return store;
    }

    @Override
    public SnapshotInfo getParent() {

        SnapshotDataStoreVO snapStoreVO = snapshotStoreDao.findByStoreSnapshot(store.getRole(), store.getId(), snapshot.getId());
        Long parentId = null;
        if (snapStoreVO != null) {
            parentId = snapStoreVO.getParentSnapshotId();
            if (parentId != null && parentId != 0) {
                return snapshotFactory.getSnapshot(parentId, store);
            }
        }

        return null;
    }

    @Override
    public SnapshotInfo getChild() {
        QueryBuilder<SnapshotDataStoreVO> sc = QueryBuilder.create(SnapshotDataStoreVO.class);
        sc.and(sc.entity().getDataStoreId(), Op.EQ, store.getId());
        sc.and(sc.entity().getRole(), Op.EQ, store.getRole());
        sc.and(sc.entity().getState(), Op.NIN, State.Destroying, State.Destroyed, State.Error);
        sc.and(sc.entity().getParentSnapshotId(), Op.EQ, getId());
        SnapshotDataStoreVO vo = sc.find();
        if (vo == null) {
            return null;
        }
        return snapshotFactory.getSnapshot(vo.getId(), store);
    }

    @Override
    public List<SnapshotInfo> getChildren() {
        QueryBuilder<SnapshotDataStoreVO> sc = QueryBuilder.create(SnapshotDataStoreVO.class);
        sc.and(sc.entity().getDataStoreId(), Op.EQ, store.getId());
        sc.and(sc.entity().getRole(), Op.EQ, store.getRole());
        sc.and(sc.entity().getState(), Op.NIN, State.Destroying, State.Destroyed, State.Error);
        sc.and(sc.entity().getParentSnapshotId(), Op.EQ, getId());
        List<SnapshotDataStoreVO> vos = sc.list();

        List<SnapshotInfo> children = new ArrayList<>();
        if (vos != null) {
            for (SnapshotDataStoreVO vo : vos) {
                SnapshotInfo info = snapshotFactory.getSnapshot(vo.getSnapshotId(), DataStoreRole.Image);
                if (info != null) {
                    children.add(info);
                }
            }
        }
        return children;
    }

    @Override
    public boolean isRevertable() {
        SnapshotStrategy snapshotStrategy = storageStrategyFactory.getSnapshotStrategy(snapshot, SnapshotOperation.REVERT);
        if (snapshotStrategy != null) {
            return true;
        }

        return false;
    }

    @Override
    public long getPhysicalSize() {
        long physicalSize = 0;
        SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Image);
        if (snapshotStore != null) {
            physicalSize = snapshotStore.getPhysicalSize();
        }
        return physicalSize;
    }

    @Override
    public void markBackedUp() throws CloudRuntimeException{
        try {
            processEvent(Event.OperationNotPerformed);
        } catch (NoTransitionException ex) {
            s_logger.error("no transition error: ", ex);
            throw new CloudRuntimeException("Error marking snapshot backed up: " +
                    this.snapshot.getId() + " " + ex.getMessage());
        }
    }

    @Override
    public VolumeInfo getBaseVolume() {
        return volFactory.getVolume(snapshot.getVolumeId());
    }

    @Override
    public long getId() {
        return snapshot.getId();
    }

    @Override
    public String getUri() {
        return snapshot.getUuid();
    }

    @Override
    public DataStore getDataStore() {
        return store;
    }

    @Override
    public Long getSize() {
        return snapshot.getSize();
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.SNAPSHOT;
    }

    @Override
    public String getUuid() {
        return snapshot.getUuid();
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
        try {
            objectInStoreMgr.update(this, event);
        } catch (Exception e) {
            s_logger.debug("Failed to update state:" + e.toString());
            throw new CloudRuntimeException("Failed to update state: " + e.toString());
        } finally {
            DataObjectInStore obj = objectInStoreMgr.findObject(this, this.getDataStore());
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed && !obj.getState().equals(ObjectInDataStoreStateMachine.State.Destroying)) {
                // Don't delete db entry if snapshot is successfully removed.
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }
    }

    @Override
    public long getAccountId() {
        return snapshot.getAccountId();
    }

    @Override
    public long getVolumeId() {
        return snapshot.getVolumeId();
    }

    @Override
    public String getPath() {
        if (installPath != null)
            return installPath;

        DataObjectInStore objectInStore = objectInStoreMgr.findObject(this, getDataStore());
        if (objectInStore != null) {
            return objectInStore.getInstallPath();
        }
        return null;
    }

    public void setPath(String installPath) {
        this.installPath = installPath;
    }

    @Override
    public String getName() {
        return snapshot.getName();
    }

    @Override
    public long getSnapshotId() {
        return snapshot.getSnapshotId();
    }

    @Override
    public Date getCreated() {
        return snapshot.getCreated();
    }

    @Override
    public Type getRecurringType() {
        return snapshot.getRecurringType();
    }

    @Override
    public LocationType getLocationType() { return snapshot.getLocationType(); }

    @Override
    public State getState() {
        return snapshot.getState();
    }

    @Override
    public HypervisorType getHypervisorType() {
        return snapshot.getHypervisorType();
    }

    @Override
    public boolean isRecursive() {
        return snapshot.isRecursive();
    }

    @Override
    public short getsnapshotType() {
        return snapshot.getsnapshotType();
    }

    @Override
    public long getDomainId() {
        return snapshot.getDomainId();
    }

    @Override
    public Long getDataCenterId() {
        return snapshot.getDataCenterId();
    }

    public void processEvent(Snapshot.Event event) throws NoTransitionException {
        stateMachineMgr.processEvent(snapshot, event);
    }

    public SnapshotVO getSnapshotVO() {
        return snapshot;
    }

    @Override
    public DataTO getTO() {
        DataTO to = store.getDriver().getTO(this);
        if (to == null) {
            return new SnapshotObjectTO(this);
        }
        return to;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findByStoreSnapshot(getDataStore().getRole(), getDataStore().getId(), getId());
            if (answer instanceof CreateObjectAnswer) {
                SnapshotObjectTO snapshotTO = (SnapshotObjectTO)((CreateObjectAnswer)answer).getData();
                snapshotStore.setInstallPath(snapshotTO.getPath());
                snapshotStoreDao.update(snapshotStore.getId(), snapshotStore);
            } else if (answer instanceof CopyCmdAnswer) {
                SnapshotObjectTO snapshotTO = (SnapshotObjectTO)((CopyCmdAnswer)answer).getNewData();
                snapshotStore.setInstallPath(snapshotTO.getPath());
                if (snapshotTO.getPhysicalSize() != null) {
                    // For S3 delta snapshot, physical size is currently not set
                snapshotStore.setPhysicalSize(snapshotTO.getPhysicalSize());
                }
                if (snapshotTO.getParentSnapshotPath() == null) {
                    snapshotStore.setParentSnapshotId(0L);
                }
                snapshotStoreDao.update(snapshotStore.getId(), snapshotStore);

                // update side-effect of snapshot operation
                if (snapshotTO.getVolume() != null && snapshotTO.getVolume().getPath() != null) {
                    VolumeVO vol = volumeDao.findByUuid(snapshotTO.getVolume().getUuid());
                    if (vol != null) {
                        s_logger.info("Update volume path change due to snapshot operation, volume " + vol.getId() + " path: " + vol.getPath() + "->" +
                            snapshotTO.getVolume().getPath());
                        vol.setPath(snapshotTO.getVolume().getPath());
                        volumeDao.update(vol.getId(), vol);
                    } else {
                        s_logger.error("Cound't find the original volume with uuid: " + snapshotTO.getVolume().getUuid());
                    }
                }
            } else {
                throw new CloudRuntimeException("Unknown answer: " + answer.getClass());
            }
        } catch (RuntimeException ex) {
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
            throw ex;
        }
        this.processEvent(event);
    }

    @Override
    public void incRefCount() {
        if (store == null) {
            return;
        }

        if (store.getRole() == DataStoreRole.Image || store.getRole() == DataStoreRole.ImageCache) {
            SnapshotDataStoreVO store = snapshotStoreDao.findByStoreSnapshot(this.store.getRole(), this.store.getId(), getId());
            store.incrRefCnt();
            store.setLastUpdated(new Date());
            snapshotStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public void decRefCount() {
        if (store == null) {
            return;
        }
        if (store.getRole() == DataStoreRole.Image || store.getRole() == DataStoreRole.ImageCache) {
            SnapshotDataStoreVO store = snapshotStoreDao.findByStoreSnapshot(this.store.getRole(), this.store.getId(), getId());
            store.decrRefCnt();
            store.setLastUpdated(new Date());
            snapshotStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public Long getRefCount() {
        if (store == null) {
            return null;
        }
        if (store.getRole() == DataStoreRole.Image || store.getRole() == DataStoreRole.ImageCache) {
            SnapshotDataStoreVO store = snapshotStoreDao.findByStoreSnapshot(this.store.getRole(), this.store.getId(), getId());
            return store.getRefCnt();
        }
        return null;
    }

    @Override
    public ObjectInDataStoreStateMachine.State getStatus() {
        return objectInStoreMgr.findObject(this, store).getObjectInStoreState();
    }

    @Override
    public void addPayload(Object data) {
        payload = data;
    }

    @Override
    public Object getPayload() {
        return payload;
    }

    @Override
    public void setFullBackup(Boolean data) {
        fullBackup = data;
    }

    @Override
    public Boolean getFullBackup() {
        return fullBackup;
    }

    @Override
    public boolean delete() {
        if (store != null) {
            return store.delete(this);
        }
        return true;
    }

    @Override
    public Class<?> getEntityType() {
        return Snapshot.class;
    }
}
