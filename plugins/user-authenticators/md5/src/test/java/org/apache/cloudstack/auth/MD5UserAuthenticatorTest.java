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

package org.apache.cloudstack.auth;

import java.lang.reflect.Field;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.user.UserAccountVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class MD5UserAuthenticatorTest {
    @Mock
    UserAccountDao dao;

    @Test
    public void encode() {
        Assert.assertEquals("5f4dcc3b5aa765d61d8327deb882cf99",
                new MD5UserAuthenticator().encode("password"));
    }

    @Test
    public void authenticate() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        MD5UserAuthenticator authenticator = new MD5UserAuthenticator();
        Field daoField = MD5UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, dao);
        UserAccountVO account = new UserAccountVO();
        account.setPassword("5f4dcc3b5aa765d61d8327deb882cf99");
        Mockito.when(dao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(account);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> pair = authenticator.authenticate("admin", "password", 1l, null);
        Assert.assertTrue(pair.first());
    }

    @Test
    public void authenticateBadPass() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        MD5UserAuthenticator authenticator = new MD5UserAuthenticator();
        Field daoField = MD5UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, dao);
        UserAccountVO account = new UserAccountVO();
        account.setPassword("surprise");
        Mockito.when(dao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(account);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> pair = authenticator.authenticate("admin", "password", 1l, null);
        Assert.assertFalse(pair.first());
    }

    @Test
    public void authenticateBadUser() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        MD5UserAuthenticator authenticator = new MD5UserAuthenticator();
        Field daoField = MD5UserAuthenticator.class.getDeclaredField("_userAccountDao");
        daoField.setAccessible(true);
        daoField.set(authenticator, dao);
        Mockito.when(dao.getUserAccount(Mockito.anyString(), Mockito.anyLong())).thenReturn(null);
        Pair<Boolean, UserAuthenticator.ActionOnFailedAuthentication> pair = authenticator.authenticate("admin", "password", 1l, null);
        Assert.assertFalse(pair.first());
    }
}
