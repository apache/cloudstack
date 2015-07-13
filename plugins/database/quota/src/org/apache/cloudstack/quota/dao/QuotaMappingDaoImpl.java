//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.quota.dao;

import java.util.List;

import javax.ejb.Local;

import org.springframework.stereotype.Component;
import org.apache.cloudstack.quota.QuotaMappingVO;

import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = { QuotaMappingDao.class })
public class QuotaMappingDaoImpl extends GenericDaoBase<QuotaMappingVO, Long> implements QuotaMappingDao {
    private final SearchBuilder<QuotaMappingVO> searchUsageType;
    private final SearchBuilder<QuotaMappingVO> listAllIncludedUsageType;

    public QuotaMappingDaoImpl() {
        super();
        searchUsageType = createSearchBuilder();
        searchUsageType.and("usage_type", searchUsageType.entity().getUsageType(), SearchCriteria.Op.EQ);
        searchUsageType.done();

        listAllIncludedUsageType = createSearchBuilder();
        listAllIncludedUsageType.and("include", listAllIncludedUsageType.entity().getInclude(), SearchCriteria.Op.EQ);
        listAllIncludedUsageType.done();
    }

    @Override
    public QuotaMappingVO findByUsageType(final int usageType) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            final SearchCriteria<QuotaMappingVO> sc = searchUsageType.create();
            sc.setParameters("usage_type", usageType);
            return findOneBy(sc);
        } finally {
            txn.close();
        }
    }

    @Override
    public Pair<List<QuotaMappingVO>, Integer> listAllMapping() {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            final SearchCriteria<QuotaMappingVO> sc = listAllIncludedUsageType.create();
            sc.setParameters("include", 1);
            return searchAndCount(sc, null);
        } finally {
            txn.close();
        }
    }

}
