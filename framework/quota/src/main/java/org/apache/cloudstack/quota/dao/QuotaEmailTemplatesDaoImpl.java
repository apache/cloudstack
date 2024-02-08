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

import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import org.apache.commons.lang3.StringUtils;

@Component
public class QuotaEmailTemplatesDaoImpl extends GenericDaoBase<QuotaEmailTemplatesVO, Long> implements QuotaEmailTemplatesDao {

    protected SearchBuilder<QuotaEmailTemplatesVO> QuotaEmailTemplateSearch;

    public QuotaEmailTemplatesDaoImpl() {
        super();

        QuotaEmailTemplateSearch = createSearchBuilder();
        QuotaEmailTemplateSearch.and("template_name", QuotaEmailTemplateSearch.entity().getTemplateName(), SearchCriteria.Op.EQ);
        QuotaEmailTemplateSearch.done();
    }

    @Override
    public List<QuotaEmailTemplatesVO> listAllQuotaEmailTemplates(final String templateName) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<List<QuotaEmailTemplatesVO>>() {
            @Override
            public List<QuotaEmailTemplatesVO> doInTransaction(final TransactionStatus status) {
                SearchCriteria<QuotaEmailTemplatesVO> sc = QuotaEmailTemplateSearch.create();
                if (StringUtils.isNotEmpty(templateName)) {
                    sc.setParameters("template_name", templateName);
                }
                return listBy(sc);
            }
        });
    }

    @Override
    public boolean updateQuotaEmailTemplate(final QuotaEmailTemplatesVO template) {
        return Transaction.execute(TransactionLegacy.USAGE_DB, new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(final TransactionStatus status) {
                return update(template.getId(), template);
            }
        });
    }
}
