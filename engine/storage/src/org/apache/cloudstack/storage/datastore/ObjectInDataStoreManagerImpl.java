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
import org.apache.cloudstack.storage.db.ObjectInDataStoreDao;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeHostDao;
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
    ObjectInDataStoreDao objectDataStoreDao;
    @Inject
    VolumeHostDao volumeHostDao;
    @Inject
    VMTemplateHostDao templateHostDao;
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
        if (obj.getType() == DataObjectType.TEMPLATE && dataStore.getRole() == DataStoreRole.Primary) {
            VMTemplateStoragePoolVO vo = new VMTemplateStoragePoolVO(dataStore.getId(), obj.getId());
            vo = templatePoolDao.persist(vo);
        } else {
            ObjectInDataStoreVO vo = new ObjectInDataStoreVO();
            vo.setDataStoreRole(dataStore.getRole());
            vo.setDataStoreUuid(dataStore.getUuid());
            vo.setObjectType(obj.getType());
            vo.setObjectUuid(obj.getUuid());
            vo = objectDataStoreDao.persist(vo);
        }

        if (obj.getType() == DataObjectType.TEMPLATE) {
            return imageFactory.getTemplate(obj, dataStore);
        } else if (obj.getType() == DataObjectType.VOLUME) {
            return volumeFactory.getVolume(obj, dataStore); 
        } else if (obj.getType() == DataObjectType.SNAPSHOT) {
            return snapshotFactory.getSnapshot(obj, dataStore);
        }
        throw new CloudRuntimeException("unknown type");
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
        
        if (data.getType() == DataObjectType.TEMPLATE && data.getDataStore().getRole() == DataStoreRole.Primary) {
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
            this.stateMachines.transitTo(obj, event, null, objectDataStoreDao);
        }
        return true;
    }

    @Override
    public DataObject get(DataObject dataObj, DataStore store) {
        if (dataObj.getType() == DataObjectType.TEMPLATE) {
            return imageFactory.getTemplate(dataObj, store);
        } else if (dataObj.getType() == DataObjectType.VOLUME) {
            return volumeFactory.getVolume(dataObj, store); 
        }
        throw new CloudRuntimeException("unknown type");
    }

    @Override
    public DataObjectInStore findObject(DataObject obj, DataStore store) {
        DataObjectInStore vo = null;
        SearchCriteriaService<ObjectInDataStoreVO, ObjectInDataStoreVO> sc = SearchCriteria2.create(ObjectInDataStoreVO.class);
        
        if (store.getRole() == DataStoreRole.Image) {
            sc.addAnd(sc.getEntity().getDataStoreUuid(), Op.EQ, store.getUuid());
            sc.addAnd(sc.getEntity().getDataStoreRole(), Op.EQ, store.getRole());
            sc.addAnd(sc.getEntity().getObjectUuid(), Op.EQ, obj.getUuid());
            sc.addAnd(sc.getEntity().getObjectType(), Op.EQ, obj.getType());
            vo = sc.find();
        } else if (obj.getType() == DataObjectType.TEMPLATE && store.getRole() == DataStoreRole.Primary) {
            vo = templatePoolDao.findByPoolTemplate(store.getId(), obj.getId());
        } else {
            s_logger.debug("unknown type: " + obj.getType() + " " + store.getRole());
            throw new CloudRuntimeException("unknown type");
        }
        return vo;
    }

    @Override
    public DataObjectInStore findObject(String uuid, DataObjectType type,
            String dataStoreUuid, DataStoreRole role) {
        DataObjectInStore vo = null;
        SearchCriteriaService<ObjectInDataStoreVO, ObjectInDataStoreVO> sc = SearchCriteria2.create(ObjectInDataStoreVO.class);
        
        if (role == DataStoreRole.Image) {
            sc.addAnd(sc.getEntity().getDataStoreUuid(), Op.EQ, dataStoreUuid);
            sc.addAnd(sc.getEntity().getDataStoreRole(), Op.EQ, role);
            sc.addAnd(sc.getEntity().getObjectUuid(), Op.EQ, uuid);
            sc.addAnd(sc.getEntity().getObjectType(), Op.EQ, type);
            vo = sc.find();
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
