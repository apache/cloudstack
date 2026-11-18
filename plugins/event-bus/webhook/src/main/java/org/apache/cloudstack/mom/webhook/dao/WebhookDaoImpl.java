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
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.mom.webhook.Webhook;
import org.apache.cloudstack.mom.webhook.vo.WebhookVO;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class WebhookDaoImpl extends GenericDaoBase<WebhookVO, Long> implements WebhookDao {
    SearchBuilder<WebhookVO> accountIdSearch;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        accountIdSearch = createSearchBuilder();
        accountIdSearch.and("accountId", accountIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);

        return true;
    }
    @Override
    public List<WebhookVO> listByEnabledForDelivery(Long accountId, List<Long> domainIds) {
        SearchBuilder<WebhookVO> sb = createSearchBuilder();
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);
        sb.and().op("scopeGlobal", sb.entity().getScope(), SearchCriteria.Op.EQ);
        if (accountId != null) {
            sb.or().op("scopeLocal", sb.entity().getScope(), SearchCriteria.Op.EQ);
            sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
            sb.cp();
        }
        if (CollectionUtils.isNotEmpty(domainIds)) {
            sb.or().op("scopeDomain", sb.entity().getScope(), SearchCriteria.Op.EQ);
            sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.IN);
            sb.cp();
        }
        sb.cp();
        SearchCriteria<WebhookVO> sc = sb.create();
        sc.setParameters("state", Webhook.State.Enabled.name());
        sc.setParameters("scopeGlobal", Webhook.Scope.Global.name());
        if (accountId != null) {
            sc.setParameters("scopeLocal", Webhook.Scope.Local.name());
            sc.setParameters("accountId", accountId);
        }
        if (CollectionUtils.isNotEmpty(domainIds)) {
            sc.setParameters("scopeDomain", Webhook.Scope.Domain.name());
            sc.setParameters("domainId", domainIds.toArray());
        }
        return listBy(sc);
    }

    @Override
    public void deleteByAccount(long accountId) {
        SearchCriteria<WebhookVO> sc = accountIdSearch.create();
        sc.setParameters("accountId", accountId);
        remove(sc);
    }

    @Override
    public List<WebhookVO> listByAccount(long accountId) {
        SearchCriteria<WebhookVO> sc = accountIdSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
    }

    @Override
    public WebhookVO findByAccountAndPayloadUrl(long accountId, String payloadUrl) {
        SearchBuilder<WebhookVO> sb = createSearchBuilder();
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("payloadUrl", sb.entity().getPayloadUrl(), SearchCriteria.Op.EQ);
        SearchCriteria<WebhookVO> sc = sb.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("payloadUrl", payloadUrl);
        return findOneBy(sc);
    }
}
