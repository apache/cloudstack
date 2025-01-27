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

import org.apache.cloudstack.mom.webhook.vo.WebhookJoinVO;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class WebhookJoinDaoImpl extends GenericDaoBase<WebhookJoinVO, Long> implements WebhookJoinDao {
    @Override
    public List<WebhookJoinVO> listByAccountOrDomain(long accountId, String domainPath) {
        SearchBuilder<WebhookJoinVO> sb = createSearchBuilder();
        sb.and().op("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        if (StringUtils.isNotBlank(domainPath)) {
            sb.or("domainPath", sb.entity().getDomainPath(), SearchCriteria.Op.LIKE);
        }
        sb.cp();
        SearchCriteria<WebhookJoinVO> sc = sb.create();
        sc.setParameters("accountId", accountId);
        if (StringUtils.isNotBlank(domainPath)) {
            sc.setParameters("domainPath", domainPath);
        }
        return listBy(sc);
    }
}
