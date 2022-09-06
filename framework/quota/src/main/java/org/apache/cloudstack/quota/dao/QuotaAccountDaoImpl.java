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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;

@Component
public class QuotaAccountDaoImpl extends GenericDaoBase<QuotaAccountVO, Long> implements QuotaAccountDao {
    public static final Logger s_logger = Logger.getLogger(QuotaAccountDaoImpl.class);

    @Override
    public List<QuotaAccountVO> listAllQuotaAccount() {
        List<QuotaAccountVO> accountsWithQuotaEnabled = new ArrayList<>();
        for (QuotaAccountVO account : listAllQuotaAccount(null, null).first()) {
            if (Boolean.TRUE.equals(getQuotaAccountEnabled(account.getAccountId()))) {
                accountsWithQuotaEnabled.add(account);
                continue;
            }
            s_logger.trace(String.format("Account [%s] has the quota plugin disabled. Thus, it will not receive quota emails.", account));
        }
        return accountsWithQuotaEnabled;
    }

    @Override
    public Pair<List<QuotaAccountVO>,Integer> listAllQuotaAccount(final Long startIndex, final Long pageSize) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Pair<List<QuotaAccountVO>,Integer>>() {
            @Override
            public Pair<List<QuotaAccountVO>,Integer> doInTransaction(final TransactionStatus status) {
                Filter filter = new Filter(QuotaAccountVO.class, "accountId", true, startIndex, pageSize);
                return searchAndCount(null, filter);
            }
        });
    }

    @Override
    public QuotaAccountVO findByIdQuotaAccount(final Long id) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaAccountVO>() {
            @Override
            public QuotaAccountVO doInTransaction(final TransactionStatus status) {
                return findById(id);
            }
        });
    }

    @Override
    public QuotaAccountVO persistQuotaAccount(final QuotaAccountVO entity) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<QuotaAccountVO>() {
            @Override
            public QuotaAccountVO doInTransaction(final TransactionStatus status) {
                return persist(entity);
            }
        });
    }

    @Override
    public boolean updateQuotaAccount(final Long id, final QuotaAccountVO entity) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(final TransactionStatus status) {
                return update(id, entity);
            }
        });
    }

    public Boolean getQuotaAccountEnabled(Long accountId) {
        return QuotaConfig.QuotaAccountEnabled.valueIn(accountId);
    }
}
