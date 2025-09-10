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

import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryVO;

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class WebhookDeliveryDaoImpl extends GenericDaoBase<WebhookDeliveryVO, Long> implements WebhookDeliveryDao {
    @Override
    public int deleteByDeleteApiParams(Long id, Long webhookId, Long managementServerId, Date startDate,
           Date endDate) {
        SearchBuilder<WebhookDeliveryVO> sb = createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("webhookId", sb.entity().getWebhookId(), SearchCriteria.Op.EQ);
        sb.and("managementServerId", sb.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        sb.and("startDate", sb.entity().getStartTime(), SearchCriteria.Op.GTEQ);
        sb.and("endDate", sb.entity().getEndTime(), SearchCriteria.Op.LTEQ);
        SearchCriteria<WebhookDeliveryVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (webhookId != null) {
            sc.setParameters("webhookId", webhookId);
        }
        if (managementServerId != null) {
            sc.setParameters("managementServerId", managementServerId);
        }
        if (startDate != null) {
            sc.setParameters("startDate", startDate);
        }
        if (endDate != null) {
            sc.setParameters("endDate", endDate);
        }
        return remove(sc);
    }

    @Override
    public void removeOlderDeliveries(long webhookId, long limit) {
        Filter searchFilter = new Filter(WebhookDeliveryVO.class, "id", false, 0L, limit);
        SearchBuilder<WebhookDeliveryVO> sb = createSearchBuilder();
        sb.and("webhookId", sb.entity().getWebhookId(), SearchCriteria.Op.EQ);
        SearchCriteria<WebhookDeliveryVO> sc = sb.create();
        sc.setParameters("webhookId", webhookId);
        List<WebhookDeliveryVO> keep = listBy(sc, searchFilter);
        SearchBuilder<WebhookDeliveryVO> sbDelete = createSearchBuilder();
        sbDelete.and("id", sbDelete.entity().getId(), SearchCriteria.Op.NOTIN);
        SearchCriteria<WebhookDeliveryVO> scDelete = sbDelete.create();
        scDelete.setParameters("id", keep.stream().map(WebhookDeliveryVO::getId).toArray());
        remove(scDelete);
    }
}
