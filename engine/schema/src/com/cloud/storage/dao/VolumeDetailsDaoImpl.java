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
package com.cloud.storage.dao;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.storage.VolumeDetailVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Component
@Local(value=VolumeDetailsDao.class)
public class VolumeDetailsDaoImpl extends GenericDaoBase<VolumeDetailVO, Long> implements VolumeDetailsDao {
    protected final SearchBuilder<VolumeDetailVO> VolumeSearch;
    protected final SearchBuilder<VolumeDetailVO> DetailSearch;
    protected final SearchBuilder<VolumeDetailVO> VolumeDetailSearch;

    public VolumeDetailsDaoImpl() {
        VolumeSearch = createSearchBuilder();
        VolumeSearch.and("volumeId", VolumeSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("volumeId", DetailSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();

        VolumeDetailSearch = createSearchBuilder();
        VolumeDetailSearch.and("volumeId", VolumeDetailSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        VolumeDetailSearch.and("name", VolumeDetailSearch.entity().getName(), SearchCriteria.Op.IN);
        VolumeDetailSearch.done();

    }

    @Override
    public void deleteDetails(long volumeId) {
        SearchCriteria<VolumeDetailVO> sc = VolumeSearch.create();
        sc.setParameters("volumeId", volumeId);

        List<VolumeDetailVO> results = search(sc, null);
        for (VolumeDetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public VolumeDetailVO findDetail(long volumeId, String name) {
        SearchCriteria<VolumeDetailVO> sc = DetailSearch.create();
        sc.setParameters("volumeId", volumeId);
        sc.setParameters("name", name);

        return findOneBy(sc);
    }

    @Override
    public void removeDetails(long volumeId, String key) {

        if(key != null){
            VolumeDetailVO detail = findDetail(volumeId, key);
            if(detail != null){
                remove(detail.getId());
            }
        }else {
           deleteDetails(volumeId);
        }

    }

    @Override
    public List<VolumeDetailVO> findDetails(long volumeId) {
        SearchCriteria<VolumeDetailVO> sc = VolumeSearch.create();
        sc.setParameters("volumeId", volumeId);

        List<VolumeDetailVO> results = search(sc, null);
        return results;
    }

    @Override
    public void persist(long volumeId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<VolumeDetailVO> sc = VolumeSearch.create();
        sc.setParameters("volumeId", volumeId);
        expunge(sc);

        for (Map.Entry<String, String> detail : details.entrySet()) {
            VolumeDetailVO vo = new VolumeDetailVO(volumeId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();
    }

}
