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

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.db.ObjectInDataStoreDao;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.ObjectInDataStoreStateMachine.Event;
import org.springframework.stereotype.Component;



@Component
public  class ObjectInDataStoreManagerImpl implements ObjectInDataStoreManager {
    @Inject
    ObjectInDataStoreDao objectDataStoreDao;
    @Override
    public TemplateInfo create(TemplateInfo template, DataStore dataStore) {
        ObjectInDataStoreVO vo = new ObjectInDataStoreVO();
        vo.setDataStoreId(dataStore.getId());
        vo.setDataStoreType(dataStore.getRole());
        vo.setObjectId(template.getId());
        vo.setObjectType("template");
        vo = objectDataStoreDao.persist(vo);
        TemplateInDataStore tmpl = new TemplateInDataStore(template, dataStore, vo);
        return tmpl;
    }

    @Override
    public ObjectInDataStoreVO create(VolumeInfo volume, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ObjectInDataStoreVO create(SnapshotInfo snapshot, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TemplateInfo findTemplate(TemplateInfo template, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeInfo findVolume(VolumeInfo volume, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SnapshotInfo findSnapshot(SnapshotInfo snapshot, DataStore dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean update(TemplateInfo vo, Event event) {
        // TODO Auto-generated method stub
        return false;
    }

}
