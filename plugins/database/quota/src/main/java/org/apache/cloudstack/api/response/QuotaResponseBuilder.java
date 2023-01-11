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
package org.apache.cloudstack.api.response;

import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaTariffCreateCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;

import java.util.Date;
import java.util.List;

import com.cloud.utils.Pair;

public interface QuotaResponseBuilder {

    QuotaTariffVO updateQuotaTariffPlan(QuotaTariffUpdateCmd cmd);

    Pair<List<QuotaTariffVO>, Integer> listQuotaTariffPlans(QuotaTariffListCmd cmd);

    QuotaTariffResponse createQuotaTariffResponse(QuotaTariffVO configuration);

    QuotaStatementResponse createQuotaStatementResponse(List<QuotaUsageVO> quotaUsage);

    QuotaBalanceResponse createQuotaBalanceResponse(List<QuotaBalanceVO> quotaUsage, Date startDate, Date endDate);

    Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(Boolean listAll);

    Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(Boolean listAll, String keyword, Long startIndex, Long pageSize);

    Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(String accountName, Long domainId);

    QuotaBalanceResponse createQuotaLastBalanceResponse(List<QuotaBalanceVO> quotaBalance, Date startDate);

    List<QuotaUsageVO> getQuotaUsage(QuotaStatementCmd cmd);

    List<QuotaBalanceVO> getQuotaBalance(QuotaBalanceCmd cmd);

    QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Double amount, Long updatedBy, Boolean enforce);

    List<QuotaEmailTemplateResponse> listQuotaEmailTemplates(QuotaEmailTemplateListCmd cmd);

    boolean updateQuotaEmailTemplate(QuotaEmailTemplateUpdateCmd cmd);

    Date startOfNextDay(Date dt);

    Date startOfNextDay();

    QuotaTariffVO createQuotaTariff(QuotaTariffCreateCmd cmd);

    boolean deleteQuotaTariff(String quotaTariffUuid);
}
