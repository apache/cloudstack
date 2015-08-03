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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.db.TransactionLegacy;

import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.region.RegionManager;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.ejb.Local;
import javax.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Component
@Local(value = QuotaResponseBuilderImpl.class)
public class QuotaResponseBuilderImpl implements QuotaResponseBuilder {
    private static final Logger s_logger = Logger.getLogger(QuotaResponseBuilderImpl.class.getName());

    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private QuotaCreditsDao _quotaCreditsDao;
    @Inject
    private QuotaEmailTemplatesDao _quotaEmailTemplateDao;

    @Inject
    private UserDao _userDao;
    @Inject
    private QuotaService _quotaService;
    @Inject
    AccountDao _accountDao;
    @Inject
    private RegionManager _regionMgr;

    @Override
    public QuotaTariffResponse createQuotaTariffResponse(QuotaTariffVO tariff) {
        final QuotaTariffResponse response = new QuotaTariffResponse();
        response.setUsageType(tariff.getUsageType());
        response.setUsageName(tariff.getUsageName());
        response.setUsageUnit(tariff.getUsageUnit());
        response.setUsageDiscriminator(tariff.getUsageDiscriminator());
        response.setTariffValue(tariff.getCurrencyValue());
        response.setEffectiveOn(tariff.getEffectiveOn());
        response.setDescription(tariff.getDescription());
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        return response;
    }

    @Override
    public QuotaBalanceResponse createQuotaBalanceResponse(List<QuotaBalanceVO> quotaBalance, Date startDate, Date endDate) {
        if (quotaBalance.size() == 0) {
            new InvalidParameterValueException("The request period does not contain balance entries.");
        }
        Collections.sort(quotaBalance, new Comparator<QuotaBalanceVO>() {
            public int compare(QuotaBalanceVO o1, QuotaBalanceVO o2) {
                return o2.getUpdatedOn().compareTo(o1.getUpdatedOn()); // desc
            }
        });

        QuotaBalanceResponse resp = new QuotaBalanceResponse();
        BigDecimal lastCredits = new BigDecimal(0);
        for (Iterator<QuotaBalanceVO> it = quotaBalance.iterator(); it.hasNext();) {
            QuotaBalanceVO entry = it.next();
            s_logger.info("createQuotaBalanceResponse: Date=" + entry.getUpdatedOn().toGMTString() + " balance=" + entry.getCreditBalance() + " credit=" + entry.getCreditsId());
            if (entry.getCreditsId() > 0) {
                lastCredits = lastCredits.add(entry.getCreditBalance());
                resp.addCredits(entry);
                it.remove();
            } else {
                break;
            }
        }

        if (quotaBalance.size() > 0) {
            // order is desc last item is the start item
            QuotaBalanceVO startItem = quotaBalance.get(quotaBalance.size() - 1);
            QuotaBalanceVO endItem = quotaBalance.get(0);
            resp.setStartDate(startDate);
            resp.setStartQuota(startItem.getCreditBalance());
            resp.setEndDate(endDate);
            resp.setEndQuota(endItem.getCreditBalance().add(lastCredits));
        } else {
            resp.setStartQuota(new BigDecimal(0));
            resp.setEndQuota(new BigDecimal(0));
        }
        resp.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        resp.setObjectName("balance");
        return resp;
    }

    @Override
    public QuotaStatementResponse createQuotaStatementResponse(final List<QuotaUsageVO> quotaUsage) {
        if (quotaUsage == null || quotaUsage.size() == 0) {
            throw new InvalidParameterValueException("There is no usage data found for period mentioned.");
        }
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        QuotaStatementResponse statement = new QuotaStatementResponse();

        HashMap<Integer, QuotaTariffVO> quotaTariffMap = new HashMap<Integer, QuotaTariffVO>();
        List<QuotaTariffVO> result = _quotaTariffDao.listAll();

        for (QuotaTariffVO quotaTariff : result) {
            quotaTariffMap.put(quotaTariff.getUsageType(), quotaTariff);
            // add dummy record for each usage type
            QuotaUsageVO dummy = new QuotaUsageVO(quotaUsage.get(0));
            dummy.setUsageType(quotaTariff.getUsageType());
            dummy.setQuotaUsed(new BigDecimal(0));
            quotaUsage.add(dummy);
        }

        Collections.sort(quotaUsage, new Comparator<QuotaUsageVO>() {
            public int compare(QuotaUsageVO o1, QuotaUsageVO o2) {
                if (o1.getUsageType() == o2.getUsageType())
                    return 0;
                return o1.getUsageType() < o2.getUsageType() ? -1 : 1;
            }
        });

        List<QuotaStatementItemResponse> items = new ArrayList<QuotaStatementItemResponse>();
        QuotaStatementItemResponse lineitem;
        int type = -1;
        BigDecimal usage = new BigDecimal(0);
        BigDecimal totalUsage = new BigDecimal(0);
        quotaUsage.add(new QuotaUsageVO());// boundary
        QuotaUsageVO prev = quotaUsage.get(0);
        for (final QuotaUsageVO quotaRecord : quotaUsage) {
            s_logger.info("createQuotaStatementResponse Type=" + quotaRecord.getUsageType() + " usage=" + usage + " name" + quotaRecord.getUsageItemId());
            if (type != quotaRecord.getUsageType()) {
                if (type != -1) {
                    lineitem = new QuotaStatementItemResponse();
                    lineitem.setUsageType(type);
                    lineitem.setQuotaUsed(usage);
                    lineitem.setAccountId(prev.getAccountId());
                    lineitem.setDomainId(prev.getDomainId());
                    lineitem.setStartDate(prev.getStartDate());
                    lineitem.setEndDate(prev.getEndDate());
                    lineitem.setUsageUnit(quotaTariffMap.get(type).getUsageUnit());
                    lineitem.setUsageName(quotaTariffMap.get(type).getUsageName());
                    lineitem.setObjectName("quotausage");
                    items.add(lineitem);
                    totalUsage = totalUsage.add(usage);
                    usage = new BigDecimal(0);
                }
                type = quotaRecord.getUsageType();
            }
            prev = quotaRecord;
            usage = usage.add(quotaRecord.getQuotaUsed());
        }
        TransactionLegacy.open(opendb).close();

        statement.setLineItem(items);
        statement.setTotalQuota(totalUsage);
        statement.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        statement.setObjectName("statement");
        return statement;
    }

    @Override
    public List<QuotaTariffVO> listQuotaTariffPlans(final QuotaTariffListCmd cmd) {
        List<QuotaTariffVO> result = new ArrayList<QuotaTariffVO>();
        Date effectiveDate = cmd.getEffectiveDate() == null ? new Date() : cmd.getEffectiveDate();
        Date adjustedEffectiveDate = _quotaService.computeAdjustedTime(effectiveDate);
        s_logger.info("Effective datec=" + effectiveDate + " quotatype=" + cmd.getUsageType() + " Adjusted date=" + adjustedEffectiveDate);
        if (cmd.getUsageType() != null) {
            QuotaTariffVO tariffPlan = _quotaTariffDao.findTariffPlanByUsageType(cmd.getUsageType(), adjustedEffectiveDate);
            if (tariffPlan != null) {
                result.add(tariffPlan);
            }
        } else {
            result = _quotaTariffDao.listAllTariffPlans(adjustedEffectiveDate);
        }
        return result;
    }

    @Override
    public QuotaTariffVO updateQuotaTariffPlan(QuotaTariffUpdateCmd cmd) {
        final int quotaType = cmd.getUsageType();
        final BigDecimal quotaCost = new BigDecimal(cmd.getValue());
        final Date effectiveDate = _quotaService.computeAdjustedTime(cmd.getStartDate());
        final Date now = _quotaService.computeAdjustedTime(new Date());
        // if effective date is in the past return error
        if (effectiveDate.compareTo(now) < 0) {
            throw new InvalidParameterValueException("Incorrect effective date for tariff " + effectiveDate + " is less than now " + now);
        }
        QuotaTypes quotaConstant = QuotaTypes.listQuotaTypes().get(quotaType);
        if (quotaConstant == null) {
            throw new InvalidParameterValueException("Quota type does not exists " + quotaType);
        }
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        TransactionLegacy.open(TransactionLegacy.USAGE_DB).close();
        QuotaTariffVO result = null;
        try {
            result = new QuotaTariffVO();
            result.setUsageType(quotaType);
            result.setUsageName(quotaConstant.getQuotaName());
            result.setUsageUnit(quotaConstant.getQuotaUnit());
            result.setUsageDiscriminator(quotaConstant.getDiscriminator());
            result.setCurrencyValue(quotaCost);
            result.setEffectiveOn(effectiveDate);
            result.setUpdatedOn(now);
            result.setUpdatedBy(cmd.getEntityOwnerId());

            s_logger.debug(String.format("Updating Quota Tariff Plan: New value=%s for resource type=%d effective on date=%s", quotaCost, quotaType, effectiveDate));
            _quotaTariffDao.addQuotaTariff(result);
        } catch (Exception pokemon) {
            s_logger.error("Error in update quota tariff plan: " + pokemon);
        } finally {
            TransactionLegacy.open(opendb).close();
        }
        return result;
    }

    @Override
    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Double amount, Long updatedBy) {
        Date depositDate = new Date();
        Date adjustedStartDate = _quotaService.computeAdjustedTime(depositDate);
        QuotaBalanceVO qb = _quotaBalanceDao.findLaterBalanceEntry(accountId, domainId, adjustedStartDate);

        if (qb != null) {
            throw new InvalidParameterValueException("Incorrect deposit date: " + adjustedStartDate + " there are balance entries after this date");
        }

        return addQuotaCredits(accountId, domainId, amount, updatedBy, adjustedStartDate);
    }

    @Override
    public QuotaCreditsResponse addQuotaCredits(final Long accountId, final Long domainId, final Double amount, final Long updatedBy, final Date despositedOn) {
        final short opendb = TransactionLegacy.currentTxn().getDatabaseId();
        QuotaCreditsVO result = null;
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.USAGE_DB);
        try {
            QuotaCreditsVO credits = new QuotaCreditsVO(accountId, domainId, new BigDecimal(amount), updatedBy);
            s_logger.info("addQuotaCredits: Depositing " + amount + " on adjusted date " + despositedOn);
            credits.setUpdatedOn(despositedOn);
            result = _quotaCreditsDao.saveCredits(credits);
        } finally {
            txn.close();
        }
        TransactionLegacy.open(TransactionLegacy.CLOUD_DB).close();
        final AccountVO account = _accountDao.findById(accountId);
        final boolean lockAccountEnforcement = QuotaConfig.QuotaEnableEnforcement.value().equalsIgnoreCase("true");
        final BigDecimal currentAccountBalance = _quotaBalanceDao.lastQuotaBalance(accountId, domainId, startOfNextDay(despositedOn));
        if (lockAccountEnforcement && (currentAccountBalance.compareTo(new BigDecimal(0)) >= 0)) {
            if (account.getState() == Account.State.locked) {
                try {
                    _regionMgr.enableAccount(account.getAccountName(), domainId, accountId);
                    //_quotaMgr.sendQuotaAlert(account, currentAccountBalance, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_UNLOCK_ACCOUNT);
                } catch (Exception e) {
                    s_logger.error(String.format("Unable to unlock account %s after getting enough quota credits", account.getAccountName()));
                }
            }
        }

        String creditor = String.valueOf(Account.ACCOUNT_ID_SYSTEM);
        User creditorUser = _userDao.getUser(updatedBy);
        if (creditorUser != null) {
            creditor = creditorUser.getUsername();
        }
        TransactionLegacy.open(opendb).close();
        QuotaCreditsResponse response = new QuotaCreditsResponse(result, creditor);
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        return response;
    }

    private QuotaEmailTemplateResponse createQuotaEmailResponse(QuotaEmailTemplatesVO template) {
        QuotaEmailTemplateResponse response = new QuotaEmailTemplateResponse();
        response.setTemplateType(template.getTemplateName());
        response.setTemplateSubject(template.getTemplateSubject());
        response.setTemplateText(template.getTemplateBody());
        response.setLocale(template.getLocale());
        response.setLastUpdatedOn(template.getLastUpdated());
        return response;
    }

    @Override
    public List<QuotaEmailTemplateResponse> listQuotaEmailTemplates(QuotaEmailTemplateListCmd cmd) {
        final String templateName = cmd.getTemplateName();
        List<QuotaEmailTemplatesVO> templates = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(templateName);
        final List<QuotaEmailTemplateResponse> responses = new ArrayList<QuotaEmailTemplateResponse>();
        for (final QuotaEmailTemplatesVO template : templates) {
            responses.add(createQuotaEmailResponse(template));
        }
        return responses;
    }

    @Override
    public boolean updateQuotaEmailTemplate(QuotaEmailTemplateUpdateCmd cmd) {
        final String templateName = cmd.getTemplateName();
        final String templateSubject = StringEscapeUtils.escapeJavaScript(cmd.getTemplateSubject());
        final String templateBody = StringEscapeUtils.escapeJavaScript(cmd.getTemplateBody());
        final String locale = cmd.getLocale();

        final List<QuotaEmailTemplatesVO> templates = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(templateName);
        if (templates.size() == 1) {
            final QuotaEmailTemplatesVO template = templates.get(0);
            template.setTemplateSubject(templateSubject);
            template.setTemplateBody(templateBody);
            if (locale != null) {
                template.setLocale(locale);
            }
            return _quotaEmailTemplateDao.updateQuotaEmailTemplate(template);
        }
        return false;
    }

    @Override
    public QuotaBalanceResponse createQuotaLastBalanceResponse(List<QuotaBalanceVO> quotaBalance, Date startDate) {
        if (quotaBalance.size() == 0) {
            new InvalidParameterValueException("There are no balance entries on or before the requested date.");
        }
        if (startDate == null) {
            startDate = new Date();
        }
        QuotaBalanceResponse resp = new QuotaBalanceResponse();
        BigDecimal lastCredits = new BigDecimal(0);
        for (Iterator<QuotaBalanceVO> it = quotaBalance.iterator(); it.hasNext();) {
            QuotaBalanceVO entry = it.next();
            s_logger.info("createQuotaLastBalanceResponse Date=" + entry.getUpdatedOn() + " balance=" + entry.getCreditBalance() + " credit=" + entry.getCreditsId());
            lastCredits = lastCredits.add(entry.getCreditBalance());
        }
        resp.setStartQuota(lastCredits);
        resp.setStartDate(_quotaService.computeAdjustedTime(startDate));
        resp.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        resp.setObjectName("balance");
        return resp;
    }

    @Override
    public List<QuotaUsageVO> getQuotaUsage(QuotaStatementCmd cmd) {
        return _quotaService.getQuotaUsage(cmd.getAccountId(), cmd.getAccountName(), cmd.getDomainId(), cmd.getUsageType(), cmd.getStartDate(), cmd.getEndDate());
    }

    @Override
    public List<QuotaBalanceVO> getQuotaBalance(QuotaBalanceCmd cmd) {
        return _quotaService.getQuotaBalance(cmd.getAccountId(), cmd.getAccountName(), cmd.getDomainId(), cmd.getStartDate(), cmd.getEndDate());
    }

    @Override
    public Date startOfNextDay(Date dt) {
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, 1);
        dt = c.getTime();
        return dt;
    }

    @Override
    public Date startOfNextDay() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 1);
        Date dt = c.getTime();
        return dt;
    }

}
