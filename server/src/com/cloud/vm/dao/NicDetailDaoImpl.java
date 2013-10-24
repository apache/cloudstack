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
package com.cloud.vm.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.NicDetailVO;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;
import java.util.Map;

@Component
@Local (value={NicDetailDao.class})
public class NicDetailDaoImpl extends GenericDaoBase<NicDetailVO, Long> implements NicDetailDao {
    protected final SearchBuilder<NicDetailVO> NicSearch;
    protected final SearchBuilder<NicDetailVO> DetailSearch;

    public NicDetailDaoImpl() {
        NicSearch = createSearchBuilder();
        NicSearch.and("nicId", NicSearch.entity().getNicId(), SearchCriteria.Op.EQ);
        NicSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("nicId", DetailSearch.entity().getNicId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }

    @Override
    public void deleteDetails(long nicId) {
        SearchCriteria<NicDetailVO> sc = NicSearch.create();
        sc.setParameters("nicId", nicId);

        List<NicDetailVO> results = search(sc, null);
        for (NicDetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public NicDetailVO findDetail(long nicId, String name) {
        SearchCriteria<NicDetailVO> sc = DetailSearch.create();
        sc.setParameters("nicId", nicId);
        sc.setParameters("name", name);

        return findOneBy(sc);
    }

    @Override
    public List<NicDetailVO> findDetails(long nicId) {
        SearchCriteria<NicDetailVO> sc = NicSearch.create();
        sc.setParameters("nicId", nicId);

        List<NicDetailVO> results = search(sc, null);
        /*Map<String, String> details = new HashMap<String, String>(results.size());
        for (NicDetailVO result : results) {
            details.put(result.getName(), result.getValue());
        } */

        return results;
    }

    @Override
    public void persist(long nicId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<NicDetailVO> sc = NicSearch.create();
        sc.setParameters("nicId", nicId);
        expunge(sc);

        for (Map.Entry<String, String> detail : details.entrySet()) {
            NicDetailVO vo = new NicDetailVO(nicId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }

    @Override
    public void removeDetails(long nicId, String key) {
        if(key != null){
            NicDetailVO detail = findDetail(nicId, key);
            if(detail != null){
                remove(detail.getId());
            }
        }else {
            deleteDetails(nicId);
        }
    }
    
}
