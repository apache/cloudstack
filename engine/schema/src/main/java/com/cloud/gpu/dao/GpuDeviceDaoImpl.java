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
package com.cloud.gpu.dao;

import com.cloud.gpu.GpuCardVO;
import com.cloud.gpu.GpuDeviceVO;
import com.cloud.gpu.VgpuProfileVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.gpu.GpuDevice;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GpuDeviceDaoImpl extends GenericDaoBase<GpuDeviceVO, Long> implements GpuDeviceDao {

    private static final String IDS = "ids";
    private static final String HOST_ID = "hostId";
    private static final String VM_ID = "vmId";
    private static final String BUS_ADDRESS = "busAddress";
    private static final String CARD_ID = "cardId";
    private static final String VGPU_PROFILE_ID = "vgpuProfileId";
    private static final String PARENT_GPU_DEVICE_ID = "parentGpuDeviceId";
    private static final String STATE = "state";
    private static final String MANAGED_STATE = "managedState";
    private static final String TYPE = "type";
    private final SearchBuilder<GpuDeviceVO> allFieldSearch;
    private SearchBuilder<GpuDeviceVO> devicesForAllocationSearch;
    @Inject
    private GpuCardDao gpuCardDao;
    @Inject
    private VgpuProfileDao vgpuProfileDao;

    public GpuDeviceDaoImpl() {
        allFieldSearch = createSearchBuilder();
        allFieldSearch.and(IDS, allFieldSearch.entity().getId(), SearchCriteria.Op.IN);
        allFieldSearch.and(HOST_ID, allFieldSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        allFieldSearch.and(CARD_ID, allFieldSearch.entity().getCardId(), SearchCriteria.Op.EQ);
        allFieldSearch.and(BUS_ADDRESS, allFieldSearch.entity().getBusAddress(), SearchCriteria.Op.EQ);
        allFieldSearch.and(STATE, allFieldSearch.entity().getState(), SearchCriteria.Op.EQ);
        allFieldSearch.and(VGPU_PROFILE_ID, allFieldSearch.entity().getVgpuProfileId(), SearchCriteria.Op.EQ);
        allFieldSearch.and(PARENT_GPU_DEVICE_ID, allFieldSearch.entity().getParentGpuDeviceId(), SearchCriteria.Op.EQ);
        allFieldSearch.and(VM_ID, allFieldSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        allFieldSearch.done();

        devicesForAllocationSearch = createSearchBuilder();
        devicesForAllocationSearch.and(HOST_ID, devicesForAllocationSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        devicesForAllocationSearch.and(VGPU_PROFILE_ID, devicesForAllocationSearch.entity().getVgpuProfileId(), SearchCriteria.Op.IN);
        devicesForAllocationSearch.and(STATE, devicesForAllocationSearch.entity().getState(), SearchCriteria.Op.EQ);
        devicesForAllocationSearch.and(MANAGED_STATE, devicesForAllocationSearch.entity().getManagedState(), SearchCriteria.Op.EQ);
        devicesForAllocationSearch.and(TYPE, devicesForAllocationSearch.entity().getType(), SearchCriteria.Op.NEQ);
        devicesForAllocationSearch.done();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return super.configure(name, params);
    }

    @Override
    public List<GpuDeviceVO> listByIds(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        SearchCriteria<GpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters(IDS, ids.toArray());
        return listBy(sc);
    }

    @Override
    public GpuDeviceVO findByHostIdAndBusAddress(long hostId, String busAddress) {
        SearchCriteria<GpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters(HOST_ID, hostId);
        sc.setParameters(BUS_ADDRESS, busAddress);
        return findOneBy(sc);
    }

    @Override
    public List<GpuDeviceVO> listByHostId(long hostId) {
        SearchCriteria<GpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters(HOST_ID, hostId);
        return listBy(sc);
    }

    @Override
    public List<GpuDeviceVO> listByVmId(long vmId) {
        SearchCriteria<GpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters(VM_ID, vmId);
        return listBy(sc);
    }

    @Override
    public boolean isVgpuProfileInUse(long vgpuProfileId) {
        SearchCriteria<GpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters(VGPU_PROFILE_ID, vgpuProfileId);
        return getCount(sc) > 0;
    }

    @Override
    public boolean isGpuCardInUse(long cardId) {
        SearchCriteria<GpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters(CARD_ID, cardId);
        return getCount(sc) > 0;
    }

    @Override
    public List<GpuDeviceVO> listByHostAndVm(Long hostId, long vmId) {
        SearchCriteria<GpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters(HOST_ID, hostId);
        sc.setParameters(VM_ID, vmId);
        return search(sc, null);
    }

    @Override
    public List<GpuDeviceVO> listDevicesForAllocation(Long hostId, Long vgpuProfileId) {
        SearchCriteria<GpuDeviceVO> sc = devicesForAllocationSearch.create();
        sc.setParameters(HOST_ID, hostId);
        sc.setParameters(VGPU_PROFILE_ID, vgpuProfileId);
        sc.setParameters(STATE, GpuDevice.State.Free);
        sc.setParameters(MANAGED_STATE, GpuDevice.ManagedState.Managed);
        sc.setParameters(TYPE, GpuDevice.DeviceType.VGPUOnly);
        return search(sc, null);
    }

    @Override
    public Pair<List<GpuDeviceVO>, Integer> searchAndCountGpuDevices(Long id, String keyword, Long hostId, Long vmId,
            Long gpuCardId, Long vgpuProfileId, Long startIndex, Long pageSize) {
        Filter searchFilter = new Filter(GpuDeviceVO.class, "id", true, startIndex, pageSize);
        SearchBuilder<GpuDeviceVO> sb = createSearchBuilder();

        if (id != null) {
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        }
        if (hostId != null) {
            sb.and("hostId", sb.entity().getHostId(), SearchCriteria.Op.EQ);
        }
        if (vmId != null) {
            sb.and("vmId", sb.entity().getVmId(), SearchCriteria.Op.EQ);
        }
        if (gpuCardId != null) {
            sb.and("cardId", sb.entity().getCardId(), SearchCriteria.Op.EQ);
        }
        if (vgpuProfileId != null) {
            sb.and("vgpuProfileId", sb.entity().getVgpuProfileId(), SearchCriteria.Op.EQ);
        }
        if (keyword != null) {
            SearchBuilder<GpuCardVO> cardSb = gpuCardDao.createSearchBuilder();
            SearchBuilder<VgpuProfileVO> profileSb = vgpuProfileDao.createSearchBuilder();
            sb.join("cardJoin", cardSb, sb.entity().getCardId(), cardSb.entity().getId(), JoinBuilder.JoinType.INNER);
            sb.join("profileJoin", profileSb, sb.entity().getCardId(), profileSb.entity().getId(),
                    JoinBuilder.JoinType.INNER);

            sb.op("cardNameKeyword", cardSb.entity().getName(), SearchCriteria.Op.LIKE);
            sb.or("cardNameKeyword", cardSb.entity().getVendorName(), SearchCriteria.Op.LIKE);
            sb.or("cardNameKeyword", cardSb.entity().getDeviceName(), SearchCriteria.Op.LIKE);

            sb.op("profileNameKeyword", profileSb.entity().getName(), SearchCriteria.Op.LIKE);
            sb.op("profileDescriptionKeyword", profileSb.entity().getDescription(), SearchCriteria.Op.LIKE);
            sb.cp();
        }

        sb.done();

        // Build search criteria
        SearchCriteria<GpuDeviceVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (hostId != null) {
            sc.setParameters("hostId", hostId);
        }
        if (vmId != null) {
            sc.setParameters("vmId", vmId);
        }
        if (gpuCardId != null) {
            sc.setParameters("cardId", gpuCardId);
        }
        if (vgpuProfileId != null) {
            sc.setParameters("vgpuProfileId", vgpuProfileId);
        }

        if (keyword != null) {
            sc.setJoinParameters("cardJoin", "cardNameKeyword", "%" + keyword + "%");
            sc.setJoinParameters("cardJoin", "cardNameKeyword", "%" + keyword + "%");
            sc.setJoinParameters("cardJoin", "cardNameKeyword", "%" + keyword + "%");
            sc.setJoinParameters("profileJoin", "profileNameKeyword", "%" + keyword + "%");
            sc.setJoinParameters("profileJoin", "profileDescriptionKeyword", "%" + keyword + "%");
        }

        return searchAndCount(sc, searchFilter);
    }

    @Override
    public List<Long> getDistinctGpuCardIds() {
        SearchBuilder<GpuDeviceVO> sb = createSearchBuilder();
        sb.select(null, SearchCriteria.Func.DISTINCT, sb.entity().getCardId());
        sb.done();
        SearchCriteria<GpuDeviceVO> sc = sb.create();

        List<GpuDeviceVO> gpuDevices = listBy(sc);
        if (CollectionUtils.isEmpty(gpuDevices)) {
            return Collections.emptyList();
        }

        return gpuDevices.stream()
                .map(GpuDeviceVO::getCardId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getDistinctVgpuProfileIds() {
        SearchBuilder<GpuDeviceVO> sb = createSearchBuilder();
        sb.select(null, SearchCriteria.Func.DISTINCT, sb.entity().getVgpuProfileId());
        sb.done();
        SearchCriteria<GpuDeviceVO> sc = sb.create();

        List<GpuDeviceVO> gpuDevices = listBy(sc);
        if (CollectionUtils.isEmpty(gpuDevices)) {
            return Collections.emptyList();
        }

        return gpuDevices.stream()
                .map(GpuDeviceVO::getVgpuProfileId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<GpuDeviceVO> listByParentGpuDeviceId(Long parentGpuDeviceId) {
        SearchCriteria<GpuDeviceVO> sc = allFieldSearch.create();
        sc.setParameters(PARENT_GPU_DEVICE_ID, parentGpuDeviceId);
        return listBy(sc);
    }
}
