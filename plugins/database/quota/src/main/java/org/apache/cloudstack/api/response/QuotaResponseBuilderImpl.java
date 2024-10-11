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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.cloud.utils.DateUtil;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.QuotaBalanceCmd;
import org.apache.cloudstack.api.command.QuotaConfigureEmailCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.api.command.QuotaPresetVariablesListCmd;
import org.apache.cloudstack.api.command.QuotaStatementCmd;
import org.apache.cloudstack.api.command.QuotaTariffCreateCmd;
import org.apache.cloudstack.api.command.QuotaTariffListCmd;
import org.apache.cloudstack.api.command.QuotaTariffUpdateCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.discovery.ApiDiscoveryService;
import org.apache.cloudstack.quota.QuotaManager;
import org.apache.cloudstack.quota.QuotaManagerImpl;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.QuotaStatement;
import org.apache.cloudstack.quota.activationrule.presetvariables.ComputingResources;
import org.apache.cloudstack.quota.activationrule.presetvariables.GenericPresetVariable;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariableDefinition;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.cloudstack.quota.activationrule.presetvariables.Value;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;

@Component
public class QuotaResponseBuilderImpl implements QuotaResponseBuilder {
    protected Logger logger = LogManager.getLogger(getClass());

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
    private QuotaAccountDao quotaAccountDao;
    @Inject
    private DomainDao _domainDao;
    @Inject
    private AccountManager _accountMgr;
    @Inject
    private QuotaStatement _statement;
    @Inject
    private QuotaManager _quotaManager;
    @Inject
    private QuotaEmailConfigurationDao quotaEmailConfigurationDao;

    private final Class<?>[] assignableClasses = {GenericPresetVariable.class, ComputingResources.class};

    @Inject
    private ApiDiscoveryService apiDiscoveryService;

    @Override
    public QuotaTariffResponse createQuotaTariffResponse(QuotaTariffVO tariff, boolean returnActivationRule) {
        final QuotaTariffResponse response = new QuotaTariffResponse();
        response.setUsageType(tariff.getUsageType());
        response.setUsageName(tariff.getUsageName());
        response.setUsageUnit(tariff.getUsageUnit());
        response.setUsageDiscriminator(tariff.getUsageDiscriminator());
        response.setTariffValue(tariff.getCurrencyValue());
        response.setEffectiveOn(tariff.getEffectiveOn());
        response.setUsageTypeDescription(tariff.getUsageTypeDescription());
        response.setCurrency(QuotaConfig.QuotaCurrencySymbol.value());
        response.setName(tariff.getName());
        response.setEndDate(tariff.getEndDate());
        response.setDescription(tariff.getDescription());
        response.setId(tariff.getUuid());
        response.setRemoved(tariff.getRemoved());
        response.setPosition(tariff.getPosition());
        if (returnActivationRule) {
            response.setActivationRule(tariff.getActivationRule());
        }
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
            Pair<List<QuotaAccountVO>, Integer> data = quotaAccountDao.listAllQuotaAccount(startIndex, pageSize);
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

    protected QuotaSummaryResponse getQuotaSummaryResponse(final Account account) {
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
            qr.setQuotaEnabled(QuotaConfig.QuotaAccountEnabled.valueIn(account.getId()));
            qr.setObjectName("summary");
            return qr;
        } else {
            return new QuotaSummaryResponse();
        }
    }

    public boolean isUserAllowedToSeeActivationRules(User user) {
        List<ApiDiscoveryResponse> apiList = (List<ApiDiscoveryResponse>) apiDiscoveryService.listApis(user, null).getResponses();
        return apiList.stream().anyMatch(response -> StringUtils.equalsAny(response.getName(), "quotaTariffCreate", "quotaTariffUpdate"));
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
                if (logger.isDebugEnabled()) {
                    logger.debug("createQuotaBalanceResponse: Entry=" + entry);
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
            if (logger.isDebugEnabled()) {
                logger.debug("createQuotaBalanceResponse: All Credit Entry=" + entry);
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
            if (logger.isDebugEnabled()) {
                logger.debug("createQuotaBalanceResponse: Start Entry=" + startItem);
                logger.debug("createQuotaBalanceResponse: End Entry=" + endItem);
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

        if (logger.isDebugEnabled()) {
            logger.debug(
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
        if (logger.isDebugEnabled()) {
            logger.debug("createQuotaStatementResponse record count=" + quotaUsage.size());
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
        Date startDate = cmd.getEffectiveDate();
        Date endDate = cmd.getEndDate();
        Integer usageType = cmd.getUsageType();
        String name = cmd.getName();
        boolean listAll = cmd.isListAll();
        Long startIndex = cmd.getStartIndex();
        Long pageSize = cmd.getPageSizeVal();
        String uuid = cmd.getId();
        boolean listOnlyRemoved = cmd.isListOnlyRemoved();
        String keyword = cmd.getKeyword();

        logger.debug("Listing quota tariffs for parameters [{}].", ReflectionToStringBuilderUtils.reflectOnlySelectedFields(cmd, "effectiveDate",
                "endDate", "listAll", "name", "page", "pageSize", "usageType", "uuid", "listOnlyRemoved", "keyword"));

        return _quotaTariffDao.listQuotaTariffs(startDate, endDate, usageType, name, uuid, listAll, listOnlyRemoved, startIndex, pageSize, keyword);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_QUOTA_TARIFF_UPDATE, eventDescription = "updating Quota Tariff")
    public QuotaTariffVO updateQuotaTariffPlan(QuotaTariffUpdateCmd cmd) {
        String name = cmd.getName();
        Double value = cmd.getValue();
        Date endDate = cmd.getEndDate();
        String description = cmd.getDescription();
        String activationRule = cmd.getActivationRule();
        Date now = new Date();
        Integer position = cmd.getPosition();

        warnQuotaTariffUpdateDeprecatedFields(cmd);

        QuotaTariffVO currentQuotaTariff = _quotaTariffDao.findByName(name);

        if (currentQuotaTariff == null) {
            throw new InvalidParameterValueException(String.format("There is no quota tariffs with name [%s].", name));
        }

        Date currentQuotaTariffStartDate = currentQuotaTariff.getEffectiveOn();

        currentQuotaTariff.setRemoved(now);

        QuotaTariffVO newQuotaTariff = persistNewQuotaTariff(currentQuotaTariff, name, 0, currentQuotaTariffStartDate, cmd.getEntityOwnerId(), endDate, value, description,
                activationRule, position);
        _quotaTariffDao.updateQuotaTariff(currentQuotaTariff);

        CallContext.current().setEventResourceId(newQuotaTariff.getId());

        return newQuotaTariff;
    }

    protected void warnQuotaTariffUpdateDeprecatedFields(QuotaTariffUpdateCmd cmd) {
        String warnMessage = "The parameter 's%s' for API 'quotaTariffUpdate' is no longer needed and it will be removed in future releases.";

        if (cmd.getStartDate() != null) {
            logger.warn(String.format(warnMessage,"startdate"));
        }

        if (cmd.getUsageType() != null) {
            logger.warn(String.format(warnMessage,"usagetype"));
        }
    }

    protected QuotaTariffVO persistNewQuotaTariff(QuotaTariffVO currentQuotaTariff, String name, int usageType, Date startDate, Long entityOwnerId, Date endDate, Double value,
            String description, String activationRule, Integer position) {

        QuotaTariffVO newQuotaTariff = getNewQuotaTariffObject(currentQuotaTariff, name, usageType);

        newQuotaTariff.setEffectiveOn(startDate);
        newQuotaTariff.setUpdatedOn(startDate);
        newQuotaTariff.setUpdatedBy(entityOwnerId);

        validateEndDateOnCreatingNewQuotaTariff(newQuotaTariff, startDate, endDate);
        validateValueOnCreatingNewQuotaTariff(newQuotaTariff, value);
        validateStringsOnCreatingNewQuotaTariff(newQuotaTariff::setDescription, description);
        validateStringsOnCreatingNewQuotaTariff(newQuotaTariff::setActivationRule, activationRule);
        validatePositionOnCreatingNewQuotaTariff(newQuotaTariff, position);

        _quotaTariffDao.addQuotaTariff(newQuotaTariff);
        return newQuotaTariff;
    }

    protected QuotaTariffVO getNewQuotaTariffObject(QuotaTariffVO currentQuotaTariff, String name, int usageType) {
        if (currentQuotaTariff != null) {
            return new QuotaTariffVO(currentQuotaTariff);
        }

        QuotaTariffVO newQuotaTariff = new QuotaTariffVO();

        if (!newQuotaTariff.setUsageTypeData(usageType)) {
            throw new InvalidParameterValueException(String.format("There is no usage type with value [%s].", usageType));
        }

        newQuotaTariff.setName(name);
        return newQuotaTariff;
    }

    protected void validatePositionOnCreatingNewQuotaTariff(QuotaTariffVO newQuotaTariff, Integer position) {
        if (position != null) {
            newQuotaTariff.setPosition(position);
        }
    }


    protected void validateStringsOnCreatingNewQuotaTariff(Consumer<String> method, String value){
        if (value != null) {
            method.accept(value.isBlank() ? null : value);
        }
    }

    protected void validateValueOnCreatingNewQuotaTariff(QuotaTariffVO newQuotaTariff, Double value) {
        if (value != null) {
            newQuotaTariff.setCurrencyValue(BigDecimal.valueOf(value));
        }
    }

    protected void validateEndDateOnCreatingNewQuotaTariff(QuotaTariffVO newQuotaTariff, Date startDate, Date endDate) {
        if (endDate == null) {
            return;
        }

        if (endDate.compareTo(startDate) < 0) {
            throw new InvalidParameterValueException(String.format("The quota tariff's end date [%s] cannot be less than the start date [%s].",
                    endDate, startDate));
        }

        Date now = new Date();
        if (endDate.compareTo(now) < 0) {
            throw new InvalidParameterValueException(String.format("The quota tariff's end date [%s] cannot be less than now [%s].",
                    endDate, now));
        }

        newQuotaTariff.setEndDate(endDate);
    }

    @Override
    public QuotaCreditsResponse addQuotaCredits(Long accountId, Long domainId, Double amount, Long updatedBy, Boolean enforce) {
        Date despositedOn = new Date();
        QuotaBalanceVO qb = _quotaBalanceDao.findLaterBalanceEntry(accountId, domainId, despositedOn);

        if (qb != null) {
            throw new InvalidParameterValueException(String.format("Incorrect deposit date [%s], as there are balance entries after this date.",
                    despositedOn));
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
        logger.debug("Depositing [{}] credits on adjusted date [{}]; current balance is [{}].", amount,
                DateUtil.displayDateInTimezone(QuotaManagerImpl.getUsageAggregationTimeZone(), despositedOn), currentAccountBalance);
        // update quota account with the balance
        _quotaService.saveQuotaAccount(account, currentAccountBalance, despositedOn);
        if (lockAccountEnforcement) {
            if (currentAccountBalance.compareTo(new BigDecimal(0)) >= 0) {
                if (account.getState() == Account.State.LOCKED) {
                    logger.info("UnLocking account " + account.getAccountName() + " , due to positive balance " + currentAccountBalance);
                    _accountMgr.enableAccount(account.getAccountName(), domainId, accountId);
                }
            } else { // currentAccountBalance < 0 then lock the account
                if (_quotaManager.isLockable(account) && account.getState() == Account.State.ENABLED && enforce) {
                    logger.info("Locking account " + account.getAccountName() + " , due to negative balance " + currentAccountBalance);
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
        final String templateSubject = cmd.getTemplateSubject();
        final String templateBody = cmd.getTemplateBody();
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
            logger.debug("createQuotaLastBalanceResponse Date={} balance={} credit={}",
                    DateUtil.displayDateInTimezone(QuotaManagerImpl.getUsageAggregationTimeZone(), entry.getUpdatedOn()),
                    entry.getCreditBalance(), entry.getCreditsId());

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

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_QUOTA_TARIFF_CREATE, eventDescription = "creating Quota Tariff")
    public QuotaTariffVO createQuotaTariff(QuotaTariffCreateCmd cmd) {
        String name = cmd.getName();
        int usageType = cmd.getUsageType();
        Date startDate = cmd.getStartDate();
        Date now = new Date();
        startDate = startDate == null ? now : startDate;
        Date endDate = cmd.getEndDate();
        Double value = cmd.getValue();
        String description = cmd.getDescription();
        String activationRule = cmd.getActivationRule();
        Integer position = ObjectUtils.defaultIfNull(cmd.getPosition(), 1);

        QuotaTariffVO currentQuotaTariff = _quotaTariffDao.findByName(name);

        if (currentQuotaTariff != null) {
            throw new InvalidParameterValueException(String.format("A quota tariff with name [%s] already exist.", name));
        }

        if (startDate.compareTo(now) < 0) {
            throw new InvalidParameterValueException(String.format("The value passed as Quota tariff's start date is in the past: [%s]. " +
                    "Please, inform a date in the future or do not pass the parameter to use the current date and time.", startDate));
        }

        QuotaTariffVO newQuotaTariff = persistNewQuotaTariff(null, name, usageType, startDate, cmd.getEntityOwnerId(), endDate, value, description, activationRule, position);

        CallContext.current().setEventResourceId(newQuotaTariff.getId());

        return newQuotaTariff;
    }

    @ActionEvent(eventType = EventTypes.EVENT_QUOTA_TARIFF_DELETE, eventDescription = "removing Quota Tariff")
    public boolean deleteQuotaTariff(String quotaTariffUuid) {
        QuotaTariffVO quotaTariff = _quotaTariffDao.findByUuid(quotaTariffUuid);

        if (quotaTariff == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Quota tariff with the provided UUID does not exist.");
        }

        quotaTariff.setRemoved(new Date());
        CallContext.current().setEventResourceId(quotaTariff.getId());
        return _quotaTariffDao.updateQuotaTariff(quotaTariff);
    }

    @Override
    public List<QuotaPresetVariablesItemResponse> listQuotaPresetVariables(QuotaPresetVariablesListCmd cmd) {
        List<QuotaPresetVariablesItemResponse> response;
        List<Pair<String, String>> variables = new ArrayList<>();

        QuotaTypes quotaType = cmd.getQuotaType();
        addAllPresetVariables(PresetVariables.class, quotaType, variables, null);
        response = createQuotaPresetVariablesResponse(variables);

        return response;
    }

    /**
     * Adds all preset variables for the given quota type. It recursively finds all presets variables for the given {@link Class} and puts it in a {@link List}. Each item in the
     * list is a {@link Pair} that consists of the variable name and its description.
     *
     * @param clazz used to find the non-transient fields. If it is equal to the {@link Value} class, then it only gets the declared fields, otherwise, it gets all fields,
     *              including its parent's fields.
     * @param quotaType used to check if the field supports the quota resource type. It uses the annotation method {@link PresetVariableDefinition#supportedTypes()} for this
     *                  verification.
     * @param variables the {@link List} which contains the {@link Pair} of the preset variable and its description.
     * @param recursiveVariableName {@link String} used for recursively building the preset variable string.
     */
    public void addAllPresetVariables(Class<?> clazz, QuotaTypes quotaType, List<Pair<String, String>> variables, String recursiveVariableName) {
        Field[] allFields = Value.class.equals(clazz) ? clazz.getDeclaredFields() : FieldUtils.getAllFields(clazz);
        List<Field> fieldsNonTransients = Arrays.stream(allFields).filter(field -> !Modifier.isTransient(field.getModifiers())).collect(Collectors.toList());
        for (Field field : fieldsNonTransients) {
            PresetVariableDefinition presetVariableDefinitionAnnotation = field.getAnnotation(PresetVariableDefinition.class);
            Class<?> fieldClass = getClassOfField(field);
            String presetVariableName = field.getName();

            if (presetVariableDefinitionAnnotation == null) {
                continue;
            }

            if (StringUtils.isNotEmpty(recursiveVariableName)) {
                presetVariableName = String.format("%s.%s", recursiveVariableName, field.getName());
            }
            filterSupportedTypes(variables, quotaType, presetVariableDefinitionAnnotation, fieldClass, presetVariableName);
        }
    }

    /**
     * Returns the class of the {@link Field} depending on its type. This method is required for retrieving the Class of Generic Types, i.e. {@link List}.
     */
    protected Class<?> getClassOfField(Field field){
        if (field.getGenericType() instanceof ParameterizedType) {
            ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            return (Class<?>) genericType.getActualTypeArguments()[0];
        }

        return field.getType();
    }

    /**
     * Checks if the {@link PresetVariableDefinition} supports the given {@link QuotaTypes}. If it supports it, it adds the preset variable to the {@link List} recursively
     * if it is from the one of the classes in the {@link QuotaResponseBuilderImpl#assignableClasses} array or directly if not.
     *
     * @param variables {@link List} of the {@link Pair} of the preset variable and its description.
     * @param quotaType the given {@link QuotaTypes} to filter.
     * @param presetVariableDefinitionAnnotation used to check if the quotaType is supported.
     * @param fieldClass class of the field used to verify if it is from the {@link GenericPresetVariable} or {@link ComputingResources} classes. If it is, then it calls
     *                   {@link QuotaResponseBuilderImpl#addAllPresetVariables(Class, QuotaTypes, List, String)} to add the preset variable. Otherwise, the {@link Pair} is
     *                   added directly to the variables {@link List}.
     * @param presetVariableName {@link String} that contains the recursive created preset variable name.
     */
    public void filterSupportedTypes(List<Pair<String, String>> variables, QuotaTypes quotaType, PresetVariableDefinition presetVariableDefinitionAnnotation, Class<?> fieldClass,
                                     String presetVariableName) {
        if (Arrays.stream(presetVariableDefinitionAnnotation.supportedTypes()).noneMatch(supportedType ->
                supportedType == quotaType.getQuotaType() || supportedType == 0)) {
            return;
        }

        String presetVariableDescription = presetVariableDefinitionAnnotation.description();

        Pair<String, String> pair = new Pair<>(presetVariableName, presetVariableDescription);
        variables.add(pair);

        if (isRecursivePresetVariable(fieldClass)) {
            addAllPresetVariables(fieldClass, quotaType, variables, presetVariableName);
        }
    }

    /**
     * Returns true if the {@link Class} of the {@link Field} is from one of the classes in the array {@link QuotaResponseBuilderImpl#assignableClasses}, i.e., it is a recursive
     * {@link PresetVariables}, returns false otherwise.
     */
    private boolean isRecursivePresetVariable(Class<?> fieldClass) {
        for (Class<?> clazz : assignableClasses) {
            if (clazz.isAssignableFrom(fieldClass)) {
                return true;
            }
        }
        return false;
    }

    public List<QuotaPresetVariablesItemResponse> createQuotaPresetVariablesResponse(List<Pair<String, String>> variables) {
        final List<QuotaPresetVariablesItemResponse> responses = new ArrayList<>();

        for (Pair<String, String> variable : variables) {
            responses.add(createPresetVariablesItemResponse(variable));
        }

        return responses;
    }

    public QuotaPresetVariablesItemResponse createPresetVariablesItemResponse(Pair<String, String> variable) {
        QuotaPresetVariablesItemResponse response = new QuotaPresetVariablesItemResponse();
        response.setVariable(variable.first());
        response.setDescription(variable.second());
        return response;
    }

    @Override
    public Pair<QuotaEmailConfigurationVO, Double> configureQuotaEmail(QuotaConfigureEmailCmd cmd) {
        validateQuotaConfigureEmailCmdParameters(cmd);

        Double minBalance = cmd.getMinBalance();

        if (minBalance != null) {
            _quotaService.setMinBalance(cmd.getAccountId(), cmd.getMinBalance());
        }

        QuotaEmailConfigurationVO configurationVO = getQuotaEmailConfigurationVo(cmd);
        return new Pair<>(configurationVO, minBalance);
    }

    protected QuotaEmailConfigurationVO getQuotaEmailConfigurationVo(QuotaConfigureEmailCmd cmd) {
        if (cmd.getTemplateName() == null) {
            return null;
        }

        List<QuotaEmailTemplatesVO> templateVO = _quotaEmailTemplateDao.listAllQuotaEmailTemplates(cmd.getTemplateName());
        if (templateVO.isEmpty()) {
            throw new InvalidParameterValueException(String.format("Could not find template with name [%s].", cmd.getTemplateName()));
        }
        long templateId = templateVO.get(0).getId();
        QuotaEmailConfigurationVO configurationVO = quotaEmailConfigurationDao.findByAccountIdAndEmailTemplateId(cmd.getAccountId(), templateId);

        if (configurationVO == null) {
            configurationVO = new QuotaEmailConfigurationVO(cmd.getAccountId(), templateId, cmd.getEnable());
            quotaEmailConfigurationDao.persistQuotaEmailConfiguration(configurationVO);
            return configurationVO;
        }

        configurationVO.setEnabled(cmd.getEnable());
        return quotaEmailConfigurationDao.updateQuotaEmailConfiguration(configurationVO);
    }

    protected void validateQuotaConfigureEmailCmdParameters(QuotaConfigureEmailCmd cmd) {
        if (quotaAccountDao.findByIdQuotaAccount(cmd.getAccountId()) == null) {
            throw new InvalidParameterValueException("You must have the quota enabled for this account to configure quota emails.");
        }

        if (cmd.getTemplateName() == null && cmd.getMinBalance() == null) {
            throw new InvalidParameterValueException("You should inform at least the 'minbalance' or both the 'templatename' and 'enable' parameters.");
        }

        if ((cmd.getTemplateName() != null && cmd.getEnable() == null) || (cmd.getTemplateName() == null && cmd.getEnable() != null)) {
            throw new InvalidParameterValueException("Parameter 'enable' must be informed along with 'templatename'.");
        }
    }

    public QuotaConfigureEmailResponse createQuotaConfigureEmailResponse(QuotaEmailConfigurationVO quotaEmailConfigurationVO, Double minBalance, long accountId) {
        QuotaConfigureEmailResponse quotaConfigureEmailResponse = new QuotaConfigureEmailResponse();

        Account account = _accountDao.findByIdIncludingRemoved(accountId);
        if (quotaEmailConfigurationVO != null) {
            QuotaEmailTemplatesVO templateVO = _quotaEmailTemplateDao.findById(quotaEmailConfigurationVO.getEmailTemplateId());

            quotaConfigureEmailResponse.setAccountId(account.getUuid());
            quotaConfigureEmailResponse.setTemplateName(templateVO.getTemplateName());
            quotaConfigureEmailResponse.setEnabled(quotaEmailConfigurationVO.isEnabled());
        }

        quotaConfigureEmailResponse.setMinBalance(minBalance);

        return quotaConfigureEmailResponse;
    }

    @Override
    public List<QuotaConfigureEmailResponse> listEmailConfiguration(long accountId) {
        List<QuotaEmailConfigurationVO> emailConfigurationVOList = quotaEmailConfigurationDao.listByAccount(accountId);
        Account account = _accountDao.findById(accountId);
        QuotaAccountVO quotaAccountVO = quotaAccountDao.findByIdQuotaAccount(accountId);

        List<QuotaConfigureEmailResponse> quotaConfigureEmailResponseList = new ArrayList<>();
        for (QuotaEmailConfigurationVO quotaEmailConfigurationVO : emailConfigurationVOList) {
            quotaConfigureEmailResponseList.add(createQuotaConfigureEmailResponse(quotaEmailConfigurationVO, account, quotaAccountVO));
        }

        return quotaConfigureEmailResponseList;
    }

    protected QuotaConfigureEmailResponse createQuotaConfigureEmailResponse(QuotaEmailConfigurationVO quotaEmailConfigurationVO, Account account, QuotaAccountVO quotaAccountVO) {
        QuotaConfigureEmailResponse quotaConfigureEmailResponse = new QuotaConfigureEmailResponse();

        QuotaEmailTemplatesVO templateVO = _quotaEmailTemplateDao.findById(quotaEmailConfigurationVO.getEmailTemplateId());

        quotaConfigureEmailResponse.setAccountId(account.getUuid());
        quotaConfigureEmailResponse.setTemplateName(templateVO.getTemplateName());
        quotaConfigureEmailResponse.setEnabled(quotaEmailConfigurationVO.isEnabled());

        quotaConfigureEmailResponse.setMinBalance(quotaAccountVO.getQuotaMinBalance().doubleValue());

        return quotaConfigureEmailResponse;
    }
}
