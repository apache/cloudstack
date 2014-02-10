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
package com.cloud.bridge.persist.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.bridge.model.SHostVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {SHostDao.class})
public class SHostDaoImpl extends GenericDaoBase<SHostVO, Long> implements SHostDao {
    public SHostDaoImpl() {
    }

    @Override
    public SHostVO getByHost(String host) {
        SearchBuilder<SHostVO> HostSearch = createSearchBuilder();
        HostSearch.and("Host", HostSearch.entity().getHost(), SearchCriteria.Op.EQ);
        HostSearch.done();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        try {
            txn.start();
            SearchCriteria<SHostVO> sc = HostSearch.create();
            sc.setParameters("Host", host);
            return findOneBy(sc);
        } finally {
            txn.commit();
            txn.close();
        }

    }

    @Override
    public SHostVO getLocalStorageHost(long mhostId, String storageRoot) {
        SearchBuilder<SHostVO> LocalStorageHostSearch = createSearchBuilder();
        LocalStorageHostSearch.and("MHostID", LocalStorageHostSearch.entity().getMhostid(), SearchCriteria.Op.EQ);
        LocalStorageHostSearch.and("ExportRoot", LocalStorageHostSearch.entity().getExportRoot(), SearchCriteria.Op.EQ);
        LocalStorageHostSearch.done();
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            txn.start();
            SearchCriteria<SHostVO> sc = LocalStorageHostSearch.create();
            sc.setParameters("MHostID", mhostId);
            sc.setParameters("ExportRoot", storageRoot);
            return findOneBy(sc);
        } finally {
            txn.commit();
            txn.close();
        }
    }
}
