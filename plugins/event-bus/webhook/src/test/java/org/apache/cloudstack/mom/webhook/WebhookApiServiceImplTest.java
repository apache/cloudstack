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

import java.util.List;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.mom.webhook.api.command.user.DeleteWebhookCmd;
import org.apache.cloudstack.mom.webhook.api.response.WebhookResponse;
import org.apache.cloudstack.mom.webhook.dao.WebhookDao;
import org.apache.cloudstack.mom.webhook.dao.WebhookJoinDao;
import org.apache.cloudstack.mom.webhook.vo.WebhookJoinVO;
import org.apache.cloudstack.mom.webhook.vo.WebhookVO;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.ApiResponseHelper;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;

@RunWith(MockitoJUnitRunner.class)
public class WebhookApiServiceImplTest {

    @Mock
    WebhookDao webhookDao;
    @Mock
    WebhookJoinDao webhookJoinDao;
    @Mock
    AccountManager accountManager;

    @Mock
    DomainDao domainDao;

    @InjectMocks
    WebhookApiServiceImpl webhookApiServiceImpl = new WebhookApiServiceImpl();

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
        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            DeleteWebhookCmd cmd = Mockito.mock(DeleteWebhookCmd.class);
            Mockito.when(cmd.getId()).thenReturn(1L);
            CallContext callContextMock = Mockito.mock(CallContext.class);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            webhookApiServiceImpl.deleteWebhook(cmd);
        }
    }

    @Test(expected = PermissionDeniedException.class)
    public void testDeleteWebhookNoPermission() {
        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            DeleteWebhookCmd cmd = Mockito.mock(DeleteWebhookCmd.class);
            Mockito.when(cmd.getId()).thenReturn(1L);
            WebhookVO webhookVO = Mockito.mock(WebhookVO.class);
            Mockito.when(webhookDao.findById(1L)).thenReturn(webhookVO);
            CallContext callContextMock = Mockito.mock(CallContext.class);
            Account account = Mockito.mock(Account.class);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(account);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Mockito.doThrow(PermissionDeniedException.class).when(accountManager).checkAccess(account,
                    SecurityChecker.AccessType.OperateEntry, false, webhookVO);
            webhookApiServiceImpl.deleteWebhook(cmd);
        }
    }

    @Test
    public void testDeleteWebhook() {
        try (MockedStatic<CallContext> callContextMocked = Mockito.mockStatic(CallContext.class)) {
            DeleteWebhookCmd cmd = Mockito.mock(DeleteWebhookCmd.class);
            Mockito.when(cmd.getId()).thenReturn(1L);
            WebhookVO webhookVO = Mockito.mock(WebhookVO.class);
            Mockito.when(webhookDao.findById(1L)).thenReturn(webhookVO);
            CallContext callContextMock = Mockito.mock(CallContext.class);
            Account account = Mockito.mock(Account.class);
            Mockito.when(callContextMock.getCallingAccount()).thenReturn(account);
            callContextMocked.when(CallContext::current).thenReturn(callContextMock);
            Mockito.doNothing().when(accountManager).checkAccess(account,
                    SecurityChecker.AccessType.OperateEntry, false, webhookVO);
            Mockito.doReturn(true).when(webhookDao).remove(Mockito.anyLong());
            Assert.assertTrue(webhookApiServiceImpl.deleteWebhook(cmd));
        }
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
}
