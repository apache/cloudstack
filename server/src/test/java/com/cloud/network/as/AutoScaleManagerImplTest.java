// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.as;

import com.cloud.api.dispatch.DispatchChain;
import com.cloud.api.dispatch.DispatchChainFactory;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.as.dao.AutoScalePolicyConditionMapDao;
import com.cloud.network.as.dao.AutoScalePolicyDao;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmProfileDao;
import com.cloud.network.as.dao.ConditionDao;
import com.cloud.network.as.dao.CounterDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.autoscale.CreateCounterCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateConditionCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListCountersCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateAutoScaleVmProfileCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateConditionCmd;
import org.apache.cloudstack.api.command.user.vm.DeployVMCmd;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class AutoScaleManagerImplTest {

    @Spy
    @InjectMocks
    AutoScaleManagerImpl autoScaleManagerImplSpy = new AutoScaleManagerImpl();

    @Mock
    CounterDao counterDao;

    @Mock
    ConditionDao conditionDao;

    @Mock
    AutoScalePolicyDao asPolicyDao;

    @Mock
    AccountManager accountManager;

    @Mock
    EntityManager entityManager;

    @Mock
    DispatchChainFactory dispatchChainFactory;

    @Mock
    AutoScalePolicyConditionMapDao autoScalePolicyConditionMapDao;

    @Mock
    AutoScaleVmGroupPolicyMapDao autoScaleVmGroupPolicyMapDao;

    @Mock
    AutoScaleVmGroupDao autoScaleVmGroupDao;

    @Mock
    AutoScaleVmProfileDao autoScaleVmProfileDao;

    @Mock
    UserDao userDao;

    AccountVO account;
    UserVO user;

    final static String INVALID = "invalid";

    private static final Long counterId = 1L;
    private static final String counterName = "counter name";
    private static final Counter.Source counterSource = Counter.Source.CPU;
    private static final String counterValue = "counter value";
    private static final String counterProvider = "VIRTUALROUTER";

    private static final Long conditionId = 2L;
    private static final Long threshold = 100L;
    private static final Condition.Operator relationalOperator = Condition.Operator.GT;

    private static final Long scaleUpPolicyId = 11L;
    private static final int scaleUpPolicyDuration = 61;
    private static final int scaleUpPolicyQuietTime = 31;
    private static final Date scaleUpPolicyLastQuietTime = new Date();

    private static final Long scaleDownPolicyId = 12L;
    private static final int scaleDownPolicyDuration = 62;
    private static final int scaleDownPolicyQuietTime = 32;
    private static final Date scaleDownPolicyLastQuietTime = new Date();

    private static final String zoneUuid = "1111-1111-1112";
    private static final String domainUuid = "1111-1111-1113";
    private static final String serviceOfferingUuid = "1111-1111-1114";
    private static final String templateUuid = "1111-1111-1115";
    private static final Long zoneId = 1L;
    private static final Long domainId = 2L;
    private static final Long serviceOfferingId = 3L;
    private static final Long templateId = 4L;
    private static final Long accountId = 5L;
    private static final String accountName = "test-user";
    private static final Map<String, HashMap<String, String>> otherDeployParams = new HashMap<>();
    private static final Map<String, HashMap<String, String>> counterParamList = new HashMap<>();
    private static final Integer destroyVmGraceperiod = 33;
    private static final String cloudStackApiUrl = "cloudstack url";
    private static final String autoScaleUserApiKey = "cloudstack api key";
    private static final String autoScaleUserSecretKey = "cloudstack secret key";
    private static final String vmName = "vm name";
    private static final String networkId = "1111-1111-1116";
    private static final Long vmProfileId = 23L;

    private static final Long vmGroupId = 22L;
    private static final String vmGroupUuid = "2222-2222-1111";
    private static final int minMembers = 2;
    private static final int maxMembers = 3;
    private static final int memberPort = 8080;
    private static final int interval = 30;
    private static final Long loadBalancerId = 21L;

    private static final Long autoScaleUserId = 24L;

    @Mock
    DataCenterVO zoneMock;
    @Mock
    ServiceOfferingVO serviceOfferingMock;
    @Mock
    VMTemplateVO templateMock;
    @Mock
    CounterVO counterMock;
    @Mock
    ConditionVO conditionMock;
    @Mock
    AutoScaleVmGroupVO asVmGroupMock;
    @Mock
    AutoScaleVmProfileVO asVmProfileMock;
    @Mock
    AutoScalePolicyVO asPolicyMock;
    @Mock
    AutoScalePolicyConditionMapVO autoScalePolicyConditionMapVOMock;
    @Mock
    AutoScaleVmGroupPolicyMapVO autoScaleVmGroupPolicyMapVOMock;
    @Mock
    UserVO userMock;

    @Before
    public void setUp() {

        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(2L);
        user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        when(counterDao.persist(any(CounterVO.class))).thenReturn(counterMock);
        when(counterDao.findById(anyLong())).thenReturn(counterMock);
        when(conditionDao.findById(any())).thenReturn(conditionMock);
        when(conditionDao.persist(any(ConditionVO.class))).thenReturn(conditionMock);

        when(accountManager.finalizeOwner(nullable(Account.class), nullable(String.class), nullable(Long.class), nullable(Long.class))).thenReturn(account);
        Mockito.doNothing().when(accountManager).checkAccess(Mockito.any(Account.class), Mockito.isNull(), Mockito.anyBoolean(), Mockito.any(Account.class));

        when(asPolicyDao.persist(any(AutoScalePolicyVO.class))).thenReturn(asPolicyMock);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testCreateCounterCmd() throws IllegalArgumentException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.NAME, counterName);
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, counterProvider);
        ReflectionTestUtils.setField(cmd, ApiConstants.SOURCE, counterSource.toString());
        ReflectionTestUtils.setField(cmd, ApiConstants.VALUE, counterValue);

        Counter counter = autoScaleManagerImplSpy.createCounter(cmd);

        Assert.assertEquals(counterMock, counter);
        Mockito.verify(counterDao).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCounterCmdWithInvalidSource() throws IllegalArgumentException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.NAME, counterName);
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, counterProvider);
        ReflectionTestUtils.setField(cmd, ApiConstants.SOURCE, INVALID);
        ReflectionTestUtils.setField(cmd, ApiConstants.VALUE, counterValue);

        Counter counter = autoScaleManagerImplSpy.createCounter(cmd);

        Mockito.verify(counterDao, never()).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCounterCmdWithInvalidProvider() throws IllegalArgumentException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.NAME, counterName);
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, INVALID);
        ReflectionTestUtils.setField(cmd, ApiConstants.SOURCE, counterSource.toString());
        ReflectionTestUtils.setField(cmd, ApiConstants.VALUE, counterValue);

        Counter counter = autoScaleManagerImplSpy.createCounter(cmd);

        Mockito.verify(counterDao, never()).persist(Mockito.any());
    }

    @Test
    public void testListCounters() {
        List<CounterVO> countersMock = Arrays.asList(counterMock);
        when(counterDao.listCounters(any(), any(), any(), any(), any(), any())).thenReturn(countersMock);

        ListCountersCmd cmd = new ListCountersCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, counterProvider);

        List<? extends Counter> counters = autoScaleManagerImplSpy.listCounters(cmd);
        Assert.assertEquals(countersMock, counters);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListCountersWithInvalidProvider() {
        ListCountersCmd cmd = new ListCountersCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, INVALID);

        List<? extends Counter> counters = autoScaleManagerImplSpy.listCounters(cmd);
    }

    @Test
    public void testDeleteCounter() throws ResourceInUseException {
        when(counterDao.remove(counterId)).thenReturn(true);

        boolean success = autoScaleManagerImplSpy.deleteCounter(counterId);

        Assert.assertTrue(success);
        Mockito.verify(counterDao).remove(counterId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteCounterInvalidCounter() throws ResourceInUseException {
        when(counterDao.findById(counterId)).thenReturn(null);

        boolean success = autoScaleManagerImplSpy.deleteCounter(counterId);
        Mockito.verify(counterDao, never()).remove(counterId);
    }

    @Test(expected = ResourceInUseException.class)
    public void testDeleteCounterWithUsedCounter() throws ResourceInUseException {
        when(conditionDao.findByCounterId(counterId)).thenReturn(conditionMock);

        boolean success = autoScaleManagerImplSpy.deleteCounter(counterId);
        Mockito.verify(counterDao, never()).remove(counterId);
    }

    @Test
    public void testCreateConditionCmd() throws IllegalArgumentException {
        CreateConditionCmd cmd = new CreateConditionCmd();

        ReflectionTestUtils.setField(cmd, "counterId", counterId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", String.valueOf(relationalOperator));
        ReflectionTestUtils.setField(cmd, "threshold", threshold);

        Condition condition = autoScaleManagerImplSpy.createCondition(cmd);

        Assert.assertEquals(conditionMock, condition);
        Mockito.verify(conditionDao).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateConditionCmdWithInvalidOperator() throws IllegalArgumentException {
        CreateConditionCmd cmd = new CreateConditionCmd();

        ReflectionTestUtils.setField(cmd, "counterId", counterId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", INVALID);
        ReflectionTestUtils.setField(cmd, "threshold", threshold);

        Condition condition = autoScaleManagerImplSpy.createCondition(cmd);

        Mockito.verify(counterDao, never()).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateConditionCmdWithInvalidThreshold() throws IllegalArgumentException {
        CreateConditionCmd cmd = new CreateConditionCmd();

        ReflectionTestUtils.setField(cmd, "counterId", counterId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", String.valueOf(relationalOperator));
        ReflectionTestUtils.setField(cmd, "threshold", -1L);

        Condition condition = autoScaleManagerImplSpy.createCondition(cmd);

        Mockito.verify(counterDao, never()).persist(Mockito.any());
    }

    @Test
    public void testDeleteCondition() throws ResourceInUseException {
        when(autoScalePolicyConditionMapDao.isConditionInUse(conditionId)).thenReturn(false);
        when(conditionDao.remove(conditionId)).thenReturn(true);

        boolean success = autoScaleManagerImplSpy.deleteCondition(conditionId);

        Assert.assertTrue(success);
        Mockito.verify(conditionDao).remove(conditionId);
    }

    @Test(expected = ResourceInUseException.class)
    public void testDeleteConditionWithUsedCondition() throws ResourceInUseException {
        when(autoScalePolicyConditionMapDao.isConditionInUse(conditionId)).thenReturn(true);

        boolean success = autoScaleManagerImplSpy.deleteCondition(conditionId);

        Assert.assertFalse(success);
        Mockito.verify(conditionDao, never()).remove(conditionId);
    }

    @Test
    public void testUpdateCondition() throws ResourceInUseException {
        GenericSearchBuilder<AutoScalePolicyConditionMapVO, Long> searchBuilderMock = Mockito.mock(GenericSearchBuilder.class);
        SearchCriteria<Long> searchCriteriaMock = Mockito.mock(SearchCriteria.class);

        Mockito.doReturn(searchBuilderMock).when(autoScalePolicyConditionMapDao).createSearchBuilder(any());
        when(searchBuilderMock.entity()).thenReturn(autoScalePolicyConditionMapVOMock);
        Mockito.doReturn(searchCriteriaMock).when(searchBuilderMock).create();
        Mockito.doReturn(Arrays.asList()).when(autoScalePolicyConditionMapDao).customSearch(searchCriteriaMock, null);

        when(conditionDao.update(eq(conditionId), any())).thenReturn(true);

        UpdateConditionCmd cmd = new UpdateConditionCmd();

        ReflectionTestUtils.setField(cmd, "id", conditionId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", String.valueOf(relationalOperator));
        ReflectionTestUtils.setField(cmd, "threshold", 100L);

        Condition condition = autoScaleManagerImplSpy.updateCondition(cmd);

        Assert.assertEquals(conditionMock, condition);
        Mockito.verify(conditionDao).update(eq(conditionId), Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateConditionWithInvalidOperator() throws ResourceInUseException {
        UpdateConditionCmd cmd = new UpdateConditionCmd();

        ReflectionTestUtils.setField(cmd, "id", conditionId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", INVALID);
        ReflectionTestUtils.setField(cmd, "threshold", 100L);

        Condition condition = autoScaleManagerImplSpy.updateCondition(cmd);

        Mockito.verify(conditionDao, never()).update(eq(conditionId), Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateConditionWithInvalidThreshold() throws ResourceInUseException {
        UpdateConditionCmd cmd = new UpdateConditionCmd();

        ReflectionTestUtils.setField(cmd, "id", conditionId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", String.valueOf(relationalOperator));
        ReflectionTestUtils.setField(cmd, "threshold", -1L);

        Condition condition = autoScaleManagerImplSpy.updateCondition(cmd);

        Mockito.verify(conditionDao, never()).update(eq(conditionId), Mockito.any());
    }

    @Test(expected = ResourceInUseException.class)
    public void testUpdateConditionWithPolicies() throws ResourceInUseException {
        GenericSearchBuilder<AutoScalePolicyConditionMapVO, Long> genericSearchBuilderMock = Mockito.mock(GenericSearchBuilder.class);
        SearchCriteria<Long> searchCriteriaLongMock = Mockito.mock(SearchCriteria.class);

        Mockito.doReturn(genericSearchBuilderMock).when(autoScalePolicyConditionMapDao).createSearchBuilder(Long.class);
        when(genericSearchBuilderMock.entity()).thenReturn(autoScalePolicyConditionMapVOMock);
        Mockito.doReturn(searchCriteriaLongMock).when(genericSearchBuilderMock).create();
        Mockito.doReturn(Arrays.asList(scaleUpPolicyId)).when(autoScalePolicyConditionMapDao).customSearch(searchCriteriaLongMock, null);

        SearchBuilder<AutoScaleVmGroupPolicyMapVO> searchBuilderMock1 = Mockito.mock(SearchBuilder.class);
        Mockito.doReturn(searchBuilderMock1).when(autoScaleVmGroupPolicyMapDao).createSearchBuilder();
        when(searchBuilderMock1.entity()).thenReturn(autoScaleVmGroupPolicyMapVOMock);

        SearchBuilder<AutoScaleVmGroupVO> searchBuilderMock2 = Mockito.mock(SearchBuilder.class);
        SearchCriteria<AutoScaleVmGroupVO> searchCriteriaMock2 = Mockito.mock(SearchCriteria.class);

        Mockito.doReturn(searchBuilderMock2).when(autoScaleVmGroupDao).createSearchBuilder();
        when(searchBuilderMock2.entity()).thenReturn(asVmGroupMock);
        Mockito.doReturn(searchCriteriaMock2).when(searchBuilderMock2).create();
        Mockito.doReturn(Arrays.asList(asVmGroupMock)).when(autoScaleVmGroupDao).search(searchCriteriaMock2, null);

        UpdateConditionCmd cmd = new UpdateConditionCmd();

        ReflectionTestUtils.setField(cmd, "id", conditionId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", String.valueOf(relationalOperator));
        ReflectionTestUtils.setField(cmd, "threshold", 100L);

        Condition condition = autoScaleManagerImplSpy.updateCondition(cmd);

        Mockito.verify(conditionDao, never()).update(eq(conditionId), Mockito.any());
    }

    @Test
    public void testCreateAutoScalePolicyCmd() throws IllegalArgumentException {
        CreateAutoScalePolicyCmd cmd = new CreateAutoScalePolicyCmd();

        when(entityManager.findById(Condition.class, conditionId)).thenReturn(conditionMock);
        when(conditionMock.getAccountId()).thenReturn(2L);
        when(conditionMock.getDomainId()).thenReturn(1L);

        SearchBuilder<ConditionVO> searchBuilderMock = Mockito.mock(SearchBuilder.class);
        SearchCriteria<ConditionVO> searchCriteriaMock = Mockito.mock(SearchCriteria.class);
        List<ConditionVO> conditions = Arrays.asList(conditionMock);

        Mockito.doReturn(searchBuilderMock).when(conditionDao).createSearchBuilder();
        when(searchBuilderMock.entity()).thenReturn(conditionMock);
        Mockito.doReturn(searchCriteriaMock).when(searchBuilderMock).create();
        Mockito.doReturn(conditions).when(conditionDao).search(searchCriteriaMock, null);

        ReflectionTestUtils.setField(cmd, "_entityMgr", entityManager);
        ReflectionTestUtils.setField(cmd, "conditionIds", Arrays.asList(conditionId));
        ReflectionTestUtils.setField(cmd, "action", AutoScalePolicy.Action.SCALEUP.toString());
        ReflectionTestUtils.setField(cmd, "duration", 300);
        ReflectionTestUtils.setField(cmd, "quietTime", 60);

        AutoScalePolicy policy = autoScaleManagerImplSpy.createAutoScalePolicy(cmd);

        Assert.assertEquals(asPolicyMock, policy);
        Mockito.verify(asPolicyDao).persist(Mockito.any(AutoScalePolicyVO.class));
        Mockito.verify(autoScalePolicyConditionMapDao).persist(Mockito.any(AutoScalePolicyConditionMapVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateAutoScalePolicyCmdWithInvalidAction() throws IllegalArgumentException {
        CreateAutoScalePolicyCmd cmd = new CreateAutoScalePolicyCmd();

        ReflectionTestUtils.setField(cmd, "action", INVALID);
        ReflectionTestUtils.setField(cmd, "duration", 300);
        ReflectionTestUtils.setField(cmd, "quietTime", 60);

        AutoScalePolicy policy = autoScaleManagerImplSpy.createAutoScalePolicy(cmd);

        Mockito.verify(asPolicyDao, never()).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateAutoScalePolicyCmdWithInvalidDuration() throws IllegalArgumentException {
        CreateAutoScalePolicyCmd cmd = new CreateAutoScalePolicyCmd();

        EntityManager entityMgr = Mockito.spy(EntityManager.class);
        when(entityMgr.findById(Condition.class, conditionId)).thenReturn(conditionMock);
        when(conditionMock.getAccountId()).thenReturn(2L);
        when(conditionMock.getDomainId()).thenReturn(1L);

        ReflectionTestUtils.setField(cmd, "_entityMgr", entityMgr);
        ReflectionTestUtils.setField(cmd, "conditionIds", Arrays.asList(conditionId));
        ReflectionTestUtils.setField(cmd, "action", AutoScalePolicy.Action.SCALEUP.toString());
        ReflectionTestUtils.setField(cmd, "duration", -1);
        ReflectionTestUtils.setField(cmd, "quietTime", 60);

        AutoScalePolicy policy = autoScaleManagerImplSpy.createAutoScalePolicy(cmd);

        Mockito.verify(asPolicyDao, never()).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateAutoScalePolicyCmdWithInvalidQuietTime() throws IllegalArgumentException {
        CreateAutoScalePolicyCmd cmd = new CreateAutoScalePolicyCmd();

        EntityManager entityMgr = Mockito.spy(EntityManager.class);
        when(entityMgr.findById(Condition.class, conditionId)).thenReturn(conditionMock);
        when(conditionMock.getAccountId()).thenReturn(2L);
        when(conditionMock.getDomainId()).thenReturn(1L);

        ReflectionTestUtils.setField(cmd, "_entityMgr", entityMgr);
        ReflectionTestUtils.setField(cmd, "conditionIds", Arrays.asList(conditionId));
        ReflectionTestUtils.setField(cmd, "action", AutoScalePolicy.Action.SCALEUP.toString());
        ReflectionTestUtils.setField(cmd, "duration", 300);
        ReflectionTestUtils.setField(cmd, "quietTime", -1);

        AutoScalePolicy policy = autoScaleManagerImplSpy.createAutoScalePolicy(cmd);

        Mockito.verify(asPolicyDao, never()).persist(Mockito.any());
    }

    @Test
    @PrepareForTest(ComponentContext.class)
    public void testCreateAutoScaleVmProfile() {
        when(entityManager.findById(DataCenter.class, zoneId)).thenReturn(zoneMock);
        when(entityManager.findById(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOfferingMock);
        when(entityManager.findByIdIncludingRemoved(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOfferingMock);
        when(entityManager.findById(VirtualMachineTemplate.class, templateId)).thenReturn(templateMock);
        when(serviceOfferingMock.isDynamic()).thenReturn(false);

        DispatchChain dispatchChainMock = Mockito.mock(DispatchChain.class);
        when(dispatchChainFactory.getStandardDispatchChain()).thenReturn(dispatchChainMock);
        Mockito.doNothing().when(dispatchChainMock).dispatch(any());
        PowerMockito.mockStatic(ComponentContext.class);
        when(ComponentContext.inject(DeployVMCmd.class)).thenReturn(Mockito.mock(DeployVMCmd.class));

        when(autoScaleVmProfileDao.persist(any())).thenReturn(asVmProfileMock);
        CreateAutoScaleVmProfileCmd cmd = new CreateAutoScaleVmProfileCmd();

        ReflectionTestUtils.setField(cmd, "zoneId", zoneId);
        ReflectionTestUtils.setField(cmd, "serviceOfferingId", serviceOfferingId);
        ReflectionTestUtils.setField(cmd, "templateId", templateId);
        ReflectionTestUtils.setField(cmd, "destroyVmGraceperiod", destroyVmGraceperiod);
        ReflectionTestUtils.setField(cmd, "otherDeployParams", otherDeployParams);
        ReflectionTestUtils.setField(cmd, "counterParamList", counterParamList);

        AutoScaleVmProfile vmProfile = autoScaleManagerImplSpy.createAutoScaleVmProfile(cmd);

        Assert.assertEquals(asVmProfileMock, vmProfile);
        Mockito.verify(autoScaleVmProfileDao).persist(Mockito.any());
    }

    @Test
    public void testUpdateAutoScaleVmProfile() {
        when(autoScaleVmProfileDao.findById(vmProfileId)).thenReturn(asVmProfileMock);
        when(autoScaleVmGroupDao.listByAll(null, vmProfileId)).thenReturn(new ArrayList<>());
        when(autoScaleVmGroupDao.listByProfile(vmProfileId)).thenReturn(new ArrayList<>());
        when(autoScaleVmProfileDao.persist(any())).thenReturn(asVmProfileMock);

        when(asVmProfileMock.getServiceOfferingId()).thenReturn(serviceOfferingId);
        when(asVmProfileMock.getTemplateId()).thenReturn(templateId);
        when(entityManager.findById(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOfferingMock);
        when(entityManager.findByIdIncludingRemoved(ServiceOffering.class, serviceOfferingId)).thenReturn(serviceOfferingMock);
        when(entityManager.findById(VirtualMachineTemplate.class, templateId)).thenReturn(templateMock);
        when(serviceOfferingMock.isDynamic()).thenReturn(false);

        UpdateAutoScaleVmProfileCmd cmd = new UpdateAutoScaleVmProfileCmd();

        ReflectionTestUtils.setField(cmd, "id", vmProfileId);
        ReflectionTestUtils.setField(cmd, "serviceOfferingId", serviceOfferingId);
        ReflectionTestUtils.setField(cmd, "templateId", templateId);

        AutoScaleVmProfile vmProfile = autoScaleManagerImplSpy.updateAutoScaleVmProfile(cmd);

        Assert.assertEquals(asVmProfileMock, vmProfile);
        Mockito.verify(autoScaleVmProfileDao).persist(Mockito.any());
    }

    @Test
    public void testDeleteAutoScaleVmProfile() {
        when(autoScaleVmProfileDao.findById(vmProfileId)).thenReturn(asVmProfileMock);
        when(autoScaleVmGroupDao.isProfileInUse(vmProfileId)).thenReturn(false);
        when(autoScaleVmProfileDao.remove(vmProfileId)).thenReturn(true);

        boolean result = autoScaleManagerImplSpy.deleteAutoScaleVmProfile(vmProfileId);

        Assert.assertTrue(result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteAutoScaleVmProfileInUse() {
        when(autoScaleVmProfileDao.findById(vmProfileId)).thenReturn(asVmProfileMock);
        when(autoScaleVmGroupDao.isProfileInUse(vmProfileId)).thenReturn(true);

        boolean result = autoScaleManagerImplSpy.deleteAutoScaleVmProfile(vmProfileId);
    }

    @Test
    public void testDeleteAutoScaleVmProfileFail() {
        when(autoScaleVmProfileDao.findById(vmProfileId)).thenReturn(asVmProfileMock);
        when(autoScaleVmGroupDao.isProfileInUse(vmProfileId)).thenReturn(false);
        when(autoScaleVmProfileDao.remove(vmProfileId)).thenReturn(false);

        boolean result = autoScaleManagerImplSpy.deleteAutoScaleVmProfile(vmProfileId);

        Assert.assertFalse(result);
    }

    @Test
    public void testCheckAutoScaleUserSucceed() throws NoSuchFieldException, IllegalAccessException {
        when(userDao.findById(any())).thenReturn(userMock);
        when(userMock.getAccountId()).thenReturn(accountId);
        when(userMock.getApiKey()).thenReturn(autoScaleUserApiKey);
        when(userMock.getSecretKey()).thenReturn(autoScaleUserSecretKey);

        final Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(ApiServiceConfiguration.ApiServletPath, "http://10.10.10.10:8080/client/api");

        autoScaleManagerImplSpy.checkAutoScaleUser(autoScaleUserId, accountId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAutoScaleUserFail1() {
        when(userDao.findById(any())).thenReturn(userMock);
        when(userMock.getAccountId()).thenReturn(accountId);
        when(userMock.getApiKey()).thenReturn(autoScaleUserApiKey);
        when(userMock.getSecretKey()).thenReturn(null);

        autoScaleManagerImplSpy.checkAutoScaleUser(autoScaleUserId, accountId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAutoScaleUserFail2() {
        when(userDao.findById(any())).thenReturn(userMock);
        when(userMock.getAccountId()).thenReturn(accountId);
        when(userMock.getApiKey()).thenReturn(null);

        autoScaleManagerImplSpy.checkAutoScaleUser(autoScaleUserId, accountId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAutoScaleUserFail3() {
        when(userDao.findById(any())).thenReturn(userMock);
        when(userMock.getAccountId()).thenReturn(accountId + 1L);

        autoScaleManagerImplSpy.checkAutoScaleUser(autoScaleUserId, accountId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAutoScaleUserFail4() {
        when(userDao.findById(any())).thenReturn(null);

        autoScaleManagerImplSpy.checkAutoScaleUser(autoScaleUserId, accountId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckAutoScaleUserFail5() throws NoSuchFieldException, IllegalAccessException {
        when(userDao.findById(any())).thenReturn(userMock);
        when(userMock.getAccountId()).thenReturn(accountId);
        when(userMock.getApiKey()).thenReturn(autoScaleUserApiKey);
        when(userMock.getSecretKey()).thenReturn(autoScaleUserSecretKey);

        final Field f = ConfigKey.class.getDeclaredField("_defaultValue");
        f.setAccessible(true);
        f.set(ApiServiceConfiguration.ApiServletPath, "http://localhost:8080/client/api");

        autoScaleManagerImplSpy.checkAutoScaleUser(autoScaleUserId, accountId);
    }

    @Test
    public void testCreateAutoScaleVmGroup() {

    }

    @Test
    public void testCheckValidityAndPersistVmGroup() {

    }

    @Test
    public void testUpdateAutoScaleVmGroup() {

    }

    @Test
    public void testEnableAutoScaleVmGroup() {

    }

    @Test
    public void testDisableAutoScaleVmGroup() {

    }

    @Test
    public void testDeleteAutoScaleVmGroupsByAccount() {

    }

    @Test
    public void testCleanUpAutoScaleResources() {

    }

    @Test
    public void testGetDeployParams() {

    }

    @Test
    public void testCreateNewVM() {

    }

    @Test
    public void gestGetVmNetworkIds() {

    }

    @Test
    public void getVmOverrideDiskOfferingId() {

    }

    @Test
    public void getVmDataDiskSize() {

    }

    @Test
    public void getVmAffinityGroupId() {

    }

    @Test
    public void updateVmDetails() {

    }

    @Test
    public void testDoScaleUp() {

    }

    @Test
    public void testDoScaleDown() {

    }

    @Test
    public void checkAllAutoScaleVmGroups() {

    }

    @Test
    public void checkAutoScaleVmGroup() {

    }

    @Test
    public void isNative() {

    }

    @Test
    public void getHostAndVmIdsMap() {

    }

    @Test
    public void updateHostAndVmIdsMap() {

    }

    @Test
    public void getPolicyCounters() {

    }

    @Test
    public void getAutoscaleAction() {

    }

    @Test
    public void isQuitTimePassForPolicy() {

    }

    @Test
    public void checkConditionsForPolicy() {

    }

    @Test
    public void getPairofCounternameAndDuration() {

    }

    @Test
    public void getNetwork() {

    }

    @Test
    public void getPublicIpAndPort() {

    }

    @Test
    public void checkNetScalerAsGroup() {

    }

    @Test
    public void updateCountersMapWithInstantData() {

    }

    @Test
    public void updateCountersMapWithAggregatedData() {

    }

    @Test
    public void monitorVirtualRouterAsGroup() {

    }

    @Test
    public void checkVirtualRouterAsGroup() {

    }

    @Test
    public void getVmStatsFromHosts() {

    }

    @Test
    public void getVmStatsByIdFromHost() {

    }

    @Test
    public void processVmStatsByIdFromHost() {

    }

    @Test
    public void getNetworkStatsFromVirtualRouter() {

    }

    @Test
    public void setGetAutoScaleMetricsCommandMetrics() {

    }

    @Test
    public void processGetAutoScaleMetricsAnswer() {

    }

    @Test
    public void updateCountersMap() {

    }

    @Test
    public void cleanupAsVmGroupStatistics() {

    }

    @Test
    public void scheduleMonitorTasks() {

    }

    @Test
    public void cancelMonitorTask() {

    }

    @Test
    public void checkIfVmActionAllowed() {

    }

    @Test
    public void destroyVm() {

    }
}
