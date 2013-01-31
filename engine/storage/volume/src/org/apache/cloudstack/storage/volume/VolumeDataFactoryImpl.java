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
package org.apache.cloudstack.storage.volume;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.ObjectInDataStoreManager;
import org.springframework.stereotype.Component;

import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;

@Component
public class VolumeDataFactoryImpl implements VolumeDataFactory {
    @Inject
    VolumeDao volumeDao;
    @Inject
    ObjectInDataStoreManager objMap;
    @Inject
    DataStoreManager storeMgr;
    @Override
    public VolumeInfo getVolume(long volumeId, DataStore store) {
        VolumeVO volumeVO = volumeDao.findById(volumeId);
       
        VolumeObject vol = VolumeObject.getVolumeObject(store, volumeVO);
     
        return vol;
    }
    
    @Override
    public VolumeInfo getVolume(long volumeId) {
        VolumeVO volumeVO = volumeDao.findById(volumeId);
        VolumeObject vol = null;
        if (volumeVO.getPoolId() == null) {
            DataStore store = objMap.findStore(volumeVO.getUuid(), DataObjectType.VOLUME, DataStoreRole.Image);
            vol = VolumeObject.getVolumeObject(store, volumeVO);
        } else {
            DataStore store = this.storeMgr.getDataStore(volumeVO.getPoolId(), DataStoreRole.Primary);
            vol = VolumeObject.getVolumeObject(store, volumeVO);
        }
        return vol;
    }

    @Override
    public VolumeInfo getVolume(DataObject volume, DataStore store) {
        VolumeInfo vol = (VolumeObject)getVolume(volume.getId(), store);
        vol.addPayload(((VolumeInfo)volume).getpayload());
        return vol;
    }

}
