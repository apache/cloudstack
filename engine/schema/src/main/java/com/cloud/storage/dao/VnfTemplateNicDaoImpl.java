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

import com.cloud.storage.VnfTemplateNicVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

public class VnfTemplateNicDaoImpl extends GenericDaoBase<VnfTemplateNicVO, Long> implements VnfTemplateNicDao {

    protected SearchBuilder<VnfTemplateNicVO> TemplateSearch;

    public VnfTemplateNicDaoImpl() {
        TemplateSearch = createSearchBuilder();
        TemplateSearch.and("templateId", TemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateSearch.done();
    }

    @Override
    public List<VnfTemplateNicVO> listByTemplateId(long templateId) {
        SearchCriteria<VnfTemplateNicVO> sc = TemplateSearch.create();
        sc.setParameters("templateId", templateId);
        return listBy(sc);
    }

    @Override
    public void deleteByTemplateId(long templateId) {
        SearchCriteria<VnfTemplateNicVO> sc = TemplateSearch.create();
        sc.setParameters("templateId", templateId);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        remove(sc);
        txn.commit();
    }
}
