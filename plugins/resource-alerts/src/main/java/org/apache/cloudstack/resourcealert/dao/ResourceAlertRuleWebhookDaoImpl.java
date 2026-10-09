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

package org.apache.cloudstack.resourcealert.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cloudstack.resourcealert.vo.ResourceAlertRuleWebhookVO;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;

public class ResourceAlertRuleWebhookDaoImpl extends GenericDaoBase<ResourceAlertRuleWebhookVO, Long>
        implements ResourceAlertRuleWebhookDao {

    // Webhooks are soft deleted, so the foreign key cascade never removes their links.
    private static final String REMOVE_LINKS_TO_REMOVED_WEBHOOKS = "DELETE rw FROM `cloud`.`resource_alert_rules_webhook` rw "
            + "JOIN `cloud`.`webhook` w ON w.id = rw.webhook_id WHERE w.removed IS NOT NULL";

    private final SearchBuilder<ResourceAlertRuleWebhookVO> ruleSearch;

    public ResourceAlertRuleWebhookDaoImpl() {
        ruleSearch = createSearchBuilder();
        ruleSearch.and("ruleId", ruleSearch.entity().getRuleId(), SearchCriteria.Op.EQ);
        ruleSearch.done();
    }

    @Override
    public List<Long> listWebhookIdsByRule(long ruleId) {
        SearchCriteria<ResourceAlertRuleWebhookVO> sc = ruleSearch.create();
        sc.setParameters("ruleId", ruleId);
        return listBy(sc).stream().map(ResourceAlertRuleWebhookVO::getWebhookId).collect(Collectors.toList());
    }

    @Override
    public void replaceWebhooksForRule(long ruleId, List<Long> webhookIds) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                SearchCriteria<ResourceAlertRuleWebhookVO> sc = ruleSearch.create();
                sc.setParameters("ruleId", ruleId);
                expunge(sc);
                for (Long webhookId : webhookIds) {
                    persist(new ResourceAlertRuleWebhookVO(ruleId, webhookId));
                }
            }
        });
    }

    @Override
    public int removeLinksToRemovedWebhooks() {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (PreparedStatement pstmt = txn.prepareStatement(REMOVE_LINKS_TO_REMOVED_WEBHOOKS)) {
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to remove resource alert rule links to removed webhooks", e);
        }
    }
}
