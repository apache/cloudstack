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
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.exception.CloudRuntimeException;

public class SnapshotDataFactoryImpl implements SnapshotDataFactory {

    @Inject
    private SnapshotDao snapshotDao;
    @Inject
    private SnapshotDataStoreDao snapshotStoreDao;
    @Inject
    private DataStoreManager storeMgr;

    @Override
    public SnapshotInfo getSnapshot(long snapshotId, DataStore store) {
        SnapshotVO snapshot = snapshotDao.findById(snapshotId);
        SnapshotObject so = SnapshotObject.getSnapshotObject(snapshot, store);
        return so;
    }

    @Override
    public SnapshotInfo getSnapshot(DataObject obj, DataStore store) {
        SnapshotVO snapshot = snapshotDao.findById(obj.getId());
        if (snapshot == null) {
            throw new CloudRuntimeException("Can't find snapshot: " + obj.getId());
        }
        SnapshotObject so = SnapshotObject.getSnapshotObject(snapshot, store);
        return so;
    }

    @Override
    public List<SnapshotInfo> getSnapshots(long volumeId, DataStoreRole role) {
        List<SnapshotDataStoreVO> allSnapshotsFromVolumeAndDataStore = snapshotStoreDao.listAllByVolumeAndDataStore(volumeId, role);
        if (CollectionUtils.isEmpty(allSnapshotsFromVolumeAndDataStore)) {
            return new ArrayList<>();
        }
        List<SnapshotInfo> infos = new ArrayList<>();
        for (SnapshotDataStoreVO snapshotDataStoreVO : allSnapshotsFromVolumeAndDataStore) {
            DataStore store = storeMgr.getDataStore(snapshotDataStoreVO.getDataStoreId(), role);
            SnapshotVO snapshot = snapshotDao.findById(snapshotDataStoreVO.getSnapshotId());
            if (snapshot == null){ //snapshot may have been removed;
                continue;
            }
            SnapshotObject info = SnapshotObject.getSnapshotObject(snapshot, store);

            infos.add(info);
        }
        return infos;
    }


    @Override
    public SnapshotInfo getSnapshot(long snapshotId, DataStoreRole role) {
        SnapshotVO snapshot = snapshotDao.findById(snapshotId);
        if (snapshot == null) {
            return null;
        }
        SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findBySnapshot(snapshotId, role);
        if (snapshotStore == null) {
            snapshotStore = snapshotStoreDao.findByVolume(snapshot.getVolumeId(), role);
            if (snapshotStore == null) {
                return null;
            }
        }
        DataStore store = storeMgr.getDataStore(snapshotStore.getDataStoreId(), role);
        SnapshotObject so = SnapshotObject.getSnapshotObject(snapshot, store);
        return so;
    }

    @Override
    public SnapshotInfo getReadySnapshotOnCache(long snapshotId) {
        SnapshotDataStoreVO snapStore = snapshotStoreDao.findReadyOnCache(snapshotId);
        if (snapStore != null) {
            DataStore store = storeMgr.getDataStore(snapStore.getDataStoreId(), DataStoreRole.ImageCache);
            return getSnapshot(snapshotId, store);
        } else {
            return null;
        }

    }

    @Override
    public List<SnapshotInfo> listSnapshotOnCache(long snapshotId) {
        List<SnapshotDataStoreVO> cacheSnapshots = snapshotStoreDao.listOnCache(snapshotId);
        List<SnapshotInfo> snapObjs = new ArrayList<SnapshotInfo>();
        for (SnapshotDataStoreVO cacheSnap : cacheSnapshots) {
            long storeId = cacheSnap.getDataStoreId();
            DataStore store = storeMgr.getDataStore(storeId, DataStoreRole.ImageCache);
            SnapshotInfo tmplObj = getSnapshot(snapshotId, store);
            snapObjs.add(tmplObj);
        }
        return snapObjs;
    }

}
