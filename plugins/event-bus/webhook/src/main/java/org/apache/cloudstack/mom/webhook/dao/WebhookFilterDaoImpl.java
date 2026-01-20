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

package org.apache.cloudstack.mom.webhook.dao;

import java.util.List;

import org.apache.cloudstack.mom.webhook.vo.WebhookFilterVO;
import org.apache.commons.lang3.ObjectUtils;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class WebhookFilterDaoImpl extends GenericDaoBase<WebhookFilterVO, Long> implements WebhookFilterDao {

    SearchBuilder<WebhookFilterVO> IdWebhookIdSearch;

    public WebhookFilterDaoImpl() {
        IdWebhookIdSearch = createSearchBuilder();
        IdWebhookIdSearch.and("id", IdWebhookIdSearch.entity().getId(), SearchCriteria.Op.EQ);
        IdWebhookIdSearch.and("webhookId", IdWebhookIdSearch.entity().getWebhookId(), SearchCriteria.Op.EQ);
        IdWebhookIdSearch.done();
    }

    @Override
    public Pair<List<WebhookFilterVO>, Integer> searchBy(Long id, Long webhookId, Long startIndex, Long pageSize) {
        Filter searchFilter = new Filter(WebhookFilterVO.class, "id", false, startIndex,
                pageSize);
        SearchCriteria<WebhookFilterVO> sc = IdWebhookIdSearch.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (webhookId != null) {
            sc.setParameters("webhookId", webhookId);
        }
        return searchAndCount(sc, searchFilter);
    }

    @Override
    public List<WebhookFilterVO> listByWebhook(Long webhookId) {
        SearchCriteria<WebhookFilterVO> sc = IdWebhookIdSearch.create();
        if (webhookId != null) {
            sc.setParameters("webhookId", webhookId);
        }
        return listBy(sc);
    }

    @Override
    public int delete(Long id, Long webhookId) {
        SearchCriteria<WebhookFilterVO> sc = IdWebhookIdSearch.create();
        if (ObjectUtils.allNull(id, webhookId)) {
            return 0;
        }
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (webhookId != null) {
            sc.setParameters("webhookId", webhookId);
        }
        return remove(sc);
    }
}
