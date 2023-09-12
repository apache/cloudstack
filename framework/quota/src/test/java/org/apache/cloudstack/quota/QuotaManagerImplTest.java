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
package org.apache.cloudstack.quota;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.quota.activationrule.presetvariables.Domain;
import org.apache.cloudstack.quota.activationrule.presetvariables.GenericPresetVariable;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariableHelper;
import org.apache.cloudstack.quota.activationrule.presetvariables.PresetVariables;
import org.apache.cloudstack.quota.activationrule.presetvariables.Value;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.usage.UsageUnitTypes;
import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.cloudstack.utils.jsinterpreter.JsInterpreter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class QuotaManagerImplTest {

    @Mock
    UsageDao usageDaoMock;

    @Mock
    PresetVariableHelper presetVariableHelperMock;

    @Mock
    QuotaUsageDao quotaUsageDaoMock;

    @InjectMocks
    QuotaManagerImpl quotaManagerImplSpy = Mockito.spy(QuotaManagerImpl.class);

    @Mock
    AccountVO accountVoMock;

    @Mock
    Pair<List<UsageVO>, Integer> pairMock;

    @Mock
    UsageVO usageVoMock;

    @Mock
    QuotaTariffDao quotaTariffDaoMock;

    @Mock
    QuotaTariffVO quotaTariffVoMock;

    @Mock
    JsInterpreter jsInterpreterMock;

    @Mock
    PresetVariables presetVariablesMock;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void isLockableTestValidateAccountTypes() {
        List<Account.Type> lockablesAccountTypes = Arrays.asList(Account.Type.NORMAL, Account.Type.DOMAIN_ADMIN);

        AccountVO accountVO = new AccountVO();
        Arrays.asList(Account.Type.values()).forEach(accountType -> {
            accountVO.setType(accountType);

            if (lockablesAccountTypes.contains(accountType)) {
                Assert.assertTrue(quotaManagerImplSpy.isLockable(accountVO));
            } else {
                Assert.assertFalse(quotaManagerImplSpy.isLockable(accountVO));
            }
        });
    }

    @Test
    public void getPendingUsageRecordsForQuotaAggregationTestNullListReturnNull() {
        Mockito.doReturn(pairMock).when(usageDaoMock).listUsageRecordsPendingForQuotaAggregation(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(null).when(pairMock).first();

        List<UsageVO> result = quotaManagerImplSpy.getPendingUsageRecordsForQuotaAggregation(accountVoMock);

        Assert.assertNull(result);
    }

    @Test
    public void getPendingUsageRecordsForQuotaAggregationTestEmptyListReturnNull() {
        Mockito.doReturn(pairMock).when(usageDaoMock).listUsageRecordsPendingForQuotaAggregation(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(new ArrayList<>()).when(pairMock).first();

        List<UsageVO> result = quotaManagerImplSpy.getPendingUsageRecordsForQuotaAggregation(accountVoMock);

        Assert.assertNull(result);
    }

    @Test
    public void getPendingUsageRecordsForQuotaAggregationTesNotEmptyListReturnList() {
        List<UsageVO> expected = Arrays.asList(new UsageVO());

        Mockito.doReturn(pairMock).when(usageDaoMock).listUsageRecordsPendingForQuotaAggregation(Mockito.anyLong(), Mockito.anyLong());
        Mockito.doReturn(expected).when(pairMock).first();

        List<UsageVO> result = quotaManagerImplSpy.getPendingUsageRecordsForQuotaAggregation(accountVoMock);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getUsageValueAccordingToUsageUnitTypeTestAllTypes() {
        Mockito.doReturn(10.0).when(usageVoMock).getRawUsage();
        Mockito.doReturn(ByteScaleUtils.GiB).when(usageVoMock).getSize();
        BigDecimal aggregatedQuotaTariffsValue = new BigDecimal(400);

        Arrays.asList(UsageUnitTypes.values()).forEach(type -> {
           BigDecimal result = quotaManagerImplSpy.getUsageValueAccordingToUsageUnitType(usageVoMock, aggregatedQuotaTariffsValue, type.toString());
           Double expected = null;

           switch (type) {
               case COMPUTE_MONTH:
               case IP_MONTH:
               case POLICY_MONTH:
                   //The value 5.5555556 is referent to the calculation (( tariffs values / hours in month ) * raw usage ).
                   expected = 5.5555556;
                   break;

               case GB:
                   //The value 0.000004 is referent to the calculation (( raw usage / gib) * tariffs values ).
                   expected = 0.000004;
                   break;

               case GB_MONTH:
                   //The value 5.5555556 is referent to the calculation (( usage size / gib ) * raw usage * ( tariffs values / hours in month )).
                   expected = 5.5555556;
                   break;

               case BYTES:
               case IOPS:
                   //The value 4000.0 is referent to the calculation ( raw usage * tariffs values ).
                   expected = 4000.0;
                   break;

               default:
                   break;
           }

           Assert.assertEquals(expected, result.doubleValue(), 0);
        });
    }

    private void mockUsageRecordAndQuotaTariffForTests(Date usageRecordStartDate, Date usageRecordEndDate, Date quotaTariffStartDate, Date quotaTariffEndDate) {
        Mockito.doReturn(usageRecordStartDate).when(usageVoMock).getStartDate();
        Mockito.doReturn(usageRecordEndDate).when(usageVoMock).getEndDate();
        Mockito.doReturn(quotaTariffStartDate).when(quotaTariffVoMock).getEffectiveOn();
        Mockito.doReturn(quotaTariffEndDate).when(quotaTariffVoMock).getEndDate();
    }

    @Test
    public void isQuotaTariffInPeriodToBeAppliedTestQuotaTariffEndDateIsNullAndUsageRecordEndDateIsBeforeQuotaTariffStartDateReturnFalse() throws ParseException {
        mockUsageRecordAndQuotaTariffForTests(null, sdf.parse("2022-01-20 10:00:00"), sdf.parse("2022-01-20 12:00:00"), null);
        boolean result = quotaManagerImplSpy.isQuotaTariffInPeriodToBeApplied(usageVoMock, quotaTariffVoMock, "");

        Assert.assertFalse(result);
    }

    @Test
    public void isQuotaTariffInPeriodToBeAppliedTestQuotaTariffEndDateIsNullAndUsageRecordEndDateIsAfterQuotaTariffStartDateReturnTrue() throws ParseException {
        mockUsageRecordAndQuotaTariffForTests(null, sdf.parse("2022-01-21 20:00:00"), sdf.parse("2022-01-18 11:30:00"), null);
        boolean result = quotaManagerImplSpy.isQuotaTariffInPeriodToBeApplied(usageVoMock, quotaTariffVoMock, "");

        Assert.assertTrue(result);
    }

    @Test
    public void isQuotaTariffInPeriodToBeAppliedTestQuotaTariffEndDateIsNullAndUsageRecordEndDateIsEqualToQuotaTariffStartDateReturnTrue() throws ParseException {
        mockUsageRecordAndQuotaTariffForTests(null, sdf.parse("2022-01-12 00:00:00"), sdf.parse("2022-01-12 00:00:00"), null);
        boolean result = quotaManagerImplSpy.isQuotaTariffInPeriodToBeApplied(usageVoMock, quotaTariffVoMock, "");

        Assert.assertTrue(result);
    }

    @Test
    public void isQuotaTariffInPeriodToBeAppliedTestUsageRecordStartDateIsAfterQuotaTariffEndDateAndUsageRecordEndDateIsAfterQuotaTariffStartDateReturnFalse() throws ParseException {
        mockUsageRecordAndQuotaTariffForTests(sdf.parse("2022-01-11 00:00:00"), sdf.parse("2022-01-12 00:00:00"), sdf.parse("2022-01-08 00:00:00"), sdf.parse("2022-01-10 00:00:00"));
        boolean result = quotaManagerImplSpy.isQuotaTariffInPeriodToBeApplied(usageVoMock, quotaTariffVoMock, "");

        Assert.assertFalse(result);
    }

    @Test
    public void isQuotaTariffInPeriodToBeAppliedTestUsageRecordStartDateIsEqualToQuotaTariffEndDateAndUsageRecordEndDateIsAfterQuotaTariffStartDateReturnTrue() throws ParseException {
        mockUsageRecordAndQuotaTariffForTests(sdf.parse("2022-01-18 17:35:12"), sdf.parse("2022-01-12 00:00:00"), sdf.parse("2022-01-08 00:00:00"), sdf.parse("2022-01-18 17:35:12"));
        boolean result = quotaManagerImplSpy.isQuotaTariffInPeriodToBeApplied(usageVoMock, quotaTariffVoMock, "");

        Assert.assertTrue(result);
    }

    @Test
    public void isQuotaTariffInPeriodToBeAppliedTestUsageRecordStartDateIsBeforeQuotaTariffEndDateAndUsageRecordEndDateIsAfterQuotaTariffStartDateReturnTrue() throws ParseException {
        mockUsageRecordAndQuotaTariffForTests(sdf.parse("2022-01-15 00:23:15"), sdf.parse("2022-01-16 00:23:15"), sdf.parse("2022-01-08 00:00:00"), sdf.parse("2022-01-16 00:50:08"));
        boolean result = quotaManagerImplSpy.isQuotaTariffInPeriodToBeApplied(usageVoMock, quotaTariffVoMock, "");

        Assert.assertTrue(result);
    }

    @Test
    public void isQuotaTariffInPeriodToBeAppliedTestUsageRecordStartDateIsBeforeQuotaTariffEndDateAndUsageRecordEndDateIsBeforeQuotaTariffStartDateReturnFalse() throws ParseException {
        mockUsageRecordAndQuotaTariffForTests(sdf.parse("2022-01-20 11:44:37"), sdf.parse("2022-01-21 11:44:37"), sdf.parse("2022-01-22 15:06:32"), sdf.parse("2022-01-28 18:33:01"));
        boolean result = quotaManagerImplSpy.isQuotaTariffInPeriodToBeApplied(usageVoMock, quotaTariffVoMock, "");

        Assert.assertFalse(result);
    }

    @Test
    public void isQuotaTariffInPeriodToBeAppliedTestUsageRecordStartDateIsBeforeQuotaTariffEndDateAndUsageRecordEndDateIsEqualToQuotaTariffStartDateReturnTrue() throws ParseException {
        mockUsageRecordAndQuotaTariffForTests(sdf.parse("2022-01-20 11:44:37"), sdf.parse("2022-01-22 15:06:32"), sdf.parse("2022-01-22 15:06:32"), sdf.parse("2022-01-28 18:33:01"));
        boolean result = quotaManagerImplSpy.isQuotaTariffInPeriodToBeApplied(usageVoMock, quotaTariffVoMock, "");

        Assert.assertTrue(result);
    }

    @Test
    public void injectPresetVariablesIntoJsInterpreterTestProjectIsNullDoNotInjectProject() {
        Mockito.doNothing().when(jsInterpreterMock).injectVariable(Mockito.anyString(), Mockito.anyString());

        Mockito.doReturn(new org.apache.cloudstack.quota.activationrule.presetvariables.Account()).when(presetVariablesMock).getAccount();
        Mockito.doReturn(new Domain()).when(presetVariablesMock).getDomain();
        Mockito.doReturn(null).when(presetVariablesMock).getProject();
        Mockito.doReturn("test").when(presetVariablesMock).getResourceType();
        Mockito.doReturn(new Value()).when(presetVariablesMock).getValue();
        Mockito.doReturn(new GenericPresetVariable()).when(presetVariablesMock).getZone();

        quotaManagerImplSpy.injectPresetVariablesIntoJsInterpreter(jsInterpreterMock, presetVariablesMock);

        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("account"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("domain"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock, Mockito.never()).injectVariable(Mockito.eq("project"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("resourceType"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("value"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("zone"), Mockito.anyString());
    }

    @Test
    public void injectPresetVariablesIntoJsInterpreterTestProjectIsNotNullInjectProject() {
        Mockito.doNothing().when(jsInterpreterMock).injectVariable(Mockito.anyString(), Mockito.anyString());

        Mockito.doReturn(new org.apache.cloudstack.quota.activationrule.presetvariables.Account()).when(presetVariablesMock).getAccount();
        Mockito.doReturn(new Domain()).when(presetVariablesMock).getDomain();
        Mockito.doReturn(new GenericPresetVariable()).when(presetVariablesMock).getProject();
        Mockito.doReturn("test").when(presetVariablesMock).getResourceType();
        Mockito.doReturn(new Value()).when(presetVariablesMock).getValue();
        Mockito.doReturn(new GenericPresetVariable()).when(presetVariablesMock).getZone();

        quotaManagerImplSpy.injectPresetVariablesIntoJsInterpreter(jsInterpreterMock, presetVariablesMock);

        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("account"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("domain"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("project"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("resourceType"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("value"), Mockito.anyString());
        Mockito.verify(jsInterpreterMock).injectVariable(Mockito.eq("zone"), Mockito.anyString());
    }

    @Test
    public void createMapQuotaTariffsPerUsageTypeTestNoTariffs() {
        Mockito.doReturn(new Pair<>(new ArrayList<>(), 0)).when(quotaTariffDaoMock).listQuotaTariffs(Mockito.any(), Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any());

        Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> result = quotaManagerImplSpy.createMapQuotaTariffsPerUsageType();

        for (Map.Entry<Integer, QuotaTypes> entry : QuotaTypes.listQuotaTypes().entrySet()) {
            Pair<List<QuotaTariffVO>, Boolean> pair = result.get(entry.getKey());
            Assert.assertTrue(pair.first().isEmpty());
            Assert.assertFalse(pair.second());
        }
    }

    @Test
    public void createMapQuotaTariffsPerUsageTypeTestTariffsWithEmptyActivationRule() {
        List<QuotaTariffVO> tariffs = new ArrayList<>();
        QuotaTariffVO tariff = new QuotaTariffVO(1);
        tariff.setActivationRule("");
        tariffs.add(tariff);

        Mockito.doReturn(new Pair<>(tariffs, tariffs.size())).when(quotaTariffDaoMock).listQuotaTariffs(Mockito.any(), Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any());

        Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> result = quotaManagerImplSpy.createMapQuotaTariffsPerUsageType();

        for (Map.Entry<Integer, QuotaTypes> entry : QuotaTypes.listQuotaTypes().entrySet()) {
            Pair<List<QuotaTariffVO>, Boolean> pair = result.get(entry.getKey());
            if (entry.getKey() == 1) {
                Assert.assertFalse(pair.first().isEmpty());
            } else {
                Assert.assertTrue(pair.first().isEmpty());
            }
            Assert.assertFalse(pair.second());
        }
    }

    @Test
    public void createMapQuotaTariffsPerUsageTypeTestTariffsWithActivationRule() {
        List<QuotaTariffVO> tariffs = new ArrayList<>();
        QuotaTariffVO tariff = new QuotaTariffVO(1);
        tariff.setActivationRule(" ");
        tariffs.add(tariff);

        Mockito.doReturn(new Pair<>(tariffs, tariffs.size())).when(quotaTariffDaoMock).listQuotaTariffs(Mockito.any(), Mockito.any(), Mockito.any(),Mockito.any(), Mockito.any(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any());

        Map<Integer, Pair<List<QuotaTariffVO>, Boolean>> result = quotaManagerImplSpy.createMapQuotaTariffsPerUsageType();

        for (Map.Entry<Integer, QuotaTypes> entry : QuotaTypes.listQuotaTypes().entrySet()) {
            Pair<List<QuotaTariffVO>, Boolean> pair = result.get(entry.getKey());
            if (entry.getKey() == 1) {
                Assert.assertFalse(pair.first().isEmpty());
                Assert.assertTrue(pair.second());
            } else {
                Assert.assertTrue(pair.first().isEmpty());
                Assert.assertFalse(pair.second());
            }
        }
    }

    @Test
    public void createQuotaUsageAccordingToUsageUnitTariffValueZeroReturnNull() {
        QuotaUsageVO result = quotaManagerImplSpy.createQuotaUsageAccordingToUsageUnit(usageVoMock, BigDecimal.ZERO, null);
        Assert.assertNull(result);
    }

    @Test
    public void createQuotaUsageAccordingToUsageUnitTariffValueIsNotZeroReturnObject() {
        Date startDate = new Date();
        Date endDate = new Date();

        QuotaTypes.listQuotaTypes().entrySet().forEach(entry -> {
            Mockito.doReturn(entry.getKey()).when(usageVoMock).getUsageType();
            Mockito.doReturn(BigDecimal.TEN).when(quotaManagerImplSpy).getUsageValueAccordingToUsageUnitType(Mockito.any(), Mockito.any(), Mockito.anyString());
            Mockito.doReturn(2l).when(usageVoMock).getId();
            Mockito.doReturn(3l).when(usageVoMock).getZoneId();
            Mockito.doReturn(4l).when(usageVoMock).getAccountId();
            Mockito.doReturn(5l).when(usageVoMock).getDomainId();
            Mockito.doReturn(startDate).when(usageVoMock).getStartDate();
            Mockito.doReturn(endDate).when(usageVoMock).getEndDate();

            QuotaUsageVO result = quotaManagerImplSpy.createQuotaUsageAccordingToUsageUnit(usageVoMock, BigDecimal.ONE, null);

            Assert.assertEquals(2l, result.getUsageItemId().longValue());
            Assert.assertEquals(3l, result.getZoneId().longValue());
            Assert.assertEquals(4l, result.getAccountId().longValue());
            Assert.assertEquals(5l, result.getDomainId().longValue());
            Assert.assertEquals(entry.getKey().intValue(), result.getUsageType());
            Assert.assertEquals(BigDecimal.TEN, result.getQuotaUsed());
            Assert.assertEquals(startDate, result.getStartDate());
            Assert.assertEquals(endDate, result.getEndDate());
        });
    }

    @Test
    public void getQuotaTariffValueToBeAppliedTestActivationRuleIsNullReturnTariffValue() {
        Mockito.doReturn(null).when(quotaTariffVoMock).getActivationRule();
        Mockito.doReturn(BigDecimal.ONE).when(quotaTariffVoMock).getCurrencyValue();

        BigDecimal result = quotaManagerImplSpy.getQuotaTariffValueToBeApplied(quotaTariffVoMock, null, null);

        Assert.assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void getQuotaTariffValueToBeAppliedTestActivationRuleIsEmptyReturnTariffValue() {
        Mockito.doReturn("").when(quotaTariffVoMock).getActivationRule();
        Mockito.doReturn(BigDecimal.TEN).when(quotaTariffVoMock).getCurrencyValue();

        BigDecimal result = quotaManagerImplSpy.getQuotaTariffValueToBeApplied(quotaTariffVoMock, null, null);

        Assert.assertEquals(BigDecimal.TEN, result);
    }

    @Test
    public void getQuotaTariffValueToBeAppliedTestScriptResultIsNumberReturnIt() {
        BigDecimal expected = new BigDecimal(50.1);

        Mockito.doReturn(" ").when(quotaTariffVoMock).getActivationRule();
        Mockito.doReturn(BigDecimal.TEN).when(quotaTariffVoMock).getCurrencyValue();
        Mockito.doNothing().when(quotaManagerImplSpy).injectPresetVariablesIntoJsInterpreter(Mockito.any(), Mockito.any());
        Mockito.doReturn(expected).when(jsInterpreterMock).executeScript(Mockito.anyString());

        BigDecimal result = quotaManagerImplSpy.getQuotaTariffValueToBeApplied(quotaTariffVoMock, jsInterpreterMock, presetVariablesMock);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getQuotaTariffValueToBeAppliedTestScriptResultIsTrueReturnTariffValue() {
        BigDecimal expected = new BigDecimal(236.84);

        Mockito.doReturn(" ").when(quotaTariffVoMock).getActivationRule();
        Mockito.doReturn(expected).when(quotaTariffVoMock).getCurrencyValue();
        Mockito.doNothing().when(quotaManagerImplSpy).injectPresetVariablesIntoJsInterpreter(Mockito.any(), Mockito.any());
        Mockito.doReturn(true).when(jsInterpreterMock).executeScript(Mockito.anyString());

        BigDecimal result = quotaManagerImplSpy.getQuotaTariffValueToBeApplied(quotaTariffVoMock, jsInterpreterMock, presetVariablesMock);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getQuotaTariffValueToBeAppliedTestScriptResultIsFalseReturnZero() {
        Mockito.doReturn(" ").when(quotaTariffVoMock).getActivationRule();
        Mockito.doReturn(BigDecimal.TEN).when(quotaTariffVoMock).getCurrencyValue();
        Mockito.doNothing().when(quotaManagerImplSpy).injectPresetVariablesIntoJsInterpreter(Mockito.any(), Mockito.any());
        Mockito.doReturn(false).when(jsInterpreterMock).executeScript(Mockito.anyString());

        BigDecimal result = quotaManagerImplSpy.getQuotaTariffValueToBeApplied(quotaTariffVoMock, jsInterpreterMock, presetVariablesMock);

        Assert.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void getQuotaTariffValueToBeAppliedTestScriptResultIsNotBooleanNorNumericReturnZero() {
        Mockito.doReturn(" ").when(quotaTariffVoMock).getActivationRule();
        Mockito.doReturn(BigDecimal.TEN).when(quotaTariffVoMock).getCurrencyValue();
        Mockito.doNothing().when(quotaManagerImplSpy).injectPresetVariablesIntoJsInterpreter(Mockito.any(), Mockito.any());
        Mockito.doReturn("test").when(jsInterpreterMock).executeScript(Mockito.anyString());

        BigDecimal result = quotaManagerImplSpy.getQuotaTariffValueToBeApplied(quotaTariffVoMock, jsInterpreterMock, presetVariablesMock);

        Assert.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void getPresetVariablesTestDoesNotHaveTariffsWithActivationRuleReturnNull() {
        PresetVariables result = quotaManagerImplSpy.getPresetVariables(false, usageVoMock);
        Assert.assertNull(result);
    }

    @Test
    public void getPresetVariablesTestHasTariffsWithActivationRuleReturnPresetVariables() {
        Mockito.doReturn(presetVariablesMock).when(presetVariableHelperMock).getPresetVariables(Mockito.any());
        PresetVariables result = quotaManagerImplSpy.getPresetVariables(true, usageVoMock);
        Assert.assertEquals(presetVariablesMock, result);
    }

    @Test
    public void aggregateQuotaTariffsValuesTestTariffsWereNotInPeriodToBeAppliedReturnZero() {
        List<QuotaTariffVO> tariffs = new ArrayList<>();
        tariffs.add(new QuotaTariffVO());
        tariffs.add(new QuotaTariffVO());
        tariffs.add(new QuotaTariffVO());

        Mockito.doReturn(false).when(quotaManagerImplSpy).isQuotaTariffInPeriodToBeApplied(Mockito.any(), Mockito.any(), Mockito.anyString());
        BigDecimal result = quotaManagerImplSpy.aggregateQuotaTariffsValues(usageVoMock, tariffs, false, jsInterpreterMock, "");

        Assert.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void aggregateQuotaTariffsValuesTestTariffsIsEmptyReturnZero() {
        BigDecimal result = quotaManagerImplSpy.aggregateQuotaTariffsValues(usageVoMock, new ArrayList<>(), false, jsInterpreterMock, "");

        Assert.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void aggregateQuotaTariffsValuesTestTariffsAreInPeriodToBeAppliedReturnAggregation() {
        List<QuotaTariffVO> tariffs = new ArrayList<>();
        tariffs.add(new QuotaTariffVO());
        tariffs.add(new QuotaTariffVO());
        tariffs.add(new QuotaTariffVO());

        Mockito.doReturn(true, false, true).when(quotaManagerImplSpy).isQuotaTariffInPeriodToBeApplied(Mockito.any(), Mockito.any(), Mockito.anyString());
        Mockito.doReturn(BigDecimal.TEN).when(quotaManagerImplSpy).getQuotaTariffValueToBeApplied(Mockito.any(), Mockito.any(), Mockito.any());
        BigDecimal result = quotaManagerImplSpy.aggregateQuotaTariffsValues(usageVoMock, tariffs, false, jsInterpreterMock, "");

        Assert.assertEquals(BigDecimal.TEN.multiply(new BigDecimal(2)), result);
    }

    @Test
    public void persistUsagesAndQuotaUsagesAndRetrievePersistedQuotaUsagesTestReturnOnlyPersistedQuotaUsageVo() {
        List<Pair<UsageVO, QuotaUsageVO>> listPair = new ArrayList<>();
        QuotaUsageVO quotaUsageVoMock1 = Mockito.mock(QuotaUsageVO.class);
        QuotaUsageVO quotaUsageVoMock2 = Mockito.mock(QuotaUsageVO.class);

        listPair.add(new Pair<>(new UsageVO(), quotaUsageVoMock1));
        listPair.add(new Pair<>(new UsageVO(), null));
        listPair.add(new Pair<>(new UsageVO(), quotaUsageVoMock2));

        Mockito.doReturn(null).when(usageDaoMock).persistUsage(Mockito.any());
        Mockito.doReturn(null).when(quotaUsageDaoMock).persistQuotaUsage(Mockito.any());

        List<QuotaUsageVO> result = quotaManagerImplSpy.persistUsagesAndQuotaUsagesAndRetrievePersistedQuotaUsages(listPair);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals(quotaUsageVoMock1, result.get(0));
        Assert.assertEquals(quotaUsageVoMock2, result.get(1));
    }
}
