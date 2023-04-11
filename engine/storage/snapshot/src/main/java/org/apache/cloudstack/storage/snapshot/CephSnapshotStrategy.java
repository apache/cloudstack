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

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotStrategy.SnapshotOperation;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;

public class CephSnapshotStrategy extends StorageSystemSnapshotStrategy {
    @Inject
    private SnapshotDataStoreDao snapshotStoreDao;
    @Inject
    private PrimaryDataStoreDao primaryDataStoreDao;
    @Inject
    private VolumeDao volumeDao;

    private static final Logger s_logger = Logger.getLogger(CephSnapshotStrategy.class);

    @Override
    public StrategyPriority canHandle(Snapshot snapshot, SnapshotOperation op) {
        long volumeId = snapshot.getVolumeId();
        VolumeVO volumeVO = volumeDao.findByIdIncludingRemoved(volumeId);
        boolean baseVolumeExists = volumeVO.getRemoved() == null;
        if (!baseVolumeExists) {
            return StrategyPriority.CANT_HANDLE;
        }

        if (!isSnapshotStoredOnRbdStoragePool(snapshot)) {
            return StrategyPriority.CANT_HANDLE;
        }

        if (SnapshotOperation.REVERT.equals(op)) {
            return StrategyPriority.HIGHEST;
        }

        return StrategyPriority.CANT_HANDLE;
    }

    @Override
    public boolean revertSnapshot(SnapshotInfo snapshotInfo) {
        VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();
        ImageFormat imageFormat = volumeInfo.getFormat();
        if (!ImageFormat.RAW.equals(imageFormat)) {
            s_logger.error(String.format("Does not support revert snapshot of the image format [%s] on Ceph/RBD. Can only rollback snapshots of format RAW", imageFormat));
            return false;
        }

        executeRevertSnapshot(snapshotInfo, volumeInfo);

        return true;
    }

    protected boolean isSnapshotStoredOnRbdStoragePool(Snapshot snapshot) {
        SnapshotDataStoreVO snapshotStore = snapshotStoreDao.findBySnapshot(snapshot.getId(), DataStoreRole.Primary);
        if (snapshotStore == null) {
            return false;
        }
        StoragePoolVO storagePoolVO = primaryDataStoreDao.findById(snapshotStore.getDataStoreId());
        return storagePoolVO != null && storagePoolVO.getPoolType() == StoragePoolType.RBD;
    }
}
