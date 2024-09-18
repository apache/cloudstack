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
package org.apache.cloudstack.network.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;

import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import org.apache.cloudstack.network.BgpPeer;
import org.apache.cloudstack.network.BgpPeerDetailsVO;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.apache.commons.lang3.EnumUtils;

public class BgpPeerDetailsDaoImpl extends ResourceDetailsDaoBase<BgpPeerDetailsVO> implements BgpPeerDetailsDao {
    protected final SearchBuilder<BgpPeerDetailsVO> DetailSearch;
    private final GenericSearchBuilder<BgpPeerDetailsVO, String> ValueSearch;

    public BgpPeerDetailsDaoImpl() {

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
    public Map<BgpPeer.Detail, String> getBgpPeerDetails(long bgpPeerId) {
        SearchCriteria<BgpPeerDetailsVO> sc = DetailSearch.create();
        sc.setParameters("resourceId", bgpPeerId);
        sc.setParameters("display", true);

        List<BgpPeerDetailsVO> results = search(sc, null);
        if (results.size() == 0) {
            return null;
        }
        Map<BgpPeer.Detail, String> details = new HashMap<>(results.size());
        for (BgpPeerDetailsVO result : results) {
            details.put(result.getDetailName(), result.getValue());
        }

        return details;
    }

    @Override
    public String getDetail(long bgpPeerId, BgpPeer.Detail detailName) {
        SearchCriteria<String> sc = ValueSearch.create();
        sc.setParameters("name", detailName);
        sc.setParameters("resourceId", bgpPeerId);
        List<String> results = customSearch(sc, null);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        persist(new BgpPeerDetailsVO(resourceId, EnumUtils.getEnumIgnoreCase(BgpPeer.Detail.class, key), value, display));
    }

    @Override
    public List<Long> findDomainIds(long resourceId) {
        final List<Long> domainIds = new ArrayList<>();
        for (final BgpPeerDetailsVO detail: findDetails(resourceId, ApiConstants.DOMAIN_ID)) {
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
        for (final BgpPeerDetailsVO detail: findDetails(resourceId, ApiConstants.ZONE_ID)) {
            final Long zoneId = Long.valueOf(detail.getValue());
            if (zoneId > 0) {
                zoneIds.add(zoneId);
            }
        }
        return zoneIds;
    }

    @Override
    public int removeByBgpPeerId(long bgpPeerId) {
        SearchCriteria<BgpPeerDetailsVO> sc = DetailSearch.create();
        sc.setParameters("resourceId", bgpPeerId);
        return remove(sc);
    }
}
