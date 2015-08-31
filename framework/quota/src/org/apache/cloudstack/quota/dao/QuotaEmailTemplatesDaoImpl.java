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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import java.util.List;

@Component
@Local(value = { QuotaEmailTemplatesDao.class })
public class QuotaEmailTemplatesDaoImpl extends GenericDaoBase<QuotaEmailTemplatesVO, Long> implements QuotaEmailTemplatesDao {

    protected SearchBuilder<QuotaEmailTemplatesVO> QuotaEmailTemplateSearch;

    public QuotaEmailTemplatesDaoImpl() {
        super();

        QuotaEmailTemplateSearch = createSearchBuilder();
        QuotaEmailTemplateSearch.and("template_name", QuotaEmailTemplateSearch.entity().getTemplateName(), SearchCriteria.Op.EQ);
        QuotaEmailTemplateSearch.done();
    }

    @Override
    public List<QuotaEmailTemplatesVO> listAllQuotaEmailTemplates(String templateName) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        SearchCriteria<QuotaEmailTemplatesVO> sc = QuotaEmailTemplateSearch.create();
        if (templateName != null) {
            sc.setParameters("template_name", templateName);
        }
        List<QuotaEmailTemplatesVO> result = this.listBy(sc);
        TransactionLegacy.open(opendb).close();
        return result;
    }

    @Override
    public boolean updateQuotaEmailTemplate(QuotaEmailTemplatesVO template) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        final boolean result = this.update(template.getId(), template);
        TransactionLegacy.open(opendb).close();
        return result;
    }
}
