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
package org.apache.cloudstack.resourcedetail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ResourceDetail;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;

public abstract class ResourceDetailsDaoBase<R extends ResourceDetail> extends GenericDaoBase<R, Long> implements ResourceDetailsDao<R> {
    private SearchBuilder<R> AllFieldsSearch;

    public ResourceDetailsDaoBase() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("resourceId", AllFieldsSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("value", AllFieldsSearch.entity().getValue(), SearchCriteria.Op.EQ);
        // FIXME SnapshotDetailsVO doesn't have a display field
        if (_allAttributes.containsKey("display")) {
            AllFieldsSearch.and("display", AllFieldsSearch.entity().isDisplay(), SearchCriteria.Op.EQ);
        }
        AllFieldsSearch.done();
    }

    public R findDetail(long resourceId, String name) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("name", name);

        return findOneBy(sc);
    }

    public List<R> findDetails(long resourceId, String key) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("name", key);
        return listBy(sc);
    }

    public List<R> findDetails(String name, String value, Boolean display) {
        SearchCriteria<R> sc = AllFieldsSearch.create();

        if(display != null){
            sc.setParameters("display", display);
        }

        if(name != null){
            sc.setParameters("name", name);
        }

        if(value != null){
            sc.setParameters("value", value);
        }

        List<R> results = search(sc, null);
        return results;
    }

    public Map<String, String> listDetailsKeyPairs(long resourceId) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);

        List<R> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (R result : results) {
            details.put(result.getName(), result.getValue());
        }
        return details;
    }

    public Map<String, Boolean> listDetailsVisibility(long resourceId) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);

        List<R> results = search(sc, null);
        Map<String, Boolean> details = new HashMap<>(results.size());
        for (R result : results) {
            details.put(result.getName(), result.isDisplay());
        }
        return details;
    }

    public List<R> listDetails(long resourceId) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);

        List<R> results = search(sc, null);
        return results;
    }

    public void removeDetails(long resourceId) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        remove(sc);
    }

    public void removeDetail(long resourceId, String key, String value) {
        if (key != null) {
            SearchCriteria<R> sc = AllFieldsSearch.create();
            sc.setParameters("resourceId", resourceId);
            sc.setParameters("name", key);
            sc.setParameters("value", value);
            remove(sc);
        }
    }

    public void removeDetail(long resourceId, String key) {
        if (key != null) {
            SearchCriteria<R> sc = AllFieldsSearch.create();
            sc.setParameters("resourceId", resourceId);
            sc.setParameters("name", key);
            remove(sc);
        }
    }

    public void saveDetails(List<R> details) {
        if (details.isEmpty()) {
            return;
        }
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", details.get(0).getResourceId());
        expunge(sc);

        for (R detail : details) {
            persist(detail);
        }

        txn.commit();
    }

    protected void addDetail(R detail) {
        if (detail == null) {
            return;
        }
        R existingDetail = findDetail(detail.getResourceId(), detail.getName());
        if (existingDetail != null) {
            remove(existingDetail.getId());
        }
        persist(detail);
    }

    public Map<String, String> listDetailsKeyPairs(long resourceId, boolean forDisplay) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("display", forDisplay);

        List<R> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (R result : results) {
            details.put(result.getName(), result.getValue());
        }
        return details;
    }

    public List<R> listDetails(long resourceId, boolean forDisplay) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("display", forDisplay);

        List<R> results = search(sc, null);
        return results;
    }

    @Override
    public List<Long> findResourceIdsByNameAndValueIn(String name, Object[] values) {
        GenericSearchBuilder<R, Long> sb = createSearchBuilder(Long.class);
        sb.selectFields(sb.entity().getResourceId());
        sb.and("name", sb.entity().getName(), Op.EQ);
        sb.and().op("value", sb.entity().getValue(), Op.IN);
        sb.or("valueNull", sb.entity().getValue(), Op.NULL);
        sb.cp();
        sb.done();

        SearchCriteria<Long> sc = sb.create();
        sc.setParameters("name", name);
        sc.setParameters("value", values);

        return customSearch(sc, null);
    }

    @Override
    public long batchExpungeForResources(final List<Long> ids, final Long batchSize) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        SearchBuilder<R> sb = createSearchBuilder();
        sb.and("ids", sb.entity().getResourceId(), Op.IN);
        sb.done();
        SearchCriteria<R> sc = sb.create();
        sc.setParameters("ids", ids.toArray());
        return batchExpunge(sc, batchSize);
    }
}
