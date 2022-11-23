// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.dao;


import org.springframework.stereotype.Component;

import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.network.Network;

@Component
@DB
public class NetworkDetailsDaoImpl extends ResourceDetailsDaoBase<NetworkDetailVO> implements NetworkDetailsDao, ScopedConfigStorage {
    protected final SearchBuilder<NetworkDetailVO> networkSearch;

    protected NetworkDetailsDaoImpl() {
        networkSearch = createSearchBuilder();
        networkSearch.and("networkId", networkSearch.entity().getResourceId(), Op.EQ);
        networkSearch.done();
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new NetworkDetailVO(resourceId, key, value, display));
    }

    @Override
    public boolean isNetworkUsageHidden(long networkId) {
        NetworkDetailVO networkDetail = findDetail(networkId, Network.hideIpAddressUsage);
        return networkDetail != null && "true".equals(networkDetail.getValue());
    }

    @Override
    public Map<String, String> findDetails(long networkId) {
        QueryBuilder<NetworkDetailVO> sc = QueryBuilder.create(NetworkDetailVO.class);
        sc.and(sc.entity().getResourceId(), Op.EQ, networkId);
        List<NetworkDetailVO> results = sc.list();
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (NetworkDetailVO r : results) {
            details.put(r.getName(), r.getValue());
        }
        return details;
    }

    @Override
    public void persist(long networkId, Map<String, String> details) {
        try(TransactionLegacy txn = TransactionLegacy.currentTxn()) {
          txn.start();
          SearchCriteria<NetworkDetailVO> sc = networkSearch.create();
          sc.setParameters("networkId", networkId);
          expunge(sc);
          for (Map.Entry<String, String> detail : details.entrySet()) {
            NetworkDetailVO vo = new NetworkDetailVO(networkId, detail.getKey(), detail.getValue(), true);
            persist(vo);
          }
          txn.commit();
        }
    }

    @Override
    public NetworkDetailVO findDetail(long networkId, String name) {
        QueryBuilder<NetworkDetailVO> sc = QueryBuilder.create(NetworkDetailVO.class);
        sc.and(sc.entity().getResourceId(), Op.EQ, networkId);
        sc.and(sc.entity().getName(), Op.EQ, name);
        return sc.find();
    }

    @Override
    public List<NetworkDetailVO> listDetailsByName(String name) {
        QueryBuilder<NetworkDetailVO> sc = QueryBuilder.create(NetworkDetailVO.class);
        sc.and(sc.entity().getName(), Op.EQ, name);
        return sc.list();
    }

    @Override
    public void deleteDetails(long networkId) {
        SearchCriteria<NetworkDetailVO> sc = networkSearch.create();
        sc.setParameters("networkId", networkId);
        List<NetworkDetailVO> results = search(sc, null);
        for (NetworkDetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public void update(long networkId, Map<String, String> details) {
        Map<String, String> oldDetails = findDetails(networkId);
        oldDetails.putAll(details);
        persist(networkId, oldDetails);
    }

    @Override
    public Scope getScope() {
        return ConfigKey.Scope.Network;
    }

    @Override
    public String getConfigValue(long id, ConfigKey<?> key) {
        NetworkDetailVO vo = findDetail(id, key.key());
        return vo == null ? null : vo.getValue();
    }
}
