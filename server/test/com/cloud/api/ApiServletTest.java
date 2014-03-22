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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;

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
    User user;

    @Mock
    Account account;

    @Mock
    HttpSession session;

    StringWriter responseWriter;

    ApiServlet servlet;

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
        Field accountMgrField = ApiServlet.class
                .getDeclaredField("_accountMgr");
        accountMgrField.setAccessible(true);
        accountMgrField.set(servlet, accountService);

        Field apiServerField = ApiServlet.class.getDeclaredField("_apiServer");
        apiServerField.setAccessible(true);
        apiServerField.set(servlet, apiServer);
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

    @Test
    public void processRequestInContextUnauthorizedGET() {
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(
                apiServer.verifyRequest(Mockito.anyMap(), Mockito.anyLong()))
                .thenReturn(false);
        servlet.processRequestInContext(request, response);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        Mockito.verify(apiServer, Mockito.never()).handleRequest(
                Mockito.anyMap(), Mockito.anyString(),
                Mockito.any(StringBuffer.class));
    }

    @Test
    public void processRequestInContextAuthorizedGet() {
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(
                apiServer.verifyRequest(Mockito.anyMap(), Mockito.anyLong()))
                .thenReturn(true);
        servlet.processRequestInContext(request, response);
        Mockito.verify(response).setStatus(HttpServletResponse.SC_OK);
        Mockito.verify(apiServer, Mockito.times(1)).handleRequest(
                Mockito.anyMap(), Mockito.anyString(),
                Mockito.any(StringBuffer.class));
    }

    @Test
    public void processRequestInContextLougout() {
        Mockito.when(request.getMethod()).thenReturn("GET");
        Mockito.when(request.getSession(Mockito.anyBoolean())).thenReturn(
                session);
        Mockito.when(session.getAttribute("userid")).thenReturn(1l);
        Mockito.when(session.getAttribute("accountobj")).thenReturn(account);
        HashMap<String, String[]> params = new HashMap<String, String[]>();
        params.put(ApiConstants.COMMAND, new String[] { "logout" });
        Mockito.when(request.getParameterMap()).thenReturn(params);

        servlet.processRequestInContext(request, response);

        Mockito.verify(apiServer).logoutUser(1l);
        Mockito.verify(session).invalidate();
    }

}
