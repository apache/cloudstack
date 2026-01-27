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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryJoinVO;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class WebhookDeliveryJoinDaoImpl extends GenericDaoBase<WebhookDeliveryJoinVO, Long>
        implements WebhookDeliveryJoinDao {
    @Override
    public Pair<List<WebhookDeliveryJoinVO>, Integer> searchAndCountByListApiParameters(Long id,
            List<Long> webhookIds, Long managementServerId, String keyword, final Date startDate,
            final Date endDate, final String eventType, Filter searchFilter) {
        SearchBuilder<WebhookDeliveryJoinVO> sb = createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("webhookId", sb.entity().getWebhookId(), SearchCriteria.Op.IN);
        sb.and("managementServerId", sb.entity().getManagementServerMsId(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getPayload(), SearchCriteria.Op.LIKE);
        sb.and("startDate", sb.entity().getStartTime(), SearchCriteria.Op.GTEQ);
        sb.and("endDate", sb.entity().getEndTime(), SearchCriteria.Op.LTEQ);
        sb.and("eventType", sb.entity().getEventType(), SearchCriteria.Op.EQ);
        SearchCriteria<WebhookDeliveryJoinVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (CollectionUtils.isNotEmpty(webhookIds)) {
            sc.setParameters("webhookId", webhookIds.toArray());
        }
        if (managementServerId != null) {
            sc.setParameters("managementServerId", managementServerId);
        }
        if (keyword != null) {
            sc.setParameters("keyword", "%" + keyword + "%");
        }
        if (startDate != null) {
            sc.setParameters("startDate", startDate);
        }
        if (endDate != null) {
            sc.setParameters("endDate", endDate);
        }
        if (StringUtils.isNotBlank(eventType)) {
            sc.setParameters("eventType", eventType);
        }
        return searchAndCount(sc, searchFilter);
    }
}
