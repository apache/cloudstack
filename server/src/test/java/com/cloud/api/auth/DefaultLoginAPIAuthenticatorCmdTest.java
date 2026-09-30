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
package com.cloud.api.auth;

import com.cloud.domain.Domain;
import com.cloud.user.AccountService;
import com.cloud.user.DomainService;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.resourcedetail.dao.UserDetailsDao;
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
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DefaultLoginAPIAuthenticatorCmdTest {

    private static final String USERNAME = "ldap-user";
    private static final long DOMAIN_ID = 2L;

    @Mock
    private ApiServerService apiServer;

    @Mock
    private AccountService accountService;

    @Mock
    private DomainService domainService;

    @Mock
    private UserDetailsDao userDetailsDao;

    @Mock
    private HttpSession session;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Domain domain;

    private DefaultLoginAPIAuthenticatorCmd command;

    @Before
    public void setUp() {
        command = new DefaultLoginAPIAuthenticatorCmd();
        command._apiServer = apiServer;
        command._accountService = accountService;
        command._domainService = domainService;
        command.userDetailsDao = userDetailsDao;
    }

    @Test
    public void testAuthenticateFirstLdapLoginWithoutExistingUser() throws Exception {
        Map<String, Object[]> params = new HashMap<>();
        params.put(ApiConstants.USERNAME, new String[] {USERNAME});
        params.put(ApiConstants.PASSWORD, new String[] {"password"});
        LoginCmdResponse loginResponse = new LoginCmdResponse();
        loginResponse.setResponseName("loginresponse");

        Mockito.when(request.getMethod()).thenReturn("POST");
        Mockito.when(apiServer.getDomainId(params)).thenReturn(null);
        Mockito.when(domainService.findDomainByIdOrPath(null, null)).thenReturn(domain);
        Mockito.when(domain.getId()).thenReturn(DOMAIN_ID);
        Mockito.when(accountService.getActiveUserAccount(USERNAME, DOMAIN_ID)).thenReturn(null);
        Mockito.when(apiServer.loginUser(Mockito.eq(session), Mockito.eq(USERNAME), Mockito.eq("password"),
                Mockito.eq(DOMAIN_ID), Mockito.isNull(), Mockito.any(InetAddress.class), Mockito.eq(params)))
                .thenReturn(loginResponse);

        String result = command.authenticate("login", params, session, InetAddress.getLoopbackAddress(),
                "json", new StringBuilder(), request, response);

        Assert.assertEquals("{\"loginresponse\":{}}", result);
        Mockito.verify(userDetailsDao, Mockito.never()).removeDetail(Mockito.anyLong(), Mockito.anyString());
    }
}
