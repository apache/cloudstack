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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.quota.QuotaManager;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.QuotaStatement;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;

@Component
public class QuotaResponseBuilderImpl implements QuotaResponseBuilder {
    private static final Logger s_logger = Logger.getLogger(QuotaResponseBuilderImpl.class);

    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private QuotaCreditsDao _quotaCreditsDao;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private QuotaEmailTemplatesDao _quotaEmailTemplateDao;

    @Inject
    private UserDao _userDao;
    @Inject
    private QuotaService _quotaService;
    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAccountDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private QuotaStatement _statement;
    @Inject
    private QuotaManager _quotaManager;

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
    public Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(final String accountName, final Long domainId) {
        List<QuotaSummaryResponse> result = new ArrayList<QuotaSummaryResponse>();

        if (accountName != null && domainId != null) {
            Account account = _accountDao.findActiveAccount(accountName, domainId);
            QuotaSummaryResponse qr = getQuotaSummaryResponse(account);
            result.add(qr);
        }

        return new Pair<>(result, result.size());
    }

    @Override
    public Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(Boolean listAll) {
        return createQuotaSummaryResponse(listAll, null, null, null);
    }

    @Override
    public Pair<List<QuotaSummaryResponse>, Integer> createQuotaSummaryResponse(Boolean listAll, final String keyword, final Long startIndex, final Long pageSize) {
        List<QuotaSummaryResponse> result = new ArrayList<QuotaSummaryResponse>();
        Integer count = 0;
        if (listAll) {
            Filter filter = new Filter(AccountVO.class, "accountName", true, startIndex, pageSize);
            Pair<List<AccountVO>, Integer> data = _accountDao.findAccountsLike(keyword, filter);
            count = data.second();
            for (final AccountVO account : data.first()) {
                QuotaSummaryResponse qr = getQuotaSummaryResponse(account);
                result.add(qr);
            }
        } else {
            Pair<List<QuotaAccountVO>, Integer> data = _quotaAccountDao.listAllQuotaAccount(startIndex, pageSize);
            count = data.second();
            for (final QuotaAccountVO quotaAccount : data.first()) {
                AccountVO account = _accountDao.findById(quotaAccount.getId());
                if (account == null) {
                    continue;
                }
                QuotaSummaryResponse qr = getQuotaSummaryResponse(account);
                result.add(qr);
            }
        }
        return new Pair<>(result, count);
    }

    private QuotaSummaryResponse getQuotaSummaryResponse(final Account account) {
        Calendar[] period = _statement.getCurrentStatementTime();

        if (account != null) {
            QuotaSummaryResponse qr = new QuotaSummaryResponse();
            DomainVO domain = _domainDao.findById(account.getDomainId());
            BigDecimal curBalance = _quotaBalanceDao.lastQuotaBalance(account.getAccountId(), account.getDomainId(), period[1].getTime());
            BigDecimal quotaUsage = _quotaUsageDao.findTotalQuotaUsage(account.getAccountId(), account.getDomainId(), null, period[0].getTime(), period[1].getTime());

            qr.setAccountId(account.getUuid());
            qr.setAccountName(account.getAccountName());
            qr.setDomainId(domain.getUuid());
            qr.setDomainName(domain.getName());
            qr.setBalance(curBalance);
            qr.setQuotaUsage(quotaUsage);
            qr.setState(account.getState());
            qr.setStartDate(period[0].getTime());
            qr.setEndDate(period[1].getTime());
            qr.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
            qr.setObjectName("summary");
            return qr;
        } else {
            return new QuotaSummaryResponse();
        }
    }

    @Override
    public QuotaBalanceResponse createQuotaBalanceResponse(List<QuotaBalanceVO> quotaBalance, Date startDate, Date endDate) {
        if (quotaBalance == null || quotaBalance.isEmpty()) {
            throw new InvalidParameterValueException("The request period does not contain balance entries.");
        }
        Collections.sort(quotaBalance, new Comparator<QuotaBalanceVO>() {
            @Override
            public int compare(QuotaBalanceVO o1, QuotaBalanceVO o2) {
                o1 = o1 == null ? new QuotaBalanceVO() : o1;
                o2 = o2 == null ? new QuotaBalanceVO() : o2;
                return o2.getUpdatedOn().compareTo(o1.getUpdatedOn()); // desc
            }
        });

        boolean have_balance_entries = false;
        //check that there is at least one balance entry
        for (Iterator<QuotaBalanceVO> it = quotaBalance.iterator(); it.hasNext();) {
            QuotaBalanceVO entry = it.next();
            if (entry.isBalanceEntry()) {
                have_balance_entries = true;
                break;
            }
        }
        //if last entry is a credit deposit then remove that as that is already
        //accounted for in the starting balance after that entry, note the sort is desc
        if (have_balance_entries) {
            ListIterator<QuotaBalanceVO> li = quotaBalance.listIterator(quotaBalance.size());
            // Iterate in reverse.
            while (li.hasPrevious()) {
                QuotaBalanceVO entry = li.previous();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("createQuotaBalanceResponse: Entry=" + entry);
                }
                if (entry.getCreditsId() > 0) {
                    li.remove();
                } else {
                    break;
                }
            }
        }

        int quota_activity = quotaBalance.size();
        QuotaBalanceResponse resp = new QuotaBalanceResponse();
        BigDecimal lastCredits = new BigDecimal(0);
        boolean consecutive = true;
        for (Iterator<QuotaBalanceVO> it = quotaBalance.iterator(); it.hasNext();) {
            QuotaBalanceVO entry = it.next();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("createQuotaBalanceResponse: All Credit Entry=" + entry);
            }
            if (entry.getCreditsId() > 0) {
                if (consecutive) {
                    lastCredits = lastCredits.add(entry.getCreditBalance());
                }
                resp.addCredits(entry);
                it.remove();
            } else {
                consecutive = false;
            }
        }

        if (quota_activity > 0 && quotaBalance.size() > 0) {
            // order is desc last item is the start item
            QuotaBalanceVO startItem = quotaBalance.get(quotaBalance.size() - 1);
            QuotaBalanceVO endItem = quotaBalance.get(0);
            resp.setStartDate(startDate);
            resp.setStartQuota(startItem.getCreditBalance());
            resp.setEndDate(endDate);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("createQuotaBalanceResponse: Start Entry=" + startItem);
                s_logger.debug("createQuotaBalanceResponse: End Entry=" + endItem);
            }
            resp.setEndQuota(endItem.getCreditBalance().add(lastCredits));
        } else if (quota_activity > 0) {
            // order is desc last item is the start item
            resp.setStartDate(startDate);
            resp.setStartQuota(new BigDecimal(0));
            resp.setEndDate(endDate);
            resp.setEndQuota(new BigDecimal(0).add(lastCredits));
        } else {
            resp.setStartDate(startDate);
            resp.setEndDate(endDate);
            resp.setStartQuota(new BigDecimal(0));
            resp.setEndQuota(new BigDecimal(0));
        }
        resp.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        resp.setObjectName("balance");
        return resp;
    }

    @Override
    public QuotaStatementResponse createQuotaStatementResponse(final List<QuotaUsageVO> quotaUsage) {
        if (quotaUsage == null || quotaUsage.isEmpty()) {
            throw new InvalidParameterValueException("There is no usage data found for period mentioned.");
        }

        QuotaStatementResponse statement = new QuotaStatementResponse();

        HashMap<Integer, QuotaTypes> quotaTariffMap = new HashMap<Integer, QuotaTypes>();
        Collection<QuotaTypes> result = QuotaTypes.listQuotaTypes().values();

        for (QuotaTypes quotaTariff : result) {
            quotaTariffMap.put(quotaTariff.getQuotaType(), quotaTariff);
            // add dummy record for each usage type
            QuotaUsageVO dummy = new QuotaUsageVO(quotaUsage.get(0));
            dummy.setUsageType(quotaTariff.getQuotaType());
            dummy.setQuotaUsed(new BigDecimal(0));
            quotaUsage.add(dummy);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug(
                    "createQuotaStatementResponse Type=" + quotaUsage.get(0).getUsageType() + " usage=" + quotaUsage.get(0).getQuotaUsed().setScale(2, RoundingMode.HALF_EVEN)
                    + " rec.id=" + quotaUsage.get(0).getUsageItemId() + " SD=" + quotaUsage.get(0).getStartDate() + " ED=" + quotaUsage.get(0).getEndDate());
        }

        Collections.sort(quotaUsage, new Comparator<QuotaUsageVO>() {
            @Override
            public int compare(QuotaUsageVO o1, QuotaUsageVO o2) {
                if (o1.getUsageType() == o2.getUsageType()) {
                    return 0;
                }
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
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("createQuotaStatementResponse record count=" + quotaUsage.size());
        }
        for (final QuotaUsageVO quotaRecord : quotaUsage) {
            if (type != quotaRecord.getUsageType()) {
                if (type != -1) {
                    lineitem = new QuotaStatementItemResponse(type);
                    lineitem.setQuotaUsed(usage);
                    lineitem.setAccountId(prev.getAccountId());
                    lineitem.setDomainId(prev.getDomainId());
                    lineitem.setUsageUnit(quotaTariffMap.get(type).getQuotaUnit());
                    lineitem.setUsageName(quotaTariffMap.get(type).getQuotaName());
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

        statement.setLineItem(items);
        statement.setTotalQuota(totalUsage);
        statement.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        statement.setObjectName("statement");
        return statement;
    }

    @Override
    public Pair<List<QuotaTariffVO>, Integer> listQuotaTariffPlans(final QuotaTariffListCmd cmd) {
        Pair<List<QuotaTariffVO>, Integer> result;
        Date effectiveDate = cmd.getEffectiveDate() == null ? new Date() : cmd.getEffectiveDate();
        Date adjustedEffectiveDate = _quotaService.computeAdjustedTime(effectiveDate);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Effective datec=" + effectiveDate + " quotatype=" + cmd.getUsageType() + " Adjusted date=" + adjustedEffectiveDate);
        }
        if (cmd.getUsageType() != null) {
            QuotaTariffVO tariffPlan = _quotaTariffDao.findTariffPlanByUsageType(cmd.getUsageType(), adjustedEffectiveDate);
            if (tariffPlan != null) {
                List<QuotaTariffVO> list = new ArrayList<>();
                list.add(tariffPlan);
                result = new Pair<>(list, list.size());
            } else {
                result = new Pair<>(new ArrayList<>(), 0);
            }
        } else {
            result = _quotaTariffDao.listAllTariffPlans(adjustedEffectiveDate, cmd.getStartIndex(), cmd.getPageSizeVal());
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

        QuotaTariffVO result = null;
        result = new QuotaTariffVO(quotaType);
        result.setUsageName(quotaConstant.getQuotaName());
        result.setUsageUnit(quotaConstant.getQuotaUnit());
        result.setUsageDiscriminator(quotaConstant.getDiscriminator());
        result.setCurrencyValue(quotaCost);
        result.setEffectiveOn(effectiveDate);
        result.setUpdatedOn(now);
        result.setUpdatedBy(cmd.getEntityOwnerId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("Updating Quota Tariff Plan: New value=%s for resource type=%d effective on date=%s", quotaCost, quotaType, effectiveDate));
        }
        _quotaTariffDao.addQuotaTariff(result);

        return result;
    }

    @Override
    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Double amount, Long updatedBy, Boolean enforce) {
        Date despositedOn = _quotaService.computeAdjustedTime(new Date());
        QuotaBalanceVO qb = _quotaBalanceDao.findLaterBalanceEntry(accountId, domainId, despositedOn);

        if (qb != null) {
            throw new InvalidParameterValueException("Incorrect deposit date: " + despositedOn + " there are balance entries after this date");
        }

        QuotaCreditsVO credits = new QuotaCreditsVO(accountId, domainId, new BigDecimal(amount), updatedBy);
        credits.setUpdatedOn(despositedOn);
        QuotaCreditsVO result = _quotaCreditsDao.saveCredits(credits);

        final AccountVO account = _accountDao.findById(accountId);
        if (account == null) {
            throw new InvalidParameterValueException("Account does not exist with account id " + accountId);
        }
        final boolean lockAccountEnforcement = "true".equalsIgnoreCase(QuotaConfig.QuotaEnableEnforcement.value());
        final BigDecimal currentAccountBalance = _quotaBalanceDao.lastQuotaBalance(accountId, domainId, startOfNextDay(new Date(despositedOn.getTime())));
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("AddQuotaCredits: Depositing " + amount + " on adjusted date " + despositedOn + ", current balance " + currentAccountBalance);
        }
        // update quota account with the balance
        _quotaService.saveQuotaAccount(account, currentAccountBalance, despositedOn);
        if (lockAccountEnforcement) {
            if (currentAccountBalance.compareTo(new BigDecimal(0)) >= 0) {
                if (account.getState() == Account.State.LOCKED) {
                    s_logger.info("UnLocking account " + account.getAccountName() + " , due to positive balance " + currentAccountBalance);
                    _accountMgr.enableAccount(account.getAccountName(), domainId, accountId);
                }
            } else { // currentAccountBalance < 0 then lock the account
                if (_quotaManager.isLockable(account) && account.getState() == Account.State.ENABLED && enforce) {
                    s_logger.info("Locking account " + account.getAccountName() + " , due to negative balance " + currentAccountBalance);
                    _accountMgr.lockAccount(account.getAccountName(), domainId, accountId);
                }
            }
        }

        String creditor = String.valueOf(Account.ACCOUNT_ID_SYSTEM);
        User creditorUser = _userDao.getUser(updatedBy);
        if (creditorUser != null) {
            creditor = creditorUser.getUsername();
        }
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
        if (quotaBalance == null) {
            throw new InvalidParameterValueException("There are no balance entries on or before the requested date.");
        }
        if (startDate == null) {
            startDate = new Date();
        }
        QuotaBalanceResponse resp = new QuotaBalanceResponse();
        BigDecimal lastCredits = new BigDecimal(0);
        for (QuotaBalanceVO entry : quotaBalance) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("createQuotaLastBalanceResponse Date=" + entry.getUpdatedOn() + " balance=" + entry.getCreditBalance() + " credit=" + entry.getCreditsId());
            }
            lastCredits = lastCredits.add(entry.getCreditBalance());
        }
        resp.setStartQuota(lastCredits);
        resp.setStartDate(startDate);
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
        return _quotaService.findQuotaBalanceVO(cmd.getAccountId(), cmd.getAccountName(), cmd.getDomainId(), cmd.getStartDate(), cmd.getEndDate());
    }
    @Override
    public Date startOfNextDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return createDateAtTheStartOfNextDay(localDate);
    }

    @Override
    public Date startOfNextDay() {
        LocalDate localDate = LocalDate.now();
        return createDateAtTheStartOfNextDay(localDate);
    }

    private Date createDateAtTheStartOfNextDay(LocalDate localDate) {
        LocalDate nextDayLocalDate = localDate.plusDays(1);
        return Date.from(nextDayLocalDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

}