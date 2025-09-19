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

import com.cloud.gpu.VgpuProfileVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class VgpuProfileDaoImpl extends GenericDaoBase<VgpuProfileVO, Long> implements VgpuProfileDao {

    private final SearchBuilder<VgpuProfileVO> allFieldSearch;

    @Inject
    private GpuDeviceDao gpuDeviceDao;

    public VgpuProfileDaoImpl() {
        allFieldSearch = createSearchBuilder();
        allFieldSearch.and("name", allFieldSearch.entity().getName(), SearchCriteria.Op.EQ);
        allFieldSearch.and("cardId", allFieldSearch.entity().getCardId(), SearchCriteria.Op.IN);
        allFieldSearch.done();
    }

    @Override
    public VgpuProfileVO findByNameAndCardId(String name, long cardId) {
        SearchCriteria<VgpuProfileVO> sc = allFieldSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("cardId", cardId);
        return findOneBy(sc);
    }

    @Override
    public int removeByCardId(long cardId) {
        SearchCriteria<VgpuProfileVO> sc = allFieldSearch.create();
        sc.setParameters("cardId", cardId);
        return remove(sc);
    }

    @Override
    public Pair<List<VgpuProfileVO>, Integer> searchAndCountVgpuProfiles(Long id, String name, String keyword,
            Long gpuCardId, boolean activeOnly, Long startIndex, Long pageSize) {
        Filter searchFilter = new Filter(VgpuProfileVO.class, "id", true, startIndex, pageSize);
        SearchBuilder<VgpuProfileVO> sb = createSearchBuilder();

        if (id != null) {
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        }
        if (name != null) {
            sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        }
        if (keyword != null) {
            sb.and("keywordName", sb.entity().getName(), SearchCriteria.Op.LIKE);
            sb.and("keywordDescription", sb.entity().getDescription(), SearchCriteria.Op.LIKE);
        }
        if (gpuCardId != null) {
            sb.and("cardId", sb.entity().getCardId(), SearchCriteria.Op.EQ);
        }
        if (activeOnly) {
            sb.and("ids", sb.entity().getId(), SearchCriteria.Op.IN);
        }
        sb.done();

        // Build search criteria
        SearchCriteria<VgpuProfileVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (name != null) {
            sc.setParameters("name", name);
        }
        if (keyword != null) {
            sc.setParameters("keywordName", "%" + keyword + "%");
            sc.setParameters("keywordDescription", "%" + keyword + "%");
        }
        if (gpuCardId != null) {
            sc.setParameters("cardId", gpuCardId);
        }

        if (activeOnly) {
            List<Long> vgpuProfileIds = gpuDeviceDao.getDistinctVgpuProfileIds();
            if (vgpuProfileIds.isEmpty()) {
                return new Pair<>(List.of(), 0);
            }
            sc.setParameters("ids", vgpuProfileIds.toArray());
        }

        return searchAndCount(sc, searchFilter);
    }
}
