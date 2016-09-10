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

import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;


public class DefaultVolumeNamingPolicy extends AbstractResourceNamingPolicy implements VolumeNamingPolicy {

    private static final Logger s_logger = Logger.getLogger(DefaultVolumeNamingPolicy.class);

    @Override
    public void finalizeIdentifiers(VolumeVO vo) {
        if (!checkUuidSimple(vo.getUuid(), Volume.class)) {
            String oldUuid = vo.getUuid();
            vo.setUuid(generateUuid(vo.getId(), vo.getAccountId(), null));
            s_logger.warn("Invalid uuid for resource: '" + oldUuid + "'; changed to " + vo.getUuid());
        }
    }

    @Override
    public void checkCustomUuid(String uuid) {
        super.checkUuid(uuid, Volume.class);
    }

    @Override
    public String generateUuid(Long resourceId, Long userId, String customUuid) {
        return super.generateUuid(Volume.class, resourceId, userId, customUuid);
    }


    @Override
    public String getDatadiskName() {
        return UUID.randomUUID().toString();
    }


    @Override
    public String getDatadiskName(long instanceId) {
        return "DATA-" + instanceId;
    }


    @Override
    public String getRootName(Long instanceId) {
        return "ROOT-" + instanceId;
    }

}
