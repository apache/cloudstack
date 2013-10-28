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
package com.cloud.dc.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ResourceDetail;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;


public abstract class ResourceDetailDaoBase<R extends ResourceDetail> extends GenericDaoBase<R, Long>{
    private SearchBuilder<R> AllFieldsSearch;
    
    public ResourceDetailDaoBase() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("resourceId", AllFieldsSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        AllFieldsSearch.and("name", AllFieldsSearch.entity().getName(), SearchCriteria.Op.EQ);
        AllFieldsSearch.done();
    }

    public R findDetail(long resourceId, String name) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        sc.setParameters("name", name);
        
        return findOneBy(sc);
    }


    public Map<String, String> findDetails(long resourceId) {
        SearchCriteria<R> sc = AllFieldsSearch.create();
        sc.setParameters("resourceId", resourceId);
        
        List<R> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (R result : results) {
            details.put(result.getName(), result.getValue());
        }
        return details;
    }

    public List<R> findDetailsList(long resourceId) {
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

    
    public void removeDetail(long resourceId, String key) {
        if (key != null){
            SearchCriteria<R> sc = AllFieldsSearch.create();
            sc.setParameters("name", key);
            remove(sc);
        }
    }


    public void addDetails(List<R> details) {
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
    

    public void addDetail(R detail) {
        if (detail == null) {
            return;
        }
        R existingDetail = findDetail(detail.getResourceId(), detail.getName());
        if (existingDetail != null) {
            remove(existingDetail.getId());
        }
        persist(detail);
    }

}
