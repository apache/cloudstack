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
package org.apache.cloudstack.quota;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import com.cloud.user.Account;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.activationrule.presetvariables.GenericPresetVariable;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariableHelper;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.usage.UsageTypes;
import org.apache.cloudstack.usage.UsageUnitTypes;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;

@Component
public class QuotaManagerImpl extends ManagerBase implements QuotaManager {
    private static final Logger s_logger = Logger.getLogger(QuotaManagerImpl.class.getName());

    @Inject
    private AccountDao _accountDao;
    @Inject
    private QuotaAccountDao _quotaAcc;
    @Inject
    private UsageDao _usageDao;
    @Inject
    private QuotaTariffDao _quotaTariffDao;
    @Inject
    private QuotaUsageDao _quotaUsageDao;
    @Inject
    private QuotaBalanceDao _quotaBalanceDao;
    @Inject
    private ConfigurationDao _configDao;

    @Inject
    protected PresetVariableHelper presetVariableHelper;

    private TimeZone _usageTimezone;
    private int _aggregationDuration = 0;

    static final BigDecimal s_hoursInMonth = BigDecimal.valueOf(DateUtil.HOURS_IN_A_MONTH);
    static final BigDecimal GiB_DECIMAL = BigDecimal.valueOf(ByteScaleUtils.GiB);
    List<Account.Type> lockablesAccountTypes = Arrays.asList(Account.Type.NORMAL, Account.Type.DOMAIN_ADMIN);

    List<Integer> usageTypesToAvoidCalculation = Arrays.asList(UsageTypes.VM_DISK_IO_READ, UsageTypes.VM_DISK_IO_WRITE, UsageTypes.VM_DISK_BYTES_READ,
            UsageTypes.VM_DISK_BYTES_WRITE);

    public QuotaManagerImpl() {
        super();
    }

    private void mergeConfigs(Map<String, String> dbParams, Map<String, Object> xmlParams) {
        for (Map.Entry<String, Object> param : xmlParams.entrySet()) {
            dbParams.put(param.getKey(), (String)param.getValue());
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        Map<String, String> configs = _configDao.getConfiguration(params);

        if (params != null) {
            mergeConfigs(configs, params);
        }

        String aggregationRange = configs.get("usage.stats.job.aggregation.range");
        String timeZoneStr = configs.get("usage.aggregation.timezone");

        if (timeZoneStr == null) {
            timeZoneStr = "GMT";
        }
        _usageTimezone = TimeZone.getTimeZone(timeZoneStr);

        _aggregationDuration = Integer.parseInt(aggregationRange);
        if (_aggregationDuration < UsageUtils.USAGE_AGGREGATION_RANGE_MIN) {
            s_logger.warn("Usage stats job aggregation range is to small, using the minimum value of " + UsageUtils.USAGE_AGGREGATION_RANGE_MIN);
            _aggregationDuration = UsageUtils.USAGE_AGGREGATION_RANGE_MIN;
        }
        s_logger.info("Usage timezone = " + _usageTimezone + " AggregationDuration=" + _aggregationDuration);

        return true;
    }

    @Override
    public boolean start() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Starting Quota Manager");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (s_logger.isInfoEnabled()) {
            s_logger.info("Stopping Quota Manager");
        }
        return true;
    }

    protected void processQuotaBalanceForAccount(AccountVO accountVo, List<QuotaUsageVO> accountQuotaUsages) {
        String accountToString = accountVo.reflectionToString();

        if (CollectionUtils.isEmpty(accountQuotaUsages)) {
            s_logger.info(String.format("Account [%s] does not have quota usages to process. Skipping it.", accountToString));
            return;
        }

        QuotaUsageVO firstQuotaUsage = accountQuotaUsages.get(0);
        Date startDate = firstQuotaUsage.getStartDate();
        Date endDate = firstQuotaUsage.getStartDate();

        s_logger.info(String.format("Processing quota balance for account [%s] between [%s] and [%s].", accountToString, startDate,
                accountQuotaUsages.get(accountQuotaUsages.size() - 1).getEndDate()));

        BigDecimal aggregatedUsage = BigDecimal.ZERO;
        long accountId = accountVo.getAccountId();
        long domainId = accountVo.getDomainId();

        aggregatedUsage = getUsageValueAccordingToLastQuotaUsageEntryAndLastQuotaBalance(accountId, domainId, startDate, endDate, aggregatedUsage, accountToString);

        for (QuotaUsageVO quotaUsage : accountQuotaUsages) {
            Date quotaUsageStartDate = quotaUsage.getStartDate();
            Date quotaUsageEndDate = quotaUsage.getEndDate();
            BigDecimal quotaUsed = quotaUsage.getQuotaUsed();

            if (quotaUsed.equals(BigDecimal.ZERO)) {
                aggregatedUsage = aggregatedUsage.add(aggregateCreditBetweenDates(accountId, domainId, quotaUsageStartDate, quotaUsageEndDate, accountToString));
                continue;
            }

            if (startDate.compareTo(quotaUsageStartDate) == 0) {
                aggregatedUsage = aggregatedUsage.subtract(quotaUsed);
                continue;
            }

            _quotaBalanceDao.saveQuotaBalance(new QuotaBalanceVO(accountId, domainId, aggregatedUsage, endDate));

            aggregatedUsage = BigDecimal.ZERO;
            startDate = quotaUsageStartDate;
            endDate = quotaUsageEndDate;

            QuotaBalanceVO lastRealBalanceEntry = _quotaBalanceDao.findLastBalanceEntry(accountId, domainId, endDate);
            Date lastBalanceDate = new Date(0);

            if (lastRealBalanceEntry != null) {
                lastBalanceDate = lastRealBalanceEntry.getUpdatedOn();
                aggregatedUsage = aggregatedUsage.add(lastRealBalanceEntry.getCreditBalance());
            }

            aggregatedUsage = aggregatedUsage.add(aggregateCreditBetweenDates(accountId, domainId, lastBalanceDate, endDate, accountToString));
            aggregatedUsage = aggregatedUsage.subtract(quotaUsed);
        }

        _quotaBalanceDao.saveQuotaBalance(new QuotaBalanceVO(accountId, domainId, aggregatedUsage, endDate));
        saveQuotaAccount(accountId, aggregatedUsage, endDate);
    }

    protected BigDecimal getUsageValueAccordingToLastQuotaUsageEntryAndLastQuotaBalance(long accountId, long domainId, Date startDate, Date endDate, BigDecimal aggregatedUsage,
            String accountToString) {
        QuotaUsageVO lastQuotaUsage = _quotaUsageDao.findLastQuotaUsageEntry(accountId, domainId, startDate);

        if (lastQuotaUsage == null) {
            aggregatedUsage = aggregatedUsage.add(aggregateCreditBetweenDates(accountId, domainId, new Date(0), startDate, accountToString));
            QuotaBalanceVO firstBalance = new QuotaBalanceVO(accountId, domainId, aggregatedUsage, startDate);

            s_logger.debug(String.format("Persisting the first quota balance [%s] for account [%s].", firstBalance, accountToString));
            _quotaBalanceDao.saveQuotaBalance(firstBalance);
        } else {
            QuotaBalanceVO lastRealBalance = _quotaBalanceDao.findLastBalanceEntry(accountId, domainId, endDate);

            if (lastRealBalance != null) {
                aggregatedUsage = aggregatedUsage.add(lastRealBalance.getCreditBalance());
                aggregatedUsage = aggregatedUsage.add(aggregateCreditBetweenDates(accountId, domainId, lastRealBalance.getUpdatedOn(), endDate, accountToString));
            } else {
                s_logger.warn(String.format("Account [%s] has quota usage entries, however it does not have a quota balance.", accountToString));
            }
        }

        return aggregatedUsage;
    }

    protected void saveQuotaAccount(long accountId, BigDecimal aggregatedUsage, Date endDate) {
        QuotaAccountVO quotaAccount = _quotaAcc.findByIdQuotaAccount(accountId);

        if (quotaAccount != null) {
            quotaAccount.setQuotaBalance(aggregatedUsage);
            quotaAccount.setQuotaBalanceDate(endDate);
            _quotaAcc.updateQuotaAccount(accountId, quotaAccount);
            return;
        }

        quotaAccount = new QuotaAccountVO(accountId);
        quotaAccount.setQuotaBalance(aggregatedUsage);
        quotaAccount.setQuotaBalanceDate(endDate);
        _quotaAcc.persistQuotaAccount(quotaAccount);
    }

    protected BigDecimal aggregateCreditBetweenDates(Long accountId, Long domainId, Date startDate, Date endDate, String accountToString) {
        List<QuotaBalanceVO> creditsReceived = _quotaBalanceDao.findCreditBalance(accountId, domainId, startDate, endDate);
        s_logger.debug(String.format("Account [%s] has [%s] credit entries before [%s].", accountToString, creditsReceived.size(), endDate));

        BigDecimal aggregatedUsage = BigDecimal.ZERO;

        s_logger.debug(String.format("Aggregating the account [%s] credit entries before [%s].", accountToString, endDate));

        for (QuotaBalanceVO credit : creditsReceived) {
            aggregatedUsage = aggregatedUsage.add(credit.getCreditBalance());
        }

        s_logger.debug(String.format("The aggregation of the account [%s] credit entries before [%s] resulted in the value [%s].", accountToString, endDate, aggregatedUsage));

        return aggregatedUsage;
    }

    @Override
    public boolean calculateQuotaUsage() {
        List<AccountVO> accounts = _accountDao.listAll();
        String accountsToString = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(accounts, "id", "uuid", "accountName", "domainId");

        s_logger.info(String.format("Starting quota usage calculation for accounts [%s].", accountsToString));

        Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType = createMapQuotaTariffsPerUsageType();

        for (AccountVO account : accounts) {
            List<UsageVO> usageRecords = getPendingUsageRecordsForQuotaAggregation(account);

            if (usageRecords == null) {
                s_logger.debug(String.format("Account [%s] does not have pending usage records. Skipping to next account.", account.reflectionToString()));
                continue;
            }

            List<QuotaUsageVO> quotaUsages = createQuotaUsagesAccordingToQuotaTariffs(account, usageRecords, mapQuotaTariffsPerUsageType);
            processQuotaBalanceForAccount(account, quotaUsages);
        }

        s_logger.info(String.format("Finished quota usage calculation for accounts [%s].", accountsToString));

        return true;
    }

    protected List<UsageVO> getPendingUsageRecordsForQuotaAggregation(AccountVO account) {
        Long accountId = account.getId();
        Long domainId = account.getDomainId();

        Pair<List<UsageVO>, Integer> usageRecords = _usageDao.listUsageRecordsPendingForQuotaAggregation(accountId, domainId);

        List<UsageVO> records = usageRecords.first();
        if (CollectionUtils.isEmpty(records)) {
            return null;
        }

        s_logger.debug(String.format("Retrieved [%s] pending usage records for account [%s].", usageRecords.second(), account.reflectionToString()));

        return records;
    }

    protected List<QuotaUsageVO> createQuotaUsagesAccordingToQuotaTariffs(AccountVO account, List<UsageVO> usageRecords,
            Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType) {
        String accountToString = account.reflectionToString();
        s_logger.info(String.format("Calculating quota usage of [%s] usage records for account [%s].", usageRecords.size(), accountToString));

        List<Pair<UsageVO, QuotaUsageVO>> pairsUsageAndQuotaUsage = new ArrayList<>();

        try (JsInterpreter jsInterpreter = new JsInterpreter(QuotaConfig.QuotaActivationRuleTimeout.value())) {
            for (UsageVO usageRecord : usageRecords) {
                int usageType = usageRecord.getUsageType();

                if (Boolean.FALSE.equals(shouldCalculateUsageRecord(account,usageRecord))) {
                    pairsUsageAndQuotaUsage.add(new Pair<>(usageRecord, null));
                    continue;
                }

                Pair<List<QuotaTariffVO>, Boolean> pairQuotaTariffsPerUsageTypeAndHasActivationRule = mapQuotaTariffsPerUsageType.get(usageType);
                List<QuotaTariffVO> quotaTariffs = pairQuotaTariffsPerUsageTypeAndHasActivationRule.first();
                boolean hasAnyQuotaTariffWithActivationRule = pairQuotaTariffsPerUsageTypeAndHasActivationRule.second();

                BigDecimal aggregatedQuotaTariffsValue = aggregateQuotaTariffsValues(usageRecord, quotaTariffs, hasAnyQuotaTariffWithActivationRule, jsInterpreter, accountToString);

                QuotaUsageVO quotaUsage = createQuotaUsageAccordingToUsageUnit(usageRecord, aggregatedQuotaTariffsValue, accountToString);

                pairsUsageAndQuotaUsage.add(new Pair<>(usageRecord, quotaUsage));
            }
        } catch (Exception e) {
            s_logger.error(String.format("Failed to calculate the quota usage for account [%s] due to [%s].", accountToString, e.getMessage()), e);
            return new ArrayList<>();
        }

        return persistUsagesAndQuotaUsagesAndRetrievePersistedQuotaUsages(pairsUsageAndQuotaUsage);
    }

    protected boolean shouldCalculateUsageRecord(AccountVO accountVO, UsageVO usageRecord) {
        if (usageTypesToAvoidCalculation.contains(usageRecord.getUsageType())) {
            s_logger.debug(String.format("Considering usage record [%s] as calculated and skipping it because the calculation of the types [%s] has not been implemented yet.",
                    usageRecord, usageTypesToAvoidCalculation));
            return false;
        }

        if (Boolean.FALSE.equals(QuotaConfig.QuotaAccountEnabled.valueIn(accountVO.getAccountId()))) {
            s_logger.debug(String.format("Considering usage record [%s] as calculated and skipping it because account [%s] has the quota plugin disabled.",
                    usageRecord, accountVO.reflectionToString()));
            return false;
        }
        return true;
    }

    protected List<QuotaUsageVO> persistUsagesAndQuotaUsagesAndRetrievePersistedQuotaUsages(List<Pair<UsageVO, QuotaUsageVO>> pairsUsageAndQuotaUsage) {
        List<QuotaUsageVO> quotaUsages = new ArrayList<>();

        for (Pair<UsageVO, QuotaUsageVO> pairUsageAndQuotaUsage : pairsUsageAndQuotaUsage) {
            UsageVO usageVo = pairUsageAndQuotaUsage.first();
            usageVo.setQuotaCalculated(1);
            _usageDao.persistUsage(usageVo);

            QuotaUsageVO quotaUsageVo = pairUsageAndQuotaUsage.second();
            if (quotaUsageVo != null) {
                _quotaUsageDao.persistQuotaUsage(quotaUsageVo);
                quotaUsages.add(quotaUsageVo);
            }
        }

        return quotaUsages;
    }

    protected BigDecimal aggregateQuotaTariffsValues(UsageVO usageRecord, List<QuotaTariffVO> quotaTariffs, boolean hasAnyQuotaTariffWithActivationRule,
            JsInterpreter jsInterpreter, String accountToString) {
        String usageRecordToString = usageRecord.toString();
        s_logger.debug(String.format("Validating usage record [%s] for account [%s] against [%s] quota tariffs.", usageRecordToString, accountToString,
                quotaTariffs.size()));

        PresetVariables presetVariables = getPresetVariables(hasAnyQuotaTariffWithActivationRule, usageRecord);
        BigDecimal aggregatedQuotaTariffsValue = BigDecimal.ZERO;

        for (QuotaTariffVO quotaTariff : quotaTariffs) {
            if (isQuotaTariffInPeriodToBeApplied(usageRecord, quotaTariff, accountToString)) {
                aggregatedQuotaTariffsValue = aggregatedQuotaTariffsValue.add(getQuotaTariffValueToBeApplied(quotaTariff, jsInterpreter, presetVariables));
            }
        }

        s_logger.debug(String.format("The aggregation of the quota tariffs resulted in the value [%s] for the usage record [%s]. We will use this value to calculate the final"
                + " usage value.", aggregatedQuotaTariffsValue, usageRecordToString));

        return aggregatedQuotaTariffsValue;
    }

    protected PresetVariables getPresetVariables(boolean hasAnyQuotaTariffWithActivationRule, UsageVO usageRecord) {
        if (hasAnyQuotaTariffWithActivationRule) {
            return presetVariableHelper.getPresetVariables(usageRecord);
        }

        return null;
    }

    /**
     * Returns the quota tariff value according to the result of the activation rule.<br/>
     * <ul>
     *   <li>If the activation rule is null or empty, returns {@link QuotaTariffVO#getCurrencyValue()}.</li>
     *   <li>If the activation rule result in a number, returns it.</li>
     *   <li>If the activation rule result in a boolean and its is true, returns {@link QuotaTariffVO#getCurrencyValue()}.</li>
     *   <li>If the activation rule result in a boolean and its is false, returns {@link BigDecimal#ZERO}.</li>
     *   <li>If the activation rule result in something else, returns {@link BigDecimal#ZERO}.</li>
     * </ul>
     */
    protected BigDecimal getQuotaTariffValueToBeApplied(QuotaTariffVO quotaTariff, JsInterpreter jsInterpreter, PresetVariables presetVariables) {
        String activationRule = quotaTariff.getActivationRule();
        BigDecimal quotaTariffValue = quotaTariff.getCurrencyValue();
        String quotaTariffToString = quotaTariff.toString();

        if (StringUtils.isEmpty(activationRule)) {
            s_logger.debug(String.format("Quota tariff [%s] does not have an activation rule, therefore we will use the quota tariff value [%s] in the calculation.",
                    quotaTariffToString, quotaTariffValue));
            return quotaTariffValue;
        }

        injectPresetVariablesIntoJsInterpreter(jsInterpreter, presetVariables);

        String scriptResult = jsInterpreter.executeScript(activationRule).toString();

        if (NumberUtils.isParsable(scriptResult)) {
            s_logger.debug(String.format("The script [%s] of quota tariff [%s] had a numeric value [%s], therefore we will use it in the calculation.", activationRule,
                    quotaTariffToString, scriptResult));

            return new BigDecimal(scriptResult);
        }

        if (BooleanUtils.toBoolean(scriptResult)) {
            s_logger.debug(String.format("The script [%s] of quota tariff [%s] had a true boolean result, therefore we will use the quota tariff's value [%s] in the calculation.",
                    activationRule, quotaTariffToString, quotaTariffValue));

            return quotaTariffValue;
        }

        s_logger.debug(String.format("The script [%s] of quota tariff [%s] had the result [%s], therefore we will not use this quota tariff in the calculation.", activationRule,
                quotaTariffToString, quotaTariffValue));

        return BigDecimal.ZERO;
    }

    /**
     * Injects the preset variables into the JS interpreter.
     */
    protected void injectPresetVariablesIntoJsInterpreter(JsInterpreter jsInterpreter, PresetVariables presetVariables) {
        jsInterpreter.discardCurrentVariables();

        jsInterpreter.injectVariable("account", presetVariables.getAccount().toString());
        jsInterpreter.injectVariable("domain", presetVariables.getDomain().toString());

        GenericPresetVariable project = presetVariables.getProject();
        if (project != null) {
            jsInterpreter.injectVariable("project", project.toString());

        }

        jsInterpreter.injectVariable("resourceType", presetVariables.getResourceType());
        jsInterpreter.injectVariable("value", presetVariables.getValue().toString());
        jsInterpreter.injectVariable("zone", presetVariables.getZone().toString());
    }

    /**
     * Verifies if the quota tariff should be applied on the usage record according to their respectively start and end date.<br/><br/>
     */
    protected boolean isQuotaTariffInPeriodToBeApplied(UsageVO usageRecord, QuotaTariffVO quotaTariff, String accountToString) {
        Date usageRecordStartDate = usageRecord.getStartDate();
        Date usageRecordEndDate = usageRecord.getEndDate();
        Date quotaTariffStartDate = quotaTariff.getEffectiveOn();
        Date quotaTariffEndDate = quotaTariff.getEndDate();

        if ((quotaTariffEndDate != null && usageRecordStartDate.after(quotaTariffEndDate)) || usageRecordEndDate.before(quotaTariffStartDate)) {
            s_logger.debug(String.format("Not applying quota tariff [%s] in usage record [%s] of account [%s] due to it is out of the period to be applied. Period of the usage"
                    + " record [startDate: %s, endDate: %s], period of the quota tariff [startDate: %s, endDate: %s].", quotaTariff, usageRecord.toString(), accountToString,
                    DateUtil.getOutputString(usageRecordStartDate), DateUtil.getOutputString(usageRecordEndDate), DateUtil.getOutputString(quotaTariffStartDate),
                    DateUtil.getOutputString(quotaTariffEndDate)));

            return false;
        }

        return true;
    }

    protected Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> createMapQuotaTariffsPerUsageType() {
        List<QuotaTariffVO> quotaTariffs = _quotaTariffDao.listQuotaTariffs(null, null, null, null, null, false, null, null).first();

        Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType = new HashMap<>();

        for (Map.Entry<Integer, QuotaTypes> entry : QuotaTypes.listQuotaTypes().entrySet()) {
            int quotaType = entry.getKey();

            List<QuotaTariffVO> quotaTariffsFiltered = quotaTariffs.stream().filter(quotaTariff -> quotaTariff.getUsageType() == quotaType).collect(Collectors.toList());
            Boolean hasAnyQuotaTariffWithActivationRule = quotaTariffsFiltered.stream().anyMatch(quotaTariff -> StringUtils.isNotEmpty(quotaTariff.getActivationRule()));

            mapQuotaTariffsPerUsageType.put(quotaType, new Pair<>(quotaTariffsFiltered, hasAnyQuotaTariffWithActivationRule));
        }

        return mapQuotaTariffsPerUsageType;
    }

    protected QuotaUsageVO createQuotaUsageAccordingToUsageUnit(UsageVO usageRecord, BigDecimal aggregatedQuotaTariffsValue, String accountToString) {
        String usageRecordToString = usageRecord.toString();

        if (aggregatedQuotaTariffsValue.equals(BigDecimal.ZERO)) {
            s_logger.debug(String.format("Usage record [%s] for account [%s] does not have quota tariffs to be calculated, therefore we will mark it as calculated.",
                    usageRecordToString, accountToString));
            return null;
        }

        QuotaTypes quotaType = QuotaTypes.listQuotaTypes().get(usageRecord.getUsageType());
        String quotaUnit = quotaType.getQuotaUnit();

        s_logger.debug(String.format("Calculating value of usage record [%s] for account [%s] according to the aggregated quota tariffs value [%s] and its usage unit [%s].",
                usageRecordToString, accountToString, aggregatedQuotaTariffsValue, quotaUnit));

        BigDecimal usageValue = getUsageValueAccordingToUsageUnitType(usageRecord, aggregatedQuotaTariffsValue, quotaUnit);

        s_logger.debug(String.format("The calculation of the usage record [%s] for account [%s] according to the aggregated quota tariffs value [%s] and its usage unit [%s] "
                + "resulted in the value [%s].", usageRecordToString, accountToString, aggregatedQuotaTariffsValue, quotaUnit, usageValue));

        QuotaUsageVO quotaUsageVo = new QuotaUsageVO();
        quotaUsageVo.setUsageItemId(usageRecord.getId());
        quotaUsageVo.setZoneId(usageRecord.getZoneId());
        quotaUsageVo.setAccountId(usageRecord.getAccountId());
        quotaUsageVo.setDomainId(usageRecord.getDomainId());
        quotaUsageVo.setUsageType(quotaType.getQuotaType());
        quotaUsageVo.setQuotaUsed(usageValue);
        quotaUsageVo.setStartDate(usageRecord.getStartDate());
        quotaUsageVo.setEndDate(usageRecord.getEndDate());

        return quotaUsageVo;
    }

    protected BigDecimal getUsageValueAccordingToUsageUnitType(UsageVO usageRecord, BigDecimal aggregatedQuotaTariffsValue, String quotaUnit) {
        BigDecimal rawUsage = BigDecimal.valueOf(usageRecord.getRawUsage());
        BigDecimal costPerHour = aggregatedQuotaTariffsValue.divide(s_hoursInMonth, 8, RoundingMode.HALF_EVEN);

        switch (UsageUnitTypes.getByDescription(quotaUnit)) {
            case COMPUTE_MONTH:
            case IP_MONTH:
            case POLICY_MONTH:
                return rawUsage.multiply(costPerHour);

            case GB:
                BigDecimal rawUsageInGb = rawUsage.divide(GiB_DECIMAL, 8, RoundingMode.HALF_EVEN);
                return rawUsageInGb.multiply(aggregatedQuotaTariffsValue);

            case GB_MONTH:
                BigDecimal gbInUse = BigDecimal.valueOf(usageRecord.getSize()).divide(GiB_DECIMAL, 8, RoundingMode.HALF_EVEN);
                return rawUsage.multiply(costPerHour).multiply(gbInUse);

            default:
                return BigDecimal.ZERO;
        }
    }

    @Override
    public boolean isLockable(AccountVO account) {
        return lockablesAccountTypes.contains(account.getType());
    }

}
