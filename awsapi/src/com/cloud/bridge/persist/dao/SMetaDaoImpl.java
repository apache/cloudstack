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

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.bridge.model.SMetaVO;
import com.cloud.bridge.service.core.s3.S3MetaDataEntry;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {SMetaDao.class})
public class SMetaDaoImpl extends GenericDaoBase<SMetaVO, Long> implements SMetaDao {

    public SMetaDaoImpl() {
    }

    @Override
    public List<SMetaVO> getByTarget(String target, long targetId) {
        SearchBuilder<SMetaVO> SearchByTarget = createSearchBuilder();
        SearchByTarget.and("Target", SearchByTarget.entity().getTarget(), SearchCriteria.Op.EQ);
        SearchByTarget.and("TargetID", SearchByTarget.entity().getTargetId(), SearchCriteria.Op.EQ);
        SearchByTarget.done();
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        try {
            txn.start();
            SearchCriteria<SMetaVO> sc = SearchByTarget.create();
            sc.setParameters("Target", target);
            sc.setParameters("TargetID", targetId);
            return listBy(sc);
        } finally {
            txn.close();
        }

    }

    @Override
    public SMetaVO save(String target, long targetId, S3MetaDataEntry entry) {
        SMetaVO meta = new SMetaVO();
        meta.setTarget(target);
        meta.setTargetId(targetId);
        meta.setName(entry.getName());
        meta.setValue(entry.getValue());
        meta = this.persist(meta);
        return meta;
    }

    @Override
    public void save(String target, long targetId, S3MetaDataEntry[] entries) {
        // To redefine the target's metadaa
        SearchBuilder<SMetaVO> SearchByTarget = createSearchBuilder();
        SearchByTarget.and("Target", SearchByTarget.entity().getTarget(), SearchCriteria.Op.EQ);
        SearchByTarget.and("TargetID", SearchByTarget.entity().getTargetId(), SearchCriteria.Op.EQ);
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.AWSAPI_DB);
        try {
            txn.start();
            SearchCriteria<SMetaVO> sc = SearchByTarget.create();
            sc.setParameters("Target", target);
            sc.setParameters("TargetID", targetId);
            this.remove(sc);

            if (entries != null) {
                for (S3MetaDataEntry entry : entries)
                    save(target, targetId, entry);
            }
            txn.commit();
        } finally {
            txn.close();
        }
    }
}