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
package com.cloud.api;

import com.cloud.api.auth.ListUserTwoFactorAuthenticatorProvidersCmd;
import com.cloud.api.auth.SetupUserTwoFactorAuthenticationCmd;
import com.cloud.api.auth.ValidateUserTwoFactorAuthenticationCodeCmd;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.utils.HttpUtils;
import com.cloud.vm.UserVmManager;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.auth.APIAuthenticationManager;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.command.admin.config.ListCfgsByCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.nullable;

@RunWith(MockitoJUnitRunner.class)
public class ApiServletTest {

    @Mock
    ApiServer apiServer;

    @Mock
    HttpServletRequest request;

    @Mock
    HttpServletResponse response;

    @Mock
    AccountService accountService;

    @Mock
    APIAuthenticationManager authManager;

    @Mock
    APIAuthenticator authenticator;

    @Mock
    User user;

    @Mock
    Account account;

    @Mock
    HttpSession session;

    @Mock
    ManagementServer managementServer;

    @Mock
    UserAccount userAccount;

    @Mock
    AccountService accountMgr;

    StringWriter responseWriter;

    ApiServlet servlet;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws SecurityException, NoSuchFieldException,
    IllegalArgumentException, IllegalAccessException, IOException, UnknownHostException {
        servlet = new ApiServlet();
        responseWriter = new StringWriter();
        Mockito.when(response.getWriter()).thenReturn(
                new PrintWriter(responseWriter));
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        Mockito.when(accountService.getSystemUser()).thenReturn(user);
        Mockito.when(accountService.getSystemAccount()).thenReturn(account);

        Field accountMgrField = ApiServlet.class.getDeclaredField("accountMgr");
        accountMgrField.setAccessible(true);
        accountMgrField.set(servlet, accountService);

        Mockito.when(authManager.getAPIAuthenticator(Mockito.anyString())).thenReturn(authenticator);
        Mockito.lenient().when(authenticator.authenticate(Mockito.anyString(), Mockito.anyMap(), Mockito.isA(HttpSession.class),
                Mockito.same(InetAddress.getByName("127.0.0.1")), Mockito.anyString(), Mockito.isA(StringBuilder.class), Mockito.isA(HttpServletRequest.class), Mockito.isA(HttpServletResponse.class))).thenReturn("{\"loginresponse\":{}");

        Field authManagerField = ApiServlet.class.getDeclaredField("authManager");
        authManagerField.setAccessible(true);
        authManagerField.set(servlet, authManager);

        Field apiServerField = ApiServlet.class.getDeclaredField("apiServer");
        apiServerField.setAccessible(true);
        apiServerField.set(servlet, apiServer);

    }

    /**
     * These are envinonment hacks, actually getting into the behavior of other
     * classes, but there is no other way to run the test.
     */
    @Before
    public void hackEnvironment() throws Exception {
        Field smsField = ApiDBUtils.class.getDeclaredField("s_ms");
        smsField.setAccessible(true);
        smsField.set(null, managementServer);
        Mockito.lenient().when(managementServer.getVersion()).thenReturn(
                "LATEST-AND-GREATEST");
    }

    @After
    public void cleanupEnvironmentHacks() throws Exception {
        Field smsField = ApiDBUtils.class.getDeclaredField("s_ms");
        smsField.setAccessible(true);
        smsField.set(null, null);
    }

    @Test
    public void utf8Fixup() {
        Mockito.when(request.getQueryString()).thenReturn(
                "foo=12345&bar=blah&baz=&param=param");
        HashMap<String, Object[]> params = new HashMap<String, Object[]>();
        servlet.utf8Fixup(request, params);
        Assert.assertEquals("12345", params.get("foo")[0]);
        Assert.assertEquals("blah", params.get("bar")[0]);
    }

    @Test
    public void utf8FixupNull() {
        Mockito.when(request.getQueryString()).thenReturn("&&=a&=&&a&a=a=a=a");
        servlet.utf8Fixup(request, new HashMap<String, Object[]>());
    }

    @Test
    public void utf8FixupStrangeInputs() {
        Mockito.when(request.getQueryString()).thenReturn("&&=a&=&&a&a=a=a=a");
        HashMap<String, Object[]> params = new HashMap<String, Object[]>();
        servlet.utf8Fixup(request, params);
        Assert.assertTrue(params.containsKey(""));
    }

    @Test
    public void utf8FixupUtf() throws UnsupportedEncodingException {
        Mockito.when(request.getQueryString()).thenReturn(
                URLEncoder.encode("防水镜钻孔机", "UTF-8") + "="
                        + URLEncoder.encode("árvíztűrőtükörfúró", "UTF-8"));
        HashMap<String, Object[]> params = new HashMap<String, Object[]>();
        servlet.utf8Fixup(request, params);
        Assert.assertEquals("árvíztűrőtükörfúró", params.get("防水镜钻孔机")[0]);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processRequestInContextUnauthorizedGET() {
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.lenient().when(
                apiServer.verifyRequest(Mockito.anyMap(), Mockito.anyLong(), Mockito.any(InetAddress.class)))
        .thenReturn(false);
        servlet.processRequestInContext(request, response);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Mockito.verify(apiServer, Mockito.never()).handleRequest(
                Mockito.anyMap(), Mockito.anyString(),
                Mockito.any(StringBuilder.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processRequestInContextAuthorizedGet() {
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(
                apiServer.verifyRequest(nullable(Map.class), nullable(Long.class), nullable(InetAddress.class)))
        .thenReturn(true);
        servlet.processRequestInContext(request, response);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(apiServer, Mockito.times(1)).handleRequest(
                Mockito.anyMap(), Mockito.anyString(),
                Mockito.any(StringBuilder.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processRequestInContextLogout() throws UnknownHostException {
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getSession(Mockito.anyBoolean())).thenReturn(
                session);
        Mockito.when(session.getAttribute("userid")).thenReturn(1l);
        Mockito.when(session.getAttribute("accountobj")).thenReturn(account);
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        params.put(ApiConstants.COMMAND, new String[] { "logout" });
        Mockito.when(request.getParameterMap()).thenReturn(params);

        Mockito.when(authenticator.getAPIType()).thenReturn(APIAuthenticationType.LOGOUT_API);

        servlet.processRequestInContext(request, response);


        Mockito.verify(authManager).getAPIAuthenticator("logout");
        Mockito.verify(authenticator).authenticate(Mockito.anyString(), Mockito.anyMap(), Mockito.isA(HttpSession.class),
                Mockito.eq(InetAddress.getByName("127.0.0.1")), Mockito.anyString(), Mockito.isA(StringBuilder.class), Mockito.isA(HttpServletRequest.class), Mockito.isA(HttpServletResponse.class));
        Mockito.verify(session).invalidate();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processRequestInContextLogin() throws UnknownHostException {
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getSession(Mockito.anyBoolean())).thenReturn(
                session);
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        params.put(ApiConstants.COMMAND, new String[] { "login" });
        params.put(ApiConstants.USERNAME, new String[] { "TEST" });
        params.put(ApiConstants.PASSWORD, new String[] { "TEST-PWD" });
        params.put(ApiConstants.DOMAIN_ID, new String[] { "42" });
        params.put(ApiConstants.DOMAIN, new String[] { "TEST-DOMAIN" });
        Mockito.when(request.getParameterMap()).thenReturn(params);

        servlet.processRequestInContext(request, response);

        Mockito.verify(authManager).getAPIAuthenticator("login");
        Mockito.verify(authenticator).authenticate(Mockito.anyString(), Mockito.anyMap(), Mockito.isA(HttpSession.class),
                Mockito.eq(InetAddress.getByName("127.0.0.1")), Mockito.anyString(), Mockito.isA(StringBuilder.class), Mockito.isA(HttpServletRequest.class), Mockito.isA(HttpServletResponse.class));
    }

    @Test
    public void getClientAddressWithXForwardedFor() throws UnknownHostException {
        Mockito.when(request.getHeader(Mockito.eq("X-Forwarded-For"))).thenReturn("192.168.1.1");
        Assert.assertEquals(InetAddress.getByName("192.168.1.1"), ApiServlet.getClientAddress(request));
    }

    @Test
    public void getClientAddressWithHttpXForwardedFor() throws UnknownHostException {
        Mockito.when(request.getHeader(Mockito.eq("HTTP_X_FORWARDED_FOR"))).thenReturn("192.168.1.1");
        Assert.assertEquals(InetAddress.getByName("192.168.1.1"), ApiServlet.getClientAddress(request));
    }

    @Test
    public void getClientAddressWithXRemoteAddr() throws UnknownHostException {
        Mockito.when(request.getHeader(Mockito.eq("Remote_Addr"))).thenReturn("192.168.1.1");
        Assert.assertEquals(InetAddress.getByName("192.168.1.1"), ApiServlet.getClientAddress(request));
    }

    @Test
    public void getClientAddressWithHttpClientIp() throws UnknownHostException {
        Mockito.when(request.getHeader(Mockito.eq("HTTP_CLIENT_IP"))).thenReturn("192.168.1.1");
        Assert.assertEquals(InetAddress.getByName("192.168.1.1"), ApiServlet.getClientAddress(request));
    }

    @Test
    public void getClientAddressDefault() throws UnknownHostException {
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        Assert.assertEquals(InetAddress.getByName("127.0.0.1"), ApiServlet.getClientAddress(request));
    }

    @Test
    public void testSkip2FAcheckForAPIs() {
        String command = "listZones";
        Map<String, Object[]> params = new HashMap<String, Object[]>();
        boolean result = servlet.skip2FAcheckForAPIs(command);
        Assert.assertEquals(false, result);

        command = ListCfgsByCmd.APINAME;
        params.put(ApiConstants.NAME, new String[] { UserVmManager.AllowUserExpungeRecoverVm.key() });
        result = servlet.skip2FAcheckForAPIs(command);
        Assert.assertEquals(false, result);
    }

    @Test
    public void testDoNotSkip2FAcheckForAPIs() {
        String[] commands = new String[] {ApiConstants.LIST_IDPS, ApiConstants.LIST_APIS,
                ListUserTwoFactorAuthenticatorProvidersCmd.APINAME, SetupUserTwoFactorAuthenticationCmd.APINAME};
        Map<String, Object[]> params = new HashMap<String, Object[]>();
        for (String cmd: commands) {
            boolean result = servlet.skip2FAcheckForAPIs(cmd);
            Assert.assertEquals(true, result);
        }
    }

    @Test
    public void testSkip2FAcheckForUserWhenAlreadyVerified() {
        Mockito.when(session.getAttribute("userid")).thenReturn(1L);
        Mockito.when(session.getAttribute(ApiConstants.IS_2FA_VERIFIED)).thenReturn(true);

        boolean result = servlet.skip2FAcheckForUser(session);
        Assert.assertEquals(true, result);
    }

    @Test
    public void testDoNotSkip2FAcheckForUserWhen2FAEnabled() {
        servlet.accountMgr = accountMgr;
        HttpSession cuurentSession = Mockito.mock(HttpSession.class);
        Mockito.when(cuurentSession.getAttribute("userid")).thenReturn(1L);
        Mockito.when(cuurentSession.getAttribute(ApiConstants.IS_2FA_VERIFIED)).thenReturn(false);
        Mockito.when(accountMgr.getUserAccountById(1L)).thenReturn(userAccount);
        Mockito.when(userAccount.isUser2faEnabled()).thenReturn(true);

        boolean result = servlet.skip2FAcheckForUser(cuurentSession);
        Assert.assertEquals(false, result);
    }

    @Test
    public void testDoNotSkip2FAcheckForUserWhen2FAMandated() {
        servlet.accountMgr = accountMgr;
        HttpSession cuurentSession = Mockito.mock(HttpSession.class);
        Mockito.when(cuurentSession.getAttribute("userid")).thenReturn(1L);
        Mockito.when(cuurentSession.getAttribute(ApiConstants.IS_2FA_VERIFIED)).thenReturn(false);

        Mockito.when(accountMgr.getUserAccountById(1L)).thenReturn(userAccount);
        Mockito.when(userAccount.getDomainId()).thenReturn(1L);
        Mockito.when(userAccount.isUser2faEnabled()).thenReturn(false);

        ConfigKey<Boolean> mandateUserTwoFactorAuthentication = Mockito.mock(ConfigKey.class);
        AccountManagerImpl.mandateUserTwoFactorAuthentication = mandateUserTwoFactorAuthentication;
        Mockito.when(mandateUserTwoFactorAuthentication.valueIn(1L)).thenReturn(false);

        boolean result = servlet.skip2FAcheckForUser(cuurentSession);
        Assert.assertEquals(true, result);
    }

    @Test
    public void testSkip2FAcheckForUserWhen2FAisNotEnabledAndNotMandated() {
        servlet.accountMgr = accountMgr;
        HttpSession cuurentSession = Mockito.mock(HttpSession.class);
        Mockito.when(cuurentSession.getAttribute("userid")).thenReturn(1L);
        Mockito.when(cuurentSession.getAttribute(ApiConstants.IS_2FA_VERIFIED)).thenReturn(false);

        Mockito.when(accountMgr.getUserAccountById(1L)).thenReturn(userAccount);
        Mockito.when(userAccount.getDomainId()).thenReturn(1L);
        Mockito.when(userAccount.isUser2faEnabled()).thenReturn(false);

        ConfigKey<Boolean> enableUserTwoFactorAuthentication = Mockito.mock(ConfigKey.class);
        AccountManagerImpl.enableUserTwoFactorAuthentication = enableUserTwoFactorAuthentication;
        Mockito.when(enableUserTwoFactorAuthentication.valueIn(1L)).thenReturn(true);

        ConfigKey<Boolean> mandateUserTwoFactorAuthentication = Mockito.mock(ConfigKey.class);
        AccountManagerImpl.mandateUserTwoFactorAuthentication = mandateUserTwoFactorAuthentication;
        Mockito.when(mandateUserTwoFactorAuthentication.valueIn(1L)).thenReturn(true);

        boolean result = servlet.skip2FAcheckForUser(cuurentSession);
        Assert.assertEquals(false, result);
    }

    @Test
    public void testVerify2FA() throws UnknownHostException {
        String command = ValidateUserTwoFactorAuthenticationCodeCmd.APINAME;
        Mockito.lenient().when(authenticator.authenticate(Mockito.anyString(), Mockito.anyMap(), Mockito.isA(HttpSession.class),
                Mockito.same(InetAddress.getByName("127.0.0.1")), Mockito.anyString(), Mockito.isA(StringBuilder.class), Mockito.isA(HttpServletRequest.class), Mockito.isA(HttpServletResponse.class))).thenReturn("{\"Success\":{}");

        StringBuilder auditTrailSb = new StringBuilder();
        Map<String, Object[]> params = new HashMap<String, Object[]>();
        String responseType = HttpUtils.RESPONSE_TYPE_XML;
        boolean result = servlet.verify2FA(session, command, auditTrailSb, params, InetAddress.getByName("192.168.1.1"),
                responseType, request, response);

        Assert.assertEquals(true, result);
    }

    @Test
    public void testVerify2FAWhenAuthenticatorNotFound() throws UnknownHostException {
        String command = ValidateUserTwoFactorAuthenticationCodeCmd.APINAME;
        Mockito.when(authManager.getAPIAuthenticator(command)).thenReturn(null);
        StringBuilder auditTrailSb = new StringBuilder();
        Map<String, Object[]> params = new HashMap<String, Object[]>();
        String responseType = HttpUtils.RESPONSE_TYPE_XML;
        boolean result = servlet.verify2FA(session, command, auditTrailSb, params, InetAddress.getByName("192.168.1.1"),
                responseType, request, response);

        Assert.assertEquals(false, result);
    }

    @Test
    public void testVerify2FAWhenExpectedCommandIsNotCalled() throws UnknownHostException {
        servlet.accountMgr = accountMgr;
        String command = "listZones";
        Mockito.when(session.getAttribute("userid")).thenReturn(1L);
        Mockito.when(accountMgr.getUserAccountById(1L)).thenReturn(userAccount);
        Mockito.when(userAccount.isUser2faEnabled()).thenReturn(true);

        StringBuilder auditTrailSb = new StringBuilder();
        Map<String, Object[]> params = new HashMap<String, Object[]>();
        String responseType = HttpUtils.RESPONSE_TYPE_XML;
        boolean result = servlet.verify2FA(session, command, auditTrailSb, params, InetAddress.getByName("192.168.1.1"),
                responseType, request, response);

        Assert.assertEquals(false, result);
    }
}
