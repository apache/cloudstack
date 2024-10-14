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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import org.apache.cloudstack.quota.activationrule.presetvariables.Tariff;
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
import org.apache.cloudstack.usage.UsageUnitTypes;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
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

    private static TimeZone usageAggregationTimeZone = TimeZone.getTimeZone("GMT");
    static final BigDecimal GiB_DECIMAL = BigDecimal.valueOf(ByteScaleUtils.GiB);
    List<Account.Type> lockablesAccountTypes = Arrays.asList(Account.Type.NORMAL, Account.Type.DOMAIN_ADMIN);

    static BigDecimal hoursInCurrentMonth;

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

        String usageAggregationTimeZoneStr = ObjectUtils.defaultIfNull(configs.get("usage.aggregation.timezone"), "GMT");
        usageAggregationTimeZone = TimeZone.getTimeZone(usageAggregationTimeZoneStr);

        return true;
    }

    public static TimeZone getUsageAggregationTimeZone() {
        return usageAggregationTimeZone;
    }

    @Override
    public boolean start() {
        if (logger.isInfoEnabled()) {
            logger.info("Starting Quota Manager");
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (logger.isInfoEnabled()) {
            logger.info("Stopping Quota Manager");
        }
        return true;
    }

    protected void processQuotaBalanceForAccount(AccountVO accountVo, List<QuotaUsageVO> accountQuotaUsages) {
        String accountToString = accountVo.reflectionToString();

        if (CollectionUtils.isEmpty(accountQuotaUsages)) {
            logger.info(String.format("Account [%s] does not have quota usages to process. Skipping it.", accountToString));
            return;
        }

        Date startDate = accountQuotaUsages.get(0).getStartDate();
        Date endDate = accountQuotaUsages.get(0).getEndDate();
        Date lastQuotaUsageEndDate = accountQuotaUsages.get(accountQuotaUsages.size() - 1).getEndDate();

        LinkedHashSet<Pair<Date, Date>> periods = accountQuotaUsages.stream()
                .map(quotaUsageVO -> new Pair<>(quotaUsageVO.getStartDate(), quotaUsageVO.getEndDate()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        logger.info(String.format("Processing quota balance for account[{}] between [{}] and [{}].", accountToString, startDate, lastQuotaUsageEndDate));

        long accountId = accountVo.getAccountId();
        long domainId = accountVo.getDomainId();
        BigDecimal accountBalance = retrieveBalanceForUsageCalculation(accountId, domainId, startDate, accountToString);

        for (Pair<Date, Date> period : periods) {
            startDate = period.first();
            endDate = period.second();

            accountBalance = calculateBalanceConsideringCreditsAddedAndQuotaUsed(accountBalance, accountQuotaUsages, accountId, domainId, startDate, endDate, accountToString);
            _quotaBalanceDao.saveQuotaBalance(new QuotaBalanceVO(accountId, domainId, accountBalance, endDate));
        }
        saveQuotaAccount(accountId, accountBalance, endDate);
    }

    /**
     * Calculates the balance for the given account considering the specified period. The balance is calculated as follows:
     * <ol>
     *     <li>The credits added in this period are added to the balance.</li>
     *     <li>All quota consumed in this period are subtracted from the account balance.</li>
     * </ol>
     */
    protected BigDecimal calculateBalanceConsideringCreditsAddedAndQuotaUsed(BigDecimal accountBalance, List<QuotaUsageVO> accountQuotaUsages, long accountId, long domainId,
                                                                             Date startDate, Date endDate, String accountToString) {
        accountBalance = accountBalance.add(aggregateCreditBetweenDates(accountId, domainId, startDate, endDate, accountToString));

        for (QuotaUsageVO quotaUsageVO : accountQuotaUsages) {
            if (DateUtils.isSameInstant(quotaUsageVO.getStartDate(), startDate)) {
                accountBalance = accountBalance.subtract(quotaUsageVO.getQuotaUsed());
            }
        }
        return accountBalance;
    }

    /**
     * Retrieves the initial balance prior to the period of the quota processing.
     * <ul>
     *     <li>
     *         If it is the first time of processing for the account, the credits prior to the quota processing are added, and the first balance is persisted in the DB.
     *     </li>
     *     <li>
     *         Otherwise, the last real balance of the account is retrieved.
     *     </li>
     * </ul>
     */
    protected BigDecimal retrieveBalanceForUsageCalculation(long accountId, long domainId, Date startDate, String accountToString) {
        BigDecimal accountBalance = BigDecimal.ZERO;
        QuotaUsageVO lastQuotaUsage = _quotaUsageDao.findLastQuotaUsageEntry(accountId, domainId, startDate);

        if (lastQuotaUsage == null) {
            accountBalance = accountBalance.add(aggregateCreditBetweenDates(accountId, domainId, new Date(0), startDate, accountToString));
            QuotaBalanceVO firstBalance = new QuotaBalanceVO(accountId, domainId, accountBalance, startDate);

            logger.debug(String.format("Persisting the first quota balance [%s] for account [%s].", firstBalance, accountToString));
            _quotaBalanceDao.saveQuotaBalance(firstBalance);
        } else {
            QuotaBalanceVO lastRealBalance = _quotaBalanceDao.findLastBalanceEntry(accountId, domainId, startDate);

            if (lastRealBalance == null) {
                logger.warn("Account [{}] has quota usage entries, however it does not have a quota balance.", accountToString);
            } else {
                accountBalance = accountBalance.add(lastRealBalance.getCreditBalance());
            }
        }

        return accountBalance;
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
        logger.debug("Account [{}] has [{}] credit entries before [{}].", accountToString, creditsReceived.size(),
                DateUtil.displayDateInTimezone(usageAggregationTimeZone, endDate));

        BigDecimal aggregatedUsage = BigDecimal.ZERO;

        logger.debug("Aggregating the account [{}] credit entries before [{}].", accountToString,
                DateUtil.displayDateInTimezone(usageAggregationTimeZone, endDate));

        for (QuotaBalanceVO credit : creditsReceived) {
            aggregatedUsage = aggregatedUsage.add(credit.getCreditBalance());
        }

        logger.debug("The aggregation of the account [{}] credit entries before [{}] resulted in the value [{}].",
                accountToString, DateUtil.displayDateInTimezone(usageAggregationTimeZone, endDate), aggregatedUsage);

        return aggregatedUsage;
    }

    @Override
    public boolean calculateQuotaUsage() {
        List<AccountVO> accounts = _accountDao.listAll();
        String accountsToString = ReflectionToStringBuilderUtils.reflectOnlySelectedFields(accounts, "id", "uuid", "accountName", "domainId");

        logger.info(String.format("Starting quota usage calculation for accounts [%s].", accountsToString));

        Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType = createMapQuotaTariffsPerUsageType();

        for (AccountVO account : accounts) {
            List<UsageVO> usageRecords = getPendingUsageRecordsForQuotaAggregation(account);

            if (usageRecords == null) {
                logger.debug(String.format("Account [%s] does not have pending usage records. Skipping to next account.", account.reflectionToString()));
                continue;
            }

            List<QuotaUsageVO> quotaUsages = createQuotaUsagesAccordingToQuotaTariffs(account, usageRecords, mapQuotaTariffsPerUsageType);
            processQuotaBalanceForAccount(account, quotaUsages);
        }

        logger.info(String.format("Finished quota usage calculation for accounts [%s].", accountsToString));

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

        logger.debug(String.format("Retrieved [%s] pending usage records for account [%s].", usageRecords.second(), account.reflectionToString()));

        return records;
    }

    protected List<QuotaUsageVO> createQuotaUsagesAccordingToQuotaTariffs(AccountVO account, List<UsageVO> usageRecords,
            Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> mapQuotaTariffsPerUsageType) {
        String accountToString = account.reflectionToString();
        logger.info("Calculating quota usage of [{}] usage records for account [{}].", usageRecords.size(), accountToString);

        List<Pair<UsageVO, QuotaUsageVO>> pairsUsageAndQuotaUsage = new ArrayList<>();

        try (JsInterpreter jsInterpreter = new JsInterpreter(QuotaConfig.QuotaActivationRuleTimeout.value())) {
            for (UsageVO usageRecord : usageRecords) {
                int usageType = usageRecord.getUsageType();

                if (!shouldCalculateUsageRecord(account, usageRecord)) {
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
            logger.error(String.format("Failed to calculate the quota usage for account [%s] due to [%s].", accountToString, e.getMessage()), e);
            return new ArrayList<>();
        }

        return persistUsagesAndQuotaUsagesAndRetrievePersistedQuotaUsages(pairsUsageAndQuotaUsage);
    }

    protected boolean shouldCalculateUsageRecord(AccountVO accountVO, UsageVO usageRecord) {
        if (Boolean.FALSE.equals(QuotaConfig.QuotaAccountEnabled.valueIn(accountVO.getAccountId()))) {
            logger.debug("Considering usage record [{}] as calculated and skipping it because account [{}] has the quota plugin disabled.",
                    usageRecord.toString(usageAggregationTimeZone), accountVO.reflectionToString());
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
        String usageRecordToString = usageRecord.toString(usageAggregationTimeZone);
        logger.debug("Validating usage record [{}] for account [{}] against [{}] quota tariffs.", usageRecordToString, accountToString, quotaTariffs.size());

        PresetVariables presetVariables = getPresetVariables(hasAnyQuotaTariffWithActivationRule, usageRecord);
        BigDecimal aggregatedQuotaTariffsValue = BigDecimal.ZERO;

        quotaTariffs.sort(Comparator.comparing(QuotaTariffVO::getPosition));

        List<Tariff> lastTariffs = new ArrayList<>();


        for (QuotaTariffVO quotaTariff : quotaTariffs) {
            if (isQuotaTariffInPeriodToBeApplied(usageRecord, quotaTariff, accountToString)) {

                BigDecimal tariffValue = getQuotaTariffValueToBeApplied(quotaTariff, jsInterpreter, presetVariables, lastTariffs);

                aggregatedQuotaTariffsValue = aggregatedQuotaTariffsValue.add(tariffValue);

                Tariff tariffPresetVariable = new Tariff();
                tariffPresetVariable.setId(quotaTariff.getUuid());
                tariffPresetVariable.setValue(tariffValue);
                lastTariffs.add(tariffPresetVariable);
            }
        }

        logger.debug(String.format("The aggregation of the quota tariffs resulted in the value [%s] for the usage record [%s]. We will use this value to calculate the final"
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
    protected BigDecimal getQuotaTariffValueToBeApplied(QuotaTariffVO quotaTariff, JsInterpreter jsInterpreter, PresetVariables presetVariables, List<Tariff> lastAppliedTariffsList) {
        String activationRule = quotaTariff.getActivationRule();
        BigDecimal quotaTariffValue = quotaTariff.getCurrencyValue();
        String quotaTariffToString = quotaTariff.toString(usageAggregationTimeZone);

        if (StringUtils.isEmpty(activationRule)) {
            logger.debug(String.format("Quota tariff [%s] does not have an activation rule, therefore we will use the quota tariff value [%s] in the calculation.",
                    quotaTariffToString, quotaTariffValue));
            return quotaTariffValue;
        }

        injectPresetVariablesIntoJsInterpreter(jsInterpreter, presetVariables);
        jsInterpreter.injectVariable("lastTariffs", lastAppliedTariffsList.toString());

        String scriptResult = jsInterpreter.executeScript(activationRule).toString();

        if (NumberUtils.isParsable(scriptResult)) {
            logger.debug(String.format("The script [%s] of quota tariff [%s] had a numeric value [%s], therefore we will use it in the calculation.", activationRule,
                    quotaTariffToString, scriptResult));

            return new BigDecimal(scriptResult);
        }

        if (BooleanUtils.toBoolean(scriptResult)) {
            logger.debug(String.format("The script [%s] of quota tariff [%s] had a true boolean result, therefore we will use the quota tariff's value [%s] in the calculation.",
                    activationRule, quotaTariffToString, quotaTariffValue));

            return quotaTariffValue;
        }

        logger.debug(String.format("The script [%s] of quota tariff [%s] had the result [%s], therefore we will not use this quota tariff in the calculation.", activationRule,
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

        jsInterpreter.injectStringVariable("resourceType", presetVariables.getResourceType());
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
            logger.debug("Not applying quota tariff [{}] in usage record [{}] of account [{}] due to it is out of the period to be applied. Period of the usage"
                            + " record [startDate: {}, endDate: {}], period of the quota tariff [startDate: {}, endDate: {}].", quotaTariff.toString(usageAggregationTimeZone),
                    usageRecord.toString(usageAggregationTimeZone), accountToString, DateUtil.displayDateInTimezone(usageAggregationTimeZone, usageRecordStartDate),
                    DateUtil.displayDateInTimezone(usageAggregationTimeZone, usageRecordEndDate), DateUtil.displayDateInTimezone(usageAggregationTimeZone, quotaTariffStartDate),
                    DateUtil.displayDateInTimezone(usageAggregationTimeZone, quotaTariffEndDate));

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
        String usageRecordToString = usageRecord.toString(usageAggregationTimeZone);

        if (aggregatedQuotaTariffsValue.equals(BigDecimal.ZERO)) {
            logger.debug("No tariffs were applied to usage record [{}] of account [{}] or they resulted in 0; We will only mark the usage record as calculated.",
                    usageRecordToString, accountToString);
            return null;
        }

        QuotaTypes quotaType = QuotaTypes.listQuotaTypes().get(usageRecord.getUsageType());
        String quotaUnit = quotaType.getQuotaUnit();

        logger.debug(String.format("Calculating value of usage record [%s] for account [%s] according to the aggregated quota tariffs value [%s] and its usage unit [%s].",
                usageRecordToString, accountToString, aggregatedQuotaTariffsValue, quotaUnit));

        BigDecimal usageValue = getUsageValueAccordingToUsageUnitType(usageRecord, aggregatedQuotaTariffsValue, quotaUnit);

        logger.debug(String.format("The calculation of the usage record [%s] for account [%s] according to the aggregated quota tariffs value [%s] and its usage unit [%s] "
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
        BigDecimal costPerHour = getCostPerHour(aggregatedQuotaTariffsValue, usageRecord.getStartDate());

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

            case BYTES:
            case IOPS:
                return rawUsage.multiply(aggregatedQuotaTariffsValue);

            default:
                return BigDecimal.ZERO;
        }
    }

    protected BigDecimal getCostPerHour(BigDecimal costPerMonth, Date date) {
        BigDecimal hoursInCurrentMonth = BigDecimal.valueOf(DateUtil.getHoursInCurrentMonth(date));
        logger.trace(String.format("Dividing tariff cost per month [%s] by [%s] to get the tariffs cost per hour.", costPerMonth, hoursInCurrentMonth));
        return costPerMonth.divide(hoursInCurrentMonth, 8, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean isLockable(AccountVO account) {
        return lockablesAccountTypes.contains(account.getType());
    }

}
