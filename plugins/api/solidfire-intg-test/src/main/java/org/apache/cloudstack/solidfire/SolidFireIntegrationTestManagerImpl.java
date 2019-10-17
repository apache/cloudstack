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
package org.apache.cloudstack.solidfire;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.VolumeDetailVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.dao.VolumeDetailsDao;
import com.cloud.user.AccountDetailVO;
import com.cloud.user.AccountDetailsDao;
import com.cloud.utils.exception.CloudRuntimeException;

import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.util.SolidFireUtil;
import org.apache.cloudstack.util.solidfire.SolidFireIntegrationTestUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

@Component
public class SolidFireIntegrationTestManagerImpl implements SolidFireIntegrationTestManager {
    @Inject private AccountDetailsDao accountDetailsDao;
    @Inject private HostDao hostDao;
    @Inject private SolidFireIntegrationTestUtil util;
    @Inject private StoragePoolDetailsDao storagePoolDetailsDao;
    @Inject private VolumeDao volumeDao;
    @Inject private VolumeDetailsDao volumeDetailsDao;

    @Override
    public long getSolidFireAccountId(String csAccountUuid, String storagePoolUuid) {
        long csAccountId = util.getAccountIdForAccountUuid(csAccountUuid);
        long storagePoolId = util.getStoragePoolIdForStoragePoolUuid(storagePoolUuid);

        AccountDetailVO accountDetail = accountDetailsDao.findDetail(csAccountId, SolidFireUtil.getAccountKey(storagePoolId));

        if (accountDetail == null) {
            throw new CloudRuntimeException("Unable to find SF account for storage " + storagePoolUuid + " for CS account " + csAccountUuid);
        }

        String sfAccountId = accountDetail.getValue();

        return Long.parseLong(sfAccountId);
    }

    @Override
    public long[] getSolidFireVolumeAccessGroupIds(String csClusterUuid, String storagePoolUuid) {
        long storagePoolId = util.getStoragePoolIdForStoragePoolUuid(storagePoolUuid);

        SolidFireUtil.SolidFireConnection sfConnection = SolidFireUtil.getSolidFireConnection(storagePoolId, storagePoolDetailsDao);

        List<SolidFireUtil.SolidFireVag> sfVags = SolidFireUtil.getAllVags(sfConnection);

        long csClusterId = util.getClusterIdForClusterUuid(csClusterUuid);
        List<HostVO> hosts = hostDao.findByClusterId(csClusterId);

        if (hosts == null) {
            return new long[0];
        }

        List<Long> vagIds = new ArrayList<>(hosts.size());

        for (HostVO host : hosts) {
            String iqn = host.getStorageUrl();

            SolidFireUtil.SolidFireVag sfVag = SolidFireUtil.getVolumeAccessGroup(iqn, sfVags);

            if (sfVag != null) {
                if (!vagIds.contains(sfVag.getId())) {
                    vagIds.add(sfVag.getId());
                }
            }
        }

        return vagIds.stream().mapToLong(l -> l).toArray();
    }

    @Override
    public long getSolidFireVolumeSize(String volumeUuid) {
        VolumeVO volume = volumeDao.findByUuid(volumeUuid);

        VolumeDetailVO volumeDetail = volumeDetailsDao.findDetail(volume.getId(), SolidFireUtil.VOLUME_SIZE);

        if (volumeDetail != null && volumeDetail.getValue() != null) {
            return Long.parseLong(volumeDetail.getValue());
        }

        throw new CloudRuntimeException("Unable to determine the size of the SolidFire volume");
    }
}
