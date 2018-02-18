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
package org.apache.cloudstack.engine.datacenter.entity.api.db.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.datacenter.entity.api.db.DcDetailVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component(value = "EngineDcDetailsDao")
public class DcDetailsDaoImpl extends GenericDaoBase<DcDetailVO, Long> implements DcDetailsDao {
    protected final SearchBuilder<DcDetailVO> DcSearch;
    protected final SearchBuilder<DcDetailVO> DetailSearch;

    protected DcDetailsDaoImpl() {
        DcSearch = createSearchBuilder();
        DcSearch.and("dcId", DcSearch.entity().getDcId(), SearchCriteria.Op.EQ);
        DcSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("dcId", DetailSearch.entity().getDcId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }

    @Override
    public DcDetailVO findDetail(long dcId, String name) {
        SearchCriteria<DcDetailVO> sc = DetailSearch.create();
        sc.setParameters("dcId", dcId);
        sc.setParameters("name", name);

        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public Map<String, String> findDetails(long dcId) {
        SearchCriteria<DcDetailVO> sc = DcSearch.create();
        sc.setParameters("dcId", dcId);

        List<DcDetailVO> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (DcDetailVO result : results) {
            details.put(result.getName(), result.getValue());
        }
        return details;
    }

    @Override
    public void deleteDetails(long dcId) {
        SearchCriteria sc = DcSearch.create();
        sc.setParameters("dcId", dcId);

        List<DcDetailVO> results = search(sc, null);
        for (DcDetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public void persist(long dcId, Map<String, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<DcDetailVO> sc = DcSearch.create();
        sc.setParameters("dcId", dcId);
        expunge(sc);

        for (Map.Entry<String, String> detail : details.entrySet()) {
            DcDetailVO vo = new DcDetailVO(dcId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }
}
