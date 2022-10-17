/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.storage.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.storage.VolumeGroupVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;


public class VolumeGroupDaoImpl extends GenericDaoBase<VolumeGroupVO, Long> implements VolumeGroupDao {
    protected final SearchBuilder<VolumeGroupVO> allFieldsSearch;
    private static final String VOLUME_ID = "volumeId";
    private static final String VM_ID = "vmId";
    private static final String GROUP_NUMBER = "groupNumber";

    public VolumeGroupDaoImpl(){
        allFieldsSearch = createSearchBuilder();
        allFieldsSearch.and(VM_ID, allFieldsSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(VOLUME_ID, allFieldsSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        allFieldsSearch.and(GROUP_NUMBER, allFieldsSearch.entity().getGroupNumber(), SearchCriteria.Op.EQ);
        allFieldsSearch.done();
    }

    @Override
    public void addVolumeToGroup(long vmId, long volumeId, long deviceId, int groupNumber) {
        if (groupNumber != -1) {
            if (deviceId == 0L) {
                groupNumber = 0;
            }
            VolumeGroupVO group = new VolumeGroupVO();
            group.setVmId(vmId);
            group.setVolumeId(volumeId);
            group.setGroupNumber(groupNumber);
            persist(group);
        }
    }

    @Override
    public void deleteVolumeFromGroup(long volumeId) {
        SearchCriteria<VolumeGroupVO> sc = allFieldsSearch.create();
        sc.setParameters(VOLUME_ID, volumeId);
        expunge(sc);
    }

    @Override
    public VolumeGroupVO findByVmAndVolume(long vmId, long volumeId){
        SearchCriteria<VolumeGroupVO> sc = allFieldsSearch.create();
        sc.setParameters(VM_ID, vmId);
        sc.setParameters(VOLUME_ID, volumeId);
        return findOneBy(sc);
    }
}