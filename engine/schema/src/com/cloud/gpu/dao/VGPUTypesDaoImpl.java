//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package com.cloud.gpu.dao;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.gpu.HostGpuGroupsVO;
import com.cloud.gpu.VGPUTypesVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = VGPUTypesDao.class)
public class VGPUTypesDaoImpl extends GenericDaoBase<VGPUTypesVO, Long> implements VGPUTypesDao {
    private static final Logger s_logger = Logger.getLogger(VGPUTypesDaoImpl.class);

    private final SearchBuilder<VGPUTypesVO> _searchByGroupId;
    private final SearchBuilder<VGPUTypesVO> _searchByGroupIdVGPUType;
    // private final SearchBuilder<VGPUTypesVO> _searchByHostId;
    // private final SearchBuilder<VGPUTypesVO> _searchForStaleEntries;

    @Inject protected HostGpuGroupsDao _hostGpuGroupsDao;

    public VGPUTypesDaoImpl() {

        _searchByGroupId = createSearchBuilder();
        _searchByGroupId.and("groupId", _searchByGroupId.entity().getGpuGroupId(), SearchCriteria.Op.EQ);
        _searchByGroupId.done();

        _searchByGroupIdVGPUType = createSearchBuilder();
        _searchByGroupIdVGPUType.and("groupId", _searchByGroupIdVGPUType.entity().getGpuGroupId(), SearchCriteria.Op.EQ);
        _searchByGroupIdVGPUType.and("vgpuType", _searchByGroupIdVGPUType.entity().getVgpuType(), SearchCriteria.Op.EQ);
        _searchByGroupIdVGPUType.done();
    }

    @Override
    public List<VGPUTypesVO> listByGroupId(long groupId) {
        SearchCriteria<VGPUTypesVO> sc = _searchByGroupId.create();
        sc.setParameters("groupId", groupId);
        return listBy(sc);
    }

    @Override
    public VGPUTypesVO findByGroupIdVGPUType(long groupId, String vgpuType) {
        SearchCriteria<VGPUTypesVO> sc = _searchByGroupIdVGPUType.create();
        sc.setParameters("groupId", groupId);
        sc.setParameters("vgpuType", vgpuType);
        return findOneBy(sc);
    }

    @Override
    public void persist(long hostId, HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails) {
        Iterator<Entry<String, HashMap<String, VgpuTypesInfo>>> it1 = groupDetails.entrySet().iterator();
        while (it1.hasNext()) {
            Entry<String, HashMap<String, VgpuTypesInfo>> entry = it1.next();
            HostGpuGroupsVO gpuGroup = _hostGpuGroupsDao.findByHostIdGroupName(hostId, entry.getKey());
            HashMap<String, VgpuTypesInfo> values = entry.getValue();
            Iterator<Entry<String, VgpuTypesInfo>> it2 = values.entrySet().iterator();
            while (it2.hasNext()) {
                Entry<String, VgpuTypesInfo> record = it2.next();
                VgpuTypesInfo details = record.getValue();
                VGPUTypesVO vgpuType = null;
                if ((vgpuType = findByGroupIdVGPUType(gpuGroup.getId(), record.getKey())) == null) {
                    persist(new VGPUTypesVO(gpuGroup.getId(), record.getKey(), details.getVideoRam(), details.getMaxHeads(), details.getMaxResolutionX(),
                            details.getMaxResolutionY(), details.getMaxVpuPerGpu(), details.getRemainingCapacity(), details.getMaxCapacity()));
                } else {
                    vgpuType.setRemainingCapacity(details.getRemainingCapacity());
                    vgpuType.setMaxCapacity(details.getMaxCapacity());
                    update(vgpuType.getId(), vgpuType);
                }
            }
        }
    }
}
