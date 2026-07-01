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

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.api.command.user.AddWebhookFilterCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.CreateWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookDeliveryCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookFilterCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ExecuteWebhookDeliveryCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ListWebhookDeliveriesCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ListWebhookFiltersCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.ListWebhooksCmd;
import org.apache.cloudstack.mom.webhook.api.command.user.UpdateWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.response.WebhookDeliveryResponse;
import org.apache.cloudstack.mom.webhook.api.response.WebhookFilterResponse;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;
import org.apache.cloudstack.mom.webhook.dao.WebhookDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookDeliveryDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookDeliveryJoinDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookFilterDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookJoinDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryJoinVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookDeliveryVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookFilterVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookJoinVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookVO;
import org.apache.commons.collections.CollectionUtils;
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

import com.cloud.api.ApiResponseHelper;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class WebhookApiServiceImplTest {

    @Mock
    WebhookDao webhookDao;
    @Mock
    WebhookJoinDao webhookJoinDao;
    @Mock
    WebhookDeliveryDao webhookDeliveryDao;
    @Mock
    WebhookDeliveryJoinDao webhookDeliveryJoinDao;
    @Mock
    WebhookFilterDao webhookFilterDao;
    @Mock
    AccountManager accountManager;
    @Mock
    DomainDao domainDao;
    @Mock
    WebhookService webhookService;
    @Mock
    ManagementServerHostDao managementServerHostDao;

    @Mock
    Account caller;

    @Spy
    @InjectMocks
    WebhookApiServiceImpl webhookApiServiceImpl = new WebhookApiServiceImpl();

    MockedStatic<CallContext> callContextMocked;

    @Before
    public void setup() {
        callContextMocked = Mockito.mockStatic(CallContext.class);
        CallContext callContextMock = Mockito.mock(CallContext.class);
        callContextMocked.when(CallContext::current).thenReturn(callContextMock);
        Mockito.when(callContextMock.getCallingAccount()).thenReturn(caller);
    }

    @After
    public void cleanup() {
        callContextMocked.close();
    }


    private WebhookJoinVO prepareTestWebhookJoinVO() {
        String name = "webhook";
        String description = "webhook-description";
        Webhook.State state = Webhook.State.Enabled;
        String payloadUrl = "url";
        String secretKey = "key";
        boolean sslVerification = false;
        Webhook.Scope scope = Webhook.Scope.Local;
        WebhookJoinVO webhookJoinVO = new WebhookJoinVO();
        ReflectionTestUtils.setField(webhookJoinVO, "name", name);
        ReflectionTestUtils.setField(webhookJoinVO, "description", description);
        ReflectionTestUtils.setField(webhookJoinVO, "state", state);
        ReflectionTestUtils.setField(webhookJoinVO, "payloadUrl", payloadUrl);
        ReflectionTestUtils.setField(webhookJoinVO, "secretKey", secretKey);
        ReflectionTestUtils.setField(webhookJoinVO, "sslVerification", sslVerification);
        ReflectionTestUtils.setField(webhookJoinVO, "scope", scope);
        return webhookJoinVO;
    }

    private void validateWebhookResponseWithWebhookJoinVO(WebhookResponse response, WebhookJoinVO webhookJoinVO) {
        Assert.assertEquals(webhookJoinVO.getName(), ReflectionTestUtils.getField(response, "name"));
        Assert.assertEquals(webhookJoinVO.getDescription(), ReflectionTestUtils.getField(response, "description"));
        Assert.assertEquals(webhookJoinVO.getState().toString(), ReflectionTestUtils.getField(response, "state"));
        Assert.assertEquals(webhookJoinVO.getPayloadUrl(), ReflectionTestUtils.getField(response, "payloadUrl"));
        Assert.assertEquals(webhookJoinVO.getSecretKey(), ReflectionTestUtils.getField(response, "secretKey"));
        Assert.assertEquals(webhookJoinVO.isSslVerification(), ReflectionTestUtils.getField(response, "sslVerification"));
        Assert.assertEquals(webhookJoinVO.getScope().toString(), ReflectionTestUtils.getField(response, "scope"));
    }

    @Test
    public void testCreateWebhookResponse() {
        WebhookJoinVO webhookJoinVO = prepareTestWebhookJoinVO();
        try (MockedStatic<ApiResponseHelper> mockedApiResponseHelper = Mockito.mockStatic(ApiResponseHelper.class)) {
            WebhookResponse response = webhookApiServiceImpl.createWebhookResponse(webhookJoinVO);
            validateWebhookResponseWithWebhookJoinVO(response, webhookJoinVO);
        }
    }

    @Test
    public void testCreateWebhookResponseId() {
        WebhookJoinVO webhookJoinVO = prepareTestWebhookJoinVO();
        long id = 1L;
        Mockito.when(webhookJoinDao.findById(id)).thenReturn(webhookJoinVO);
        try (MockedStatic<ApiResponseHelper> mockedApiResponseHelper = Mockito.mockStatic(ApiResponseHelper.class)) {
            WebhookResponse response = webhookApiServiceImpl.createWebhookResponse(id);
            validateWebhookResponseWithWebhookJoinVO(response, webhookJoinVO);
        }
    }

    @Test
    public void testGetIdsOfAccessibleWebhooksAdmin() {
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getType()).thenReturn(Account.Type.ADMIN);
        Assert.assertTrue(CollectionUtils.isEmpty(webhookApiServiceImpl.getIdsOfAccessibleWebhooks(account)));
    }

    @Test
    public void testGetIdsOfAccessibleWebhooksDomainAdmin() {
        Long accountId = 1L;
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getType()).thenReturn(Account.Type.DOMAIN_ADMIN);
        Mockito.when(account.getDomainId()).thenReturn(1L);
        Mockito.when(account.getId()).thenReturn(accountId);
        String domainPath = "d1";
        DomainVO domain = Mockito.mock(DomainVO.class);
        Mockito.when(domain.getPath()).thenReturn(domainPath);
        Mockito.when(domainDao.findById(1L)).thenReturn(domain);
        WebhookJoinVO webhookJoinVO = Mockito.mock(WebhookJoinVO.class);
        Mockito.when(webhookJoinVO.getId()).thenReturn(1L);
        Mockito.when(webhookJoinDao.listByAccountOrDomain(accountId, domainPath)).thenReturn(List.of(webhookJoinVO));
        List<Long> result = webhookApiServiceImpl.getIdsOfAccessibleWebhooks(account);
        Assert.assertTrue(CollectionUtils.isNotEmpty(result));
        Assert.assertEquals(1, result.size());
    }

    @Test
    public void testGetIdsOfAccessibleWebhooksNormalUser() {
        Long accountId = 1L;
        Account account = Mockito.mock(Account.class);
        Mockito.when(account.getType()).thenReturn(Account.Type.NORMAL);
        Mockito.when(account.getId()).thenReturn(accountId);
        WebhookJoinVO webhookJoinVO = Mockito.mock(WebhookJoinVO.class);
        Mockito.when(webhookJoinVO.getId()).thenReturn(1L);
        Mockito.when(webhookJoinDao.listByAccountOrDomain(accountId, null)).thenReturn(List.of(webhookJoinVO));
        List<Long> result = webhookApiServiceImpl.getIdsOfAccessibleWebhooks(account);
        Assert.assertTrue(CollectionUtils.isNotEmpty(result));
        Assert.assertEquals(1, result.size());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDeleteWebhookInvalidWebhook() {
        DeleteWebhookCmd cmd = Mockito.mock(DeleteWebhookCmd.class);
        Mockito.when(cmd.getId()).thenReturn(1L);
        webhookApiServiceImpl.deleteWebhook(cmd);
    }

    @Test(expected = PermissionDeniedException.class)
    public void testDeleteWebhookNoPermission() {
        DeleteWebhookCmd cmd = Mockito.mock(DeleteWebhookCmd.class);
        Mockito.when(cmd.getId()).thenReturn(1L);
        WebhookVO webhookVO = Mockito.mock(WebhookVO.class);
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhookVO);
        Mockito.doThrow(PermissionDeniedException.class).when(accountManager).checkAccess(caller,
                SecurityChecker.AccessType.OperateEntry, false, webhookVO);
        webhookApiServiceImpl.deleteWebhook(cmd);
    }

    @Test
    public void testDeleteWebhook() {
        DeleteWebhookCmd cmd = Mockito.mock(DeleteWebhookCmd.class);
        Mockito.when(cmd.getId()).thenReturn(1L);
        WebhookVO webhookVO = Mockito.mock(WebhookVO.class);
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhookVO);
        Mockito.doNothing().when(accountManager).checkAccess(caller,
                SecurityChecker.AccessType.OperateEntry, false, webhookVO);
        Mockito.doReturn(true).when(webhookDao).remove(Mockito.anyLong());
        Assert.assertTrue(webhookApiServiceImpl.deleteWebhook(cmd));
    }

    @Test
    public void testValidateWebhookOwnerPayloadUrlNonExistent() {
        Mockito.when(webhookDao.findByAccountAndPayloadUrl(Mockito.anyLong(), Mockito.anyString())).thenReturn(null);
        Account account = Mockito.mock(Account.class);
        String url = "url";
        webhookApiServiceImpl.validateWebhookOwnerPayloadUrl(account, url, null);
        webhookApiServiceImpl.validateWebhookOwnerPayloadUrl(account, url, Mockito.mock(Webhook.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateWebhookOwnerPayloadUrlCreateExist() {
        Mockito.when(webhookDao.findByAccountAndPayloadUrl(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(Mockito.mock(WebhookVO.class));
        webhookApiServiceImpl.validateWebhookOwnerPayloadUrl(Mockito.mock(Account.class), "url",
                null);
    }

    private Webhook mockWebhook(long id) {
        Webhook webhook = Mockito.mock(Webhook.class);
        Mockito.when(webhook.getId()).thenReturn(id);
        return webhook;
    }

    private WebhookVO mockWebhookVO(long id) {
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        Mockito.when(webhook.getId()).thenReturn(id);
        return webhook;
    }

    @Test
    public void testValidateWebhookOwnerPayloadUrlUpdateSameExist() {
        WebhookVO webhookVO = mockWebhookVO(1L);
        Mockito.when(webhookDao.findByAccountAndPayloadUrl(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(webhookVO);
        webhookApiServiceImpl.validateWebhookOwnerPayloadUrl(Mockito.mock(Account.class), "url",
                mockWebhook(1L));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateWebhookOwnerPayloadUrlUpdateDifferentExist() {
        WebhookVO webhookVO = mockWebhookVO(2L);
        Mockito.when(webhookDao.findByAccountAndPayloadUrl(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(webhookVO);
        webhookApiServiceImpl.validateWebhookOwnerPayloadUrl(Mockito.mock(Account.class), "url",
                mockWebhook(1L));
    }

    @Test
    public void testGetNormalizedPayloadUrl() {
        Assert.assertEquals("http://abc.com", webhookApiServiceImpl.getNormalizedPayloadUrl("abc.com"));
        Assert.assertEquals("http://abc.com", webhookApiServiceImpl.getNormalizedPayloadUrl("http://abc.com"));
        Assert.assertEquals("https://abc.com",
                webhookApiServiceImpl.getNormalizedPayloadUrl("https://abc.com"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void basicWebhookDeliveryApiCheckThrowsExceptionForInvalidDeliveryId() {
        Mockito.when(webhookDeliveryDao.findById(1L)).thenReturn(null);
        webhookApiServiceImpl.basicWebhookDeliveryApiCheck(Mockito.mock(Account.class), 1L, null, null, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void basicWebhookDeliveryApiCheckThrowsExceptionForInvalidWebhookId() {
        Mockito.when(webhookDao.findById(1L)).thenReturn(null);
        webhookApiServiceImpl.basicWebhookDeliveryApiCheck(Mockito.mock(Account.class), null, 1L, null, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void basicWebhookDeliveryApiCheckThrowsExceptionForEndDateBeforeStartDate() {
        webhookApiServiceImpl.basicWebhookDeliveryApiCheck(Mockito.mock(Account.class), null, null, null, new Date(), new Date(System.currentTimeMillis() - 1000));
    }

    @Test(expected = PermissionDeniedException.class)
    public void basicWebhookDeliveryApiCheckThrowsExceptionForNonAdminAccessToManagementServer() {
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getType()).thenReturn(Account.Type.NORMAL);
        webhookApiServiceImpl.basicWebhookDeliveryApiCheck(caller, null, null, 1L, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void basicWebhookDeliveryApiCheckThrowsExceptionForInvalidManagementServerId() {
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getType()).thenReturn(Account.Type.ADMIN);
        Mockito.when(managementServerHostDao.findById(1L)).thenReturn(null);
        webhookApiServiceImpl.basicWebhookDeliveryApiCheck(caller, null, null, 1L, null, null);
    }

    @Test
    public void basicWebhookDeliveryApiCheckReturnsManagementServerHostVOForValidInput() {
        Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getType()).thenReturn(Account.Type.ADMIN);
        ManagementServerHostVO managementServerHostVO = Mockito.mock(ManagementServerHostVO.class);
        Mockito.when(managementServerHostDao.findById(1L)).thenReturn(managementServerHostVO);
        ManagementServerHostVO result = webhookApiServiceImpl.basicWebhookDeliveryApiCheck(caller, null, null, 1L, null, null);
        Assert.assertEquals(managementServerHostVO, result);
    }

    @Test
    public void basicWebhookDeliveryApiCheckValidatesWebhookDeliveryAccess() {
        Account caller = Mockito.mock(Account.class);
        WebhookDeliveryVO webhookDeliveryVO = Mockito.mock(WebhookDeliveryVO.class);
        WebhookVO webhookVO = Mockito.mock(WebhookVO.class);
        Mockito.when(webhookDeliveryDao.findById(1L)).thenReturn(webhookDeliveryVO);
        Mockito.when(webhookDeliveryVO.getWebhookId()).thenReturn(1L);
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhookVO);
        Mockito.doNothing().when(accountManager).checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, webhookVO);
        webhookApiServiceImpl.basicWebhookDeliveryApiCheck(caller, 1L, null, null, null, null);
        Mockito.verify(accountManager).checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, webhookVO);
    }

    @Test
    public void basicWebhookDeliveryApiCheckValidatesWebhookAccess() {
        Account caller = Mockito.mock(Account.class);
        WebhookVO webhookVO = Mockito.mock(WebhookVO.class);
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhookVO);
        Mockito.doNothing().when(accountManager).checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, webhookVO);
        webhookApiServiceImpl.basicWebhookDeliveryApiCheck(caller, null, 1L, null, null, null);
        Mockito.verify(accountManager).checkAccess(caller, SecurityChecker.AccessType.OperateEntry, false, webhookVO);
    }

    @Test
    public void createWebhookDeliveryResponsePopulatesAllFieldsCorrectly() {
        WebhookDeliveryJoinVO webhookDeliveryVO = Mockito.mock(WebhookDeliveryJoinVO.class);
        Mockito.when(webhookDeliveryVO.getUuid()).thenReturn("uuid");
        Mockito.when(webhookDeliveryVO.getEventUuid()).thenReturn("eventUuid");
        Mockito.when(webhookDeliveryVO.getEventType()).thenReturn("eventType");
        Mockito.when(webhookDeliveryVO.getWebhookUuId()).thenReturn("webhookUuid");
        Mockito.when(webhookDeliveryVO.getWebhookName()).thenReturn("webhookName");
        Mockito.when(webhookDeliveryVO.getManagementServerUuId()).thenReturn("managementServerUuid");
        Mockito.when(webhookDeliveryVO.getManagementServerName()).thenReturn("managementServerName");
        Mockito.when(webhookDeliveryVO.getHeaders()).thenReturn("headers");
        Mockito.when(webhookDeliveryVO.getPayload()).thenReturn("payload");
        Mockito.when(webhookDeliveryVO.isSuccess()).thenReturn(true);
        Mockito.when(webhookDeliveryVO.getResponse()).thenReturn("response");
        Mockito.when(webhookDeliveryVO.getStartTime()).thenReturn(new Date(1000));
        Mockito.when(webhookDeliveryVO.getEndTime()).thenReturn(new Date(2000));

        WebhookDeliveryResponse response = webhookApiServiceImpl.createWebhookDeliveryResponse(webhookDeliveryVO);

        Assert.assertEquals("webhookdelivery", ReflectionTestUtils.getField(response, "objectName"));
        Assert.assertEquals("uuid", ReflectionTestUtils.getField(response, "id"));
        Assert.assertEquals("eventUuid", ReflectionTestUtils.getField(response, "eventId"));
        Assert.assertEquals("eventType", ReflectionTestUtils.getField(response, "eventType"));
        Assert.assertEquals("webhookUuid", ReflectionTestUtils.getField(response, "webhookId"));
        Assert.assertEquals("webhookName", ReflectionTestUtils.getField(response, "webhookName"));
        Assert.assertEquals("managementServerUuid", ReflectionTestUtils.getField(response, "managementServerId"));
        Assert.assertEquals("managementServerName", ReflectionTestUtils.getField(response, "managementServerName"));
        Assert.assertEquals("headers", ReflectionTestUtils.getField(response, "headers"));
        Assert.assertEquals("payload", ReflectionTestUtils.getField(response, "payload"));
        Assert.assertTrue((Boolean) ReflectionTestUtils.getField(response, "success"));
        Assert.assertEquals("response", ReflectionTestUtils.getField(response, "response"));
        Assert.assertEquals(new Date(1000), ReflectionTestUtils.getField(response, "startTime"));
        Assert.assertEquals(new Date(2000), ReflectionTestUtils.getField(response, "endTime"));
    }

    @Test(expected = NullPointerException.class)
    public void createWebhookDeliveryResponseThrowsExceptionForNullInput() {
        webhookApiServiceImpl.createWebhookDeliveryResponse(null);
    }

    @Test
    public void createTestWebhookDeliveryResponsePopulatesAllFieldsCorrectly() {
        WebhookDelivery webhookDelivery = Mockito.mock(WebhookDelivery.class);
        Mockito.when(webhookDelivery.getUuid()).thenReturn("uuid");
        Mockito.when(webhookDelivery.getManagementServerId()).thenReturn(1L);
        Mockito.when(webhookDelivery.getHeaders()).thenReturn("headers");
        Mockito.when(webhookDelivery.getPayload()).thenReturn("payload");
        Mockito.when(webhookDelivery.isSuccess()).thenReturn(true);
        Mockito.when(webhookDelivery.getResponse()).thenReturn("response");
        Mockito.when(webhookDelivery.getStartTime()).thenReturn(new Date(1000));
        Mockito.when(webhookDelivery.getEndTime()).thenReturn(new Date(2000));

        Webhook webhook = Mockito.mock(Webhook.class);
        Mockito.when(webhook.getUuid()).thenReturn("webhookUuid");
        Mockito.when(webhook.getName()).thenReturn("webhookName");

        ManagementServerHostVO msHost = Mockito.mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getUuid()).thenReturn("managementServerUuid");
        Mockito.when(msHost.getName()).thenReturn("managementServerName");
        Mockito.when(managementServerHostDao.findByMsid(1L)).thenReturn(msHost);

        WebhookDeliveryResponse response = webhookApiServiceImpl.createTestWebhookDeliveryResponse(webhookDelivery, webhook);

        Assert.assertEquals("webhookdelivery", ReflectionTestUtils.getField(response, "objectName"));
        Assert.assertEquals("uuid", ReflectionTestUtils.getField(response, "id"));
        Assert.assertEquals(WebhookDelivery.TEST_EVENT_TYPE, ReflectionTestUtils.getField(response, "eventType"));
        Assert.assertEquals("webhookUuid", ReflectionTestUtils.getField(response, "webhookId"));
        Assert.assertEquals("webhookName", ReflectionTestUtils.getField(response, "webhookName"));
        Assert.assertEquals("managementServerUuid", ReflectionTestUtils.getField(response, "managementServerId"));
        Assert.assertEquals("managementServerName", ReflectionTestUtils.getField(response, "managementServerName"));
        Assert.assertEquals("headers", ReflectionTestUtils.getField(response, "headers"));
        Assert.assertEquals("payload", ReflectionTestUtils.getField(response, "payload"));
        Assert.assertTrue((Boolean) ReflectionTestUtils.getField(response, "success"));
        Assert.assertEquals("response", ReflectionTestUtils.getField(response, "response"));
        Assert.assertEquals(new Date(1000), ReflectionTestUtils.getField(response, "startTime"));
        Assert.assertEquals(new Date(2000), ReflectionTestUtils.getField(response, "endTime"));
    }

    @Test
    public void createTestWebhookDeliveryResponseHandlesNullWebhook() {
        WebhookDelivery webhookDelivery = Mockito.mock(WebhookDelivery.class);
        Mockito.when(webhookDelivery.getUuid()).thenReturn("uuid");
        Mockito.when(webhookDelivery.getManagementServerId()).thenReturn(1L);
        Mockito.when(webhookDelivery.getHeaders()).thenReturn("headers");
        Mockito.when(webhookDelivery.getPayload()).thenReturn("payload");
        Mockito.when(webhookDelivery.isSuccess()).thenReturn(true);
        Mockito.when(webhookDelivery.getResponse()).thenReturn("response");
        Mockito.when(webhookDelivery.getStartTime()).thenReturn(new Date(1000));
        Mockito.when(webhookDelivery.getEndTime()).thenReturn(new Date(2000));

        ManagementServerHostVO msHost = Mockito.mock(ManagementServerHostVO.class);
        Mockito.when(msHost.getUuid()).thenReturn("managementServerUuid");
        Mockito.when(msHost.getName()).thenReturn("managementServerName");
        Mockito.when(managementServerHostDao.findByMsid(1L)).thenReturn(msHost);

        WebhookDeliveryResponse response = webhookApiServiceImpl.createTestWebhookDeliveryResponse(webhookDelivery, null);

        Assert.assertEquals("webhookdelivery", ReflectionTestUtils.getField(response, "objectName"));
        Assert.assertEquals("uuid", ReflectionTestUtils.getField(response, "id"));
        Assert.assertEquals(WebhookDelivery.TEST_EVENT_TYPE, ReflectionTestUtils.getField(response, "eventType"));
        Assert.assertNull(ReflectionTestUtils.getField(response, "webhookId"));
        Assert.assertNull(ReflectionTestUtils.getField(response, "webhookName"));
        Assert.assertEquals("managementServerUuid", ReflectionTestUtils.getField(response, "managementServerId"));
        Assert.assertEquals("managementServerName", ReflectionTestUtils.getField(response, "managementServerName"));
        Assert.assertEquals("headers", ReflectionTestUtils.getField(response, "headers"));
        Assert.assertEquals("payload", ReflectionTestUtils.getField(response, "payload"));
        Assert.assertTrue((Boolean) ReflectionTestUtils.getField(response, "success"));
        Assert.assertEquals("response", ReflectionTestUtils.getField(response, "response"));
        Assert.assertEquals(new Date(1000), ReflectionTestUtils.getField(response, "startTime"));
        Assert.assertEquals(new Date(2000), ReflectionTestUtils.getField(response, "endTime"));
    }

    @Test
    public void createTestWebhookDeliveryResponseHandlesNullManagementServer() {
        WebhookDelivery webhookDelivery = Mockito.mock(WebhookDelivery.class);
        Mockito.when(webhookDelivery.getUuid()).thenReturn("uuid");
        Mockito.when(webhookDelivery.getManagementServerId()).thenReturn(1L);
        Mockito.when(webhookDelivery.getHeaders()).thenReturn("headers");
        Mockito.when(webhookDelivery.getPayload()).thenReturn("payload");
        Mockito.when(webhookDelivery.isSuccess()).thenReturn(true);
        Mockito.when(webhookDelivery.getResponse()).thenReturn("response");
        Mockito.when(webhookDelivery.getStartTime()).thenReturn(new Date(1000));
        Mockito.when(webhookDelivery.getEndTime()).thenReturn(new Date(2000));

        Webhook webhook = Mockito.mock(Webhook.class);
        Mockito.when(webhook.getUuid()).thenReturn("webhookUuid");
        Mockito.when(webhook.getName()).thenReturn("webhookName");

        Mockito.when(managementServerHostDao.findByMsid(1L)).thenReturn(null);

        WebhookDeliveryResponse response = webhookApiServiceImpl.createTestWebhookDeliveryResponse(webhookDelivery, webhook);

        Assert.assertEquals("webhookdelivery", ReflectionTestUtils.getField(response, "objectName"));
        Assert.assertEquals("uuid", ReflectionTestUtils.getField(response, "id"));
        Assert.assertEquals(WebhookDelivery.TEST_EVENT_TYPE, ReflectionTestUtils.getField(response, "eventType"));
        Assert.assertEquals("webhookUuid", ReflectionTestUtils.getField(response, "webhookId"));
        Assert.assertEquals("webhookName", ReflectionTestUtils.getField(response, "webhookName"));
        Assert.assertNull(ReflectionTestUtils.getField(response, "managementServerId"));
        Assert.assertNull(ReflectionTestUtils.getField(response, "managementServerName"));
        Assert.assertEquals("headers", ReflectionTestUtils.getField(response, "headers"));
        Assert.assertEquals("payload", ReflectionTestUtils.getField(response, "payload"));
        Assert.assertTrue((Boolean) ReflectionTestUtils.getField(response, "success"));
        Assert.assertEquals("response", ReflectionTestUtils.getField(response, "response"));
        Assert.assertEquals(new Date(1000), ReflectionTestUtils.getField(response, "startTime"));
        Assert.assertEquals(new Date(2000), ReflectionTestUtils.getField(response, "endTime"));
    }

    @Test
    public void getOwnerReturnsFinalizedOwnerForValidInput() {
        CreateWebhookCmd cmd = Mockito.mock(CreateWebhookCmd.class);
        Account finalizedOwner = Mockito.mock(Account.class);

        Mockito.when(cmd.getAccountName()).thenReturn("accountName");
        Mockito.when(cmd.getDomainId()).thenReturn(1L);
        Mockito.when(cmd.getProjectId()).thenReturn(2L);
        Mockito.when(accountManager.finalizeOwner(caller, "accountName", 1L, 2L)).thenReturn(finalizedOwner);

        Account result = webhookApiServiceImpl.getOwner(cmd);

        Assert.assertEquals(finalizedOwner, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void getOwnerThrowsExceptionForInvalidAccount() {
        CreateWebhookCmd cmd = Mockito.mock(CreateWebhookCmd.class);

        Mockito.when(cmd.getAccountName()).thenReturn("invalidAccount");
        Mockito.when(cmd.getDomainId()).thenReturn(1L);
        Mockito.when(cmd.getProjectId()).thenReturn(null);
        Mockito.when(accountManager.finalizeOwner(caller, "invalidAccount", 1L, null))
                .thenThrow(new InvalidParameterValueException("Invalid account"));

        webhookApiServiceImpl.getOwner(cmd);
    }

    @Test
    public void createWebhookFilterResponsePopulatesAllFieldsCorrectly() {
        WebhookFilter webhookFilter = Mockito.mock(WebhookFilter.class);
        Mockito.when(webhookFilter.getUuid()).thenReturn("filterUuid");
        Mockito.when(webhookFilter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(webhookFilter.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(webhookFilter.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(webhookFilter.getValue()).thenReturn("value");
        Mockito.when(webhookFilter.getCreated()).thenReturn(new Date(1000));

        WebhookVO webhookVO = Mockito.mock(WebhookVO.class);
        Mockito.when(webhookVO.getUuid()).thenReturn("webhookUuid");
        Mockito.when(webhookVO.getName()).thenReturn("webhookName");

        WebhookFilterResponse response = webhookApiServiceImpl.createWebhookFilterResponse(webhookFilter, webhookVO);

        Assert.assertEquals("webhookfilter", ReflectionTestUtils.getField(response, "objectName"));
        Assert.assertEquals("filterUuid", ReflectionTestUtils.getField(response, "id"));
        Assert.assertEquals("webhookUuid", ReflectionTestUtils.getField(response, "webhookId"));
        Assert.assertEquals("webhookName", ReflectionTestUtils.getField(response, "webhookName"));
        Assert.assertEquals("EventType", ReflectionTestUtils.getField(response, "type"));
        Assert.assertEquals("Include", ReflectionTestUtils.getField(response, "mode"));
        Assert.assertEquals("Exact", ReflectionTestUtils.getField(response, "matchType"));
        Assert.assertEquals("value", ReflectionTestUtils.getField(response, "value"));
        Assert.assertEquals(new Date(1000), ReflectionTestUtils.getField(response, "created"));
    }

    @Test
    public void createWebhookFilterResponseHandlesNullWebhookVO() {
        WebhookFilter webhookFilter = Mockito.mock(WebhookFilter.class);
        Mockito.when(webhookFilter.getUuid()).thenReturn("filterUuid");
        Mockito.when(webhookFilter.getWebhookId()).thenReturn(1L);
        Mockito.when(webhookFilter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(webhookFilter.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(webhookFilter.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(webhookFilter.getValue()).thenReturn("value");
        Mockito.when(webhookFilter.getCreated()).thenReturn(new Date(1000));

        WebhookVO webhookVO = Mockito.mock(WebhookVO.class);
        Mockito.when(webhookVO.getUuid()).thenReturn("webhookUuid");
        Mockito.when(webhookVO.getName()).thenReturn("webhookName");
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhookVO);

        WebhookFilterResponse response = webhookApiServiceImpl.createWebhookFilterResponse(webhookFilter, null);

        Assert.assertEquals("webhookfilter", ReflectionTestUtils.getField(response, "objectName"));
        Assert.assertEquals("filterUuid", ReflectionTestUtils.getField(response, "id"));
        Assert.assertEquals("webhookUuid", ReflectionTestUtils.getField(response, "webhookId"));
        Assert.assertEquals("webhookName", ReflectionTestUtils.getField(response, "webhookName"));
        Assert.assertEquals("EventType", ReflectionTestUtils.getField(response, "type"));
        Assert.assertEquals("Include", ReflectionTestUtils.getField(response, "mode"));
        Assert.assertEquals("Exact", ReflectionTestUtils.getField(response, "matchType"));
        Assert.assertEquals("value", ReflectionTestUtils.getField(response, "value"));
        Assert.assertEquals(new Date(1000), ReflectionTestUtils.getField(response, "created"));
    }

    @Test
    public void createWebhookFilterResponseHandlesNullWebhookFromDao() {
        WebhookFilter webhookFilter = Mockito.mock(WebhookFilter.class);
        Mockito.when(webhookFilter.getUuid()).thenReturn("filterUuid");
        Mockito.when(webhookFilter.getWebhookId()).thenReturn(1L);
        Mockito.when(webhookFilter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(webhookFilter.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(webhookFilter.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(webhookFilter.getValue()).thenReturn("value");
        Mockito.when(webhookFilter.getCreated()).thenReturn(new Date(1000));

        Mockito.when(webhookDao.findById(1L)).thenReturn(null);

        WebhookFilterResponse response = webhookApiServiceImpl.createWebhookFilterResponse(webhookFilter, null);

        Assert.assertEquals("webhookfilter", ReflectionTestUtils.getField(response, "objectName"));
        Assert.assertEquals("filterUuid", ReflectionTestUtils.getField(response, "id"));
        Assert.assertNull(ReflectionTestUtils.getField(response, "webhookId"));
        Assert.assertNull(ReflectionTestUtils.getField(response, "webhookName"));
        Assert.assertEquals("EventType", ReflectionTestUtils.getField(response, "type"));
        Assert.assertEquals("Include", ReflectionTestUtils.getField(response, "mode"));
        Assert.assertEquals("Exact", ReflectionTestUtils.getField(response, "matchType"));
        Assert.assertEquals("value", ReflectionTestUtils.getField(response, "value"));
        Assert.assertEquals(new Date(1000), ReflectionTestUtils.getField(response, "created"));
    }

    @Test
    public void listWebhooksReturnsEmptyResponseForNoWebhooks() {
        ListWebhooksCmd cmd = Mockito.mock(ListWebhooksCmd.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(null);
        Mockito.when(cmd.getName()).thenReturn(null);
        Mockito.when(cmd.getKeyword()).thenReturn(null);
        Mockito.when(cmd.getScope()).thenReturn(null);
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);
        SearchBuilder<WebhookJoinVO> sb = Mockito.mock(SearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(Mockito.mock(WebhookJoinVO.class));
        SearchCriteria<WebhookJoinVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.when(webhookJoinDao.createSearchBuilder()).thenReturn(sb);
        Mockito.when(webhookJoinDao.searchAndCount(Mockito.any(), Mockito.any())).thenReturn(new Pair<>(List.of(), 0));

        ListResponse<WebhookResponse> response = webhookApiServiceImpl.listWebhooks(cmd);

        Assert.assertTrue(response.getResponses().isEmpty());
        Assert.assertEquals(0, (int) response.getCount());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void listWebhooksThrowsExceptionForInvalidScope() {
        ListWebhooksCmd cmd = Mockito.mock(ListWebhooksCmd.class);

        Mockito.when(cmd.getScope()).thenReturn("InvalidScope");

        webhookApiServiceImpl.listWebhooks(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void listWebhooksThrowsExceptionForInvalidState() {
        ListWebhooksCmd cmd = Mockito.mock(ListWebhooksCmd.class);

        Mockito.when(cmd.getState()).thenReturn("InvalidState");

        webhookApiServiceImpl.listWebhooks(cmd);
    }

    @Test
    public void listWebhooksFiltersByValidScopeAndState() {
        ListWebhooksCmd cmd = Mockito.mock(ListWebhooksCmd.class);
        Mockito.when(caller.getType()).thenReturn(Account.Type.DOMAIN_ADMIN);
        WebhookJoinVO webhook = Mockito.mock(WebhookJoinVO.class);
        Mockito.when(webhook.getState()).thenReturn(Webhook.State.Enabled);
        Mockito.when(webhook.getScope()).thenReturn(Webhook.Scope.Domain);

        Mockito.when(cmd.getScope()).thenReturn("Domain");
        Mockito.when(cmd.getState()).thenReturn("Enabled");
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);
        SearchBuilder<WebhookJoinVO> sb = Mockito.mock(SearchBuilder.class);
        Mockito.when(sb.entity()).thenReturn(Mockito.mock(WebhookJoinVO.class));
        SearchCriteria<WebhookJoinVO> sc = Mockito.mock(SearchCriteria.class);
        Mockito.when(sb.create()).thenReturn(sc);
        Mockito.when(webhookJoinDao.createSearchBuilder()).thenReturn(sb);
        Mockito.when(webhookJoinDao.searchAndCount(Mockito.any(), Mockito.any())).thenReturn(new Pair<>(List.of(webhook), 1));

        ListResponse<WebhookResponse> response = webhookApiServiceImpl.listWebhooks(cmd);

        Assert.assertEquals(1, (int) response.getCount());
        Assert.assertEquals(1, response.getResponses().size());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void listWebhooksThrowsExceptionForUnauthorizedScope() {
        ListWebhooksCmd cmd = Mockito.mock(ListWebhooksCmd.class);

        Mockito.when(cmd.getScope()).thenReturn("Global");
        Mockito.when(caller.getType()).thenReturn(Account.Type.NORMAL);

        webhookApiServiceImpl.listWebhooks(cmd);
    }

    @Test
    public void createWebhookCreatesWebhookSuccessfully() {
        CreateWebhookCmd cmd = Mockito.mock(CreateWebhookCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        WebhookJoinVO webhookJoinVO = Mockito.mock(WebhookJoinVO.class);
        Mockito.when(webhookJoinVO.getState()).thenReturn(Webhook.State.Enabled);
        Mockito.when(webhookJoinVO.getScope()).thenReturn(Webhook.Scope.Local);

        Mockito.when(cmd.getName()).thenReturn("webhookName");
        Mockito.when(cmd.getDescription()).thenReturn("webhookDescription");
        Mockito.when(cmd.getPayloadUrl()).thenReturn("https://example.com");
        Mockito.when(cmd.getSecretKey()).thenReturn("secretKey");
        Mockito.when(cmd.isSslVerification()).thenReturn(true);
        Mockito.when(cmd.getScope()).thenReturn("Local");
        Mockito.when(cmd.getState()).thenReturn("Enabled");
        Mockito.when(caller.getType()).thenReturn(Account.Type.ADMIN);
        Mockito.when(caller.getDomainId()).thenReturn(1L);
        Mockito.when(webhookDao.persist(Mockito.any(WebhookVO.class))).thenReturn(webhook);
        Mockito.when(webhook.getId()).thenReturn(1L);
        Mockito.when(webhookJoinDao.findById(1L)).thenReturn(webhookJoinVO);
        Mockito.doReturn(caller).when(webhookApiServiceImpl).getOwner(cmd);

        WebhookResponse response = webhookApiServiceImpl.createWebhook(cmd);

        Assert.assertNotNull(response);
        Assert.assertEquals("webhook", ReflectionTestUtils.getField(response, "objectName"));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createWebhookThrowsExceptionForInvalidScope() {
        CreateWebhookCmd cmd = Mockito.mock(CreateWebhookCmd.class);

        Mockito.when(cmd.getScope()).thenReturn("InvalidScope");

        webhookApiServiceImpl.createWebhook(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createWebhookThrowsExceptionForInvalidState() {
        CreateWebhookCmd cmd = Mockito.mock(CreateWebhookCmd.class);

        Mockito.when(cmd.getState()).thenReturn("InvalidState");

        webhookApiServiceImpl.createWebhook(cmd);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createWebhookThrowsExceptionForInvalidUrl() {
        CreateWebhookCmd cmd = Mockito.mock(CreateWebhookCmd.class);

        Mockito.when(cmd.getPayloadUrl()).thenReturn("invalid-url");
        Mockito.doReturn(caller).when(webhookApiServiceImpl).getOwner(cmd);

        webhookApiServiceImpl.createWebhook(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createWebhookThrowsExceptionForNonHttpsWithSslVerification() {
        CreateWebhookCmd cmd = Mockito.mock(CreateWebhookCmd.class);

        Mockito.when(cmd.getPayloadUrl()).thenReturn("http://example.com");
        Mockito.when(cmd.isSslVerification()).thenReturn(true);
        Mockito.doReturn(caller).when(webhookApiServiceImpl).getOwner(cmd);
        Mockito.doNothing().when(webhookApiServiceImpl)
                .validateWebhookOwnerPayloadUrl(caller, "http://example.com", null);

        webhookApiServiceImpl.createWebhook(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void createWebhookThrowsExceptionForDuplicatePayloadUrl() {
        CreateWebhookCmd cmd = Mockito.mock(CreateWebhookCmd.class);
        WebhookVO existingWebhook = Mockito.mock(WebhookVO.class);

        Mockito.when(cmd.getPayloadUrl()).thenReturn("https://example.com");
        Mockito.when(caller.getId()).thenReturn(1L);
        Mockito.doReturn(caller).when(webhookApiServiceImpl).getOwner(cmd);
        Mockito.when(webhookDao.findByAccountAndPayloadUrl(1L, "https://example.com")).thenReturn(existingWebhook);

        webhookApiServiceImpl.createWebhook(cmd);
    }

    @Test
    public void updateWebhookUpdatesAllFieldsSuccessfully() {
        UpdateWebhookCmd cmd = Mockito.mock(UpdateWebhookCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        Mockito.when(webhook.getId()).thenReturn(1L);
        Mockito.when(webhook.getPayloadUrl()).thenReturn("http://abc.xyz");
        String updatedUrl = "https://cloudstack.apache.org/";

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getName()).thenReturn("Updated Name");
        Mockito.when(cmd.getDescription()).thenReturn("Updated Description");
        Mockito.when(cmd.getPayloadUrl()).thenReturn(updatedUrl);
        Mockito.when(cmd.getSecretKey()).thenReturn("UpdatedSecretKey");
        Mockito.when(cmd.isSslVerification()).thenReturn(true);
        Mockito.when(cmd.getScope()).thenReturn("Local");
        Mockito.when(cmd.getState()).thenReturn("Enabled");
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.when(webhook.getAccountId()).thenReturn(1L);
        Mockito.when(accountManager.getAccount(1L)).thenReturn(caller);
        Mockito.when(webhookDao.update(1L, webhook)).thenReturn(true);
        Mockito.doReturn(Mockito.mock(WebhookResponse.class)).when(webhookApiServiceImpl)
                .createWebhookResponse(1L);

        WebhookResponse response = webhookApiServiceImpl.updateWebhook(cmd);


        Assert.assertNotNull(response);
        Mockito.verify(webhook).setName("Updated Name");
        Mockito.verify(webhook).setDescription("Updated Description");
        Mockito.verify(webhook).setPayloadUrl(updatedUrl);
        Mockito.verify(webhook).setSecretKey("UpdatedSecretKey");
        Mockito.verify(webhook).setSslVerification(true);
        Mockito.verify(webhook).setScope(Webhook.Scope.Local);
        Mockito.verify(webhook).setState(Webhook.State.Enabled);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void updateWebhookThrowsExceptionForInvalidState() {
        UpdateWebhookCmd cmd = Mockito.mock(UpdateWebhookCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getState()).thenReturn("InvalidState");
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);

        webhookApiServiceImpl.updateWebhook(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void updateWebhookThrowsExceptionForInvalidScope() {
        UpdateWebhookCmd cmd = Mockito.mock(UpdateWebhookCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getScope()).thenReturn("InvalidScope");
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.when(webhook.getAccountId()).thenReturn(1L);
        Mockito.when(accountManager.getAccount(1L)).thenReturn(caller);

        webhookApiServiceImpl.updateWebhook(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void updateWebhookThrowsExceptionForNonHttpsWithSslVerification() {
        UpdateWebhookCmd cmd = Mockito.mock(UpdateWebhookCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        Mockito.when(webhook.getPayloadUrl()).thenReturn("http://abc.xyz");

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getPayloadUrl()).thenReturn("http://cloudstack.apache.org/");
        Mockito.when(cmd.isSslVerification()).thenReturn(true);
        Mockito.when(webhook.getAccountId()).thenReturn(1L);
        Mockito.when(accountManager.getAccount(1L)).thenReturn(caller);
        Mockito.doNothing().when(webhookApiServiceImpl)
                .validateWebhookOwnerPayloadUrl(caller, "http://cloudstack.apache.org/", webhook);
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);

        webhookApiServiceImpl.updateWebhook(cmd);
    }

    @Test
    public void updateWebhookDoesNotUpdateUnchangedFields() {
        UpdateWebhookCmd cmd = Mockito.mock(UpdateWebhookCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        Mockito.when(webhook.getId()).thenReturn(1L);
        Mockito.when(webhook.getPayloadUrl()).thenReturn("http://abc.xyz");

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getName()).thenReturn(null);
        Mockito.when(cmd.getDescription()).thenReturn(null);
        Mockito.when(cmd.getPayloadUrl()).thenReturn(null);
        Mockito.when(cmd.getSecretKey()).thenReturn(null);
        Mockito.when(cmd.isSslVerification()).thenReturn(null);
        Mockito.when(cmd.getScope()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(null);
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.doReturn(Mockito.mock(WebhookResponse.class)).when(webhookApiServiceImpl)
                .createWebhookResponse(1L);

        WebhookResponse response = webhookApiServiceImpl.updateWebhook(cmd);

        Assert.assertNotNull(response);
        Mockito.verify(webhookDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }

    @Test
    public void listWebhookDeliveriesReturnsEmptyResponseForNoDeliveries() {
        ListWebhookDeliveriesCmd cmd = Mockito.mock(ListWebhookDeliveriesCmd.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(null);
        Mockito.when(cmd.getManagementServerId()).thenReturn(null);
        Mockito.when(cmd.getKeyword()).thenReturn(null);
        Mockito.when(cmd.getStartDate()).thenReturn(null);
        Mockito.when(cmd.getEndDate()).thenReturn(null);
        Mockito.when(cmd.getEventType()).thenReturn(null);
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);
        Mockito.when(webhookDeliveryJoinDao.searchAndCountByListApiParameters(Mockito.any(), Mockito.anyList(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new Pair<>(List.of(), 0));

        ListResponse<WebhookDeliveryResponse> response = webhookApiServiceImpl.listWebhookDeliveries(cmd);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.getResponses().isEmpty());
        Assert.assertEquals(0, (int) response.getCount());
    }

    @Test
    public void listWebhookDeliveriesFiltersByWebhookId() {
        ListWebhookDeliveriesCmd cmd = Mockito.mock(ListWebhookDeliveriesCmd.class);
        WebhookDeliveryJoinVO delivery = Mockito.mock(WebhookDeliveryJoinVO.class);
        WebhookDeliveryResponse deliveryResponse = Mockito.mock(WebhookDeliveryResponse.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(1L);
        Mockito.when(cmd.getManagementServerId()).thenReturn(null);
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);
        Mockito.when(webhookDeliveryJoinDao.searchAndCountByListApiParameters(Mockito.any(), Mockito.anyList(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new Pair<>(List.of(delivery), 1));
        Mockito.doReturn(deliveryResponse).when(webhookApiServiceImpl).createWebhookDeliveryResponse(delivery);
        Mockito.doReturn(null).when(webhookApiServiceImpl)
                .basicWebhookDeliveryApiCheck(caller, null, 1L, null, null, null);

        ListResponse<WebhookDeliveryResponse> response = webhookApiServiceImpl.listWebhookDeliveries(cmd);

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getResponses().size());
        Assert.assertEquals(deliveryResponse, response.getResponses().get(0));
        Assert.assertEquals(1, (int) response.getCount());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void listWebhookDeliveriesThrowsExceptionForEndDateBeforeStartDate() {
        ListWebhookDeliveriesCmd cmd = Mockito.mock(ListWebhookDeliveriesCmd.class);

        Mockito.when(cmd.getStartDate()).thenReturn(new Date(System.currentTimeMillis()));
        Mockito.when(cmd.getEndDate()).thenReturn(new Date(System.currentTimeMillis() - 1000));

        webhookApiServiceImpl.listWebhookDeliveries(cmd);
    }

    @Test
    public void listWebhookDeliveriesFiltersByEventType() {
        ListWebhookDeliveriesCmd cmd = Mockito.mock(ListWebhookDeliveriesCmd.class);
        WebhookDeliveryJoinVO delivery = Mockito.mock(WebhookDeliveryJoinVO.class);
        WebhookDeliveryResponse deliveryResponse = Mockito.mock(WebhookDeliveryResponse.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(null);
        Mockito.when(cmd.getManagementServerId()).thenReturn(null);
        Mockito.when(cmd.getEventType()).thenReturn("EventType");
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);
        Mockito.when(webhookDeliveryJoinDao.searchAndCountByListApiParameters(Mockito.any(), Mockito.anyList(),
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(new Pair<>(List.of(delivery), 1));
        Mockito.doReturn(deliveryResponse).when(webhookApiServiceImpl).createWebhookDeliveryResponse(delivery);

        ListResponse<WebhookDeliveryResponse> response = webhookApiServiceImpl.listWebhookDeliveries(cmd);

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getResponses().size());
        Assert.assertEquals(deliveryResponse, response.getResponses().get(0));
        Assert.assertEquals(1, (int) response.getCount());
    }

    @Test
    public void deleteWebhookDeliveryRemovesDeliveriesSuccessfully() {
        DeleteWebhookDeliveryCmd cmd = Mockito.mock(DeleteWebhookDeliveryCmd.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(1L);
        Mockito.when(cmd.getManagementServerId()).thenReturn(null);
        Mockito.when(cmd.getStartDate()).thenReturn(null);
        Mockito.when(cmd.getEndDate()).thenReturn(null);
        Mockito.doReturn(null).when(webhookApiServiceImpl)
                .basicWebhookDeliveryApiCheck(caller, null, 1L, null, null, null);
        Mockito.when(webhookDeliveryDao.deleteByDeleteApiParams(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(5);

        int removed = webhookApiServiceImpl.deleteWebhookDelivery(cmd);

        Assert.assertEquals(5, removed);
        Mockito.verify(webhookDeliveryDao).deleteByDeleteApiParams(null, 1L, null, null, null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void deleteWebhookDeliveryThrowsExceptionForInvalidDates() {
        DeleteWebhookDeliveryCmd cmd = Mockito.mock(DeleteWebhookDeliveryCmd.class);
        Mockito.when(cmd.getStartDate()).thenReturn(new Date(System.currentTimeMillis()));
        Mockito.when(cmd.getEndDate()).thenReturn(new Date(System.currentTimeMillis() - 1000));

        webhookApiServiceImpl.deleteWebhookDelivery(cmd);
    }

    @Test
    public void deleteWebhookDeliveryHandlesNoDeliveriesToRemove() {
        DeleteWebhookDeliveryCmd cmd = Mockito.mock(DeleteWebhookDeliveryCmd.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(1L);
        Mockito.when(cmd.getManagementServerId()).thenReturn(null);
        Mockito.when(cmd.getStartDate()).thenReturn(null);
        Mockito.when(cmd.getEndDate()).thenReturn(null);
        Mockito.when(webhookDao.findById(1L)).thenReturn(Mockito.mock(WebhookVO.class));
        Mockito.when(webhookDeliveryDao.deleteByDeleteApiParams(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(0);

        int removed = webhookApiServiceImpl.deleteWebhookDelivery(cmd);

        Assert.assertEquals(0, removed);
        Mockito.verify(webhookDeliveryDao).deleteByDeleteApiParams(null, 1L, null, null, null);
    }

    @Test(expected = PermissionDeniedException.class)
    public void deleteWebhookDeliveryThrowsExceptionForUnauthorizedAccess() {
        DeleteWebhookDeliveryCmd cmd = Mockito.mock(DeleteWebhookDeliveryCmd.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(1L);
        Mockito.when(caller.getType()).thenReturn(Account.Type.NORMAL);
        Mockito.when(webhookDao.findById(1L)).thenReturn(Mockito.mock(WebhookVO.class));

        webhookApiServiceImpl.deleteWebhookDelivery(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void executeWebhookDeliveryThrowsExceptionWhenNoIdentifiersProvided() {
        ExecuteWebhookDeliveryCmd cmd = Mockito.mock(ExecuteWebhookDeliveryCmd.class);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(null);
        Mockito.when(cmd.getPayloadUrl()).thenReturn(null);

        webhookApiServiceImpl.executeWebhookDelivery(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void executeWebhookDeliveryThrowsExceptionWhenBothDeliveryIdAndWebhookIdProvided() {
        ExecuteWebhookDeliveryCmd cmd = Mockito.mock(ExecuteWebhookDeliveryCmd.class);
        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getWebhookId()).thenReturn(2L);

        webhookApiServiceImpl.executeWebhookDelivery(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void executeWebhookDeliveryThrowsExceptionForInvalidDeliveryId() {
        ExecuteWebhookDeliveryCmd cmd = Mockito.mock(ExecuteWebhookDeliveryCmd.class);
        Mockito.when(cmd.getId()).thenReturn(1L);

        webhookApiServiceImpl.executeWebhookDelivery(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void executeWebhookDeliveryThrowsExceptionForInvalidWebhookId() {
        ExecuteWebhookDeliveryCmd cmd = Mockito.mock(ExecuteWebhookDeliveryCmd.class);
        Mockito.when(cmd.getWebhookId()).thenReturn(1L);

        webhookApiServiceImpl.executeWebhookDelivery(cmd);
    }

    @Test
    public void executeWebhookDeliveryExecutesSuccessfullyForValidDeliveryId() {
        ExecuteWebhookDeliveryCmd cmd = Mockito.mock(ExecuteWebhookDeliveryCmd.class);
        WebhookDeliveryVO delivery = Mockito.mock(WebhookDeliveryVO.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        WebhookDelivery webhookDelivery = Mockito.mock(WebhookDelivery.class);
        WebhookDeliveryResponse response = Mockito.mock(WebhookDeliveryResponse.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getWebhookId()).thenReturn(null);
        Mockito.when(webhookDeliveryDao.findById(1L)).thenReturn(delivery);
        Mockito.when(delivery.getWebhookId()).thenReturn(2L);
        Mockito.when(webhookDao.findById(2L)).thenReturn(webhook);
        Mockito.when(webhookService.executeWebhookDelivery(delivery, webhook, null)).thenReturn(webhookDelivery);
        Mockito.when(webhookDelivery.getId()).thenReturn(3L);
        Mockito.when(webhookDeliveryJoinDao.findById(3L)).thenReturn(Mockito.mock(WebhookDeliveryJoinVO.class));
        Mockito.doReturn(response).when(webhookApiServiceImpl).createWebhookDeliveryResponse(Mockito.any());

        WebhookDeliveryResponse result = webhookApiServiceImpl.executeWebhookDelivery(cmd);

        Assert.assertNotNull(result);
        Assert.assertEquals(response, result);
    }

    @Test
    public void executeWebhookDeliveryExecutesSuccessfullyForValidWebhookId() {
        ExecuteWebhookDeliveryCmd cmd = Mockito.mock(ExecuteWebhookDeliveryCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        WebhookDelivery webhookDelivery = Mockito.mock(WebhookDelivery.class);
        WebhookDeliveryResponse response = Mockito.mock(WebhookDeliveryResponse.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(1L);
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.when(webhookService.executeWebhookDelivery(null, webhook, null)).thenReturn(webhookDelivery);
        Mockito.when(webhookDelivery.getId()).thenReturn(WebhookDelivery.ID_DUMMY);
        Mockito.doReturn(response).when(webhookApiServiceImpl).createTestWebhookDeliveryResponse(webhookDelivery, webhook);

        WebhookDeliveryResponse result = webhookApiServiceImpl.executeWebhookDelivery(cmd);

        Assert.assertNotNull(result);
        Assert.assertEquals(response, result);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void executeWebhookDeliveryThrowsExceptionForInvalidPayloadUrl() {
        ExecuteWebhookDeliveryCmd cmd = Mockito.mock(ExecuteWebhookDeliveryCmd.class);
        Mockito.when(cmd.getPayloadUrl()).thenReturn("invalid-url");

        webhookApiServiceImpl.executeWebhookDelivery(cmd);
    }

    @Test
    public void executeWebhookDeliveryExecutesSuccessfullyForValidPayloadUrl() {
        ExecuteWebhookDeliveryCmd cmd = Mockito.mock(ExecuteWebhookDeliveryCmd.class);
        WebhookDelivery webhookDelivery = Mockito.mock(WebhookDelivery.class);
        WebhookDeliveryResponse response = Mockito.mock(WebhookDeliveryResponse.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(null);
        Mockito.when(cmd.getPayloadUrl()).thenReturn("https://example.com");
        Mockito.when(webhookService.executeWebhookDelivery(Mockito.eq(null), Mockito.any(Webhook.class),
                Mockito.eq(null))).thenReturn(webhookDelivery);
        Mockito.when(webhookDelivery.getId()).thenReturn(WebhookDelivery.ID_DUMMY);
        Mockito.doReturn(response).when(webhookApiServiceImpl).createTestWebhookDeliveryResponse(
                Mockito.eq(webhookDelivery), Mockito.any(Webhook.class));

        WebhookDeliveryResponse result = webhookApiServiceImpl.executeWebhookDelivery(cmd);

        Assert.assertNotNull(result);
        Assert.assertEquals(response, result);
    }

    @Test
    public void listWebhookFiltersReturnsEmptyResponseForNoFilters() {
        ListWebhookFiltersCmd cmd = Mockito.mock(ListWebhookFiltersCmd.class);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(null);
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);
        Mockito.when(webhookFilterDao.searchBy(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(new Pair<>(List.of(), 0));

        ListResponse<WebhookFilterResponse> response = webhookApiServiceImpl.listWebhookFilters(cmd);

        Assert.assertNotNull(response);
        Assert.assertTrue(response.getResponses().isEmpty());
    }

    @Test
    public void listWebhookFiltersReturnsFiltersSuccessfully() {
        ListWebhookFiltersCmd cmd = Mockito.mock(ListWebhookFiltersCmd.class);
        WebhookFilterVO filter = Mockito.mock(WebhookFilterVO.class);
        Mockito.when(filter.getWebhookId()).thenReturn(1L);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        WebhookFilterResponse filterResponse = Mockito.mock(WebhookFilterResponse.class);

        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.getWebhookId()).thenReturn(1L);
        Mockito.when(cmd.getStartIndex()).thenReturn(0L);
        Mockito.when(cmd.getPageSizeVal()).thenReturn(10L);
        Mockito.when(webhookFilterDao.searchBy(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(new Pair<>(List.of(filter), 1));
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.doReturn(filterResponse).when(webhookApiServiceImpl).createWebhookFilterResponse(filter, webhook);

        ListResponse<WebhookFilterResponse> response = webhookApiServiceImpl.listWebhookFilters(cmd);

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getResponses().size());
        Assert.assertEquals(filterResponse, response.getResponses().get(0));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void addWebhookFilterThrowsExceptionForInvalidWebhookId() {
        AddWebhookFilterCmd cmd = Mockito.mock(AddWebhookFilterCmd.class);
        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(webhookDao.findById(1L)).thenReturn(null);

        webhookApiServiceImpl.addWebhookFilter(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void addWebhookFilterThrowsExceptionForInvalidMode() {
        AddWebhookFilterCmd cmd = Mockito.mock(AddWebhookFilterCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getMode()).thenReturn("InvalidMode");
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);

        webhookApiServiceImpl.addWebhookFilter(cmd);
    }

    @Test
    public void addWebhookFilterAddsFilterSuccessfully() {
        AddWebhookFilterCmd cmd = Mockito.mock(AddWebhookFilterCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        WebhookFilterVO filter = Mockito.mock(WebhookFilterVO.class);
        WebhookFilterResponse filterResponse = Mockito.mock(WebhookFilterResponse.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getType()).thenReturn("EventType");
        Mockito.when(cmd.getMode()).thenReturn("Include");
        Mockito.when(cmd.getMatchType()).thenReturn("Exact");
        Mockito.when(cmd.getValue()).thenReturn("value");
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.when(webhookFilterDao.persist(Mockito.any(WebhookFilterVO.class))).thenReturn(filter);
        Mockito.doReturn(filterResponse).when(webhookApiServiceImpl).createWebhookFilterResponse(filter, webhook);

        WebhookFilterResponse response = webhookApiServiceImpl.addWebhookFilter(cmd);

        Assert.assertNotNull(response);
        Assert.assertEquals(filterResponse, response);
    }

    @Test
    public void addWebhookFilterAddsFilterSuccessfullyEvenWithExisting() {
        AddWebhookFilterCmd cmd = Mockito.mock(AddWebhookFilterCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        WebhookFilterVO filter = Mockito.mock(WebhookFilterVO.class);
        WebhookFilterVO newFilter = Mockito.mock(WebhookFilterVO.class);
        WebhookFilterResponse filterResponse = Mockito.mock(WebhookFilterResponse.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(webhook.getId()).thenReturn(1L);
        Mockito.when(filter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(filter.getMode()).thenReturn(WebhookFilter.Mode.Exclude);
        Mockito.when(filter.getMatchType()).thenReturn(WebhookFilter.MatchType.Prefix);
        Mockito.when(filter.getValue()).thenReturn("value.old");
        Mockito.when(cmd.getType()).thenReturn("EventType");
        Mockito.when(cmd.getMode()).thenReturn("Include");
        Mockito.when(cmd.getMatchType()).thenReturn("Exact");
        Mockito.when(cmd.getValue()).thenReturn("value");
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.when(webhookFilterDao.listByWebhook(1L)).thenReturn(List.of(filter));
        Mockito.when(webhookFilterDao.persist(Mockito.any(WebhookFilterVO.class))).thenReturn(newFilter);
        Mockito.doReturn(filterResponse).when(webhookApiServiceImpl).createWebhookFilterResponse(newFilter, webhook);

        WebhookFilterResponse response = webhookApiServiceImpl.addWebhookFilter(cmd);

        Assert.assertNotNull(response);
        Assert.assertEquals(filterResponse, response);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void addWebhookFilterConflictsWithExisting() {
        AddWebhookFilterCmd cmd = Mockito.mock(AddWebhookFilterCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        WebhookFilterVO filter = Mockito.mock(WebhookFilterVO.class);

        Mockito.when(webhook.getId()).thenReturn(1L);
        Mockito.when(filter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(filter.getMode()).thenReturn(WebhookFilter.Mode.Exclude);
        Mockito.when(filter.getMatchType()).thenReturn(WebhookFilter.MatchType.Prefix);
        Mockito.when(filter.getValue()).thenReturn("value");
        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getType()).thenReturn("EventType");
        Mockito.when(cmd.getMode()).thenReturn("Include");
        Mockito.when(cmd.getMatchType()).thenReturn("Exact");
        Mockito.when(cmd.getValue()).thenReturn("value.extra");
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.when(webhookFilterDao.listByWebhook(1L)).thenReturn(List.of(filter));

        webhookApiServiceImpl.addWebhookFilter(cmd);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void addWebhookFilterSameEventConflictsWithExisting() {
        AddWebhookFilterCmd cmd = Mockito.mock(AddWebhookFilterCmd.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);
        WebhookFilterVO filter = Mockito.mock(WebhookFilterVO.class);

        Mockito.when(webhook.getId()).thenReturn(1L);
        Mockito.when(filter.getType()).thenReturn(WebhookFilter.Type.EventType);
        Mockito.when(filter.getMode()).thenReturn(WebhookFilter.Mode.Include);
        Mockito.when(filter.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact);
        Mockito.when(filter.getValue()).thenReturn(EventTypes.EVENT_VM_CREATE);
        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(cmd.getType()).thenReturn(WebhookFilter.Type.EventType.name());
        Mockito.when(cmd.getMode()).thenReturn(WebhookFilter.Mode.Exclude.name());
        Mockito.when(cmd.getMatchType()).thenReturn(WebhookFilter.MatchType.Exact.name());
        Mockito.when(cmd.getValue()).thenReturn(EventTypes.EVENT_VM_CREATE);
        Mockito.when(webhookDao.findById(1L)).thenReturn(webhook);
        Mockito.when(webhookFilterDao.listByWebhook(1L)).thenReturn(List.of(filter));

        webhookApiServiceImpl.addWebhookFilter(cmd);
    }

    @Test
    public void deleteWebhookFilterDeletesFilterSuccessfully() {
        DeleteWebhookFilterCmd cmd = Mockito.mock(DeleteWebhookFilterCmd.class);
        WebhookFilterVO filter = Mockito.mock(WebhookFilterVO.class);
        WebhookVO webhook = Mockito.mock(WebhookVO.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(webhookFilterDao.searchBy(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(new Pair<>(List.of(filter), 1));
        Mockito.when(webhookDao.findById(Mockito.anyLong())).thenReturn(webhook);
        Mockito.when(webhookFilterDao.delete(Mockito.anyLong(), Mockito.anyLong())).thenReturn(1);

        int result = webhookApiServiceImpl.deleteWebhookFilter(cmd);

        Assert.assertEquals(1, result);
    }

    @Test
    public void deleteWebhookFilterHandlesNoFiltersToDelete() {
        DeleteWebhookFilterCmd cmd = Mockito.mock(DeleteWebhookFilterCmd.class);

        Mockito.when(cmd.getId()).thenReturn(1L);
        Mockito.when(webhookFilterDao.searchBy(Mockito.any(), Mockito.any(), Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(new Pair<>(List.of(), 0));

        int result = webhookApiServiceImpl.deleteWebhookFilter(cmd);

        Assert.assertEquals(0, result);
    }
}
