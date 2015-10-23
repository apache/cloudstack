/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.api.command;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.HttpUtils;
import junit.framework.TestCase;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ListAndSwitchSAMLAccountCmdTest extends TestCase {
    @Mock
    ApiServerService apiServer;

    @Mock
    SAML2AuthManager samlAuthManager;

    @Mock
    AccountService accountService;

    @Mock
    UserAccountDao userAccountDao;

    @Mock
    UserDao userDao;

    @Mock
    DomainDao domainDao;

    @Mock
    HttpSession session;

    @Mock
    HttpServletResponse resp;

    @Mock
    HttpServletRequest req;

    @Test
    public void testListAndSwitchSAMLAccountCmd() throws Exception {
        // Setup
        final Map<String, Object[]> params = new HashMap<String, Object[]>();
        final String sessionKeyValue = "someSessionIDValue";
        Mockito.when(session.getAttribute(ApiConstants.SESSIONKEY)).thenReturn(sessionKeyValue);
        Mockito.when(session.getAttribute("userid")).thenReturn(2L);
        params.put(ApiConstants.USER_ID, new String[]{"2"});
        params.put(ApiConstants.DOMAIN_ID, new String[]{"1"});
        Mockito.when(userDao.findByUuid(Mockito.anyString())).thenReturn(new UserVO(2L));
        Mockito.when(domainDao.findByUuid(Mockito.anyString())).thenReturn(new DomainVO());

        // Mock/field setup
        ListAndSwitchSAMLAccountCmd cmd = new ListAndSwitchSAMLAccountCmd();
        Field apiServerField = ListAndSwitchSAMLAccountCmd.class.getDeclaredField("_apiServer");
        apiServerField.setAccessible(true);
        apiServerField.set(cmd, apiServer);

        Field managerField = ListAndSwitchSAMLAccountCmd.class.getDeclaredField("_samlAuthManager");
        managerField.setAccessible(true);
        managerField.set(cmd, samlAuthManager);

        Field accountServiceField = BaseCmd.class.getDeclaredField("_accountService");
        accountServiceField.setAccessible(true);
        accountServiceField.set(cmd, accountService);

        Field userAccountDaoField = ListAndSwitchSAMLAccountCmd.class.getDeclaredField("_userAccountDao");
        userAccountDaoField.setAccessible(true);
        userAccountDaoField.set(cmd, userAccountDao);

        Field userDaoField = ListAndSwitchSAMLAccountCmd.class.getDeclaredField("_userDao");
        userDaoField.setAccessible(true);
        userDaoField.set(cmd, userDao);

        Field domainDaoField = ListAndSwitchSAMLAccountCmd.class.getDeclaredField("_domainDao");
        domainDaoField.setAccessible(true);
        domainDaoField.set(cmd, domainDao);

        // invalid session test
        try {
            cmd.authenticate("command", params, null, null, HttpUtils.RESPONSE_TYPE_JSON, new StringBuilder(), req, resp);
        } catch (ServerApiException exception) {
            assertEquals(exception.getErrorCode(), ApiErrorCode.UNAUTHORIZED);
        } finally {
            Mockito.verify(accountService, Mockito.times(0)).getUserAccountById(Mockito.anyLong());
        }

        // invalid sessionkey value test
        params.put(ApiConstants.SESSIONKEY, new String[]{"someOtherValue"});
        try {
            Mockito.when(session.isNew()).thenReturn(false);
            cmd.authenticate("command", params, session, null, HttpUtils.RESPONSE_TYPE_JSON, new StringBuilder(), req, resp);
        } catch (ServerApiException exception) {
            assertEquals(exception.getErrorCode(), ApiErrorCode.UNAUTHORIZED);
        } finally {
            Mockito.verify(accountService, Mockito.times(0)).getUserAccountById(Mockito.anyLong());
        }

        // valid sessionkey value test
        params.put(ApiConstants.SESSIONKEY, new String[]{sessionKeyValue});
        try {
            cmd.authenticate("command", params, session, null, HttpUtils.RESPONSE_TYPE_JSON, new StringBuilder(), req, resp);
        } catch (ServerApiException exception) {
            assertEquals(exception.getErrorCode(), ApiErrorCode.ACCOUNT_ERROR);
        } finally {
            Mockito.verify(accountService, Mockito.times(1)).getUserAccountById(Mockito.anyLong());
        }

        // valid sessionkey, invalid useraccount type (non-saml) value test
        UserAccountVO mockedUserAccount = new UserAccountVO();
        mockedUserAccount.setId(2L);
        mockedUserAccount.setAccountState(Account.State.enabled.toString());
        mockedUserAccount.setUsername("someUsername");
        mockedUserAccount.setExternalEntity("some IDP ID");
        mockedUserAccount.setDomainId(0L);
        mockedUserAccount.setSource(User.Source.UNKNOWN);
        Mockito.when(accountService.getUserAccountById(Mockito.anyLong())).thenReturn(mockedUserAccount);
        try {
            cmd.authenticate("command", params, session, null, HttpUtils.RESPONSE_TYPE_JSON, new StringBuilder(), req, resp);
        } catch (ServerApiException exception) {
            assertEquals(exception.getErrorCode(), ApiErrorCode.ACCOUNT_ERROR);
        } finally {
            // accountService should have been called twice by now, for this case and the case above
            Mockito.verify(accountService, Mockito.times(2)).getUserAccountById(Mockito.anyLong());
        }

        // all valid test
        mockedUserAccount.setSource(User.Source.SAML2);
        Mockito.when(accountService.getUserAccountById(Mockito.anyLong())).thenReturn(mockedUserAccount);
        Mockito.when(apiServer.verifyUser(Mockito.anyLong())).thenReturn(true);
        LoginCmdResponse loginCmdResponse = new LoginCmdResponse();
        loginCmdResponse.setUserId("1");
        loginCmdResponse.setDomainId("1");
        loginCmdResponse.setType("1");
        loginCmdResponse.setUsername("userName");
        loginCmdResponse.setAccount("someAccount");
        loginCmdResponse.setFirstName("firstName");
        loginCmdResponse.setLastName("lastName");
        loginCmdResponse.setSessionKey("newSessionKeyString");
        Mockito.when(apiServer.loginUser(Mockito.any(HttpSession.class), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyLong(), Mockito.anyString(), Mockito.any(InetAddress.class), Mockito.anyMap())).thenReturn(loginCmdResponse);
        try {
            cmd.authenticate("command", params, session, null, HttpUtils.RESPONSE_TYPE_JSON, new StringBuilder(), req, resp);
        } catch (ServerApiException exception) {
            fail("SAML list and switch account API failed to pass for all valid data: " + exception.getMessage());
        } finally {
            // accountService should have been called 4 times by now, for this case twice and 2 for cases above
            Mockito.verify(accountService, Mockito.times(4)).getUserAccountById(Mockito.anyLong());
            Mockito.verify(resp, Mockito.times(1)).sendRedirect(Mockito.anyString());
        }
    }

    @Test
    public void testGetAPIType() {
        Assert.assertTrue(new ListAndSwitchSAMLAccountCmd().getAPIType() == APIAuthenticationType.READONLY_API);
    }

}
