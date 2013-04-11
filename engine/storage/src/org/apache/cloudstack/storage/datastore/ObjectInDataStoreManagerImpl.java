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

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class ObjectInDataStoreManagerImpl implements ObjectInDataStoreManager {
    private static final Logger s_logger = Logger
            .getLogger(ObjectInDataStoreManagerImpl.class);
    @Inject
    ImageDataFactory imageFactory;
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
    protected StateMachine2<State, Event, DataObjectInStore> stateMachines;

    public ObjectInDataStoreManagerImpl() {
        stateMachines = new StateMachine2<State, Event, DataObjectInStore>();
        stateMachines.addTransition(State.Allocated, Event.CreateRequested,
                State.Creating);
        stateMachines.addTransition(State.Creating, Event.OperationSuccessed,
                State.Created);
        stateMachines.addTransition(State.Creating, Event.OperationFailed,
                State.Failed);
        stateMachines.addTransition(State.Failed, Event.CreateRequested,
                State.Creating);
        stateMachines.addTransition(State.Ready, Event.DestroyRequested,
                State.Destroying);
        stateMachines.addTransition(State.Destroying, Event.OperationSuccessed,
                State.Destroyed);
        stateMachines.addTransition(State.Destroying, Event.OperationFailed,
                State.Destroying);
        stateMachines.addTransition(State.Destroying, Event.DestroyRequested,
                State.Destroying);
        stateMachines.addTransition(State.Created, Event.CopyingRequested,
                State.Copying);
        stateMachines.addTransition(State.Copying, Event.OperationFailed,
                State.Created);
        stateMachines.addTransition(State.Copying, Event.OperationSuccessed,
                State.Ready);
        stateMachines.addTransition(State.Allocated, Event.CreateOnlyRequested,
                State.Creating2);
        stateMachines.addTransition(State.Creating2, Event.OperationFailed,
                State.Allocated);
        stateMachines.addTransition(State.Creating2, Event.OperationSuccessed,
                State.Ready);
    }

    @Override
    public DataObject create(DataObject obj, DataStore dataStore) {
        if (dataStore.getRole() == DataStoreRole.Primary) {
            if ( obj.getType() == DataObjectType.TEMPLATE){
                VMTemplateStoragePoolVO vo = new VMTemplateStoragePoolVO(dataStore.getId(), obj.getId());
                vo = templatePoolDao.persist(vo);
            }
        } else {
            // Image store
            switch ( obj.getType()){
            case TEMPLATE:
                TemplateDataStoreVO ts = new TemplateDataStoreVO();
                ts.setTemplateId(obj.getId());
                ts.setDataStoreId(dataStore.getId());
                ts = templateDataStoreDao.persist(ts);
                break;
            case SNAPSHOT:
                SnapshotDataStoreVO ss = new SnapshotDataStoreVO();
                ss.setSnapshotId(obj.getId());
                ss.setDataStoreId(dataStore.getId());
                ss = snapshotDataStoreDao.persist(ss);
                break;
            case VOLUME:
                VolumeDataStoreVO vs = new VolumeDataStoreVO();
                vs.setVolumeId(obj.getId());
                vs.setDataStoreId(dataStore.getId());
                vs = volumeDataStoreDao.persist(vs);
                break;
            }
        }

        return this.get(obj,  dataStore);
    }

    @Override
    public boolean update(DataObject data, Event event)
            throws NoTransitionException {
        DataObjectInStore obj = this.findObject(data, data.getDataStore());
        if (obj == null) {
            throw new CloudRuntimeException(
                    "can't find mapping in ObjectInDataStore table for: "
                            + data);
        }

        if ( data.getDataStore().getRole() == DataStoreRole.Image){
            switch (data.getType()){
            case TEMPLATE:
                this.stateMachines.transitTo(obj, event, null, templateDataStoreDao);
            case SNAPSHOT:
                this.stateMachines.transitTo(obj, event, null, snapshotDataStoreDao);
            case VOLUME:
                this.stateMachines.transitTo(obj, event, null, volumeDataStoreDao);
            }
        } else if (data.getType() == DataObjectType.TEMPLATE && data.getDataStore().getRole() == DataStoreRole.Primary) {
            try {
            this.stateMachines.transitTo(obj, event, null,
                    templatePoolDao);
            } catch (NoTransitionException e) {
                if (event == Event.CreateOnlyRequested || event == Event.OperationSuccessed) {
                    s_logger.debug("allow muliple create requests");
                } else {
                    throw e;
                }
            }
        } else {
            throw new CloudRuntimeException("Invalid data or store type: " + data.getType() + " " + data.getDataStore().getRole());
        }
        return true;
    }

    @Override
    public DataObject get(DataObject dataObj, DataStore store) {
        if (dataObj.getType() == DataObjectType.TEMPLATE) {
            return imageFactory.getTemplate(dataObj, store);
        } else if (dataObj.getType() == DataObjectType.VOLUME) {
            return volumeFactory.getVolume(dataObj, store);
        } else if (dataObj.getType() == DataObjectType.SNAPSHOT) {
            return snapshotFactory.getSnapshot(dataObj, store);
        }

        throw new CloudRuntimeException("unknown type");
    }

    @Override
    public DataObjectInStore findObject(DataObject obj, DataStore store) {
        return findObject(obj.getId(), obj.getType(), store.getId(), store.getRole());
    }


    @Override
    public DataObjectInStore findObject(long objId, DataObjectType type,
            long dataStoreId, DataStoreRole role) {
        DataObjectInStore vo = null;
        if (role == DataStoreRole.Image) {
            switch (type){
            case TEMPLATE:
                SearchCriteria<TemplateDataStoreVO> ts =  templateDataStoreDao.createSearchCriteria();
                ts.addAnd("templateId", SearchCriteria.Op.EQ, objId);
                ts.addAnd("dataStoreId", SearchCriteria.Op.EQ, dataStoreId);
                vo =  templateDataStoreDao.findOneBy(ts);
            case SNAPSHOT:
                SearchCriteria<SnapshotDataStoreVO> ss =  snapshotDataStoreDao.createSearchCriteria();
                ss.addAnd("snapshotId", SearchCriteria.Op.EQ, objId);
                ss.addAnd("dataStoreId", SearchCriteria.Op.EQ, objId);
                vo =  snapshotDataStoreDao.findOneBy(ss);
            case VOLUME:
                SearchCriteria<VolumeDataStoreVO> vs =  volumeDataStoreDao.createSearchCriteria();
                vs.addAnd("volumeId", SearchCriteria.Op.EQ, objId);
                vs.addAnd("dataStoreId", SearchCriteria.Op.EQ, objId);
                vo =  volumeDataStoreDao.findOneBy(vs);
            }
        } else if (type == DataObjectType.TEMPLATE && role == DataStoreRole.Primary) {
            vo = templatePoolDao.findByPoolTemplate(dataStoreId, objId);
        } else {
            s_logger.debug("Invalid data or store type: " + type + " " + role);
            throw new CloudRuntimeException("Invalid data or store type: " + type + " " + role);
        }

        return vo;

    }

    @Override
    public DataStore findStore(String objUuid, DataObjectType type,  DataStoreRole role) {
        DataStore store = null;
        if (role == DataStoreRole.Image) {
            SearchCriteriaService<ObjectInDataStoreVO, ObjectInDataStoreVO> sc = SearchCriteria2.create(ObjectInDataStoreVO.class);
            sc.addAnd(sc.getEntity().getDataStoreRole(), Op.EQ, role);
            sc.addAnd(sc.getEntity().getObjectUuid(), Op.EQ, objUuid);
            sc.addAnd(sc.getEntity().getObjectType(), Op.EQ, type);
            ObjectInDataStoreVO vo = sc.find();
            if (vo != null) {
                store = this.storeMgr.getDataStore(vo.getDataStoreUuid(), vo.getDataStoreRole());
            }
        }
        return store;
    }

}
