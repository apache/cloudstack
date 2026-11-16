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
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class GpuCardDaoImpl extends GenericDaoBase<GpuCardVO, Long> implements GpuCardDao {

    private final SearchBuilder<GpuCardVO> allFieldSearch;

    @Inject
    private GpuDeviceDao gpuDeviceDao;

    public GpuCardDaoImpl() {
        allFieldSearch = createSearchBuilder();
        allFieldSearch.and("name", allFieldSearch.entity().getName(), SearchCriteria.Op.EQ);
        allFieldSearch.and("vendorId", allFieldSearch.entity().getVendorId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("vendorName", allFieldSearch.entity().getVendorName(), SearchCriteria.Op.EQ);
        allFieldSearch.and("deviceId", allFieldSearch.entity().getDeviceId(), SearchCriteria.Op.EQ);
        allFieldSearch.and("deviceName", allFieldSearch.entity().getDeviceName(), SearchCriteria.Op.EQ);
        allFieldSearch.done();
    }

    @Override
    public GpuCardVO findByVendorIdAndDeviceId(String vendorId, String deviceId) {
        SearchCriteria<GpuCardVO> sc = allFieldSearch.create();
        sc.setParameters("vendorId", vendorId);
        sc.setParameters("deviceId", deviceId);
        return findOneBy(sc);
    }

    @Override
    public Pair<List<GpuCardVO>, Integer> searchAndCountGpuCards(Long id, String keyword, String vendorId,
            String vendorName, String deviceId, String deviceName, boolean activeOnly, Long startIndex, Long pageSize
    ) {

        Filter searchFilter = new Filter(GpuCardVO.class, "id", true, startIndex, pageSize);
        SearchBuilder<GpuCardVO> sb = createSearchBuilder();

        if (id != null) {
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        }
        if (keyword != null) {
            sb.op("nameKeyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
            sb.and("deviceNameKeyword", sb.entity().getDeviceName(), SearchCriteria.Op.LIKE);
            sb.and("vendorNameKeyword", sb.entity().getVendorName(), SearchCriteria.Op.LIKE);
            sb.cp();
        }
        if (vendorId != null) {
            sb.and("vendorId", sb.entity().getVendorId(), SearchCriteria.Op.EQ);
        }
        if (vendorName != null) {
            sb.and("vendorName", sb.entity().getVendorName(), SearchCriteria.Op.EQ);
        }
        if (deviceId != null) {
            sb.and("deviceId", sb.entity().getDeviceId(), SearchCriteria.Op.EQ);
        }
        if (deviceName != null) {
            sb.and("deviceName", sb.entity().getDeviceName(), SearchCriteria.Op.EQ);
        }
        if (activeOnly) {
            sb.and("ids", sb.entity().getId(), SearchCriteria.Op.IN);
        }
        sb.done();

        // Build search criteria
        SearchCriteria<GpuCardVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (keyword != null) {
            sc.setParameters("nameKeyword", "%" + keyword + "%");
            sc.setParameters("deviceNameKeyword", "%" + keyword + "%");
            sc.setParameters("vendorNameKeyword", "%" + keyword + "%");
        }
        if (vendorId != null) {
            sc.setParameters("vendorId", vendorId);
        }
        if (vendorName != null) {
            sc.setParameters("vendorName", vendorName);
        }
        if (deviceId != null) {
            sc.setParameters("deviceId", deviceId);
        }
        if (deviceName != null) {
            sc.setParameters("deviceName", deviceName);
        }
        if (activeOnly) {
            List<Long> cardIds = gpuDeviceDao.getDistinctGpuCardIds();
            if (cardIds.isEmpty()) {
                return new Pair<>(List.of(), 0);
            }
            sc.setParameters("ids", cardIds.toArray());
        }

        return searchAndCount(sc, searchFilter);
    }
}
