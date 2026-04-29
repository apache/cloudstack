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

package com.cloud.network.vpc.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.network.vpc.VpcOfferingDetailsVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class VpcOfferingDetailsDaoImpl extends ResourceDetailsDaoBase<VpcOfferingDetailsVO> implements VpcOfferingDetailsDao {
    private final SearchBuilder<VpcOfferingDetailsVO> ValueSearch;

    public VpcOfferingDetailsDaoImpl() {
        ValueSearch = createSearchBuilder();
        ValueSearch.select(null, SearchCriteria.Func.DISTINCT, ValueSearch.entity().getValue());
        ValueSearch.and("resourceId", ValueSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        ValueSearch.and("name", ValueSearch.entity().getName(), SearchCriteria.Op.EQ);
        ValueSearch.and("display", ValueSearch.entity().isDisplay(), SearchCriteria.Op.EQ);
        ValueSearch.done();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new VpcOfferingDetailsVO(resourceId, key, value, display));
    }

    @Override
    public List<Long> findDomainIds(long resourceId) {
        final List<Long> domainIds = new ArrayList<>();
        for (final VpcOfferingDetailsVO detail: findDetails(resourceId, ApiConstants.DOMAIN_ID)) {
            final Long domainId = Long.valueOf(detail.getValue());
            if (domainId > 0) {
                domainIds.add(domainId);
            }
        }
        return domainIds;
    }

    @Override
    public List<Long> findZoneIds(long resourceId) {
        final List<Long> zoneIds = new ArrayList<>();
        for (final VpcOfferingDetailsVO detail: findDetails(resourceId, ApiConstants.ZONE_ID)) {
            final Long zoneId = Long.valueOf(detail.getValue());
            if (zoneId > 0) {
                zoneIds.add(zoneId);
            }
        }
        return zoneIds;
    }

    @Override
    public String getDetail(long offeringId, String detailName) {
        SearchCriteria<VpcOfferingDetailsVO> sc = ValueSearch.create();
        sc.setParameters("name", detailName);
        sc.setParameters("resourceId", offeringId);
        List<VpcOfferingDetailsVO> results = search(sc, null);
        if (CollectionUtils.isEmpty(results)) {
            return null;
        } else {
            return results.get(0).getValue();
        }
    }
}
