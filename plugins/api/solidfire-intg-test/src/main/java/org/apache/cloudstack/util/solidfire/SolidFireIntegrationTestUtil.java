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
package org.apache.cloudstack.util.solidfire;

import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.SnapshotDetailsDao;
import com.cloud.storage.dao.SnapshotDetailsVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.api.response.solidfire.ApiVolumeSnapshotDetailsResponse;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

public class SolidFireIntegrationTestUtil {
    @Inject private AccountDao accountDao;
    @Inject private ClusterDao clusterDao;
    @Inject private PrimaryDataStoreDao storagePoolDao;
    @Inject private SnapshotDao snapshotDao;
    @Inject private SnapshotDetailsDao snapshotDetailsDao;
    @Inject private VolumeDao volumeDao;

    private SolidFireIntegrationTestUtil() {}

    public long getAccountIdForAccountUuid(String accountUuid) {
        Account account = accountDao.findByUuid(accountUuid);

        if (account == null) {
            throw new CloudRuntimeException("Unable to find Account for ID: " + accountUuid);
        }

        return account.getAccountId();
    }

    public long getAccountIdForVolumeUuid(String volumeUuid) {
        VolumeVO volume = volumeDao.findByUuid(volumeUuid);

        if (volume == null) {
            throw new CloudRuntimeException("Unable to find Volume for ID: " + volumeUuid);
        }

        return volume.getAccountId();
    }

    public long getAccountIdForSnapshotUuid(String snapshotUuid) {
        SnapshotVO snapshot = snapshotDao.findByUuid(snapshotUuid);

        if (snapshot == null) {
            throw new CloudRuntimeException("Unable to find Volume for ID: " + snapshotUuid);
        }

        return snapshot.getAccountId();
    }

    public long getClusterIdForClusterUuid(String clusterUuid) {
        ClusterVO cluster = clusterDao.findByUuid(clusterUuid);

        if (cluster == null) {
            throw new CloudRuntimeException("Unable to find Volume for ID: " + clusterUuid);
        }

        return cluster.getId();
    }

    public long getStoragePoolIdForStoragePoolUuid(String storagePoolUuid) {
        StoragePoolVO storagePool = storagePoolDao.findByUuid(storagePoolUuid);

        if (storagePool == null) {
            throw new CloudRuntimeException("Unable to find Volume for ID: " + storagePoolUuid);
        }

        return storagePool.getId();
    }

    public String getPathForVolumeUuid(String volumeUuid) {
        VolumeVO volume = volumeDao.findByUuid(volumeUuid);

        if (volume == null) {
            throw new CloudRuntimeException("Unable to find Volume for ID: " + volumeUuid);
        }

        return volume.getPath();
    }

    public String getVolume_iScsiName(String volumeUuid) {
        VolumeVO volume = volumeDao.findByUuid(volumeUuid);

        if (volume == null) {
            throw new CloudRuntimeException("Unable to find Volume for ID: " + volumeUuid);
        }

        return volume.get_iScsiName();
    }

    public List<ApiVolumeSnapshotDetailsResponse> getSnapshotDetails(String snapshotUuid) {
        SnapshotVO snapshot = snapshotDao.findByUuid(snapshotUuid);

        if (snapshot == null) {
            throw new CloudRuntimeException("Unable to find Volume for ID: " + snapshotUuid);
        }

        List<SnapshotDetailsVO> snapshotDetails = snapshotDetailsDao.listDetails(snapshot.getId());

        List<ApiVolumeSnapshotDetailsResponse> responses = new ArrayList<>();

        if (snapshotDetails != null) {
            for (SnapshotDetailsVO snapshotDetail : snapshotDetails) {
                ApiVolumeSnapshotDetailsResponse response = new ApiVolumeSnapshotDetailsResponse(
                    snapshotDetail.getResourceId(),
                    snapshotDetail.getName(),
                    snapshotDetail.getValue()
                );

                responses.add(response);
            }
        }

        return responses;
    }
}
