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
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.db.ObjectInDataStoreDao;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.ImageDataFactory;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.State;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;

@Component
public class ObjectInDataStoreManagerImpl implements ObjectInDataStoreManager {
    @Inject
    ImageDataFactory imageFactory;
    @Inject
    VolumeDataFactory volumeFactory;
    @Inject
    ObjectInDataStoreDao objectDataStoreDao;
    protected StateMachine2<State, Event, ObjectInDataStoreVO> stateMachines;

    public ObjectInDataStoreManagerImpl() {
        stateMachines = new StateMachine2<State, Event, ObjectInDataStoreVO>();
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
                State.Failed);
        stateMachines.addTransition(State.Creating2, Event.OperationSuccessed,
                State.Ready);
    }

    @Override
    public DataObject create(DataObject obj, DataStore dataStore) {
        
        ObjectInDataStoreVO vo = new ObjectInDataStoreVO();
        vo.setDataStoreId(dataStore.getId());
        vo.setDataStoreRole(dataStore.getRole());
        vo.setObjectId(obj.getId());
        vo.setSize(obj.getSize());

        vo.setObjectType(obj.getType());
        vo = objectDataStoreDao.persist(vo);

        if (obj.getType() == DataObjectType.TEMPLATE) {
            return imageFactory.getTemplate(obj.getId(), dataStore);
        } else if (obj.getType() == DataObjectType.VOLUME) {
            return volumeFactory.getVolume(obj.getId(), dataStore); 
        }
        throw new CloudRuntimeException("unknown type");
    }

    @Override
    public VolumeInfo create(VolumeInfo volume, DataStore dataStore) {
        ObjectInDataStoreVO vo = new ObjectInDataStoreVO();
        vo.setDataStoreId(dataStore.getId());
        vo.setDataStoreRole(dataStore.getRole());
        vo.setObjectId(volume.getId());
        vo.setObjectType(volume.getType());
        vo = objectDataStoreDao.persist(vo);

        return volumeFactory.getVolume(volume.getId(), dataStore);
    }

    @Override
    public SnapshotInfo create(SnapshotInfo snapshot, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectInDataStoreVO findObject(long objectId, DataObjectType type,
            long dataStoreId, DataStoreRole role) {
        SearchCriteriaService<ObjectInDataStoreVO, ObjectInDataStoreVO> sc = SearchCriteria2
                .create(ObjectInDataStoreVO.class);
        sc.addAnd(sc.getEntity().getObjectId(), Op.EQ, objectId);
        sc.addAnd(sc.getEntity().getDataStoreId(), Op.EQ, dataStoreId);
        sc.addAnd(sc.getEntity().getObjectType(), Op.EQ, type);
        sc.addAnd(sc.getEntity().getDataStoreRole(), Op.EQ, role);
        sc.addAnd(sc.getEntity().getState(), Op.NIN,
                ObjectInDataStoreStateMachine.State.Destroyed,
                ObjectInDataStoreStateMachine.State.Failed);
        ObjectInDataStoreVO objectStoreVO = sc.find();
        return objectStoreVO;

    }

    @Override
    public boolean update(DataObject data, Event event)
            throws NoTransitionException {
        ObjectInDataStoreVO obj = this.findObject(data.getId(), data.getType(),
                data.getDataStore().getId(), data.getDataStore().getRole());
        if (obj == null) {
            throw new CloudRuntimeException(
                    "can't find mapping in ObjectInDataStore table for: "
                            + data);
        }
        return this.stateMachines.transitTo(obj, event, null,
                objectDataStoreDao);

    }
    
    @Override
    public boolean update(ObjectInDataStoreVO obj, Event event)
            throws NoTransitionException {
        return this.stateMachines.transitTo(obj, event, null,
                objectDataStoreDao);

    }

    @Override
    public DataObject get(DataObject dataObj, DataStore store) {
        if (dataObj.getType() == DataObjectType.TEMPLATE) {
            return imageFactory.getTemplate(dataObj.getId(), store);
        } else if (dataObj.getType() == DataObjectType.VOLUME) {
            return volumeFactory.getVolume(dataObj.getId(), store); 
        }
        throw new CloudRuntimeException("unknown type");
    }

    @Override
    public boolean update(ObjectInDataStoreVO obj) {
        return objectDataStoreDao.update(obj.getId(), obj);
    }
}
