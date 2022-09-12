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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.network.as.dao.AutoScalePolicyConditionMapDao;
import com.cloud.network.as.dao.AutoScalePolicyDao;
import com.cloud.network.as.dao.AutoScaleVmGroupDao;
import com.cloud.network.as.dao.AutoScaleVmGroupPolicyMapDao;
import com.cloud.network.as.dao.AutoScaleVmProfileDao;
import com.cloud.network.as.dao.ConditionDao;
import com.cloud.network.as.dao.CounterDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.autoscale.CreateCounterCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateAutoScalePolicyCmd;
import org.apache.cloudstack.api.command.user.autoscale.CreateConditionCmd;
import org.apache.cloudstack.api.command.user.autoscale.ListCountersCmd;
import org.apache.cloudstack.api.command.user.autoscale.UpdateConditionCmd;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AutoScaleManagerImplTest {

    @Spy
    @InjectMocks
    AutoScaleManagerImpl autoScaleManagerImplSpy = new AutoScaleManagerImpl();

    @Mock
    CounterDao counterDao;

    @Mock
    CounterVO counterMock;

    @Mock
    ConditionDao conditionDao;

    @Mock
    ConditionVO conditionMock;

    @Mock
    AutoScalePolicyDao asPolicyDao;

    @Mock
    AutoScalePolicyVO asPolicyMock;

    @Mock
    AccountManager accountManager;

    @Mock
    AutoScalePolicyConditionMapDao autoScalePolicyConditionMapDao;

    @Mock
    AutoScaleVmGroupPolicyMapDao autoScaleVmGroupPolicyMapDao;

    @Mock
    AutoScaleVmGroupDao autoScaleVmGroupDao;

    @Mock
    AutoScaleVmProfileDao autoScaleVmProfileDao;

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

    private static final String zoneId = "1111-1111-1112";
    private static final String domainId = "1111-1111-1113";
    private static final String serviceOfferingId = "1111-1111-1114";
    private static final String templateId = "1111-1111-1115";
    private static final String otherDeployParams = "otherDeployParams";
    private static final List<Pair<String, String>> counterParamList = new ArrayList<>();
    private static final Integer destroyVmGraceperiod = 33;
    private static final String cloudStackApiUrl = "cloudstack url";
    private static final String autoScaleUserApiKey = "cloudstack api key";
    private static final String autoScaleUserSecretKey = "cloudstack secret key";
    private static final String vmName = "vm name";
    private static final String networkId = "1111-1111-1116";

    private static final Long vmGroupId = 22L;
    private static final String vmGroupUuid = "2222-2222-1111";
    private static final int minMembers = 2;
    private static final int maxMembers = 3;
    private static final int memberPort = 8080;
    private static final int interval = 30;
    private static final Long loadBalancerId = 21L;

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

        when(asPolicyDao.persist(any(AutoScalePolicyVO.class))).thenReturn(asPolicyMock);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testCreateCounterCmd() throws IllegalArgumentException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.NAME, "test-name");
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, "VirtualRouter");
        ReflectionTestUtils.setField(cmd, ApiConstants.SOURCE, "virtualrouter");
        ReflectionTestUtils.setField(cmd, ApiConstants.VALUE, "test-value");

        Counter counter = autoScaleManagerImplSpy.createCounter(cmd);

        Assert.assertEquals(counterMock, counter);
        Mockito.verify(counterDao).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCounterCmdWithInvalidSource() throws IllegalArgumentException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.NAME, "test-name");
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, "VirtualRouter");
        ReflectionTestUtils.setField(cmd, ApiConstants.SOURCE, INVALID);
        ReflectionTestUtils.setField(cmd, ApiConstants.VALUE, "test-value");

        Counter counter = autoScaleManagerImplSpy.createCounter(cmd);

        Assert.assertNull(counter);
        Mockito.verify(counterDao, never()).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateCounterCmdWithInvalidProvider() throws IllegalArgumentException {
        CreateCounterCmd cmd = new CreateCounterCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.NAME, "test-name");
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, INVALID);
        ReflectionTestUtils.setField(cmd, ApiConstants.SOURCE, "virtualrouter");
        ReflectionTestUtils.setField(cmd, ApiConstants.VALUE, "test-value");

        Counter counter = autoScaleManagerImplSpy.createCounter(cmd);

        Assert.assertNull(counter);
        Mockito.verify(counterDao, never()).persist(Mockito.any());
    }

    @Test
    public void testListCounters() {
        List<CounterVO> countersMock = Arrays.asList(counterMock);
        when(counterDao.listCounters(any(), any(), any(), any(), any(), any())).thenReturn(countersMock);

        ListCountersCmd cmd = new ListCountersCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, "VirtualRouter");

        List<? extends Counter> counters = autoScaleManagerImplSpy.listCounters(cmd);
        Assert.assertEquals(countersMock, counters);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testListCountersWithInvalidProvider() {
        ListCountersCmd cmd = new ListCountersCmd();
        ReflectionTestUtils.setField(cmd, ApiConstants.PROVIDER, INVALID);

        List<? extends Counter> counters = autoScaleManagerImplSpy.listCounters(cmd);
        Assert.assertNull(counters);
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
        Assert.assertFalse(success);
        Mockito.verify(counterDao, never()).remove(counterId);
    }

    @Test(expected = ResourceInUseException.class)
    public void testDeleteCounterWithUsedCounter() throws ResourceInUseException {
        when(conditionDao.findByCounterId(counterId)).thenReturn(conditionMock);

        boolean success = autoScaleManagerImplSpy.deleteCounter(counterId);
        Assert.assertFalse(success);
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

        Assert.assertNull(condition);
        Mockito.verify(counterDao, never()).persist(Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCreateConditionCmdWithInvalidThreshold() throws IllegalArgumentException {
        CreateConditionCmd cmd = new CreateConditionCmd();

        ReflectionTestUtils.setField(cmd, "counterId", counterId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", String.valueOf(relationalOperator));
        ReflectionTestUtils.setField(cmd, "threshold", -1L);

        Condition condition = autoScaleManagerImplSpy.createCondition(cmd);

        Assert.assertNull(condition);
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
        AutoScalePolicyConditionMapVO autoScalePolicyConditionMapVOMock = Mockito.mock(AutoScalePolicyConditionMapVO.class);

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

        Assert.assertNull(condition);
        Mockito.verify(conditionDao, never()).update(eq(conditionId), Mockito.any());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testUpdateConditionWithInvalidThreshold() throws ResourceInUseException {
        UpdateConditionCmd cmd = new UpdateConditionCmd();

        ReflectionTestUtils.setField(cmd, "id", conditionId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", String.valueOf(relationalOperator));
        ReflectionTestUtils.setField(cmd, "threshold", -1L);

        Condition condition = autoScaleManagerImplSpy.updateCondition(cmd);

        Assert.assertNull(condition);
        Mockito.verify(conditionDao, never()).update(eq(conditionId), Mockito.any());
    }

    @Test(expected = ResourceInUseException.class)
    public void testUpdateConditionWithPolicies() throws ResourceInUseException {
        GenericSearchBuilder<AutoScalePolicyConditionMapVO, Long> genericSearchBuilderMock = Mockito.mock(GenericSearchBuilder.class);
        SearchCriteria<Long> searchCriteriaLongMock = Mockito.mock(SearchCriteria.class);
        AutoScalePolicyConditionMapVO autoScalePolicyConditionMapVOMock = Mockito.mock(AutoScalePolicyConditionMapVO.class);

        Mockito.doReturn(genericSearchBuilderMock).when(autoScalePolicyConditionMapDao).createSearchBuilder(Long.class);
        when(genericSearchBuilderMock.entity()).thenReturn(autoScalePolicyConditionMapVOMock);
        Mockito.doReturn(searchCriteriaLongMock).when(genericSearchBuilderMock).create();
        Mockito.doReturn(Arrays.asList(scaleUpPolicyId)).when(autoScalePolicyConditionMapDao).customSearch(searchCriteriaLongMock, null);

        SearchBuilder<AutoScaleVmGroupPolicyMapVO> searchBuilderMock1 = Mockito.mock(SearchBuilder.class);
        AutoScaleVmGroupPolicyMapVO autoScaleVmGroupPolicyMapVOMock = Mockito.mock(AutoScaleVmGroupPolicyMapVO.class);
        Mockito.doReturn(searchBuilderMock1).when(autoScaleVmGroupPolicyMapDao).createSearchBuilder();
        when(searchBuilderMock1.entity()).thenReturn(autoScaleVmGroupPolicyMapVOMock);

        SearchBuilder<AutoScaleVmGroupVO> searchBuilderMock2 = Mockito.mock(SearchBuilder.class);
        SearchCriteria<AutoScaleVmGroupVO> searchCriteriaMock2 = Mockito.mock(SearchCriteria.class);
        AutoScaleVmGroupVO autoScaleVmGroupVOMock = Mockito.mock(AutoScaleVmGroupVO.class);

        Mockito.doReturn(searchBuilderMock2).when(autoScaleVmGroupDao).createSearchBuilder();
        when(searchBuilderMock2.entity()).thenReturn(autoScaleVmGroupVOMock);
        Mockito.doReturn(searchCriteriaMock2).when(searchBuilderMock2).create();
        Mockito.doReturn(Arrays.asList(autoScaleVmGroupVOMock)).when(autoScaleVmGroupDao).search(searchCriteriaMock2, null);

        UpdateConditionCmd cmd = new UpdateConditionCmd();

        ReflectionTestUtils.setField(cmd, "id", conditionId);
        ReflectionTestUtils.setField(cmd, "relationalOperator", String.valueOf(relationalOperator));
        ReflectionTestUtils.setField(cmd, "threshold", 100L);

        Condition condition = autoScaleManagerImplSpy.updateCondition(cmd);

        Assert.assertNull(condition);
        Mockito.verify(conditionDao, never()).update(eq(conditionId), Mockito.any());
    }

    @Test
    public void testCreateAutoScalePolicyCmd() throws IllegalArgumentException {
        CreateAutoScalePolicyCmd cmd = new CreateAutoScalePolicyCmd();

        EntityManager entityMgr = Mockito.spy(EntityManager.class);
        when(entityMgr.findById(Condition.class, conditionId)).thenReturn(conditionMock);
        when(conditionMock.getAccountId()).thenReturn(2L);
        when(conditionMock.getDomainId()).thenReturn(1L);

        SearchBuilder<ConditionVO> searchBuilderMock = Mockito.mock(SearchBuilder.class);
        SearchCriteria<ConditionVO> searchCriteriaMock = Mockito.mock(SearchCriteria.class);
        ConditionVO conditionVOMock = Mockito.mock(ConditionVO.class);
        List<ConditionVO> conditions = Arrays.asList(conditionVOMock);

        Mockito.doReturn(searchBuilderMock).when(conditionDao).createSearchBuilder();
        when(searchBuilderMock.entity()).thenReturn(conditionVOMock);
        Mockito.doReturn(searchCriteriaMock).when(searchBuilderMock).create();
        Mockito.doReturn(conditions).when(conditionDao).search(searchCriteriaMock, null);

        ReflectionTestUtils.setField(cmd, "_entityMgr", entityMgr);
        ReflectionTestUtils.setField(cmd, "conditionIds", Arrays.asList(conditionId));
        ReflectionTestUtils.setField(cmd, "action", "ScaleUp");
        ReflectionTestUtils.setField(cmd, "duration", 300);

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

        AutoScalePolicy policy = autoScaleManagerImplSpy.createAutoScalePolicy(cmd);

        Assert.assertNull(policy);
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
        ReflectionTestUtils.setField(cmd, "action", "ScaleUp");
        ReflectionTestUtils.setField(cmd, "duration", -1);

        AutoScalePolicy policy = autoScaleManagerImplSpy.createAutoScalePolicy(cmd);

        Assert.assertNull(policy);
        Mockito.verify(asPolicyDao, never()).persist(Mockito.any());
    }

    @Test
    public void testCheckValidityAndPersistPolicy() {

    }

    @Test
    public void testCreateAutoScaleVmProfile() {

    }

    @Test
    public void testUpdateAutoScaleVmProfile() {

    }

    @Test
    public void testDeleteAutoScaleVmProfile() {

    }

    @Test
    public void testCheckValidityAndPersistVmProfile() {

    }

    @Test
    public void testCheckAutoScaleUser() {

    }

    @Test
    public void testCreateAutoScaleVmGroup() {

    }

    @Test
    public void testCheckValidityAndPersistVmProfilGroup() {

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
