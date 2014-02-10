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
package com.cloud.host.dao;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.host.HostTagVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = HostTagsDao.class)
public class HostTagsDaoImpl extends GenericDaoBase<HostTagVO, Long> implements HostTagsDao {
    protected final SearchBuilder<HostTagVO> HostSearch;

    public HostTagsDaoImpl() {
        HostSearch = createSearchBuilder();
        HostSearch.and("hostId", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();
    }

    @Override
    public List<String> gethostTags(long hostId) {
        SearchCriteria<HostTagVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);

        List<HostTagVO> results = search(sc, null);
        List<String> hostTags = new ArrayList<String>(results.size());
        for (HostTagVO result : results) {
            hostTags.add(result.getTag());
        }

        return hostTags;
    }

    @Override
    public void persist(long hostId, List<String> hostTags) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        txn.start();
        SearchCriteria<HostTagVO> sc = HostSearch.create();
        sc.setParameters("hostId", hostId);
        expunge(sc);

        for (String tag : hostTags) {
            tag = tag.trim();
            if (tag.length() > 0) {
                HostTagVO vo = new HostTagVO(hostId, tag);
                persist(vo);
            }
        }
        txn.commit();
    }
}
