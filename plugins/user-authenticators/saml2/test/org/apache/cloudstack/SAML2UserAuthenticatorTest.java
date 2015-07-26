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

package org.apache.cloudstack;

import com.cloud.server.auth.UserAuthenticator.ActionOnFailedAuthentication;
import com.cloud.user.UserAccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Pair;
import org.apache.cloudstack.saml.SAMLPluginConstants;
import org.apache.cloudstack.saml.SAML2UserAuthenticator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class SAML2UserAuthenticatorTest {

    @Mock
    UserAccountDao userAccountDao;
    @Mock
    UserDao userDao;

    @Test
    public void encode() {
        Assert.assertTrue(new SAML2UserAuthenticator().encode("random String").length() > 0);
    }

    @Test
    public void authenticate() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        SAML2UserAuthenticator authenticator = new SAML2UserAuthenticator();

        Field daoField = SAML2UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, userAccountDao);

        Field userDaoField = SAML2UserAuthenticator.class.getDeclaredField("_userDao");
        userDaoField.setAccessible(true);
        userDaoField.set(authenticator, userDao);

        UserAccountVO account = new UserAccountVO();
        account.setPassword("5f4dcc3b5aa765d61d8327deb882cf99");
        account.setId(1L);

        UserVO user = new UserVO();
        Mockito.when(userAccountDao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(account);
        Mockito.when(userDao.getUser(Mockito.anyLong())).thenReturn(user);

        Pair<Boolean, ActionOnFailedAuthentication> pair;
        Map<String, Object[]> params = new HashMap<String, Object[]>();

        // When there is no SAMLRequest in params
        pair = authenticator.authenticate("someUID", "random", 1l, params);
        Assert.assertFalse(pair.first());

        // When there is SAMLRequest in params and user is same as the mocked one
        params.put(SAMLPluginConstants.SAML_RESPONSE, new String[]{"RandomString"});
        pair = authenticator.authenticate("someUID", "random", 1l, params);
        Assert.assertFalse(pair.first());

        // When there is SAMLRequest in params but username is null
        pair = authenticator.authenticate(null, "random", 1l, params);
        Assert.assertFalse(pair.first());

        // When there is SAMLRequest in params but username is empty
        pair = authenticator.authenticate("", "random", 1l, params);
        Assert.assertFalse(pair.first());

        // When there is SAMLRequest in params but username is not valid
        pair = authenticator.authenticate("someOtherUID", "random", 1l, params);
        Assert.assertFalse(pair.first());
    }
}
