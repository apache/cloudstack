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

import com.cloud.gpu.GpuOfferingDetailVO;
import com.cloud.gpu.GpuOfferingVO;
import com.cloud.gpu.VgpuProfileVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.gpu.GpuOffering;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static org.apache.cloudstack.query.QueryService.SortKeyAscending;

@Component
public class GpuOfferingDaoImpl extends GenericDaoBase<GpuOfferingVO, Long> implements GpuOfferingDao {

    @Inject
    protected GpuOfferingDetailsDao gpuOfferingDetailsDao;
    private SearchBuilder<GpuOfferingVO> allFieldSearch;
    @Inject
    private VgpuProfileDao vgpuProfileDao;
    @Inject
    private GpuCardDao gpuCardDao;

    public GpuOfferingDaoImpl() {
        allFieldSearch = createSearchBuilder();
        allFieldSearch.and("name", allFieldSearch.entity().getName(), SearchCriteria.Op.EQ);
        allFieldSearch.and("description", allFieldSearch.entity().getDescription(),
                SearchCriteria.Op.EQ);
        allFieldSearch.and("state", allFieldSearch.entity().getState(), SearchCriteria.Op.EQ);
        allFieldSearch.done();
    }

    @Override
    public Pair<List<GpuOfferingVO>, Integer> searchAndCountGpuOfferings(Long id, String keyword,
                                                                         String name,
                                                                         GpuOffering.State state, Long startIndex,
                                                                         Long pageSize) {
        SearchBuilder<GpuOfferingVO> sb = createSearchBuilder();

        Filter searchFilter = new Filter(GpuOfferingVO.class, "sortKey", SortKeyAscending.value(), startIndex, pageSize);
        searchFilter.addOrderBy(GpuOfferingVO.class, "id", true);

        if (id != null) {
            sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        }
        if (keyword != null) {
            sb.op("keywordName", sb.entity().getName(), SearchCriteria.Op.LIKE);
            sb.or("keywordDescription", sb.entity().getDescription(), SearchCriteria.Op.LIKE);
            sb.cp();
        }
        if (name != null) {
            sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        }
        if (state != null) {
            sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        }
        sb.done();

        SearchCriteria<GpuOfferingVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id", id);
        }
        if (keyword != null) {
            sc.setJoinParameters("keywordName", "keywordName", "%" + keyword + "%");
            sc.setJoinParameters("keywordDescription", "keywordDescription", "%" + keyword + "%");
        }
        if (name != null) {
            sc.setParameters("name", name);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        return searchAndCount(sc, searchFilter);


    }

    @Override
    public GpuOfferingVO findByName(String name) {
        SearchCriteria<GpuOfferingVO> sc = allFieldSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public void loadVgpuProfiles(GpuOfferingVO gpuOffering) {
        List<GpuOfferingDetailVO> details = gpuOfferingDetailsDao.findDetails(
                gpuOffering.getId(), GpuOfferingDetailVO.VgpuProfileId);

        for (GpuOfferingDetailVO detail : details) {
            if (detail.getName().equals(GpuOfferingDetailVO.VgpuProfileId)) {
                VgpuProfileVO profile = vgpuProfileDao.findById(Long.parseLong(detail.getValue()));
                gpuOffering.addVgpuProfile(profile);
            }
        }
    }
}
