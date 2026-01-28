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

package org.apache.cloudstack.mom.webhook;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.framework.async.AsyncCallbackDispatcher;
import org.apache.cloudstack.framework.events.Event;
import org.apache.cloudstack.framework.events.EventBusException;
import org.apache.cloudstack.mom.webhook.dao.WebhookDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookDeliveryDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookFilterDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookVO;
import org.apache.cloudstack.utils.cache.LazyCache;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.query.vo.EventJoinVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventCategory;
import com.cloud.event.dao.EventJoinDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;

@RunWith(MockitoJUnitRunner.class)
public class WebhookServiceImplTest {
    @Mock
    EventJoinDao eventJoinDao;
    @Mock
    WebhookDao webhookDao;
    @Mock
    WebhookDeliveryDao webhookDeliveryDao;
    @Mock
    WebhookFilterDao webhookFilterDao;
    @Mock
    ManagementServerHostDao managementServerHostDao;
    @Mock
    DomainDao domainDao;
    @Mock
    AccountManager accountManager;

    @Spy
    @InjectMocks
    private WebhookServiceImpl webhookServiceImpl;

    MockedStatic<ComponentContext> componentContextMockedStatic;

    @Before
    public void setup() {
        componentContextMockedStatic = Mockito.mockStatic(ComponentContext.class);
        componentContextMockedStatic.when(() -> ComponentContext.inject(Mockito.any(WebhookDeliveryThread.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        webhookServiceImpl.initCaches();
    }

    @After
    public void tearDown() {
        componentContextMockedStatic.close();
    }

    @Test
    public void getDeliveryJobReturnsProperlyConfiguredJob() {
        Event event = Mockito.mock(Event.class);
        Webhook webhook = Mockito.mock(Webhook.class);
        Pair<Integer, Integer> configs = new Pair<>(4, 5000);

        Mockito.when(event.getEventId()).thenReturn(123L);
        Mockito.when(webhook.getId()).thenReturn(456L);

        WebhookDeliveryThread job = webhookServiceImpl.getDeliveryJob(event, webhook, configs);

        Assert.assertNotNull(job);
        Assert.assertEquals(4, ReflectionTestUtils.getField(job, "deliveryTries"));
        Assert.assertEquals(5000, ReflectionTestUtils.getField(job, "deliveryTimeout"));
    }

    @Test
    public void getDeliveryJobInjectsDependencies() {
        Event event = Mockito.mock(Event.class);
        Webhook webhook = Mockito.mock(Webhook.class);
        Pair<Integer, Integer> configs = new Pair<>(1, 1000);

        WebhookDeliveryThread job = webhookServiceImpl.getDeliveryJob(event, webhook, configs);

        Mockito.verify(webhookServiceImpl, Mockito.times(1)).getDeliveryJob(event, webhook, configs);
        componentContextMockedStatic.verify(() -> ComponentContext.inject(job), Mockito.times(1));
    }

    @Test
    public void getEventValueByFilterTypeReturnsEventTypeWhenFilterTypeIsEventType() {
        Event event = Mockito.mock(Event.class);
        Mockito.when(event.getEventType()).thenReturn("USER.LOGIN");

        String result = webhookServiceImpl.getEventValueByFilterType(event, WebhookFilter.Type.EventType);

        Assert.assertEquals("USER.LOGIN", result);
    }

    @Test
    public void getEventValueByFilterTypeReturnsNullWhenFilterTypeIsNotEventType() {
        Event event = Mockito.mock(Event.class);

        String result = webhookServiceImpl.getEventValueByFilterType(event, null);

        Assert.assertNull(result);
    }

    @Test
    public void isValueMatchingFilterReturnsTrueForExactMatch() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Exact, "USER.LOGIN");

        Assert.assertTrue(result);
    }

    @Test
    public void isValueMatchingFilterReturnsFalseForNonExactMatch() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Exact, "USER.LOGOUT");

        Assert.assertFalse(result);
    }

    @Test
    public void isValueMatchingFilterReturnsTrueForPrefixMatch() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Prefix, "USER");

        Assert.assertTrue(result);
    }

    @Test
    public void isValueMatchingFilterReturnsFalseForNonPrefixMatch() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Prefix, "ADMIN");

        Assert.assertFalse(result);
    }

    @Test
    public void isValueMatchingFilterReturnsTrueForSuffixMatch() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Suffix, "LOGIN");

        Assert.assertTrue(result);
    }

    @Test
    public void isValueMatchingFilterReturnsFalseForNonSuffixMatch() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Suffix, "LOGOUT");

        Assert.assertFalse(result);
    }

    @Test
    public void isValueMatchingFilterReturnsTrueForContainsMatch() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Contains, "USER");

        Assert.assertTrue(result);
    }

    @Test
    public void isValueMatchingFilterReturnsFalseForNonContainsMatch() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Contains, "ADMIN");

        Assert.assertFalse(result);
    }

    @Test
    public void isValueMatchingFilterReturnsFalseForNullFilterValue() {
        boolean result = webhookServiceImpl.isValueMatchingFilter("USER.LOGIN", WebhookFilter.MatchType.Exact, null);

        Assert.assertFalse(result);
    }

    @Test
    public void isEventMatchingFiltersReturnsTrueWhenFiltersAreEmpty() {
        List<WebhookFilter> filters = new ArrayList<>();

        boolean result = webhookServiceImpl.isEventMatchingFilters(Mockito.mock(Event.class), filters);

        Assert.assertTrue(result);
    }

    @Test
    public void isEventMatchingFiltersReturnsFalseWhenEventMatchesExcludeFilter() {
        Event event = Mockito.mock(Event.class);
        WebhookFilter excludeFilter = Mockito.mock(WebhookFilter.class);

        Mockito.when(excludeFilter.getMode()).thenReturn(WebhookFilter.Mode.Exclude);
        Mockito.when(excludeFilter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(excludeFilter.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(excludeFilter.getValue()).thenReturn("USER.LOGIN");
        Mockito.when(event.getEventType()).thenReturn("USER.LOGIN");

        List<WebhookFilter> filters = List.of(excludeFilter);

        boolean result = webhookServiceImpl.isEventMatchingFilters(event, filters);

        Assert.assertFalse(result);
    }

    @Test
    public void isEventMatchingFiltersReturnsTrueWhenEventMatchesIncludeFilter() {
        Event event = Mockito.mock(Event.class);
        WebhookFilter includeFilter = Mockito.mock(WebhookFilter.class);

        Mockito.when(includeFilter.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(includeFilter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(includeFilter.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(includeFilter.getValue()).thenReturn("USER.LOGIN");
        Mockito.when(event.getEventType()).thenReturn("USER.LOGIN");

        List<WebhookFilter> filters = List.of(includeFilter);

        boolean result = webhookServiceImpl.isEventMatchingFilters(event, filters);

        Assert.assertTrue(result);
    }

    @Test
    public void isEventMatchingFiltersReturnsFalseWhenEventDoesNotMatchAnyIncludeFilter() {
        Event event = Mockito.mock(Event.class);
        WebhookFilter includeFilter = Mockito.mock(WebhookFilter.class);

        Mockito.when(includeFilter.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(includeFilter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(includeFilter.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(includeFilter.getValue()).thenReturn("USER.LOGOUT");
        Mockito.when(event.getEventType()).thenReturn("USER.LOGIN");

        List<WebhookFilter> filters = List.of(includeFilter);

        boolean result = webhookServiceImpl.isEventMatchingFilters(event, filters);

        Assert.assertFalse(result);
    }

    @Test
    public void isEventMatchingFiltersReturnsTrueWhenEventMatchesAtLeastOneIncludeFilter() {
        Event event = Mockito.mock(Event.class);
        WebhookFilter includeFilter1 = Mockito.mock(WebhookFilter.class);
        WebhookFilter includeFilter2 = Mockito.mock(WebhookFilter.class);

        Mockito.when(includeFilter1.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(includeFilter1.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(includeFilter1.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(includeFilter1.getValue()).thenReturn("USER.LOGOUT");

        Mockito.when(includeFilter2.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(includeFilter2.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(includeFilter2.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(includeFilter2.getValue()).thenReturn("USER.LOGIN");

        Mockito.when(event.getEventType()).thenReturn("USER.LOGIN");

        List<WebhookFilter> filters = List.of(includeFilter1, includeFilter2);

        boolean result = webhookServiceImpl.isEventMatchingFilters(event, filters);

        Assert.assertTrue(result);
    }

    @Test
    public void isEventMatchingFiltersReturnsFalseWhenEventMatchesExcludeFilterEvenWithIncludeFilters() {
        Event event = Mockito.mock(Event.class);
        WebhookFilter excludeFilter = Mockito.mock(WebhookFilter.class);
        WebhookFilter includeFilter = Mockito.mock(WebhookFilter.class);

        Mockito.when(includeFilter.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(includeFilter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(includeFilter.getMatchType()).thenReturn(WebhookFilter.MatchType.Prefix);
        Mockito.when(includeFilter.getValue()).thenReturn("USER.");

        Mockito.when(excludeFilter.getMode()).thenReturn(WebhookFilter.Mode.Exclude);
        Mockito.when(excludeFilter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(excludeFilter.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(excludeFilter.getValue()).thenReturn("USER.LOGIN");

        Mockito.when(event.getEventType()).thenReturn("USER.LOGIN");

        List<WebhookFilter> filters = List.of(includeFilter, excludeFilter);

        boolean result = webhookServiceImpl.isEventMatchingFilters(event, filters);

        Assert.assertFalse(result);
    }

    @Test
    public void getDeliveryJobsReturnsEmptyListWhenEventCategoryIsNotActionEvent() throws EventBusException {
        Event event = Mockito.mock(Event.class);
        Mockito.when(event.getEventCategory()).thenReturn("NON_ACTION_EVENT");

        List<Runnable> jobs = webhookServiceImpl.getDeliveryJobs(event);

        Assert.assertTrue(jobs.isEmpty());
    }

    @Test
    public void getDeliveryJobsThrowsExceptionWhenEventAccountIdIsNull() {
        Event event = Mockito.mock(Event.class);
        Mockito.when(event.getEventCategory()).thenReturn(EventCategory.ACTION_EVENT.getName());
        Mockito.when(event.getResourceAccountId()).thenReturn(null);

        Assert.assertThrows(EventBusException.class, () -> webhookServiceImpl.getDeliveryJobs(event));
    }

    @Test
    public void getDeliveryJobsReturnsEmptyListWhenNoWebhooksMatchFilters() throws EventBusException {
        Event event = Mockito.mock(Event.class);
        Mockito.when(event.getEventCategory()).thenReturn(EventCategory.ACTION_EVENT.getName());
        Mockito.when(event.getResourceAccountId()).thenReturn(1L);
        Mockito.when(event.getResourceDomainId()).thenReturn(2L);
        Mockito.when(domainDao.getDomainParentIds(2L)).thenReturn(Set.of(3L));

        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        Mockito.when(webhook.getId()).thenReturn(1L);

        Mockito.when(webhookDao.listByEnabledForDelivery(Mockito.anyLong(), Mockito.anyList())).thenReturn(List.of(webhook));
        Mockito.when(webhookFilterDao.listByWebhook(Mockito.anyLong())).thenReturn(List.of());
        Mockito.doReturn(false).when(webhookServiceImpl).isEventMatchingFilters(Mockito.any(), Mockito.anyList());

        List<Runnable> jobs = webhookServiceImpl.getDeliveryJobs(event);

        Assert.assertTrue(jobs.isEmpty());
    }

    @Test
    public void getDeliveryJobsCreatesJobsForMatchingWebhooks() throws EventBusException {
        Event event = Mockito.mock(Event.class);
        Mockito.when(event.getEventCategory()).thenReturn(EventCategory.ACTION_EVENT.getName());
        Mockito.when(event.getResourceAccountId()).thenReturn(1L);
        Mockito.when(event.getResourceDomainId()).thenReturn(2L);
        Mockito.when(domainDao.getDomainParentIds(2L)).thenReturn(Set.of(3L));

        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        Mockito.when(webhook.getId()).thenReturn(1L);
        Mockito.when(webhook.getDomainId()).thenReturn(2L);

        Mockito.when(webhookDao.listByEnabledForDelivery(Mockito.anyLong(), Mockito.anyList())).thenReturn(List.of(webhook));
        Mockito.when(webhookFilterDao.listByWebhook(Mockito.anyLong())).thenReturn(List.of());
        Mockito.doReturn(true).when(webhookServiceImpl).isEventMatchingFilters(Mockito.any(), Mockito.anyList());
        Mockito.doReturn(Mockito.mock(WebhookDeliveryThread.class)).when(webhookServiceImpl).getDeliveryJob(Mockito.any(), Mockito.any(), Mockito.any());

        List<Runnable> jobs = webhookServiceImpl.getDeliveryJobs(event);

        Assert.assertEquals(1, jobs.size());
    }

    @Test
    public void getDeliveryJobsUsesCachedDomainConfigs() throws EventBusException {
        Event event = Mockito.mock(Event.class);
        Mockito.when(event.getEventCategory()).thenReturn(EventCategory.ACTION_EVENT.getName());
        Mockito.when(event.getResourceAccountId()).thenReturn(1L);
        Mockito.when(event.getResourceDomainId()).thenReturn(2L);
        Mockito.when(domainDao.getDomainParentIds(2L)).thenReturn(Set.of(3L));

        WebhookVO webhook1 = Mockito.mock(WebhookVO.class);
        Mockito.when(webhook1.getId()).thenReturn(1L);
        Mockito.when(webhook1.getDomainId()).thenReturn(2L);

        WebhookVO webhook2 = Mockito.mock(WebhookVO.class);
        Mockito.when(webhook2.getId()).thenReturn(2L);
        Mockito.when(webhook2.getDomainId()).thenReturn(2L);

        Mockito.when(webhookDao.listByEnabledForDelivery(Mockito.anyLong(), Mockito.anyList())).thenReturn(List.of(webhook1, webhook2));
        Mockito.when(webhookFilterDao.listByWebhook(Mockito.anyLong())).thenReturn(List.of());
        Mockito.doReturn(true).when(webhookServiceImpl).isEventMatchingFilters(Mockito.any(), Mockito.anyList());
        Mockito.doReturn(Mockito.mock(WebhookDeliveryThread.class)).when(webhookServiceImpl).getDeliveryJob(Mockito.any(), Mockito.any(), Mockito.any());

        List<Runnable> jobs = webhookServiceImpl.getDeliveryJobs(event);

        Assert.assertEquals(2, jobs.size());
        Mockito.verify(webhookServiceImpl, Mockito.times(1)).getDeliveryJob(Mockito.eq(event), Mockito.eq(webhook1), Mockito.any());
        Mockito.verify(webhookServiceImpl, Mockito.times(1)).getDeliveryJob(Mockito.eq(event), Mockito.eq(webhook2), Mockito.any());
    }

    @Test
    public void getManualDeliveryJobCreatesJobWithDefaultPayloadWhenPayloadIsBlank() {
        Webhook webhook = Mockito.mock(Webhook.class);
        CompletableFuture<WebhookDeliveryThread.WebhookDeliveryResult> future = Mockito.mock(CompletableFuture.class);
        Account account = Mockito.mock(Account.class);

        Mockito.when(webhook.getAccountId()).thenReturn(2L);
        Mockito.when(accountManager.getAccount(Mockito.anyLong())).thenReturn(account);

        Runnable job = webhookServiceImpl.getManualDeliveryJob(null, webhook, "   ", future);

        Assert.assertNotNull(job);
        Event event = (Event) ReflectionTestUtils.getField(job, "event");
        Assert.assertNotNull(event);
        Assert.assertTrue(StringUtils.isNotBlank(event.getDescription()));
    }

    @Test
    public void getManualDeliveryJobCreatesJobWithExistingDeliveryDetails() {
        WebhookDelivery existingDelivery = Mockito.mock(WebhookDelivery.class);
        Webhook webhook = Mockito.mock(Webhook.class);
        EventJoinVO eventJoinVO = Mockito.mock(EventJoinVO.class);
        CompletableFuture<WebhookDeliveryThread.WebhookDeliveryResult> future = Mockito.mock(CompletableFuture.class);

        Mockito.when(existingDelivery.getEventId()).thenReturn(123L);
        Mockito.when(eventJoinDao.findById(123L)).thenReturn(eventJoinVO);
        Mockito.when(eventJoinVO.getId()).thenReturn(123L);
        Mockito.when(eventJoinVO.getType()).thenReturn("TEST.EVENT");
        Mockito.when(eventJoinVO.getUuid()).thenReturn("test-uuid");
        Mockito.when(existingDelivery.getPayload()).thenReturn("test-payload");
        Mockito.when(eventJoinVO.getAccountUuid()).thenReturn("account-uuid");

        Runnable job = webhookServiceImpl.getManualDeliveryJob(existingDelivery, webhook, null, future);

        Assert.assertNotNull(job);
        Mockito.verify(eventJoinDao, Mockito.times(1)).findById(123L);
    }

    @Test
    public void getManualDeliveryJobCreatesJobWithWebhookAccountDetailsWhenNoExistingDelivery() {
        Webhook webhook = Mockito.mock(Webhook.class);
        Account account = Mockito.mock(Account.class);
        CompletableFuture<WebhookDeliveryThread.WebhookDeliveryResult> future = Mockito.mock(CompletableFuture.class);

        Mockito.when(webhook.getAccountId()).thenReturn(1L);
        Mockito.when(accountManager.getAccount(1L)).thenReturn(account);
        Mockito.when(account.getUuid()).thenReturn("account-uuid");

        Runnable job = webhookServiceImpl.getManualDeliveryJob(null, webhook, "test-payload", future);

        Assert.assertNotNull(job);
        Mockito.verify(accountManager, Mockito.times(1)).getAccount(1L);
    }

    @Test
    public void getManualDeliveryJobSetsDeliveryTriesAndTimeoutFromWebhookDomain() {
        Webhook webhook = Mockito.mock(Webhook.class);
        CompletableFuture<WebhookDeliveryThread.WebhookDeliveryResult> future = Mockito.mock(CompletableFuture.class);
        Account account = Mockito.mock(Account.class);

        Mockito.when(webhook.getDomainId()).thenReturn(2L);
        Mockito.when(webhook.getAccountId()).thenReturn(2L);
        Mockito.when(accountManager.getAccount(Mockito.anyLong())).thenReturn(account);

        WebhookDeliveryThread job = (WebhookDeliveryThread) webhookServiceImpl.getManualDeliveryJob(null, webhook, "test-payload", future);

        Assert.assertEquals(3, ReflectionTestUtils.getField(job, "deliveryTries"));
        Assert.assertEquals(10, ReflectionTestUtils.getField(job, "deliveryTimeout"));
    }

    @Test
    public void deliveryCompleteCallbackPersistsDeliveryVO() {
        WebhookDeliveryThread.WebhookDeliveryResult result = Mockito.mock(WebhookDeliveryThread.WebhookDeliveryResult.class);
        WebhookDeliveryThread.WebhookDeliveryContext<Webhook> context = Mockito.mock(WebhookDeliveryThread.WebhookDeliveryContext.class);

        Mockito.when(context.getEventId()).thenReturn(123L);
        Mockito.when(context.getRuleId()).thenReturn(456L);
        Mockito.when(result.getHeaders()).thenReturn("headers");
        Mockito.when(result.getPayload()).thenReturn("payload");
        Mockito.when(result.isSuccess()).thenReturn(true);
        Mockito.when(result.getResult()).thenReturn("result");

        AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDeliveryThread.WebhookDeliveryResult> callback = Mockito.mock(AsyncCallbackDispatcher.class);
        Mockito.when(callback.getResult()).thenReturn(result);

        webhookServiceImpl.deliveryCompleteCallback(callback, context);

        Mockito.verify(webhookDeliveryDao, Mockito.times(1)).persist(Mockito.any(WebhookDeliveryVO.class));
    }

    @Test
    public void manualDeliveryCompleteCallbackCompletesFuture() {
        WebhookDeliveryThread.WebhookDeliveryResult result = Mockito.mock(WebhookDeliveryThread.WebhookDeliveryResult.class);
        WebhookServiceImpl.ManualDeliveryContext<WebhookDeliveryThread.WebhookDeliveryResult> context = Mockito.mock(WebhookServiceImpl.ManualDeliveryContext.class);
        CompletableFuture<WebhookDeliveryThread.WebhookDeliveryResult> future = Mockito.mock(CompletableFuture.class);

        Mockito.when(context.getFuture()).thenReturn(future);
        AsyncCallbackDispatcher<WebhookServiceImpl, WebhookDeliveryThread.WebhookDeliveryResult> callback = Mockito.mock(AsyncCallbackDispatcher.class);
        Mockito.when(callback.getResult()).thenReturn(result);

        webhookServiceImpl.manualDeliveryCompleteCallback(callback, context);

        Mockito.verify(future, Mockito.times(1)).complete(result);
    }

    @Test
    public void cleanupOldWebhookDeliveriesProcessesAllWebhooks() {
        WebhookVO webhook1 = Mockito.mock(WebhookVO.class);
        WebhookVO webhook2 = Mockito.mock(WebhookVO.class);

        Mockito.when(webhook1.getId()).thenReturn(1L);
        Mockito.when(webhook2.getId()).thenReturn(2L);

        List<WebhookVO> webhooks = List.of(webhook1, webhook2);
        Pair<List<WebhookVO>, Integer> webhooksAndCount = new Pair<>(webhooks, 2);

        Mockito.when(webhookDao.searchAndCount(Mockito.any(), Mockito.any())).thenReturn(webhooksAndCount);

        long processed = webhookServiceImpl.cleanupOldWebhookDeliveries(10);

        Assert.assertEquals(2, processed);
        Mockito.verify(webhookDeliveryDao, Mockito.times(1)).removeOlderDeliveries(1L, 10);
        Mockito.verify(webhookDeliveryDao, Mockito.times(1)).removeOlderDeliveries(2L, 10);
    }

    @Test
    public void listWebhooksByAccountReturnsEmptyListWhenNoWebhooksExist() {
        Mockito.when(webhookDao.listByAccount(1L)).thenReturn(new ArrayList<>());

        List<? extends ControlledEntity> result = webhookServiceImpl.listWebhooksByAccount(1L);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void listWebhooksByAccountReturnsWebhooksForValidAccount() {
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        Mockito.when(webhookDao.listByAccount(1L)).thenReturn(List.of(webhook));

        List<? extends ControlledEntity> result = webhookServiceImpl.listWebhooksByAccount(1L);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals(webhook, result.get(0));
    }

    @Test
    public void handleEventSubmitsJobsToExecutor() throws EventBusException {
        Event event = Mockito.mock(Event.class);
        Runnable job1 = Mockito.mock(Runnable.class);
        Runnable job2 = Mockito.mock(Runnable.class);
        ExecutorService webhookJobExecutor = Mockito.mock(ExecutorService.class);
        ReflectionTestUtils.setField(webhookServiceImpl, "webhookJobExecutor", webhookJobExecutor);

        Mockito.doReturn(List.of(job1, job2)).when(webhookServiceImpl).getDeliveryJobs(event);

        webhookServiceImpl.handleEvent(event);

        Mockito.verify(webhookJobExecutor, Mockito.times(1)).submit(job1);
        Mockito.verify(webhookJobExecutor, Mockito.times(1)).submit(job2);
    }

    @Test
    public void handleEventDoesNotSubmitJobsWhenNoJobsExist() throws EventBusException {
        Event event = Mockito.mock(Event.class);
        ExecutorService webhookJobExecutor = Mockito.mock(ExecutorService.class);
        ReflectionTestUtils.setField(webhookServiceImpl, "webhookJobExecutor", webhookJobExecutor);

        Mockito.doReturn(new ArrayList<>()).when(webhookServiceImpl).getDeliveryJobs(event);

        webhookServiceImpl.handleEvent(event);

        Mockito.verify(webhookJobExecutor, Mockito.never()).submit(Mockito.any(Runnable.class));
    }

    @Test
    public void executeWebhookDeliveryPersistsDeliveryWhenDeliveryExists() {
        WebhookDelivery delivery = Mockito.mock(WebhookDelivery.class);
        Webhook webhook = Mockito.mock(Webhook.class);
        WebhookDeliveryThread.WebhookDeliveryResult result = Mockito.mock(WebhookDeliveryThread.WebhookDeliveryResult.class);

        Mockito.when(delivery.getEventId()).thenReturn(123L);
        Mockito.when(delivery.getWebhookId()).thenReturn(456L);
        Mockito.when(result.getHeaders()).thenReturn("headers");
        Mockito.when(result.getPayload()).thenReturn("payload");
        Mockito.when(result.isSuccess()).thenReturn(true);
        Mockito.when(result.getResult()).thenReturn("result");
        Mockito.when(result.getStarTime()).thenReturn(new Date(System.currentTimeMillis() - (2 * 1000L)));
        Mockito.when(result.getEndTime()).thenReturn(new Date(System.currentTimeMillis()));
        Mockito.when(eventJoinDao.findById(123L)).thenReturn(Mockito.mock(EventJoinVO.class));
        ExecutorService executorService = Mockito.mock(ExecutorService.class);
        Mockito.when(executorService.submit(Mockito.any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            WebhookDeliveryThread webhookDeliveryThread = (WebhookDeliveryThread) runnable;
            webhookDeliveryThread.callback.complete(result);
            return CompletableFuture.completedFuture(null);
        });
        ReflectionTestUtils.setField(webhookServiceImpl, "webhookJobExecutor", executorService);

        WebhookDeliveryVO persistedDelivery = Mockito.mock(WebhookDeliveryVO.class);
        Mockito.when(webhookDeliveryDao.persist(Mockito.any(WebhookDeliveryVO.class))).thenReturn(persistedDelivery);

        WebhookDelivery returnedDelivery = webhookServiceImpl.executeWebhookDelivery(delivery, webhook, "payload");

        Assert.assertEquals(persistedDelivery, returnedDelivery);
        Mockito.verify(webhookDeliveryDao, Mockito.times(1)).persist(Mockito.any(WebhookDeliveryVO.class));
    }

    @Test
    public void executeWebhookDeliveryCreatesAndReturnsNewDeliveryWhenDeliveryIsNull() {
        Webhook webhook = Mockito.mock(Webhook.class);
        WebhookDeliveryThread.WebhookDeliveryResult result =
                Mockito.mock(WebhookDeliveryThread.WebhookDeliveryResult.class);
        Account account = Mockito.mock(Account.class);

        Mockito.when(webhook.getAccountId()).thenReturn(2L);
        Mockito.when(accountManager.getAccount(Mockito.anyLong())).thenReturn(account);
        Mockito.when(result.getHeaders()).thenReturn("headers");
        Mockito.when(result.getPayload()).thenReturn("payload");
        Mockito.when(result.isSuccess()).thenReturn(true);
        Mockito.when(result.getResult()).thenReturn("result");
        Mockito.when(result.getStarTime()).thenReturn(new Date(System.currentTimeMillis() - (2 * 1000L)));
        Mockito.when(result.getEndTime()).thenReturn(new Date(System.currentTimeMillis()));
        ExecutorService executorService = Mockito.mock(ExecutorService.class);
        Mockito.when(executorService.submit(Mockito.any(Runnable.class))).thenAnswer(invocation -> {
            System.out.println("Submitting runnable to executor");
            Runnable runnable = invocation.getArgument(0);
            WebhookDeliveryThread webhookDeliveryThread = (WebhookDeliveryThread) runnable;
            webhookDeliveryThread.callback.complete(result);
            return CompletableFuture.completedFuture(null);
        });
        ReflectionTestUtils.setField(webhookServiceImpl, "webhookJobExecutor", executorService);

        WebhookDelivery returnedDelivery = webhookServiceImpl.executeWebhookDelivery(null, webhook, "payload");

        Assert.assertNotNull(returnedDelivery);
        Assert.assertEquals("headers", returnedDelivery.getHeaders());
        Assert.assertEquals("payload", returnedDelivery.getPayload());
        Assert.assertTrue(returnedDelivery.isSuccess());
        Assert.assertEquals("result", returnedDelivery.getResponse());
    }

    @Test
    public void invalidateWebhooksCacheClearsCache() {
        LazyCache<?, ?> cache = Mockito.mock(LazyCache.class);
        ReflectionTestUtils.setField(webhookServiceImpl, "webhooksCache", cache);

        webhookServiceImpl.invalidateWebhooksCache();

        Mockito.verify(cache, Mockito.times(1)).clear();
    }

    @Test
    public void invalidateWebhookFiltersCacheInvalidatesSpecificCacheEntry() {
        LazyCache<Long, ?> cache = Mockito.mock(LazyCache.class);
        ReflectionTestUtils.setField(webhookServiceImpl, "webhookFiltersCache", cache);

        webhookServiceImpl.invalidateWebhookFiltersCache(123L);

        Mockito.verify(cache, Mockito.times(1)).invalidate(123L);
    }
}
