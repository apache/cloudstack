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
package org.apache.cloudstack.storage.datastore.db;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.storage.Volume;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface VolumeDataStoreDao extends GenericDao<VolumeDataStoreVO, Long>,
        StateDao<ObjectInDataStoreStateMachine.State, ObjectInDataStoreStateMachine.Event, DataObjectInStore> {

    List<VolumeDataStoreVO> listByStoreId(long id);

    List<VolumeDataStoreVO> listActiveOnCache(long id);

    void deletePrimaryRecordsForStore(long id);

    VolumeDataStoreVO findByVolume(long volumeId);

    VolumeDataStoreVO findByStoreVolume(long storeId, long volumeId);

    VolumeDataStoreVO findByStoreVolume(long storeId, long volumeId, boolean lock);

    List<VolumeDataStoreVO> listDestroyed(long storeId);

    void duplicateCacheRecordsOnRegionStore(long storeId);

    List<VolumeDataStoreVO> listVolumeDownloadUrls();

    void expireDnldUrlsForZone(Long dcId);

    List<VolumeDataStoreVO> listUploadedVolumesByStoreId(long id);

    List<VolumeDataStoreVO> listByVolumeState(Volume.State... states);

    boolean updateVolumeId(long srcVolId, long destVolId);

    List<VolumeDataStoreVO> listVolumeDownloadUrlsByZoneId(long zoneId);
}
