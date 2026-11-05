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

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.resourcedetail.UserDetailVO;
import org.apache.cloudstack.resourcedetail.dao.UserDetailsDao;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.domain.Domain;
import com.cloud.user.AccountService;
import com.cloud.user.DomainService;
import com.cloud.user.UserAccount;

@RunWith(MockitoJUnitRunner.class)
public class DefaultLoginAPIAuthenticatorCmdTest {

    private static final String USERNAME = "user@example.com";
    private static final long DOMAIN_ID = 1L;
    private static final long USER_ID = 42L;
    private static final String SERIALIZED_RESPONSE = "{\"loginresponse\":{}}";

    @Mock
    ApiServerService apiServer;
    @Mock
    UserDetailsDao userDetailsDao;
    @Mock
    AccountService accountService;
    @Mock
    DomainService domainService;
    @Mock
    HttpSession session;
    @Mock
    HttpServletRequest req;
    @Mock
    HttpServletResponse resp;
    @Mock
    Domain domain;
    @Mock
    ResponseObject loginResponse;

    DefaultLoginAPIAuthenticatorCmd cmd = new DefaultLoginAPIAuthenticatorCmd();

    private MockedStatic<ApiResponseSerializer> serializerMock;
    private Map<String, Object[]> params;

    @Before
    public void setUp() throws Exception {
        cmd._apiServer = apiServer;
        cmd._accountService = accountService;
        cmd._domainService = domainService;
        cmd.userDetailsDao = userDetailsDao;

        params = new HashMap<>();
        params.put(ApiConstants.USERNAME, new String[] {USERNAME});
        params.put(ApiConstants.PASSWORD, new String[] {"password"});
        params.put(ApiConstants.DOMAIN, new String[] {"/"});

        Mockito.when(req.getMethod()).thenReturn("POST");
        Mockito.when(domain.getId()).thenReturn(DOMAIN_ID);
        Mockito.when(domainService.findDomainByIdOrPath(Mockito.any(), Mockito.anyString())).thenReturn(domain);
        Mockito.when(apiServer.loginUser(Mockito.eq(session), Mockito.eq(USERNAME), Mockito.anyString(), Mockito.eq(DOMAIN_ID),
                Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(loginResponse);
        Mockito.when(session.getAttribute("userid")).thenReturn(USER_ID);

        serializerMock = Mockito.mockStatic(ApiResponseSerializer.class);
        serializerMock.when(() -> ApiResponseSerializer.toSerializedString(loginResponse, "json")).thenReturn(SERIALIZED_RESPONSE);
    }

    @After
    public void tearDown() {
        serializerMock.close();
    }

    private String authenticate() throws Exception {
        return cmd.authenticate("login", params, session, InetAddress.getLoopbackAddress(), "json", new StringBuilder(), req, resp);
    }

    @Test
    public void authenticateUserCreatedDuringLoginReturnsLoginResponse() throws Exception {
        Mockito.when(accountService.getActiveUserAccount(USERNAME, DOMAIN_ID)).thenReturn(null);

        Assert.assertEquals(SERIALIZED_RESPONSE, authenticate());
        Mockito.verify(userDetailsDao).removeDetail(USER_ID, UserDetailVO.OauthLogin);
    }

    @Test
    public void authenticateExistingUserRemovesOauthLoginDetail() throws Exception {
        UserAccount userAccount = Mockito.mock(UserAccount.class);
        Mockito.when(accountService.getActiveUserAccount(USERNAME, DOMAIN_ID)).thenReturn(userAccount);

        Assert.assertEquals(SERIALIZED_RESPONSE, authenticate());
        Mockito.verify(userDetailsDao).removeDetail(USER_ID, UserDetailVO.OauthLogin);
    }
}
