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

import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.quota.vo.QuotaUsageDetailVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class QuotaUsageDetailDaoImpl extends GenericDaoBase<QuotaUsageDetailVO, Long> implements QuotaUsageDetailDao {
    private SearchBuilder<QuotaUsageDetailVO> searchQuotaUsageDetails;

    @PostConstruct
    public void init() {
        searchQuotaUsageDetails = createSearchBuilder();
        searchQuotaUsageDetails.and("quotaUsageId", searchQuotaUsageDetails.entity().getQuotaUsageId(), SearchCriteria.Op.EQ);
        searchQuotaUsageDetails.done();
    }

    @Override
    public void persistQuotaUsageDetail(final QuotaUsageDetailVO quotaUsageDetail) {
        logger.trace("Persisting quota usage detail [{}].", quotaUsageDetail);
        Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaUsageDetailVO>) status -> persist(quotaUsageDetail));
    }

    @Override
    public List<QuotaUsageDetailVO> listQuotaUsageDetails(Long quotaUsageId) {
        SearchCriteria<QuotaUsageDetailVO> sc = searchQuotaUsageDetails.create();
        sc.setParameters("quotaUsageId", quotaUsageId);
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaUsageDetailVO>>) status -> listBy(sc));
    }

}
