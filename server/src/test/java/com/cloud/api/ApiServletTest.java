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

import static org.mockito.ArgumentMatchers.nullable;

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
import com.cloud.utils.StringUtils;
import com.cloud.api.dispatch.DispatchTask;
import com.cloud.api.dispatch.ParamUnpackWorker;
import com.cloud.vm.UserVmManager;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.offering.IsAccountAllowedToCreateOfferingsWithTagsCmd;
import org.apache.cloudstack.api.command.admin.storage.AddImageStoreCmd;
import org.apache.cloudstack.api.command.admin.storage.AddObjectStoragePoolCmd;
import org.apache.cloudstack.api.auth.APIAuthenticationManager;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.command.admin.config.ListCfgsByCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


@RunWith(MockitoJUnitRunner.class)
public class ApiServletTest {

    private static final String[] STATE_CHANGING_COMMAND_CHECK_NAME_PARAM =
            {ApiServer.EnforcePostRequestsAndTimestamps.key()};

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

    @Mock
    ConfigDepotImpl mockConfigDepot;

    StringWriter responseWriter;

    ApiServlet servlet;

    private ConfigDepotImpl originalConfigDepot;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws SecurityException, NoSuchFieldException,
            IllegalArgumentException, IllegalAccessException, IOException {
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

        setupConfigDepotMock();
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
        restoreConfigDepot();
    }

    private void setupConfigDepotMock() throws NoSuchFieldException, IllegalAccessException {
        Field depotField = ConfigKey.class.getDeclaredField("s_depot");
        depotField.setAccessible(true);
        originalConfigDepot = (ConfigDepotImpl) depotField.get(null);
        depotField.set(null, mockConfigDepot);
        Mockito.when(mockConfigDepot.getConfigStringValue(
            Mockito.anyString(),
            Mockito.any(ConfigKey.Scope.class),
            Mockito.any()
        )).thenReturn(null);
    }

    private void restoreConfigDepot() throws Exception {
        Field depotField = ConfigKey.class.getDeclaredField("s_depot");
        depotField.setAccessible(true);
        depotField.set(null, originalConfigDepot);
    }

    private void setConfigValue(String configName, String value) {
        Mockito.when(mockConfigDepot.getConfigStringValue(
            Mockito.eq(configName),
            Mockito.eq(ConfigKey.Scope.Global),
            Mockito.isNull()
        )).thenReturn(value);
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
        setConfigValue("proxy.header.verify", "true");
        setConfigValue("proxy.cidr", "127.0.0.0/8");
        Mockito.when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        Mockito.when(request.getHeader(Mockito.eq("X-Forwarded-For"))).thenReturn("192.168.1.1");
        Assert.assertEquals(InetAddress.getByName("192.168.1.1"), ApiServlet.getClientAddress(request));
    }

    @Test
    public void getClientAddressWithHttpXForwardedFor() throws UnknownHostException {
        setConfigValue("proxy.header.verify", "true");
        setConfigValue("proxy.cidr", "127.0.0.0/8");
        Mockito.when(request.getHeader(Mockito.eq("HTTP_X_FORWARDED_FOR"))).thenReturn("192.168.1.1");
        Assert.assertEquals(InetAddress.getByName("192.168.1.1"), ApiServlet.getClientAddress(request));
    }

    @Test
    public void getClientAddressWithRemoteAddr() throws UnknownHostException {
        setConfigValue("proxy.header.verify", "true");
        setConfigValue("proxy.cidr", "127.0.0.0/8");
        Assert.assertEquals(InetAddress.getByName("127.0.0.1"), ApiServlet.getClientAddress(request));
    }

    @Test
    public void getClientAddressWithHttpClientIp() throws UnknownHostException {
        setConfigValue("proxy.header.verify", "true");
        setConfigValue("proxy.cidr", "127.0.0.0/8");
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

    @Test
    public void isStateChangingCommandNotUsingPOSTReturnsFalseForPostMethod() {
        String command = "updateConfiguration";
        String method = "POST";
        Map<String, Object[]> params = new HashMap<>();

        boolean result = servlet.isStateChangingCommandNotUsingPOST(command, method, params);

        Assert.assertFalse(result);
    }

    @Test
    public void isStateChangingCommandNotUsingPOSTReturnsTrueForNullCommandAndMethod() {
        String command = null;
        String method = null;
        Map<String, Object[]> params = new HashMap<>();

        boolean result = servlet.isStateChangingCommandNotUsingPOST(command, method, params);

        Assert.assertTrue(result);
    }

    @Test
    public void isStateChangingCommandNotUsingPOSTReturnsFalseForGetHttpMethodAnnotation() {
        String command = "isAccountAllowedToCreateOfferingsWithTags";
        String method = "GET";
        Map<String, Object[]> params = new HashMap<>();
        Class<?> cmdClass = IsAccountAllowedToCreateOfferingsWithTagsCmd.class;
        APICommand apiCommand = cmdClass.getAnnotation(APICommand.class);
        Mockito.doReturn(cmdClass).when(apiServer).getCmdClass(command);
        Assert.assertNotNull(apiCommand);
        Assert.assertEquals("GET", apiCommand.httpMethod());
        boolean result = servlet.isStateChangingCommandNotUsingPOST(command, method, params);
        Assert.assertFalse(result);
    }

    @Test
    public void isStateChangingCommandNotUsingPOSTReturnsFalseForMatchingGetRequestPattern() {
        String command = "listZones";
        String method = "GET";
        Map<String, Object[]> params = new HashMap<>();
        boolean result = servlet.isStateChangingCommandNotUsingPOST(command, method, params);
        Assert.assertFalse(result);
    }

    @Test
    public void isStateChangingCommandNotUsingPOSTReturnsTrueForMissingNameParameter() {
        String command = "updateConfiguration";
        String method = "GET";
        Map<String, Object[]> params = new HashMap<>();
        boolean result = servlet.isStateChangingCommandNotUsingPOST(command, method, params);
        Assert.assertTrue(result);
    }

    @Test
    public void isStateChangingCommandNotUsingPOSTReturnsFalseForUpdateConfigurationEnforcePostRequestsKey() {
        String command = "updateConfiguration";
        String method = "GET";
        Map<String, Object[]> params = new HashMap<>();
        params.put("name", STATE_CHANGING_COMMAND_CHECK_NAME_PARAM);
        boolean result = servlet.isStateChangingCommandNotUsingPOST(command, method, params);
        Assert.assertFalse(result);
    }

    @Test
    public void isStateChangingCommandNotUsingPOSTReturnsFalseForWrongApiEnforcePostRequestsKey() {
        String command = "updateSomeApi";
        String method = "GET";
        Map<String, Object[]> params = new HashMap<>();
        params.put("name", STATE_CHANGING_COMMAND_CHECK_NAME_PARAM);
        boolean result = servlet.isStateChangingCommandNotUsingPOST(command, method, params);
        Assert.assertTrue(result);
    }

    @Test
    public void isStateChangingCommandNotUsingPOSTReturnsFalseForUpdateConfigurationNonEnforcePostRequestsKey() {
        String command = "updateConfiguration";
        String method = "GET";
        Map<String, Object[]> params = new HashMap<>();
        params.put("name", new String[] { "key" });
        boolean result = servlet.isStateChangingCommandNotUsingPOST(command, method, params);
        Assert.assertTrue(result);
    }

    @Test
    public void preservesPostParameterLoggingPolicy() {
        for (String command : new String[] {"addObjectStoragePool", "addHost", "createAccount"}) {
            Assert.assertFalse(servlet.shouldLogRequestParameters(command, new HashMap<>()));
        }
        Assert.assertTrue(servlet.shouldLogRequestParameters("listZones", new HashMap<>()));
        Map<String, String[]> params = new HashMap<>();
        params.put(ApiConstants.USER_DATA, new String[] {"USER_DATA_SENTINEL"});
        Assert.assertFalse(servlet.shouldLogRequestParameters("deployVirtualMachine", params));
    }

    @Test
    public void preservesAddHostQueryContextInAllRequestLogs() {
        String query = "command=addHost&zoneid=zone-1&username=test-user&password=PASSWORD_HEAD%26PASSWORD_TAIL&response=json";
        List<String> logs = captureRequestLogs("GET", query, queryParameters(query), true, false, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, "command=addHost&zoneid=zone-1&username=test-user&response=json", "PASSWORD_HEAD", "PASSWORD_TAIL");
        Mockito.verify(apiServer).handleRequest(Mockito.anyMap(), Mockito.eq("json"), Mockito.any(StringBuilder.class));
    }

    @Test
    public void preservesCreateAccountQueryContextInAllRequestLogs() {
        String query = "command=createAccount&username=test-user&account=test-account&password=PASSWORD_SENTINEL&response=json";
        List<String> logs = captureRequestLogs("GET", query, queryParameters(query), true, false, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, "command=createAccount&username=test-user&account=test-account&response=json", "PASSWORD_SENTINEL");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void redactsUserDataWithoutRemovingItsQueryContextOrChangingTheRequest() {
        String query = "command=deployVirtualMachine&id=vm-1&userdata=USER_DATA_HEAD%26USER_DATA_TAIL&response=json";
        List<String> logs = captureRequestLogs("GET", query, queryParameters(query), true, false, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, "command=deployVirtualMachine&id=vm-1&response=json", "USER_DATA_HEAD", "USER_DATA_TAIL");
        ArgumentCaptor<Map> captured = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(apiServer).handleRequest(captured.capture(), Mockito.eq("json"), Mockito.any(StringBuilder.class));
        Assert.assertArrayEquals(new String[] {"USER_DATA_HEAD&USER_DATA_TAIL"}, (Object[]) captured.getValue().get(ApiConstants.USER_DATA));
    }

    @Test
    public void preservesPostEnforcementAndRedactsRejectedObjectStoreGet() {
        String query = "command=addObjectStoragePool&name=store-1&provider=MinIO&details[0].key=accesskey"
                + "&details[0].value=ACCESS_SENTINEL&details[1].key=secretkey"
                + "&details[1].value=SECRET_HEAD%2B%26SECRET_TAIL%3D&response=json";
        List<String> logs = captureRequestLogs("GET", query, queryParameters(query), false, true, HttpServletResponse.SC_BAD_REQUEST);
        assertRequestLogs(logs, "command=addObjectStoragePool&name=store-1&provider=MinIO&response=json", "ACCESS_SENTINEL", "SECRET_HEAD", "SECRET_TAIL");
        Mockito.verify(apiServer, Mockito.never()).verifyRequest(Mockito.anyMap(), nullable(Long.class), Mockito.any(InetAddress.class));
        Mockito.verify(apiServer, Mockito.never()).handleRequest(Mockito.anyMap(), Mockito.anyString(), Mockito.any(StringBuilder.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void acceptsObjectStorePostWithoutLoggingOrChangingItsBody() {
        Map<String, String[]> params = queryParameters("command=addObjectStoragePool&name=store-1&provider=MinIO&response=json");
        params.put("url", new String[] {"https://storage.example.test"});
        params.put("details[0].key", new String[] {"accesskey"});
        params.put("details[0].value", new String[] {"BODY_ACCESS_SENTINEL"});
        params.put("details[1].key", new String[] {"secretkey"});
        params.put("details[1].value", new String[] {"BODY_SECRET+&=SENTINEL"});
        String responseBody = "{\"addobjectstoragepoolresponse\":{\"objectstore\":{\"id\":\"store-id\"}}}";
        Mockito.when(apiServer.handleRequest(Mockito.anyMap(), Mockito.eq("json"), Mockito.any(StringBuilder.class))).thenReturn(responseBody);
        List<String> logs = captureRequestLogs("POST", null, params, true, true, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, "command=addObjectStoragePool", "BODY_ACCESS_SENTINEL", "BODY_SECRET", "SENTINEL");
        Assert.assertEquals(responseBody, responseWriter.toString());
        ArgumentCaptor<Map> captured = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(apiServer).handleRequest(captured.capture(), Mockito.eq("json"), Mockito.any(StringBuilder.class));
        Assert.assertArrayEquals(new String[] {"BODY_SECRET+&=SENTINEL"}, (Object[]) captured.getValue().get("details[1].value"));
        Assert.assertArrayEquals(new String[] {"POST"}, (Object[]) captured.getValue().get("httpmethod"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void protectsMixedQueryAndBodyDetailsWithoutReinterpretingFieldCase() {
        String query = "command=addObjectStoragePool&name=store-1&provider=MinIO&response=json"
                + "&details[1].KEY=public&details[1].value=MIXED_SECRET_SENTINEL";
        Map<String, String[]> params = queryParameters(query);
        params.put("details[1].key", new String[] {"secretkey"});
        params.put("details[0].key", new String[] {"accesskey"});
        params.put("details[0].value", new String[] {"BODY_ACCESS_SENTINEL"});
        List<String> logs = captureRequestLogs("POST", query, params, true, true, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, "command=addObjectStoragePool&name=store-1&provider=MinIO&response=json", "MIXED_SECRET_SENTINEL", "BODY_ACCESS_SENTINEL");

        ArgumentCaptor<Map> captured = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(apiServer).handleRequest(captured.capture(), Mockito.eq("json"), Mockito.any(StringBuilder.class));
        Map<String, String> commandParameters = new HashMap<>();
        Map<String, Object[]> forwarded = captured.getValue();
        forwarded.forEach((key, values) -> commandParameters.put(key, (String) values[0]));
        AddObjectStoragePoolCmd command = new AddObjectStoragePoolCmd();
        DispatchTask task = new DispatchTask(command, commandParameters);
        new ParamUnpackWorker().handle(task);
        command.setDetails((Map) task.getParams().get(ApiConstants.DETAILS));
        Assert.assertEquals("MIXED_SECRET_SENTINEL", command.getDetails().get("secretkey"));
        Assert.assertEquals("BODY_ACCESS_SENTINEL", command.getDetails().get("accesskey"));
    }

    @Test
    public void retainsQueryMetadataButNeverCopiesPostOnlyFieldsIntoTheAccessLog() {
        String query = "name=query-store&provider=MinIO&response=json";
        Map<String, String[]> params = queryParameters(query);
        params.put(ApiConstants.COMMAND, new String[] {"addObjectStoragePool"});
        params.put("details[1].key", new String[] {"secretkey"});
        params.put("details[1].value", new String[] {"BODY_SECRET_SENTINEL"});
        params.put("tags", new String[] {"BODY_ONLY_TAG_SENTINEL"});
        List<String> logs = captureRequestLogs("POST", query, params, true, true, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, "command=addObjectStoragePool&name=query-store&provider=MinIO&response=json", "BODY_SECRET_SENTINEL", "BODY_ONLY_TAG_SENTINEL");
    }

    @Test
    public void preservesAuthorizationRejectionWithoutLoggingProviderDetails() {
        String query = "command=addObjectStoragePool&name=store-1&provider=Cloudian%20HyperStore"
                + "&details[2].key=s3Url&details[2].value=PROVIDER_DETAIL_SENTINEL&response=json";
        List<String> logs = captureRequestLogs("POST", query, queryParameters(query), false, true, HttpServletResponse.SC_UNAUTHORIZED);
        assertRequestLogs(logs, "command=addObjectStoragePool&name=store-1&provider=Cloudian%20HyperStore&response=json", "PROVIDER_DETAIL_SENTINEL");
        Mockito.verify(apiServer, Mockito.never()).handleRequest(Mockito.anyMap(), Mockito.anyString(), Mockito.any(StringBuilder.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void doesNotPrintDuplicateCredentialValuesAndKeepsExistingLastQueryValue() {
        String query = "command=addObjectStoragePool&name=store-1&details[1].key=secretkey"
                + "&details[1].value=FIRST_SECRET_SENTINEL&details[1].value=LAST_SECRET_SENTINEL&response=json";
        List<String> logs = captureRequestLogs("POST", query, queryParameters(query), true, true, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, "command=addObjectStoragePool&name=store-1&response=json", "FIRST_SECRET_SENTINEL", "LAST_SECRET_SENTINEL");
        Assert.assertTrue(logs.stream().anyMatch(line -> line.contains("has 2 values")));
        ArgumentCaptor<Map> captured = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(apiServer).handleRequest(captured.capture(), Mockito.eq("json"), Mockito.any(StringBuilder.class));
        Assert.assertArrayEquals(new String[] {"LAST_SECRET_SENTINEL"}, (Object[]) captured.getValue().get("details[1].value"));
    }

    @Test
    public void doesNotEchoUnstructuredQueryTokensBeforeRedaction() {
        String query = "command=addObjectStoragePool&name=store-1&MALFORMED_SECRET_SENTINEL&response=json";
        List<String> logs = captureRequestLogs("POST", query, queryParameters(query), false, false, HttpServletResponse.SC_UNAUTHORIZED);
        assertRequestLogs(logs, "command=addObjectStoragePool&name=store-1&response=json", "MALFORMED_SECRET_SENTINEL");
        Assert.assertTrue(logs.stream().anyMatch(line -> line.contains("Invalid parameter in URL found")));
    }

    @Test
    public void preservesBackendFailureStatusAndDescription() {
        String query = "command=addObjectStoragePool&name=store-1&details[1].key=secretkey"
                + "&details[1].value=ERROR_SECRET_SENTINEL&response=json";
        Mockito.when(apiServer.handleRequest(Mockito.anyMap(), Mockito.eq("json"), Mockito.any(StringBuilder.class)))
                .thenThrow(new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Provider unavailable"));
        List<String> logs = captureRequestLogs("POST", query, queryParameters(query), true, true, ApiErrorCode.INTERNAL_ERROR.getHttpCode());
        assertRequestLogs(logs, "command=addObjectStoragePool&name=store-1&response=json", "ERROR_SECRET_SENTINEL");
        Mockito.verify(response).setHeader("X-Description", "Provider unavailable");
    }

    @Test
    public void keepsOrdinaryGetAndPostDiagnostics() {
        String query = "command=listZones&name=zone-1&response=json";
        List<String> logs = captureRequestLogs("POST", query, queryParameters(query), true, false, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, query);
        Assert.assertTrue(logs.stream().anyMatch(line -> line.contains("===START===") && line.contains("\n") && line.contains("name=zone-1")));
        Assert.assertEquals(query, servlet.getCleanQueryString("listZones", query, queryParameters(query)));
    }

    @Test
    public void excludesObjectStorageDetailsRegardlessOfEncodingOrderOrDescriptor() {
        for (String fields : new String[] {
                "details%5b001%5d.KEY=public&DETAILS[%2B1].value=SECRET_SENTINEL",
                "details[1].value=SECRET_SENTINEL&details[1].key=secretkey",
                "details[-1]suffix.VALUE=SECRET_SENTINEL",
                "details[invalid].value=SECRET_SENTINEL",
                "details[1].value=SECRET_SENTINEL",
                "details=SECRET_SENTINEL"}) {
            String query = "command=addObjectStoragePool&name=store-1&" + fields + "&response=json";
            Assert.assertEquals("command=addObjectStoragePool&name=store-1&response=json",
                    servlet.getCleanQueryString("addObjectStoragePool", query, new HashMap<>()));
        }
    }

    @Test
    public void redactsCompleteSensitiveTopLevelValuesButKeepsNonSensitiveFlags() {
        String query = "command=listZones&userdataid=id-1&passwordenabled=true&newPassword=PASSWORD_HEAD%26PASSWORD_TAIL"
                + "&ACCESSKEY=ACCESS_SENTINEL&privatekey=PRIVATE_SENTINEL&SeSsIoNkEy=SESSION_SENTINEL%2B%3D"
                + "&secretcode=CODE_SENTINEL&token=RESET_TOKEN_SENTINEL&UserData=DATA_SENTINEL&response=json";
        Assert.assertEquals("command=listZones&userdataid=id-1&passwordenabled=true&response=json",
                servlet.getCleanQueryString("listZones", query, new HashMap<>()));
    }

    @Test
    public void keepsExistingNestedUrlCleaningAndOrdinaryDetails() {
        String query = "command=addHost&zoneid=zone-1&url=cifs%3A%2F%2Fstorage%2Fshare%3Fuser%3Dtest"
                + "%26password%3DPASSWORD_SENTINEL%26domain%3Dexample";
        Assert.assertEquals(StringUtils.cleanString(query), servlet.getCleanQueryString("addHost", query, new HashMap<>()));
        String ordinary = "command=deployVirtualMachine&details[0].key=keyboard&details[0].value=us&response=json";
        Assert.assertEquals(ordinary, servlet.getCleanQueryString("deployVirtualMachine", ordinary, new HashMap<>()));
    }

    @Test
    public void cannotEchoAnUndecodableQueryAndSanitizesCommandMetadata() {
        String query = "details%ZZ.value=SECRET_SENTINEL";
        Assert.assertEquals("command=addObjectStoragePool", servlet.getCleanQueryString("addObjectStoragePool", query, new HashMap<>()));
        Assert.assertEquals("", servlet.getCleanQueryString("listZones", query, new HashMap<>()));
        Map<String, String[]> params = new HashMap<>();
        params.put(ApiConstants.USER_DATA, new String[] {"DATA_SENTINEL"});
        Assert.assertEquals("command=unknown_command__", servlet.getCleanQueryString("unknown\ncommand\r\t", null, params));
    }

    @Test
    public void preservesContextWithoutExposingOtherSensitiveCommandsIndexedMaps() {
        String[][] cases = {
                {"createStoragePool", "details[0].key=secretkey&details[0].value=STORAGE_SECRET_SENTINEL%26TAIL"},
                {"createAccount", "accountdetails[0].key=secretkey&accountdetails[0].value=ACCOUNT_SECRET_SENTINEL"},
                {"addHost", "externaldetails[0].endpoint.password=HOST_SECRET_SENTINEL"}
        };
        for (String[] testCase : cases) {
            String query = "command=" + testCase[0] + "&name=resource-1&" + testCase[1] + "&response=json";
            Assert.assertEquals("command=" + testCase[0] + "&name=resource-1&response=json",
                    servlet.getCleanQueryString(testCase[0], query, new HashMap<>()));
        }
        String reset = "command=resetPassword&username=test-user&token=RESET_TOKEN_SENTINEL&password=PASSWORD_SENTINEL";
        Assert.assertEquals("command=resetPassword&username=test-user",
                servlet.getCleanQueryString("resetPassword", reset, new HashMap<>()));
    }

    @Test
    public void preservesImageStoreCredentialRedactionAfterRemovingTrailingSessionKey() {
        assertImageStoreQueryRemainsRedacted(false);
    }

    @Test
    public void preservesEncodedImageStoreCredentialRedactionAfterRemovingTrailingSessionKey() {
        assertImageStoreQueryRemainsRedacted(true);
    }

    @Test
    public void preservesIndexedCredentialRedactionAfterFilteringOtherTrailingFields() {
        String context = "command=addImageStore&provider=S3&name=store-1";
        String credentials = "&details[1].value=SECRET_SENTINEL&details[1].key=secretkey";
        for (String trailing : new String[] {"", "&sessionkey=SESSION_SENTINEL", "&password=PASSWORD_SENTINEL",
                "&token=TOKEN_SENTINEL", "&privatekey=PRIVATE_SENTINEL", "&userdata=USERDATA_SENTINEL"}) {
            String query = context + credentials + trailing;
            String cleaned = servlet.getCleanQueryString("addImageStore", query, queryParameters(query));
            Assert.assertTrue("Missing metadata: " + cleaned, cleaned.startsWith(context));
            Assert.assertFalse("Credential survived filtering: " + cleaned, cleaned.contains("SENTINEL"));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void assertImageStoreQueryRemainsRedacted(boolean encodedBrackets) {
        String context = "command=addImageStore&provider=S3&name=store-1&response=json"
                + "&details[2].key=bucket&details[2].value=archive";
        String query = context + "&details[0].key=accesskey&details[0].value=ACCESS_SENTINEL"
                + "&details[1].value=SECRET_SENTINEL&details[1].key=secretkey&sessionkey=SESSION_SENTINEL";
        if (encodedBrackets) {
            query = query.replace("[", "%5B").replace("]", "%5D");
            context = context.replace("[", "%5B").replace("]", "%5D");
        }
        List<String> logs = captureRequestLogs("GET", query, queryParameters(query), true, false, HttpServletResponse.SC_OK);
        assertRequestLogs(logs, context, "ACCESS_SENTINEL", "SECRET_SENTINEL", "SESSION_SENTINEL");

        ArgumentCaptor<Map> captured = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(apiServer).handleRequest(captured.capture(), Mockito.eq("json"), Mockito.any(StringBuilder.class));
        Map<String, String> unpackInput = new HashMap<>();
        Map<String, Object[]> forwarded = captured.getValue();
        forwarded.forEach((key, values) -> unpackInput.put(key, (String) values[0]));
        AddImageStoreCmd command = new AddImageStoreCmd();
        DispatchTask task = new DispatchTask(command, unpackInput);
        new ParamUnpackWorker().handle(task);
        command.setDetails((Map) task.getParams().get(ApiConstants.DETAILS));
        Map<String, String> expectedDetails = new HashMap<>();
        expectedDetails.put("accesskey", "ACCESS_SENTINEL");
        expectedDetails.put("secretkey", "SECRET_SENTINEL");
        expectedDetails.put("bucket", "archive");
        Assert.assertEquals(expectedDetails, command.getDetails());
    }

    private Map<String, String[]> queryParameters(String query) {
        Map<String, String[]> params = new HashMap<>();
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            String name = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            params.merge(name, new String[] {value}, (existing, added) -> {
                String[] values = Arrays.copyOf(existing, existing.length + 1);
                values[existing.length] = added[0];
                return values;
            });
        }
        return params;
    }

    private List<String> captureRequestLogs(String method, String query, Map<String, String[]> params,
            boolean authorized, boolean enforcePost, int expectedStatus) {
        Map<String, String[]> original = new HashMap<>();
        params.forEach((key, values) -> original.put(key, values.clone()));
        List<String> logs = new ArrayList<>();
        AbstractAppender appender = new AbstractAppender("ApiServletRequestLoggingTest", null, null, true, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                logs.add(event.getLoggerName() + ": " + event.getMessage().getFormattedMessage());
            }
        };
        org.apache.logging.log4j.core.Logger managementLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(ApiServlet.class);
        org.apache.logging.log4j.core.Logger accessLogger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger("apiserver." + ApiServlet.class.getName());
        Level managementLevel = managementLogger.getLevel();
        Level accessLevel = accessLogger.getLevel();
        appender.start();
        managementLogger.addAppender(appender);
        accessLogger.addAppender(appender);
        managementLogger.setLevel(Level.DEBUG);
        accessLogger.setLevel(Level.INFO);
        try {
            Mockito.when(request.getMethod()).thenReturn(method);
            Mockito.when(request.getQueryString()).thenReturn(query);
            Mockito.when(request.getParameterMap()).thenReturn(params);
            Mockito.when(authManager.getAPIAuthenticator(Mockito.anyString())).thenReturn(null);
            if (authorized) {
                Mockito.when(apiServer.verifyRequest(Mockito.anyMap(), nullable(Long.class), Mockito.any(InetAddress.class))).thenReturn(true);
            }
            if (enforcePost) {
                Mockito.when(apiServer.isPostRequestsAndTimestampsEnforced()).thenReturn(true);
            }
            servlet.processRequestInContext(request, response);
            Mockito.verify(response).setStatus(expectedStatus);
            Assert.assertEquals(original.keySet(), params.keySet());
            original.forEach((key, values) -> Assert.assertArrayEquals(values, params.get(key)));
            return logs;
        } finally {
            managementLogger.removeAppender(appender);
            accessLogger.removeAppender(appender);
            managementLogger.setLevel(managementLevel);
            accessLogger.setLevel(accessLevel);
            appender.stop();
        }
    }

    private void assertRequestLogs(List<String> logs, String expectedContext, String... secrets) {
        int boundaries = 0;
        for (String line : logs) {
            if (line.contains("===START===") || line.contains("===END===") || line.startsWith("apiserver.")) {
                boundaries++;
                Assert.assertTrue("Missing request context: " + line, line.contains(expectedContext));
            }
            for (String secret : secrets) {
                Assert.assertFalse("Sensitive data in log: " + line, line.contains(secret));
            }
        }
        Assert.assertEquals("Expected START, END and access records", 3, boundaries);
    }
}
