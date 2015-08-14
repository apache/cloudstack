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

import com.cloud.user.DomainManager;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import junit.framework.TestCase;
import org.apache.cloudstack.framework.security.keystore.KeystoreDao;
import org.apache.cloudstack.saml.SAML2AuthManagerImpl;
import org.apache.cloudstack.saml.SAMLTokenDao;
import org.apache.cloudstack.saml.SAMLTokenVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;

@RunWith(MockitoJUnitRunner.class)
public class SAML2AuthManagerImplTest extends TestCase {
    @Mock
    private KeystoreDao ksDao;

    @Mock
    private SAMLTokenDao samlTokenDao;

    @Mock
    private UserDao userDao;

    @Mock
    DomainManager domainMgr;

    SAML2AuthManagerImpl saml2AuthManager;

    @Override
    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        saml2AuthManager = Mockito.spy(new SAML2AuthManagerImpl());

        Field ksDaoField = SAML2AuthManagerImpl.class.getDeclaredField("_ksDao");
        ksDaoField.setAccessible(true);
        ksDaoField.set(saml2AuthManager, ksDao);

        Field samlTokenDaoField = SAML2AuthManagerImpl.class.getDeclaredField("_samlTokenDao");
        samlTokenDaoField.setAccessible(true);
        samlTokenDaoField.set(saml2AuthManager, samlTokenDao);

        Field userDaoField = SAML2AuthManagerImpl.class.getDeclaredField("_userDao");
        userDaoField.setAccessible(true);
        userDaoField.set(saml2AuthManager, userDao);

        Field domainMgrField = SAML2AuthManagerImpl.class.getDeclaredField("_domainMgr");
        domainMgrField.setAccessible(true);
        domainMgrField.set(saml2AuthManager, domainMgr);

        // enable the plugin
        Mockito.doReturn(true).when(saml2AuthManager).isSAMLPluginEnabled();
    }

    @Test
    public void testIsUserAuthorized() {
        final String entityID = "some IDP ID";

        // Test unauthorized user
        UserVO user = new UserVO(200L);
        user.setUsername("someuser");
        user.setSource(User.Source.UNKNOWN);
        user.setExternalEntity(entityID);
        Mockito.when(userDao.getUser(Mockito.anyLong())).thenReturn(user);
        assertFalse(saml2AuthManager.isUserAuthorized(user.getId(), "someID"));

        // Test authorized user with wrong IDP
        user.setSource(User.Source.SAML2);
        Mockito.when(userDao.getUser(Mockito.anyLong())).thenReturn(user);
        assertFalse(saml2AuthManager.isUserAuthorized(user.getId(), "someID"));

        // Test authorized user with wrong IDP
        user.setSource(User.Source.SAML2);
        Mockito.when(userDao.getUser(Mockito.anyLong())).thenReturn(user);
        assertTrue(saml2AuthManager.isUserAuthorized(user.getId(), entityID));
    }

    @Test
    public void testAuthorizeUser() {
        // Test invalid user
        Mockito.when(userDao.getUser(Mockito.anyLong())).thenReturn(null);
        assertFalse(saml2AuthManager.authorizeUser(1L, "someID", true));

        // Test valid user
        UserVO user = new UserVO(200L);
        user.setUsername("someuser");
        Mockito.when(userDao.getUser(Mockito.anyLong())).thenReturn(user);
        assertTrue(saml2AuthManager.authorizeUser(1L, "someID", true));
        Mockito.verify(userDao, Mockito.atLeastOnce()).update(Mockito.anyLong(), Mockito.any(user.getClass()));
    }



    @Test
    public void testSaveToken() {
        // duplicate token test
        Mockito.when(samlTokenDao.findByUuid(Mockito.anyString())).thenReturn(new SAMLTokenVO());
        saml2AuthManager.saveToken("someAuthnID", null, "https://idp.bhaisaab.org/profile/shibboleth");
        Mockito.verify(samlTokenDao, Mockito.times(0)).persist(Mockito.any(SAMLTokenVO.class));

        // valid test
        Mockito.when(samlTokenDao.findByUuid(Mockito.anyString())).thenReturn(null);
        saml2AuthManager.saveToken("someAuthnID", null, "https://idp.bhaisaab.org/profile/shibboleth");
        Mockito.verify(samlTokenDao, Mockito.times(1)).persist(Mockito.any(SAMLTokenVO.class));
    }

    @Test
    public void testGetToken() {
        SAMLTokenVO randomToken = new SAMLTokenVO("uuid", 1L, "someIDPDI");
        Mockito.when(samlTokenDao.findByUuid(Mockito.anyString())).thenReturn(randomToken);
        assertEquals(saml2AuthManager.getToken("someAuthnID"), randomToken);
    }

    @Test
    public void testExpireToken() {
        saml2AuthManager.expireTokens();
        Mockito.verify(samlTokenDao, Mockito.atLeast(1)).expireTokens();
    }

    @Test
    public void testPluginEnabled() {
        assertTrue(saml2AuthManager.isSAMLPluginEnabled());
    }

    @Test
    public void testPluginComponentName() {
        assertEquals(saml2AuthManager.getConfigComponentName(), "SAML2-PLUGIN");
    }

    @Test
    public void testGetCommands() {
        // Plugin enabled
        assertTrue(saml2AuthManager.getCommands().size() > 0);
        assertTrue(saml2AuthManager.getAuthCommands().size() > 0);

        // Plugin disabled
        Mockito.doReturn(false).when(saml2AuthManager).isSAMLPluginEnabled();
        assertTrue(saml2AuthManager.getCommands().size() == 0);
        assertTrue(saml2AuthManager.getAuthCommands().size() == 0);
        // Re-enable the plugin
        Mockito.doReturn(true).when(saml2AuthManager).isSAMLPluginEnabled();
    }

    @Test
    public void testConfigKeys() {
        assertTrue(saml2AuthManager.getConfigKeys().length > 0);
    }
}
