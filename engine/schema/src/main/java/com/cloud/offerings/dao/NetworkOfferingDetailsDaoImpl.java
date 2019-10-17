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
package com.cloud.offerings.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Detail;
import com.cloud.offerings.NetworkOfferingDetailsVO;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

public class NetworkOfferingDetailsDaoImpl extends ResourceDetailsDaoBase<NetworkOfferingDetailsVO> implements NetworkOfferingDetailsDao {
    protected final SearchBuilder<NetworkOfferingDetailsVO> DetailSearch;
    private final GenericSearchBuilder<NetworkOfferingDetailsVO, String> ValueSearch;

    public NetworkOfferingDetailsDaoImpl() {

        DetailSearch = createSearchBuilder();
        DetailSearch.and("resourceId", DetailSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.and("value", DetailSearch.entity().getValue(), SearchCriteria.Op.EQ);
        DetailSearch.and("display", DetailSearch.entity().isDisplay(), SearchCriteria.Op.EQ);
        DetailSearch.done();

        ValueSearch = createSearchBuilder(String.class);
        ValueSearch.select(null, Func.DISTINCT, ValueSearch.entity().getValue());
        ValueSearch.and("resourceId", ValueSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        ValueSearch.and("name", ValueSearch.entity().getName(), Op.EQ);
        ValueSearch.and("display", ValueSearch.entity().isDisplay(), SearchCriteria.Op.EQ);
        ValueSearch.done();
    }

    @Override
    public Map<NetworkOffering.Detail, String> getNtwkOffDetails(long offeringId) {
        SearchCriteria<NetworkOfferingDetailsVO> sc = DetailSearch.create();
        sc.setParameters("resourceId", offeringId);
        sc.setParameters("display", true);

        List<NetworkOfferingDetailsVO> results = search(sc, null);
        Map<NetworkOffering.Detail, String> details = new HashMap<NetworkOffering.Detail, String>(results.size());
        for (NetworkOfferingDetailsVO result : results) {
            details.put(result.getDetailName(), result.getValue());
        }

        return details;
    }

    @Override
    public String getDetail(long offeringId, Detail detailName) {
        SearchCriteria<String> sc = ValueSearch.create();
        sc.setParameters("name", detailName);
        sc.setParameters("resourceId", offeringId);
        List<String> results = customSearch(sc, null);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        persist(new NetworkOfferingDetailsVO(resourceId, Detail.valueOf(key), value, display));
    }

    @Override
    public List<Long> findDomainIds(long resourceId) {
        final List<Long> domainIds = new ArrayList<>();
        for (final NetworkOfferingDetailsVO detail: findDetails(resourceId, ApiConstants.DOMAIN_ID)) {
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
        for (final NetworkOfferingDetailsVO detail: findDetails(resourceId, ApiConstants.ZONE_ID)) {
            final Long zoneId = Long.valueOf(detail.getValue());
            if (zoneId > 0) {
                zoneIds.add(zoneId);
            }
        }
        return zoneIds;
    }
}
