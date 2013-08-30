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

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.*;
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
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;

public class SnapshotObject implements SnapshotInfo {
    private static final Logger s_logger = Logger.getLogger(SnapshotObject.class);
    private SnapshotVO snapshot;
    private DataStore store;
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
        return this.store;
    }

    @Override
    public SnapshotInfo getParent() {

        SnapshotDataStoreVO snapStoreVO = this.snapshotStoreDao.findByStoreSnapshot(this.store.getRole(),
                this.store.getId(), this.snapshot.getId());
        Long parentId = null;
        if (snapStoreVO != null) {
            parentId = snapStoreVO.getParentSnapshotId();
            if (parentId != null && parentId != 0) {
                return this.snapshotFactory.getSnapshot(parentId, store);
            }
        }

        return null;
    }

    @Override
    public SnapshotInfo getChild() {
        SearchCriteriaService<SnapshotDataStoreVO, SnapshotDataStoreVO> sc = SearchCriteria2
                .create(SnapshotDataStoreVO.class);
        sc.addAnd(sc.getEntity().getDataStoreId(), Op.EQ, this.store.getId());
        sc.addAnd(sc.getEntity().getRole(), Op.EQ, this.store.getRole());
        sc.addAnd(sc.getEntity().getState(), Op.NIN, State.Destroying, State.Destroyed, State.Error);
        sc.addAnd(sc.getEntity().getParentSnapshotId(), Op.EQ, this.getId());
        SnapshotDataStoreVO vo = sc.find();
        if (vo == null) {
            return null;
        }
        return this.snapshotFactory.getSnapshot(vo.getId(), store);
    }

    @Override
    public VolumeInfo getBaseVolume() {
        return volFactory.getVolume(this.snapshot.getVolumeId());
    }

    @Override
    public long getId() {
        return this.snapshot.getId();
    }

    @Override
    public String getUri() {
        return this.snapshot.getUuid();
    }

    @Override
    public DataStore getDataStore() {
        return this.store;
    }

    @Override
    public Long getSize() {
        return this.snapshot.getSize();
    }

    @Override
    public DataObjectType getType() {
        return DataObjectType.SNAPSHOT;
    }

    @Override
    public String getUuid() {
        return this.snapshot.getUuid();
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
        try {
            objectInStoreMgr.update(this, event);
        } catch (Exception e) {
            s_logger.debug("Failed to update state:" + e.toString());
            throw new CloudRuntimeException("Failed to update state: " + e.toString());
        } finally {
            if (event == ObjectInDataStoreStateMachine.Event.OperationFailed) {
                objectInStoreMgr.deleteIfNotReady(this);
            }
        }
    }

    @Override
    public long getAccountId() {
        return this.snapshot.getAccountId();
    }

    @Override
    public long getVolumeId() {
        return this.snapshot.getVolumeId();
    }

    @Override
    public String getPath() {
        DataObjectInStore objectInStore = this.objectInStoreMgr.findObject(this, getDataStore());
        if (objectInStore != null) {
            return objectInStore.getInstallPath();
        }
        return null;
    }

    @Override
    public String getName() {
        return this.snapshot.getName();
    }

    @Override
    public Date getCreated() {
        return this.snapshot.getCreated();
    }

    @Override
    public Type getRecurringType() {
        return this.snapshot.getRecurringType();
    }

    @Override
    public State getState() {
        return this.snapshot.getState();
    }

    @Override
    public HypervisorType getHypervisorType() {
        return this.snapshot.getHypervisorType();
    }

    @Override
    public boolean isRecursive() {
        return this.snapshot.isRecursive();
    }

    @Override
    public short getsnapshotType() {
        return this.snapshot.getsnapshotType();
    }

    @Override
    public long getDomainId() {
        return this.snapshot.getDomainId();
    }

    @Override
    public Long getDataCenterId() {
        return this.snapshot.getDataCenterId();
    }

    public void processEvent(Snapshot.Event event) throws NoTransitionException {
        stateMachineMgr.processEvent(this.snapshot, event);
    }

    public SnapshotVO getSnapshotVO() {
        return this.snapshot;
    }

    @Override
    public DataTO getTO() {
        DataTO to = this.store.getDriver().getTO(this);
        if (to == null) {
            return new SnapshotObjectTO(this);
        }
        return to;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
        try {
            SnapshotDataStoreVO snapshotStore = this.snapshotStoreDao.findByStoreSnapshot(
                    this.getDataStore().getRole(), this.getDataStore().getId(), this.getId());
            if (answer instanceof CreateObjectAnswer) {
                SnapshotObjectTO snapshotTO = (SnapshotObjectTO) ((CreateObjectAnswer) answer).getData();
                snapshotStore.setInstallPath(snapshotTO.getPath());
                this.snapshotStoreDao.update(snapshotStore.getId(), snapshotStore);
            } else if (answer instanceof CopyCmdAnswer) {
                SnapshotObjectTO snapshotTO = (SnapshotObjectTO) ((CopyCmdAnswer) answer).getNewData();
                snapshotStore.setInstallPath(snapshotTO.getPath());
                if (snapshotTO.getParentSnapshotPath() == null) {
                    snapshotStore.setParentSnapshotId(0L);
                }
                this.snapshotStoreDao.update(snapshotStore.getId(), snapshotStore);
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
        if (this.store == null) {
            return;
        }

        if (this.store.getRole() == DataStoreRole.Image || this.store.getRole() == DataStoreRole.ImageCache) {
            SnapshotDataStoreVO store = snapshotStoreDao.findByStoreSnapshot(this.store.getRole(), this.store.getId(),
                    this.getId());
            store.incrRefCnt();
            store.setLastUpdated(new Date());
            snapshotStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public void decRefCount() {
        if (this.store == null) {
            return;
        }
        if (this.store.getRole() == DataStoreRole.Image || this.store.getRole() == DataStoreRole.ImageCache) {
            SnapshotDataStoreVO store = snapshotStoreDao.findByStoreSnapshot(this.store.getRole(), this.store.getId(),
                    this.getId());
            store.decrRefCnt();
            store.setLastUpdated(new Date());
            snapshotStoreDao.update(store.getId(), store);
        }
    }

    @Override
    public Long getRefCount() {
        if (this.store == null) {
            return null;
        }
        if (this.store.getRole() == DataStoreRole.Image || this.store.getRole() == DataStoreRole.ImageCache) {
            SnapshotDataStoreVO store = snapshotStoreDao.findByStoreSnapshot(this.store.getRole(), this.store.getId(),
                    this.getId());
            return store.getRefCnt();
        }
        return null;
    }

    @Override
    public ObjectInDataStoreStateMachine.State getStatus() {
        return this.objectInStoreMgr.findObject(this, store).getObjectInStoreState();
    }

    @Override
    public void addPayload(Object data) {
    }

    @Override
    public boolean delete() {
        if (store != null) {
            return store.delete(this);
        }
        return true;
    }
}
