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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.gpu.GpuOfferingDetailVO;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class GpuOfferingDetailsDaoImpl extends ResourceDetailsDaoBase<GpuOfferingDetailVO> implements GpuOfferingDetailsDao {

    private SearchBuilder<GpuOfferingDetailVO> offeringDetailsSearch;
    private SearchBuilder<GpuOfferingDetailVO> detailSearch;

    public GpuOfferingDetailsDaoImpl() {
        offeringDetailsSearch = createSearchBuilder();
        offeringDetailsSearch.and("gpuOfferingId", offeringDetailsSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        offeringDetailsSearch.done();

        detailSearch = createSearchBuilder();
        detailSearch.and("gpuOfferingId", detailSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        detailSearch.and("name", detailSearch.entity().getName(), SearchCriteria.Op.EQ);
        detailSearch.done();
    }

    @Override
    public void addDetail(long gpuOfferingId, String name, String value, boolean display) {
        GpuOfferingDetailVO detail = findDetail(gpuOfferingId, name);
        if (detail == null) {
            detail = new GpuOfferingDetailVO(gpuOfferingId, name, value, display);
            persist(detail);
        } else {
            detail.setValue(value);
            detail.setDisplay(display);
            update(detail.getId(), detail);
        }
    }


    @Override
    public Map<String, String> getDetailsMap(long gpuOfferingId) {
        Map<String, String> details = new HashMap<>();
        List<GpuOfferingDetailVO> detailList = listDetails(gpuOfferingId);
        for (GpuOfferingDetailVO detail : detailList) {
            details.put(detail.getName(), detail.getValue());
        }
        return details;
    }

    @Override
    public boolean update(long gpuOfferingId, String name, String value, boolean display) {
        GpuOfferingDetailVO detail = findDetail(gpuOfferingId, name);
        if (detail != null) {
            detail.setValue(value);
            detail.setDisplay(display);
            return update(detail.getId(), detail);
        }
        return false;
    }

    @Override
    public void addVgpuProfileIds(long gpuOfferingId, String vgpuProfileIds, boolean display) {
        // This method specifically adds vGPU profile IDs to a GPU offering
        addDetail(gpuOfferingId, GpuOfferingDetailVO.VgpuProfileId, vgpuProfileIds, display);
    }

    @Override
    public String getVgpuProfileIds(long gpuOfferingId) {
        GpuOfferingDetailVO detail = findDetail(gpuOfferingId, GpuOfferingDetailVO.VgpuProfileId);
        return detail != null ? detail.getValue() : null;
    }
}
