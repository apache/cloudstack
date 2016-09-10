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
package com.cloud.naming;

import java.util.Date;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.log4j.Logger;

import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.utils.DateUtil;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmDao;

public class DefaultSnapshotNamingPolicy extends AbstractResourceNamingPolicy implements SnapshotNamingPolicy {

    @Inject
    UserVmDao _vmDao;
    @Inject
    VolumeDataFactory volFactory;

    private static final Logger s_logger = Logger.getLogger(DefaultSnapshotNamingPolicy.class);

    @Override
    public void finalizeIdentifiers(SnapshotVO vo) {
        if (!checkUuidSimple(vo.getUuid(), Snapshot.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }


    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, Snapshot.class);
    }

    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(Snapshot.class, resourceId, userId, customUuid);
    }

    @Override
    public String getSnapshotName(Long volumeId) {
        VolumeInfo volume = volFactory.getVolume(volumeId);
        VMInstanceVO vmInstance = _vmDao.findById(volume.getInstanceId());
        String vmDisplayName = "detached";
        if (vmInstance != null) {
            vmDisplayName = vmInstance.getHostName();
        }
        String timeString = DateUtil.getDateDisplayString(DateUtil.GMT_TIMEZONE, new Date(), DateUtil.YYYYMMDD_FORMAT);
        String snapshotName = vmDisplayName + "_" + volume.getName() + "_" + timeString;
        return snapshotName;
    }


}
