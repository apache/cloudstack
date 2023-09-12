//  Licensed to the Apache Software Foundation (ASF) under one or more
//  contributor license agreements.  See the NOTICE file distributed with
//  this work for additional information regarding copyright ownership.
//  The ASF licenses this file to You under the Apache License, Version 2.0
//  (the "License"); you may not use this file except in compliance with
//  the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.apache.cloudstack.server.auth;

import com.cloud.user.UserAccountVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;
import org.apache.cloudstack.auth.UserAuthenticator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.security.NoSuchAlgorithmException;

@RunWith(MockitoJUnitRunner.class)
public class PBKD2UserAuthenticatorTest {
    @Mock
    UserAccountDao dao;

    @Test
    public void encodePasswordTest() {
        PBKDF2UserAuthenticator authenticator = new PBKDF2UserAuthenticator();
        String encodedPassword = authenticator.encode("password123ABCS!@#$%");
        Assert.assertTrue(encodedPassword.length() < 255 && encodedPassword.length() >= 182);
    }

    @Test
    public void saltTest() throws NoSuchAlgorithmException {
        byte[] salt = new PBKDF2UserAuthenticator().makeSalt();
        Assert.assertTrue(salt.length > 16);
    }

    @Test
    public void authenticateValidTest() throws IllegalAccessException, NoSuchFieldException {
        PBKDF2UserAuthenticator authenticator = new PBKDF2UserAuthenticator();
        Field daoField = PBKDF2UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, dao);
        UserAccountVO account = new UserAccountVO();
        account.setPassword("FMDMdx/2QjrZniqNRAgOAC1ai/CY/C+2kmKhp3vo+98pkqhO+AR6hCyUl0bOXtkq3XWqNiSQTwbi7KTiwuWhyw==:+u8T5LzCtikCPvKnUDn6JDezf1Hg2bood/ke5Oo93pz9s1eD9k/JLsa497Z3h9QWfOQfq0zvCRmkzfXMF913vQ==:4096");
        Mockito.when(dao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(account);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> pair = authenticator.authenticate("admin", "password", 1l, null);
        Assert.assertTrue(pair.first());
    }

    @Test
    public void authenticateInValidTest() throws IllegalAccessException, NoSuchFieldException {
        PBKDF2UserAuthenticator authenticator = new PBKDF2UserAuthenticator();
        Field daoField = PBKDF2UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, dao);
        UserAccountVO account = new UserAccountVO();
        account.setPassword("5f4dcc3b5aa765d61d8327deb882cf99");
        Mockito.when(dao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(account);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> pair = authenticator.authenticate("admin", "password", 1l, null);
        Assert.assertFalse(pair.first());
    }
}
