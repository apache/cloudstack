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

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.quota.QuotaUsageVO;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;

@Component
@Local(value = { QuotaUsageDao.class })
public class QuotaUsageDaoImpl extends GenericDaoBase<QuotaUsageVO, Long> implements QuotaUsageDao {

    @Override
    public Pair<List<QuotaUsageVO>, Integer> searchAndCountAllRecords(SearchCriteria<QuotaUsageVO> sc, Filter filter) {
        return listAndCountIncludingRemovedBy(sc, filter);
    }

    @Override
    public void saveQuotaUsage(List<QuotaUsageVO> records) {
        for (QuotaUsageVO usageRecord : records) {
            persist(usageRecord);
        }
    }

}
