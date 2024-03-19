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
package org.apache.cloudstack.quota.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;

@Component
public class QuotaEmailConfigurationDaoImpl extends GenericDaoBase<QuotaEmailConfigurationVO, Long> implements QuotaEmailConfigurationDao {

    @Inject
    private QuotaEmailTemplatesDao quotaEmailTemplatesDao;

    private SearchBuilder<QuotaEmailConfigurationVO> searchBuilderFindByIds;

    private SearchBuilder<QuotaEmailTemplatesVO> searchBuilderFindByTemplateName;

    private SearchBuilder<QuotaEmailConfigurationVO> searchBuilderFindByTemplateTypeAndAccountId;

    @PostConstruct
    public void init() {
        searchBuilderFindByIds = createSearchBuilder();
        searchBuilderFindByIds.and("account_id", searchBuilderFindByIds.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchBuilderFindByIds.and("email_template_id", searchBuilderFindByIds.entity().getEmailTemplateId(), SearchCriteria.Op.EQ);
        searchBuilderFindByIds.done();

        searchBuilderFindByTemplateName = quotaEmailTemplatesDao.createSearchBuilder();
        searchBuilderFindByTemplateName.and("template_name", searchBuilderFindByTemplateName.entity().getTemplateName(), SearchCriteria.Op.EQ);

        searchBuilderFindByTemplateTypeAndAccountId = createSearchBuilder();
        searchBuilderFindByTemplateTypeAndAccountId.and("account_id", searchBuilderFindByTemplateTypeAndAccountId.entity().getAccountId(), SearchCriteria.Op.EQ);
        searchBuilderFindByTemplateTypeAndAccountId.join("email_template_id", searchBuilderFindByTemplateName, searchBuilderFindByTemplateName.entity().getId(),
                searchBuilderFindByTemplateTypeAndAccountId.entity().getEmailTemplateId(), JoinBuilder.JoinType.INNER);

        searchBuilderFindByTemplateName.done();
        searchBuilderFindByTemplateTypeAndAccountId.done();
    }

    @Override
    public QuotaEmailConfigurationVO findByAccountIdAndEmailTemplateId(long accountId, long emailTemplateId) {
        SearchCriteria<QuotaEmailConfigurationVO> sc = searchBuilderFindByIds.create();
        sc.setParameters("account_id", accountId);
        sc.setParameters("email_template_id", emailTemplateId);
        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaEmailConfigurationVO>) status -> findOneBy(sc));
    }

    @Override
    public QuotaEmailConfigurationVO updateQuotaEmailConfiguration(QuotaEmailConfigurationVO quotaEmailConfigurationVO) {
        SearchCriteria<QuotaEmailConfigurationVO> sc = searchBuilderFindByIds.create();
        sc.setParameters("account_id", quotaEmailConfigurationVO.getAccountId());
        sc.setParameters("email_template_id", quotaEmailConfigurationVO.getEmailTemplateId());
        Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<Integer>) status -> update(quotaEmailConfigurationVO, sc));

        return quotaEmailConfigurationVO;
    }

    @Override
    public void persistQuotaEmailConfiguration(QuotaEmailConfigurationVO quotaEmailConfigurationVO) {
        Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaEmailConfigurationVO>) status -> persist(quotaEmailConfigurationVO));
    }

    @Override
    public List<QuotaEmailConfigurationVO> listByAccount(long accountId) {
        SearchCriteria<QuotaEmailConfigurationVO> sc = searchBuilderFindByIds.create();
        sc.setParameters("account_id", accountId);

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<List<QuotaEmailConfigurationVO>>) status -> listBy(sc));
    }

    @Override
    public QuotaEmailConfigurationVO findByAccountIdAndEmailTemplateType(long accountId, QuotaConfig.QuotaEmailTemplateTypes quotaEmailTemplateType) {
        SearchCriteria<QuotaEmailConfigurationVO> sc = searchBuilderFindByTemplateTypeAndAccountId.create();
        sc.setParameters("account_id", accountId);
        sc.setJoinParameters("email_template_id", "template_name", quotaEmailTemplateType.toString());

        return Transaction.execute(TransactionLegacy.USAGE_DB, (TransactionCallback<QuotaEmailConfigurationVO>) status -> findOneBy(sc));
    }
}
